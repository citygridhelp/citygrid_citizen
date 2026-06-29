package com.example.potholereport.data

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.auth.auth
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.BoundingBox
import java.io.File

data class PersistedPotholeReport(
    val id: Long,
    val cityKey: String,
    val createdAtMs: Long,
    /** Close-up pothole photo on disk. */
    val photoPath: String,
    /** [Double.NaN] if missing in persisted JSON (legacy). */
    val latitude: Double,
    val longitude: Double,
    /** Neighborhood / street label from GPS at submit (may be empty for legacy rows). */
    val areaLabel: String = "",
    /** Wide context photo; empty for legacy reports. */
    val widePhotoPath: String = "",
    val severity: PotholeSeverity = PotholeSeverity.MODERATE,
    val note: String = "",
    /** True when the user was signed in at submit time (shown under MY REPORTS). */
    val submittedSignedIn: Boolean = false,
    val status: PotholeReportStatus = PotholeReportStatus.OPEN,
    /** Municipal accountability routing (nearest zone officer). */
    val assigneeKey: String = "",
    val assigneeCorporation: String = "",
    val assigneeZone: String = "",
    val assigneeName: String = "",
    val assigneePosition: String = "",
    val assigneeOfficeAddress: String = "",
    /** Stable reporter id ([UserProfile.anonymousUserId] or [DeviceReporterId]). */
    val reporterUserId: String = "",
) {
    fun hasCoordinates(): Boolean = !latitude.isNaN() && !longitude.isNaN()

    fun hasAssignee(): Boolean = assigneeName.isNotBlank()

    fun withAssignee(assignee: MunicipalAssignee): PersistedPotholeReport {
        val fields = assignee.toPersistedFields()
        return copy(
            assigneeKey = fields.assigneeKey,
            assigneeCorporation = fields.assigneeCorporation,
            assigneeZone = fields.assigneeZone,
            assigneeName = fields.assigneeName,
            assigneePosition = fields.assigneePosition,
            assigneeOfficeAddress = fields.assigneeOfficeAddress,
        )
    }
}

/**
 * Local store for submitted reports (photo + city + coordinates + time).
 * Map clusters and "recent" strip filter by metro [BoundingBox] when coordinates exist.
 */
sealed class AddReportResult {
    data class Success(val report: PersistedPotholeReport) : AddReportResult()
    data object Failed : AddReportResult()
    data class DuplicateActive(val existing: PersistedPotholeReport) : AddReportResult()
}

object RecentReportsRepository {

    private const val PREFS_NAME = "pothole_recent_reports"
    private const val KEY_JSON = "reports_json"
    private const val MAX_STORED = 80

    private val lock = Any()
    private var appContext: Context? = null
    private var cache: MutableList<PersistedPotholeReport> = mutableListOf()

    fun init(context: Context) {
        synchronized(lock) {
            if (appContext != null) return
            appContext = context.applicationContext
            cache = loadFromDisk().toMutableList()
            ensureAccountabilityAssignmentsLocked()
        }
    }

    /** All reports with municipal assignee, newest first (for Accountability tab). */
    fun reportsForAccountability(): List<PersistedPotholeReport> {
        synchronized(lock) {
            return cache
                .filter { it.hasAssignee() }
                .sortedByDescending { it.createdAtMs }
        }
    }

    /**
     * Reports that count toward the map for [cityKey]: same city tag and coordinates inside [bbox].
     */
    fun reportsForMapInMetro(cityKey: String, bbox: BoundingBox): List<PersistedPotholeReport> {
        synchronized(lock) {
            return cache.filter { r ->
                r.cityKey == cityKey &&
                    r.hasCoordinates() &&
                    bbox.contains(r.latitude, r.longitude)
            }
        }
    }

    /**
     * Recent thumbnails: [cityKey] matches and either legacy (no coords) or coords inside [bbox].
     */
    fun recentForCityInMetro(cityKey: String, bbox: BoundingBox?, limit: Int = 5): List<PersistedPotholeReport> {
        synchronized(lock) {
            return cache
                .asSequence()
                .filter { r ->
                    if (r.cityKey != cityKey) return@filter false
                    if (bbox == null) return@filter true
                    if (!r.hasCoordinates()) return@filter true
                    bbox.contains(r.latitude, r.longitude)
                }
                .sortedByDescending { it.createdAtMs }
                .take(limit)
                .toList()
        }
    }

