package com.example.homeworkout.domain.models.running

enum class RunStatus { RUNNING, PAUSED, FINISHED, ERROR }

enum class RunActivityType { RUNNING, WALKING }

data class RunSessionMetadata(
    val activityType: RunActivityType = RunActivityType.RUNNING,
    val title: String? = null,
    val programId: String? = null,
    val trainingSessionId: String? = null
)

data class RunCoordinate(val latitude: Double, val longitude: Double)

data class RunPoint(
    val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val elapsedRealtimeNanos: Long,
    val sequence: Int,
    val segmentIndex: Int
)

data class RunSession(
    val id: Long,
    val startedAt: Long,
    val finishedAt: Long?,
    val activeDurationMillis: Long,
    val runningStartedElapsedRealtimeMillis: Long?,
    val distanceMeters: Double,
    val calories: Double?,
    val weightKg: Double?,
    val status: RunStatus,
    val currentSegmentIndex: Int,
    val errorMessage: String? = null,
    val encodedPolyline: String? = null,
    val activityType: RunActivityType = RunActivityType.RUNNING,
    val title: String? = null,
    val programId: String? = null,
    val trainingSessionId: String? = null,
    val points: List<RunPoint> = emptyList()
) {
    val durationSeconds: Long get() = activeDurationMillis / 1_000L

    val averagePaceMinutesPerKilometer: Double
        get() {
            val distanceKilometers = distanceMeters / 1_000.0
            return if (distanceKilometers > 0.0 && durationSeconds > 0L) {
                durationSeconds / 60.0 / distanceKilometers
            } else 0.0
        }

    fun activeDurationAt(elapsedRealtimeMillis: Long): Long = activeDurationMillis +
        if (status == RunStatus.RUNNING && runningStartedElapsedRealtimeMillis != null) {
            (elapsedRealtimeMillis - runningStartedElapsedRealtimeMillis).coerceAtLeast(0L)
        } else 0L
}

data class RunningSnapshot(val session: RunSession?)
