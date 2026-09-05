package com.example.homeworkout.running.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.homeworkout.R
import com.example.homeworkout.domain.models.running.RunPoint
import com.example.homeworkout.domain.models.running.RunActivityType
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.models.running.RunSessionMetadata
import com.example.homeworkout.domain.models.running.RunStatus
import com.example.homeworkout.domain.running.EncodedPolylineCodec
import com.example.homeworkout.domain.running.LocationFilter
import com.example.homeworkout.domain.running.RunStateMachine
import com.example.homeworkout.domain.running.RunningTelemetryCalculator
import com.example.homeworkout.running.location.RunningLocationProvider
import com.example.homeworkout.ui.App
import com.example.homeworkout.ui.MainActivity
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RunningTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val filter = LocationFilter()
    private val telemetryCalculator = RunningTelemetryCalculator()
    private val stateMachine = RunStateMachine()
    private val mutex = Mutex()
    private lateinit var locationProvider: RunningLocationProvider
    private val repository get() = (application as App).runningRepository
    private var session: RunSession? = null
    private var previousPoint: RunPoint? = null
    private var ticker: Job? = null

    override fun onCreate() {
        super.onCreate()
        locationProvider = RunningLocationProvider(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification(session))
        when (intent?.action ?: ACTION_RECOVER) {
            ACTION_START -> scope.launch { startOrRecover(intent?.toMetadata()) }
            ACTION_RECOVER -> scope.launch { startOrRecover(null) }
            ACTION_PAUSE -> scope.launch { pause() }
            ACTION_RESUME -> scope.launch { resume() }
            ACTION_FINISH -> scope.launch { finish() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun startOrRecover(metadata: RunSessionMetadata?) = mutex.withLock {
        val recovered = repository.getRecoverableSession()
        val current = recovered ?: repository.createSession(
            System.currentTimeMillis(),
            SystemClock.elapsedRealtime(),
            metadata ?: RunSessionMetadata()
        )
        session = current
        if (current.status == RunStatus.PAUSED) {
            updateNotification()
            return
        }
        if (!hasPrecisePermission()) {
            fail(current, "Precise location permission is required")
            return
        }
        if (!locationProvider.isGpsEnabled()) {
            repository.updateState(
                current.id, current.status, current.activeDurationMillis,
                current.runningStartedElapsedRealtimeMillis, current.currentSegmentIndex,
                errorMessage = "Location is off. Waiting for GPS…"
            )
            session = current.copy(errorMessage = "Location is off. Waiting for GPS…")
        }
        previousPoint = current.points.lastOrNull { it.segmentIndex == current.currentSegmentIndex }
        requestLocations()
        startTicker()
    }

    private fun requestLocations() {
        locationProvider.start(
            onLocation = { location -> scope.launch { processLocation(location) } },
            onProviderDisabled = { scope.launch { updateGpsAvailability("Location is off. Waiting for GPS…") } },
            onProviderEnabled = { scope.launch { updateGpsAvailability(null) } }
        )
    }

    private suspend fun updateGpsAvailability(message: String?) = mutex.withLock {
        val current = session ?: return
        if (current.status != RunStatus.RUNNING) return
        repository.updateState(
            current.id,
            current.status,
            current.activeDurationMillis,
            current.runningStartedElapsedRealtimeMillis,
            current.currentSegmentIndex,
            errorMessage = message
        )
        session = current.copy(errorMessage = message)
        updateNotification()
    }

    private suspend fun processLocation(location: android.location.Location) = mutex.withLock {
        val current = session ?: return
        if (current.status != RunStatus.RUNNING) return
        val candidate = RunPoint(
            sessionId = current.id,
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
            accuracyMeters = location.accuracy,
            speedMps = location.speed.takeIf { location.hasSpeed() },
            elapsedRealtimeNanos = location.elapsedRealtimeNanos,
            sequence = current.points.size,
            segmentIndex = current.currentSegmentIndex
        )
        val result = filter.evaluate(previousPoint, candidate)
        if (!result.accepted) return
        if (current.errorMessage != null) {
            repository.updateState(
                current.id,
                current.status,
                current.activeDurationMillis,
                current.runningStartedElapsedRealtimeMillis,
                current.currentSegmentIndex,
                errorMessage = null
            )
        }
        val nowElapsed = SystemClock.elapsedRealtime()
        val duration = current.activeDurationAt(nowElapsed)
        val segmentDistance = if (previousPoint == null) 0.0 else androidDistanceMeters(previousPoint!!, candidate)
        val distance = current.distanceMeters + segmentDistance
        val calories = telemetryCalculator.calculate(distance, duration, current.weightKg).calories
        repository.appendPoint(candidate, distance, duration, nowElapsed, calories)
        previousPoint = candidate
        session = current.copy(
            activeDurationMillis = duration,
            runningStartedElapsedRealtimeMillis = nowElapsed,
            distanceMeters = distance,
            calories = calories,
            errorMessage = null,
            points = current.points + candidate
        )
        updateNotification()
    }

    private suspend fun pause() = mutex.withLock {
        val current = session ?: repository.getRecoverableSession() ?: return
        if (!stateMachine.transition(current.status, RunStatus.PAUSED)) return
        val duration = current.activeDurationAt(SystemClock.elapsedRealtime())
        repository.updateState(current.id, RunStatus.PAUSED, duration, null, current.currentSegmentIndex)
        session = current.copy(status = RunStatus.PAUSED, activeDurationMillis = duration, runningStartedElapsedRealtimeMillis = null)
        previousPoint = null
        locationProvider.stop()
        ticker?.cancel()
        updateNotification()
    }

    private suspend fun resume() = mutex.withLock {
        val current = session ?: repository.getRecoverableSession() ?: return
        if (!stateMachine.transition(current.status, RunStatus.RUNNING)) return
        if (!hasPrecisePermission()) {
            fail(current, "Precise location permission is required")
            return
        }
        val now = SystemClock.elapsedRealtime()
        val segment = current.currentSegmentIndex + 1
        val gpsMessage = if (locationProvider.isGpsEnabled()) null else "Location is off. Waiting for GPS…"
        repository.updateState(
            current.id, RunStatus.RUNNING, current.activeDurationMillis, now, segment,
            errorMessage = gpsMessage
        )
        session = current.copy(
            status = RunStatus.RUNNING,
            runningStartedElapsedRealtimeMillis = now,
            currentSegmentIndex = segment,
            errorMessage = gpsMessage
        )
        previousPoint = null
        requestLocations()
        startTicker()
        updateNotification()
    }

    private suspend fun finish() = mutex.withLock {
        val current = session ?: repository.getRecoverableSession() ?: return
        if (!stateMachine.transition(current.status, RunStatus.FINISHED)) return
        val duration = current.activeDurationAt(SystemClock.elapsedRealtime())
        val encodedPolyline = EncodedPolylineCodec.encode(current.points)
        repository.updateState(
            current.id,
            RunStatus.FINISHED,
            duration,
            null,
            current.currentSegmentIndex,
            System.currentTimeMillis(),
            encodedPolyline = encodedPolyline
        )
        session = current.copy(
            status = RunStatus.FINISHED,
            activeDurationMillis = duration,
            runningStartedElapsedRealtimeMillis = null,
            encodedPolyline = encodedPolyline
        )
        locationProvider.stop()
        ticker?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun fail(current: RunSession, message: String) {
        val duration = current.activeDurationAt(SystemClock.elapsedRealtime())
        repository.updateState(current.id, RunStatus.ERROR, duration, null, current.currentSegmentIndex, errorMessage = message)
        session = current.copy(status = RunStatus.ERROR, activeDurationMillis = duration, errorMessage = message)
        locationProvider.stop()
        ticker?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(1_000)
                updateNotification()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Running tracking", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(current: RunSession?): Notification {
        val isPaused = current?.status == RunStatus.PAUSED
        val duration = current?.activeDurationAt(SystemClock.elapsedRealtime()) ?: 0L
        val content = "${String.format(Locale.US, "%.2f km", (current?.distanceMeters ?: 0.0) / 1_000.0)} • ${formatDuration(duration)}"
        val openIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), pendingFlags())
        val toggleAction = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        val toggleLabel = if (isPaused) "Resume" else "Pause"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_running)
            .setContentTitle(
                when {
                    isPaused -> "Workout paused"
                    current?.activityType == RunActivityType.WALKING -> "Walking in progress"
                    else -> "Running in progress"
                }
            )
            .setContentText(content)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, toggleLabel, servicePendingIntent(toggleAction, 1))
            .addAction(0, "Finish", servicePendingIntent(ACTION_FINISH, 2))
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(session))
    }

    private fun servicePendingIntent(action: String, requestCode: Int) = PendingIntent.getService(
        this, requestCode, Intent(this, RunningTrackingService::class.java).setAction(action), pendingFlags()
    )

    private fun pendingFlags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun hasPrecisePermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun androidDistanceMeters(from: RunPoint, to: RunPoint): Double {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, result)
        return result[0].toDouble()
    }

    override fun onDestroy() {
        locationProvider.stop()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.homeworkout.running.START"
        const val ACTION_RECOVER = "com.example.homeworkout.running.RECOVER"
        const val ACTION_PAUSE = "com.example.homeworkout.running.PAUSE"
        const val ACTION_RESUME = "com.example.homeworkout.running.RESUME"
        const val ACTION_FINISH = "com.example.homeworkout.running.FINISH"
        private const val CHANNEL_ID = "running_tracking"
        private const val NOTIFICATION_ID = 7301

        fun send(context: Context, action: String, metadata: RunSessionMetadata? = null) {
            val intent = Intent(context, RunningTrackingService::class.java).setAction(action).apply {
                metadata?.let {
                    putExtra(EXTRA_ACTIVITY_TYPE, it.activityType.name)
                    putExtra(EXTRA_TITLE, it.title)
                    putExtra(EXTRA_PROGRAM_ID, it.programId)
                    putExtra(EXTRA_TRAINING_SESSION_ID, it.trainingSessionId)
                }
            }
            if (action == ACTION_START || action == ACTION_RECOVER) ContextCompat.startForegroundService(context, intent)
            else context.startService(intent)
        }

        private fun formatDuration(millis: Long): String {
            val seconds = millis / 1_000
            return String.format(Locale.US, "%02d:%02d:%02d", seconds / 3_600, seconds / 60 % 60, seconds % 60)
        }

        private const val EXTRA_ACTIVITY_TYPE = "activity_type"
        private const val EXTRA_TITLE = "activity_title"
        private const val EXTRA_PROGRAM_ID = "program_id"
        private const val EXTRA_TRAINING_SESSION_ID = "training_session_id"
    }

    private fun Intent.toMetadata() = RunSessionMetadata(
        activityType = getStringExtra(EXTRA_ACTIVITY_TYPE)?.let(RunActivityType::valueOf) ?: RunActivityType.RUNNING,
        title = getStringExtra(EXTRA_TITLE),
        programId = getStringExtra(EXTRA_PROGRAM_ID),
        trainingSessionId = getStringExtra(EXTRA_TRAINING_SESSION_ID)
    )
}
