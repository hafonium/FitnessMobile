package com.example.homeworkout.domain.models.enums

/** Labels who sent a stored chat message, for UI bubble styling and ordering. This is a local
 * storage label only, decoupled from whatever wire vocabulary the current LLM provider's API
 * uses (see docs/chatbot-feature.md for which provider is wired up) - requests to it carry a
 * rolling context summary rather than replaying these stored messages verbatim. */
enum class ChatMessageRole {
    USER,
    MODEL
}
