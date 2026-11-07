package com.example.bmi

import com.example.bmi.data.AppLanguage
import com.example.bmi.data.BmiProfile
import com.example.bmi.data.Gender
import com.example.bmi.data.PromptBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The prompt is the contract with the LLM: if the user runs the app in Chinese,
 * the request must tell the model to answer in Chinese, and the BMI / gender /
 * age data must be present.
 */
class PromptBuilderTest {

    private val profile = BmiProfile(bmi = 23.4, age = 22, gender = Gender.MALE)

    @Test
    fun chinesePromptForcesChineseAnswer() {
        val systemPrompt = PromptBuilder.systemPrompt(AppLanguage.CHINESE)
        assertTrue(systemPrompt.contains("简体中文"))
        assertFalse(systemPrompt.contains("Answer in English"))
    }

    @Test
    fun chineseUserPromptContainsProfileData() {
        val userPrompt = PromptBuilder.userPrompt(profile, AppLanguage.CHINESE)
        assertTrue(userPrompt.contains("BMI：23.4"))
        assertTrue(userPrompt.contains("性别：男"))
        assertTrue(userPrompt.contains("年龄：22"))
    }

    @Test
    fun englishPromptForcesEnglishAnswer() {
        val systemPrompt = PromptBuilder.systemPrompt(AppLanguage.ENGLISH)
        assertTrue(systemPrompt.contains("Answer in English only"))
        assertFalse(systemPrompt.contains("简体中文"))
    }

    @Test
    fun englishUserPromptContainsProfileData() {
        val female = profile.copy(gender = Gender.FEMALE)
        val userPrompt = PromptBuilder.userPrompt(female, AppLanguage.ENGLISH)
        assertTrue(userPrompt.contains("BMI: 23.4"))
        assertTrue(userPrompt.contains("Gender: female"))
        assertTrue(userPrompt.contains("Age: 22"))
    }

    @Test
    fun systemPromptAsksForThreeNumberedSuggestions() {
        listOf(AppLanguage.CHINESE, AppLanguage.ENGLISH).forEach { language ->
            val prompt = PromptBuilder.systemPrompt(language)
            assertTrue(prompt.contains("3 "))
        }
    }
}
