package com.example.homeworkout.ui.core.running.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.running.RunCoordinate
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.running.EncodedPolylineCodec
import com.example.homeworkout.domain.usecases.running.GetRunDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RunDetailUiState {
    data object Loading : RunDetailUiState
    data class Success(val session: RunSession, val routeSegments: List<List<RunCoordinate>>) : RunDetailUiState
    data object NotFound : RunDetailUiState
}

class RunDetailViewModel(
    private val sessionId: Long,
    private val getRunDetail: GetRunDetailUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<RunDetailUiState>(RunDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = getRunDetail(sessionId)
            _uiState.value = if (session == null) {
                RunDetailUiState.NotFound
            } else {
                val decoded = runCatching {
                    session.encodedPolyline?.takeIf { it.isNotBlank() }?.let(EncodedPolylineCodec::decode)
                }.getOrNull()?.filter { it.isNotEmpty() }
                val fallback = session.points.groupBy { it.segmentIndex }.toSortedMap().values.map { points ->
                    points.map { RunCoordinate(it.latitude, it.longitude) }
                }.filter { it.isNotEmpty() }
                val routeSegments = if (!decoded.isNullOrEmpty()) decoded else fallback
                RunDetailUiState.Success(session, routeSegments)
            }
        }
    }
}
