package com.example.homeworkout.ui.core.formcheck

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.models.enums.FormCheckExercise
import com.example.homeworkout.domain.usecases.formcheck.AnalyzeFormVideoUseCase
import com.example.homeworkout.domain.usecases.formcheck.SaveFormCheckResultUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FormCheckUiState {
    data object Idle : FormCheckUiState
    data object Loading : FormCheckUiState
    data class Success(val analysis: FormAnalysis, val saved: Boolean = false) : FormCheckUiState
    data class Error(val message: String) : FormCheckUiState
}

/** Backs the isolated Record -> Process -> Display Result flow for AI Video Form Check - see
 * docs/form-check-feature.md. Deliberately has no chat/message-list state. */
class FormCheckViewModel(
    private val analyzeFormVideoUseCase: AnalyzeFormVideoUseCase,
    private val saveFormCheckResultUseCase: SaveFormCheckResultUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<FormCheckUiState>(FormCheckUiState.Idle)
    val uiState: StateFlow<FormCheckUiState> = _uiState.asStateFlow()

    fun analyze(frames: List<ByteArray>, exerciseHint: FormCheckExercise) {
        if (_uiState.value is FormCheckUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = FormCheckUiState.Loading
            _uiState.value = try {
                FormCheckUiState.Success(analyzeFormVideoUseCase(frames, exerciseHint))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Form analysis failed", error)
                FormCheckUiState.Error(error.message ?: "Could not analyze this video. Please try again.")
            }
        }
    }

    fun saveToHistory() {
        val state = _uiState.value
        if (state !is FormCheckUiState.Success || state.saved) return
        viewModelScope.launch {
            runCatching { saveFormCheckResultUseCase(state.analysis) }
                .onSuccess {
                    if (_uiState.value == state) {
                        _uiState.value = state.copy(saved = true)
                    }
                }
        }
    }

    /** Backs "Re-test Form" - clears the result so the capture sheet is shown again. */
    fun reset() {
        if (_uiState.value !is FormCheckUiState.Loading) {
            _uiState.value = FormCheckUiState.Idle
        }
    }

    private companion object {
        const val TAG = "FormCheckViewModel"
    }
}
