package com.example.bmi.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * ============================================================================
 * BmiDates.kt —— 日期工具（字符串日期 ↔ Date 对象的转换）
 * ============================================================================
 * 【这个文件是干什么的】
 *   SQLite 里存的是字符串日期（比如 "2026-11-14"），而图表要按真实日期计算
 *   横坐标、要显示成 "11-14" 这种短标签。这里提供三个工具函数：
 *     today()      → 今天的日期字符串；
 *     parse()      → 字符串转 Date 对象；
 *     shortLabel() → 完整日期缩短成 "MM-dd"。
 *
 * 【为什么单独一个文件】
 *   原来 BmiDates 写在 BmiRecord.kt 里，但 BmiDbHelper、BmiTrendChartView、
 *   ReportActivity 也都在用 BmiDates。单独拆出来，删“记录”时不会误伤“日期工具”。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】（考试删文件前先看这里）
 *   本文件依赖：无（只用 Java 自带的日期类）。
 *   谁依赖本文件：BmiDbHelper、BmiTrendChartView、ReportActivity。
 *   删除指引：BmiDates 是“按天存记录 + 画趋势图”才需要的日期工具；
 *             如果砍掉 SQLite 历史 / 趋势图，可以连同本文件一起删。
 * ────────────────────────────────────────────────────────────────────────────
 */

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
