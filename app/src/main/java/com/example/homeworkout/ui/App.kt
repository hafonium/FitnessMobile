package com.example.homeworkout.ui

import android.app.Application
import com.example.homeworkout.data.local.AppDatabase
import com.example.homeworkout.data.repositories.WorkoutRepositoryImpl
import com.example.homeworkout.domain.repositories.WorkoutRepository
import com.example.homeworkout.domain.usecases.details.GetWorkoutDetailsUseCase
import com.example.homeworkout.domain.usecases.home.GetWorkoutsUseCase

class App : Application() {

    // Room database
    private val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    // Repositories
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepositoryImpl(database.workoutDao()) }

    // Use Cases
    val getWorkoutsUseCase by lazy { GetWorkoutsUseCase(workoutRepository) }
    val getWorkoutDetailsUseCase by lazy { GetWorkoutDetailsUseCase(workoutRepository) }
}
