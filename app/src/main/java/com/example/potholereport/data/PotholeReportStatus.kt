package com.example.potholereport.data

enum class PotholeReportStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    ;

    val displayLabel: String
        get() = when (this) {
            OPEN -> "Open"
            IN_PROGRESS -> "In progress"
            COMPLETED -> "Completed"
        }

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
