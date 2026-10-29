package com.example.smartgpstracker.service

import android.location.Location
import com.example.smartgpstracker.data.model.LocationPoint
import com.example.smartgpstracker.data.model.TripRecord
import com.example.smartgpstracker.data.model.TripState
import com.example.smartgpstracker.utils.DistanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object TripTrackingManager {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private val _tripState = MutableStateFlow(TripState.IDLE)
    val tripState: StateFlow<TripState> = _tripState.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationPoint?>(null)
    val currentLocation: StateFlow<LocationPoint?> = _currentLocation.asStateFlow()

    private val _isGpsFixAcquired = MutableStateFlow(false)
    val isGpsFixAcquired: StateFlow<Boolean> = _isGpsFixAcquired.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val routePoints: StateFlow<List<LocationPoint>> = _routePoints.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds: StateFlow<Long> = _elapsedTimeSeconds.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0f)
    val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

    private val _maxSpeedKmh = MutableStateFlow(0f)
    val maxSpeedKmh: StateFlow<Float> = _maxSpeedKmh.asStateFlow()

    private val _avgSpeedKmh = MutableStateFlow(0f)
    val avgSpeedKmh: StateFlow<Float> = _avgSpeedKmh.asStateFlow()

    // Settings
    val isImperial = MutableStateFlow(false)
    val minDistanceFilterMeters = MutableStateFlow(3.0f)
    val gpsIntervalMillis = MutableStateFlow(2000L)

    private var tripStartTimeMillis: Long = 0L
    private var lastRecordedPoint: LocationPoint? = null

    fun startTrip() {
        if (_tripState.value == TripState.RECORDING) return

        _tripState.value = TripState.RECORDING
        tripStartTimeMillis = System.currentTimeMillis()
        _distanceMeters.value = 0.0
        _elapsedTimeSeconds.value = 0L
        _maxSpeedKmh.value = 0f
        _avgSpeedKmh.value = 0f
        _routePoints.value = emptyList()
        lastRecordedPoint = null

        // Add current location if available
        _currentLocation.value?.let { current ->
            _routePoints.value = listOf(current)
            lastRecordedPoint = current
        }

        startTimer()
    }

    fun pauseTrip() {
        if (_tripState.value == TripState.RECORDING) {
            _tripState.value = TripState.PAUSED
            stopTimer()
        }
    }

    fun resumeTrip() {
        if (_tripState.value == TripState.PAUSED) {
            _tripState.value = TripState.RECORDING
            startTimer()
        }
    }

    fun stopTrip(title: String = ""): TripRecord? {
        if (_tripState.value == TripState.IDLE) return null

        stopTimer()
        val endTime = System.currentTimeMillis()
        val record = TripRecord(
            title = if (title.isNotBlank()) title else "Trip ${System.currentTimeMillis()}",
            startTimeMillis = tripStartTimeMillis,
            endTimeMillis = endTime,
            durationSeconds = _elapsedTimeSeconds.value,
            totalDistanceMeters = _distanceMeters.value,
            avgSpeedKmh = _avgSpeedKmh.value,
            maxSpeedKmh = _maxSpeedKmh.value,
            routePoints = _routePoints.value,
            createdAtMillis = endTime
        )

        _tripState.value = TripState.STOPPED
        return record
    }

    fun resetTrip() {
        stopTimer()
        _tripState.value = TripState.IDLE
        _distanceMeters.value = 0.0
        _elapsedTimeSeconds.value = 0L
        _currentSpeedKmh.value = 0f
        _maxSpeedKmh.value = 0f
        _avgSpeedKmh.value = 0f
        _routePoints.value = emptyList()
        lastRecordedPoint = null
    }

    fun onLocationUpdated(location: Location) {
        val speedMps = if (location.hasSpeed()) location.speed else 0f
        val speedKmh = DistanceCalculator.metersPerSecondToKmh(speedMps)

        val point = LocationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else 0.0,
            speedMps = speedMps,
            accuracy = if (location.hasAccuracy()) location.accuracy else 0f,
            timestampMillis = location.time
        )

        _currentLocation.value = point
        _isGpsFixAcquired.value = true
        _currentSpeedKmh.value = speedKmh

        if (_tripState.value == TripState.RECORDING) {
            // Update max speed
            if (speedKmh > _maxSpeedKmh.value) {
                _maxSpeedKmh.value = speedKmh
            }

            val last = lastRecordedPoint
            if (last == null) {
                lastRecordedPoint = point
                _routePoints.value = listOf(point)
            } else {
                val dist = DistanceCalculator.calculateDistanceMeters(
                    last.latitude, last.longitude,
                    point.latitude, point.longitude
                )

                // Filter jitter if under minDistanceFilter (unless moving fast)
                if (dist >= minDistanceFilterMeters.value || speedKmh > 5f) {
                    val newDistance = _distanceMeters.value + dist
                    _distanceMeters.value = newDistance
                    lastRecordedPoint = point
                    _routePoints.value = _routePoints.value + point

                    // Recalculate average speed
                    val avg = DistanceCalculator.calculateAverageSpeedKmh(
                        newDistance,
                        _elapsedTimeSeconds.value
                    )
                    _avgSpeedKmh.value = avg
                }
            }
        }
    }

    fun onGpsLost() {
        _isGpsFixAcquired.value = false
        _currentSpeedKmh.value = 0f
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _tripState.value == TripState.RECORDING) {
                delay(1000L)
                val newElapsed = _elapsedTimeSeconds.value + 1L
                _elapsedTimeSeconds.value = newElapsed
                _avgSpeedKmh.value = DistanceCalculator.calculateAverageSpeedKmh(
                    _distanceMeters.value,
                    newElapsed
                )
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
