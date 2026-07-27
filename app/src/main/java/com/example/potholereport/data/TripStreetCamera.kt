package com.example.potholereport.data

/** Street-follow camera helpers (speed-linked zoom + route stroke polish). */
object TripStreetCamera {

    /**
     * Street-follow zoom bands (osmdroid zoom levels).
     * Higher = closer to the road. Only applied while street follow is on.
     */
    /** Below ~30 km/h — street view (still closer than overview). */
    const val ZOOM_SLOW = 20.5

    /** Around 30–69 km/h. */
    const val ZOOM_MODERATE = 21.2

    /** 70 km/h and above — max street zoom (overzooms CARTO tiles so roads look wider). */
    const val ZOOM_FAST = 22.0

    /** @deprecated Prefer [ZOOM_SLOW]; kept for call sites / tests. */
    const val ZOOM_IDLE = ZOOM_SLOW

    /** @deprecated Prefer [ZOOM_FAST]. */
    const val ZOOM_MOVING = ZOOM_FAST

    /** Upper edge of the slow band (km/h). */
    const val SPEED_SLOW_KMH = 30.0

    /** Start of max-zoom band (km/h). */
    const val SPEED_FAST_KMH = 70.0

    /** Soft zoom lerp per continuous follow update (0–1). Lower = smoother. */
    const val ZOOM_SMOOTH = 0.16

    /**
     * Duration of the Navigate → street-view zoom-in transition (ms).
     * Driven by osmdroid's ValueAnimator (AccelerateDecelerate) for smooth rendering.
     */
    const val INTRO_ZOOM_MS = 1400L

    /**
     * Look-ahead along bearing (meters). Kept small — vehicle bottom placement
     * primarily uses [VEHICLE_BOTTOM_OFFSET_FRAC] screen offset.
     */
    const val LOOKAHEAD_M = 0.0

    /**
     * Street follow: draw the map center this fraction of the view height *below*
     * the screen midpoint so the vehicle sits near the bottom (road ahead on top).
     */
    const val VEHICLE_BOTTOM_OFFSET_FRAC = 0.30f

    /**
     * Below this speed (m/s, ~7 km/h) do not rotate the map — freezes orientation
     * and avoids compass/GPS flips that look upside-down when nearly stopped.
     */
    const val HEADING_LOCK_MIN_SPEED_MPS = 2.0f

    /** Ignore heading samples that would snap the map nearly inverted in one update. */
    const val HEADING_FLIP_REJECT_DEG = 135f

    /** Max map rotation applied per follow tick (smooth turns, no hard flips). */
    const val HEADING_MAX_STEP_DEG = 28f

    /** Ignore tiny bearing jitter when rotating the map (degrees). */
    const val HEADING_UPDATE_MIN_DEG = 8f

    /**
     * Perspective tilt for a future 3D street POV. Unused while street view is
     * flat top-down 2D.
     */
    const val TILT_DEG = 48f
    const val TILT_SCALE = 1.38f

    /**
     * Continuous street zoom from GPS speed (m/s).
     * 0 → [ZOOM_SLOW], 30 km/h → [ZOOM_MODERATE], 70+ km/h → [ZOOM_FAST].
     */
    fun zoomForSpeedMps(speedMps: Float?): Double {
        val kmh = mpsToKmh(speedMps)
        return when {
            kmh <= 0.0 -> ZOOM_SLOW
            kmh < SPEED_SLOW_KMH -> {
                val t = kmh / SPEED_SLOW_KMH
                ZOOM_SLOW + t * (ZOOM_MODERATE - ZOOM_SLOW)
            }
            kmh < SPEED_FAST_KMH -> {
                val t = (kmh - SPEED_SLOW_KMH) / (SPEED_FAST_KMH - SPEED_SLOW_KMH)
                ZOOM_MODERATE + t * (ZOOM_FAST - ZOOM_MODERATE)
            }
            else -> ZOOM_FAST
        }
    }

    /**
     * 0 at rest → 1 at ≥70 km/h.
     * Drives mild route-line / outline emphasis in street follow (not screen-filling width).
     */
    fun movingFactor(speedMps: Float?): Float {
        val kmh = mpsToKmh(speedMps)
        return when {
            kmh <= 0.0 -> 0f
            kmh < SPEED_SLOW_KMH -> (kmh / SPEED_SLOW_KMH).toFloat() * 0.45f
            kmh < SPEED_FAST_KMH -> {
                val t = (kmh - SPEED_SLOW_KMH) / (SPEED_FAST_KMH - SPEED_SLOW_KMH)
                (0.45 + t * 0.55).toFloat()
            }
            else -> 1f
        }
    }

