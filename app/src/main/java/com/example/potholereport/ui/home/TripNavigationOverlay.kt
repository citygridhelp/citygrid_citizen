package com.example.potholereport.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import com.example.potholereport.data.PotholeSeverity
import com.example.potholereport.data.TripNavigationMatcher
import com.example.potholereport.data.TripPotholeAlert
import com.example.potholereport.data.TripStreetCamera
import com.example.potholereport.data.formatTripPotholeAlertLine
import com.example.potholereport.data.tripPotholeAlertAccentArgb
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color as AndroidColor
import kotlin.math.hypot

/**
 * Draws trip route polyline (completed grey + remaining blue), optional alternate
 * preview routes, and upcoming pothole markers (severity color + L/M/R/F in street
 * follow, or numbered pins in map overview).
 */
class TripNavigationOverlay : Overlay() {

    /** Wide under-outline for the remaining route (crisp edges at street zoom). */
    private val remainingRibbon = Polyline().apply {
        outlinePaint.color = AndroidColor.parseColor("#0F172A")
        outlinePaint.strokeWidth = 16f
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.isAntiAlias = true
        outlinePaint.isDither = true
        outlinePaint.isFilterBitmap = true
        outlinePaint.strokeMiter = 2.5f
        outlinePaint.alpha = 0
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        isEnabled = false
    }

    /** Bright center stroke on top of [remainingRibbon]. */
    private val remainingLine = Polyline().apply {
        outlinePaint.color = AndroidColor.parseColor("#1D4ED8")
        outlinePaint.strokeWidth = 10f
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.isAntiAlias = true
        outlinePaint.isDither = true
        outlinePaint.isFilterBitmap = true
        outlinePaint.strokeMiter = 2.5f
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
    }

    private val completedLine = Polyline().apply {
        outlinePaint.color = AndroidColor.parseColor("#9CA3AF")
        outlinePaint.strokeWidth = 8f
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.isAntiAlias = true
        outlinePaint.isDither = true
        outlinePaint.isFilterBitmap = true
        outlinePaint.strokeMiter = 2.5f
        outlinePaint.alpha = 180
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
    }

