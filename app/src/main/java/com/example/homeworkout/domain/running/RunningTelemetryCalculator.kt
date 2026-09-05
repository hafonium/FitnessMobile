package com.example.homeworkout.domain.running

data class RunningTelemetry(
    val distanceKilometers: Double,
    val paceSecondsPerKilometer: Double?,
    val calories: Double?
)

class RunningTelemetryCalculator {
    fun calculate(distanceMeters: Double, activeDurationMillis: Long, weightKg: Double?): RunningTelemetry {
        val kilometers = distanceMeters.coerceAtLeast(0.0) / 1_000.0
        val pace = if (kilometers >= MIN_PACE_DISTANCE_KM && activeDurationMillis > 0L) {
            activeDurationMillis / 1_000.0 / kilometers
        } else null
        return RunningTelemetry(
            distanceKilometers = kilometers,
            paceSecondsPerKilometer = pace,
            calories = weightKg?.takeIf { it > 0.0 }?.let { it * kilometers * 1.036 }
        )
    }

    companion object { const val MIN_PACE_DISTANCE_KM = 0.05 }
}
