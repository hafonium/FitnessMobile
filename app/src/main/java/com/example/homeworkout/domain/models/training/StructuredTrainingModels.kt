package com.example.homeworkout.domain.models.training

enum class TrainingProgramKind { RUNNING, WALKING }
enum class TrainingEnrollmentStatus { NOT_ENROLLED, ACTIVE, COMPLETED }
enum class TrainingSessionStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

data class StructuredTrainingProgram(
    val id: String,
    val kind: TrainingProgramKind,
    val title: String,
    val subtitle: String?,
    val durationWeeks: Int,
    val sessionsPerWeek: String,
    val level: String,
    val goal: String,
    val startingAbility: String?,
    val intensities: List<TrainingIntensityDefinition>,
    val phases: List<TrainingPhase>,
    val weeks: List<StructuredTrainingWeek>,
    val maintenanceTitle: String,
    val maintenanceGuidance: String,
    val maintenanceDays: List<String>,
    val rules: List<TrainingRule>
)

data class TrainingPhase(
    val index: Int,
    val title: String,
    val weekStart: Int,
    val weekEnd: Int,
    val goal: String
)

data class StructuredTrainingWeek(
    val id: String,
    val weekNumber: Int,
    val title: String,
    val durationMinMinutes: Int,
    val durationMaxMinutes: Int,
    val phaseIndex: Int?,
    val sessions: List<StructuredTrainingSession>,
    val goal: String?,
    val coachTip: String?,
    val milestone: String?,
    val note: String?
)

data class StructuredTrainingSession(
    val id: String,
    val sessionIndex: Int,
    val title: String,
    val type: String,
    val durationMinMinutes: Int,
    val durationMaxMinutes: Int,
    val isOptional: Boolean,
    val steps: List<StructuredTrainingStep>
)

data class StructuredTrainingStep(
    val label: String,
    val durationSeconds: Int,
    val intensity: String
)

data class TrainingIntensityDefinition(
    val id: String,
    val name: String,
    val rpe: String,
    val description: String,
    val colorHex: String
)

data class TrainingRule(val title: String, val description: String)

data class StructuredProgramProgress(
    val programId: String,
    val status: TrainingEnrollmentStatus,
    val currentWeekNumber: Int,
    val completedSessions: Set<String>,
    val activeSessionId: String?
)
