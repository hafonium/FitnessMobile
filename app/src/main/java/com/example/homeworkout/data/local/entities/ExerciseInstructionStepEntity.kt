package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room table row for `exercise_instruction_steps`. Maps the `instructions` array in
 * data/data.json in order; four records in the supplied dataset have no instruction rows.
 */
@Entity(
    tableName = "exercise_instruction_steps",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseId", "stepNumber"], unique = true)]
)
data class ExerciseInstructionStepEntity(
    @PrimaryKey(autoGenerate = true)
    val instructionId: Long = 0,
    val exerciseId: Long,
    val stepNumber: Int,
    val instructionText: String
)
