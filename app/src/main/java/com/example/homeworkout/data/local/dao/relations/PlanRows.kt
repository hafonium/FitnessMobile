package com.example.homeworkout.data.local.dao.relations

import androidx.room.Embedded
import com.example.homeworkout.data.local.entities.WorkoutPlanEntity
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseLevel

/**
 * Flat projection returned by [com.example.homeworkout.data.local.dao.WorkoutPlanDao.observePlanSummaries].
 * `totalDays`/`totalExercises` are computed in SQL rather than stored on [WorkoutPlanEntity].
 */
data class WorkoutPlanSummaryRow(
    @Embedded val plan: WorkoutPlanEntity,
    val totalDays: Int,
    val totalExercises: Int
)

/**
 * Flat, pre-joined projection of a [com.example.homeworkout.data.local.entities.WorkoutPlanDayEntity]
 * row and one of its [com.example.homeworkout.data.local.entities.WorkoutPlanExerciseEntity] rows,
 * joined with the [com.example.homeworkout.data.local.entities.ExerciseEntity] it points at.
 * Grouped back into a day -> exercises tree by the repository mapper.
 */
data class PlanDayExerciseRow(
    val planExerciseId: Long,
    val planDayId: Long,
    val dayNumber: Int,
    val dayTitle: String?,
    val orderIndex: Int,
    val targetReps: Int?,
    val targetDurationSec: Int?,
    val restAfterSec: Int?,
    val exerciseId: Long,
    val exerciseTitle: String,
    val exerciseGifUrl: String?,
    val exerciseCategory: ExerciseCategory,
    val exerciseLevel: ExerciseLevel
)
