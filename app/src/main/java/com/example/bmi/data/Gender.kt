package com.example.bmi.data

/*
 * ============================================================================
 * Gender.kt —— 用户的性别（枚举）
 * ============================================================================
 * 【这个文件是干什么的】
 *   定义“性别”只有两个取值：男 / 女。很多地方都要用到它：
 *   - 报告页根据性别查不同的判定表；
 *   - 数据库里存性别（MALE / FEMALE）；
 *   - AI 提示词里显示“男 / 女”。
 *
 * 【为什么要单独一个文件】
 *   原来 Gender 写在 BmiProfile.kt 里，但 BmiRecord、BmiDbHelper、ReportActivity、
 *   PromptBuilder 都在用 Gender。如果谁以为“BmiProfile 是 AI 用的”就删掉它，
 *   连 SQLite 和趋势图都会一起报错。单独拆出来后，每个文件只做一件事，
 *   删文件时不会“连坐”别的功能。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】（考试删文件前先看这里）
 *   本文件依赖：无（不 import 任何本项目内的类）。
 *   谁依赖本文件：BmiProfile、BmiRecord、BmiDbHelper、ReportActivity、PromptBuilder。
 *   删除指引：Gender 属于“性别”这个基础数据，只要用到性别就保留它；
 *             只有当你连性别都不要（最简版计算器）时，才随其它文件一起删。
 * ────────────────────────────────────────────────────────────────────────────
 */

/**
 * 用户的性别。
 *
 * 注意：性别是从 MainActivity 的下拉框（Spinner）里选出来的，
 * 下拉框只给了我们一个“位置序号”（0 或 1），
 * 这里把它翻译成有明确含义的 MALE / FEMALE。
 */
enum class Gender {
    MALE,    // 男
    FEMALE;  // 女

    companion object {
        /**
         * 把下拉框的位置序号转换成 Gender。
         *
         * 下拉框的数据来自 res/values/strings.xml 里的 <string-array name="gender">，
         * 第 0 项是“男”，第 1 项是“女”。
         * 因此：位置 1 是女性，其余情况一律按男性处理（防御性写法，避免越界出错）。
         */
        fun fromSpinnerPosition(position: Int): Gender =
            if (position == 1) FEMALE else MALE
    }
}
