package com.example.homeworkout.domain.models.catalog

/**
 * Parsed form of `assets/workout_plan_catalog.json` (docs/workout_plan_catalog.json). The catalog
 * is the single source of truth for plan-template metadata and slot structure; the seeder
 * materializes it into `workout_plans` rows and the recommender scores its [plans] against a
 * [com.example.homeworkout.domain.models.FitnessProfile].
 */
data class WorkoutPlanCatalog(
    val version: Int,
    val prescriptions: Map<String, Prescription>,
    val dayPatterns: Map<String, DayPattern>,
    val plans: List<CatalogPlan>
)

data class Prescription(
    val workingSets: Int,
    val repMin: Int,
    val repMax: Int,
    val strengthRestSeconds: Int,
    val timedWorkSeconds: Int,
    val timedRestSeconds: Int
) {
    val repMid: Int get() = (repMin + repMax) / 2
}

data class DayPattern(
    val id: String,
    val label: String,
    val slots: List<PatternSlot>
)

data class PatternSlot(
    /** An exercise-dataset category key, e.g. "legs_glutes". */
    val category: String,
    val primaryMuscles: List<String>,
    /** "pull" / "push" / "static", or null when unconstrained. */
    val force: String?,
    val count: Int,
    val role: String
)

data class CatalogPlan(
    val id: String,
    val title: String,
    /** general_fitness | build_muscle | fat_loss | mobility | focus_area */
    val goal: String,
    /** beginner | intermediate | expert | adaptive */
    val level: String,
    val daysPerWeek: Int,
    val minutesMin: Int,
    val minutesMax: Int,
    /** Ordered day-pattern ids, one per training day. */
    val schedule: List<String>,
    val preferredEquipment: List<String>,
    /** The user must own at least one of these, when non-empty. */
    val requiredAnyEquipment: List<String>,
    /** When non-null, exercises may only use equipment in this set. */
    val allowedEquipment: List<String>?,
    val focusCategories: List<String>
)
