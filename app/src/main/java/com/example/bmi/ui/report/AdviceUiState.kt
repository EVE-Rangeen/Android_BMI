package com.example.bmi.ui.report

import androidx.annotation.StringRes
import com.example.bmi.R
import com.example.bmi.data.AppLanguage
import com.example.bmi.data.BmiProfile

/** Everything the ViewModel needs to ask for advice. */
data class AdviceRequest(
    val profile: BmiProfile,
    val language: AppLanguage
)

/** Why the advice could not be loaded, mapped to a localized message. */
enum class ErrorReason(@param:StringRes val messageRes: Int) {
    MISSING_API_KEY(R.string.llm_error_missing_key),
    NETWORK(R.string.llm_error_network),
    API(R.string.llm_error_api),
    UNKNOWN(R.string.llm_error_unknown)
}

/** One-shot state of the AI advice card. */
sealed class AdviceUiState {

    /** No request has been made yet. */
    object Idle : AdviceUiState()

    object Loading : AdviceUiState()

    data class Success(val text: String) : AdviceUiState()

    data class Error(val reason: ErrorReason) : AdviceUiState()
}
