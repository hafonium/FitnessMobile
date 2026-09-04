package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.homeworkout.data.local.entities.ChatMessageEntity
import com.example.homeworkout.data.local.entities.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeSessions(userId: Long): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: Long): ChatSessionEntity?

    @Query("UPDATE chat_sessions SET title = :title WHERE sessionId = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, title: String)

    @Query("UPDATE chat_sessions SET contextSummary = :contextSummary, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateSessionContext(sessionId: Long, contextSummary: String, updatedAt: Long)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>>
}
