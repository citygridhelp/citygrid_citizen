package com.example.potholereport.data.remote

import com.example.potholereport.data.EmailChangeStartResult
import com.example.potholereport.data.EmailChangeVerifyResult
import com.example.potholereport.data.LoginResult
import com.example.potholereport.data.PasswordResetCodeResult
import com.example.potholereport.data.SignupStartResult
import com.example.potholereport.data.SignupVerifyResult
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Supabase Auth wrapper for the citizen app. Uses an email-OTP signup flow:
 * signUpWith(Email) sends a 6-digit code (Supabase "Confirm signup" template set
 * to {{ .Token }}), then verifyEmailOtp confirms it and establishes the session.
 *
 * Results are mapped to the app's existing enums so the auth screens are unchanged.
 */
object SupabaseAuthRepository {

    fun currentUserId(): String? =
        SupabaseClientProvider.client?.auth?.currentUserOrNull()?.id

    fun isSignedIn(): Boolean = currentUserId() != null

    /**
     * Creates the account and triggers the verification email. [name] is stored as
     * user metadata (full_name) in auth.users; it is not shown anywhere in the app UI.
     */
    suspend fun startEmailSignup(name: String, email: String, password: String): SignupStartResult {
        val client = SupabaseClientProvider.client ?: return SignupStartResult.FAILED
        val normalized = email.trim().lowercase()
        val cleanName = name.trim()
        if (emailExists(normalized) == true) return SignupStartResult.EMAIL_ALREADY_REGISTERED
        return try {
            client.auth.signUpWith(Email) {
                this.email = normalized
                this.password = password
                if (cleanName.isNotEmpty()) {
                    this.data = buildJsonObject {
                        put("full_name", cleanName)
                    }
                }
            }
            SignupStartResult.CODE_SENT
        } catch (e: Exception) {
            val msg = (e.message ?: "").lowercase()
            if (msg.contains("already") || msg.contains("registered") || msg.contains("exists")) {
                SignupStartResult.EMAIL_ALREADY_REGISTERED
            } else {
                SignupStartResult.FAILED
            }
        }
    }

    /** Verifies the emailed OTP and, on success, signs the user in. */
    suspend fun verifyEmailSignup(email: String, token: String): SignupVerifyResult {
        val client = SupabaseClientProvider.client ?: return SignupVerifyResult.FAILED
        val normalized = email.trim().lowercase()
        val cleanToken = token.trim()
        var lastError: String? = null
        // Token from the "Confirm signup" email verifies under EMAIL; fall back to SIGNUP.
        for (type in listOf(OtpType.Email.EMAIL, OtpType.Email.SIGNUP)) {
            try {
                client.auth.verifyEmailOtp(type = type, email = normalized, token = cleanToken)
                runCatching { client.auth.awaitInitialization() }
                return SignupVerifyResult.SUCCESS
            } catch (e: Exception) {
                lastError = (e.message ?: "").lowercase()
            }
        }
        return when {
            lastError == null -> SignupVerifyResult.FAILED
            lastError.contains("expired") -> SignupVerifyResult.EXPIRED
            lastError.contains("invalid") || lastError.contains("token") -> SignupVerifyResult.INVALID_CODE
            else -> SignupVerifyResult.FAILED
        }
    }

    /** Password sign-in. Distinguishes unknown email from wrong password via [emailExists]. */
    suspend fun login(email: String, password: String): LoginResult {
        val client = SupabaseClientProvider.client ?: return LoginResult.NO_ACCOUNT
        val normalized = email.trim().lowercase()
        return try {
            client.auth.signInWith(Email) {
                this.email = normalized
                this.password = password
            }
            runCatching { client.auth.awaitInitialization() }
            LoginResult.SUCCESS
        } catch (e: Exception) {
            val msg = (e.message ?: "").lowercase()
            when {
                msg.contains("not confirmed") || msg.contains("not verified") ->
                    LoginResult.EMAIL_NOT_VERIFIED
                // Supabase returns a generic "invalid login credentials"; use the
                // email_exists RPC to tell "no account" from "wrong password".
                emailExists(normalized) == false -> LoginResult.NO_ACCOUNT
                else -> LoginResult.WRONG_PASSWORD
            }
        }
    }

