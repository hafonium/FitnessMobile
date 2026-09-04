package com.example.homeworkout.domain.running

import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.models.running.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunStateTest {
    private val machine = RunStateMachine()

    @Test fun supportsExpectedLifecycleTransitions() {
        assertTrue(machine.transition(null, RunStatus.RUNNING))
        assertTrue(machine.transition(RunStatus.RUNNING, RunStatus.PAUSED))
        assertTrue(machine.transition(RunStatus.PAUSED, RunStatus.RUNNING))
        assertTrue(machine.transition(RunStatus.RUNNING, RunStatus.FINISHED))
        assertFalse(machine.transition(RunStatus.FINISHED, RunStatus.RUNNING))
    }

    @Test fun pausedDurationDoesNotIncrease() {
        val paused = RunSession(
            id = 1, startedAt = 0, finishedAt = null, activeDurationMillis = 12_000,
            runningStartedElapsedRealtimeMillis = null, distanceMeters = 0.0, calories = null,
            weightKg = null, status = RunStatus.PAUSED, currentSegmentIndex = 0
        )
        assertEquals(12_000, paused.activeDurationAt(99_000))
    }

    @Test fun runningDurationIncludesOnlyCurrentActivePeriod() {
        val running = RunSession(
            id = 1, startedAt = 0, finishedAt = null, activeDurationMillis = 12_000,
            runningStartedElapsedRealtimeMillis = 50_000, distanceMeters = 0.0, calories = null,
            weightKg = null, status = RunStatus.RUNNING, currentSegmentIndex = 1
        )
        assertEquals(17_000, running.activeDurationAt(55_000))
    }
}
