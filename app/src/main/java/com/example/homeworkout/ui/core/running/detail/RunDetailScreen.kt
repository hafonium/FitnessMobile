package com.example.homeworkout.ui.core.running.detail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property.LINE_CAP_ROUND
import org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND
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
    val coordinates = remember(routeSegments) { routeSegments.flatten() }
    Box(modifier.background(BrandBlueTint), contentAlignment = Alignment.Center) {
        if (BuildConfig.STADIA_MAPS_API_KEY.isBlank()) {
            Text("Add STADIA_MAPS_API_KEY to display the saved route map.", color = SlateGray)
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).also { view ->
                        mapView = view
                        view.onCreate(null)
                        view.getMapAsync { map ->
                            map.uiSettings.isAttributionEnabled = true
                            map.uiSettings.isLogoEnabled = true
                            val styleUrl = "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=${BuildConfig.STADIA_MAPS_API_KEY}"
                            map.setStyle(styleUrl) { style ->
                                val lineSegments = routeSegments.filter { it.size >= 2 }.map { segment ->
                                    segment.map { Point.fromLngLat(it.longitude, it.latitude) }
                                }
                                val features = if (lineSegments.isEmpty()) FeatureCollection.fromFeatures(emptyArray())
                                else FeatureCollection.fromFeature(Feature.fromGeometry(MultiLineString.fromLngLats(lineSegments)))
                                style.addSource(GeoJsonSource(HISTORY_ROUTE_SOURCE, features))
                                style.addLayer(
                                    LineLayer(HISTORY_ROUTE_LAYER, HISTORY_ROUTE_SOURCE).withProperties(
                                        lineColor("#0052FE"),
                                        lineWidth(6f),
                                        lineCap(LINE_CAP_ROUND),
                                        lineJoin(LINE_JOIN_ROUND)
                                    )
                                )
                                when {
                                    coordinates.size >= 2 -> {
                                        val bounds = LatLngBounds.Builder()
                                        coordinates.forEach { bounds.include(LatLng(it.latitude, it.longitude)) }
                                        view.post {
                                            runCatching {
                                                map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100), 900)
                                            }
                                        }
                                    }
                                    coordinates.size == 1 -> view.post {
                                        map.easeCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(coordinates.first().latitude, coordinates.first().longitude),
                                                16.0
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val view = mapView
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view?.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view?.onResume()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> view?.onStart()
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                Lifecycle.Event.ON_STOP -> view?.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            view?.onPause()
            view?.onStop()
            view?.onDestroy()
        }
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
