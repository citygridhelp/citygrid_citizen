package com.example.potholereport.data

/**
 * Assigns each pothole report to the nearest municipal zone officer
 * based on GPS coordinates and city.
 */
object MunicipalAssignmentResolver {

    fun resolve(cityKey: String, latitude: Double, longitude: Double): MunicipalAssignee {
        if (latitude.isFinite() && longitude.isFinite()) {
            MunicipalContactsRegistry.nearestAssignee(cityKey, latitude, longitude)?.let { return it }
        }
        return MunicipalContactsRegistry.fallbackForCity(cityKey)
    }

    fun resolveForReport(report: PersistedPotholeReport): MunicipalAssignee =
        resolve(report.cityKey, report.latitude, report.longitude)
}
