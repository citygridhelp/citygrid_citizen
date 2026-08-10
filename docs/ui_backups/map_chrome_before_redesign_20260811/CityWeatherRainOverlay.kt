package com.example.potholereport.ui.home

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import com.example.potholereport.data.CityWeatherRainCell
import com.example.potholereport.data.CityWeatherRainColors
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

/**
 * Soft rain-risk grid over the metro (under report clusters). Color-only; no tap popup.
 */
internal class CityWeatherRainOverlay(
    private val mapView: MapView,
    cells: List<CityWeatherRainCell>,
) : Overlay() {

    @Volatile
    var cells: List<CityWeatherRainCell> = cells
        private set

    @Volatile
    var drawSuppressed: Boolean = false

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = AndroidColor.argb(28, 120, 170, 190)
    }
    private val path = Path()
    private val scr = Point()

    fun updateCells(next: List<CityWeatherRainCell>) {
        cells = next
        mapView.postInvalidate()
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        if (!isEnabled || drawSuppressed || cells.isEmpty()) return
        val bb = mapView.boundingBox ?: return
        val pad = 0.02
        val n = bb.latNorth + pad
        val s = bb.latSouth - pad
        val e = bb.lonEast + pad
        val w = bb.lonWest - pad
        val d = mapView.resources.displayMetrics.density
        strokePaint.strokeWidth = (0.7f * d).coerceIn(0.5f, 1.4f)

        for (cell in cells) {
            if (cell.risk01 <= 0.05f) continue
            if (cell.south > n || cell.north < s || cell.west > e || cell.east < w) continue
            val ring = cell.ringLatLon
            if (ring.size < 3) continue
            path.rewind()
            projection.toPixels(GeoPoint(ring[0].first, ring[0].second), scr)
            path.moveTo(scr.x.toFloat(), scr.y.toFloat())
            for (i in 1 until ring.size) {
                projection.toPixels(GeoPoint(ring[i].first, ring[i].second), scr)
                path.lineTo(scr.x.toFloat(), scr.y.toFloat())
            }
            path.close()
            fillPaint.color = CityWeatherRainColors.riskFillArgb(cell.risk01)
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }
    }
}
