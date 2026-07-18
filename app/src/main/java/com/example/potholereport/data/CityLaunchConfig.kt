package com.example.potholereport.data

/**
 * Cities where reporting, municipal officer routing, and full map features are live.
 * Other cities appear in the picker as "Coming soon" until official directories are verified.
 */
object CityLaunchConfig {

    const val PRIMARY_CITY = "BENGALURU"

    private val enabledCities: Set<String> = setOf(PRIMARY_CITY)

    fun isCityEnabled(cityKey: String): Boolean =
        CityMetroKeys.canonical(cityKey) in enabledCities

    fun enabledCities(): Set<String> = enabledCities.toSet()
}
