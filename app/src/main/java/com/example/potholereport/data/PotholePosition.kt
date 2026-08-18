package com.example.potholereport.data

/**
 * Where the user says the pothole sits across the framed road (wide-shot L/M/R span).
 *
 * Contiguous spans only: L, M, R, LM, MR, LMR ([FULL_WIDTH]). Legacy code `F` still maps to [FULL_WIDTH].
 * The risk analyzer constrains its dark-blob search to [laneRange].
 */
enum class PotholePosition(val displayLabel: String, val code: String) {
    LEFT("Left", "L"),
    MIDDLE("Middle", "M"),
    RIGHT("Right", "R"),
    /** Spans left + middle of the framed road. */
    LEFT_MIDDLE("Left+Middle", "LM"),
    /** Spans middle + right of the framed road. */
    MIDDLE_RIGHT("Middle+Right", "MR"),
    /** LMR — full / near-full road width (replaces separate “Full road” control). */
    FULL_WIDTH("Full road", "LMR"),
    ;

    companion object {
        fun fromCode(code: String?): PotholePosition {
            val raw = code?.trim().orEmpty()
            if (raw.isEmpty()) return MIDDLE
            entries.find { it.code.equals(raw, ignoreCase = true) }?.let { return it }
            entries.find { it.name.equals(raw, ignoreCase = true) }?.let { return it }
            entries.find { it.displayLabel.equals(raw, ignoreCase = true) }?.let { return it }
            return when (raw.lowercase()) {
                "f", "across", "across road", "whole", "whole road", "full", "w",
                "full width", "fullwidth", "full road", "full road pothole", "crater",
                -> FULL_WIDTH
                "left+middle", "left-middle", "l+m", "ml" -> LEFT_MIDDLE
                "middle+right", "middle-right", "m+r", "rm" -> MIDDLE_RIGHT
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
            LEFT_MIDDLE -> 0f..0.70f
            MIDDLE_RIGHT -> 0.30f..1f
            FULL_WIDTH -> 0f..1f
        }

    /** Mirror L↔R for against-traffic / opposite-driver cases. */
    fun mirrored(): PotholePosition = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
        MIDDLE -> MIDDLE
        LEFT_MIDDLE -> MIDDLE_RIGHT
        MIDDLE_RIGHT -> LEFT_MIDDLE
        FULL_WIDTH -> FULL_WIDTH
    }
}
