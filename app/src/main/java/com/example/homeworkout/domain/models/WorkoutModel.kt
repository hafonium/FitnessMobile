package com.example.homeworkout.domain.models

/**
 * Pure Kotlin domain representation of a workout — no Room/Android imports allowed here.
 * Mapping to/from [com.example.homeworkout.data.local.entities.WorkoutEntity] lives in
 * data/repositories/WorkoutRepositoryImpl, never in this file.
 */
data class WorkoutModel(
    val id: Int,
    val name: String,
    val category: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val imageUrl: String?,
    val description: String
)
