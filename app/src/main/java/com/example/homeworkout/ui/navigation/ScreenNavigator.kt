package com.example.homeworkout.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.homeworkout.ui.core.achievements.AchievementsScreen
import com.example.homeworkout.ui.core.achievements.AchievementsViewModel
import com.example.homeworkout.ui.core.customworkout.CreateCustomPlanScreen
import com.example.homeworkout.ui.core.customworkout.CreateCustomPlanViewModel
import com.example.homeworkout.ui.core.customworkout.CustomWorkoutListScreen
import com.example.homeworkout.ui.core.customworkout.CustomWorkoutListViewModel
import com.example.homeworkout.ui.core.details.DetailScreen
import com.example.homeworkout.ui.core.details.DetailViewModel
import com.example.homeworkout.ui.core.discovery.DiscoveryScreen
import com.example.homeworkout.ui.core.editgoal.EditGoalScreen
import com.example.homeworkout.ui.core.editgoal.EditGoalViewModel
import com.example.homeworkout.ui.core.exerciseinfo.ExerciseInfoScreen
import com.example.homeworkout.ui.core.exerciseinfo.ExerciseInfoViewModel
import com.example.homeworkout.ui.core.foodscan.FoodScanScreen
import com.example.homeworkout.ui.core.foodscan.FoodScanViewModel
import com.example.homeworkout.ui.core.history.HistoryScreen
import com.example.homeworkout.ui.core.history.HistoryViewModel
import com.example.homeworkout.ui.core.home.HomeScreen
import com.example.homeworkout.ui.core.home.HomeViewModel
import com.example.homeworkout.ui.core.onboarding.OnboardingScreen
import com.example.homeworkout.ui.core.onboarding.OnboardingViewModel
import com.example.homeworkout.ui.core.planedit.AddExercisesScreen
import com.example.homeworkout.ui.core.planedit.AlterExerciseScreen
import com.example.homeworkout.ui.core.planedit.EditPlanExercisesScreen
import com.example.homeworkout.ui.core.planedit.ExerciseBrowserViewModel
import com.example.homeworkout.ui.core.planedit.PlanExerciseEditViewModel
import com.example.homeworkout.ui.core.player.WorkoutPlayerScreen
import com.example.homeworkout.ui.core.player.WorkoutPlayerViewModel
import com.example.homeworkout.ui.core.report.ReportScreen
import com.example.homeworkout.ui.core.report.ReportViewModel
import com.example.homeworkout.ui.core.settings.GeneralSettingsScreen
import com.example.homeworkout.ui.core.settings.SettingsScreen
import com.example.homeworkout.ui.core.settings.SettingsViewModel
import com.example.homeworkout.ui.core.settings.VoiceOptionsScreen
import com.example.homeworkout.ui.core.settings.WorkoutSettingsScreen
import com.example.homeworkout.ui.core.workoutlist.WorkoutListScreen
import com.example.homeworkout.ui.core.workoutlist.WorkoutListViewModel
import com.example.homeworkout.ui.core.workoutsettings.WorkoutSettingsSheetScreen
import com.example.homeworkout.ui.core.weight.WeightScreen
import com.example.homeworkout.ui.core.weight.WeightViewModel

private data class BottomTab(val screen: Screen, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Screen.Home, "Training", Icons.Default.Home),
    BottomTab(Screen.Discovery, "Discovery", Icons.Default.Explore),
    BottomTab(Screen.Report, "Report", Icons.Default.BarChart),
    BottomTab(Screen.SettingsHome, "Settings", Icons.Default.Person)
)

