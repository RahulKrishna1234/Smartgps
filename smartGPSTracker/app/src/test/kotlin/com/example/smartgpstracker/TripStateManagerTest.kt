package com.example.smartgpstracker

import com.example.smartgpstracker.data.model.TripState
import com.example.smartgpstracker.service.TripTrackingManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TripStateManagerTest {

    @Before
    fun setup() {
        TripTrackingManager.resetTrip()
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(TripState.IDLE, TripTrackingManager.tripState.value)
        assertEquals(0.0, TripTrackingManager.distanceMeters.value, 0.001)
        assertEquals(0L, TripTrackingManager.elapsedTimeSeconds.value)
    }

    @Test
    fun startTrip_transitionsToRecording() {
        TripTrackingManager.startTrip()
        assertEquals(TripState.RECORDING, TripTrackingManager.tripState.value)
    }

    @Test
    fun pauseTrip_transitionsToPaused() {
        TripTrackingManager.startTrip()
        TripTrackingManager.pauseTrip()
        assertEquals(TripState.PAUSED, TripTrackingManager.tripState.value)
    }

    @Test
    fun resumeTrip_transitionsBackToRecording() {
        TripTrackingManager.startTrip()
        TripTrackingManager.pauseTrip()
        TripTrackingManager.resumeTrip()
        assertEquals(TripState.RECORDING, TripTrackingManager.tripState.value)
    }

    @Test
    fun pauseTrip_whenIdle_remainsIdle() {
        TripTrackingManager.pauseTrip()
        assertEquals(TripState.IDLE, TripTrackingManager.tripState.value)
    }

    @Test
    fun stopTrip_whenRecording_transitionsToStoppedAndReturnsRecord() {
        TripTrackingManager.startTrip()
        val record = TripTrackingManager.stopTrip("Morning Run")

        assertEquals(TripState.STOPPED, TripTrackingManager.tripState.value)
        assertNotNull(record)
        assertEquals("Morning Run", record?.title)
    }

    @Test
    fun stopTrip_whenIdle_returnsNull() {
        val record = TripTrackingManager.stopTrip("Idle Stop")
        assertNull(record)
        assertEquals(TripState.IDLE, TripTrackingManager.tripState.value)
    }

    @Test
    fun resetTrip_clearsAllMetrics() {
        TripTrackingManager.startTrip()
        TripTrackingManager.stopTrip("Test Trip")
        TripTrackingManager.resetTrip()

        assertEquals(TripState.IDLE, TripTrackingManager.tripState.value)
        assertEquals(0.0, TripTrackingManager.distanceMeters.value, 0.001)
        assertEquals(0L, TripTrackingManager.elapsedTimeSeconds.value)
        assertEquals(0f, TripTrackingManager.currentSpeedKmh.value, 0.001f)
        assertEquals(0f, TripTrackingManager.maxSpeedKmh.value, 0.001f)
        assertEquals(0f, TripTrackingManager.avgSpeedKmh.value, 0.001f)
        assertEquals(0, TripTrackingManager.routePoints.value.size)
    }
}
