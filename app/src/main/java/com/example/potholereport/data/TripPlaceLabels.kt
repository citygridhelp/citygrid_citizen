package com.example.potholereport.data

import android.location.Address
import java.util.Locale

/** Platform-free fields used to build trip search labels. */
data class PlaceAddressFields(
    val featureName: String? = null,
    val premises: String? = null,
    val thoroughfare: String? = null,
    val subLocality: String? = null,
    val locality: String? = null,
    val subAdminArea: String? = null,
    val addressLines: List<String> = emptyList(),
) {
    companion object {
        fun from(address: Address): PlaceAddressFields {
            val lines = buildList {
                for (i in 0..address.maxAddressLineIndex.coerceAtMost(3)) {
                    address.getAddressLine(i)?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                }
            }
            return PlaceAddressFields(
                featureName = address.featureName,
                premises = address.premises,
                thoroughfare = address.thoroughfare,
                subLocality = address.subLocality,
                locality = address.locality,
                subAdminArea = address.subAdminArea,
                addressLines = lines,
            )
        }
    }
}

/**
 * Builds trip search labels that lead with a recognizable place / society / institution
 * name, then a short area context — not a building number, tower label, or bare locality.
 */
object TripPlaceLabels {

    private val fillerQueryWords = setOf(
        "in", "at", "near", "the", "and", "of", "to", "for", "on", "by",
        "india", "bengaluru", "bangalore", "mumbai", "delhi", "chennai",
        "hyderabad", "kolkata", "pune",
    )

    private val buildingLabelRegex = Regex(
        "(?i)^(building|bldg\\.?|blk\\.?|block|tower|wing|unit|flat|floor|door|" +
            "house|villa|apt\\.?|apartment|shop|plot|sy\\.?|survey|no\\.?)\\s*[#]?[\\dA-Za-z].*",
    )

    fun formatSearchResult(address: Address, queryHint: String = ""): String =
        formatSearchResult(PlaceAddressFields.from(address), queryHint)

    /**
     * @param queryHint optional user search text; when present, prefer fields
     *   that match those words, or the cleaned query when the geocoder omits the POI name.
     */
    fun formatSearchResult(fields: PlaceAddressFields, queryHint: String = ""): String {
        val title = pickPlaceTitle(fields, queryHint)
        val subtitle = pickPlaceSubtitle(fields, title)
        return if (subtitle.isBlank()) title else "$title · $subtitle"
    }

    fun pickPlaceTitle(fields: PlaceAddressFields, queryHint: String = ""): String {
        val queryTokens = tokenizeQuery(queryHint)
        val areaNorms = listOfNotNull(
            fields.subLocality,
            fields.locality,
            fields.subAdminArea,
        ).map { it.trim().lowercase(Locale.US) }
            .filter { it.isNotEmpty() }
            .toSet()

        val candidates = buildList {
            fields.featureName?.trim()?.let { add(it) }
            fields.premises?.trim()?.let { add(it) }
            fields.addressLines.forEach { line ->
                line.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(it) }
            }
            fields.thoroughfare?.trim()?.let { add(it) }
            fields.subLocality?.trim()?.let { add(it) }
        }.distinct()

        val named = candidates.filter { isPlaceNameCandidate(it) }
        val placeNamed = named.filter { candidate ->
            val n = candidate.lowercase(Locale.US)
            n !in areaNorms
        }

        if (queryTokens.isNotEmpty()) {
            val distinctiveTokens = queryTokens.filter { token ->
                areaNorms.none { area ->
                    area == token || area.contains(token) || token.contains(area)
                }
            }

            // Prefer society / POI names that match the distinctive search words.
            val placeMatch = placeNamed
                .filter { scoreAgainstQuery(it, distinctiveTokens.ifEmpty { queryTokens }) > 0 }
                .maxByOrNull { scoreAgainstQuery(it, queryTokens) }
            if (placeMatch != null) return placeMatch

            // Strong non-area match even if some query tokens are area words.
            val strong = named
                .filter { candidate ->
                    val n = candidate.lowercase(Locale.US)
                    n !in areaNorms &&
                        (distinctiveTokens.isEmpty() ||
                            distinctiveTokens.any { token -> n.contains(token) })
                }
                .maxByOrNull { scoreAgainstQuery(it, queryTokens) }
            if (strong != null && scoreAgainstQuery(strong, queryTokens) > 0) {
                return strong
            }

            // Geocoder often returns "Building 41" / locality only — keep the user's place name.
            if (distinctiveTokens.isNotEmpty()) {
                titleFromQuery(queryHint)?.let { return it }
            }
        }

