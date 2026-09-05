package com.example.homeworkout.domain.usecases.running

import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.repositories.RunningRepository

class GetRunDetailUseCase(private val repository: RunningRepository) {
    suspend operator fun invoke(id: Long): RunSession? = repository.getSession(id)
}
