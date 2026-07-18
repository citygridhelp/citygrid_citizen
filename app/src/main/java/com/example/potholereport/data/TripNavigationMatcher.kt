package com.example.potholereport.data

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

object TripNavigationMatcher {

    /** Max perpendicular distance from route polyline to count a report (meters). */
    const val ROUTE_CORRIDOR_M = 28.0

    /** Max distance along route to show alerts (meters). */
    const val MAX_AHEAD_M = 2_500.0

    /** Minimum GPS accuracy to show lane side (meters). */
    const val SIDE_MIN_ACCURACY_M = 25f

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
            val side = if (gpsAccuracyM != null && gpsAccuracyM <= SIDE_MIN_ACCURACY_M) {
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
