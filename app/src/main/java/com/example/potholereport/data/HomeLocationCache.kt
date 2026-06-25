package com.example.potholereport.data

import android.content.Context
import android.location.Location

/**
 * Process + disk cache for the last successful GPS metro fix.
 * Survives activity recreate (sign-in keyboard, minimize) so home does not cold-start GPS again.
 */
object HomeLocationCache {

    private const val PREFS = "home_location_cache"
    private const val KEY_LAT = "lat"
    private const val KEY_LNG = "lng"
    private const val KEY_METRO = "metro"
    private const val KEY_FIX_MS = "fix_ms"
    private const val KEY_BOOTSTRAP_DONE = "bootstrap_done"

    /** In-memory mirror; survives activity recreate within the same process. */
    @Volatile
    private var memorySnapshot: Snapshot? = null

    data class Snapshot(
        val latitude: Double,
        val longitude: Double,
        val metroCity: String?,
        val fixTimeMs: Long,
        val bootstrapCompleted: Boolean,
    ) {
        fun toLocation(): Location = Location("cached").apply {
            latitude = this@Snapshot.latitude
            longitude = this@Snapshot.longitude
            time = fixTimeMs.coerceAtLeast(0L)
        }

        fun isFresh(maxAgeMs: Long = FRESH_MS): Boolean {
            if (fixTimeMs <= 0L) return false
            if (latitude.isNaN() || longitude.isNaN()) return false
            if (System.currentTimeMillis() - fixTimeMs >= maxAgeMs) return false
            return !metroCity.isNullOrBlank()
        }
    }

    const val FRESH_MS = 30 * 60 * 1_000L

    fun load(context: Context): Snapshot? {
        memorySnapshot?.let { return it }
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAT) || !prefs.contains(KEY_LNG)) return null
        val lat = prefs.getFloat(KEY_LAT, Float.NaN).toDouble()
        val lng = prefs.getFloat(KEY_LNG, Float.NaN).toDouble()
        if (lat.isNaN() || lng.isNaN()) return null
        return Snapshot(
            latitude = lat,
            longitude = lng,
            metroCity = prefs.getString(KEY_METRO, null)?.takeIf { it.isNotBlank() },
            fixTimeMs = prefs.getLong(KEY_FIX_MS, 0L),
            bootstrapCompleted = prefs.getBoolean(KEY_BOOTSTRAP_DONE, false),
        ).also { memorySnapshot = it }
    }

    fun save(
        context: Context,
        location: Location,
        metroCity: String?,
        markBootstrapComplete: Boolean = true,
    ) {
        val fixMs = location.time.coerceAtLeast(System.currentTimeMillis())
        val snapshot = Snapshot(
            latitude = location.latitude,
            longitude = location.longitude,
            metroCity = metroCity?.takeIf { it.isNotBlank() },
            fixTimeMs = fixMs,
            bootstrapCompleted = markBootstrapComplete ||
                (memorySnapshot?.bootstrapCompleted == true) ||
                load(context)?.bootstrapCompleted == true,
        )
        memorySnapshot = snapshot
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_LAT, snapshot.latitude.toFloat())
            .putFloat(KEY_LNG, snapshot.longitude.toFloat())
            .putString(KEY_METRO, snapshot.metroCity)
            .putLong(KEY_FIX_MS, snapshot.fixTimeMs)
            .putBoolean(KEY_BOOTSTRAP_DONE, snapshot.bootstrapCompleted)
            .apply()
    }

    fun hasFreshFix(context: Context, maxAgeMs: Long = FRESH_MS): Boolean =
        load(context)?.isFresh(maxAgeMs) == true

    fun bootstrapCompleted(context: Context): Boolean =
        load(context)?.bootstrapCompleted == true || memorySnapshot?.bootstrapCompleted == true
}
