package com.example.potholereport.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OsrmBikeFallbackTest {

    private fun route(
        distanceM: Double,
        startSnapM: Double = 5.0,
        endSnapM: Double = 5.0,
    ) = OsrmRouteResult(
        points = listOf(12.91 to 77.66, 12.92 to 77.67),
        durationSeconds = distanceM / 10.0,
        distanceMeters = distanceM,
        startSnapMeters = startSnapM,
        endSnapMeters = endSnapM,
    )

    @Test
    fun pickFeasibleBikeRoute_keepsReasonableCyclingRoute() {
        val bike = route(2_500.0, startSnapM = 10.0)
        val car = route(2_700.0, startSnapM = 8.0)
        val picked = OsrmRouteClient.pickFeasibleBikeRoute(bike, car)
        assertSame(bike, picked)
    }

    @Test
    fun pickFeasibleBikeRoute_fallsBackWhenCyclingIsLongDetour() {
        val bike = route(8_000.0, startSnapM = 15.0)
        val car = route(2_500.0, startSnapM = 10.0)
        val picked = OsrmRouteClient.pickFeasibleBikeRoute(bike, car)!!
        assertEquals(2_500.0, picked.distanceMeters, 0.1)
        // Bike ETA (~16 km/h) should be slower than a car-duration copy of the same geometry.
        assertTrue(picked.durationSeconds > car.durationSeconds)
    }

    @Test
    fun pickFeasibleBikeRoute_fallsBackWhenStartSnapIsUnreachable() {
        val bike = route(3_000.0, startSnapM = 180.0)
        val car = route(2_800.0, startSnapM = 12.0)
        val picked = OsrmRouteClient.pickFeasibleBikeRoute(bike, car)!!
        assertEquals(2_800.0, picked.distanceMeters, 0.1)
    }

    @Test
    fun pickFeasibleBikeRoute_usesCarWhenBikeMissing() {
        val car = route(2_000.0)
        val picked = OsrmRouteClient.pickFeasibleBikeRoute(null, car)!!
        assertEquals(2_000.0, picked.distanceMeters, 0.1)
    }
}
