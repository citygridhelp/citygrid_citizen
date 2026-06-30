package com.example.potholereport.data.remote

import android.util.Log
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.RecentReportsRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Pushes citizen reports (photos + row) to Supabase and reads them back so
 * government status/proof updates surface in "My Reports".
 *
 * All methods are suspend and no-op safely when Supabase is not configured or
 * the user is not signed in, so the app degrades to local-only behavior.
 */
object ReportSyncRepository {

    private const val TAG = "ReportSync"
    private const val EVIDENCE_BUCKET = "evidence"
    private const val REPORTS_TABLE = "reports"

    // App-level scope so an in-flight push is NOT cancelled when the report
    // screen closes (navigation) right after enqueuing the upload.
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fire-and-forget push that survives screen navigation. Safe to call from the UI;
     * the work runs on an app-level scope, not the caller's composition scope.
     */
    fun enqueuePush(report: PersistedPotholeReport) {
        syncScope.launch {
            if (pushReport(report)) return@launch
            kotlinx.coroutines.delay(1_500)
            if (pushReport(report)) return@launch
            kotlinx.coroutines.delay(3_000)
            pushReport(report)
        }
    }

    /**
     * Uploads the report's two photos to Storage and inserts the report row.
     * Requires a live Supabase session (signed-in citizen). Returns true on success.
     */
    suspend fun pushReport(report: PersistedPotholeReport): Boolean {
        if (!SupabaseClientProvider.isConfigured) {
            Log.w(TAG, "Skip push: Supabase not configured in this build")
            return false
        }
        val client = SupabaseClientProvider.client ?: return false
        runCatching { client.auth.awaitInitialization() }
        val authId = client.auth.currentUserOrNull()?.id ?: run {
            Log.w(TAG, "Skip push: no Supabase session for report ${report.id}")
            return false
        }

        return try {
            val closeFile = File(report.photoPath)
            if (!closeFile.exists()) {
                Log.w(TAG, "Skip push: close photo missing for report ${report.id}")
                return false
            }
            val closeObjectPath = "${report.id}/close.jpg"
            client.storage.from(EVIDENCE_BUCKET)
                .upload(closeObjectPath, closeFile.readBytes()) { upsert = true }
            Log.i(TAG, "Uploaded close photo for report ${report.id}")

            // A wide-photo failure must not block the report row insert.
            var wideObjectPath = ""
            val wideFile = File(report.widePhotoPath)
            if (report.widePhotoPath.isNotBlank() && wideFile.exists()) {
                try {
                    val path = "${report.id}/wide.jpg"
                    client.storage.from(EVIDENCE_BUCKET)
                        .upload(path, wideFile.readBytes()) { upsert = true }
                    wideObjectPath = path
                    Log.i(TAG, "Uploaded wide photo for report ${report.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Wide photo upload failed for report ${report.id}: ${e.message}", e)
                }
            }

            val row = RemoteReportRow(
                id = report.id,
                cityKey = report.cityKey,
                createdAtMs = report.createdAtMs,
                reporterUserId = report.reporterUserId,
                reporterAuthId = authId,
                photoPath = closeObjectPath,
                widePhotoPath = wideObjectPath,
                latitude = report.latitude.takeIf { !it.isNaN() },
                longitude = report.longitude.takeIf { !it.isNaN() },
                areaLabel = report.areaLabel,
                severity = report.severity.name,
                citizenNote = report.note,
                status = report.status.name,
                citizenVisibleStatus = report.status.name,
                assigneeKey = report.assigneeKey,
                assigneeCorp = report.assigneeCorporation,
                assigneeZone = report.assigneeZone,
                assigneeName = report.assigneeName,
                assigneeRole = report.assigneePosition,
                assigneeAddr = report.assigneeOfficeAddress,
            )
            try {
                client.from(REPORTS_TABLE).insert(row)
            } catch (insertError: Exception) {
                val msg = (insertError.message ?: "").lowercase()
                if (msg.contains("duplicate") || msg.contains("unique") || msg.contains("23505")) {
                    Log.i(TAG, "Report ${report.id} already exists in Supabase")
                    RecentReportsRepository.markCloudSynced(
                        report.id,
                        report.reporterUserId,
                        closeObjectPath,
                        wideObjectPath,
                    )
                } else {
                    throw insertError
                }
            }
            Log.i(TAG, "Pushed report ${report.id} to Supabase")
            RecentReportsRepository.markCloudSynced(
                report.id,
                report.reporterUserId,
                closeObjectPath,
                wideObjectPath,
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push report ${report.id}: ${e.message}", e)
            false
        }
    }

    /**
     * Recent reports for a city — available to guests (anon) and signed-in users.
     * Powers the home "Recent reports" strip, map clusters, and accountability tab.
     */
    suspend fun fetchRecentCityReports(cityKey: String, limit: Int = 80): List<RemoteReportRow> {
        return fetchRecentCityReportsOrNull(cityKey, limit) ?: emptyList()
    }

    /** Null when Supabase is unavailable or the fetch failed (do not prune local cache). */
    suspend fun fetchRecentCityReportsOrNull(cityKey: String, limit: Int = 80): List<RemoteReportRow>? {
        if (!SupabaseClientProvider.isConfigured) return null
        val client = SupabaseClientProvider.client ?: return null
        val canonicalCity = com.example.potholereport.data.CityMetroKeys.canonical(cityKey)
        return try {
            client.from(REPORTS_TABLE)
                .select(Columns.raw(RemoteReportRow.SELECTED_COLUMNS)) {
                    filter { eq("city_key", canonicalCity) }
                    order(column = "created_at_ms", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<RemoteReportRow>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch city reports for $canonicalCity: ${e.message}", e)
            null
        }
    }

    /** Reads the signed-in citizen's reports back from Supabase (status round-trip). */
    suspend fun fetchMyReports(): List<RemoteReportRow> {
        return fetchMyReportsOrNull() ?: emptyList()
    }

    /** Null when Supabase is unavailable or the fetch failed (do not prune local cache). */
    suspend fun fetchMyReportsOrNull(): List<RemoteReportRow>? {
        val client = SupabaseClientProvider.client ?: return null
        runCatching { client.auth.awaitInitialization() }
        val authId = client.auth.currentUserOrNull()?.id ?: return null
        return try {
            client.from(REPORTS_TABLE)
                .select(Columns.raw(RemoteReportRow.SELECTED_COLUMNS)) {
                    filter { eq("reporter_auth_id", authId) }
                }
                .decodeList<RemoteReportRow>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch reports: ${e.message}", e)
            null
        }
    }
}
