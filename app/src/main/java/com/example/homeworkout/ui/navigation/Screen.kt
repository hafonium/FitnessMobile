package com.example.homeworkout.ui.navigation

/** Every destination in the app. Route params are typed via the `createRoute` helpers below. */
sealed class Screen(val route: String) {
    // Bottom navigation bar tabs
    object Home : Screen("home")
    object Discovery : Screen("discovery")
    object Report : Screen("report")
    object SettingsHome : Screen("settings")

    // Training -> Workout Screen (plan detail) and its editing flows
    object Details : Screen("plan_detail/{planId}") {
        fun createRoute(planId: Long) = "plan_detail/$planId"
    }
    object EditPlanExercises : Screen("edit_plan/{planId}") {
        fun createRoute(planId: Long) = "edit_plan/$planId"
    }
    object AlterExercise : Screen("alter_exercise/{planExerciseId}") {
        fun createRoute(planExerciseId: Long) = "alter_exercise/$planExerciseId"
    }
    object AddExercises : Screen("add_exercises/{planDayId}") {
        fun createRoute(planDayId: Long) = "add_exercises/$planDayId"
    }
    object WorkoutSettingsSheet : Screen("workout_settings_sheet")
    object ExerciseInfo : Screen("exercise_info/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "exercise_info/$exerciseId"
    }

    // During Workout
    object Player : Screen("player/{planId}?planDayId={planDayId}&resume={resume}") {
        fun createRoute(planId: Long, planDayId: Long? = null, resume: Boolean = false): String {
            var route = "player/$planId"
            if (planDayId != null) route += "?planDayId=$planDayId"
            if (resume) route += if (planDayId != null) "&resume=true" else "?resume=true"
            return route
        }
    }

    // Training extras
    object Onboarding : Screen("onboarding")
    object EditGoal : Screen("edit_goal")
    object WorkoutList : Screen("workout_list/{category}") {
        fun createRoute(category: String) = "workout_list/$category"
    }
    object CustomWorkoutList : Screen("custom_workout_list")
    object CreateCustomPlan : Screen("create_custom_plan")

    // Discovery tab
    object FoodScanner : Screen("food_scanner")

    // Report tab
    object History : Screen("history")
    object Achievements : Screen("achievements")
    object Weight : Screen("weight")

    // Settings tab
    object SettingsWorkout : Screen("settings_workout")
    object SettingsGeneral : Screen("settings_general")
    object SettingsVoice : Screen("settings_voice")

    companion object {
        // NOTE: must not eagerly reference the `object` subclasses here — the companion
        // initializes during Screen.<clinit>, before those objects exist. Hence string literals.
        val bottomBarRoutes = setOf("home", "discovery", "report", "settings")
    }
}
