package com.example.homeworkout.ui.core.planedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.Exercise
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.usecases.exercises.SearchExercisesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Backs both the Add Exercises and Alter Workout Exercise screens — a search box plus an
 * optional body-focus category, over the real exercise library.
 */
class ExerciseBrowserViewModel(
    private val searchExercisesUseCase: SearchExercisesUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow<ExerciseCategory?>(null)
    val category: StateFlow<ExerciseCategory?> = _category.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises: StateFlow<List<Exercise>> = combine(_query, _category) { query, category -> query to category }
        .flatMapLatest { (query, category) -> searchExercisesUseCase(category = category, query = query.ifBlank { null }) }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setCategory(value: ExerciseCategory?) {
        _category.value = value
    }
}
