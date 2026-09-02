package com.example.homeworkout.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.ui.App
import com.example.homeworkout.ui.core.customworkout.CustomWorkoutListScreen
import com.example.homeworkout.ui.core.details.DetailScreen
import com.example.homeworkout.ui.core.details.DetailViewModel
import com.example.homeworkout.ui.core.editgoal.EditGoalScreen
import com.example.homeworkout.ui.core.editgoal.EditGoalViewModel
import com.example.homeworkout.ui.core.exerciseinfo.ExerciseInfoScreen
import com.example.homeworkout.ui.core.exerciseinfo.ExerciseInfoViewModel
import com.example.homeworkout.ui.core.history.HistoryScreen
import com.example.homeworkout.ui.core.home.HomeScreen
import com.example.homeworkout.ui.core.home.HomeViewModel
import com.example.homeworkout.ui.core.planedit.AddExercisesScreen
import com.example.homeworkout.ui.core.planedit.AlterExerciseScreen
import com.example.homeworkout.ui.core.planedit.EditPlanExercisesScreen
import com.example.homeworkout.ui.core.planedit.ExerciseBrowserViewModel
import com.example.homeworkout.ui.core.planedit.FilterExerciseScreen
import com.example.homeworkout.ui.core.player.WorkoutPlayerScreen
import com.example.homeworkout.ui.core.report.ReportScreen
import com.example.homeworkout.ui.core.settings.GeneralSettingsScreen
import com.example.homeworkout.ui.core.settings.SettingsScreen
import com.example.homeworkout.ui.core.settings.SettingsViewModel
import com.example.homeworkout.ui.core.settings.VoiceOptionsScreen
import com.example.homeworkout.ui.core.settings.WorkoutSettingsScreen
import com.example.homeworkout.ui.core.workoutlist.WorkoutListScreen
import com.example.homeworkout.ui.core.workoutlist.WorkoutListViewModel
import com.example.homeworkout.ui.core.workoutsettings.WorkoutSettingsSheetScreen

private data class BottomTab(val screen: Screen, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Screen.Home, "Training", Icons.Default.Home),
    BottomTab(Screen.Report, "Report", Icons.Default.BarChart),
    BottomTab(Screen.SettingsHome, "Settings", Icons.Default.Person)
)

