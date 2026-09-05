package com.example.homeworkout.ui.core.running.detail

import android.graphics.Color
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.BuildConfig
import com.example.homeworkout.domain.models.running.RunActivityType
import com.example.homeworkout.domain.models.running.RunCoordinate
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PageBackground
import com.example.homeworkout.ui.theme.SlateGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property.LINE_CAP_ROUND
import org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.Point

private const val HISTORY_ROUTE_SOURCE = "history-route-source"
private const val HISTORY_ROUTE_LAYER = "history-route-layer"
private const val HISTORY_START_SOURCE = "history-start-source"
private const val HISTORY_START_LAYER = "history-start-layer"
private const val HISTORY_FINISH_SOURCE = "history-finish-source"
private const val HISTORY_FINISH_LAYER = "history-finish-layer"

@Composable
fun RunDetailScreen(viewModel: RunDetailViewModel, onNavigateBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Session details", onNavigateBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(PageBackground)) {
            when (val current = state) {
                RunDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = BrandBlue)
                RunDetailUiState.NotFound -> Text("Saved session not found", Modifier.align(Alignment.Center), color = SlateGray)
                is RunDetailUiState.Success -> DetailContent(current.session, current.routeSegments)
            }
        }
    }
}

@Composable
private fun DetailContent(session: RunSession, routeSegments: List<List<RunCoordinate>>) {
    Column(Modifier.fillMaxSize()) {
        HistoricalRouteMap(routeSegments, Modifier.fillMaxWidth().weight(1f))
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp).border(1.dp, HairlineGray, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    session.title ?: if (session.activityType == RunActivityType.WALKING) "Walking session" else "Running session",
                    color = InkBlack,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(formatTimestamp(session.startedAt), color = SlateGray, fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text(formatDistance(session.distanceMeters), color = BrandBlue, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("Total distance", color = SlateGray, fontSize = 12.sp)
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = HairlineGray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailMetric("Time", formatDuration(session.durationSeconds))
                    DetailMetric("Average pace", formatPace(session.averagePaceMinutesPerKilometer))
                    DetailMetric("Calories", String.format(Locale.getDefault(), "%.0f kcal", session.calories ?: 0.0))
                }
            }
        }
    }
}

@Composable
private fun HistoricalRouteMap(routeSegments: List<List<RunCoordinate>>, modifier: Modifier = Modifier) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    val coordinates = remember(routeSegments) { routeSegments.flatten() }

    Box(modifier.background(BrandBlueTint), contentAlignment = Alignment.Center) {
        if (BuildConfig.STADIA_MAPS_API_KEY.isBlank()) {
            Text("Add STADIA_MAPS_API_KEY to display the saved route map.", color = SlateGray)
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        mapView = this
                        onCreate(null)
                        onStart()
                        onResume()
                        getMapAsync { map ->
                            mapLibreMap = map
                            map.uiSettings.isAttributionEnabled = true
                            map.uiSettings.isLogoEnabled = true
                            val styleUrl = "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=${BuildConfig.STADIA_MAPS_API_KEY}"
                            map.setStyle(styleUrl) { style ->
                                setupHistoricalRouteStyle(style, routeSegments, coordinates)
                                post {
                                    fitCameraToRoute(map, coordinates, animated = true)
                                }
                            }
                        }
                    }
                }
            )

            if (coordinates.isNotEmpty()) {
                IconButton(
                    onClick = { fitCameraToRoute(mapLibreMap, coordinates, animated = true) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                        .background(CardWhite.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
                        .border(1.dp, HairlineGray, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Fit route",
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text(
                    "No GPS route coordinates recorded for this session.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(CardWhite.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = SlateGray,
                    fontSize = 13.sp
                )
            }
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val view = mapView ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> view.onStart()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_STOP -> view.onStop()
                Lifecycle.Event.ON_DESTROY -> view.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

private fun setupHistoricalRouteStyle(
    style: Style,
    routeSegments: List<List<RunCoordinate>>,
    coordinates: List<RunCoordinate>
) {
    // 1. Line Route
    val lineSegments = routeSegments.filter { it.size >= 2 }.map { segment ->
        segment.map { Point.fromLngLat(it.longitude, it.latitude) }
    }
    val routeFeatures = if (lineSegments.isEmpty()) FeatureCollection.fromFeatures(emptyArray())
    else FeatureCollection.fromFeature(Feature.fromGeometry(MultiLineString.fromLngLats(lineSegments)))
    style.addSource(GeoJsonSource(HISTORY_ROUTE_SOURCE, routeFeatures))
    style.addLayer(
        LineLayer(HISTORY_ROUTE_LAYER, HISTORY_ROUTE_SOURCE).withProperties(
            lineColor(Color.rgb(0, 82, 254)),
            lineWidth(6f),
            lineCap(LINE_CAP_ROUND),
            lineJoin(LINE_JOIN_ROUND)
        )
    )

    // 2. Start Marker (Green)
    val startPoint = coordinates.firstOrNull()
    val startFeature = startPoint?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) }
    val startCollection = startFeature?.let { FeatureCollection.fromFeature(it) }
        ?: FeatureCollection.fromFeatures(emptyArray())
    style.addSource(GeoJsonSource(HISTORY_START_SOURCE, startCollection))
    style.addLayer(
        CircleLayer(HISTORY_START_LAYER, HISTORY_START_SOURCE).withProperties(
            circleRadius(7.5f),
            circleColor(Color.rgb(16, 185, 129)), // Emerald / Green
            circleStrokeColor(Color.WHITE),
            circleStrokeWidth(3f)
        )
    )

    // 3. Finish Marker (Red)
    val endPoint = if (coordinates.size > 1) coordinates.lastOrNull() else null
    val endFeature = endPoint?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) }
    val endCollection = endFeature?.let { FeatureCollection.fromFeature(it) }
        ?: FeatureCollection.fromFeatures(emptyArray())
    style.addSource(GeoJsonSource(HISTORY_FINISH_SOURCE, endCollection))
    style.addLayer(
        CircleLayer(HISTORY_FINISH_LAYER, HISTORY_FINISH_SOURCE).withProperties(
            circleRadius(7.5f),
            circleColor(Color.rgb(239, 68, 68)), // Red / Finish
            circleStrokeColor(Color.WHITE),
            circleStrokeWidth(3f)
        )
    )
}

