package com.example.potholereport.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPotholeAlertFeedbackTest {

    @Test
    fun formatTripPotholeSide_mapsLaneLabels() {
        assertEquals("on your left", formatTripPotholeSide("Left"))
        assertEquals("on your right", formatTripPotholeSide("R"))
        assertEquals("across the road", formatTripPotholeSide("Full width"))
        assertEquals("across the road", formatTripPotholeSide("F"))
        assertNull(formatTripPotholeSide(null))
        assertNull(formatTripPotholeSide("  "))
    }

    @Test
    fun formatTripPotholeAlertLine_includesSeverityDistanceAndSide() {
        val alert = TripPotholeAlert(
            report = PersistedPotholeReport(
                id = 42L,
                cityKey = "bengaluru",
                createdAtMs = 0L,
                photoPath = "",
                latitude = 12.97,
                longitude = 77.59,
                severity = PotholeSeverity.CRITICAL,
            ),
            distanceMeters = 80.4,
            sideLabel = "Left",
            alongRouteMeters = 100.0,
        )
        val line = formatTripPotholeAlertLine(alert)
        assertTrue(line.contains("CRITICAL"))
        assertTrue(line.contains("80 m"))
        assertTrue(line.contains("on your left"))
    }

    @Test
    fun notifyDistance_isWithinDriverReactionRange() {
        assertEquals(150.0, TRIP_POTHOLE_NOTIFY_WITHIN_M, 0.01)
    }
}
