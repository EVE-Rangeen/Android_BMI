package com.example.bmi.ui.report

import androidx.annotation.StringRes
import com.example.bmi.R
import com.example.bmi.data.AppLanguage
import com.example.bmi.data.BmiProfile

/*
 * ============================================================================
 * AdviceUiState.kt —— 定义 AI 建议功能用到的几种“状态”和“请求”
 * ============================================================================
 * 【这个文件是干什么的】
 *   AI 建议功能有一个清晰的“状态机”：
 *     没开始 → 加载中 → 成功 / 失败。
 *   这里用 Kotlin 的 sealed class（密封类）把这些状态建模出来，
 *   让界面只能处理这几种确定的状态，不会漏、不会出现乱七八糟的值。
 *
 * 【sealed class（密封类）是什么】
 *   普通 class 可以随便继承；sealed class 只能在“同一个文件里”被继承，
 *   所以编译器知道它一共只有几种可能，配合 when 使用时能保证“穷举”。
 *
 * 【谁在用这个文件】
 *   - ReportViewModel 创建这些状态；
 *   - ReportActivity 观察这些状态并渲染。
 */

/**
 * 一次 AI 请求需要的一切信息。
 * 用一个 data class 打包，方便 ViewModel 记住“上次请求”，失败后重试。
 */
data class AdviceRequest(
    val profile: BmiProfile,     // 用户的 BMI / 年龄 / 性别
    val language: AppLanguage    // 回答语言
)

/**
 * 失败原因枚举。每个原因对应一个“本地化文案 id”。
 *
 * @param messageRes 指向 strings.xml 里的文案 id（例如 R.string.llm_error_network）。
 *                   界面拿到这个枚举后，用 getString(messageRes) 显示对应语言的文字。
 */
enum class ErrorReason(@param:StringRes val messageRes: Int) {
    MISSING_API_KEY(R.string.llm_error_missing_key),  // 没配置 key
    NETWORK(R.string.llm_error_network),              // 网络问题
    API(R.string.llm_error_api),                      // 接口返回错误
    UNKNOWN(R.string.llm_error_unknown)               // 其他未知错误
}

/**
 * AI 建议卡片的“一次性状态”。
 */
sealed class AdviceUiState {

    /** 还没有发起请求。 */
    object Idle : AdviceUiState()

    /** 请求已发出，等待返回（界面显示转圈）。 */
    object Loading : AdviceUiState()

    /** 请求成功，text 是 AI 返回的建议。 */
    data class Success(val text: String) : AdviceUiState()

    /** 请求失败，reason 说明为什么失败。 */
    data class Error(val reason: ErrorReason) : AdviceUiState()
}
