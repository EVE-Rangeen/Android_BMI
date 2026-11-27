package com.example.bmi.data

import com.example.bmi.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/*
 * ============================================================================
 * DeepSeekClient.kt —— 调用 DeepSeek 接口的 HTTP 客户端（【AI 功能 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   用最基础的 HttpURLConnection 向 DeepSeek 的 /chat/completions 接口发一个
 *   POST 请求：把提示词发过去，再把返回 JSON 里的回答文字抠出来。
 *
 * 【为什么不引入 OkHttp / Retrofit】
 *   整个 App 只需要发一种请求，用系统自带的 HttpURLConnection + org.json 就够：
 *   不加第三方依赖，考试环境联网拉依赖失败也不会影响编译。
 *
 * 【重要：网络请求必须在子线程】
 *   requestAdvice() 是“阻塞”调用（发请求期间当前线程会卡住等结果）。
 *   放在主线程（UI 线程）会抛 NetworkOnMainThreadException 并卡住界面，
 *   所以它是在 WellnessAdviceRepository 里用 withContext(Dispatchers.IO) 调到子线程的。
 *
 * 【这个接口长什么样】
 *   请求：POST https://api.deepseek.com/chat/completions
 *         Header: Authorization: Bearer <你的 key>
 *         Body:   { "model": "...", "messages": [ {"role":"system",...}, {"role":"user",...} ] }
 *   返回：{ "choices": [ { "message": { "content": "回答正文" } } ] }
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：BuildConfig.DEEPSEEK_API_KEY / DEEPSEEK_BASE_URL（在 app/build.gradle 注入）。
 *   谁依赖本文件：WellnessAdviceRepository。
 *   ★ 考试不做 AI 时删掉本文件（见 docs/exam-guide.md B 部分）。
 * ────────────────────────────────────────────────────────────────────────────
 */

/** API key 没配置时抛出的异常（BuildConfig 里是空字符串）。 */
class MissingApiKeyException : IllegalStateException("DeepSeek API key is not configured")

/** 远端接口返回了非 2xx 状态码时抛出的异常（带着 HTTP 状态码和返回内容）。 */
class LlmException(val statusCode: Int, val responseBody: String) :
    IOException("DeepSeek API error, HTTP $statusCode")

/**
 * DeepSeek 的 HTTP 客户端。
 *
 * @param apiKey  密钥，默认取 BuildConfig.DEEPSEEK_API_KEY（编译期从 local.properties 注入）。
 * @param baseUrl 接口地址，默认取 BuildConfig.DEEPSEEK_BASE_URL。
 */
class DeepSeekClient(
    private val apiKey: String,
    private val baseUrl: String = BuildConfig.DEEPSEEK_BASE_URL
) {

    /**
     * 发送一次对话请求，返回 AI 的回答文字。
     * 这是阻塞调用，必须在子线程执行（见文件头部说明）。
     */
    fun requestAdvice(systemPrompt: String, userPrompt: String): String {
        val connection = openConnection()   // 建立连接并设置请求头
        try {
            // 把请求体（JSON 字符串）写入连接的输出流 —— 这一步才真正把数据发出去。
            connection.outputStream.use { out ->
                out.write(buildPayload(systemPrompt, userPrompt).toByteArray(Charsets.UTF_8))
            }
            val statusCode = connection.responseCode      // 读 HTTP 状态码（200 表示成功）
            val responseBody = readBody(connection, statusCode)
            if (statusCode !in 200..299) {
                // 非 2xx（401 没权限、429 太频繁、500 服务端错…）都视为失败。
                throw LlmException(statusCode, responseBody)
            }
            return parseReply(responseBody)               // 从返回 JSON 里抠出回答文字
        } finally {
            connection.disconnect()   // 无论成功失败都断开连接，释放资源
        }
    }

    /** 配置并打开一个 HttpURLConnection（阻塞连接，调用方负责切线程）。 */
    private fun openConnection(): HttpURLConnection {
        // baseUrl 末尾如果有 "/"，先去掉再拼 "/chat/completions"，避免出现双斜杠。
        val url = URL("${baseUrl.trimEnd('/')}/chat/completions")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"                       // 用 POST 提交数据
            connectTimeout = CONNECT_TIMEOUT_MS          // 建立连接的最长等待时间
            readTimeout = READ_TIMEOUT_MS                // 等待返回内容的最长时间
            doOutput = true                              // 允许向连接写入请求体
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")  // 用 key 鉴权
        }
    }

    /**
     * 拼出 DeepSeek 需要的请求体（JSON 字符串）。
     *
     * messages 数组里两条消息：
     *   role = system → 规定 AI 的身份和回答规则；
     *   role = user   → 具体的用户数据。
     */
    private fun buildPayload(systemPrompt: String, userPrompt: String): String {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            put(JSONObject().put("role", "user").put("content", userPrompt))
        }
        return JSONObject().apply {
            put("model", MODEL)              // 模型名
            put("temperature", TEMPERATURE)  // 随机性：0.7 比较自然
            put("max_tokens", MAX_TOKENS)    // 最多生成多少 token
            put("stream", false)             // 不流式，一次性返回
            put("messages", messages)
        }.toString()
    }

    /** 读返回内容：成功读 inputStream，失败读 errorStream（错误信息在后者里）。 */
    private fun readBody(connection: HttpURLConnection, statusCode: Int): String {
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        // use { } 会读完自动关闭流。
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    /**
     * 从返回 JSON 里取出回答正文：
     *   { "choices": [ { "message": { "content": "..." } } ] }
     */
    private fun parseReply(responseBody: String): String {
        val content = JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .optString("content")
            .trim()
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
