package com.example.homeworkout.domain.usecases.planselection

import com.example.homeworkout.domain.models.FitnessProfile
import com.example.homeworkout.domain.repositories.FitnessProfileRepository
import kotlinx.coroutines.flow.Flow

class GetFitnessProfileUseCase(
    private val fitnessProfileRepository: FitnessProfileRepository
) {
    operator fun invoke(): Flow<FitnessProfile?> = fitnessProfileRepository.observeProfile()
}
