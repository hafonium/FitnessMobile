package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room table row for `food_logs` — one saved Food Calorie Scanner result (docs/weight-forecast-feature.md).
 * The source photo is never persisted, matching the [FormCheckResultEntity] convention of keeping
 * only the structured result, not the source media. */
@Entity(
    tableName = "food_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "loggedAt"])]
)
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,
    val userId: Long,
    val category: String,
    val categoryProbability: Double,
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val recipesUsed: Int,
    val loggedAt: Long = System.currentTimeMillis()
)
