package com.example.smartgpstracker.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.smartgpstracker.MainActivity
import com.example.smartgpstracker.R
import com.example.smartgpstracker.data.model.TripState
import com.example.smartgpstracker.utils.Formatters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var notificationUpdateJob: Job? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var fusedLocationCallback: LocationCallback? = null

    private var locationManager: LocationManager? = null
    private var frameworkLocationListener: LocationListener? = null

    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "gps_tracker_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                TripTrackingManager.startTrip()
                startLocationUpdates()
                startNotificationUpdates()
            }
            ACTION_PAUSE -> {
                TripTrackingManager.pauseTrip()
                updateNotification()
            }
            ACTION_RESUME -> {
                TripTrackingManager.resumeTrip()
                updateNotification()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopNotificationUpdates()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingAppIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isImperial = TripTrackingManager.isImperial.value
        val formattedDist = Formatters.formatDistance(TripTrackingManager.distanceMeters.value, isImperial)
        val formattedTime = Formatters.formatDuration(TripTrackingManager.elapsedTimeSeconds.value)
        val formattedSpeed = Formatters.formatSpeed(TripTrackingManager.currentSpeedKmh.value, isImperial)

        val state = TripTrackingManager.tripState.value
        val title = when (state) {
            TripState.PAUSED -> "Trip Paused"
            TripState.RECORDING -> "Tracking Trip..."
            else -> "GPS Tracker"
        }

        val content = "$formattedDist • $formattedTime • $formattedSpeed"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        // Action buttons
        if (state == TripState.RECORDING) {
            val pauseIntent = Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePending = PendingIntent.getService(
                this, 1, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Pause", pausePending)
        } else if (state == TripState.PAUSED) {
            val resumeIntent = Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePending = PendingIntent.getService(
                this, 2, resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Resume", resumePending)
        }

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Stop", stopPending)

        return builder.build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = serviceScope.launch {
            while (isActive) {
                delay(1500L)
                updateNotification()
            }
        }
    }

    private fun stopNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = null
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val interval = TripTrackingManager.gpsIntervalMillis.value
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .setMinUpdateIntervalMillis(interval / 2)
                .setMinUpdateDistanceMeters(1.0f)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    TripTrackingManager.onLocationUpdated(location)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                request,
                fusedLocationCallback!!,
                Looper.getMainLooper()
            ).addOnFailureListener {
                // Fallback to framework LocationManager
                startFrameworkLocationUpdates()
            }
        } catch (_: Exception) {
            startFrameworkLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startFrameworkLocationUpdates() {
        val lm = locationManager ?: return
        frameworkLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                TripTrackingManager.onLocationUpdated(location)
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
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    TripTrackingManager.gpsIntervalMillis.value,
                    1.0f,
                    frameworkLocationListener!!
                )
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    TripTrackingManager.gpsIntervalMillis.value,
                    1.0f,
                    frameworkLocationListener!!
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (_: Exception) {}
        }
        fusedLocationCallback = null

        frameworkLocationListener?.let {
            try {
                locationManager?.removeUpdates(it)
            } catch (_: Exception) {}
        }
        frameworkLocationListener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        stopNotificationUpdates()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
