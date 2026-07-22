package com.example.potholereport.data

import com.example.potholereport.data.remote.OsrmRouteStep
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Pothole ahead on a trip with distance and optional lane side. */
data class TripPotholeAlert(
    val report: PersistedPotholeReport,
    val distanceMeters: Double,
    val sideLabel: String?,
    val alongRouteMeters: Double,
)

/** Driver position projected onto the active route. */
data class TripRouteProgress(
    val traveledMeters: Double,
    val remainingMeters: Double,
    val totalMeters: Double,
    val completionFraction: Double,
    val offRouteMeters: Double,
)

/** Upcoming turn guidance for the trip banner. */
data class TripTurnGuidance(
    val instruction: String,
    val distanceMeters: Double,
    val maneuverType: String,
)

object TripNavigationMatcher {

    /** Max perpendicular distance from route polyline to count a report (meters). */
    const val ROUTE_CORRIDOR_M = 28.0

    /** Max distance along route to show alerts (meters). */
    const val MAX_AHEAD_M = 2_500.0

    /** Minimum GPS accuracy to show lane side (meters). */
    const val SIDE_MIN_ACCURACY_M = 25f

    /** Base distance from the route that triggers a reroute. */
    const val OFF_ROUTE_REROUTE_M = 60.0

    /** Smaller one-time tolerance used to correct an inaccurate initial route origin. */
    const val START_CORRECTION_M = 15.0

    fun routeStartNeedsCorrection(
        routePoints: List<Pair<Double, Double>>,
        driverLat: Double,
        driverLon: Double,
        gpsAccuracyM: Float,
    ): Boolean {
        val start = routePoints.firstOrNull() ?: return false
        if (!driverLat.isFinite() || !driverLon.isFinite() || !gpsAccuracyM.isFinite()) return false
        val threshold = maxOf(START_CORRECTION_M, gpsAccuracyM * 1.25)
        return haversineM(start.first, start.second, driverLat, driverLon) > threshold
    }

    /**
     * Optionally prepends the live GPS pin when OSRM’s first vertex is only a few meters away.
     * Long bridges are never drawn — they cut across parks / open land and look like off-road
     * shortcuts. Prefer a full OSRM re-route from the GPS (caller) instead.
     *
     * @param maxBridgeMeters max allowed GPS→road gap to fill (meters). Car/Bike should pass
     *   a very small value or skip this helper entirely.
     */
    fun pinRouteOriginToGps(
        routePoints: List<Pair<Double, Double>>,
        gpsLat: Double,
        gpsLon: Double,
        maxBridgeMeters: Double = 10.0,
    ): List<Pair<Double, Double>> {
        if (!gpsLat.isFinite() || !gpsLon.isFinite()) return routePoints
        if (routePoints.isEmpty()) return listOf(gpsLat to gpsLon)
        val start = routePoints.first()
        val deltaM = haversineM(start.first, start.second, gpsLat, gpsLon)
        if (deltaM < 1.5) return routePoints
        if (deltaM > maxBridgeMeters) return routePoints
        return listOf(gpsLat to gpsLon) + routePoints
    }

    /**
     * Picks the next guidance step after the driver has passed previous maneuvers.
     * [steps] must use cumulative [OsrmRouteStep.alongRouteMeters].
     */
    fun nextTurnGuidance(
        steps: List<OsrmRouteStep>,
        traveledMeters: Double,
    ): TripTurnGuidance? {
        if (steps.isEmpty() || !traveledMeters.isFinite()) return null
        val upcoming = steps.firstOrNull { step ->
            val type = step.maneuverType.lowercase()
            if (type == "depart") return@firstOrNull false
            step.alongRouteMeters >= traveledMeters - 8.0
        } ?: return null
        val distance = (upcoming.alongRouteMeters - traveledMeters).coerceAtLeast(0.0)
        return TripTurnGuidance(
            instruction = upcoming.instruction,
            distanceMeters = distance,
            maneuverType = upcoming.maneuverType,
        )
    }

