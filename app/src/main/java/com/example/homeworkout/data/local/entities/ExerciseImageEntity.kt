package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room table row for `exercise_images`. Maps the `imageUrls` array in data/data.json in order;
 * every supplied exercise currently has exactly two HTTPS images.
 */
@Entity(
    tableName = "exercise_images",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["exerciseId", "orderIndex"], unique = true),
        Index(value = ["exerciseId", "imageUrl"], unique = true)
    ]
)
data class ExerciseImageEntity(
    @PrimaryKey(autoGenerate = true)
    val imageId: Long = 0,
    val exerciseId: Long,
    val orderIndex: Int,
    val imageUrl: String
)
