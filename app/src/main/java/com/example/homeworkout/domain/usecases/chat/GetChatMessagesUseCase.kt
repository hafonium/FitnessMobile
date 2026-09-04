package com.example.homeworkout.domain.usecases.chat

import com.example.homeworkout.domain.models.chat.ChatMessage
import com.example.homeworkout.domain.repositories.ChatRepository
import kotlinx.coroutines.flow.Flow

class GetChatMessagesUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(sessionId: Long): Flow<List<ChatMessage>> = chatRepository.observeMessages(sessionId)
}
