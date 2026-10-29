package com.example.smartgpstracker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.example.smartgpstracker.service.TripTrackingManager
import com.example.smartgpstracker.utils.PermissionUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationManagerHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var fusedCallback: LocationCallback? = null

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var platformListener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (!PermissionUtils.hasLocationPermission(context)) return

        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(0.5f)
                .setWaitForAccurateLocation(false)
                .build()

            fusedCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    TripTrackingManager.onLocationUpdated(loc)
                }
            }

            fusedClient.requestLocationUpdates(request, fusedCallback!!, Looper.getMainLooper())
                .addOnFailureListener {
                    startPlatformFallback()
                }
        } catch (_: Exception) {
            startPlatformFallback()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startPlatformFallback() {
        val lm = locationManager ?: return
        platformListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                TripTrackingManager.onLocationUpdated(loc)
            }
            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    TripTrackingManager.onGpsLost()
                }
            }
            override fun onProviderEnabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500L, 0.5f, platformListener!!)
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500L, 0.5f, platformListener!!)
            }
        } catch (_: Exception) {}
    }

    fun stopListening() {
        fusedCallback?.let {
            try {
                fusedClient.removeLocationUpdates(it)
            } catch (_: Exception) {}
        }
        fusedCallback = null

        platformListener?.let {
            try {
                locationManager?.removeUpdates(it)
            } catch (_: Exception) {}
        }
        platformListener = null
    }
}
