package com.example.potholereport.data

import android.content.Context
import android.net.Uri
import com.example.potholereport.data.remote.RemoteReportRow
import com.example.potholereport.data.remote.trafficFacingStorageKey
import com.example.potholereport.data.remote.withRemoteDirection
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
    /** Civic category; legacy rows without this field are treated as potholes. */
    val issueType: CivicIssueType = CivicIssueType.POTHOLE,
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
    /** GBA ward routing snapshot (Bengaluru only; from official GIS at submit). */
    val wardKey: String = "",
    val wardNumber: Int = 0,
    val wardName: String = "",
    /** Lane span: L / M / R / LM / MR / LMR ([PotholePosition.code]; legacy F → LMR). */
    val potholePosition: String = PotholePosition.MIDDLE.code,
    /** Open / closed / broken / overflowing — manhole reports only. */
    val manholeCondition: String = "",
    /** Morning / afternoon / evening / night — stray-dog (incident) reports. */
    val incidentTimeOfDay: String = "",
    /** GPS / device heading at submit (degrees). NaN if unknown. */
    val reportBearingDeg: Float = Float.NaN,
    /** Traffic flow toward (degrees). NaN if unknown. */
    val trafficBearingDeg: Float = Float.NaN,
    /** Wide-shot traffic confirmation; empty = unknown. */
    val trafficFacing: String = TrafficFacingMode.UNKNOWN.storageKey,
    /** Stable reporter id ([UserProfile.anonymousUserId] or [DeviceReporterId]). */
    val reporterUserId: String = "",
    /** True once this row exists in Supabase (successful push or server restore). */
    val cloudSynced: Boolean = false,
    /** Supabase Storage object path for the close-up photo (e.g. `{id}/close.jpg`). */
    val storageClosePath: String = "",
    /** Supabase Storage object path for the wide context photo. */
    val storageWidePath: String = "",
) {
    fun hasCoordinates(): Boolean = !latitude.isNaN() && !longitude.isNaN()

    fun hasAssignee(): Boolean = assigneeName.isNotBlank() || assigneeZone.isNotBlank()

    fun hasWard(): Boolean = wardNumber > 0 && wardName.isNotBlank()

    fun potholePositionEnum(): PotholePosition = PotholePosition.fromCode(potholePosition)

    fun manholeConditionEnum(): ManholeCondition? = ManholeCondition.fromStored(manholeCondition)

    fun incidentTimeOfDayEnum(): IncidentTimeOfDay? = IncidentTimeOfDay.fromStored(incidentTimeOfDay)

    fun cloudCitizenNote(): String {
        val withManhole = ManholeCondition.mergeIntoNote(manholeConditionEnum(), note)
        return IncidentTimeOfDay.mergeIntoNote(incidentTimeOfDayEnum(), withManhole)
    }

    fun withManholeFromNote(): PersistedPotholeReport {
        val stored = manholeConditionEnum()
        if (stored != null) {
            return copy(note = ManholeCondition.stripFromNote(note))
        }
        val (parsed, rest) = ManholeCondition.parseFromNote(note)
        if (parsed == null) return this
        return copy(
            issueType = CivicIssueType.MANHOLE,
            manholeCondition = parsed.name,
            note = rest,
        )
    }

    fun withIncidentTimeFromNote(): PersistedPotholeReport {
        val stored = incidentTimeOfDayEnum()
        if (stored != null) {
            return copy(note = IncidentTimeOfDay.stripFromNote(note))
        }
        val (parsed, rest) = IncidentTimeOfDay.parseFromNote(note)
        if (parsed == null) return this
        return copy(
            issueType = CivicIssueType.STRAY_DOG,
            incidentTimeOfDay = parsed.name,
            note = rest,
        )
    }

    fun trafficFacingMode(): TrafficFacingMode = TrafficFacingMode.fromStorage(trafficFacing)

    fun hasTrafficBearing(): Boolean = !trafficBearingDeg.isNaN()

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

    fun withWard(ward: GbaWardMatch): PersistedPotholeReport = copy(
        wardKey = ward.wardKey,
        wardNumber = ward.wardNumber,
        wardName = ward.wardName,
    )
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

    /** Application context after [init], else null. */
    fun appContextOrNull(): Context? = synchronized(lock) { appContext }

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
     * City reports eligible for the home Reports strip (metro bbox when known).
     * Sorting / limit applied by [sortReportsForStrip].
     */
    fun reportsForCityInMetro(cityKey: String, bbox: BoundingBox?): List<PersistedPotholeReport> {
        synchronized(lock) {
            return cache.filter { r ->
                if (r.cityKey != cityKey) return@filter false
                if (bbox == null) return@filter true
                if (!r.hasCoordinates()) return@filter true
                bbox.contains(r.latitude, r.longitude)
            }
        }
    }

    fun reportById(id: Long): PersistedPotholeReport? {
        synchronized(lock) {
            return cache.find { it.id == id }
        }
    }

    fun allNewestFirst(): List<PersistedPotholeReport> {
        synchronized(lock) {
            return cache.sortedByDescending { it.createdAtMs }
        }
    }

    /**
     * Reports strip: [cityKey] matches and either legacy (no coords) or coords inside [bbox].
     */
    fun recentForCityInMetro(
        cityKey: String,
        bbox: BoundingBox?,
        limit: Int = 5,
        sort: ReportsStripSort = ReportsStripSort.RECENT,
    ): List<PersistedPotholeReport> =
        sortReportsForStrip(reportsForCityInMetro(cityKey, bbox), sort, limit)

    /**
     * Returns an existing OPEN / IN_PROGRESS report from this reporter within the
     * effective same-pothole radius of the coordinates (see [PotholeDuplicateGuard]).
     */
    fun findActiveDuplicateForReporter(
        reporterUserId: String,
        cityKey: String,
        latitude: Double,
        longitude: Double,
        gpsAccuracyM: Float? = null,
        issueType: CivicIssueType = CivicIssueType.POTHOLE,
    ): PersistedPotholeReport? {
        synchronized(lock) {
            return PotholeDuplicateGuard.findActiveDuplicate(
                reports = cache,
                reporterUserId = reporterUserId,
                cityKey = cityKey,
                latitude = latitude,
                longitude = longitude,
                gpsAccuracyM = gpsAccuracyM,
                issueType = issueType,
            )
        }
    }

    /** Reports for the signed-in citizen ([reporterUserId] = PW-xxx), newest first. */
    fun signedInReportsOrdered(reporterUserId: String, limit: Int = 80): List<PersistedPotholeReport> {
        val ownerId = reporterUserId.trim()
        if (ownerId.isBlank()) return emptyList()
        synchronized(lock) {
            return cache
                .asSequence()
                .filter { it.submittedSignedIn && it.reporterUserId == ownerId }
                .sortedByDescending { it.createdAtMs }
                .take(limit)
                .toList()
        }
    }

    /**
     * Re-tags signed-in reports when the canonical PW-xxx id changes after
     * [CitizenProfileRepository.resolveReporterUserId].
     */
    fun migrateReporterUserId(fromId: String, toId: String): Boolean {
        val from = fromId.trim()
        val to = toId.trim()
        if (from.isBlank() || to.isBlank() || from == to) return false
        val ctx = appContext ?: return false
        return synchronized(lock) {
            var changed = false
            for (i in cache.indices) {
                val report = cache[i]
                if (report.submittedSignedIn && report.reporterUserId == from) {
                    cache[i] = report.copy(reporterUserId = to)
                    changed = true
                }
            }
            if (changed) saveToDisk(ctx)
            changed
        }
    }

    /**
     * Copies report photos into app storage and prepends to the list.
     * Call from a background dispatcher; does not touch Compose state.
     */
    fun addReport(
        cityKey: String,
        closeUpUri: Uri,
        wideUri: Uri? = null,
        latitude: Double,
        longitude: Double,
        severity: PotholeSeverity,
        note: String,
        reporterUserId: String,
        submittedSignedIn: Boolean = false,
        potholePosition: PotholePosition = PotholePosition.MIDDLE,
        reportBearingDeg: Float = Float.NaN,
        trafficFacing: TrafficFacingMode = TrafficFacingMode.UNKNOWN,
        issueType: CivicIssueType = CivicIssueType.POTHOLE,
        manholeCondition: ManholeCondition? = null,
        incidentTimeOfDay: IncidentTimeOfDay? = null,
    ): AddReportResult {
        val ctx = appContext ?: return AddReportResult.Failed
        return synchronized(lock) {
            PotholeDuplicateGuard.findActiveDuplicate(
                reports = cache,
                reporterUserId = reporterUserId,
                cityKey = cityKey,
                latitude = latitude,
                longitude = longitude,
                issueType = issueType,
            )?.let { return@synchronized AddReportResult.DuplicateActive(it) }

            val dir = File(ctx.filesDir, "report_photos").apply { mkdirs() }
            val id = System.currentTimeMillis()
            val closeDest = File(dir, "${id}_close.jpg")
            if (!copyUriToFile(ctx, closeUpUri, closeDest)) return@synchronized AddReportResult.Failed
            val widePath = if (wideUri != null) {
                val wideDest = File(dir, "${id}_wide.jpg")
                if (!copyUriToFile(ctx, wideUri, wideDest)) {
                    closeDest.delete()
                    return@synchronized AddReportResult.Failed
                }
                wideDest.absolutePath
            } else {
                ""
            }
            val areaLabel = resolveAreaLabel(ctx, latitude, longitude).takeIf { isMeaningfulAreaName(it) }.orEmpty()
            val routing = BengaluruMunicipalRouting.resolve(cityKey, latitude, longitude)
            val trafficBearing = resolveTrafficBearingDeg(
                reportBearingDeg.takeIf { !it.isNaN() },
                trafficFacing,
            )
            var entry = PersistedPotholeReport(
                id = id,
                cityKey = cityKey,
                createdAtMs = id,
                photoPath = closeDest.absolutePath,
                latitude = latitude,
                longitude = longitude,
                areaLabel = areaLabel,
                widePhotoPath = widePath,
                severity = severity,
                issueType = issueType,
                note = note.trim(),
                submittedSignedIn = submittedSignedIn,
                status = PotholeReportStatus.OPEN,
                reporterUserId = reporterUserId,
                potholePosition = potholePosition.code,
                manholeCondition = manholeCondition?.name.orEmpty(),
                incidentTimeOfDay = incidentTimeOfDay?.name.orEmpty(),
                reportBearingDeg = reportBearingDeg,
                trafficBearingDeg = trafficBearing ?: Float.NaN,
                trafficFacing = trafficFacing.storageKey,
            ).withAssignee(routing.assignee)
            routing.ward?.let { entry = entry.withWard(it) }
            cache.add(0, entry)
            while (cache.size > MAX_STORED) {
                val removed = cache.removeAt(cache.lastIndex)
                deleteReportFiles(removed)
            }
            saveToDisk(ctx)
            AddReportResult.Success(entry)
        }
    }

    /**
     * Location-only OPEN pothole filed when 5 users confirm the same unregistered
     * spot. No photos — shown on every map; skipped by cloud photo sync.
     */
    fun addCrowdConfirmedOpenReport(
        cityKey: String,
        latitude: Double,
        longitude: Double,
        severity: PotholeSeverity,
        note: String,
        reporterUserId: String,
    ): AddReportResult {
        val ctx = appContext ?: return AddReportResult.Failed
        if (!latitude.isFinite() || !longitude.isFinite()) return AddReportResult.Failed
        return synchronized(lock) {
            val nearby = cache.firstOrNull { report ->
                report.cityKey == cityKey &&
                    report.issueType == CivicIssueType.POTHOLE &&
                    report.hasCoordinates() &&
                    report.status.blocksNewReportAtSamePlace() &&
                    PotholeDuplicateGuard.distanceMeters(
                        latitude,
                        longitude,
                        report.latitude,
                        report.longitude,
                    ) <= PotholeDuplicateGuard.SAME_POTHOLE_RADIUS_METERS
            }
            if (nearby != null) return@synchronized AddReportResult.DuplicateActive(nearby)

            val id = System.currentTimeMillis()
            val areaLabel = resolveAreaLabel(ctx, latitude, longitude)
                .takeIf { isMeaningfulAreaName(it) }
                .orEmpty()
            val routing = BengaluruMunicipalRouting.resolve(cityKey, latitude, longitude)
            val owner = reporterUserId.ifBlank { "crowd-hazard" }
            var entry = PersistedPotholeReport(
                id = id,
                cityKey = cityKey,
                createdAtMs = id,
                photoPath = "",
                latitude = latitude,
                longitude = longitude,
                areaLabel = areaLabel,
                widePhotoPath = "",
                severity = severity,
                issueType = CivicIssueType.POTHOLE,
                note = note.trim(),
                submittedSignedIn = owner != "local" && owner != "crowd-hazard",
                status = PotholeReportStatus.OPEN,
                reporterUserId = owner,
                potholePosition = PotholePosition.MIDDLE.code,
            ).withAssignee(routing.assignee)
            routing.ward?.let { entry = entry.withWard(it) }
            cache.add(0, entry)
            while (cache.size > MAX_STORED) {
                val removed = cache.removeAt(cache.lastIndex)
                deleteReportFiles(removed)
            }
            saveToDisk(ctx)
            AddReportResult.Success(entry)
        }
    }

    /** Marks a report as present in Supabase after a successful cloud push. */
    fun markCloudSynced(
        reportId: Long,
        reporterUserId: String,
        storageClosePath: String = "",
        storageWidePath: String = "",
    ): Boolean {
        val ownerId = reporterUserId.trim()
        if (ownerId.isBlank()) return false
        val ctx = appContext ?: return false
        return synchronized(lock) {
            val idx = cache.indexOfFirst {
                it.id == reportId && it.submittedSignedIn && it.reporterUserId == ownerId
            }
            if (idx < 0) return@synchronized false
            val existing = cache[idx]
            val closeStorage = storageClosePath.ifBlank { existing.storageClosePath }
            val wideStorage = storageWidePath.ifBlank { existing.storageWidePath }
            if (existing.cloudSynced &&
                existing.storageClosePath == closeStorage &&
                existing.storageWidePath == wideStorage
            ) {
                return@synchronized true
            }
            cache[idx] = existing.copy(
                cloudSynced = true,
                storageClosePath = closeStorage,
                storageWidePath = wideStorage,
            )
            saveToDisk(ctx)
            true
        }
    }

    /** Signed-in reports saved on-device that still need a Supabase push. */
    fun listUnsyncedSignedInReports(): List<PersistedPotholeReport> {
        return synchronized(lock) {
            cache.filter { it.submittedSignedIn && !it.cloudSynced && it.photoPath.isNotBlank() }
                .sortedByDescending { it.createdAtMs }
                .toList()
        }
    }

    private data class PhotoDownloadJob(
        val reportId: Long,
        val storageClose: String,
        val storageWide: String,
    )

    private suspend fun downloadQueuedPhotos(ctx: Context, jobs: List<PhotoDownloadJob>): Boolean {
        if (jobs.isEmpty()) return false
        var changed = false
        for (job in jobs.distinctBy { it.reportId }) {
            if (job.storageClose.isBlank()) continue
            val paths = com.example.potholereport.data.remote.ReportPhotoCache.ensureLocalPhotos(
                ctx,
                job.reportId,
                job.storageClose,
                job.storageWide,
            ) ?: continue
            synchronized(lock) {
                val idx = cache.indexOfFirst { it.id == job.reportId }
                if (idx < 0) return@synchronized
                val existing = cache[idx]
                val updated = existing.copy(
                    photoPath = paths.first,
                    widePhotoPath = paths.second.ifBlank { existing.widePhotoPath },
                    storageClosePath = job.storageClose,
                    storageWidePath = job.storageWide,
                )
                if (updated != existing) {
                    cache[idx] = updated
                    changed = true
                }
            }
        }
        if (changed) {
            synchronized(lock) { saveToDisk(ctx) }
        }
        return changed
    }

    private fun needsPhotoDownload(report: PersistedPotholeReport, storageClose: String): Boolean {
        if (storageClose.isBlank()) return false
        val local = report.photoPath
        return local.isBlank() || !File(local).exists()
    }

    private fun queuePhotoDownload(
        jobs: MutableList<PhotoDownloadJob>,
        report: PersistedPotholeReport,
        storageClose: String,
        storageWide: String,
    ) {
        if (!needsPhotoDownload(report, storageClose)) return
        jobs += PhotoDownloadJob(report.id, storageClose, storageWide)
    }

    /**
     * Merges the signed-in citizen's Supabase reports locally: restores missing rows,
     * updates status, and removes cloud-synced rows deleted on the server.
     */
    suspend fun syncSignedInReportsFromSupabase(reporterUserId: String): Boolean {
        val ctx = appContext ?: return false
        if (!com.example.potholereport.data.remote.SupabaseClientProvider.isConfigured) return false
        val client = com.example.potholereport.data.remote.SupabaseClientProvider.client ?: return false
        runCatching { client.auth.awaitInitialization() }
        if (client.auth.currentUserOrNull()?.id == null) return false
        val ownerId = reporterUserId.trim()
        if (ownerId.isBlank()) return false
        val rows = com.example.potholereport.data.remote.ReportSyncRepository.fetchMyReportsOrNull()
            ?: return false
        val remoteIds = rows.map { it.id }.toSet()
        val photoJobs = mutableListOf<PhotoDownloadJob>()
        val metadataChanged = synchronized(lock) {
            var changed = false
            val pruneCandidates = cache.filter { report ->
                report.submittedSignedIn &&
                    report.reporterUserId == ownerId &&
                    report.cloudSynced &&
                    report.id !in remoteIds
            }
            for (report in pruneCandidates) {
                if (cache.remove(report)) {
                    deleteReportFiles(report)
                    changed = true
                }
            }
            for (row in rows) {
                val visibleStatus = PotholeReportStatus.fromStored(row.citizenVisibleStatus)
                val storageClose = row.photoPath.trim()
                val storageWide = row.widePhotoPath.trim()
                val idx = cache.indexOfFirst { it.id == row.id }
                if (idx < 0) {
                    val lat = row.latitude ?: Double.NaN
                    val lon = row.longitude ?: Double.NaN
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
                        submittedSignedIn = true,
                        status = visibleStatus,
                        reporterUserId = ownerId,
                        cloudSynced = true,
                        storageClosePath = storageClose,
                        storageWidePath = storageWide,
                        assigneeKey = row.assigneeKey,
                        assigneeCorporation = row.assigneeCorp,
                        assigneeZone = row.assigneeZone,
                        assigneeName = row.assigneeName,
                        assigneePosition = row.assigneeRole,
                        assigneeOfficeAddress = row.assigneeAddr,
                        wardKey = row.wardKey,
                        wardNumber = row.wardNumber,
                        wardName = row.wardName,
                        potholePosition = row.potholePosition.ifBlank { PotholePosition.MIDDLE.code },
                        reportBearingDeg = row.reportBearingDeg ?: Float.NaN,
                        trafficBearingDeg = row.trafficBearingDeg ?: Float.NaN,
                        trafficFacing = row.trafficFacingStorageKey(),
                    ).withManholeFromNote().withIncidentTimeFromNote()
                    if (row.assigneeName.isBlank()) {
                        entry = entry.withAssignee(MunicipalAssignmentResolver.resolve(row.cityKey, lat, lon))
                    }
                    cache.add(entry)
                    queuePhotoDownload(photoJobs, entry, storageClose, storageWide)
                    changed = true
                    continue
                }
                val existing = cache[idx]
                val merged = existing.copy(
                    submittedSignedIn = true,
                    reporterUserId = ownerId,
                    status = visibleStatus,
                    cloudSynced = true,
                    storageClosePath = storageClose.ifBlank { existing.storageClosePath },
                    storageWidePath = storageWide.ifBlank { existing.storageWidePath },
                    assigneeKey = row.assigneeKey.ifBlank { existing.assigneeKey },
                    assigneeCorporation = row.assigneeCorp.ifBlank { existing.assigneeCorporation },
                    assigneeZone = row.assigneeZone.ifBlank { existing.assigneeZone },
                    assigneeName = row.assigneeName.ifBlank { existing.assigneeName },
                    assigneePosition = row.assigneeRole.ifBlank { existing.assigneePosition },
                    assigneeOfficeAddress = row.assigneeAddr.ifBlank { existing.assigneeOfficeAddress },
                    wardKey = row.wardKey.ifBlank { existing.wardKey },
                    wardNumber = row.wardNumber.takeIf { it > 0 } ?: existing.wardNumber,
                    wardName = row.wardName.ifBlank { existing.wardName },
                ).withRemoteDirection(row)
                if (merged != existing) {
                    if (existing.status != visibleStatus) {
                        CitizenNotificationsRepository.addStatusChange(
                            reportId = existing.id,
                            previousStatus = existing.status,
                            newStatus = visibleStatus,
                            areaLabel = existing.areaLabel,
                        )
                    }
                    cache[idx] = merged
                    changed = true
                }
                queuePhotoDownload(
                    photoJobs,
                    merged,
                    merged.storageClosePath,
                    merged.storageWidePath,
                )
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
        val photosChanged = downloadQueuedPhotos(ctx, photoJobs)
        return metadataChanged || photosChanged
    }

    /** Pulls recent city reports from Supabase for guests and fresh installs. */
    suspend fun syncPublicCityReportsFromSupabase(cityKey: String): Boolean {
        if (cityKey.isBlank()) return false
        val ctx = appContext ?: return false
        if (!com.example.potholereport.data.remote.SupabaseClientProvider.isConfigured) return false
        val canonicalCity = CityMetroKeys.canonical(cityKey)
        val rows = com.example.potholereport.data.remote.ReportSyncRepository
            .fetchRecentCityReportsOrNull(canonicalCity, MAX_STORED)
            ?: return false
        val remoteIds = rows.map { it.id }.toSet()
        val photoJobs = mutableListOf<PhotoDownloadJob>()
        val metadataChanged = synchronized(lock) {
            var changed = false
            val stalePublic = cache.filter { report ->
                !report.submittedSignedIn &&
                    CityMetroKeys.canonical(report.cityKey) == canonicalCity &&
                    report.id !in remoteIds &&
                    (report.storageClosePath.isNotBlank() || report.cloudSynced || report.photoPath.isNotBlank())
            }
            for (report in stalePublic) {
                if (cache.remove(report)) {
                    deleteReportFiles(report)
                    changed = true
                }
            }
            for (row in rows) {
                val visibleStatus = PotholeReportStatus.fromStored(row.citizenVisibleStatus)
                val storageClose = row.photoPath.trim()
                val storageWide = row.widePhotoPath.trim()
                val idx = cache.indexOfFirst { it.id == row.id }
                if (idx >= 0) {
                    val existing = cache[idx]
                    if (existing.submittedSignedIn) {
                        val merged = existing.copy(
                            status = visibleStatus,
                            storageClosePath = storageClose.ifBlank { existing.storageClosePath },
                            storageWidePath = storageWide.ifBlank { existing.storageWidePath },
                        )
                        if (merged != existing) {
                            cache[idx] = merged
                            changed = true
                        }
                        queuePhotoDownload(
                            photoJobs,
                            merged,
                            merged.storageClosePath,
                            merged.storageWidePath,
                        )
                        continue
                    }
                    val merged = existing.copy(
                        status = visibleStatus,
                        areaLabel = row.areaLabel.ifBlank { existing.areaLabel },
                        storageClosePath = storageClose.ifBlank { existing.storageClosePath },
                        storageWidePath = storageWide.ifBlank { existing.storageWidePath },
                        assigneeKey = row.assigneeKey.ifBlank { existing.assigneeKey },
                        assigneeCorporation = row.assigneeCorp.ifBlank { existing.assigneeCorporation },
                        assigneeZone = row.assigneeZone.ifBlank { existing.assigneeZone },
                        assigneeName = row.assigneeName.ifBlank { existing.assigneeName },
                        assigneePosition = row.assigneeRole.ifBlank { existing.assigneePosition },
                        assigneeOfficeAddress = row.assigneeAddr.ifBlank { existing.assigneeOfficeAddress },
                        wardKey = row.wardKey.ifBlank { existing.wardKey },
                        wardNumber = row.wardNumber.takeIf { it > 0 } ?: existing.wardNumber,
                        wardName = row.wardName.ifBlank { existing.wardName },
                    ).withRemoteDirection(row)
                    if (merged != existing) {
                        cache[idx] = merged
                        changed = true
                    }
                    queuePhotoDownload(
                        photoJobs,
                        merged,
                        merged.storageClosePath,
                        merged.storageWidePath,
                    )
                    continue
                }
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
                    status = visibleStatus,
                    reporterUserId = row.reporterUserId,
                    cloudSynced = true,
                    storageClosePath = storageClose,
                    storageWidePath = storageWide,
                    assigneeKey = row.assigneeKey,
                    assigneeCorporation = row.assigneeCorp,
                    assigneeZone = row.assigneeZone,
                    assigneeName = row.assigneeName,
                    assigneePosition = row.assigneeRole,
                    assigneeOfficeAddress = row.assigneeAddr,
                    wardKey = row.wardKey,
                    wardNumber = row.wardNumber,
                    wardName = row.wardName,
                    potholePosition = row.potholePosition.ifBlank { PotholePosition.MIDDLE.code },
                    reportBearingDeg = row.reportBearingDeg ?: Float.NaN,
                    trafficBearingDeg = row.trafficBearingDeg ?: Float.NaN,
                    trafficFacing = row.trafficFacingStorageKey(),
                ).withManholeFromNote().withIncidentTimeFromNote()
                if (assignee != null && row.assigneeName.isBlank()) {
                    entry = entry.withAssignee(assignee)
                }
                cache.add(entry)
                queuePhotoDownload(photoJobs, entry, storageClose, storageWide)
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
        val photosChanged = downloadQueuedPhotos(ctx, photoJobs)
        return metadataChanged || photosChanged
    }

    /**
     * Reserved for the government response application to sync status updates.
     * Not exposed in the citizen app UI.
     */
    fun updateSignedInReportStatus(
        reportId: Long,
        reporterUserId: String,
        status: PotholeReportStatus,
    ): Boolean {
        val ownerId = reporterUserId.trim()
        if (ownerId.isBlank()) return false
        val ctx = appContext ?: return false
        return synchronized(lock) {
            val idx = cache.indexOfFirst {
                it.id == reportId && it.submittedSignedIn && it.reporterUserId == ownerId
            }
            if (idx < 0) return@synchronized false
            cache[idx] = cache[idx].copy(status = status)
            saveToDisk(ctx)
            true
        }
    }

    fun deleteSignedInReport(reportId: Long, reporterUserId: String): Boolean {
        val ownerId = reporterUserId.trim()
        if (ownerId.isBlank()) return false
        val ctx = appContext ?: return false
        return synchronized(lock) {
            val idx = cache.indexOfFirst {
                it.id == reportId && it.submittedSignedIn && it.reporterUserId == ownerId
            }
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
                            issueType = CivicIssueType.fromStored(o.optString("issueType", "")),
                            note = o.optString("note", ""),
                            submittedSignedIn = o.optBoolean("signedIn", false),
                            status = PotholeReportStatus.fromStored(o.optString("status", "")),
                            assigneeKey = o.optString("assigneeKey", ""),
                            assigneeCorporation = o.optString("assigneeCorp", ""),
                            assigneeZone = o.optString("assigneeZone", ""),
                            assigneeName = o.optString("assigneeName", ""),
                            assigneePosition = o.optString("assigneeRole", ""),
                            assigneeOfficeAddress = o.optString("assigneeAddr", ""),
                            wardKey = o.optString("wardKey", ""),
                            wardNumber = o.optInt("wardNumber", 0),
                            wardName = o.optString("wardName", ""),
                            potholePosition = o.optString("potholePosition", PotholePosition.MIDDLE.code),
                            manholeCondition = o.optString("manholeCondition", ""),
                            incidentTimeOfDay = o.optString("incidentTimeOfDay", ""),
                            reportBearingDeg = o.optDouble("reportBearingDeg", Double.NaN).toFloat(),
                            trafficBearingDeg = o.optDouble("trafficBearingDeg", Double.NaN).toFloat(),
                            trafficFacing = o.optString("trafficFacing", ""),
                            reporterUserId = o.optString("reporterId", ""),
                            cloudSynced = o.optBoolean("cloudSynced", false),
                            storageClosePath = o.optString("storageClose", ""),
                            storageWidePath = o.optString("storageWide", ""),
                        ).withManholeFromNote().withIncidentTimeFromNote(),
                    )
                }
            }.filter { r ->
                r.submittedSignedIn ||
                    r.cloudSynced ||
                    r.storageClosePath.isNotBlank() ||
                    (r.photoPath.isNotBlank() && File(r.photoPath).exists())
            }
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
                    if (r.issueType != CivicIssueType.POTHOLE) {
                        put("issueType", r.issueType.name)
                    }
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
                    if (r.wardKey.isNotBlank()) {
                        put("wardKey", r.wardKey)
                        put("wardNumber", r.wardNumber)
                        put("wardName", r.wardName)
                    }
                    if (r.potholePosition.isNotBlank() && r.potholePosition != PotholePosition.MIDDLE.code) {
                        put("potholePosition", r.potholePosition)
                    }
                    if (r.manholeCondition.isNotBlank()) {
                        put("manholeCondition", r.manholeCondition)
                    }
                    if (r.incidentTimeOfDay.isNotBlank()) {
                        put("incidentTimeOfDay", r.incidentTimeOfDay)
                    }
                    if (!r.reportBearingDeg.isNaN()) {
                        put("reportBearingDeg", r.reportBearingDeg.toDouble())
                    }
                    if (!r.trafficBearingDeg.isNaN()) {
                        put("trafficBearingDeg", r.trafficBearingDeg.toDouble())
                    }
                    if (r.trafficFacing.isNotBlank()) {
                        put("trafficFacing", r.trafficFacing)
                    }
                    if (r.reporterUserId.isNotBlank()) {
                        put("reporterId", r.reporterUserId)
                    }
                    if (r.cloudSynced) {
                        put("cloudSynced", true)
                    }
                    if (r.storageClosePath.isNotBlank()) {
                        put("storageClose", r.storageClosePath)
                    }
                    if (r.storageWidePath.isNotBlank()) {
                        put("storageWide", r.storageWidePath)
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
