package com.example.bmi

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.bmi.data.WellnessDbHelper
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.roundToInt

/*
 * ============================================================================
 * TrendActivity.kt —— 最近 7 天步数折线图（【图表 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   从 SQLite 读出最近 7 条记录，把「日期 → 步数」画成一条折线。
 *   题目只说“查看历史”时，HistoryActivity 的列表就够了，这个页面属于加分项。
 *
 * 【为什么用 MPAndroidChart 而不是自己画】
 *   自定义 View（main 分支的 BmiTrendChartView）要用 Canvas + Paint 手动画坐标轴、
 *   网格、刻度、文字，代码 200 行左右；MPAndroidChart 只要准备好 Entry 列表，
 *   坐标轴、网格、缩放、点击高亮都由库负责。
 *   代价是它只发布在 JitPack 仓库（见 settings.gradle），第一次 Sync 需要联网。
 *
 * 【MVVM 在这里为什么没用 ViewModel】
 *   图表是“一次性读库 + 画一次”，没有跨屏幕旋转要保住的状态，
 *   也不涉及网络请求，所以直接在 onCreate 里读库 + 设置数据即可。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：WellnessDbHelper、Dates 不直接用到、activity_trend.xml、
 *               strings.xml 里的 trend_* 文案、MPAndroidChart 依赖。
 *   谁依赖本文件：MainActivity（菜单“趋势图”跳转）、AndroidManifest 注册。
 *   ★ 不需要图表时，按 docs/exam-guide.md 的 C 部分删除：
 *     本文件、activity_trend.xml、菜单项、Manifest 注册、build.gradle 依赖、
 *     settings.gradle 的 jitpack 仓库。
 * ────────────────────────────────────────────────────────────────────────────
 */
class TrendActivity : BaseActivity() {

    /** 折线展示最近多少天。改这里就能变成 14 天 / 30 天。 */
    private val days = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trend)

        // 【语言切换 · 考试可删】标题跟着 App 内语言走（原因见 MainActivity 的说明）。
        title = getString(R.string.trend_title)

        val chart: LineChart = findViewById(R.id.trendChart)
        val summary: TextView = findViewById(R.id.trendSummary)
        val empty: TextView = findViewById(R.id.trendEmpty)

        /*
         * allRecords() 是按日期倒序（最新在前）返回的。
         * 折线图的横轴要从“早”到“晚”，所以 take(最近 N 条) 之后要 reversed()。
         */
        val records = WellnessDbHelper(this).allRecords().take(days).reversed()

        if (records.isEmpty()) {
            // 一条记录都没有：隐藏图表和统计，只留提示文字。
            chart.isVisible = false
            summary.isVisible = false
            empty.isVisible = true
            return
        }

        chart.isVisible = true
        summary.isVisible = true
        empty.isVisible = false

        // 统计行：最近 N 天平均步数。average() 返回 Double，四舍五入成整数显示。
        summary.text = getString(
            R.string.trend_summary,
            records.size,
            records.map { it.steps }.average().roundToInt()
        )

        drawChart(chart, records.map { it.date }, records.map { it.steps })
    }

    /**
     * 把数据画进 LineChart。
     *
     * MPAndroidChart 的数据模型分两层：
     *   Entry(x, y)   —— 一个点，x 是“第几个”，y 是数值（这里是步数）；
     *   LineDataSet   —— 一组点 + 这条线的样式（颜色、线宽、圆点、填充）；
     *   LineData      —— 整个图表的数据，可以放多条线（这里只有一条）。
     */
    private fun drawChart(chart: LineChart, dates: List<String>, steps: List<Int>) {
        // x 用下标 0、1、2…，y 用步数。
        val entries = steps.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val lineColor = Color.parseColor("#2E7D32")   // 绿色，和 wellness 主题一致

        val dataSet = LineDataSet(entries, getString(R.string.trend_series_steps)).apply {
            color = lineColor                 // 线的颜色
            lineWidth = 2.5f                  // 线宽（单位 dp）
            setCircleColor(lineColor)         // 数据点圆点的颜色
            circleRadius = 4f                 // 圆点半径
            setDrawCircleHole(false)          // 实心圆点
            setDrawValues(false)              // 不在点旁边标数值，避免 7 个数字挤在一起
            setDrawFilled(true)               // 线下方面积填充
            fillColor = lineColor
            fillAlpha = 40                    // 填充透明度（0-255）
            mode = LineDataSet.Mode.LINEAR    // 直线连接；想要平滑曲线改成 CUBIC_BEZIER
        }

        // ── 横轴：显示日期（把 2026-11-26 截成 11-26，7 个标签才放得下）──
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f                            // 刻度间隔为 1，一天一个点
            setDrawGridLines(false)                      // 不画竖向网格线，画面更干净
            labelCount = dates.size                      // 让所有点都显示刻度，不被自动跳过
            labelRotationAngle = -30f                    // 倾斜，避免日期互相压住
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in dates.indices) dates[index].takeLast(5) else ""
                }
            }
        }

        // ── 纵轴：步数从 0 开始，右侧轴和描述文字都不需要 ──
        chart.axisRight.isEnabled = false
        chart.axisLeft.axisMinimum = 0f
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        // 关掉双指缩放：这个页面套在 ScrollView 里，缩放会和页面滚动抢手势。
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)

        // 没数据时库自己显示这行字（本项目在 records 为空时直接隐藏图表，属于双保险）。
        chart.setNoDataText(getString(R.string.trend_empty))

        chart.data = LineData(dataSet)
        chart.invalidate()      // 数据变了必须调用，否则界面不刷新
        chart.animateX(500)     // 横向展开动画，0.5 秒
    }
}
