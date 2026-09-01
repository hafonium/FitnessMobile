package com.example.homeworkout.domain.usecases.exerciseinfo

import com.example.homeworkout.domain.models.ExerciseDetail
import com.example.homeworkout.domain.repositories.ExerciseRepository

/** Backs the Exercise Information sheet: instructions, duration and focus-area muscles. */
class GetExerciseDetailUseCase(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(exerciseId: Long): Result<ExerciseDetail> {
        val detail = exerciseRepository.getExerciseDetail(exerciseId)
        return if (detail != null) {
            Result.success(detail)
        } else {
            Result.failure(NoSuchElementException("Exercise $exerciseId not found"))
        }
    }
}
