package com.example.potholereport.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.potholereport.BuildConfig
import com.example.potholereport.data.PersistedPotholeReport
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads evidence-bucket photos into app storage so synced reports can show
 * thumbnails without re-fetching on every scroll.
 */
object ReportPhotoCache {

    private const val EVIDENCE_BUCKET = "evidence"

    fun photosDir(context: Context): File =
        File(context.filesDir, "report_photos").apply { mkdirs() }

    fun publicObjectUrl(objectPath: String): String? {
        if (!SupabaseClientProvider.isConfigured || objectPath.isBlank()) return null
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        val encoded = objectPath.split('/').joinToString("/") {
            URLEncoder.encode(it, Charsets.UTF_8.name())
        }
        return "$base/storage/v1/object/public/$EVIDENCE_BUCKET/$encoded"
    }

    /**
     * Ensures close (and optional wide) photos exist locally. Returns local paths
     * or null when the close photo could not be fetched.
     */
    suspend fun ensureLocalPhotos(
        context: Context,
        reportId: Long,
        storageClosePath: String,
        storageWidePath: String,
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        if (storageClosePath.isBlank()) return@withContext null
        val dir = photosDir(context)
        val closeDest = File(dir, "${reportId}_close.jpg")
        val wideDest = File(dir, "${reportId}_wide.jpg")
        if (!closeDest.exists() || closeDest.length() == 0L) {
            if (!downloadToFile(storageClosePath, closeDest)) return@withContext null
        }
        var wideLocal = ""
        if (storageWidePath.isNotBlank()) {
            if (!wideDest.exists() || wideDest.length() == 0L) {
                downloadToFile(storageWidePath, wideDest)
            }
            if (wideDest.exists() && wideDest.length() > 0L) {
                wideLocal = wideDest.absolutePath
            }
        }
        closeDest.absolutePath to wideLocal
    }

    suspend fun loadCloseBitmap(context: Context, report: PersistedPotholeReport): Bitmap? =
        withContext(Dispatchers.IO) {
            decodeLocal(report.photoPath)?.let { return@withContext it }
            val storageClose = report.storageClosePath.ifBlank {
                if (report.cloudSynced) "${report.id}/close.jpg" else ""
            }
            if (storageClose.isBlank()) return@withContext null
            val storageWide = report.storageWidePath.ifBlank {
                if (report.cloudSynced) "${report.id}/wide.jpg" else ""
            }
            val paths = ensureLocalPhotos(context, report.id, storageClose, storageWide) ?: return@withContext null
            decodeLocal(paths.first)
        }

    suspend fun loadWideBitmap(context: Context, report: PersistedPotholeReport): Bitmap? =
        withContext(Dispatchers.IO) {
            decodeLocal(report.widePhotoPath)?.let { return@withContext it }
            val storageWide = report.storageWidePath.ifBlank {
                if (report.cloudSynced) "${report.id}/wide.jpg" else ""
            }
            if (storageWide.isBlank()) return@withContext null
            val storageClose = report.storageClosePath.ifBlank {
                if (report.cloudSynced) "${report.id}/close.jpg" else ""
            }
            val paths = ensureLocalPhotos(context, report.id, storageClose, storageWide) ?: return@withContext null
            decodeLocal(paths.second)
        }

    private fun decodeLocal(path: String): Bitmap? {
        if (path.isBlank() || !File(path).exists()) return null
        return runCatching {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeFile(path, opts)
        }.getOrNull()
    }

    private fun downloadToFile(objectPath: String, dest: File): Boolean {
        val bytes = downloadBytes(objectPath) ?: return false
        if (bytes.isEmpty()) return false
        dest.parentFile?.mkdirs()
        dest.writeBytes(bytes)
        return true
    }

    private fun downloadBytes(objectPath: String): ByteArray? {
        val url = publicObjectUrl(objectPath) ?: return null
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            conn.inputStream.use { it.readBytes() }
        }.getOrNull()
    }
}
