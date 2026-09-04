package com.example.homeworkout.ui.core.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.chat.ChatMessage
import com.example.homeworkout.domain.models.chat.ChatSession
import com.example.homeworkout.domain.usecases.chat.CreateChatSessionUseCase
import com.example.homeworkout.domain.usecases.chat.DeleteChatSessionUseCase
import com.example.homeworkout.domain.usecases.chat.GetChatMessagesUseCase
import com.example.homeworkout.domain.usecases.chat.GetChatSessionsUseCase
import com.example.homeworkout.domain.usecases.chat.SendChatMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the floating chat bubble + popup (see docs/chatbot-feature.md). Lives above the nav
 * graph in HomeWorkoutApp, not inside a screen destination, since the bubble is a global overlay. */
class ChatViewModel(
    private val getChatSessionsUseCase: GetChatSessionsUseCase,
    private val getChatMessagesUseCase: GetChatMessagesUseCase,
    private val createChatSessionUseCase: CreateChatSessionUseCase,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val deleteChatSessionUseCase: DeleteChatSessionUseCase,
    private val chatPanelController: ChatPanelController
) : ViewModel() {

    val sessions: StateFlow<List<ChatSession>> = getChatSessionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList()) else getChatMessagesUseCase(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun openSession(sessionId: Long) {
        _activeSessionId.value = sessionId
    }

    fun closeActiveSession() {
        _activeSessionId.value = null
    }

    fun startNewSession() {
        viewModelScope.launch {
            _activeSessionId.value = createChatSessionUseCase()
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _activeSessionId.value ?: return
        if (text.isBlank() || _isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                sendChatMessageUseCase(sessionId, text)?.let { chatPanelController.proposePlan(it) }
            } finally {
                _isSending.value = false
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            deleteChatSessionUseCase(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
            }
        }
    }
}
