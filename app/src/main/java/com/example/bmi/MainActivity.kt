package com.example.bmi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    lateinit var heightET: EditText
    lateinit var weightET: EditText
    lateinit var ageET: EditText
    lateinit var genderSpinner: Spinner
    lateinit var bmiImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        heightET = findViewById(R.id.heightET)
        weightET = findViewById(R.id.weightET)
        ageET = findViewById(R.id.ageET)
        genderSpinner = findViewById(R.id.genderSpinner)
        bmiImage = findViewById(R.id.imageView)
        val reportBtn: Button = findViewById<Button>(R.id.reportBtn)

        // Long press the BMI icon image to display the context menu
        registerForContextMenu(bmiImage)

        reportBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val height = heightET.text.toString()
                val weight = weightET.text.toString()
                val age = ageET.text.toString()
                // Pass the position in the list, not the translated text,
                // so the gender logic still works when the app is in Chinese.
                val gender = genderSpinner.selectedItemPosition

                if (height.isBlank() || weight.isBlank() || age.isBlank()) {
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
                    bundle.putInt("gender", gender)
                    bundle.putString("age", age)
                    intent.putExtras(bundle)
                    startActivity(intent)
                }

                savePreferences(height, weight)
            }
        })

        val aboutBtn: Button = findViewById(R.id.aboutBtn)
        aboutBtn.setOnClickListener {
            about_BMI_Dialog()
        }
    }

    override fun onStart() {
        super.onStart()
        loadPreferences()
    }

    // ---------- Options menu (action bar) ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar menu items
        when (item.itemId) {
            R.id.menu_about -> {
                about_BMI_Dialog()
                return true
            }
            R.id.menu_wiki -> {
                openWebpage(getString(R.string.wiki_url))
                return true
            }
            R.id.menu_exit -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // ---------- Context menu (long press on the BMI icon) ----------

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_about -> {
                about_BMI_Dialog()
                return true
            }
            R.id.menu_item_wiki -> {
                openWebpage(getString(R.string.wiki_url))
                return true
            }
            else -> return super.onContextItemSelected(item)
        }
    }

    private fun openWebpage(url: String) {
        val webpage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(
                findViewById<View>(android.R.id.content),
                R.string.no_browser,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    fun about_BMI_Dialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_button)
            .setMessage(R.string.about_msg)
            .setPositiveButton(R.string.button_ok) { dialog, which ->
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.button_ok),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
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
