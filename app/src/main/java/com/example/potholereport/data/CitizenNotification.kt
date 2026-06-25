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
    }
}
