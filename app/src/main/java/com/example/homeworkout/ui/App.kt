package com.example.homeworkout.ui

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.homeworkout.data.catalog.WorkoutPlanCatalogSource
import com.example.homeworkout.data.local.AppDatabase
import com.example.homeworkout.data.remote.groq.GroqClient
import com.example.homeworkout.data.repositories.ChatRepositoryImpl
import com.example.homeworkout.data.repositories.ExerciseRepositoryImpl
import com.example.homeworkout.data.repositories.BadgeRepositoryImpl
import com.example.homeworkout.data.repositories.FitnessProfileRepositoryImpl
import com.example.homeworkout.data.repositories.SettingsRepositoryImpl
import com.example.homeworkout.data.repositories.WorkoutRepositoryImpl
import com.example.homeworkout.data.repositories.WorkoutSessionRepositoryImpl
import com.example.homeworkout.domain.repositories.ChatRepository
import com.example.homeworkout.domain.repositories.ExerciseRepository
import com.example.homeworkout.domain.repositories.BadgeRepository
import com.example.homeworkout.domain.repositories.FitnessProfileRepository
import com.example.homeworkout.domain.repositories.PlanCatalogRepository
import com.example.homeworkout.domain.repositories.SettingsRepository
import com.example.homeworkout.domain.repositories.WorkoutRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
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
import com.example.homeworkout.domain.usecases.home.GetWeeklyGoalProgressUseCase
import com.example.homeworkout.domain.usecases.home.GetWorkoutsUseCase
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
import com.example.homeworkout.domain.usecases.player.ResolveNextPlanDayUseCase
import com.example.homeworkout.domain.usecases.player.RestartWorkoutDayUseCase
import com.example.homeworkout.domain.usecases.player.StartSpecificWorkoutDayUseCase
import com.example.homeworkout.domain.usecases.player.StartWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.report.GetStreakUseCase
import com.example.homeworkout.domain.usecases.settings.GetSettingsUseCase
import com.example.homeworkout.domain.usecases.settings.ResetWorkoutProgressUseCase
import com.example.homeworkout.domain.usecases.settings.UpdateSettingsUseCase
import com.example.homeworkout.ui.core.chat.ChatPanelController
import com.example.homeworkout.ui.services.ReminderScheduler
import com.example.homeworkout.ui.services.TtsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class App : Application(), ImageLoaderFactory {

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

    // Services
    val ttsService: TtsService by lazy { TtsService(this) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }

    // Use Cases
    val getWorkoutsUseCase by lazy { GetWorkoutsUseCase(workoutRepository) }
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
    val getStreakUseCase by lazy { GetStreakUseCase(workoutSessionRepository) }
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
