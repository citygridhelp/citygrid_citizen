package com.example.potholereport.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlaceHelpersTest {

    @Test
    fun formatTripPlaceAge_relativeBuckets() {
        val now = 1_000_000_000L
        assertEquals("just now", formatTripPlaceAge(now - 10_000L, now))
        assertEquals("5m ago", formatTripPlaceAge(now - 5 * 60_000L, now))
        assertEquals("2h ago", formatTripPlaceAge(now - 2 * 60 * 60_000L, now))
        assertEquals("3d ago", formatTripPlaceAge(now - 3 * 24 * 60 * 60_000L, now))
        assertEquals("", formatTripPlaceAge(0L, now))
    }

    @Test
    fun placeComparator_favoritesFirst() {
        val a = TripRecentPlace("A", 1.0, 1.0, usedAtMs = 10L, isFavorite = false)
        val b = TripRecentPlace("B", 2.0, 2.0, usedAtMs = 1L, isFavorite = true)
        val sorted = listOf(a, b).sortedWith(
            compareByDescending<TripRecentPlace> { it.isFavorite }
                .thenByDescending { it.usedAtMs },
        )
        assertEquals("B", sorted.first().label)
        assertTrue(sorted.first().isFavorite)
    }

    @Test
    fun displayName_prefersSavedNickname() {
        val named = TripRecentPlace(
            label = "Indiranagar 100 Feet Rd",
            latitude = 12.97,
            longitude = 77.64,
            isFavorite = true,
            savedName = "Home",
        )
        assertEquals("Home", named.displayName)
        val plain = TripRecentPlace("Koramangala", 12.93, 77.62)
        assertEquals("Koramangala", plain.displayName)
    }
}
