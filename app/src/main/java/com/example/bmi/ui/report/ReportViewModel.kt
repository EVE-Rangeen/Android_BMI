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

/*
 * ============================================================================
 * ReportViewModel.kt —— 报告页的 ViewModel（MVVM 里的 VM）
 * ============================================================================
 * 【这个文件是干什么的】
 *   负责 AI 建议的“业务逻辑”：发起请求、把结果/异常转成状态、支持重试。
 *   Activity 不碰网络，只观察这里的状态。
 *
 * 【ViewModel 是什么（考试高频概念）】
 *   ViewModel 是 Android 的架构组件，专门用来“装跟界面相关的数据 + 逻辑”。
 *   关键特性：当屏幕旋转、Activity 被销毁重建时，ViewModel 不会销毁，
 *   所以它里面的数据能跨“旋转”存活。这就是为什么旋转屏幕不会重复发请求。
 *
 * 【LiveData 是什么】
 *   LiveData 是一种“可观察的数据”。Activity 对它 observe（观察）之后，
 *   只要数据变了，观察者就会自动收到新数据并刷新界面。
 *   - MutableLiveData：可以修改的 LiveData（内部用）；
 *   - LiveData：只读的 LiveData（对外暴露，防止外部乱改）。
 */
class ReportViewModel(
    // 数据来源。默认 new 一个真的 Repository；测试时可以传“假的”。
    private val repository: HealthAdviceRepository = HealthAdviceRepository()
) : ViewModel() {

    /*
     * _adviceState 是内部可写的状态；adviceState 是对外暴露的只读版本。
     * 一开始设为 Idle（还没请求）。
     */
    private val _adviceState = MutableLiveData<AdviceUiState>(AdviceUiState.Idle)
    val adviceState: LiveData<AdviceUiState> = _adviceState

    /** 记住“上一次请求”，供 retry() 重试。 */
    private var lastRequest: AdviceRequest? = null

    /**
     * 发起一次 AI 建议请求。
     */
    fun loadAdvice(request: AdviceRequest) {
        lastRequest = request                 // 记住请求，方便重试
        _adviceState.value = AdviceUiState.Loading   // 先进入“加载中”

        /*
         * viewModelScope.launch { }：在 ViewModel 的协程作用域里启动一个协程。
         * 这个作用域会在 ViewModel 被清除时自动取消，避免内存泄漏。
         */
        viewModelScope.launch {
            _adviceState.value = try {
                // 调用仓库去拿建议（内部已经切到 IO 线程）。
                val text = repository.fetchAdvice(request.profile, request.language)
                AdviceUiState.Success(text)   // 成功 → Success 状态
            } catch (e: CancellationException) {
                throw e   // 协程被取消必须重新抛出，不能吞掉，否则协程无法正常取消
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

    /** 重新发起上一次请求（Retry 按钮调用）。 */
    fun retry() {
        lastRequest?.let { loadAdvice(it) }
    }

    /**
     * 是否已经发起过请求。
     * ReportActivity 用它判断“要不要在 onCreate 里再请求一次”，
     * 从而避免旋转屏幕后重复请求。
     */
    val hasRequested: Boolean
        get() = adviceState.value != AdviceUiState.Idle
}
