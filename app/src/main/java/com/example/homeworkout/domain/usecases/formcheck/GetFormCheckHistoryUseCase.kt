package com.example.homeworkout.domain.usecases.formcheck

import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.repositories.FormCheckRepository
import kotlinx.coroutines.flow.Flow

class GetFormCheckHistoryUseCase(
    private val repository: FormCheckRepository
) {
    operator fun invoke(): Flow<List<FormAnalysis>> = repository.observeHistory()
}
