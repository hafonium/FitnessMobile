package com.example.homeworkout.domain.usecases.details

import com.example.homeworkout.data.local.dao.WorkoutPlanDao
import com.example.homeworkout.data.local.entities.WorkoutPlanExerciseEntity

class EditWorkoutPlanUseCase(private val workoutPlanDao: WorkoutPlanDao) {

    suspend fun addExercises(planDayId: Long, exerciseIds: List<Long>) {
        if (exerciseIds.isEmpty()) return
        
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
        workoutPlanDao.updatePlanExercise(existing.copy(exerciseId = newExerciseId))
    }

    suspend fun updateReps(planExerciseId: Long, targetReps: Int) {
        val existing = workoutPlanDao.getPlanExerciseById(planExerciseId) ?: return
        workoutPlanDao.updatePlanExercise(existing.copy(targetReps = targetReps))
    }
}
