package com.example.homeworkout.ui.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.homeworkout.ui.App
import com.example.homeworkout.ui.services.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-arms the daily reminder alarm after a device reboot — `AlarmManager` alarms are cleared on
 * reboot, so without this the reminder would silently stop firing until the user reopens Settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as App
        app.applicationScope.launch {
            try {
                val settings = app.settingsRepository.observeSettings().first()
                val time = settings.dailyReminderTime
                if (settings.dailyReminderEnabled && time != null) {
                    ReminderScheduler(context).schedule(time)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
