package com.example.bmi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bmi.data.BmiDates
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
 *   3. 删掉了 SharedPreferences 自动填充、选项菜单、上下文菜单、对话框
 *      —— 这些都不是本题必需，属于“可以不要的冗余功能”。
 *
 * 【本文件对应的考试知识点】
 *   · findViewById 找控件；
 *   · EditText 读取输入 + toIntOrNull() 校验；
 *   · Spinner 读取 selectedItemPosition；
 *   · SQLite 保存（通过 WellnessDbHelper）；
 *   · Intent 跳转到另一个 Activity；
 *   · Toast 提示。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】
 *   本文件依赖：WellnessDbHelper、WellnessRecord、BmiDates（日期工具）、
 *               HistoryActivity、activity_main.xml、strings.xml。
 *   谁依赖本文件：无（它是桌面图标启动的入口页）。
 * ────────────────────────────────────────────────────────────────────────────
 */
class MainActivity : AppCompatActivity() {

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

        // 创建数据库助手（此时不会真的建库，第一次读写时才创建 wellness.db）。
        db = WellnessDbHelper(this)

        dateTV = findViewById(R.id.dateTV)
        conditionSpinner = findViewById(R.id.conditionSpinner)
        stepsET = findViewById(R.id.stepsET)
        noteET = findViewById(R.id.noteET)
        val saveBtn: Button = findViewById(R.id.saveBtn)
        val historyBtn: Button = findViewById(R.id.historyBtn)

        // 把“今天”的日期显示在标题下面，让用户知道这条记录属于哪一天。
        // BmiDates 是作业里现成的日期工具，虽然名字带 Bmi，但内容只是通用的日期格式化。
        dateTV.text = getString(R.string.wellness_today, BmiDates.today())

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
     * 注意：日期不是让用户手填的，而是直接用 BmiDates.today() 取系统日期，
     * 这样既符合“每天记录一次”的需求，也避免用户输错日期格式。
     */
    private fun saveTodayRecord() {
        /*
         * toIntOrNull()：能转成整数就返回数字，转不了（空、字母、小数）返回 null。
         * 用 trim() 去掉首尾空格，避免 " 8000 " 这种输入被判为非法。
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
         */
        val record = WellnessRecord(
            date = BmiDates.today(),
            condition = conditionSpinner.selectedItemPosition,
            steps = steps,
            note = noteET.text.toString().trim()
        )

        // 同一天重复保存会覆盖旧记录（数据库里 date 是主键 + CONFLICT_REPLACE）。
        db.save(record)

        Toast.makeText(this, R.string.wellness_saved, Toast.LENGTH_SHORT).show()
    }
}
