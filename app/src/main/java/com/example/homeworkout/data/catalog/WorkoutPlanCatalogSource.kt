package com.example.homeworkout.data.catalog

import android.content.Context
import com.example.homeworkout.domain.models.catalog.WorkoutPlanCatalog
import com.example.homeworkout.domain.repositories.PlanCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads and caches the workout-plan catalog from assets. */
class WorkoutPlanCatalogSource(
    private val context: Context
) : PlanCatalogRepository {

    @Volatile
    private var cached: WorkoutPlanCatalog? = null

    override suspend fun getCatalog(): WorkoutPlanCatalog {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            WorkoutPlanCatalogParser.parse(json).also { cached = it }
        }
    }

    companion object {
        const val ASSET_NAME = "workout_plan_catalog.json"
    }
}
