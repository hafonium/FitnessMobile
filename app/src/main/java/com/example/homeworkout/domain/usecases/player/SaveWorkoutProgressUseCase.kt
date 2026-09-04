package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.models.enums.WorkoutPhase
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository

/** Auto-save point during active play: exercise completed, phase changed, or the user paused. */
class SaveWorkoutProgressUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(sessionId: Long, phase: WorkoutPhase, orderIndex: Int, remainingSec: Int?) =
        workoutSessionRepository.saveProgress(sessionId, phase, orderIndex, remainingSec)
}
