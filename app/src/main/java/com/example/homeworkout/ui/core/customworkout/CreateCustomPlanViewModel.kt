package com.example.homeworkout.ui.core.customworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.CustomDaySpec
import com.example.homeworkout.domain.models.CustomExerciseSpec
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.usecases.customworkout.CreateCustomWorkoutPlanUseCase
import com.example.homeworkout.domain.usecases.customworkout.GetExercisesByIdsUseCase
import com.example.homeworkout.domain.usecases.details.GetWorkoutDetailsUseCase
import com.example.homeworkout.domain.usecases.home.GetWorkoutsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One exercise already added to a [DraftDay] — enough to render a row without a DB round trip. */
data class DraftExercise(
    val exerciseId: Long,
    val title: String,
    val imageUrl: String?,
    val targetReps: Int?,
    val targetDurationSec: Int? = null,
    val restAfterSec: Int? = 15
)

/** One day of the plan being built. [localId] is a stable Compose key — real day numbers are
 *  assigned by position only once the plan is actually saved. */
data class DraftDay(
    val localId: Int,
    val title: String = "",
    val exercises: List<DraftExercise> = emptyList()
)

data class CustomPlanForm(
    val title: String = "",
    val description: String = "",
    val category: WorkoutCategory = WorkoutCategory.BUILD_MUSCLE,
    val level: WorkoutLevel = WorkoutLevel.BEGINNER,
    val days: List<DraftDay> = listOf(DraftDay(localId = 0))
) {
    /** A title plus at least one day that actually has an exercise in it — an empty trailing day
     *  the user never filled in shouldn't block (or get persisted by) save. */
    val canSave: Boolean
        get() = title.isNotBlank() && days.any { it.exercises.isNotEmpty() }
}

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    data class Saved(val planId: Long) : SaveState()
}

/**
 * Backs "Create Workout": builds a multi-day plan entirely in memory — the same plan -> days ->
 * exercises shape as a real [com.example.homeworkout.domain.models.WorkoutPlanDetail] — and only
 * writes it to `workout_plans` (source = CUSTOM) once, when [save] is called. Nothing is persisted
 * if the user backs out.
 */