    /**
     * Returns an existing OPEN / IN_PROGRESS report from this reporter within
     * [PotholeDuplicateGuard.SAME_POTHOLE_RADIUS_METERS] of the coordinates.
     */
    fun findActiveDuplicateForReporter(
        reporterUserId: String,
        cityKey: String,
        latitude: Double,
        longitude: Double,
    ): PersistedPotholeReport? {
        synchronized(lock) {
            return PotholeDuplicateGuard.findActiveDuplicate(
                reports = cache,
                reporterUserId = reporterUserId,
                cityKey = cityKey,
                latitude = latitude,
                longitude = longitude,
            )
        }
    }

    /** Reports submitted while the user was signed in (MY REPORTS tab), newest first. */
    fun signedInReportsOrdered(limit: Int = 80): List<PersistedPotholeReport> {
        synchronized(lock) {
            return cache
                .asSequence()
                .filter { it.submittedSignedIn }
                .sortedByDescending { it.createdAtMs }
                .take(limit)
                .toList()
        }
    }

    /**
     * Copies report photos into app storage and prepends to the list.
     * Call from a background dispatcher; does not touch Compose state.
     */
    fun addReport(
        cityKey: String,
        closeUpUri: Uri,
        wideUri: Uri,
        latitude: Double,
        longitude: Double,
        severity: PotholeSeverity,
        note: String,
        reporterUserId: String,
        submittedSignedIn: Boolean = false,
    ): AddReportResult {
        val ctx = appContext ?: return AddReportResult.Failed
        return synchronized(lock) {
            PotholeDuplicateGuard.findActiveDuplicate(
                reports = cache,
                reporterUserId = reporterUserId,
                cityKey = cityKey,
                latitude = latitude,
                longitude = longitude,
            )?.let { return@synchronized AddReportResult.DuplicateActive(it) }

            val dir = File(ctx.filesDir, "report_photos").apply { mkdirs() }
            val id = System.currentTimeMillis()
            val closeDest = File(dir, "${id}_close.jpg")
            val wideDest = File(dir, "${id}_wide.jpg")
            if (!copyUriToFile(ctx, closeUpUri, closeDest)) return@synchronized AddReportResult.Failed
            if (!copyUriToFile(ctx, wideUri, wideDest)) {
                closeDest.delete()
                return@synchronized AddReportResult.Failed
            }
            val areaLabel = resolveAreaLabel(ctx, latitude, longitude).takeIf { isMeaningfulAreaName(it) }.orEmpty()
            val assignee = MunicipalAssignmentResolver.resolve(cityKey, latitude, longitude)
            val entry = PersistedPotholeReport(
                id = id,
                cityKey = cityKey,
                createdAtMs = id,
                photoPath = closeDest.absolutePath,
                latitude = latitude,
                longitude = longitude,
                areaLabel = areaLabel,
                widePhotoPath = wideDest.absolutePath,
                severity = severity,
                note = note.trim(),
                submittedSignedIn = submittedSignedIn,
                status = PotholeReportStatus.OPEN,
                reporterUserId = reporterUserId,
            ).withAssignee(assignee)
            cache.add(0, entry)
            while (cache.size > MAX_STORED) {
                val removed = cache.removeAt(cache.lastIndex)
                deleteReportFiles(removed)
            }
            saveToDisk(ctx)
            AddReportResult.Success(entry)
        }
    }

