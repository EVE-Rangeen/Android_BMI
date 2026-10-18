package com.example.bmi

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class ReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val resultTV: TextView = findViewById<TextView>(R.id.report_result)
        val adviceTV: TextView = findViewById<TextView>(R.id.report_advice)
        val resultImage: ImageView = findViewById<ImageView>(R.id.report_image)

        val bundle = intent.extras
        val height = bundle!!.getString("height")!!.toDouble() / 100
        val weight = bundle.getString("weight")!!.toDouble()
        // 0 = male, 1 = female. The index is language independent, so the
        // child cut points stay correct in every locale.
        val gender = bundle.getInt("gender", 0)
        val age = bundle.getString("age")!!.toInt()
        val bmi = weight / (height * height)
        val nf = DecimalFormat("0.00")

        resultTV.text = getString(R.string.bmi_result) + " " + nf.format(bmi)

        // Two cut points are enough to pick the advice:
        //   bmi <  thinLimit -> underweight
        //   bmi >= fatLimit  -> overweight
        //   in between       -> acceptable
        val thinLimit: Double
        val fatLimit: Double
        if (age in 6..18) {
            // Child: the cut points come from the BMI-for-age percentile table,
            // where "acceptable weight" starts at the 5th percentile and
            // "overweight" starts at the 90th percentile.
            val limits = if (gender == 0) boyLimits(age) else girlLimits(age)
            thinLimit = limits.first
            fatLimit = limits.second
        } else {
            // Adult
            thinLimit = 18.5
            fatLimit = 22.9
        }

        if (bmi < thinLimit) {
            adviceTV.setText(R.string.advice_light)
            resultImage.setImageResource(R.drawable.bot_thin)
        } else if (bmi >= fatLimit) {
            adviceTV.setText(R.string.advice_heavy)
            resultImage.setImageResource(R.drawable.bot_fat)
        } else {
            adviceTV.setText(R.string.advice_average)
            resultImage.setImageResource(R.drawable.bot_fit)
        }
    }

    // BMI-for-age cut points for boys, taken from the percentile table.
    // first  = 5th percentile  (below it: underweight)
    // second = 90th percentile (at or above it: overweight)
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
        else -> 16.7 to 27.5
    }

    // Same table for girls.
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
