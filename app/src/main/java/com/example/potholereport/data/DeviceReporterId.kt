package com.example.potholereport.data

import android.content.Context
import java.util.Locale
import kotlin.random.Random

/**
 * Stable on-device reporter id for users who submit without signing in.
 * Signed-in users use [UserProfile.anonymousUserId] instead.
 */
object DeviceReporterId {

    private const val PREFS_NAME = "pothole_device_reporter"
    private const val KEY_ID = "reporter_id"

    fun getOrCreate(context: Context): String {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_ID, null)?.let { if (it.isNotBlank()) return it }
        val created = buildString {
            append("PD-")
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            repeat(10) { append(chars[Random.nextInt(chars.length)]) }
        }.uppercase(Locale.US)
        prefs.edit().putString(KEY_ID, created).apply()
        return created
    }
}
