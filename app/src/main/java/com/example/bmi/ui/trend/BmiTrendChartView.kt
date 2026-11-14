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

/**
 * A small line chart drawn with Canvas, so the app does not need an extra
 * charting dependency. It shows the BMI value of each day and uses the real
 * date distance on the X axis, which makes gaps in the history visible.
 */
class BmiTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var records: List<BmiRecord> = emptyList()

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val isNight = (resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

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

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        alpha = 28
        style = Paint.Style.FILL
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
        textAlign = Paint.Align.RIGHT
    }

    private val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelColor
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = valueColor
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val valueBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelBackgroundColor
        style = Paint.Style.FILL
    }

    /** Replaces the plotted data. [records] are expected oldest first. */
    fun setRecords(records: List<BmiRecord>) {
        this.records = records.sortedBy { it.date }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (records.isEmpty()) return

        val left = dp(44f)
        val top = dp(20f)
        val right = width - dp(12f)
        val bottom = height - dp(30f)
        if (right - left <= 0f || bottom - top <= 0f) return

        var minBmi = records.minOf { it.bmi }
        var maxBmi = records.maxOf { it.bmi }
        if (maxBmi - minBmi < 1.0) {
            val middle = (maxBmi + minBmi) / 2
            minBmi = middle - 0.5
            maxBmi = middle + 0.5
        }
        val padding = (maxBmi - minBmi) * 0.18
        val yMin = minBmi - padding
        val yMax = maxBmi + padding
        val yRange = yMax - yMin

        // Horizontal grid + Y axis labels.
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = bottom - (bottom - top) * i / gridLines
            canvas.drawLine(left, y, right, y, gridPaint)
            val value = yMin + yRange * i / gridLines
            canvas.drawText(
                String.format(Locale.US, "%.1f", value),
                left - dp(8f),
                y + sp(3.5f),
                yLabelPaint
            )
        }

        // X positions follow the real dates so a long gap looks long.
        val times = records.map { BmiDates.parse(it.date)?.time ?: 0L }
        val minTime = times.minOrNull() ?: 0L
        val maxTime = times.maxOrNull() ?: 0L

        fun xAt(index: Int): Float {
            if (records.size == 1) return (left + right) / 2f
            if (maxTime <= minTime) {
                return left + (right - left) * index / (records.size - 1)
            }
            val fraction = (times[index] - minTime).toFloat() / (maxTime - minTime)
            return left + (right - left) * fraction
        }

        fun yAt(value: Double): Float =
            bottom - (bottom - top) * ((value - yMin) / yRange).toFloat()

        val xs = FloatArray(records.size) { xAt(it) }
        val ys = FloatArray(records.size) { yAt(records[it].bmi) }

        val linePath = Path()
        records.indices.forEach { i ->
            if (i == 0) linePath.moveTo(xs[i], ys[i]) else linePath.lineTo(xs[i], ys[i])
        }

        // Soft area below the line.
        val areaPath = Path(linePath)
        areaPath.lineTo(xs[records.lastIndex], bottom)
        areaPath.lineTo(xs[0], bottom)
        areaPath.close()
        canvas.drawPath(areaPath, areaPaint)

        canvas.drawPath(linePath, linePaint)

        // Dots on every measurement.
        records.indices.forEach { i ->
            canvas.drawCircle(xs[i], ys[i], dp(4.5f), pointFillPaint)
            canvas.drawCircle(xs[i], ys[i], dp(4.5f), pointStrokePaint)
        }

        // Value labels: all when there are few points, otherwise only the newest.
        if (records.size <= 6) {
            records.indices.forEach { i ->
                drawValueLabel(canvas, i, xs, ys, left, right, bottom)
            }
        } else {
            val i = records.lastIndex
            drawValueLabel(canvas, i, xs, ys, left, right, bottom)
        }

        // Date labels: at most five so they never overlap.
        val labelIndices = if (records.size <= 6) {
            records.indices.toList()
        } else {
            listOf(0, records.size / 4, records.size / 2, records.size * 3 / 4, records.lastIndex)
                .distinct()
        }
        labelIndices.forEach { i ->
            canvas.drawText(
                BmiDates.shortLabel(records[i].date),
                xs[i],
                bottom + sp(16f),
                xLabelPaint
            )
        }
    }

    /**
     * Draws the BMI value of one point. The label sits above the point, except
     * when the point is a local minimum: there both line segments are above it,
     * so the label would cut through the line and is placed below instead.
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
        val textWidth = valuePaint.measureText(text)
        val boxWidth = textWidth + padding * 2

        // Keep the label inside the plot so it never covers the axis labels.
        val minCenter = left + boxWidth / 2f + dp(2f)
        val maxCenter = right - boxWidth / 2f - dp(2f)
        val centerX = if (minCenter > maxCenter) {
            (left + right) / 2f
        } else {
            xs[index].coerceIn(minCenter, maxCenter)
        }

        val y = ys[index]
        val previousY = if (index > 0) ys[index - 1] else Float.NaN
        val nextY = if (index < ys.lastIndex) ys[index + 1] else Float.NaN
        val isLocalMinimum =
            !previousY.isNaN() && !nextY.isNaN() && previousY <= y && nextY <= y

        val belowBaseline = y + dp(9f) + valuePaint.textSize
        val drawBelow = isLocalMinimum && belowBaseline + padding <= bottom
        val baseline = if (drawBelow) belowBaseline else y - dp(8f)

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

    private fun dp(value: Float): Float = value * density

    private fun sp(value: Float): Float = value * scaledDensity
}
