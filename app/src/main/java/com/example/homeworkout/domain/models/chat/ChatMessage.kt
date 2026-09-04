package com.example.homeworkout.domain.models.chat

import com.example.homeworkout.domain.models.enums.ChatMessageRole

data class ChatMessage(
    val messageId: Long,
    val sessionId: Long,
    val role: ChatMessageRole,
    val content: String,
    val timestamp: Long
)
