package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.Exercise
import com.example.homeworkout.domain.models.ExerciseDetail
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseLevel
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    /** Backs Filter Exercise / Alter Workout Exercise / Add Exercises; any filter left null is not applied. */
    fun searchExercises(
        category: ExerciseCategory? = null,
        level: ExerciseLevel? = null,
        equipmentName: String? = null,
        query: String? = null
    ): Flow<List<Exercise>>

    fun getEquipmentNames(): Flow<List<String>>

    /** Full detail for the Exercise Information sheet. */
    suspend fun getExerciseDetail(exerciseId: Long): ExerciseDetail?

    /** Batch lookup used by the Custom Workout builder to resolve picked exercise ids. */
    suspend fun getExercisesByIds(exerciseIds: List<Long>): List<Exercise>
}
