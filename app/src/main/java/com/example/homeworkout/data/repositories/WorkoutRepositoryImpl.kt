package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.WorkoutPlanDao
import com.example.homeworkout.data.local.dao.relations.WorkoutPlanSummaryRow
import com.example.homeworkout.data.local.entities.WorkoutPlanEntity
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.WorkoutPlanDayDetail
import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.repositories.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepositoryImpl(
    private val workoutPlanDao: WorkoutPlanDao
) : WorkoutRepository {

    override fun getWorkouts(category: WorkoutCategory?): Flow<List<WorkoutModel>> {
        return workoutPlanDao.observePlanSummaries(category = category).map { rows -> rows.map { it.toDomain() } }
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
                            targetDurationSec = row.targetDurationSec
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
}

private fun WorkoutPlanSummaryRow.toDomain(): WorkoutModel = plan.toDomain(totalDays = totalDays, totalExercises = totalExercises)

private fun WorkoutPlanEntity.toDomain(totalDays: Int, totalExercises: Int): WorkoutModel = WorkoutModel(
    id = planId,
    title = title,
    description = description,
    category = category,
    level = level,
    coverImageUrl = coverImageUrl,
    requiresPremium = requiresPremium,
    totalDays = totalDays,
    totalExercises = totalExercises
)
