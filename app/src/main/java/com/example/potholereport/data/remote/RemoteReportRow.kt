package com.example.potholereport.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row shape for the Supabase `public.reports` table. Column names match
 * supabase/migrations/0001_init.sql exactly. Used for both insert (citizen
 * upload) and decode (status/proof round-trip from the government app).
 */
@Serializable
data class RemoteReportRow(
    val id: Long,
    @SerialName("city_key") val cityKey: String,
    @SerialName("created_at_ms") val createdAtMs: Long,
    @SerialName("reporter_user_id") val reporterUserId: String = "",
    @SerialName("reporter_auth_id") val reporterAuthId: String? = null,

    @SerialName("photo_path") val photoPath: String = "",
    @SerialName("wide_photo_path") val widePhotoPath: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("area_label") val areaLabel: String = "",
    val severity: String = "MODERATE",
    @SerialName("citizen_note") val citizenNote: String = "",

    val status: String = "OPEN",
    @SerialName("citizen_visible_status") val citizenVisibleStatus: String = "OPEN",

    @SerialName("assignee_key") val assigneeKey: String = "",
    @SerialName("assignee_corp") val assigneeCorp: String = "",
    @SerialName("assignee_zone") val assigneeZone: String = "",
    @SerialName("assignee_name") val assigneeName: String = "",
    @SerialName("assignee_role") val assigneeRole: String = "",
    @SerialName("assignee_addr") val assigneeAddr: String = "",

    @SerialName("ward_key") val wardKey: String = "",
    @SerialName("ward_number") val wardNumber: Int = 0,
    @SerialName("ward_name") val wardName: String = "",

    @SerialName("pothole_position") val potholePosition: String = "",
    @SerialName("report_bearing_deg") val reportBearingDeg: Float? = null,
    @SerialName("traffic_bearing_deg") val trafficBearingDeg: Float? = null,
    @SerialName("traffic_faces_camera") val trafficFacesCamera: Boolean? = null,

    @SerialName("completion_top_path") val completionTopPath: String = "",
    @SerialName("completion_wide_path") val completionWidePath: String = "",
    @SerialName("completed_at_ms") val completedAtMs: Long? = null,
    @SerialName("commissioner_completion_note") val commissionerCompletionNote: String = "",
) {
    companion object {
        /** Columns fetched on read; keep in sync with the fields above. */
        val SELECTED_COLUMNS: String = listOf(
            "id", "city_key", "created_at_ms", "reporter_user_id", "reporter_auth_id",
            "photo_path", "wide_photo_path", "latitude", "longitude", "area_label",
            "severity", "citizen_note", "status", "citizen_visible_status",
            "assignee_key", "assignee_corp", "assignee_zone", "assignee_name",
            "assignee_role", "assignee_addr",
            "ward_key", "ward_number", "ward_name",
            "pothole_position", "report_bearing_deg", "traffic_bearing_deg", "traffic_faces_camera",
            "completion_top_path", "completion_wide_path", "completed_at_ms",
            "commissioner_completion_note",
        ).joinToString(",")
    }
}
