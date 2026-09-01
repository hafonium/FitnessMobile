package com.example.homeworkout.data.local.dao.relations

import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseForce
import com.example.homeworkout.domain.models.enums.ExerciseLevel

/**
 * Flat projection returned by [com.example.homeworkout.data.local.dao.ExerciseDao.searchExercises],
 * pre-joined with the exercise's [com.example.homeworkout.data.local.entities.EquipmentTypeEntity]
 * name so the repository doesn't need a second round trip to resolve it.
 */
data class ExerciseListRow(
    val exerciseId: Long,
    val title: String,
    val gifUrl: String?,
    val category: ExerciseCategory,
    val level: ExerciseLevel,
    val force: ExerciseForce,
    val equipmentName: String
)
