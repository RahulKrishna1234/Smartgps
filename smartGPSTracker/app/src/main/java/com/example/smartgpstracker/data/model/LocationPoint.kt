package com.example.smartgpstracker.data.model

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speedMps: Float = 0f,
    val accuracy: Float = 0f,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val speedKmh: Float
        get() = speedMps * 3.6f
}
