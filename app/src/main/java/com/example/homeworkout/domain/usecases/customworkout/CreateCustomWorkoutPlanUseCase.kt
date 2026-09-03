package com.example.homeworkout.domain.usecases.customworkout

import com.example.homeworkout.domain.models.CustomDaySpec
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel
import com.example.homeworkout.domain.repositories.WorkoutRepository

/**
 * Writes a brand-new, user-authored multi-day plan to `workout_plans` (source = CUSTOM, owned by
 * the single local user) — the "Create Workout" counterpart to how
 * [com.example.homeworkout.data.local.seed.AppDatabaseSeeder] builds a system plan. Days are
 * numbered by their position in [days]; empty days should be filtered out by the caller before
 * invoking this, since a day with no exercises isn't worth persisting.
 */
class CreateCustomWorkoutPlanUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        category: WorkoutCategory,
        level: WorkoutLevel,
        days: List<CustomDaySpec>
    ): Long = workoutRepository.createCustomPlan(
        title = title,
        description = description,
        category = category,
        level = level,
        days = days
    )
}
