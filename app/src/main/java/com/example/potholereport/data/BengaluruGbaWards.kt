package com.example.potholereport.data

import android.content.Context
import org.json.JSONObject
import org.osmdroid.util.GeoPoint

/** Resolved GBA ward from official delimitation GIS (point-in-polygon at submit / map heat). */
data class GbaWardMatch(
    val wardKey: String,
    val wardNumber: Int,
    val wardName: String,
    val corporationKey: String,
    val corporationLabel: String,
)

/** Full ward polygon for map choropleth / hit-testing (Namma Kasa–style area layer). */
data class GbaWardPolygon(
    val wardKey: String,
    val wardNumber: Int,
    val wardName: String,
    val corporationKey: String,
    val corporationLabel: String,
    val bboxNorth: Double,
    val bboxSouth: Double,
    val bboxEast: Double,
    val bboxWest: Double,
    val ring: List<GeoPoint>,
) {
    fun toMatch(): GbaWardMatch = GbaWardMatch(
        wardKey = wardKey,
        wardNumber = wardNumber,
        wardName = wardName,
        corporationKey = corporationKey,
        corporationLabel = corporationLabel,
    )
}

/**
 * Official GBA ward boundaries (369 wards, Nov 2025).
 * Used for report submit routing and optional citizen map area-density heat.
 *
 * Regenerate: `python tools/generate_bengaluru_gba_wards_assets.py`
 */
object BengaluruGbaWards {

    private const val ASSET_PATH = "bengaluru_gba_wards.json"

    private val lock = Any()
    private var wards: List<GbaWardPolygon> = emptyList()

    fun init(context: Context) {
        synchronized(lock) {
            if (wards.isNotEmpty()) return
            val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val arr = root.getJSONArray("wards")
            val list = ArrayList<GbaWardPolygon>(arr.length())
            for (i in 0 until arr.length()) {
                val w = arr.getJSONObject(i)
                val ringArr = w.getJSONArray("ringLatLon")
                val ring = ArrayList<GeoPoint>(ringArr.length())
                for (j in 0 until ringArr.length()) {
                    val pair = ringArr.getJSONArray(j)
                    ring.add(GeoPoint(pair.getDouble(0), pair.getDouble(1)))
                }
                list.add(
                    GbaWardPolygon(
                        wardKey = w.getString("wardKey"),
                        wardNumber = w.getInt("wardNumber"),
                        wardName = w.getString("wardName"),
                        corporationKey = w.getString("corporationKey"),
                        corporationLabel = w.getString("corporationLabel"),
                        bboxNorth = w.getDouble("bboxNorth"),
                        bboxSouth = w.getDouble("bboxSouth"),
                        bboxEast = w.getDouble("bboxEast"),
                        bboxWest = w.getDouble("bboxWest"),
                        ring = ring,
                    ),
                )
            }
            wards = list
        }
    }

    fun isInitialized(): Boolean = synchronized(lock) { wards.isNotEmpty() }

    fun wardCount(): Int = synchronized(lock) { wards.size }

    /** Snapshot of all ward polygons (empty until [init]). */
    fun allPolygons(): List<GbaWardPolygon> = synchronized(lock) { wards }

    /** Returns the ward whose polygon contains [latitude]/[longitude], or null. */
    fun findWard(latitude: Double, longitude: Double): GbaWardMatch? =
        findPolygon(latitude, longitude)?.toMatch()

    fun findPolygon(latitude: Double, longitude: Double): GbaWardPolygon? {
        val candidates = synchronized(lock) { wards }
        if (candidates.isEmpty()) return null
        for (w in candidates) {
            if (latitude > w.bboxNorth || latitude < w.bboxSouth ||
                longitude > w.bboxEast || longitude < w.bboxWest
            ) {
                continue
            }
            if (pointInRing(longitude, latitude, w.ring)) {
                return w
            }
        }
        return null
    }

    private fun pointInRing(lon: Double, lat: Double, ring: List<GeoPoint>): Boolean {
        var inside = false
        var j = ring.lastIndex
        for (i in ring.indices) {
            val xi = ring[i].longitude
            val yi = ring[i].latitude
            val xj = ring[j].longitude
            val yj = ring[j].latitude
            val intersect = (yi > lat) != (yj > lat) &&
                lon < (xj - xi) * (lat - yi) / (yj - yi + 1e-15) + xi
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
