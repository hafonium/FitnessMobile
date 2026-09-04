package com.example.homeworkout.domain.usecases.chat

import com.example.homeworkout.domain.repositories.ChatRepository

class SendChatMessageUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(sessionId: Long, text: String) {
        chatRepository.sendMessage(sessionId, text)
    }
}
