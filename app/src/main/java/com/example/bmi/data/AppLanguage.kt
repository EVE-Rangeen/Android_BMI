package com.example.bmi.data

/**
 * The language the app currently shows to the user.
 *
 * It is used in two places:
 *  - to tell the LLM which language it must answer in;
 *  - (implicitly) to pick the translated UI strings.
 */
enum class AppLanguage {
    ENGLISH,
    CHINESE;

    companion object {
        fun fromLanguageTag(languageTag: String): AppLanguage =
            if (languageTag.startsWith("zh", ignoreCase = true)) CHINESE else ENGLISH
    }
}
