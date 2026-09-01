package com.example.homeworkout.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homeworkout.ui.App
import com.example.homeworkout.ui.core.details.DetailScreen
import com.example.homeworkout.ui.core.details.DetailViewModel
import com.example.homeworkout.ui.core.home.HomeScreen
import com.example.homeworkout.ui.core.home.HomeViewModel

@Composable
fun ScreenNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appInstance = context.applicationContext as App

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        HomeViewModel(appInstance.getWorkoutsUseCase)
                    }
                }
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToDetails = { workoutId ->
                    navController.navigate(Screen.Details.createRoute(workoutId))
                }
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
            val detailViewModel: DetailViewModel = viewModel(
                key = "details-$workoutId",
                factory = viewModelFactory {
                    initializer {
                        DetailViewModel(workoutId, appInstance.getWorkoutDetailsUseCase)
                    }
                }
            )
            DetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
