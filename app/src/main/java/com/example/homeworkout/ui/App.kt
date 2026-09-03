package com.example.homeworkout.ui

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.homeworkout.data.catalog.WorkoutPlanCatalogSource
import com.example.homeworkout.data.local.AppDatabase
import com.example.homeworkout.data.repositories.ExerciseRepositoryImpl
import com.example.homeworkout.data.repositories.FitnessProfileRepositoryImpl
import com.example.homeworkout.data.repositories.SettingsRepositoryImpl
import com.example.homeworkout.data.repositories.WorkoutRepositoryImpl
import com.example.homeworkout.data.repositories.WorkoutSessionRepositoryImpl
import com.example.homeworkout.domain.repositories.ExerciseRepository
import com.example.homeworkout.domain.repositories.FitnessProfileRepository
import com.example.homeworkout.domain.repositories.PlanCatalogRepository
import com.example.homeworkout.domain.repositories.SettingsRepository
import com.example.homeworkout.domain.repositories.WorkoutRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import com.example.homeworkout.domain.usecases.details.GetWorkoutDetailsUseCase
import com.example.homeworkout.domain.usecases.exerciseinfo.GetExerciseDetailUseCase
import com.example.homeworkout.domain.usecases.exercises.SearchExercisesUseCase
import com.example.homeworkout.domain.usecases.home.GetWeeklyGoalProgressUseCase
import com.example.homeworkout.domain.usecases.home.GetWorkoutsUseCase
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

    val workoutPlanDao by lazy { database.workoutPlanDao() }

    // Repositories
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepositoryImpl(database.workoutPlanDao()) }
    val exerciseRepository: ExerciseRepository by lazy { ExerciseRepositoryImpl(database.exerciseDao()) }
    val planCatalogRepository: PlanCatalogRepository by lazy { WorkoutPlanCatalogSource(this) }
    val fitnessProfileRepository: FitnessProfileRepository by lazy { FitnessProfileRepositoryImpl(database.userDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(database.userDao(), database.workoutSessionDao()) }
    val workoutSessionRepository: WorkoutSessionRepository by lazy { WorkoutSessionRepositoryImpl(database.userDao(), database.workoutSessionDao()) }

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
    val resolveNextPlanDayUseCase by lazy { ResolveNextPlanDayUseCase(workoutRepository, workoutSessionRepository) }
    val startWorkoutSessionUseCase by lazy { StartWorkoutSessionUseCase(resolveNextPlanDayUseCase, workoutSessionRepository) }
    val startSpecificWorkoutDayUseCase by lazy { StartSpecificWorkoutDayUseCase(workoutRepository, workoutSessionRepository) }
    val restartWorkoutDayUseCase by lazy { RestartWorkoutDayUseCase(workoutSessionRepository) }
    val completeWorkoutSessionUseCase by lazy { CompleteWorkoutSessionUseCase(workoutSessionRepository) }
    val abandonWorkoutSessionUseCase by lazy { AbandonWorkoutSessionUseCase(workoutSessionRepository) }

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
