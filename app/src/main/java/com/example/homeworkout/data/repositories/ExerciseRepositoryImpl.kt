package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.ExerciseDao
import com.example.homeworkout.data.local.dao.relations.ExerciseListRow
import com.example.homeworkout.domain.models.Exercise
import com.example.homeworkout.domain.models.ExerciseDetail
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseLevel
import com.example.homeworkout.domain.models.enums.MuscleRole
import com.example.homeworkout.domain.repositories.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun searchExercises(
        category: ExerciseCategory?,
        level: ExerciseLevel?,
        equipmentName: String?,
        query: String?
    ): Flow<List<Exercise>> {
        return exerciseDao.searchExercises(category, level, equipmentName, query?.ifBlank { null })
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun getEquipmentNames(): Flow<List<String>> {
        return exerciseDao.observeEquipmentTypes().map { types -> types.map { it.name } }
    }

    override suspend fun getExerciseDetail(exerciseId: Long): ExerciseDetail? {
        val row = exerciseDao.getExerciseRowById(exerciseId) ?: return null
        val primaryMuscles = exerciseDao.getMuscleNamesForExercise(exerciseId, MuscleRole.PRIMARY)
        val secondaryMuscles = exerciseDao.getMuscleNamesForExercise(exerciseId, MuscleRole.SECONDARY)
        val instructions = exerciseDao.getInstructionSteps(exerciseId).map { it.instructionText }
        val imageUrls = exerciseDao.getExerciseImages(exerciseId).map { it.imageUrl }
        return ExerciseDetail(
            exercise = row.toDomain(),
            primaryMuscles = primaryMuscles,
            secondaryMuscles = secondaryMuscles,
            instructions = instructions,
            imageUrls = imageUrls
        )
    }
}

private fun ExerciseListRow.toDomain(): Exercise = Exercise(
    id = exerciseId,
    title = title,
    gifUrl = gifUrl,
    category = category,
    equipmentName = equipmentName,
    level = level,
    force = force
)
