package com.example.homeworkout.domain.usecases.planselection

import com.example.homeworkout.domain.models.FitnessProfile
import com.example.homeworkout.domain.models.PlanRecommendation
import com.example.homeworkout.domain.models.RecommendedPlan
import com.example.homeworkout.domain.models.catalog.CatalogPlan
import com.example.homeworkout.domain.repositories.PlanCatalogRepository
import com.example.homeworkout.domain.repositories.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.max

/**
 * Scores the 16 catalog templates against a [FitnessProfile] and returns the best plan plus up to
 * two alternatives — docs/workout_plan_selection_guide.md §4, steps 2 and 3.
 */
class RecommendPlanUseCase(
    private val planCatalogRepository: PlanCatalogRepository,
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(profile: FitnessProfile): PlanRecommendation? {
        val catalog = planCatalogRepository.getCatalog()
        val planByTitle = workoutRepository.getWorkouts().first().associateBy { it.title }
        val ownedEquipment = profile.availableEquipment + "bodyweight"

        fun rank(plans: List<CatalogPlan>): List<RecommendedPlan> = plans.mapNotNull { plan ->
            val model = planByTitle[plan.title] ?: return@mapNotNull null
            RecommendedPlan(
                plan = model,
                catalogId = plan.id,
                score = templateScore(plan, profile, ownedEquipment),
                rationale = rationale(plan, profile)
            )
        }.sortedByDescending { it.score }

        val eligible = catalog.plans.filter { passesHardFilters(it, profile, ownedEquipment) }
        // Fallback: if the hard day cap removed everything, rank the whole catalog instead.
        val ranked = rank(eligible).ifEmpty { rank(catalog.plans) }

        val top = ranked.firstOrNull() ?: return null
        return PlanRecommendation(recommended = top, alternatives = ranked.drop(1).take(2))
    }

    private fun passesHardFilters(plan: CatalogPlan, profile: FitnessProfile, owned: Set<String>): Boolean {
        if (plan.daysPerWeek > profile.daysPerWeek) return false
        if (plan.requiredAnyEquipment.isNotEmpty() && plan.requiredAnyEquipment.none { it in owned }) return false
        return true
    }

    private fun templateScore(plan: CatalogPlan, profile: FitnessProfile, owned: Set<String>): Double {
        val goalMatch = if (plan.goal == profile.primaryGoal.key) 1.0 else 0.0

        val levelMatch = when {
            plan.level == "adaptive" -> 0.7
            plan.level == profile.experienceLevel.key -> 1.0
            abs(levelIndex(plan.level) - levelIndex(profile.experienceLevel.key)) == 1 -> 0.5
            else -> 0.0
        }

        val daysMatch = when {
            plan.daysPerWeek == profile.daysPerWeek -> 1.0
            plan.daysPerWeek < profile.daysPerWeek -> 0.8
            else -> 0.0
        }

        val durationFit = if (profile.sessionMinutes in plan.minutesMin..plan.minutesMax) {
            1.0
        } else {
            val distance = if (profile.sessionMinutes < plan.minutesMin) plan.minutesMin - profile.sessionMinutes
            else profile.sessionMinutes - plan.minutesMax
            max(0.0, 1.0 - distance / 30.0)
        }

        val preferred = plan.preferredEquipment.filter { it != "bodyweight" }
        val equipmentCoverage = if (preferred.isEmpty()) 1.0 else preferred.count { it in owned }.toDouble() / preferred.size

        val profileFocusKeys = profile.focusCategories.map { it.name.lowercase() }
        val focusMatch = when {
            plan.focusCategories.isEmpty() && profileFocusKeys.isEmpty() -> 1.0
            plan.focusCategories.isEmpty() -> 0.6
            plan.focusCategories.any { it in profileFocusKeys } -> 1.0
            else -> 0.0
        }

        return 0.35 * goalMatch +
            0.20 * levelMatch +
            0.15 * daysMatch +
            0.10 * durationFit +
            0.10 * equipmentCoverage +
            0.10 * focusMatch
    }

    private fun levelIndex(key: String): Int = when (key) {
        "beginner" -> 0
        "intermediate" -> 1
        "expert" -> 2
        else -> 1
    }

    private fun rationale(plan: CatalogPlan, profile: FitnessProfile): String {
        val bits = buildList {
            if (plan.goal == profile.primaryGoal.key) add("your goal")
            add("${plan.daysPerWeek} days/week")
            add("${plan.minutesMin}–${plan.minutesMax} min sessions")
        }
        return "Matches " + bits.joinToString(", ")
    }
}
