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
        assertEquals("across the road", formatTripPotholeSide("LMR"))
        assertEquals("between the left and middle of the road", formatTripPotholeSide("LM"))
        assertEquals("between the middle and right of the road", formatTripPotholeSide("MR"))
        assertNull(formatTripPotholeSide(null))
        assertNull(formatTripPotholeSide("  "))
    }

    @Test
    fun formatTripPotholeSideLetter_mapsCompactCodes() {
        assertEquals("L", formatTripPotholeSideLetter("Left"))
        assertEquals("M", formatTripPotholeSideLetter("Middle"))
        assertEquals("R", formatTripPotholeSideLetter("R"))
        assertEquals("LM", formatTripPotholeSideLetter("LM"))
        assertEquals("MR", formatTripPotholeSideLetter("Middle+Right"))
        assertEquals("LMR", formatTripPotholeSideLetter("Full road"))
        assertEquals("LMR", formatTripPotholeSideLetter("F"))
        assertNull(formatTripPotholeSideLetter(null))
    }

    @Test
    fun tripPotholeAlertInk_isReadableOnWhite() {
        // Pale fill accents must not be used as body text ink.
        assertTrue(tripPotholeAlertInkArgb(PotholeSeverity.MINOR) != tripPotholeAlertAccentArgb(PotholeSeverity.MINOR))
        assertEquals(0xFFDC2626.toInt(), tripPotholeAlertAccentArgb(PotholeSeverity.CRITICAL))
        assertEquals(0xFFB91C1C.toInt(), tripPotholeAlertInkArgb(PotholeSeverity.SEVERE))
        assertEquals(0xFFB91C1C.toInt(), tripPotholeAlertInkArgb(PotholeSeverity.CRITICAL))
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
        assertTrue(line.contains("dangerous crater"))
        assertTrue(line.contains("80 m"))
        assertTrue(line.contains("on your left"))
        assertTrue(!line.contains("CRITICAL pothole"))
    }

    @Test
    fun driverPhrases_areHumanReadable() {
        assertEquals("Caution", PotholeSeverity.MINOR.driverShort)
        assertTrue(PotholeSeverity.SEVERE.driverPhrase.contains("slow down"))
        assertTrue(PotholeSeverity.CRITICAL.driverPhrase.contains("divert"))
    }

    @Test
    fun notifyDistance_isWithinDriverReactionRange() {
        assertEquals(150.0, TRIP_POTHOLE_NOTIFY_WITHIN_M, 0.01)
    }
}
