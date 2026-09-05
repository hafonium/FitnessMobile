package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.UserGender

/** One persisted body-weight measurement. Weight is stored canonically in kilograms. */
data class WeightRecord(
    val weightKg: Double,
    val heightCmSnapshot: Double,
    val loggedAt: Long
)

/** Raw user/measurement data exposed by the repository to the domain layer. */
data class WeightProfile(
    val heightCm: Double?,
    val ageYears: Int?,
    val gender: UserGender?,
    val records: List<WeightRecord>
)

enum class BmiCategory(val label: String) {
    SEVERELY_UNDERWEIGHT("Severely underweight"),
    UNDERWEIGHT("Underweight"),
    HEALTHY("Healthy weight"),
    OVERWEIGHT("Overweight"),
    OBESE_CLASS_I("Obese class I"),
    OBESE_CLASS_II("Obese class II+")
}

/** Derived, display-ready values shared by Report and the pushed Weight screen. */
data class WeightDashboard(
    val currentWeightKg: Double?,
    val currentLoggedAt: Long?,
    val heaviestWeightKg: Double?,
    val lightestWeightKg: Double?,
    val averageWeightKg: Double?,
    val lastSevenDaysChangeKg: Double?,
    val heightCm: Double?,
    val ageYears: Int?,
    val bmi: Double?,
    val bmiCategory: BmiCategory?,
    /** At most the seven newest records, ordered oldest to newest for chart rendering. */
    val chartRecords: List<WeightRecord>
)
