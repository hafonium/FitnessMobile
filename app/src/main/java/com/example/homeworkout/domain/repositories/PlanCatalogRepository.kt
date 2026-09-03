package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.catalog.WorkoutPlanCatalog

interface PlanCatalogRepository {
    suspend fun getCatalog(): WorkoutPlanCatalog
}
