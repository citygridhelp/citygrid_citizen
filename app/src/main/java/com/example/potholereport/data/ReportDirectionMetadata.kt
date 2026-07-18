package com.example.potholereport.data

import kotlin.math.abs

/**
 * Traffic flow relative to the wide-shot camera at report time.
 * [UNKNOWN] still allows submit; trip nav may hide left/right side.
 */
enum class TrafficFacingMode(val storageKey: String) {
    UNKNOWN(""),
    FACING_CAMERA("FACING"),
    AWAY_FROM_CAMERA("AWAY"),
    ;

    companion object {
        fun fromStorage(key: String?): TrafficFacingMode =
            entries.find { it.storageKey == key?.trim() } ?: UNKNOWN
    }
}

/**
 * Computes traffic bearing (degrees, 0–360) from device heading and user confirmation.
 */
fun resolveTrafficBearingDeg(
    reportBearingDeg: Float?,
    trafficFacing: TrafficFacingMode,
): Float? {
    val base = reportBearingDeg?.takeIf { it.isFinite() } ?: return null
    val normalized = ((base % 360f) + 360f) % 360f
    return when (trafficFacing) {
        TrafficFacingMode.FACING_CAMERA -> normalized
        TrafficFacingMode.AWAY_FROM_CAMERA -> (normalized + 180f) % 360f
        TrafficFacingMode.UNKNOWN -> null
    }
}

/**
 * Maps reporter lane (L/M/R) to driver lane given bearings.
 * Returns null when metadata is insufficient.
 */
fun resolveDriverLaneLabel(
    potholePosition: PotholePosition,
    reportBearingDeg: Float?,
    trafficBearingDeg: Float?,
    driverBearingDeg: Float?,
): String? {
    val traffic = trafficBearingDeg?.takeIf { it.isFinite() } ?: return null
    val driver = driverBearingDeg?.takeIf { it.isFinite() } ?: return null
    val opposite = abs(normalizeBearingDelta(traffic - driver)) > 90f
    val lane = when (potholePosition) {
        PotholePosition.LEFT -> if (opposite) PotholePosition.RIGHT else PotholePosition.LEFT
        PotholePosition.RIGHT -> if (opposite) PotholePosition.LEFT else PotholePosition.RIGHT
        PotholePosition.MIDDLE -> PotholePosition.MIDDLE
    }
    return lane.displayLabel
}

fun normalizeBearingDelta(delta: Float): Float {
    var d = delta
    while (d > 180f) d -= 360f
    while (d < -180f) d += 360f
    return d
}

fun bearingBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val dLon = Math.toRadians(lon2 - lon1)
    val lat1r = Math.toRadians(lat1)
    val lat2r = Math.toRadians(lat2)
    val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2r)
    val x = kotlin.math.cos(lat1r) * kotlin.math.sin(lat2r) -
        kotlin.math.sin(lat1r) * kotlin.math.cos(lat2r) * kotlin.math.cos(dLon)
    val brng = Math.toDegrees(kotlin.math.atan2(y, x))
    return ((brng % 360.0) + 360.0).toFloat() % 360f
}
