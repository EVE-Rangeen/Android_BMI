package com.example.bmi.ui.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.HealthAdviceRepository
import com.example.bmi.data.LlmException
import com.example.bmi.data.MissingApiKeyException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * MVVM ViewModel for the report screen.
 *
 * The Activity only renders [adviceState] and calls [loadAdvice] / [retry];
 * all networking, error mapping and retry logic lives here. Because the
 * ViewModel survives configuration changes, rotating the device does not
 * trigger (and pay for) a second LLM request.
 */
class ReportViewModel(
    private val repository: HealthAdviceRepository = HealthAdviceRepository()
) : ViewModel() {

    private val _adviceState = MutableLiveData<AdviceUiState>(AdviceUiState.Idle)
    val adviceState: LiveData<AdviceUiState> = _adviceState

    private var lastRequest: AdviceRequest? = null

    fun loadAdvice(request: AdviceRequest) {
        lastRequest = request
        _adviceState.value = AdviceUiState.Loading
        viewModelScope.launch {
            _adviceState.value = try {
                val text = repository.fetchAdvice(request.profile, request.language)
                AdviceUiState.Success(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: MissingApiKeyException) {
                AdviceUiState.Error(ErrorReason.MISSING_API_KEY)
            } catch (e: LlmException) {
                AdviceUiState.Error(ErrorReason.API)
            } catch (e: IOException) {
                AdviceUiState.Error(ErrorReason.NETWORK)
            } catch (e: Exception) {
                AdviceUiState.Error(ErrorReason.UNKNOWN)
            }
        }
    }

    /** Re-runs the last request, used by the Retry button. */
    fun retry() {
        lastRequest?.let { loadAdvice(it) }
    }

    val hasRequested: Boolean
        get() = adviceState.value != AdviceUiState.Idle
}
