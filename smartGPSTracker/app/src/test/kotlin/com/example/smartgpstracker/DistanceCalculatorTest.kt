package com.example.smartgpstracker

import com.example.smartgpstracker.data.model.LocationPoint
import com.example.smartgpstracker.utils.DistanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceCalculatorTest {

    @Test
    fun calculateDistanceMeters_sameCoordinates_returnsZero() {
        val lat = 35.6895
        val lon = 139.6917
        val distance = DistanceCalculator.calculateDistanceMeters(lat, lon, lat, lon)
        assertEquals(0.0, distance, 0.0001)
    }

    @Test
    fun calculateDistanceMeters_knownLocations_returnsAccurateDistance() {
        // Distance between Tokyo Station (35.6812, 139.7671) and Shinjuku Station (35.6896, 139.7006)
        // is approximately 6.2 km (6200 meters)
        val tokyoLat = 35.6812
        val tokyoLon = 139.7671
        val shinjukuLat = 35.6896
        val shinjukuLon = 139.7006

        val distance = DistanceCalculator.calculateDistanceMeters(tokyoLat, tokyoLon, shinjukuLat, shinjukuLon)

        // Tolerance within ±100m of 6200m
        assertTrue("Distance $distance should be around 6200m", distance in 6100.0..6300.0)
    }

    @Test
    fun calculateDistanceMeters_parisToLondon_returnsAccurateDistance() {
        // Paris (48.8566, 2.3522) to London (51.5074, -0.1278) ~ 343 km
        val distance = DistanceCalculator.calculateDistanceMeters(48.8566, 2.3522, 51.5074, -0.1278)
        assertTrue("Distance $distance should be around 343km", distance in 340000.0..346000.0)
    }

    @Test
    fun calculateTotalRouteDistanceMeters_emptyOrSingle_returnsZero() {
        assertEquals(0.0, DistanceCalculator.calculateTotalRouteDistanceMeters(emptyList()), 0.0001)
        assertEquals(
            0.0,
            DistanceCalculator.calculateTotalRouteDistanceMeters(
                listOf(LocationPoint(35.0, 139.0))
            ),
            0.0001
        )
    }

    @Test
    fun calculateTotalRouteDistanceMeters_multiPointRoute_returnsSumOfSegments() {
        val p1 = LocationPoint(35.6812, 139.7671)
        val p2 = LocationPoint(35.6850, 139.7300)
        val p3 = LocationPoint(35.6896, 139.7006)

        val total = DistanceCalculator.calculateTotalRouteDistanceMeters(listOf(p1, p2, p3))
        val seg1 = DistanceCalculator.calculateDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        val seg2 = DistanceCalculator.calculateDistanceMeters(p2.latitude, p2.longitude, p3.latitude, p3.longitude)

        assertEquals(seg1 + seg2, total, 0.001)
    }

    @Test
    fun metersPerSecondToKmh_convertsCorrectly() {
        assertEquals(0f, DistanceCalculator.metersPerSecondToKmh(0f), 0.001f)
        assertEquals(36f, DistanceCalculator.metersPerSecondToKmh(10f), 0.001f)
        assertEquals(72f, DistanceCalculator.metersPerSecondToKmh(20f), 0.001f)
        assertEquals(0f, DistanceCalculator.metersPerSecondToKmh(-5f), 0.001f)
    }

    @Test
    fun calculateAverageSpeedKmh_convertsCorrectly() {
        // 10 km in 1 hour (3600 seconds) -> 10 km/h
        assertEquals(10f, DistanceCalculator.calculateAverageSpeedKmh(10000.0, 3600L), 0.01f)
        // 50 km in 30 minutes (1800 seconds) -> 100 km/h
        assertEquals(100f, DistanceCalculator.calculateAverageSpeedKmh(50000.0, 1800L), 0.01f)
        // Edge cases
        assertEquals(0f, DistanceCalculator.calculateAverageSpeedKmh(0.0, 100L), 0.01f)
        assertEquals(0f, DistanceCalculator.calculateAverageSpeedKmh(1000.0, 0L), 0.01f)
    }

    @Test
    fun kmhToMph_convertsCorrectly() {
        assertEquals(0f, DistanceCalculator.kmhToMph(0f), 0.001f)
        assertEquals(62.1371f, DistanceCalculator.kmhToMph(100f), 0.01f)
    }
}
