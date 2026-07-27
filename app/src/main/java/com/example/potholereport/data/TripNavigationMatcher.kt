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

/** How to recover after the driver leaves the active route. */
enum class TripDeviationAction {
    NONE,
    /** User is near a preview alternate — wait for tap to confirm. */
    OFFER_KNOWN_ALTERNATE,
    /** Short return to original route is viable (dashed rejoin line). */
    SHOW_REJOIN,
    /** Rejoin is too costly — full OSRM refetch to destination. */
    FULL_REROUTE,
}

/** Planned recovery for an off-route GPS fix. */
data class TripDeviationPlan(
    val action: TripDeviationAction,
    val knownAlternateIndex: Int? = null,
    val rejoinPoints: List<Pair<Double, Double>> = emptyList(),
    val rejoinReturnMeters: Double = 0.0,
)

/** Upcoming turn guidance for the trip banner. */
data class TripTurnGuidance(
    val instruction: String,
    val distanceMeters: Double,
    val maneuverType: String,
    /** OSRM modifier: left / right / straight / slight left / … */
    val modifier: String = "",
)

object TripNavigationMatcher {

    /** Max perpendicular distance from route polyline to count a report (meters). */
    const val ROUTE_CORRIDOR_M = 28.0

    /** Max distance along route to show alerts (meters). */
    const val MAX_AHEAD_M = 2_500.0

    /** Street-follow map: only the nearest few hazards, closer ahead. */
    const val STREET_FOLLOW_MAP_MAX_ALERTS = 2
    const val STREET_FOLLOW_MAP_MAX_AHEAD_M = 400.0

    /** Map overview: more ahead along the corridor for planning. */
    const val MAP_OVERVIEW_MAX_ALERTS = 10
    const val MAP_OVERVIEW_MAX_AHEAD_M = 2_000.0

    /** Lateral offset on the map when side (L/R) is known (meters). */
    const val MAP_SIDE_OFFSET_M = 7.0

    /** Minimum GPS accuracy to show lane side (meters). */
    const val SIDE_MIN_ACCURACY_M = 25f

    /** Base distance from the route that triggers a reroute. */
    const val OFF_ROUTE_REROUTE_M = 60.0

    /**
     * Max lateral GPS drift (meters) we still pull onto the route polyline for the
     * map cursor / camera. Beyond this, show raw GPS (truly off-route).
     */
    const val DRIVER_MAP_SNAP_MAX_M = 45.0

    /** Smaller one-time tolerance used to correct an inaccurate initial route origin. */
    const val START_CORRECTION_M = 15.0

    /** Prefer rejoin when return-to-route distance is at or below this (hybrid #23.3). */
    const val REJOIN_PREF_MAX_RETURN_M = 400.0

    /** Beyond this return distance, always full-reroute. */
    const val REJOIN_HARD_MAX_RETURN_M = 500.0

    /** Rejoin may be up to this many seconds slower than a full reroute and still win. */
    const val REJOIN_ETA_SLACK_SEC = 90.0

    /** Corridor to treat GPS as “on” a known preview alternate. */
    const val KNOWN_ALTERNATE_CORRIDOR_M = 45.0

    /** Fallback speed (~30 km/h) when GPS speed is missing / too low for ETA estimates. */
    const val DEVIATION_FALLBACK_SPEED_MPS = 8.33

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
            modifier = upcoming.modifier,
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