    /**
     * Dashed return-to-route cue (#23.3) — teal grading, distinct from blue active
     * and orange preview alternates.
     */
    private val rejoinLine = Polyline().apply {
        outlinePaint.color = AndroidColor.parseColor("#0F766E")
        outlinePaint.strokeWidth = 10f
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.isAntiAlias = true
        outlinePaint.isDither = true
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(22f, 14f), 0f)
        outlinePaint.alpha = 230
        isEnabled = false
    }

    /** Soft under-glow for the dashed rejoin so it reads at street zoom. */
    private val rejoinGlow = Polyline().apply {
        outlinePaint.color = AndroidColor.parseColor("#5EEAD4")
        outlinePaint.strokeWidth = 16f
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.isAntiAlias = true
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(22f, 14f), 0f)
        outlinePaint.alpha = 90
        isEnabled = false
    }

    var remainingRoutePoints: List<GeoPoint> = emptyList()
        set(value) {
            field = value
            val display = densifyForDisplay(value)
            // Ribbon is only shown after Navigate; keep it empty during preview.
            if (remainingRibbon.isEnabled) {
                remainingRibbon.setPoints(display)
            }
            remainingLine.setPoints(display)
        }

    var completedRoutePoints: List<GeoPoint> = emptyList()
        set(value) {
            field = value
            completedLine.setPoints(densifyForDisplay(value))
        }

    /** Route used to nudge pins left/right of the road when side is known. */
    var routeLatLons: List<Pair<Double, Double>> = emptyList()

    /** @deprecated Prefer [remainingRoutePoints] + [completedRoutePoints]. */
    var routePoints: List<GeoPoint>
        get() = remainingRoutePoints
        set(value) {
            completedRoutePoints = emptyList()
            remainingRoutePoints = value
        }

    var alerts: List<TripPotholeAlert> = emptyList()
        set(value) {
            field = value
            rebuildMarkers()
        }

    /** Invoked when the user taps an alternate preview polyline (index in [setPreviewAlternates]). */
    var onAlternateRouteClick: ((Int) -> Unit)? = null

    /** Mid-nav: tap the offered known-alternate corridor to confirm switch. */
    var onOfferedAlternateClick: (() -> Unit)? = null

    /**
     * Street follow: show L/M/R/F placement on pins.
     * Map overview: number pins by distance ahead (1, 2, …) — no side letters.
     */
    var showSidePlacement: Boolean = true

    private val alternateLines = mutableListOf<Polyline>()
    private val etaMarkers = mutableListOf<Marker>()
    private val markers = mutableListOf<Marker>()
    private var offeredAlternateLine: Polyline? = null
    private var offeredAlternateEta: Marker? = null

    /**
     * Route stroke widths + crisp under-outline in street follow.
     * Before Navigate: thin preview lines (no outline) so orange alternates stay visible.
     * After Navigate: dark under-outline + bright center — road “expansion” from camera zoom.
     * [mapZoom] thins strokes at high overzoom so edges stay sharp.
     */
    fun applyRouteStrokeWidths(
        movingFactor: Float,
        mapWidthPx: Int,
        navigationActive: Boolean,
        mapZoom: Double = TripStreetCamera.ZOOM_SLOW,
    ) {
        if (!navigationActive) {
            remainingRibbon.isEnabled = false
            remainingRibbon.setPoints(emptyList())
            remainingRibbon.outlinePaint.alpha = 0
            remainingLine.outlinePaint.strokeWidth = TripStreetCamera.ROUTE_PREVIEW_STROKE_PX
            remainingLine.outlinePaint.alpha = 255
            completedLine.outlinePaint.strokeWidth = TripStreetCamera.ROUTE_PREVIEW_COMPLETED_STROKE_PX
            return
        }
        val t = movingFactor.coerceIn(0f, 1f)
        val rem = TripStreetCamera.remainingRouteStrokePx(t, mapZoom)
        val remOut = TripStreetCamera.remainingOutlineStrokePx(t, mapZoom)
        remainingRibbon.isEnabled = true
        val displayRem = densifyForDisplay(remainingRoutePoints)
        remainingRibbon.setPoints(displayRem)
        remainingLine.setPoints(displayRem)
        remainingRibbon.outlinePaint.color = AndroidColor.parseColor("#0F172A")
        remainingRibbon.outlinePaint.strokeWidth = remOut
        remainingRibbon.outlinePaint.alpha = 210
        remainingLine.outlinePaint.strokeWidth = rem
        remainingLine.outlinePaint.alpha = 255
        completedLine.outlinePaint.strokeWidth =
            TripStreetCamera.completedRouteStrokePx(t, mapZoom)
        completedLine.outlinePaint.alpha = 200
    }

    private fun densifyForDisplay(points: List<GeoPoint>): List<GeoPoint> {
        if (points.size < 2) return points
        val pairs = points.map { it.latitude to it.longitude }
        return TripStreetCamera.densifyLatLons(pairs).map { (lat, lon) -> GeoPoint(lat, lon) }
    }

    fun attachTo(map: MapView) {
        if (!map.overlays.contains(completedLine)) {
            map.overlays.add(0, completedLine)
        }
        if (!map.overlays.contains(remainingRibbon)) {
            val completedIdx = map.overlays.indexOf(completedLine)
            val insertAt = if (completedIdx >= 0) completedIdx + 1 else 0
            map.overlays.add(insertAt, remainingRibbon)
        }
        if (!map.overlays.contains(remainingLine)) {
            val ribbonIdx = map.overlays.indexOf(remainingRibbon)
            val insertAt = if (ribbonIdx >= 0) ribbonIdx + 1 else map.overlays.size
            map.overlays.add(insertAt, remainingLine)
        }
        if (!map.overlays.contains(rejoinGlow)) {
            map.overlays.add(rejoinGlow)
        }
        if (!map.overlays.contains(rejoinLine)) {
            map.overlays.add(rejoinLine)
        }
    }

    fun detachFrom(map: MapView) {
        clearAlternateLines(map)
        clearEtaMarkers(map)
        clearOfferedAlternate(map)
        clearRejoinPath(map)
        map.overlays.remove(completedLine)
        map.overlays.remove(remainingRibbon)
        map.overlays.remove(remainingLine)
        map.overlays.remove(rejoinGlow)
        map.overlays.remove(rejoinLine)
        markers.forEach { map.overlays.remove(it) }
        markers.clear()
    }

    /** Dashed teal return-to-route path while hybrid prefers rejoin. */
    fun setRejoinPath(map: MapView, points: List<GeoPoint>) {
        if (points.size < 2) {
            clearRejoinPath(map)
            return
        }
        val display = densifyForDisplay(points)
        if (!map.overlays.contains(rejoinGlow)) map.overlays.add(rejoinGlow)
        if (!map.overlays.contains(rejoinLine)) map.overlays.add(rejoinLine)
        rejoinGlow.isEnabled = true
        rejoinLine.isEnabled = true
        rejoinGlow.setPoints(display)
        rejoinLine.setPoints(display)
    }

    fun clearRejoinPath(map: MapView) {
        rejoinGlow.isEnabled = false
        rejoinLine.isEnabled = false
        rejoinGlow.setPoints(emptyList())
        rejoinLine.setPoints(emptyList())
        map.overlays.remove(rejoinGlow)
        map.overlays.remove(rejoinLine)
    }

    /**
     * Mid-navigation: highlight a known alternate corridor; tap confirms switch
     * (does not auto-switch — #23.3).
     */
    fun setOfferedAlternate(map: MapView, points: List<GeoPoint>) {
        clearOfferedAlternate(map)
        if (points.size < 2) return
        val remainingIdx = map.overlays.indexOf(remainingLine)
        val insertAt = if (remainingIdx >= 0) remainingIdx else map.overlays.size
        val line = Polyline().apply {
            outlinePaint.color = AndroidColor.parseColor("#EA580C")
            outlinePaint.strokeWidth = 14f
            outlinePaint.style = Paint.Style.STROKE
            outlinePaint.isAntiAlias = true
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            setPoints(densifyForDisplay(points))
            setOnClickListener { _, _, _ ->
                onOfferedAlternateClick?.invoke()
                true
            }
        }
        offeredAlternateLine = line
        map.overlays.add(insertAt, line)
        val tip = pointAlongRoute(points, 0.45)
        val eta = Marker(map).apply {
            position = tip
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setInfoWindow(null)
            title = "Tap to use"
            setIcon(buildRouteEtaBadgeDrawable(map.context, "Tap to use", selected = false))
            setOnMarkerClickListener { _, _ ->
                onOfferedAlternateClick?.invoke()
                true
            }
        }
        offeredAlternateEta = eta
        map.overlays.add(eta)
    }

    fun clearOfferedAlternate(map: MapView) {
        offeredAlternateLine?.let { map.overlays.remove(it) }
        offeredAlternateLine = null
        offeredAlternateEta?.let { map.overlays.remove(it) }
        offeredAlternateEta = null
    }

    /**
     * Preview mode: orange lines for non-selected routes; selected stays on
     * [remainingLine]. Speech-cloud tip sits on the route itself.
     */
    fun setPreviewAlternates(
        map: MapView,
        alternates: List<List<GeoPoint>>,
        selectedIndex: Int,
        etas: List<String> = emptyList(),
    ) {
        clearAlternateLines(map)
        clearEtaMarkers(map)
        if (alternates.isEmpty()) return

        // Draw orange alternates above the (disabled) ribbon, under the selected blue line.
        val ribbonIdx = map.overlays.indexOf(remainingRibbon)
        val remainingIdx = map.overlays.indexOf(remainingLine)
        val insertAt = when {
            remainingIdx >= 0 -> remainingIdx
            ribbonIdx >= 0 -> ribbonIdx + 1
            else -> map.overlays.size
        }

        if (alternates.size > 1) {
            alternates.forEachIndexed { index, points ->
                if (index == selectedIndex || points.size < 2) return@forEachIndexed
                val line = Polyline().apply {
                    outlinePaint.color = AndroidColor.parseColor("#EA580C")
                    outlinePaint.strokeWidth = 14f
                    outlinePaint.style = Paint.Style.STROKE
                    outlinePaint.isAntiAlias = true
                    outlinePaint.alpha = 240
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(points)
                    setOnClickListener { _, _, _ ->
                        onAlternateRouteClick?.invoke(index)
                        true
                    }
                }
                alternateLines.add(line)
                map.overlays.add(insertAt, line)
            }
        }

        val labelPoints = pickSpacedLabelPoints(map, alternates, selectedIndex)
        alternates.forEachIndexed { index, points ->
            if (points.size < 2) return@forEachIndexed
            val label = etas.getOrNull(index) ?: return@forEachIndexed
            val onRoute = labelPoints.getOrNull(index) ?: pointAlongRoute(points, 0.45)
            val selected = index == selectedIndex
            val marker = Marker(map).apply {
                // Tip of the speech bubble (ANCHOR_BOTTOM) lands on this route point.
                position = onRoute
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setInfoWindow(null)
                title = label
                setIcon(
                    buildRouteEtaBadgeDrawable(
                        map.context,
                        label,
                        selected = selected,
                    ),
                )
                setOnMarkerClickListener { _, _ ->
                    onAlternateRouteClick?.invoke(index)
                    true
                }
            }
            etaMarkers.add(marker)
            map.overlays.add(marker)
        }
    }

    /**
     * Place ETA clouds along routes so their on-screen footprints stay apart
     * (fixed fractions collapse when routes share a corridor).
     */
    private fun pickSpacedLabelPoints(
        map: MapView,
        routes: List<List<GeoPoint>>,
        selectedIndex: Int,
    ): List<GeoPoint> {
        val density = map.context.resources.displayMetrics.density
        val minSepPx = 150f * density
        val candidates = listOf(0.18, 0.28, 0.38, 0.48, 0.58, 0.68, 0.78)
        val proj = map.projection
        val placedPx = mutableListOf<Point>()
        val result = MutableList(routes.size) { GeoPoint(0.0, 0.0) }
        val order = buildList {
            if (selectedIndex in routes.indices) add(selectedIndex)
            routes.indices.filter { it != selectedIndex }.forEach { add(it) }
        }
        for (index in order) {
            val points = routes[index]
            if (points.size < 2) continue
            var bestPoint = pointAlongRoute(points, 0.45)
            var bestScore = -1.0
            for (frac in candidates) {
                val gp = pointAlongRoute(points, frac)
                val px = Point()
                proj.toPixels(gp, px)
                val minDist = placedPx.minOfOrNull { other ->
                    hypot((other.x - px.x).toDouble(), (other.y - px.y).toDouble())
                } ?: (minSepPx * 4.0)
                // Prefer candidates that clear the min gap; among those, prefer mid-route.
                val midBonus = 1.0 - kotlin.math.abs(frac - 0.48)
                val score = minDist + midBonus * 20.0
                if (score > bestScore) {
                    bestScore = score
                    bestPoint = gp
                }
            }
            result[index] = bestPoint
            val px = Point()
            proj.toPixels(bestPoint, px)
            placedPx.add(Point(px.x, px.y))
            // Softly reject very close picks on the next pass by treating
            // under-separated placements as occupying extra space.
            if (bestScore < minSepPx) {
                placedPx.add(Point(px.x, (px.y - minSepPx).toInt()))
                placedPx.add(Point(px.x, (px.y + minSepPx / 2f).toInt()))
            }
        }
        return result
    }

    /** Exact geometry point used as the speech-bubble tip anchor. */
    private fun pointAlongRoute(points: List<GeoPoint>, fraction: Double): GeoPoint {
        if (points.isEmpty()) return GeoPoint(0.0, 0.0)
        if (points.size == 1) return points[0]
        var total = 0.0
        val segLens = DoubleArray(points.lastIndex) { i ->
            val a = points[i]
            val b = points[i + 1]
            val d = a.distanceToAsDouble(b)
            total += d
            d
        }
        if (total <= 1e-3) {
            val idx = (points.size * fraction).toInt().coerceIn(0, points.lastIndex)
            return points[idx]
        }
        var target = (total * fraction.coerceIn(0.0, 1.0)).coerceIn(0.0, total)
        for (i in segLens.indices) {
            val len = segLens[i]
            if (target <= len || i == segLens.lastIndex) {
                val t = if (len <= 1e-9) 0.0 else (target / len).coerceIn(0.0, 1.0)
                val a = points[i]
                val b = points[i + 1]
                return GeoPoint(
                    a.latitude + (b.latitude - a.latitude) * t,
                    a.longitude + (b.longitude - a.longitude) * t,
                )
            }
            target -= len
        }
        return points.last()
    }

    private fun clearAlternateLines(map: MapView) {
        alternateLines.forEach { map.overlays.remove(it) }
        alternateLines.clear()
    }

    private fun clearEtaMarkers(map: MapView) {
        etaMarkers.forEach { map.overlays.remove(it) }
        etaMarkers.clear()
    }

    private fun rebuildMarkers() {
        // Markers are re-bound when [syncMarkers] runs on the map view.
    }

    fun syncMarkers(map: MapView) {
        markers.forEach { map.overlays.remove(it) }
        markers.clear()
        alerts.forEachIndexed { index, alert ->
            val r = alert.report
            if (!r.hasCoordinates()) return@forEachIndexed
            val (lat, lon) = if (showSidePlacement) {
                TripNavigationMatcher.displayPositionForAlert(routeLatLons, alert)
            } else {
                // Overview: pin at report coords — no L/M/R lane nudge.
                r.latitude to r.longitude
            }
            if (!lat.isFinite() || !lon.isFinite()) return@forEachIndexed
            val m = Marker(map).apply {
                position = GeoPoint(lat, lon)
                title = formatTripPotholeAlertLine(alert)
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setIcon(
                    buildTripPotholeMarkerDrawable(
                        context = map.context,
                        severity = r.severity,
                        sideLabel = if (showSidePlacement) alert.sideLabel else null,
                        numberLabel = if (showSidePlacement) null else (index + 1).toString(),
                    ),
                )
            }
            markers.add(m)
            map.overlays.add(m)
        }
        completedLine.setPoints(completedRoutePoints)
        // Don't resurrect the ribbon while it is disabled for preview.
        if (remainingRibbon.isEnabled) {
            remainingRibbon.setPoints(remainingRoutePoints)
        }
        remainingLine.setPoints(remainingRoutePoints)
    }

    fun clear(map: MapView) {
        remainingRoutePoints = emptyList()
        completedRoutePoints = emptyList()
        routeLatLons = emptyList()
        alerts = emptyList()
        completedLine.setPoints(emptyList())
        remainingRibbon.setPoints(emptyList())
        remainingLine.setPoints(emptyList())
        clearAlternateLines(map)
        clearEtaMarkers(map)
        clearOfferedAlternate(map)
        clearRejoinPath(map)
        markers.forEach { map.overlays.remove(it) }
        markers.clear()
    }
}

