package com.example.bmi.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * ============================================================================
 * BmiRecord.kt —— SQLite 里的一条 BMI 记录 + 日期工具
 * ============================================================================
 * 【这个文件是干什么的】
 *   1. BmiRecord 是“数据库里一行记录”在内存中的样子（data class）。
 *   2. BmiDates 是一组日期工具函数，负责把日期在“字符串”和“Date 对象”之间转换，
 *      数据库里存的是字符串（比如 "2026-11-14"），图表里要拿它排序、显示。
 *
 * 【为什么数据库存字符串日期，而不是存时间戳】
 *   因为需求是“一天只保留一条最新记录”，直接拿 "2026-11-14" 这种字符串当主键，
 *   同一天再存一次就会自动覆盖，实现起来最简单。而且字符串排序（ASC）就是按日期排序。
 */

/**
 * 一天的 BMI 记录（内存里的样子）。
 *
 * @property date      日期字符串，格式 "yyyy-MM-dd"，例如 "2026-11-14"。
 *                     它是数据库表的主键，所以同一天只会存在一条。
 * @property bmi       当天的 BMI 值。
 * @property heightCm  当天输入的身高（厘米），留作以后展示详情用。
 * @property weightKg  当天输入的体重（千克），留作以后展示详情用。
 * @property age       年龄。
 * @property gender    性别。
 */
data class BmiRecord(
    val date: String,
    val bmi: Double,
    val heightCm: Double,
    val weightKg: Double,
    val age: Int,
    val gender: Gender
)

/**
 * 日期工具（object 表示“单例”，全局只有一份，直接 BmiDates.today() 调用）。
 */
object BmiDates {

    /** 数据库里统一使用的日期格式。 */
    const val PATTERN = "yyyy-MM-dd"

    /**
     * 返回“今天”的日期字符串，例如 "2026-11-14"。
     * ReportActivity 保存记录时就用它作为这一天的 key。
     */
    fun today(): String =
        SimpleDateFormat(PATTERN, Locale.US).format(Date())

    /**
     * 把 "2026-11-14" 这种字符串解析成 Date 对象。
     * 图表要根据真实日期计算横坐标，所以需要拿到“这一天的毫秒时间戳”。
     *
     * @return 解析成功返回 Date；解析失败返回 null（调用方自己兜底）。
     */
    fun parse(date: String): Date? =
        SimpleDateFormat(PATTERN, Locale.US).parse(date)

    /**
     * 把完整日期缩短成 "MM-dd"，用于图表下方的日期标签。
     * 例如 "2026-11-14" → "11-14"，这样横轴上放得下。
     */
    fun shortLabel(date: String): String {
        val parsed = parse(date) ?: return date
        return SimpleDateFormat("MM-dd", Locale.US).format(parsed)
    }
}