    private fun mpsToKmh(speedMps: Float?): Double {
        val speed = (speedMps?.takeIf { it.isFinite() && it >= 0f } ?: 0f).toDouble()
        return speed * 3.6
    }

    /** Thin stroke before Navigate — curves stay readable on the overview map. */
    const val ROUTE_PREVIEW_STROKE_PX = 8f
    const val ROUTE_PREVIEW_COMPLETED_STROKE_PX = 5.5f

    /** After Navigate — fine guidance line (road size comes from zoom, not fat strokes). */
    const val ROUTE_NAV_REMAINING_IDLE_PX = 9f
    const val ROUTE_NAV_COMPLETED_IDLE_PX = 6f

    /** Slight thicken at speed; still thin so edges stay sharp when overzoomed. */
    const val ROUTE_NAV_REMAINING_MOVING_PX = 12f
    const val ROUTE_NAV_COMPLETED_MOVING_PX = 8f

    /** Extra width of the dark under-outline (hairline rim, not a blob). */
    const val ROUTE_OUTLINE_EXTRA_PX = 3f

    /** Densify display polyline so long OSRM segments don’t look broken at high zoom. */
    const val ROUTE_DISPLAY_MAX_SEGMENT_M = 6.0

    fun remainingRouteStrokePx(movingFactor: Float, mapZoom: Double = ZOOM_SLOW): Float {
        val t = movingFactor.coerceIn(0f, 1f)
        val base = ROUTE_NAV_REMAINING_IDLE_PX +
            t * (ROUTE_NAV_REMAINING_MOVING_PX - ROUTE_NAV_REMAINING_IDLE_PX)
        return base * strokeScaleForZoom(mapZoom)
    }

    fun completedRouteStrokePx(movingFactor: Float, mapZoom: Double = ZOOM_SLOW): Float {
        val t = movingFactor.coerceIn(0f, 1f)
        val base = ROUTE_NAV_COMPLETED_IDLE_PX +
            t * (ROUTE_NAV_COMPLETED_MOVING_PX - ROUTE_NAV_COMPLETED_IDLE_PX)
        return base * strokeScaleForZoom(mapZoom)
    }

    fun remainingOutlineStrokePx(movingFactor: Float, mapZoom: Double = ZOOM_SLOW): Float =
        remainingRouteStrokePx(movingFactor, mapZoom) +
            ROUTE_OUTLINE_EXTRA_PX * strokeScaleForZoom(mapZoom)

    fun completedOutlineStrokePx(movingFactor: Float, mapZoom: Double = ZOOM_SLOW): Float =
        completedRouteStrokePx(movingFactor, mapZoom) +
            ROUTE_OUTLINE_EXTRA_PX * 0.65f * strokeScaleForZoom(mapZoom)

    /**
     * At high overzoom, thin screen-space strokes so route edges stay refined
     * (road “width” comes from the basemap, not a fat blue ribbon).
     */
    fun strokeScaleForZoom(mapZoom: Double): Float {
        if (!mapZoom.isFinite()) return 1f
        // 20.5 → 1.0, 22 → ~0.62
        val t = ((mapZoom - ZOOM_SLOW) / (ZOOM_FAST - ZOOM_SLOW).coerceAtLeast(0.01))
            .coerceIn(0.0, 1.0)
        return (1.0 - t * 0.38).toFloat()
    }

    /**
     * Insert intermediate lat/lon samples so long OSRM segments don’t look
     * faceted when the camera is deep into street zoom.
     */
    fun densifyLatLons(
        points: List<Pair<Double, Double>>,
        maxSegmentM: Double = ROUTE_DISPLAY_MAX_SEGMENT_M,
    ): List<Pair<Double, Double>> {
        if (points.size < 2 || maxSegmentM <= 0.0) return points
        val out = ArrayList<Pair<Double, Double>>(points.size * 2)
        out.add(points.first())
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val dist = haversineM(a.first, a.second, b.first, b.second)
            if (dist > maxSegmentM) {
                val steps = kotlin.math.ceil(dist / maxSegmentM).toInt().coerceAtLeast(2)
                for (s in 1 until steps) {
                    val t = s.toDouble() / steps
                    out.add(
                        a.first + (b.first - a.first) * t to
                            a.second + (b.second - a.second) * t,
                    )
                }
            }
            out.add(b)
        }
        return out
    }

    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(p1) * kotlin.math.cos(p2) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return 2.0 * r * kotlin.math.asin(kotlin.math.sqrt(h))
    }

    /** Ease-out quadratic: fast start, soft settle into street zoom. */
    fun introZoomEase(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return 1f - (1f - x) * (1f - x)
    }
}
