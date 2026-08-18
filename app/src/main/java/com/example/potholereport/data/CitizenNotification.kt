package com.example.potholereport.data

data class CitizenNotification(
    val id: String,
    val type: Type,
    val title: String,
    val body: String,
    val createdAtMs: Long,
    val read: Boolean = false,
    val reportId: Long? = null,
) {
    enum class Type {
        APP_UPDATE,
        STATUS_CHANGE,
        GENERAL,
        WEATHER,
        UNCONFIRMED,
    }

    /** Alert should open My reports → Unconfirmed review. */
    fun opensUnconfirmedReview(): Boolean {
        if (type == Type.UNCONFIRMED) return true
        val hay = "$title $body".lowercase()
        return hay.contains("unconfirmed") && !hay.contains("filed from")
    }
}
