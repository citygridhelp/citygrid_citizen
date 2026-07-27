package com.example.potholereport.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripClarityHelpersTest {

    @Test
    fun remainingSeconds_scalesWithProgress() {
        val total = 600.0
        val halfDone = TripRouteProgress(
            traveledMeters = 500.0,
            remainingMeters = 500.0,
            totalMeters = 1_000.0,
            completionFraction = 0.5,
            offRouteMeters = 0.0,
        )
        val remaining = total * (1.0 - halfDone.completionFraction)
        assertEquals(300.0, remaining, 0.1)
    }

    @Test
    fun arrivalThreshold_usesNearbyRemainingDistance() {
        val nearEnd = TripRouteProgress(
            traveledMeters = 980.0,
            remainingMeters = 20.0,
            totalMeters = 1_000.0,
            completionFraction = 0.98,
            offRouteMeters = 2.0,
        )
        assertTrue(nearEnd.remainingMeters <= 25.0)
    }
}
