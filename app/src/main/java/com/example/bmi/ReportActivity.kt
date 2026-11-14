package com.example.bmi

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.ConfigurationCompat
import androidx.core.view.isVisible
import com.example.bmi.data.AppLanguage
import com.example.bmi.data.BmiDates
import com.example.bmi.data.BmiHistoryRepository
import com.example.bmi.data.BmiProfile
import com.example.bmi.data.BmiRecord
import com.example.bmi.data.Gender
import com.example.bmi.ui.report.AdviceRequest
import com.example.bmi.ui.report.AdviceUiState
import com.example.bmi.ui.report.ReportViewModel
import java.text.DecimalFormat
import java.util.Locale

/*
 * ============================================================================
 * ReportActivity.kt —— 报告页（计算 BMI + 本地建议 + AI 建议 + 保存历史）
 * ============================================================================
 * 【这个文件是干什么的】
 *   MainActivity 跳转到这里。这里做四件事：
 *     1. 从 Intent 里取出 MainActivity 传过来的身高/体重/年龄/性别；
 *     2. 用公式 BMI = 体重(kg) / 身高(m)^2 计算结果并显示；
 *     3. 根据年龄性别查表，给出一条“本地建议”（偏瘦/正常/偏重）；
 *     4. 把本次结果按“天”存进 SQLite（供趋势图用）；
 *     5. （可选）调用 AI 接口要一份更详细的健康建议。
 *
 * 【MVVM 架构在这里的体现】
 *   本页面只负责“展示”和“触发”，所有跟网络/状态相关的逻辑都在 ReportViewModel 里。
 *   Activity 只做两件事：
 *     - 调 viewModel.loadAdvice(...) 发起请求；
 *     - observe（观察）viewModel.adviceState 并照着渲染界面。
 */
class ReportActivity : AppCompatActivity() {

    // by viewModels() 会自动帮我们创建并绑定一个 ReportViewModel（跟随本 Activity 生命周期）。
    private val viewModel: ReportViewModel by viewModels()

    // by lazy 表示“用到的时候才创建”，这里延迟到第一次保存记录时才创建数据库助手。
    private val historyRepository by lazy { BmiHistoryRepository(this) }

    // 界面上的控件引用。
    private lateinit var resultTV: TextView      // 显示 BMI 数值
    private lateinit var adviceTV: TextView      // 显示本地建议（偏瘦/正常/偏重）
    private lateinit var resultImage: ImageView  // 显示对应状态的小图
    private lateinit var llmAdviceTV: TextView   // 显示 AI 建议文字
    private lateinit var llmProgress: ProgressBar// AI 加载中的转圈
    private lateinit var llmRetry: Button        // AI 失败后的“重试”按钮

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        // 绑定控件。
        resultTV = findViewById(R.id.report_result)
        adviceTV = findViewById(R.id.report_advice)
        resultImage = findViewById(R.id.report_image)
        llmAdviceTV = findViewById(R.id.report_llm_advice)
        llmProgress = findViewById(R.id.report_llm_progress)
        llmRetry = findViewById(R.id.report_llm_retry)

        /*
         * 从 Intent 里读取 MainActivity 传来的数据，封装成 BmiProfile。
         * 如果 Intent 没有数据（比如有人直接打开这个页面），readProfile() 会 finish() 结束本页。
         */
        val profile = readProfile() ?: return
        showLocalResult(profile)   // 先显示本地计算的 BMI 结果和建议

        /*
         * 保存历史记录。为什么加 savedInstanceState == null 判断？
         * 旋转屏幕时 onCreate 会被再次调用，如果每次旋转都存一次，会重复写数据库。
         * 加这个判断后，只有“第一次创建”才保存（旋转后的重建不重复保存）。
         */
        if (savedInstanceState == null) {
            saveToHistory(profile)
        }

        // 点击“重试”按钮 → 交给 ViewModel 重新发一次 AI 请求。
        llmRetry.setOnClickListener { viewModel.retry() }

