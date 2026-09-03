package com.example.homeworkout.ui.receivers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.homeworkout.R
import com.example.homeworkout.ui.MainActivity
import com.example.homeworkout.ui.services.ReminderScheduler
import java.util.Calendar

/**
 * Fires the "time to work out" notification for the daily reminder, then immediately re-arms
 * [ReminderScheduler] for +1 day — see [ReminderScheduler] for why it isn't `setRepeating`. See
 * `docs/notifications.md` for the full design and OS/OEM reliability caveats.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive fired — this proves the OS delivered the alarm broadcast.")

        ensureChannel(context)
        showNotification(context)

        val now = Calendar.getInstance()
        val time = "%02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        ReminderScheduler(context).schedule(time)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Workout Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Daily reminder to complete your workout"
                enableVibration(true)
                enableLights(true)
            }
        )
    }

    private fun showNotification(context: Context) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted — alarm fired but notification was skipped.")
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("Time to Workout!")
            .setContentText("Keep your streak going — your workout is waiting.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // sound + vibration + lights
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "notify() called successfully.")
    }

    companion object {
        const val CHANNEL_ID = "workout_reminders"
        private const val NOTIFICATION_ID = 2001
        private const val TAG = "ReminderReceiver"
    }
}
