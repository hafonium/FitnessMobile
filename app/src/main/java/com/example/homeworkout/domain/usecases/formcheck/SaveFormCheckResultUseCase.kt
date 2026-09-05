package com.example.homeworkout.domain.usecases.formcheck

import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.repositories.FormCheckRepository

class SaveFormCheckResultUseCase(
    private val repository: FormCheckRepository
) {
    suspend operator fun invoke(analysis: FormAnalysis): Long = repository.saveResult(analysis)
}
