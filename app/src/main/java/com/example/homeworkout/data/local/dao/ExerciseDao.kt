package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.homeworkout.data.local.dao.relations.ExerciseListRow
import com.example.homeworkout.data.local.dao.relations.ExerciseRefRow
import com.example.homeworkout.data.local.entities.EquipmentTypeEntity
import com.example.homeworkout.data.local.entities.ExerciseEntity
import com.example.homeworkout.data.local.entities.ExerciseImageEntity
import com.example.homeworkout.data.local.entities.ExerciseInstructionStepEntity
import com.example.homeworkout.data.local.entities.ExerciseMuscleEntity
import com.example.homeworkout.data.local.entities.MuscleEntity
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseLevel
import com.example.homeworkout.domain.models.enums.MuscleRole
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    // --- Lookup tables (equipment / muscles) ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEquipmentTypes(equipment: List<EquipmentTypeEntity>): List<Long>

    @Query("SELECT * FROM equipment_types ORDER BY name")
    fun observeEquipmentTypes(): Flow<List<EquipmentTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMuscles(muscles: List<MuscleEntity>): List<Long>

    @Query("SELECT * FROM muscles ORDER BY name")
    fun observeMuscles(): Flow<List<MuscleEntity>>

    // --- Exercises ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseMuscles(crossRefs: List<ExerciseMuscleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructionSteps(steps: List<ExerciseInstructionStepEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseImages(images: List<ExerciseImageEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int

    /** Compact projection of the whole active library for the plan seeder. */
    @Query(
        """
        SELECT e.exerciseId, e.category, e.level, e.force, eq.name AS equipmentName,
               COALESCE(GROUP_CONCAT(m.name, ','), '') AS primaryMusclesCsv
        FROM exercises e
        INNER JOIN equipment_types eq ON eq.equipmentId = e.equipmentId
        LEFT JOIN exercise_muscles em ON em.exerciseId = e.exerciseId AND em.role = 'PRIMARY'
        LEFT JOIN muscles m ON m.muscleId = em.muscleId
        WHERE e.isActive = 1
        GROUP BY e.exerciseId
        """
    )
    suspend fun getAllExerciseRefs(): List<ExerciseRefRow>

    @Query("SELECT * FROM exercises WHERE title = :title LIMIT 1")
    suspend fun getExerciseByTitle(title: String): ExerciseEntity?

    @Query(
        """
        SELECT e.exerciseId, e.title, e.gifUrl, e.category, e.level, e.force, eq.name AS equipmentName
        FROM exercises e
        INNER JOIN equipment_types eq ON eq.equipmentId = e.equipmentId
        WHERE e.title = :title LIMIT 1
        """
    )
    suspend fun getExerciseRowByTitle(title: String): ExerciseListRow?

    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId")
    suspend fun getExerciseById(exerciseId: Long): ExerciseEntity?

    @Query(
        """
        SELECT e.exerciseId, e.title, e.gifUrl, e.category, e.level, e.force, eq.name AS equipmentName
        FROM exercises e
        INNER JOIN equipment_types eq ON eq.equipmentId = e.equipmentId
        WHERE e.exerciseId = :exerciseId
        """
    )
    suspend fun getExerciseRowById(exerciseId: Long): ExerciseListRow?

    /** Batch lookup for the Custom Workout builder, resolving picked exercise ids back to display rows. */
    @Query(
        """
        SELECT e.exerciseId, e.title, e.gifUrl, e.category, e.level, e.force, eq.name AS equipmentName
        FROM exercises e
        INNER JOIN equipment_types eq ON eq.equipmentId = e.equipmentId
        WHERE e.exerciseId IN (:exerciseIds)
        """
    )
    suspend fun getExerciseRowsByIds(exerciseIds: List<Long>): List<ExerciseListRow>

    @Query("SELECT * FROM exercises WHERE isActive = 1 ORDER BY title")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    /**
     * Filters the exercise library for the Filter Exercise / Alter Workout Exercise / Add
     * Exercises screens, pre-joined with its equipment name. Any parameter left null is not applied.
     */
    @Query(
        """
        SELECT e.exerciseId, e.title, e.gifUrl, e.category, e.level, e.force, eq.name AS equipmentName
        FROM exercises e
        INNER JOIN equipment_types eq ON eq.equipmentId = e.equipmentId
        WHERE e.isActive = 1
        AND (:category IS NULL OR e.category = :category)
        AND (:level IS NULL OR e.level = :level)
        AND (:equipmentName IS NULL OR eq.name = :equipmentName)
        AND (:query IS NULL OR e.title LIKE '%' || :query || '%')
        ORDER BY e.title
        """
    )
    fun searchExercises(
        category: ExerciseCategory?,
        level: ExerciseLevel?,
        equipmentName: String?,
        query: String?
    ): Flow<List<ExerciseListRow>>

    @Query("SELECT * FROM exercise_muscles WHERE exerciseId = :exerciseId")
    suspend fun getMusclesForExercise(exerciseId: Long): List<ExerciseMuscleEntity>

    @Query(
        """
        SELECT m.name FROM muscles m
        INNER JOIN exercise_muscles em ON em.muscleId = m.muscleId
        WHERE em.exerciseId = :exerciseId AND em.role = :role
        ORDER BY m.name
        """
    )
    suspend fun getMuscleNamesForExercise(
        exerciseId: Long,
        role: MuscleRole
    ): List<String>

    @Query("SELECT * FROM exercise_instruction_steps WHERE exerciseId = :exerciseId ORDER BY stepNumber")
    suspend fun getInstructionSteps(exerciseId: Long): List<ExerciseInstructionStepEntity>

    @Query("SELECT * FROM exercise_images WHERE exerciseId = :exerciseId ORDER BY orderIndex")
    suspend fun getExerciseImages(exerciseId: Long): List<ExerciseImageEntity>
}
