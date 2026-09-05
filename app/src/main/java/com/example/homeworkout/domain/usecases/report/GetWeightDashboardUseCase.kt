package com.example.homeworkout.domain.usecases.report

import com.example.homeworkout.domain.models.BmiCategory
import com.example.homeworkout.domain.models.WeightDashboard
import com.example.homeworkout.domain.models.WeightRecord
import com.example.homeworkout.domain.repositories.WeightRepository
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetWeightDashboardUseCase(
    private val weightRepository: WeightRepository
) {
    operator fun invoke(): Flow<WeightDashboard> = weightRepository.observeWeightProfile().map { profile ->
        // A user can log more than once on the same calendar day (re-weighing, correcting a typo,
        // just testing); only the latest entry for a given day should count everywhere below —
        // otherwise same-day duplicates show up as extra, artificially bunched-up chart points.
        val records = profile.records.sortedBy { it.loggedAt }.collapseToLatestPerDay()
        val current = records.lastOrNull()
        val effectiveHeightCm = profile.heightCm ?: current?.heightCmSnapshot
        val bmi = if (current != null && effectiveHeightCm != null && effectiveHeightCm > 0.0) {
            current.weightKg / ((effectiveHeightCm / 100.0) * (effectiveHeightCm / 100.0))
        } else {
            null
        }
        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val lastSevenDays = records.filter { it.loggedAt >= sevenDaysAgo }

        WeightDashboard(
            currentWeightKg = current?.weightKg,
            currentLoggedAt = current?.loggedAt,
            heaviestWeightKg = records.maxOfOrNull { it.weightKg },
            lightestWeightKg = records.minOfOrNull { it.weightKg },
            averageWeightKg = lastSevenDays.takeIf { it.isNotEmpty() }?.map { it.weightKg }?.average(),
            lastSevenDaysChangeKg = if (lastSevenDays.size >= 2) {
                lastSevenDays.last().weightKg - lastSevenDays.first().weightKg
            } else {
                null
            },
            heightCm = effectiveHeightCm,
            ageYears = profile.ageYears,
            bmi = bmi,
            bmiCategory = bmi?.let(::bmiCategory),
            chartRecords = records.takeLast(7)
        )
    }

    /** [this] must already be sorted ascending by [WeightRecord.loggedAt]. */
    private fun List<WeightRecord>.collapseToLatestPerDay(): List<WeightRecord> =
        groupBy { startOfDay(it.loggedAt) }.values.map { it.last() }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun bmiCategory(bmi: Double): BmiCategory = when {
        bmi < 16.0 -> BmiCategory.SEVERELY_UNDERWEIGHT
        bmi < 18.5 -> BmiCategory.UNDERWEIGHT
        bmi < 25.0 -> BmiCategory.HEALTHY
        bmi < 30.0 -> BmiCategory.OVERWEIGHT
        bmi < 35.0 -> BmiCategory.OBESE_CLASS_I
        else -> BmiCategory.OBESE_CLASS_II
    }
}
