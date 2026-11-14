package com.example.bmi.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One BMI result stored for one calendar day.
 *
 * The date is the primary key of the SQLite table, so measuring twice on the
 * same day replaces the earlier value and only the latest result is kept.
 */
data class BmiRecord(
    val date: String,
    val bmi: Double,
    val heightCm: Double,
    val weightKg: Double,
    val age: Int,
    val gender: Gender
)

/** Date helpers shared by the database and the chart. */
object BmiDates {

    const val PATTERN = "yyyy-MM-dd"

    fun today(): String =
        SimpleDateFormat(PATTERN, Locale.US).format(Date())

    fun parse(date: String): Date? =
        SimpleDateFormat(PATTERN, Locale.US).parse(date)

    fun shortLabel(date: String): String {
        val parsed = parse(date) ?: return date
        return SimpleDateFormat("MM-dd", Locale.US).format(parsed)
    }
}
