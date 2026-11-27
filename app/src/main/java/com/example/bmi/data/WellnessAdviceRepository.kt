package com.example.bmi.data

import com.example.bmi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * ============================================================================
 * WellnessAdviceRepository.kt —— AI 健康建议的“统一入口”（【AI 功能 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   把“调用 AI 拿建议”包装成一个简单的挂起函数 fetchAdvice(...)。
 *   ViewModel 只调用它，不需要知道 HTTP、JSON、密钥这些细节。
 *
 * 【为什么网络请求要在这里用 withContext(Dispatchers.IO)】
 *   Android 规定耗时操作不能在主线程做，否则界面卡死甚至抛
 *   NetworkOnMainThreadException。Dispatchers.IO 是协程提供的 IO 线程池，
 *   withContext 表示“切到 IO 线程执行括号里的代码，执行完再切回来”。
 *
 * 【suspend 是什么】
 *   suspend 函数（挂起函数）只能在协程里调用。它不会阻塞线程，
 *   而是“暂停”当前协程，等 IO 完成后再继续，这样主线程始终能响应点击。
 *
 * 【Repository（仓库）这个角色】
 *   它是“数据从哪来”的统一出口：界面层只管要数据，不管数据来自网络、
 *   数据库还是本地假数据。考试题目如果要求“分层”，就可以这样答。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：DeepSeekClient、PromptBuilder、WellnessRecord、AppLanguage。
 *   谁依赖本文件：AdviceViewModel。
 *   ★ 考试不做 AI 时删掉本文件（见 docs/exam-guide.md B 部分）。
 * ────────────────────────────────────────────────────────────────────────────
 */
class WellnessAdviceRepository(
    // API key 默认从 BuildConfig 读（编译期由 app/build.gradle 注入）。
    private val apiKey: String = BuildConfig.DEEPSEEK_API_KEY,
    // 真正发请求的客户端。
    private val client: DeepSeekClient = DeepSeekClient(apiKey)
) {

    /**
     * 获取一次健康建议。
     *
     * @param record   要分析的一天的记录。
     * @param language 回答语言（跟着界面语言走）。
     * @return AI 返回的建议文字。
     * @throws MissingApiKeyException 没有配置 key 时抛出。
     */
    suspend fun fetchAdvice(record: WellnessRecord, language: AppLanguage): String {
        // key 是空字符串就直接抛异常，没必要发一个注定失败的请求。
        if (apiKey.isBlank()) {
            throw MissingApiKeyException()
        }
        return withContext(Dispatchers.IO) {   // 切到 IO 线程跑网络请求
            client.requestAdvice(
                systemPrompt = PromptBuilder.systemPrompt(language),       // 系统提示词
                userPrompt = PromptBuilder.userPrompt(record, language)    // 用户提示词
            )
        }
    }
}