private fun fitCameraToRoute(map: MapLibreMap?, coordinates: List<RunCoordinate>, animated: Boolean = true) {
    val readyMap = map ?: return
    if (coordinates.isEmpty()) return

    if (coordinates.size == 1) {
        val single = coordinates.first()
        val update = CameraUpdateFactory.newLatLngZoom(LatLng(single.latitude, single.longitude), 16.0)
        if (animated) readyMap.easeCamera(update, 500) else readyMap.moveCamera(update)
        return
    }

    val minLat = coordinates.minOf { it.latitude }
    val maxLat = coordinates.maxOf { it.latitude }
    val minLng = coordinates.minOf { it.longitude }
    val maxLng = coordinates.maxOf { it.longitude }
    val centerLat = (minLat + maxLat) / 2.0
    val centerLng = (minLng + maxLng) / 2.0

    if (Math.abs(maxLat - minLat) < 0.00001 && Math.abs(maxLng - minLng) < 0.00001) {
        val update = CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), 16.0)
        if (animated) readyMap.easeCamera(update, 500) else readyMap.moveCamera(update)
        return
    }

    val bounds = LatLngBounds.Builder()
        .include(LatLng(minLat, minLng))
        .include(LatLng(maxLat, maxLng))
        .build()

    val boundsUpdate = runCatching {
        CameraUpdateFactory.newLatLngBounds(bounds, 64)
    }.getOrNull()

    if (boundsUpdate != null) {
        runCatching {
            if (animated) readyMap.easeCamera(boundsUpdate, 900) else readyMap.moveCamera(boundsUpdate)
        }.onFailure {
            val fallbackUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), 15.0)
            if (animated) readyMap.easeCamera(fallbackUpdate, 500) else readyMap.moveCamera(fallbackUpdate)
        }
    } else {
        val fallbackUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), 15.0)
        if (animated) readyMap.easeCamera(fallbackUpdate, 500) else readyMap.moveCamera(fallbackUpdate)
    }
}

@Composable private fun DetailMetric(label: String, value: String) {
    Column {
        Text(label, color = SlateGray, fontSize = 11.sp)
        Text(value, color = InkBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTimestamp(timestamp: Long) = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(timestamp))
private fun formatDistance(meters: Double) = String.format(Locale.getDefault(), "%.2f km", meters / 1_000.0)
private fun formatDuration(seconds: Long) = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
private fun formatPace(pace: Double): String {
    if (!pace.isFinite() || pace <= 0.0) return "--:-- /km"
    val minutes = pace.toInt()
    val seconds = ((pace - minutes) * 60).toInt()
    return String.format(Locale.getDefault(), "%d:%02d /km", minutes, seconds)
}
