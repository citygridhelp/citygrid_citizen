package com.example.potholereport.ui.home

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import com.example.potholereport.data.GbaWardPolygon
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.PotholeSeverity
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.roundToInt

/**
 * Area-division choropleth: GBA ward polygons filled by report density under the active
 * severity filter. Drawn under cluster markers. Color-only for now — tap/name popup is deferred.
 */
internal class AreaDensityHeatOverlay(
    private val mapView: MapView,
    private val polygons: List<GbaWardPolygon>,
    countsByWardKey: Map<String, Int>,
) : Overlay() {

    @Volatile
    var countsByWardKey: Map<String, Int> = countsByWardKey
        private set

    @Volatile
    var drawSuppressed: Boolean = false

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // Soft boundary only — avoid strong red ward lines over the choropleth.
        color = AndroidColor.argb(42, 190, 150, 150)
    }
    private val path = Path()
    private val scr = Point()

    fun updateDensity(counts: Map<String, Int>) {
        countsByWardKey = counts
        mapView.postInvalidate()
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        if (!isEnabled || drawSuppressed || polygons.isEmpty()) return
        val bb = mapView.boundingBox ?: return
        val pad = 0.02
        val n = bb.latNorth + pad
        val s = bb.latSouth - pad
        val e = bb.lonEast + pad
        val w = bb.lonWest - pad
        val counts = countsByWardKey
        val maxCount = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val d = mapView.resources.displayMetrics.density
        strokePaint.strokeWidth = (0.9f * d).coerceIn(0.7f, 1.8f)

        for (poly in polygons) {
            if (poly.bboxSouth > n || poly.bboxNorth < s || poly.bboxWest > e || poly.bboxEast < w) {
                continue
            }
            val count = counts[poly.wardKey] ?: 0
            path.rewind()
            val ring = poly.ring
            if (ring.size < 3) continue
            projection.toPixels(ring[0], scr)
            path.moveTo(scr.x.toFloat(), scr.y.toFloat())
            for (i in 1 until ring.size) {
                projection.toPixels(ring[i], scr)
                path.lineTo(scr.x.toFloat(), scr.y.toFloat())
            }
            path.close()
            fillPaint.color = densityFillArgb(count, maxCount)
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }
    }

    companion object {
        fun densityFillArgb(count: Int, maxCount: Int): Int {
            if (count <= 0) {
                return AndroidColor.argb(28, 255, 220, 220)
            }
            val t = (count.toFloat() / maxCount.coerceAtLeast(1)).coerceIn(0.08f, 1f)
            val r = 255
            val g = (210 - (210 - 40) * t).roundToInt().coerceIn(40, 210)
            val b = (210 - (210 - 45) * t).roundToInt().coerceIn(45, 210)
            val a = (55 + (145 * t)).roundToInt().coerceIn(55, 200)
            return AndroidColor.argb(a, r, g, b)
        }
    }
}

/** Counts reports per ward key; uses stored wardKey or GPS point-in-polygon fallback. */
internal fun countReportsByWardKey(
    reports: List<PersistedPotholeReport>,
    findWardKey: (lat: Double, lon: Double) -> String?,
): Map<String, Int> {
    if (reports.isEmpty()) return emptyMap()
    val out = HashMap<String, Int>(64)
    for (r in reports) {
        if (!r.hasCoordinates()) continue
        val key = r.wardKey.takeIf { it.isNotBlank() }
            ?: findWardKey(r.latitude, r.longitude)
            ?: continue
        out[key] = (out[key] ?: 0) + 1
    }
    return out
}

internal fun reportsForAreaHeat(
    reports: List<PersistedPotholeReport>,
    severityFilter: PotholeSeverity?,
): List<PersistedPotholeReport> =
    if (severityFilter == null) reports else reports.filter { it.severity == severityFilter }
