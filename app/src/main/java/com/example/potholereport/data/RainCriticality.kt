package com.example.potholereport.data

enum class RainCriticality(val label: String) {
    NONE("No rain"),
    LIGHT("Light"),
    MEDIUM("Medium"),
    HEAVY("Heavy"),
    STORM("Storm"),
    ;

    companion object {
        fun fromDailyForecast(precipitationMm: Double, weatherCode: Int): RainCriticality {
            if (weatherCode in STORM_WEATHER_CODES) return STORM
            return when {
                precipitationMm <= 0.1 -> NONE
                precipitationMm <= 2.5 -> LIGHT
                precipitationMm <= 10.0 -> MEDIUM
                precipitationMm <= 30.0 -> HEAVY
                else -> STORM
            }
        }

        private val STORM_WEATHER_CODES = setOf(95, 96, 99)
    }
}
