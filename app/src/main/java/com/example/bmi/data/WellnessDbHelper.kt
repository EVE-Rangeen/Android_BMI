package com.example.bmi.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/*
 * ============================================================================
 * WellnessDbHelper.kt —— 用 SQLite 保存每天的健康日志
 * ============================================================================
 * 【这个文件是干什么的】
 *   1. 第一次使用时创建 wellness.db 数据库和 wellness_records 表；
 *   2. save() 把一条记录写进表里（同一天再存会覆盖旧记录）；
 *   3. allRecords() 把所有记录按日期从新到旧读出来，给历史页显示。
 *
 * 【为什么继承 SQLiteOpenHelper】
 *   Android 内置的 SQLite 需要我们自己写建表 SQL，但“什么时候建库、什么时候升级”
 *   这些琐事由 SQLiteOpenHelper 管：
 *     - 数据库文件不存在 → 自动调用 onCreate()；
 *     - DATABASE_VERSION 变大 → 自动调用 onUpgrade()。
 *
 * 【和作业里 BmiDbHelper 的关系】
 *   结构几乎一样，只是表名、列名、字段不同。考试时可以直接照抄 BmiDbHelper，
 *   把这三样东西换掉即可，这是最省时间的做法。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】
 *   本文件依赖：WellnessRecord。
 *   谁依赖本文件：MainActivity（保存）、HistoryActivity（读取）。
 * ────────────────────────────────────────────────────────────────────────────
 */
class WellnessDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,   // 用 Application Context，避免持有 Activity 造成内存泄漏
    DATABASE_NAME,
    null,                         // CursorFactory，一般传 null
    DATABASE_VERSION
) {

    /**
     * 数据库第一次创建时调用，只执行一次。
     *
     * date 设成 PRIMARY KEY：同一天只能有一行；
     * updated_at 记录最后修改时间，方便以后按“最近修改”排序或显示。
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_RECORDS (
                $COL_DATE TEXT PRIMARY KEY NOT NULL,
                $COL_CONDITION INTEGER NOT NULL,
                $COL_STEPS INTEGER NOT NULL,
                $COL_NOTE TEXT NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    /**
     * 数据库版本号变大时调用（表结构变了才会走到这里）。
     * 教学项目里直接删表重建最省事；真实项目要用 ALTER TABLE 保留用户数据。
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        onCreate(db)
    }

    /**
     * 保存一条记录。
     *
     * insertWithOnConflict(..., CONFLICT_REPLACE) 的含义：
     *   如果 date 已经存在（同一天已经记过），就把旧行替换成新行，
     *   而不是报错。这样“一天多条”最终只留最后一条。
     */
    fun save(record: WellnessRecord) {
        val values = ContentValues().apply {
            put(COL_DATE, record.date)
            put(COL_CONDITION, record.condition)
            put(COL_STEPS, record.steps)
            put(COL_NOTE, record.note)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_RECORDS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * 读取全部记录，按日期从新到旧排序（DESC）。
     * 历史页直接把结果丢给 ListView，所以最新的记录显示在最上面。
     */
    fun allRecords(): List<WellnessRecord> {
        val records = mutableListOf<WellnessRecord>()

        // query(...) 的参数依次是：表名、要查的列、where 条件、条件参数、groupBy、having、orderBy
        // 这里没有条件（null），排序规则是 "date DESC"。
        val cursor = readableDatabase.query(
            TABLE_RECORDS,
            arrayOf(COL_DATE, COL_CONDITION, COL_STEPS, COL_NOTE),
            null,
            null,
            null,
            null,
            "$COL_DATE DESC"
        )

        // cursor.use { } 会在读完后自动 close()，不会忘记释放资源。
        cursor.use { c ->
            while (c.moveToNext()) {
                records.add(
                    WellnessRecord(
                        date = c.getString(0),
                        condition = c.getInt(1),
                        steps = c.getInt(2),
                        note = c.getString(3)
                    )
                )
            }
        }
        return records
    }

    /**
     * 按日期查一条记录，查不到返回 null。
     *
     * 【AI 功能 · 考试可删】目前只有 AdviceActivity 用它取“今天的记录”。
     * 不做 AI 时这个方法可以删掉，不影响保存和历史页。
     *
     * query 的第三个参数是 where 条件，第四个参数是条件里的值：
     *   selection     = "date = ?"   —— ? 是占位符；
     *   selectionArgs = arrayOf(date) —— 真正的值按顺序填进 ? 里。
     * 用 ? 占位而不是把日期拼进 SQL 字符串，可以防止 SQL 注入、也不用手动加引号。
     */
    fun findByDate(date: String): WellnessRecord? {
        val cursor = readableDatabase.query(
            TABLE_RECORDS,
            arrayOf(COL_DATE, COL_CONDITION, COL_STEPS, COL_NOTE),
            "$COL_DATE = ?",
            arrayOf(date),
            null,
            null,
            null
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                WellnessRecord(
                    date = c.getString(0),
                    condition = c.getInt(1),
                    steps = c.getInt(2),
                    note = c.getString(3)
                )
            } else {
                null
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "wellness.db"     // 数据库文件名
        const val DATABASE_VERSION = 1              // 改表结构时 +1

        const val TABLE_RECORDS = "wellness_records"
        const val COL_DATE = "date"
        const val COL_CONDITION = "condition"
        const val COL_STEPS = "steps"
        const val COL_NOTE = "note"
        const val COL_UPDATED_AT = "updated_at"
    }
}
