package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.FitnessProfile
import kotlinx.coroutines.flow.Flow

interface FitnessProfileRepository {
    fun observeProfile(): Flow<FitnessProfile?>
    suspend fun getProfile(): FitnessProfile?
    suspend fun saveProfile(profile: FitnessProfile, recommendedCatalogId: String?, catalogVersion: Int)
}
