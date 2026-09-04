package com.example.homeworkout.ui.core.running

import android.graphics.Color
import com.example.homeworkout.domain.models.running.RunPoint
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

class RunningRouteRenderer(private val style: Style) {
    init {
        style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
        style.addLayer(LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
            lineColor(Color.rgb(0, 82, 254)), lineWidth(6f), lineCap(LINE_CAP_ROUND), lineJoin(LINE_JOIN_ROUND)
        ))
        style.addSource(GeoJsonSource(RUNNER_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
        style.addLayer(CircleLayer(RUNNER_LAYER_ID, RUNNER_SOURCE_ID).withProperties(
            circleRadius(8f), circleColor(Color.rgb(0, 82, 254)), circleStrokeColor(Color.WHITE), circleStrokeWidth(3f)
        ))
    }

    fun update(points: List<RunPoint>) {
        val lines = points.groupBy { it.segmentIndex }.toSortedMap().values
            .map { segment -> segment.map { Point.fromLngLat(it.longitude, it.latitude) } }
            .filter { it.size >= 2 }
        val routeFeature = if (lines.isEmpty()) FeatureCollection.fromFeatures(emptyArray())
        else FeatureCollection.fromFeature(Feature.fromGeometry(MultiLineString.fromLngLats(lines)))
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)?.setGeoJson(routeFeature)
        val current = points.lastOrNull()
        val runnerFeature = current?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) }
        style.getSourceAs<GeoJsonSource>(RUNNER_SOURCE_ID)?.setGeoJson(
            runnerFeature?.let(FeatureCollection::fromFeature) ?: FeatureCollection.fromFeatures(emptyArray())
        )
    }

    companion object {
        const val ROUTE_SOURCE_ID = "run-route-source"
        const val ROUTE_LAYER_ID = "run-route-layer"
        const val RUNNER_SOURCE_ID = "run-runner-source"
        const val RUNNER_LAYER_ID = "run-runner-layer"
    }
}
