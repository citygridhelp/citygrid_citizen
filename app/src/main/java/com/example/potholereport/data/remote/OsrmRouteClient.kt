package com.example.potholereport.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Trip travel mode. Maps to an OSRM routing profile. */
enum class TripVehicle(val osrmProfile: String, val label: String) {
    CAR("driving", "Car"),
    BIKE("bike", "Bike"),
}

/**
 * Fetches a route polyline from the public OSRM demo server.
 * Falls back to empty on network / parse errors.
 *
 * Note: the public demo (`router.project-osrm.org`) only hosts the `driving`
 * profile, so [TripVehicle.BIKE] falls back to driving geometry.
 */
object OsrmRouteClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val http by lazy { HttpClient(Android) }

    suspend fun fetchRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle = TripVehicle.CAR,
    ): List<Pair<Double, Double>> {
        if (!startLat.isFinite() || !startLon.isFinite() || !endLat.isFinite() || !endLon.isFinite()) {
            return emptyList()
        }
        val requested = fetchForProfile(startLat, startLon, endLat, endLon, vehicle.osrmProfile)
        if (requested.isNotEmpty()) return requested
        // Public OSRM demo only serves "driving"; fall back so bike still routes.
        if (vehicle != TripVehicle.CAR) {
            return fetchForProfile(startLat, startLon, endLat, endLon, TripVehicle.CAR.osrmProfile)
        }
        return emptyList()
    }

    private suspend fun fetchForProfile(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        profile: String,
    ): List<Pair<Double, Double>> {
        val url =
            "https://router.project-osrm.org/route/v1/$profile/" +
                "$startLon,$startLat;$endLon,$endLat?overview=full&geometries=geojson"
        return try {
            val body = http.get(url).bodyAsText()
            val parsed = json.decodeFromString<OsrmRouteResponse>(body)
            val coords = parsed.routes.firstOrNull()?.geometry?.coordinates.orEmpty()
            coords.map { (lon, lat) -> lat to lon }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Serializable
private data class OsrmRouteResponse(
    val routes: List<OsrmRoute> = emptyList(),
)

@Serializable
private data class OsrmRoute(
    val geometry: OsrmGeometry = OsrmGeometry(),
)

@Serializable
private data class OsrmGeometry(
    val coordinates: List<List<Double>> = emptyList(),
)
