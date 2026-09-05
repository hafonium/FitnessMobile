package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.FoodAnalysis
import com.example.homeworkout.domain.models.FoodLogEntry
import kotlinx.coroutines.flow.Flow

interface FoodLogRepository {
    fun observeFoodLogs(): Flow<List<FoodLogEntry>>

    suspend fun saveFoodLog(analysis: FoodAnalysis)
}
