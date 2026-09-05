package com.example.homeworkout.domain.running

import com.example.homeworkout.domain.models.running.RunPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class FilteredLocation(val accepted: Boolean, val segmentDistanceMeters: Double = 0.0)

class LocationFilter(
    private val maxAccuracyMeters: Float = 15f,
    private val minMovingSpeedMps: Float = 0.5f,
    private val maxRunningSpeedMps: Float = 12f
) {
    fun evaluate(previous: RunPoint?, candidate: RunPoint): FilteredLocation {
        if (candidate.latitude !in -90.0..90.0 || candidate.longitude !in -180.0..180.0) return FilteredLocation(false)
        if (!candidate.accuracyMeters.isFinite() || candidate.accuracyMeters <= 0f || candidate.accuracyMeters > maxAccuracyMeters) {
            return FilteredLocation(false)
        }
        if (candidate.elapsedRealtimeNanos <= 0L) return FilteredLocation(false)
        if (previous == null) return FilteredLocation(true)
        val elapsedSeconds = (candidate.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000_000.0
        if (elapsedSeconds <= 0.0) return FilteredLocation(false)
        val distance = distanceMeters(previous.latitude, previous.longitude, candidate.latitude, candidate.longitude)
        val derivedSpeed = distance / elapsedSeconds
        // Derived segment speed is consistent across devices; reported Location.speed is often
        // stale or zero and is retained as metadata rather than trusted as the deciding signal.
        if (derivedSpeed < minMovingSpeedMps || derivedSpeed > maxRunningSpeedMps) return FilteredLocation(false)
        return FilteredLocation(true, distance)
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6_371_000.0
        val latDelta = Math.toRadians(lat2 - lat1)
        val lonDelta = Math.toRadians(lon2 - lon1)
        val a = sin(latDelta / 2) * sin(latDelta / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(lonDelta / 2) * sin(lonDelta / 2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
