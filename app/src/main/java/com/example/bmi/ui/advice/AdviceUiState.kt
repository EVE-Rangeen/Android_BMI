package com.example.bmi.ui.advice

import androidx.annotation.StringRes
import com.example.bmi.R
import com.example.bmi.data.AppLanguage
import com.example.bmi.data.WellnessRecord

/*
 * ============================================================================
 * AdviceUiState.kt —— AI 建议的“请求”和“状态”（【AI 功能 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   AI 功能有一个清晰的状态机：没开始 → 加载中 → 成功 / 失败。
 *   这里用 sealed class（密封类）把这些状态建模出来，
 *   界面只可能收到这几种确定的状态，不会漏处理。
 *
 * 【sealed class（密封类）是什么】
 *   普通 class 谁都能继承；sealed class 只允许在“同一个文件里”被继承，
 *   所以编译器知道它一共只有几种可能。配合 when 使用时能强制穷举，
 *   少写一种情况就会编译报错。
 *
 * 【谁在用这个文件】
 *   - AdviceViewModel 产生这些状态；
 *   - AdviceActivity 观察这些状态并渲染界面。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：WellnessRecord、AppLanguage、R.string.*（错误文案）。
 *   谁依赖本文件：AdviceActivity、AdviceViewModel。
 *   ★ 考试不做 AI 时删掉本文件（见 docs/exam-guide.md B 部分）。
 * ────────────────────────────────────────────────────────────────────────────
 */

/**
 * 一次 AI 请求需要的一切信息。
 * 用 data class 打包，ViewModel 就能记住“上次请求”，失败后原样重试。
 */
data class AdviceRequest(
    val record: WellnessRecord,   // 要分析的那一天记录
    val language: AppLanguage     // 回答语言
)

/**
 * 失败原因。每个原因对应一个本地化文案 id。
 *
 * @param messageRes 指向 strings.xml 里的文案（例如 R.string.ai_error_network）。
 *                   界面拿到后用 getString(messageRes) 显示当前语言的文字。
 */
enum class ErrorReason(@param:StringRes val messageRes: Int) {
    MISSING_API_KEY(R.string.ai_error_missing_key),  // 没配置 API key
    NETWORK(R.string.ai_error_network),              // 网络问题（断网、超时）
    API(R.string.ai_error_api),                      // 接口返回错误（401/429/500…）
    UNKNOWN(R.string.ai_error_unknown)               // 其它未知错误
}

/** AI 建议页面的四种状态。 */
sealed class AdviceUiState {

    /** 还没有发起请求。 */
    object Idle : AdviceUiState()

    /** 请求已发出，等待返回（界面显示转圈）。 */
    object Loading : AdviceUiState()

    /** 成功，text 是 AI 返回的建议正文。 */
    data class Success(val text: String) : AdviceUiState()

    /** 失败，reason 说明失败原因。 */
    data class Error(val reason: ErrorReason) : AdviceUiState()
}
