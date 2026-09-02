package com.example.homeworkout.ui

import android.app.Application
import com.example.homeworkout.data.local.AppDatabase
import com.example.homeworkout.data.repositories.ExerciseRepositoryImpl
import com.example.homeworkout.data.repositories.SettingsRepositoryImpl
import com.example.homeworkout.data.repositories.WorkoutRepositoryImpl
import com.example.homeworkout.data.repositories.WorkoutSessionRepositoryImpl
import com.example.homeworkout.domain.repositories.ExerciseRepository
import com.example.homeworkout.domain.repositories.SettingsRepository
import com.example.homeworkout.domain.repositories.WorkoutRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import com.example.homeworkout.domain.usecases.details.GetWorkoutDetailsUseCase
import com.example.homeworkout.domain.usecases.exerciseinfo.GetExerciseDetailUseCase
import com.example.homeworkout.domain.usecases.exercises.SearchExercisesUseCase
import com.example.homeworkout.domain.usecases.home.GetWeeklyGoalProgressUseCase
import com.example.homeworkout.domain.usecases.home.GetWorkoutsUseCase
import com.example.homeworkout.domain.usecases.settings.GetSettingsUseCase
import com.example.homeworkout.domain.usecases.settings.ResetWorkoutProgressUseCase
import com.example.homeworkout.domain.usecases.settings.UpdateSettingsUseCase
import com.example.homeworkout.ui.services.TtsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class App : Application() {

    // Lives for the whole process — used once to seed the database on first launch.
    val applicationScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // Room database
    private val database: AppDatabase by lazy { AppDatabase.getInstance(this, applicationScope) }

    // Repositories
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepositoryImpl(database.workoutPlanDao()) }
    val exerciseRepository: ExerciseRepository by lazy { ExerciseRepositoryImpl(database.exerciseDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(database.userDao(), database.workoutSessionDao()) }
    val workoutSessionRepository: WorkoutSessionRepository by lazy { WorkoutSessionRepositoryImpl(database.userDao(), database.workoutSessionDao()) }

    // Services
    val ttsService: TtsService by lazy { TtsService(this) }

    // Use Cases
    val getWorkoutsUseCase by lazy { GetWorkoutsUseCase(workoutRepository) }
    val getWorkoutDetailsUseCase by lazy { GetWorkoutDetailsUseCase(workoutRepository) }
    val getExerciseDetailUseCase by lazy { GetExerciseDetailUseCase(exerciseRepository) }
    val searchExercisesUseCase by lazy { SearchExercisesUseCase(exerciseRepository) }
    val getSettingsUseCase by lazy { GetSettingsUseCase(settingsRepository) }
    val updateSettingsUseCase by lazy { UpdateSettingsUseCase(settingsRepository) }
    val resetWorkoutProgressUseCase by lazy { ResetWorkoutProgressUseCase(settingsRepository) }
    val getWeeklyGoalProgressUseCase by lazy { GetWeeklyGoalProgressUseCase(settingsRepository, workoutSessionRepository) }
}
