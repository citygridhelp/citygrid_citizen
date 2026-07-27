package com.example.potholereport.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripDeviceHeadingTest {

    @Test
    fun fuseNavHeading_usesCompassWhenStationary() {
        val fused = fuseNavHeadingDeg(
            deviceHeadingDeg = 95f,
            gpsBearingDeg = 10f,
            gpsSpeedMps = 0.2f,
        )
        assertEquals(95f, fused!!, 0.1f)
    }

    @Test
    fun fuseNavHeading_usesGpsCourseWhenMoving() {
        val fused = fuseNavHeadingDeg(
            deviceHeadingDeg = 95f,
            gpsBearingDeg = 40f,
            gpsSpeedMps = 8f,
        )
        assertEquals(40f, fused!!, 0.1f)
    }

    @Test
    fun fuseNavHeading_fallsBackToGpsBearingWithoutCompass() {
        val fused = fuseNavHeadingDeg(
            deviceHeadingDeg = null,
            gpsBearingDeg = 200f,
            gpsSpeedMps = null,
        )
        assertEquals(200f, fused!!, 0.1f)
    }

    @Test
    fun fuseNavHeading_nullWithoutSources() {
        assertNull(fuseNavHeadingDeg(null, null, null))
    }

    @Test
    fun normalizeHeadingDelta_wrapsAround() {
        assertEquals(10f, normalizeHeadingDelta(10f), 0.01f)
        assertEquals(-20f, normalizeHeadingDelta(340f), 0.01f)
    }
}
