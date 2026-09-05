package com.example.homeworkout.data.catalog

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredTrainingCatalogParserTest {
    @Test fun parsesCompleteRunningCatalog() {
        val program = parseAsset("beginner_running_12w.json")
        assertEquals(12, program.weeks.size)
        assertEquals(37, program.weeks.sumOf { it.sessions.size })
        assertTrue(program.weeks.single { it.weekNumber == 11 }.sessions.single { it.sessionIndex == 4 }.isOptional)
        assertEquals(35, program.weeks.single { it.weekNumber == 7 }.sessions.single { it.sessionIndex == 3 }.durationMaxMinutes)
        assertEquals(55, program.weeks.single { it.weekNumber == 12 }.sessions.single { it.sessionIndex == 3 }.durationMaxMinutes)
    }

    @Test fun parsesCompleteWalkingCatalogAndIntervals() {
        val program = parseAsset("walking_weight_loss_20w.json")
        assertEquals(5, program.phases.size)
        assertEquals(20, program.weeks.size)
        assertEquals(107, program.weeks.sumOf { it.sessions.size })
        val week20 = program.weeks.single { it.weekNumber == 20 }
        assertTrue(week20.sessions.single { it.sessionIndex == 6 }.isOptional)
        assertEquals(320, week20.durationMaxMinutes)
        assertEquals(14, program.weeks.single { it.weekNumber == 18 }.sessions.first().steps.size)
    }

    private fun parseAsset(name: String) = StructuredTrainingCatalogParser.parse(
        File("src/main/assets/$name").readText()
    )
}
