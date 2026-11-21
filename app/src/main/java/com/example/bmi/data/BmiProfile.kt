package com.example.bmi.data

/*
 * ============================================================================
 * BmiProfile.kt —— 一次 BMI 计算涉及的“用户个人信息”
 * ============================================================================
 * 【这个文件是干什么的】
 *   把“性别 + 年龄 + 算好的 BMI 值”打包成一个对象。这个对象会作为参数
 *   在 ReportActivity → ReportViewModel → HealthAdviceRepository → PromptBuilder
 *   之间传递，最终变成发给 AI 的提示词。
 *
 * 【为什么不直接用三个零散变量】
 *   把相关的数据放在一个 data class 里，一次传递一个对象，比传三个参数更清晰，
 *   也不容易漏传、传错顺序。这就是“Model（数据模型）”的含义。
 *
 * 【data class 是什么】
 *   data class 会自动生成 equals / hashCode / toString / copy 等方法，
 *   非常适合“只用来装数据”的类。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】（考试删文件前先看这里）
 *   本文件依赖：Gender（同目录 Gender.kt）。
 *   谁依赖本文件：ReportActivity、ReportViewModel、HealthAdviceRepository、
 *                 PromptBuilder、AdviceUiState。
 *   删除指引：BmiProfile 只被“报告页 + AI 建议”用到，主界面和趋势图不用它。
 *             如果你砍掉报告页 / AI 功能，可以连同本文件一起删（Gender 是否保留，
 *             取决于趋势/SQLite 功能还在不在，见 Gender.kt 的说明）。
 * ────────────────────────────────────────────────────────────────────────────
 */

/**
 * 一次 BMI 计算所需的全部个人信息。
 *
 * @property bmi    已经算好的 BMI 数值（体重 / 身高²），由 ReportActivity 计算。
 * @property age    年龄（整数）。
 * @property gender 性别（见同目录 Gender.kt）。
 */
data class BmiProfile(
    val bmi: Double,
    val age: Int,
    val gender: Gender
)
