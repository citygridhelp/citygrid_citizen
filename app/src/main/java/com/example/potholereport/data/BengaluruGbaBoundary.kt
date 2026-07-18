package com.example.potholereport.data

import android.content.Context
import org.json.JSONObject
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

/**
 * Official Greater Bengaluru Authority (GBA) outer boundary for map chrome, pan limits,
 * and report eligibility. Source: OpenCity / GBA GIS (September 2025 notification).
 *
 * Polygon is Douglas–Peucker simplified (~130 m tolerance) from the published KML.
 * Regenerate: `python tools/generate_bengaluru_gba_boundary_assets.py`
 */
object BengaluruGbaBoundary {

    private const val ASSET_PATH = "bengaluru_gba_boundary.json"

    private val lock = Any()
    private var ring: List<GeoPoint> = emptyList()
    private var bbox: BoundingBox? = null

    fun init(context: Context) {
        synchronized(lock) {
            if (ring.isNotEmpty()) return
            val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val arr = root.getJSONArray("ringLatLon")
            val pts = ArrayList<GeoPoint>(arr.length())
            for (i in 0 until arr.length()) {
                val pair = arr.getJSONArray(i)
                pts.add(GeoPoint(pair.getDouble(0), pair.getDouble(1)))
            }
            ring = pts
            bbox = BoundingBox(
                root.getDouble("bboxNorth"),
                root.getDouble("bboxEast"),
                root.getDouble("bboxSouth"),
                root.getDouble("bboxWest"),
            )
        }
    }

    fun isInitialized(): Boolean = synchronized(lock) { ring.isNotEmpty() }

    fun outlineRing(): List<GeoPoint> = synchronized(lock) { ring }

    fun boundingBox(): BoundingBox? = synchronized(lock) { bbox }

    fun contains(latitude: Double, longitude: Double): Boolean {
        val verts = synchronized(lock) { ring }
        if (verts.size < 3) return false
        return pointInLonLatPolygon(longitude, latitude, verts)
    }

    /** Ray-cast point-in-polygon; [polygon] vertices are lat/lon [GeoPoint]s. */
    private fun pointInLonLatPolygon(lon: Double, lat: Double, polygon: List<GeoPoint>): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val xi = polygon[i].longitude
            val yi = polygon[i].latitude
            val xj = polygon[j].longitude
            val yj = polygon[j].latitude
            val intersect = (yi > lat) != (yj > lat) &&
                lon < (xj - xi) * (lat - yi) / (yj - yi + 1e-15) + xi
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
