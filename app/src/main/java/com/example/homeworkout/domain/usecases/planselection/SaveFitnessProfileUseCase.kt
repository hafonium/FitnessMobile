package com.example.homeworkout.domain.usecases.planselection

import com.example.homeworkout.domain.models.FitnessProfile
import com.example.homeworkout.domain.repositories.FitnessProfileRepository
import com.example.homeworkout.domain.repositories.PlanCatalogRepository

class SaveFitnessProfileUseCase(
    private val fitnessProfileRepository: FitnessProfileRepository,
    private val planCatalogRepository: PlanCatalogRepository
) {
    suspend operator fun invoke(profile: FitnessProfile, recommendedCatalogId: String?) {
        val version = planCatalogRepository.getCatalog().version
        fitnessProfileRepository.saveProfile(profile, recommendedCatalogId, version)
    }
}
