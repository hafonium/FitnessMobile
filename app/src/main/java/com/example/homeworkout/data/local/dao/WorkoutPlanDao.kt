package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.homeworkout.data.local.dao.relations.PlanDayExerciseRow
import com.example.homeworkout.data.local.dao.relations.WorkoutPlanSummaryRow
import com.example.homeworkout.data.local.entities.WorkoutPlanDayEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanEntity
import com.example.homeworkout.data.local.entities.WorkoutPlanExerciseEntity
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT MAX(orderIndex) FROM workout_plan_exercises WHERE planDayId = :planDayId")
    suspend fun getMaxOrderIndexForDay(planDayId: Long): Int?

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
}
