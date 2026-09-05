package com.example.homeworkout.domain.usecases.running

import com.example.homeworkout.domain.repositories.RunningRepository

class DeleteRunUseCase(private val repository: RunningRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteSession(id)
}
