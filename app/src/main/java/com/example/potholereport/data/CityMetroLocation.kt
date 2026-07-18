package com.example.potholereport.data

import android.location.Location
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.util.Locale

/**
 * GPS metro detection and report eligibility: citizens may browse any city but may
 * only submit a report when [selectedCity] matches their physical [gpsMetroCity].
 */
object CityMetroLocation {

    private val dynamicMetroBounds: MutableMap<String, BoundingBox> = mutableMapOf()
    private val dynamicCityCenters: MutableMap<String, GeoPoint> = mutableMapOf()

    fun registerDynamicCityCenter(cityKey: String, lat: Double, lon: Double) {
        if (lat.isNaN() || lon.isNaN()) return
        val key = CityMetroKeys.canonical(cityKey)
        dynamicCityCenters[key] = GeoPoint(lat, lon)
        dynamicMetroBounds[key] = IndiaCityMapCatalog.defaultBoundsAround(GeoPoint(lat, lon))
    }

    fun cityCenterFor(cityKey: String): GeoPoint? {
        val key = CityMetroKeys.canonical(cityKey)
        dynamicCityCenters[key]?.let { return it }
        return IndiaCityMapCatalog.centerFor(key)
    }

    fun metroBoundsFor(cityKey: String): BoundingBox? {
        val key = CityMetroKeys.canonical(cityKey)
        dynamicMetroBounds[key]?.let { return it }
        return IndiaCityMapCatalog.metroBoundsFor(key)
    }

    /** Metro key whose boundary contains the coordinates, or null if outside supported metros. */
    fun resolveMetroCity(latitude: Double, longitude: Double): String? {
        for ((city, bbox) in dynamicMetroBounds) {
            if (coordinatesInMetroCity(city, latitude, longitude)) return city
        }
        for (city in IndiaCityMapCatalog.allCityKeys()) {
            if (coordinatesInMetroCity(city, latitude, longitude)) {
                return CityMetroKeys.canonical(city)
            }
        }
        return null
    }

    fun resolveMetroCity(location: Location): String? =
        resolveMetroCity(location.latitude, location.longitude)

    fun coordinatesInMetroCity(cityKey: String, latitude: Double, longitude: Double): Boolean {
        val key = CityMetroKeys.canonical(cityKey)
        if (key == "BENGALURU" && BengaluruGbaBoundary.isInitialized()) {
            return BengaluruGbaBoundary.contains(latitude, longitude)
        }
        val bbox = metroBoundsFor(key) ?: return false
        return bbox.contains(latitude, longitude)
    }

    fun canReportInCity(selectedCity: String, gpsMetroCity: String?): Boolean {
        if (selectedCity.isBlank() || gpsMetroCity.isNullOrBlank()) return false
        return CityMetroKeys.matches(selectedCity, gpsMetroCity)
    }

    fun reportBlockedMessage(selectedCity: String, gpsMetroCity: String?): String? {
        if (canReportInCity(selectedCity, gpsMetroCity)) return null
        return when {
            gpsMetroCity.isNullOrBlank() ->
                "Enable GPS and stand in a supported city to report a pothole."
            selectedCity.isBlank() ->
                "Waiting for your location…"
            else ->
                "You are in ${formatDisplayName(gpsMetroCity)}. " +
                    "Change city to ${formatDisplayName(gpsMetroCity)} to report here."
        }
    }

    /**
     * Citizen-facing error when report coordinates cannot be submitted under [cityKey],
     * or null when the point lies inside that city's metro boundary.
     */
    fun validateSubmitLocation(cityKey: String, latitude: Double, longitude: Double): String? {
        val city = CityMetroKeys.canonical(cityKey)
        if (!CityLaunchConfig.isCityEnabled(city)) {
            return "Reporting is not yet available for ${formatDisplayName(city)}."
        }
        if (coordinatesInMetroCity(city, latitude, longitude)) return null

        val detectedMetro = resolveMetroCity(latitude, longitude)
        return when {
            detectedMetro != null ->
                "This location is in ${formatDisplayName(detectedMetro)}, not ${formatDisplayName(city)}. " +
                    "Go back home, select ${formatDisplayName(detectedMetro)}, and open New Report again."
            else ->
                "This location is outside the ${formatDisplayName(city)} area. " +
                    "Move inside the city boundary or enter coordinates within ${formatDisplayName(city)}."
        }
    }

    fun formatDisplayName(cityKey: String): String {
        if (cityKey.isBlank()) return "Detecting location…"
        return cityKey.trim()
            .split(' ', '_')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
    }
}
