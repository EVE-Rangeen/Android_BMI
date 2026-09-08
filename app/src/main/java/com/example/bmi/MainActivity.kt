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
    }

    val heightET = findViewById<EditText>(R.id.height_input)
    val weightET = findViewById<EditText>(R.id.weight_input)

    val reportBtn : Buttom = findViewById<Button>(R.id.reportBtn)

}