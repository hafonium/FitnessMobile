package com.example.homeworkout.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

internal data class SpoonacularRangeDto(val minimum: Double, val maximum: Double)

internal data class SpoonacularNutrientDto(
    val value: Double,
    val unit: String,
    val range: SpoonacularRangeDto
)

internal data class SpoonacularFoodAnalysisDto(
    val category: String,
    val probability: Double,
    val calories: SpoonacularNutrientDto,
    val fat: SpoonacularNutrientDto,
    val protein: SpoonacularNutrientDto,
    val carbohydrates: SpoonacularNutrientDto,
    val recipesUsed: Int
)

internal class SpoonacularApiException(
    val statusCode: Int,
    message: String
) : Exception(message)

internal class SpoonacularFoodApi(
    private val apiKey: String,
    private val endpoint: String = "https://api.spoonacular.com/food/images/analyze"
) {
    suspend fun analyzeFoodImage(image: ByteArray): SpoonacularFoodAnalysisDto =
        withContext(Dispatchers.IO) {
            check(apiKey.isNotBlank()) {
                "Spoonacular API key is missing. Add SPOONACULAR_API_KEY to local.properties."
            }

            val boundary = "HomeWorkout-${UUID.randomUUID()}"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-api-key", apiKey)
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            try {
                DataOutputStream(connection.outputStream).use { output ->
                    output.writeBytes("--$boundary\r\n")
                    output.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"meal.jpg\"\r\n")
                    output.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                    output.write(image)
                    output.writeBytes("\r\n--$boundary--\r\n")
                }

                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (statusCode !in 200..299) {
                    val apiMessage = runCatching { JSONObject(response).optString("message") }.getOrNull()
                    throw SpoonacularApiException(
                        statusCode,
                        apiMessage?.takeIf { it.isNotBlank() } ?: "Spoonacular request failed ($statusCode)."
                    )
                }
                parseResponse(response)
            } finally {
                connection.disconnect()
            }
        }

    private fun parseResponse(response: String): SpoonacularFoodAnalysisDto {
        val root = JSONObject(response)
        val nutrition = root.getJSONObject("nutrition")
        val category = root.getJSONObject("category")
        return SpoonacularFoodAnalysisDto(
            category = category.getString("name"),
            probability = category.getDouble("probability"),
            calories = nutrition.getNutrient("calories"),
            fat = nutrition.getNutrient("fat"),
            protein = nutrition.getNutrient("protein"),
            carbohydrates = nutrition.getNutrient("carbs"),
            recipesUsed = nutrition.optInt("recipesUsed")
        )
    }

    private fun JSONObject.getNutrient(name: String): SpoonacularNutrientDto {
        val nutrient = getJSONObject(name)
        val range = nutrient.getJSONObject("confidenceRange95Percent")
        return SpoonacularNutrientDto(
            value = nutrient.getDouble("value"),
            unit = nutrient.getString("unit"),
            range = SpoonacularRangeDto(
                minimum = range.getDouble("min"),
                maximum = range.getDouble("max")
            )
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
