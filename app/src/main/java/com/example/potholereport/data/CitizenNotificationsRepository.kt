package com.example.potholereport.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CitizenNotificationsRepository {

    private const val PREFS_NAME = "citizen_notifications"
    private const val KEY_JSON = "notifications_json"
    private const val KEY_LAST_SEEN_VERSION = "last_seen_app_version"
    /** Max unread pile while the user has not cleared the inbox. */
    private const val MAX_PILE = 50
    /** Steady inbox size once the user is active; read items beyond this are removed on close / new arrivals. */
    private const val STEADY_CAP = 10

    private val lock = Any()
    private var appContext: Context? = null
    private var cache: MutableList<CitizenNotification> = mutableListOf()

    fun init(context: Context) {
        synchronized(lock) {
            if (appContext != null) return
            appContext = context.applicationContext
            val loaded = loadFromDisk().sortedBy { it.createdAtMs }
            cache = loaded.takeLast(MAX_PILE).toMutableList()
            if (loaded.size != cache.size) {
                saveToDisk(context.applicationContext)
            }
        }
    }

    fun all(): List<CitizenNotification> {
        synchronized(lock) {
            return cache.sortedBy { it.createdAtMs }
        }
    }

    fun unreadCount(): Int {
        synchronized(lock) {
            return cache.count { !it.read }
        }
    }

    fun markRead(id: String) {
        val ctx = appContext ?: return
        synchronized(lock) {
            val idx = cache.indexOfFirst { it.id == id }
            if (idx < 0 || cache[idx].read) return
            cache[idx] = cache[idx].copy(read = true)
            saveToDisk(ctx)
        }
    }

    fun markAllRead() {
        val ctx = appContext ?: return
        synchronized(lock) {
            var changed = false
            for (i in cache.indices) {
                if (!cache[i].read) {
                    cache[i] = cache[i].copy(read = true)
                    changed = true
                }
            }
            if (changed) saveToDisk(ctx)
        }
    }

    /**
     * After the notifications dialog closes: remove oldest read items while the inbox has more
     * than [STEADY_CAP] entries. At [STEADY_CAP] or below, read items are kept until a new
     * notification evicts the oldest read one.
     */
    fun applyReadDeletionsOnClose() {
        val ctx = appContext ?: return
        synchronized(lock) {
            if (trimReadWhileOverSteadyCap()) {
                saveToDisk(ctx)
            }
        }
    }

    fun addStatusChange(
        reportId: Long,
        previousStatus: PotholeReportStatus,
        newStatus: PotholeReportStatus,
        areaLabel: String,
    ) {
        if (previousStatus == newStatus) return
        val locationHint = areaLabel.trim().ifBlank { "your report" }
        addNotification(
            CitizenNotification(
                id = "status-$reportId-${newStatus.name}-${System.currentTimeMillis()}",
                type = CitizenNotification.Type.STATUS_CHANGE,
                title = "Report status updated",
                body = "$locationHint is now ${newStatus.displayLabel.lowercase()} " +
                    "(was ${previousStatus.displayLabel.lowercase()}).",
                createdAtMs = System.currentTimeMillis(),
                reportId = reportId,
            ),
        )
    }

    fun checkAppVersion(currentVersionName: String) {
        val ctx = appContext ?: return
        val version = currentVersionName.trim()
        if (version.isEmpty()) return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeen = prefs.getString(KEY_LAST_SEEN_VERSION, null)
        prefs.edit().putString(KEY_LAST_SEEN_VERSION, version).apply()
        if (lastSeen == null || lastSeen == version) return
        addNotification(
            CitizenNotification(
                id = "app-update-$version-${System.currentTimeMillis()}",
                type = CitizenNotification.Type.APP_UPDATE,
                title = "App updated",
                body = "City Grid was updated to v$version. Check Recent Reports and map " +
                    "features for the latest improvements.",
                createdAtMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun addNotification(notification: CitizenNotification) {
        val ctx = appContext ?: return
        synchronized(lock) {
            if (cache.any { it.id == notification.id }) return
            cache.add(notification)
            trimPileOverflow()
            trimReadWhileOverSteadyCap()
            saveToDisk(ctx)
        }
    }

    private fun trimPileOverflow() {
        while (cache.size > MAX_PILE) {
            cache.removeAt(0)
        }
    }

    /** Returns true if anything was removed. */
    private fun trimReadWhileOverSteadyCap(): Boolean {
        var changed = false
        while (cache.size > STEADY_CAP) {
            val oldestRead = cache.filter { it.read }.minByOrNull { it.createdAtMs } ?: break
            cache.removeAll { it.id == oldestRead.id }
            changed = true
        }
        return changed
    }

    private fun loadFromDisk(): List<CitizenNotification> {
        val ctx = appContext ?: return emptyList()
        val raw = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        CitizenNotification(
                            id = o.getString("id"),
                            type = CitizenNotification.Type.valueOf(
                                o.optString("type", CitizenNotification.Type.GENERAL.name),
                            ),
                            title = o.getString("title"),
                            body = o.getString("body"),
                            createdAtMs = o.getLong("time"),
                            read = o.optBoolean("read", false),
                            reportId = if (o.has("reportId")) o.getLong("reportId") else null,
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToDisk(ctx: Context) {
        val arr = JSONArray()
        for (n in cache) {
            arr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("type", n.type.name)
                    put("title", n.title)
                    put("body", n.body)
                    put("time", n.createdAtMs)
                    put("read", n.read)
                    n.reportId?.let { put("reportId", it) }
                },
            )
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, arr.toString())
            .apply()
    }

    fun formatTimestamp(ms: Long): String {
        val fmt = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
        return fmt.format(Date(ms))
    }
}
