package com.example.homeworkout.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home_screen")
    object Details : Screen("details_screen/{workoutId}") {
        fun createRoute(workoutId: Int) = "details_screen/$workoutId"
    }
}