class CreateCustomPlanViewModel(
    private val getExercisesByIdsUseCase: GetExercisesByIdsUseCase,
    private val createCustomWorkoutPlanUseCase: CreateCustomWorkoutPlanUseCase,
    getWorkoutsUseCase: GetWorkoutsUseCase,
    private val getWorkoutDetailsUseCase: GetWorkoutDetailsUseCase
) : ViewModel() {

    val templates: StateFlow<List<WorkoutModel>> = getWorkoutsUseCase(source = WorkoutPlanSource.SYSTEM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isImportingTemplate = MutableStateFlow(false)
    val isImportingTemplate: StateFlow<Boolean> = _isImportingTemplate.asStateFlow()

    private val _form = MutableStateFlow(CustomPlanForm())
    val form: StateFlow<CustomPlanForm> = _form.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private var nextDayLocalId = 1

    fun setTitle(value: String) = _form.update { it.copy(title = value) }
    fun setDescription(value: String) = _form.update { it.copy(description = value) }
    fun setCategory(value: WorkoutCategory) = _form.update { it.copy(category = value) }
    fun setLevel(value: WorkoutLevel) = _form.update { it.copy(level = value) }

    fun addDay() {
        _form.update { it.copy(days = it.days + DraftDay(localId = nextDayLocalId++)) }
    }

    /** Always leaves at least one day — a plan can't save with zero days to begin with. */
    fun removeDay(localId: Int) {
        _form.update { form ->
            if (form.days.size <= 1) form else form.copy(days = form.days.filterNot { it.localId == localId })
        }
    }

    fun setDayTitle(localId: Int, title: String) {
        _form.update { form ->
            form.copy(days = form.days.map { if (it.localId == localId) it.copy(title = title) else it })
        }
    }

    fun removeExercise(dayLocalId: Int, exerciseId: Long) {
        _form.update { form ->
            form.copy(days = form.days.map { day ->
                if (day.localId != dayLocalId) day else day.copy(exercises = day.exercises.filterNot { it.exerciseId == exerciseId })
            })
        }
    }

    fun updateReps(dayLocalId: Int, exerciseId: Long, reps: Int) {
        if (reps < 1) return
        _form.update { form ->
            form.copy(days = form.days.map { day ->
                if (day.localId != dayLocalId) day else day.copy(
                    exercises = day.exercises.map { if (it.exerciseId == exerciseId) it.copy(targetReps = reps) else it }
                )
            })
        }
    }

    fun updateDuration(dayLocalId: Int, exerciseId: Long, durationSec: Int) {
        if (durationSec < 5) return
        _form.update { form ->
            form.copy(days = form.days.map { day ->
                if (day.localId != dayLocalId) day else day.copy(
                    exercises = day.exercises.map {
                        if (it.exerciseId == exerciseId) it.copy(targetDurationSec = durationSec) else it
                    }
                )
            })
        }
    }

    /** Copies a system template into the unsaved draft. The source plan is never modified. */
    fun importTemplate(templatePlanId: Long) {
        if (_isImportingTemplate.value) return
        _isImportingTemplate.value = true
        viewModelScope.launch {
            try {
                val detail = getWorkoutDetailsUseCase(templatePlanId).first() ?: return@launch
                if (detail.plan.source != WorkoutPlanSource.SYSTEM) return@launch
                val draftDays = detail.days.mapIndexed { index, day ->
                    DraftDay(
                        localId = index,
                        title = day.title.orEmpty(),
                        exercises = day.exercises.map { exercise ->
                            DraftExercise(
                                exerciseId = exercise.exerciseId,
                                title = exercise.title,
                                imageUrl = exercise.gifUrl ?: exercise.imageUrl,
                                targetReps = exercise.targetReps,
                                targetDurationSec = exercise.targetDurationSec,
                                restAfterSec = exercise.restAfterSec
                            )
                        }
                    )
                }.ifEmpty { listOf(DraftDay(localId = 0)) }
                _form.value = CustomPlanForm(
                    title = "${detail.plan.title} Copy",
                    description = detail.plan.description.orEmpty(),
                    category = detail.plan.category,
                    level = detail.plan.level,
                    days = draftDays
                )
                nextDayLocalId = draftDays.maxOf { it.localId } + 1
            } finally {
                _isImportingTemplate.value = false
            }
        }
    }

    /** Resolves picked ids from the Add Exercises browser and appends any not already on this day. */
    fun addExercisesToDay(dayLocalId: Int, exerciseIds: List<Long>) {
        if (exerciseIds.isEmpty()) return
        viewModelScope.launch {
            val resolved = getExercisesByIdsUseCase(exerciseIds)
            _form.update { form ->
                form.copy(days = form.days.map { day ->
                    if (day.localId != dayLocalId) return@map day
                    val existingIds = day.exercises.map { it.exerciseId }.toSet()
                    val toAdd = resolved.filter { it.id !in existingIds }
                        .map { DraftExercise(exerciseId = it.id, title = it.title, imageUrl = it.gifUrl, targetReps = 10) }
                    day.copy(exercises = day.exercises + toAdd)
                })
            }
        }
    }

    fun save() {
        val form = _form.value
        if (!form.canSave || _saveState.value is SaveState.Saving) return
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            val planId = createCustomWorkoutPlanUseCase(
                title = form.title.trim(),
                description = form.description.trim().ifBlank { null },
                category = form.category,
                level = form.level,
                days = form.days.filter { it.exercises.isNotEmpty() }.map { day ->
                    CustomDaySpec(
                        title = day.title.trim().ifBlank { null },
                        exercises = day.exercises.map { exercise ->
                            CustomExerciseSpec(
                                exerciseId = exercise.exerciseId,
                                targetReps = exercise.targetReps,
                                targetDurationSec = exercise.targetDurationSec,
                                restAfterSec = exercise.restAfterSec
                            )
                        }
                    )
                }
            )
            _saveState.value = SaveState.Saved(planId)
        }
    }
}
