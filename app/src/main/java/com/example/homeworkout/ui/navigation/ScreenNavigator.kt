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
import com.example.homeworkout.ui.core.running.RunningViewModel
import com.example.homeworkout.ui.core.running.WalkRunScreen
import com.example.homeworkout.ui.core.running.detail.RunDetailScreen
import com.example.homeworkout.ui.core.running.detail.RunDetailViewModel
import com.example.homeworkout.ui.core.running.history.RunHistoryScreen
import com.example.homeworkout.ui.core.running.history.RunHistoryViewModel
import com.example.homeworkout.ui.core.trainingplan.StructuredTrainingPlanScreen
import com.example.homeworkout.ui.core.trainingplan.StructuredTrainingPlanViewModel
import com.example.homeworkout.ui.core.trainingplayer.StructuredTrainingPlayerScreen
import com.example.homeworkout.ui.core.trainingplayer.StructuredTrainingPlayerViewModel
import com.example.homeworkout.ui.core.formcheck.FormCheckHistoryScreen
import com.example.homeworkout.ui.core.formcheck.FormCheckHistoryViewModel
import com.example.homeworkout.ui.core.formcheck.FormCheckScreen
import com.example.homeworkout.ui.core.formcheck.FormCheckViewModel
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
                            appInstance.getStreakUseCase,
                            appInstance.getActiveWorkoutUseCase
                        )
                    }
                })
                HomeScreen(
                    viewModel = vm,
                    onOpenPlan = { planId -> navController.navigate(Screen.Details.createRoute(planId)) },
                    onOpenCustomWorkout = { navController.navigate(Screen.CustomWorkoutList.route) },
                    onOpenEditGoal = { navController.navigate(Screen.EditGoal.route) },
                    onOpenWorkoutList = { category -> navController.navigate(Screen.WorkoutList.createRoute(category.name)) },
                    onOpenOnboarding = { navController.navigate(Screen.Onboarding.route) },
                    onResumeWorkout = { planId -> navController.navigate(Screen.Player.createRoute(planId, resume = true)) }
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
                    onOpenFoodScanner = { navController.navigate(Screen.FoodScanner.route) },
                    onOpenRunning = { navController.navigate(Screen.WalkRun.route) },
                    onOpenRunHistory = { navController.navigate(Screen.RunHistory.route) },
                    onOpenTrainingPlan = { programId ->
                        val route = if (programId == "walking-weight-loss-20w") {
                            Screen.WalkingPlanDetail.createRoute(programId)
                        } else Screen.RunningPlanDetail.createRoute(programId)
                        navController.navigate(route)
                    },
                    onOpenFormCheck = { navController.navigate(Screen.FormCheck.route) }
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
                    initializer {
                        DetailViewModel(
                            planId,
                            appInstance.getWorkoutDetailsUseCase,
                            appInstance.resolveNextPlanDayUseCase,
                            appInstance.getResumableWorkoutUseCase,
                            appInstance.abandonWorkoutSessionUseCase
                        )
                    }
                })
                DetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onStartWorkout = { pId, planDayId -> navController.navigate(Screen.Player.createRoute(pId, planDayId)) },
                    onResumeWorkout = { pId -> navController.navigate(Screen.Player.createRoute(pId, resume = true)) },
                    onRestartWorkout = { pId, planDayId, oldSessionId ->
                        vm.restart(oldSessionId)
                        navController.navigate(Screen.Player.createRoute(pId, planDayId))
                    },
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
                    initializer {
                        DetailViewModel(
                            planId,
                            appInstance.getWorkoutDetailsUseCase,
                            appInstance.resolveNextPlanDayUseCase,
                            appInstance.getResumableWorkoutUseCase,
                            appInstance.abandonWorkoutSessionUseCase
                        )
                    }
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
                    },
                    navArgument("resume") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: return@composable
                val requestedPlanDayId = entry.arguments?.getLong("planDayId", -1L)?.takeIf { it != -1L }
                val resume = entry.arguments?.getBoolean("resume", false) ?: false
                val vm: WorkoutPlayerViewModel = viewModel(key = "player-$planId", factory = viewModelFactory {
                    initializer {
                        WorkoutPlayerViewModel(
                            planId,
                            requestedPlanDayId,
                            resume,
                            appInstance.startWorkoutSessionUseCase,
                            appInstance.startSpecificWorkoutDayUseCase,
                            appInstance.restartWorkoutDayUseCase,
                            appInstance.completeWorkoutSessionUseCase,
                            appInstance.abandonWorkoutSessionUseCase,
                            appInstance.getResumableWorkoutUseCase,
                            appInstance.saveWorkoutProgressUseCase,
                            appInstance.saveAndExitWorkoutSessionUseCase,
                            appInstance.getExerciseDetailUseCase,
                            appInstance.markBadgesSeenUseCase,
                            appInstance.getSettingsUseCase,
                            appInstance.ttsService,
                            appInstance.tickSoundPlayer
                        )
                    }
                })
                WorkoutPlayerScreen(
                    viewModel = vm,
                    // Plain pop-one-back rather than popBackStack(Screen.Details.route, ...): the
                    // player can be entered either from Details (Start/Resume/Restart) or directly
                    // from Home (the "Continue Workout" card's Resume) — targeting Details by route
                    // silently no-ops when it isn't actually on the back stack, which made every
                    // onClose-driven action (Keep exercising / Do it later / Save & Exit / Discard)
                    // appear to do nothing when entered from Home.
                    onClose = { navController.popBackStack() },
                    onOpenFormCheck = { navController.navigate(Screen.FormCheck.route) }
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

            composable(Screen.WalkRun.route) {
                val vm: RunningViewModel = viewModel(factory = viewModelFactory {
                    initializer { RunningViewModel(appInstance.observeRunningSessionUseCase) }
                })
                WalkRunScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.RunHistory.route) {
                val vm: RunHistoryViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        RunHistoryViewModel(appInstance.getRunHistoryUseCase, appInstance.deleteRunUseCase)
                    }
                })
                RunHistoryScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDetail = { runId -> navController.navigate(Screen.RunDetail.createRoute(runId)) }
                )
            }

            composable(
                route = Screen.RunDetail.route,
                arguments = listOf(navArgument("runId") { type = NavType.LongType })
            ) { entry ->
                val runId = entry.arguments?.getLong("runId") ?: return@composable
                val vm: RunDetailViewModel = viewModel(key = "run-detail-$runId", factory = viewModelFactory {
                    initializer { RunDetailViewModel(runId, appInstance.getRunDetailUseCase) }
                })
                RunDetailScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.RunningPlanDetail.route,
                arguments = listOf(navArgument("programId") { type = NavType.StringType })
            ) { entry ->
                val programId = entry.arguments?.getString("programId") ?: return@composable
                val vm: StructuredTrainingPlanViewModel = viewModel(key = "running-plan-$programId", factory = viewModelFactory {
                    initializer {
                        StructuredTrainingPlanViewModel(
                            programId,
                            appInstance.getTrainingProgramUseCase,
                            appInstance.getTrainingProgressUseCase,
                            appInstance.enrollTrainingProgramUseCase,
                            appInstance.startStructuredSessionUseCase,
                            appInstance.repeatStructuredWeekUseCase
                        )
                    }
                })
                StructuredTrainingPlanScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onStartSession = { progId, sessionId ->
                        navController.navigate(Screen.RunningPlayer.createRoute(progId, sessionId))
                    }
                )
            }

            composable(
                route = Screen.WalkingPlanDetail.route,
                arguments = listOf(navArgument("programId") { type = NavType.StringType })
            ) { entry ->
                val programId = entry.arguments?.getString("programId") ?: return@composable
                val vm: StructuredTrainingPlanViewModel = viewModel(key = "walking-plan-$programId", factory = viewModelFactory {
                    initializer {
                        StructuredTrainingPlanViewModel(
                            programId,
                            appInstance.getTrainingProgramUseCase,
                            appInstance.getTrainingProgressUseCase,
                            appInstance.enrollTrainingProgramUseCase,
                            appInstance.startStructuredSessionUseCase,
                            appInstance.repeatStructuredWeekUseCase
                        )
                    }
                })
                StructuredTrainingPlanScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onStartSession = { progId, sessionId ->
                        navController.navigate(Screen.WalkingPlayer.createRoute(progId, sessionId))
                    }
                )
            }

            composable(
                route = Screen.RunningPlayer.route,
                arguments = listOf(
                    navArgument("programId") { type = NavType.StringType },
                    navArgument("sessionId") { type = NavType.StringType }
                )
            ) { entry ->
                val programId = entry.arguments?.getString("programId") ?: return@composable
                val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
                val vm: StructuredTrainingPlayerViewModel = viewModel(key = "running-player-$programId-$sessionId", factory = viewModelFactory {
                    initializer {
                        StructuredTrainingPlayerViewModel(
                            programId, sessionId,
                            appInstance.getTrainingProgramUseCase,
                            appInstance.startStructuredSessionUseCase,
                            appInstance.completeStructuredSessionUseCase,
                            appInstance.observeRunningSessionUseCase,
                            appInstance.getSettingsUseCase,
                            appInstance.ttsService,
                            appInstance.tickSoundPlayer
                        )
                    }
                })
                StructuredTrainingPlayerScreen(vm, onClose = { navController.popBackStack() })
            }

            composable(
                route = Screen.WalkingPlayer.route,
                arguments = listOf(
                    navArgument("programId") { type = NavType.StringType },
                    navArgument("sessionId") { type = NavType.StringType }
                )
            ) { entry ->
                val programId = entry.arguments?.getString("programId") ?: return@composable
                val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
                val vm: StructuredTrainingPlayerViewModel = viewModel(key = "walking-player-$programId-$sessionId", factory = viewModelFactory {
                    initializer {
                        StructuredTrainingPlayerViewModel(
                            programId, sessionId,
                            appInstance.getTrainingProgramUseCase,
                            appInstance.startStructuredSessionUseCase,
                            appInstance.completeStructuredSessionUseCase,
                            appInstance.observeRunningSessionUseCase,
                            appInstance.getSettingsUseCase,
                            appInstance.ttsService,
                            appInstance.tickSoundPlayer
                        )
                    }
                })
                StructuredTrainingPlayerScreen(vm, onClose = { navController.popBackStack() })
            }

            composable(Screen.FormCheck.route) {
                val vm: FormCheckViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        FormCheckViewModel(
                            appInstance.analyzeFormVideoUseCase,
                            appInstance.saveFormCheckResultUseCase
                        )
                    }
                })
                FormCheckScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenHistory = { navController.navigate(Screen.FormCheckHistory.route) }
                )
            }

            composable(Screen.FormCheckHistory.route) {
                val vm: FormCheckHistoryViewModel = viewModel(factory = viewModelFactory {
                    initializer { FormCheckHistoryViewModel(appInstance.getFormCheckHistoryUseCase) }
                })
                FormCheckHistoryScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
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
