package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseForce
import com.example.homeworkout.domain.models.enums.ExerciseLevel

/**
 * Room table row for `exercises`. Maps data/data.json fields id, name, originalName, category,
 * equipment, level and force — the (source, externalExerciseId) pair is the stable import key
 * used by the seeder to avoid duplicate inserts on re-seed.
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentTypeEntity::class,
            parentColumns = ["equipmentId"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["source", "externalExerciseId"], unique = true),
        Index(value = ["equipmentId"])
    ]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Long = 0,
    val source: String = "free-exercise-db",
    val externalExerciseId: String,
    val title: String,
    val originalName: String,
    val gifUrl: String? = null,
    val category: ExerciseCategory,
    val equipmentId: Long,
    val level: ExerciseLevel,
    val force: ExerciseForce,
    val isActive: Boolean = true,
    val syncedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
