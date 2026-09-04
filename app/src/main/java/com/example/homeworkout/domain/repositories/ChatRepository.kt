package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.chat.ChatMessage
import com.example.homeworkout.domain.models.chat.ChatSession
import com.example.homeworkout.domain.models.chat.PlanProposal
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeSessions(): Flow<List<ChatSession>>
    fun observeMessages(sessionId: Long): Flow<List<ChatMessage>>
    suspend fun createSession(): Long

    /** Persists the user's message, calls Groq with (contextSummary, text) plus a snapshot of the
     * user's fitness profile/plans, then persists the reply and updated context. On failure,
     * persists a friendly in-thread error message instead of throwing — callers observe the result
     * via [observeMessages]. Returns a [PlanProposal] when the assistant decided the user wants a
     * workout plan created, so the caller can navigate to the Create Workout screen; null
     * otherwise (including on failure). */
    suspend fun sendMessage(sessionId: Long, text: String): PlanProposal?

    suspend fun deleteSession(sessionId: Long)
}
