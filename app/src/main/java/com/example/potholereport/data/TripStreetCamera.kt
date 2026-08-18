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
    const val ZOOM_SMOOTH = 0.14

    /**
     * Duration of the Navigate → street-view zoom-in transition (ms).
     * Driven by osmdroid's ValueAnimator (AccelerateDecelerate) for smooth rendering.
     */
    const val INTRO_ZOOM_MS = 1400L

    /**
     * Look-ahead along bearing (meters) while moving — frames more road ahead.
     * Zero when nearly stopped ([HEADING_LOCK_MIN_SPEED_MPS]).
     */
    const val LOOKAHEAD_M = 18.0

    /** MapLibre street-follow ease duration (ms). */
    const val STREET_ML_EASE_MS = 360

    /**
     * Street follow: draw the map center this fraction of the view height *below*
     * the screen midpoint so the vehicle sits near the bottom (road ahead on top).
     */
    const val VEHICLE_BOTTOM_OFFSET_FRAC = 0.40f

    /**
     * Street / Play follow while the bottom Report (+ Video) chrome is visible —
     * slightly less offset so the cursor sits above the FABs (no overlap).
     */
    const val VEHICLE_BOTTOM_OFFSET_FRAC_WITH_NAV_CHROME = 0.30f

    /** Bottom inset (dp) under the MapLibre focal point so the cursor clears Report FAB. */
    const val NAV_CHROME_BOTTOM_CLEARANCE_DP = 118f

    /**
     * Play mode: sit the vehicle even lower so more road fills the top of the
     * frame (stronger “riding on the map” POV while tilted).
     */
    const val PLAY_VEHICLE_BOTTOM_OFFSET_FRAC = 0.48f

    /** Play + nav chrome: keep cursor clearly above Report / Video FABs. */
    const val PLAY_VEHICLE_BOTTOM_OFFSET_FRAC_WITH_NAV_CHROME = 0.30f

    /** MapLibre ease duration for Play tilt/zoom updates (ms) — cinematic, not jumpy. */
    const val PLAY_CAMERA_EASE_MS = 720

    /** MapLibre ease when settling to halt (flat 0°). */
    const val PLAY_HALT_CAMERA_EASE_MS = 1_100

    /**
     * MapLibre Android hard-caps camera pitch at 60°
     * (`MapLibreConstants.MAXIMUM_PITCH`). Play mode uses this ceiling.
     */
    const val MAPLIBRE_MAX_PITCH_DEG = 60.0

    /** Allow overzoom so ~7 ft play POV is reachable (default map max was 20). */
    const val PLAY_ML_MAX_ZOOM = 22.0

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

    // --- Play mode (game POV): ~15 ft at crawl → ~7 ft as speed builds ---

    /**
     * OSM play zoom. Higher = closer to the road (roads look wider).
     * REST = halted top-down; 15 ft / 7 ft while moving.
     */
    const val PLAY_ZOOM_REST = 20.5
    const val PLAY_ZOOM_15FT = 21.55
    const val PLAY_ZOOM_7FT = 22.65

    /** @deprecated Use [PLAY_ZOOM_15FT]. */
    const val PLAY_ZOOM_20FT = PLAY_ZOOM_15FT
    /** @deprecated Use [PLAY_ZOOM_7FT]. */
    const val PLAY_ZOOM_10FT = PLAY_ZOOM_7FT
    /** @deprecated Use [PLAY_ZOOM_7FT]. */
    const val PLAY_ZOOM_MOVING = PLAY_ZOOM_7FT
    /** @deprecated */
    const val PLAY_ZOOM_SLOW = PLAY_ZOOM_REST
    /** @deprecated */
    const val PLAY_ZOOM_MODERATE = PLAY_ZOOM_15FT
    /** @deprecated */
    const val PLAY_ZOOM_FAST = PLAY_ZOOM_7FT

    /** MapLibre play zoom (scale differs from osmdroid). */
    const val PLAY_ML_ZOOM_REST = 16.6
    /** ~15 ft overhead while rolling slowly (~5 km/h). */
    const val PLAY_ML_ZOOM_15FT = 18.55
    /** ~7 ft dive — max widen / riding feel (~10 km/h+). */
    const val PLAY_ML_ZOOM_7FT = 20.55

    /** @deprecated Use [PLAY_ML_ZOOM_15FT]. */
    const val PLAY_ML_ZOOM_20FT = PLAY_ML_ZOOM_15FT
    /** @deprecated Use [PLAY_ML_ZOOM_7FT]. */
    const val PLAY_ML_ZOOM_10FT = PLAY_ML_ZOOM_7FT
    /** @deprecated */
    const val PLAY_ML_ZOOM_MOVING = PLAY_ML_ZOOM_7FT
    /** @deprecated */
    const val PLAY_ML_ZOOM_SLOW = PLAY_ML_ZOOM_REST
    /** @deprecated */
    const val PLAY_ML_ZOOM_MODERATE = PLAY_ML_ZOOM_15FT
    /** @deprecated */
    const val PLAY_ML_ZOOM_FAST = PLAY_ML_ZOOM_7FT

    /**
     * MapLibre tilt: halted → 0°; moving 15 ft → 7 ft dive.
     * Max is [MAPLIBRE_MAX_PITCH_DEG] (SDK hard ceiling — values above 60° are ignored).
     */
    const val PLAY_TILT_MIN_DEG = 0.0
    /** Game tilt as soon as rolling (15 ft POV) — not flat. */
    const val PLAY_TILT_15FT_DEG = 48.0
    /** Max dive tilt at 7 ft — MapLibre absolute ceiling. */
    const val PLAY_TILT_7FT_DEG = MAPLIBRE_MAX_PITCH_DEG
    const val PLAY_TILT_MAX_DEG = PLAY_TILT_7FT_DEG

    /** @deprecated Use [PLAY_TILT_15FT_DEG]. */
    const val PLAY_TILT_20FT_DEG = PLAY_TILT_15FT_DEG
    /** @deprecated Use [PLAY_TILT_7FT_DEG]. */
    const val PLAY_TILT_10FT_DEG = PLAY_TILT_7FT_DEG

    /** @deprecated Prefer intensity/dive helpers. */
    const val PLAY_ML_TILT_SLOW = 0.0
    /** @deprecated */
    const val PLAY_ML_TILT_MODERATE = PLAY_TILT_15FT_DEG
    /** @deprecated */
    const val PLAY_ML_TILT_FAST = PLAY_TILT_MAX_DEG

    /** Street-follow MapLibre zoom bands (flat). Scale differs from osmdroid. */
    const val STREET_ML_ZOOM_SLOW = 16.8
    const val STREET_ML_ZOOM_MODERATE = 17.6
    const val STREET_ML_ZOOM_FAST = 18.4

    /** @deprecated Prefer [mapLibreZoomForSpeedMps]; mid-band default. */
    const val STREET_ML_ZOOM = STREET_ML_ZOOM_MODERATE

    /** Map overview MapLibre zoom. */
    const val MAP_ML_ZOOM = 15.5

    /**
     * Continuous street zoom from GPS speed (m/s).
     * 0 → [ZOOM_SLOW], 30 km/h → [ZOOM_MODERATE], 70+ km/h → [ZOOM_FAST].
     */
    fun zoomForSpeedMps(speedMps: Float?): Double {
        return lerpZoomForSpeed(
            speedMps,
            ZOOM_SLOW,
            ZOOM_MODERATE,
            ZOOM_FAST,
        )
    }

    /** MapLibre street zoom from the same speed bands as [zoomForSpeedMps]. */
    fun mapLibreZoomForSpeedMps(speedMps: Float?): Double {
        return lerpZoomForSpeed(
            speedMps,
            STREET_ML_ZOOM_SLOW,
            STREET_ML_ZOOM_MODERATE,
            STREET_ML_ZOOM_FAST,
        )
    }

    /**
     * Look-ahead meters while street-following. Zero when speed is below the
     * heading-lock floor so the pin doesn't drift while stopped.
     */
    fun lookAheadMetersForSpeed(speedMps: Float?): Double {
        val speed = speedMps?.takeIf { it.isFinite() && it >= HEADING_LOCK_MIN_SPEED_MPS }
            ?: return 0.0
        val kmh = speed * 3.6
        val t = ((kmh - SPEED_SLOW_KMH * 0.2) / (SPEED_FAST_KMH - SPEED_SLOW_KMH * 0.2))
            .coerceIn(0.0, 1.0)
        return LOOKAHEAD_M * (0.45 + 0.55 * t)
    }

    /** Soft-lerp current zoom toward a speed target (MapLibre / osmdroid). */
    fun smoothZoomToward(current: Double, target: Double, factor: Double = ZOOM_SMOOTH): Double {
        if (!current.isFinite()) return target
        if (!target.isFinite()) return current
        return current + (target - current) * factor.coerceIn(0.0, 1.0)
    }

    /**
     * Play OSM zoom from motion.
     * Halted → [PLAY_ZOOM_REST]; moving dive 0 → [PLAY_ZOOM_15FT]; dive 1 → [PLAY_ZOOM_7FT].
     */
    fun playZoomForIntensity(intensity: Float, moving: Boolean = intensity > 0.001f): Double {
        if (!moving) return PLAY_ZOOM_REST
        val t = intensity.coerceIn(0f, 1f).toDouble()
        return PLAY_ZOOM_15FT + t * (PLAY_ZOOM_7FT - PLAY_ZOOM_15FT)
    }

    /** Play MapLibre zoom from motion (15 ft → 7 ft while rolling). */
    fun playMapLibreZoomForIntensity(intensity: Float, moving: Boolean = intensity > 0.001f): Double {
        if (!moving) return PLAY_ML_ZOOM_REST
        val t = intensity.coerceIn(0f, 1f).toDouble()
        return PLAY_ML_ZOOM_15FT + t * (PLAY_ML_ZOOM_7FT - PLAY_ML_ZOOM_15FT)
    }

    /**
     * Play MapLibre tilt: 0° halted; as soon as moving use ≥ [PLAY_TILT_15FT_DEG],
     * diving to [PLAY_TILT_7FT_DEG] (SDK max 60°) with speed/accel.
     */
    fun playMapLibreTiltForIntensity(intensity: Float, moving: Boolean = intensity > 0.001f): Double {
        if (!moving) return PLAY_TILT_MIN_DEG
        val t = intensity.coerceIn(0f, 1f).toDouble()
        return PLAY_TILT_15FT_DEG + t * (PLAY_TILT_7FT_DEG - PLAY_TILT_15FT_DEG)
    }

    /**
     * Extra look-ahead in Play so the tilted POV frames more road ahead
     * (stronger riding feel as dive increases).
     */
    fun playLookAheadMeters(speedMps: Float?, dive: Float): Double {
        val base = lookAheadMetersForSpeed(speedMps)
        if (base <= 0.0) return 0.0
        val t = dive.coerceIn(0f, 1f).toDouble()
        return base * (1.20 + 0.45 * t)
    }

    fun playZoomForSnapshot(motion: TripPlayMotionSnapshot): Double =
        playZoomForIntensity(motion.dive, motion.moving)

    fun playMapLibreZoomForSnapshot(motion: TripPlayMotionSnapshot): Double =
        playMapLibreZoomForIntensity(motion.dive, motion.moving)

    fun playMapLibreTiltForSnapshot(motion: TripPlayMotionSnapshot): Double =
        playMapLibreTiltForIntensity(motion.dive, motion.moving)

    /**
     * One-shot play zoom from speed alone (no accel history). Prefer
     * [TripPlayMotionTracker] + [playZoomForSnapshot] while navigating.
     */
    fun playZoomForSpeedMps(speedMps: Float?): Double {
        val speed = speedMps?.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val (moving, dive) = TripPlayMotionTracker.nextDive(
            currentDive = 0f,
            wasMoving = false,
            speedMps = speed,
            accelMps2 = 0f,
        )
        return playZoomForIntensity(dive, moving)
    }

    fun playMapLibreZoomForSpeedMps(speedMps: Float?): Double {
        val speed = speedMps?.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val (moving, dive) = TripPlayMotionTracker.nextDive(
            currentDive = 0f,
            wasMoving = false,
            speedMps = speed,
            accelMps2 = 0f,
        )
        return playMapLibreZoomForIntensity(dive, moving)
    }

    fun playMapLibreTiltForSpeedMps(speedMps: Float?): Double {
        val speed = speedMps?.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val (moving, dive) = TripPlayMotionTracker.nextDive(
            currentDive = 0f,
            wasMoving = false,
            speedMps = speed,
            accelMps2 = 0f,
        )
        return playMapLibreTiltForIntensity(dive, moving)
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

    private fun lerpZoomForSpeed(
        speedMps: Float?,
        slow: Double,
        moderate: Double,
        fast: Double,
    ): Double {
        val kmh = mpsToKmh(speedMps)
        return when {
            kmh <= 0.0 -> slow
            kmh < SPEED_SLOW_KMH -> {
                val t = kmh / SPEED_SLOW_KMH
                slow + t * (moderate - slow)
            }
            kmh < SPEED_FAST_KMH -> {
                val t = (kmh - SPEED_SLOW_KMH) / (SPEED_FAST_KMH - SPEED_SLOW_KMH)
                moderate + t * (fast - moderate)
            }
            else -> fast
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
