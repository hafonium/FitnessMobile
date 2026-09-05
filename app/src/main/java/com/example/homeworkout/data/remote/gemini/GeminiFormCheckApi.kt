package com.example.homeworkout.data.remote.gemini

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class GeminiFormObservationDto(
    val jointArea: String,
    val isCorrect: Boolean,
    val feedback: String
)

internal data class GeminiFormAnalysisDto(
    val exerciseName: String,
    val score: Int,
    val status: String,
    val observations: List<GeminiFormObservationDto>,
    val primaryCorrectionTip: String,
    val recordingTip: String
)

/** Thrown for any failure along the way (network, HTTP error, empty response, unparseable JSON),
 * carrying a short message that's safe to show in the UI - see the `Log.e` calls below for the
 * full exception/HTTP error body during development. */
internal class GeminiFormCheckException(message: String) : Exception(message)

/**
 * Direct-from-app REST client for Gemini's video-as-storyboard analysis: rather than uploading the
 * whole clip, the caller extracts a handful of evenly-spaced frames client-side
 * ([com.example.homeworkout.ui.core.formcheck.FormCheckScreen]'s `extractFrames`, using
 * `MediaMetadataRetriever`) and this class POSTs them as an ordered sequence of inline base64 JPEG
 * images alongside one text instruction, so Gemini reasons over the sequence like a movement
 * storyboard instead of decoding video itself.
 *
 * This calls the `generateContent` REST endpoint with plain OkHttp rather than the
 * `com.google.ai.client.generativeai` Kotlin SDK: that SDK targets whatever model aliases were
 * current when it was published and, in practice, has 404'd on this API surface every time Google
 * retired an alias (`gemini-1.5-flash`, then `gemini-2.0-flash`) faster than an SDK update landed.
 * A raw REST call only depends on the model name in the URL - bumping [MODEL_NAME] is the entire
 * fix next time, with no SDK version to chase and no gRPC/serialization error surface at all.
 *
 * No backend yet - same caveat as [com.example.homeworkout.data.remote.SpoonacularFoodApi] and
 * [com.example.homeworkout.data.remote.groq.GroqClient]; see docs/form-check-feature.md.
 */
