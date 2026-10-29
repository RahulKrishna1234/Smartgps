package com.example.smartgpstracker.utils

import com.example.smartgpstracker.data.model.LocationPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates great-circle distance between two coordinates in meters using the Haversine formula.
     * Can run in standard JVM unit tests without Android OS mocking.
     */
    fun calculateDistanceMeters(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Double {
        if (startLat == endLat && startLon == endLon) {
            return 0.0
        }

        val dLat = Math.toRadians(endLat - startLat)
        val dLon = Math.toRadians(endLon - startLon)

        val lat1Rad = Math.toRadians(startLat)
        val lat2Rad = Math.toRadians(endLat)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates the cumulative distance of a route of points in meters.
     */
    fun calculateTotalRouteDistanceMeters(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            total += calculateDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        }
        return total
    }

    /**
     * Converts meters per second to kilometers per hour.
     */
    fun metersPerSecondToKmh(speedMps: Float): Float {
        if (speedMps <= 0f) return 0f
        return speedMps * 3.6f
    }

    /**
     * Converts kilometers per hour to miles per hour.
     */
    fun kmhToMph(kmh: Float): Float {
        if (kmh <= 0f) return 0f
        return kmh * 0.621371f
    }

    /**
     * Computes average speed in km/h from total distance in meters and elapsed time in seconds.
     */
    fun calculateAverageSpeedKmh(distanceMeters: Double, durationSeconds: Long): Float {
        if (durationSeconds <= 0L || distanceMeters <= 0.0) return 0f
        val distanceKm = distanceMeters / 1000.0
        val durationHours = durationSeconds.toDouble() / 3600.0
        return (distanceKm / durationHours).toFloat()
    }
}
