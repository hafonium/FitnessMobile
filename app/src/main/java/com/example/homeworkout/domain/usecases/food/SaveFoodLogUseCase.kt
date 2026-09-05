package com.example.homeworkout.domain.usecases.food

import com.example.homeworkout.domain.models.FoodAnalysis
import com.example.homeworkout.domain.repositories.FoodLogRepository

class SaveFoodLogUseCase(
    private val repository: FoodLogRepository
) {
    suspend operator fun invoke(analysis: FoodAnalysis) {
        repository.saveFoodLog(analysis)
    }
}
