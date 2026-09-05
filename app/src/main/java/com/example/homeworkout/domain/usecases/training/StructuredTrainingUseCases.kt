package com.example.homeworkout.domain.usecases.training

import com.example.homeworkout.domain.models.training.StructuredProgramProgress
import com.example.homeworkout.domain.models.training.StructuredTrainingProgram
import com.example.homeworkout.domain.repositories.StructuredTrainingCatalogRepository
import com.example.homeworkout.domain.repositories.StructuredTrainingProgressRepository
import kotlinx.coroutines.flow.Flow

class GetTrainingProgramUseCase(private val repository: StructuredTrainingCatalogRepository) {
    suspend operator fun invoke(programId: String): StructuredTrainingProgram? = repository.getProgram(programId)
}

class GetTrainingProgressUseCase(private val repository: StructuredTrainingProgressRepository) {
    operator fun invoke(programId: String): Flow<StructuredProgramProgress> = repository.observeProgress(programId)
}

class EnrollTrainingProgramUseCase(private val repository: StructuredTrainingProgressRepository) {
    suspend operator fun invoke(programId: String) = repository.enroll(programId)
}

class StartStructuredSessionUseCase(private val repository: StructuredTrainingProgressRepository) {
    suspend operator fun invoke(programId: String, sessionId: String, weekNumber: Int) =
        repository.setActiveSession(programId, sessionId, weekNumber)
}

class CompleteStructuredSessionUseCase(
    private val progressRepository: StructuredTrainingProgressRepository,
    private val catalogRepository: StructuredTrainingCatalogRepository
) {
    suspend operator fun invoke(
        programId: String,
        sessionId: String,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null
    ) {
        val program = catalogRepository.getProgram(programId) ?: return
        val week = program.weeks.firstOrNull { candidate -> candidate.sessions.any { it.id == sessionId } } ?: return
        progressRepository.completeSession(
            programId = programId,
            sessionId = sessionId,
            weekNumber = week.weekNumber,
            requiredSessionIds = week.sessions.filterNot { it.isOptional }.mapTo(mutableSetOf()) { it.id },
            nextWeekNumber = program.weeks.firstOrNull { it.weekNumber > week.weekNumber }?.weekNumber,
            isLastWeek = week.weekNumber == program.weeks.maxOf { it.weekNumber },
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters
        )
    }
}

class RepeatStructuredWeekUseCase(private val repository: StructuredTrainingProgressRepository) {
    suspend operator fun invoke(programId: String, weekNumber: Int) = repository.resetWeek(programId, weekNumber)
}
