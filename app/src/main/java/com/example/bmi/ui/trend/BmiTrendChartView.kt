package com.example.bmi.ui.trend

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.bmi.R
import com.example.bmi.data.BmiDates
import com.example.bmi.data.BmiRecord
import java.util.Locale

/*
 * ============================================================================
 * BmiTrendChartView.kt —— 自定义 View，用 Canvas 手绘 BMI 折线图
 * ============================================================================
 * 【这个文件是干什么的】
 *   不依赖任何图表库，直接用 Android 的 Canvas（画布）画一张折线图：
 *   横轴是日期、纵轴是 BMI 值，点与点之间连线，下面标日期、点上标数值。
 *
 * 【自定义 View 是什么（考试高频考点）】
 *   View 是 Android 里所有可见控件的老祖宗。自定义 View = 继承 View，
 *   重写 onDraw(canvas) 方法，在里面用 Paint（画笔）在 Canvas（画布）上画东西。
 *   Android 每次需要刷新这个控件时，就会调用 onDraw。
 *
 * 【坐标系】
 *   Canvas 的坐标系原点在控件左上角，x 向右增大，y 向下增大。
 *   也就是说“y 越小越靠上”，画图时要记得把“数值大”映射到“靠上的位置”。
 *
 * 【谁在用这个文件】
 *   TrendActivity 调用 setRecords(...) 传入数据，然后控件自己 invalidate() 重画。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】（考试删文件前先看这里）
 *   本文件依赖：BmiRecord、BmiDates，以及资源 R.color.*（取主题色）。
 *   谁依赖本文件：TrendActivity（以及 activity_trend.xml 里直接引用本类）。
 *   删除指引：BmiTrendChartView 只服务于“画折线图”，属于【趋势/历史】功能；
 *             砍掉趋势时连同本文件 + activity_trend.xml 一起删。
 * ────────────────────────────────────────────────────────────────────────────
 */
class BmiTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 当前要画的记录（按日期升序）。 */
    private var records: List<BmiRecord> = emptyList()

    /*
     * resources.displayMetrics 里保存了屏幕的像素密度。
     *   density：1dp 对应的像素倍数（用 dp 能保证不同手机看起来大小一致）；
     *   scaledDensity：1sp 对应的像素倍数（sp 用于字体，会随系统字体大小缩放）。
     */
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    /** 是否处于“深色模式”。 */
    private val isNight = (resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /*
     * 主题颜色。ContextCompat.getColor(context, id) 根据 id 取颜色。
     * 深色模式用亮一点的紫色（purple_200），浅色模式用深一点的紫色（purple_500）。
     */
    private val lineColor = ContextCompat.getColor(
        context,
        if (isNight) R.color.purple_200 else R.color.purple_500
    )
    private val pointFillColor = ContextCompat.getColor(context, R.color.white)
    private val valueColor = ContextCompat.getColor(
        context,
        if (isNight) R.color.purple_200 else R.color.purple_700
    )
    private val labelColor = if (isNight) Color.parseColor("#B0B0B0") else Color.parseColor("#757575")
    private val gridColor = if (isNight) Color.parseColor("#33FFFFFF") else Color.parseColor("#1F000000")
    private val labelBackgroundColor = if (isNight) Color.parseColor("#1E1E1E") else Color.WHITE

    /*
     * Paint = 画笔。每种画笔负责画一种东西：
     *   - linePaint：折线本体（描边）；
     *   - areaPaint：折线下方的半透明面积（填充）；
     *   - pointFillPaint / pointStrokePaint：数据点的填充和描边；
     *   - gridPaint：横向网格线；
     *   - yLabelPaint / xLabelPaint：纵轴、横轴文字；
     *   - valuePaint / valueBackgroundPaint：数值标签文字 + 它的背景。
     */
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        style = Paint.Style.STROKE          // 只画边框线，不填充
        strokeWidth = dp(2.5f)              // 线宽
        strokeCap = Paint.Cap.ROUND         // 线头圆角
        strokeJoin = Paint.Join.ROUND       // 折角圆滑
    }

    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        alpha = 28                           // 透明度（0~255），28 表示很淡
        style = Paint.Style.FILL             // 填充
    }

    private val pointFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pointFillColor
        style = Paint.Style.FILL
    }

    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor
        strokeWidth = dp(1f)
    }

    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelColor
        textSize = sp(10f)
        textAlign = Paint.Align.RIGHT        // 文字右对齐（贴着纵轴左侧）
    }

    private val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelColor
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER       // 文字居中（对着数据点）
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = valueColor
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true                // 伪加粗（没有对应粗体字时模拟加粗）
    }

    private val valueBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelBackgroundColor
        style = Paint.Style.FILL
    }

    /**
     * 替换要画的数据。调用后 invalidate() 触发一次重绘。
     * 参数 [records] 按日期排序，保证横轴从旧到新。
     */
    fun setRecords(records: List<BmiRecord>) {
        this.records = records.sortedBy { it.date }
        invalidate()   // 通知系统：我这个控件需要重画，系统会再次调用 onDraw
    }

    /**
     * 真正的绘图入口。系统每次需要显示这个控件时都会调用。
     * 所有“画”的动作都发生在 canvas 上。
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (records.isEmpty()) return   // 没数据就什么都不画

        // 留出四周边距：左边留给 Y 轴文字，上/下/右留空。
        val left = dp(44f)
        val top = dp(20f)
        val right = width - dp(12f)
        val bottom = height - dp(30f)
        if (right - left <= 0f || bottom - top <= 0f) return

        // ===== 计算 Y 轴范围（BMI 的取值区间）=====
        var minBmi = records.minOf { it.bmi }
        var maxBmi = records.maxOf { it.bmi }
        if (maxBmi - minBmi < 1.0) {
            // 如果所有点挤得太近（差值小于 1），人为把范围撑开 ±0.5，避免曲线太平。
            val middle = (maxBmi + minBmi) / 2
            minBmi = middle - 0.5
            maxBmi = middle + 0.5
        }
        val padding = (maxBmi - minBmi) * 0.18   // 上下各留 18% 的空白，让点不贴边
        val yMin = minBmi - padding
        val yMax = maxBmi + padding
        val yRange = yMax - yMin

        // ===== 画横向网格线 + Y 轴刻度文字 =====
        val gridLines = 4
        for (i in 0..gridLines) {
            // 把 [0, gridLines] 等分映射到 [bottom, top] 之间。
            val y = bottom - (bottom - top) * i / gridLines
            canvas.drawLine(left, y, right, y, gridPaint)   // 画一条横线

            // 这条线对应的 BMI 数值。
            val value = yMin + yRange * i / gridLines
            canvas.drawText(
                String.format(Locale.US, "%.1f", value),   // 保留 1 位小数
                left - dp(8f),                             // 画在纵轴左侧
                y + sp(3.5f),                              // 稍微下移让文字居中于线
                yLabelPaint
            )
        }

        // ===== 计算 X 轴位置（按真实日期距离）=====
        // 每个记录拿到它的日期毫秒时间戳，用它算“离最早那天的距离比例”。
        val times = records.map { BmiDates.parse(it.date)?.time ?: 0L }
        val minTime = times.minOrNull() ?: 0L
        val maxTime = times.maxOrNull() ?: 0L

        /**
         * 第 index 个数据点的 X 坐标。
         * 用真实日期差计算比例，所以“间隔很久的两次记录”在图上也会离得远。
         */
        fun xAt(index: Int): Float {
            if (records.size == 1) return (left + right) / 2f   // 只有 1 个点就放中间
            if (maxTime <= minTime) {
                // 日期都一样时退化为“均匀分布”。
                return left + (right - left) * index / (records.size - 1)
            }
            val fraction = (times[index] - minTime).toFloat() / (maxTime - minTime)
            return left + (right - left) * fraction
        }

        /**
         * 把某个 BMI 数值映射成 Y 坐标。
         * 注意：数值越大，y 应该越小（越靠上），所以用 bottom 去减。
         */
        fun yAt(value: Double): Float =
            bottom - (bottom - top) * ((value - yMin) / yRange).toFloat()

        // 提前算好所有点的 x、y，后面画线和标签都要用到。
        val xs = FloatArray(records.size) { xAt(it) }
        val ys = FloatArray(records.size) { yAt(records[it].bmi) }

        // ===== 画折线路径 =====
        val linePath = Path()
        records.indices.forEach { i ->
            if (i == 0) linePath.moveTo(xs[i], ys[i])   // 起点：移到第一个点
            else linePath.lineTo(xs[i], ys[i])          // 之后：连线到下一个点
        }

        // ===== 画折线下方的半透明面积 =====
        val areaPath = Path(linePath)                   // 复制折线路径
        areaPath.lineTo(xs[records.lastIndex], bottom)  // 连到底部右下角
        areaPath.lineTo(xs[0], bottom)                  // 再连到底部左下角
        areaPath.close()                                // 闭合，形成封闭多边形
        canvas.drawPath(areaPath, areaPaint)

        canvas.drawPath(linePath, linePaint)            // 画折线本身

        // ===== 画数据点（圆点）=====
        records.indices.forEach { i ->
            canvas.drawCircle(xs[i], ys[i], dp(4.5f), pointFillPaint)    // 白色实心圆
            canvas.drawCircle(xs[i], ys[i], dp(4.5f), pointStrokePaint)  // 外面描边
        }

        // ===== 画数值标签（点旁边的数字）=====
        // 点少时每个点都标；点多时只标最新那个，避免互相重叠。
        if (records.size <= 6) {
            records.indices.forEach { i ->
                drawValueLabel(canvas, i, xs, ys, left, right, bottom)
            }
        } else {
            val i = records.lastIndex
            drawValueLabel(canvas, i, xs, ys, left, right, bottom)
        }

        // ===== 画日期标签（X 轴文字）=====
        // 最多 5 个，避免挤成一团。
        val labelIndices = if (records.size <= 6) {
            records.indices.toList()
        } else {
            listOf(0, records.size / 4, records.size / 2, records.size * 3 / 4, records.lastIndex)
                .distinct()
        }
        labelIndices.forEach { i ->
            canvas.drawText(
                BmiDates.shortLabel(records[i].date),  // "2026-11-14" → "11-14"
                xs[i],                                  // 画在数据点正下方
                bottom + sp(16f),
                xLabelPaint
            )
        }
    }

    /**
     * 在某个数据点旁边画它的 BMI 数值标签。
     * 默认画在点的上方；如果这个点是“局部最低点”（两边都比它高），
     * 折线会从上方穿过它，此时改成画在下方，避免标签挡住折线。
     */
    private fun drawValueLabel(
        canvas: Canvas,
        index: Int,
        xs: FloatArray,
        ys: FloatArray,
        left: Float,
        right: Float,
        bottom: Float
    ) {
        val text = String.format(Locale.US, "%.1f", records[index].bmi)
        val padding = dp(3f)
        val textWidth = valuePaint.measureText(text)   // 量一下文字有多宽
        val boxWidth = textWidth + padding * 2

        // 让标签整体落在绘图区内部，不要压到坐标轴文字。
        val minCenter = left + boxWidth / 2f + dp(2f)
        val maxCenter = right - boxWidth / 2f - dp(2f)
        val centerX = if (minCenter > maxCenter) {
            (left + right) / 2f
        } else {
            xs[index].coerceIn(minCenter, maxCenter)
        }

        val y = ys[index]
        // 看前一个点和后一个点是不是都比它高（y 更小 = 更高）。
        val previousY = if (index > 0) ys[index - 1] else Float.NaN
        val nextY = if (index < ys.lastIndex) ys[index + 1] else Float.NaN
        val isLocalMinimum =
            !previousY.isNaN() && !nextY.isNaN() && previousY <= y && nextY <= y

        val belowBaseline = y + dp(9f) + valuePaint.textSize
        // 是局部最低点、且下方放得下 → 放下面；否则放上面。
        val drawBelow = isLocalMinimum && belowBaseline + padding <= bottom
        val baseline = if (drawBelow) belowBaseline else y - dp(8f)

        // 画一个圆角矩形当文字背景（避免文字和网格线/折线糊在一起）。
        canvas.drawRoundRect(
            centerX - boxWidth / 2f,
            baseline - valuePaint.textSize - padding,
            centerX + boxWidth / 2f,
            baseline + padding,
            dp(3f),
            dp(3f),
            valueBackgroundPaint
        )
        canvas.drawText(text, centerX, baseline, valuePaint)
    }

    /** dp 转像素（尺寸用，不随系统字体缩放）。 */
    private fun dp(value: Float): Float = value * density

    /** sp 转像素（字体用，会随系统字体缩放）。 */
    private fun sp(value: Float): Float = value * scaledDensity
}

/*
 * ── 【考点 / 备用方案】如果老师不要求折线图，只想“用列表显示趋势” ──────
 * 可以完全删掉这个自定义 View，换成最简单的做法：在 activity_trend.xml 里放一个
 * LinearLayout（id 例如 trend_list），然后在 TrendActivity 里用代码把每一天
 * 的 BMI 拼成一行文字加进去：
 *
 *   viewModel.records.observe(this) { records ->
 *       val container = findViewById<LinearLayout>(R.id.trend_list)
 *       container.removeAllViews()
 *       for (r in records) {
 *           val tv = TextView(this)
 *           tv.text = "${r.date}  BMI ${"%.1f".format(r.bmi)}"
 *           container.addView(tv)     // 每加一条，就多显示一行
 *       }
 *   }
 *
 * 这样“趋势”就退化成一张按天排列的文字列表，代码量最小，
 * 但也能满足“查看 BMI 变化”的需求。
 * ────────────────────────────────────────────────────────────────────────────
 */
