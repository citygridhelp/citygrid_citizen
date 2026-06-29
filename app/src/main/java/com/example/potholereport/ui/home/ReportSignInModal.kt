package com.example.potholereport.ui.home

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.potholereport.data.LoginResult
import com.example.potholereport.data.SignupStartResult
import com.example.potholereport.data.SignupVerifyResult
import com.example.potholereport.ui.theme.citizenLightSurfaceFieldColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private val ModalDark = Color(0xFF1A1A2E)
private val ModalFieldBorder = Color(0xFF374151)

private fun isStrongCitizenPassword(password: String): Boolean =
    password.length >= 6 &&
        password.any { it.isUpperCase() } &&
        password.any { it.isLowerCase() } &&
        password.any { it.isDigit() } &&
        password.any { !it.isLetterOrDigit() }

@Composable
fun ReportSignInModal(
    onDismiss: () -> Unit,
    onSignIn: suspend (String, String) -> LoginResult,
    onAuthSuccess: (String) -> Unit,
    onStartSignup: suspend (String, String, String) -> SignupStartResult,
    onVerifyCode: suspend (String, String) -> SignupVerifyResult,
) {
    var modalTab by rememberSaveable { mutableIntStateOf(0) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmError by rememberSaveable { mutableStateOf<String?>(null) }
    var codeError by rememberSaveable { mutableStateOf<String?>(null) }
    var infoMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var verificationRequested by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val modalFieldColors = citizenLightSurfaceFieldColors(
        textColor = ModalDark,
        borderColor = ModalFieldBorder,
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            color = Color.White,
            border = BorderStroke(1.dp, ModalDark),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ModalDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SIGN IN",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    ModalAuthTab(
                        title = "SIGN IN",
                        selected = modalTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            modalTab = 0
                            nameError = null
                            emailError = null
                            passwordError = null
                            confirmError = null
                            codeError = null
                            infoMessage = null
                        },
                    )
                    ModalAuthTab(
                        title = "SIGN UP",
                        selected = modalTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            modalTab = 1
                            nameError = null
                            emailError = null
                            passwordError = null
                            confirmError = null
                            codeError = null
                            infoMessage = null
                        },
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (modalTab == 0) {
                        Text(
                            text = "Sign in to see your own report history and resolution rate. Your reports remain anonymous to other users.",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                        Spacer(Modifier.height(14.dp))

                        Text("EMAIL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("you@example.com", color = Color(0xFF9CA3AF)) },
                            singleLine = true,
                            enabled = !submitting,
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it, color = Color(0xFFB91C1C)) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(0.dp),
                            colors = modalFieldColors,
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !submitting,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF6B7280),
                                    )
                                }
                            },
                            isError = passwordError != null,
                            supportingText = passwordError?.let { { Text(it, color = Color(0xFFB91C1C)) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(0.dp),
                            colors = modalFieldColors,
                        )

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                var ok = true
                                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                    emailError = "Enter a valid email"
                                    ok = false
                                }
                                if (password.length < 6) {
                                    passwordError = "Password must be at least 6 characters"
                                    ok = false
                                }
                                if (ok) {
                                    submitting = true
                                    scope.launch {
                                        val result = onSignIn(email.trim(), password)
                                        submitting = false
                                        when (result) {
                                            LoginResult.SUCCESS -> {
                                                val signedInEmail = email.trim()
                                                onDismiss()
                                                scope.launch {
                                                    yield()
                                                    onAuthSuccess(signedInEmail)
                                                }
                                            }
                                            LoginResult.NO_ACCOUNT ->
                                                emailError = "No account found. Please sign up first."
                                            LoginResult.WRONG_PASSWORD ->
                                                passwordError = "Incorrect password. Please try again."
                                            LoginResult.EMAIL_NOT_VERIFIED ->
                                                emailError = "Email not verified. Finish signup first."
                                        }
                                    }
                                }
                            },
                            enabled = !submitting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ModalDark, contentColor = Color.White),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (submitting) "SIGNING IN..." else "SIGN IN",
                                fontWeight = FontWeight.Black,
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Sign-in is verified against your registered account.",
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF9CA3AF),
                            fontSize = 10.sp,
                        )
                    } else {
                        val fieldsLocked = submitting || verificationRequested
                        Text(
                            text = "Create your account and verify your email with the code we send you. Your name is stored securely and is never shown to other users.",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                        Spacer(Modifier.height(14.dp))

                        Text("FULL NAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                nameError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Your full name", color = Color(0xFF9CA3AF)) },
                            singleLine = true,
                            enabled = !fieldsLocked,
                            isError = nameError != null,
                            supportingText = nameError?.let { { Text(it, color = Color(0xFFB91C1C)) } },
                            shape = RoundedCornerShape(0.dp),
                            colors = modalFieldColors,
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("EMAIL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("you@example.com", color = Color(0xFF9CA3AF)) },
                            singleLine = true,
                            enabled = !fieldsLocked,
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it, color = Color(0xFFB91C1C)) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(0.dp),
                            colors = modalFieldColors,
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !fieldsLocked,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF6B7280),
                                    )
                                }
                            },
                            isError = passwordError != null,
                            supportingText = {
                                Text(
                                    text = passwordError
                                        ?: "Use 6+ chars with uppercase, lowercase, a number and a symbol.",
                                    color = if (passwordError != null) Color(0xFFB91C1C) else Color(0xFF6B7280),
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(0.dp),
                            colors = modalFieldColors,
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("CONFIRM PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !fieldsLocked,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF6B7280),
                                    )
                                }
                            },
                            isError = confirmError != null,
                            supportingText = confirmError?.let { { Text(it, color = Color(0xFFB91C1C)) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(0.dp),
                            colors = modalFieldColors,
                        )

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                var ok = true
                                if (fullName.isBlank()) {
                                    nameError = "Enter your full name"
                                    ok = false
                                }
                                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                    emailError = "Enter a valid email"
                                    ok = false
                                }
                                if (!isStrongCitizenPassword(password)) {
                                    passwordError = "Use 6+ chars: uppercase, lowercase, number & symbol"
                                    ok = false
                                }
                                if (password != confirmPassword) {
                                    confirmError = "Passwords do not match"
                                    ok = false
                                }
                                if (ok) {
                                    submitting = true
                                    infoMessage = "Sending verification code..."
                                    scope.launch {
                                        val result = onStartSignup(fullName.trim(), email.trim(), password)
                                        submitting = false
                                        when (result) {
                                            SignupStartResult.CODE_SENT -> {
                                                verificationRequested = true
                                                infoMessage = "We emailed a verification code. Enter it below to finish."
                                            }
                                            SignupStartResult.EMAIL_ALREADY_REGISTERED -> {
                                                infoMessage = null
                                                emailError = "Email already registered. Please sign in."
                                            }
                                            SignupStartResult.FAILED ->
                                                infoMessage = "Could not send the code. Please try again."
                                        }
                                    }
                                }
                            },
                            enabled = !submitting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ModalDark, contentColor = Color.White),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    submitting && !verificationRequested -> "SENDING..."
                                    verificationRequested -> "RESEND VERIFICATION CODE"
                                    else -> "SEND VERIFICATION CODE"
                                },
                                fontWeight = FontWeight.Black,
                            )
                        }

                        if (verificationRequested) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "VERIFICATION CODE FROM EMAIL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B7280),
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = code,
                                onValueChange = {
                                    code = it
                                    codeError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("123456", color = Color(0xFF9CA3AF)) },
                                singleLine = true,
                                enabled = !submitting,
                                isError = codeError != null,
                                supportingText = codeError?.let { { Text(it, color = Color(0xFFB91C1C)) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(0.dp),
                                colors = modalFieldColors,
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (code.trim().length < 4) {
                                        codeError = "Enter the verification code"
                                    } else {
                                        submitting = true
                                        scope.launch {
                                            val result = onVerifyCode(email.trim(), code.trim())
                                            submitting = false
                                            when (result) {
                                                SignupVerifyResult.SUCCESS -> {
                                                    val signedInEmail = email.trim()
                                                    onDismiss()
                                                    scope.launch {
                                                        yield()
                                                        onAuthSuccess(signedInEmail)
                                                    }
                                                }
                                                SignupVerifyResult.INVALID_CODE ->
                                                    codeError = "Invalid verification code"
                                                SignupVerifyResult.EXPIRED ->
                                                    codeError = "Code expired. Resend a new code."
                                                SignupVerifyResult.FAILED ->
                                                    codeError = "Could not verify. Please try again."
                                            }
                                        }
                                    }
                                },
                                enabled = !submitting,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ModalDark, contentColor = Color.White),
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (submitting) "VERIFYING..." else "VERIFY & SIGN IN",
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }

                        infoMessage?.let {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = it,
                                modifier = Modifier.fillMaxWidth(),
                                color = ModalDark,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalAuthTab(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = if (selected) ModalDark else Color.Gray,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) ModalDark else Color.Transparent),
        )
    }
}