/**
 * Teardrop-style pin: severity fill + either placement letter (L/M/R/F) or
 * a distance-order number for map overview.
 */
internal fun buildTripPotholeMarkerDrawable(
    context: Context,
    severity: PotholeSeverity,
    sideLabel: String?,
    numberLabel: String? = null,
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val w = (28f * d).toInt().coerceIn(24, 64)
    val h = (36f * d).toInt().coerceIn(30, 80)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fillArgb = tripPotholeAlertAccentArgb(severity)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillArgb
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * d
    }
    val cx = w / 2f
    val cy = h * 0.38f
    val radius = w * 0.34f
    canvas.drawCircle(cx, cy, radius, fill)
    canvas.drawCircle(cx, cy, radius, stroke)
    // Point toward the road position.
    val tip = android.graphics.Path().apply {
        moveTo(cx - radius * 0.55f, cy + radius * 0.55f)
        lineTo(cx + radius * 0.55f, cy + radius * 0.55f)
        lineTo(cx, h - 2f * d)
        close()
    }
    canvas.drawPath(tip, fill)
    canvas.drawPath(tip, stroke)

    val label = numberLabel?.takeIf { it.isNotBlank() }
        ?: sideBadgeLetter(sideLabel)
    if (label != null) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#111827")
            textAlign = Paint.Align.CENTER
            textSize = if (label.length > 1) 9.5f * d else 11f * d
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, cx, textY, textPaint)
    }
    return BitmapDrawable(context.resources, bmp).apply { setBounds(0, 0, w, h) }
}

