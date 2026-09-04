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

/** Raw plan-creation fields exactly as the model returned them, before validation/defaulting into
 * a real [com.example.homeworkout.domain.models.FitnessProfile] — see
 * [com.example.homeworkout.data.repositories.ChatRepositoryImpl] for that mapping. Only meaningful
 * when [GroqChatResult.planProposal] is non-null (the model set "wantsToCreatePlan" to true). */
data class GroqPlanProposal(
    val title: String,
    val description: String,
    val primaryGoal: String,
    val experienceLevel: String,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val equipment: List<String>,
    val focusCategories: List<String>,
    val focusMuscles: List<String>
)

/** Result of one turn with the assistant: the reply to show the user, the updated rolling context
 * summary to persist on the session, and — only when the user asked to create a plan and enough
 * was known — a [GroqPlanProposal] to seed the Create Workout screen. See docs/chatbot-feature.md
 * for the design rationale. */
data class GroqChatResult(
    val reply: String,
    val updatedContext: String,
    val planProposal: GroqPlanProposal? = null
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

    /** [appContext] is a snapshot of the app's data model plus this user's saved fitness profile
     *  and existing plans — see [com.example.homeworkout.data.repositories.ChatRepositoryImpl] —
     *  so the assistant can converse about, and propose, real workout plans. */
    suspend fun sendMessage(contextSummary: String, userMessage: String, appContext: String): GroqChatResult =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                .post(buildRequestBody(contextSummary, userMessage, appContext).toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw GroqApiException("Groq request failed: HTTP ${response.code} $bodyString")
                }
                parseResponse(bodyString)
            }
        }

    private fun buildRequestBody(contextSummary: String, userMessage: String, appContext: String): JSONObject {
        val promptText = buildString {
            append("APP CONTEXT:\n")
            append(appContext)
            append("\n\nPRIOR CONTEXT:\n")
            append(contextSummary.ifBlank { "(none - this is the first message in this conversation)" })
            append("\n\nNEW USER MESSAGE:\n")
            append(userMessage)
        }

        val planProposalSchema = JSONObject().apply {
            put("type", "object")
            put(
                "properties",
                JSONObject().apply {
                    put("title", JSONObject().put("type", "string"))
                    put("description", JSONObject().put("type", "string"))
                    put(
                        "primaryGoal",
                        JSONObject().apply {
                            put("type", "string")
                            put(
                                "enum",
                                JSONArray(listOf("general_fitness", "build_muscle", "fat_loss", "mobility", "focus_area"))
                            )
                        }
                    )
                    put(
                        "experienceLevel",
                        JSONObject().apply {
                            put("type", "string")
                            put("enum", JSONArray(listOf("beginner", "intermediate", "expert")))
                        }
                    )
                    put("daysPerWeek", JSONObject().put("type", "integer"))
                    put("sessionMinutes", JSONObject().put("type", "integer"))
                    put(
                        "equipment",
                        JSONObject().apply {
                            put("type", "array")
                            put("items", JSONObject().put("type", "string"))
                        }
                    )
                    put(
                        "focusCategories",
                        JSONObject().apply {
                            put("type", "array")
                            put("items", JSONObject().put("type", "string"))
                        }
                    )
                    put(
                        "focusMuscles",
                        JSONObject().apply {
                            put("type", "array")
                            put("items", JSONObject().put("type", "string"))
                        }
                    )
                }
            )
            put(
                "required",
                JSONArray(
                    listOf(
                        "title", "description", "primaryGoal", "experienceLevel",
                        "daysPerWeek", "sessionMinutes", "equipment", "focusCategories", "focusMuscles"
                    )
                )
            )
            put("additionalProperties", false)
        }

        val schema = JSONObject().apply {
            put("type", "object")
            put(
                "properties",
                JSONObject().apply {
                    put("reply", JSONObject().put("type", "string"))
                    put("updatedContext", JSONObject().put("type", "string"))
                    put("wantsToCreatePlan", JSONObject().put("type", "boolean"))
                    put("planProposal", planProposalSchema)
                }
            )
            put("required", JSONArray(listOf("reply", "updatedContext", "wantsToCreatePlan", "planProposal")))
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
        val wantsToCreatePlan = parsed.optBoolean("wantsToCreatePlan", false)
        val planProposal = if (wantsToCreatePlan) {
            parsed.optJSONObject("planProposal")?.let { p ->
                GroqPlanProposal(
                    title = p.optString("title"),
                    description = p.optString("description"),
                    primaryGoal = p.optString("primaryGoal"),
                    experienceLevel = p.optString("experienceLevel"),
                    daysPerWeek = p.optInt("daysPerWeek", 3),
                    sessionMinutes = p.optInt("sessionMinutes", 30),
                    equipment = p.optJSONArray("equipment").toStringList(),
                    focusCategories = p.optJSONArray("focusCategories").toStringList(),
                    focusMuscles = p.optJSONArray("focusMuscles").toStringList()
                )
            }
        } else {
            null
        }

        return GroqChatResult(
            reply = normalizeInlineBullets(parsed.getString("reply")),
            updatedContext = parsed.getString("updatedContext"),
            planProposal = planProposal
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }

    // The system prompt asks for real Markdown "- " list syntax, but small/fast models under
    // strict JSON-schema output don't reliably comply - in practice this model still falls back to
    // a literal "•" character run inline into one paragraph instead of a newline-separated
    // list. CommonMark (and this app's Markdown renderer) only ever recognizes "-"/"*"/"+" at the
    // start of a line as a list marker, so an inline bullet character can never render as a list no
    // matter how the renderer is configured. Rather than depend on the model's compliance,
    // normalize this deterministically: turn every inline bullet into a real line-starting "- " item.
    private fun normalizeInlineBullets(text: String): String =
        text.replace(Regex("""\s*•\s*"""), "\n- ").trim()

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
            "reply" is rendered as Markdown, so use real Markdown syntax, not visual approximations: for
            a list, put each item on its own line starting with "- " (hyphen, space) - never inline bullet
            characters like "*" strung together in one paragraph, since only a line-starting "-" actually
            renders as a list. Use "**bold**" for emphasis. Keep formatting light - a short paragraph or a
            handful of "- " list lines, not headings or nested structure.
            If the user asks something unrelated to fitness, health or the app, briefly redirect them back
            to fitness topics rather than answering it at length.
            Always reply in the same language the user's latest message is written in.
            You are given APP CONTEXT (the app's data model plus this user's saved fitness profile and
            existing plans, if any), PRIOR CONTEXT (a running summary of the conversation so far, or a
            note that this is the first message) and a NEW USER MESSAGE. Use APP CONTEXT to have a more
            informed conversation and, when asked, to help set up a workout plan.
            If - and only if - the user has clearly asked you to create, build or generate a workout plan
            for them, AND you have a reasonable goal, experience level, days per week and session length
            to work with (use their saved profile from APP CONTEXT when it already covers something they
            haven't restated), set "wantsToCreatePlan" to true and fill "planProposal" with your best
            values: a short "title", a one-sentence "description", "primaryGoal" and "experienceLevel"
            using exactly one of the listed enum values, "daysPerWeek" (2-6), "sessionMinutes" (one of
            15/20/30/45/60), "equipment" (from the app's equipment list; may be empty since bodyweight is
            always available), and "focusCategories"/"focusMuscles" (may be empty). The app will open a
            plan-builder screen prefilled from these values for the user to review, so when
            "wantsToCreatePlan" is true, keep "reply" a short confirmation (e.g. "Let's set that up -
            opening the plan builder for you.") rather than describing the plan in detail.
            If the user hasn't clearly asked for a plan, or you're missing goal/level/days/session length
            and the saved profile doesn't cover them, set "wantsToCreatePlan" to false, use "reply" to
            continue the conversation or ask what's missing, and still fill every "planProposal" field
            with a reasonable placeholder value (the app ignores "planProposal" whenever
            "wantsToCreatePlan" is false, but the field itself is always required).
            Never propose a plan if the user mentions an injury, acute pain, or a medical restriction -
            instead suggest they get appropriate clearance first, the same as for any other fitness
            question involving a possible injury.
            Respond ONLY with a JSON object matching the required schema; "updatedContext" is a concise,
            updated summary of the conversation so far (prior context plus this exchange) to use as
            context on the next turn - a few sentences at most, never a full transcript.
        """.trimIndent()
    }
}
