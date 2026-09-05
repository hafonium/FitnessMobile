package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.FoodLogDao
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.entities.FoodLogEntity
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.FoodAnalysis
import com.example.homeworkout.domain.models.FoodLogEntry
import com.example.homeworkout.domain.repositories.FoodLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FoodLogRepositoryImpl(
    private val userDao: UserDao,
    private val foodLogDao: FoodLogDao
) : FoodLogRepository {

    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(
            UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only")
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFoodLogs(): Flow<List<FoodLogEntry>> = flow { emit(currentUserId()) }
        .flatMapLatest { userId -> foodLogDao.observeFoodLogs(userId) }
        .map { logs ->
            logs.map { log ->
                FoodLogEntry(
                    logId = log.logId,
                    category = log.category,
                    caloriesKcal = log.caloriesKcal,
                    proteinG = log.proteinG,
                    carbsG = log.carbsG,
                    fatG = log.fatG,
                    loggedAt = log.loggedAt
                )
            }
        }

    override suspend fun saveFoodLog(analysis: FoodAnalysis) {
        val userId = currentUserId()
        foodLogDao.insertFoodLog(
            FoodLogEntity(
                userId = userId,
                category = analysis.category,
                categoryProbability = analysis.categoryProbability,
                caloriesKcal = analysis.calories.value,
                proteinG = analysis.protein.value,
                carbsG = analysis.carbohydrates.value,
                fatG = analysis.fat.value,
                recipesUsed = analysis.recipesUsed
            )
        )
    }
}
