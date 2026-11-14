package com.example.bmi.data

import android.content.Context

/**
 * Remembers the last values entered on the main screen so the user does not
 * have to type height / weight / age / gender again next time.
 */
class BmiPreferences(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun save(height: String, weight: String, age: String, gender: Int) {
        preferences.edit()
            .putString(KEY_HEIGHT, height)
            .putString(KEY_WEIGHT, weight)
            .putString(KEY_AGE, age)
            .putInt(KEY_GENDER, gender)
            .apply()
    }

    fun height(): String = preferences.getString(KEY_HEIGHT, "") ?: ""

    fun weight(): String = preferences.getString(KEY_WEIGHT, "") ?: ""

    fun age(): String = preferences.getString(KEY_AGE, "") ?: ""

    fun gender(): Int = preferences.getInt(KEY_GENDER, 0)

    private companion object {
        const val NAME = "BMI"
        const val KEY_HEIGHT = "height"
        const val KEY_WEIGHT = "weight"
        const val KEY_AGE = "age"
        const val KEY_GENDER = "gender"
    }
}
