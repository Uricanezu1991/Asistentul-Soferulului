package com.example.driverassistant.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float? = null,
    val accuracyMeters: Float? = null
)

class LocationClient(context: Context) {
    private val fused: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val _locationState = MutableStateFlow<LocationPoint?>(null)
    val locationState: StateFlow<LocationPoint?> = _locationState

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                _locationState.value = LocationPoint(it.latitude, it.longitude, it.speed, it.accuracy)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun start() {
        fused.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
                .setMinUpdateDistanceMeters(25f)
                .build(),
            callback,
            Looper.getMainLooper()
        )
    }
}
