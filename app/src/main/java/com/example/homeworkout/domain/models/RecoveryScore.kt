package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.RecoveryTier

/** Result of the offline [com.example.homeworkout.domain.usecases.home.RecoveryCalculator] heuristic. */
data class RecoveryScore(
    val score: Int,
    val tier: RecoveryTier,
    val badges: List<String>
)
