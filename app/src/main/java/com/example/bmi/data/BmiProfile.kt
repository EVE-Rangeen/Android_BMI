package com.example.bmi.data

/** Gender of the user, as chosen in the spinner on the main screen. */
enum class Gender {
    MALE,
    FEMALE;

    companion object {
        /** Spinner position 0 = male, 1 = female. */
        fun fromSpinnerPosition(position: Int): Gender =
            if (position == 1) FEMALE else MALE
    }
}

/**
 * Everything the LLM needs to know about the user.
 *
 * This is a plain data class on purpose: it makes the request easy to test
 * and keeps [com.example.bmi.ui.report.ReportViewModel] independent from Android.
 */
data class BmiProfile(
    val bmi: Double,
    val age: Int,
    val gender: Gender
)
