package com.example.homeworkout.domain.usecases.food

import com.example.homeworkout.domain.models.FoodLogEntry
import com.example.homeworkout.domain.repositories.FoodLogRepository
import kotlinx.coroutines.flow.Flow

class GetFoodLogHistoryUseCase(
    private val repository: FoodLogRepository
) {
    operator fun invoke(): Flow<List<FoodLogEntry>> = repository.observeFoodLogs()
}
