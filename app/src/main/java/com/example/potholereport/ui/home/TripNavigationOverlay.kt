package com.example.potholereport.ui.home

import android.graphics.Paint
import com.example.potholereport.data.PotholeSeverity
import com.example.potholereport.data.TripPotholeAlert
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color as AndroidColor

/**
 * Draws trip route polyline and upcoming pothole markers on the home map.
 */
class TripNavigationOverlay : Overlay() {

    var routePoints: List<GeoPoint> = emptyList()
        set(value) {
            field = value
            routeLine.setPoints(value)
        }

  var alerts: List<TripPotholeAlert> = emptyList()
        set(value) {
            field = value
            rebuildMarkers()
        }

    private val routeLine = Polyline().apply {
        outlinePaint.color = AndroidColor.parseColor("#2563EB")
        outlinePaint.strokeWidth = 8f
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.isAntiAlias = true
    }

    private val markers = mutableListOf<Marker>()

    fun attachTo(map: MapView) {
        if (!map.overlays.contains(routeLine)) {
            map.overlays.add(0, routeLine)
        }
    }

    fun detachFrom(map: MapView) {
        map.overlays.remove(routeLine)
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
                title = buildString {
                    append(alert.distanceMeters.toInt())
                    append(" m")
                    alert.sideLabel?.let { append(" · ").append(it) }
                }
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
        routeLine.setPoints(routePoints)
    }

    fun clear(map: MapView) {
        routePoints = emptyList()
        alerts = emptyList()
        routeLine.setPoints(emptyList())
        markers.forEach { map.overlays.remove(it) }
        markers.clear()
    }
}
