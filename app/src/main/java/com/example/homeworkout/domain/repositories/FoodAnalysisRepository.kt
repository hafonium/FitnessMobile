package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.FoodAnalysis

interface FoodAnalysisRepository {
    suspend fun analyzeFoodImage(image: ByteArray): FoodAnalysis
}