        // 观察 AI 状态：状态一变，就调用 renderLlmState 更新界面。
        viewModel.adviceState.observe(this) { state -> renderLlmState(state) }

        /*
         * 只在“还没有请求过”时发请求。ViewModel 在旋转屏幕时不会销毁，
         * 所以 hasRequested 能防止旋转后重复发起（也就不会重复扣费）。
         */
        if (!viewModel.hasRequested) {
            viewModel.loadAdvice(AdviceRequest(profile, currentLanguage()))
        }
    }

    /**
     * 从 Intent 的 extras 里取出 MainActivity 传过来的数据，算出 BmiProfile。
     *
     * @return 数据齐全时返回 BmiProfile；否则结束本页并返回 null。
     */
    private fun readProfile(): BmiProfile? {
        val bundle = intent.extras
        if (bundle == null) {
            finish()   // 没有数据，直接关闭页面
            return null
        }
        /*
         * 【重点】从 Intent 取值：
         *   getString("height") 对应 MainActivity 里 bundle.putString("height", height)，
         *   key 必须完全一致（大小写也不能差）。
         *   !! 表示“我确定它不是 null”（因为 MainActivity 一定会传这些 key）。
         *
         * 身高是厘米，转成米：/ 100。
         */
        val heightMeters = bundle.getString("height")!!.toDouble() / 100
        val weightKg = bundle.getString("weight")!!.toDouble()
        val age = bundle.getString("age")!!.toInt()
        // gender 传的是位置 0/1，用 Gender.fromSpinnerPosition 翻译成 MALE/FEMALE。
        val gender = Gender.fromSpinnerPosition(bundle.getInt("gender", 0))
        return BmiProfile(
            bmi = weightKg / (heightMeters * heightMeters),   // BMI = 体重 / 身高²
            age = age,
            gender = gender
        )
    }

    /**
     * 把今天的结果存进 SQLite。
     * 因为数据库以日期为主键、写冲突时替换，所以同一天多次测量只留最后一次。
     */
    private fun saveToHistory(profile: BmiProfile) {
        val heightCm = intent.getStringExtra("height")?.toDoubleOrNull() ?: return
        val weightKg = intent.getStringExtra("weight")?.toDoubleOrNull() ?: return
        historyRepository.saveRecord(
            BmiRecord(
                date = BmiDates.today(),   // 今天的日期字符串，作为主键
                bmi = profile.bmi,
                heightCm = heightCm,
                weightKg = weightKg,
                age = profile.age,
                gender = profile.gender
            )
        )
    }

    /**
     * 显示本地计算的结果：BMI 数值 + 一句本地建议 + 对应的状态图片。
     */
    private fun showLocalResult(profile: BmiProfile) {
        val bmi = profile.bmi
        // DecimalFormat("0.00")：保留两位小数，例如 24.809 → "24.81"。
        resultTV.text = getString(R.string.bmi_result) + " " + DecimalFormat("0.00").format(bmi)

        // limits.first 是“偏瘦上限”，limits.second 是“偏重下限”，中间就是“正常”。
        val limits = localLimits(profile)
        when {
            bmi < limits.first -> {                       // 低于下限 → 偏瘦
                adviceTV.setText(R.string.advice_light)
                resultImage.setImageResource(R.drawable.bot_thin)
            }
            bmi >= limits.second -> {                     // 高于等于上限 → 偏重
                adviceTV.setText(R.string.advice_heavy)
                resultImage.setImageResource(R.drawable.bot_fat)
            }
            else -> {                                     // 中间 → 正常
                adviceTV.setText(R.string.advice_average)
                resultImage.setImageResource(R.drawable.bot_fit)
            }
        }
    }

    /**
     * 根据 ViewModel 的 AI 状态，更新界面。
     * 状态一共有四种：Idle（还没请求）、Loading（加载中）、Success（成功）、Error（失败）。
     */
    private fun renderLlmState(state: AdviceUiState) {
        when (state) {
            AdviceUiState.Idle -> {
                llmProgress.isVisible = false
                llmAdviceTV.text = ""
                llmRetry.isVisible = false
            }
            AdviceUiState.Loading -> {
                llmProgress.isVisible = true      // 显示转圈
                llmAdviceTV.setText(R.string.llm_loading)
                llmRetry.isVisible = false
            }
            is AdviceUiState.Success -> {
                llmProgress.isVisible = false
                llmAdviceTV.text = state.text     // 显示 AI 返回的文字
                llmRetry.isVisible = false
            }
            is AdviceUiState.Error -> {
                llmProgress.isVisible = false
                // state.reason.messageRes 是错误类型对应的“本地化文案 id”，
                // getString 把它转成当前语言下的文字。
                llmAdviceTV.text = getString(state.reason.messageRes)
                llmRetry.isVisible = true         // 显示重试按钮
            }
        }
    }

    /**
     * 判断当前 App 语言（中文/英文）。
     * 用 ConfigurationCompat.getLocales(...) 读取系统当前语言区域设置。
     */
    private fun currentLanguage(): AppLanguage {
        val locale = ConfigurationCompat.getLocales(resources.configuration).get(0)
            ?: Locale.getDefault()
        return AppLanguage.fromLanguageTag(locale.language)
    }

    // ===================== BMI 判定标准 =====================

    /**
     * 返回“偏瘦上限 / 偏重下限”这一对阈值。
     * 儿童青少年（6~18 岁）按性别查百分位表；成人按通用的 18.5 / 22.9（亚洲标准）。
     */
    private fun localLimits(profile: BmiProfile): Pair<Double, Double> =
        if (profile.age in 6..18) {
            if (profile.gender == Gender.MALE) boyLimits(profile.age) else girlLimits(profile.age)
        } else {
            18.5 to 22.9
        }

    /*
     * ── 【考点 / 备用方案】如果老师只要求“成人版”BMI 判定 ──────────────
     * 把 localLimits 和下面 boyLimits/girlLimits 整段替换成这一行就行，
     * 这样只有一条规则，不需要年龄和性别分表：
     *
     *   private fun localLimits(profile: BmiProfile): Pair<Double, Double> =
     *       18.5 to 22.9
     *
     * 也可以换成国际通用标准：偏瘦 < 18.5，正常 18.5~24.9，偏重 >= 25：
     *       18.5 to 25.0
     * ────────────────────────────────────────────────────────────────────
     */

    /**
     * 男孩各年龄的 BMI 百分位阈值。
     * first  = 第 5 百分位（低于它算偏瘦）；
     * second = 第 90 百分位（达到或超过它算偏重）。
     * 数据来自《中国儿童青少年 BMI 分类标准》的百分位表。
     */
    private fun boyLimits(age: Int): Pair<Double, Double> = when (age) {
        6 -> 13.2 to 18.9
        7 -> 13.4 to 19.9
        8 -> 13.7 to 21.0
        9 -> 13.9 to 21.9
        10 -> 14.2 to 22.8
        11 -> 14.6 to 23.7
        12 -> 14.9 to 24.4
        13 -> 15.2 to 25.1
        14 -> 15.5 to 25.6
        15 -> 15.9 to 26.2
        16 -> 16.2 to 26.6
        17 -> 16.4 to 27.1
        else -> 16.7 to 27.5   // 18 岁及以上按成人末行处理（实际成人走上面的 18.5/22.9）
    }

    /** 女孩各年龄的 BMI 百分位阈值，含义同 boyLimits。 */
    private fun girlLimits(age: Int): Pair<Double, Double> = when (age) {
        6 -> 12.9 to 18.4
        7 -> 13.2 to 19.2
        8 -> 13.5 to 20.2
        9 -> 13.8 to 21.1
        10 -> 14.2 to 22.0
        11 -> 14.5 to 22.8
        12 -> 14.9 to 23.5
        13 -> 15.3 to 24.1
        14 -> 15.6 to 24.7
        15 -> 15.9 to 25.1
        16 -> 16.2 to 25.5
        17 -> 16.4 to 25.8
        else -> 16.6 to 26.0
    }
}
