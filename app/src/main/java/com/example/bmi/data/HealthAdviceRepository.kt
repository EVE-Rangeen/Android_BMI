package com.example.bmi.data

import com.example.bmi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * ============================================================================
 * HealthAdviceRepository.kt —— AI 健康建议的“统一入口”
 * ============================================================================
 * 【这个文件是干什么的】
 *   它把“调用 AI 拿建议”这件事包装成一个简单的挂起函数 fetchAdvice(...)。
 *   ViewModel 只调用它，不需要知道 HTTP、JSON、密钥这些细节。
 *
 * 【为什么网络请求要在这里用 withContext(Dispatchers.IO)】
 *   Android 规定：网络等耗时操作不能在主线程做，否则界面会卡死甚至崩溃。
 *   Dispatchers.IO 是协程提供的“IO 线程池”，专门用来跑网络、磁盘这类操作。
 *   withContext(...) 表示“切到 IO 线程执行括号里的代码，执行完再切回来”。
 *
 * 【谁在用这个文件】
 *   ReportViewModel 的 loadAdvice() 调用 fetchAdvice()。
 */
class HealthAdviceRepository(
    // API key 默认从 BuildConfig 读（编译期注入，见 app/build.gradle）。
    private val apiKey: String = BuildConfig.DEEPSEEK_API_KEY,
    // 真正发请求的客户端，默认用上面的 key 构造。
    private val client: DeepSeekClient = DeepSeekClient(apiKey)
) {

    /**
     * 获取一次健康建议。
     *
     * @param profile  用户的 BMI / 年龄 / 性别。
     * @param language 回答语言。
     * @return AI 返回的建议文字。
     * @throws MissingApiKeyException 没有配置 key 时抛出。
     */
    suspend fun fetchAdvice(profile: BmiProfile, language: AppLanguage): String {
        // 如果 key 是空字符串，直接抛异常，没必要发一个注定失败的请求。
        if (apiKey.isBlank()) {
            throw MissingApiKeyException()
        }
        return withContext(Dispatchers.IO) {   // 切到 IO 线程执行网络请求
            client.requestAdvice(
                systemPrompt = PromptBuilder.systemPrompt(language),       // 系统提示词
                userPrompt = PromptBuilder.userPrompt(profile, language)   // 用户提示词
            )
        }
    }
}
