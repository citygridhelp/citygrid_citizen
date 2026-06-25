package com.example.potholereport.data

/**
 * Where the user says the pothole sits in the close-up frame.
 *
 * The risk analyzer constrains its dark-blob search to the corresponding
 * vertical third (with a small overlap) so it picks up the actual pothole
 * the user is reporting and not a wall shadow / drain edge that happens to
 * be in another lane.
 */
enum class PotholePosition(val displayLabel: String, val code: String) {
    LEFT("Left", "L"),
    MIDDLE("Middle", "M"),
    RIGHT("Right", "R"),
    ;

    /** Vertical band [start, end] (normalised to image width 0..1). */
    val laneRange: ClosedFloatingPointRange<Float>
        get() = when (this) {
            LEFT -> 0f..0.42f
            MIDDLE -> 0.30f..0.70f
            RIGHT -> 0.58f..1f
        }
}
