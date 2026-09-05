package com.example.homeworkout.ui.core.trainingplayer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.running.service.RunningTrackingService
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PageBackground
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.ui.theme.StreakRed
import java.util.Locale

@Composable
fun StructuredTrainingPlayerScreen(
    viewModel: StructuredTrainingPlayerViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    var permissionRequested by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingStart by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun gpsEnabled() = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        .isProviderEnabled(LocationManager.GPS_PROVIDER)
    fun beginTracking() {
        if (!gpsEnabled()) {
            message = "Turn on Location/GPS to track this workout."
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val serviceAction = if (state.tracking?.status == com.example.homeworkout.domain.models.running.RunStatus.PAUSED) {
            RunningTrackingService.ACTION_RESUME
        } else RunningTrackingService.ACTION_START
        val started = runCatching { RunningTrackingService.send(context, serviceAction) }
        if (started.isFailure) {
            message = "Could not start GPS tracking: ${started.exceptionOrNull()?.message}"
            return
        }
        viewModel.begin()
        message = null
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionRequested = true
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true && pendingStart) beginTracking()
        else message = "Precise Location is required for route and distance tracking."
        pendingStart = false
    }
    fun requestStart() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            beginTracking()
        } else if (permissionRequested && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            message = "Enable Precise Location in App Settings."
        } else {
            pendingStart = true
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    LaunchedEffect(state.status) {
        if (state.status == IntervalPlayerStatus.FINISHED) {
            RunningTrackingService.send(context, RunningTrackingService.ACTION_FINISH)
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(PageBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBlue)
        }
        return
    }

    Column(
        Modifier.fillMaxSize().background(PageBackground).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.background(CloudGray, CircleShape)) {
                Icon(Icons.Default.Close, contentDescription = "Close workout", tint = InkBlack)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(state.program?.title.orEmpty(), color = SlateGray, fontSize = 12.sp)
                Text(state.session?.title.orEmpty(), color = InkBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        val completedSteps = state.steps.take(state.stepIndex).sumOf { it.durationSeconds }
        val totalSeconds = state.steps.sumOf { it.durationSeconds }.coerceAtLeast(1)
        val progress = (completedSteps + (state.currentStep?.durationSeconds ?: 0) - state.secondsRemaining).toFloat() / totalSeconds
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = BrandBlue,
            trackColor = CloudGray
        )

        Column(
            Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(20.dp))
                .border(1.dp, HairlineGray, RoundedCornerShape(20.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                when (state.status) {
                    IntervalPlayerStatus.READY -> "READY"
                    IntervalPlayerStatus.PAUSED -> "PAUSED"
                    IntervalPlayerStatus.FINISHED -> "COMPLETE"
                    IntervalPlayerStatus.ACTIVE -> "CURRENT INTERVAL"
                },
                color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
            Text(state.currentStep?.label ?: "Workout complete", color = InkBlack, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(formatClock(state.secondsRemaining), color = InkBlack, fontSize = 54.sp, fontWeight = FontWeight.ExtraBold)
            Text("Step ${minOf(state.stepIndex + 1, state.steps.size)} of ${state.steps.size}", color = SlateGray)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(Icons.AutoMirrored.Filled.DirectionsRun, "DISTANCE", String.format(Locale.US, "%.2f km", (state.tracking?.distanceMeters ?: 0.0) / 1000), Modifier.weight(1f))
            Metric(Icons.Default.LocalFireDepartment, "ELAPSED", formatClock(state.elapsedSeconds), Modifier.weight(1f))
        }

        message?.let { Text(it, color = StreakRed, fontSize = 13.sp) }
        when (state.status) {
            IntervalPlayerStatus.READY -> AppButton("START WORKOUT", ::requestStart, Modifier.fillMaxWidth())
            IntervalPlayerStatus.ACTIVE -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppButton("PAUSE", {
                    viewModel.pause()
                    RunningTrackingService.send(context, RunningTrackingService.ACTION_PAUSE)
                }, Modifier.weight(1f), variant = AppButtonVariant.Tonal)
                AppButton("FINISH", viewModel::finish, Modifier.weight(1f), variant = AppButtonVariant.Outlined)
            }
            IntervalPlayerStatus.PAUSED -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppButton("RESUME", {
                    viewModel.resume()
                    RunningTrackingService.send(context, RunningTrackingService.ACTION_RESUME)
                }, Modifier.weight(1f))
                AppButton("FINISH", viewModel::finish, Modifier.weight(1f), variant = AppButtonVariant.Outlined)
            }
            IntervalPlayerStatus.FINISHED -> AppButton("DONE", onClose, Modifier.fillMaxWidth())
        }

        if (message != null) {
            AppButton(
                if (gpsEnabled()) "OPEN APP SETTINGS" else "OPEN LOCATION SETTINGS",
                {
                    val intent = if (gpsEnabled()) {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    } else Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                },
                Modifier.fillMaxWidth(),
                variant = AppButtonVariant.Outlined
            )
        }

        Text("Workout steps", color = InkBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        state.steps.forEachIndexed { index, step ->
            Row(
                Modifier.fillMaxWidth()
                    .background(if (index == state.stepIndex) BrandBlueTint else CardWhite, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (index == state.stepIndex) BrandBlue.copy(alpha = .25f) else HairlineGray,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (index == state.stepIndex && state.status == IntervalPlayerStatus.ACTIVE) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (index <= state.stepIndex) BrandBlue else SlateGray
                )
                Spacer(Modifier.width(10.dp))
                Text(step.label, color = InkBlack, modifier = Modifier.weight(1f))
                Text(formatClock(step.durationSeconds), color = SlateGray)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun Metric(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier) {
    Column(
        modifier.background(CardWhite, RoundedCornerShape(14.dp))
            .border(1.dp, HairlineGray, RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        Text(value, color = InkBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = SlateGray, fontSize = 10.sp)
    }
}

private fun formatClock(totalSeconds: Int): String = String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
