package com.example.homeworkout.domain.models

enum class BadgeMetric {
    COMPLETED_SESSIONS,
    BEST_STREAK_DAYS,
    TOTAL_DURATION_SECONDS,
    COMPLETED_PLANS
}

enum class BadgeIcon {
    DUMBBELL,
    TRENDING_UP,
    STARS,
    MEDAL,
    PREMIUM,
    FIRE,
    BOLT,
    CALENDAR,
    CLOCK,
    TROPHY
}

enum class BadgeTier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM
}

data class BadgeDefinition(
    val id: String,
    val title: String,
    val description: String,
    val metric: BadgeMetric,
    val targetValue: Long,
    val icon: BadgeIcon,
    val tier: BadgeTier
)

data class UnlockedBadge(
    val badgeId: String,
    val unlockedAt: Long,
    val triggerSessionId: Long?,
    val isSeen: Boolean
)

data class BadgeProgress(
    val definition: BadgeDefinition,
    val currentValue: Long,
    val unlockedAt: Long? = null,
    val isSeen: Boolean = true
) {
    val isUnlocked: Boolean get() = unlockedAt != null
    val progressFraction: Float
        get() = (currentValue.toFloat() / definition.targetValue.toFloat()).coerceIn(0f, 1f)
}

data class AchievementTotals(
    val completedSessions: Long,
    val totalDurationSeconds: Long,
    val completedPlans: Long
)

object BadgeCatalog {
    val all: List<BadgeDefinition> = listOf(
        BadgeDefinition(
            id = "first_step",
            title = "First Step",
            description = "Complete your first workout.",
            metric = BadgeMetric.COMPLETED_SESSIONS,
            targetValue = 1,
            icon = BadgeIcon.DUMBBELL,
            tier = BadgeTier.BRONZE
        ),
        BadgeDefinition(
            id = "getting_started",
            title = "Getting Started",
            description = "Complete 5 workouts.",
            metric = BadgeMetric.COMPLETED_SESSIONS,
            targetValue = 5,
            icon = BadgeIcon.TRENDING_UP,
            tier = BadgeTier.BRONZE
        ),
        BadgeDefinition(
            id = "ten_strong",
            title = "Ten Strong",
            description = "Complete 10 workouts.",
            metric = BadgeMetric.COMPLETED_SESSIONS,
            targetValue = 10,
            icon = BadgeIcon.STARS,
            tier = BadgeTier.SILVER
        ),
        BadgeDefinition(
            id = "dedicated",
            title = "Dedicated",
            description = "Complete 25 workouts.",
            metric = BadgeMetric.COMPLETED_SESSIONS,
            targetValue = 25,
            icon = BadgeIcon.MEDAL,
            tier = BadgeTier.GOLD
        ),
        BadgeDefinition(
            id = "workout_veteran",
            title = "Workout Veteran",
            description = "Complete 100 workouts.",
            metric = BadgeMetric.COMPLETED_SESSIONS,
            targetValue = 100,
            icon = BadgeIcon.PREMIUM,
            tier = BadgeTier.PLATINUM
        ),
        BadgeDefinition(
            id = "on_fire",
            title = "On Fire",
            description = "Reach a 3-day workout streak.",
            metric = BadgeMetric.BEST_STREAK_DAYS,
            targetValue = 3,
            icon = BadgeIcon.FIRE,
            tier = BadgeTier.BRONZE
        ),
        BadgeDefinition(
            id = "unstoppable",
            title = "Unstoppable",
            description = "Reach a 7-day workout streak.",
            metric = BadgeMetric.BEST_STREAK_DAYS,
            targetValue = 7,
            icon = BadgeIcon.BOLT,
            tier = BadgeTier.SILVER
        ),
        BadgeDefinition(
            id = "monthly_warrior",
            title = "Monthly Warrior",
            description = "Reach a 30-day workout streak.",
            metric = BadgeMetric.BEST_STREAK_DAYS,
            targetValue = 30,
            icon = BadgeIcon.CALENDAR,
            tier = BadgeTier.GOLD
        ),
        BadgeDefinition(
            id = "ten_hour_club",
            title = "Ten Hour Club",
            description = "Accumulate 10 hours of completed workouts.",
            metric = BadgeMetric.TOTAL_DURATION_SECONDS,
            targetValue = 10 * 60 * 60,
            icon = BadgeIcon.CLOCK,
            tier = BadgeTier.GOLD
        ),
        BadgeDefinition(
            id = "plan_finisher",
            title = "Plan Finisher",
            description = "Complete every day in a workout plan.",
            metric = BadgeMetric.COMPLETED_PLANS,
            targetValue = 1,
            icon = BadgeIcon.TROPHY,
            tier = BadgeTier.GOLD
        )
    )
}
