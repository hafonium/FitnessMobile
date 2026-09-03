package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Room table row for `user_fitness_profiles` — the saved onboarding answers plus the plan that was
 * recommended from them (docs/workout_plan_selection_guide.md §5).
 *
 * Multi-value answers (equipment, focus categories/muscles) are stored here as comma-separated
 * strings rather than the normalized `user_available_equipment` / `user_focus_preferences` tables
 * the guide suggests — a deliberate simplification for this single-user local app.
 */
@Entity(
    tableName = "user_fitness_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserFitnessProfileEntity(
    @PrimaryKey
    val userId: Long,
    val primaryGoal: String,
    val experienceLevel: String,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val availableEquipmentCsv: String,
    val focusCategoriesCsv: String,
    val focusMusclesCsv: String,
    val injuriesOrLimitations: String,
    val recommendedCatalogId: String? = null,
    val catalogVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
