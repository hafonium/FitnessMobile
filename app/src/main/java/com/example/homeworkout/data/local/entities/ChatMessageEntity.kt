package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.homeworkout.domain.models.enums.ChatMessageRole

/** Room table row for `chat_messages` — one row per message in a [ChatSessionEntity], oldest
 * first. `role` mirrors Gemini's own request vocabulary (see [ChatMessageRole]). */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val messageId: Long = 0,
    val sessionId: Long,
    val role: ChatMessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