/** Speech-cloud badge beside a route: time + distance (e.g. "3 min · 1.3 km"). */
internal fun buildRouteEtaBadgeDrawable(
    context: Context,
    etaLabel: String,
    selected: Boolean,
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val lines = etaLabel.split('\n', limit = 2).map { it.trim() }.filter { it.isNotEmpty() }
    val primary = lines.getOrElse(0) { etaLabel }
    val secondary = lines.getOrNull(1)

    val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) AndroidColor.WHITE else AndroidColor.parseColor("#111827")
        textAlign = Paint.Align.CENTER
        textSize = 13f * d
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) AndroidColor.parseColor("#DBEAFE") else AndroidColor.parseColor("#6B7280")
        textAlign = Paint.Align.CENTER
        textSize = 10f * d
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val padH = 12f * d
    val padTop = 8f * d
    val padBottom = 14f * d // room for cloud tip
    val gap = if (secondary != null) 3f * d else 0f
    val textW = maxOf(
        primaryPaint.measureText(primary),
        secondary?.let { secondaryPaint.measureText(it) } ?: 0f,
    )
    val textBlockH = primaryPaint.textSize + gap + (secondary?.let { secondaryPaint.textSize } ?: 0f)
    val w = (textW + padH * 2f).toInt().coerceAtLeast((56f * d).toInt())
    val h = (textBlockH + padTop + padBottom).toInt().coerceAtLeast((36f * d).toInt())
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) {
            AndroidColor.parseColor("#2563EB")
        } else {
            AndroidColor.WHITE
        }
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) {
            AndroidColor.parseColor("#1D4ED8")
        } else {
            AndroidColor.parseColor("#EA580C")
        }
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * d
    }
    val bubbleBottom = h - 9f * d
    val rect = android.graphics.RectF(2f * d, 2f * d, w - 2f * d, bubbleBottom)
    val radius = 10f * d
    canvas.drawRoundRect(rect, radius, radius, fill)
    canvas.drawRoundRect(rect, radius, radius, stroke)

    // Tip apex is at the very bottom-center of the bitmap (= Marker ANCHOR_BOTTOM on the route).
    val tip = android.graphics.Path().apply {
        val cx = w / 2f
        moveTo(cx - 7f * d, bubbleBottom - 0.5f * d)
        lineTo(cx + 7f * d, bubbleBottom - 0.5f * d)
        lineTo(cx, (h - 1f).toFloat())
        close()
    }
    canvas.drawPath(tip, fill)
    canvas.drawPath(tip, stroke)

    var y = padTop - primaryPaint.ascent()
    canvas.drawText(primary, w / 2f, y, primaryPaint)
    if (secondary != null) {
        y += primaryPaint.textSize + gap
        canvas.drawText(secondary, w / 2f, y - secondaryPaint.ascent() * 0.15f, secondaryPaint)
    }
    return BitmapDrawable(context.resources, bmp).apply { setBounds(0, 0, w, h) }
}

private fun sideBadgeLetter(sideLabel: String?): String? =
    when (TripNavigationMatcher.normalizeSideKey(sideLabel)) {
        "left" -> "L"
        "right" -> "R"
        "middle" -> "M"
        "full" -> "F"
        else -> null
    }
