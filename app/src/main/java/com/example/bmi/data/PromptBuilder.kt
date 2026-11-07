package com.example.bmi.data

import java.util.Locale

/**
 * Builds the prompts sent to the LLM.
 *
 * The language of the answer is forced by the system prompt, so a user running
 * the app in Chinese always gets Chinese advice, even though the model behind
 * the API is trained mostly on English data.
 */
object PromptBuilder {

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

    fun userPrompt(profile: BmiProfile, language: AppLanguage): String = when (language) {
        AppLanguage.CHINESE -> String.format(
            Locale.US,
            "BMI：%.1f%n性别：%s%n年龄：%d%n请给出健康建议。",
            profile.bmi,
            genderLabel(profile.gender, language),
            profile.age
        )

        AppLanguage.ENGLISH -> String.format(
            Locale.US,
            "BMI: %.1f%nGender: %s%nAge: %d%nPlease give me health suggestions.",
            profile.bmi,
            genderLabel(profile.gender, language),
            profile.age
        )
    }

    private fun genderLabel(gender: Gender, language: AppLanguage): String =
        when (language) {
            AppLanguage.CHINESE -> if (gender == Gender.MALE) "男" else "女"
            AppLanguage.ENGLISH -> if (gender == Gender.MALE) "male" else "female"
        }
}
