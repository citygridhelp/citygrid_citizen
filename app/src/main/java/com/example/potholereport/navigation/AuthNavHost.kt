package com.example.potholereport.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.potholereport.data.AppAutoRefresh
import com.example.potholereport.data.AuthTransitionGuard
import com.example.potholereport.data.DeviceReporterId
import com.example.potholereport.data.EmailChangeStartResult
import com.example.potholereport.data.EmailChangeVerifyResult
import com.example.potholereport.data.LoginResult
import com.example.potholereport.data.PasswordResetCodeResult
import com.example.potholereport.data.ProfileAvatarIds
import com.example.potholereport.data.RecentReportsRepository
import com.example.potholereport.data.SignupStartResult
import com.example.potholereport.data.SignupVerifyResult
import com.example.potholereport.data.UserProfileRepository
import com.example.potholereport.data.remote.CitizenProfileRepository
import com.example.potholereport.data.remote.SupabaseAuthRepository
import com.example.potholereport.data.remote.SupabaseClientProvider
import com.example.potholereport.ui.auth.ForgotPasswordScreen
import com.example.potholereport.ui.auth.LoginScreen
import com.example.potholereport.ui.auth.SignupScreen
import com.example.potholereport.ui.emergency.EmergencyScreen
import com.example.potholereport.ui.home.HomeScreen
import com.example.potholereport.ui.report.NewReportScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@Composable
fun AuthNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isSignedIn by rememberSaveable { mutableStateOf(false) }
    var signedInEmail by rememberSaveable { mutableStateOf("") }
    var anonymousUserId by rememberSaveable { mutableStateOf("") }
    var avatarId by rememberSaveable { mutableStateOf(ProfileAvatarIds.NEUTRAL) }
    var recentReportsEpoch by rememberSaveable { mutableIntStateOf(0) }
    var uiRefreshEpoch by rememberSaveable { mutableIntStateOf(0) }
    var lastBackgroundAtMs by rememberSaveable { mutableLongStateOf(0L) }
    var sawBackgroundStop by rememberSaveable { mutableStateOf(false) }
    var blockSessionRestore by rememberSaveable { mutableStateOf(false) }

    fun bumpRecentReportsDisplay() {
        recentReportsEpoch++
    }

    fun bumpUiRefresh() {
        recentReportsEpoch++
        uiRefreshEpoch++
    }

    fun clearLocalSignIn() {
        isSignedIn = false
        signedInEmail = ""
        anonymousUserId = ""
        avatarId = ProfileAvatarIds.NEUTRAL
    }

    fun applySignIn(
        email: String,
        pullRemoteReports: Boolean = true,
        deferBackgroundSync: Boolean = false,
    ) {
        val profile = UserProfileRepository.ensureProfile(email)
        signedInEmail = profile.email
        anonymousUserId = profile.anonymousUserId
        avatarId = profile.avatarId
        isSignedIn = true
        if (!SupabaseClientProvider.isConfigured) return
        scope.launch {
            if (deferBackgroundSync) delay(900)
            val stableId = withContext(Dispatchers.IO) {
                CitizenProfileRepository.resolveReporterUserId(profile.anonymousUserId)
            }
            if (stableId != anonymousUserId &&
                UserProfileRepository.updateAnonymousUserId(profile.email, stableId)
            ) {
                anonymousUserId = stableId
            }
            if (pullRemoteReports) {
                withContext(Dispatchers.IO) {
                    RecentReportsRepository.syncSignedInReportsFromSupabase()
                }
                bumpUiRefresh()
            }
        }
    }

    fun refreshAfterSignIn(cityKey: String?) {
        scope.launch {
            withContext(Dispatchers.IO) {
                AppAutoRefresh.refreshSignedInData(cityKey)
            }
            bumpRecentReportsDisplay()
        }
    }

    fun completeSignIn(email: String, snackbarMessage: String, reportCityKey: String = "") {
        AuthTransitionGuard.begin()
        scope.launch {
            try {
                delay(150)
                applySignIn(email, pullRemoteReports = false, deferBackgroundSync = true)
                snackbarHostState.showSnackbar(snackbarMessage)
                delay(600)
                val city = reportCityKey.trim().ifBlank { null }
                refreshAfterSignIn(city)
            } finally {
                delay(400)
                AuthTransitionGuard.end()
            }
        }
    }

    suspend fun reconcileSessionFromSupabase(): Boolean {
        if (blockSessionRestore) return false
        if (!SupabaseClientProvider.isConfigured) return isSignedIn
        val liveEmail = withContext(Dispatchers.IO) {
            SupabaseAuthRepository.restoreSignedInEmail()
        }
        return withContext(Dispatchers.Main.immediate) {
            when {
                liveEmail != null -> {
                    if (!isSignedIn || !signedInEmail.equals(liveEmail, ignoreCase = true)) {
                        applySignIn(liveEmail, pullRemoteReports = false)
                    }
                    true
                }
                isSignedIn -> {
                    clearLocalSignIn()
                    false
                }
                else -> false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (SupabaseClientProvider.isConfigured) {
            reconcileSessionFromSupabase()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    sawBackgroundStop = true
                    lastBackgroundAtMs = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    if (!sawBackgroundStop || lastBackgroundAtMs <= 0L) return@LifecycleEventObserver
                    sawBackgroundStop = false
                    val idleMs = System.currentTimeMillis() - lastBackgroundAtMs
                    if (idleMs >= AppAutoRefresh.RESUME_IDLE_MS) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                if (isSignedIn) {
                                    AppAutoRefresh.refreshSignedInData()
                                } else {
                                    AppAutoRefresh.refreshLocalData()
                                }
                            }
                            bumpUiRefresh()
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                uiRefreshEpoch = uiRefreshEpoch,
                onRecentReportsMutated = { bumpRecentReportsDisplay() },
                isSignedIn = isSignedIn,
                signedInEmail = signedInEmail,
                anonymousUserId = anonymousUserId,
                avatarId = avatarId,
                onSignOut = {
                    blockSessionRestore = true
                    clearLocalSignIn()
                    UserProfileRepository.clearEmailVerification()
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            SupabaseAuthRepository.signOut()
                        }
                        delay(120)
                        bumpUiRefresh()
                        blockSessionRestore = false
                    }
                },
                onReportModalSignIn = { email, password ->
                    withContext(Dispatchers.IO) {
                        SupabaseAuthRepository.login(email, password)
                    }
                },
                onReportModalAuthSuccess = { email, cityKey ->
                    completeSignIn(email, "Signed in successfully", cityKey)
                },
                onReportModalStartSignup = { name, email, password ->
                    withContext(Dispatchers.IO) {
                        SupabaseAuthRepository.startEmailSignup(name, email, password)
                    }
                },
                onReportModalVerifyCode = { email, code ->
                    withContext(Dispatchers.IO) {
                        SupabaseAuthRepository.verifyEmailSignup(email, code)
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
                onProfileStartEmailChange = { newEmail ->
                    withContext(Dispatchers.IO) {
                        SupabaseAuthRepository.startEmailChange(newEmail)
                    }
                },
                onProfileVerifyEmailChange = { otpEmail, code, targetNewEmail ->
                    withContext(Dispatchers.IO) {
                        SupabaseAuthRepository.verifyEmailChangeOtp(otpEmail, code, targetNewEmail)
                    }
                },
            )
        }
        composable(AuthRoute.Emergency.route) {
            EmergencyScreen(
                onClose = { navController.popBackStack() },
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
                onReportPersisted = { bumpUiRefresh() },
                onSyncMessage = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
            )
        }
        composable(AuthRoute.Login.route) {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(AuthRoute.Signup.route) },
                onNavigateToForgotPassword = { navController.navigate(AuthRoute.ForgotPassword.route) },
                onNavigateBack = { navController.popBackStack() },
                onLogin = { email, password ->
                    val loginResult = runBlocking(Dispatchers.IO) {
                        SupabaseAuthRepository.login(email, password)
                    }
                    when (loginResult) {
                        LoginResult.SUCCESS -> {
                            completeSignIn(email, "Signed in successfully")
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
                    val result = runBlocking(Dispatchers.IO) {
                        SupabaseAuthRepository.startEmailSignup(name, email, password)
                    }
                    result to null
                },
                onVerifySignupCode = { email, code ->
                    runBlocking(Dispatchers.IO) {
                        SupabaseAuthRepository.verifyEmailSignup(email, code)
                    }
                },
                onSignupSuccess = { email ->
                    navController.popBackStack(AuthRoute.Home.route, inclusive = false)
                    completeSignIn(email, "Email verified. You're signed in.")
                },
            )
        }
        composable(AuthRoute.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBackToLogin = { navController.popBackStack() },
                onRequestResetCode = { email ->
                    val result = runBlocking(Dispatchers.IO) {
                        SupabaseAuthRepository.requestPasswordReset(email)
                    }
                    result to null
                },
                onResetPassword = { _, _, _ ->
                    com.example.potholereport.data.PasswordResetResult.FAILED
                },
                onResetSuccess = { navController.popBackStack() },
                supabaseLinkReset = SupabaseClientProvider.isConfigured,
            )
        }
    }
}
