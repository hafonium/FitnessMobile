package com.example.homeworkout.domain.usecases.running

import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.repositories.RunningRepository
import kotlinx.coroutines.flow.Flow

class ObserveRunningSessionUseCase(private val repository: RunningRepository) {
    operator fun invoke(): Flow<RunSession?> = repository.observeLatestSession()
}
