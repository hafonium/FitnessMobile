package com.example.homeworkout.ui

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.homeworkout.BuildConfig
import com.example.homeworkout.data.catalog.WorkoutPlanCatalogSource
import com.example.homeworkout.data.catalog.StructuredTrainingCatalogSource
import com.example.homeworkout.data.local.AppDatabase
import com.example.homeworkout.data.remote.groq.GroqClient
import com.example.homeworkout.data.remote.gemini.GeminiFormCheckApi
import com.example.homeworkout.data.repositories.ChatRepositoryImpl
import com.example.homeworkout.data.remote.SpoonacularFoodApi
import com.example.homeworkout.data.repositories.ExerciseRepositoryImpl
import com.example.homeworkout.data.repositories.BadgeRepositoryImpl
import com.example.homeworkout.data.repositories.FitnessProfileRepositoryImpl
import com.example.homeworkout.data.repositories.FoodAnalysisRepositoryImpl
import com.example.homeworkout.data.repositories.FormCheckRepositoryImpl
import com.example.homeworkout.data.repositories.SettingsRepositoryImpl
import com.example.homeworkout.data.repositories.WorkoutRepositoryImpl
import com.example.homeworkout.data.repositories.WorkoutSessionRepositoryImpl
import com.example.homeworkout.data.repositories.RunningRepositoryImpl
import com.example.homeworkout.data.repositories.StructuredTrainingProgressRepositoryImpl
import com.example.homeworkout.domain.repositories.ChatRepository
import com.example.homeworkout.data.repositories.WeightRepositoryImpl
import com.example.homeworkout.domain.repositories.ExerciseRepository
import com.example.homeworkout.domain.repositories.BadgeRepository
import com.example.homeworkout.domain.repositories.FitnessProfileRepository
import com.example.homeworkout.domain.repositories.FoodAnalysisRepository
import com.example.homeworkout.domain.repositories.FormCheckRepository
import com.example.homeworkout.domain.repositories.PlanCatalogRepository
import com.example.homeworkout.domain.repositories.SettingsRepository
import com.example.homeworkout.domain.repositories.WorkoutRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import com.example.homeworkout.domain.repositories.WeightRepository
import com.example.homeworkout.domain.repositories.RunningRepository
import com.example.homeworkout.domain.repositories.StructuredTrainingCatalogRepository
import com.example.homeworkout.domain.repositories.StructuredTrainingProgressRepository
import com.example.homeworkout.domain.usecases.customworkout.CreateCustomWorkoutPlanUseCase
import com.example.homeworkout.domain.usecases.badges.EvaluateBadgesUseCase
import com.example.homeworkout.domain.usecases.chat.CreateChatSessionUseCase
import com.example.homeworkout.domain.usecases.chat.DeleteChatSessionUseCase
import com.example.homeworkout.domain.usecases.chat.GetChatMessagesUseCase
import com.example.homeworkout.domain.usecases.chat.GetChatSessionsUseCase
import com.example.homeworkout.domain.usecases.chat.SendChatMessageUseCase
import com.example.homeworkout.domain.usecases.badges.GetBadgesUseCase
import com.example.homeworkout.domain.usecases.badges.MarkBadgesSeenUseCase
import com.example.homeworkout.domain.usecases.customworkout.DeleteCustomWorkoutPlanUseCase
import com.example.homeworkout.domain.usecases.customworkout.GetExercisesByIdsUseCase
import com.example.homeworkout.domain.usecases.details.GetWorkoutDetailsUseCase
import com.example.homeworkout.domain.usecases.exerciseinfo.GetExerciseDetailUseCase
import com.example.homeworkout.domain.usecases.exercises.SearchExercisesUseCase
import com.example.homeworkout.domain.usecases.food.AnalyzeFoodImageUseCase
import com.example.homeworkout.domain.usecases.formcheck.AnalyzeFormVideoUseCase
import com.example.homeworkout.domain.usecases.formcheck.GetFormCheckHistoryUseCase
import com.example.homeworkout.domain.usecases.formcheck.SaveFormCheckResultUseCase
import com.example.homeworkout.domain.usecases.home.GetActiveWorkoutUseCase
import com.example.homeworkout.domain.usecases.home.GetWeeklyGoalProgressUseCase
import com.example.homeworkout.domain.usecases.home.GetWorkoutsUseCase
import com.example.homeworkout.domain.usecases.history.GetWorkoutHistoryUseCase
import com.example.homeworkout.domain.usecases.planedit.AddExercisesToPlanDayUseCase
import com.example.homeworkout.domain.usecases.planedit.DeletePlanExerciseUseCase
import com.example.homeworkout.domain.usecases.planedit.ReplacePlanExerciseUseCase
import com.example.homeworkout.domain.usecases.planedit.ReorderPlanExercisesUseCase
import com.example.homeworkout.domain.usecases.planedit.UpdatePlanExerciseRepsUseCase
import com.example.homeworkout.domain.usecases.planselection.GetFitnessProfileUseCase
import com.example.homeworkout.domain.usecases.planselection.RecommendPlanUseCase
import com.example.homeworkout.domain.usecases.planselection.SaveFitnessProfileUseCase
import com.example.homeworkout.domain.usecases.player.AbandonWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.player.CompleteWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.player.GetResumableWorkoutUseCase
import com.example.homeworkout.domain.usecases.player.ResolveNextPlanDayUseCase
import com.example.homeworkout.domain.usecases.player.RestartWorkoutDayUseCase
import com.example.homeworkout.domain.usecases.player.SaveAndExitWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.player.SaveWorkoutProgressUseCase
import com.example.homeworkout.domain.usecases.player.StartSpecificWorkoutDayUseCase
import com.example.homeworkout.domain.usecases.player.StartWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.report.GetStreakUseCase
import com.example.homeworkout.domain.usecases.report.GetWeightDashboardUseCase
import com.example.homeworkout.domain.usecases.report.RecordWeightUseCase
import com.example.homeworkout.domain.usecases.report.UpdateHeightUseCase
import com.example.homeworkout.domain.usecases.settings.GetSettingsUseCase
import com.example.homeworkout.domain.usecases.settings.ResetWorkoutProgressUseCase
import com.example.homeworkout.domain.usecases.settings.UpdateSettingsUseCase
import com.example.homeworkout.domain.usecases.running.ObserveRunningSessionUseCase
import com.example.homeworkout.domain.usecases.running.DeleteRunUseCase
import com.example.homeworkout.domain.usecases.running.GetRunDetailUseCase
import com.example.homeworkout.domain.usecases.running.GetRunHistoryUseCase
import com.example.homeworkout.domain.usecases.training.CompleteStructuredSessionUseCase
import com.example.homeworkout.domain.usecases.training.EnrollTrainingProgramUseCase
import com.example.homeworkout.domain.usecases.training.GetTrainingProgramUseCase
import com.example.homeworkout.domain.usecases.training.GetTrainingProgressUseCase
import com.example.homeworkout.domain.usecases.training.RepeatStructuredWeekUseCase
import com.example.homeworkout.domain.usecases.training.StartStructuredSessionUseCase
import com.example.homeworkout.ui.core.chat.ChatPanelController
import com.example.homeworkout.ui.services.ReminderScheduler
import com.example.homeworkout.ui.services.TickSoundPlayer
import com.example.homeworkout.ui.services.TtsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.maplibre.android.MapLibre
import kotlinx.coroutines.launch

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }

    // Lives for the whole process — used once to seed the database on first launch.
    val applicationScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // Room database
    private val database: AppDatabase by lazy { AppDatabase.getInstance(this, applicationScope) }

    // Repositories
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepositoryImpl(database.workoutPlanDao(), database.userDao()) }
    val exerciseRepository: ExerciseRepository by lazy { ExerciseRepositoryImpl(database.exerciseDao()) }
    val planCatalogRepository: PlanCatalogRepository by lazy { WorkoutPlanCatalogSource(this) }
    val fitnessProfileRepository: FitnessProfileRepository by lazy { FitnessProfileRepositoryImpl(database.userDao()) }
    val badgeRepository: BadgeRepository by lazy { BadgeRepositoryImpl(database.userDao(), database.badgeDao()) }
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(database.userDao(), database.workoutSessionDao(), database.badgeDao())
    }
    val workoutSessionRepository: WorkoutSessionRepository by lazy { WorkoutSessionRepositoryImpl(database.userDao(), database.workoutSessionDao()) }
    val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(database.chatDao(), database.userDao(), GroqClient(), fitnessProfileRepository, workoutRepository)
    }
    // Bridges the floating chat overlay (no nav-graph reference) and ScreenNavigator (no chat
    // reference) - see ChatPanelController's own KDoc and docs/chatbot-feature.md.
    val chatPanelController: ChatPanelController by lazy { ChatPanelController() }
    val weightRepository: WeightRepository by lazy {
        WeightRepositoryImpl(database, database.userDao(), database.weightLogDao())
    }
    val foodAnalysisRepository: FoodAnalysisRepository by lazy {
        FoodAnalysisRepositoryImpl(SpoonacularFoodApi(BuildConfig.SPOONACULAR_API_KEY))
    }
    val runningRepository: RunningRepository by lazy {
        RunningRepositoryImpl(database.runningDao(), database.weightLogDao(), database.userDao())
    }
    val structuredTrainingCatalogRepository: StructuredTrainingCatalogRepository by lazy {
        StructuredTrainingCatalogSource(this)
    }
    val structuredTrainingProgressRepository: StructuredTrainingProgressRepository by lazy {
        StructuredTrainingProgressRepositoryImpl(database.structuredTrainingDao()) }
    val formCheckRepository: FormCheckRepository by lazy {
        val geminiFormCheckApi = GeminiFormCheckApi(BuildConfig.GEMINI_API_KEY)
        // Debug-only, one-time diagnostic (see GeminiFormCheckApi.logAvailableModels KDoc): logs
        // every model this API key can actually call, so a 404 from a retired model alias can be
        // root-caused from Logcat instead of guessed at from Google's deprecation messages. Fires
        // once, the first time this screen's dependencies are touched - never in release builds,
        // never on the request path itself.
        if (BuildConfig.DEBUG) {
            applicationScope.launch { geminiFormCheckApi.logAvailableModels() }
        }
        FormCheckRepositoryImpl(
            geminiFormCheckApi,
            database.formCheckResultDao(),
            database.userDao()
        )
    }

    // Services
    val ttsService: TtsService by lazy { TtsService(this) }
    val tickSoundPlayer: TickSoundPlayer by lazy { TickSoundPlayer(this) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }

    // Use Cases
    val getWorkoutsUseCase by lazy { GetWorkoutsUseCase(workoutRepository) }
    val analyzeFoodImageUseCase by lazy { AnalyzeFoodImageUseCase(foodAnalysisRepository) }
    val analyzeFormVideoUseCase by lazy { AnalyzeFormVideoUseCase(formCheckRepository) }
    val saveFormCheckResultUseCase by lazy { SaveFormCheckResultUseCase(formCheckRepository) }
    val getFormCheckHistoryUseCase by lazy { GetFormCheckHistoryUseCase(formCheckRepository) }
    val getWorkoutDetailsUseCase by lazy { GetWorkoutDetailsUseCase(workoutRepository) }
    val getExerciseDetailUseCase by lazy { GetExerciseDetailUseCase(exerciseRepository) }
    val searchExercisesUseCase by lazy { SearchExercisesUseCase(exerciseRepository) }
    val recommendPlanUseCase by lazy { RecommendPlanUseCase(planCatalogRepository, workoutRepository) }
    val saveFitnessProfileUseCase by lazy { SaveFitnessProfileUseCase(fitnessProfileRepository, planCatalogRepository) }
    val getFitnessProfileUseCase by lazy { GetFitnessProfileUseCase(fitnessProfileRepository) }
    val getSettingsUseCase by lazy { GetSettingsUseCase(settingsRepository) }
    val updateSettingsUseCase by lazy { UpdateSettingsUseCase(settingsRepository) }
    val resetWorkoutProgressUseCase by lazy { ResetWorkoutProgressUseCase(settingsRepository) }
    val getWeeklyGoalProgressUseCase by lazy { GetWeeklyGoalProgressUseCase(settingsRepository, workoutSessionRepository) }
    val getWorkoutHistoryUseCase by lazy { GetWorkoutHistoryUseCase(workoutSessionRepository) }
    val getStreakUseCase by lazy { GetStreakUseCase(workoutSessionRepository) }
    val getWeightDashboardUseCase by lazy { GetWeightDashboardUseCase(weightRepository) }
    val recordWeightUseCase by lazy { RecordWeightUseCase(weightRepository) }
    val updateHeightUseCase by lazy { UpdateHeightUseCase(weightRepository) }
    val getBadgesUseCase by lazy { GetBadgesUseCase(badgeRepository, workoutSessionRepository, getStreakUseCase) }
    val evaluateBadgesUseCase by lazy { EvaluateBadgesUseCase(getBadgesUseCase, badgeRepository) }
    val markBadgesSeenUseCase by lazy { MarkBadgesSeenUseCase(badgeRepository) }
    val resolveNextPlanDayUseCase by lazy { ResolveNextPlanDayUseCase(workoutRepository, workoutSessionRepository) }
    val startWorkoutSessionUseCase by lazy { StartWorkoutSessionUseCase(resolveNextPlanDayUseCase, workoutSessionRepository) }
    val startSpecificWorkoutDayUseCase by lazy { StartSpecificWorkoutDayUseCase(workoutRepository, workoutSessionRepository) }
    val restartWorkoutDayUseCase by lazy { RestartWorkoutDayUseCase(workoutSessionRepository) }
    val completeWorkoutSessionUseCase by lazy {
        CompleteWorkoutSessionUseCase(workoutSessionRepository, evaluateBadgesUseCase)
    }
    val abandonWorkoutSessionUseCase by lazy { AbandonWorkoutSessionUseCase(workoutSessionRepository) }
    val getResumableWorkoutUseCase by lazy { GetResumableWorkoutUseCase(workoutRepository, workoutSessionRepository) }
    val getActiveWorkoutUseCase by lazy { GetActiveWorkoutUseCase(workoutSessionRepository, workoutRepository) }
    val saveWorkoutProgressUseCase by lazy { SaveWorkoutProgressUseCase(workoutSessionRepository) }
    val saveAndExitWorkoutSessionUseCase by lazy { SaveAndExitWorkoutSessionUseCase(workoutSessionRepository) }
    val getExercisesByIdsUseCase by lazy { GetExercisesByIdsUseCase(exerciseRepository) }
    val createCustomWorkoutPlanUseCase by lazy { CreateCustomWorkoutPlanUseCase(workoutRepository) }
    val deleteCustomWorkoutPlanUseCase by lazy { DeleteCustomWorkoutPlanUseCase(workoutRepository) }
    val addExercisesToPlanDayUseCase by lazy { AddExercisesToPlanDayUseCase(workoutRepository) }
    val replacePlanExerciseUseCase by lazy { ReplacePlanExerciseUseCase(workoutRepository) }
    val updatePlanExerciseRepsUseCase by lazy { UpdatePlanExerciseRepsUseCase(workoutRepository) }
    val deletePlanExerciseUseCase by lazy { DeletePlanExerciseUseCase(workoutRepository) }
    val reorderPlanExercisesUseCase by lazy { ReorderPlanExercisesUseCase(workoutRepository) }
    val getChatSessionsUseCase by lazy { GetChatSessionsUseCase(chatRepository) }
    val getChatMessagesUseCase by lazy { GetChatMessagesUseCase(chatRepository) }
    val createChatSessionUseCase by lazy { CreateChatSessionUseCase(chatRepository) }
    val sendChatMessageUseCase by lazy { SendChatMessageUseCase(chatRepository) }
    val deleteChatSessionUseCase by lazy { DeleteChatSessionUseCase(chatRepository) }
    val observeRunningSessionUseCase by lazy { ObserveRunningSessionUseCase(runningRepository) }
    val getRunHistoryUseCase by lazy { GetRunHistoryUseCase(runningRepository) }
    val getRunDetailUseCase by lazy { GetRunDetailUseCase(runningRepository) }
    val deleteRunUseCase by lazy { DeleteRunUseCase(runningRepository) }
    val getTrainingProgramUseCase by lazy { GetTrainingProgramUseCase(structuredTrainingCatalogRepository) }
    val getTrainingProgressUseCase by lazy { GetTrainingProgressUseCase(structuredTrainingProgressRepository) }
    val enrollTrainingProgramUseCase by lazy { EnrollTrainingProgramUseCase(structuredTrainingProgressRepository) }
    val startStructuredSessionUseCase by lazy { StartStructuredSessionUseCase(structuredTrainingProgressRepository) }
    val completeStructuredSessionUseCase by lazy {
        CompleteStructuredSessionUseCase(structuredTrainingProgressRepository, structuredTrainingCatalogRepository)
    }
    val repeatStructuredWeekUseCase by lazy { RepeatStructuredWeekUseCase(structuredTrainingProgressRepository) }

    // Lets every AsyncImage/SubcomposeAsyncImage in the app decode animated exercise GIFs
    // (gif_url) without passing an ImageLoader explicitly at each call site.
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}
