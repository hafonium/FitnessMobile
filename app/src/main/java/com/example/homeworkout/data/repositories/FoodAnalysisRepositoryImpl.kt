package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.remote.SpoonacularFoodAnalysisDto
import com.example.homeworkout.data.remote.SpoonacularFoodApi
import com.example.homeworkout.data.remote.SpoonacularNutrientDto
import com.example.homeworkout.domain.models.FoodAnalysis
import com.example.homeworkout.domain.models.NutritionEstimate
import com.example.homeworkout.domain.repositories.FoodAnalysisRepository

class FoodAnalysisRepositoryImpl internal constructor(
    private val api: SpoonacularFoodApi
) : FoodAnalysisRepository {
    override suspend fun analyzeFoodImage(image: ByteArray): FoodAnalysis =
        api.analyzeFoodImage(image).toDomain()

    private fun SpoonacularFoodAnalysisDto.toDomain() = FoodAnalysis(
        category = category,
        categoryProbability = probability,
        calories = calories.toDomain(),
        fat = fat.toDomain(),
        protein = protein.toDomain(),
        carbohydrates = carbohydrates.toDomain(),
        recipesUsed = recipesUsed
    )

    private fun SpoonacularNutrientDto.toDomain() = NutritionEstimate(
        value = value,
        unit = unit,
        minimum = range.minimum,
        maximum = range.maximum
    )
}
