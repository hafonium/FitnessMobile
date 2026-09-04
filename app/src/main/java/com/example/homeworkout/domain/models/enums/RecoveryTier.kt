package com.example.homeworkout.domain.models.enums

/** Threshold tiers for the offline Readiness & Recovery score (0-100). See [RecoveryTier.fromScore]. */
enum class RecoveryTier(val title: String, val rationale: String, val ctaLabel: String?) {
    OPTIMAL(
        title = "Prime Readiness",
        rationale = "Full recovery achieved. Muscular and nervous systems are primed for maximum intensity.",
        ctaLabel = null
    ),
    MODERATE(
        title = "Moderate Fatigue",
        rationale = "Accumulated training strain detected. Pacing and hydration recommended.",
        ctaLabel = "Add +15s Rest Between Sets"
    ),
    HIGH_STRAIN(
        title = "Recovery Priority",
        rationale = "Consecutive active days or high deficit detected. High risk of overtraining.",
        ctaLabel = "Switch to 10-Min Mobility / Rest"
    );

    companion object {
        fun fromScore(score: Int): RecoveryTier = when {
            score >= 80 -> OPTIMAL
            score >= 50 -> MODERATE
            else -> HIGH_STRAIN
        }
    }
}
