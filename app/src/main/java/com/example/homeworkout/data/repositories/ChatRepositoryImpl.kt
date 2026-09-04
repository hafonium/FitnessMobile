package com.example.homeworkout.data.repositories

import android.util.Log
import com.example.homeworkout.data.local.dao.ChatDao
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.entities.ChatMessageEntity
import com.example.homeworkout.data.local.entities.ChatSessionEntity
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.data.remote.groq.GroqClient
import com.example.homeworkout.domain.models.chat.ChatMessage
import com.example.homeworkout.domain.models.chat.ChatSession
import com.example.homeworkout.domain.models.enums.ChatMessageRole
import com.example.homeworkout.domain.repositories.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val groqClient: GroqClient
) : ChatRepository {

    /** This app has a single local user; resolve (or lazily create) it by its seeded email. */
    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSessions(): Flow<List<ChatSession>> = flow { emit(currentUserId()) }
        .flatMapLatest { userId ->
            chatDao.observeSessions(userId).map { entities -> entities.map { it.toDomain() } }
        }

    override fun observeMessages(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.observeMessages(sessionId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createSession(): Long {
        val now = System.currentTimeMillis()
        return chatDao.insertSession(
            ChatSessionEntity(
                userId = currentUserId(),
                title = DEFAULT_SESSION_TITLE,
                contextSummary = "",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun sendMessage(sessionId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        chatDao.insertMessage(
            ChatMessageEntity(sessionId = sessionId, role = ChatMessageRole.USER, content = trimmed, timestamp = System.currentTimeMillis())
        )

        val session = chatDao.getSession(sessionId)
        if (session != null && session.title == DEFAULT_SESSION_TITLE) {
            chatDao.updateSessionTitle(sessionId, trimmed.take(TITLE_PREVIEW_LENGTH))
        }

        try {
            val result = groqClient.sendMessage(session?.contextSummary.orEmpty(), trimmed)
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = ChatMessageRole.MODEL,
                    content = result.reply,
                    timestamp = System.currentTimeMillis()
                )
            )
            chatDao.updateSessionContext(sessionId, result.updatedContext, System.currentTimeMillis())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e("ChatRepository", "Groq request failed", error)
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = ChatMessageRole.MODEL,
                    content = FALLBACK_ERROR_MESSAGE,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    companion object {
        private const val DEFAULT_SESSION_TITLE = "New chat"
        private const val TITLE_PREVIEW_LENGTH = 40
        private const val FALLBACK_ERROR_MESSAGE =
            "Sorry, I couldn't reach the assistant just now. Please check your connection and try again."
    }
}

private fun ChatSessionEntity.toDomain(): ChatSession = ChatSession(
    sessionId = sessionId,
    title = title,
    contextSummary = contextSummary,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    messageId = messageId,
    sessionId = sessionId,
    role = role,
    content = content,
    timestamp = timestamp
)
