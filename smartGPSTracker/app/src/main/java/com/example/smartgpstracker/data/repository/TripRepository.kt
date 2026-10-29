package com.example.smartgpstracker.data.repository

import com.example.smartgpstracker.data.db.TripDao
import com.example.smartgpstracker.data.model.TripRecord
import kotlinx.coroutines.flow.Flow

class TripRepository(private val tripDao: TripDao) {
    val allTrips: Flow<List<TripRecord>> = tripDao.getAllTrips()

    suspend fun getTripById(id: Long): TripRecord? {
        return tripDao.getTripById(id)
    }

    suspend fun saveTrip(trip: TripRecord): Long {
        return tripDao.insertTrip(trip)
    }

    suspend fun deleteTrip(trip: TripRecord) {
        tripDao.deleteTrip(trip)
    }

    suspend fun deleteTripById(id: Long) {
        tripDao.deleteTripById(id)
    }

    suspend fun clearAllTrips() {
        tripDao.deleteAllTrips()
    }
}
