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
import kotlinx.coroutines.flow.map

data class FilterState(
    val bodyParts: Set<String> = emptySet(),
    val difficulty: String? = null,
    val types: Set<String> = emptySet(),
    val equipment: Set<String> = emptySet()
)

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

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _selectedExerciseIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedExerciseIds: StateFlow<Set<Long>> = _selectedExerciseIds.asStateFlow()

    fun toggleSelection(exerciseId: Long) {
        val current = _selectedExerciseIds.value
        if (exerciseId in current) {
            _selectedExerciseIds.value = current - exerciseId
        } else {
            _selectedExerciseIds.value = current + exerciseId
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises: StateFlow<List<Exercise>> = combine(_query, _category, _filterState) { query, category, filter ->
        Triple(query, category, filter)
    }
        .flatMapLatest { (query, category, filter) ->
            searchExercisesUseCase(category = category, query = query.ifBlank { null })
                .map { list ->
                    list.filter { exercise ->
                        val matchDifficulty = filter.difficulty == null || when (filter.difficulty.lowercase()) {
                            "easy" -> exercise.level.name == "BEGINNER"
                            "medium" -> exercise.level.name == "INTERMEDIATE"
                            "hard" -> exercise.level.name == "EXPERT"
                            else -> false
                        }
                        
                        val matchEquipment = filter.equipment.isEmpty() || filter.equipment.any {
                            val target = if (it.equals("No equipment", ignoreCase = true)) "bodyweight" else it
                            target.equals(exercise.equipmentName, ignoreCase = true)
                        }
                        
                        val matchBodyPart = filter.bodyParts.isEmpty() || filter.bodyParts.any { bp ->
                            val catStr = exercise.category.name
                            when (bp.lowercase()) {
                                "back" -> catStr == "BACK_PULL"
                                "arm" -> catStr == "ARMS_SHOULDERS"
                                "leg", "glutes" -> catStr == "LEGS_GLUTES"
                                "shoulder" -> catStr == "ARMS_SHOULDERS"
                                "chest" -> catStr == "CHEST_PUSH"
                                "core", "abs" -> catStr == "ABS_CORE"
                                else -> false
                            }
                        }
                        
                        val matchType = filter.types.isEmpty() || filter.types.any { type ->
                            val catStr = exercise.category.name
                            when (type.lowercase()) {
                                "warm up" -> catStr == "CARDIO_HIIT" || catStr == "GENERAL_FITNESS"
                                "stretch" -> catStr == "STRETCHING"
                                "training" -> catStr == "GENERAL_FITNESS" || catStr == "CARDIO_HIIT" || catStr == "BACK_PULL" || catStr == "CHEST_PUSH" || catStr == "ARMS_SHOULDERS" || catStr == "LEGS_GLUTES" || catStr == "ABS_CORE"
                                else -> false
                            }
                        }
                        
                        matchDifficulty && matchEquipment && matchBodyPart && matchType
                    }
                }
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setCategory(value: ExerciseCategory?) {
        _category.value = value
    }
    
    fun setFilterState(state: FilterState) {
        _filterState.value = state
    }
}
