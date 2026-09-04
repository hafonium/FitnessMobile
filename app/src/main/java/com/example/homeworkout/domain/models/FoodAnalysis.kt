package com.example.homeworkout.domain.models

data class NutritionEstimate(
    val value: Double,
    val unit: String,
    val minimum: Double,
    val maximum: Double
)

data class FoodAnalysis(
    val category: String,
    val categoryProbability: Double,
    val calories: NutritionEstimate,
    val fat: NutritionEstimate,
    val protein: NutritionEstimate,
    val carbohydrates: NutritionEstimate,
    val recipesUsed: Int
)
