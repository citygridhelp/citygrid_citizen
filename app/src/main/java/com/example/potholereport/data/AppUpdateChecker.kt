package com.example.potholereport.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.potholereport.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Asks Google Play whether a newer City Grid build is published.
 * Adds an in-app bell notification when the installed app is older.
 * No-op on sideload / debug installs that are not from Play.
 */
object AppUpdateChecker {

    /**
     * @return true if a newer Play version exists and a notification was (or already is) present.
     */
    suspend fun checkAndNotifyIfNeeded(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val manager = AppUpdateManagerFactory.create(context.applicationContext)
            val info: AppUpdateInfo? = suspendCancellableCoroutine { cont ->
                manager.appUpdateInfo
                    .addOnSuccessListener { result -> cont.resume(result) }
                    .addOnFailureListener { cont.resume(null) }
            }
            if (info == null) return@withContext false

            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                return@withContext false
            }
            val availableCode = info.availableVersionCode()
            if (availableCode <= BuildConfig.VERSION_CODE) return@withContext false
            CitizenNotificationsRepository.notifyUpdateAvailable(
                installedVersionName = BuildConfig.VERSION_NAME,
                availableVersionCode = availableCode,
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openPlayStoreListing(context: Context) {
        val packageName = context.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(market) }
            .recoverCatching { context.startActivity(web) }
    }
}