    /**
     * Nearest point on [routePoints] for map display when GPS has drifted slightly
     * off the road (e.g. onto a building). Returns null if farther than [maxSnapMeters]
     * so a real off-route position is not forced onto the polyline.
     */
    fun snapDriverToRoute(
        routePoints: List<Pair<Double, Double>>,
        driverLat: Double,
        driverLon: Double,
        maxSnapMeters: Double = DRIVER_MAP_SNAP_MAX_M,
    ): Pair<Double, Double>? {
        if (routePoints.size < 2 || !driverLat.isFinite() || !driverLon.isFinite()) return null
        if (!maxSnapMeters.isFinite() || maxSnapMeters <= 0.0) return null
        val seg = nearestSegmentIndex(routePoints, driverLat, driverLon) ?: return null
        val (aLat, aLon) = routePoints[seg]
        val (bLat, bLon) = routePoints[seg + 1]
        val t = projectT(aLat, aLon, bLat, bLon, driverLat, driverLon).coerceIn(0.0, 1.0)
        val projLat = aLat + t * (bLat - aLat)
        val projLon = aLon + t * (bLon - aLon)
        val drift = haversineM(driverLat, driverLon, projLat, projLon)
        if (drift > maxSnapMeters) return null
        return projLat to projLon
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

    /**
     * Subset of [alerts] for drawing on the map: denser in overview, sparse in street follow.
     * Voice / banner should keep using the full sorted [alerts] list.
     */
    fun alertsForMapDisplay(
        alerts: List<TripPotholeAlert>,
        streetFollow: Boolean,
    ): List<TripPotholeAlert> {
        val maxAhead = if (streetFollow) STREET_FOLLOW_MAP_MAX_AHEAD_M else MAP_OVERVIEW_MAX_AHEAD_M
        val maxCount = if (streetFollow) STREET_FOLLOW_MAP_MAX_ALERTS else MAP_OVERVIEW_MAX_ALERTS
        return alerts
            .asSequence()
            .filter { it.distanceMeters.isFinite() && it.distanceMeters <= maxAhead }
            .sortedBy { it.distanceMeters }
            .take(maxCount)
            .toList()
    }

    /**
     * Map pin lat/lon for [alert]: report coordinates, nudged left/right of the route
     * when placement is known so the driver can see approximate lane side.
     */
    fun displayPositionForAlert(
        routePoints: List<Pair<Double, Double>>,
        alert: TripPotholeAlert,
        offsetMeters: Double = MAP_SIDE_OFFSET_M,
    ): Pair<Double, Double> {
        val lat = alert.report.latitude
        val lon = alert.report.longitude
        if (!lat.isFinite() || !lon.isFinite()) return lat to lon
        val side = normalizeSideKey(alert.sideLabel) ?: return lat to lon
        if (side == "middle" || side == "full") return lat to lon
        if (routePoints.size < 2 || offsetMeters <= 0.0) return lat to lon
        val seg = nearestSegmentIndex(routePoints, lat, lon) ?: return lat to lon
        val (aLat, aLon) = routePoints[seg]
        val (bLat, bLon) = routePoints[seg + 1]
        val routeBearing = bearingBetween(aLat, aLon, bLat, bLon)
        val perpBearing = when (side) {
            "left" -> routeBearing - 90f
            "right" -> routeBearing + 90f
            else -> return lat to lon
        }
        return destinationPoint(lat, lon, perpBearing, offsetMeters)
    }

    internal fun normalizeSideKey(sideLabel: String?): String? {
        val raw = sideLabel?.trim()?.lowercase().orEmpty()
        if (raw.isEmpty()) return null
        return when (raw) {
            "left", "l", "on your left" -> "left"
            "right", "r", "on your right" -> "right"
            "middle", "m", "center", "centre", "in the middle of the road" -> "middle"
            "full width", "fullwidth", "f", "across", "across the road", "whole road",
            "full road", "full road pothole", "crater",
            -> "full"
            else -> when {
                raw.contains("left") -> "left"
                raw.contains("right") -> "right"
                raw.contains("middle") || raw.contains("center") || raw.contains("centre") -> "middle"
                raw.contains("full") || raw.contains("across") -> "full"
                else -> null
            }
        }
    }

    /** Move [distanceM] along [bearingDeg] from [lat]/[lon] (approx local tangent). */
    fun destinationPoint(
        lat: Double,
        lon: Double,
        bearingDeg: Float,
        distanceM: Double,
    ): Pair<Double, Double> {
        if (!lat.isFinite() || !lon.isFinite() || !bearingDeg.isFinite() || !distanceM.isFinite()) {
            return lat to lon
        }
        val br = Math.toRadians(bearingDeg.toDouble())
        val dLat = (distanceM * cos(br)) / 111_320.0
        val dLon = (distanceM * sin(br)) / (111_320.0 * cos(Math.toRadians(lat)).coerceAtLeast(1e-6))
        return (lat + dLat) to (lon + dLon)
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

    /**
     * Builds a short dashed rejoin path from the driver to the nearest point on
     * [routePoints], then continues along the remaining original route.
     * @return polyline + pure return distance (driver → snap), or null if impossible.
     */
    fun buildRejoinPolyline(
        routePoints: List<Pair<Double, Double>>,
        driverLat: Double,
        driverLon: Double,
    ): Pair<List<Pair<Double, Double>>, Double>? {
        if (routePoints.size < 2 || !driverLat.isFinite() || !driverLon.isFinite()) return null
        val seg = nearestSegmentIndex(routePoints, driverLat, driverLon) ?: return null
        val (aLat, aLon) = routePoints[seg]
        val (bLat, bLon) = routePoints[seg + 1]
        val t = projectT(aLat, aLon, bLat, bLon, driverLat, driverLon).coerceIn(0.0, 1.0)
        val snapLat = aLat + t * (bLat - aLat)
        val snapLon = aLon + t * (bLon - aLon)
        val returnM = haversineM(driverLat, driverLon, snapLat, snapLon)
        if (!returnM.isFinite()) return null
        val alongAtSnap = alongDistanceAt(routePoints, seg, snapLat, snapLon)
        val (_, remaining) = splitRouteByTraveled(routePoints, alongAtSnap)
        val points = buildList {
            add(driverLat to driverLon)
            if (returnM > 2.0) add(snapLat to snapLon)
            // Skip duplicate first remaining vertex if it matches snap.
            remaining.forEachIndexed { i, p ->
                if (i == 0 && haversineM(p.first, p.second, snapLat, snapLon) < 1.5) return@forEachIndexed
                add(p)
            }
        }
        if (points.size < 2) return null
        return points to returnM
    }

    /** Index of a known alternate corridor the driver is currently near, or null. */
    fun findNearbyKnownAlternateIndex(
        knownAlternates: List<List<Pair<Double, Double>>>,
        activeRoute: List<Pair<Double, Double>>,
        driverLat: Double,
        driverLon: Double,
        corridorM: Double = KNOWN_ALTERNATE_CORRIDOR_M,
    ): Int? {
        if (!driverLat.isFinite() || !driverLon.isFinite() || corridorM <= 0.0) return null
        var bestIdx: Int? = null
        var bestDist = Double.MAX_VALUE
        knownAlternates.forEachIndexed { index, alt ->
            if (alt.size < 2) return@forEachIndexed
            if (routesLookAlike(alt, activeRoute)) return@forEachIndexed
            val progress = routeProgress(alt, driverLat, driverLon) ?: return@forEachIndexed
            if (progress.offRouteMeters <= corridorM && progress.offRouteMeters < bestDist) {
                bestDist = progress.offRouteMeters
                bestIdx = index
            }
        }
        return bestIdx
    }

    fun routesLookAlike(
        a: List<Pair<Double, Double>>,
        b: List<Pair<Double, Double>>,
        sampleGapMaxM: Double = 80.0,
    ): Boolean {
        if (a.size < 2 || b.size < 2) return false
        val stations = listOf(0.25, 0.5, 0.75)
        var maxGap = 0.0
        for (frac in stations) {
            val ai = (a.size * frac).toInt().coerceIn(0, a.lastIndex)
            val bi = (b.size * frac).toInt().coerceIn(0, b.lastIndex)
            maxGap = maxOf(
                maxGap,
                haversineM(a[ai].first, a[ai].second, b[bi].first, b[bi].second),
            )
        }
        return maxGap <= sampleGapMaxM
    }

    fun estimateTravelSeconds(distanceM: Double, speedMps: Float?): Double {
        if (!distanceM.isFinite() || distanceM <= 0.0) return 0.0
        val speed = speedMps?.takeIf { it.isFinite() && it >= 1.5f }?.toDouble()
            ?: DEVIATION_FALLBACK_SPEED_MPS
        return distanceM / speed
    }

    /**
     * Hybrid #23.3 decision once the driver is clearly off the active route.
     * [fullRerouteDurationSec] may be null before an OSRM probe completes —
     * then distance alone decides (≤ [REJOIN_PREF_MAX_RETURN_M] → rejoin).
     */
    fun planDeviation(
        routePoints: List<Pair<Double, Double>>,
        knownAlternates: List<List<Pair<Double, Double>>>,
        driverLat: Double,
        driverLon: Double,
        progress: TripRouteProgress,
        speedMps: Float?,
        fullRerouteDurationSec: Double?,
    ): TripDeviationPlan {
        val nearbyAlt = findNearbyKnownAlternateIndex(
            knownAlternates, routePoints, driverLat, driverLon,
        )
        if (nearbyAlt != null) {
            return TripDeviationPlan(
                action = TripDeviationAction.OFFER_KNOWN_ALTERNATE,
                knownAlternateIndex = nearbyAlt,
            )
        }

        val rejoin = buildRejoinPolyline(routePoints, driverLat, driverLon)
            ?: return TripDeviationPlan(TripDeviationAction.FULL_REROUTE)
        val (rejoinPoints, returnM) = rejoin

        // Snap behind the driver by a lot while drifting → treat as intentional detour.
        val snapAlong = progress.traveledMeters
        val headingAway =
            returnM > 120.0 &&
                progress.offRouteMeters > OFF_ROUTE_REROUTE_M * 1.5 &&
                snapAlong < progress.totalMeters * 0.15 &&
                progress.completionFraction < 0.12

        if (headingAway || returnM > REJOIN_HARD_MAX_RETURN_M) {
            return TripDeviationPlan(TripDeviationAction.FULL_REROUTE)
        }

        val remainingAfterSnap = (progress.remainingMeters).coerceAtLeast(0.0)
        // Rough remaining after rejoining ≈ remaining on original − nothing for return on-route.
        // Return leg is off-route; remaining is still roughly progress.remainingMeters from snap.
        val rejoinEtaSec =
            estimateTravelSeconds(returnM, speedMps) +
                estimateTravelSeconds(remainingAfterSnap, speedMps)

        val preferRejoin = when {
            returnM > REJOIN_PREF_MAX_RETURN_M -> false
            fullRerouteDurationSec == null -> true
            else -> rejoinEtaSec <= fullRerouteDurationSec + REJOIN_ETA_SLACK_SEC
        }

        return if (preferRejoin) {
            TripDeviationPlan(
                action = TripDeviationAction.SHOW_REJOIN,
                rejoinPoints = rejoinPoints,
                rejoinReturnMeters = returnM,
            )
        } else {
            TripDeviationPlan(TripDeviationAction.FULL_REROUTE)
        }
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
