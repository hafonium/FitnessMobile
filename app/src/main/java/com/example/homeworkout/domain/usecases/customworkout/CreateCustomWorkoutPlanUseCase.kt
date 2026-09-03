package com.example.homeworkout.domain.usecases.customworkout

import com.example.homeworkout.data.local.dao.NewPlanDay
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.WorkoutPlanDao
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanExerciseEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource

/** One exercise slot for [CreateCustomWorkoutPlanUseCase], before it has a real planDayId. */
data class CustomExerciseSpec(
    val exerciseId: Long,
    val targetReps: Int?,
    val targetDurationSec: Int?,
    val restAfterSec: Int? = 15
)

/** One day of a plan being created via [CreateCustomWorkoutPlanUseCase]. */
data class CustomDaySpec(
    val title: String?,
    val exercises: List<CustomExerciseSpec>
)

/**
 * Writes a brand-new, user-authored multi-day plan to `workout_plans` (source = CUSTOM, owned by
 * the single local user) — the "Create Workout" counterpart to how
 * [com.example.homeworkout.data.local.seed.AppDatabaseSeeder] builds a system plan. Days are
 * numbered by their position in [days]; empty days should be filtered out by the caller before
 * invoking this, since a day with no exercises isn't worth persisting.
 */
class CreateCustomWorkoutPlanUseCase(
    private val workoutPlanDao: WorkoutPlanDao,
    private val userDao: UserDao
) {
    /** This app has a single local user; resolve (or lazily create) it by its seeded email. */
    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only"))
    }

    suspend operator fun invoke(
        title: String,
        description: String?,
        category: WorkoutCategory,
        level: WorkoutLevel,
        days: List<CustomDaySpec>
    ): Long {
        val plan = WorkoutPlanEntity(
            ownerUserId = currentUserId(),
            title = title,
            description = description,
            category = category,
            level = level,
            source = WorkoutPlanSource.CUSTOM
        )
        val dayInputs = days.mapIndexed { index, day ->
            NewPlanDay(
                dayNumber = index + 1,
                title = day.title,
                exercises = day.exercises.map { exercise ->
                    WorkoutPlanExerciseEntity(
                        planDayId = 0L, // placeholder — insertCustomPlan assigns the real id per day
                        exerciseId = exercise.exerciseId,
                        orderIndex = 0, // placeholder — insertCustomPlan assigns the real order per day
                        targetReps = exercise.targetReps,
                        targetDurationSec = exercise.targetDurationSec,
                        restAfterSec = exercise.restAfterSec
                    )
                }
            )
        }
        return workoutPlanDao.insertCustomPlan(plan, dayInputs)
    }
}
