package com.example.homeworkout.domain.usecases.chat

import com.example.homeworkout.domain.models.chat.ChatSession
import com.example.homeworkout.domain.repositories.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetChatSessionsUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<ChatSession>> = chatRepository.observeSessions()
}
