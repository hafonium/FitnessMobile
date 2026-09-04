package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room table row for `chat_sessions` — one row per chat conversation the user has with the
 * in-app Gemini-backed fitness assistant. [contextSummary] is the rolling conversation summary
 * (empty for a brand-new session) sent back to Gemini on the next message instead of full
 * message history — see docs/chatbot-feature.md. */
@Entity(
    tableName = "chat_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    val userId: Long,
    val title: String,
    val contextSummary: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
