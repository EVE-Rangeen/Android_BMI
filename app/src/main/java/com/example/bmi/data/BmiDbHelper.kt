package com.example.bmi.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * SQLite storage for the BMI trend.
 *
 * Each row is one day: the date is the primary key, and inserts use
 * CONFLICT_REPLACE, so a second measurement on the same day overwrites the
 * first one and only the latest result is kept.
 */
class BmiDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_RECORDS (
                $COL_DATE TEXT PRIMARY KEY,
                $COL_BMI REAL NOT NULL,
                $COL_HEIGHT REAL NOT NULL,
                $COL_WEIGHT REAL NOT NULL,
                $COL_AGE INTEGER NOT NULL,
                $COL_GENDER TEXT NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        onCreate(db)
    }

    /** Inserts the record, replacing the row of the same date if it exists. */
    fun save(record: BmiRecord) {
        val values = ContentValues().apply {
            put(COL_DATE, record.date)
            put(COL_BMI, record.bmi)
            put(COL_HEIGHT, record.heightCm)
            put(COL_WEIGHT, record.weightKg)
            put(COL_AGE, record.age)
            put(COL_GENDER, record.gender.name)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_RECORDS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /** Records of the last [days] days (today included), oldest first. */
    fun recentRecords(days: Int): List<BmiRecord> {
        if (days <= 0) return emptyList()

        val start = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        val startDate = SimpleDateFormat(BmiDates.PATTERN, Locale.US).format(start.time)

        val cursor = readableDatabase.query(
            TABLE_RECORDS,
            arrayOf(COL_DATE, COL_BMI, COL_HEIGHT, COL_WEIGHT, COL_AGE, COL_GENDER),
            "$COL_DATE >= ?",
            arrayOf(startDate),
            null,
            null,
            "$COL_DATE ASC"
        )

        val records = mutableListOf<BmiRecord>()
        cursor.use { c ->
            while (c.moveToNext()) {
                records.add(
                    BmiRecord(
                        date = c.getString(0),
                        bmi = c.getDouble(1),
                        heightCm = c.getDouble(2),
                        weightKg = c.getDouble(3),
                        age = c.getInt(4),
                        gender = Gender.valueOf(c.getString(5))
                    )
                )
            }
        }
        return records
    }

    private companion object {
        const val DATABASE_NAME = "bmi.db"
        const val DATABASE_VERSION = 1

        const val TABLE_RECORDS = "bmi_records"
        const val COL_DATE = "date"
        const val COL_BMI = "bmi"
        const val COL_HEIGHT = "height_cm"
        const val COL_WEIGHT = "weight_kg"
        const val COL_AGE = "age"
        const val COL_GENDER = "gender"
        const val COL_UPDATED_AT = "updated_at"
    }
}
