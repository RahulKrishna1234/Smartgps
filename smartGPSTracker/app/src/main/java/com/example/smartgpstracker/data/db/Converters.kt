package com.example.smartgpstracker.data.db

import androidx.room.TypeConverter
import com.example.smartgpstracker.data.model.LocationPoint
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromLocationPointList(points: List<LocationPoint>?): String {
        if (points.isNullOrEmpty()) return "[]"
        val jsonArray = JSONArray()
        for (point in points) {
            val obj = JSONObject().apply {
                put("lat", point.latitude)
                put("lng", point.longitude)
                put("alt", point.altitude)
                put("spd", point.speedMps.toDouble())
                put("acc", point.accuracy.toDouble())
                put("ts", point.timestampMillis)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toLocationPointList(data: String?): List<LocationPoint> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<LocationPoint>()
        try {
            val jsonArray = JSONArray(data)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LocationPoint(
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", 0.0),
                        altitude = obj.optDouble("alt", 0.0),
                        speedMps = obj.optDouble("spd", 0.0).toFloat(),
                        accuracy = obj.optDouble("acc", 0.0).toFloat(),
                        timestampMillis = obj.optLong("ts", 0L)
                    )
                )
            }
        } catch (_: Exception) {
            // Gracefully return parsed points
        }
        return list
    }
}
