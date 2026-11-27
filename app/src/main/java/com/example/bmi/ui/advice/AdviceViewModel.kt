package com.example.bmi.ui.advice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.LlmException
import com.example.bmi.data.MissingApiKeyException
import com.example.bmi.data.WellnessAdviceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

/*
 * ============================================================================
 * AdviceViewModel.kt —— AI 建议页的 ViewModel（MVVM 里的 VM）（【AI 功能 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   负责 AI 建议的“业务逻辑”：发起请求、把结果/异常转成状态、支持重试。
 *   Activity 不碰网络，只 observe（观察）这里的状态并照着渲染。
 *
 * 【ViewModel 是什么（考试高频概念）】
 *   专门“装跟界面相关的数据 + 逻辑”的组件。关键特性：
 *   屏幕旋转导致 Activity 销毁重建时，ViewModel 不会被销毁，
 *   所以里面的数据能跨旋转存活 —— 旋转后不会重复发请求、不会重复扣费。
 *
 * 【LiveData 是什么】
 *   一种可观察的数据容器。Activity observe 它之后，值一变界面就自动刷新。
 *   - MutableLiveData：内部可修改的版本；
 *   - LiveData：对外只读的版本（防止外部乱改）。
 *
 * 【viewModelScope 是什么】
 *   ViewModel 自带的协程作用域，ViewModel 被清除时会自动取消里面的协程，
 *   避免“页面已经关了还在跑请求”造成的内存泄漏。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：WellnessAdviceRepository、AdviceUiState。
 *   谁依赖本文件：AdviceActivity。
 *   ★ 考试不做 AI 时删掉本文件，并把 AdviceActivity 里所有 viewModel 相关代码删掉
 *     （见 docs/exam-guide.md B 部分；也可以直接用 lifecycleScope 代替 ViewModel）。
 * ────────────────────────────────────────────────────────────────────────────
 */
class AdviceViewModel(
    // 数据来源。默认创建真的 Repository；测试时可以传入“假”的实现。
    private val repository: WellnessAdviceRepository = WellnessAdviceRepository()
) : ViewModel() {

    // 内部可写状态；对外只暴露只读的 adviceState。
    private val _adviceState = MutableLiveData<AdviceUiState>(AdviceUiState.Idle)
    val adviceState: LiveData<AdviceUiState> = _adviceState

    /** 记住上一次请求，供 retry() 使用。 */
    private var lastRequest: AdviceRequest? = null

    /** 发起一次 AI 建议请求。 */
    fun loadAdvice(request: AdviceRequest) {
        lastRequest = request                        // 记住请求，方便重试
        _adviceState.value = AdviceUiState.Loading   // 先进入“加载中”

        viewModelScope.launch {
            _adviceState.value = try {
                // 调仓库拿建议（内部已切到 IO 线程，不会卡界面）。
                val text = repository.fetchAdvice(request.record, request.language)
                AdviceUiState.Success(text)
            } catch (e: CancellationException) {
                throw e   // 协程被取消必须重新抛出，否则协程无法正常结束
            } catch (e: MissingApiKeyException) {
                AdviceUiState.Error(ErrorReason.MISSING_API_KEY)
            } catch (e: LlmException) {
                AdviceUiState.Error(ErrorReason.API)
            } catch (e: IOException) {
                // 断网、超时、DNS 失败等都会被包成 IOException。
                AdviceUiState.Error(ErrorReason.NETWORK)
            } catch (e: Exception) {
                AdviceUiState.Error(ErrorReason.UNKNOWN)
            }
        }
    }

    /** 重新发起上一次请求（“重试”按钮调用）。 */
    fun retry() {
        lastRequest?.let { loadAdvice(it) }
    }

    /**
     * 是否已经请求过。
     * AdviceActivity 用它判断“要不要在 onCreate 里再请求一次”，
     * 从而避免旋转屏幕后重复请求。
     */
    val hasRequested: Boolean
        get() = adviceState.value != AdviceUiState.Idle
}