internal class GeminiFormCheckApi(
    private val apiKey: String,
    private val modelName: String = MODEL_NAME
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Defensive: [modelName] is expected bare (e.g. "gemini-2.5-flash"), but the ListModels
    // endpoint below returns fully-qualified names ("models/gemini-2.5-flash"), and a future
    // caller could plausibly paste one of those straight into the constructor - strip it either way
    // rather than build a request against ".../models/models/gemini-2.5-flash:generateContent".
    private val normalizedModelName: String
        get() = modelName.removePrefix("models/")

    /** [frames] must already be in chronological order - each is base64-encoded and sent as one
     * inline JPEG image, in list order, followed by the text instruction, so Gemini can read them
     * as a single storyboard. */
    suspend fun analyzeFrames(frames: List<ByteArray>, exerciseHint: String?): GeminiFormAnalysisDto =
        withContext(Dispatchers.IO) {
            check(apiKey.isNotBlank()) {
                "Gemini API key is missing. Add GEMINI_API_KEY to local.properties."
            }
            require(frames.isNotEmpty()) { "No frames were extracted from this video." }

            val request = Request.Builder()
                .url("$ENDPOINT_BASE/$normalizedModelName:generateContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(buildRequestBody(frames, exerciseHint).toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val bodyString = try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        // Always the full raw body, never swallowed and never replaced by a single
                        // hardcoded message here - httpErrorMessage below only shapes what the UI
                        // shows, this line is the unfiltered diagnostic record.
                        Log.e(TAG, "Status: ${response.code}, Body: $responseBody")
                        throw GeminiFormCheckException(httpErrorMessage(response.code, responseBody))
                    }
                    responseBody
                }
            } catch (e: GeminiFormCheckException) {
                throw e
            } catch (e: IOException) {
                // Connectivity failure (no network, DNS, timeout, TLS) - never reaches the server,
                // so there's no HTTP status/body to log; the exception itself is the whole story.
                Log.e(TAG, "Gemini request failed (network)", e)
                throw GeminiFormCheckException("Could not reach Gemini. Check your internet connection and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Gemini request failed", e)
                throw GeminiFormCheckException("Gemini request failed. Please try again.")
            }

            val responseText = extractResponseText(bodyString)
            if (responseText.isBlank()) {
                Log.e(TAG, "Gemini returned no usable text: $bodyString")
                throw GeminiFormCheckException("Gemini returned an empty response. Please try again.")
            }
            parseResponse(responseText)
        }

    /**
     * Debug diagnostic only - never called from the analysis flow itself. GETs the `ListModels`
     * endpoint and logs every model name this API key can currently call (stripped of its
     * "models/" prefix), plus whether each supports `generateContent`, so a developer can read
     * Logcat to see exactly which "gemini-*-flash" alias is actually live for this key instead of
     * guessing from Google's shifting deprecation notices. Wired to run once, debug-build-only,
     * from [com.example.homeworkout.ui.App]'s `formCheckRepository` initializer.
     */
    suspend fun logAvailableModels() = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "Skipping ListModels check - GEMINI_API_KEY is missing.")
            return@withContext
        }

        val request = Request.Builder()
            .url("$ENDPOINT_BASE?key=$apiKey")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "ListModels failed - Status: ${response.code}, Body: $responseBody")
                    return@withContext
                }

                val models = JSONObject(responseBody).optJSONArray("models")
                if (models == null || models.length() == 0) {
                    Log.w(TAG, "ListModels returned no models for this API key: $responseBody")
                    return@withContext
                }

                Log.i(TAG, "Gemini models available for this API key (${models.length()}):")
                for (i in 0 until models.length()) {
                    val model = models.optJSONObject(i) ?: continue
                    val name = model.optString("name").removePrefix("models/")
                    val methods = model.optJSONArray("supportedGenerationMethods")
                    val supportsGenerateContent = methods != null &&
                        (0 until methods.length()).any { methods.optString(it) == "generateContent" }
                    Log.i(TAG, "  - $name (generateContent: $supportsGenerateContent)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ListModels request failed", e)
        }
    }

    private fun buildRequestBody(frames: List<ByteArray>, exerciseHint: String?): JSONObject {
        val promptText = if (exerciseHint.isNullOrBlank()) {
            "These ${frames.size} frames are ordered chronologically from one exercise repetition. " +
                "Identify the exercise being performed and evaluate the cleanest complete repetition visible across them."
        } else {
            "These ${frames.size} frames are ordered chronologically from one repetition the user tagged as " +
                "\"$exerciseHint\". Confirm or correct the exercise name yourself based on what you actually observe."
        }

        val parts = JSONArray().apply {
            frames.forEach { frame ->
                put(
                    JSONObject().put(
                        "inline_data",
                        JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", Base64.encodeToString(frame, Base64.NO_WRAP))
                        }
                    )
                )
            }
            put(JSONObject().put("text", promptText))
        }

        return JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION)))
            )
            put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            put(
                "generationConfig",
                JSONObject().apply {
                    // 0.0 = greedy decoding (always pick the highest-probability token) rather
                    // than sampling - the same frames + prompt should score the same way every
                    // time, not vary run to run.
                    put("temperature", 0.0)
                    put("responseMimeType", "application/json")
                }
            )
        }
    }

    /** Pulls `candidates[0].content.parts[*].text` (joined - Gemini occasionally splits one reply
     * across multiple text parts) out of a raw `generateContent` response body. Returns an empty
     * string for any unexpected shape rather than throwing - the caller treats that the same as
     * an empty response, one of the already-handled failure states. */
    private fun extractResponseText(bodyString: String): String {
        return try {
            val root = JSONObject(bodyString)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) return ""
            val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                ?: return ""
            buildString {
                for (i in 0 until parts.length()) {
                    append(parts.optJSONObject(i)?.optString("text").orEmpty())
                }
            }.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Could not read Gemini response body: $bodyString", e)
            ""
        }
    }

    /** Maps an HTTP failure to a short, actionable, UI-safe message - the caller has already
     * logged the status code and full error body via [Log.e], so nothing diagnostic is lost. */
    private fun httpErrorMessage(code: Int, errorBody: String): String = when (code) {
        404 -> "This AI model is no longer available. Please update the app and try again."
        401, 403 -> "Gemini rejected the request's API key. Check GEMINI_API_KEY in local.properties."
        429 -> "Gemini's usage limit was reached. Please try again later."
        in 500..599 -> "Gemini is temporarily unavailable. Please try again shortly."
        else -> errorBody.take(MAX_ERROR_MESSAGE_LENGTH).ifBlank { "Gemini request failed (HTTP $code)." }
    }

    /**
     * Deliberately lenient: the model is instructed to always attempt a best-effort analysis
     * (see [SYSTEM_INSTRUCTION]) rather than refuse a low-visibility sequence, so a missing/odd
     * field here degrades to a sensible default instead of throwing - a thrown
     * [GeminiFormCheckException] is reserved for a response that isn't JSON at all.
     */
    private fun parseResponse(raw: String): GeminiFormAnalysisDto {
        val cleaned = sanitizeJson(raw)
        val root = try {
            JSONObject(cleaned)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini response was not valid JSON: $cleaned", e)
            throw GeminiFormCheckException("Gemini returned a response that could not be parsed as JSON.")
        }

        val observationsArray = root.optJSONArray("observations")
        val observations = observationsArray?.let { array ->
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                GeminiFormObservationDto(
                    jointArea = obj.optString("jointArea").ifBlank { "Overall form" },
                    isCorrect = obj.optBoolean("isCorrect", true),
                    feedback = obj.optString("feedback").ifBlank {
                        "Unable to fully assess due to angle/lighting."
                    }
                )
            }
        }.orEmpty()

        return GeminiFormAnalysisDto(
            exerciseName = root.optString("exerciseName").ifBlank { "Exercise (best guess)" },
            score = root.optInt("score", 50).coerceIn(0, 100),
            status = root.optString("status", "ACCEPTABLE"),
            observations = observations,
            // "correctionTip" is a defensive fallback for the pre-rename schema key.
            primaryCorrectionTip = root.optString("primaryCorrectionTip").ifBlank { root.optString("correctionTip") },
            recordingTip = root.optString("recordingTip")
        )
    }

    /** Strips markdown code fences a model occasionally wraps JSON output in despite the system
     * instruction forbidding it, and trims stray whitespace, before any JSON decoding is attempted. */
    private fun sanitizeJson(raw: String): String =
        raw.removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()

    companion object {
        private const val TAG = "GeminiFormCheckApi"
        private const val MAX_ERROR_MESSAGE_LENGTH = 180

        // "gemini-2.5-flash" 404s for this key ("no longer available to new users") despite still
        // being listed by ListModels - verified against this project's own key via
        // GeminiFormCheckApi.logAvailableModels() on 2026-09-05, which confirmed "gemini-3.6-flash"
        // is both listed and generateContent-capable. The REST endpoint only ever depends on this
        // one string - if "gemini-3.6-flash" is retired in turn, re-run logAvailableModels() and
        // bump this constant to whatever it reports, rather than guessing from an error message
        // alone (see docs/form-check-feature.md).
        private const val MODEL_NAME = "gemini-3.6-flash"
        private const val ENDPOINT_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val SYSTEM_INSTRUCTION = """
            You are a strict, objective biomechanics evaluator and calisthenics coach.
            The user provides several still frames extracted from a short exercise video, in
            chronological order (earliest first). Treat them as a movement storyboard, not
            independent photos - use the sequence to infer motion, tempo, and joint trajectory
            across frames.
            Disregard frames that only show setup, repositioning, or idle standing. Isolate the
            frames that capture the active repetition and evaluate:
            1. Spinal neutrality and core bracing across the sequence.
            2. Visible joint angles and trajectory frame-to-frame.
            3. Depth and range of motion implied by the sequence.

            Even if the lighting is poor, the camera angle is sub-optimal, or the full body is not
            completely visible in every frame, DO NOT refuse the request. Perform a best-effort
            analysis based on whatever movement or posture is visible. Mark anything you genuinely
            cannot assess with "Unable to fully assess due to angle/lighting" rather than declining
            to answer.

            Calculate "score" deterministically with this exact deductive rubric - do not estimate
            or eyeball a number, compute it:
            - Start from a baseline of 100 points.
            - Major fault (e.g. severe lumbar hyperextension/sagging, dangerous elbow flare beyond
              70 degrees, incomplete depth below 50% range of motion): deduct 20 points per fault.
            - Minor fault (e.g. slight head/neck misalignment, minor hip shift, a slight pause at
              lockout): deduct 10 points per fault.
            - If no faults are clearly observed in the visible frames, default to 95 (reserve 100
              only for a rare, unambiguously flawless repetition).
            - "score" is 100 minus the sum of every deduction, floored at 0.
            - Every deduction MUST be explained in one of the "observations" entries' "feedback" -
              never deduct points silently.
            Map the final "score" to "status" with these fixed thresholds so the two fields never
            disagree: 90-100 -> EXCELLENT, 70-89 -> ACCEPTABLE, below 70 -> NEEDS_IMPROVEMENT.

            Output MUST strictly follow the JSON schema below. All feedback must be in English.
            Output JSON Schema:
            {
              "exerciseName": "Standard Push-up (or Best Guess)",
              "score": 80,
              "status": "ACCEPTABLE",
              "observations": [
                {
                  "jointArea": "Elbows & Arms",
                  "isCorrect": true,
                  "feedback": "Elbow flare is within an acceptable 45-degree range. No deduction."
                },
                {
                  "jointArea": "Hips & Lower Back",
                  "isCorrect": false,
                  "feedback": "Minor hip shift observed at the bottom of the rep. Minor fault: -10 points, plus a second minor fault for a brief lockout pause: -10 points."
                }
              ],
              "primaryCorrectionTip": "Keep your core braced to avoid pelvic sag.",
              "recordingTip": "For best accuracy next time: ensure full-body visibility and adequate front-facing light."
            }
            "status" MUST be exactly one of: EXCELLENT, ACCEPTABLE, NEEDS_IMPROVEMENT, and MUST
            match the score-to-status mapping above. Do not wrap output in markdown codeblocks.
        """.trimIndent()
    }
}
