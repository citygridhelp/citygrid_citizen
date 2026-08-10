package com.example.potholereport.data

/**
 * Rain-risk map horizon chips (Phase A/B weather overlay).
 */
enum class WeatherHorizon(val chipLabel: String) {
    NEXT_1H("1h"),
    NEXT_3H("3h"),
    TODAY("Today"),
    ;
}
