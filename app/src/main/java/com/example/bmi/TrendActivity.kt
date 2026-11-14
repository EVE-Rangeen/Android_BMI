package com.example.bmi

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.bmi.ui.trend.BmiTrendChartView
import com.example.bmi.ui.trend.TrendViewModel

/*
 * ============================================================================
 * TrendActivity.kt —— 趋势页（显示最近 30 天 BMI 变化的折线图）
 * ============================================================================
 * 【这个文件是干什么的】
 *   从数据库读出最近 30 天的 BMI 记录，交给自定义控件 BmiTrendChartView 画成折线图。
 *   它自己不做任何计算，只负责“读状态 → 喂给图表 → 更新摘要文字”。
 *
 * 【这个页面没有 Intent 传值】
 *   因为它不需要 MainActivity 给它数据，而是自己从 SQLite 数据库里读历史。
 *   这正是“持久化存储”的意义：数据存在数据库里，任何页面都能随时读取。
 */
class TrendActivity : AppCompatActivity() {

    // by viewModels() 自动创建 TrendViewModel。
    private val viewModel: TrendViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trend)

        // 绑定控件：图表、摘要、空状态提示、图例说明。
        val chart: BmiTrendChartView = findViewById(R.id.trend_chart)
        val summary: TextView = findViewById(R.id.trend_summary)
        val empty: TextView = findViewById(R.id.trend_empty)
        val legend: TextView = findViewById(R.id.trend_legend)

        /*
         * 观察 ViewModel 里的记录列表。列表一变（比如第一次加载完成），就执行下面代码。
         */
        viewModel.records.observe(this) { records ->
            // 把数据交给自定义控件，它会自己 invalidate() 重画。
            chart.setRecords(records)

            val hasRecords = records.isNotEmpty()
            // isVisible = true/false 用来切换“显示/隐藏”控件。
            chart.isVisible = hasRecords        // 有数据 → 显示图表
            empty.isVisible = !hasRecords       // 没数据 → 显示空提示
            summary.isVisible = hasRecords
            legend.isVisible = hasRecords

            // records 是按日期升序排的，所以 lastOrNull() 是“最新一天”的记录。
            val latest = records.lastOrNull()
            if (latest == null) {
                // 没有记录时，给图表设置一个无障碍描述（读屏软件会读它）。
                chart.contentDescription = getString(R.string.trend_empty)
            } else {
                // 摘要文字：例如 “共 5 条记录 · 最新BMI 24.8”。
                summary.text = getString(R.string.trend_summary, records.size, latest.bmi)
                chart.contentDescription =
                    getString(R.string.trend_chart_description, records.size, latest.bmi)
            }
        }

        // 触发加载：从数据库读最近 30 天记录。
        viewModel.load()
    }
}
