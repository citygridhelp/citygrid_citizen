package com.example.potholereport.data

enum class PotholeSeverity(val code: String, val title: String, val blurb: String) {
    MINOR("01", "MINOR", "Small crack or shallow dip"),
    MODERATE("02", "MODERATE", "Noticeable, avoidable"),
    SEVERE("03", "SEVERE", "Damages vehicles, hard to avoid"),
    CRITICAL("04", "CRITICAL", "Danger to life, road impassable"),
    ;

    val displayLabel: String get() = "${code} ${title}"

    companion object {
        fun fromStored(value: String?): PotholeSeverity {
            if (value.isNullOrBlank()) return MODERATE
            entries.find { it.name == value }?.let { return it }
            entries.find { it.code == value }?.let { return it }
            return MODERATE
        }
    }
}
