package com.example.potholereport.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.potholereport.data.AuthAccountRepository
import com.example.potholereport.data.DeviceReporterId
import com.example.potholereport.data.LoginResult
import com.example.potholereport.data.PasswordResetCodeResult
import com.example.potholereport.data.PasswordResetResult
import com.example.potholereport.data.ProfileAvatarIds
import com.example.potholereport.data.SignupStartResult
import com.example.potholereport.data.SignupVerifyResult
import com.example.potholereport.data.UserProfileRepository
import androidx.compose.ui.platform.LocalContext
import com.example.potholereport.ui.auth.ForgotPasswordScreen
import com.example.potholereport.ui.auth.LoginScreen
import com.example.potholereport.ui.auth.SignupScreen
import com.example.potholereport.ui.emergency.EmergencyScreen
import com.example.potholereport.ui.home.HomeScreen
import com.example.potholereport.ui.report.NewReportScreen
import kotlinx.coroutines.launch

@Composable
fun AuthNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isSignedIn by rememberSaveable { mutableStateOf(false) }
    var signedInEmail by rememberSaveable { mutableStateOf("") }
    var anonymousUserId by rememberSaveable { mutableStateOf("") }
    var avatarId by rememberSaveable { mutableStateOf(ProfileAvatarIds.NEUTRAL) }
    var recentReportsEpoch by rememberSaveable { mutableIntStateOf(0) }

    fun applySignIn(email: String) {
        val profile = UserProfileRepository.ensureProfile(email)
        signedInEmail = profile.email
        anonymousUserId = profile.anonymousUserId
        avatarId = profile.avatarId
        isSignedIn = true
    }

    NavHost(
        navController = navController,
        startDestination = AuthRoute.Home.route,
        modifier = modifier,
    ) {
        composable(AuthRoute.Home.route) {
            HomeScreen(
                onEmergencyClick = { navController.navigate(AuthRoute.Emergency.route) },
                onOpenNewReport = { city ->
                    navController.navigate(AuthRoute.NewReport.createRoute(city))
                },
                recentReportsEpoch = recentReportsEpoch,
                isSignedIn = isSignedIn,
                signedInEmail = signedInEmail,
                anonymousUserId = anonymousUserId,
                avatarId = avatarId,
                onSignOut = {
                    isSignedIn = false
                    signedInEmail = ""
                    anonymousUserId = ""
                    avatarId = ProfileAvatarIds.NEUTRAL
                    UserProfileRepository.clearEmailVerification()
                },
                onAuthSuccessFromReportModal = { email ->
                    applySignIn(email)
                    scope.launch {
                        snackbarHostState.showSnackbar("Signed in successfully")
                    }
                },
                onProfileSaved = { savedAvatar, verifiedNewEmail ->
                    val emailKey = signedInEmail
                    if (savedAvatar != avatarId) {
                        UserProfileRepository.updateAvatar(emailKey, savedAvatar)
                        avatarId = savedAvatar
                    }
                    if (verifiedNewEmail != null &&
                        UserProfileRepository.updateEmail(emailKey, verifiedNewEmail)
                    ) {
                        signedInEmail = verifiedNewEmail.trim().lowercase()
                    }
                    scope.launch {
                        snackbarHostState.showSnackbar("Profile saved")
                    }
                },
                onRecentReportsMutated = { recentReportsEpoch++ },
            )
        }
        composable(AuthRoute.Emergency.route) {
            EmergencyScreen(
                onClose = { navController.popBackStack() }
            )
        }
        composable(
            route = AuthRoute.NewReport.route,
            arguments = listOf(
                navArgument("city") {
                    type = NavType.StringType
                    defaultValue = "BENGALURU"
                },
            ),
        ) { entry ->
            val city = entry.arguments?.getString("city") ?: "BENGALURU"
            val context = LocalContext.current
            val reporterUserId = if (isSignedIn && anonymousUserId.isNotBlank()) {
                anonymousUserId
            } else {
                DeviceReporterId.getOrCreate(context)
            }
            NewReportScreen(
                reportCityKey = city,
                reporterUserId = reporterUserId,
                submittedWhileSignedIn = isSignedIn,
                onClose = { navController.popBackStack() },
                onReportPersisted = { recentReportsEpoch++ },
            )
        }
        composable(AuthRoute.Login.route) {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(AuthRoute.Signup.route) },
                onNavigateToForgotPassword = { navController.navigate(AuthRoute.ForgotPassword.route) },
                onNavigateBack = { navController.popBackStack() },
                onLogin = { email, password ->
                    val loginResult = AuthAccountRepository.login(email, password)
                    when (loginResult) {
                        LoginResult.SUCCESS -> {
                            applySignIn(email)
                            scope.launch {
                                snackbarHostState.showSnackbar("Signed in successfully")
                            }
                            navController.popBackStack()
                            LoginResult.SUCCESS
                        }
                        else -> loginResult
                    }
                },
            )
        }
        composable(AuthRoute.Signup.route) {
            SignupScreen(
                onNavigateBackToLogin = { navController.popBackStack() },
                onStartSignupVerification = { name, email, password ->
                    AuthAccountRepository.startSignup(name, email, password)
                },
                onVerifySignupCode = { email, code ->
                    AuthAccountRepository.verifySignup(email, code)
                },
                onSignupSuccess = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Email verified. Account created.")
                    }
                    navController.popBackStack(AuthRoute.Login.route, inclusive = false)
                },
            )
        }
        composable(AuthRoute.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBackToLogin = { navController.popBackStack() },
                onRequestResetCode = { email ->
                    AuthAccountRepository.requestPasswordReset(email)
                },
                onResetPassword = { email, code, newPassword ->
                    AuthAccountRepository.resetPassword(email, code, newPassword)
                },
                onResetSuccess = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Password reset successful. Please log in.")
                    }
                    navController.popBackStack()
                },
            )
        }
    }
}
