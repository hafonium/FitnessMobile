package com.example.homeworkout.domain.usecases.details

import com.example.homeworkout.data.local.dao.WorkoutPlanDao
import com.example.homeworkout.data.local.entities.WorkoutPlanExerciseEntity
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource

class EditWorkoutPlanUseCase(private val workoutPlanDao: WorkoutPlanDao) {

    suspend fun addExercises(planDayId: Long, exerciseIds: List<Long>) {
        if (exerciseIds.isEmpty()) return
        if (workoutPlanDao.getPlanSourceForDay(planDayId) != WorkoutPlanSource.CUSTOM) return
        
        val maxOrderIndex = workoutPlanDao.getMaxOrderIndexForDay(planDayId) ?: -1
        var currentOrderIndex = maxOrderIndex + 1

        val entities = exerciseIds.map { exerciseId ->
            WorkoutPlanExerciseEntity(
                planDayId = planDayId,
                exerciseId = exerciseId,
                orderIndex = currentOrderIndex++,
                targetReps = 10, // default target
                targetDurationSec = null,
                restAfterSec = 15 // default rest
            )
        }
        
        entities.forEach { entity ->
            workoutPlanDao.insertPlanExercise(entity)
        }
    }

    suspend fun replaceExercise(planExerciseId: Long, newExerciseId: Long) {
        val existing = workoutPlanDao.getPlanExerciseById(planExerciseId) ?: return
        if (workoutPlanDao.getPlanSourceForDay(existing.planDayId) != WorkoutPlanSource.CUSTOM) return
        workoutPlanDao.updatePlanExercise(existing.copy(exerciseId = newExerciseId))
    }

    suspend fun updateReps(planExerciseId: Long, targetReps: Int) {
        val existing = workoutPlanDao.getPlanExerciseById(planExerciseId) ?: return
        if (workoutPlanDao.getPlanSourceForDay(existing.planDayId) != WorkoutPlanSource.CUSTOM) return
        workoutPlanDao.updatePlanExercise(existing.copy(targetReps = targetReps))
    }

    suspend fun deleteExercise(planExerciseId: Long) {
        workoutPlanDao.deleteCustomPlanExercise(planExerciseId)
    }

    suspend fun reorderExercises(planDayId: Long, orderedExerciseIds: List<Long>) {
        workoutPlanDao.reorderPlanExercises(planDayId, orderedExerciseIds)
    }
}
