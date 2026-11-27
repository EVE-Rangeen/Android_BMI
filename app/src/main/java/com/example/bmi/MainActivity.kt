package com.example.bmi

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.bmi.data.Dates
import com.example.bmi.data.WellnessDbHelper
import com.example.bmi.data.WellnessRecord

/*
 * ============================================================================
 * MainActivity.kt —— 健康日志主界面（记录今天）
 * ============================================================================
 * 【这个文件是干什么的】
 *   用户在这里选择今天的身体状况、填步数和备注，点“保存”写进 SQLite；
 *   点“查看历史”跳到 HistoryActivity 看所有记录。
 *
 * 【从作业的 BMI 版改过来时，这个文件改了哪些地方】
 *   1. 输入项：身高/体重/年龄/性别 → 身体状况/步数/备注；
 *   2. 按钮行为：计算 BMI 并跳转报告页 → 直接写数据库（不再需要第二个页面算结果）；
 *   3. 删掉了 SharedPreferences 自动填充、上下文菜单、对话框
 *      —— 这些都不是本题必需，属于“可以不要的冗余功能”。
 *
 * 【后来又加回来的两个“上课内容”（都是可选的，考试按需删）】
 *   A. 中英文切换：继承 BaseActivity + 右上角菜单的“切换语言”，
 *      点击后 toggleLanguage() 保存选择并 recreate() 重建界面。
 *   B. AI 健康建议：菜单里的“AI 健康建议”跳转到 AdviceActivity。
 *   这两块的删除清单见 docs/exam-guide.md。
 *
 * 【本文件对应的考试知识点】
 *   · findViewById 找控件；
 *   · EditText 读取输入 + toIntOrNull() 校验；
 *   · Spinner 读取 selectedItemPosition；
 *   · SQLite 保存（通过 WellnessDbHelper）；
 *   · Intent 跳转到另一个 Activity；
 *   · Toast 提示；
 *   · 选项菜单（onCreateOptionsMenu / onOptionsItemSelected）。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】
 *   本文件依赖：WellnessDbHelper、WellnessRecord、Dates（日期工具）、
 *               HistoryActivity、AdviceActivity（可选）、
 *               BaseActivity（可选）、activity_main.xml、strings.xml、menu_main.xml。
 *   谁依赖本文件：无（它是桌面图标启动的入口页）。
 * ────────────────────────────────────────────────────────────────────────────
 */
class MainActivity : BaseActivity() {

    // 数据库助手：Activity 创建时初始化，后面保存记录时直接用。
    private lateinit var db: WellnessDbHelper

    // 界面控件。
    private lateinit var dateTV: TextView        // 显示“今天日期”
    private lateinit var conditionSpinner: Spinner
    private lateinit var stepsET: EditText
    private lateinit var noteET: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
         * 【语言切换 · 考试可删】手动设置标题栏文字。
         *
         * 标题栏默认显示 Manifest 里 android:label 指向的文字，而那是“系统”
         * 按手机的设置解析的；我们在 App 内切换语言后，系统并不知道，
         * 标题就会停在原来的语言。所以这里用 getString() 从“当前页面的资源”取，
         * 跟着 App 内语言走。不做语言切换时可以删掉这一行。
         */
        title = getString(R.string.app_name)

        // 创建数据库助手（此时不会真的建库，第一次读写时才创建 wellness.db）。
        db = WellnessDbHelper(this)

        dateTV = findViewById(R.id.dateTV)
        conditionSpinner = findViewById(R.id.conditionSpinner)
        stepsET = findViewById(R.id.stepsET)
        noteET = findViewById(R.id.noteET)
        val saveBtn: Button = findViewById(R.id.saveBtn)
        val historyBtn: Button = findViewById(R.id.historyBtn)

        // 把“今天”的日期显示在标题下面，让用户知道这条记录属于哪一天。
        dateTV.text = getString(R.string.wellness_today, Dates.today())

        // 保存按钮：校验 → 写数据库 → 提示。
        saveBtn.setOnClickListener {
            saveTodayRecord()
        }

        // 历史按钮：打开历史页（不需要传数据，历史页自己从数据库读）。
        historyBtn.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    /**
     * 读取界面输入，校验后保存“今天”的记录。
     *
     * 注意：日期不是让用户手填的，而是直接用 Dates.today() 取系统日期，
     * 这样既符合“每天记录一次”的需求，也避免用户输错日期格式。
     */
    private fun saveTodayRecord() {
        /*
         * toIntOrNull()：能转成整数就返回数字，转不了（空、字母、小数）返回 null。
         * 用 trim() 去掉首尾空格，避免 " 8000 " 这种输入被判为非法。
         *
         * >>> 【改题点 C5】题目换了数值含义时，改这两行：
         *    · 整数 → 小数：toIntOrNull() 换成 toDoubleOrNull()；
         *    · 取值范围：把下面 if 里的判据换成题目的上限
         *      （喝水 0..20000、学习 0..1440、金额 > 0.0 且 < 100000）。
         *    · 变量名 steps 可以顺手改成 waterMl 之类，IDE 会让你一起改。
         * <<<
         */
        val steps = stepsET.text.toString().trim().toIntOrNull()

        // 步数必须是非负整数。非法时只提示，不写数据库。
        if (steps == null || steps < 0) {
            Toast.makeText(this, R.string.wellness_steps_warning, Toast.LENGTH_SHORT).show()
            return
        }

        /*
         * Spinner 的 selectedItemPosition 就是选中的序号（0/1/2），
         * 刚好对应 strings.xml 里 wellness_condition 数组的三项。
         *
         * >>> 【改题点 C6】这里把 4 个字段装进一条记录。
         *     改了 WellnessRecord 的字段（C1）之后，这一行会立刻编译报错，
         *     按提示补上新字段即可。如果题目要求“一天多条”，
         *     日期不再是主键，见 docs/wellness-tracker.md 7.1。
         * <<<
         */
        val record = WellnessRecord(
            date = Dates.today(),
            condition = conditionSpinner.selectedItemPosition,
            steps = steps,
            note = noteET.text.toString().trim()
        )

        // 同一天重复保存会覆盖旧记录（数据库里 date 是主键 + CONFLICT_REPLACE）。
        db.save(record)

        Toast.makeText(this, R.string.wellness_saved, Toast.LENGTH_SHORT).show()
    }

    // ===================== 选项菜单（右上角三个点）=====================

    /**
     * 把 res/menu/menu_main.xml 加载成右上角菜单。
     *
     * 【考试可删】不需要中英文切换、也不需要 AI 时，删掉本方法和
     * onOptionsItemSelected()，再删掉 res/menu/menu_main.xml。
     *
     * 【改题点 C11】如果题目要求“菜单里增加一个页面”（比如统计页），
     * 就在这里加一个 when 分支 + 在 menu_main.xml 加一个 item，
     * 再在 AndroidManifest.xml 注册新 Activity，三处缺一不可。
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * 菜单项点击处理：靠 itemId 区分点的是哪一项。
     *
     * 注意返回 true 表示“这个事件我处理了”，返回 false 会继续交给父类处理。
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            // ── 【语言切换 · 考试可删】中文 ↔ 英文，切换后整页重建 ──
            R.id.menu_language -> {
                // toggleLanguage() 在 BaseActivity 里：保存选择 + recreate()
                toggleLanguage()
                true
            }

            // ── 【AI 功能 · 考试可删】打开 AI 健康建议页 ──
            R.id.menu_ai_advice -> {
                // 不传数据：建议页自己按 Dates.today() 去数据库查今天的记录。
                startActivity(Intent(this, AdviceActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