    /**
     * Splits [routePoints] into completed (grey) and remaining (blue) polylines at
     * [traveledMeters] along the route. Both lists share the split vertex so the line joins.
     */
    fun splitRouteByTraveled(
        routePoints: List<Pair<Double, Double>>,
        traveledMeters: Double,
    ): Pair<List<Pair<Double, Double>>, List<Pair<Double, Double>>> {
        if (routePoints.size < 2 || !traveledMeters.isFinite() || traveledMeters <= 0.0) {
            return emptyList<Pair<Double, Double>>() to routePoints
        }
        var remainingBudget = traveledMeters
        val completed = mutableListOf<Pair<Double, Double>>()
        completed.add(routePoints.first())
        for (i in 0 until routePoints.lastIndex) {
            val a = routePoints[i]
            val b = routePoints[i + 1]
            val segLen = haversineM(a.first, a.second, b.first, b.second)
            if (segLen <= 1e-3) {
                completed.add(b)
                continue
            }
            if (remainingBudget >= segLen) {
                remainingBudget -= segLen
                completed.add(b)
                continue
            }
            val t = (remainingBudget / segLen).coerceIn(0.0, 1.0)
            val split = (a.first + t * (b.first - a.first)) to (a.second + t * (b.second - a.second))
            completed.add(split)
            val remaining = buildList {
                add(split)
                for (j in (i + 1)..routePoints.lastIndex) add(routePoints[j])
            }
            return completed to remaining
        }
        return completed to emptyList()
    }

    fun routeProgress(
        routePoints: List<Pair<Double, Double>>,
        driverLat: Double,
        driverLon: Double,
    ): TripRouteProgress? {
        if (routePoints.size < 2 || !driverLat.isFinite() || !driverLon.isFinite()) return null
        val segment = nearestSegmentIndex(routePoints, driverLat, driverLon) ?: return null
        val traveled = alongDistanceAt(routePoints, segment, driverLat, driverLon)
        var total = 0.0
        for (index in 0 until routePoints.lastIndex) {
            total += haversineM(
                routePoints[index].first,
                routePoints[index].second,
                routePoints[index + 1].first,
                routePoints[index + 1].second,
            )
        }
        val offRoute = perpendicularDistanceM(
            routePoints[segment],
            routePoints[segment + 1],
            driverLat,
            driverLon,
        )
        val clampedTraveled = traveled.coerceIn(0.0, total)
        return TripRouteProgress(
            traveledMeters = clampedTraveled,
            remainingMeters = (total - clampedTraveled).coerceAtLeast(0.0),
            totalMeters = total,
            completionFraction = if (total > 0.0) clampedTraveled / total else 0.0,
            offRouteMeters = offRoute,
        )
    }

    fun matchAlongRoute(
        routePoints: List<Pair<Double, Double>>,
        reports: List<PersistedPotholeReport>,
        driverLat: Double,
        driverLon: Double,
        driverBearingDeg: Float?,
        gpsAccuracyM: Float?,
    ): List<TripPotholeAlert> {
        if (routePoints.size < 2) return emptyList()
        val driverSeg = nearestSegmentIndex(routePoints, driverLat, driverLon) ?: return emptyList()
        val driverAlong = alongDistanceAt(routePoints, driverSeg, driverLat, driverLon)

        return reports.mapNotNull { report ->
            if (!report.hasCoordinates()) return@mapNotNull null
            val seg = nearestSegmentIndex(routePoints, report.latitude, report.longitude) ?: return@mapNotNull null
            val perp = perpendicularDistanceM(
                routePoints[seg],
                routePoints[seg + 1],
                report.latitude,
                report.longitude,
            )
            if (perp > ROUTE_CORRIDOR_M) return@mapNotNull null
            val along = alongDistanceAt(routePoints, seg, report.latitude, report.longitude)
            val ahead = along - driverAlong
            if (ahead < 5.0 || ahead > MAX_AHEAD_M) return@mapNotNull null
            val side = resolveSideLabel(report, driverBearingDeg, gpsAccuracyM, routePoints, seg, report.latitude, report.longitude)
            TripPotholeAlert(
                report = report,
                distanceMeters = ahead,
                sideLabel = side,
                alongRouteMeters = along,
            )
        }.sortedBy { it.distanceMeters }
    }

