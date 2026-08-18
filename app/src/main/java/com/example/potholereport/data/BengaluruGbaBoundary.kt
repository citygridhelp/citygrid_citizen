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

    /**
     * Approximate geodesic distance (meters) from [latitude]/[longitude] to the nearest
     * outline segment. Returns [Double.POSITIVE_INFINITY] when the ring is unavailable.
     */
    fun distanceToOutlineM(latitude: Double, longitude: Double): Double {
        val verts = synchronized(lock) { ring }
        if (verts.size < 2) return Double.POSITIVE_INFINITY
        var best = Double.POSITIVE_INFINITY
        for (i in 0 until verts.lastIndex) {
            val a = verts[i]
            val b = verts[i + 1]
            val d = distancePointToSegmentM(
                latitude, longitude,
                a.latitude, a.longitude,
                b.latitude, b.longitude,
            )
            if (d < best) best = d
        }
        // Closed ring: last → first (outlineRing usually already closes; safe either way).
        val first = verts.first()
        val last = verts.last()
        if (first.latitude != last.latitude || first.longitude != last.longitude) {
            val d = distancePointToSegmentM(
                latitude, longitude,
                last.latitude, last.longitude,
                first.latitude, first.longitude,
            )
            if (d < best) best = d
        }
        return best
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

    /** Local ENU approximation — fine for city-scale clearance checks. */
    internal fun distancePointToSegmentM(
        pLat: Double,
        pLon: Double,
        aLat: Double,
        aLon: Double,
        bLat: Double,
        bLon: Double,
    ): Double {
        val midLat = Math.toRadians((aLat + bLat) * 0.5)
        val mPerDegLat = 111_320.0
        val mPerDegLon = 111_320.0 * kotlin.math.cos(midLat).coerceAtLeast(0.2)
        val ax = (aLon) * mPerDegLon
        val ay = (aLat) * mPerDegLat
        val bx = (bLon) * mPerDegLon
        val by = (bLat) * mPerDegLat
        val px = (pLon) * mPerDegLon
        val py = (pLat) * mPerDegLat
        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay
        val ab2 = abx * abx + aby * aby
        if (ab2 < 1e-6) {
            val dx = px - ax
            val dy = py - ay
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }
        val t = ((apx * abx + apy * aby) / ab2).coerceIn(0.0, 1.0)
        val cx = ax + t * abx
        val cy = ay + t * aby
        val dx = px - cx
        val dy = py - cy
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
