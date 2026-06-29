package com.example.potholereport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.potholereport.data.AppAutoRefresh
import com.example.potholereport.data.CitizenNotificationsRepository
import com.example.potholereport.data.RecentReportsRepository
import com.example.potholereport.data.UserProfileRepository
import com.example.potholereport.data.remote.SupabaseAuthRepository
import com.example.potholereport.data.remote.SupabaseClientProvider
import com.example.potholereport.navigation.AuthNavHost
import com.example.potholereport.ui.splash.AppSplashOverlay
import com.example.potholereport.ui.theme.PotholeReportTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @Volatile
    private var keepSystemSplash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_PotholeReport_Launch)
        window.setBackgroundDrawableResource(R.drawable.splash_window_background)
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSystemSplash }
        splashScreen.setOnExitAnimationListener { provider ->
            provider.remove()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RecentReportsRepository.init(applicationContext)
        UserProfileRepository.init(applicationContext)
        CitizenNotificationsRepository.init(applicationContext)
        setContent {
            var splashVisible by remember {
                mutableStateOf(!AppAutoRefresh.initialSplashCompleted)
            }
            LaunchedEffect(splashVisible) {
                if (!splashVisible) return@LaunchedEffect
                val startedAt = System.currentTimeMillis()
                if (SupabaseClientProvider.isConfigured) {
                    withContext(Dispatchers.IO) {
                        SupabaseAuthRepository.restoreSignedInEmail()
                        AppAutoRefresh.refreshLocalData()
                    }
                }
                val elapsed = System.currentTimeMillis() - startedAt
                val remaining = AppAutoRefresh.SPLASH_MIN_MS - elapsed
                if (remaining > 0L) delay(remaining)
                AppAutoRefresh.initialSplashCompleted = true
                splashVisible = false
                keepSystemSplash = false
                setTheme(R.style.Theme_PotholeReport)
                window.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@MainActivity,
                        R.color.new_app_icon_launcher_background,
                    ),
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                PotholeReportTheme(dynamicColor = false) {
                    val navController = rememberNavController()
                    val snackbarHostState = remember { SnackbarHostState() }
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = if (splashVisible) {
                            Color.Transparent
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { innerPadding ->
                        AuthNavHost(
                            navController = navController,
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    }
                }
                if (splashVisible) {
                    AppSplashOverlay(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f),
                        onDrawn = { keepSystemSplash = false },
                    )
                }
            }
        }
    }
}
