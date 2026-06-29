package com.example.potholereport.data

import android.content.Context
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random

data class UserProfile(
    val email: String,
    val anonymousUserId: String,
    val avatarId: String,
    val displayName: String,
)

object UserProfileRepository {

    private const val PREFS_NAME = "pothole_user_profile"
    private const val KEY_PROFILES_JSON = "profiles_by_email"

    private val lock = Any()
    private var appContext: Context? = null
    /** Demo in-app verification: target email (lowercase) → 4-digit code. */
    private var pendingEmailVerification: Pair<String, String>? = null

    fun init(context: Context) {
        synchronized(lock) {
            if (appContext != null) return
            appContext = context.applicationContext
        }
    }

    /** Loads or creates a privacy-safe profile for this email (no username in user id). */
    fun ensureProfile(email: String): UserProfile {
        val normalized = email.trim().lowercase(Locale.US)
        synchronized(lock) {
            val map = loadProfilesMap().toMutableMap()
            val existing = map[normalized]
            if (existing != null) return existing
            val created = UserProfile(
                email = normalized,
                anonymousUserId = generateAnonymousUserId(),
                avatarId = ProfileAvatarIds.NEUTRAL,
                displayName = DEFAULT_DISPLAY_NAME,
            )
            map[normalized] = created
            saveProfilesMap(map)
            return created
        }
    }

    fun updateEmail(currentEmail: String, newEmail: String): Boolean {
        val oldKey = currentEmail.trim().lowercase(Locale.US)
        val newKey = newEmail.trim().lowercase(Locale.US)
        if (oldKey == newKey) return true
        synchronized(lock) {
            val map = loadProfilesMap().toMutableMap()
            val profile = map.remove(oldKey) ?: return false
            if (map.containsKey(newKey)) return false
            map[newKey] = profile.copy(email = newKey)
            saveProfilesMap(map)
            clearEmailVerification()
            return true
        }
    }

    fun updateDisplayName(email: String, displayName: String): Boolean {
        val key = email.trim().lowercase(Locale.US)
        val name = displayName.trim().ifBlank { DEFAULT_DISPLAY_NAME }
        synchronized(lock) {
            val map = loadProfilesMap().toMutableMap()
            val profile = map[key] ?: return false
            map[key] = profile.copy(displayName = name)
            saveProfilesMap(map)
            return true
        }
    }

    fun updateAvatar(email: String, avatarId: String): Boolean {
        if (avatarId !in ProfileAvatarIds.ALL) return false
        val key = email.trim().lowercase(Locale.US)
        synchronized(lock) {
            val map = loadProfilesMap().toMutableMap()
            val profile = map[key] ?: return false
            map[key] = profile.copy(avatarId = avatarId)
            saveProfilesMap(map)
            return true
        }
    }

    /** Replaces the privacy-safe reporter id after Supabase reconciliation. */
    fun updateAnonymousUserId(email: String, anonymousUserId: String): Boolean {
        val key = email.trim().lowercase(Locale.US)
        val id = anonymousUserId.trim()
        if (id.isBlank()) return false
        synchronized(lock) {
            val map = loadProfilesMap().toMutableMap()
            val profile = map[key] ?: return false
            if (profile.anonymousUserId == id) return true
            map[key] = profile.copy(anonymousUserId = id)
            saveProfilesMap(map)
            return true
        }
    }

    fun maskEmail(email: String): String {
        val trimmed = email.trim()
        val at = trimmed.indexOf('@')
        if (at <= 0 || at >= trimmed.lastIndex) return "••••••@••••"
        val local = trimmed.substring(0, at)
        val domain = trimmed.substring(at + 1)
        val maskedLocal = when {
            local.length <= 2 -> "${local.first()}••"
            else -> {
                val stars = "•".repeat((local.length - 2).coerceIn(2, 6))
                "${local.first()}$stars${local.last()}"
            }
        }
        val domainParts = domain.split('.')
        val maskedDomain = if (domainParts.size >= 2) {
            val name = domainParts.first()
            val tld = domainParts.last()
            val nameMask = if (name.length <= 2) "••" else "${name.first()}••"
            "$nameMask.$tld"
        } else {
            "••••"
        }
        return "$maskedLocal@$maskedDomain"
    }

    /**
     * Simulates sending a 4-digit code to [targetEmail]. Returns the code for demo UI only
     * (no real email server in this prototype).
     */
    fun requestEmailVerificationCode(targetEmail: String): String? {
        val normalized = targetEmail.trim().lowercase(Locale.US)
        if (!normalized.contains('@')) return null
        val code = (1000 + Random.nextInt(9000)).toString()
        synchronized(lock) {
            pendingEmailVerification = normalized to code
        }
        return code
    }

    fun verifyEmailChangeCode(targetEmail: String, code: String): Boolean {
        val normalized = targetEmail.trim().lowercase(Locale.US)
        val entered = code.trim()
        synchronized(lock) {
            val pending = pendingEmailVerification ?: return false
            if (pending.first != normalized) return false
            if (pending.second != entered) return false
            return true
        }
    }

    fun clearEmailVerification() {
        synchronized(lock) {
            pendingEmailVerification = null
        }
    }

    private fun generateAnonymousUserId(): String {
        val token = buildString(8) {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            repeat(8) { append(chars[Random.nextInt(chars.length)]) }
        }
        return "PW-$token"
    }

    private fun loadProfilesMap(): Map<String, UserProfile> {
        val ctx = appContext ?: return emptyMap()
        val raw = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROFILES_JSON, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val emailKey = keys.next()
                    val o = root.getJSONObject(emailKey)
                    put(
                        emailKey,
                        UserProfile(
                            email = o.getString("email"),
                            anonymousUserId = o.getString("anonId"),
                            avatarId = o.optString("avatar", ProfileAvatarIds.NEUTRAL),
                            displayName = o.optString("displayName", DEFAULT_DISPLAY_NAME),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveProfilesMap(map: Map<String, UserProfile>) {
        val ctx = appContext ?: return
        val root = JSONObject()
        for ((key, p) in map) {
            root.put(
                key,
                JSONObject().apply {
                    put("email", p.email)
                    put("anonId", p.anonymousUserId)
                    put("avatar", p.avatarId)
                    put("displayName", p.displayName)
                },
            )
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILES_JSON, root.toString())
            .apply()
    }

    const val DEFAULT_DISPLAY_NAME = "Anonymous reporter"
}

