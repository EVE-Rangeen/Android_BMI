package com.example.bmi.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
 * ============================================================================
 * BmiDbHelper.kt —— 用 SQLite 保存每天的 BMI 记录
 * ============================================================================
 * 【这个文件是干什么的】
 *   把每一次 BMI 计算结果按“天”存进 SQLite 数据库，用于画趋势折线图。
 *   关键需求：如果同一天测了多次，只保留“最后”那一次。
 *
 * 【SQLite 是什么】
 *   SQLite 是 Android 内置的轻量级关系数据库，不需要单独安装服务器，
 *   就是一个文件（这里叫 bmi.db），用 SQL 语句增删改查。
 *
 * 【SQLiteOpenHelper 是什么】
 *   Android 提供的一个“数据库管家”父类。继承它之后，Android 会自动帮我们：
 *     1. 第一次使用时创建数据库文件；
 *     2. 调用 onCreate() 建表；
 *     3. 数据库版本升级时调用 onUpgrade()。
 *   我们只需要告诉它“建表 SQL 是什么”，其余都由它管理。
 *
 * 【谁在用这个文件】
 *   BmiHistoryRepository 通过它 save(...) 写记录、recentRecords(...) 读记录；
 *   ReportActivity 保存记录时，最终也是走到这里。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】（考试删文件前先看这里）
 *   本文件依赖：BmiRecord、Gender、BmiDates（都在同目录 data/ 下）。
 *   谁依赖本文件：BmiHistoryRepository。
 *   删除指引：BmiDbHelper 是“SQLite 存历史”的核心，属于【趋势/历史】功能。
 *             砍掉该功能时，连同 BmiRecord、BmiDates、Gender、BmiHistoryRepository
 *             一起删（并删 TrendActivity / 趋势相关布局与菜单入口）。
 * ────────────────────────────────────────────────────────────────────────────
 */
class BmiDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,  // 用 applicationContext，理由同 BmiPreferences
    DATABASE_NAME,               // 数据库文件名 bmi.db
    null,                        // CursorFactory，一般传 null
    DATABASE_VERSION             // 数据库版本号，升级表结构时 +1
) {

    /**
     * 数据库第一次被创建时调用，负责“建表”。
     * 注意：这个方法只在数据库文件不存在时执行一次，之后不会再执行。
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_RECORDS (
                $COL_DATE TEXT PRIMARY KEY,        -- 日期，作为主键（PRIMARY KEY）
                $COL_BMI REAL NOT NULL,           -- BMI 值（REAL = 小数）
                $COL_HEIGHT REAL NOT NULL,        -- 身高（厘米）
                $COL_WEIGHT REAL NOT NULL,        -- 体重（千克）
                $COL_AGE INTEGER NOT NULL,        -- 年龄
                $COL_GENDER TEXT NOT NULL,        -- 性别（MALE / FEMALE）
                $COL_UPDATED_AT INTEGER NOT NULL  -- 记录时间戳（毫秒）
            )
            """.trimIndent()
        )
        /*
         * ── 【考点 / 备用方案】PRIMARY KEY 为什么能让“一天只保留最新一条” ───
         * 主键（PRIMARY KEY）要求“这一列的值不能重复”。
         * 我们把 date 设为主键，于是同一天（同一个日期字符串）只能插入一行。
         * 配合 save() 里的 insertWithOnConflict(CONFLICT_REPLACE)：
         *   插入重复日期时不是报错，而是“把旧的那行替换掉”，
         *   因此自然实现了“一天多次测量只留最后一次”。
         *
         * 如果老师改需求为“同一天的所有记录都要保留”，只需两步：
         *   1) 去掉 PRIMARY KEY，改用一个自增 id 做主键；
         *   2) 把 save() 里的 CONFLICT_REPLACE 换成 CONFLICT_ABORT（或直接 insert）。
         * 等价建表语句（把下面这行注释去掉、把上面带 PRIMARY KEY 的注释掉即可）：
         *
         *   CREATE TABLE $TABLE_RECORDS (
         *       id INTEGER PRIMARY KEY AUTOINCREMENT,
         *       $COL_DATE TEXT NOT NULL,
         *       ...
         *   )
         * ────────────────────────────────────────────────────────────────────
         */
    }

    /**
     * 数据库版本号变大时调用（旧表结构需要升级时）。
     * 这里是最粗暴的写法：直接删表重建。因为是教学项目、数据不重要，所以可行。
     * 生产项目里应该用 ALTER TABLE 迁移数据，避免用户数据丢失。
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        onCreate(db)
    }

    /**
     * 保存（插入或替换）一条记录。
     * 同一天再存一次会自动覆盖，只保留最新的那次。
     */
    fun save(record: BmiRecord) {
        // ContentValues 相当于一个“键值对盒子”，key 是列名，value 是值。
        val values = ContentValues().apply {
            put(COL_DATE, record.date)
            put(COL_BMI, record.bmi)
            put(COL_HEIGHT, record.heightCm)
            put(COL_WEIGHT, record.weightKg)
            put(COL_AGE, record.age)
            put(COL_GENDER, record.gender.name)      // enum 的 name 就是 "MALE"/"FEMALE"
            put(COL_UPDATED_AT, System.currentTimeMillis())  // 当前毫秒时间戳
        }
        /*
         * insertWithOnConflict(表名, null, 值, CONFLICT_REPLACE)：
         *   有冲突（date 重复）时 REPLACE = 替换旧行。这就是“一天只留最新一条”的关键。
         */
        writableDatabase.insertWithOnConflict(
            TABLE_RECORDS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * 读取最近 [days] 天的记录（含今天），按日期从旧到新排序。
     * 趋势图默认读最近 30 天。
     */
    fun recentRecords(days: Int): List<BmiRecord> {
        if (days <= 0) return emptyList()

        // 计算“起点日期”：今天是 start，往前推 (days-1) 天。
        // 例如 days=30，就得到“今天往前 29 天”的日期字符串。
        val start = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        val startDate = SimpleDateFormat(BmiDates.PATTERN, Locale.US).format(start.time)

        /*
         * query(表, 列数组, 条件, 条件参数, 分组, 排序, 排序方式)：
         *   这里条件是 "date >= ?"（? 是占位符），参数是 startDate；
         *   按 date 升序（ASC）排列，让图表拿到“从旧到新”的顺序。
         * 使用 ? 占位符而不是直接拼接字符串，能防止 SQL 注入。
         */
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
        // cursor 类似“结果集游标”，moveToNext() 一次移动到下一行，读完为止。
        cursor.use { c ->
            while (c.moveToNext()) {
                records.add(
                    BmiRecord(
                        date = c.getString(0),       // 第 0 列：date
                        bmi = c.getDouble(1),        // 第 1 列：bmi
                        heightCm = c.getDouble(2),   // 第 2 列：height_cm
                        weightKg = c.getDouble(3),   // 第 3 列：weight_kg
                        age = c.getInt(4),           // 第 4 列：age
                        gender = Gender.valueOf(c.getString(5))  // 第 5 列：gender 转回枚举
                    )
                )
            }
        }
        return records
    }

    private companion object {
        const val DATABASE_NAME = "bmi.db"          // 数据库文件名
        const val DATABASE_VERSION = 1              // 版本号，改表结构时 +1

        const val TABLE_RECORDS = "bmi_records"     // 表名
        const val COL_DATE = "date"                 // 日期列
        const val COL_BMI = "bmi"                   // BMI 列
        const val COL_HEIGHT = "height_cm"          // 身高列
        const val COL_WEIGHT = "weight_kg"          // 体重列
        const val COL_AGE = "age"                   // 年龄列
        const val COL_GENDER = "gender"             // 性别列
        const val COL_UPDATED_AT = "updated_at"     // 时间戳列
    }
}
