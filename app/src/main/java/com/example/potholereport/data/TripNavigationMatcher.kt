package com.example.potholereport.data

import com.example.potholereport.data.remote.OsrmRouteStep
import com.example.potholereport.data.remote.TripVehicle
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
    /** Driver is on a preview alternate that still reaches the destination — continue it. */
    ADOPT_KNOWN_ALTERNATE,
    /**
     * Driver is on a corridor that does **not** reach the destination —
     * dashed rejoin back onto a route that does.
     */
    SHOW_REJOIN,
    /**
     * Path is unsuitable for the chosen vehicle (e.g. car on a jungle / dirt-bike trail) —
     * guide a U-turn back onto the vehicle network.
     */
    SUGGEST_UTURN,
    /** Recalculate the best vehicle-appropriate route to the destination. */
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

    /**
     * Lateral offset from the **snapped route centerline** for a full Left/Right third
     * (meters). Kept inside a typical urban carriageway (~half of ~5–6 m painted road on
     * the basemap), not on sidewalks / medians. Slightly conservative so L/R stay on asphalt
     * even when the OSRM centerline drifts toward one curb.
     */
    const val MAP_SIDE_OFFSET_M = 1.7

    /** Hard cap so pins never leave the road corridor even if callers pass a large offset. */
    const val MAP_SIDE_MAX_OFFSET_M = 2.2

    /** LM / MR sit closer to center than pure L / R. */
    const val MAP_SIDE_OFFSET_PARTIAL_FRAC = 0.50

    /**
     * MapLibre zoom at/above which trip potholes use L/M/R lane placement + span icons.
     * Below this → small centerline dots (road not wide enough on screen).
     */
    const val DETAILED_PLACEMENT_ZOOM_MAPLIBRE = 15.8

    /** osmdroid / CARTO zoom for the same switch (scale differs from MapLibre). */
    const val DETAILED_PLACEMENT_ZOOM_OSM = 17.5

    /** True when zoom is high enough to show L/M/R placement (vs overview dots). */
    fun useDetailedLanePlacement(zoom: Double, mapLibre: Boolean): Boolean {
        if (!zoom.isFinite()) return false
        val threshold = if (mapLibre) DETAILED_PLACEMENT_ZOOM_MAPLIBRE else DETAILED_PLACEMENT_ZOOM_OSM
        return zoom >= threshold
    }

    /** Coarse bucket so UI only rebuilds markers when crossing the placement threshold. */
    fun placementZoomBucket(zoom: Double, mapLibre: Boolean): Int =
        if (useDetailedLanePlacement(zoom, mapLibre)) 1 else 0

    /** Minimum GPS accuracy to show lane side (meters). */
    const val SIDE_MIN_ACCURACY_M = 25f

    /** Base distance from the route that triggers a reroute. */
    const val OFF_ROUTE_REROUTE_M = 60.0

    /**
     * Max lateral GPS drift (meters) we still pull onto the route polyline for the
     * map cursor / camera when accuracy is unknown. Beyond the hard cap, show raw GPS.
     */
    const val DRIVER_MAP_SNAP_MAX_M = 65.0

    /** Absolute ceiling for map snap (poor urban GPS / multipath). */
    const val DRIVER_MAP_SNAP_HARD_MAX_M = 100.0

    /** Smaller one-time tolerance used to correct an inaccurate initial route origin. */
    const val START_CORRECTION_M = 15.0

    /** Prefer rejoin when return-to-route distance is at or below this (hybrid #23.3). */
    const val REJOIN_PREF_MAX_RETURN_M = 400.0

    /** Beyond this return distance, always full-reroute. */
    const val REJOIN_HARD_MAX_RETURN_M = 500.0

    /** Rejoin may be up to this many seconds slower than a full reroute and still win. */
    const val REJOIN_ETA_SLACK_SEC = 90.0

    /** Tiny GPS spur: allow dashed rejoin only within this return distance. */
    const val REJOIN_GPS_DRIFT_MAX_M = 35.0

    /** Tiny GPS spur: allow dashed rejoin only within this off-route distance. */
    const val REJOIN_GPS_DRIFT_OFF_ROUTE_M = 28.0

    /** Car GPS this far from the driving network → wrong path (trail / footway). */
    const val WRONG_NETWORK_SNAP_CAR_M = 90.0

    const val WRONG_NETWORK_SNAP_BIKE_M = 140.0

    const val WRONG_NETWORK_SNAP_WALK_M = 200.0

    /** Also require this much departure from the planned corridor before U-turn. */
    const val WRONG_NETWORK_MIN_OFF_ROUTE_M = 35.0

    /** Corridor to treat GPS as “on” a known preview alternate (parallel urban streets). */
    const val KNOWN_ALTERNATE_CORRIDOR_M = 80.0

    /**
     * Adopt an alternate only when it is at least this much closer than the active
     * route (avoids flapping near shared start/end segments).
     */
    const val KNOWN_ALTERNATE_MIN_IMPROVEMENT_M = 22.0

    /** Alternate endpoint must finish this close to the trip destination. */
    const val ALTERNATE_DEST_MATCH_M = 160.0

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

    /** Point on [routePoints] at cumulative distance [distanceM] from the start. */
    fun pointAtDistanceAlongRoute(
        routePoints: List<Pair<Double, Double>>,
        distanceM: Double,
    ): Pair<Double, Double>? {
        if (routePoints.isEmpty()) return null
        if (routePoints.size == 1 || !distanceM.isFinite() || distanceM <= 0.0) {
            return routePoints.first()
        }
        var remainingBudget = distanceM
        for (i in 0 until routePoints.lastIndex) {
            val a = routePoints[i]
            val b = routePoints[i + 1]
            val segLen = haversineM(a.first, a.second, b.first, b.second)
            if (segLen <= 1e-3) continue
            if (remainingBudget <= segLen) {
                val t = (remainingBudget / segLen).coerceIn(0.0, 1.0)
                return (a.first + t * (b.first - a.first)) to (a.second + t * (b.second - a.second))
            }
            remainingBudget -= segLen
        }
        return routePoints.last()
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
     * How far sideways GPS may drift and still be drawn on the route.
     * Scales with reported accuracy so a coarse fix does not leave the pin
     * floating over buildings when zoomed in.
     */
    fun mapSnapLimitM(gpsAccuracyM: Float?): Double {
        if (gpsAccuracyM == null || !gpsAccuracyM.isFinite() || gpsAccuracyM <= 0f) {
            return DRIVER_MAP_SNAP_MAX_M
        }
        return maxOf(DRIVER_MAP_SNAP_MAX_M, gpsAccuracyM.toDouble() * 1.8)
            .coerceAtMost(DRIVER_MAP_SNAP_HARD_MAX_M)
    }

    /**
     * Nearest point on [routePoints] for map display when GPS has drifted slightly
     * off the road (e.g. onto a building). Returns null if farther than [maxSnapMeters]
     * so a real off-route position is not forced onto the polyline.
     *
     * @param preferredAlongMeters when set (current trip progress), prefers segments
     *   near that distance so parallel / return-path roads do not steal the cursor.
     */
    fun snapDriverToRoute(
        routePoints: List<Pair<Double, Double>>,
        driverLat: Double,
        driverLon: Double,
        maxSnapMeters: Double = DRIVER_MAP_SNAP_MAX_M,
        preferredAlongMeters: Double? = null,
    ): Pair<Double, Double>? {
        if (routePoints.size < 2 || !driverLat.isFinite() || !driverLon.isFinite()) return null
        if (!maxSnapMeters.isFinite() || maxSnapMeters <= 0.0) return null
        val seg = bestSnapSegmentIndex(
            routePoints, driverLat, driverLon, preferredAlongMeters,
        ) ?: return null
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
     * Map pin placement for [alert]:
     * 1) snap report GPS onto the route centerline (kills sidewalk / building drift),
     * 2) nudge by reported side (L / M / R / LM / MR) while staying inside the road,
     * 3) [bearingDeg] = route travel direction at the pin (for rotating span icons
     *    so the long axis lies across the carriageway).
     *
     * Mid / full stay on the centerline; L/R stay within [MAP_SIDE_MAX_OFFSET_M].
     * Off-road GPS is always pulled onto the route when a polyline exists — never leave
     * the marker on a sidewalk/building just because the stored coordinate drifted.
     */
    data class AlertMapPlacement(
        val latitude: Double,
        val longitude: Double,
        /** Degrees clockwise from north along the route at the pin. */
        val bearingDeg: Float,
    )

    fun displayPositionForAlert(
        routePoints: List<Pair<Double, Double>>,
        alert: TripPotholeAlert,
        offsetMeters: Double = MAP_SIDE_OFFSET_M,
        applyLaneOffset: Boolean = true,
    ): Pair<Double, Double> {
        val p = displayPlacementForAlert(routePoints, alert, offsetMeters, applyLaneOffset)
        return p.latitude to p.longitude
    }

    fun displayPlacementForAlert(
        routePoints: List<Pair<Double, Double>>,
        alert: TripPotholeAlert,
        offsetMeters: Double = MAP_SIDE_OFFSET_M,
        applyLaneOffset: Boolean = true,
    ): AlertMapPlacement {
        val lat = alert.report.latitude
        val lon = alert.report.longitude
        if (!lat.isFinite() || !lon.isFinite()) {
            return AlertMapPlacement(lat, lon, 0f)
        }
        if (routePoints.size < 2) {
            return AlertMapPlacement(lat, lon, 0f)
        }

        // Always snap onto the route centerline first (off-road GPS → on-road pin).
        val snapped = projectPointOntoRoute(routePoints, lat, lon) ?: (lat to lon)
        val seg = nearestSegmentIndex(routePoints, snapped.first, snapped.second)
        val routeBearing = if (seg != null) {
            val (aLat, aLon) = routePoints[seg]
            val (bLat, bLon) = routePoints[seg + 1]
            bearingBetween(aLat, aLon, bLat, bLon)
        } else {
            0f
        }

        if (!applyLaneOffset || offsetMeters <= 0.0) {
            return AlertMapPlacement(snapped.first, snapped.second, routeBearing)
        }

        val side = normalizeSideKey(alert.sideLabel)
            ?: normalizeSideKey(alert.report.potholePositionEnum().displayLabel)
            ?: normalizeSideKey(alert.report.potholePosition)
        val cappedBase = offsetMeters.coerceIn(0.0, MAP_SIDE_MAX_OFFSET_M)
        val lateralM = side?.let { laneLateralOffsetM(it, cappedBase) }
        if (lateralM == null || lateralM == 0.0) {
            return AlertMapPlacement(snapped.first, snapped.second, routeBearing)
        }

        val perpBearing = if (lateralM < 0.0) routeBearing - 90f else routeBearing + 90f
        val nudgeM = kotlin.math.abs(lateralM).coerceAtMost(MAP_SIDE_MAX_OFFSET_M)
        val (olat, olon) = destinationPoint(
            snapped.first,
            snapped.second,
            perpBearing,
            nudgeM,
        )
        // Safety: if numerical drift somehow exceeds the corridor, fall back to centerline.
        val drift = haversineM(snapped.first, snapped.second, olat, olon)
        if (drift > MAP_SIDE_MAX_OFFSET_M + 0.35) {
            return AlertMapPlacement(snapped.first, snapped.second, routeBearing)
        }
        return AlertMapPlacement(olat, olon, routeBearing)
    }

    /**
     * Signed meters left (−) / right (+) of travel direction from centerline,
     * or null when no lateral nudge applies (middle / full / unknown).
     */
    internal fun laneLateralOffsetM(sideKey: String, baseOffsetM: Double): Double? {
        val capped = baseOffsetM.coerceIn(0.0, MAP_SIDE_MAX_OFFSET_M)
        return when (sideKey) {
            "left" -> -capped
            "right" -> capped
            "lm" -> -capped * MAP_SIDE_OFFSET_PARTIAL_FRAC
            "mr" -> capped * MAP_SIDE_OFFSET_PARTIAL_FRAC
            "middle", "full" -> 0.0
            else -> null
        }
    }

    /** Nearest point on the route polyline (clamped to segments). */
    fun projectPointOntoRoute(
        routePoints: List<Pair<Double, Double>>,
        lat: Double,
        lon: Double,
    ): Pair<Double, Double>? {
        if (routePoints.size < 2 || !lat.isFinite() || !lon.isFinite()) return null
        val seg = nearestSegmentIndex(routePoints, lat, lon) ?: return null
        val (aLat, aLon) = routePoints[seg]
        val (bLat, bLon) = routePoints[seg + 1]
        val t = projectT(aLat, aLon, bLat, bLon, lat, lon).coerceIn(0.0, 1.0)
        return aLat + t * (bLat - aLat) to aLon + t * (bLon - aLon)
    }

    internal fun normalizeSideKey(sideLabel: String?): String? {
        val raw = sideLabel?.trim()?.lowercase().orEmpty()
        if (raw.isEmpty()) return null
        return when (raw) {
            "left", "l", "on your left" -> "left"
            "right", "r", "on your right" -> "right"
            "middle", "m", "center", "centre", "in the middle of the road" -> "middle"
            "lm", "left+middle", "left-middle", "l+m", "ml", "on your left-middle" -> "lm"
            "mr", "middle+right", "middle-right", "m+r", "rm", "on your right-middle" -> "mr"
            "lmr", "full width", "fullwidth", "f", "across", "across the road", "whole road",
            "full road", "full road pothole", "crater",
            -> "full"
            else -> when {
                raw.contains("left") && raw.contains("middle") -> "lm"
                raw.contains("right") && raw.contains("middle") -> "mr"
                raw.contains("left") -> "left"
                raw.contains("right") -> "right"
                raw.contains("middle") || raw.contains("center") || raw.contains("centre") -> "middle"
                raw.contains("full") || raw.contains("across") || raw == "lmr" -> "full"
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

    /**
     * Segment pick for map snapping: nearest lateral distance, biased toward the
     * driver's current along-route progress so the cursor stays on the active road.
     */
    private fun bestSnapSegmentIndex(
        route: List<Pair<Double, Double>>,
        lat: Double,
        lon: Double,
        preferredAlongMeters: Double?,
    ): Int? {
        if (route.size < 2) return null
        var best = 0
        var bestScore = Double.MAX_VALUE
        var cumAlong = 0.0
        for (i in 0 until route.size - 1) {
            val (aLat, aLon) = route[i]
            val (bLat, bLon) = route[i + 1]
            val segLen = haversineM(aLat, aLon, bLat, bLon)
            val d = perpendicularDistanceM(route[i], route[i + 1], lat, lon)
            val t = projectT(aLat, aLon, bLat, bLon, lat, lon).coerceIn(0.0, 1.0)
            val along = cumAlong + t * segLen
            var score = d
            val preferred = preferredAlongMeters
            if (preferred != null && preferred.isFinite()) {
                val delta = along - preferred
                // Heavily penalize snapping far behind (common parallel / opposite carriageway).
                if (delta < -35.0) {
                    score += (-delta - 35.0) * 0.35
                }
                // Mild penalty for jumping far ahead of progress on a parallel road.
                if (delta > 280.0) {
                    score += (delta - 280.0) * 0.08
                }
            }
            if (score < bestScore) {
                bestScore = score
                best = i
            }
            cumAlong += segLen
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
        destLat: Double? = null,
        destLon: Double? = null,
    ): Int? = findPreferredKnownAlternateIndex(
        knownAlternates = knownAlternates,
        activeRoute = activeRoute,
        driverLat = driverLat,
        driverLon = driverLon,
        activeOffRouteMeters = routeProgress(activeRoute, driverLat, driverLon)?.offRouteMeters,
        corridorM = corridorM,
        destLat = destLat,
        destLon = destLon,
    )

    /**
     * Prefer a known alternate when the driver is clearly on it (closer than the
     * active route) and that corridor still ends at the destination.
     */
    fun findPreferredKnownAlternateIndex(
        knownAlternates: List<List<Pair<Double, Double>>>,
        activeRoute: List<Pair<Double, Double>>,
        driverLat: Double,
        driverLon: Double,
        activeOffRouteMeters: Double?,
        corridorM: Double = KNOWN_ALTERNATE_CORRIDOR_M,
        minImprovementM: Double = KNOWN_ALTERNATE_MIN_IMPROVEMENT_M,
        destLat: Double? = null,
        destLon: Double? = null,
    ): Int? {
        if (!driverLat.isFinite() || !driverLon.isFinite() || corridorM <= 0.0) return null
        val activeOff = activeOffRouteMeters?.takeIf { it.isFinite() } ?: return null
        val destPair = when {
            destLat != null && destLon != null && destLat.isFinite() && destLon.isFinite() ->
                destLat to destLon
            activeRoute.size >= 2 -> activeRoute.last()
            else -> null
        }
        var bestIdx: Int? = null
        var bestDist = Double.MAX_VALUE
        knownAlternates.forEachIndexed { index, alt ->
            if (alt.size < 2) return@forEachIndexed
            if (routesLookAlike(alt, activeRoute)) return@forEachIndexed
            if (destPair != null && !routeReachesDestination(alt, destPair.first, destPair.second)) {
                return@forEachIndexed
            }
            val progress = routeProgress(alt, driverLat, driverLon) ?: return@forEachIndexed
            if (progress.offRouteMeters > corridorM) return@forEachIndexed
            // Need a clear improvement vs the active corridor (not GPS noise at a shared start).
            if (progress.offRouteMeters + minImprovementM >= activeOff) return@forEachIndexed
            if (progress.offRouteMeters < bestDist) {
                bestDist = progress.offRouteMeters
                bestIdx = index
            }
        }
        return bestIdx
    }

    /** True when the route’s last vertex is close enough to the trip destination. */
    fun routeReachesDestination(
        routePoints: List<Pair<Double, Double>>,
        destLat: Double,
        destLon: Double,
        maxEndGapM: Double = ALTERNATE_DEST_MATCH_M,
    ): Boolean {
        val end = routePoints.lastOrNull() ?: return false
        if (!destLat.isFinite() || !destLon.isFinite() || !maxEndGapM.isFinite()) return false
        return haversineM(end.first, end.second, destLat, destLon) <= maxEndGapM
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
     * #23.3 — recover after leaving the active route, using the trip vehicle.
     *
     * - On a **known alternate that reaches the destination** → auto-adopt (continue).
     * - On a corridor that **does not reach the destination** → dashed **rejoin** to one that does.
     * - On a path **unsuitable for the vehicle** (poor network snap, e.g. car in jungle) → **U-turn**.
     * - Otherwise → **full reroute** for the best vehicle-appropriate path to the destination.
     *
     * @param vehicleNetworkSnapM OSRM start-snap from the driver to the vehicle profile
     *   network (meters). Large values mean the GPS is not on roads for that vehicle.
     */
    fun planDeviation(
        routePoints: List<Pair<Double, Double>>,
        knownAlternates: List<List<Pair<Double, Double>>>,
        driverLat: Double,
        driverLon: Double,
        progress: TripRouteProgress,
        speedMps: Float?,
        fullRerouteDurationSec: Double?,
        destLat: Double? = null,
        destLon: Double? = null,
        vehicle: TripVehicle = TripVehicle.CAR,
        vehicleNetworkSnapM: Double? = null,
    ): TripDeviationPlan {
        @Suppress("UNUSED_PARAMETER")
        val unusedEtaProbe = fullRerouteDurationSec
        val destPair = when {
            destLat != null && destLon != null && destLat.isFinite() && destLon.isFinite() ->
                destLat to destLon
            routePoints.size >= 2 -> routePoints.last()
            else -> null
        }

        // 1) Vehicle-valid preview alternate that still reaches dest → continue it.
        val nearbyAlt = findPreferredKnownAlternateIndex(
            knownAlternates = knownAlternates,
            activeRoute = routePoints,
            driverLat = driverLat,
            driverLon = driverLon,
            activeOffRouteMeters = progress.offRouteMeters,
            destLat = destPair?.first,
            destLon = destPair?.second,
        )
        if (nearbyAlt != null) {
            return TripDeviationPlan(
                action = TripDeviationAction.ADOPT_KNOWN_ALTERNATE,
                knownAlternateIndex = nearbyAlt,
            )
        }
        val softAlt = findPreferredKnownAlternateIndex(
            knownAlternates = knownAlternates,
            activeRoute = routePoints,
            driverLat = driverLat,
            driverLon = driverLon,
            activeOffRouteMeters = progress.offRouteMeters,
            minImprovementM = 0.0,
            destLat = destPair?.first,
            destLon = destPair?.second,
        )
        if (softAlt != null) {
            return TripDeviationPlan(
                action = TripDeviationAction.ADOPT_KNOWN_ALTERNATE,
                knownAlternateIndex = softAlt,
            )
        }

        // 2) Wrong network for this vehicle (jungle / footpath while driving a car, etc.).
        if (isWrongNetworkForVehicle(vehicle, vehicleNetworkSnapM, progress.offRouteMeters)) {
            return TripDeviationPlan(TripDeviationAction.SUGGEST_UTURN)
        }

        // 3) On a known corridor that does NOT reach the destination → dashed rejoin
        //    onto the active route (which does).
        if (destPair != null) {
            val deadEndIdx = findKnownAlternateMissingDestination(
                knownAlternates = knownAlternates,
                activeRoute = routePoints,
                driverLat = driverLat,
                driverLon = driverLon,
                destLat = destPair.first,
                destLon = destPair.second,
            )
            if (deadEndIdx != null) {
                val rejoin = buildRejoinPolyline(routePoints, driverLat, driverLon)
                if (rejoin != null && rejoin.second <= REJOIN_HARD_MAX_RETURN_M) {
                    return TripDeviationPlan(
                        action = TripDeviationAction.SHOW_REJOIN,
                        rejoinPoints = rejoin.first,
                        rejoinReturnMeters = rejoin.second,
                    )
                }
                return TripDeviationPlan(TripDeviationAction.FULL_REROUTE)
            }
        }

        // 4) Rematch the best vehicle route to the destination (no rejoin dash for
        //    parallel / intentional corridors). Tiny GPS spur may still rejoin.
        val rejoin = buildRejoinPolyline(routePoints, driverLat, driverLon)
        if (rejoin != null) {
            val returnM = rejoin.second
            val activeReaches = destPair == null || routeReachesDestination(
                routePoints,
                destPair.first,
                destPair.second,
            )
            if (
                activeReaches &&
                returnM <= REJOIN_GPS_DRIFT_MAX_M &&
                progress.offRouteMeters <= REJOIN_GPS_DRIFT_OFF_ROUTE_M
            ) {
                return TripDeviationPlan(
                    action = TripDeviationAction.SHOW_REJOIN,
                    rejoinPoints = rejoin.first,
                    rejoinReturnMeters = returnM,
                )
            }
        }
        return TripDeviationPlan(TripDeviationAction.FULL_REROUTE)
    }

    /**
     * OSRM / GPS says the driver is not on roads for [vehicle]
     * (e.g. car GPS deep on a foot / dirt-bike trail).
     */
    fun isWrongNetworkForVehicle(
        vehicle: TripVehicle,
        vehicleNetworkSnapM: Double?,
        offRouteMeters: Double,
    ): Boolean {
        val snap = vehicleNetworkSnapM?.takeIf { it.isFinite() && it >= 0.0 } ?: return false
        val threshold = when (vehicle) {
            TripVehicle.CAR -> WRONG_NETWORK_SNAP_CAR_M
            TripVehicle.BIKE -> WRONG_NETWORK_SNAP_BIKE_M
            TripVehicle.WALK -> WRONG_NETWORK_SNAP_WALK_M
        }
        if (snap < threshold) return false
        return offRouteMeters >= WRONG_NETWORK_MIN_OFF_ROUTE_M
    }

    /**
     * Index of a known alternate the driver is near that does **not** finish at the destination.
     */
    fun findKnownAlternateMissingDestination(
        knownAlternates: List<List<Pair<Double, Double>>>,
        activeRoute: List<Pair<Double, Double>>,
        driverLat: Double,
        driverLon: Double,
        destLat: Double,
        destLon: Double,
        corridorM: Double = KNOWN_ALTERNATE_CORRIDOR_M,
    ): Int? {
        if (!driverLat.isFinite() || !driverLon.isFinite()) return null
        var bestIdx: Int? = null
        var bestDist = Double.MAX_VALUE
        knownAlternates.forEachIndexed { index, alt ->
            if (alt.size < 2) return@forEachIndexed
            if (routesLookAlike(alt, activeRoute)) return@forEachIndexed
            if (routeReachesDestination(alt, destLat, destLon)) return@forEachIndexed
            val progress = routeProgress(alt, driverLat, driverLon) ?: return@forEachIndexed
            if (progress.offRouteMeters > corridorM) return@forEachIndexed
            if (progress.offRouteMeters < bestDist) {
                bestDist = progress.offRouteMeters
                bestIdx = index
            }
        }
        return bestIdx
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
