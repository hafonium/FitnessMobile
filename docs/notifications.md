# Daily Workout Reminder — Notification Design

Everything about the "Remind me to work out every day" feature on `GeneralSettingsScreen`: how it's
built, what permissions it needs, why it can still fail to fire on some phones even when the code is
correct, and how to test it. Read this before touching `ui/services/ReminderScheduler.kt`,
`ui/receivers/`, or the reminder rows on `GeneralSettingsScreen.kt`.

The in-app UI for this feature is deliberately minimal — just the switch and the reminder-time row.
Earlier iterations added persistent warning banners (missing-permission hints, "grant this"
buttons) and a debug test-notification button directly in Settings; those were removed on request
to keep the screen clean. Everything they explained now lives here instead.

## Architecture

- `ReminderScheduler` (`ui/services/`) — thin wrapper around `AlarmManager`. `schedule("HH:mm")`
  computes the next occurrence of that wall-clock time (today if still ahead, else tomorrow) and
  arms `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis,
  pendingIntent)` — the mode designed specifically to still fire while the device is in Doze/idle.
  Falls back to the inexact `setAndAllowWhileIdle` only when `canScheduleExact()` is false (exact
  alarm permission not granted), rather than crashing.
- `ReminderReceiver` (`ui/receivers/`, registered in `AndroidManifest.xml` with
  `android:exported="false"` — it only ever needs to receive the app's own explicit broadcast, never
  one from another app) — fires on the alarm: ensures the `"workout_reminders"` notification channel
  exists at `IMPORTANCE_HIGH` (with vibration/lights), posts the notification via
  `NotificationManagerCompat` (gated on the `POST_NOTIFICATIONS` runtime permission, silently
  skipped rather than crashing if not granted), then immediately re-arms `ReminderScheduler` for
  tomorrow using the current wall-clock hour/minute. There's no `setRepeating` involved —
  `AlarmManager` has no mode that's both exact and idle-tolerant *and* repeating, so "daily" is
  "fire once, then the receiver re-arms itself for +1 day" every time it fires.
- `BootReceiver` (`ui/receivers/`, `exported="true"` with a `BOOT_COMPLETED` intent-filter — this one
  *must* be exported to receive the system broadcast) — `AlarmManager` alarms are cleared on every
  reboot; this reads the persisted `dailyReminderEnabled`/`dailyReminderTime` off
  `App.settingsRepository` on `App.applicationScope` (via `goAsync()` to survive the suspend read)
  and re-arms the alarm if it was on.
- `SettingsViewModel.setDailyReminder(enabled, time)` persists the preference **and** calls
  `reminderScheduler.schedule()`/`cancel()` to match, in the same call — the two are never allowed to
  drift apart.
- `GeneralSettingsScreen` requests `POST_NOTIFICATIONS` (API 33+, via
  `rememberLauncherForActivityResult(RequestPermission())`) the first time the switch is turned on;
  if denied, `setDailyReminder(true, ...)` is simply never called, so the switch stays off — no
  toast, no persistent warning, it just doesn't turn on.

## Manifest permissions

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

- **`POST_NOTIFICATIONS`** (API 33+) — runtime-requested; without it, `notify()` never shows
  anything, and `ReminderReceiver` detects and logs this rather than crashing.
