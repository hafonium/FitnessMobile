package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.NewPlanDay
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.WorkoutPlanDao
import com.example.homeworkout.data.local.dao.relations.WorkoutPlanSummaryRow
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanExerciseEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.CustomDaySpec
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.WorkoutPlanDayDetail
import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource
import com.example.homeworkout.domain.repositories.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepositoryImpl(
    private val workoutPlanDao: WorkoutPlanDao,
    private val userDao: UserDao
) : WorkoutRepository {

    override fun getWorkouts(category: WorkoutCategory?, source: WorkoutPlanSource?): Flow<List<WorkoutModel>> {
        return workoutPlanDao.observePlanSummaries(category = category, source = source).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getWorkoutById(id: Long): WorkoutModel? {
        val plan = workoutPlanDao.getPlanById(id) ?: return null
        val totalDays = workoutPlanDao.getPlanDays(id).size
        val totalExercises = workoutPlanDao.countExercisesForPlan(id)
        return plan.toDomain(totalDays = totalDays, totalExercises = totalExercises)
    }

    override fun getWorkoutPlanDetail(id: Long): Flow<WorkoutPlanDetail?> {
        return workoutPlanDao.observePlanExerciseRows(id).map { rows ->
            val plan = workoutPlanDao.getPlanById(id) ?: return@map null
            val days = workoutPlanDao.getPlanDays(id)
            val exerciseRowsByDay = rows.groupBy { it.planDayId }

            val dayDetails = days.map { day ->
                val dayRows = exerciseRowsByDay[day.planDayId].orEmpty().sortedBy { it.orderIndex }
                WorkoutPlanDayDetail(
                    planDayId = day.planDayId,
                    dayNumber = day.dayNumber,
                    title = day.title,
                    exercises = dayRows.map { row ->
                        PlanExerciseSummary(
                            planExerciseId = row.planExerciseId,
                            exerciseId = row.exerciseId,
                            title = row.exerciseTitle,
                            gifUrl = row.exerciseGifUrl,
                            imageUrl = row.exerciseImageUrl,
                            targetReps = row.targetReps,
                            targetDurationSec = row.targetDurationSec,
                            restAfterSec = row.restAfterSec
                        )
                    }
                )
            }

            WorkoutPlanDetail(
                plan = plan.toDomain(totalDays = days.size, totalExercises = dayDetails.sumOf { it.exercises.size }),
                days = dayDetails
            )
        }
    }

    override suspend fun addExercisesToDay(planDayId: Long, exerciseIds: List<Long>) {
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

    override suspend fun replacePlanExercise(planExerciseId: Long, newExerciseId: Long) {
        val existing = workoutPlanDao.getPlanExerciseById(planExerciseId) ?: return
        if (workoutPlanDao.getPlanSourceForDay(existing.planDayId) != WorkoutPlanSource.CUSTOM) return
        workoutPlanDao.updatePlanExercise(existing.copy(exerciseId = newExerciseId))
    }

    override suspend fun updatePlanExerciseReps(planExerciseId: Long, targetReps: Int) {
        val existing = workoutPlanDao.getPlanExerciseById(planExerciseId) ?: return
        if (workoutPlanDao.getPlanSourceForDay(existing.planDayId) != WorkoutPlanSource.CUSTOM) return
        workoutPlanDao.updatePlanExercise(existing.copy(targetReps = targetReps))
    }

    override suspend fun deletePlanExercise(planExerciseId: Long) {
        workoutPlanDao.deleteCustomPlanExercise(planExerciseId)
    }

    override suspend fun reorderPlanExercises(planDayId: Long, orderedPlanExerciseIds: List<Long>) {
        workoutPlanDao.reorderPlanExercises(planDayId, orderedPlanExerciseIds)
    }

    override suspend fun createCustomPlan(
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

    override suspend fun deleteCustomPlan(planId: Long) {
        workoutPlanDao.deleteOrArchiveCustomPlan(planId)
    }

    /** This app has a single local user; resolve (or lazily create) it by its seeded email. */
    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only"))
    }
}

private fun WorkoutPlanSummaryRow.toDomain(): WorkoutModel = plan.toDomain(totalDays = totalDays, totalExercises = totalExercises)

private fun WorkoutPlanEntity.toDomain(totalDays: Int, totalExercises: Int): WorkoutModel = WorkoutModel(
    id = planId,
    title = title,
    description = description,
    category = category,
    level = level,
    source = source,
    coverImageUrl = coverImageUrl,
    requiresPremium = requiresPremium,
    totalDays = totalDays,
    totalExercises = totalExercises
)
