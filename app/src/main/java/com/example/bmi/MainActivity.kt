package com.example.bmi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    lateinit var heightET: EditText
    lateinit var weightET: EditText
    lateinit var ageET: EditText
    lateinit var genderSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        heightET = findViewById(R.id.heightET)
        weightET = findViewById(R.id.weightET)
        ageET = findViewById(R.id.ageET)
        genderSpinner = findViewById(R.id.genderSpinner)
        val reportBtn: Button = findViewById<Button>(R.id.reportBtn)

        reportBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val height = heightET.text.toString()
                val weight = weightET.text.toString()
                val age = ageET.text.toString()
                val gender = genderSpinner.selectedItem.toString()

                if (height == "" || weight == "" || age == "") {
                    Snackbar.make(
                        findViewById<View>(android.R.id.content),
                        R.string.bmi_warning,
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    val intent = Intent(applicationContext, ReportActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("height", height)
                    bundle.putString("weight", weight)
                    bundle.putString("gender", gender)
                    bundle.putString("age", age)
                    intent.putExtras(bundle)
                    startActivity(intent)
                }

                savePreferences(height, weight)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        loadPreferences()
    }

    fun savePreferences(h: String, w: String) {
        val pref = getSharedPreferences("BMI", Context.MODE_PRIVATE)
        pref.edit().putString("height", h).apply()
        pref.edit().putString("weight", w).apply()
    }

    fun loadPreferences() {
        val pref = getSharedPreferences("BMI", Context.MODE_PRIVATE)
        heightET.setText(pref.getString("height", ""))
        weightET.setText(pref.getString("weight", ""))
    }
}
