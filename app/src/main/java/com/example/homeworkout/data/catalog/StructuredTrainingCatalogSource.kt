package com.example.homeworkout.data.catalog

import android.content.Context
import com.example.homeworkout.domain.models.training.StructuredTrainingProgram
import com.example.homeworkout.domain.repositories.StructuredTrainingCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StructuredTrainingCatalogSource(private val context: Context) : StructuredTrainingCatalogRepository {
    private val cache = mutableMapOf<String, StructuredTrainingProgram>()

    override suspend fun getProgram(programId: String): StructuredTrainingProgram? = withContext(Dispatchers.IO) {
        cache[programId] ?: assetFor(programId)?.let { asset ->
            context.assets.open(asset).bufferedReader().use { StructuredTrainingCatalogParser.parse(it.readText()) }
                .also { cache[programId] = it }
        }
    }

    private fun assetFor(programId: String): String? = when (programId) {
        "beginner-running-12w" -> "beginner_running_12w.json"
        "walking-weight-loss-20w" -> "walking_weight_loss_20w.json"
        else -> null
    }
}
