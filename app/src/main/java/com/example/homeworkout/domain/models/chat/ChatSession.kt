package com.example.homeworkout.domain.models.chat

/** One conversation with the in-app fitness assistant. [contextSummary] is the rolling summary
 * Gemini maintains across turns instead of replaying full message history on every request. */
data class ChatSession(
    val sessionId: Long,
    val title: String,
    val contextSummary: String,
    val createdAt: Long,
    val updatedAt: Long
)
