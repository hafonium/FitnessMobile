package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseForce
import com.example.homeworkout.domain.models.enums.ExerciseLevel

/**
 * Pure Kotlin domain representation of one exercise from the library (Filter Exercise, Alter
 * Workout Exercise, Add Exercises). Mapping to/from
 * [com.example.homeworkout.data.local.entities.ExerciseEntity] lives in
 * data/repositories/ExerciseRepositoryImpl.
 */
data class Exercise(
    val id: Long,
    val title: String,
    val gifUrl: String?,
    val category: ExerciseCategory,
    val equipmentName: String,
    val level: ExerciseLevel,
    val force: ExerciseForce
)

/** Full detail shown on the Exercise Information sheet. */
data class ExerciseDetail(
    val exercise: Exercise,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
    val imageUrls: List<String>
)
