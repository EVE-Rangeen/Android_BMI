package com.example.bmi

import android.os.Bundle
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.bmi.data.WellnessDbHelper
import com.example.bmi.data.WellnessRecord

/*
 * ============================================================================
 * HistoryActivity.kt —— 历史记录页
 * ============================================================================
 * 【这个文件是干什么的】
 *   打开时从 SQLite 读出所有记录，按日期从新到旧显示在 ListView 里；
 *   顺便显示“一共记录了多少天、平均每天多少步”。
 *   一条记录都没有时，显示一句空状态提示。
 *
 * 【为什么不需要 Intent 传数据】
 *   数据已经存在数据库里了，任何页面都能自己查。这正是“持久化”的好处，
 *   也是作业里趋势页 TrendActivity 的做法。
 *
 * 【ListView + Adapter 是最基础的列表写法】
 *   ListView 负责滚动显示，Adapter 负责把数据变成一行行界面。
 *   这里用系统自带的 SimpleAdapter：它按 key 把 Map 里的值填到行布局的控件里，
 *   不用自己写 BaseAdapter 的 getView()，代码最少、最不容易出错。
 *   如果考试只要求一行文字，换成 ArrayAdapter + simple_list_item_1 会更短。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】
 *   本文件依赖：WellnessDbHelper、WellnessRecord、activity_history.xml、strings.xml。
 *   谁依赖本文件：MainActivity（点“查看历史”跳转）。
 *
 * 【为什么继承 BaseActivity 而不是 AppCompatActivity】
 *   为了让本页也跟着用户选的语言显示（中文/英文）。
 *   BaseActivity 是 AppCompatActivity 的子类，用法完全一样。
 *   ★ 考试不要求中英文切换时，把这里改回 `class HistoryActivity : AppCompatActivity()`
 *     并重新 import androidx.appcompat.app.AppCompatActivity（见 docs/exam-guide.md）。
 * ────────────────────────────────────────────────────────────────────────────
 */
class HistoryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 【语言切换 · 考试可删】标题跟着 App 内语言走（原因见 MainActivity 的说明）。
        title = getString(R.string.wellness_history_title)

        val list: ListView = findViewById(R.id.historyList)
        val summary: TextView = findViewById(R.id.historySummary)
        val empty: TextView = findViewById(R.id.historyEmpty)

        // 每次打开页面都重新查一遍，保证刚保存的记录能立刻看到。
        val records = WellnessDbHelper(this).allRecords()

        if (records.isEmpty()) {
            // 没有记录：隐藏列表和统计，只显示提示文字。
            list.isVisible = false
            summary.isVisible = false
            empty.isVisible = true
            return
        }

        empty.isVisible = false
        list.isVisible = true
        summary.isVisible = true

        // 平均步数：先求和再除以天数。用 Double 计算后 roundToInt()，避免整数除法丢小数。
        // 【改题点 C7】统计口径换了（比如改成“本月总饮水量”）就改这两行；
        // 换了数值类型（Int → Double）时，sumOf 与 toDouble() 的写法也要跟着调。
        val averageSteps = records.sumOf { it.steps }.toDouble() / records.size
        summary.text = getString(
            R.string.wellness_history_summary,
            records.size,
            Math.round(averageSteps).toInt()
        )

        /*
         * 每一行显示成两行文字：
         *   第一行：日期 · 身体状况
         *   第二行：步数 · 备注
         * 这里用 simple_list_item_1 一个 TextView 装不下两行样式，
         * 所以用自定义的 item_wellness.xml + SimpleAdapter。
         *
         * 【改题点 C7/C8】换题目时，改的是 map 里的两个 key 对应的内容：
         *   "title"    → 想放主信息（日期 + 分类）
         *   "subtitle" → 想放次要信息（数值 + 单位 + 备注）
         * 想改成三行？在 item_wellness.xml 加一个 TextView，
         * 这里加一个 key，并把下面 SimpleAdapter 的数组各加一项。
         */
        val rows = records.map { record ->
            mapOf(
                "title" to getString(
                    R.string.wellness_history_title_row,
                    record.date,
                    conditionText(record.condition)
                ),
                "subtitle" to buildSubtitle(record)
            )
        }

        // SimpleAdapter 的五个参数：Context、数据、行布局、数据 key 数组、布局里对应的控件 id 数组
        list.adapter = SimpleAdapter(
            this,
            rows,
            R.layout.item_wellness,
            arrayOf("title", "subtitle"),
            intArrayOf(R.id.itemTitle, R.id.itemSubtitle)
        )
    }

    /**
     * 把数据库里的 0/1/2 翻译成当前语言下的文字。
     *
     * 【改题点 C7】如果题目不是“身体状况”，这里改成题目的选项映射；
     * 选项数量变了（比如心情 1–5），记得同时改 strings.xml 的数组（C3）
     * 和布局里 Spinner 的 entries（C4）。
     */
    private fun conditionText(condition: Int): String = getString(
        when (condition) {
            0 -> R.string.wellness_condition_good
            1 -> R.string.wellness_condition_fair
            else -> R.string.wellness_condition_poor
        }
    )

    /** 第二行：步数 + （有备注时）备注。 */
    private fun buildSubtitle(record: WellnessRecord): String {
        // 【改题点 C7】单位/格式在这里：想显示 “1200 ml”“¥12.30”“60 分钟”，
        // 改 wellness_history_steps 的文案（C3），或在这里用 String.format 补小数位。
        val steps = getString(R.string.wellness_history_steps, record.steps)
        return if (record.note.isBlank()) steps else "$steps  ·  ${record.note}"
    }
}
