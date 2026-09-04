package com.example.homeworkout.ui.core.running

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.BuildConfig
import com.example.homeworkout.domain.models.running.RunStatus
import com.example.homeworkout.running.service.RunningTrackingService
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.StatTile
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.SlateGray
import java.util.Locale
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun WalkRunScreen(viewModel: RunningViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var mapMessage by remember { mutableStateOf<String?>(null) }
    var pendingStart by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun gpsEnabled(): Boolean = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        .isProviderEnabled(LocationManager.GPS_PROVIDER)
    fun startTracking() {
        if (!gpsEnabled()) {
            permissionMessage = "Turn on device Location/GPS before starting your run."
            return
        }
        permissionMessage = null
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        runCatching { RunningTrackingService.send(context, RunningTrackingService.ACTION_START) }
            .onFailure { permissionMessage = "Could not start running tracker: ${it.message}" }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionRequested = true
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (pendingStart) startTracking()
        } else {
            permissionMessage = if (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                "Precise Location is required for accurate distance and route tracking."
            } else "Location permission is required to track a run."
        }
        pendingStart = false
    }

    fun requestStart() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startTracking()
        } else if (permissionRequested && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionMessage = "Precise Location is disabled. Open App Settings to enable it."
        } else {
            pendingStart = true
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    val permanentlyDenied = permissionRequested &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(state.session?.id, state.session?.status) {
        if (state.session?.status == RunStatus.RUNNING) {
            RunningTrackingService.send(context, RunningTrackingService.ACTION_RECOVER)
        }
    }

    Scaffold(topBar = { BackTopBar("Running", onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(360.dp).background(BrandBlueTint, RoundedCornerShape(20.dp))) {
                RunningMap(
                    points = state.session?.points.orEmpty(),
                    apiKey = BuildConfig.STADIA_MAPS_API_KEY,
                    onMapError = { mapMessage = it }
                )
                mapMessage?.let {
                    Text(it, Modifier.align(Alignment.TopCenter).padding(12.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            val status = when (state.session?.status) {
                RunStatus.RUNNING -> state.session?.errorMessage
                    ?: if (state.session?.points.isNullOrEmpty()) "Searching for GPS…" else "GPS tracking active"
                RunStatus.PAUSED -> "Run paused"
                RunStatus.FINISHED -> "Run finished"
                RunStatus.ERROR -> state.session?.errorMessage ?: "Tracking error"
                null -> "Ready to run"
            }
            Text(status, style = MaterialTheme.typography.titleMedium)
            permissionMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(Icons.AutoMirrored.Filled.DirectionsRun, String.format(Locale.US, "%.2f km", state.telemetry.distanceKilometers), "DISTANCE", Modifier.weight(1f))
                StatTile(Icons.Default.Timer, formatDuration(state.activeDurationMillis), "DURATION", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(Icons.Default.Speed, formatPace(state.telemetry.paceSecondsPerKilometer), "AVG PACE", Modifier.weight(1f))
                StatTile(Icons.Default.LocalFireDepartment, state.telemetry.calories?.let { String.format(Locale.US, "%.0f kcal", it) } ?: "--", "CALORIES", Modifier.weight(1f))
            }

            when (state.session?.status) {
                RunStatus.RUNNING -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppButton("Pause", { RunningTrackingService.send(context, RunningTrackingService.ACTION_PAUSE) }, Modifier.weight(1f), variant = AppButtonVariant.Tonal)
                    AppButton("Finish", { RunningTrackingService.send(context, RunningTrackingService.ACTION_FINISH) }, Modifier.weight(1f), variant = AppButtonVariant.Dark)
                }
                RunStatus.PAUSED -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppButton("Resume", { RunningTrackingService.send(context, RunningTrackingService.ACTION_RESUME) }, Modifier.weight(1f))
                    AppButton("Finish", { RunningTrackingService.send(context, RunningTrackingService.ACTION_FINISH) }, Modifier.weight(1f), variant = AppButtonVariant.Dark)
                }
                else -> AppButton("Start Run", ::requestStart, Modifier.fillMaxWidth())
            }

            if (permissionMessage != null) {
                val actionText = when {
                    permanentlyDenied -> "Open App Settings"
                    !gpsEnabled() -> "Open Location Settings"
                    else -> "Grant Precise Location"
                }
                AppButton(actionText, {
                    when {
                        permanentlyDenied -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                        !gpsEnabled() -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        else -> locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                }, Modifier.fillMaxWidth(), variant = AppButtonVariant.Outlined)
            }
            if (state.session?.status == RunStatus.RUNNING && state.session?.errorMessage?.contains("Location is off") == true) {
                AppButton(
                    "Open Location Settings",
                    { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                    Modifier.fillMaxWidth(),
                    variant = AppButtonVariant.Outlined
                )
            }
            Text("Run tracking continues without map tiles if the network is unavailable.", style = MaterialTheme.typography.bodySmall, color = SlateGray)
        }
    }
}

@Composable
private fun RunningMap(points: List<com.example.homeworkout.domain.models.running.RunPoint>, apiKey: String, onMapError: (String) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var renderer by remember { mutableStateOf<RunningRouteRenderer?>(null) }
    var autoFollow by remember { mutableStateOf(true) }
    var centeredOnce by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            MapView(context).also { view ->
                mapView = view
                view.onCreate(null)
                view.getMapAsync { readyMap ->
                    map = readyMap
                    readyMap.addOnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) autoFollow = false
                    }
                    if (apiKey.isBlank()) {
                        onMapError("Add STADIA_MAPS_API_KEY to local.properties to load Stadia Outdoors.")
                    } else {
                        readyMap.setStyle("https://tiles.stadiamaps.com/styles/outdoors.json?api_key=$apiKey") { style ->
                            renderer = RunningRouteRenderer(style).also { it.update(points) }
                        }
                    }
                }
            }
        }
    )

    DisposableEffect(lifecycle, mapView) {
        val view = mapView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> view?.onStart()
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                Lifecycle.Event.ON_STOP -> view?.onStop()
                Lifecycle.Event.ON_DESTROY -> view?.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(apiKey, renderer) {
        if (apiKey.isNotBlank() && renderer == null) {
            kotlinx.coroutines.delay(10_000)
            if (renderer == null) onMapError("Map unavailable. GPS tracking will continue.")
        }
    }

    LaunchedEffect(points, renderer, autoFollow) {
        renderer?.update(points)
        val current = points.lastOrNull() ?: return@LaunchedEffect
        if (!centeredOnce || autoFollow) {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(current.latitude, current.longitude), 16.0), 500)
            centeredOnce = true
        }
    }

    if (!autoFollow && points.isNotEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            IconButton(onClick = { autoFollow = true }, modifier = Modifier.padding(12.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter map")
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1_000
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}

private fun formatPace(secondsPerKm: Double?): String {
    if (secondsPerKm == null || !secondsPerKm.isFinite()) return "-- /km"
    val seconds = secondsPerKm.toLong()
    return String.format(Locale.US, "%d:%02d /km", seconds / 60, seconds % 60)
}
