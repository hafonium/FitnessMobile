package com.example.homeworkout.domain.usecases.report

import com.example.homeworkout.domain.repositories.WeightRepository

class RecordWeightUseCase(private val weightRepository: WeightRepository) {
    suspend operator fun invoke(weightKg: Double, heightCm: Double) {
        if (weightKg in MIN_WEIGHT_KG..MAX_WEIGHT_KG && heightCm in MIN_HEIGHT_CM..MAX_HEIGHT_CM) {
            weightRepository.recordWeight(weightKg, heightCm)
        }
    }

    companion object {
        const val MIN_WEIGHT_KG = 20.0
        const val MAX_WEIGHT_KG = 500.0
        const val MIN_HEIGHT_CM = 50.0
        const val MAX_HEIGHT_CM = 300.0
    }
}
