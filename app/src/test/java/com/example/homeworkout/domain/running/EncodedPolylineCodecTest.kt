package com.example.homeworkout.domain.running

import com.example.homeworkout.domain.models.running.RunCoordinate
import com.example.homeworkout.domain.models.running.RunPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class EncodedPolylineCodecTest {
    @Test
    fun encodesKnownGooglePolylineExample() {
        val coordinates = listOf(
            RunCoordinate(38.5, -120.2),
            RunCoordinate(40.7, -120.95),
            RunCoordinate(43.252, -126.453)
        )

        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", EncodedPolylineCodec.encodeCoordinates(coordinates))
        assertEquals(coordinates, EncodedPolylineCodec.decodeCoordinates("_p~iF~ps|U_ulLnnqC_mqNvxq`@"))
    }

    @Test
    fun preservesPauseResumeSegments() {
        val points = listOf(
            point(10.00001, 106.00001, sequence = 0, segment = 0),
            point(10.00002, 106.00002, sequence = 1, segment = 0),
            point(10.00101, 106.00101, sequence = 2, segment = 1),
            point(10.00102, 106.00102, sequence = 3, segment = 1)
        )

        val encoded = EncodedPolylineCodec.encode(points)
        val decoded = EncodedPolylineCodec.decode(encoded)

        assertEquals(2, decoded.size)
        assertEquals(2, decoded[0].size)
        assertEquals(2, decoded[1].size)
        assertEquals(RunCoordinate(10.00102, 106.00102), decoded[1].last())
    }

    @Test
    fun emptyRouteRoundTrips() {
        assertEquals("", EncodedPolylineCodec.encode(emptyList()))
        assertEquals(emptyList<List<RunCoordinate>>(), EncodedPolylineCodec.decode(""))
    }

    private fun point(latitude: Double, longitude: Double, sequence: Int, segment: Int) = RunPoint(
        sessionId = 1,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = null,
        accuracyMeters = 3f,
        speedMps = null,
        elapsedRealtimeNanos = sequence.toLong(),
        sequence = sequence,
        segmentIndex = segment
    )
}
