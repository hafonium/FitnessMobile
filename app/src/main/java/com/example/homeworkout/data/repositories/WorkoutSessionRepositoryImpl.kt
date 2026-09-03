package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.WorkoutSessionDao
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.enums.WorkoutSessionStatus
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class WorkoutSessionRepositoryImpl(
    private val userDao: UserDao,
    private val workoutSessionDao: WorkoutSessionDao
) : WorkoutSessionRepository {

    /** This app has a single local user; resolve (or lazily create) it by its seeded email. */
    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCompletedSessionTimestamps(fromMillis: Long, toMillis: Long): Flow<List<Long>> =
        flow { emit(currentUserId()) }.flatMapLatest { userId ->
            workoutSessionDao.observeCompletedSessionEndTimes(userId, WorkoutSessionStatus.COMPLETED, fromMillis, toMillis)
        }
}
