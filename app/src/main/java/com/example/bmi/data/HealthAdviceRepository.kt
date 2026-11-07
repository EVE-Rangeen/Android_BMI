package com.example.bmi.data

import com.example.bmi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single entry point for the AI advice feature.
 *
 * Keeping the network call behind a repository means the ViewModel never
 * touches HTTP details, and the repository can be swapped for a fake in tests.
 */
class HealthAdviceRepository(
    private val apiKey: String = BuildConfig.DEEPSEEK_API_KEY,
    private val client: DeepSeekClient = DeepSeekClient(apiKey)
) {

    suspend fun fetchAdvice(profile: BmiProfile, language: AppLanguage): String {
        if (apiKey.isBlank()) {
            throw MissingApiKeyException()
        }
        return withContext(Dispatchers.IO) {
            client.requestAdvice(
                systemPrompt = PromptBuilder.systemPrompt(language),
                userPrompt = PromptBuilder.userPrompt(profile, language)
            )
        }
    }
}
