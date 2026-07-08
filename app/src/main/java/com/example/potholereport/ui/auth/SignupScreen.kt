package com.example.potholereport.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.potholereport.data.SignupStartResult
import com.example.potholereport.data.SignupVerifyResult
import com.example.potholereport.ui.theme.PotholeReportTheme

@Composable
fun SignupScreen(
    onNavigateBackToLogin: () -> Unit,
    onStartSignupVerification: (String, String, String) -> Pair<SignupStartResult, String?>,
    onVerifySignupCode: (String, String) -> SignupVerifyResult,
    onSignupSuccess: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialEmail: String = "",
    initialPassword: String = "",
    initialVerificationRequested: Boolean = false,
    onPendingSignupChanged: (String, String, String, Boolean) -> Unit = { _, _, _, _ -> },
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var email by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    var password by rememberSaveable(initialPassword) { mutableStateOf(initialPassword) }
    var confirmPassword by rememberSaveable(initialPassword) { mutableStateOf(initialPassword) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmError by rememberSaveable { mutableStateOf<String?>(null) }
    var code by rememberSaveable { mutableStateOf("") }
    var codeError by rememberSaveable { mutableStateOf<String?>(null) }
    var infoMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var verificationRequested by rememberSaveable(initialVerificationRequested) {
        mutableStateOf(initialVerificationRequested)
    }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(name, email, password, verificationRequested) {
        onPendingSignupChanged(name, email, password, verificationRequested)
    }

    LaunchedEffect(initialVerificationRequested) {
        if (initialVerificationRequested && initialEmail.isNotBlank()) {
            infoMessage = "Verification pending. Enter the code below or resend."
        }
    }

    fun requestLeaveSignup() {
        if (verificationRequested) {
            showLeaveDialog = true
        } else {
            onNavigateBackToLogin()
        }
    }

    BackHandler(enabled = verificationRequested) {
        showLeaveDialog = true
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Verification pending") },
            text = {
                Text(
                    "You have not verified your email yet. You can return to finish signup, " +
                        "or leave and come back later to enter the code.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Stay")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        onNavigateBackToLogin()
                    },
                ) {
                    Text("Leave")
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        TextButton(
            onClick = { requestLeaveSignup() },
            modifier = Modifier.align(Alignment.Start),
        ) {
            Text("Back to login")
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Create account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enter your details to sign up",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Full name") },
            singleLine = true,
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
                codeError = null
                infoMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
                infoMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                confirmError = null
                infoMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                    )
                }
            },
            isError = confirmError != null,
            supportingText = confirmError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                var valid = true
                if (name.isBlank()) {
                    nameError = "Enter your name"
                    valid = false
                }
                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    emailError = "Enter a valid email"
                    valid = false
                }
                if (password.length < 6) {
                    passwordError = "At least 6 characters"
                    valid = false
                }
                if (password != confirmPassword) {
                    confirmError = "Passwords do not match"
                    valid = false
                }
                if (!valid) return@Button

                val wasPending = verificationRequested
                val (result, debugCode) = onStartSignupVerification(name.trim(), email.trim(), password)
                when (result) {
                    SignupStartResult.CODE_SENT -> {
                        verificationRequested = true
                        infoMessage = when {
                            debugCode != null -> "Verification email sent. Use code: $debugCode"
                            wasPending -> "Verification email sent again. Enter the code below."
                            else -> "Verification email sent. Enter the code to complete signup."
                        }
                    }
                    SignupStartResult.EMAIL_ALREADY_REGISTERED -> {
                        emailError = "An account with this email already exists. Sign in instead."
                    }
                    SignupStartResult.FAILED -> {
                        infoMessage = "Could not start verification. Try again."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (verificationRequested) "Resend verification code" else "Send verification code")
        }

        if (verificationRequested) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    codeError = null
                    infoMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Verification code") },
                singleLine = true,
                isError = codeError != null,
                supportingText = codeError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (code.trim().length < 4) {
                        codeError = "Enter the verification code"
                        return@Button
                    }
                    when (onVerifySignupCode(email.trim(), code.trim())) {
                        SignupVerifyResult.SUCCESS -> onSignupSuccess(email.trim())
                        SignupVerifyResult.INVALID_CODE -> codeError = "Invalid verification code"
                        SignupVerifyResult.EXPIRED -> codeError = "Code expired. Request a new code."
                        SignupVerifyResult.FAILED -> codeError = "Could not verify code. Try again."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Verify email and create account")
            }
        }

        Spacer(Modifier.height(16.dp))

        infoMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { requestLeaveSignup() }) {
                Text("Log in")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignupScreenPreview() {
    PotholeReportTheme {
        SignupScreen(
            onNavigateBackToLogin = {},
            onStartSignupVerification = { _, _, _ -> SignupStartResult.CODE_SENT to "123456" },
            onVerifySignupCode = { _, _ -> SignupVerifyResult.SUCCESS },
            onSignupSuccess = { _ -> },
        )
    }
}
