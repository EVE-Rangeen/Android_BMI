package com.example.bmi.data

/*
 * ============================================================================
 * PromptBuilder.kt —— 拼装发给 AI 的“提示词”（【AI 功能 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   大语言模型（LLM）靠“提示词（prompt）”工作：你问什么，它答什么。
 *   这里把“今天的健康记录 + 用中文还是英文回答”拼成两段文本：
 *     1. systemPrompt：系统提示词，规定 AI 的身份和回答规则；
 *     2. userPrompt：  用户提示词，放入具体的日期、身体状况、步数、备注。
 *
 * 【为什么单独抽成一个文件（重要设计理由）】
 *   它不 import 任何 Android 类，是“纯 Kotlin”，
 *   所以可以在电脑上直接用 JUnit 跑单元测试（见 app/src/test/PromptBuilderTest.kt），
 *   不需要模拟器。考试里“把逻辑和界面分开”通常能加分。
 *
 * 【为什么身体状况文字要在这里自己映射，而不用 R.string.xxx】
 *   因为提示词的语言必须跟“回答语言”一致：用户切到英文界面时，
 *   AI 也应该用英文回答。用 Android 资源的话，这段代码就不能脱离 Context 测试了。
 *
 * >>> 【改题点 C9】题目换了记录内容时，改 userPrompt() 里的字段名和单位，
 *     再改 conditionLabel() 的映射（如果选项不是“良好/一般/较差”）。
 *     改完一定要去 PromptBuilderTest 里同步断言，否则单元测试会红。
 *     systemPrompt() 里的“给出 3 条建议”也可以按题目要求改成别的条数/角度。
 * <<<
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：WellnessRecord、AppLanguage。
 *   谁依赖本文件：WellnessAdviceRepository。
 *   ★ 考试不做 AI 时删掉本文件（见 docs/exam-guide.md B 部分）；
 *     如果保留 AI 但不要中英文切换，把 AppLanguage 换成写死的"中文"分支即可。
 * ────────────────────────────────────────────────────────────────────────────
 */
object PromptBuilder {

    /**
     * 系统提示词：告诉 AI “你是谁、要遵守什么规则”。
     * 中文界面 → 中文规则；英文界面 → 英文规则。
     */
    fun systemPrompt(language: AppLanguage): String = when (language) {
        AppLanguage.CHINESE ->
            "你是一位专业的健康管理顾问。用户会提供某一天的身体状况、步数和备注。" +
                "请根据这些信息给出一份简短、实用、可执行的健康建议。" +
                "要求：1) 只用简体中文回答；2) 给出 3 条建议，分别涉及运动、饮食和作息，" +
                "每条不超过 40 个字；3) 用「1. 2. 3.」编号，不要使用 Markdown 符号；" +
                "4) 语气友好、专业，不要做医学诊断，也不要重复用户已经提供的数据。"

        AppLanguage.ENGLISH ->
            "You are a professional wellness advisor. " +
                "The user will give you the condition, step count and notes of one day. " +
                "Give short, practical and actionable health suggestions based on that information. " +
                "Requirements: 1) Answer in English only; 2) Give exactly 3 suggestions covering " +
                "exercise, diet and daily routine, at most 40 words each; 3) Number them as " +
                "\"1. 2. 3.\" without any Markdown formatting; 4) Be friendly and professional, " +
                "do not make a medical diagnosis and do not repeat the data the user already provided."
    }

    /**
     * 用户提示词：把一条具体记录填进模板。
     *
     * @param record   要分析的那一天的记录（日期 / 身体状况 / 步数 / 备注）。
     * @param language 决定模板语言（同时决定状况、备注标签用哪种语言）。
     */
    fun userPrompt(record: WellnessRecord, language: AppLanguage): String = when (language) {
        AppLanguage.CHINESE -> buildString {
            // 【改题点 C9】这里每一个字段都要和 WellnessRecord、数据库列对应上；
            // 单位（ml / 元 / 分钟）也要写清楚，AI 才知道数字的含义。
            append("日期：").append(record.date).append('\n')
            append("身体状况：").append(conditionLabel(record.condition, language)).append('\n')
            append("步数：").append(record.steps).append('\n')
            // 备注可以为空，空的时候写“无”，避免发给 AI 一个空行造成误解。
            append("备注：").append(record.note.ifBlank { "无" }).append('\n')
            append("请给出健康建议。")
        }

        AppLanguage.ENGLISH -> buildString {
            append("Date: ").append(record.date).append('\n')
            append("Condition: ").append(conditionLabel(record.condition, language)).append('\n')
            append("Steps: ").append(record.steps).append('\n')
            append("Notes: ").append(record.note.ifBlank { "none" }).append('\n')
            append("Please give me health suggestions.")
        }
    }

    /**
     * 把数据库里的 0/1/2 翻成提示词里用的文字。
     * 0 = 良好（Good），1 = 一般（Fair），2 = 较差（Poor），顺序和 Spinner 一致。
     */
    private fun conditionLabel(condition: Int, language: AppLanguage): String =
        when (language) {
            AppLanguage.CHINESE -> when (condition) {
                0 -> "良好"
                1 -> "一般"
                else -> "较差"
            }
            AppLanguage.ENGLISH -> when (condition) {
                0 -> "Good"
                1 -> "Fair"
                else -> "Poor"
            }
        }
}
