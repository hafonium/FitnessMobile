package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.WeightProfile
import kotlinx.coroutines.flow.Flow

interface WeightRepository {
    fun observeWeightProfile(): Flow<WeightProfile>

    /** Records a new measurement and keeps the user's current height in sync atomically. */
    suspend fun recordWeight(weightKg: Double, heightCm: Double)

    suspend fun updateHeight(heightCm: Double)

    suspend fun updateAge(ageYears: Int)
}
