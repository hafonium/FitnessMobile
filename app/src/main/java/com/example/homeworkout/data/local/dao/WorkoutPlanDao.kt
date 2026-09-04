package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.homeworkout.data.local.dao.relations.PlanDayExerciseRow
import com.example.homeworkout.data.local.dao.relations.SessionExerciseSnapshotSourceRow
import com.example.homeworkout.data.local.dao.relations.SessionPlanSnapshotRow
import com.example.homeworkout.data.local.dao.relations.WorkoutPlanSummaryRow
import com.example.homeworkout.data.local.entities.WorkoutPlanDayEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanExerciseEntity
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource
import kotlinx.coroutines.flow.Flow

/**
 * One day's worth of exercises for [WorkoutPlanDao.insertCustomPlan]. Each exercise's
 * [WorkoutPlanExerciseEntity.planDayId] is a placeholder (ignored) — the real id is assigned once
 * the day itself has been inserted.
 */
data class NewPlanDay(
    val dayNumber: Int,
    val title: String?,
    val exercises: List<WorkoutPlanExerciseEntity>
)

@Dao
interface WorkoutPlanDao {
    // --- Plans ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlanEntity)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlanEntity)

    @Query("SELECT COUNT(*) FROM workout_plans")
    suspend fun countPlans(): Int

    @Query("SELECT * FROM workout_plans WHERE planId = :planId")
    suspend fun getPlanById(planId: Long): WorkoutPlanEntity?

    @Query(
        """
        SELECT p.planId, p.title AS planTitle, p.coverImageUrl AS planCoverImageUrl,
               d.planDayId, d.dayNumber, d.title AS dayTitle
        FROM workout_plans p
        INNER JOIN workout_plan_days d ON d.planId = p.planId
        WHERE p.planId = :planId AND d.planDayId = :planDayId
        LIMIT 1
        """
    )
    suspend fun getSessionPlanSnapshotSource(
        planId: Long,
        planDayId: Long
    ): SessionPlanSnapshotRow?

    @Query(
        """
        SELECT p.*,
               (SELECT COUNT(*) FROM workout_plan_days d WHERE d.planId = p.planId) AS totalDays,
               (SELECT COUNT(*) FROM workout_plan_exercises pe
                    INNER JOIN workout_plan_days d2 ON d2.planDayId = pe.planDayId
                    WHERE d2.planId = p.planId) AS totalExercises
        FROM workout_plans p
        WHERE p.isActive = 1
        AND (:category IS NULL OR p.category = :category)
        AND (:source IS NULL OR p.source = :source)
        ORDER BY p.createdAt DESC
        """
    )
    fun observePlanSummaries(
        category: WorkoutCategory? = null,
        source: WorkoutPlanSource? = null
    ): Flow<List<WorkoutPlanSummaryRow>>

    // --- Plan days ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanDay(day: WorkoutPlanDayEntity): Long

    @Query("SELECT * FROM workout_plan_days WHERE planId = :planId ORDER BY dayNumber")
    suspend fun getPlanDays(planId: Long): List<WorkoutPlanDayEntity>

    @Query(
        """
        SELECT COUNT(*) FROM workout_plan_exercises pe
        INNER JOIN workout_plan_days d ON d.planDayId = pe.planDayId
        WHERE d.planId = :planId
        """
    )
    suspend fun countExercisesForPlan(planId: Long): Int

    @Query(
        """
        SELECT pe.exerciseId, pe.orderIndex, e.title AS exerciseTitle,
               pe.targetReps, pe.targetDurationSec
        FROM workout_plan_exercises pe
        INNER JOIN exercises e ON e.exerciseId = pe.exerciseId
        WHERE pe.planDayId = :planDayId
        ORDER BY pe.orderIndex
        """
    )
    suspend fun getSessionExerciseSnapshotSources(
        planDayId: Long
    ): List<SessionExerciseSnapshotSourceRow>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE planId = :planId")
    suspend fun countSessionsForPlan(planId: Long): Int

    @Query("UPDATE workout_plans SET isActive = 0, updatedAt = :updatedAt WHERE planId = :planId")
    suspend fun archivePlan(planId: Long, updatedAt: Long)

    /** Hard-deletes an unused custom plan, but archives one referenced by session history. */
    @Transaction
    suspend fun deleteOrArchiveCustomPlan(planId: Long) {
        val plan = getPlanById(planId) ?: return
        if (plan.source != WorkoutPlanSource.CUSTOM) return
        if (countSessionsForPlan(planId) == 0) {
            deletePlan(plan)
        } else {
            archivePlan(planId, System.currentTimeMillis())
        }
    }

    // --- Plan exercises ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercise(exercise: WorkoutPlanExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercises(exercises: List<WorkoutPlanExerciseEntity>)

    @Update
    suspend fun updatePlanExercise(exercise: WorkoutPlanExerciseEntity)

    @Delete
    suspend fun deletePlanExercise(exercise: WorkoutPlanExerciseEntity)

