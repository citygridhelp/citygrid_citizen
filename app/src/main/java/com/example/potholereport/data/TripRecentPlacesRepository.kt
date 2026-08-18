package com.example.potholereport.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TripRecentPlace(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val usedAtMs: Long = 0L,
    val isFavorite: Boolean = false,
    /** User-chosen name when saved (e.g. Home, Work). Null = untitled favorite. */
    val savedName: String? = null,
) {
    /** Name shown in lists: saved nickname, else address/search label. */
    val displayName: String
        get() = savedName?.trim()?.takeIf { it.isNotEmpty() } ?: label
}

/** Device-local recent + saved places for the trip planner. */
object TripRecentPlacesRepository {

    private const val PREFS_NAME = "trip_recent_places"
    private const val KEY_DESTINATIONS = "destinations_v2"
    private const val KEY_DESTINATIONS_LEGACY = "destinations"
    private const val KEY_STARTS = "starts_v2"
    private const val MAX_DESTINATIONS = 12
    private const val MAX_STARTS = 8
    private const val MAX_FAVORITES = 6

    /** Suggested nicknames when saving a place. */
    val SAVE_NAME_PRESETS: List<String> = listOf("Home", "Work", "Gym", "School", "Family", "Other")

    fun destinations(context: Context): List<TripRecentPlace> =
        loadPlaces(context, KEY_DESTINATIONS, KEY_DESTINATIONS_LEGACY)

    fun starts(context: Context): List<TripRecentPlace> =
        loadPlaces(context, KEY_STARTS, legacyKey = null)

    fun rememberDestination(
        context: Context,
        label: String,
        latitude: Double,
        longitude: Double,
    ): List<TripRecentPlace> =
        rememberPlace(context, KEY_DESTINATIONS, destinations(context), label, latitude, longitude, MAX_DESTINATIONS)

    fun rememberStart(
        context: Context,
        label: String,
        latitude: Double,
        longitude: Double,
    ): List<TripRecentPlace> =
        rememberPlace(context, KEY_STARTS, starts(context), label, latitude, longitude, MAX_STARTS)

    /**
     * Save or update a named favorite destination (Home, Work, …).
     * Replaces an existing favorite at the same coordinates.
     */
    fun saveNamedDestination(
        context: Context,
        label: String,
        latitude: Double,
        longitude: Double,
        savedName: String,
    ): List<TripRecentPlace> {
        val name = savedName.trim().ifBlank { return destinations(context) }
        val cleanLabel = label.trim().ifBlank { name }
        if (!latitude.isFinite() || !longitude.isFinite()) return destinations(context)

        val current = destinations(context)
        val withoutSameCoords = current.filterNot {
            coordinatesMatch(it.latitude, it.longitude, latitude, longitude)
        }
        // One place per nickname: drop older favorite with the same saved name.
        val withoutSameName = withoutSameCoords.filterNot {
            it.isFavorite && it.savedName?.equals(name, ignoreCase = true) == true
        }
        val favoriteCount = withoutSameName.count { it.isFavorite }
        if (favoriteCount >= MAX_FAVORITES) {
            return current
        }
        val updated = (
            listOf(
                TripRecentPlace(
                    label = cleanLabel,
                    latitude = latitude,
                    longitude = longitude,
                    usedAtMs = System.currentTimeMillis(),
                    isFavorite = true,
                    savedName = name,
                ),
            ) + withoutSameName
            ).sortedWith(placeComparator()).take(MAX_DESTINATIONS)
        persist(context, KEY_DESTINATIONS, updated)
        return updated
    }

    /** Star toggle without a name (legacy). Prefer [saveNamedDestination] for new saves. */
    fun toggleFavoriteDestination(
        context: Context,
        label: String,
        latitude: Double,
        longitude: Double,
    ): List<TripRecentPlace> {
        val current = destinations(context)
        val existing = current.firstOrNull {
            coordinatesMatch(it.latitude, it.longitude, latitude, longitude)
        }
        if (existing?.isFavorite == true) {
            // Unsave
            val updated = current.map {
                if (coordinatesMatch(it.latitude, it.longitude, latitude, longitude)) {
                    it.copy(isFavorite = false, savedName = null, usedAtMs = System.currentTimeMillis())
                } else {
                    it
                }
            }.sortedWith(placeComparator())
            persist(context, KEY_DESTINATIONS, updated)
            return updated
        }
        // Star without name — keep for quick save; UI should offer naming.
        val favoriteCount = current.count { it.isFavorite }
        if (favoriteCount >= MAX_FAVORITES) return current
        val updated = if (existing != null) {
            current.map {
                if (coordinatesMatch(it.latitude, it.longitude, latitude, longitude)) {
                    it.copy(isFavorite = true, usedAtMs = System.currentTimeMillis())
                } else {
                    it
                }
            }
        } else {
            listOf(
                TripRecentPlace(
                    label = label.trim().ifBlank { "Saved place" },
                    latitude = latitude,
                    longitude = longitude,
                    usedAtMs = System.currentTimeMillis(),
                    isFavorite = true,
                ),
            ) + current
        }.sortedWith(placeComparator()).take(MAX_DESTINATIONS)
        persist(context, KEY_DESTINATIONS, updated)
        return updated
    }

