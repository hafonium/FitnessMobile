package com.example.homeworkout.ui.core.foodscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.FoodAnalysis
import com.example.homeworkout.domain.usecases.food.AnalyzeFoodImageUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FoodScanUiState {
    data object Idle : FoodScanUiState
    data object Loading : FoodScanUiState
    data class Success(val analysis: FoodAnalysis) : FoodScanUiState
    data class Error(val message: String) : FoodScanUiState
}

class FoodScanViewModel(
    private val analyzeFoodImageUseCase: AnalyzeFoodImageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<FoodScanUiState>(FoodScanUiState.Idle)
    val uiState: StateFlow<FoodScanUiState> = _uiState.asStateFlow()

    fun analyze(image: ByteArray) {
        if (_uiState.value is FoodScanUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = FoodScanUiState.Loading
            _uiState.value = try {
                FoodScanUiState.Success(analyzeFoodImageUseCase(image))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                FoodScanUiState.Error(error.message ?: "Could not analyze this image. Please try again.")
            }
        }
    }

    fun clearResult() {
        if (_uiState.value !is FoodScanUiState.Loading) {
            _uiState.value = FoodScanUiState.Idle
        }
    }
}