@Composable
fun ScreenNavigator() {
    val navController = rememberNavController()
    val appInstance = LocalContext.current.applicationContext as App

    // The floating chat overlay lives outside this NavHost and has no NavController reference; it
    // posts a proposal here instead - see ChatPanelController's KDoc and docs/chatbot-feature.md.
    val pendingPlanProposal by appInstance.chatPanelController.pendingPlanProposal.collectAsStateWithLifecycle()
    LaunchedEffect(pendingPlanProposal) {
        if (pendingPlanProposal != null) {
            navController.navigate(Screen.CreateCustomPlan.route)
        }
    }

    val currentRoute by navController.currentBackStackEntryAsState()
    val showBottomBar = currentRoute?.destination?.route in Screen.bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
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
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
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
                    initializer {
                        HomeViewModel(
                            appInstance.getWorkoutsUseCase,
                            appInstance.getFitnessProfileUseCase,
                            appInstance.recommendPlanUseCase,
                            appInstance.getWeeklyGoalProgressUseCase,
                            appInstance.getStreakUseCase
                        )
                    }
                })
                HomeScreen(
                    viewModel = vm,
                    onOpenPlan = { planId -> navController.navigate(Screen.Details.createRoute(planId)) },
                    onOpenCustomWorkout = { navController.navigate(Screen.CustomWorkoutList.route) },
                    onOpenEditGoal = { navController.navigate(Screen.EditGoal.route) },
                    onOpenWorkoutList = { category -> navController.navigate(Screen.WorkoutList.createRoute(category.name)) },
                    onOpenOnboarding = { navController.navigate(Screen.Onboarding.route) }
                )
            }

            composable(Screen.Report.route) {
                val vm: ReportViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        ReportViewModel(
                            appInstance.getStreakUseCase,
                            appInstance.getWeightDashboardUseCase,
                            appInstance.getBadgesUseCase,
                            appInstance.getWeeklyGoalProgressUseCase,
                            appInstance.getWorkoutHistoryUseCase,
                            appInstance.evaluateBadgesUseCase,
                            appInstance.markBadgesSeenUseCase
                        )
                    }
                })
                ReportScreen(
                    viewModel = vm,
                    onOpenHistory = { navController.navigate(Screen.History.route) },
                    onOpenAchievements = { navController.navigate(Screen.Achievements.route) },
                    onOpenWeight = { navController.navigate(Screen.Weight.route) }
                )
            }

            composable(Screen.Discovery.route) {
                DiscoveryScreen(
                    onOpenFoodScanner = { navController.navigate(Screen.FoodScanner.route) }
                )
            }

            composable(Screen.SettingsHome.route) {
                SettingsScreen(
                    onOpenPlanSetup = { navController.navigate(Screen.Onboarding.route) },
                    onOpenWorkoutSettings = { navController.navigate(Screen.SettingsWorkout.route) },
                    onOpenGeneralSettings = { navController.navigate(Screen.SettingsGeneral.route) },
                    onOpenVoiceOptions = { navController.navigate(Screen.SettingsVoice.route) }
                )
            }

            composable(Screen.Onboarding.route) {
                val vm: OnboardingViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        OnboardingViewModel(
                            appInstance.recommendPlanUseCase,
                            appInstance.saveFitnessProfileUseCase,
                            appInstance.getFitnessProfileUseCase
                        )
                    }
                })
                OnboardingScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenPlan = { planId ->
                        navController.navigate(Screen.Details.createRoute(planId)) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // --- Workout Screen (plan detail) + editing ---
            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: return@composable
                val vm: DetailViewModel = viewModel(key = "detail-$planId", factory = viewModelFactory {
                    initializer { DetailViewModel(planId, appInstance.getWorkoutDetailsUseCase, appInstance.resolveNextPlanDayUseCase) }
                })
                DetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onStartWorkout = { pId, planDayId -> navController.navigate(Screen.Player.createRoute(pId, planDayId)) },
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
                    initializer { DetailViewModel(planId, appInstance.getWorkoutDetailsUseCase, appInstance.resolveNextPlanDayUseCase) }
                })
                val editVm: PlanExerciseEditViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        PlanExerciseEditViewModel(
                            appInstance.addExercisesToPlanDayUseCase,
                            appInstance.replacePlanExerciseUseCase,
                            appInstance.updatePlanExerciseRepsUseCase,
                            appInstance.deletePlanExerciseUseCase,
                            appInstance.reorderPlanExercisesUseCase
                        )
                    }
                })

                EditPlanExercisesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onAlterExercise = { planExerciseId -> navController.navigate(Screen.AlterExercise.createRoute(planExerciseId)) },
                    onAddExercises = { planDayId -> navController.navigate(Screen.AddExercises.createRoute(planDayId)) },
                    onUpdateReps = { planExerciseId, reps -> editVm.updateReps(planExerciseId, reps) },
                    onDeleteExercise = editVm::deleteExercise,
                    onReorder = editVm::reorderExercises
                )
            }

            composable(
                route = Screen.AlterExercise.route,
                arguments = listOf(navArgument("planExerciseId") { type = NavType.LongType })
            ) { entry ->
                val planExerciseId = entry.arguments?.getLong("planExerciseId") ?: return@composable
                val vm: ExerciseBrowserViewModel = viewModel(factory = viewModelFactory {
                    initializer { ExerciseBrowserViewModel(appInstance.searchExercisesUseCase) }
                })
                val editVm: PlanExerciseEditViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        PlanExerciseEditViewModel(
                            appInstance.addExercisesToPlanDayUseCase,
                            appInstance.replacePlanExerciseUseCase,
                            appInstance.updatePlanExerciseRepsUseCase,
                            appInstance.deletePlanExerciseUseCase,
                            appInstance.reorderPlanExercisesUseCase
                        )
                    }
                })

                AlterExerciseScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onExerciseInfo = { exerciseId -> navController.navigate(Screen.ExerciseInfo.createRoute(exerciseId)) },
                    onReplaceExercise = { newExerciseId ->
                        editVm.replaceExercise(planExerciseId, newExerciseId) { navController.popBackStack() }
                    }
                )
            }

            composable(
                route = Screen.AddExercises.route,
                arguments = listOf(navArgument("planDayId") { type = NavType.LongType })
            ) { entry ->
                val planDayId = entry.arguments?.getLong("planDayId") ?: return@composable
                val vm: ExerciseBrowserViewModel = viewModel(factory = viewModelFactory {
                    initializer { ExerciseBrowserViewModel(appInstance.searchExercisesUseCase) }
                })
                val editVm: PlanExerciseEditViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        PlanExerciseEditViewModel(
                            appInstance.addExercisesToPlanDayUseCase,
                            appInstance.replacePlanExerciseUseCase,
                            appInstance.updatePlanExerciseRepsUseCase,
                            appInstance.deletePlanExerciseUseCase,
                            appInstance.reorderPlanExercisesUseCase
                        )
                    }
                })

                AddExercisesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onExerciseInfo = { exerciseId -> navController.navigate(Screen.ExerciseInfo.createRoute(exerciseId)) },
                    onAddExercises = { exerciseIds ->
                        editVm.addExercises(planDayId, exerciseIds) { navController.popBackStack() }
                    }
                )
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
                arguments = listOf(
                    navArgument("planId") { type = NavType.LongType },
                    navArgument("planDayId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: return@composable
                val requestedPlanDayId = entry.arguments?.getLong("planDayId", -1L)?.takeIf { it != -1L }
                val vm: WorkoutPlayerViewModel = viewModel(key = "player-$planId", factory = viewModelFactory {
                    initializer {
                        WorkoutPlayerViewModel(
                            planId,
                            requestedPlanDayId,
                            appInstance.startWorkoutSessionUseCase,
                            appInstance.startSpecificWorkoutDayUseCase,
                            appInstance.restartWorkoutDayUseCase,
                            appInstance.completeWorkoutSessionUseCase,
                            appInstance.abandonWorkoutSessionUseCase,
                            appInstance.markBadgesSeenUseCase,
                            appInstance.getSettingsUseCase,
                            appInstance.ttsService,
                            appInstance.tickSoundPlayer
                        )
                    }
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
                val vm: CustomWorkoutListViewModel = viewModel(factory = viewModelFactory {
                    initializer { CustomWorkoutListViewModel(appInstance.getWorkoutsUseCase, appInstance.deleteCustomWorkoutPlanUseCase) }
                })
                CustomWorkoutListScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onCreatePlan = { navController.navigate(Screen.CreateCustomPlan.route) },
                    onOpenPlan = { planId -> navController.navigate(Screen.Details.createRoute(planId)) }
                )
            }

            composable(Screen.CreateCustomPlan.route) {
                val vm: CreateCustomPlanViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        CreateCustomPlanViewModel(
                            appInstance.getExercisesByIdsUseCase,
                            appInstance.createCustomWorkoutPlanUseCase,
                            appInstance.getWorkoutsUseCase,
                            appInstance.getWorkoutDetailsUseCase,
                            appInstance.recommendPlanUseCase,
                            appInstance.saveFitnessProfileUseCase
                        )
                    }
                })
                val exerciseBrowserVm: ExerciseBrowserViewModel = viewModel(factory = viewModelFactory {
                    initializer { ExerciseBrowserViewModel(appInstance.searchExercisesUseCase) }
                })

                // A pending proposal means the chat overlay triggered this navigation - see
                // ChatPanelController's KDoc. Consumed exactly once per screen instance so a manual
                // "+ Create Workout" entry (proposal == null) is completely unaffected.
                var cameFromChat by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    appInstance.chatPanelController.consumePendingPlanProposal()?.let { proposal ->
                        cameFromChat = true
                        vm.applyProposal(proposal)
                    }
                }

                CreateCustomPlanScreen(
                    viewModel = vm,
                    exerciseBrowserViewModel = exerciseBrowserVm,
                    onNavigateBack = {
                        if (cameFromChat) appInstance.chatPanelController.open()
                        navController.popBackStack()
                    },
                    onExerciseInfo = { exerciseId -> navController.navigate(Screen.ExerciseInfo.createRoute(exerciseId)) },
                    onPlanCreated = { planId ->
                        if (cameFromChat) {
                            appInstance.chatPanelController.open()
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Details.createRoute(planId)) {
                                popUpTo(Screen.CreateCustomPlan.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // --- Report tab ---
            composable(Screen.History.route) {
                val vm: HistoryViewModel = viewModel(factory = viewModelFactory {
                    initializer { HistoryViewModel(appInstance.getWorkoutHistoryUseCase) }
                })
                HistoryScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Achievements.route) {
                val vm: AchievementsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        AchievementsViewModel(
                            appInstance.getBadgesUseCase,
                            appInstance.evaluateBadgesUseCase
                        )
                    }
                })
                AchievementsScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Weight.route) {
                val vm: WeightViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        WeightViewModel(
                            appInstance.getWeightDashboardUseCase,
                            appInstance.recordWeightUseCase,
                            appInstance.updateHeightUseCase
                        )
                    }
                })
                WeightScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.FoodScanner.route) {
                val vm: FoodScanViewModel = viewModel(factory = viewModelFactory {
                    initializer { FoodScanViewModel(appInstance.analyzeFoodImageUseCase) }
                })
                FoodScanScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
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
