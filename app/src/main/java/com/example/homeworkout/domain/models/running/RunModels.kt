package com.example.homeworkout.domain.models.running

enum class RunStatus { RUNNING, PAUSED, FINISHED, ERROR }

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
    val points: List<RunPoint> = emptyList()
) {
    fun activeDurationAt(elapsedRealtimeMillis: Long): Long = activeDurationMillis +
        if (status == RunStatus.RUNNING && runningStartedElapsedRealtimeMillis != null) {
            (elapsedRealtimeMillis - runningStartedElapsedRealtimeMillis).coerceAtLeast(0L)
        } else 0L
}

data class RunningSnapshot(val session: RunSession?)
