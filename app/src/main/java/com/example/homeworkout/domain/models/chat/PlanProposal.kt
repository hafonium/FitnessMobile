package com.example.homeworkout.domain.models.chat

import com.example.homeworkout.domain.models.FitnessProfile

/**
 * A workout plan the assistant proposes to create, extracted from a chat turn once enough
 * information has been gathered — see docs/chatbot-feature.md. Carried by
 * [com.example.homeworkout.ui.core.chat.ChatPanelController] to trigger navigation to the Create
 * Workout screen, which seeds its draft from [profile] via the app's existing plan recommender
 * ([com.example.homeworkout.domain.usecases.planselection.RecommendPlanUseCase]).
 */
data class PlanProposal(
    val profile: FitnessProfile,
    val suggestedTitle: String,
    val suggestedDescription: String
)
