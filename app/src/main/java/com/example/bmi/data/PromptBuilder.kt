package com.example.bmi.data

import java.util.Locale

/*
 * ============================================================================
 * PromptBuilder.kt —— 拼装发给 AI 的“提示词”
 * ============================================================================
 * 【这个文件是干什么的】
 *   大语言模型（LLM）靠“提示词（prompt）”工作：你问什么，它答什么。
 *   这里负责把用户的 BMI / 性别 / 年龄，以及“用中文还是英文回答”的要求，
 *   拼成两段文本：
 *     1. systemPrompt：系统提示词，规定 AI 的身份和输出规则（先说规则）；
 *     2. userPrompt：  用户提示词，把用户的具体数据放进去。
 *
 * 【为什么单独抽成一个文件】
 *   因为它不依赖任何 Android 代码，是“纯 Kotlin”，
 *   所以可以写单元测试（见 PromptBuilderTest.kt）直接验证字符串内容是否正确。
 *
 * 【谁在用这个文件】
 *   HealthAdviceRepository 在发请求前调用这两个方法拿到提示词。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】（考试删文件前先看这里）
 *   本文件依赖：BmiProfile、Gender、AppLanguage（都在同目录 data/ 下）。
 *   谁依赖本文件：HealthAdviceRepository。
 *   删除指引：PromptBuilder 只服务于“给 AI 拼提示词”，属于【AI 建议】功能；
 *             砍掉 AI 时连同本文件一起删。
 * ────────────────────────────────────────────────────────────────────────────
 */
object PromptBuilder {

    /**
     * 系统提示词：告诉 AI “你是谁、要遵守什么规则”。
     * 语言跟界面走：中文界面 → 中文规则，英文界面 → 英文规则。
     */
    fun systemPrompt(language: AppLanguage): String = when (language) {
        AppLanguage.CHINESE ->
            "你是一位专业的营养与健康顾问。用户会提供自己的 BMI 数值、性别和年龄。" +
                "请根据这些信息给出一份简短、实用、可执行的健康建议。" +
                "要求：1) 只用简体中文回答；2) 给出 3 条建议，分别涉及饮食、运动和生活习惯，" +
                "每条不超过 40 个字；3) 用「1. 2. 3.」编号，不要使用 Markdown 符号；" +
                "4) 语气友好、专业，不要做医学诊断，也不要重复用户已经提供的数据。"

        AppLanguage.ENGLISH ->
            "You are a professional nutrition and health advisor. " +
                "The user will give you their BMI, gender and age. " +
                "Give short, practical and actionable health suggestions based on that information. " +
                "Requirements: 1) Answer in English only; 2) Give exactly 3 suggestions covering diet, " +
                "exercise and daily habits, at most 40 words each; 3) Number them as \"1. 2. 3.\" " +
                "without any Markdown formatting; 4) Be friendly and professional, do not make a " +
                "medical diagnosis and do not repeat the data the user already provided."
    }

    /**
     * 用户提示词：把一次 BMI 的具体数据填进模板。
     *
     * @param profile  本次的 BMI / 年龄 / 性别。
     * @param language 决定模板语言（顺带也决定了性别显示成“男”还是“male”）。
     */
    fun userPrompt(profile: BmiProfile, language: AppLanguage): String = when (language) {
        AppLanguage.CHINESE -> String.format(
            Locale.US,
            "BMI：%.1f%n性别：%s%n年龄：%d%n请给出健康建议。",
            profile.bmi,                                  // %.1f → 保留 1 位小数的 BMI
            genderLabel(profile.gender, language),        // %s → 性别文本
            profile.age                                   // %d → 年龄
        )

        AppLanguage.ENGLISH -> String.format(
            Locale.US,
            "BMI: %.1f%nGender: %s%nAge: %d%nPlease give me health suggestions.",
            profile.bmi,
            genderLabel(profile.gender, language),
            profile.age
        )
    }

    /**
     * 把 Gender 枚举翻译成对应语言下的文本。
     * 注意：这里不是用 Android 的字符串资源，而是直接写字面量，
     * 因为提示词是发给 AI 的，必须跟着“回答语言”走，而不是跟着手机界面语言走。
     */
    private fun genderLabel(gender: Gender, language: AppLanguage): String =
        when (language) {
            AppLanguage.CHINESE -> if (gender == Gender.MALE) "男" else "女"
            AppLanguage.ENGLISH -> if (gender == Gender.MALE) "male" else "female"
        }
}
