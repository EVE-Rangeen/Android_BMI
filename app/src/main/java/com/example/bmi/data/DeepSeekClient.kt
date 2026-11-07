package com.example.bmi.data

import com.example.bmi.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** The API key was not configured in local.properties when the app was built. */
class MissingApiKeyException : IllegalStateException("DeepSeek API key is not configured")

/** The remote API answered with a non-2xx status code. */
class LlmException(val statusCode: Int, val responseBody: String) :
    IOException("DeepSeek API error, HTTP $statusCode")

/**
 * Thin HTTP client for the DeepSeek chat-completions endpoint.
 *
 * Only one request is needed by the app, so a full networking library is not
 * worth the extra dependency; [HttpURLConnection] + org.json are enough.
 * This call is blocking and must run on a background thread.
 */
class DeepSeekClient(
    private val apiKey: String,
    private val baseUrl: String = BuildConfig.DEEPSEEK_BASE_URL
) {

    fun requestAdvice(systemPrompt: String, userPrompt: String): String {
        val connection = openConnection()
        try {
            connection.outputStream.use { out ->
                out.write(buildPayload(systemPrompt, userPrompt).toByteArray(Charsets.UTF_8))
            }
            val statusCode = connection.responseCode
            val responseBody = readBody(connection, statusCode)
            if (statusCode !in 200..299) {
                throw LlmException(statusCode, responseBody)
            }
            return parseReply(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(): HttpURLConnection {
        val url = URL("${baseUrl.trimEnd('/')}/chat/completions")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
    }

    private fun buildPayload(systemPrompt: String, userPrompt: String): String {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            put(JSONObject().put("role", "user").put("content", userPrompt))
        }
        return JSONObject().apply {
            put("model", MODEL)
            put("temperature", TEMPERATURE)
            put("max_tokens", MAX_TOKENS)
            put("stream", false)
            put("messages", messages)
        }.toString()
    }

    private fun readBody(connection: HttpURLConnection, statusCode: Int): String {
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

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
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 90_000
    }
}
