package com.example.potholereport.data

import com.example.potholereport.data.remote.TripVehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripNavigationMatcherTest {

    private val straightRoute = listOf(
        0.0 to 0.0,
        0.0 to 0.01,
    )

    @Test
    fun routeProgress_projectsDriverOntoRoute() {
        val progress = TripNavigationMatcher.routeProgress(
            straightRoute,
            driverLat = 0.0,
            driverLon = 0.005,
        )

        assertNotNull(progress)
        progress!!
        assertEquals(0.5, progress.completionFraction, 0.02)
        assertEquals(progress.traveledMeters, progress.remainingMeters, 5.0)
        assertTrue(progress.offRouteMeters < 1.0)
    }

    @Test
    fun routeProgress_reportsPerpendicularDeviation() {
        val progress = TripNavigationMatcher.routeProgress(
            straightRoute,
            driverLat = 0.001,
            driverLon = 0.005,
        )

        assertNotNull(progress)
        assertTrue(progress!!.offRouteMeters in 105.0..117.0)
    }

    @Test
    fun routeStartCorrection_usesReliableMovingFix() {
        assertTrue(
            TripNavigationMatcher.routeStartNeedsCorrection(
                straightRoute,
                driverLat = 0.0,
                driverLon = 0.0003,
                gpsAccuracyM = 5f,
            ),
        )
        assertFalse(
            TripNavigationMatcher.routeStartNeedsCorrection(
                straightRoute,
                driverLat = 0.0,
                driverLon = 0.0001,
                gpsAccuracyM = 5f,
            ),
        )
    }

    @Test
    fun routeStartCorrection_ignoresNormalGpsJitterUnder25m() {
        // ~11 m east of route start (0.0001 lon ≈ 11 m near equator).
        assertFalse(
            TripNavigationMatcher.routeStartNeedsCorrection(
                straightRoute,
                driverLat = 0.0,
                driverLon = 0.0001,
                gpsAccuracyM = 8f,
            ),
        )
    }

    @Test
    fun routeStartCorrection_triggersWhenSettledFixIsClearlyWrong() {
        // ~33 m east of route start with good accuracy — intentional path rebuild.
        assertTrue(
            TripNavigationMatcher.routeStartNeedsCorrection(
                straightRoute,
                driverLat = 0.0,
                driverLon = 0.0003,
                gpsAccuracyM = 8f,
            ),
        )
    }

    @Test
    fun routeStartCorrection_raisesThresholdWhenAccuracyIsPoor() {
        // Same ~33 m offset, but poor accuracy (30 m) raises threshold to 45 m.
        assertFalse(
            TripNavigationMatcher.routeStartNeedsCorrection(
                straightRoute,
                driverLat = 0.0,
                driverLon = 0.0003,
                gpsAccuracyM = 30f,
            ),
        )
    }

    @Test
    fun offRouteReroute_baseThresholdIsSixtyMeters() {
        assertEquals(60.0, TripNavigationMatcher.OFF_ROUTE_REROUTE_M, 0.0)
        assertEquals(15.0, TripNavigationMatcher.START_CORRECTION_M, 0.0)
        assertEquals(400.0, TripNavigationMatcher.REJOIN_PREF_MAX_RETURN_M, 0.0)
        assertEquals(500.0, TripNavigationMatcher.REJOIN_HARD_MAX_RETURN_M, 0.0)
        assertEquals(90.0, TripNavigationMatcher.REJOIN_ETA_SLACK_SEC, 0.0)
    }

    @Test
    fun buildRejoinPolyline_returnsDriverToSnapThenRemaining() {
        val rejoin = TripNavigationMatcher.buildRejoinPolyline(
            straightRoute,
            driverLat = 0.001,
            driverLon = 0.005,
        )
        assertNotNull(rejoin)
        val (points, returnM) = rejoin!!
        assertTrue(points.size >= 2)
        assertEquals(0.001, points.first().first, 1e-6)
        assertEquals(0.005, points.first().second, 1e-6)
        assertTrue(returnM in 105.0..117.0)
    }

    @Test
    fun planDeviation_adoptsKnownAlternateWhenNearIt() {
        val dest = 0.0 to 0.01
        val active = listOf(0.0 to 0.0, dest)
        // Parallel mid-corridor that still finishes at the same destination.
        val alt = listOf(0.002 to 0.0, 0.002 to 0.008, dest)
        val progress = TripNavigationMatcher.routeProgress(active, 0.002, 0.005)!!
        assertTrue(progress.offRouteMeters > 60.0)
        val plan = TripNavigationMatcher.planDeviation(
            routePoints = active,
            knownAlternates = listOf(active, alt),
            driverLat = 0.002,
            driverLon = 0.005,
            progress = progress,
            speedMps = 8f,
            fullRerouteDurationSec = null,
            destLat = dest.first,
            destLon = dest.second,
        )
        assertEquals(TripDeviationAction.ADOPT_KNOWN_ALTERNATE, plan.action)
        assertEquals(1, plan.knownAlternateIndex)
    }

    @Test
    fun planDeviation_prefersAlternateOverRejoinWhenSoftOnAlt() {
        val dest = 0.0 to 0.01
        val active = listOf(0.0 to 0.0, dest)
        // Alternate only modestly closer than active — still must not show rejoin-to-old.
        val alt = listOf(0.0004 to 0.0, 0.0004 to 0.008, dest)
        val progress = TripNavigationMatcher.routeProgress(active, 0.0004, 0.005)!!
        assertTrue(progress.offRouteMeters > 20.0)
        val plan = TripNavigationMatcher.planDeviation(
            routePoints = active,
            knownAlternates = listOf(active, alt),
            driverLat = 0.0004,
            driverLon = 0.005,
            progress = progress,
            speedMps = 8f,
            fullRerouteDurationSec = null,
            destLat = dest.first,
            destLon = dest.second,
        )
        assertEquals(TripDeviationAction.ADOPT_KNOWN_ALTERNATE, plan.action)
        assertEquals(1, plan.knownAlternateIndex)
        assertTrue(plan.rejoinPoints.isEmpty())
    }

    @Test
    fun findPreferredKnownAlternate_rejectsCorridorThatMissesDestination() {
        val active = listOf(0.0 to 0.0, 0.0 to 0.01)
        val altElsewhere = listOf(0.002 to 0.0, 0.002 to 0.01) // ends ~222 m north of dest
        val idx = TripNavigationMatcher.findPreferredKnownAlternateIndex(
            knownAlternates = listOf(altElsewhere),
            activeRoute = active,
            driverLat = 0.002,
            driverLon = 0.005,
            activeOffRouteMeters = 220.0,
            destLat = 0.0,
            destLon = 0.01,
        )
        assertNull(idx)
    }

    @Test
    fun planDeviation_prefersRejoinWhenReturnIsShort() {
        // ~28 m north — GPS spur that does not qualify as a dest-reaching alternate.
        val nearLat = 28.0 / 111_320.0
        val progress = TripNavigationMatcher.routeProgress(straightRoute, nearLat, 0.005)!!
        val plan = TripNavigationMatcher.planDeviation(
            routePoints = straightRoute,
            knownAlternates = emptyList(),
            driverLat = nearLat,
            driverLon = 0.005,
            progress = progress,
            speedMps = 8f,
            fullRerouteDurationSec = null,
        )
        assertEquals(TripDeviationAction.SHOW_REJOIN, plan.action)
        assertTrue(plan.rejoinPoints.size >= 2)
        assertTrue(plan.rejoinReturnMeters < 70.0)
    }

    @Test
    fun planDeviation_fullReroutesWhenOnParallelCorridor() {
        // ~55 m north — typical parallel street; rematch best vehicle route (no rejoin dash).
        val parallelLat = 55.0 / 111_320.0
        val progress = TripNavigationMatcher.routeProgress(straightRoute, parallelLat, 0.005)!!
        val plan = TripNavigationMatcher.planDeviation(
            routePoints = straightRoute,
            knownAlternates = emptyList(),
            driverLat = parallelLat,
            driverLon = 0.005,
            progress = progress,
            speedMps = 8f,
            fullRerouteDurationSec = null,
        )
        assertEquals(TripDeviationAction.FULL_REROUTE, plan.action)
    }

    @Test
    fun planDeviation_rejoinsWhenOnAlternateThatMissesDestination() {
        val dest = 0.0 to 0.01
        val active = listOf(0.0 to 0.0, dest)
        // Parallel spur that does not finish at dest.
        val deadEnd = listOf(0.002 to 0.0, 0.002 to 0.006)
        val progress = TripNavigationMatcher.routeProgress(active, 0.002, 0.003)!!
        val plan = TripNavigationMatcher.planDeviation(
            routePoints = active,
            knownAlternates = listOf(active, deadEnd),
            driverLat = 0.002,
            driverLon = 0.003,
            progress = progress,
            speedMps = 8f,
            fullRerouteDurationSec = null,
            destLat = dest.first,
            destLon = dest.second,
        )
        assertEquals(TripDeviationAction.SHOW_REJOIN, plan.action)
        assertTrue(plan.rejoinPoints.isNotEmpty())
    }

    @Test
    fun planDeviation_suggestsUturnOnWrongNetworkForCar() {
        val progress = TripNavigationMatcher.routeProgress(straightRoute, 0.001, 0.005)!!
        val plan = TripNavigationMatcher.planDeviation(
            routePoints = straightRoute,
            knownAlternates = emptyList(),
            driverLat = 0.001,
            driverLon = 0.005,
            progress = progress,
            speedMps = 8f,
            fullRerouteDurationSec = null,
            destLat = 0.0,
            destLon = 0.01,
            vehicle = TripVehicle.CAR,
            vehicleNetworkSnapM = 120.0,
        )
        assertEquals(TripDeviationAction.SUGGEST_UTURN, plan.action)
    }

    @Test
    fun planDeviation_fullReroutesWhenReturnExceedsHardMax() {
        val progress = TripNavigationMatcher.routeProgress(straightRoute, 0.006, 0.005)!!
        val plan = TripNavigationMatcher.planDeviation(
            routePoints = straightRoute,
            knownAlternates = emptyList(),
            driverLat = 0.006,
            driverLon = 0.005,
            progress = progress,
            speedMps = 8f,
            fullRerouteDurationSec = 60.0,
        )
        assertEquals(TripDeviationAction.FULL_REROUTE, plan.action)
    }

    @Test
    fun pinRouteOriginToGps_prependsOnlyWithinBridgeLimit() {
        val route = listOf(
            12.97 to 77.59,
            12.971 to 77.591,
        )
        // ~55 m north of snapped start — must NOT cut across open land.
        val gpsLat = 12.97 + (55.0 / 111_320.0)
        val pinned = TripNavigationMatcher.pinRouteOriginToGps(
            route,
            gpsLat,
            77.59,
            maxBridgeMeters = 10.0,
        )
        assertEquals(route, pinned)
    }

    @Test
    fun pinRouteOriginToGps_prependsWhenOsrmSnappedSlightlyAway() {
        val route = listOf(
            12.97 to 77.59,
            12.971 to 77.591,
        )
        // ~8 m north — short bridge is OK for walk pin match.
        val gpsLat = 12.97 + (8.0 / 111_320.0)
        val pinned = TripNavigationMatcher.pinRouteOriginToGps(
            route,
            gpsLat,
            77.59,
            maxBridgeMeters = 12.0,
        )
        assertEquals(3, pinned.size)
        assertEquals(gpsLat, pinned.first().first, 1e-9)
        assertEquals(77.59, pinned.first().second, 1e-9)
        assertEquals(route.first(), pinned[1])
    }

    @Test
    fun pinRouteOriginToGps_keepsRouteWhenAlreadyAtPin() {
        val route = listOf(12.97 to 77.59, 12.971 to 77.591)
        val pinned = TripNavigationMatcher.pinRouteOriginToGps(route, 12.97, 77.59)
        assertEquals(route, pinned)
    }

    @Test
    fun nextTurnGuidance_advancesPastCompletedSteps() {
        val steps = listOf(
            com.example.potholereport.data.remote.OsrmRouteStep(
                instruction = "Start",
                alongRouteMeters = 0.0,
                stepDistanceMeters = 40.0,
                maneuverType = "depart",
                modifier = "",
                roadName = "",
            ),
            com.example.potholereport.data.remote.OsrmRouteStep(
                instruction = "Turn left onto MG Road",
                alongRouteMeters = 40.0,
                stepDistanceMeters = 120.0,
                maneuverType = "turn",
                modifier = "left",
                roadName = "MG Road",
            ),
            com.example.potholereport.data.remote.OsrmRouteStep(
                instruction = "Arrive at destination",
                alongRouteMeters = 160.0,
                stepDistanceMeters = 0.0,
                maneuverType = "arrive",
                modifier = "",
                roadName = "",
            ),
        )
        val first = TripNavigationMatcher.nextTurnGuidance(steps, traveledMeters = 10.0)
        assertNotNull(first)
        assertEquals("Turn left onto MG Road", first!!.instruction)
        assertEquals(30.0, first.distanceMeters, 0.5)

        val afterTurn = TripNavigationMatcher.nextTurnGuidance(steps, traveledMeters = 55.0)
        assertNotNull(afterTurn)
        assertEquals("Arrive at destination", afterTurn!!.instruction)
    }

    @Test
    fun splitRouteByTraveled_greysCompletedSegment() {
        val (completed, remaining) = TripNavigationMatcher.splitRouteByTraveled(
            straightRoute,
            traveledMeters = TripNavigationMatcher.routeProgress(
                straightRoute,
                driverLat = 0.0,
                driverLon = 0.005,
            )!!.traveledMeters,
        )
        assertTrue(completed.size >= 2)
        assertTrue(remaining.size >= 2)
        assertEquals(completed.last().first, remaining.first().first, 1e-9)
        assertEquals(completed.last().second, remaining.first().second, 1e-9)
    }

    @Test
    fun alertsForMapDisplay_streetFollowKeepsOnlyNearestFew() {
        val alerts = listOf(
            fakeAlert(distanceM = 50.0),
            fakeAlert(distanceM = 200.0),
            fakeAlert(distanceM = 350.0),
            fakeAlert(distanceM = 800.0),
        )
        val shown = TripNavigationMatcher.alertsForMapDisplay(alerts, streetFollow = true)
        assertEquals(2, shown.size)
        assertEquals(50.0, shown[0].distanceMeters, 0.1)
        assertEquals(200.0, shown[1].distanceMeters, 0.1)
    }

    @Test
    fun alertsForMapDisplay_overviewShowsFartherAhead() {
        val alerts = (1..12).map { i -> fakeAlert(distanceM = i * 100.0) }
        val shown = TripNavigationMatcher.alertsForMapDisplay(alerts, streetFollow = false)
        assertEquals(10, shown.size)
        assertEquals(100.0, shown.first().distanceMeters, 0.1)
        assertEquals(1_000.0, shown.last().distanceMeters, 0.1)
    }

    @Test
    fun snapDriverToRoute_pullsNearbyGpsOntoPolyline() {
        // Eastbound segment near equator for simple lat/lon math.
        val route = listOf(0.0 to 0.0, 0.0 to 0.01)
        // ~20 m north of mid-route — should snap back onto the segment.
        val driftedLat = 20.0 / 111_320.0
        val driftedLon = 0.005
        val snapped = TripNavigationMatcher.snapDriverToRoute(route, driftedLat, driftedLon)
        assertNotNull(snapped)
        assertEquals(0.0, snapped!!.first, 1e-5)
        assertEquals(0.005, snapped.second, 1e-5)
    }

    @Test
    fun snapDriverToRoute_returnsNullWhenFarOffRoute() {
        val route = listOf(0.0 to 0.0, 0.0 to 0.01)
        // ~120 m north — beyond DRIVER_MAP_SNAP_HARD_MAX_M.
        val farLat = 120.0 / 111_320.0
        assertNull(TripNavigationMatcher.snapDriverToRoute(route, farLat, 0.005))
    }

    @Test
    fun mapSnapLimit_scalesWithPoorAccuracy() {
        assertEquals(
            TripNavigationMatcher.DRIVER_MAP_SNAP_MAX_M,
            TripNavigationMatcher.mapSnapLimitM(null),
            0.01,
        )
        assertTrue(TripNavigationMatcher.mapSnapLimitM(50f) >= 90.0)
        assertEquals(
            TripNavigationMatcher.DRIVER_MAP_SNAP_HARD_MAX_M,
            TripNavigationMatcher.mapSnapLimitM(80f),
            0.01,
        )
    }

    @Test
    fun snapDriverToRoute_prefersSegmentNearProgress() {
        // Two parallel eastbound segments ~55 m apart; GPS near the northern one,
        // but progress says we are on the southern corridor.
        val south = listOf(0.0 to 0.0, 0.0 to 0.01)
        val northLat = 55.0 / 111_320.0
        val north = listOf(northLat to 0.0, northLat to 0.01)
        val route = south + north
        val gpsLat = northLat
        val gpsLon = 0.005
        val alongSouthMid = TripNavigationMatcher.routeProgress(south, 0.0, 0.005)!!.traveledMeters
        val snapped = TripNavigationMatcher.snapDriverToRoute(
            routePoints = route,
            driverLat = gpsLat,
            driverLon = gpsLon,
            maxSnapMeters = 80.0,
            preferredAlongMeters = alongSouthMid,
        )
        assertNotNull(snapped)
        // Should pull down onto the southern (progress) corridor, not stay on north.
        assertEquals(0.0, snapped!!.first, 1e-4)
    }

    @Test
    fun displayPositionForAlert_offsetsLeftOfEastboundRoute() {
        val route = listOf(0.0 to 0.0, 0.0 to 0.01) // eastbound
        val alert = fakeAlert(distanceM = 100.0, lat = 0.0, lon = 0.005, side = "Left")
        val (lat, lon) = TripNavigationMatcher.displayPositionForAlert(route, alert, offsetMeters = 3.2)
        // Left of eastbound travel is north → latitude increases.
        assertTrue(lat > 0.0)
        assertEquals(0.005, lon, 1e-5)
    }

    @Test
    fun displayPositionForAlert_snapsOffRoadGpsOntoRouteBeforeLaneOffset() {
        val route = listOf(0.0 to 0.0, 0.0 to 0.01) // eastbound along lat=0
        // Report GPS drifted ~22 m north of the road, but labeled Left.
        val alert = fakeAlert(distanceM = 100.0, lat = 0.0002, lon = 0.005, side = "Left")
        val (lat, lon) = TripNavigationMatcher.displayPositionForAlert(
            route,
            alert,
            offsetMeters = 3.2,
            applyLaneOffset = true,
        )
        // Must stay near the road (centerline + ~3.2 m), not at the 22 m GPS drift.
        val driftFromRoad = TripNavigationMatcher.haversineM(0.0, 0.005, lat, lon)
        assertTrue("pin still ~${driftFromRoad}m from road", driftFromRoad < 6.0)
        assertTrue(lat > 0.0) // still left/north of centerline
        assertEquals(0.005, lon, 1e-5)
    }

    @Test
    fun displayPositionForAlert_middleStaysOnCenterlineEvenIfGpsDrifted() {
        val route = listOf(0.0 to 0.0, 0.0 to 0.01)
        val alert = fakeAlert(distanceM = 50.0, lat = 0.0002, lon = 0.004, side = "Middle")
        val (lat, lon) = TripNavigationMatcher.displayPositionForAlert(route, alert)
        assertEquals(0.0, lat, 1e-6)
        assertEquals(0.004, lon, 1e-5)
    }

    @Test
    fun displayPositionForAlert_overviewStillAppliesLaneNudgeOnRoad() {
        val route = listOf(0.0 to 0.0, 0.0 to 0.01)
        val alert = fakeAlert(distanceM = 50.0, lat = 0.0002, lon = 0.004, side = "Right")
        val (lat, lon) = TripNavigationMatcher.displayPositionForAlert(
            route,
            alert,
            applyLaneOffset = true,
        )
        // Snapped to centerline then nudged right (south of eastbound) — stays on-road.
        assertTrue(lat < 0.0)
        assertEquals(0.004, lon, 1e-5)
        val driftFromRoad = TripNavigationMatcher.haversineM(0.0, 0.004, lat, lon)
        assertTrue(driftFromRoad <= TripNavigationMatcher.MAP_SIDE_MAX_OFFSET_M + 0.5)
    }

    @Test
    fun laneLateralOffset_clampedInsideRoadCorridor() {
        val left = TripNavigationMatcher.laneLateralOffsetM("left", 99.0)!!
        assertEquals(-TripNavigationMatcher.MAP_SIDE_MAX_OFFSET_M, left, 1e-9)
        val right = TripNavigationMatcher.laneLateralOffsetM("right", 99.0)!!
        assertEquals(TripNavigationMatcher.MAP_SIDE_MAX_OFFSET_M, right, 1e-9)
        assertTrue(TripNavigationMatcher.MAP_SIDE_MAX_OFFSET_M <= 2.5)
    }

    @Test
    fun detailedPlacement_zoomThresholds() {
        assertTrue(
            !TripNavigationMatcher.useDetailedLanePlacement(14.0, mapLibre = true),
        )
        assertTrue(
            TripNavigationMatcher.useDetailedLanePlacement(16.0, mapLibre = true),
        )
        assertTrue(
            !TripNavigationMatcher.useDetailedLanePlacement(16.0, mapLibre = false),
        )
        assertTrue(
            TripNavigationMatcher.useDetailedLanePlacement(18.0, mapLibre = false),
        )
    }

    @Test
    fun normalizeSideKey_mapsCommonLabels() {
        assertEquals("left", TripNavigationMatcher.normalizeSideKey("on your left"))
        assertEquals("right", TripNavigationMatcher.normalizeSideKey("R"))
        assertEquals("full", TripNavigationMatcher.normalizeSideKey("Full width"))
        assertEquals("middle", TripNavigationMatcher.normalizeSideKey("Middle"))
    }

    private fun fakeAlert(
        distanceM: Double,
        lat: Double = 0.0,
        lon: Double = 0.0,
        side: String? = null,
    ) = TripPotholeAlert(
        report = PersistedPotholeReport(
            id = distanceM.toLong(),
            cityKey = "bengaluru",
            createdAtMs = 0L,
            photoPath = "",
            latitude = lat,
            longitude = lon,
            severity = PotholeSeverity.MODERATE,
        ),
        distanceMeters = distanceM,
        sideLabel = side,
        alongRouteMeters = distanceM,
    )
}
