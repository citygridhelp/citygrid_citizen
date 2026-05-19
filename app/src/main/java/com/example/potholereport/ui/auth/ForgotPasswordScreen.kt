package com.example.potholereport.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.potholereport.data.PasswordResetCodeResult
import com.example.potholereport.data.PasswordResetResult
import com.example.potholereport.ui.theme.PotholeReportTheme

@Composable
fun ForgotPasswordScreen(
    onNavigateBackToLogin: () -> Unit,
    onRequestResetCode: (String) -> Pair<PasswordResetCodeResult, String?>,
    onResetPassword: (String, String, String) -> PasswordResetResult,
    onResetSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var code by rememberSaveable { mutableStateOf("") }
    var codeError by rememberSaveable { mutableStateOf<String?>(null) }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmError by rememberSaveable { mutableStateOf<String?>(null) }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var infoMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        TextButton(
            onClick = onNavigateBackToLogin,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Text("Back to login")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Forgot password",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enter your email to receive a reset code and set a new password.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
                infoMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    emailError = "Enter a valid email"
                } else {
                    val (status, debugCode) = onRequestResetCode(email.trim())
                    when (status) {
                        PasswordResetCodeResult.CODE_SENT -> {
                            codeSent = true
                            infoMessage = if (debugCode != null) {
                                "Reset code sent. Use code: $debugCode"
                            } else {
                                "Reset code sent. Check your email."
                            }
                        }
                        PasswordResetCodeResult.ACCOUNT_NOT_FOUND ->
                            emailError = "No account found for this email"
                        PasswordResetCodeResult.EMAIL_NOT_VERIFIED ->
                            emailError = "Email is not verified yet"
                        PasswordResetCodeResult.FAILED ->
                            infoMessage = "Could not send reset code. Try again."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (codeSent) "Resend reset code" else "Send reset code")
        }

        if (codeSent) {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    codeError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reset code") },
                singleLine = true,
                isError = codeError != null,
                supportingText = codeError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    passwordError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New password") },
                singleLine = true,
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm new password") },
                singleLine = true,
                isError = confirmError != null,
                supportingText = confirmError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    var valid = true
                    if (code.trim().isBlank()) {
                        codeError = "Enter reset code"
                        valid = false
                    }
                    if (newPassword.length < 6) {
                        passwordError = "Password must be at least 6 characters"
                        valid = false
                    }
                    if (newPassword != confirmPassword) {
                        confirmError = "Passwords do not match"
                        valid = false
                    }
                    if (!valid) return@Button

                    when (onResetPassword(email.trim(), code.trim(), newPassword)) {
                        PasswordResetResult.SUCCESS -> onResetSuccess()
                        PasswordResetResult.INVALID_CODE -> codeError = "Invalid reset code"
                        PasswordResetResult.EXPIRED -> codeError = "Code expired. Request a new one."
                        PasswordResetResult.ACCOUNT_NOT_FOUND -> emailError = "No account found for this email"
                        PasswordResetResult.FAILED -> codeError = "Could not reset password. Try again."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset password")
            }
        }

        infoMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    PotholeReportTheme {
        ForgotPasswordScreen(
            onNavigateBackToLogin = {},
            onRequestResetCode = { PasswordResetCodeResult.CODE_SENT to "123456" },
            onResetPassword = { _, _, _ -> PasswordResetResult.SUCCESS },
            onResetSuccess = {},
        )
    }
}
