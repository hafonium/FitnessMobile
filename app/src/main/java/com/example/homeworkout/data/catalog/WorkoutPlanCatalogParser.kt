package com.example.homeworkout.data.catalog

import com.example.homeworkout.domain.models.catalog.CatalogPlan
import com.example.homeworkout.domain.models.catalog.DayPattern
import com.example.homeworkout.domain.models.catalog.PatternSlot
import com.example.homeworkout.domain.models.catalog.Prescription
import com.example.homeworkout.domain.models.catalog.WorkoutPlanCatalog
import org.json.JSONObject

/** Parses `assets/workout_plan_catalog.json` into a [WorkoutPlanCatalog]. Shared by the seeder and the recommender. */
internal object WorkoutPlanCatalogParser {

    fun parse(json: String): WorkoutPlanCatalog {
        val root = JSONObject(json)

        val prescriptions = root.getJSONObject("prescriptions").let { obj ->
            obj.keys().asSequence().associateWith { key ->
                val p = obj.getJSONObject(key)
                val (min, max) = parseRepRange(p.getString("repRange"))
                Prescription(
                    workingSets = p.getInt("workingSets"),
                    repMin = min,
                    repMax = max,
                    strengthRestSeconds = p.getInt("strengthRestSeconds"),
                    timedWorkSeconds = p.getInt("timedWorkSeconds"),
                    timedRestSeconds = p.getInt("timedRestSeconds")
                )
            }
        }

        val dayPatterns = root.getJSONObject("dayPatterns").let { obj ->
            obj.keys().asSequence().associateWith { id ->
                val dp = obj.getJSONObject(id)
                val slots = dp.getJSONArray("slots").let { arr ->
                    (0 until arr.length()).map { i ->
                        val s = arr.getJSONObject(i)
                        PatternSlot(
                            category = s.getString("category"),
                            primaryMuscles = s.optJSONArray("primaryMuscles").toStringList(),
                            force = if (s.has("force")) s.getString("force") else null,
                            count = s.getInt("count"),
                            role = s.getString("role")
                        )
                    }
                }
                DayPattern(id = id, label = dp.getString("label"), slots = slots)
            }
        }

        val plans = root.getJSONArray("plans").let { arr ->
            (0 until arr.length()).map { i ->
                val p = arr.getJSONObject(i)
                val minutes = p.getJSONArray("minutes")
                CatalogPlan(
                    id = p.getString("id"),
                    title = p.getString("title"),
                    goal = p.getString("goal"),
                    level = p.getString("level"),
                    daysPerWeek = p.getInt("daysPerWeek"),
                    minutesMin = minutes.getInt(0),
                    minutesMax = minutes.getInt(minutes.length() - 1),
                    schedule = p.getJSONArray("schedule").toStringList(),
                    preferredEquipment = p.optJSONArray("preferredEquipment").toStringList(),
                    requiredAnyEquipment = p.optJSONArray("requiredAnyEquipment").toStringList(),
                    allowedEquipment = if (p.has("allowedEquipment")) p.getJSONArray("allowedEquipment").toStringList() else null,
                    focusCategories = p.optJSONArray("focusCategories").toStringList()
                )
            }
        }

        return WorkoutPlanCatalog(
            version = root.optInt("version", 1),
            prescriptions = prescriptions,
            dayPatterns = dayPatterns,
            plans = plans
        )
    }

    private fun parseRepRange(range: String): Pair<Int, Int> {
        val parts = range.split("-").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] to parts[1]
            1 -> parts[0] to parts[0]
            else -> 8 to 12
        }
    }

    private fun org.json.JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }
}
