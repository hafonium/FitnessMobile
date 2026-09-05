package com.example.homeworkout.domain.usecases.report

import com.example.homeworkout.domain.models.ForecastPoint
import com.example.homeworkout.domain.models.WeightForecast
import com.example.homeworkout.domain.models.enums.UserGender
import com.example.homeworkout.domain.repositories.FoodLogRepository
import com.example.homeworkout.domain.repositories.RunningRepository
import com.example.homeworkout.domain.repositories.WeightRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * TDEE-based weight projection (Mifflin-St Jeor formula). See docs/weight-forecast-feature.md for
 * the full derivation and its limitations — this is a transparent physics-based estimate, not a
 * trained model, and it degrades gracefully (returns [WeightForecast.hasEnoughData] = false with a
 * [WeightForecast.missingReason]) when the inputs it needs aren't there yet.
 */
class GetWeightForecastUseCase(
    private val weightRepository: WeightRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val runningRepository: RunningRepository,
    private val foodLogRepository: FoodLogRepository
) {
    operator fun invoke(): Flow<WeightForecast> = combine(
        weightRepository.observeWeightProfile(),
        workoutSessionRepository.observeCompletedSessions(),
        runningRepository.observeFinishedSessions(),
        foodLogRepository.observeFoodLogs()
    ) { profile, completedWorkouts, finishedRuns, foodLogs ->
        val currentWeightKg = profile.records.maxByOrNull { it.loggedAt }?.weightKg
        val heightCm = profile.heightCm
        val ageYears = profile.ageYears

        if (currentWeightKg == null || heightCm == null || ageYears == null) {
            return@combine emptyForecast(
                currentWeightKg,
                "Add your age, height and a weight entry first."
            )
        }

        // Window is 14 whole calendar days (today back through 13 days ago), not a raw 14*24h
        // slide from the current instant — the latter would clip a sliver off today and tack an
        // uneven sliver onto a 15th day depending on what time it happens to be right now.
        val todayStart = startOfDay(System.currentTimeMillis())
        val windowStart = todayStart - (WINDOW_DAYS - 1) * DAY_MILLIS
        val recentFoodLogs = foodLogs.filter { it.loggedAt >= windowStart }
        if (recentFoodLogs.isEmpty()) {
            return@combine emptyForecast(
                currentWeightKg,
                "Log a few meals with the Food Calorie Scanner to estimate your intake."
            )
        }

        val usedNeutral = profile.gender != UserGender.MALE && profile.gender != UserGender.FEMALE
        val genderConstant = when (profile.gender) {
            UserGender.MALE -> 5.0
            UserGender.FEMALE -> -161.0
            else -> -78.0 // average of the male/female offsets; disclosed via usedNeutralGenderConstant
        }
        val bmr = 10 * currentWeightKg + 6.25 * heightCm - 5 * ageYears + genderConstant

        // Intake is only known for days the scanner was actually used, so exercise must be
        // averaged over that exact same set of calendar days too — averaging exercise over the
        // full 14-day window while intake only covers a handful of logged days would compare two
        // different denominators and silently bias the balance either way.
        val loggedDayStarts = recentFoodLogs.map { startOfDay(it.loggedAt) }.distinct()

        fun caloriesBurnedOnDay(dayStart: Long): Double {
            val dayEnd = dayStart + DAY_MILLIS
            val workout = completedWorkouts
                .filter { it.endedAt in dayStart until dayEnd }
                .sumOf { it.caloriesBurned ?: 0.0 }
            val run = finishedRuns
                .filter { it.startedAt in dayStart until dayEnd }
                .sumOf { it.calories ?: 0.0 }
            return workout + run
        }

        fun caloriesEatenOnDay(dayStart: Long): Double {
            val dayEnd = dayStart + DAY_MILLIS
            return recentFoodLogs.filter { it.loggedAt in dayStart until dayEnd }.sumOf { it.caloriesKcal }
        }

        val avgDailyExerciseKcal = loggedDayStarts.map(::caloriesBurnedOnDay).average()
        val avgDailyIntakeKcal = loggedDayStarts.map(::caloriesEatenOnDay).average()
        val tdee = bmr * SEDENTARY_ACTIVITY_FACTOR + avgDailyExerciseKcal
        val netBalance = avgDailyIntakeKcal - tdee
        val now = System.currentTimeMillis()
        val projection = (0..MAX_PROJECTION_DAYS step PROJECTION_STEP_DAYS).map { dayOffset ->
            ForecastPoint(
                dayOffset = dayOffset,
                projectedAt = now + dayOffset * DAY_MILLIS,
                weightKg = currentWeightKg + netBalance * dayOffset / KCAL_PER_KG
            )
        }

        WeightForecast(
            hasEnoughData = true,
            missingReason = null,
            currentWeightKg = currentWeightKg,
            bmrKcal = bmr,
            tdeeKcal = tdee,
            avgDailyIntakeKcal = avgDailyIntakeKcal,
            netDailyBalanceKcal = netBalance,
            usedNeutralGenderConstant = usedNeutral,
            projection = projection
        )
    }

    private fun emptyForecast(currentWeightKg: Double?, reason: String) = WeightForecast(
        hasEnoughData = false,
        missingReason = reason,
        currentWeightKg = currentWeightKg,
        bmrKcal = null,
        tdeeKcal = null,
        avgDailyIntakeKcal = null,
        netDailyBalanceKcal = null,
        usedNeutralGenderConstant = false,
        projection = emptyList()
    )

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val WINDOW_DAYS = 14
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
        private const val SEDENTARY_ACTIVITY_FACTOR = 1.2
        private const val KCAL_PER_KG = 7700.0
        private const val MAX_PROJECTION_DAYS = 90
        private const val PROJECTION_STEP_DAYS = 7
    }
}
