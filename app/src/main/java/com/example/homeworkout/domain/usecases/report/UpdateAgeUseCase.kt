package com.example.homeworkout.domain.usecases.report

import com.example.homeworkout.domain.repositories.WeightRepository

class UpdateAgeUseCase(private val weightRepository: WeightRepository) {
    suspend operator fun invoke(ageYears: Int) {
        if (ageYears in MIN_AGE..MAX_AGE) {
            weightRepository.updateAge(ageYears)
        }
    }

    companion object {
        const val MIN_AGE = 5
        const val MAX_AGE = 120
    }
}
