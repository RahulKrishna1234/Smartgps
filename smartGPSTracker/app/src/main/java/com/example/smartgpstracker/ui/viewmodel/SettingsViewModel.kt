package com.example.smartgpstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.smartgpstracker.service.TripTrackingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {

    private val _isImperial = MutableStateFlow(TripTrackingManager.isImperial.value)
    val isImperial: StateFlow<Boolean> = _isImperial.asStateFlow()

    private val _gpsIntervalMillis = MutableStateFlow(TripTrackingManager.gpsIntervalMillis.value)
    val gpsIntervalMillis: StateFlow<Long> = _gpsIntervalMillis.asStateFlow()

    private val _minDistanceFilter = MutableStateFlow(TripTrackingManager.minDistanceFilterMeters.value)
    val minDistanceFilter: StateFlow<Float> = _minDistanceFilter.asStateFlow()

    fun setImperial(imperial: Boolean) {
        _isImperial.value = imperial
        TripTrackingManager.isImperial.value = imperial
    }

    fun setGpsInterval(intervalMillis: Long) {
        _gpsIntervalMillis.value = intervalMillis
        TripTrackingManager.gpsIntervalMillis.value = intervalMillis
    }

    fun setMinDistanceFilter(distanceMeters: Float) {
        _minDistanceFilter.value = distanceMeters
        TripTrackingManager.minDistanceFilterMeters.value = distanceMeters
    }
}
