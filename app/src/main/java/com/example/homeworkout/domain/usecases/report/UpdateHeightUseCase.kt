package com.example.homeworkout.domain.usecases.report

import com.example.homeworkout.domain.repositories.WeightRepository

class UpdateHeightUseCase(private val weightRepository: WeightRepository) {
    suspend operator fun invoke(heightCm: Double) {
        if (heightCm in RecordWeightUseCase.MIN_HEIGHT_CM..RecordWeightUseCase.MAX_HEIGHT_CM) {
            weightRepository.updateHeight(heightCm)
        }
    }
}