    /** Ahead corridor when no destination route — cone along current bearing. */
    fun matchAheadCone(
        reports: List<PersistedPotholeReport>,
        driverLat: Double,
        driverLon: Double,
        driverBearingDeg: Float?,
        gpsAccuracyM: Float?,
        coneHalfAngleDeg: Float = 35f,
        maxAheadM: Double = MAX_AHEAD_M,
    ): List<TripPotholeAlert> {
        val bearing = driverBearingDeg?.takeIf { it.isFinite() } ?: return emptyList()
        return reports.mapNotNull { report ->
            if (!report.hasCoordinates()) return@mapNotNull null
            val dist = haversineM(driverLat, driverLon, report.latitude, report.longitude)
            if (dist < 5.0 || dist > maxAheadM) return@mapNotNull null
            val brg = bearingBetween(driverLat, driverLon, report.latitude, report.longitude)
            if (abs(normalizeBearingDelta(brg - bearing)) > coneHalfAngleDeg) return@mapNotNull null
            val side = if (report.potholePositionEnum() == PotholePosition.FULL_WIDTH) {
                PotholePosition.FULL_WIDTH.displayLabel
            } else if (gpsAccuracyM != null && gpsAccuracyM <= SIDE_MIN_ACCURACY_M) {
                resolveDriverLaneLabel(
                    report.potholePositionEnum(),
                    report.reportBearingDeg.takeIf { !it.isNaN() },
                    report.trafficBearingDeg.takeIf { !it.isNaN() },
                    bearing,
                )
            } else {
                null
            }
            TripPotholeAlert(report, dist, side, dist)
        }.sortedBy { it.distanceMeters }
    }

    private fun resolveSideLabel(
        report: PersistedPotholeReport,
        driverBearingDeg: Float?,
        gpsAccuracyM: Float?,
        route: List<Pair<Double, Double>>,
        seg: Int,
        lat: Double,
        lon: Double,
    ): String? {
        if (report.potholePositionEnum() == PotholePosition.FULL_WIDTH) {
            return PotholePosition.FULL_WIDTH.displayLabel
        }
        if (gpsAccuracyM != null && gpsAccuracyM > SIDE_MIN_ACCURACY_M) return null
        val fromMeta = resolveDriverLaneLabel(
            report.potholePositionEnum(),
            report.reportBearingDeg.takeIf { !it.isNaN() },
            report.trafficBearingDeg.takeIf { !it.isNaN() },
            driverBearingDeg,
        )
        if (fromMeta != null) return fromMeta
        val (aLat, aLon) = route[seg]
        val (bLat, bLon) = route[seg + 1]
        val routeBearing = bearingBetween(aLat, aLon, bLat, bLon)
        val toReport = bearingBetween(aLat, aLon, lat, lon)
        val delta = normalizeBearingDelta(toReport - routeBearing)
        return when {
            abs(delta) < 12f -> "Middle"
            delta > 0 -> "Right"
            else -> "Left"
        }
    }

    private fun nearestSegmentIndex(route: List<Pair<Double, Double>>, lat: Double, lon: Double): Int? {
        if (route.size < 2) return null
        var best = 0
        var bestD = Double.MAX_VALUE
        for (i in 0 until route.size - 1) {
            val d = perpendicularDistanceM(route[i], route[i + 1], lat, lon)
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        return best
    }

    private fun alongDistanceAt(
        route: List<Pair<Double, Double>>,
        seg: Int,
        lat: Double,
        lon: Double,
    ): Double {
        var sum = 0.0
        for (i in 0 until seg) {
            sum += haversineM(route[i].first, route[i].second, route[i + 1].first, route[i + 1].second)
        }
        val (aLat, aLon) = route[seg]
        val (bLat, bLon) = route[seg + 1]
        val t = projectT(aLat, aLon, bLat, bLon, lat, lon).coerceIn(0.0, 1.0)
        val projLat = aLat + t * (bLat - aLat)
        val projLon = aLon + t * (bLon - aLon)
        sum += haversineM(aLat, aLon, projLat, projLon)
        return sum
    }

    private fun projectT(
        aLat: Double, aLon: Double,
        bLat: Double, bLon: Double,
        pLat: Double, pLon: Double,
    ): Double {
        val ax = aLon
        val ay = aLat
        val bx = bLon
        val by = bLat
        val px = pLon
        val py = pLat
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        if (len2 < 1e-12) return 0.0
        return ((px - ax) * dx + (py - ay) * dy) / len2
    }

    private fun perpendicularDistanceM(
        a: Pair<Double, Double>,
        b: Pair<Double, Double>,
        pLat: Double,
        pLon: Double,
    ): Double {
        val t = projectT(a.first, a.second, b.first, b.second, pLat, pLon).coerceIn(0.0, 1.0)
        val projLat = a.first + t * (b.first - a.first)
        val projLon = a.second + t * (b.second - a.second)
        return haversineM(pLat, pLon, projLat, projLon)
    }

    fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
