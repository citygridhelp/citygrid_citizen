package com.example.potholereport

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
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
import com.example.potholereport.data.BengaluruGbaBoundary
import com.example.potholereport.data.BengaluruGbaWards
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
        // If the process already finished cold splash, never hold the system splash again —
        // otherwise Activity recreate on return-from-background shows a stuck white screen.
        keepSystemSplash = !AppAutoRefresh.initialSplashCompleted
        setTheme(R.style.Theme_PotholeReport_Launch)
        window.setBackgroundDrawableResource(R.drawable.splash_window_background)
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSystemSplash }
        splashScreen.setOnExitAnimationListener { provider ->
            provider.remove()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestUsbCameraPermission(intent)
        RecentReportsRepository.init(applicationContext)
        UserProfileRepository.init(applicationContext)
        CitizenNotificationsRepository.init(applicationContext)
        BengaluruGbaBoundary.init(applicationContext)
        BengaluruGbaWards.init(applicationContext)
        com.example.potholereport.ui.home.CitizenMapTiler.ensureInitialized(applicationContext)
        com.example.potholereport.data.WeatherAlertWorker.schedule(applicationContext)
        setContent {
            var splashVisible by remember {
                mutableStateOf(!AppAutoRefresh.initialSplashCompleted)
            }
            LaunchedEffect(Unit) {
                if (!splashVisible) {
                    applyPostSplashWindowChrome()
                    return@LaunchedEffect
                }
                val startedAt = System.currentTimeMillis()
                if (SupabaseClientProvider.isConfigured) {
                    withContext(Dispatchers.IO) {
                        SupabaseAuthRepository.restoreSignedInEmail()
                        AppAutoRefresh.refreshLocalData()
                        AppAutoRefresh.checkPlayStoreUpdate(this@MainActivity)
                        AppAutoRefresh.refreshWeatherAlerts(this@MainActivity)
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        AppAutoRefresh.refreshLocalData()
                        AppAutoRefresh.checkPlayStoreUpdate(this@MainActivity)
                        AppAutoRefresh.refreshWeatherAlerts(this@MainActivity)
                    }
                }
                val elapsed = System.currentTimeMillis() - startedAt
                val remaining = AppAutoRefresh.SPLASH_MIN_MS - elapsed
                if (remaining > 0L) delay(remaining)
                AppAutoRefresh.initialSplashCompleted = true
                splashVisible = false
                applyPostSplashWindowChrome()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeRequestUsbCameraPermission(intent)
    }

    private fun maybeRequestUsbCameraPermission(intent: Intent?) {
        if (intent?.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        } ?: return
        com.example.potholereport.data.TripUsbCameraInventory.let { inventory ->
            if (inventory.shouldRequestWebcamPermission(device)) {
                inventory.requestPermission(this, device)
            }
        }
    }

    private fun applyPostSplashWindowChrome() {
        keepSystemSplash = false
        setTheme(R.style.Theme_PotholeReport)
        window.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.color.new_app_icon_launcher_background,
            ),
        )
    }
}