        placeNamed.firstOrNull()?.let { return it }
        named.firstOrNull()?.let { return it }

        val line = fields.addressLines.firstOrNull().orEmpty()
        if (line.length >= 4) {
            val cleaned = stripLeadingAddressNumber(line)
            val firstSegment = cleaned.split(',').firstOrNull()?.trim().orEmpty()
            if (isPlaceNameCandidate(firstSegment)) return firstSegment
            if (cleaned.length >= 3 && isPlaceNameCandidate(cleaned)) return cleaned
            if (line.length >= 3 && isPlaceNameCandidate(line)) return line
        }
        return listOfNotNull(
            fields.subLocality,
            fields.locality,
            fields.subAdminArea,
        ).mapNotNull { it?.trim()?.takeIf { s -> s.isNotEmpty() } }
            .firstOrNull()
            ?: "Unknown place"
    }

    fun pickPlaceSubtitle(fields: PlaceAddressFields, title: String): String {
        val titleNorm = title.lowercase(Locale.US)
        val parts = listOf(
            fields.subLocality,
            fields.locality,
            fields.subAdminArea,
        ).mapNotNull { it?.trim()?.takeIf { s -> s.isNotEmpty() } }
            .distinct()
            .filter { part ->
                val p = part.lowercase(Locale.US)
                p != titleNorm &&
                    !titleNorm.contains(p) &&
                    !p.contains(titleNorm) &&
                    part !in setOf("India", "Unnamed Road")
            }
            .take(2)
        return parts.joinToString(", ")
    }

    /** True when [value] looks like a named place, not "#12" / "Building 41" / PIN. */
    fun isPlaceNameCandidate(value: String): Boolean {
        if (!isMeaningfulAreaName(value)) return false
        val s = value.trim()
        if (buildingLabelRegex.matches(s)) return false
        if (s.matches(Regex("(?i)^\\d+[A-Za-z]?\\s*(st|nd|rd|th)?\\s*(main|cross|road|rd|st|street)?$"))) {
            return false
        }
        if (s.matches(Regex("(?i)^(sy\\.?|survey|plot|door|flat|apt|apartment|no\\.?)\\s*\\d+.*"))) {
            return false
        }
        return true
    }

    /** Title-case a cleaned user query for use when geocoder omits the POI name. */
    internal fun titleFromQuery(query: String): String? {
        val words = query.trim()
            .split(Regex("\\s+"))
            .map { it.trim().trim(',', '.', ';') }
            .filter { it.isNotEmpty() }
            .filter { word ->
                val low = word.lowercase(Locale.US)
                low !in fillerQueryWords &&
                    !low.matches(Regex("\\d{6}")) &&
                    low.any { it.isLetter() }
            }
        if (words.isEmpty()) return null
        val titled = words.joinToString(" ") { word ->
            if (word.all { it.isDigit() || it == '#' }) {
                word
            } else {
                word.lowercase(Locale.US).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }
            }
        }
        return titled.takeIf { it.length >= 3 && isPlaceNameCandidate(it) }
    }

    private fun stripLeadingAddressNumber(line: String): String {
        val stripped = line.replace(Regex("^\\s*[#]?\\d+[A-Za-z]?\\s*[,\\-]\\s*"), "")
        return stripped.ifBlank { line }
    }

    private fun tokenizeQuery(query: String): List<String> =
        query.lowercase(Locale.US)
            .split(Regex("[^a-z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in fillerQueryWords }

    private fun scoreAgainstQuery(candidate: String, queryTokens: List<String>): Int {
        if (queryTokens.isEmpty()) return 0
        val hay = candidate.lowercase(Locale.US)
        var matched = 0
        var score = 0
        for (token in queryTokens) {
            if (hay.contains(token)) {
                matched += 1
                score += 3 + token.length
            }
        }
        if (matched == 0) return 0
        if (candidate.contains(' ')) score += 2
        if (candidate.length in 8..64) score += 1
        return score
    }
}
