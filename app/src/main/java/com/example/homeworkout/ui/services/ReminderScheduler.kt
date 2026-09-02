package com.example.homeworkout.ui.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.homeworkout.ui.receivers.ReminderReceiver
import java.util.Calendar
import java.util.Date

/**
 * Arms/disarms the exact daily workout-reminder alarm via [AlarmManager]. [ReminderReceiver] fires
 * the notification and reschedules the next day's alarm itself — `AlarmManager` has no built-in
 * exact *and* idle-tolerant repeating mode, so "repeat daily" is implemented as "fire once, then
 * re-arm for +1 day" rather than `setRepeating`. See `docs/notifications.md` for the full design
 * (permissions, OS/OEM reliability caveats, how to test).
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val pendingIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** [time] is `"HH:mm"`. Schedules the next occurrence of that time (today if still ahead, else tomorrow). */
    fun schedule(time: String) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return
        setAlarm(nextTriggerMillis(hour, minute))
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Exact alarms are a special permission on API 31+ (auto-granted below API 33 for most apps,
     * user-grantable via Settings above that, or via the `USE_EXACT_ALARM` manifest permission).
     * When it's not available we fall back to an inexact alarm instead of crashing — the reminder
     * just fires a little late rather than not at all.
     */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun setAlarm(triggerAtMillis: Long) {
        val exact = canScheduleExact()
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        Log.d(TAG, "Armed alarm for ${Date(triggerAtMillis)} (exact=$exact)")
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    companion object {
        private const val REQUEST_CODE = 1001
        private const val TAG = "ReminderScheduler"
    }
}
