package com.example.homeworkout.data.catalog

import com.example.homeworkout.domain.models.training.StructuredTrainingProgram
import com.example.homeworkout.domain.models.training.StructuredTrainingSession
import com.example.homeworkout.domain.models.training.StructuredTrainingStep
import com.example.homeworkout.domain.models.training.StructuredTrainingWeek
import com.example.homeworkout.domain.models.training.TrainingIntensityDefinition
import com.example.homeworkout.domain.models.training.TrainingPhase
import com.example.homeworkout.domain.models.training.TrainingProgramKind
import com.example.homeworkout.domain.models.training.TrainingRule
import org.json.JSONArray
import org.json.JSONObject

object StructuredTrainingCatalogParser {
    fun parse(json: String): StructuredTrainingProgram {
        val root = JSONObject(json)
        return StructuredTrainingProgram(
            id = root.getString("id"),
            kind = TrainingProgramKind.valueOf(root.getString("kind")),
            title = root.getString("title"),
            subtitle = root.optionalString("subtitle"),
            durationWeeks = root.getInt("durationWeeks"),
            sessionsPerWeek = root.getString("sessionsPerWeek"),
            level = root.getString("level"),
            goal = root.getString("goal"),
            startingAbility = root.optionalString("startingAbility"),
            intensities = root.getJSONArray("intensities").mapObjects { intensity ->
                TrainingIntensityDefinition(
                    intensity.getString("id"), intensity.getString("name"), intensity.getString("rpe"),
                    intensity.getString("description"), intensity.getString("color")
                )
            },
            phases = root.optJSONArray("phases")?.mapObjects { phase ->
                TrainingPhase(
                    phase.getInt("index"), phase.getString("title"), phase.getInt("weekStart"),
                    phase.getInt("weekEnd"), phase.getString("goal")
                )
            }.orEmpty(),
            weeks = root.getJSONArray("weeks").mapObjects(::parseWeek),
            maintenanceTitle = root.getJSONObject("maintenance").getString("title"),
            maintenanceGuidance = root.getJSONObject("maintenance").getString("guidance"),
            maintenanceDays = root.getJSONObject("maintenance").getJSONArray("days").mapStrings(),
            rules = root.optJSONArray("rules")?.mapObjects { rule ->
                TrainingRule(rule.getString("title"), rule.getString("description"))
            }.orEmpty()
        )
    }

    private fun parseWeek(week: JSONObject): StructuredTrainingWeek {
        val weekNumber = week.getInt("number")
        val sessions = mutableListOf<StructuredTrainingSession>()
        week.getJSONArray("sessions").mapObjects { it }.forEach { session ->
            val copies = session.optInt("copies", 1)
            repeat(copies) {
                val sessionIndex = sessions.size + 1
                sessions += StructuredTrainingSession(
                    id = "w${weekNumber}s$sessionIndex",
                    sessionIndex = sessionIndex,
                    title = session.getString("title"),
                    type = session.optString("type", "EASY"),
                    durationMinMinutes = session.getInt("durationMin"),
                    durationMaxMinutes = session.optInt("durationMax", session.getInt("durationMin")),
                    isOptional = session.optBoolean("optional", false),
                    steps = session.optJSONArray("steps")?.let(::parseSteps).orEmpty()
                )
            }
        }
        return StructuredTrainingWeek(
            id = "week-$weekNumber",
            weekNumber = weekNumber,
            title = week.getString("title"),
            durationMinMinutes = week.getInt("durationMin"),
            durationMaxMinutes = week.optInt("durationMax", week.getInt("durationMin")),
            phaseIndex = week.optInt("phase", 0).takeIf { it > 0 },
            sessions = sessions,
            goal = week.optionalString("goal"),
            coachTip = week.optionalString("coachTip"),
            milestone = week.optionalString("milestone"),
            note = week.optionalString("note")
        )
    }

    private fun parseSteps(array: JSONArray): List<StructuredTrainingStep> {
        val output = mutableListOf<StructuredTrainingStep>()
        array.mapObjects { it }.forEach { item ->
            if (item.has("group")) {
                repeat(item.getInt("repeat")) { output += parseSteps(item.getJSONArray("group")) }
            } else {
                output += StructuredTrainingStep(
                    item.getString("label"), item.getInt("seconds"), item.getString("intensity")
                )
            }
        }
        return output
    }

    private fun JSONObject.optionalString(key: String): String? = optString(key).takeIf { it.isNotBlank() }
    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }
    private fun JSONArray.mapStrings(): List<String> = List(length(), ::getString)
}
