package com.example.homeworkout.domain.running

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunningTelemetryCalculatorTest {
    private val calculator = RunningTelemetryCalculator()

    @Test fun calculatesDistancePaceAndCalories() {
        val result = calculator.calculate(distanceMeters = 5_000.0, activeDurationMillis = 30 * 60_000L, weightKg = 60.0)
        assertEquals(5.0, result.distanceKilometers, 0.0001)
        assertEquals(360.0, result.paceSecondsPerKilometer!!, 0.0001)
        assertEquals(310.8, result.calories!!, 0.0001)
    }

    @Test fun zeroDistanceHasNoPace() {
        assertNull(calculator.calculate(0.0, 60_000, 60.0).paceSecondsPerKilometer)
    }

    @Test fun missingWeightHasNoCalories() {
        assertNull(calculator.calculate(1_000.0, 300_000, null).calories)
    }
}
