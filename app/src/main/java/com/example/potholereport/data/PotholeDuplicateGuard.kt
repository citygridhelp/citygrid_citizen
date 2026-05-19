package com.example.potholereport.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One user may report the same physical pothole only once until it is resolved (COMPLETED).
 */
object PotholeDuplicateGuard {

    /** ~40 m — same pothole / GPS jitter; avoids blocking the opposite lane. */
    const val SAME_POTHOLE_RADIUS_METERS = 40.0

    fun findActiveDuplicate(
        reports: List<PersistedPotholeReport>,
        reporterUserId: String,
        cityKey: String,
        latitude: Double,
        longitude: Double,
    ): PersistedPotholeReport? {
        if (reporterUserId.isBlank() || latitude.isNaN() || longitude.isNaN()) return null
        return reports.firstOrNull { report ->
            report.reporterUserId == reporterUserId &&
                report.cityKey == cityKey &&
                report.hasCoordinates() &&
                report.status.blocksNewReportAtSamePlace() &&
                distanceMeters(latitude, longitude, report.latitude, report.longitude) <=
                SAME_POTHOLE_RADIUS_METERS
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
