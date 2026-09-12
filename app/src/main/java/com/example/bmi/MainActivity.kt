package com.example.bmi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class MainActivity : AppCompatActivity() {

    lateinit var heightET: EditText
    lateinit var weightET: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        heightET = findViewById<EditText>(R.id.height_input)
        weightET = findViewById<EditText>(R.id.weight_input)

        val reportBtn : Button = findViewById<Button>(R.id.reportBtn)

        reportBtn.setOnClickListener {
            val height = heightET.text.toString()
            val weight = weightET.text.toString()

            val intent = Intent(applicationContext, ReportActivity::class.java)
            val bundle = Bundle()
            bundle.putString("height", height)
            bundle.putString("weight", weight)
            intent.putExtras(bundle)
            startActivity(intent)

    }


}