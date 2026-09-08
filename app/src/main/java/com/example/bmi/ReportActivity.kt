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
        val height = java.lang.Double.parseDouble(bundle!!.getString("height")!!) / 100
        val weight = java.lang.Double.parseDouble(bundle.getString("weight")!!)
        val bmi = weight / (height * height)
        val nf = DecimalFormat("0.00")

        resultTV.text = "Your BMI value is " + nf.format(bmi)

        if (bmi > 25) {
            resultImage.setImageResource(R.drawable.bot_fat)
            adviceTV.setText("You should be on diet!")
        } else if (bmi < 20) {
            resultImage.setImageResource(R.drawable.bot_thin)
            adviceTV.setText("You need more calories!")
        } else {
            resultImage.setImageResource(R.drawable.bot_fit)
            adviceTV.setText("You look great!")
        }
    }
}
