package com.example.potholereport.ui.profile

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.potholereport.data.EmailChangeStartResult
import com.example.potholereport.data.EmailChangeVerifyResult
import com.example.potholereport.data.ProfileAvatarIds
import com.example.potholereport.data.UserProfileRepository
import com.example.potholereport.ui.theme.CitizenIcon
import com.example.potholereport.ui.theme.CitizenInk
import com.example.potholereport.ui.theme.CitizenLightSurface
import com.example.potholereport.ui.theme.CitizenRadius
import com.example.potholereport.ui.theme.CitizenSpace
import com.example.potholereport.ui.theme.CitizenType
import com.example.potholereport.ui.theme.CitizenPrivacyCopy
import kotlinx.coroutines.launch

@Composable
fun ProfileDialog(
    email: String,
    userId: String,
    avatarId: String,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onSave: (avatarId: String, verifiedNewEmail: String?) -> Unit,
    onStartEmailChange: suspend (String) -> EmailChangeStartResult,
    onVerifyEmailChange: suspend (otpEmail: String, code: String, targetNewEmail: String) -> EmailChangeVerifyResult,
    startInEditMode: Boolean = false,
    showSignOut: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var editMode by rememberSaveable { mutableStateOf(startInEditMode) }
    var draftAvatarId by rememberSaveable(avatarId) { mutableStateOf(avatarId) }

    var newEmailInput by rememberSaveable { mutableStateOf("") }
    var verificationCodeInput by rememberSaveable { mutableStateOf("") }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var awaitingCurrentEmailCode by rememberSaveable { mutableStateOf(false) }
    var emailVerified by rememberSaveable { mutableStateOf(false) }
    var verifiedNewEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailBusy by rememberSaveable { mutableStateOf(false) }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = valueColor,
        unfocusedTextColor = valueColor,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

    val shownAvatarId = if (editMode) draftAvatarId else avatarId
    val shownEmail = verifiedNewEmail?.takeIf { editMode && emailVerified } ?: email
    val maskedEmail = UserProfileRepository.maskEmail(shownEmail)

    val avatarChanged = draftAvatarId != avatarId
    val emailChanged = emailVerified && verifiedNewEmail != null &&
        !verifiedNewEmail.equals(email, ignoreCase = true)
    val hasPendingChanges = editMode && (avatarChanged || emailChanged)

    fun resetEditState() {
        draftAvatarId = avatarId
        newEmailInput = ""
        verificationCodeInput = ""
        codeSent = false
        awaitingCurrentEmailCode = false
        emailVerified = false
        verifiedNewEmail = null
        emailError = null
        emailBusy = false
    }

    fun exitEditMode() {
        resetEditState()
        editMode = false
    }

    Dialog(onDismissRequest = onDismiss) {
        CitizenLightSurface {
        Surface(
            shape = CitizenRadius.Card,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(CitizenSpace.Xl),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Profile",
                            style = CitizenType.ScreenTitle,
                            color = valueColor,
                        )
                        Text(
                            CitizenPrivacyCopy.SHORT,
                            style = CitizenType.Helper,
                            color = CitizenInk.Helper,
                            modifier = Modifier.padding(top = CitizenSpace.Xs),
                        )
                    }
                    if (!editMode) {
                        IconButton(
                            onClick = {
                                resetEditState()
                                draftAvatarId = avatarId
                                editMode = true
                            },
                            modifier = Modifier.size(CitizenIcon.Tap),
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(CitizenIcon.Header),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(CitizenSpace.Lg))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CartoonAvatar(avatarId = shownAvatarId, size = 72.dp)
                    Spacer(modifier = Modifier.width(CitizenSpace.Lg))
                    Column {
                        Text(
                            "USER ID",
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = labelColor,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            userId,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = valueColor,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "EMAIL",
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = labelColor,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(maskedEmail, fontSize = 14.sp, color = valueColor)
                    }
                }

                if (editMode) {
                    Spacer(modifier = Modifier.height(CitizenSpace.Lg))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(CitizenSpace.Md))

                    Text(
                        "CHANGE AVATAR",
                        style = CitizenType.Caption,
                        fontWeight = FontWeight.Bold,
                        color = CitizenInk.Helper,
                    )
                    Text(
                        "Preset characters only — no photo uploads.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                        modifier = Modifier.padding(top = CitizenSpace.Xs, bottom = CitizenSpace.Sm),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm)) {
                        items(ProfileAvatarIds.ORDERED) { id ->
                            SelectableCartoonAvatar(
                                avatarId = id,
                                selected = id == draftAvatarId,
                                onClick = { draftAvatarId = id },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(CitizenSpace.Md))
                    Text(
                        "CHANGE EMAIL",
                        style = CitizenType.Caption,
                        fontWeight = FontWeight.Bold,
                        color = CitizenInk.Helper,
                    )
                    Text(
                        if (awaitingCurrentEmailCode) {
                            "Enter the code sent to your current email (${UserProfileRepository.maskEmail(email)})."
                        } else {
                            "A verification code will be sent to your new email. Enter it to confirm."
                        },
                        fontSize = 11.sp,
                        color = labelColor,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                    if (!awaitingCurrentEmailCode) {
                        OutlinedTextField(
                            value = newEmailInput,
                            onValueChange = {
                                newEmailInput = it
                                emailError = null
                                emailVerified = false
                                verifiedNewEmail = null
                                codeSent = false
                                verificationCodeInput = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("New email") },
                            singleLine = true,
                            enabled = !emailVerified && !emailBusy,
                            isError = emailError != null,
                            supportingText = emailError?.let { err -> { Text(err, color = CitizenInk.Error) } },
                            colors = fieldColors,
                        )
                    }
                    if (!emailVerified) {
                        if (!awaitingCurrentEmailCode) {
                            TextButton(
                                onClick = {
                                    val trimmed = newEmailInput.trim().lowercase()
                                    if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                                        emailError = "Enter a valid email"
                                        return@TextButton
                                    }
                                    if (trimmed == email.lowercase()) {
                                        emailError = "This is already your email"
                                        return@TextButton
                                    }
                                    emailBusy = true
                                    scope.launch {
                                        when (onStartEmailChange(trimmed)) {
                                            EmailChangeStartResult.CODE_SENT -> {
                                                codeSent = true
                                                awaitingCurrentEmailCode = false
                                                emailError = null
                                            }
                                            EmailChangeStartResult.EMAIL_ALREADY_REGISTERED ->
                                                emailError = "That email is already registered"
                                            EmailChangeStartResult.SAME_EMAIL ->
                                                emailError = "This is already your email"
                                            EmailChangeStartResult.NOT_SIGNED_IN ->
                                                emailError = "Sign in again to change email"
                                            EmailChangeStartResult.FAILED ->
                                                emailError = "Could not send code. Try again."
                                        }
                                        emailBusy = false
                                    }
                                },
                                enabled = newEmailInput.isNotBlank() && !emailBusy,
                            ) {
                                if (emailBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("Send verification code", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (codeSent || awaitingCurrentEmailCode) {
                            if (awaitingCurrentEmailCode) {
                                Text(
                                    "New address confirmed. Enter the code sent to your current email " +
                                        "(${UserProfileRepository.maskEmail(email)}).",
                                    fontSize = 11.sp,
                                    color = labelColor,
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                            } else if (codeSent) {
                                Text(
                                    "Code sent to ${UserProfileRepository.maskEmail(newEmailInput.trim())}. " +
                                        "Enter the numeric code from the email (not the confirmation link).",
                                    fontSize = 11.sp,
                                    color = labelColor,
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                            }
                            OutlinedTextField(
                                value = verificationCodeInput,
                                onValueChange = {
                                    verificationCodeInput = it.filter { ch -> ch.isDigit() }.take(8)
                                    emailError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        if (awaitingCurrentEmailCode) "Code from current email" else "Verification code",
                                    )
                                },
                                singleLine = true,
                                enabled = !emailBusy,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = fieldColors,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val target = newEmailInput.trim().lowercase()
                                    if (verificationCodeInput.length < 4) {
                                        emailError = "Enter the verification code"
                                        return@Button
                                    }
                                    val otpEmail = if (awaitingCurrentEmailCode) {
                                        email.trim().lowercase()
                                    } else {
                                        target
                                    }
                                    emailBusy = true
                                    scope.launch {
                                        when (
                                            onVerifyEmailChange(otpEmail, verificationCodeInput, target)
                                        ) {
                                            EmailChangeVerifyResult.SUCCESS -> {
                                                awaitingCurrentEmailCode = false
                                                emailError = null
                                                onSave(draftAvatarId, target)
                                                exitEditMode()
                                            }
                                            EmailChangeVerifyResult.NEED_CURRENT_EMAIL -> {
                                                awaitingCurrentEmailCode = true
                                                verificationCodeInput = ""
                                                emailError = null
                                                codeSent = true
                                            }
                                            EmailChangeVerifyResult.INVALID_CODE ->
                                                emailError = "Invalid verification code"
                                            EmailChangeVerifyResult.EXPIRED ->
                                                emailError = "Code expired. Request a new code."
                                            EmailChangeVerifyResult.FAILED ->
                                                emailError = "Could not verify code. Try again."
                                        }
                                        emailBusy = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = verificationCodeInput.length >= 4 && !emailBusy,
                                shape = CitizenRadius.Sharp,
                            ) {
                                if (emailBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Text(
                                        if (awaitingCurrentEmailCode) "Confirm current email code" else "Confirm verification code",
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "Email verified. Save to apply this address.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                ) {
                    if (showSignOut) {
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.weight(1f),
                            shape = CitizenRadius.Sharp,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIGN OUT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (hasPendingChanges) {
                        Button(
                            onClick = {
                                val newMail = if (emailChanged) verifiedNewEmail else null
                                onSave(draftAvatarId, newMail)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = CitizenRadius.Sharp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text("SAVE & CLOSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (editMode && !startInEditMode) exitEditMode() else onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = CitizenRadius.Sharp,
                        ) {
                            Text("CLOSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        }
    }
}
