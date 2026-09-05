package com.example.homeworkout.running.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper

class RunningLocationProvider(context: Context) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var listener: LocationListener? = null

    fun isGpsEnabled(): Boolean = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    @SuppressLint("MissingPermission")
    fun start(onLocation: (Location) -> Unit, onProviderDisabled: () -> Unit, onProviderEnabled: () -> Unit) {
        stop()
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onLocation(location)
            override fun onProviderDisabled(provider: String) = onProviderDisabled()
            override fun onProviderEnabled(provider: String) = onProviderEnabled()
            @Deprecated("Deprecated in Android") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }.also {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL_MILLIS,
                MIN_DISTANCE_METERS,
                it,
                Looper.getMainLooper()
            )
        }
    }

    fun stop() {
        listener?.let(manager::removeUpdates)
        listener = null
    }

    companion object {
        const val LOCATION_INTERVAL_MILLIS = 1_500L
        const val MIN_DISTANCE_METERS = 2f
    }
}
