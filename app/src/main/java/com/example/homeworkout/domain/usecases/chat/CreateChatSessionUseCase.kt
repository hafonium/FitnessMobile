package com.example.homeworkout.domain.usecases.chat

import com.example.homeworkout.domain.repositories.ChatRepository

/** Creates a new, empty chat session and returns its id. */
class CreateChatSessionUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(): Long = chatRepository.createSession()
}
