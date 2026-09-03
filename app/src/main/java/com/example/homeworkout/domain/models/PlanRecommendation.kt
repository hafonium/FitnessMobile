package com.example.homeworkout.domain.models

/** Result of scoring the catalog against a [FitnessProfile]: the best plan plus up to two alternatives. */
data class PlanRecommendation(
    val recommended: RecommendedPlan,
    val alternatives: List<RecommendedPlan>
)

data class RecommendedPlan(
    /** The materialized [WorkoutModel] in the database, matched by title to the catalog plan. */
    val plan: WorkoutModel,
    /** Catalog plan id, e.g. "muscle_intermediate_4d". */
    val catalogId: String,
    /** 0..1 template score from docs/workout_plan_selection_guide.md §Step 3. */
    val score: Double,
    /** One-line reason shown to the user, e.g. "Matches your goal and 4 days/week". */
    val rationale: String
)
