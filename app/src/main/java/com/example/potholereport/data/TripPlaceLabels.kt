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
    val adminArea: String? = null,
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
                adminArea = address.adminArea,
                addressLines = lines,
            )
        }
    }
}

/**
 * Builds trip search labels that lead with a recognizable place / society / institution
 * name, then a short area context — Google Maps style, not admin jargon
 * (e.g. never "Bangalore Division").
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

    /**
     * Android / OSM geocoders often put railway or revenue units in [subAdminArea]
     * (e.g. "Bangalore Division", "Bengaluru Urban"). Those are not citizen-facing place names.
     */
    private val administrativeUnitRegex = Regex(
        "(?i)\\b(" +
            "division|district|taluk|taluka|tehsil|mandal|zone|circle|" +
            "corporation|municipality|municipal\\s+corporation|nagar\\s+nigam|" +
            "revenue(\\s+district)?|sub[- ]?division|sub[- ]?district|range|" +
            "commissionerate|police\\s+district|urban\\s+district|rural\\s+district" +
            ")\\b",
    )

    /** Map noisy locality / admin strings to the metro name we show in product UI. */
    private val metroCanonicalRules: List<Pair<Regex, String>> = listOf(
        Regex("(?i).*\\b(bengaluru|bangalore)\\b.*") to "Bengaluru",
        Regex("(?i).*\\b(mumbai|bombay)\\b.*") to "Mumbai",
        Regex("(?i).*\\b(delhi|new delhi)\\b.*") to "Delhi",
        Regex("(?i).*\\bchennai\\b.*|.*\\bmadras\\b.*") to "Chennai",
        Regex("(?i).*\\bhyderabad\\b.*") to "Hyderabad",
        Regex("(?i).*\\bkolkata\\b.*|.*\\bcalcutta\\b.*") to "Kolkata",
        Regex("(?i).*\\bpune\\b.*") to "Pune",
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

    /**
     * City-picker / change-city search: a clean metro or place name (never "BANGALORE DIVISION").
     */
    fun formatCitySearchLabel(fields: PlaceAddressFields): String {
        val locality = cleanAreaPart(fields.locality)
        if (locality != null) return locality
        val fromAdmin = cleanAreaPart(fields.subAdminArea)
        if (fromAdmin != null) return fromAdmin
        val sub = cleanAreaPart(fields.subLocality)
        if (sub != null) return sub
        fields.featureName?.trim()?.takeIf { isPlaceNameCandidate(it) }?.let { return it }
        val line = fields.addressLines.firstOrNull()
            ?.split(',')
            ?.mapNotNull { cleanAreaPart(it) }
            ?.firstOrNull()
        return line ?: "Unknown place"
    }

    fun formatCitySearchLabel(address: Address): String =
        formatCitySearchLabel(PlaceAddressFields.from(address))

    fun pickPlaceTitle(fields: PlaceAddressFields, queryHint: String = ""): String {
        val queryTokens = tokenizeQuery(queryHint)
        val areaNorms = listOfNotNull(
            cleanAreaPart(fields.subLocality),
            cleanAreaPart(fields.locality),
            cleanAreaPart(fields.subAdminArea),
        ).map { it.lowercase(Locale.US) }
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
            .mapNotNull { candidate ->
                // Prefer cleaned form when the segment is an admin wrapper that still maps to a city.
                when {
                    isAdministrativeUnitName(candidate) -> cleanAreaPart(candidate)
                    else -> candidate
                }
            }
            .distinct()

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

            val placeMatch = placeNamed
                .filter { scoreAgainstQuery(it, distinctiveTokens.ifEmpty { queryTokens }) > 0 }
                .maxByOrNull { scoreAgainstQuery(it, queryTokens) }
            if (placeMatch != null) return placeMatch

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
            cleanAreaPart(firstSegment)?.let { return it }
            if (cleaned.length >= 3 && isPlaceNameCandidate(cleaned)) return cleaned
        }
        return listOfNotNull(
            cleanAreaPart(fields.subLocality),
            cleanAreaPart(fields.locality),
            cleanAreaPart(fields.subAdminArea),
        ).firstOrNull()
            ?: "Unknown place"
    }

    fun pickPlaceSubtitle(fields: PlaceAddressFields, title: String): String {
        val titleNorm = title.lowercase(Locale.US)
        // GMaps-style: neighbourhood, city — skip state and admin divisions.
        val parts = listOfNotNull(
            cleanAreaPart(fields.subLocality),
            cleanAreaPart(fields.locality),
            // Only if locality missing: a cleaned admin that resolved to a metro name.
            cleanAreaPart(fields.subAdminArea).takeIf {
                cleanAreaPart(fields.locality) == null
            },
        ).distinct()
            .filter { part ->
                val p = part.lowercase(Locale.US)
                p != titleNorm &&
                    !titleNorm.contains(p) &&
                    !p.contains(titleNorm) &&
                    part !in setOf("India", "Unnamed Road") &&
                    !isAdministrativeUnitName(part)
            }
            .take(2)
        return parts.joinToString(", ")
    }

    /** True when [value] looks like a named place, not "#12" / "Building 41" / PIN / admin unit. */
    fun isPlaceNameCandidate(value: String): Boolean {
        if (!isMeaningfulAreaName(value)) return false
        if (isAdministrativeUnitName(value)) return false
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

    /** Railway / revenue / municipal admin labels that should not appear in trip UI. */
    fun isAdministrativeUnitName(value: String): Boolean {
        val s = value.trim()
        if (s.length < 3) return false
        if (administrativeUnitRegex.containsMatchIn(s)) return true
        // e.g. "Bengaluru Urban", "Bangalore Rural"
        if (s.matches(Regex("(?i)^.+\\s+(urban|rural)$"))) return true
        return false
    }

    /**
     * Drop admin jargon, or rewrite to a canonical metro name when the string
     * clearly refers to one (e.g. "Bangalore Division" → "Bengaluru").
     */
    fun cleanAreaPart(raw: String?): String? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty() || !isMeaningfulAreaName(s)) return null
        if (isAdministrativeUnitName(s)) {
            return canonicalizeMetroName(s)
        }
        return canonicalizeMetroName(s) ?: s
    }

    /** Map "Bangalore" / "Bengaluru Urban" style strings to product metro names. */
    fun canonicalizeMetroName(value: String): String? {
        val s = value.trim()
        if (s.isEmpty()) return null
        for ((regex, canonical) in metroCanonicalRules) {
            if (regex.matches(s) || regex.containsMatchIn(s)) {
                // Only rewrite when the whole string is metro-ish or admin-wrapped metro.
                val low = s.lowercase(Locale.US)
                val isBareOrAdminWrapped = isAdministrativeUnitName(s) ||
                    low == "bengaluru" || low == "bangalore" ||
                    low == "mumbai" || low == "bombay" ||
                    low == "delhi" || low == "new delhi" ||
                    low == "chennai" || low == "madras" ||
                    low == "hyderabad" || low == "kolkata" || low == "calcutta" ||
                    low == "pune"
                if (isBareOrAdminWrapped) return canonical
            }
        }
        return null
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
