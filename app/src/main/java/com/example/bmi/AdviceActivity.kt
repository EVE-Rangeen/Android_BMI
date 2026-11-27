package com.example.bmi

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.example.bmi.data.Dates
import com.example.bmi.data.WellnessDbHelper
import com.example.bmi.ui.advice.AdviceRequest
import com.example.bmi.ui.advice.AdviceUiState
import com.example.bmi.ui.advice.AdviceViewModel

/*
 * ============================================================================
 * AdviceActivity.kt —— AI 健康建议页（【AI 功能 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   1. 从 SQLite 取出“今天”的记录（没有就提示先保存）；
 *   2. 显示这条记录的摘要；
 *   3. 让 ViewModel 去请求 DeepSeek，把返回的建议显示出来；
 *   4. 请求失败时显示原因，并提供“重试”按钮。
 *
 * 【MVVM 在这里怎么分工（考试重点）】
 *   Activity（V，View）：只管“显示”和“触发”，不认识 HTTP、不处理异常；
 *   ViewModel（VM）     ：拿着 Repository 发请求，把结果/异常转成 AdviceUiState；
 *   Repository（M，Model）：真正从网络拿数据。
 *   这样 Activity 里没有一行网络代码，逻辑也能单独测试。
 *
 * 【为什么用 observe 而不是“等结果返回再 setText”】
 *   viewModel.adviceState.observe(this) { ... } 注册一次，
 *   之后每次状态变化（Loading → Success/Error）都会自动回调，
 *   界面永远是当前状态的正确样子；而且 Activity 销毁后会自动解除观察，不会泄漏。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：AdviceViewModel、AdviceUiState、WellnessDbHelper.findByDate、
 *               Dates、activity_advice.xml、strings.xml（ai_* 文案）。
 *   谁依赖本文件：MainActivity（菜单“AI 健康建议”跳转）。
 *   ★ 考试不做 AI 时，按 docs/exam-guide.md B 部分整块删除：
 *     本文件、activity_advice.xml、ui/advice/ 目录、data/ 下 DeepSeekClient、
 *     PromptBuilder、WellnessAdviceRepository、
 *     Manifest 里的 AdviceActivity 注册和 INTERNET 权限、
 *     build.gradle 里的 BuildConfig 注入和协程/ViewModel 依赖、
 *     MainActivity 菜单里的 menu_ai_advice 分支。
 * ────────────────────────────────────────────────────────────────────────────
 */
class AdviceActivity : BaseActivity() {

    // by viewModels()：自动创建并绑定一个 AdviceViewModel（跟随本 Activity 生命周期）。
    private val viewModel: AdviceViewModel by viewModels()

    // by lazy：第一次用到时才创建数据库助手。
    private val db by lazy { WellnessDbHelper(this) }

    private lateinit var summaryTV: TextView   // 记录摘要
    private lateinit var noteTV: TextView      // 备注行
    private lateinit var progress: ProgressBar // 加载转圈
    private lateinit var adviceTV: TextView    // AI 建议正文
    private lateinit var retryBtn: Button      // 重试按钮

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advice)

        // 【语言切换 · 考试可删】标题跟着 App 内语言走（原因见 MainActivity 的说明）。
        title = getString(R.string.ai_title)

        summaryTV = findViewById(R.id.adviceSummary)
        noteTV = findViewById(R.id.adviceNote)
        progress = findViewById(R.id.adviceProgress)
        adviceTV = findViewById(R.id.adviceText)
        retryBtn = findViewById(R.id.adviceRetry)

        // 查今天的记录。日期统一用 Dates.today()，和主界面保存时用的是同一个格式。
        val record = db.findByDate(Dates.today())

        if (record == null) {
            // 今天还没记录：只显示一句提示，不发请求（没有数据可分析）。
            summaryTV.setText(R.string.ai_no_record)
            noteTV.isVisible = false
            adviceTV.isVisible = false
            progress.isVisible = false
            retryBtn.isVisible = false
            return
        }

        // 摘要：日期 · 身体状况 · 步数（占位符按顺序对应 %1$s %2$s %3$d）。
        summaryTV.text = getString(
            R.string.ai_record_summary,
            record.date,
            conditionText(record.condition),
            record.steps
        )

        // 备注为空就整行隐藏，避免界面上出现“备注：”后面什么都没有。
        if (record.note.isBlank()) {
            noteTV.isVisible = false
        } else {
            noteTV.text = getString(R.string.ai_note, record.note)
        }

        // 重试按钮 → 交给 ViewModel 用上一次的请求再发一次。
        retryBtn.setOnClickListener { viewModel.retry() }

        // 观察状态：状态一变就刷新界面。
        viewModel.adviceState.observe(this) { state -> render(state) }

        /*
         * 只在“还没请求过”时才发请求。
         * ViewModel 在旋转屏幕时不会销毁，所以 hasRequested 能防止旋转后重复请求。
         */
        if (!viewModel.hasRequested) {
            viewModel.loadAdvice(AdviceRequest(record, currentLanguage()))
        }
    }

    /** 把 ViewModel 的状态渲染到界面上（四种状态一一对应）。 */
    private fun render(state: AdviceUiState) {
        when (state) {
            AdviceUiState.Idle -> {
                progress.isVisible = false
                adviceTV.text = ""
                retryBtn.isVisible = false
            }

            AdviceUiState.Loading -> {
                progress.isVisible = true                 // 显示转圈
                adviceTV.setText(R.string.ai_loading)
                retryBtn.isVisible = false
            }

            is AdviceUiState.Success -> {
                progress.isVisible = false
                adviceTV.text = state.text                // AI 返回的建议
                retryBtn.isVisible = false
            }

            is AdviceUiState.Error -> {
                progress.isVisible = false
                // reason.messageRes 是错误类型对应的文案 id，getString 转成当前语言。
                adviceTV.text = getString(state.reason.messageRes)
                retryBtn.isVisible = true                 // 让用户重试
            }
        }
    }

    /**
     * 把数据库里的 0/1/2 翻成当前语言的文字。
     * （HistoryActivity 里有一份同样的映射，故意各自保留一个小函数，
     *   这样任一面被删除时另一面不受影响。）
     */
    private fun conditionText(condition: Int): String = getString(
        when (condition) {
            0 -> R.string.wellness_condition_good
            1 -> R.string.wellness_condition_fair
            else -> R.string.wellness_condition_poor
        }
    )
}
