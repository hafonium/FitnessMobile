package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.BadgeDao
import com.example.homeworkout.data.local.dao.WorkoutSessionDao
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.entities.UserSettingsEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.SettingsPreferences
import com.example.homeworkout.domain.models.enums.UserGender
import com.example.homeworkout.domain.repositories.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val userDao: UserDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val badgeDao: BadgeDao
) : SettingsRepository {

    /** This app has a single local user; resolve (or lazily create) it by its seeded email. */
    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSettings(): Flow<SettingsPreferences> = flow { emit(currentUserId()) }
        .flatMapLatest { userId ->
            userDao.observeUserSettings(userId).map { entity ->
                val user = userDao.getUserById(userId)
                (entity ?: UserSettingsEntity(userId = userId)).toDomain(gender = user?.gender)
            }
        }

    override suspend fun updateSettings(preferences: SettingsPreferences) {
        val userId = currentUserId()
        val existing = userDao.getUserSettings(userId) ?: UserSettingsEntity(userId = userId)
        userDao.upsertUserSettings(existing.applyDomain(preferences))

        val user = userDao.getUserById(userId) ?: return
        if (user.gender != preferences.gender) {
            userDao.updateUser(user.copy(gender = preferences.gender, updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun resetWorkoutProgress() {
        val userId = currentUserId()
        workoutSessionDao.deleteAllSessionsForUser(userId)
        badgeDao.deleteAllBadgesForUser(userId)
    }
}

private fun UserSettingsEntity.toDomain(gender: UserGender?): SettingsPreferences = SettingsPreferences(
    gender = gender,
    weeklyGoalDays = weeklyGoalDays,
    firstDayOfWeek = firstDayOfWeek,
    musicEnabled = musicEnabled,
    musicVolume = musicVolume,
    soundEnabled = soundEnabled,
    soundVolume = soundVolume,
    restTimerSec = restTimerSec,
    prepTimerSec = prepTimerSec,
    unitSystem = unitSystem,
    keepScreenOn = keepScreenOn,
    dailyReminderEnabled = dailyReminderEnabled,
    dailyReminderTime = dailyReminderTime,
    ttsVoiceType = ttsVoiceType,
    customVoiceName = ttsVoiceName
)

/** Applies the editable [SettingsPreferences] fields onto [this], preserving the rest (coach video, etc). */
private fun UserSettingsEntity.applyDomain(preferences: SettingsPreferences): UserSettingsEntity = copy(
    weeklyGoalDays = preferences.weeklyGoalDays,
    firstDayOfWeek = preferences.firstDayOfWeek,
    restTimerSec = preferences.restTimerSec,
    prepTimerSec = preferences.prepTimerSec,
    musicEnabled = preferences.musicEnabled,
    musicVolume = preferences.musicVolume,
    soundEnabled = preferences.soundEnabled,
    soundVolume = preferences.soundVolume,
    unitSystem = preferences.unitSystem,
    ttsVoiceType = preferences.ttsVoiceType,
    ttsVoiceName = preferences.customVoiceName,
    keepScreenOn = preferences.keepScreenOn,
    dailyReminderEnabled = preferences.dailyReminderEnabled,
    dailyReminderTime = preferences.dailyReminderTime,
    updatedAt = System.currentTimeMillis()
)
