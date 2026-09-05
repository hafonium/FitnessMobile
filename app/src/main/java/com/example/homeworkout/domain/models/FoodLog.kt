package com.example.homeworkout.domain.models

/** One saved Food Calorie Scanner result (`food_logs`). See docs/weight-forecast-feature.md. */
data class FoodLogEntry(
    val logId: Long,
    val category: String,
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val loggedAt: Long
)
