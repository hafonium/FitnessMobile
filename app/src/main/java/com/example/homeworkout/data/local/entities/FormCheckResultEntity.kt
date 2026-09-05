package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.homeworkout.domain.models.enums.FormCheckStatus

/** Room table row for `form_check_results` — a saved AI Video Form Check result ("Save to
 * History" CTA). `observationsJson` stores the joint-checkpoint breakdown as a JSON array rather
 * than a child table: it's a small, fixed-shape blob that is always read back whole with its
 * parent row, never queried on its own. */
@Entity(
    tableName = "form_check_results",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "analyzedAt"])]
)
data class FormCheckResultEntity(
    @PrimaryKey(autoGenerate = true)
    val resultId: Long = 0,
    val userId: Long,
    val exerciseName: String,
    val score: Int,
    val status: FormCheckStatus,
    val observationsJson: String,
    val primaryCorrectionTip: String,
    val recordingTip: String,
    val analyzedAt: Long
)
