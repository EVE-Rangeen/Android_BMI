package com.example.bmi.data

import android.content.Context

/**
 * The only place the UI touches the BMI history storage.
 */
class BmiHistoryRepository(context: Context) {

    private val dbHelper = BmiDbHelper(context)

    fun saveRecord(record: BmiRecord) {
        dbHelper.save(record)
    }

    fun recentRecords(days: Int = DEFAULT_DAYS): List<BmiRecord> =
        dbHelper.recentRecords(days)

    companion object {
        /** The trend screen shows the last month of measurements. */
        const val DEFAULT_DAYS = 30
    }
}
