package com.example.homeworkout.domain.usecases.customworkout

import com.example.homeworkout.domain.models.Exercise
import com.example.homeworkout.domain.repositories.ExerciseRepository

/** Resolves exercise ids picked in the Add Exercises browser back to display info (title, gif). */
class GetExercisesByIdsUseCase(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(exerciseIds: List<Long>): List<Exercise> {
        return exerciseRepository.getExercisesByIds(exerciseIds)
    }
}
