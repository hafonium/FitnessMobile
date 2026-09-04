package com.example.homeworkout.domain.usecases.home

import com.example.homeworkout.domain.models.RecoveryScore
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Backs the Home screen's offline Readiness & Recovery Score card. Zero network calls: reads local session history and runs [RecoveryCalculator] instantly. */
class GetRecoveryScoreUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val recoveryCalculator: RecoveryCalculator = RecoveryCalculator()
) {
    operator fun invoke(): Flow<RecoveryScore> =
        workoutSessionRepository.observeCompletedSessions().map { sessions ->
            recoveryCalculator.calculate(sessions)
        }
}
