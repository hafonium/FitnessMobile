package com.example.homeworkout.domain.usecases.running

import com.example.homeworkout.domain.models.running.RunActivityType
import com.example.homeworkout.domain.models.running.RunCoordinate
import com.example.homeworkout.domain.models.running.RunPoint
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.models.running.RunSessionMetadata
import com.example.homeworkout.domain.models.running.RunStatus
import com.example.homeworkout.domain.repositories.RunningRepository
import com.example.homeworkout.domain.running.EncodedPolylineCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GetRunDetailUseCaseTest {
    @Test
    fun returnsSessionWhenFound() = runBlocking {
        val points = listOf(
            RunPoint(1, 100, 10.762, 106.682, null, 5f, null, 1000L, 0, 0),
            RunPoint(2, 100, 10.763, 106.683, null, 5f, null, 2000L, 1, 0)
        )
        val encoded = EncodedPolylineCodec.encode(points)
        val session = RunSession(
            id = 100,
            startedAt = 1000L,
            finishedAt = 2000L,
            activeDurationMillis = 60000L,
            runningStartedElapsedRealtimeMillis = null,
            distanceMeters = 270.0,
            calories = 15.0,
            weightKg = 65.0,
            status = RunStatus.FINISHED,
            currentSegmentIndex = 0,
            encodedPolyline = encoded,
            activityType = RunActivityType.RUNNING,
            title = "Free run",
            points = points
        )
        val repository = FakeRunningRepository(session)
        val useCase = GetRunDetailUseCase(repository)

        val result = useCase(100)
        assertNotNull(result)
        assertEquals(100L, result?.id)
        assertEquals(2, result?.points?.size)
        assertEquals(encoded, result?.encodedPolyline)

        val decoded = runCatching {
            result?.encodedPolyline?.takeIf { it.isNotBlank() }?.let(EncodedPolylineCodec::decode)
        }.getOrNull()?.filter { it.isNotEmpty() }

        val fallback = result?.points.orEmpty().groupBy { it.segmentIndex }.toSortedMap().values.map { pList ->
            pList.map { RunCoordinate(it.latitude, it.longitude) }
        }.filter { it.isNotEmpty() }

        val routeSegments = if (!decoded.isNullOrEmpty()) decoded else fallback
        assertEquals(1, routeSegments.size)
        assertEquals(2, routeSegments[0].size)
    }

    @Test
    fun returnsNullWhenSessionNotFound() = runBlocking {
        val repository = FakeRunningRepository(null)
        val useCase = GetRunDetailUseCase(repository)
        assertNull(useCase(999))
    }

    @Test
    fun fallsBackToPointsWhenPolylineIsBlank() = runBlocking {
        val points = listOf(
            RunPoint(1, 101, 10.762, 106.682, null, 5f, null, 1000L, 0, 0),
            RunPoint(2, 101, 10.763, 106.683, null, 5f, null, 2000L, 1, 0)
        )
        val session = RunSession(
            id = 101,
            startedAt = 1000L,
            finishedAt = 2000L,
            activeDurationMillis = 60000L,
            runningStartedElapsedRealtimeMillis = null,
            distanceMeters = 270.0,
            calories = 15.0,
            weightKg = 65.0,
            status = RunStatus.FINISHED,
            currentSegmentIndex = 0,
            encodedPolyline = "",
            activityType = RunActivityType.RUNNING,
            title = "Free run",
            points = points
        )
        val repository = FakeRunningRepository(session)
        val useCase = GetRunDetailUseCase(repository)

        val result = useCase(101)
        assertNotNull(result)

        val decoded = runCatching {
            result?.encodedPolyline?.takeIf { it.isNotBlank() }?.let(EncodedPolylineCodec::decode)
        }.getOrNull()?.filter { it.isNotEmpty() }

        val fallback = result?.points.orEmpty().groupBy { it.segmentIndex }.toSortedMap().values.map { pList ->
            pList.map { RunCoordinate(it.latitude, it.longitude) }
        }.filter { it.isNotEmpty() }

        val routeSegments = if (!decoded.isNullOrEmpty()) decoded else fallback
        assertEquals(1, routeSegments.size)
        assertEquals(2, routeSegments[0].size)
        assertEquals(RunCoordinate(10.762, 106.682), routeSegments[0][0])
    }

    private class FakeRunningRepository(private val session: RunSession?) : RunningRepository {
        override fun observeLatestSession(): Flow<RunSession?> = flowOf(session)
        override fun observeFinishedSessions(): Flow<List<RunSession>> = flowOf(session?.let { listOf(it) } ?: emptyList())
        override suspend fun getSession(id: Long): RunSession? = session?.takeIf { it.id == id }
        override suspend fun getRecoverableSession(): RunSession? = null
        override suspend fun createSession(startedAt: Long, elapsedRealtimeMillis: Long, metadata: RunSessionMetadata): RunSession = error("Not used")
        override suspend fun appendPoint(point: RunPoint, distanceMeters: Double, activeDurationMillis: Long, runningStartedElapsedRealtimeMillis: Long, calories: Double?) = Unit
        override suspend fun updateState(id: Long, status: RunStatus, activeDurationMillis: Long, runningStartedElapsedRealtimeMillis: Long?, segmentIndex: Int, finishedAt: Long?, errorMessage: String?, encodedPolyline: String?) = Unit
        override suspend fun deleteSession(id: Long) = Unit
    }
}
