package com.example.homeworkout.domain.running

import com.example.homeworkout.domain.models.running.RunPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFilterTest {
    private val filter = LocationFilter()

    @Test fun rejectsPoorAccuracy() {
        assertFalse(filter.evaluate(null, point(accuracy = 16f)).accepted)
    }

    @Test fun rejectsStationaryDrift() {
        val previous = point(latitude = 10.0, nanos = 1_000_000_000)
        val drift = point(latitude = 10.000001, nanos = 3_000_000_000)
        assertFalse(filter.evaluate(previous, drift).accepted)
    }

    @Test fun acceptsNormalRunningMovement() {
        val previous = point(latitude = 10.0, nanos = 1_000_000_000)
        val next = point(latitude = 10.00009, nanos = 3_000_000_000)
        assertTrue(filter.evaluate(previous, next).accepted)
    }

    @Test fun rejectsImpossibleJump() {
        val previous = point(latitude = 10.0, nanos = 1_000_000_000)
        val jump = point(latitude = 10.01, nanos = 3_000_000_000)
        assertFalse(filter.evaluate(previous, jump).accepted)
    }

    @Test fun rejectedPointDoesNotEnterAccumulatedDistance() {
        val first = point(latitude = 10.0, nanos = 1_000_000_000)
        val drift = point(latitude = 10.000001, nanos = 3_000_000_000)
        val normal = point(latitude = 10.00009, nanos = 5_000_000_000)
        val acceptedDistance = listOf(filter.evaluate(first, drift), filter.evaluate(first, normal))
            .filter { it.accepted }
            .sumOf { it.segmentDistanceMeters }
        assertTrue(acceptedDistance in 9.0..11.0)
    }

    private fun point(latitude: Double = 10.0, accuracy: Float = 5f, nanos: Long = 1_000_000_000) = RunPoint(
        sessionId = 1, latitude = latitude, longitude = 106.0, altitudeMeters = null,
        accuracyMeters = accuracy, speedMps = null, elapsedRealtimeNanos = nanos,
        sequence = 0, segmentIndex = 0
    )
}
