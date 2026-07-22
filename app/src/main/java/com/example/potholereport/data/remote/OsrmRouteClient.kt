package com.example.potholereport.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Trip travel mode. Maps to an OSRM routing profile. */
enum class TripVehicle(
    val serverPath: String,
    val osrmProfile: String,
    val label: String,
) {
    CAR("routed-car", "driving", "Car"),
    BIKE("routed-bike", "cycling", "Bike"),
    WALK("routed-foot", "walking", "Walk"),
}

/** One turn / guidance step along a route. */
data class OsrmRouteStep(
    val instruction: String,
    val alongRouteMeters: Double,
    val stepDistanceMeters: Double,
    val maneuverType: String,
    val modifier: String,
    val roadName: String,
)

/** Route geometry plus travel estimates and turn steps from OSRM. */
data class OsrmRouteResult(
    val points: List<Pair<Double, Double>>,
    val durationSeconds: Double,
    val distanceMeters: Double,
    val steps: List<OsrmRouteStep> = emptyList(),
    /** How far OSRM moved the requested start onto the network (meters). */
    val startSnapMeters: Double = 0.0,
    /** How far OSRM moved the requested end onto the network (meters). */
    val endSnapMeters: Double = 0.0,
)

/**
 * Fetches mode-specific route geometry from the FOSSGIS public OSRM service.
 *
 * Bike uses the cycling profile first, but falls back to the car road network when the
 * cycle graph produces an impractical detour (common where OSM cycleways are sparse or
 * disconnected — e.g. no path from the snapped start back to the user).
 */
object OsrmRouteClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val http by lazy { HttpClient(Android) }
    private val requestMutex = Mutex()
    private var lastRequestAtMs = 0L

    /** Generous snap so GPS in a courtyard/park still finds a nearby road. */
    private const val SNAP_RADIUS_M = 500

    /** Bike vs car: if cycling is this much longer, prefer drivable roads. */
    internal const val BIKE_DETOUR_RATIO = 1.35

    /** Bike vs car: also fall back if absolute extra length exceeds this. */
    internal const val BIKE_DETOUR_EXTRA_M = 600.0

    /** Prefer car snap if bike start snap is farther than this and worse than car. */
    internal const val BIKE_BAD_SNAP_M = 90.0

    suspend fun fetchRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle = TripVehicle.CAR,
    ): OsrmRouteResult? {
        if (!startLat.isFinite() || !startLon.isFinite() || !endLat.isFinite() || !endLon.isFinite()) {
            return null
        }
        return when (vehicle) {
            TripVehicle.BIKE -> fetchBikeWithRoadFallback(startLat, startLon, endLat, endLon)
            else -> fetchPrimary(startLat, startLon, endLat, endLon, vehicle)
        }
    }

    private suspend fun fetchPrimary(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle,
    ): OsrmRouteResult? =
        fetchForProfile(startLat, startLon, endLat, endLon, vehicle, useRadiuses = true)
            ?: fetchForProfile(startLat, startLon, endLat, endLon, vehicle, useRadiuses = false)

    private suspend fun fetchBikeWithRoadFallback(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
    ): OsrmRouteResult? {
        val bike = fetchPrimary(startLat, startLon, endLat, endLon, TripVehicle.BIKE)
        val car = fetchPrimary(startLat, startLon, endLat, endLon, TripVehicle.CAR)
        return pickFeasibleBikeRoute(bike, car)
    }

    /**
     * Chooses cycling geometry when it is reasonable; otherwise uses the car road
     * network with a bike-speed ETA (typical for Indian cities with thin cycle graphs).
     */
    internal fun pickFeasibleBikeRoute(
        bike: OsrmRouteResult?,
        car: OsrmRouteResult?,
    ): OsrmRouteResult? {
        if (bike == null) return car?.let { asBikeEta(it) }
        if (car == null) return bike

        val detourTooLong =
            bike.distanceMeters > car.distanceMeters * BIKE_DETOUR_RATIO &&
                bike.distanceMeters - car.distanceMeters > BIKE_DETOUR_EXTRA_M

        val bikeSnapWorse =
            bike.startSnapMeters >= BIKE_BAD_SNAP_M &&
                bike.startSnapMeters > car.startSnapMeters + 40.0

        val bikeEndSnapWorse =
            bike.endSnapMeters >= BIKE_BAD_SNAP_M &&
                bike.endSnapMeters > car.endSnapMeters + 40.0

        return if (detourTooLong || bikeSnapWorse || bikeEndSnapWorse) {
            asBikeEta(car)
        } else {
            bike
        }
    }

    /** Re-tag a road network route with a cycling travel-time estimate (~16 km/h). */
    private fun asBikeEta(roadRoute: OsrmRouteResult): OsrmRouteResult {
        val bikeSeconds = (roadRoute.distanceMeters / 4.5).coerceAtLeast(60.0)
        return roadRoute.copy(durationSeconds = bikeSeconds)
    }

    private suspend fun fetchForProfile(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle,
        useRadiuses: Boolean,
    ): OsrmRouteResult? {
        val radiusQuery = if (useRadiuses) {
            "&radiuses=$SNAP_RADIUS_M;$SNAP_RADIUS_M"
        } else {
            ""
        }
        val url =
            "https://routing.openstreetmap.de/${vehicle.serverPath}/route/v1/${vehicle.osrmProfile}/" +
                "$startLon,$startLat;$endLon,$endLat" +
                "?overview=full&geometries=geojson&steps=true$radiusQuery"
        return throttledGet(url) { body ->
            val parsed = json.decodeFromString<OsrmRouteResponse>(body)
            if (!parsed.code.equals("Ok", ignoreCase = true) && parsed.routes.isEmpty()) {
                return@throttledGet null
            }
            val route = parsed.routes.firstOrNull() ?: return@throttledGet null
            val points = route.geometry.coordinates.mapNotNull { coordinate ->
                if (coordinate.size < 2) null else coordinate[1] to coordinate[0]
            }
            if (points.size < 2) return@throttledGet null
            val startSnap = parsed.waypoints.getOrNull(0)?.distance?.coerceAtLeast(0.0) ?: 0.0
            val endSnap = parsed.waypoints.getOrNull(1)?.distance?.coerceAtLeast(0.0) ?: 0.0
            OsrmRouteResult(
                points = points,
                durationSeconds = route.duration.coerceAtLeast(0.0),
                distanceMeters = route.distance.coerceAtLeast(0.0),
                steps = parseSteps(route.legs),
                startSnapMeters = startSnap,
                endSnapMeters = endSnap,
            )
        }
    }

    private suspend fun <T> throttledGet(url: String, parse: (String) -> T?): T? {
        return requestMutex.withLock {
            val waitMs = (400L - (System.currentTimeMillis() - lastRequestAtMs)).coerceAtLeast(0L)
            if (waitMs > 0L) delay(waitMs)
            try {
                val body = http.get(url) {
                    header(HttpHeaders.UserAgent, "CityGridCitizen/1.0 (citygridhelp@gmail.com)")
                }.bodyAsText()
                parse(body)
            } catch (_: Exception) {
                null
            } finally {
                lastRequestAtMs = System.currentTimeMillis()
            }
        }
    }

    private fun parseSteps(legs: List<OsrmRouteLeg>): List<OsrmRouteStep> {
        val out = mutableListOf<OsrmRouteStep>()
        var along = 0.0
        for (leg in legs) {
            for (step in leg.steps) {
                val type = step.maneuver?.type.orEmpty()
                val modifier = step.maneuver?.modifier.orEmpty()
                val road = step.name.trim()
                out.add(
                    OsrmRouteStep(
                        instruction = formatStepInstruction(type, modifier, road),
                        alongRouteMeters = along,
                        stepDistanceMeters = step.distance.coerceAtLeast(0.0),
                        maneuverType = type,
                        modifier = modifier,
                        roadName = road,
                    ),
                )
                along += step.distance.coerceAtLeast(0.0)
            }
        }
        return out
    }

    private fun formatStepInstruction(type: String, modifier: String, roadName: String): String {
        val onto = roadName.takeIf { it.isNotBlank() }?.let { " onto $it" }.orEmpty()
        val turnWord = when (modifier.lowercase()) {
            "left" -> "Turn left"
            "slight left" -> "Keep left"
            "sharp left" -> "Sharp left"
            "right" -> "Turn right"
            "slight right" -> "Keep right"
            "sharp right" -> "Sharp right"
            "straight" -> "Continue straight"
            "uturn", "u-turn" -> "Make a U-turn"
            else -> null
        }
        return when (type.lowercase()) {
            "depart" -> if (roadName.isNotBlank()) "Start on $roadName" else "Start"
            "arrive" -> "Arrive at destination"
            "turn", "end of road", "new name" ->
                (turnWord ?: "Continue") + onto
            "continue" ->
                if (roadName.isNotBlank()) "Continue on $roadName" else (turnWord ?: "Continue")
            "merge" -> "Merge" + onto
            "fork" -> when {
                modifier.contains("left", ignoreCase = true) -> "Take the left fork$onto"
                modifier.contains("right", ignoreCase = true) -> "Take the right fork$onto"
                else -> "Keep going at the fork$onto"
            }
            "ramp", "on ramp", "off ramp" ->
                (turnWord?.let { "$it for the ramp" } ?: "Take the ramp") + onto
            "roundabout", "rotary" ->
                "Enter the roundabout" + onto
            "notification" ->
                if (roadName.isNotBlank()) "Continue on $roadName" else "Continue"
            else ->
                (turnWord ?: "Continue") + onto
        }.trim()
    }
}

@Serializable
private data class OsrmRouteResponse(
    val code: String = "",
    val routes: List<OsrmRoute> = emptyList(),
    val waypoints: List<OsrmWaypoint> = emptyList(),
)

@Serializable
private data class OsrmWaypoint(
    val distance: Double = 0.0,
    val name: String = "",
    val location: List<Double> = emptyList(),
)

@Serializable
private data class OsrmRoute(
    val geometry: OsrmGeometry = OsrmGeometry(),
    val duration: Double = 0.0,
    val distance: Double = 0.0,
    val legs: List<OsrmRouteLeg> = emptyList(),
)

@Serializable
private data class OsrmRouteLeg(
    val steps: List<OsrmStep> = emptyList(),
)

@Serializable
private data class OsrmStep(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val name: String = "",
    val maneuver: OsrmManeuver? = null,
)

@Serializable
private data class OsrmManeuver(
    val type: String = "",
    val modifier: String = "",
)

@Serializable
private data class OsrmGeometry(
    val coordinates: List<List<Double>> = emptyList(),
)
