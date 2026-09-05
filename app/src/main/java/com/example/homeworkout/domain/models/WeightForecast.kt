package com.example.homeworkout.domain.models

/** One projected future point on the Weight Curve chart. See docs/weight-forecast-feature.md. */
data class ForecastPoint(
    val dayOffset: Int,
    val projectedAt: Long,
    val weightKg: Double
)

/**
 * TDEE-based weight projection (docs/weight-forecast-feature.md). [hasEnoughData] gates whether
 * the rest of the fields are meaningful; when false, [missingReason] explains what to add first
 * and every other field is null/empty.
 */
data class WeightForecast(
    val hasEnoughData: Boolean,
    val missingReason: String?,
    val currentWeightKg: Double?,
    val bmrKcal: Double?,
    val tdeeKcal: Double?,
    val avgDailyIntakeKcal: Double?,
    val netDailyBalanceKcal: Double?,
    val usedNeutralGenderConstant: Boolean,
    val projection: List<ForecastPoint>
)
