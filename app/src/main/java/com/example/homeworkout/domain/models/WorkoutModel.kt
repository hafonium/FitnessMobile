package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel

/**
 * Pure Kotlin domain representation of a workout plan summary card, as shown on the Training
 * (home) screen and the category Workout List screen — no Room/Android imports allowed here.
 * Mapping to/from [com.example.homeworkout.data.local.entities.WorkoutPlanEntity] lives in
 * data/repositories/WorkoutRepositoryImpl, never in this file.
 */
data class WorkoutModel(
    val id: Long,
    val title: String,
    val description: String?,
    val category: WorkoutCategory,
    val level: WorkoutLevel,
    val coverImageUrl: String?,
    val requiresPremium: Boolean,
    val totalDays: Int,
    val totalExercises: Int
)
