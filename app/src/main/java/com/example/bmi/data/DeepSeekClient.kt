package com.example.bmi.data

import com.example.bmi.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/*
 * ============================================================================
 * DeepSeekClient.kt —— 真正去调用 DeepSeek 接口的 HTTP 客户端
 * ============================================================================
 * 【这个文件是干什么的】
 *   用最基础的 HttpURLConnection 向 DeepSeek 的 /chat/completions 接口发一个
 *   POST 请求，把提示词发过去，再把返回的 JSON 里的回答文字抠出来。
 *
 * 【为什么不引入 OkHttp/Retrofit】
 *   整个 App 只需要发一种请求，用系统自带的 HttpURLConnection + org.json 就够，
 *   不引入第三方库可以减少依赖、避免考试环境联网拉依赖出问题。
 *
 * 【重要：网络请求必须在子线程】
 *   requestAdvice() 是“阻塞”调用（发请求期间当前线程会卡住等结果）。
 *   如果放在主线程（UI 线程）会触发 NetworkOnMainThreadException 并卡界面。
 *   所以它一定是在 HealthAdviceRepository 里通过 withContext(Dispatchers.IO)
 *   切到子线程去执行的（见 HealthAdviceRepository.kt）。
 */

/** API key 没配置时抛出的异常（BuildConfig 里是空字符串）。 */
class MissingApiKeyException : IllegalStateException("DeepSeek API key is not configured")

/** 远端接口返回了非 2xx 状态码时抛出的异常（带着 HTTP 状态码和返回内容）。 */
class LlmException(val statusCode: Int, val responseBody: String) :
    IOException("DeepSeek API error, HTTP $statusCode")

/**
 * DeepSeek 的 HTTP 客户端。
 *
 * @param apiKey  密钥，默认取自 BuildConfig.DEEPSEEK_API_KEY（编译期注入）。
 * @param baseUrl 接口地址，默认取自 BuildConfig.DEEPSEEK_BASE_URL。
 */
class DeepSeekClient(
    private val apiKey: String,
    private val baseUrl: String = BuildConfig.DEEPSEEK_BASE_URL
) {

    /**
     * 发送一次对话请求，返回 AI 的回答文字（阻塞调用，必须在子线程）。
     */
    fun requestAdvice(systemPrompt: String, userPrompt: String): String {
        val connection = openConnection()   // 先建立连接
        try {
            // 把请求体（JSON 字符串）写入连接的输出流 —— 这就是“发出去的数据”。
            connection.outputStream.use { out ->
                out.write(buildPayload(systemPrompt, userPrompt).toByteArray(Charsets.UTF_8))
            }
            val statusCode = connection.responseCode   // 读取 HTTP 状态码（200 表示成功）
            val responseBody = readBody(connection, statusCode)  // 读取返回内容
            if (statusCode !in 200..299) {
                // 状态码不是 2xx（比如 401 没权限、500 服务器错），视为失败。
                throw LlmException(statusCode, responseBody)
            }
            return parseReply(responseBody)   // 从返回 JSON 里抠出回答文字
        } finally {
            connection.disconnect()   // 无论成功失败都关闭连接，释放资源
        }
    }

    /**
     * 配置并打开一个 HttpURLConnection。
     * 注意：这是“阻塞”连接，调用方必须自己在子线程执行。
     */
    private fun openConnection(): HttpURLConnection {
        // baseUrl 末尾如果有 "/"，去掉它再拼 "/chat/completions"，避免出现双斜杠。
        val url = URL("${baseUrl.trimEnd('/')}/chat/completions")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"                       // 用 POST 方式提交数据
            connectTimeout = CONNECT_TIMEOUT_MS          // 建立连接的最长等待时间
            readTimeout = READ_TIMEOUT_MS                // 读取返回的最长等待时间
            doOutput = true                              // 允许向连接写入请求体
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")  // 用 key 鉴权
        }
    }

    /**
     * 拼出 DeepSeek 需要的请求体（JSON 字符串）。
     * 结构大概是：
     *   { "model": "deepseek-chat", "messages": [ {role:"system"...}, {role:"user"...} ], ... }
     */
    private fun buildPayload(systemPrompt: String, userPrompt: String): String {
        // JSONArray 就是一个 JSON 数组；这里放两条消息。
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            put(JSONObject().put("role", "user").put("content", userPrompt))
        }
        return JSONObject().apply {
            put("model", MODEL)          // 模型名
            put("temperature", TEMPERATURE)  // 随机性：0.7 比较自然
            put("max_tokens", MAX_TOKENS)    // 最多生成多少 token
            put("stream", false)             // 不用流式，一次性返回
            put("messages", messages)
        }.toString()
    }

    /**
     * 读取返回内容：成功读 inputStream，失败读 errorStream。
     */
    private fun readBody(connection: HttpURLConnection, statusCode: Int): String {
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        // bufferedReader 逐块读取字符流；use { } 会在读完自动关闭。
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    /**
     * 从 DeepSeek 返回的 JSON 里取出真正的回答文字。
     * 返回结构形如：
     *   { "choices": [ { "message": { "content": "这里是回答..." } } ] }
     */
    private fun parseReply(responseBody: String): String {
        val content = JSONObject(responseBody)      // 把字符串解析成 JSON 对象
            .getJSONArray("choices")                // 取 choices 数组
            .getJSONObject(0)                       // 取第 0 个结果
            .getJSONObject("message")               // 取 message 对象
            .optString("content")                   // 取 content 字段（回答正文）
            .trim()                                 // 去掉首尾空白
        if (content.isEmpty()) {
            throw LlmException(-1, responseBody)
        }
        return content
    }

    private companion object {
        const val MODEL = "deepseek-chat"
        const val TEMPERATURE = 0.7
        const val MAX_TOKENS = 320
        const val CONNECT_TIMEOUT_MS = 15_000   // 连接超时 15 秒
        const val READ_TIMEOUT_MS = 90_000      // 读取超时 90 秒
    }
}
