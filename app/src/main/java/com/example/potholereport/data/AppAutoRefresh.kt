package com.example.potholereport.data

import com.example.potholereport.BuildConfig
import com.example.potholereport.data.remote.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Splash timing and background-idle thresholds for automatic data refresh.
 */
object AppAutoRefresh {

    /** Minimum splash logo display time (cold start only). */
    const val SPLASH_MIN_MS = 1_500L

    /** Full refresh when returning after this long in the background (no splash overlay). */
    const val RESUME_IDLE_MS = 10 * 60 * 1_000L

    /** True after the first launch splash finishes; survives for the app process lifetime. */
    var initialSplashCompleted: Boolean = false

    /**
     * Pulls remote report status, checks for app-update notifications, and reloads local caches.
     * Call from a background dispatcher during the splash window.
     */
    suspend fun refreshSignedInData(cityKey: String? = null): Boolean {
        if (!SupabaseClientProvider.isConfigured) {
            withContext(Dispatchers.IO) {
                CitizenNotificationsRepository.checkAppVersion(BuildConfig.VERSION_NAME)
            }
            return false
        }
        return withContext(Dispatchers.IO) {
            CitizenNotificationsRepository.checkAppVersion(BuildConfig.VERSION_NAME)
            if (!cityKey.isNullOrBlank()) {
                RecentReportsRepository.syncPublicCityReportsFromSupabase(cityKey)
            }
            RecentReportsRepository.syncSignedInReportsFromSupabase()
        }
    }

    /** Guest / signed-out refresh: public city feed when [cityKey] is known + app-update notices. */
    suspend fun refreshLocalData(cityKey: String? = null): Boolean {
        if (!SupabaseClientProvider.isConfigured) {
            withContext(Dispatchers.IO) {
                CitizenNotificationsRepository.checkAppVersion(BuildConfig.VERSION_NAME)
            }
            return false
        }
        return withContext(Dispatchers.IO) {
            CitizenNotificationsRepository.checkAppVersion(BuildConfig.VERSION_NAME)
            if (!cityKey.isNullOrBlank()) {
                RecentReportsRepository.syncPublicCityReportsFromSupabase(cityKey)
            }
            false
        }
    }
}
