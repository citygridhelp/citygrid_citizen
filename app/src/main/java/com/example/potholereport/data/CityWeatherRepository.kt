package com.example.potholereport.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Today's temperature and rain outlook for Indian cities (Open-Meteo, no API key).
 */
object CityWeatherRepository {

    private val cityCoordinates: Map<String, Pair<Double, Double>> = mapOf(
        "BENGALURU" to (12.9716 to 77.5946),
        "MUMBAI" to (19.0760 to 72.8777),
        "DELHI" to (28.6139 to 77.2090),
        "CHENNAI" to (13.0827 to 80.2707),
        "HYDERABAD" to (17.3850 to 78.4867),
        "KOLKATA" to (22.5726 to 88.3639),
        "PUNE" to (18.5204 to 73.8567),
        "AHMEDABAD" to (23.0225 to 72.5714),
        "JAIPUR" to (26.9124 to 75.7873),
        "LUCKNOW" to (26.8467 to 80.9462),
    )

    private const val CACHE_TTL_MS = 30 * 60 * 1000L

    private val cache = ConcurrentHashMap<String, Pair<CityWeatherForecast, Long>>()

    fun fetchToday(cityKey: String): CityWeatherForecast? {
        val key = cityKey.uppercase()
        val now = System.currentTimeMillis()
        cache[key]?.let { (forecast, cachedAt) ->
            if (now - cachedAt < CACHE_TTL_MS) return forecast
        }
        val coords = cityCoordinates[key] ?: return null
        val forecast = fetchFromOpenMeteo(key, coords.first, coords.second) ?: return null
        cache[key] = forecast to now
        return forecast
    }

    private fun fetchFromOpenMeteo(cityKey: String, lat: Double, lon: Double): CityWeatherForecast? {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code" +
                "&daily=weather_code,precipitation_sum,precipitation_probability_max" +
                "&forecast_days=1&timezone=Asia%2FKolkata",
        )
        return try {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 12_000
                requestMethod = "GET"
            }
            connection.inputStream.bufferedReader().use { reader ->
                parseResponse(cityKey, JSONObject(reader.readText()))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseResponse(cityKey: String, root: JSONObject): CityWeatherForecast? {
        val current = root.optJSONObject("current") ?: return null
        val daily = root.optJSONObject("daily") ?: return null
        val temp = current.optDouble("temperature_2m", Double.NaN)
        if (temp.isNaN()) return null

        val codes = daily.optJSONArray("weather_code") ?: return null
        val rainMm = daily.optJSONArray("precipitation_sum") ?: return null
        val rainChance = daily.optJSONArray("precipitation_probability_max") ?: return null
        if (codes.length() == 0) return null

        val todayCode = codes.optInt(0, current.optInt("weather_code", 0))
        val todayRainMm = rainMm.optDouble(0, 0.0)
        val todayChance = rainChance.optInt(0, 0).coerceIn(0, 100)
        val criticality = RainCriticality.fromDailyForecast(todayRainMm, todayCode)

        return CityWeatherForecast(
            cityKey = cityKey,
            temperatureC = temp,
            conditionLabel = wmoCodeToLabel(todayCode),
            rainCriticality = criticality,
            precipitationChancePercent = todayChance,
            expectedRainMm = todayRainMm,
        )
    }

    private fun wmoCodeToLabel(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Rain showers"
        in 85..86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Variable"
    }
}
