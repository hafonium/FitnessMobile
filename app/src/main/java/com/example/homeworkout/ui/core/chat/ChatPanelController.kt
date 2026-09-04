package com.example.homeworkout.ui.core.chat

import com.example.homeworkout.domain.models.chat.PlanProposal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped (lives on [com.example.homeworkout.ui.App]) bridge between the floating [ChatOverlay]
 * — which has no reference to the nav graph — and
 * [com.example.homeworkout.ui.navigation.ScreenNavigator] — which owns the NavController but never
 * sees chat state. Two jobs: track whether the chat panel is expanded (so returning from a
 * chat-triggered screen can reopen it), and carry a one-shot [PlanProposal] from a chat turn to
 * the Create Workout screen. See docs/chatbot-feature.md.
 */
class ChatPanelController {
    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()
    fun open() { _isOpen.value = true }
    fun close() { _isOpen.value = false }

    private val _pendingPlanProposal = MutableStateFlow<PlanProposal?>(null)
    /** Observed by ScreenNavigator to know when to auto-navigate to Create Workout. */
    val pendingPlanProposal: StateFlow<PlanProposal?> = _pendingPlanProposal.asStateFlow()

    /** Called once the assistant decides the user wants a plan created. Collapses the panel so the
     *  destination screen isn't shown underneath it. */
    fun proposePlan(proposal: PlanProposal) {
        _pendingPlanProposal.value = proposal
        close()
    }

    /** Called by the Create Workout screen exactly once, when it first composes, to claim (and
     *  clear) a pending proposal — a manual "+ Create Workout" entry sees null and behaves as
     *  before. */
    fun consumePendingPlanProposal(): PlanProposal? {
        val proposal = _pendingPlanProposal.value
        _pendingPlanProposal.value = null
        return proposal
    }
}
