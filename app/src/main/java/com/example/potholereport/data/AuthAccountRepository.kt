package com.example.potholereport.data

import android.content.Context
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random
import org.json.JSONObject

data class AuthAccount(
    val email: String,
    val fullName: String,
    val passwordHash: String,
    val emailVerified: Boolean,
    val createdAtMs: Long,
)

enum class SignupStartResult {
    CODE_SENT,
    EMAIL_ALREADY_REGISTERED,
    FAILED,
}

enum class SignupVerifyResult {
    SUCCESS,
    INVALID_CODE,
    EXPIRED,
    FAILED,
}

enum class EmailChangeStartResult {
    CODE_SENT,
    EMAIL_ALREADY_REGISTERED,
    SAME_EMAIL,
    NOT_SIGNED_IN,
    FAILED,
}

enum class EmailChangeVerifyResult {
    /** New address is active on the Supabase session. */
    SUCCESS,
    /** Secure email change: new address confirmed; code also sent to current email. */
    NEED_CURRENT_EMAIL,
    INVALID_CODE,
    EXPIRED,
    FAILED,
}

enum class LoginResult {
    SUCCESS,
    NO_ACCOUNT,
    EMAIL_NOT_VERIFIED,
    WRONG_PASSWORD,
}

enum class PasswordResetCodeResult {
    CODE_SENT,
    ACCOUNT_NOT_FOUND,
    EMAIL_NOT_VERIFIED,
    FAILED,
}

enum class PasswordResetResult {
    SUCCESS,
    INVALID_CODE,
    EXPIRED,
    ACCOUNT_NOT_FOUND,
    FAILED,
}

/**
 * Local auth repository (prototype): account, email-verification OTP, and password reset OTP.
 * OTPs are generated and stored locally until backend/email service is integrated.
 */
object AuthAccountRepository {
    private const val PREFS_NAME = "pothole_auth_accounts"
    private const val KEY_ACCOUNTS_JSON = "accounts_json"
    private const val OTP_EXPIRY_MS = 10 * 60 * 1000L

    private data class PendingSignup(
        val email: String,
        val fullName: String,
        val passwordHash: String,
        val code: String,
        val expiresAtMs: Long,
    )

    private data class PendingReset(
        val email: String,
        val code: String,
        val expiresAtMs: Long,
    )

    private val lock = Any()
    private var appContext: Context? = null
    private var pendingSignup: PendingSignup? = null
    private var pendingReset: PendingReset? = null

    fun init(context: Context) {
        synchronized(lock) {
            if (appContext != null) return
            appContext = context.applicationContext
        }
    }

    fun startSignup(name: String, email: String, rawPassword: String): Pair<SignupStartResult, String?> {
        val normalized = email.trim().lowercase(Locale.US)
        if (normalized.isBlank()) return SignupStartResult.FAILED to null
        synchronized(lock) {
            val accounts = loadAccounts()
            if (accounts.containsKey(normalized)) {
                return SignupStartResult.EMAIL_ALREADY_REGISTERED to null
            }
            val code = generateCode()
            pendingSignup = PendingSignup(
                email = normalized,
                fullName = name.trim(),
                passwordHash = sha256(rawPassword),
                code = code,
                expiresAtMs = System.currentTimeMillis() + OTP_EXPIRY_MS,
            )
            return SignupStartResult.CODE_SENT to code
        }
    }

    fun verifySignup(email: String, code: String): SignupVerifyResult {
        val normalized = email.trim().lowercase(Locale.US)
        synchronized(lock) {
            val pending = pendingSignup ?: return SignupVerifyResult.FAILED
            if (pending.email != normalized) return SignupVerifyResult.INVALID_CODE
            if (System.currentTimeMillis() > pending.expiresAtMs) {
                pendingSignup = null
                return SignupVerifyResult.EXPIRED
            }
            if (pending.code != code.trim()) return SignupVerifyResult.INVALID_CODE
            val accounts = loadAccounts().toMutableMap()
            accounts[normalized] = AuthAccount(
                email = normalized,
                fullName = pending.fullName.ifBlank { "Citizen" },
                passwordHash = pending.passwordHash,
                emailVerified = true,
                createdAtMs = System.currentTimeMillis(),
            )
            saveAccounts(accounts)
            pendingSignup = null
            return SignupVerifyResult.SUCCESS
        }
    }

    fun login(email: String, rawPassword: String): LoginResult {
        val normalized = email.trim().lowercase(Locale.US)
        synchronized(lock) {
            val account = loadAccounts()[normalized] ?: return LoginResult.NO_ACCOUNT
            if (!account.emailVerified) return LoginResult.EMAIL_NOT_VERIFIED
            if (account.passwordHash != sha256(rawPassword)) return LoginResult.WRONG_PASSWORD
            return LoginResult.SUCCESS
        }
    }

    fun requestPasswordReset(email: String): Pair<PasswordResetCodeResult, String?> {
        val normalized = email.trim().lowercase(Locale.US)
        synchronized(lock) {
            val account = loadAccounts()[normalized] ?: return PasswordResetCodeResult.ACCOUNT_NOT_FOUND to null
            if (!account.emailVerified) return PasswordResetCodeResult.EMAIL_NOT_VERIFIED to null
            val code = generateCode()
            pendingReset = PendingReset(
                email = normalized,
                code = code,
                expiresAtMs = System.currentTimeMillis() + OTP_EXPIRY_MS,
            )
            return PasswordResetCodeResult.CODE_SENT to code
        }
    }

    fun resetPassword(email: String, code: String, newRawPassword: String): PasswordResetResult {
        val normalized = email.trim().lowercase(Locale.US)
        synchronized(lock) {
            val pending = pendingReset ?: return PasswordResetResult.FAILED
            if (pending.email != normalized) return PasswordResetResult.INVALID_CODE
            if (System.currentTimeMillis() > pending.expiresAtMs) {
                pendingReset = null
                return PasswordResetResult.EXPIRED
            }
            if (pending.code != code.trim()) return PasswordResetResult.INVALID_CODE
            val accounts = loadAccounts().toMutableMap()
            val current = accounts[normalized] ?: return PasswordResetResult.ACCOUNT_NOT_FOUND
            accounts[normalized] = current.copy(passwordHash = sha256(newRawPassword))
            saveAccounts(accounts)
            pendingReset = null
            return PasswordResetResult.SUCCESS
        }
    }

    private fun generateCode(): String = (100000 + Random.nextInt(900000)).toString()

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun loadAccounts(): Map<String, AuthAccount> {
        val ctx = appContext ?: return emptyMap()
        val raw = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCOUNTS_JSON, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val o = root.getJSONObject(key)
                    put(
                        key,
                        AuthAccount(
                            email = o.optString("email", key),
                            fullName = o.optString("fullName", "Citizen"),
                            passwordHash = o.getString("passwordHash"),
                            emailVerified = o.optBoolean("emailVerified", true),
                            createdAtMs = o.optLong("createdAtMs", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveAccounts(accounts: Map<String, AuthAccount>) {
        val ctx = appContext ?: return
        val root = JSONObject()
        for ((key, account) in accounts) {
            root.put(
                key,
                JSONObject().apply {
                    put("email", account.email)
                    put("fullName", account.fullName)
                    put("passwordHash", account.passwordHash)
                    put("emailVerified", account.emailVerified)
                    put("createdAtMs", account.createdAtMs)
                },
            )
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCOUNTS_JSON, root.toString())
            .apply()
    }
}

