package com.example.homeworkout.data.repositories

import androidx.room.withTransaction
import com.example.homeworkout.data.local.AppDatabase
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.WeightLogDao
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.entities.UserWeightLogEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.WeightProfile
import com.example.homeworkout.domain.models.WeightRecord
import com.example.homeworkout.domain.repositories.WeightRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class WeightRepositoryImpl(
    private val database: AppDatabase,
    private val userDao: UserDao,
    private val weightLogDao: WeightLogDao
) : WeightRepository {

    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(
            UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only")
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeWeightProfile(): Flow<WeightProfile> = flow { emit(currentUserId()) }
        .flatMapLatest { userId ->
            combine(
                userDao.observeUser(userId),
                weightLogDao.observeWeightLogs(userId)
            ) { user, logs ->
                WeightProfile(
                    heightCm = user?.heightCm,
                    ageYears = user?.ageYears,
                    gender = user?.gender,
                    records = logs.map { log ->
                        WeightRecord(
                            weightKg = log.weightKg,
                            heightCmSnapshot = log.heightCmSnapshot,
                            loggedAt = log.loggedAt
                        )
                    }
                )
            }
        }

    override suspend fun recordWeight(weightKg: Double, heightCm: Double) {
        val userId = currentUserId()
        database.withTransaction {
            val user = userDao.getUserById(userId) ?: return@withTransaction
            if (user.heightCm != heightCm) {
                userDao.updateUser(user.copy(heightCm = heightCm, updatedAt = System.currentTimeMillis()))
            }
            weightLogDao.insertWeightLog(
                UserWeightLogEntity(
                    userId = userId,
                    weightKg = weightKg,
                    heightCmSnapshot = heightCm
                )
            )
        }
    }

    override suspend fun updateHeight(heightCm: Double) {
        val userId = currentUserId()
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(heightCm = heightCm, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun updateAge(ageYears: Int) {
        val userId = currentUserId()
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(ageYears = ageYears, updatedAt = System.currentTimeMillis()))
    }
}
