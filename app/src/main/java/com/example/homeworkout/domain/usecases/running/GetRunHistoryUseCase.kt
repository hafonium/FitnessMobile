package com.example.homeworkout.domain.usecases.running

import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.repositories.RunningRepository
import kotlinx.coroutines.flow.Flow

class GetRunHistoryUseCase(private val repository: RunningRepository) {
    operator fun invoke(): Flow<List<RunSession>> = repository.observeFinishedSessions()
}
