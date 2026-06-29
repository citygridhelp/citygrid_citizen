package com.example.potholereport.data.remote

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Resolves a stable [reporter_user_id] (PW-xxx) for the signed-in Supabase account
 * so the same citizen keeps one privacy id across devices and reinstalls.
 */
object CitizenProfileRepository {

    private const val TAG = "CitizenProfile"

    suspend fun resolveReporterUserId(localFallback: String): String {
        if (!SupabaseClientProvider.isConfigured) return localFallback
        val client = SupabaseClientProvider.client ?: return localFallback
        runCatching { client.auth.awaitInitialization() }
        if (client.auth.currentUserOrNull()?.id == null) return localFallback
        return try {
            client.postgrest
                .rpc(
                    function = "citizen_reporter_user_id",
                    parameters = buildJsonObject { put("p_desired", localFallback) },
                )
                .decodeAs<String>()
                .trim()
                .ifBlank { localFallback }
        } catch (e: Exception) {
            Log.w(TAG, "resolveReporterUserId failed: ${e.message}")
            localFallback
        }
    }

    /** Keeps [citizen_profiles.email] aligned with auth after an email change. */
    suspend fun syncEmail(email: String): Boolean {
        if (!SupabaseClientProvider.isConfigured) return false
        val client = SupabaseClientProvider.client ?: return false
        runCatching { client.auth.awaitInitialization() }
        val authId = client.auth.currentUserOrNull()?.id ?: return false
        val normalized = email.trim().lowercase()
        if (normalized.isBlank()) return false
        return try {
            client.from("citizen_profiles").update(
                buildJsonObject { put("email", normalized) },
            ) {
                filter { eq("auth_id", authId) }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "syncEmail failed: ${e.message}")
            false
        }
    }
}
