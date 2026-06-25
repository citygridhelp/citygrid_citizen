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

    /** Metro key whose bounding box contains the coordinates, or null if outside supported metros. */
    fun resolveMetroCity(latitude: Double, longitude: Double): String? {
        for ((city, bbox) in dynamicMetroBounds) {
            if (bbox.contains(latitude, longitude)) return city
        }
        for (city in IndiaCityMapCatalog.allCityKeys()) {
            val bbox = metroBoundsFor(city) ?: continue
            if (bbox.contains(latitude, longitude)) {
                return CityMetroKeys.canonical(city)
            }
        }
        return null
    }

    fun resolveMetroCity(location: Location): String? =
        resolveMetroCity(location.latitude, location.longitude)

    fun coordinatesInMetroCity(cityKey: String, latitude: Double, longitude: Double): Boolean {
        val bbox = metroBoundsFor(cityKey) ?: return false
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