    /**
     * Server-side check (email_exists RPC) whether an account exists for [email].
     * Returns null when the check is inconclusive (not configured / RPC error).
     */
    suspend fun emailExists(email: String): Boolean? {
        val client = SupabaseClientProvider.client ?: return null
        val normalized = email.trim().lowercase()
        return try {
            client.postgrest
                .rpc("email_exists", buildJsonObject { put("p_email", normalized) })
                .decodeAs<Boolean>()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Best-effort: ensure a live session exists for these credentials (sign in,
     * else sign up). Used by the lightweight in-report sign-in modal.
     */
    suspend fun ensureSession(email: String, password: String): Boolean {
        val client = SupabaseClientProvider.client ?: return false
        val normalized = email.trim().lowercase()
        return try {
            client.auth.signInWith(Email) {
                this.email = normalized
                this.password = password
            }
            true
        } catch (_: Exception) {
            try {
                client.auth.signUpWith(Email) {
                    this.email = normalized
                    this.password = password
                }
                client.auth.currentUserOrNull() != null
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun signOut() {
        val client = SupabaseClientProvider.client ?: return
        runCatching { client.auth.signOut() }
    }

    /** Returns the signed-in user's email after restoring a persisted Supabase session. */
    suspend fun restoreSignedInEmail(): String? {
        val client = SupabaseClientProvider.client ?: return null
        runCatching { client.auth.awaitInitialization() }
        return client.auth.currentUserOrNull()?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    suspend fun hasLiveSession(): Boolean = restoreSignedInEmail() != null

    /**
     * Starts a signed-in email change. Supabase emails a verification code to the new
     * address (and, when secure email change is enabled, to the current address after
     * the first code is confirmed).
     */
    suspend fun startEmailChange(newEmail: String): EmailChangeStartResult {
        val client = SupabaseClientProvider.client ?: return EmailChangeStartResult.NOT_SIGNED_IN
        runCatching { client.auth.awaitInitialization() }
        if (client.auth.currentUserOrNull()?.id == null) return EmailChangeStartResult.NOT_SIGNED_IN
        val normalized = newEmail.trim().lowercase()
        val current = client.auth.currentUserOrNull()?.email?.trim()?.lowercase().orEmpty()
        if (normalized == current) return EmailChangeStartResult.SAME_EMAIL
        if (emailExists(normalized) == true) return EmailChangeStartResult.EMAIL_ALREADY_REGISTERED
        return try {
            client.auth.updateUser { email = normalized }
            runCatching { client.auth.resendEmail(OtpType.Email.EMAIL_CHANGE, normalized) }
            EmailChangeStartResult.CODE_SENT
        } catch (e: Exception) {
            val msg = (e.message ?: "").lowercase()
            when {
                msg.contains("already") || msg.contains("registered") || msg.contains("exists") ->
                    EmailChangeStartResult.EMAIL_ALREADY_REGISTERED
                else -> EmailChangeStartResult.FAILED
            }
        }
    }

    /**
     * Confirms an email-change OTP. [otpEmail] is the inbox the code was sent to
     * (new address first, then current address when secure email change is on).
     * [targetNewEmail] is the address the user requested.
     */
    suspend fun verifyEmailChangeOtp(
        otpEmail: String,
        token: String,
        targetNewEmail: String,
    ): EmailChangeVerifyResult {
        val client = SupabaseClientProvider.client ?: return EmailChangeVerifyResult.FAILED
        val otpTarget = otpEmail.trim().lowercase()
        val desired = targetNewEmail.trim().lowercase()
        val cleanToken = token.trim()
        return try {
            client.auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL_CHANGE,
                email = otpTarget,
                token = cleanToken,
            )
            runCatching { client.auth.awaitInitialization() }
            val active = client.auth.currentUserOrNull()?.email?.trim()?.lowercase().orEmpty()
            when {
                active == desired -> {
                    CitizenProfileRepository.syncEmail(desired)
                    EmailChangeVerifyResult.SUCCESS
                }
                else -> EmailChangeVerifyResult.NEED_CURRENT_EMAIL
            }
        } catch (e: Exception) {
            val msg = (e.message ?: "").lowercase()
            when {
                msg.contains("expired") -> EmailChangeVerifyResult.EXPIRED
                msg.contains("invalid") || msg.contains("token") -> EmailChangeVerifyResult.INVALID_CODE
                else -> EmailChangeVerifyResult.FAILED
            }
        }
    }

    /** Sends Supabase's password-recovery email (link-based reset). */
    suspend fun requestPasswordReset(email: String): PasswordResetCodeResult {
        val client = SupabaseClientProvider.client ?: return PasswordResetCodeResult.FAILED
        val normalized = email.trim().lowercase()
        return try {
            client.auth.resetPasswordForEmail(normalized)
            PasswordResetCodeResult.CODE_SENT
        } catch (e: Exception) {
            val msg = (e.message ?: "").lowercase()
            when {
                msg.contains("not found") || msg.contains("no user") -> PasswordResetCodeResult.ACCOUNT_NOT_FOUND
                msg.contains("not confirmed") || msg.contains("not verified") ->
                    PasswordResetCodeResult.EMAIL_NOT_VERIFIED
                else -> PasswordResetCodeResult.FAILED
            }
        }
    }
}
