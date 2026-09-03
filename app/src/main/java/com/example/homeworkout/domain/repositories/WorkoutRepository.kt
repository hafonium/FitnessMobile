package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.CustomDaySpec
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    /** All active plans, optionally narrowed to one Training-tab category and/or source (e.g. the
     *  current user's own Custom Workout plans). */
    fun getWorkouts(category: WorkoutCategory? = null, source: WorkoutPlanSource? = null): Flow<List<WorkoutModel>>

    suspend fun getWorkoutById(id: Long): WorkoutModel?

    /** The plan plus its days and each day's exercises, for the Workout Screen. */
    fun getWorkoutPlanDetail(id: Long): Flow<WorkoutPlanDetail?>

    /** Appends exercises to one day of a plan, after its existing exercises. */
    suspend fun addExercisesToDay(planDayId: Long, exerciseIds: List<Long>)

    /** Swaps one plan-exercise slot's underlying exercise, keeping its reps/duration/order. */
    suspend fun replacePlanExercise(planExerciseId: Long, newExerciseId: Long)

    /** Updates one plan-exercise slot's target rep count. */
    suspend fun updatePlanExerciseReps(planExerciseId: Long, targetReps: Int)

    /** Removes one exercise slot and compacts the remaining order; CUSTOM plans only. */
    suspend fun deletePlanExercise(planExerciseId: Long)

    /** Persists the complete exercise order for one day; CUSTOM plans only. */
    suspend fun reorderPlanExercises(planDayId: Long, orderedPlanExerciseIds: List<Long>)

    /** Writes a brand-new, user-authored multi-day plan (source = CUSTOM, owned by the single
     *  local user). Days are numbered by their position in [days]; empty days should be filtered
     *  out by the caller, since a day with no exercises isn't worth persisting. */
    suspend fun createCustomPlan(
        title: String,
        description: String?,
        category: WorkoutCategory,
        level: WorkoutLevel,
        days: List<CustomDaySpec>
    ): Long

    /** Deletes a plan the user created — guarded to CUSTOM plans only; a no-op for system plans. */
    suspend fun deleteCustomPlan(planId: Long)
}
