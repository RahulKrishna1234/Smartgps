package com.example.smartgpstracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgpstracker.GpsTrackerApp
import com.example.smartgpstracker.data.model.TripRecord
import com.example.smartgpstracker.data.model.TripState
import com.example.smartgpstracker.data.repository.TripRepository
import com.example.smartgpstracker.service.LocationTrackingService
import com.example.smartgpstracker.service.TripTrackingManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TrackerViewModel(
    private val repository: TripRepository = GpsTrackerApp.instance.tripRepository
) : ViewModel() {

    val tripState: StateFlow<TripState> = TripTrackingManager.tripState
    val currentLocation = TripTrackingManager.currentLocation
    val isGpsFixAcquired = TripTrackingManager.isGpsFixAcquired
    val routePoints = TripTrackingManager.routePoints
    val distanceMeters = TripTrackingManager.distanceMeters
    val elapsedTimeSeconds = TripTrackingManager.elapsedTimeSeconds
    val currentSpeedKmh = TripTrackingManager.currentSpeedKmh
    val maxSpeedKmh = TripTrackingManager.maxSpeedKmh
    val avgSpeedKmh = TripTrackingManager.avgSpeedKmh
    val isImperial = TripTrackingManager.isImperial

    private val _tripSavedEvent = MutableSharedFlow<TripRecord>()
    val tripSavedEvent: SharedFlow<TripRecord> = _tripSavedEvent.asSharedFlow()

    fun startTrip(context: Context) {
        LocationTrackingService.startService(context)
    }

    fun pauseTrip(context: Context) {
        LocationTrackingService.pauseService(context)
    }

    fun resumeTrip(context: Context) {
        LocationTrackingService.resumeService(context)
    }

    fun stopAndSaveTrip(context: Context, title: String) {
        val record = TripTrackingManager.stopTrip(title)
        LocationTrackingService.stopService(context)
        if (record != null && (record.routePoints.isNotEmpty() || record.totalDistanceMeters > 0)) {
            viewModelScope.launch {
                repository.saveTrip(record)
                _tripSavedEvent.emit(record)
                TripTrackingManager.resetTrip()
            }
        } else {
            TripTrackingManager.resetTrip()
        }
    }

    fun discardTrip(context: Context) {
        LocationTrackingService.stopService(context)
        TripTrackingManager.resetTrip()
    }
}
