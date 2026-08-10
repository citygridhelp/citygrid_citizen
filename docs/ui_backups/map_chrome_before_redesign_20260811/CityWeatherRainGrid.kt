package com.example.potholereport.data

import android.graphics.Color as AndroidColor
import kotlin.math.roundToInt

/**
 * One metro grid cell for the rain-risk map overlay (Open-Meteo sample at cell center).
 */
data class CityWeatherRainCell(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    /** 0 = dry … 1 = high rain risk for the selected horizon. */
    val risk01: Float,
    val precipChancePercent: Int,
    val precipMm: Double,
) {
    val ringLatLon: List<Pair<Double, Double>>
        get() = listOf(
            south to west,
            south to east,
            north to east,
            north to west,
        )
}

data class CityWeatherRainGrid(
    val cityKey: String,
    val horizon: WeatherHorizon,
    val cells: List<CityWeatherRainCell>,
)

object CityWeatherRainColors {
    /** Soft blue–green choropleth; dry cells stay fully transparent. */
    fun riskFillArgb(risk01: Float): Int {
        if (risk01 <= 0.05f) {
            return AndroidColor.TRANSPARENT
        }
        val t = risk01.coerceIn(0.08f, 1f)
        val r = (150 - (150 - 40) * t).roundToInt().coerceIn(40, 150)
        val g = (210 - (210 - 120) * t).roundToInt().coerceIn(120, 210)
        val b = (200 + (255 - 200) * t).roundToInt().coerceIn(200, 255)
        val a = (48 + (130 * t)).roundToInt().coerceIn(48, 176)
        return AndroidColor.argb(a, r, g, b)
    }

    fun riskFillRgbaCss(risk01: Float): String {
        val argb = riskFillArgb(risk01)
        val a = AndroidColor.alpha(argb) / 255f
        if (a <= 0.01f) return "rgba(0,0,0,0)"
        return "rgba(${AndroidColor.red(argb)},${AndroidColor.green(argb)},${AndroidColor.blue(argb)},${"%.2f".format(a)})"
    }
}