    @Query("SELECT * FROM workout_plan_exercises WHERE planExerciseId = :planExerciseId")
    suspend fun getPlanExerciseById(planExerciseId: Long): WorkoutPlanExerciseEntity?

    /** Deletes one exercise from a custom plan and closes any gap left in its ordering. */
    @Transaction
    suspend fun deleteCustomPlanExercise(planExerciseId: Long) {
        val existing = getPlanExerciseById(planExerciseId) ?: return
        if (getPlanSourceForDay(existing.planDayId) != WorkoutPlanSource.CUSTOM) return

        deletePlanExercise(existing)
        reorderPlanExercises(
            planDayId = existing.planDayId,
            orderedPlanExerciseIds = getPlanExerciseIdsForDay(existing.planDayId)
        )
    }

    @Query("SELECT MAX(orderIndex) FROM workout_plan_exercises WHERE planDayId = :planDayId")
    suspend fun getMaxOrderIndexForDay(planDayId: Long): Int?

    @Query(
        """
        SELECT p.source
        FROM workout_plans p
        INNER JOIN workout_plan_days d ON d.planId = p.planId
        WHERE d.planDayId = :planDayId
        """
    )
    suspend fun getPlanSourceForDay(planDayId: Long): WorkoutPlanSource?

    @Query(
        """
        SELECT planExerciseId
        FROM workout_plan_exercises
        WHERE planDayId = :planDayId
        ORDER BY orderIndex
        """
    )
    suspend fun getPlanExerciseIdsForDay(planDayId: Long): List<Long>

    @Query(
        """
        UPDATE workout_plan_exercises
        SET orderIndex = :orderIndex
        WHERE planExerciseId = :planExerciseId AND planDayId = :planDayId
        """
    )
    suspend fun updatePlanExerciseOrderIndex(
        planExerciseId: Long,
        planDayId: Long,
        orderIndex: Int
    )

    /**
     * Reorders a complete day atomically. Indices are first moved to a temporary negative range
     * so swapping two rows cannot violate the unique (planDayId, orderIndex) index halfway through.
     */
    @Transaction
    suspend fun reorderPlanExercises(planDayId: Long, orderedPlanExerciseIds: List<Long>) {
        if (getPlanSourceForDay(planDayId) != WorkoutPlanSource.CUSTOM) return

        val existingIds = getPlanExerciseIdsForDay(planDayId)
        if (
            orderedPlanExerciseIds.size != existingIds.size ||
            orderedPlanExerciseIds.toSet() != existingIds.toSet()
        ) return

        orderedPlanExerciseIds.forEachIndexed { index, planExerciseId ->
            updatePlanExerciseOrderIndex(
                planExerciseId = planExerciseId,
                planDayId = planDayId,
                orderIndex = -index - 1
            )
        }
        orderedPlanExerciseIds.forEachIndexed { index, planExerciseId ->
            updatePlanExerciseOrderIndex(
                planExerciseId = planExerciseId,
                planDayId = planDayId,
                orderIndex = index
            )
        }
    }

    /** Every exercise across every day of a plan, pre-joined with its [com.example.homeworkout.data.local.entities.ExerciseEntity]. */
    @Query(
        """
        SELECT pe.planExerciseId, pe.planDayId, d.dayNumber, d.title AS dayTitle, pe.orderIndex,
               pe.targetReps, pe.targetDurationSec, pe.restAfterSec,
               e.exerciseId, e.title AS exerciseTitle, e.gifUrl AS exerciseGifUrl,
               (SELECT ei.imageUrl FROM exercise_images ei
                    WHERE ei.exerciseId = e.exerciseId
                    ORDER BY ei.orderIndex LIMIT 1) AS exerciseImageUrl,
               e.category AS exerciseCategory, e.level AS exerciseLevel
        FROM workout_plan_exercises pe
        INNER JOIN workout_plan_days d ON d.planDayId = pe.planDayId
        INNER JOIN exercises e ON e.exerciseId = pe.exerciseId
        WHERE d.planId = :planId
        ORDER BY d.dayNumber, pe.orderIndex
        """
    )
    fun observePlanExerciseRows(planId: Long): Flow<List<PlanDayExerciseRow>>

    // --- Custom plan creation ---

    /**
     * Creates a full multi-day custom plan — the plan row, each day, and each day's exercises —
     * as one atomic write (mirroring how
     * [com.example.homeworkout.data.local.seed.AppDatabaseSeeder] builds a system plan), so an
     * interrupted save never leaves a half-created plan visible to [observePlanSummaries].
     */
    @Transaction
    suspend fun insertCustomPlan(plan: WorkoutPlanEntity, days: List<NewPlanDay>): Long {
        val planId = insertPlan(plan)
        val allExercises = ArrayList<WorkoutPlanExerciseEntity>()
        days.forEach { day ->
            val dayId = insertPlanDay(WorkoutPlanDayEntity(planId = planId, dayNumber = day.dayNumber, title = day.title))
            day.exercises.forEachIndexed { index, exercise ->
                allExercises += exercise.copy(planDayId = dayId, orderIndex = index)
            }
        }
        insertPlanExercises(allExercises)
        return planId
    }
}
