package com.example.homeworkout.domain.usecases.chat

import com.example.homeworkout.domain.models.chat.PlanProposal
import com.example.homeworkout.domain.repositories.ChatRepository

class SendChatMessageUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(sessionId: Long, text: String): PlanProposal? =
        chatRepository.sendMessage(sessionId, text)
}
