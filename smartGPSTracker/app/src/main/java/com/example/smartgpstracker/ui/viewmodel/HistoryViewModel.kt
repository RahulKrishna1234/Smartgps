package com.example.smartgpstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartgpstracker.GpsTrackerApp
import com.example.smartgpstracker.data.model.TripRecord
import com.example.smartgpstracker.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: TripRepository = GpsTrackerApp.instance.tripRepository
) : ViewModel() {

    val allTrips: StateFlow<List<TripRecord>> = repository.allTrips.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteTrip(trip: TripRecord) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }

    fun clearAllTrips() {
        viewModelScope.launch {
            repository.clearAllTrips()
        }
    }
}
