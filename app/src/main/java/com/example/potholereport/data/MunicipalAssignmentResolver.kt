package com.example.potholereport.data

/**
 * Assigns each pothole report to the nearest municipal zone officer
 * based on GPS coordinates and city.
 */
object MunicipalAssignmentResolver {

    fun resolve(cityKey: String, latitude: Double, longitude: Double): MunicipalAssignee {
        val metro = CityMetroKeys.canonical(cityKey)
        if (!CityLaunchConfig.isCityEnabled(metro)) {
            return MunicipalContactsRegistry.fallbackForCity(CityLaunchConfig.PRIMARY_CITY)
        }
        if (latitude.isFinite() && longitude.isFinite()) {
            MunicipalContactsRegistry.nearestAssignee(metro, latitude, longitude)?.let { return it }
        }
        return MunicipalContactsRegistry.fallbackForCity(metro)
    }

    fun resolveForReport(report: PersistedPotholeReport): MunicipalAssignee =
        resolve(report.cityKey, report.latitude, report.longitude)
}
