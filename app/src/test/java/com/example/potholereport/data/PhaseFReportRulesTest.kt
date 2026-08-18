package com.example.potholereport.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseFReportRulesTest {

    @Test
    fun duplicateRadius_isThirtyFeetFieldTune() {
        // 30 ft = 9.144 m
        assertEquals(9.144, PotholeDuplicateGuard.SAME_POTHOLE_RADIUS_METERS, 0.01)
        assertEquals(9, PotholeDuplicateGuard.userFacingRadiusMeters())
    }

    @Test
    fun effectiveRadius_expandsWithPoorGpsButCaps() {
        assertEquals(9.144, PotholeDuplicateGuard.effectiveRadiusMeters(null), 0.01)
        assertEquals(9.144, PotholeDuplicateGuard.effectiveRadiusMeters(6f), 0.01)
        // 12 m accuracy → 15 m effective, still under ~18.3 m cap
        assertEquals(15.0, PotholeDuplicateGuard.effectiveRadiusMeters(12f), 0.01)
        assertEquals(
            PotholeDuplicateGuard.MAX_DUPLICATE_RADIUS_METERS,
            PotholeDuplicateGuard.effectiveRadiusMeters(200f),
            0.01,
        )
    }

    @Test
    fun findActiveDuplicate_blocksWithinRadius() {
        val existing = PersistedPotholeReport(
            id = 1L,
            cityKey = "bengaluru",
            createdAtMs = 1L,
            photoPath = "",
            latitude = 12.9716,
            longitude = 77.5946,
            reporterUserId = "PW-1",
            status = PotholeReportStatus.OPEN,
        )
        // ~7 m north — inside 30 ft (9.144 m)
        val nearLat = 12.9716 + (7.0 / 111_320.0)
        val hit = PotholeDuplicateGuard.findActiveDuplicate(
            reports = listOf(existing),
            reporterUserId = "PW-1",
            cityKey = "bengaluru",
            latitude = nearLat,
            longitude = 77.5946,
        )
        assertNotNull(hit)

        // ~15 m north — outside 30 ft base radius
        val farLat = 12.9716 + (15.0 / 111_320.0)
        val miss = PotholeDuplicateGuard.findActiveDuplicate(
            reports = listOf(existing),
            reporterUserId = "PW-1",
            cityKey = "bengaluru",
            latitude = farLat,
            longitude = 77.5946,
        )
        assertNull(miss)
    }

    @Test
    fun fullWidth_fromCodeAndDriverLabel() {
        assertEquals(PotholePosition.FULL_WIDTH, PotholePosition.fromCode("F"))
        assertEquals(PotholePosition.FULL_WIDTH, PotholePosition.fromCode("full width"))
        assertEquals(
            "Full road",
            resolveDriverLaneLabel(
                PotholePosition.FULL_WIDTH,
                reportBearingDeg = null,
                trafficBearingDeg = null,
                driverBearingDeg = null,
            ),
        )
        assertEquals("across the road", formatTripPotholeSide("Full width"))
        assertEquals("across the road", formatTripPotholeSide("Full road"))
        assertEquals("across the road", formatTripPotholeSide("LMR"))
        assertEquals(PotholePosition.FULL_WIDTH, PotholePosition.fromCode("full road pothole"))
        assertEquals(PotholePosition.FULL_WIDTH, PotholePosition.fromCode("LMR"))
        assertEquals("between the left and middle of the road", formatTripPotholeSide("LM"))
        assertEquals("between the middle and right of the road", formatTripPotholeSide("MR"))
    }

    @Test
    fun fullWidth_laneCoversEntireFrame() {
        val range = PotholePosition.FULL_WIDTH.laneRange
        assertEquals(0f, range.start, 0.001f)
        assertEquals(1f, range.endInclusive, 0.001f)
        assertTrue(range.endInclusive - range.start > 0.9f)
    }
}
