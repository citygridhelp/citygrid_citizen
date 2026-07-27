package com.example.potholereport.data

/**
 * Where the user says the pothole sits in the close-up frame.
 *
 * The risk analyzer constrains its dark-blob search to the corresponding
 * vertical band (with a small overlap) so it picks up the actual pothole
 * the user is reporting and not a wall shadow / drain edge that happens to
 * be in another lane. [FULL_WIDTH] searches the whole frame for damage that
 * spans most or all of the carriageway.
 */
enum class PotholePosition(val displayLabel: String, val code: String) {
    LEFT("Left", "L"),
    MIDDLE("Middle", "M"),
    RIGHT("Right", "R"),
    /** Damage covering the full road width (large crater or many potholes across). */
    FULL_WIDTH("Full road pothole", "F"),
    ;

    companion object {
        fun fromCode(code: String?): PotholePosition {
            val raw = code?.trim().orEmpty()
            if (raw.isEmpty()) return MIDDLE
            entries.find { it.code.equals(raw, ignoreCase = true) }?.let { return it }
            entries.find { it.name.equals(raw, ignoreCase = true) }?.let { return it }
            entries.find { it.displayLabel.equals(raw, ignoreCase = true) }?.let { return it }
            return when (raw.lowercase()) {
                "across", "across road", "whole", "whole road", "full", "w",
                "full width", "fullwidth", "full road", "full road pothole", "crater",
                -> FULL_WIDTH
                else -> MIDDLE
            }
        }
    }

    /** Vertical band [start, end] (normalised to image width 0..1). */
    val laneRange: ClosedFloatingPointRange<Float>
        get() = when (this) {
            LEFT -> 0f..0.42f
            MIDDLE -> 0.30f..0.70f
            RIGHT -> 0.58f..1f
            FULL_WIDTH -> 0f..1f
        }
}
