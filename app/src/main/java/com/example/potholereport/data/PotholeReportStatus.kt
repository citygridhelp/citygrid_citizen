package com.example.potholereport.data

import java.util.Locale

enum class PotholeReportStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    ;

    /** Sentence case for notifications and prose. */
    val displayLabel: String
        get() = when (this) {
            OPEN -> "Open"
            IN_PROGRESS -> "In progress"
            COMPLETED -> "Completed"
        }

    /** ALL CAPS for screen chrome (stat chips, accountability cards, detail). */
    val displayLabelCaps: String
        get() = displayLabel.uppercase(Locale.US)

    companion object {
        fun fromStored(raw: String?): PotholeReportStatus {
            return when (raw?.uppercase()) {
                "COMPLETED" -> COMPLETED
                "IN_PROGRESS", "IN PROGRESS", "INPROGRESS" -> IN_PROGRESS
                else -> OPEN
            }
        }
    }
}
