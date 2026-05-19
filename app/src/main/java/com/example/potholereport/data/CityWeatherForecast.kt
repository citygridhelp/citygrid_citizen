package com.example.potholereport.data

import kotlin.math.roundToInt

data class CityWeatherForecast(
    val cityKey: String,
    val temperatureC: Double,
    val conditionLabel: String,
    val rainCriticality: RainCriticality,
    val precipitationChancePercent: Int,
    val expectedRainMm: Double,
) {
    val summaryLine: String
        get() {
            val temp = "${temperatureC.roundToInt()}°C"
            val rainPart = when (rainCriticality) {
                RainCriticality.NONE -> "Rain: none expected today"
                else -> {
                    val chance = if (precipitationChancePercent > 0) {
                        " · ${precipitationChancePercent}% chance"
                    } else {
                        ""
                    }
                    "Rain: ${rainCriticality.label}$chance"
                }
            }
            return "$temp · $conditionLabel · $rainPart"
        }
}
