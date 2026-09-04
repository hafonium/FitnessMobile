package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.chat.ChatMessage
import com.example.homeworkout.domain.models.chat.ChatSession
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeSessions(): Flow<List<ChatSession>>
    fun observeMessages(sessionId: Long): Flow<List<ChatMessage>>
    suspend fun createSession(): Long

    /** Persists the user's message, calls Gemini with (contextSummary, text), then persists the
     * reply and updated context. On failure, persists a friendly in-thread error message instead
     * of throwing — callers observe the result via [observeMessages]. */
    suspend fun sendMessage(sessionId: Long, text: String)

    suspend fun deleteSession(sessionId: Long)
}