    /** Pulls signed-in reports from Supabase and merges status locally. */
    suspend fun syncSignedInReportsFromSupabase(): Boolean {
        val ctx = appContext ?: return false
        if (!com.example.potholereport.data.remote.SupabaseClientProvider.isConfigured) return false
        val client = com.example.potholereport.data.remote.SupabaseClientProvider.client ?: return false
        runCatching { client.auth.awaitInitialization() }
        if (client.auth.currentUserOrNull()?.id == null) return false
        val rows = com.example.potholereport.data.remote.ReportSyncRepository.fetchMyReports()
        if (rows.isEmpty()) return false
        return synchronized(lock) {
            var changed = false
            for (row in rows) {
                val visibleStatus = PotholeReportStatus.fromStored(row.citizenVisibleStatus)
                val idx = cache.indexOfFirst { it.id == row.id && it.submittedSignedIn }
                if (idx < 0) continue
                val existing = cache[idx]
                val merged = existing.copy(
                    status = visibleStatus,
                    assigneeKey = row.assigneeKey.ifBlank { existing.assigneeKey },
                    assigneeCorporation = row.assigneeCorp.ifBlank { existing.assigneeCorporation },
                    assigneeZone = row.assigneeZone.ifBlank { existing.assigneeZone },
                    assigneeName = row.assigneeName.ifBlank { existing.assigneeName },
                    assigneePosition = row.assigneeRole.ifBlank { existing.assigneePosition },
                    assigneeOfficeAddress = row.assigneeAddr.ifBlank { existing.assigneeOfficeAddress },
                )
                if (merged != existing) {
                    cache[idx] = merged
                    changed = true
                }
            }
            if (changed) saveToDisk(ctx)
            changed
        }
    }

    /** Pulls recent city reports from Supabase for guests and fresh installs. */
    suspend fun syncPublicCityReportsFromSupabase(cityKey: String): Boolean {
        if (cityKey.isBlank()) return false
        val ctx = appContext ?: return false
        if (!com.example.potholereport.data.remote.SupabaseClientProvider.isConfigured) return false
        val canonicalCity = CityMetroKeys.canonical(cityKey)
        val rows = com.example.potholereport.data.remote.ReportSyncRepository
            .fetchRecentCityReports(canonicalCity, MAX_STORED)
        if (rows.isEmpty()) return false
        return synchronized(lock) {
            var changed = false
            for (row in rows) {
                if (cache.any { it.id == row.id }) continue
                val lat = row.latitude ?: Double.NaN
                val lon = row.longitude ?: Double.NaN
                val assignee = if (row.assigneeName.isNotBlank()) {
                    null
                } else {
                    MunicipalAssignmentResolver.resolve(row.cityKey, lat, lon)
                }
                var entry = PersistedPotholeReport(
                    id = row.id,
                    cityKey = row.cityKey,
                    createdAtMs = row.createdAtMs,
                    photoPath = "",
                    latitude = lat,
                    longitude = lon,
                    areaLabel = row.areaLabel,
                    severity = PotholeSeverity.fromStored(row.severity),
                    note = row.citizenNote,
                    submittedSignedIn = false,
                    status = PotholeReportStatus.fromStored(row.citizenVisibleStatus),
                    reporterUserId = row.reporterUserId,
                    assigneeKey = row.assigneeKey,
                    assigneeCorporation = row.assigneeCorp,
                    assigneeZone = row.assigneeZone,
                    assigneeName = row.assigneeName,
                    assigneePosition = row.assigneeRole,
                    assigneeOfficeAddress = row.assigneeAddr,
                )
                if (assignee != null && row.assigneeName.isBlank()) {
                    entry = entry.withAssignee(assignee)
                }
                cache.add(entry)
                changed = true
            }
            if (changed) {
                cache.sortByDescending { it.createdAtMs }
                while (cache.size > MAX_STORED) {
                    val removed = cache.removeAt(cache.lastIndex)
                    deleteReportFiles(removed)
                }
                saveToDisk(ctx)
            }
            changed
        }
    }

    /**
     * Reserved for the government response application to sync status updates.
     * Not exposed in the citizen app UI.
     */
    fun updateSignedInReportStatus(reportId: Long, status: PotholeReportStatus): Boolean {
        val ctx = appContext ?: return false
        return synchronized(lock) {
            val idx = cache.indexOfFirst { it.id == reportId && it.submittedSignedIn }
            if (idx < 0) return@synchronized false
            cache[idx] = cache[idx].copy(status = status)
            saveToDisk(ctx)
            true
        }
    }

