package com.example.potholereport.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Survives leaving Home (e.g. mid-trip pothole report) so navigation is not wiped
 * when [ReportAndTrackSection] leaves composition.
 */
object TripSessionRepository {

    private const val PREFS = "trip_session_prefs"
    private const val KEY_SNAPSHOT = "snapshot_json"

    @Volatile
    private var memory: TripSessionSnapshot? = null

    fun isNavigating(): Boolean = memory?.tripNavActive == true || memory?.tripModeEnabled == true

    fun isTripNavActive(): Boolean = memory?.tripNavActive == true

    fun current(): TripSessionSnapshot? = memory

    fun clear(context: Context? = null) {
        memory = null
        context?.applicationContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(KEY_SNAPSHOT)
            ?.apply()
    }

    fun save(context: Context, snapshot: TripSessionSnapshot) {
        memory = snapshot
        val ctx = context.applicationContext
        runCatching {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SNAPSHOT, snapshot.toJson())
                .apply()
        }
    }

    fun restore(context: Context): TripSessionSnapshot? {
        memory?.let { return it }
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SNAPSHOT, null)
            ?: return null
        val parsed = TripSessionSnapshot.fromJson(raw) ?: return null
        memory = parsed
        return parsed
    }

    fun updateRoute(
        context: Context,
        routePoints: List<Pair<Double, Double>>,
        durationSec: Double?,
    ) {
        val cur = memory ?: return
        save(
            context,
            cur.copy(
                routePoints = routePoints,
                routeDurationSec = durationSec,
            ),
        )
    }
}

data class TripSessionSnapshot(
    val tripModeEnabled: Boolean,
    val tripNavActive: Boolean,
    val tripStreetFollow: Boolean,
    val tripCameraMode: String = if (tripStreetFollow) {
        TripCameraMode.STREET.name
    } else {
        TripCameraMode.MAP.name
    },
    val vehicleName: String,
    val destLat: Double?,
    val destLon: Double?,
    val destLabel: String,
    val startLat: Double?,
    val startLon: Double?,
    val startLabel: String,
    val navOriginLat: Double?,
    val navOriginLon: Double?,
    val routePoints: List<Pair<Double, Double>>,
    val routeDurationSec: Double?,
) {
    fun toJson(): String {
        val o = JSONObject()
        o.put("tripModeEnabled", tripModeEnabled)
        o.put("tripNavActive", tripNavActive)
        o.put("tripStreetFollow", tripStreetFollow)
        o.put("tripCameraMode", tripCameraMode)
        o.put("vehicleName", vehicleName)
        o.put("destLat", destLat ?: JSONObject.NULL)
        o.put("destLon", destLon ?: JSONObject.NULL)
        o.put("destLabel", destLabel)
        o.put("startLat", startLat ?: JSONObject.NULL)
        o.put("startLon", startLon ?: JSONObject.NULL)
        o.put("startLabel", startLabel)
        o.put("navOriginLat", navOriginLat ?: JSONObject.NULL)
        o.put("navOriginLon", navOriginLon ?: JSONObject.NULL)
        o.put("routeDurationSec", routeDurationSec ?: JSONObject.NULL)
        val arr = JSONArray()
        for ((lat, lon) in routePoints) {
            arr.put(JSONArray().put(lat).put(lon))
        }
        o.put("routePoints", arr)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String): TripSessionSnapshot? = runCatching {
            val o = JSONObject(raw)
            fun dbl(key: String): Double? =
                if (o.isNull(key)) null else o.getDouble(key)
            val points = mutableListOf<Pair<Double, Double>>()
            val arr = o.optJSONArray("routePoints")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val pair = arr.optJSONArray(i) ?: continue
                    if (pair.length() >= 2) {
                        points.add(pair.getDouble(0) to pair.getDouble(1))
                    }
                }
            }
            val streetFollow = o.optBoolean("tripStreetFollow", true)
            val cameraMode = TripCameraMode.fromStorage(
                if (o.has("tripCameraMode")) o.optString("tripCameraMode") else null,
                streetFollowLegacy = streetFollow,
            )
            TripSessionSnapshot(
                tripModeEnabled = o.optBoolean("tripModeEnabled", false),
                tripNavActive = o.optBoolean("tripNavActive", false),
                tripStreetFollow = cameraMode.isHeadingUp,
                tripCameraMode = cameraMode.name,
                vehicleName = o.optString("vehicleName", "CAR"),
                destLat = dbl("destLat"),
                destLon = dbl("destLon"),
                destLabel = o.optString("destLabel", ""),
                startLat = dbl("startLat"),
                startLon = dbl("startLon"),
                startLabel = o.optString("startLabel", ""),
                navOriginLat = dbl("navOriginLat"),
                navOriginLon = dbl("navOriginLon"),
                routePoints = points,
                routeDurationSec = dbl("routeDurationSec"),
            )
        }.getOrNull()
    }
}
