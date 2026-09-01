package com.example.homeworkout.domain.usecases.exercises

import com.example.homeworkout.domain.models.Exercise
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseLevel
import com.example.homeworkout.domain.repositories.ExerciseRepository
import kotlinx.coroutines.flow.Flow

/** Backs Filter Exercise / Alter Workout Exercise / Add Exercises. Any filter left null is not applied. */
class SearchExercisesUseCase(
    private val exerciseRepository: ExerciseRepository
) {
    operator fun invoke(
        category: ExerciseCategory? = null,
        level: ExerciseLevel? = null,
        equipmentName: String? = null,
        query: String? = null
    ): Flow<List<Exercise>> {
        return exerciseRepository.searchExercises(category, level, equipmentName, query)
    }
}
