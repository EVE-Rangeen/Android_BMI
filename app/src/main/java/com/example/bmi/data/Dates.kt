package com.example.bmi.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * ============================================================================
 * Dates.kt —— 通用日期工具
 * ============================================================================
 * 【这个文件是干什么的】
 *   整个 App 只需要一个日期功能：取“今天”的 yyyy-MM-dd 字符串，
 *   作为健康日志的主键（同一天只保留一条记录）。
 *
 * 【为什么单独放一个文件】
 *   日期逻辑虽然只有一行，但保存记录、界面显示都要用。
 *   集中放在这里，以后要改成“可以选日期”时，只需要改这一处或在这里加方法。
 * ============================================================================
 */
object Dates {

    /** 数据库和界面统一使用的日期格式。 */
    const val PATTERN = "yyyy-MM-dd"

    /** 返回今天的日期字符串，例如 "2026-11-26"。 */
    fun today(): String =
        SimpleDateFormat(PATTERN, Locale.US).format(Date())
}
