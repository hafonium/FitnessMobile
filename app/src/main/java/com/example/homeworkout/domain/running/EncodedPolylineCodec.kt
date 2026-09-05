package com.example.homeworkout.domain.running

import com.example.homeworkout.domain.models.running.RunCoordinate
import com.example.homeworkout.domain.models.running.RunPoint
import kotlin.math.roundToInt

/** Pure Kotlin implementation of Google's encoded-polyline format. */
object EncodedPolylineCodec {
    // Encoded-polyline output uses ASCII 63..126, so newline is an unambiguous separator.
    private const val SEGMENT_SEPARATOR = '\n'

    /** Preserves pause/resume boundaries by encoding each GPS segment independently. */
    fun encode(points: List<RunPoint>): String = points
        .groupBy { it.segmentIndex }
        .toSortedMap()
        .values
        .filter { it.isNotEmpty() }
        .joinToString(SEGMENT_SEPARATOR.toString()) { segment ->
            encodeCoordinates(segment.map { RunCoordinate(it.latitude, it.longitude) })
        }

    fun decode(encoded: String): List<List<RunCoordinate>> = encoded
        .split(SEGMENT_SEPARATOR)
        .filter { it.isNotBlank() }
        .map(::decodeCoordinates)

    fun encodeCoordinates(coordinates: List<RunCoordinate>): String {
        val result = StringBuilder()
        var previousLatitude = 0
        var previousLongitude = 0
        coordinates.forEach { coordinate ->
            val latitude = (coordinate.latitude * 1e5).roundToInt()
            val longitude = (coordinate.longitude * 1e5).roundToInt()
            encodeDelta(latitude - previousLatitude, result)
            encodeDelta(longitude - previousLongitude, result)
            previousLatitude = latitude
            previousLongitude = longitude
        }
        return result.toString()
    }

    fun decodeCoordinates(encoded: String): List<RunCoordinate> {
        val result = mutableListOf<RunCoordinate>()
        var index = 0
        var latitude = 0
        var longitude = 0
        while (index < encoded.length) {
            val latitudeDelta = decodeDelta(encoded, index)
            index = latitudeDelta.nextIndex
            val longitudeDelta = decodeDelta(encoded, index)
            index = longitudeDelta.nextIndex
            latitude += latitudeDelta.value
            longitude += longitudeDelta.value
            result += RunCoordinate(latitude / 1e5, longitude / 1e5)
        }
        return result
    }

    private fun encodeDelta(delta: Int, output: StringBuilder) {
        var value = if (delta < 0) (delta shl 1).inv() else delta shl 1
        while (value >= 0x20) {
            output.append(((0x20 or (value and 0x1f)) + 63).toChar())
            value = value shr 5
        }
        output.append((value + 63).toChar())
    }

    private fun decodeDelta(encoded: String, startIndex: Int): DecodedValue {
        var index = startIndex
        var result = 0
        var shift = 0
        var chunk: Int
        do {
            require(index < encoded.length) { "Malformed encoded polyline" }
            chunk = encoded[index++].code - 63
            require(chunk >= 0) { "Malformed encoded polyline" }
            result = result or ((chunk and 0x1f) shl shift)
            shift += 5
            require(shift <= 30) { "Malformed encoded polyline" }
        } while (chunk >= 0x20)
        val value = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        return DecodedValue(value, index)
    }

    private data class DecodedValue(val value: Int, val nextIndex: Int)
}
