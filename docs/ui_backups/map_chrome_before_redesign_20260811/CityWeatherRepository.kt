package com.example.potholereport.data

import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.BoundingBox
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * City weather via Open-Meteo (no API key).
 * - [fetchToday]: home subline (point forecast)
 * - [fetchRainGrid]: map rain-risk overlay (metro grid + 1h / 3h / Today)
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
    /** Short TTL so the map can pick up the next forecast hour while Weather stays on. */
    private const val GRID_CACHE_TTL_MS = 10 * 60 * 1000L
    private const val GRID_ROWS = 5
    private const val GRID_COLS = 5
    private val IST = ZoneId.of("Asia/Kolkata")
    private val HOUR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    private val CACHE_HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMddHH")

    private val cache = ConcurrentHashMap<String, Pair<CityWeatherForecast, Long>>()
    private val gridCache = ConcurrentHashMap<String, Pair<CityWeatherRainGrid, Long>>()

    fun supportsWeatherMap(cityKey: String): Boolean {
        val key = CityMetroKeys.canonical(cityKey)
        if (key.isBlank()) return false
        return rainMapBounds(key) != null
    }

    fun fetchToday(cityKey: String): CityWeatherForecast? {
        val key = CityMetroKeys.canonical(cityKey)
        if (key.isBlank()) return null
        val now = System.currentTimeMillis()
        cache[key]?.let { (forecast, cachedAt) ->
            if (now - cachedAt < CACHE_TTL_MS) return forecast
        }
        val coords = cityCoordinates[key] ?: IndiaCityMapCatalog.centerFor(key)?.let {
            it.latitude to it.longitude
        } ?: return null
        val forecast = fetchPointToday(key, coords.first, coords.second) ?: return null
        cache[key] = forecast to now
        return forecast
    }

    /**
     * Metro rain-risk grid for map overlay. Samples a 5×5 grid in one Open-Meteo multi-point call.
     * @param forceRefresh bypass cache (used by the UI hourly refresh loop).
     */
    fun fetchRainGrid(
        cityKey: String,
        horizon: WeatherHorizon,
        forceRefresh: Boolean = false,
    ): CityWeatherRainGrid? {
        val key = CityMetroKeys.canonical(cityKey)
        if (key.isBlank()) return null
        // Bust cache when the clock hour changes so 1h/3h windows advance.
        val hourStamp = LocalDateTime.now(IST).format(CACHE_HOUR_FMT)
        val cacheKey = "$key|${horizon.name}|$hourStamp"
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            gridCache[cacheKey]?.let { (grid, cachedAt) ->
                if (now - cachedAt < GRID_CACHE_TTL_MS) return grid
            }
        }
        val bbox = rainMapBounds(key) ?: return null
        val specs = buildGridCells(bbox, GRID_ROWS, GRID_COLS)
        if (specs.isEmpty()) return null
        val cells = fetchGridFromOpenMeteo(specs, horizon) ?: return null
        val grid = CityWeatherRainGrid(cityKey = key, horizon = horizon, cells = cells)
        gridCache[cacheKey] = grid to now
        // Drop stale hour keys for this city/horizon to avoid unbounded growth.
        val prefix = "$key|${horizon.name}|"
        gridCache.keys.filter { it.startsWith(prefix) && it != cacheKey }.forEach { gridCache.remove(it) }
        return grid
    }

    private fun rainMapBounds(cityKey: String): BoundingBox? {
        IndiaCityMapCatalog.metroBoundsFor(cityKey)?.let { return it }
        if (cityKey == "BENGALURU" && BengaluruGbaBoundary.isInitialized()) {
            BengaluruGbaBoundary.boundingBox()?.let { return it }
        }
        val center = cityCoordinates[cityKey] ?: IndiaCityMapCatalog.centerFor(cityKey)?.let {
            it.latitude to it.longitude
        } ?: return null
        val lat = center.first
        val lon = center.second
        val dLat = 0.18
        val dLon = 0.18
        return BoundingBox(lat + dLat, lon + dLon, lat - dLat, lon - dLon)
    }

    private data class GridSpec(
        val south: Double,
        val west: Double,
        val north: Double,
        val east: Double,
        val centerLat: Double,
        val centerLon: Double,
    )

    private fun buildGridCells(bbox: BoundingBox, rows: Int, cols: Int): List<GridSpec> {
        val south = bbox.latSouth
        val north = bbox.latNorth
        val west = bbox.lonWest
        val east = bbox.lonEast
        if (north <= south || east <= west) return emptyList()
        val out = ArrayList<GridSpec>(rows * cols)
        val dLat = (north - south) / rows
        val dLon = (east - west) / cols
        for (r in 0 until rows) {
            val s = south + r * dLat
            val n = if (r == rows - 1) north else south + (r + 1) * dLat
            for (c in 0 until cols) {
                val w = west + c * dLon
                val e = if (c == cols - 1) east else west + (c + 1) * dLon
                out += GridSpec(
                    south = s,
                    west = w,
                    north = n,
                    east = e,
                    centerLat = (s + n) / 2.0,
                    centerLon = (w + e) / 2.0,
                )
            }
        }
        return out
    }

    private fun fetchGridFromOpenMeteo(
        specs: List<GridSpec>,
        horizon: WeatherHorizon,
    ): List<CityWeatherRainCell>? {
        val latParam = specs.joinToString(",") { "%.4f".format(it.centerLat) }
        val lonParam = specs.joinToString(",") { "%.4f".format(it.centerLon) }
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latParam&longitude=$lonParam" +
                "&hourly=precipitation_probability,precipitation" +
                "&daily=precipitation_sum,precipitation_probability_max" +
                // Two days so NEXT_1H / NEXT_3H still work past midnight.
                "&forecast_days=2&timezone=${URLEncoder.encode("Asia/Kolkata", "UTF-8")}",
        )
        return try {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 18_000
                readTimeout = 18_000
                requestMethod = "GET"
            }
            val body = connection.inputStream.bufferedReader().use { it.readText().trim() }
            val forecasts = parseMultiForecast(body) ?: return null
            if (forecasts.size != specs.size) return null
            specs.mapIndexed { index, spec ->
                val metrics = rainMetrics(forecasts[index], horizon)
                CityWeatherRainCell(
                    south = spec.south,
                    west = spec.west,
                    north = spec.north,
                    east = spec.east,
                    risk01 = riskScore(metrics.chancePercent, metrics.mm),
                    precipChancePercent = metrics.chancePercent,
                    precipMm = metrics.mm,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class RainMetrics(val chancePercent: Int, val mm: Double)

    private fun rainMetrics(root: JSONObject, horizon: WeatherHorizon): RainMetrics {
        if (horizon == WeatherHorizon.TODAY) {
            // Remaining hours today (not the whole calendar day including past rain).
            val hourly = root.optJSONObject("hourly") ?: return RainMetrics(0, 0.0)
            val times = hourly.optJSONArray("time") ?: return RainMetrics(0, 0.0)
            val probs = hourly.optJSONArray("precipitation_probability") ?: return RainMetrics(0, 0.0)
            val mmArr = hourly.optJSONArray("precipitation") ?: return RainMetrics(0, 0.0)
            val start = currentHourIndex(times)
            val today = LocalDateTime.now(IST).toLocalDate()
            var maxChance = 0
            var sumMm = 0.0
            for (i in start until times.length()) {
                val raw = times.optString(i, "") ?: break
                val t = runCatching { LocalDateTime.parse(raw, HOUR_FMT) }.getOrNull()
                    ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()
                    ?: break
                if (t.toLocalDate() != today) break
                maxChance = max(maxChance, probs.optInt(i, 0))
                sumMm += mmArr.optDouble(i, 0.0)
            }
            return RainMetrics(maxChance.coerceIn(0, 100), sumMm)
        }
        val hourly = root.optJSONObject("hourly") ?: return RainMetrics(0, 0.0)
        val times = hourly.optJSONArray("time") ?: return RainMetrics(0, 0.0)
        val probs = hourly.optJSONArray("precipitation_probability") ?: return RainMetrics(0, 0.0)
        val mmArr = hourly.optJSONArray("precipitation") ?: return RainMetrics(0, 0.0)
        val start = currentHourIndex(times)
        val hours = when (horizon) {
            WeatherHorizon.NEXT_1H -> 1
            WeatherHorizon.NEXT_3H -> 3
            WeatherHorizon.TODAY -> 24
        }
        var maxChance = 0
        var sumMm = 0.0
        val end = min(times.length(), start + hours)
        for (i in start until end) {
            maxChance = max(maxChance, probs.optInt(i, 0))
            sumMm += mmArr.optDouble(i, 0.0)
        }
        return RainMetrics(maxChance.coerceIn(0, 100), sumMm)
    }

    private fun currentHourIndex(times: JSONArray): Int {
        if (times.length() == 0) return 0
        val nowFloor = LocalDateTime.now(IST).withMinute(0).withSecond(0).withNano(0)
        var best = 0
        for (i in 0 until times.length()) {
            val raw = times.optString(i, "") ?: continue
            val t = runCatching { LocalDateTime.parse(raw, HOUR_FMT) }.getOrNull()
                ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()
                ?: continue
            if (!t.isAfter(nowFloor)) {
                best = i
            } else {
                break
            }
        }
        return best
    }

    /**
     * Map fill intensity. Speculative chance with ~0 mm must not paint blue —
     * Open-Meteo often reports 20–40% with no precipitation.
     */
    private fun riskScore(chancePercent: Int, mm: Double): Float {
        if (mm < 0.15 && chancePercent < 50) return 0f
        if (mm < 0.05 && chancePercent < 70) return 0f
        val c = ((chancePercent - 40).coerceAtLeast(0) / 60f).coerceIn(0f, 1f)
        val m = (mm / 3.5).coerceIn(0.0, 1.0).toFloat()
        return (0.30f * c + 0.70f * m).coerceIn(0f, 1f)
    }

    private fun parseMultiForecast(body: String): List<JSONObject>? {
        if (body.isBlank()) return null
        return when {
            body.startsWith("[") -> {
                val arr = JSONArray(body)
                List(arr.length()) { i -> arr.getJSONObject(i) }
            }
            body.startsWith("{") -> listOf(JSONObject(body))
            else -> null
        }
    }

    private fun fetchPointToday(cityKey: String, lat: Double, lon: Double): CityWeatherForecast? {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code" +
                "&daily=weather_code,precipitation_sum,precipitation_probability_max" +
                "&forecast_days=1&timezone=${URLEncoder.encode("Asia/Kolkata", "UTF-8")}",
        )
        return try {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 12_000
                requestMethod = "GET"
            }
            connection.inputStream.bufferedReader().use { reader ->
                parseTodayResponse(cityKey, JSONObject(reader.readText()))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTodayResponse(cityKey: String, root: JSONObject): CityWeatherForecast? {
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
