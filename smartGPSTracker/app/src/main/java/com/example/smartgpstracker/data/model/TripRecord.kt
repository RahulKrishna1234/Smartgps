package com.example.smartgpstracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val durationSeconds: Long = 0L,
    val totalDistanceMeters: Double = 0.0,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val routePoints: List<LocationPoint> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
)
