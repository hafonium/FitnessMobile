package com.example.homeworkout.domain.usecases.progression

import com.example.homeworkout.domain.models.Exercise
import com.example.homeworkout.domain.models.ExerciseSessionRecord
import com.example.homeworkout.domain.models.ProgressionNode
import com.example.homeworkout.domain.models.enums.ProgressionBranch
import com.example.homeworkout.domain.models.enums.ProgressionNodeStatus
import com.example.homeworkout.domain.repositories.ExerciseRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Backs the Discovery tab's "Calisthenics Skill Trees". Resolves [ProgressionCatalog]'s static
 * node definitions onto the local exercise library (nodes with no match render as fallback
 * placeholders, never throwing), then reactively derives each node's [ProgressionNodeStatus] from
 * the user's real completed-session history via [WorkoutSessionRepository.observeExerciseHistory].
 */
class GetProgressionTreeUseCase(
    private val exerciseRepository: ExerciseRepository,
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(branch: ProgressionBranch): Flow<List<ProgressionNode>> {
        val defs = ProgressionCatalog.nodesFor(branch)
        return flow { emit(resolveExercises(defs)) }.flatMapLatest { resolved ->
            val exerciseIds = resolved.values.filterNotNull().map { it.id }
            workoutSessionRepository.observeExerciseHistory(exerciseIds).map { history ->
                buildNodes(branch, defs, resolved, history)
            }
        }
    }

    private suspend fun resolveExercises(defs: List<ProgressionNodeDef>): Map<String, Exercise?> {
        return defs.associate { def ->
            def.id to def.exerciseTitle?.let { exerciseRepository.findExerciseByTitle(it) }
        }
    }

    private fun buildNodes(
        branch: ProgressionBranch,
        defs: List<ProgressionNodeDef>,
        resolved: Map<String, Exercise?>,
        history: List<ExerciseSessionRecord>
    ): List<ProgressionNode> {
        val historyByExerciseId = history.groupBy { it.exerciseId }
        var previousMastered = true // Node 1 (baseline) is always unlocked by default.

        return defs.map { def ->
            val exercise = resolved[def.id]
            val records = exercise?.let { historyByExerciseId[it.id] }.orEmpty()

            val bestReps = records.mapNotNull { it.actualReps }.maxOrNull()
            val bestHoldSeconds = records.mapNotNull { it.actualDurationSec }.maxOrNull()
            val completionsMeetingTarget = records.count { record ->
                when {
                    def.targetReps != null -> (record.actualReps ?: 0) >= def.targetReps
                    def.targetHoldSeconds != null -> (record.actualDurationSec ?: 0) >= def.targetHoldSeconds
                    else -> false
                }
            }
            val mastered = completionsMeetingTarget >= def.targetCompletions

            val status = when {
                !previousMastered -> ProgressionNodeStatus.LOCKED
                mastered -> ProgressionNodeStatus.MASTERED
                else -> ProgressionNodeStatus.IN_PROGRESS
            }
            previousMastered = mastered

            ProgressionNode(
                id = def.id,
                branch = branch,
                order = def.order,
                name = def.name,
                exerciseId = exercise?.id,
                gifUrl = exercise?.gifUrl,
                formTips = def.formTips,
                targetReps = def.targetReps,
                targetHoldSeconds = def.targetHoldSeconds,
                targetCompletions = def.targetCompletions,
                status = status,
                bestReps = bestReps,
                bestHoldSeconds = bestHoldSeconds,
                completionsMeetingTarget = completionsMeetingTarget,
                badgeId = def.badgeId
            )
        }
    }
}
