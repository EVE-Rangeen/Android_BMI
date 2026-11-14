package com.example.bmi

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.bmi.ui.trend.BmiTrendChartView
import com.example.bmi.ui.trend.TrendViewModel

/**
 * Shows the BMI trend of the last 30 days as a line chart.
 */
class TrendActivity : AppCompatActivity() {

    private val viewModel: TrendViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trend)

        val chart: BmiTrendChartView = findViewById(R.id.trend_chart)
        val summary: TextView = findViewById(R.id.trend_summary)
        val empty: TextView = findViewById(R.id.trend_empty)
        val legend: TextView = findViewById(R.id.trend_legend)

        viewModel.records.observe(this) { records ->
            chart.setRecords(records)

            val hasRecords = records.isNotEmpty()
            chart.isVisible = hasRecords
            empty.isVisible = !hasRecords
            summary.isVisible = hasRecords
            legend.isVisible = hasRecords

            val latest = records.lastOrNull()
            if (latest == null) {
                chart.contentDescription = getString(R.string.trend_empty)
            } else {
                summary.text = getString(R.string.trend_summary, records.size, latest.bmi)
                chart.contentDescription =
                    getString(R.string.trend_chart_description, records.size, latest.bmi)
            }
        }

        viewModel.load()
    }
}
