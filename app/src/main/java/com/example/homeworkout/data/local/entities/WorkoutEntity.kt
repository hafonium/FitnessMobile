package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table row for a single workout. Mirrors [com.example.homeworkout.domain.models.WorkoutModel]
 * one field at a time — mapping between the two happens in data/repositories/WorkoutRepositoryImpl.
 */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val imageUrl: String?,
    val description: String
)
