package com.example.potholereport.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One user may report the same physical pothole only once until it is resolved (COMPLETED).
 *
 * Radius is ~30 ft (**9.144 m**): nearby distinct holes stay reportable, while GPS jitter
 * at the same spot still collides. When GPS accuracy is worse than the base radius, the
 * effective radius expands so a coarse fix cannot bypass an open report a few meters away.
 *
 * MAJOR FIELD-TUNABLE: [SAME_POTHOLE_RADIUS_METERS] will be adjusted after more outdoor
 * field tests — treat changes here as a product calibration, not a casual tweak.
 */
object PotholeDuplicateGuard {

    /**
     * Base same-pothole radius (meters) = **30 feet**.
     * FIELD CALIBRATION — Jul 2026: 40 → 25 → 30 m → 15.24 m (50 ft) → 12.192 m (40 ft)
     * → **9.144 m (30 ft)**.
     */
    const val SAME_POTHOLE_RADIUS_METERS = 9.144

    /** Cap so a terrible GPS fix does not block an entire street (~60 ft). */
    const val MAX_DUPLICATE_RADIUS_METERS = 18.288

    /** Integer meters for user-facing copy (always keep UI in sync with [SAME_POTHOLE_RADIUS_METERS]). */
    fun userFacingRadiusMeters(): Int = SAME_POTHOLE_RADIUS_METERS.toInt()

    fun effectiveRadiusMeters(gpsAccuracyM: Float?): Double {
        if (gpsAccuracyM == null || !gpsAccuracyM.isFinite() || gpsAccuracyM <= 0f) {
            return SAME_POTHOLE_RADIUS_METERS
        }
        return max(SAME_POTHOLE_RADIUS_METERS, gpsAccuracyM.toDouble() * 1.25)
            .coerceAtMost(MAX_DUPLICATE_RADIUS_METERS)
    }

    fun findActiveDuplicate(
        reports: List<PersistedPotholeReport>,
        reporterUserId: String,
        cityKey: String,
        latitude: Double,
        longitude: Double,
        gpsAccuracyM: Float? = null,
    ): PersistedPotholeReport? {
        if (reporterUserId.isBlank() || latitude.isNaN() || longitude.isNaN()) return null
        val radius = effectiveRadiusMeters(gpsAccuracyM)
        return reports.firstOrNull { report ->
            report.reporterUserId == reporterUserId &&
                report.cityKey == cityKey &&
                report.hasCoordinates() &&
                report.status.blocksNewReportAtSamePlace() &&
                distanceMeters(latitude, longitude, report.latitude, report.longitude) <= radius
        }
    }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusM = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusM * c
    }
}

fun PotholeReportStatus.blocksNewReportAtSamePlace(): Boolean = when (this) {
    PotholeReportStatus.OPEN, PotholeReportStatus.IN_PROGRESS -> true
    PotholeReportStatus.COMPLETED -> false
}
