package com.example.potholereport.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsrmRouteAlternatesTest {

    private fun route(
        points: List<Pair<Double, Double>>,
        distanceM: Double,
        durationSec: Double = distanceM / 10.0,
        startSnap: Double = 10.0,
        endSnap: Double = 10.0,
        steps: List<OsrmRouteStep> = emptyList(),
    ) = OsrmRouteResult(
        points = points,
        durationSeconds = durationSec,
        distanceMeters = distanceM,
        steps = steps,
        startSnapMeters = startSnap,
        endSnapMeters = endSnap,
    )

    private val start = 12.90 to 77.66
    private val end = 12.98 to 77.68

    @Test
    fun diversionViaPoints_offsetsLeftAndRightOfPrimary() {
        val primary = route(
            points = listOf(
                12.920 to 77.655,
                12.918 to 77.658,
                12.916 to 77.661,
                12.914 to 77.664,
                12.912 to 77.667,
            ),
            distanceM = 1_300.0,
        )
        val vias = OsrmRouteClient.diversionViaPoints(primary)
        assertTrue(vias.size >= 4)
        val mid = primary.points[primary.points.size / 2]
        assertTrue(
            vias.any { via ->
                OsrmRouteClient.routesAreVisiblyDistinct(
                    route(
                        listOf(primary.points.first(), via, primary.points.last()),
                        1_300.0,
                    ),
                    primary,
                ) || kotlin.math.abs(via.second - mid.second) > 0.0008
            },
        )
    }

    @Test
    fun routesAreVisiblyDistinct_rejectsNearlySameMidpoint() {
        val a = route(
            listOf(12.92 to 77.65, 12.915 to 77.66, 12.91 to 77.67),
            1_200.0,
        )
        val b = route(
            listOf(12.92 to 77.65, 12.9151 to 77.6601, 12.91 to 77.67),
            1_220.0,
        )
        assertFalse(OsrmRouteClient.routesAreVisiblyDistinct(a, b))
    }

    @Test
    fun routesAreVisiblyDistinct_acceptsCloseParallelOnShortTrip() {
        val a = route(
            listOf(12.92 to 77.65, 12.915 to 77.66, 12.91 to 77.67),
            1_200.0,
        )
        val b = route(
            listOf(12.92 to 77.65, 12.915 to 77.66055, 12.91 to 77.67),
            1_280.0,
        )
        assertTrue(OsrmRouteClient.routesAreVisiblyDistinct(a, b))
    }

    @Test
    fun routesAreVisiblyDistinct_acceptsParallelCorridor() {
        val a = route(
            listOf(12.92 to 77.65, 12.915 to 77.66, 12.91 to 77.67),
            1_200.0,
        )
        val b = route(
            listOf(12.92 to 77.65, 12.915 to 77.6625, 12.91 to 77.67),
            1_350.0,
        )
        assertTrue(OsrmRouteClient.routesAreVisiblyDistinct(a, b))
    }

    @Test
    fun diversionViaPoints_longTripUsesCorridorScaleOffsets() {
        val primary = route(
            points = (0..20).map { i ->
                (12.90 + i * 0.004) to (77.66 + i * 0.001)
            },
            distanceM = 14_000.0,
        )
        val vias = OsrmRouteClient.diversionViaPoints(primary)
        assertTrue(vias.size >= 8)
        val mid = primary.points[primary.points.size / 2]
        val farthest = vias.maxOf { via ->
            val dLat = (via.first - mid.first) * 111_320.0
            val dLon = (via.second - mid.second) * 111_320.0 *
                kotlin.math.cos(Math.toRadians(mid.first))
            kotlin.math.sqrt(dLat * dLat + dLon * dLon)
        }
        assertTrue(farthest > 900.0)
    }

    @Test
    fun pickDiverseRoutes_prefersFartherAlternate() {
        val primary = route(
            listOf(12.90 to 77.66, 12.94 to 77.67, 12.98 to 77.68),
            12_000.0,
        )
        val nearParallel = route(
            listOf(12.90 to 77.66, 12.94 to 77.6708, 12.98 to 77.68),
            12_200.0,
        )
        val corridor = route(
            listOf(12.90 to 77.66, 12.94 to 77.69, 12.98 to 77.68),
            12_800.0,
        )
        val picked = OsrmRouteClient.pickDiverseRoutes(
            listOf(primary, nearParallel, corridor),
            maxCount = 2,
        )
        assertEquals(2, picked.size)
        assertTrue(picked.any { it.distanceMeters == 12_000.0 })
        assertTrue(picked.any { it.distanceMeters == 12_800.0 })
    }

    @Test
    fun passesRouteQuality_rejectsDisconnectedStart() {
        val primary = route(
            listOf(start, 12.94 to 77.67, end),
            12_000.0,
            startSnap = 20.0,
        )
        val bad = route(
            listOf(12.9027 to 77.66, 12.94 to 77.69, end),
            12_500.0,
            startSnap = 20.0,
        )
        assertFalse(
            OsrmRouteClient.passesRouteQuality(
                bad, primary,
                start.first, start.second, end.first, end.second,
            ),
        )
    }

    @Test
    fun passesRouteQuality_keepsSlightlySlowerConnectedAlt() {
        val primary = route(
            listOf(start, 12.94 to 77.67, end),
            12_000.0,
            durationSec = 2_400.0,
        )
        val slower = route(
            listOf(start, 12.94 to 77.69, end),
            12_800.0,
            durationSec = 2_880.0,
        )
        assertTrue(
            OsrmRouteClient.passesRouteQuality(
                slower, primary,
                start.first, start.second, end.first, end.second,
                geometryStrict = false,
            ),
        )
    }

    @Test
    fun passesRouteQuality_rejectsHugeTimeWaste() {
        val primary = route(
            listOf(start, 12.94 to 77.67, end),
            17_100.0,
            durationSec = 24 * 60.0,
        )
        val waste = route(
            listOf(start, 12.94 to 77.62, 12.96 to 77.60, end),
            24_300.0,
            durationSec = 49 * 60.0,
        )
        assertFalse(
            OsrmRouteClient.passesRouteQuality(
                waste, primary,
                start.first, start.second, end.first, end.second,
                geometryStrict = false,
            ),
        )
    }

    @Test
    fun hasPointlessSpur_detectsTrueOutAndBack() {
        // Travel far then return almost to the same point (ratio ≫ 5).
        val spur = route(
            points = listOf(
                12.9000 to 77.6600,
                12.9020 to 77.6600,
                12.9040 to 77.6600,
                12.9060 to 77.6600,
                12.9080 to 77.6600,
                12.9060 to 77.66005,
                12.9040 to 77.6601,
                12.9020 to 77.6601,
                12.9002 to 77.66005,
                12.9100 to 77.6700,
                12.9800 to 77.6800,
            ),
            distanceM = 8_000.0,
        )
        assertTrue(OsrmRouteClient.hasPointlessSpur(spur))
    }

    @Test
    fun hasPointlessSpur_cleanCorridorIsOk() {
        val clean = route(
            points = (0..15).map { i ->
                (12.90 + i * 0.005) to (77.66 + i * 0.0015)
            },
            distanceM = 10_000.0,
        )
        assertFalse(OsrmRouteClient.hasPointlessSpur(clean))
    }

    @Test
    fun hasGrossWrongWayDetour_rejectsFarSideBulge() {
        val primary = route(
            points = (0..12).map { i ->
                (12.90 + i * 0.006) to (77.66 + i * 0.001)
            },
            distanceM = 17_000.0,
        )
        // Multi-km west of the start–end chord.
        val wrongWay = route(
            points = listOf(
                start,
                12.91 to 77.64,
                12.93 to 77.60,
                12.95 to 77.58,
                12.97 to 77.62,
                end,
            ),
            distanceM = 22_000.0,
        )
        assertTrue(OsrmRouteClient.hasGrossWrongWayDetour(wrongWay, primary))
    }

    @Test
    fun hasGrossWrongWayDetour_allowsParallelCorridor() {
        val primary = route(
            points = (0..12).map { i ->
                (12.90 + i * 0.006) to (77.66 + i * 0.001)
            },
            distanceM = 17_000.0,
        )
        // Slightly east parallel — still a useful Google-style alt.
        val parallel = route(
            points = (0..12).map { i ->
                (12.90 + i * 0.006) to (77.665 + i * 0.001)
            },
            distanceM = 17_400.0,
        )
        assertFalse(OsrmRouteClient.hasGrossWrongWayDetour(parallel, primary))
    }

    @Test
    fun countSharpReversals_countsUTurnSteps() {
        val withUTurn = route(
            listOf(start, 12.94 to 77.67, end),
            12_000.0,
            steps = listOf(
                OsrmRouteStep("Make a U-turn", 100.0, 20.0, "turn", "uturn", ""),
                OsrmRouteStep("Turn left", 200.0, 40.0, "turn", "left", "Main"),
            ),
        )
        assertTrue(OsrmRouteClient.countSharpReversals(withUTurn) >= 1)
    }

    @Test
    fun isUsefulAlternateCorridor_rejectsLongDetourWithSmallGap() {
        val primary = route(
            listOf(start, 12.94 to 77.67, end),
            12_000.0,
        )
        val irregular = route(
            listOf(start, 12.941 to 77.6705, end),
            13_100.0,
        )
        assertFalse(OsrmRouteClient.isUsefulAlternateCorridor(irregular, primary))
    }

    @Test
    fun isUsefulAlternateCorridor_keepsParallelCorridor() {
        val primary = route(
            listOf(start, 12.94 to 77.67, end),
            12_000.0,
        )
        val parallel = route(
            listOf(start, 12.94 to 77.69, end),
            12_600.0,
        )
        assertTrue(OsrmRouteClient.isUsefulAlternateCorridor(parallel, primary))
    }

    @Test
    fun hasDeadEndReversal_rejectsExtraShortUTurns() {
        val primary = route(
            listOf(start, 12.94 to 77.67, end),
            12_000.0,
        )
        val deadEnd = route(
            listOf(start, 12.94 to 77.6702, end),
            14_000.0,
            steps = listOf(
                OsrmRouteStep("Make a U-turn", 80.0, 15.0, "turn", "uturn", ""),
                OsrmRouteStep("Make a U-turn", 90.0, 18.0, "turn", "uturn", ""),
                OsrmRouteStep("Continue", 200.0, 40.0, "continue", "", "Main"),
            ),
        )
        assertTrue(OsrmRouteClient.hasDeadEndReversal(deadEnd, primary))
    }

    @Test
    fun passesRouteQuality_rejectsSpurEvenWhenLightFilterRequested() {
        val primary = route(
            points = (0..15).map { i ->
                (12.90 + i * 0.005) to (77.66 + i * 0.0015)
            },
            distanceM = 10_000.0,
            durationSec = 1_800.0,
        )
        val spur = route(
            points = listOf(
                12.9000 to 77.6600,
                12.9020 to 77.6600,
                12.9040 to 77.6600,
                12.9060 to 77.6600,
                12.9080 to 77.6600,
                12.9060 to 77.66005,
                12.9040 to 77.6601,
                12.9020 to 77.6601,
                12.9002 to 77.66005,
                12.9100 to 77.6700,
                12.9800 to 77.6800,
            ),
            distanceM = 11_000.0,
            durationSec = 2_000.0,
        )
        assertFalse(
            OsrmRouteClient.passesRouteQuality(
                spur, primary,
                start.first, start.second, end.first, end.second,
                geometryStrict = false,
            ),
        )
    }
}
