package com.example.potholereport.data

import java.util.Locale

/**
 * Normalizes city labels from geocoders / user input to the metro keys used in the app.
 * Fixes mismatches like "BANGALORE" vs "BENGALURU" that break map bounds and recent reports.
 */
object CityMetroKeys {

    /** Nav-route fallback only; live city selection comes from GPS or user picker. */
    const val NAV_FALLBACK_CITY = "BENGALURU"

    @Deprecated("Use NAV_FALLBACK_CITY or GPS-resolved city", ReplaceWith("NAV_FALLBACK_CITY"))
    const val DEFAULT_CITY = NAV_FALLBACK_CITY

    private val aliases: Map<String, String> = mapOf(
        "BENGALURU" to "BENGALURU",
        "BANGALORE" to "BENGALURU",
        "BENGALURU URBAN" to "BENGALURU",
        "MUMBAI" to "MUMBAI",
        "BOMBAY" to "MUMBAI",
        "DELHI" to "DELHI",
        "NEW DELHI" to "DELHI",
        "CHENNAI" to "CHENNAI",
        "MADRAS" to "CHENNAI",
        "HYDERABAD" to "HYDERABAD",
        "KOLKATA" to "KOLKATA",
        "CALCUTTA" to "KOLKATA",
        "PUNE" to "PUNE",
        "PUNA" to "PUNE",
        "AHMEDABAD" to "AHMEDABAD",
        "JAIPUR" to "JAIPUR",
        "LUCKNOW" to "LUCKNOW",
        "GURUGRAM" to "GURUGRAM",
        "GURGAON" to "GURUGRAM",
        "MYSORE" to "MYSURU",
        "MYSURU" to "MYSURU",
        "PRAYAGRAJ" to "ALLAHABAD",
        "ALLAHABAD" to "ALLAHABAD",
    )

    fun canonical(raw: String): String {
        val normalized = raw.trim().replace(Regex("\\s+"), " ").uppercase(Locale.US)
        return aliases[normalized] ?: normalized
    }

    fun matches(cityA: String, cityB: String): Boolean = canonical(cityA) == canonical(cityB)
}
