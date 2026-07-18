package com.example.potholereport.data.remote

import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.PotholePosition
import com.example.potholereport.data.TrafficFacingMode

/** Maps Supabase direction columns ↔ local report fields. */
fun RemoteReportRow.trafficFacingStorageKey(): String = when (trafficFacesCamera) {
    true -> TrafficFacingMode.FACING_CAMERA.storageKey
    false -> TrafficFacingMode.AWAY_FROM_CAMERA.storageKey
    null -> TrafficFacingMode.UNKNOWN.storageKey
}

fun trafficFacesCameraForStorage(trafficFacing: String): Boolean? = when (
    TrafficFacingMode.fromStorage(trafficFacing)
) {
    TrafficFacingMode.FACING_CAMERA -> true
    TrafficFacingMode.AWAY_FROM_CAMERA -> false
    TrafficFacingMode.UNKNOWN -> null
}

fun PersistedPotholeReport.withRemoteDirection(row: RemoteReportRow): PersistedPotholeReport {
    val pos = row.potholePosition.trim().ifBlank { potholePosition }
    val rb = row.reportBearingDeg ?: reportBearingDeg
    val tb = row.trafficBearingDeg ?: trafficBearingDeg
    val tf = row.trafficFacingStorageKey().ifBlank { trafficFacing }
    return copy(
        potholePosition = if (pos.isNotBlank()) pos else potholePosition,
        reportBearingDeg = if (row.reportBearingDeg != null) rb else reportBearingDeg,
        trafficBearingDeg = if (row.trafficBearingDeg != null) tb else trafficBearingDeg,
        trafficFacing = if (row.trafficFacesCamera != null) tf else trafficFacing,
    )
}

