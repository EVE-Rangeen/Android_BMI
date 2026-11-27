package com.example.bmi

import com.example.bmi.data.AppLanguage
import com.example.bmi.data.PromptBuilder
import com.example.bmi.data.WellnessRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * ============================================================================
 * PromptBuilderTest.kt —— AI 提示词的单元测试（【AI 功能 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   验证 PromptBuilder 拼出来的提示词“内容对不对”：
 *     · 中文请求必须要求 AI 用中文回答，不能出现英文规则；
 *     · 英文请求必须要求 AI 用英文回答，不能出现中文规则；
 *     · 记录里的日期、状况、步数、备注必须都出现在用户提示词里。
 *
 * 【为什么能不用模拟器就测】
 *   PromptBuilder 是纯 Kotlin（不 import 任何 Android 类，也不用 Context），
 *   所以 JUnit 在电脑的 JVM 上就能直接跑：
 *     gradlew.bat testDebugUnitTest
 *   如果逻辑写在 Activity 里（要 Context、要资源），就只能上模拟器做仪器测试。
 *
 * 【断言用哪个方法】
 *   assertTrue(条件)  —— 条件为真才算通过；
 *   assertFalse(条件) —— 条件为假才算通过。
 *   assertEquals(期望, 实际) 用于精确比对。
 *
 * ★ 考试不做 AI 时，删掉本文件。
 * ============================================================================
 */
class PromptBuilderTest {

    // 测试用的一条固定记录：良好、8000 步、有备注。
    private val record = WellnessRecord(
        date = "2026-11-27",
        condition = 0,
        steps = 8000,
        note = "slept well"
    )

    @Test
    fun chineseSystemPromptForcesChineseAnswer() {
        val systemPrompt = PromptBuilder.systemPrompt(AppLanguage.CHINESE)
        assertTrue(systemPrompt.contains("简体中文"))
        assertFalse(systemPrompt.contains("Answer in English only"))
    }

    @Test
    fun chineseSystemPromptAsksForThreeNumberedSuggestions() {
        val systemPrompt = PromptBuilder.systemPrompt(AppLanguage.CHINESE)
        assertTrue(systemPrompt.contains("3 条建议"))
        assertTrue(systemPrompt.contains("「1. 2. 3.」"))
    }

    @Test
    fun chineseUserPromptContainsRecordData() {
        val userPrompt = PromptBuilder.userPrompt(record, AppLanguage.CHINESE)
        assertTrue(userPrompt.contains("日期：2026-11-27"))
        assertTrue(userPrompt.contains("身体状况：良好"))
        assertTrue(userPrompt.contains("步数：8000"))
        assertTrue(userPrompt.contains("备注：slept well"))
    }

    @Test
    fun englishSystemPromptForcesEnglishAnswer() {
        val systemPrompt = PromptBuilder.systemPrompt(AppLanguage.ENGLISH)
        assertTrue(systemPrompt.contains("Answer in English only"))
        assertFalse(systemPrompt.contains("简体中文"))
    }

    @Test
    fun englishUserPromptContainsRecordData() {
        val userPrompt = PromptBuilder.userPrompt(record, AppLanguage.ENGLISH)
        assertTrue(userPrompt.contains("Date: 2026-11-27"))
        assertTrue(userPrompt.contains("Condition: Good"))
        assertTrue(userPrompt.contains("Steps: 8000"))
        assertTrue(userPrompt.contains("Notes: slept well"))
    }

    @Test
    fun blankNoteIsWrittenAsNone() {
        // 备注为空时不能让 AI 看到“备注：”后面空着，中英文各写一个占位词。
        val noNote = record.copy(note = "")
        assertTrue(PromptBuilder.userPrompt(noNote, AppLanguage.CHINESE).contains("备注：无"))
        assertTrue(PromptBuilder.userPrompt(noNote, AppLanguage.ENGLISH).contains("Notes: none"))
    }

    @Test
    fun conditionIsTranslatedToTheAnswerLanguage() {
        // 同一个 1（一般），中文提示词写“一般”，英文提示词写“Fair”。
        val fair = record.copy(condition = 1)
        assertTrue(PromptBuilder.userPrompt(fair, AppLanguage.CHINESE).contains("身体状况：一般"))
        assertTrue(PromptBuilder.userPrompt(fair, AppLanguage.ENGLISH).contains("Condition: Fair"))
    }
}
