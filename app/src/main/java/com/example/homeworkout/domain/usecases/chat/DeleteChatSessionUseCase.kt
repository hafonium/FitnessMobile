package com.example.homeworkout.domain.usecases.chat

import com.example.homeworkout.domain.repositories.ChatRepository

class DeleteChatSessionUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(sessionId: Long) {
        chatRepository.deleteSession(sessionId)
    }
}
