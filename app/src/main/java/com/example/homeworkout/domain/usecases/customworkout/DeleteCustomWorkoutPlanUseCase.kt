package com.example.homeworkout.domain.usecases.customworkout

import com.example.homeworkout.data.local.dao.WorkoutPlanDao
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource

/** Deletes a plan the user created — guarded to CUSTOM plans only; a no-op for system plans. */
class DeleteCustomWorkoutPlanUseCase(
    private val workoutPlanDao: WorkoutPlanDao
) {
    suspend operator fun invoke(planId: Long) {
        val plan = workoutPlanDao.getPlanById(planId) ?: return
        if (plan.source != WorkoutPlanSource.CUSTOM) return
        workoutPlanDao.deletePlan(plan)
    }
}
