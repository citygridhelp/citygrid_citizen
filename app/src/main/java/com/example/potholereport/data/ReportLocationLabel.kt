package com.example.potholereport.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.util.Calendar
import java.util.Locale

/** True when the label is a place name, not a house number or postcode. */
fun isMeaningfulAreaName(value: String): Boolean {
    val s = value.trim()
    if (s.length < 2) return false
    if (s.none { it.isLetter() }) return false
    if (s.all { it.isDigit() || it == ' ' }) return false
    // House / unit numbers: "1701", "#12", "12A" (no word chars besides optional suffix letter)
    if (s.matches(Regex("^[#]?\\d+[A-Za-z]?$"))) return false
    // Mostly digits with punctuation
    if (s.replace(Regex("[^0-9]"), "").length >= s.length * 0.7 && s.length <= 8) return false
    // Indian PIN codes
    if (s.matches(Regex("^\\d{6}$"))) return false
    return true
}

/** Neighborhood / landmark label from a reverse-geocode result (never a bare street number). */
fun areaLabelFromAddress(address: Address): String {
    val skipNames = buildSet {
        address.locality?.trim()?.let { add(it.lowercase(Locale.getDefault())) }
        address.subAdminArea?.trim()?.let { add(it.lowercase(Locale.getDefault())) }
        address.adminArea?.trim()?.let { add(it.lowercase(Locale.getDefault())) }
        address.countryName?.trim()?.let { add(it.lowercase(Locale.getDefault())) }
    }

    fun pick(candidate: String?): String? {
        val c = candidate?.trim().orEmpty()
        if (c.isEmpty() || !isMeaningfulAreaName(c)) return null
        if (c.lowercase(Locale.getDefault()) in skipNames) return null
        return c
    }

    val orderedFields = listOf(
        address.subLocality,
        address.featureName,
        address.thoroughfare,
    )
    for (field in orderedFields) {
        pick(field)?.let { return it }
    }

    for (i in 0..address.maxAddressLineIndex) {
        val line = address.getAddressLine(i) ?: continue
        for (part in line.split(',')) {
            pick(part)?.let { return it }
        }
    }

    pick(address.locality)?.let { return it }
    pick(address.subAdminArea)?.let { return it }
    return ""
}

fun resolveAreaLabel(context: Context, latitude: Double, longitude: Double): String {
    if (latitude.isNaN() || longitude.isNaN()) return ""
    if (!Geocoder.isPresent()) return ""
    return runCatching {
        @Suppress("DEPRECATION")
        val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 5)
            ?: return ""
        for (address in addresses) {
            val label = areaLabelFromAddress(address)
            if (label.isNotEmpty()) return label
        }
        ""
    }.getOrDefault("")
}

fun formatRelativeReportAge(createdAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val createdDay = Calendar.getInstance().apply {
        timeInMillis = createdAtMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dayDiff = ((today.timeInMillis - createdDay.timeInMillis) / 86_400_000L).toInt()
    return when (dayDiff) {
        0 -> "Today"
        1 -> "1 day ago"
        else -> "$dayDiff days ago"
    }
}

fun formatRecentReportCaption(createdAtMs: Long, areaLabel: String): String {
    val age = formatRelativeReportAge(createdAtMs)
    val area = areaLabel.trim().takeIf { isMeaningfulAreaName(it) }.orEmpty()
    return if (area.isEmpty()) age else "$age · $area"
}
