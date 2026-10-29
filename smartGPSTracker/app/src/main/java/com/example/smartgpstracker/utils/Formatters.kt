package com.example.smartgpstracker.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    fun formatDistance(distanceMeters: Double, isImperial: Boolean = false): String {
        if (isImperial) {
            val miles = distanceMeters * 0.000621371
            return if (miles < 0.1) {
                val feet = distanceMeters * 3.28084
                String.format(Locale.getDefault(), "%.0f ft", feet)
            } else {
                String.format(Locale.getDefault(), "%.2f mi", miles)
            }
        } else {
            return if (distanceMeters < 1000.0) {
                String.format(Locale.getDefault(), "%.0f m", distanceMeters)
            } else {
                val km = distanceMeters / 1000.0
                String.format(Locale.getDefault(), "%.2f km", km)
            }
        }
    }

    fun formatSpeed(speedKmh: Float, isImperial: Boolean = false): String {
        val safeSpeed = if (speedKmh < 0f || speedKmh.isNaN()) 0f else speedKmh
        return if (isImperial) {
            val mph = DistanceCalculator.kmhToMph(safeSpeed)
            String.format(Locale.getDefault(), "%.1f mph", mph)
        } else {
            String.format(Locale.getDefault(), "%.1f km/h", safeSpeed)
        }
    }

    fun formatDuration(totalSeconds: Long): String {
        val seconds = (totalSeconds % 60).coerceAtLeast(0)
        val minutes = ((totalSeconds / 60) % 60).coerceAtLeast(0)
        val hours = (totalSeconds / 3600).coerceAtLeast(0)
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatCoordinate(latitude: Double, longitude: Double): String {
        val latDir = if (latitude >= 0) "N" else "S"
        val lonDir = if (longitude >= 0) "E" else "W"
        return String.format(
            Locale.getDefault(),
            "%.5f° %s, %.5f° %s",
            Math.abs(latitude), latDir,
            Math.abs(longitude), lonDir
        )
    }

    fun formatAltitude(altitudeMeters: Double, isImperial: Boolean = false): String {
        return if (isImperial) {
            val feet = altitudeMeters * 3.28084
            String.format(Locale.getDefault(), "%.1f ft", feet)
        } else {
            String.format(Locale.getDefault(), "%.1f m", altitudeMeters)
        }
    }

    fun formatAccuracy(accuracyMeters: Float, isImperial: Boolean = false): String {
        if (accuracyMeters <= 0f) return "--"
        return if (isImperial) {
            val feet = accuracyMeters * 3.28084f
            String.format(Locale.getDefault(), "±%.1f ft", feet)
        } else {
            String.format(Locale.getDefault(), "±%.1f m", accuracyMeters)
        }
    }

    fun formatDateTime(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }
}