    fun removeDestination(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): List<TripRecentPlace> {
        val updated = destinations(context).filterNot {
            coordinatesMatch(it.latitude, it.longitude, latitude, longitude)
        }
        persist(context, KEY_DESTINATIONS, updated)
        return updated
    }

    fun removeStart(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): List<TripRecentPlace> {
        val updated = starts(context).filterNot {
            coordinatesMatch(it.latitude, it.longitude, latitude, longitude)
        }
        persist(context, KEY_STARTS, updated)
        return updated
    }

    private fun rememberPlace(
        context: Context,
        key: String,
        current: List<TripRecentPlace>,
        label: String,
        latitude: Double,
        longitude: Double,
        max: Int,
    ): List<TripRecentPlace> {
        val cleanLabel = label.trim()
        if (cleanLabel.isBlank() || !latitude.isFinite() || !longitude.isFinite()) {
            return current
        }
        val previous = current.firstOrNull {
            coordinatesMatch(it.latitude, it.longitude, latitude, longitude)
        }
        val updated = buildList {
            add(
                TripRecentPlace(
                    label = cleanLabel,
                    latitude = latitude,
                    longitude = longitude,
                    usedAtMs = System.currentTimeMillis(),
                    isFavorite = previous?.isFavorite == true,
                    savedName = previous?.savedName,
                ),
            )
            addAll(
                current.filterNot {
                    coordinatesMatch(it.latitude, it.longitude, latitude, longitude)
                },
            )
        }.sortedWith(placeComparator()).take(max)
        persist(context, key, updated)
        return updated
    }

    private fun placeComparator(): Comparator<TripRecentPlace> =
        compareByDescending<TripRecentPlace> { it.isFavorite }
            .thenByDescending { it.usedAtMs }

    private fun loadPlaces(
        context: Context,
        key: String,
        legacyKey: String?,
    ): List<TripRecentPlace> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null)
            ?: legacyKey?.let { prefs.getString(it, null) }
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val label = item.optString("label").trim()
                    val latitude = item.optDouble("latitude", Double.NaN)
                    val longitude = item.optDouble("longitude", Double.NaN)
                    val usedAtMs = item.optLong("usedAtMs", 0L)
                    val isFavorite = item.optBoolean("isFavorite", false)
                    val savedName = item.optString("savedName", "").trim().takeIf { it.isNotEmpty() }
                    if (label.isNotBlank() && latitude.isFinite() && longitude.isFinite()) {
                        add(
                            TripRecentPlace(
                                label = label,
                                latitude = latitude,
                                longitude = longitude,
                                usedAtMs = usedAtMs,
                                isFavorite = isFavorite,
                                savedName = savedName,
                            ),
                        )
                    }
                }
            }.sortedWith(placeComparator())
        }.getOrDefault(emptyList())
    }

    private fun persist(context: Context, key: String, places: List<TripRecentPlace>) {
        val json = JSONArray().apply {
            places.forEach { place ->
                put(
                    JSONObject()
                        .put("label", place.label)
                        .put("latitude", place.latitude)
                        .put("longitude", place.longitude)
                        .put("usedAtMs", place.usedAtMs)
                        .put("isFavorite", place.isFavorite)
                        .put("savedName", place.savedName.orEmpty()),
                )
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, json.toString())
            .apply()
    }

    private fun coordinatesMatch(
        firstLat: Double,
        firstLon: Double,
        secondLat: Double,
        secondLon: Double,
    ): Boolean =
        kotlin.math.abs(firstLat - secondLat) < 0.0001 &&
            kotlin.math.abs(firstLon - secondLon) < 0.0001
}

/** Short relative age for recent-place rows. */
fun formatTripPlaceAge(usedAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (usedAtMs <= 0L) return ""
    val delta = (nowMs - usedAtMs).coerceAtLeast(0L)
    val minutes = delta / 60_000L
    return when {
        minutes < 1L -> "just now"
        minutes < 60L -> "${minutes}m ago"
        minutes < 60L * 24L -> "${minutes / 60L}h ago"
        else -> "${minutes / (60L * 24L)}d ago"
    }
}
