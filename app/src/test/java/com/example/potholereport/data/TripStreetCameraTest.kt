package com.example.potholereport.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripStreetCameraTest {

    private fun kmhToMps(kmh: Float): Float = kmh / 3.6f

    @Test
    fun zoomForSpeed_slowBandWhenStoppedOrUnder30() {
        assertEquals(TripStreetCamera.ZOOM_SLOW, TripStreetCamera.zoomForSpeedMps(0f), 0.05)
        assertEquals(TripStreetCamera.ZOOM_SLOW, TripStreetCamera.zoomForSpeedMps(null), 0.05)
        val at15 = TripStreetCamera.zoomForSpeedMps(kmhToMps(15f))
        assertTrue(at15 > TripStreetCamera.ZOOM_SLOW)
        assertTrue(at15 < TripStreetCamera.ZOOM_MODERATE)
    }

    @Test
    fun zoomForSpeed_moderateAt30AndRampsToFastAt70() {
        assertEquals(
            TripStreetCamera.ZOOM_MODERATE,
            TripStreetCamera.zoomForSpeedMps(kmhToMps(30f)),
            0.05,
        )
        val mid = TripStreetCamera.zoomForSpeedMps(kmhToMps(50f))
        assertTrue(mid > TripStreetCamera.ZOOM_MODERATE)
        assertTrue(mid < TripStreetCamera.ZOOM_FAST)
        assertEquals(
            TripStreetCamera.ZOOM_FAST,
            TripStreetCamera.zoomForSpeedMps(kmhToMps(70f)),
            0.05,
        )
        assertEquals(
            TripStreetCamera.ZOOM_FAST,
            TripStreetCamera.zoomForSpeedMps(kmhToMps(100f)),
            0.05,
        )
    }

    @Test
    fun zoomForSpeed_tightensAsSpeedIncreases() {
        val slow = TripStreetCamera.zoomForSpeedMps(kmhToMps(10f))
        val moderate = TripStreetCamera.zoomForSpeedMps(kmhToMps(40f))
        val fast = TripStreetCamera.zoomForSpeedMps(kmhToMps(80f))
        assertTrue(slow < moderate)
        assertTrue(moderate < fast)
    }

    @Test
    fun introZoomEase_startsAndEndsAtBounds() {
        assertEquals(0f, TripStreetCamera.introZoomEase(0f), 0.001f)
        assertEquals(1f, TripStreetCamera.introZoomEase(1f), 0.001f)
        val mid = TripStreetCamera.introZoomEase(0.5f)
        assertTrue(mid > 0.5f) // ease-out is ahead of linear at midpoint
        assertTrue(mid < 1f)
    }

    @Test
    fun routeStroke_staysModestNotHalfScreen() {
        assertEquals(
            TripStreetCamera.ROUTE_NAV_REMAINING_IDLE_PX,
            TripStreetCamera.remainingRouteStrokePx(0f),
            0.05f,
        )
        assertEquals(
            TripStreetCamera.ROUTE_NAV_REMAINING_MOVING_PX,
            TripStreetCamera.remainingRouteStrokePx(1f),
            0.05f,
        )
        assertTrue(TripStreetCamera.remainingRouteStrokePx(1f) < 40f)
        assertTrue(
            TripStreetCamera.remainingOutlineStrokePx(1f) >
                TripStreetCamera.remainingRouteStrokePx(1f),
        )
    }

    @Test
    fun routeStroke_thinsAtHighZoom() {
        val atSlow = TripStreetCamera.remainingRouteStrokePx(1f, TripStreetCamera.ZOOM_SLOW)
        val atFast = TripStreetCamera.remainingRouteStrokePx(1f, TripStreetCamera.ZOOM_FAST)
        assertTrue(atFast < atSlow)
        assertTrue(atFast > 4f)
    }

    @Test
    fun densifyLatLons_insertsPointsOnLongSegments() {
        val sparse = listOf(
            12.90 to 77.60,
            12.91 to 77.60, // ~1.1 km north
        )
        val dense = TripStreetCamera.densifyLatLons(sparse, maxSegmentM = 50.0)
        assertTrue(dense.size > sparse.size)
        assertEquals(sparse.first(), dense.first())
        assertEquals(sparse.last(), dense.last())
    }
}
