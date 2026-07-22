package com.example.potholereport.ui.home

import android.graphics.Paint
import com.example.potholereport.data.PotholeSeverity
import com.example.potholereport.data.TripPotholeAlert
import com.example.potholereport.data.formatTripPotholeAlertLine
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color as AndroidColor

/**
 * Draws trip route polyline (completed grey + remaining blue) and upcoming pothole markers.
 */
class TripNavigationOverlay : Overlay() {

    var remainingRoutePoints: List<GeoPoint> = emptyList()
        set(value) {
            field = value
            remainingLine.setPoints(value)
        }

    var completedRoutePoints: List<GeoPoint> = emptyList()
        set(value) {
            field = value
            completedLine.setPoints(value)
        }

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

    private val completedLine = Polyline().apply {
        outlinePaint.color = AndroidColor.parseColor("#9CA3AF")
        outlinePaint.strokeWidth = 8f
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.isAntiAlias = true
        outlinePaint.alpha = 180
    }

    private val remainingLine = Polyline().apply {
        outlinePaint.color = AndroidColor.parseColor("#2563EB")
        outlinePaint.strokeWidth = 8f
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.isAntiAlias = true
    }

    private val markers = mutableListOf<Marker>()

    fun attachTo(map: MapView) {
        if (!map.overlays.contains(completedLine)) {
            map.overlays.add(0, completedLine)
        }
        if (!map.overlays.contains(remainingLine)) {
            val completedIdx = map.overlays.indexOf(completedLine)
            val insertAt = if (completedIdx >= 0) completedIdx + 1 else 0
            map.overlays.add(insertAt, remainingLine)
        }
    }

    fun detachFrom(map: MapView) {
        map.overlays.remove(completedLine)
        map.overlays.remove(remainingLine)
        markers.forEach { map.overlays.remove(it) }
        markers.clear()
    }

    private fun rebuildMarkers() {
        // Markers are re-bound when [syncMarkers] runs on the map view.
    }

    fun syncMarkers(map: MapView) {
        markers.forEach { map.overlays.remove(it) }
        markers.clear()
        for (alert in alerts) {
            val r = alert.report
            if (!r.hasCoordinates()) continue
            val m = Marker(map).apply {
                position = GeoPoint(r.latitude, r.longitude)
                title = formatTripPotholeAlertLine(alert)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = map.context.getDrawable(
                    when (r.severity) {
                        PotholeSeverity.CRITICAL -> android.R.drawable.presence_busy
                        PotholeSeverity.SEVERE -> android.R.drawable.presence_away
                        else -> android.R.drawable.presence_online
                    },
                )
            }
            markers.add(m)
            map.overlays.add(m)
        }
        completedLine.setPoints(completedRoutePoints)
        remainingLine.setPoints(remainingRoutePoints)
    }

    fun clear(map: MapView) {
        remainingRoutePoints = emptyList()
        completedRoutePoints = emptyList()
        alerts = emptyList()
        completedLine.setPoints(emptyList())
        remainingLine.setPoints(emptyList())
        markers.forEach { map.overlays.remove(it) }
        markers.clear()
    }
}
