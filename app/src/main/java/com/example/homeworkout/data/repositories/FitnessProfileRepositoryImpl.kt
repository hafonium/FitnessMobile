package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.entities.UserFitnessProfileEntity
import com.example.homeworkout.domain.models.ExperienceLevel
import com.example.homeworkout.domain.models.FitnessProfile
import com.example.homeworkout.domain.models.PrimaryGoal
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.repositories.FitnessProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FitnessProfileRepositoryImpl(
    private val userDao: UserDao
) : FitnessProfileRepository {

    override fun observeProfile(): Flow<FitnessProfile?> = flow {
        val userId = userDao.getFirstUserId()
        if (userId == null) {
            emitAll(flowOf(null))
        } else {
            emitAll(userDao.observeFitnessProfile(userId).map { it?.toDomain() })
        }
    }

    override suspend fun getProfile(): FitnessProfile? {
        val userId = userDao.getFirstUserId() ?: return null
        return userDao.getFitnessProfile(userId)?.toDomain()
    }

    override suspend fun saveProfile(profile: FitnessProfile, recommendedCatalogId: String?, catalogVersion: Int) {
        val userId = userDao.getFirstUserId() ?: return
        val existing = userDao.getFitnessProfile(userId)
        userDao.upsertFitnessProfile(
            UserFitnessProfileEntity(
                userId = userId,
                primaryGoal = profile.primaryGoal.key,
                experienceLevel = profile.experienceLevel.key,
                daysPerWeek = profile.daysPerWeek,
                sessionMinutes = profile.sessionMinutes,
                availableEquipmentCsv = profile.availableEquipment.joinToString(","),
                focusCategoriesCsv = profile.focusCategories.joinToString(",") { it.name },
                focusMusclesCsv = profile.focusMuscles.joinToString(","),
                injuriesOrLimitations = profile.injuriesOrLimitations,
                recommendedCatalogId = recommendedCatalogId,
                catalogVersion = catalogVersion,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

private fun UserFitnessProfileEntity.toDomain(): FitnessProfile? {
    val goal = PrimaryGoal.fromKey(primaryGoal) ?: return null
    val level = ExperienceLevel.fromKey(experienceLevel) ?: return null
    return FitnessProfile(
        primaryGoal = goal,
        experienceLevel = level,
        daysPerWeek = daysPerWeek,
        sessionMinutes = sessionMinutes,
        availableEquipment = availableEquipmentCsv.splitCsv(),
        focusCategories = focusCategoriesCsv.splitCsv().mapNotNull { name ->
            ExerciseCategory.entries.firstOrNull { it.name == name }
        }.toSet(),
        focusMuscles = focusMusclesCsv.splitCsv(),
        injuriesOrLimitations = injuriesOrLimitations
    )
}

private fun String.splitCsv(): Set<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
