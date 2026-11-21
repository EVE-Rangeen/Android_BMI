package com.example.bmi.data

/*
 * ============================================================================
 * BmiRecord.kt —— SQLite 里的一条 BMI 记录（纯数据类）
 * ============================================================================
 * 【这个文件是干什么的】
 *   BmiRecord 是“数据库里一行记录”在内存中的样子（data class）。
 *   它本身不含任何逻辑，就是六个字段打包：日期、BMI、身高、体重、年龄、性别。
 *
 * 【为什么数据库存字符串日期，而不是存时间戳】
 *   因为需求是“一天只保留一条最新记录”，直接拿 "2026-11-14" 这种字符串当主键，
 *   同一天再存一次就会自动覆盖，实现起来最简单。而且字符串排序（ASC）就是按日期排序。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】（考试删文件前先看这里）
 *   本文件依赖：Gender（同目录 Gender.kt）。
 *   谁依赖本文件：BmiDbHelper、BmiHistoryRepository、ReportActivity、
 *                 TrendViewModel、BmiTrendChartView。
 *   删除指引：BmiRecord 是“SQLite 历史 + 趋势图”共用的数据模型；
 *             砍掉这两个功能后，本文件可连同删除（Gender 是否保留同上）。
 * ────────────────────────────────────────────────────────────────────────────
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
