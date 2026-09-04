package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.models.enums.WorkoutPhase
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository

/** "Save & Exit" from the mid-workout guard dialog: saves progress and marks the session PAUSED. */
class SaveAndExitWorkoutSessionUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(sessionId: Long, phase: WorkoutPhase, orderIndex: Int, remainingSec: Int?) =
        workoutSessionRepository.saveAndExit(sessionId, phase, orderIndex, remainingSec)
}
