package com.example.potholereport.data

enum class PotholeSeverity(val code: String, val title: String, val blurb: String) {
    MINOR("01", "MINOR", "Small crack or shallow dip"),
    MODERATE("02", "MODERATE", "Noticeable, avoidable"),
    SEVERE("03", "SEVERE", "Damages vehicles, hard to avoid"),
    CRITICAL("04", "CRITICAL", "Danger to life, road impassable"),
    ;

    val displayLabel: String get() = "${code} ${title}"

    /**
     * Plain-language urgency for trip voice / alerts / detail.
     * Filters and stats keep [title] codes (MINOR…CRITICAL).
     */
    val driverPhrase: String
        get() = when (this) {
            MINOR -> "small dip — caution"
            MODERATE -> "noticeable hole — avoid if you can"
            SEVERE -> "deep hole — slow down"
            CRITICAL -> "dangerous crater — stop or divert"
        }

    /** Compact capsule label (still human-readable). */
    val driverShort: String
        get() = when (this) {
            MINOR -> "Caution"
            MODERATE -> "Avoid"
            SEVERE -> "Slow down"
            CRITICAL -> "Stop / divert"
        }

    companion object {
        fun fromStored(value: String?): PotholeSeverity {
            if (value.isNullOrBlank()) return MODERATE
            entries.find { it.name == value }?.let { return it }
            entries.find { it.code == value }?.let { return it }
            return MODERATE
        }
    }
}
