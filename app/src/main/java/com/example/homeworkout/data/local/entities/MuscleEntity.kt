package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room table row for `muscles` — one row per distinct primary/secondary muscle name found in
 * data/data.json (abdominals, quadriceps, shoulders, etc.).
 */
@Entity(tableName = "muscles", indices = [Index(value = ["name"], unique = true)])
data class MuscleEntity(
    @PrimaryKey(autoGenerate = true)
    val muscleId: Long = 0,
    val name: String
)