    fun deleteSignedInReport(reportId: Long): Boolean {
        val ctx = appContext ?: return false
        return synchronized(lock) {
            val idx = cache.indexOfFirst { it.id == reportId && it.submittedSignedIn }
            if (idx < 0) return@synchronized false
            val removed = cache.removeAt(idx)
            deleteReportFiles(removed)
            saveToDisk(ctx)
            true
        }
    }

    private fun loadFromDisk(): List<PersistedPotholeReport> {
        val ctx = appContext ?: return emptyList()
        val raw = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val lat = if (o.has("lat")) o.getDouble("lat") else Double.NaN
                    val lon = if (o.has("lon")) o.getDouble("lon") else Double.NaN
                    add(
                        PersistedPotholeReport(
                            id = o.getLong("id"),
                            cityKey = o.getString("city"),
                            createdAtMs = o.getLong("time"),
                            photoPath = o.getString("path"),
                            latitude = lat,
                            longitude = lon,
                            areaLabel = o.optString("area", ""),
                            widePhotoPath = o.optString("widePath", ""),
                            severity = PotholeSeverity.fromStored(o.optString("severity", "")),
                            note = o.optString("note", ""),
                            submittedSignedIn = o.optBoolean("signedIn", false),
                            status = PotholeReportStatus.fromStored(o.optString("status", "")),
                            assigneeKey = o.optString("assigneeKey", ""),
                            assigneeCorporation = o.optString("assigneeCorp", ""),
                            assigneeZone = o.optString("assigneeZone", ""),
                            assigneeName = o.optString("assigneeName", ""),
                            assigneePosition = o.optString("assigneeRole", ""),
                            assigneeOfficeAddress = o.optString("assigneeAddr", ""),
                            reporterUserId = o.optString("reporterId", ""),
                        )
                    )
                }
            }.filter { File(it.photoPath).exists() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToDisk(ctx: Context) {
        val arr = JSONArray()
        for (r in cache) {
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("city", r.cityKey)
                    put("time", r.createdAtMs)
                    put("path", r.photoPath)
                    if (r.hasCoordinates()) {
                        put("lat", r.latitude)
                        put("lon", r.longitude)
                    }
                    if (r.areaLabel.isNotBlank()) {
                        put("area", r.areaLabel)
                    }
                    if (r.widePhotoPath.isNotBlank()) {
                        put("widePath", r.widePhotoPath)
                    }
                    put("severity", r.severity.name)
                    if (r.note.isNotBlank()) {
                        put("note", r.note)
                    }
                    if (r.submittedSignedIn) {
                        put("signedIn", true)
                    }
                    put("status", r.status.name)
                    if (r.assigneeKey.isNotBlank()) {
                        put("assigneeKey", r.assigneeKey)
                        put("assigneeCorp", r.assigneeCorporation)
                        put("assigneeZone", r.assigneeZone)
                        put("assigneeName", r.assigneeName)
                        put("assigneeRole", r.assigneePosition)
                        put("assigneeAddr", r.assigneeOfficeAddress)
                    }
                    if (r.reporterUserId.isNotBlank()) {
                        put("reporterId", r.reporterUserId)
                    }
                }
            )
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, arr.toString())
            .apply()
    }

    private fun copyUriToFile(ctx: Context, uri: Uri, dest: File): Boolean {
        return try {
            val stream = ctx.contentResolver.openInputStream(uri) ?: return false
            stream.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } catch (_: Exception) {
            if (dest.exists()) dest.delete()
            false
        }
    }

    private fun ensureAccountabilityAssignmentsLocked() {
        var changed = false
        for (i in cache.indices) {
            if (cache[i].hasAssignee()) continue
            cache[i] = cache[i].withAssignee(MunicipalAssignmentResolver.resolveForReport(cache[i]))
            changed = true
        }
        if (changed) {
            appContext?.let { saveToDisk(it) }
        }
    }

    private fun deleteReportFiles(report: PersistedPotholeReport) {
        runCatching { File(report.photoPath).delete() }
        if (report.widePhotoPath.isNotBlank()) {
            runCatching { File(report.widePhotoPath).delete() }
        }
    }
}
