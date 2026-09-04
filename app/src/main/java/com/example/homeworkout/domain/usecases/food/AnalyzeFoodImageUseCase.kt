package com.example.homeworkout.domain.usecases.food

import com.example.homeworkout.domain.models.FoodAnalysis
import com.example.homeworkout.domain.repositories.FoodAnalysisRepository

class AnalyzeFoodImageUseCase(
    private val repository: FoodAnalysisRepository
) {
    suspend operator fun invoke(image: ByteArray): FoodAnalysis {
        require(image.isNotEmpty()) { "Choose a food photo before analyzing." }
        return repository.analyzeFoodImage(image)
    }
}
