package com.example.homeworkout.ui.core.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.domain.models.ProgressionNode
import com.example.homeworkout.domain.models.enums.ProgressionBranch
import com.example.homeworkout.domain.models.enums.ProgressionNodeStatus
import com.example.homeworkout.domain.usecases.badges.GetBadgesUseCase
import com.example.homeworkout.domain.usecases.progression.EvaluateProgressionAchievementsUseCase
import com.example.homeworkout.domain.usecases.progression.GetProgressionTreeUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the Discovery tab's "Calisthenics Skill Trees" section. */
class DiscoveryViewModel(
    private val getProgressionTreeUseCase: GetProgressionTreeUseCase,
    private val evaluateProgressionAchievementsUseCase: EvaluateProgressionAchievementsUseCase,
    private val getBadgesUseCase: GetBadgesUseCase
) : ViewModel() {

    private val _selectedBranch = MutableStateFlow(ProgressionBranch.PUSH)
    val selectedBranch: StateFlow<ProgressionBranch> = _selectedBranch.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    private val _unlockedBadge = MutableStateFlow<BadgeProgress?>(null)
    val unlockedBadge: StateFlow<BadgeProgress?> = _unlockedBadge.asStateFlow()

    /** All statuses recompute locally from `workout_session_exercises`/`workout_sessions` history via Room Flow — no manual refresh needed. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val nodes: StateFlow<List<ProgressionNode>> = _selectedBranch
        .flatMapLatest { branch -> getProgressionTreeUseCase(branch) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val selectedNode: StateFlow<ProgressionNode?> = combine(nodes, _selectedNodeId) { list, id ->
        list.find { it.id == id }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    init {
        // Fires the achievement hook whenever a freshly-loaded branch tree contains a mastered,
        // badge-carrying node (Feature Spec §3 "Achievement & Badge Hook").
        viewModelScope.launch {
            combine(_selectedBranch, nodes) { branch, list -> branch to list }.collect { (branch, list) ->
                val hasMasteredMilestone = list.any {
                    it.status == ProgressionNodeStatus.MASTERED && it.badgeId != null
                }
                if (!hasMasteredMilestone) return@collect
                val unlockedIds = evaluateProgressionAchievementsUseCase(branch)
                if (unlockedIds.isEmpty()) return@collect
                val badge = getBadgesUseCase().first().find { it.definition.id == unlockedIds.first() }
                if (badge != null) _unlockedBadge.value = badge
            }
        }
    }

    fun selectBranch(branch: ProgressionBranch) {
        _selectedBranch.value = branch
    }

    fun selectNode(nodeId: String) {
        _selectedNodeId.value = nodeId
    }

    fun dismissNodeDetail() {
        _selectedNodeId.value = null
    }

    fun dismissUnlockedBadge() {
        _unlockedBadge.value = null
    }
}
