package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.ProgressionBranch
import com.example.homeworkout.domain.models.enums.ProgressionNodeStatus

/**
 * One tier of a [ProgressionBranch] skill tree, merging the static catalog definition
 * (`ProgressionCatalog`) with the exercise it resolved to in the local library and the user's
 * real workout history. `exerciseId` is null when the catalog's canonical exercise title has no
 * match in `exercises` yet — the UI renders those as graceful fallback placeholders rather than
 * throwing (see docs feature spec, Technical Acceptance Criteria #2).
 */
data class ProgressionNode(
    val id: String,
    val branch: ProgressionBranch,
    val order: Int,
    val name: String,
    val exerciseId: Long?,
    val gifUrl: String?,
    val formTips: List<String>,
    val targetReps: Int?,
    val targetHoldSeconds: Int?,
    val targetCompletions: Int,
    val status: ProgressionNodeStatus,
    val bestReps: Int?,
    val bestHoldSeconds: Int?,
    val completionsMeetingTarget: Int,
    val badgeId: String?
) {
    val isPlaceholder: Boolean get() = exerciseId == null

    val requirementLabel: String
        get() {
            val perAttempt = when {
                targetReps != null -> "$targetReps clean reps"
                targetHoldSeconds != null -> "a ${targetHoldSeconds}s hold"
                else -> "mastery criteria"
            }
            return "Achieve $perAttempt in $targetCompletions separate workouts"
        }

    val currentBestLabel: String?
        get() = when {
            bestReps != null -> "Your record: $bestReps reps"
            bestHoldSeconds != null -> "Your record: ${bestHoldSeconds}s hold"
            else -> null
        }
}
