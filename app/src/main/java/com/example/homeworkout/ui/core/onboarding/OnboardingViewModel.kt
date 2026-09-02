package com.example.homeworkout.ui.core.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.ExperienceLevel
import com.example.homeworkout.domain.models.FitnessProfile
import com.example.homeworkout.domain.models.PlanRecommendation
import com.example.homeworkout.domain.models.PrimaryGoal
import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.usecases.planselection.GetFitnessProfileUseCase
import com.example.homeworkout.domain.usecases.planselection.RecommendPlanUseCase
import com.example.homeworkout.domain.usecases.planselection.SaveFitnessProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingForm(
    val goal: PrimaryGoal? = null,
    val level: ExperienceLevel? = null,
    val daysPerWeek: Int? = null,
    val sessionMinutes: Int? = null,
    val equipment: Set<String> = setOf("bodyweight"),
    val focusCategories: Set<ExerciseCategory> = emptySet(),
    val focusMuscles: Set<String> = emptySet(),
    val limitations: String = ""
) {
    val canSubmit: Boolean
        get() = goal != null && level != null && daysPerWeek != null && sessionMinutes != null && equipment.isNotEmpty()
}

class OnboardingViewModel(
    private val recommendPlanUseCase: RecommendPlanUseCase,
    private val saveFitnessProfileUseCase: SaveFitnessProfileUseCase,
    getFitnessProfileUseCase: GetFitnessProfileUseCase
) : ViewModel() {

    private val _form = MutableStateFlow(OnboardingForm())
    val form: StateFlow<OnboardingForm> = _form.asStateFlow()

    private val _result = MutableStateFlow<PlanRecommendation?>(null)
    val result: StateFlow<PlanRecommendation?> = _result.asStateFlow()

    private val _computing = MutableStateFlow(false)
    val computing: StateFlow<Boolean> = _computing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            getFitnessProfileUseCase().first()?.let { saved ->
                _form.value = OnboardingForm(
                    goal = saved.primaryGoal,
                    level = saved.experienceLevel,
                    daysPerWeek = saved.daysPerWeek,
                    sessionMinutes = saved.sessionMinutes,
                    equipment = saved.availableEquipment.ifEmpty { setOf("bodyweight") },
                    focusCategories = saved.focusCategories,
                    focusMuscles = saved.focusMuscles,
                    limitations = saved.injuriesOrLimitations
                )
            }
        }
    }

    fun setGoal(goal: PrimaryGoal) = _form.update { it.copy(goal = goal) }
    fun setLevel(level: ExperienceLevel) = _form.update { it.copy(level = level) }
    fun setDays(days: Int) = _form.update { it.copy(daysPerWeek = days) }
    fun setMinutes(minutes: Int) = _form.update { it.copy(sessionMinutes = minutes) }
    fun setLimitations(text: String) = _form.update { it.copy(limitations = text) }

    fun toggleEquipment(value: String) = _form.update {
        it.copy(equipment = it.equipment.toggle(value).ifEmpty { setOf("bodyweight") })
    }

    fun toggleFocusCategory(value: ExerciseCategory) = _form.update {
        it.copy(focusCategories = it.focusCategories.toggle(value))
    }

    fun toggleFocusMuscle(value: String) = _form.update {
        it.copy(focusMuscles = it.focusMuscles.toggle(value))
    }

    fun backToForm() {
        _result.value = null
    }

    fun submit() {
        val f = _form.value
        val profile = FitnessProfile(
            primaryGoal = f.goal ?: return,
            experienceLevel = f.level ?: return,
            daysPerWeek = f.daysPerWeek ?: return,
            sessionMinutes = f.sessionMinutes ?: return,
            availableEquipment = f.equipment,
            focusCategories = f.focusCategories,
            focusMuscles = f.focusMuscles,
            injuriesOrLimitations = f.limitations.trim()
        )
        viewModelScope.launch {
            _computing.value = true
            _error.value = null
            try {
                val recommendation = recommendPlanUseCase(profile)
                if (recommendation == null) {
                    _error.value = "Couldn't find a matching plan. Try adding equipment or training days."
                } else {
                    saveFitnessProfileUseCase(profile, recommendation.recommended.catalogId)
                    _result.value = recommendation
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Something went wrong"
            } finally {
                _computing.value = false
            }
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
