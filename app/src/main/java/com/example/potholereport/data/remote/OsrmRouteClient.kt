package com.example.potholereport.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

    private val http by lazy {
        HttpClient(Android) {
            install(HttpTimeout) {
                // Fail fast offline / on flaky mobile data so nav UI is never stuck "Rerouting…".
                requestTimeoutMillis = 8_000
                connectTimeoutMillis = 4_000
                socketTimeoutMillis = 8_000
            }
        }
    }
    private val requestSemaphore = Semaphore(permits = 3)
    private var lastRequestAtMs = 0L

    /** Fallback snap when GPS accuracy is unknown (courtyard / coarse pin). */
    private const val DEFAULT_SNAP_RADIUS_M = 220

    /** Soft upper bound: 1 primary + up to 2 useful alternates (trust over quantity). */
    private const val MAX_ROUTE_OPTIONS = 3

    /** Max alternatives parameter accepted by FOSSGIS OSRM (higher → TooBig, zero routes). */
    private const val OSRM_MAX_ALTERNATIVES = 3

    /**
     * Extra via probes only when OSRM returns a single corridor.
     * Kept tiny — each probe is a full HTTP round-trip.
     */
    private const val MAX_VIA_PROBES = 4

    /** Light spacing so FOSSGIS is not hammered; primary route should not wait long. */
    private const val MIN_REQUEST_GAP_MS = 80L

    /** Bike vs car: if cycling is this much longer, prefer drivable roads. */
    internal const val BIKE_DETOUR_RATIO = 1.35

    /** Bike vs car: also fall back if absolute extra length exceeds this. */
    internal const val BIKE_DETOUR_EXTRA_M = 600.0

    /** Prefer car snap if bike start snap is farther than this and worse than car. */
    internal const val BIKE_BAD_SNAP_M = 90.0

    /** Seed routes plus whether Bike is using the car road network. */
    private data class RouteSeed(
        val routes: List<OsrmRouteResult>,
        /** When true, all diversions must stay on driving roads (bike ETA overlay). */
        val useRoadNetwork: Boolean = false,
    )

    /**
     * Tighter snap when the fix is precise so the route starts on the nearest road,
     * not a parallel arterial hundreds of meters away.
     */
    fun snapRadiusForAccuracy(accuracyM: Float?): Int {
        val acc = accuracyM?.takeIf { it.isFinite() && it > 0f } ?: return DEFAULT_SNAP_RADIUS_M
        return when {
            acc <= 10f -> 55
            acc <= 18f -> 85
            acc <= 30f -> 130
            acc <= 50f -> 180
            else -> DEFAULT_SNAP_RADIUS_M
        }
    }

    /** Best single route (used for active-trip reroutes). */
    suspend fun fetchRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle = TripVehicle.CAR,
        snapRadiusM: Int = DEFAULT_SNAP_RADIUS_M,
        viaPoints: List<Pair<Double, Double>> = emptyList(),
    ): OsrmRouteResult? = fetchRoutes(
        startLat, startLon, endLat, endLon, vehicle, snapRadiusM, viaPoints,
    ).firstOrNull()

    /**
     * Fast A→B duration/distance only (no geometry, steps, or alternates).
     * Used for SOS nearest-place ranking where many destinations are compared.
     */
    suspend fun fetchQuickRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle = TripVehicle.CAR,
        snapRadiusM: Int = DEFAULT_SNAP_RADIUS_M,
    ): OsrmRouteResult? {
        if (!startLat.isFinite() || !startLon.isFinite() || !endLat.isFinite() || !endLon.isFinite()) {
            return null
        }
        val snap = snapRadiusM.coerceIn(40, 500)
        val withSnap = fetchForProfile(
            startLat, startLon, endLat, endLon, vehicle,
            useRadiuses = true,
            snapRadiusM = snap,
            lite = true,
        )
        if (withSnap.isNotEmpty()) return withSnap.minByOrNull { it.durationSeconds }
        return fetchForProfile(
            startLat, startLon, endLat, endLon, vehicle,
            useRadiuses = false,
            snapRadiusM = snap,
            lite = true,
        ).minByOrNull { it.durationSeconds }
    }

    /**
     * Possible A→B routes for planner preview, fastest first.
     * Prefers connected, mode-appropriate paths; rejects disconnected spurs.
     * Slightly slower alts are kept so the user can choose.
     * [viaPoints] are intermediate stops (lat, lon), visited in order.
     */
    suspend fun fetchRoutes(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle = TripVehicle.CAR,
        snapRadiusM: Int = DEFAULT_SNAP_RADIUS_M,
        viaPoints: List<Pair<Double, Double>> = emptyList(),
    ): List<OsrmRouteResult> {
        if (!startLat.isFinite() || !startLon.isFinite() || !endLat.isFinite() || !endLon.isFinite()) {
            return emptyList()
        }
        val vias = viaPoints.filter { it.first.isFinite() && it.second.isFinite() }
        val snap = snapRadiusM.coerceIn(40, 500)
        val seed = when {
            vias.isNotEmpty() -> RouteSeed(
                fetchPrimaryRoutes(startLat, startLon, endLat, endLon, vehicle, snap, vias),
            )
            vehicle == TripVehicle.BIKE -> fetchBikeRoutesWithRoadFallback(
                startLat, startLon, endLat, endLon, snap,
            )
            else -> RouteSeed(
                fetchPrimaryRoutes(startLat, startLon, endLat, endLon, vehicle, snap),
            )
        }
        // With intermediate stops, keep the primary corridor only (no via-probed alts).
        if (vias.isNotEmpty()) {
            return seed.routes
                .sortedBy { it.durationSeconds }
                .distinctBy { routeFingerprint(it) }
                .take(MAX_ROUTE_OPTIONS)
        }
        return enrichWithNearbyAlternates(
            startLat, startLon, endLat, endLon, vehicle, seed, snap,
        )
    }

    private suspend fun fetchPrimaryRoutes(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle,
        snapRadiusM: Int,
        viaPoints: List<Pair<Double, Double>> = emptyList(),
    ): List<OsrmRouteResult> {
        val withSnap = fetchForProfile(
            startLat, startLon, endLat, endLon, vehicle,
            useRadiuses = true,
            snapRadiusM = snapRadiusM,
            viaPoints = viaPoints,
        )
        // One round-trip is enough when OSRM already returned a corridor.
        if (withSnap.isNotEmpty()) return withSnap
        // Only retry without radiuses when the snapped request found nothing.
        return fetchForProfile(
            startLat, startLon, endLat, endLon, vehicle,
            useRadiuses = false,
            snapRadiusM = snapRadiusM,
            viaPoints = viaPoints,
        )
    }

    /**
     * OSRM often returns a single route even when a different arterial corridor
     * exists. Probe a few left/right vias only when needed — never block the
     * primary result on a long probe chain.
     */
    private suspend fun enrichWithNearbyAlternates(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle,
        seed: RouteSeed,
        snapRadiusM: Int,
    ): List<OsrmRouteResult> {
        if (seed.routes.isEmpty()) return emptyList()
        val dedupedSeed = seed.routes
            .sortedBy { it.durationSeconds }
            .distinctBy { routeFingerprint(it) }
        val primary = dedupedSeed.first()
        val qualityCtx = RouteQualityContext(
            primary = primary,
            requestStartLat = startLat,
            requestStartLon = startLon,
            requestEndLat = endLat,
            requestEndLon = endLon,
        )
        // Native OSRM alts still must not be dead-ends / spurs.
        val usableSeed = listOf(primary) + dedupedSeed.drop(1).filter {
            passesRouteQuality(it, qualityCtx, geometryStrict = true)
        }

        // Fast path: OSRM already gave distinct usable corridors — skip via probing.
        if (usableSeed.size >= 2) {
            return pickDiverseRoutes(usableSeed, MAX_ROUTE_OPTIONS)
        }

        val viaCandidates = diversionViaPoints(primary).take(MAX_VIA_PROBES)
        val rawProbed = coroutineScope {
            viaCandidates.map { via ->
                async {
                    fetchDivertedRoute(
                        startLat, startLon, via.first, via.second, endLat, endLon,
                        vehicle = vehicle,
                        useRoadNetwork = seed.useRoadNetwork,
                        snapRadiusM = snapRadiusM,
                    )
                }
            }.mapNotNull { it.await() }
                .filter { routesAreVisiblyDistinct(it, primary) }
                .distinctBy { routeFingerprint(it) }
        }

        // Strict only — never surface irregular / dead-end corridors as “alternates”.
        val probed = rawProbed.filter {
            passesRouteQuality(it, qualityCtx, geometryStrict = true)
        }

        return pickDiverseRoutes(usableSeed + probed, MAX_ROUTE_OPTIONS)
    }

    /** Fastest first, then routes that diverge farthest from those already picked. */
    internal fun pickDiverseRoutes(
        candidates: List<OsrmRouteResult>,
        maxCount: Int,
    ): List<OsrmRouteResult> {
        if (candidates.isEmpty() || maxCount <= 0) return emptyList()
        val unique = candidates
            .sortedBy { it.durationSeconds }
            .distinctBy { routeFingerprint(it) }
        val picked = mutableListOf(unique.first())
        val rest = unique.drop(1)
            .sortedByDescending { c -> maxGapToSet(c, picked) }
        for (candidate in rest) {
            if (picked.size >= maxCount) break
            if (picked.any { !routesAreVisiblyDistinct(candidate, it) }) continue
            picked += candidate
        }
        return picked.sortedBy { it.durationSeconds }
    }

    private fun maxGapToSet(candidate: OsrmRouteResult, set: List<OsrmRouteResult>): Double =
        set.maxOfOrNull { maxSampleGapM(candidate, it) } ?: 0.0

    private fun maxPairwiseGapM(routes: List<OsrmRouteResult>): Double {
        var maxGap = 0.0
        for (i in routes.indices) {
            for (j in i + 1 until routes.size) {
                maxGap = maxOf(maxGap, maxSampleGapM(routes[i], routes[j]))
            }
        }
        return maxGap
    }

    internal fun maxSampleGapM(a: OsrmRouteResult, b: OsrmRouteResult): Double {
        val stations = listOf(0.25, 0.5, 0.75)
        var maxGap = 0.0
        for (frac in stations) {
            val ai = (a.points.size * frac).toInt().coerceIn(0, a.points.lastIndex)
            val bi = (b.points.size * frac).toInt().coerceIn(0, b.points.lastIndex)
            val ap = a.points[ai]
            val bp = b.points[bi]
            maxGap = maxOf(maxGap, haversineM(ap.first, ap.second, bp.first, bp.second))
        }
        return maxGap
    }

    private data class RouteQualityContext(
        val primary: OsrmRouteResult,
        val requestStartLat: Double,
        val requestStartLon: Double,
        val requestEndLat: Double,
        val requestEndLon: Double,
    )

    /**
     * @param geometryStrict when true (via-probed routes), also reject clear
     * out-and-back spurs and gross wrong-way detours. Native OSRM alts use false.
     */
    internal fun passesRouteQuality(
        candidate: OsrmRouteResult,
        primary: OsrmRouteResult,
        requestStartLat: Double,
        requestStartLon: Double,
        requestEndLat: Double,
        requestEndLon: Double,
        geometryStrict: Boolean = true,
    ): Boolean = passesRouteQuality(
        candidate,
        RouteQualityContext(
            primary, requestStartLat, requestStartLon, requestEndLat, requestEndLon,
        ),
        geometryStrict = geometryStrict,
    )

    private fun passesRouteQuality(
        candidate: OsrmRouteResult,
        ctx: RouteQualityContext,
        geometryStrict: Boolean,
    ): Boolean {
        if (candidate.points.size < 2) return false
        val primary = ctx.primary

        val startJoin = haversineM(
            ctx.requestStartLat, ctx.requestStartLon,
            candidate.points.first().first, candidate.points.first().second,
        )
        val endJoin = haversineM(
            ctx.requestEndLat, ctx.requestEndLon,
            candidate.points.last().first, candidate.points.last().second,
        )
        val primaryStartJoin = haversineM(
            ctx.requestStartLat, ctx.requestStartLon,
            primary.points.first().first, primary.points.first().second,
        )
        val primaryEndJoin = haversineM(
            ctx.requestEndLat, ctx.requestEndLon,
            primary.points.last().first, primary.points.last().second,
        )
        if (startJoin > (primaryStartJoin + 120.0).coerceAtLeast(160.0)) return false
        if (endJoin > (primaryEndJoin + 120.0).coerceAtLeast(160.0)) return false

        if (candidate.startSnapMeters > primary.startSnapMeters + 150.0 &&
            candidate.startSnapMeters > 160.0
        ) {
            return false
        }
        if (candidate.endSnapMeters > primary.endSnapMeters + 150.0 &&
            candidate.endSnapMeters > 160.0
        ) {
            return false
        }

        // Google-like: keep alts within a useful band (traffic can swing ETAs).
        val maxDist = primary.distanceMeters * 1.40 + 800.0
        if (candidate.distanceMeters > maxDist) return false

        val maxDuration = primary.durationSeconds * 1.35 + 420.0 // +7 min slack
        if (candidate.durationSeconds > maxDuration) return false

        // Always reject dead-ends / out-and-back spurs (native OSRM alts included).
        if (hasPointlessSpur(candidate)) return false
        if (hasDeadEndReversal(candidate, primary)) return false

        if (geometryStrict) {
            if (hasGrossWrongWayDetour(candidate, primary)) return false
            val primaryUTurns = countSharpReversals(primary)
            val candidateUTurns = countSharpReversals(candidate)
            if (candidateUTurns >= 3 && candidateUTurns > primaryUTurns + 1) return false
            // Irregular: much longer with little real corridor diversity.
            if (!isUsefulAlternateCorridor(candidate, primary)) return false
        }

        return true
    }

    /**
     * True out-and-back / cul-de-sac spur. Normal arterial zigzags must NOT match.
     */
    internal fun hasPointlessSpur(route: OsrmRouteResult): Boolean {
        val pts = route.points
        if (pts.size < 8) return false
        val alongAt = DoubleArray(pts.size)
        var along = 0.0
        for (i in 1 until pts.size) {
            along += haversineM(pts[i - 1].first, pts[i - 1].second, pts[i].first, pts[i].second)
            alongAt[i] = along
        }
        val step = (pts.size / 80).coerceAtLeast(1)
        for (i in 0 until pts.size step step) {
            for (j in (i + 3) until pts.size step step) {
                val traveled = alongAt[j] - alongAt[i]
                if (traveled < 120.0) continue
                if (traveled > 3_500.0) break
                val gap = haversineM(pts[i].first, pts[i].second, pts[j].first, pts[j].second)
                val chord = gap.coerceAtLeast(1.0)
                val ratio = traveled / chord
                // Closed loop / dead-end: long travel, return near start, highly non-linear.
                if (gap < 40.0 && ratio >= 4.0) return true
                // Clear side spur / cul-de-sac stub.
                if (traveled in 120.0..1_200.0 && gap < 30.0 && ratio >= 3.2) return true
            }
        }
        return false
    }

    /**
     * Reject alternates that reverse sharply more than the primary (typical dead-end
     * turnaround) without offering a meaningfully different corridor.
     */
    internal fun hasDeadEndReversal(
        candidate: OsrmRouteResult,
        primary: OsrmRouteResult,
    ): Boolean {
        val candidateUTurns = countSharpReversals(candidate)
        val primaryUTurns = countSharpReversals(primary)
        if (candidateUTurns >= 2 && candidateUTurns > primaryUTurns + 1) {
            // Only treat as dead-end when the path also fails to diverge usefully.
            if (maxSampleGapM(candidate, primary) < 120.0) return true
            if (candidate.distanceMeters > primary.distanceMeters * 1.15) return true
        }
        // Explicit U-turn maneuvers on short stubs.
        val uTurnSteps = candidate.steps.count { step ->
            val mod = step.modifier.lowercase()
            val type = step.maneuverType.lowercase()
            (mod.contains("uturn") || mod.contains("u-turn") ||
                type.contains("uturn") || type.contains("u-turn")) &&
                step.stepDistanceMeters < 180.0
        }
        return uTurnSteps >= 2 && candidateUTurns > primaryUTurns
    }

    /** Keep only alternates that are a real parallel corridor, not a noisy detour. */
    internal fun isUsefulAlternateCorridor(
        candidate: OsrmRouteResult,
        primary: OsrmRouteResult,
    ): Boolean {
        val gap = maxSampleGapM(candidate, primary)
        if (gap < 55.0) return false
        val extraDist = candidate.distanceMeters - primary.distanceMeters
        // Long extra distance without a wide corridor gap = irregular loop / dead-end.
        if (extraDist > 600.0 && gap < 140.0) return false
        if (extraDist > primary.distanceMeters * 0.28 && gap < 200.0) return false
        return true
    }

    /**
     * Max distance of the path from the start→end chord (corridor width).
     * Catches multi-km wrong-way bulges that destination-regression misses.
     */
    internal fun maxCorridorDeviationM(route: OsrmRouteResult): Double {
        val pts = route.points
        if (pts.size < 3) return 0.0
        val a = pts.first()
        val b = pts.last()
        val step = (pts.size / 50).coerceAtLeast(1)
        var maxDev = 0.0
        var i = step
        while (i < pts.lastIndex) {
            maxDev = maxOf(maxDev, pointToSegmentDistanceM(pts[i], a, b))
            i += step
        }
        return maxDev
    }

    /** Reject only gross wrong-way detours (circle-1 style), not parallel corridors. */
    internal fun hasGrossWrongWayDetour(
        candidate: OsrmRouteResult,
        primary: OsrmRouteResult,
    ): Boolean {
        val cDev = maxCorridorDeviationM(candidate)
        val pDev = maxCorridorDeviationM(primary)
        // Absolute: multi-km off the start–end corridor with little justification.
        if (cDev >= 3_500.0 && cDev > pDev * 1.8) return true
        // Relative: much wider bulge than the primary arterial path.
        if (cDev > pDev + 2_200.0 && cDev >= 2_500.0) return true
        return false
    }

    private fun pointToSegmentDistanceM(
        p: Pair<Double, Double>,
        a: Pair<Double, Double>,
        b: Pair<Double, Double>,
    ): Double {
        // Local equirectangular projection around the segment midpoint.
        val midLat = (a.first + b.first) / 2.0
        val cosLat = cos(Math.toRadians(midLat)).coerceAtLeast(1e-6)
        fun toXY(lat: Double, lon: Double): Pair<Double, Double> {
            val x = Math.toRadians(lon) * cosLat * 6_371_000.0
            val y = Math.toRadians(lat) * 6_371_000.0
            return x to y
        }
        val (ax, ay) = toXY(a.first, a.second)
        val (bx, by) = toXY(b.first, b.second)
        val (px, py) = toXY(p.first, p.second)
        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay
        val abLen2 = abx * abx + aby * aby
        if (abLen2 < 1.0) return haversineM(p.first, p.second, a.first, a.second)
        val t = ((apx * abx + apy * aby) / abLen2).coerceIn(0.0, 1.0)
        val cx = ax + t * abx
        val cy = ay + t * aby
        val dx = px - cx
        val dy = py - cy
        return sqrt(dx * dx + dy * dy)
    }

    /** U-turn steps plus sharp ~180° geometry reversals. */
    internal fun countSharpReversals(route: OsrmRouteResult): Int {
        val fromSteps = route.steps.count { step ->
            val mod = step.modifier.lowercase()
            val type = step.maneuverType.lowercase()
            mod.contains("uturn") || mod.contains("u-turn") ||
                type.contains("uturn") || type.contains("u-turn")
        }
        val pts = route.points
        var fromGeom = 0
        if (pts.size >= 3) {
            val stride = (pts.size / 100).coerceAtLeast(1)
            var i = stride
            while (i < pts.lastIndex) {
                val prev = pts[i - stride]
                val cur = pts[i]
                val next = pts[(i + stride).coerceAtMost(pts.lastIndex)]
                val b1 = bearingDegrees(prev.first, prev.second, cur.first, cur.second)
                val b2 = bearingDegrees(cur.first, cur.second, next.first, next.second)
                var delta = kotlin.math.abs(b2 - b1)
                if (delta > 180.0) delta = 360.0 - delta
                if (delta >= 150.0) fromGeom++
                i += stride
            }
        }
        return maxOf(fromSteps, fromGeom)
    }

    private suspend fun fetchDivertedRoute(
        startLat: Double,
        startLon: Double,
        viaLat: Double,
        viaLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle,
        useRoadNetwork: Boolean,
        snapRadiusM: Int,
    ): OsrmRouteResult? {
        return when {
            // Bike already on car roads → keep alts on the same connected network.
            vehicle == TripVehicle.BIKE && useRoadNetwork -> {
                fetchViaRoute(
                    startLat, startLon, viaLat, viaLon, endLat, endLon, TripVehicle.CAR,
                    snapRadiusM,
                )?.let { asBikeEta(it) }
            }
            vehicle == TripVehicle.BIKE -> {
                // Prefer cycling; only accept a road alt if the cycle via is missing/bad.
                val bikeVia = fetchViaRoute(
                    startLat, startLon, viaLat, viaLon, endLat, endLon, TripVehicle.BIKE,
                    snapRadiusM,
                )
                if (bikeVia != null) bikeVia
                else {
                    fetchViaRoute(
                        startLat, startLon, viaLat, viaLon, endLat, endLon, TripVehicle.CAR,
                        snapRadiusM,
                    )?.let { asBikeEta(it) }
                }
            }
            else -> fetchViaRoute(
                startLat, startLon, viaLat, viaLon, endLat, endLon, vehicle, snapRadiusM,
            )
        }
    }

    private suspend fun fetchViaRoute(
        startLat: Double,
        startLon: Double,
        viaLat: Double,
        viaLon: Double,
        endLat: Double,
        endLon: Double,
        vehicle: TripVehicle,
        snapRadiusM: Int,
    ): OsrmRouteResult? {
        if (!viaLat.isFinite() || !viaLon.isFinite()) return null
        // Tight via snap for short parallel streets; slightly looser for long
        // corridor probes so the pin can attach to a nearby arterial.
        val tripSpanM = haversineM(startLat, startLon, endLat, endLon)
        val viaRadiusM = if (tripSpanM >= 5_000.0) 120 else 55
        val url =
            "https://routing.openstreetmap.de/${vehicle.serverPath}/route/v1/${vehicle.osrmProfile}/" +
                "$startLon,$startLat;$viaLon,$viaLat;$endLon,$endLat" +
                "?overview=full&geometries=geojson&steps=true" +
                "&radiuses=$snapRadiusM;$viaRadiusM;$snapRadiusM"
        return throttledGet(url) { body ->
            val parsed = json.decodeFromString<OsrmRouteResponse>(body)
            if (!parsed.code.equals("Ok", ignoreCase = true) || parsed.routes.isEmpty()) {
                return@throttledGet null
            }
            val route = parsed.routes.firstOrNull() ?: return@throttledGet null
            val points = route.geometry.coordinates.mapNotNull { coordinate ->
                if (coordinate.size < 2) null else coordinate[1] to coordinate[0]
            }
            if (points.size < 2) return@throttledGet null
            OsrmRouteResult(
                points = points,
                durationSeconds = route.duration.coerceAtLeast(0.0),
                distanceMeters = route.distance.coerceAtLeast(0.0),
                steps = parseSteps(route.legs),
                startSnapMeters = parsed.waypoints.getOrNull(0)?.distance?.coerceAtLeast(0.0) ?: 0.0,
                endSnapMeters = parsed.waypoints.lastOrNull()?.distance?.coerceAtLeast(0.0) ?: 0.0,
            )
        }
    }

    /** Left/right offsets along the primary path to pull in parallel / corridor roads. */
    internal fun diversionViaPoints(primary: OsrmRouteResult): List<Pair<Double, Double>> {
        val pts = primary.points
        if (pts.size < 3) return emptyList()
        val offsetsM = when {
            primary.distanceMeters < 1_000.0 -> listOf(55.0, 90.0, 130.0, 180.0)
            primary.distanceMeters < 2_000.0 -> listOf(70.0, 110.0, 160.0, 220.0)
            primary.distanceMeters < 5_000.0 -> listOf(150.0, 280.0, 450.0, 700.0)
            // Long urban trips: Google-style corridor alts sit 0.5–2.5 km off the primary.
            else -> listOf(400.0, 800.0, 1_400.0, 2_200.0)
        }
        val fracs = when {
            primary.distanceMeters < 2_000.0 -> listOf(0.28, 0.45, 0.62)
            primary.distanceMeters < 5_000.0 -> listOf(0.28, 0.45, 0.62)
            else -> listOf(0.22, 0.38, 0.55, 0.70)
        }
        val preferOffset = when {
            primary.distanceMeters < 2_000.0 -> 120.0
            primary.distanceMeters < 5_000.0 -> 350.0
            else -> 1_000.0
        }
        val vias = ArrayList<Pair<Double, Double>>(offsetsM.size * fracs.size * 2)
        val orderedOffsets = offsetsM.sortedBy { kotlin.math.abs(it - preferOffset) }
        for (offsetM in orderedOffsets) {
            for (frac in fracs) {
                val idx = (pts.size * frac).toInt().coerceIn(1, (pts.lastIndex - 1).coerceAtLeast(1))
                val (lat, lon) = pts[idx]
                val prev = pts[(idx - 1).coerceAtLeast(0)]
                val next = pts[(idx + 1).coerceAtMost(pts.lastIndex)]
                val bearing = bearingDegrees(prev.first, prev.second, next.first, next.second)
                vias += offsetPoint(lat, lon, bearing - 90.0, offsetM)
                vias += offsetPoint(lat, lon, bearing + 90.0, offsetM)
            }
        }
        return vias
    }

    internal fun routesAreVisiblyDistinct(a: OsrmRouteResult, b: OsrmRouteResult): Boolean {
        val maxGap = maxSampleGapM(a, b)
        // Parallel streets on short urban trips are often only ~50–90 m apart.
        // Long trips need a larger gap so near-duplicates don't fill all slots.
        val minGap = when {
            minOf(a.distanceMeters, b.distanceMeters) < 1_500.0 -> 40.0
            minOf(a.distanceMeters, b.distanceMeters) < 2_500.0 -> 55.0
            minOf(a.distanceMeters, b.distanceMeters) < 8_000.0 -> 100.0
            else -> 150.0
        }
        return maxGap >= minGap
    }

    private fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δλ = Math.toRadians(lon2 - lon1)
        val y = sin(Δλ) * cos(φ2)
        val x = cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(Δλ)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    private fun offsetPoint(
        lat: Double,
        lon: Double,
        bearingDeg: Double,
        distanceM: Double,
    ): Pair<Double, Double> {
        val br = Math.toRadians(bearingDeg)
        val dLat = (distanceM * cos(br)) / 111_320.0
        val dLon = (distanceM * sin(br)) / (111_320.0 * cos(Math.toRadians(lat)).coerceAtLeast(1e-6))
        return (lat + dLat) to (lon + dLon)
    }

    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private suspend fun fetchBikeRoutesWithRoadFallback(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        snapRadiusM: Int,
    ): RouteSeed {
        // Bike + car primary in parallel — avoids waiting for two sequential OSRM calls.
        val (bikeRoutes, carRoutes) = coroutineScope {
            val bikeDeferred = async {
                fetchPrimaryRoutes(startLat, startLon, endLat, endLon, TripVehicle.BIKE, snapRadiusM)
            }
            val carDeferred = async {
                fetchPrimaryRoutes(startLat, startLon, endLat, endLon, TripVehicle.CAR, snapRadiusM)
            }
            bikeDeferred.await() to carDeferred.await()
        }
        val picked = pickFeasibleBikeRoute(bikeRoutes.firstOrNull(), carRoutes.firstOrNull())
            ?: return RouteSeed(emptyList())
        val bestBike = bikeRoutes.firstOrNull()
        return if (bestBike != null && picked === bestBike) {
            RouteSeed(bikeRoutes, useRoadNetwork = false)
        } else {
            RouteSeed(carRoutes.map { asBikeEta(it) }, useRoadNetwork = true)
        }
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
        snapRadiusM: Int,
        viaPoints: List<Pair<Double, Double>> = emptyList(),
        lite: Boolean = false,
    ): List<OsrmRouteResult> {
        val coordParts = buildList {
            add("$startLon,$startLat")
            viaPoints.forEach { (lat, lon) -> add("$lon,$lat") }
            add("$endLon,$endLat")
        }
        val radiusQuery = if (useRadiuses) {
            "&radiuses=" + List(coordParts.size) { snapRadiusM }.joinToString(";")
        } else {
            ""
        }
        // Lite: duration/distance only — much faster for SOS shortlist ranking.
        val alternatives = when {
            lite -> 0
            viaPoints.isEmpty() -> OSRM_MAX_ALTERNATIVES
            else -> 0
        }
        val overview = if (lite) "false" else "full"
        val steps = if (lite) "false" else "true"
        val geometries = if (lite) "" else "&geometries=geojson"
        val url =
            "https://routing.openstreetmap.de/${vehicle.serverPath}/route/v1/${vehicle.osrmProfile}/" +
                coordParts.joinToString(";") +
                "?overview=$overview$geometries&steps=$steps&alternatives=$alternatives$radiusQuery"
        return throttledGet(url) { body ->
            if (lite) {
                val parsed = json.decodeFromString<OsrmLiteRouteResponse>(body)
                if (!parsed.code.equals("Ok", ignoreCase = true) || parsed.routes.isEmpty()) {
                    return@throttledGet emptyList()
                }
                return@throttledGet parsed.routes.map { route ->
                    OsrmRouteResult(
                        points = listOf(startLat to startLon, endLat to endLon),
                        durationSeconds = route.duration.coerceAtLeast(0.0),
                        distanceMeters = route.distance.coerceAtLeast(0.0),
                        steps = emptyList(),
                        startSnapMeters = 0.0,
                        endSnapMeters = 0.0,
                    )
                }.sortedBy { it.durationSeconds }
            }
            val parsed = json.decodeFromString<OsrmRouteResponse>(body)
            if (!parsed.code.equals("Ok", ignoreCase = true) || parsed.routes.isEmpty()) {
                return@throttledGet emptyList()
            }
            val startSnap = parsed.waypoints.getOrNull(0)?.distance?.coerceAtLeast(0.0) ?: 0.0
            val endSnap = parsed.waypoints.lastOrNull()?.distance?.coerceAtLeast(0.0) ?: 0.0
            parsed.routes.mapNotNull { route ->
                val points = route.geometry.coordinates.mapNotNull { coordinate ->
                    if (coordinate.size < 2) null else coordinate[1] to coordinate[0]
                }
                if (points.size < 2) return@mapNotNull null
                OsrmRouteResult(
                    points = points,
                    durationSeconds = route.duration.coerceAtLeast(0.0),
                    distanceMeters = route.distance.coerceAtLeast(0.0),
                    steps = parseSteps(route.legs),
                    startSnapMeters = startSnap,
                    endSnapMeters = endSnap,
                )
            }
                .sortedBy { it.durationSeconds }
                .distinctBy { routeFingerprint(it) }
                .take(MAX_ROUTE_OPTIONS)
        } ?: emptyList()
    }

    /** Drop only near-duplicate geometries (keep real alternate roads). */
    private fun routeFingerprint(route: OsrmRouteResult): String {
        val mid = route.points.getOrNull(route.points.size / 2)
        val q1 = route.points.getOrNull(route.points.size / 4)
        val q3 = route.points.getOrNull(route.points.size * 3 / 4)
        // ~50 m bins so parallel streets ~80 m apart stay distinct.
        return listOf(
            (route.distanceMeters / 50.0).toInt(),
            mid?.first?.let { (it * 2_000).toInt() } ?: 0,
            mid?.second?.let { (it * 2_000).toInt() } ?: 0,
            q1?.first?.let { (it * 2_000).toInt() } ?: 0,
            q1?.second?.let { (it * 2_000).toInt() } ?: 0,
            q3?.first?.let { (it * 2_000).toInt() } ?: 0,
            q3?.second?.let { (it * 2_000).toInt() } ?: 0,
        ).joinToString("|")
    }

    private suspend fun <T> throttledGet(url: String, parse: (String) -> T?): T? {
        return requestSemaphore.withPermit {
            val waitMs = (MIN_REQUEST_GAP_MS - (System.currentTimeMillis() - lastRequestAtMs))
                .coerceAtLeast(0L)
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

/** Duration/distance only — used when overview=false (geometry may be absent or a string). */
@Serializable
private data class OsrmLiteRouteResponse(
    val code: String = "",
    val routes: List<OsrmLiteRoute> = emptyList(),
)

@Serializable
private data class OsrmLiteRoute(
    val duration: Double = 0.0,
    val distance: Double = 0.0,
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
