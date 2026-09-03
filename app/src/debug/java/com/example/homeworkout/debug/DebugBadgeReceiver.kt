package com.example.homeworkout.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.homeworkout.data.local.AppDatabase
import com.example.homeworkout.data.local.entities.UserSettingsEntity
import com.example.homeworkout.data.local.entities.WorkoutSessionEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.enums.WorkoutPhase
import com.example.homeworkout.domain.models.enums.WorkoutSessionStatus
import com.example.homeworkout.ui.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.max

/** Debug builds only: creates real qualifying workout data for the first seven badges. */
class DebugBadgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UNLOCK_BADGES) return

        val pendingResult = goAsync()
        val app = context.applicationContext as App
        app.applicationScope.launch {
            try {
                val database = AppDatabase.getInstance(app, app.applicationScope)
                val userId = database.userDao()
                    .getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)
                    ?.userId
                    ?: return@launch

                val sessionDao = database.workoutSessionDao()
                val planDao = database.workoutPlanDao()
                val completedSessions = sessionDao.observeSessionsForUser(userId).first()
                    .filter { it.status == WorkoutSessionStatus.COMPLETED }
                val completedDayIds = completedSessions.mapTo(hashSetOf()) { it.planDayId }

                val planRows = planDao.observePlanSummaries().first().filter { it.totalDays > 1 }
                val previewPlan = planRows.firstOrNull { row ->
                    planDao.getPlanDays(row.plan.planId).none { it.planDayId in completedDayIds }
                } ?: planRows.maxByOrNull { it.totalDays } ?: return@launch
                val previewDay = planDao.getPlanDays(previewPlan.plan.planId).firstOrNull()
                    ?: return@launch
                val settings = database.userDao().getUserSettings(userId)
                    ?: UserSettingsEntity(userId = userId)

                val existingDayStarts = completedSessions.mapTo(hashSetOf()) { startOfDay(it.endedAt ?: it.startedAt) }
                val requiredDays = (0 until REQUIRED_STREAK_DAYS).map { daysAgo ->
                    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }.timeInMillis
                }
                val missingStreakDays = requiredDays.filter { startOfDay(it) !in existingDayStarts }
                val sessionsNeededForCount = (REQUIRED_WORKOUT_COUNT - completedSessions.size).coerceAtLeast(0)
                val sessionsToCreate = max(sessionsNeededForCount, missingStreakDays.size)

                repeat(sessionsToCreate) { index ->
                    val endedAt = missingStreakDays.getOrNull(index)
                        ?: requiredDays[index % requiredDays.size]
                    sessionDao.insertSession(
                        WorkoutSessionEntity(
                            userId = userId,
                            planId = previewPlan.plan.planId,
                            planDayId = previewDay.planDayId,
                            status = WorkoutSessionStatus.COMPLETED,
                            currentPhase = WorkoutPhase.COMPLETED,
                            startedAt = endedAt - PREVIEW_DURATION_SECONDS * 1_000L,
                            endedAt = endedAt,
                            durationSeconds = PREVIEW_DURATION_SECONDS,
                            restTimerSec = settings.restTimerSec,
                            prepTimerSec = settings.prepTimerSec,
                            musicEnabled = settings.musicEnabled,
                            soundEnabled = settings.soundEnabled,
                            coachVideoEnabled = settings.coachVideoEnabled,
                            ttsVoiceType = settings.ttsVoiceType
                        )
                    )
                }

                database.badgeDao().deleteAllBadgesForUser(userId)
                val unlocked = app.evaluateBadgesUseCase()
                app.markBadgesSeenUseCase(unlocked.map { it.definition.id })
                val badgeState = app.getBadgesUseCase().first()
                Log.d(
                    TAG,
                    badgeState.joinToString(prefix = "Preview badge state: ") {
                        "${it.definition.id}=${it.currentValue}/${it.definition.targetValue}:${it.isUnlocked}"
                    }
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        app,
                        "${unlocked.size} badges unlocked with qualifying data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_UNLOCK_BADGES = "com.example.homeworkout.DEBUG_UNLOCK_BADGES"
        private const val REQUIRED_WORKOUT_COUNT = 100
        private const val REQUIRED_STREAK_DAYS = 7
        private const val PREVIEW_DURATION_SECONDS = 60
        private const val TAG = "DebugBadgeReceiver"
    }
}

private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().run {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    timeInMillis
}