@Composable
fun ScreenNavigator() {
    val navController = rememberNavController()
    val appInstance = LocalContext.current.applicationContext as App

    val currentRoute by navController.currentBackStackEntryAsState()
    val showBottomBar = currentRoute?.destination?.route in Screen.bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute?.destination?.route == tab.screen.route,
                            onClick = {
                                navController.navigate(tab.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            // --- Bottom bar tabs ---
            composable(Screen.Home.route) {
                val vm: HomeViewModel = viewModel(factory = viewModelFactory {
                    initializer { HomeViewModel(appInstance.getWorkoutsUseCase, appInstance.getWeeklyGoalProgressUseCase) }
                })
                HomeScreen(
                    viewModel = vm,
                    onOpenPlan = { planId -> navController.navigate(Screen.Details.createRoute(planId)) },
                    onOpenCustomWorkout = { navController.navigate(Screen.CustomWorkoutList.route) },
                    onOpenEditGoal = { navController.navigate(Screen.EditGoal.route) },
                    onOpenWorkoutList = { category -> navController.navigate(Screen.WorkoutList.createRoute(category.name)) }
                )
            }

            composable(Screen.Report.route) {
                ReportScreen(onOpenHistory = { navController.navigate(Screen.History.route) })
            }

            composable(Screen.SettingsHome.route) {
                SettingsScreen(
                    onOpenWorkoutSettings = { navController.navigate(Screen.SettingsWorkout.route) },
                    onOpenGeneralSettings = { navController.navigate(Screen.SettingsGeneral.route) },
                    onOpenVoiceOptions = { navController.navigate(Screen.SettingsVoice.route) }
                )
            }

            // --- Workout Screen (plan detail) + editing ---
            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: return@composable
                val vm: DetailViewModel = viewModel(key = "detail-$planId", factory = viewModelFactory {
                    initializer { DetailViewModel(planId, appInstance.getWorkoutDetailsUseCase) }
                })
                DetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onStartWorkout = { navController.navigate(Screen.Player.createRoute(planId)) },
                    onEditExercises = { navController.navigate(Screen.EditPlanExercises.createRoute(planId)) },
                    onOpenExerciseInfo = { exerciseId -> navController.navigate(Screen.ExerciseInfo.createRoute(exerciseId)) },
                    onOpenWorkoutSettings = { navController.navigate(Screen.WorkoutSettingsSheet.route) }
                )
            }

            composable(
                route = Screen.EditPlanExercises.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: return@composable
                val vm: DetailViewModel = viewModel(key = "edit-detail-$planId", factory = viewModelFactory {
                    initializer { DetailViewModel(planId, appInstance.getWorkoutDetailsUseCase) }
                })
                EditPlanExercisesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onAlterExercise = { navController.navigate(Screen.AlterExercise.route) },
                    onAddExercises = { navController.navigate(Screen.AddExercises.route) }
                )
            }

            composable(Screen.AlterExercise.route) {
                val vm: ExerciseBrowserViewModel = viewModel(factory = viewModelFactory {
                    initializer { ExerciseBrowserViewModel(appInstance.searchExercisesUseCase) }
                })
                AlterExerciseScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFilter = { navController.navigate(Screen.FilterExercise.route) },
                    onExerciseInfo = { exerciseId -> navController.navigate(Screen.ExerciseInfo.createRoute(exerciseId)) }
                )
            }

            composable(Screen.AddExercises.route) {
                val vm: ExerciseBrowserViewModel = viewModel(factory = viewModelFactory {
                    initializer { ExerciseBrowserViewModel(appInstance.searchExercisesUseCase) }
                })
                AddExercisesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFilter = { navController.navigate(Screen.FilterExercise.route) },
                    onExerciseInfo = { exerciseId -> navController.navigate(Screen.ExerciseInfo.createRoute(exerciseId)) }
                )
            }

            composable(Screen.FilterExercise.route) {
                FilterExerciseScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.WorkoutSettingsSheet.route) {
                WorkoutSettingsSheetScreen(onDone = { navController.popBackStack() })
            }

            composable(
                route = Screen.ExerciseInfo.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
            ) { entry ->
                val exerciseId = entry.arguments?.getLong("exerciseId") ?: return@composable
                val vm: ExerciseInfoViewModel = viewModel(key = "exercise-info-$exerciseId", factory = viewModelFactory {
                    initializer { ExerciseInfoViewModel(exerciseId, appInstance.getExerciseDetailUseCase) }
                })
                ExerciseInfoScreen(viewModel = vm, onClose = { navController.popBackStack() })
            }

            // --- During Workout ---
            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: return@composable
                val vm: DetailViewModel = viewModel(key = "player-detail-$planId", factory = viewModelFactory {
                    initializer { DetailViewModel(planId, appInstance.getWorkoutDetailsUseCase) }
                })
                WorkoutPlayerScreen(
                    viewModel = vm,
                    onClose = { navController.popBackStack(Screen.Details.route, inclusive = false) },
                    onExerciseInfo = { exerciseId -> navController.navigate(Screen.ExerciseInfo.createRoute(exerciseId)) }
                )
            }

            // --- Training extras ---
            composable(Screen.EditGoal.route) {
                val vm: EditGoalViewModel = viewModel(factory = viewModelFactory {
                    initializer { EditGoalViewModel(appInstance.getSettingsUseCase, appInstance.updateSettingsUseCase) }
                })
                EditGoalScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.WorkoutList.route,
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { entry ->
                val categoryName = entry.arguments?.getString("category") ?: return@composable
                val category = runCatching { WorkoutCategory.valueOf(categoryName) }.getOrElse { return@composable }
                val vm: WorkoutListViewModel = viewModel(key = "workout-list-$categoryName", factory = viewModelFactory {
                    initializer { WorkoutListViewModel(category, appInstance.getWorkoutsUseCase) }
                })
                WorkoutListScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenPlan = { planId -> navController.navigate(Screen.Details.createRoute(planId)) }
                )
            }

            composable(Screen.CustomWorkoutList.route) {
                CustomWorkoutListScreen(onNavigateBack = { navController.popBackStack() })
            }

            // --- Report tab ---
            composable(Screen.History.route) {
                HistoryScreen(onNavigateBack = { navController.popBackStack() })
            }

            // --- Settings tab ---
            composable(Screen.SettingsWorkout.route) {
                val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        SettingsViewModel(
                            appInstance.getSettingsUseCase,
                            appInstance.updateSettingsUseCase,
                            appInstance.resetWorkoutProgressUseCase,
                            appInstance.ttsService,
                            appInstance.reminderScheduler
                        )
                    }
                })
                WorkoutSettingsScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsGeneral.route) {
                val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        SettingsViewModel(
                            appInstance.getSettingsUseCase,
                            appInstance.updateSettingsUseCase,
                            appInstance.resetWorkoutProgressUseCase,
                            appInstance.ttsService,
                            appInstance.reminderScheduler
                        )
                    }
                })
                GeneralSettingsScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsVoice.route) {
                val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        SettingsViewModel(
                            appInstance.getSettingsUseCase,
                            appInstance.updateSettingsUseCase,
                            appInstance.resetWorkoutProgressUseCase,
                            appInstance.ttsService,
                            appInstance.reminderScheduler
                        )
                    }
                })
                VoiceOptionsScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
