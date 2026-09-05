package com.example.homeworkout.ui.core.formcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.FormAnalysis
import com.example.homeworkout.domain.usecases.formcheck.GetFormCheckHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface FormCheckHistoryUiState {
    data object Loading : FormCheckHistoryUiState
    data class Loaded(val results: List<FormAnalysis>) : FormCheckHistoryUiState
    data class Error(val message: String) : FormCheckHistoryUiState
}

class FormCheckHistoryViewModel(
    getFormCheckHistoryUseCase: GetFormCheckHistoryUseCase
) : ViewModel() {
    val uiState: StateFlow<FormCheckHistoryUiState> = getFormCheckHistoryUseCase()
        .map<List<FormAnalysis>, FormCheckHistoryUiState> { FormCheckHistoryUiState.Loaded(it) }
        .catch { error ->
            emit(FormCheckHistoryUiState.Error(error.message ?: "Unable to load saved form checks."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FormCheckHistoryUiState.Loading
        )
}