- **`SCHEDULE_EXACT_ALARM`** — a *special* permission on API 31+: auto-granted below API 33 for most
  apps, but on API 33+ the user typically has to explicitly enable it via system Settings ("Alarms &
  reminders" for this app) unless...
- **`USE_EXACT_ALARM`** (API 33+) — a *normal* permission, auto-granted at install, that also
  unlocks exact alarm scheduling without a user prompt. **Caveat:** Play Store restricts this to
  apps whose core function is alarms/calendaring and will reject a submission that declares it
  without that justification. Fine for local/sideloaded debug builds (this is one); replace with
  just `SCHEDULE_EXACT_ALARM` before any Play Store submission.
- **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** — lets the app *ask* to be exempted from stock Android's
  Doze/App-Standby battery optimization via `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
  Declared but not currently wired to any UI button (removed along with the other hints) — see
  "Requesting it back" below if you need it.
- **`RECEIVE_BOOT_COMPLETED`** — lets `BootReceiver` see `ACTION_BOOT_COMPLETED` and re-arm the alarm
  after a reboot.

`canScheduleExact()` on `ReminderScheduler` checks `AlarmManager.canScheduleExactAlarms()` at
runtime and the scheduler falls back to an inexact alarm rather than crashing if it's false — so the
app never breaks from a missing exact-alarm grant, it just becomes less punctual.

## Why "it works when the app is open, not when it's closed" can still happen with correct code

This was root-caused via live device logcat during development (not guesswork) to **two independent
layers**, both outside what `AlarmManager` usage alone can fix:

1. **Stock Android Doze/App Standby.** Covered by the exact-alarm permission above — without it,
   `setAndAllowWhileIdle` is used instead of `setExactAndAllowWhileIdle`, which the OS may defer
   more aggressively depending on the app's usage-based "standby bucket."
2. **OEM-proprietary background killers**, confirmed present on **BBK-brand phones** (Vivo/Oppo/
   OnePlus/Realme — FuntouchOS/OriginOS/ColorOS) and known to exist in similar form on Xiaomi (MIUI)
   and Samsung. These run **on top of** stock Android and can kill an app's process and revoke its
   ability to run scheduled alarms the moment it's swiped from Recents — entirely independent of
   `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`/`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` being granted and
   used correctly, because the killer isn't part of AOSP and isn't controlled by any of those
   AOSP-level permissions. There is **no code-level fix** for this — see dontkillmyapp.com for the
   general phenomenon across OEMs. It requires a manual, per-OEM opt-out in system Settings:

   **Vivo (FuntouchOS/OriginOS)** — menu names vary slightly by version:
   1. **Settings → Battery → Background power consumption management** (or **"High background
      power consumption"**) → Finess Mobile → allow/unrestricted.
   2. **Settings → Apps → Autostart management** (or **i Manager → App manager → Autostart**) →
      enable for Finess Mobile.
   3. **Settings → Battery → App power consumption management** → Finess Mobile →
      **"Allow background activity"**.
   4. Force-stop and reopen the app once afterward so the new policy applies to a fresh process.

   **Oppo/OnePlus/Realme (ColorOS)** — **Settings → Battery → App Battery Management** → find the
   app → disable "Sleep-standby optimization" / set to allow background activity, plus enable
   autostart the same way as Vivo above.

   **Xiaomi (MIUI)** — **Settings → Apps → Manage apps → Finess Mobile → Battery saver → No
   restrictions**, plus **Autostart** enabled for the app.

   **Samsung (One UI)** — **Settings → Apps → Finess Mobile → Battery → Unrestricted**, and make sure
   the app isn't in **Settings → Battery → Background usage limits → Sleeping/Deep sleeping apps**.

   None of this can be triggered or verified from application code — only
   `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (the stock-Android layer) has a programmatic request path,
   and even that only fixes layer 1, not layer 2.

## How to test manually (no in-app debug button)

There's no test-notification button in the UI anymore. To verify the pipeline:

1. Set a real reminder time ~1-2 minutes in the future via General Settings.
2. Swipe the app away from Recents (a real swipe-to-close, not just pressing Home).
3. Wait for the scheduled time and watch the notification shade.
4. If it doesn't show, capture logs to localize the failure:
   ```
   adb logcat -s ReminderScheduler:D ReminderReceiver:D
   ```
   Let this run continuously (don't stop it early) spanning the scheduled time, then check:
   - `"Armed alarm for <time> (exact=...)"` should print immediately when you set the time.
   - `"onReceive fired"` should print at the scheduled time — if this line is **missing**, the OS
     never delivered the broadcast (the #1 thing to check: exact-alarm permission, then the OEM
     background-kill settings above).
   - A `"POST_NOTIFICATIONS not granted"` warning instead of `"notify() called successfully"` means
     the alarm fired correctly but the notification permission is the remaining gap.
5. To check the boot-reschedule path: arm a reminder, `adb reboot`, then once the device is back up,
   `adb shell dumpsys alarm | grep -A3 homeworkout` should show a pending alarm without ever
   reopening the app (`BootReceiver` did it).
6. **Notification channel importance is fixed once created.** If you're testing on a
   device/emulator that ran an older build of this app (before the channel was `IMPORTANCE_HIGH`),
   the channel is stuck at whatever importance it was first created at — reinstalling the APK does
   **not** reset it. Uninstall the app first, then reinstall, to get a fresh `IMPORTANCE_HIGH`
   channel.

## Requesting a debug test button / hints back

If a future task needs to re-add a quick way to fire a test notification or surface these
permission gaps in the UI again, the pattern used previously (and removed on request) was:

- `ReminderScheduler.scheduleTest()` — arm a second `PendingIntent` (a different request code from
  the real daily one) for ~10 seconds out, carrying an extra flag so `ReminderReceiver` skips its
  reschedule-for-tomorrow step for that one firing.
- Three reactive `mutableStateOf` checks in `GeneralSettingsScreen` (notification permission via
  `ContextCompat.checkSelfPermission`, exact-alarm via `ReminderScheduler.canScheduleExact()`,
  battery optimization via `PowerManager.isIgnoringBatteryOptimizations()`), re-evaluated on every
  `ON_RESUME` via a `LifecycleEventObserver`, each with a `Text` hint + `TextButton` deep-linking to
  the relevant system Settings screen (`ACTION_APP_NOTIFICATION_SETTINGS`,
  `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

Re-derive from this doc rather than assuming that removed code still exists elsewhere — it doesn't;
it was deleted from `SettingsViewModel`, `ReminderScheduler`, `ReminderReceiver`, and
`GeneralSettingsScreen`, not just hidden.

## Known limitations

- No code-level fix exists for OEM background killers (see above) — this is a system Settings
  problem on the user's device, not an app bug, once the code-controllable layers (permissions,
  `AlarmManager` usage) are all correct.
- `PRIVACY_POLICY_URL` and other unrelated General Settings items are documented in
  `docs/settings-feature.md`, not here.
