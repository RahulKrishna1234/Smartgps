package com.example.smartgpstracker

import android.app.Application
import com.example.smartgpstracker.data.db.AppDatabase
import com.example.smartgpstracker.data.repository.TripRepository
import org.osmdroid.config.Configuration

class GpsTrackerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var tripRepository: TripRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Configure osmdroid user agent for tile fetching
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        // Initialize Database and Repository
        database = AppDatabase.getInstance(this)
        tripRepository = TripRepository(database.tripDao())
    }

    companion object {
        lateinit var instance: GpsTrackerApp
            private set
    }
}
