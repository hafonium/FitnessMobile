package com.example.homeworkout.data.remote.groq

import com.example.homeworkout.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Result of one turn with the assistant: the reply to show the user plus the updated rolling
 * context summary to persist on the session — see docs/chatbot-feature.md for the design rationale. */
data class GroqChatResult(
    val reply: String,
    val updatedContext: String
)

class GroqApiException(message: String) : Exception(message)

/**
 * Minimal direct-from-app client for Groq's OpenAI-compatible chat completions API. This app has
 * no backend, so it calls Groq itself using a key baked in at build time from local.properties
 * (never committed — see docs/chatbot-feature.md). This was originally built against Gemini;
 * switched to Groq after Gemini's Google Cloud project got hit with an unresolvable "project
 * denied access" 403 (see docs/chatbot-feature.md for the full story) — so treat this class, not
 * the historical git log, as the source of truth for which provider is actually wired up. Requests
 * structured JSON output so a single call returns both the reply and the updated context summary
 * (avoids replaying full message history on every turn).
 */
class GroqClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(contextSummary: String, userMessage: String): GroqChatResult =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                .post(buildRequestBody(contextSummary, userMessage).toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw GroqApiException("Groq request failed: HTTP ${response.code} $bodyString")
                }
                parseResponse(bodyString)
            }
        }

    private fun buildRequestBody(contextSummary: String, userMessage: String): JSONObject {
        val promptText = buildString {
            append("PRIOR CONTEXT:\n")
            append(contextSummary.ifBlank { "(none - this is the first message in this conversation)" })
            append("\n\nNEW USER MESSAGE:\n")
            append(userMessage)
        }

        val schema = JSONObject().apply {
            put("type", "object")
            put(
                "properties",
                JSONObject().apply {
                    put("reply", JSONObject().put("type", "string"))
                    put("updatedContext", JSONObject().put("type", "string"))
                }
            )
            put("required", JSONArray(listOf("reply", "updatedContext")))
            put("additionalProperties", false)
        }

        val jsonSchema = JSONObject().apply {
            put("name", "chat_reply")
            put("strict", true)
            put("schema", schema)
        }

        return JSONObject().apply {
            put("model", MODEL)
            put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_INSTRUCTION))
                    .put(JSONObject().put("role", "user").put("content", promptText))
            )
            put(
                "response_format",
                JSONObject().apply {
                    put("type", "json_schema")
                    put("json_schema", jsonSchema)
                }
            )
        }
    }

    private fun parseResponse(bodyString: String): GroqChatResult {
        val root = JSONObject(bodyString)
        val choices = root.optJSONArray("choices")
            ?: throw GroqApiException("Groq response had no choices: $bodyString")
        val content = choices.getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        val parsed = JSONObject(content)
        return GroqChatResult(
            reply = parsed.getString("reply"),
            updatedContext = parsed.getString("updatedContext")
        )
    }

    companion object {
        // Groq's OpenAI-compatible chat completions endpoint - see
        // https://console.groq.com/docs/api-reference#chat-create.
        private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"

        // openai/gpt-oss-20b: one of the few Groq models that currently supports strict JSON
        // Schema structured outputs (most Groq models, including llama-3.3-70b-versatile, only
        // support the older, unenforced "json_object" mode) - see
        // https://console.groq.com/docs/structured-outputs. If this model is ever retired, check
        // https://console.groq.com/docs/models for a current replacement that still lists
        // Structured Outputs support, the same way gemini-flash-latest quietly breaking is what
        // forced the earlier switch away from Gemini entirely.
        private const val MODEL = "openai/gpt-oss-20b"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val SYSTEM_INSTRUCTION = """
            You are the in-app fitness coach assistant for HomeWorkout, an Android workout-tracking app.
            Answer questions about exercises, workout plans, training technique, recovery, and general
            fitness/nutrition basics. Be concise, practical and encouraging - this is a small mobile chat
            widget, so prefer short paragraphs or a few bullet points over long essays.
            If the user asks something unrelated to fitness, health or the app, briefly redirect them back
            to fitness topics rather than answering it at length.
            Always reply in the same language the user's latest message is written in.
            You are given PRIOR CONTEXT (a running summary of the conversation so far, or a note that this
            is the first message) and a NEW USER MESSAGE. Respond ONLY with a JSON object matching the
            required schema: "reply" is the message to show the user; "updatedContext" is a concise,
            updated summary of the conversation so far (prior context plus this exchange) to use as context
            on the next turn - a few sentences at most, never a full transcript.
        """.trimIndent()
    }
}
