@file:Suppress("SpellCheckingInspection")

package com.example.potholereport.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.Patterns
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.example.potholereport.BuildConfig
import com.example.potholereport.data.BengaluruGbaBoundary
import com.example.potholereport.data.BengaluruGbaWards
import com.example.potholereport.data.CitizenNotificationsRepository
import com.example.potholereport.data.CityLaunchConfig
import com.example.potholereport.data.CityMetroKeys
import com.example.potholereport.data.CityMetroLocation
import com.example.potholereport.data.EmailChangeStartResult
import com.example.potholereport.data.EmailChangeVerifyResult
import com.example.potholereport.data.LoginResult
import com.example.potholereport.data.CityWeatherForecast
import com.example.potholereport.data.CityWeatherRepository
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.PotholeSeverity
import com.example.potholereport.data.PotholeReportStatus
import com.example.potholereport.data.RainCriticality
import com.example.potholereport.data.RecentReportsRepository
import com.example.potholereport.data.formatRecentReportCaption
import com.example.potholereport.data.isMeaningfulAreaName
import com.example.potholereport.data.resolveAreaLabel
import com.example.potholereport.data.SignupStartResult
import com.example.potholereport.data.SignupVerifyResult
import com.example.potholereport.ui.profile.CartoonAvatar
import com.example.potholereport.ui.profile.ProfileDialog
import com.example.potholereport.ui.theme.PotholeReportTheme
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.util.ArrayList
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
fun HomeScreen(
    onEmergencyClick: () -> Unit,
    onOpenNewReport: (String) -> Unit,
    isSignedIn: Boolean,
    signedInEmail: String,
    anonymousUserId: String,
    avatarId: String,
    onSignOut: () -> Unit,
    onReportModalSignIn: suspend (String, String) -> LoginResult,
    onReportModalAuthSuccess: (String, String) -> Unit,
    onReportModalStartSignup: suspend (String, String, String) -> SignupStartResult,
    onReportModalVerifyCode: suspend (String, String) -> SignupVerifyResult,
    onProfileSaved: (avatarId: String, verifiedNewEmail: String?) -> Unit,
    onProfileStartEmailChange: suspend (String) -> EmailChangeStartResult,
    onProfileVerifyEmailChange: suspend (otpEmail: String, code: String, targetNewEmail: String) -> EmailChangeVerifyResult,
    recentReportsEpoch: Int = 0,
    uiRefreshEpoch: Int = 0,
    /** Bumps when reports are added or removed so map / lists refresh. */
    onRecentReportsMutated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showReportSignInModal by rememberSaveable { mutableStateOf(false) }
    var pendingModalSignupName by rememberSaveable { mutableStateOf("") }
    var pendingModalSignupEmail by rememberSaveable { mutableStateOf("") }
    var pendingModalSignupPassword by rememberSaveable { mutableStateOf("") }
    var pendingModalSignupVerification by rememberSaveable { mutableStateOf(false) }

    fun clearPendingModalSignup() {
        pendingModalSignupName = ""
        pendingModalSignupEmail = ""
        pendingModalSignupPassword = ""
        pendingModalSignupVerification = false
    }

    var selectedCity by rememberSaveable { mutableStateOf(CityLaunchConfig.PRIMARY_CITY) }
    /** Marker / map focus; may be map-picked while browsing. */
    var userLocation by remember { mutableStateOf<Location?>(null) }
    /** Last fix from the device GPS pipeline only â€” never updated by map drag. */
    var lastGpsLocation by remember { mutableStateOf<Location?>(null) }
    /** Metro key from last real GPS fix ([cityForLocation]); used so pan-to-pick stays inside the same city as GPS. */
    var gpsMetroCity by remember { mutableStateOf<String?>(null) }
    /** Bumped when the user taps locate so the map re-applies region even if lat/lng match a previous GPS fix. */
    var mapLocateEpoch by remember { mutableIntStateOf(0) }
    var mapTouchActive by remember { mutableStateOf(false) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showGpsEnableDialog by remember { mutableStateOf(false) }
    var notificationsBadgeEpoch by remember { mutableIntStateOf(0) }
    val unreadNotificationCount = remember(
        isSignedIn,
        notificationsBadgeEpoch,
        uiRefreshEpoch,
        recentReportsEpoch,
    ) {
        if (isSignedIn) CitizenNotificationsRepository.unreadCount() else 0
    }

    LaunchedEffect(uiRefreshEpoch, recentReportsEpoch) {
        notificationsBadgeEpoch++
    }

    suspend fun applyLocationFromDevice() {
        val location = fetchCalibratedLocation(context) ?: return
        val addresses = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault()).getFromLocation(
                    location.latitude,
                    location.longitude,
                    1,
                )
            }.getOrNull()
        }
        lastGpsLocation = Location(location)
        userLocation = Location(location)
        gpsMetroCity = cityForLocation(location)
        if (!addresses.isNullOrEmpty()) {
            val resolved = CityMetroKeys.canonical(
                addresses[0].locality
                    ?: addresses[0].subAdminArea
                    ?: addresses[0].adminArea
                    ?: CityLaunchConfig.PRIMARY_CITY,
            )
            if (CityLaunchConfig.isCityEnabled(resolved)) {
                selectedCity = resolved
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            if (!isDeviceLocationEnabled(context)) {
                showGpsEnableDialog = true
            } else {
                coroutineScope.launch { applyLocationFromDevice() }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!CityLaunchConfig.isCityEnabled(selectedCity)) {
            selectedCity = CityLaunchConfig.PRIMARY_CITY
        }
        if (!hasLocationPermission(context) || !isDeviceLocationEnabled(context)) {
            showGpsEnableDialog = true
        } else {
            applyLocationFromDevice()
        }
    }

    LaunchedEffect(isSignedIn) {
        if (!isSignedIn && selectedTabIndex > 1) selectedTabIndex = 0
    }

    val scrollState = rememberScrollState()
    val myReportsScrollState = rememberScrollState()
    val isMyReportsTab = isSignedIn && selectedTabIndex == 2
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CITY GRID",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSignedIn) {
                    NotificationsHeaderBell(
                        unreadCount = unreadNotificationCount,
                        onClick = { showNotificationsDialog = true },
                    )
                    Spacer(Modifier.width(4.dp))
                }
                AccountHeaderIcon(
                    isSignedIn = isSignedIn,
                    avatarId = avatarId,
                    menuExpanded = accountMenuExpanded,
                    onMenuExpandedChange = { accountMenuExpanded = it },
                    onRequestSignIn = { showReportSignInModal = true },
                    onOpenProfile = {
                        if (isSignedIn) showProfileDialog = true
                    },
                    onOpenNotifications = {
                        if (isSignedIn) showNotificationsDialog = true
                    },
                    onShowAbout = { showAboutDialog = true },
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = onEmergencyClick,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .defaultMinSize(minWidth = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB74233)),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "SOS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        color = Color.White,
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            HomeTabBannerAndTabs(
                isSignedIn = isSignedIn,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
            )

            if (isMyReportsTab) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(myReportsScrollState),
                    ) {
                        Spacer(Modifier.height(12.dp))
                        MyReportsSection(
                            reporterUserId = anonymousUserId,
                            recentReportsEpoch = recentReportsEpoch,
                            onReportsMutated = onRecentReportsMutated,
                        )
                    }
                    MyReportsTabFooter()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState, enabled = !mapTouchActive),
                ) {
                    Spacer(Modifier.height(12.dp))

                    when (selectedTabIndex) {
                        1 -> AccountabilitySection(recentReportsEpoch = recentReportsEpoch)
                        else -> ReportAndTrackSection(
                            isSignedIn = isSignedIn,
                            onReportPotholeGuest = { showReportSignInModal = true },
                            onReportPotholeSignedIn = { onOpenNewReport(selectedCity) },
                            selectedCity = selectedCity,
                            onCitySelected = { selectedCity = it },
                            gpsMetroCity = gpsMetroCity,
                            userLocation = userLocation,
                            recentReportsEpoch = recentReportsEpoch,
                            onReportsMutated = onRecentReportsMutated,
                            onLocateMe = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    coroutineScope.launch {
                                        // Immediate feedback: snap to last known GPS first, then refine with calibrated fix.
                                        lastGpsLocation?.let { last ->
                                            userLocation = Location(last)
                                            val city = cityForLocation(last)
                                            gpsMetroCity = city
                                            if (city != null && city != selectedCity && CityLaunchConfig.isCityEnabled(city)) {
                                                selectedCity = city
                                            }
                                            mapLocateEpoch++
                                        }
                                        val fresh = fetchCalibratedLocation(context)
                                        val gps = when {
                                            fresh != null -> Location(fresh).also { lastGpsLocation = Location(it) }
                                            lastGpsLocation != null -> Location(lastGpsLocation!!)
                                            else -> null
                                        }
                                        if (gps != null) {
                                            userLocation = Location(gps)
                                            gpsMetroCity = cityForLocation(gps)
                                            val gpsCity = gpsMetroCity
                                            if (gpsCity != null && gpsCity != selectedCity && CityLaunchConfig.isCityEnabled(gpsCity)) {
                                                selectedCity = gpsCity
                                            }
                                            mapLocateEpoch++
                                        }
                                    }
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            onMapTouchChanged = { mapTouchActive = it },
                            onMapPanEndedAtCenter = pickMapCenter@{ lat, lng ->
                                if (!CityMetroLocation.coordinatesInMetroCity(selectedCity, lat, lng)) {
                                    return@pickMapCenter
                                }
                                userLocation = Location("map").apply {
                                    latitude = lat
                                    longitude = lng
                                }
                            },
                            mapLocateEpoch = mapLocateEpoch,
                            gpsPinLocation = lastGpsLocation,
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (showReportSignInModal) {
        ReportSignInModal(
            initialFullName = pendingModalSignupName,
            initialEmail = pendingModalSignupEmail,
            initialPassword = pendingModalSignupPassword,
            initialVerificationRequested = pendingModalSignupVerification,
            initialModalTab = if (pendingModalSignupVerification) 1 else 0,
            onPendingSignupChanged = { name, email, password, verificationRequested ->
                pendingModalSignupName = name
                pendingModalSignupEmail = email
                pendingModalSignupPassword = password
                pendingModalSignupVerification = verificationRequested
            },
            onDismiss = { showReportSignInModal = false },
            onSignIn = onReportModalSignIn,
            onAuthSuccess = { email ->
                clearPendingModalSignup()
                onReportModalAuthSuccess(email, selectedCity)
                showReportSignInModal = false
            },
            onStartSignup = onReportModalStartSignup,
            onVerifyCode = onReportModalVerifyCode,
        )
    }

    if (showGpsEnableDialog) {
        AlertDialog(
            onDismissRequest = { showGpsEnableDialog = false },
            title = { Text("Turn on location", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "City Grid needs GPS so we can show your position on the map and tag reports with the right place. Allow location access and turn on GPS in your device settings.",
                    fontSize = 14.sp,
                    color = Color(0xFF5A5A5A),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGpsEnableDialog = false
                        when {
                            !hasLocationPermission(context) -> {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            }
                            !isDeviceLocationEnabled(context) -> openLocationSettings(context)
                            else -> coroutineScope.launch { applyLocationFromDevice() }
                        }
                    },
                ) {
                    Text("Enable", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsEnableDialog = false }) {
                    Text("Not now")
                }
            },
        )
    }

    if (showProfileDialog && isSignedIn) {
        ProfileDialog(
            email = signedInEmail,
            userId = anonymousUserId,
            avatarId = avatarId,
            onDismiss = { showProfileDialog = false },
            onSignOut = {
                showProfileDialog = false
                onSignOut()
            },
            onSave = { savedAvatar, verifiedNewEmail ->
                onProfileSaved(savedAvatar, verifiedNewEmail)
            },
            onStartEmailChange = onProfileStartEmailChange,
            onVerifyEmailChange = onProfileVerifyEmailChange,
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            onDismiss = {
                showNotificationsDialog = false
                notificationsBadgeEpoch++
            },
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About City Grid", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "City Grid helps citizens report potholes and other road hazards with photos and location. " +
                        "When you are signed in, you can track your submissions in My Reports. " +
                        "Your reporter identity stays private on public maps and recent reports.\n\n" +
                        "City Grid is an independent app â€” not affiliated with BBMP or any government body. " +
                        "Reporting and municipal zone officers are available for Bengaluru (BBMP) in this release. " +
                        "More cities will be added when official directories are verified.\n\n" +
                        "BBMP directory: https://bbmp.gov.in/\n\n" +
                        "Version ${BuildConfig.VERSION_NAME}",
                    fontSize = 14.sp,
                    color = Color(0xFF5A5A5A),
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun NotificationsHeaderBell(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    Box {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(16.dp)
                    .background(Color(0xFFB74233), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AccountHeaderIcon(
    isSignedIn: Boolean,
    avatarId: String,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onRequestSignIn: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onShowAbout: () -> Unit,
) {
    Box {
        if (isSignedIn) {
            IconButton(
                onClick = { onMenuExpandedChange(true) },
                modifier = Modifier.size(44.dp),
            ) {
                CartoonAvatar(avatarId = avatarId, size = 40.dp)
            }
        } else {
            OutlinedIconButton(
                onClick = onRequestSignIn,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Sign in",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (isSignedIn) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = { Text("Profile") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onOpenProfile()
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Person, contentDescription = null)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Notifications") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onOpenNotifications()
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Notifications, contentDescription = null)
                    },
                )
                DropdownMenuItem(
                    text = { Text("About Us") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onShowAbout()
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Info, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun TopTab(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}

@Composable
private fun CityWeatherSubline(selectedCity: String) {
    var forecast by remember(selectedCity) { mutableStateOf<CityWeatherForecast?>(null) }
    var loading by remember(selectedCity) { mutableStateOf(selectedCity.isNotBlank()) }

    LaunchedEffect(selectedCity) {
        if (selectedCity.isBlank()) {
            forecast = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        forecast = withContext(Dispatchers.IO) {
            CityWeatherRepository.fetchToday(selectedCity)
        }
        loading = false
    }

    val rainColor = when (forecast?.rainCriticality) {
        RainCriticality.STORM -> Color(0xFFB91C1C)
        RainCriticality.HEAVY -> Color(0xFFC2410C)
        RainCriticality.MEDIUM -> Color(0xFFB45309)
        RainCriticality.LIGHT -> Color(0xFF0369A1)
        RainCriticality.NONE -> Color(0xFF4B5563)
        null -> Color.Gray
    }

    Text(
        text = when {
            selectedCity.isBlank() -> "Weather loads after city is set"
            loading -> "Loading today's weather..."
            forecast != null -> forecast!!.summaryLine
            else -> "Weather forecast unavailable"
        },
        fontSize = 11.sp,
        lineHeight = 13.sp,
        color = if (forecast != null) rainColor else Color.Gray,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ReportAndTrackSection(
    isSignedIn: Boolean,
    onReportPotholeGuest: () -> Unit,
    onReportPotholeSignedIn: () -> Unit,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    gpsMetroCity: String?,
    userLocation: Location?,
    onLocateMe: () -> Unit,
    onMapTouchChanged: (Boolean) -> Unit,
    onMapPanEndedAtCenter: (Double, Double) -> Unit,
    mapLocateEpoch: Int,
    gpsPinLocation: Location?,
    recentReportsEpoch: Int,
    onReportsMutated: () -> Unit = {},
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                if (isSignedIn) onReportPotholeSignedIn()
                else onReportPotholeGuest()
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB74233)),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, Color(0xFF8B2F24)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "REPORT A POTHOLE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Signed in but report anonymously",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    var expanded by remember { mutableStateOf(false) }
    /** null = all severities on the map. */
    var mapSeverityFilterKey by rememberSaveable { mutableStateOf<String?>(null) }
    val mapSeverityFilter = remember(mapSeverityFilterKey) {
        mapSeverityFilterKey?.let { key -> PotholeSeverity.entries.find { it.name == key } }
    }
    var severityMenuExpanded by remember { mutableStateOf(false) }
    var areaHeatEnabled by rememberSaveable { mutableStateOf(false) }
    val heatAvailable = CityMetroKeys.canonical(selectedCity) == "BENGALURU"
    LaunchedEffect(heatAvailable) {
        if (!heatAvailable) areaHeatEnabled = false
    }
    var citySearchQuery by rememberSaveable { mutableStateOf("") }
    var searchedPlaces by remember { mutableStateOf<List<Pair<String, GeoPoint>>>(emptyList()) }
    var mapCameraUserPositioned by rememberSaveable { mutableStateOf(false) }
    var savedMapCenterLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedMapCenterLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedMapZoom by rememberSaveable { mutableStateOf<Double?>(null) }
    var lastConsumedMapLocateEpoch by rememberSaveable { mutableIntStateOf(0) }

    fun resetMapCameraToDefault() {
        mapCameraUserPositioned = false
        savedMapCenterLat = null
        savedMapCenterLon = null
        savedMapZoom = null
    }

    fun snapshotMapCamera(lat: Double, lon: Double, zoom: Double) {
        mapCameraUserPositioned = true
        savedMapCenterLat = lat
        savedMapCenterLon = lon
        savedMapZoom = zoom
    }
    LaunchedEffect(expanded, citySearchQuery) {
        if (!expanded) return@LaunchedEffect
        val q = citySearchQuery.trim()
        if (q.length < 2) {
            searchedPlaces = emptyList()
            return@LaunchedEffect
        }
        delay(220)
        searchedPlaces = withContext(Dispatchers.IO) {
            searchIndianPlaces(context, q, limit = 40)
        }
    }
    val filteredPopularPlaces = remember(citySearchQuery) {
        if (citySearchQuery.isBlank()) indiaPopularPlaces
        else indiaPopularPlaces.filter { it.contains(citySearchQuery.trim(), ignoreCase = true) }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val metroMatchesGps = gpsMetroCity != null && selectedCity == gpsMetroCity
        val cityChipBorderColor = when {
            metroMatchesGps -> Color(0xFF000000)
            gpsMetroCity != null -> Color(0xFF111827)
            else -> Color(0xFF0F172A)
        }
        val cityChipBorderWidth = if (metroMatchesGps) 2.5.dp else 2.dp

        Column(modifier = Modifier.weight(1f)) {
            Text("$selectedCity â€¢ POTHOLE DENSITY", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            CityWeatherSubline(selectedCity = selectedCity)
        }

        Box {
            AssistChip(
                onClick = {
                    citySearchQuery = ""
                    searchedPlaces = emptyList()
                    expanded = true
                },
                label = { Text("CHANGE CITY", fontSize = 10.sp) },
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(cityChipBorderWidth, cityChipBorderColor),
                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary)
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(300.dp),
            ) {
                OutlinedTextField(
                    value = citySearchQuery,
                    onValueChange = { citySearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    singleLine = true,
                    placeholder = { Text("Search cities (Bengaluru live now)", fontSize = 10.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                )
                Spacer(Modifier.height(4.dp))
                if (citySearchQuery.isBlank()) {
                    Text(
                        "More cities coming soon",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        fontSize = 9.sp,
                        color = Color(0xFF7B7B7B),
                    )
                }

                val options = if (searchedPlaces.isNotEmpty()) {
                    searchedPlaces.take(60).map { it.first to it.second }
                } else {
                    filteredPopularPlaces.take(120).map { it to null }
                }
                options.forEach { (cityLabel, geoPoint) ->
                    val cityKey = CityMetroKeys.canonical(normalizePlaceKey(cityLabel))
                    val cityEnabled = CityLaunchConfig.isCityEnabled(cityKey)
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    cityLabel,
                                    fontSize = 12.sp,
                                    color = if (cityEnabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                                )
                                if (!cityEnabled) {
                                    Text(
                                        "Coming soon",
                                        fontSize = 9.sp,
                                        color = Color(0xFF9E9E9E),
                                    )
                                }
                            }
                        },
                        enabled = cityEnabled,
                        onClick = {
                            if (geoPoint != null) {
                                registerDynamicCityCenter(cityKey, geoPoint.latitude, geoPoint.longitude)
                            }
                            resetMapCameraToDefault()
                            onCitySelected(cityKey)
                            mapSeverityFilterKey = null
                            expanded = false
                        },
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    val mapSeverityFilterLabel = mapSeverityFilter?.title ?: "All"
    val gpsAccuracyText = remember(gpsPinLocation?.accuracy, gpsPinLocation?.time) {
        val loc = gpsPinLocation
        if (loc != null && loc.hasAccuracy()) {
            "GPS Â±${loc.accuracy.toInt().coerceAtLeast(1)}m"
        } else {
            "GPS searchingâ€¦"
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Severity",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(8.dp))
            Box {
                AssistChip(
                    onClick = { severityMenuExpanded = true },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                mapSeverityFilterLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (mapSeverityFilter != null) {
                                    Color(0xFFB91C1C)
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                },
                            )
                        }
                    },
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(
                        1.dp,
                        if (mapSeverityFilter != null) Color(0xFFB91C1C) else Color(0xFF9CA3AF),
                    ),
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (mapSeverityFilter != null) {
                            Color(0xFFB91C1C)
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                    ),
                )
                DropdownMenu(
                    expanded = severityMenuExpanded,
                    onDismissRequest = { severityMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            mapSeverityFilterKey = null
                            severityMenuExpanded = false
                        },
                    )
                    PotholeSeverity.entries.forEach { severity ->
                        DropdownMenuItem(
                            text = { Text(severity.title) },
                            onClick = {
                                mapSeverityFilterKey = severity.name
                                severityMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
        if (heatAvailable) {
            FilterChip(
                selected = areaHeatEnabled,
                onClick = { areaHeatEnabled = !areaHeatEnabled },
                label = {
                    Text(
                        "Heat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    if (areaHeatEnabled) Color(0xFFB91C1C) else Color(0xFF9CA3AF),
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    iconColor = MaterialTheme.colorScheme.onBackground,
                    selectedContainerColor = Color(0xFFB91C1C),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                ),
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(375.dp) // +20% from 312.dp (prior step from 260.dp)
            .padding(horizontal = 16.dp)
            .background(Color(0xFFFDFCF9))
    ) {
        OsmDensityMap(
            selectedCity = selectedCity,
            userLocation = userLocation,
            onLocateMe = {
                resetMapCameraToDefault()
                onLocateMe()
            },
            onMapViewControlUsed = { },
            onMapTouchChanged = onMapTouchChanged,
            onMapPanEndedAtCenter = onMapPanEndedAtCenter,
            mapLocateEpoch = mapLocateEpoch,
            lastConsumedMapLocateEpoch = lastConsumedMapLocateEpoch,
            onMapLocateEpochConsumed = { lastConsumedMapLocateEpoch = it },
            mapCameraUserPositioned = mapCameraUserPositioned,
            savedMapCenterLat = savedMapCenterLat,
            savedMapCenterLon = savedMapCenterLon,
            savedMapZoom = savedMapZoom,
            onMapCameraSnapshot = ::snapshotMapCamera,
            onResetMapCamera = ::resetMapCameraToDefault,
            gpsPinLocation = gpsPinLocation,
            gpsAccuracyText = gpsAccuracyText,
            mapSeverityFilter = mapSeverityFilter,
            areaHeatEnabled = areaHeatEnabled,
            recentReportsEpoch = recentReportsEpoch,
            modifier = Modifier.fillMaxSize()
        )
    }

    RecentReportsStrip(
        selectedCity = selectedCity,
        recentReportsEpoch = recentReportsEpoch,
        onReportsMutated = onReportsMutated,
    )
}

@Composable
private fun RecentReportsStrip(
    selectedCity: String,
    recentReportsEpoch: Int,
    onReportsMutated: () -> Unit = {},
) {
    var detailReport by remember { mutableStateOf<PersistedPotholeReport?>(null) }
    val bbox = metroBboxForCity(selectedCity)
    LaunchedEffect(selectedCity, recentReportsEpoch) {
        val changed = withContext(Dispatchers.IO) {
            RecentReportsRepository.syncPublicCityReportsFromSupabase(selectedCity)
        }
        if (changed) onReportsMutated()
    }
    val reports = remember(selectedCity, recentReportsEpoch) {
        RecentReportsRepository.recentForCityInMetro(selectedCity, bbox, 5)
    }
    detailReport?.let { report ->
        ReportDetailDialog(report = report, onDismiss = { detailReport = null })
    }
    Spacer(Modifier.height(20.dp))
    Text(
        "RECENT REPORTS - ${formatCityDisplayName(selectedCity)}",
        modifier = Modifier.padding(horizontal = 16.dp),
        fontWeight = FontWeight.Black,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 2,
    )
    Spacer(Modifier.height(10.dp))
    if (reports.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "No reports in this city yet. Submit one with the button above.",
                    fontSize = 12.sp,
                    color = Color(0xFF5A5A5A),
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(reports, key = { it.id }) {
                RecentReportThumbnail(
                    it,
                    width = 148.dp,
                    height = 132.dp,
                    onClick = { detailReport = it },
                )
            }
        }
    }
}

private fun formatCityDisplayName(cityKey: String): String =
    cityKey.trim()
        .split(' ', '_')
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }

@Composable
private fun RecentReportThumbnail(
    report: PersistedPotholeReport,
    width: Dp = 118.dp,
    height: Dp = 108.dp,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var bitmap by remember(report.id, report.photoPath, report.storageClosePath) {
        mutableStateOf<Bitmap?>(null)
    }
    var areaLabel by remember(report.id) {
        mutableStateOf(report.areaLabel.takeIf { isMeaningfulAreaName(it) }.orEmpty())
    }
    LaunchedEffect(report.id, report.photoPath, report.storageClosePath, report.storageWidePath) {
        bitmap = com.example.potholereport.data.remote.ReportPhotoCache.loadCloseBitmap(context, report)
    }
    LaunchedEffect(report.id, report.areaLabel, report.latitude, report.longitude) {
        if (isMeaningfulAreaName(areaLabel)) return@LaunchedEffect
        if (!report.hasCoordinates()) return@LaunchedEffect
        val resolved = withContext(Dispatchers.IO) {
            resolveAreaLabel(context, report.latitude, report.longitude)
        }
        if (isMeaningfulAreaName(resolved)) areaLabel = resolved
    }
    val caption = remember(report.createdAtMs, areaLabel) {
        formatRecentReportCaption(report.createdAtMs, areaLabel)
    }
    Card(
        modifier = Modifier
            .width(width)
            .height(height)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
    ) {
        Box(Modifier.fillMaxSize()) {
            when (val b = bitmap) {
                null -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE8E8E8)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(if (width > 130.dp) 38.dp else 32.dp),
                    )
                }
                else -> Image(
                    bitmap = b.asImageBitmap(),
                    contentDescription = "Pothole report photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                caption,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 11.sp,
            )
        }
    }
}

private val cityCenter = mapOf(
    "BENGALURU" to GeoPoint(12.9716, 77.5946),
    "MUMBAI" to GeoPoint(19.0760, 72.8777),
    "DELHI" to GeoPoint(28.6139, 77.2090),
    "CHENNAI" to GeoPoint(13.0827, 80.2707),
    "HYDERABAD" to GeoPoint(17.3850, 78.4867),
    "KOLKATA" to GeoPoint(22.5726, 88.3639),
    "PUNE" to GeoPoint(18.5204, 73.8567),
    "AHMEDABAD" to GeoPoint(23.0225, 72.5714),
    "JAIPUR" to GeoPoint(26.9124, 75.7873),
    "LUCKNOW" to GeoPoint(26.8467, 80.9462),
)
private val dynamicCityCenters: MutableMap<String, GeoPoint> = mutableMapOf()

private fun registerDynamicCityCenter(cityKey: String, lat: Double, lon: Double) {
    if (lat.isNaN() || lon.isNaN()) return
    dynamicCityCenters[cityKey] = GeoPoint(lat, lon)
}

private fun cityCenterForKey(cityKey: String): GeoPoint =
    dynamicCityCenters[cityKey] ?: cityCenter[cityKey] ?: cityCenter.getValue("BENGALURU")

private fun normalizePlaceKey(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ").uppercase(Locale.US)

private fun resolvePlaceLabelFromAddress(address: android.location.Address): String {
    val preferred = listOf(
        address.locality,
        address.subAdminArea,
        address.adminArea,
        address.subLocality,
        address.featureName,
    ).firstOrNull { !it.isNullOrBlank() } ?: "UNKNOWN"
    return preferred.trim()
}

private fun searchIndianPlaces(context: Context, query: String, limit: Int = 40): List<Pair<String, GeoPoint>> {
    if (query.trim().length < 2) return emptyList()
    return runCatching {
        val geocoder = Geocoder(context, Locale.Builder().setLanguage("en").setRegion("IN").build())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocationName("${query.trim()}, India", limit) ?: emptyList()
        val out = linkedMapOf<String, GeoPoint>()
        for (address in addresses) {
            val label = normalizePlaceKey(resolvePlaceLabelFromAddress(address))
            val lat = address.latitude
            val lon = address.longitude
            if (label.length < 2) continue
            out.putIfAbsent(label, GeoPoint(lat, lon))
        }
        out.entries.map { it.key to it.value }
    }.getOrDefault(emptyList())
}

private val indiaPopularPlaces = listOf(
    "BENGALURU", "MUMBAI", "DELHI", "CHENNAI", "HYDERABAD", "KOLKATA", "PUNE", "AHMEDABAD", "JAIPUR",
    "LUCKNOW", "SURAT", "KANPUR", "NAGPUR", "INDORE", "BHOPAL", "PATNA", "VISAKHAPATNAM", "VADODARA",
    "LUDHIANA", "AGRA", "NASHIK", "FARIDABAD", "MEERUT", "RAJKOT", "VARANASI", "SRINAGAR", "AURANGABAD",
    "DHANBAD", "AMRITSAR", "ALLAHABAD", "RANCHI", "HOWRAH", "COIMBATORE", "JABALPUR", "GWALIOR", "VIJAYAWADA",
    "JODHPUR", "MADURAI", "RAIPUR", "KOTA", "GUWAHATI", "CHANDIGARH", "THIRUVANANTHAPURAM", "MYSURU", "MANGALURU",
    "HUBBALLI", "BELAGAVI", "SHIVAMOGGA", "KALABURAGI", "UJJAIN", "DEHRADUN", "NOIDA", "GURUGRAM", "PUDUCHERRY",
    "TIRUCHIRAPPALLI", "SALEM", "ERODE", "TIRUPPUR", "KANNUR", "KOCHI", "KOTTAYAM", "KOZHIKODE", "ALAPPUZHA",
    "BHUBANESWAR", "CUTTACK", "SILIGURI", "ASANSOL", "JAMNAGAR", "JUNAGADH", "BHAVNAGAR", "ANAND", "VAPI",
    "UDAIPUR", "AJMER", "BIKANER", "SIKAR", "JHANSI", "ALIGARH", "MORADABAD", "GHAZIABAD", "BAREILLY", "GORAKHPUR",
    "AMRAVATI", "SOLAPUR", "KOLHAPUR", "NANDED", "AKOLA", "SANGALI", "WARANGAL", "NELLORE", "GUNTUR", "KAKINADA",
)

/** Metro bounding boxes (north, east, south, west) â€” pan is clamped inside; min zoom keeps view city-sized. */
private val cityMetroBounds: Map<String, BoundingBox> = mapOf(
    // Official GBA outer boundary bbox (Sept 2025); red outline uses full polygon asset.
    "BENGALURU" to BoundingBox(13.14266, 77.784361, 12.833625, 77.460051),
    "MUMBAI" to BoundingBox(19.28, 73.05, 18.86, 72.76),
    "DELHI" to BoundingBox(28.88, 77.45, 28.38, 76.84),
    "CHENNAI" to BoundingBox(13.24, 80.33, 12.90, 80.08),
    "HYDERABAD" to BoundingBox(17.58, 78.65, 17.22, 78.22),
    "KOLKATA" to BoundingBox(22.65, 88.55, 22.40, 88.22),
    "PUNE" to BoundingBox(18.64, 74.00, 18.40, 73.70),
    "AHMEDABAD" to BoundingBox(23.17, 72.72, 22.94, 72.44),
    "JAIPUR" to BoundingBox(27.00, 75.92, 26.74, 75.64),
    "LUCKNOW" to BoundingBox(27.00, 81.05, 26.74, 80.86),
)

/** Large outer ring (South Asia) fallback when a city has no metro bbox. */
private fun southAsiaDimOuterRing(): List<GeoPoint> = listOf(
    GeoPoint(39.0, 57.0),
    GeoPoint(39.0, 103.0),
    GeoPoint(2.5, 103.0),
    GeoPoint(2.5, 57.0),
    GeoPoint(39.0, 57.0),
)

/**
 * Outer ring expanded from the active metro bbox (not whole South Asia) so the dim polygon
 * stays numerically well-conditioned when zoomed in, which keeps panning smoother.
 */
private fun dimOuterEnvelopeForCity(city: String): List<GeoPoint> {
    val bbox = metroBboxForCity(city) ?: return southAsiaDimOuterRing()
    val o = bbox.increaseByScale(42f)
    return listOf(
        GeoPoint(o.latNorth, o.lonWest),
        GeoPoint(o.latNorth, o.lonEast),
        GeoPoint(o.latSouth, o.lonEast),
        GeoPoint(o.latSouth, o.lonWest),
        GeoPoint(o.latNorth, o.lonWest),
    )
}

private fun closedGeoRing(pts: List<GeoPoint>): ArrayList<GeoPoint> {
    val out = ArrayList<GeoPoint>(pts.size + 1)
    out.addAll(pts)
    if (out.isNotEmpty() && (out.first().latitude != out.last().latitude ||
            out.first().longitude != out.last().longitude)
    ) {
        out.add(out.first())
    }
    return out
}

private fun metroBboxForCity(city: String): BoundingBox? {
    if (city == "BENGALURU") {
        BengaluruGbaBoundary.boundingBox()?.let { return it }
    }
    return cityMetroBounds[city]
}

private fun bboxOutlineVertices(bbox: BoundingBox): List<GeoPoint> = listOf(
    GeoPoint(bbox.latNorth, bbox.lonWest),
    GeoPoint(bbox.latNorth, bbox.lonEast),
    GeoPoint(bbox.latSouth, bbox.lonEast),
    GeoPoint(bbox.latSouth, bbox.lonWest),
)

private fun visualOutlineVerticesForCity(city: String): List<GeoPoint>? {
    if (city == "BENGALURU" && BengaluruGbaBoundary.isInitialized()) {
        val ring = BengaluruGbaBoundary.outlineRing()
        if (ring.isNotEmpty()) return ring
    }
    val bbox = metroBboxForCity(city) ?: return null
    return bboxOutlineVertices(bbox)
}

/** True when [lat]/[lon] lies inside the red city outline (or metro bbox fallback). */
private fun mapCenterInsideCityOutline(city: String, lat: Double, lon: Double): Boolean {
    val verts = visualOutlineVerticesForCity(city) ?: return false
    return pointInLonLatPolygon(lon, lat, verts)
}

/** Visible map card fully contains the city overview bbox (small inset tolerance). */
private fun visibleMapContainsBbox(visible: BoundingBox, target: BoundingBox, insetFraction: Double = 0.01): Boolean {
    val latInset = (target.latNorth - target.latSouth) * insetFraction
    val lonInset = (target.lonEast - target.lonWest) * insetFraction
    return visible.latNorth + latInset >= target.latNorth &&
        visible.latSouth - latInset <= target.latSouth &&
        visible.lonEast + lonInset >= target.lonEast &&
        visible.lonWest - lonInset <= target.lonWest
}

/** City map view: center must stay in the outline and the city must remain framed on screen. */
private fun MapView.needsCityOverviewReframe(selectedCity: String): Boolean {
    if ((zoomLevelDouble - minZoomLevel) > CITY_VIEW_ZOOM_EPSILON) return false
    val cityBbox = cityOverviewBoundingBox(selectedCity) ?: return false
    val visible = boundingBox ?: return false
    if (!visibleMapContainsBbox(visible, cityBbox)) return true
    val c = mapCenter
    return !mapCenterInsideCityOutline(selectedCity, c.latitude, c.longitude)
}

private fun MapView.isAtCityOverviewZoom(): Boolean =
    (zoomLevelDouble - minZoomLevel) <= CITY_VIEW_ZOOM_EPSILON

private fun MapView.reframeCityOverviewIfNeeded(selectedCity: String, restoreSlot: Array<Runnable?>) {
    if (needsCityOverviewReframe(selectedCity)) {
        scheduleFitCityOverview(selectedCity, restoreSlot)
    }
}

private fun pointInLonLatPolygon(lon: Double, lat: Double, polygon: List<GeoPoint>): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val xi = polygon[i].longitude
        val yi = polygon[i].latitude
        val xj = polygon[j].longitude
        val yj = polygon[j].latitude
        val intersect = (yi > lat) != (yj > lat) &&
            lon < (xj - xi) * (lat - yi) / (yj - yi + 1e-15) + xi
        if (intersect) inside = !inside
        j = i
    }
    return inside
}

/** Geographic center of the red city outline (polygon centroid), else configured city center. */
private fun visualOutlineCentroidForCity(city: String): GeoPoint {
    val verts = visualOutlineVerticesForCity(city)
    if (!verts.isNullOrEmpty()) {
        var area2 = 0.0
        var cx = 0.0
        var cy = 0.0
        val n = verts.size
        for (i in 0 until n) {
            val p0 = verts[i]
            val p1 = verts[(i + 1) % n]
            val cross = p0.longitude * p1.latitude - p1.longitude * p0.latitude
            area2 += cross
            cx += (p0.longitude + p1.longitude) * cross
            cy += (p0.latitude + p1.latitude) * cross
        }
        if (abs(area2) > 1e-14) {
            val inv = 1.0 / (3.0 * area2)
            return GeoPoint(cy * inv, cx * inv)
        }
        return GeoPoint(
            verts.map { it.latitude }.average(),
            verts.map { it.longitude }.average(),
        )
    }
    return cityCenterForKey(city)
}

private fun cityOverviewBoundingBox(selectedCity: String): BoundingBox? {
    val verts = visualOutlineVerticesForCity(selectedCity)
    if (!verts.isNullOrEmpty()) {
        var north = -90.0
        var south = 90.0
        var east = -180.0
        var west = 180.0
        for (p in verts) {
            north = maxOf(north, p.latitude)
            south = minOf(south, p.latitude)
            east = maxOf(east, p.longitude)
            west = minOf(west, p.longitude)
        }
        return BoundingBox(north, east, south, west).increaseByScale(1.015f)
    }
    return metroBboxForCity(selectedCity)
}

private data class ReportGeoCluster(val lat: Double, val lon: Double, val count: Int)

private fun reportClusterMarkerRadiusPx(count: Int, density: Float): Float =
    if (count <= 1) {
        (10f * density).coerceIn(8f, 14f)
    } else {
        (16f + sqrt(count.toFloat()).coerceAtMost(34f)) * density
    }

/** Merge when marker circles would touch or overlap on screen. */
private fun reportClusterMergeDistancePx(countA: Int, countB: Int, density: Float): Float =
    reportClusterMarkerRadiusPx(countA, density) + reportClusterMarkerRadiusPx(countB, density) -
        (0.75f * density)

/**
 * Starts as one marker per report; merges pairs whose on-screen circles overlap at the
 * current zoom so "1" + "1" becomes "2" only when they would visually collide.
 */
private fun buildReportScreenOverlapClusters(
    map: MapView,
    reports: List<PersistedPotholeReport>,
): List<ReportGeoCluster> {
    if (reports.isEmpty()) return emptyList()
    if (map.width <= 0 || map.height <= 0) {
        return reports.map { ReportGeoCluster(it.latitude, it.longitude, 1) }
    }
    val density = map.resources.displayMetrics.density
    val projection = map.projection
    val scr = Point()

    data class MutableNode(
        var lat: Double,
        var lon: Double,
        var count: Int,
        var x: Int,
        var y: Int,
    )

    fun refreshPixel(node: MutableNode) {
        projection.toPixels(GeoPoint(node.lat, node.lon), scr)
        node.x = scr.x
        node.y = scr.y
    }

    val nodes = reports.map { r ->
        projection.toPixels(GeoPoint(r.latitude, r.longitude), scr)
        MutableNode(r.latitude, r.longitude, 1, scr.x, scr.y)
    }.toMutableList()

    var merged = true
    while (merged) {
        merged = false
        var i = 0
        while (i < nodes.size) {
            var j = i + 1
            while (j < nodes.size) {
                val a = nodes[i]
                val b = nodes[j]
                val dx = (a.x - b.x).toDouble()
                val dy = (a.y - b.y).toDouble()
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < reportClusterMergeDistancePx(a.count, b.count, density)) {
                    val n = a.count + b.count
                    a.lat = (a.lat * a.count + b.lat * b.count) / n
                    a.lon = (a.lon * a.count + b.lon * b.count) / n
                    a.count = n
                    refreshPixel(a)
                    nodes.removeAt(j)
                    merged = true
                } else {
                    j++
                }
            }
            i++
        }
    }

    return nodes.map { ReportGeoCluster(lat = it.lat, lon = it.lon, count = it.count) }
}

/** Cluster count disks. Heat on → white fill + black count; heat off → red fill as before. */
private class ReportClusterMarkersOverlay(
    private val mapView: MapView,
    private val clusters: List<ReportGeoCluster>,
    heatMode: Boolean = false,
    accentMode: Boolean = false,
) : Overlay() {

    private val circleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            heatMode -> AndroidColor.argb(235, 255, 255, 255)
            accentMode -> AndroidColor.argb(155, 220, 38, 38)
            else -> AndroidColor.argb(115, 252, 165, 165)
        }
        style = Paint.Style.FILL
    }

    private val circleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            heatMode -> AndroidColor.argb(180, 120, 120, 120)
            accentMode -> AndroidColor.argb(230, 153, 27, 27)
            else -> AndroidColor.argb(200, 200, 70, 70)
        }
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            heatMode -> AndroidColor.parseColor("#111827")
            accentMode -> AndroidColor.parseColor("#FFFFFF")
            else -> AndroidColor.parseColor("#5C1010")
        }
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        if (!isEnabled || clusters.isEmpty()) return
        val d = mapView.resources.displayMetrics.density
        circleStrokePaint.strokeWidth = (1.15f * d).coerceIn(0.9f, 2.4f)
        val scr = Point()
        for (c in clusters) {
            projection.toPixels(GeoPoint(c.lat, c.lon), scr)
            val rPx = reportClusterMarkerRadiusPx(c.count, d)
            canvas.drawCircle(scr.x.toFloat(), scr.y.toFloat(), rPx, circleFillPaint)
            canvas.drawCircle(scr.x.toFloat(), scr.y.toFloat(), rPx, circleStrokePaint)
            textPaint.textSize = (rPx * 0.42f).coerceIn(10f * d, 26f * d)
            val fm = textPaint.fontMetrics
            val textY = scr.y - (fm.ascent + fm.descent) / 2f
            canvas.drawText(c.count.toString(), scr.x.toFloat(), textY, textPaint)
        }
    }
}

private fun reportsForMapSeverityFilter(
    reports: List<PersistedPotholeReport>,
    severityFilter: PotholeSeverity?,
): List<PersistedPotholeReport> =
    if (severityFilter == null) reports else reports.filter { it.severity == severityFilter }

private fun attachReportClusterOverlay(
    map: MapView,
    selectedCity: String,
    decor: CityOutlineDecor,
    mapSeverityFilter: PotholeSeverity?,
    heatEnabled: Boolean = false,
) {
    decor.clusters?.let { map.overlays.remove(it) }
    decor.clusters = null
    val bbox = metroBboxForCity(selectedCity) ?: return
    val reports = reportsForMapSeverityFilter(
        RecentReportsRepository.reportsForMapInMetro(selectedCity, bbox),
        mapSeverityFilter,
    )
    if (reports.isEmpty()) return
    val clusters = buildReportScreenOverlapClusters(map, reports)
    if (clusters.isEmpty()) return
    val overlay = ReportClusterMarkersOverlay(
        mapView = map,
        clusters = clusters,
        heatMode = heatEnabled,
        accentMode = !heatEnabled && mapSeverityFilter == PotholeSeverity.CRITICAL,
    )
    decor.clusters = overlay
    map.overlays.add(overlay)
}

private class CityOutlineDecor(
    var dim: Polygon? = null,
    var edge: Polyline? = null,
    var areaHeat: AreaDensityHeatOverlay? = null,
    var regionLabels: RegionNameLabelsOverlay? = null,
    var cityName: CityNameOverlay? = null,
    var clusters: ReportClusterMarkersOverlay? = null,
)

private fun removeAreaHeatOverlay(map: MapView, decor: CityOutlineDecor) {
    decor.areaHeat?.let { map.overlays.remove(it) }
    decor.areaHeat = null
}

/**
 * Area ward choropleth under clusters (color only). Bengaluru only; no-op for other cities.
 */
private fun refreshAreaHeatOverlay(
    map: MapView,
    selectedCity: String,
    decor: CityOutlineDecor,
    mapSeverityFilter: PotholeSeverity?,
    heatEnabled: Boolean,
) {
    if (!heatEnabled || CityMetroKeys.canonical(selectedCity) != "BENGALURU" ||
        !BengaluruGbaWards.isInitialized()
    ) {
        removeAreaHeatOverlay(map, decor)
        return
    }
    val bbox = metroBboxForCity(selectedCity) ?: run {
        removeAreaHeatOverlay(map, decor)
        return
    }
    val reports = reportsForAreaHeat(
        RecentReportsRepository.reportsForMapInMetro(selectedCity, bbox),
        mapSeverityFilter,
    )
    val counts = countReportsByWardKey(reports) { lat, lon ->
        BengaluruGbaWards.findWard(lat, lon)?.wardKey
    }
    val existing = decor.areaHeat
    if (existing != null) {
        existing.updateDensity(counts)
        return
    }
    val overlay = AreaDensityHeatOverlay(
        mapView = map,
        polygons = BengaluruGbaWards.allPolygons(),
        countsByWardKey = counts,
    )
    decor.areaHeat = overlay
    val dimIdx = decor.dim?.let { map.overlays.indexOf(it) } ?: -1
    if (dimIdx >= 0) {
        map.overlays.add(dimIdx + 1, overlay)
    } else {
        map.overlays.add(0, overlay)
    }
}

private fun refreshReportClusterOverlayOnly(
    map: MapView,
    selectedCity: String,
    decor: CityOutlineDecor,
    mapSeverityFilter: PotholeSeverity?,
    heatEnabled: Boolean = false,
) {
    attachReportClusterOverlay(
        map = map,
        selectedCity = selectedCity,
        decor = decor,
        mapSeverityFilter = mapSeverityFilter,
        heatEnabled = heatEnabled,
    )
    refreshAreaHeatOverlay(
        map = map,
        selectedCity = selectedCity,
        decor = decor,
        mapSeverityFilter = mapSeverityFilter,
        heatEnabled = heatEnabled,
    )
    // Keep clusters above heat after re-attach.
    decor.clusters?.let { clusters ->
        map.overlays.remove(clusters)
        map.overlays.add(clusters)
    }
    map.invalidate()
}

/**
 * City title at the outline centroid; drawn on the map canvas so it pans with the city border.
 */
private class CityNameOverlay(
    private val mapView: MapView,
    private val position: GeoPoint,
    private val displayName: String,
) : Overlay() {

    @Volatile
    var labelVisible: Boolean = false

    @Volatile
    var drawSuppressed: Boolean = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#3D3D3D")
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.04f
    }

    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        if (!isEnabled || drawSuppressed || !labelVisible) return
        val z = mapView.zoomLevelDouble
        if ((z - mapView.minZoomLevel) > CITY_VIEW_ZOOM_EPSILON) return
        val d = mapView.resources.displayMetrics.density
        paint.textSize = 20f * d
        paint.clearShadowLayer()
        paint.setShadowLayer(1.5f * d, 0f, 0.5f * d, AndroidColor.argb(56, 0, 0, 0))
        val scr = Point()
        pProjection.toPixels(position, scr)
        val fm = paint.fontMetrics
        val y = scr.y - (fm.ascent + fm.descent) / 2f
        pCanvas.drawText(displayName, scr.x.toFloat(), y, paint)
    }
}

/**
 * Curated locality label on the home map (CARTO basemap has no built-in place names).
 *
 * G-lite tiering (v1.0.4): [minZoom] gates appearance; optional [maxZoom] hides macro names when
 * zoomed in; [priority] breaks grid ties (higher wins). Macro labels with minZoom â‰¤ 10.35 get a
 * default maxZoom of 11.9 when [maxZoom] is null.
 *
 * Street-level road names are not in scope â€” see backlog #14 deferred options.
 */
private data class RegionLabelSpec(
    val lat: Double,
    val lon: Double,
    val text: String,
    val minZoom: Double,
    val maxZoom: Double? = null,
    /** Higher wins a grid cell when two labels compete; 0 = auto from [minZoom]. */
    val priority: Int = 0,
) {
    fun effectiveMaxZoom(): Double? =
        maxZoom ?: if (minZoom <= 10.35) 11.9 else null

    fun drawPriority(): Int =
        if (priority != 0) priority
        else if (minZoom <= 10.35) 400 - (minZoom * 10).toInt()
        else (minZoom * 10).toInt()
}

/**
 * Region names: one typeface + text size from map zoom only (not per-label), so labels look uniform.
 * [labelRevealSlack] delays finer labels until zoomed in, keeping city overview readable without extra
 * work per frame (same loop as before; pan smoothness unchanged â€” still gated by [drawSuppressed]).
 */
private class RegionNameLabelsOverlay(
    private val mapView: MapView,
    specs: List<RegionLabelSpec>,
) : Overlay() {

    /** Broader (lower minZoom) names win screen slots when zoomed out. */
    private val specsMacroFirst = specs.sortedWith(
        compareByDescending<RegionLabelSpec> { it.drawPriority() }.thenBy { it.minZoom },
    )

    /** When zoomed in (slack low), prefer finer labels so hyperlocal names are not blocked by macro cells. */
    private val specsMicroFirst = specs.sortedWith(
        compareByDescending<RegionLabelSpec> { it.minZoom }.thenByDescending { it.drawPriority() },
    )

    private val gridOccupied = HashSet<Long>(256)

    @Volatile
    var drawSuppressed: Boolean = false

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#5A5A5A")
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        style = Paint.Style.FILL
        isFakeBoldText = false
        letterSpacing = 0.02f
    }

    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        if (!isEnabled || drawSuppressed) return
        val zoom = mapView.zoomLevelDouble
        val d = mapView.resources.displayMetrics.density
        val bb = mapView.boundingBox ?: return
        val vis = bb.increaseByScale(1.14f)
        labelPaint.clearShadowLayer()
        labelPaint.textSize = regionLabelTextSizePx(zoom, d)
        val fm = labelPaint.fontMetrics
        val yBaselineShift = -(fm.ascent + fm.descent) / 2f
        val slack = labelRevealSlack(zoom)
        val cellPx = labelGridCellPx(zoom, d)
        val scr = Point()
        gridOccupied.clear()
        val ordered = if (slack < 0.36) specsMicroFirst else specsMacroFirst
        for (spec in ordered) {
            if (zoom + 1e-6 < spec.minZoom + slack) continue
            val hideAbove = spec.effectiveMaxZoom()
            if (hideAbove != null && zoom > hideAbove + 1e-6) continue
            if (!vis.contains(spec.lat, spec.lon)) continue
            pProjection.toPixels(GeoPoint(spec.lat, spec.lon), scr)
            val ix = floor(scr.x / cellPx).toInt()
            val iy = floor(scr.y / cellPx).toInt()
            val key = packGridKey(ix, iy)
            if (!gridOccupied.add(key)) continue
            val y = scr.y + yBaselineShift
            pCanvas.drawText(spec.text, scr.x.toFloat(), y, labelPaint)
        }
    }
}

/** Text size depends only on zoom so every visible label matches; cheap (set once per draw pass). */
private fun regionLabelTextSizePx(zoom: Double, density: Float): Float {
    val z = zoom.coerceIn(8.5, 18.5)
    val t = ((z - 9.6) / 6.2).coerceIn(0.0, 1.0)
    return (8.5f + 5.2f * t.toFloat()) * density
}

/**
 * When zoomed out, require extra zoom beyond each spec's [RegionLabelSpec.minZoom] before drawing,
 * so minor areas stay hidden until the user zooms in; slack â†’ ~0 by ~14.5 so finer labels can appear.
 */
private fun labelRevealSlack(zoom: Double): Double {
    val z = zoom.coerceIn(8.8, 20.0)
    // G-lite: slightly lower slack (~14%) so names appear closer to minZoom (Google-like tiers).
    return (1.88 * (1.0 - (z - 8.85) / 5.45)).coerceAtLeast(0.0)
}

/** Screen-space cell size for label de-overlap: large when zoomed out (few names), shrinks as you zoom in. */
private fun labelGridCellPx(zoom: Double, density: Float): Float {
    val z = zoom.coerceIn(9.0, 18.0).toFloat()
    return (132f - 5.1f * (z - 9f)).coerceIn(36f, 128f) * density
}

private fun packGridKey(ix: Int, iy: Int): Long = ix.toLong() shl 32 or (iy.toLong() and 0xffffffffL)

private val bengaluruRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(13.07, 77.80, "Hoskote", 9.0),
    RegionLabelSpec(12.905, 77.485, "Kengeri", 9.0),
    RegionLabelSpec(13.10, 77.42, "Nelamangala", 9.1),
    RegionLabelSpec(12.80, 77.37, "Bidadi", 9.1),
    RegionLabelSpec(12.77, 77.78, "Attibele", 9.1),
    RegionLabelSpec(12.98, 77.75, "Whitefield", 9.55),
    RegionLabelSpec(12.85, 77.66, "Electronic City", 9.55),
    RegionLabelSpec(13.10, 77.60, "Yelahanka", 9.55),
    RegionLabelSpec(13.02, 77.59, "Hebbal", 9.55),
    RegionLabelSpec(13.00, 77.68, "KR Puram", 9.75),
    RegionLabelSpec(12.93, 77.78, "Sarjapur", 9.85),
    RegionLabelSpec(12.9716, 77.5946, "Majestic", 10.1),
    RegionLabelSpec(12.95, 77.70, "Marathahalli", 10.1),
    RegionLabelSpec(13.03, 77.50, "Peenya", 10.1),
    RegionLabelSpec(12.99, 77.55, "Rajajinagar", 10.35),
    RegionLabelSpec(12.935, 77.58, "Jayanagar", 10.35),
    RegionLabelSpec(12.925, 77.545, "Banashankari", 10.35),
    RegionLabelSpec(12.907, 77.59, "JP Nagar", 10.35),
    RegionLabelSpec(12.978, 77.638, "Indiranagar", 10.45),
    RegionLabelSpec(12.98, 77.615, "Ulsoor", 10.55),
    RegionLabelSpec(12.93, 77.66, "Bellandur", 10.85),
    RegionLabelSpec(12.938, 77.745, "Varthur", 10.85),
    RegionLabelSpec(12.935, 77.62, "Koramangala", 10.85),
    RegionLabelSpec(12.912, 77.64, "HSR Layout", 11.05),
    RegionLabelSpec(12.916, 77.605, "BTM Layout", 11.05),
    RegionLabelSpec(12.963, 77.645, "Domlur", 11.25),
    RegionLabelSpec(12.99, 77.695, "Mahadevapura", 11.25),
    RegionLabelSpec(13.035, 77.665, "Ramamurthy Nagar", 11.35),
    RegionLabelSpec(13.03, 77.64, "Hennur", 11.35),
    RegionLabelSpec(13.039, 77.55, "Jalahalli", 11.45),
    RegionLabelSpec(13.006, 77.59, "Malleshwaram", 11.45),
    RegionLabelSpec(12.948, 77.57, "Basavanagudi", 11.55),
    RegionLabelSpec(13.10, 77.625, "Jakkur", 11.15),
    RegionLabelSpec(13.058, 77.645, "Thanisandra", 11.35),
    RegionLabelSpec(12.972, 77.6068, "MG Road", 11.65),
    RegionLabelSpec(12.948, 77.583, "Lalbagh", 11.75),
    RegionLabelSpec(12.886, 77.575, "Bannerghatta", 11.25),
    RegionLabelSpec(13.00, 77.566, "Vijayanagar", 11.25),
    RegionLabelSpec(12.905, 77.52, "Kumaraswamy Layout", 11.85),
    RegionLabelSpec(12.939, 77.627, "Ejipura", 12.05),
    RegionLabelSpec(12.996, 77.669, "Brookefield", 11.95),
    RegionLabelSpec(13.02, 77.52, "Nagasandra", 11.85),
    RegionLabelSpec(12.995, 77.715, "Kadugodi", 12.0),
    RegionLabelSpec(12.978, 77.712, "Hoodi", 12.15),
    RegionLabelSpec(12.962, 77.718, "Graphite India", 12.35),
    RegionLabelSpec(12.938, 77.712, "Panathur", 12.25),
    RegionLabelSpec(12.915, 77.735, "Kaikondrahalli", 12.4),
    RegionLabelSpec(12.905, 77.748, "Carmelaram", 12.35),
    RegionLabelSpec(12.9195, 77.6215, "Silk Board", 12.1),
    RegionLabelSpec(12.968, 77.592, "Cubbon Park", 12.2),
    RegionLabelSpec(12.989, 77.592, "High Grounds", 12.3),
    RegionLabelSpec(13.008, 77.614, "Frazer Town", 12.25),
    RegionLabelSpec(13.015, 77.625, "Cox Town", 12.35),
    RegionLabelSpec(13.022, 77.645, "Kammanahalli", 12.4),
    RegionLabelSpec(13.015, 77.655, "Banaswadi", 12.35),
    RegionLabelSpec(13.045, 77.625, "Nagawara", 12.45),
    RegionLabelSpec(13.028, 77.64, "Kalyan Nagar", 12.45),
    RegionLabelSpec(13.01, 77.54, "Nandini Layout", 12.5),
    RegionLabelSpec(13.015, 77.555, "Mahalakshmi Layout", 12.45),
    RegionLabelSpec(12.99, 77.52, "Laggere", 12.55),
    RegionLabelSpec(12.975, 77.485, "Herohalli", 12.55),
    RegionLabelSpec(12.925, 77.51, "Rajarajeshwari Nagar", 12.35),
    RegionLabelSpec(12.87, 77.58, "Begur", 12.3),
    RegionLabelSpec(12.895, 77.57, "Gottigere", 12.5),
    RegionLabelSpec(12.905, 77.565, "Konanakunte", 12.55),
    RegionLabelSpec(12.965, 77.665, "CV Raman Nagar", 12.4),
    RegionLabelSpec(12.955, 77.655, "Kodihalli", 12.55),
    RegionLabelSpec(12.978, 77.705, "Doddanekundi", 12.35),
    RegionLabelSpec(12.935, 77.59, "St Johns Road", 12.6),
    RegionLabelSpec(12.988, 77.628, "Shivajinagar", 12.5),
    RegionLabelSpec(12.975, 77.625, "Richmond Town", 12.65),
    RegionLabelSpec(12.965, 77.615, "Austin Town", 12.65),
    RegionLabelSpec(12.959, 77.656, "Murugeshpalya", 12.7),
    RegionLabelSpec(13.05, 77.57, "Mathikere", 12.55),
    RegionLabelSpec(13.065, 77.59, "Sanjaynagar", 12.6),
    RegionLabelSpec(12.945, 77.605, "Adugodi", 12.75),
    RegionLabelSpec(12.928, 77.625, "Neelasandra", 12.75),
    RegionLabelSpec(12.918, 77.622, "Madiwala", 12.45),
    RegionLabelSpec(12.982, 77.75, "Gunjur", 12.85),
    RegionLabelSpec(12.905, 77.74, "Chandapura", 12.5),
    RegionLabelSpec(12.835, 77.68, "Jigani", 12.6),
    RegionLabelSpec(13.115, 77.57, "Sahakara Nagar", 12.4),
    RegionLabelSpec(13.08, 77.52, "Hesaraghatta", 12.7),
    RegionLabelSpec(12.99, 77.73, "Bellandur Lake", 13.0),
    RegionLabelSpec(12.97, 77.64, "Langford Town", 12.85),
    RegionLabelSpec(13.002, 77.568, "Okalipuram", 12.9),
    RegionLabelSpec(12.958, 77.648, "HAL", 13.15),
    RegionLabelSpec(12.935, 77.688, "Bellandur SEZ", 13.25),
    RegionLabelSpec(12.902, 77.625, "Arekere", 13.2),
    RegionLabelSpec(12.888, 77.605, "Gottigere South", 13.35),
    RegionLabelSpec(12.915, 77.535, "Pattanagere", 13.3),
    RegionLabelSpec(12.945, 77.495, "Kengeri Satellite Town", 13.25),
    RegionLabelSpec(12.998, 77.505, "Peenya Industrial", 13.35),
    RegionLabelSpec(13.048, 77.505, "T Dasarahalli", 13.4),
    RegionLabelSpec(13.072, 77.535, "Chikkabanavara", 13.45),
    RegionLabelSpec(13.085, 77.605, "Kodigehalli", 13.35),
    RegionLabelSpec(13.065, 77.615, "Hebbal Kempapura", 13.4),
    RegionLabelSpec(13.042, 77.665, "HBR Layout", 13.45),
    RegionLabelSpec(13.028, 77.675, "HRBR Layout", 13.5),
    RegionLabelSpec(13.008, 77.675, "Kasturi Nagar", 13.45),
    RegionLabelSpec(12.992, 77.665, "Jeevan Bheemanagar", 13.5),
    RegionLabelSpec(12.968, 77.725, "Hope Farm", 13.35),
    RegionLabelSpec(12.942, 77.698, "Seegehalli", 13.55),
    RegionLabelSpec(12.918, 77.672, "Haralur Road", 13.5),
    RegionLabelSpec(12.916, 77.619, "Madiwala Checkpost", 13.45),
    RegionLabelSpec(12.942, 77.592, "Wilson Garden", 13.55),
    RegionLabelSpec(12.932, 77.572, "Srinagar", 13.6),
    RegionLabelSpec(12.918, 77.558, "Banashankari 3rd Stage", 13.55),
    RegionLabelSpec(12.905, 77.538, "Chikkalasandra", 13.6),
    RegionLabelSpec(12.895, 77.52, "Uttarahalli", 13.55),
    RegionLabelSpec(12.882, 77.535, "Subramanyapura", 13.65),
    RegionLabelSpec(12.868, 77.52, "Poornaprajna Layout", 13.65),
    RegionLabelSpec(12.948, 77.505, "Nagarbhavi", 13.45),
    RegionLabelSpec(12.965, 77.495, "Moodalapalya", 13.55),
    RegionLabelSpec(12.985, 77.52, "Kamakshipalya", 13.5),
    RegionLabelSpec(12.992, 77.535, "Chord Road", 13.55),
    RegionLabelSpec(13.005, 77.52, "Goraguntepalya", 13.5),
    RegionLabelSpec(13.018, 77.545, "Yeshwanthpur Industry", 13.45),
    RegionLabelSpec(12.978, 77.655, "Indiranagar 100ft", 13.6),
    RegionLabelSpec(12.952, 77.628, "Shanthinagar", 13.65),
    RegionLabelSpec(12.938, 77.615, "Adugodi Lake", 13.7),
    RegionLabelSpec(12.922, 77.598, "Tavarekere", 13.65),
    RegionLabelSpec(12.908, 77.615, "Suddaguntepalya", 13.7),
    RegionLabelSpec(12.895, 77.595, "Roopena Agrahara", 13.65),
    RegionLabelSpec(12.878, 77.62, "Muneshwara Nagar", 13.75),
    RegionLabelSpec(12.865, 77.645, "Bommanahalli", 13.55),
    RegionLabelSpec(12.852, 77.665, "Hongasandra", 13.7),
    RegionLabelSpec(12.838, 77.68, "Singasandra", 13.65),
    RegionLabelSpec(12.825, 77.695, "Hosa Road", 13.55),
    RegionLabelSpec(12.998, 77.738, "Varthur Kodi", 13.6),
    RegionLabelSpec(13.018, 77.728, "Siddapura", 13.75),
    RegionLabelSpec(13.035, 77.71, "Whitefield HO", 13.65),
    RegionLabelSpec(12.988, 77.738, "Gunjur Village", 13.8),
    RegionLabelSpec(12.972, 77.752, "Balagere", 13.85),
    RegionLabelSpec(12.955, 77.765, "Bellandur Gate", 13.75),
    RegionLabelSpec(13.055, 77.58, "RMV Extension", 13.55),
    RegionLabelSpec(13.075, 77.565, "Sanjay Nagar West", 13.65),
    RegionLabelSpec(13.088, 77.59, "Yelahanka New Town", 13.45),
    RegionLabelSpec(13.095, 77.615, "Kogilu", 13.7),
    RegionLabelSpec(13.068, 77.665, "Thanisandra Main Road", 13.65),
    RegionLabelSpec(13.045, 77.695, "Hennur Bande", 13.75),
    RegionLabelSpec(13.022, 77.705, "Kothanur", 13.8),
    RegionLabelSpec(12.988, 77.585, "Sheshadripuram", 13.7),
    RegionLabelSpec(12.978, 77.575, "Chamrajpet", 13.75),
    RegionLabelSpec(12.968, 77.565, "Fort Area", 13.85),
    RegionLabelSpec(12.958, 77.555, "Kalasipalya", 13.9),
    RegionLabelSpec(12.948, 77.598, "Lalbagh West Gate", 13.75),
    RegionLabelSpec(12.942, 77.636, "Koramangala 8th Block", 13.85),
    RegionLabelSpec(12.935, 77.618, "Koramangala 4th Block", 13.9),
    RegionLabelSpec(12.937, 77.629, "Ejipura Signal", 13.95),
    RegionLabelSpec(12.935, 77.615, "Sony World Signal", 14.0),
    RegionLabelSpec(12.936, 77.610, "Forum Mall area", 14.05),
    RegionLabelSpec(12.930, 77.622, "Koramangala Inner", 14.1),
    RegionLabelSpec(12.995, 77.668, "Old Airport Road", 13.65),
    RegionLabelSpec(13.008, 77.688, "Wind Tunnel Road", 13.85),
    RegionLabelSpec(13.022, 77.698, "Murphy Town", 13.9),
    RegionLabelSpec(12.965, 77.738, "Panathur Road", 13.8),
    RegionLabelSpec(12.948, 77.728, "Sarjapur Road", 13.55),
    RegionLabelSpec(12.918, 77.712, "Wipro SEZ", 13.75),
    RegionLabelSpec(12.905, 77.702, "RGA Tech Park", 13.85),
    RegionLabelSpec(12.892, 77.692, "Sompura Gate", 13.9),
    RegionLabelSpec(12.878, 77.685, "Dommasandra", 14.0),
    RegionLabelSpec(12.865, 77.675, "Chandapura Anekal", 13.95),
    RegionLabelSpec(12.852, 77.665, "Hebbagodi", 13.85),
    RegionLabelSpec(12.838, 77.655, "Electronic City Phase 2", 13.7),
    RegionLabelSpec(12.825, 77.645, "Konappana Agrahara", 13.75),
    RegionLabelSpec(12.812, 77.635, "Bommasandra Industrial", 13.8),
    RegionLabelSpec(12.798, 77.625, "Jigani Industrial", 13.85),
    RegionLabelSpec(12.785, 77.615, "Anekal Town", 13.65),
    RegionLabelSpec(12.772, 77.605, "Attibele Industrial", 13.7),
    RegionLabelSpec(13.105, 77.645, "Kannuru", 13.75),
    RegionLabelSpec(13.118, 77.665, "Hosuru Road", 13.8),
    RegionLabelSpec(13.128, 77.685, "Budigere Cross", 13.85),
    RegionLabelSpec(13.138, 77.705, "Old Madras Road", 13.55),
    RegionLabelSpec(13.148, 77.725, "Hoskote Industrial", 13.65),
    RegionLabelSpec(13.088, 77.755, "Narasapura", 14.0),
    RegionLabelSpec(13.058, 77.735, "Seegehalli Cross", 13.95),
    RegionLabelSpec(13.042, 77.715, "Hope Farm Junction", 13.7),
    RegionLabelSpec(13.025, 77.695, "Kundalahalli Gate", 13.75),
    RegionLabelSpec(13.012, 77.678, "Graphite India Main", 13.8),
    RegionLabelSpec(12.998, 77.662, "ITPL Main Road", 13.65),
    RegionLabelSpec(12.985, 77.648, "Brookefield Mall", 13.85),
    RegionLabelSpec(12.972, 77.635, "Kundalahalli Colony", 13.9),
    RegionLabelSpec(12.958, 77.622, "Marathahalli Bridge", 13.95),
    RegionLabelSpec(12.945, 77.612, "Marathahalli ORR", 13.85),
    RegionLabelSpec(12.932, 77.602, "Devarabisanahalli", 14.0),
    RegionLabelSpec(12.918, 77.592, "Bellandur ORR", 13.95),
    RegionLabelSpec(12.905, 77.582, "Agara Village", 14.05),
    RegionLabelSpec(12.892, 77.572, "Iblur Village", 14.1),
    RegionLabelSpec(12.878, 77.562, "Haralur", 14.0),
    RegionLabelSpec(12.865, 77.552, "Hulimavu", 13.95),
    RegionLabelSpec(12.852, 77.542, "Gottigere Forest", 14.15),
    RegionLabelSpec(12.838, 77.532, "Bannerghatta National Park", 13.9),
    RegionLabelSpec(12.825, 77.518, "Kaggalipura", 14.05),
    RegionLabelSpec(12.805, 77.502, "Thalaghattapura", 13.95),
    RegionLabelSpec(12.785, 77.482, "Kumbalgodu", 14.0),
    RegionLabelSpec(12.802, 77.395, "Bidadi Town", 13.85),
    RegionLabelSpec(12.977, 77.518, "Nayandahalli", 13.85),
    RegionLabelSpec(12.935, 77.519, "Rajarajeshwari Arch", 13.9),
    RegionLabelSpec(12.915, 77.485, "Kengeri Town", 13.95),
    // G-lite v1.0.4 â€” additional Bengaluru localities (area / place names, not street geometry)
    RegionLabelSpec(13.043, 77.619, "Manyata Tech Park", 10.85),
    RegionLabelSpec(13.247, 77.708, "Devanahalli", 9.2),
    RegionLabelSpec(13.006, 77.578, "Sadashivnagar", 11.35),
    RegionLabelSpec(13.024, 77.594, "RT Nagar", 11.25),
    RegionLabelSpec(13.004, 77.602, "Benson Town", 11.85),
    RegionLabelSpec(12.989, 77.587, "Cunningham Road", 12.15),
    RegionLabelSpec(12.924, 77.638, "Agara", 11.65),
    RegionLabelSpec(13.198, 77.668, "Kempegowda Airport", 9.0, maxZoom = 10.75),
    RegionLabelSpec(13.092, 77.580, "Yelahanka New Town", 10.65, maxZoom = 12.2),
    RegionLabelSpec(12.882, 77.628, "Bommasandra", 11.15),
    RegionLabelSpec(12.978, 77.572, "Race Course Road", 12.25),
    RegionLabelSpec(12.962, 77.601, "Richmond Circle", 12.45),
    RegionLabelSpec(13.035, 77.567, "MS Palya", 12.15),
    RegionLabelSpec(12.968, 77.701, "Hosur Road", 11.05, maxZoom = 12.5),
    RegionLabelSpec(13.028, 77.584, "Bellary Road", 10.95, maxZoom = 12.3),
    RegionLabelSpec(12.946, 77.573, "Vittal Mallya Road", 12.35),
)

private val mumbaiRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(19.218, 72.978, "Thane", 9.0),
    RegionLabelSpec(19.033, 73.030, "Navi Mumbai", 9.0),
    RegionLabelSpec(19.210, 72.865, "Borivali", 9.0),
    RegionLabelSpec(18.925, 72.835, "Colaba", 9.0),
    RegionLabelSpec(19.076, 72.878, "Bandra", 9.45),
    RegionLabelSpec(19.114, 72.870, "Andheri", 9.45),
    RegionLabelSpec(19.118, 72.910, "Powai", 9.65),
    RegionLabelSpec(19.164, 72.849, "Goregaon", 9.75),
    RegionLabelSpec(19.067, 72.899, "Kurla", 9.85),
    RegionLabelSpec(19.054, 72.893, "Chembur", 9.85),
    RegionLabelSpec(19.018, 72.856, "Worli", 10.05),
    RegionLabelSpec(19.060, 72.830, "Dadar", 10.05),
    RegionLabelSpec(19.136, 72.829, "Malad", 10.25),
    RegionLabelSpec(19.087, 72.909, "Ghatkopar", 10.25),
    RegionLabelSpec(19.120, 72.840, "Jogeshwari", 10.45),
    RegionLabelSpec(19.090, 72.826, "Vile Parle", 10.45),
    RegionLabelSpec(19.054, 72.835, "Santacruz", 10.65),
    RegionLabelSpec(19.143, 72.848, "Malad West", 10.65),
    RegionLabelSpec(19.175, 72.945, "Mulund", 10.85),
    RegionLabelSpec(19.155, 72.935, "Bhandup", 10.85),
    RegionLabelSpec(19.025, 72.870, "Lower Parel", 11.05),
    RegionLabelSpec(19.010, 72.830, "Mahalaxmi", 11.05),
    RegionLabelSpec(18.978, 72.825, "Worli Sea Face", 11.25),
    RegionLabelSpec(19.045, 72.885, "Sion", 11.25),
    RegionLabelSpec(19.065, 72.915, "Matunga", 11.45),
    RegionLabelSpec(19.170, 72.955, "Kanjurmarg", 11.45),
    RegionLabelSpec(19.065, 73.015, "Vashi", 11.65),
    RegionLabelSpec(19.100, 73.045, "Airoli", 11.65),
    RegionLabelSpec(19.135, 73.005, "Ghansoli", 11.85),
    RegionLabelSpec(19.195, 72.825, "Dahisar", 11.85),
    RegionLabelSpec(19.245, 72.855, "Mira Road", 12.05),
    RegionLabelSpec(19.150, 72.935, "Vikhroli", 12.05),
    RegionLabelSpec(19.080, 72.885, "BKC", 12.25),
    RegionLabelSpec(19.035, 72.875, "Antop Hill", 12.25),
    RegionLabelSpec(18.995, 72.815, "Byculla", 12.45),
    RegionLabelSpec(18.940, 72.835, "Cuffe Parade", 12.45),
    RegionLabelSpec(19.055, 72.895, "Wadala", 12.65),
    RegionLabelSpec(19.115, 72.905, "Deonar", 12.65),
    RegionLabelSpec(19.185, 72.885, "Kandivali", 12.85),
    RegionLabelSpec(19.200, 72.905, "Borivali East", 12.85),
    RegionLabelSpec(19.050, 72.995, "Seawoods", 13.05),
    RegionLabelSpec(19.020, 73.020, "Nerul", 13.05),
    RegionLabelSpec(18.995, 73.045, "Belapur", 13.25),
    RegionLabelSpec(19.080, 73.060, "Kharghar", 13.25),
    RegionLabelSpec(19.145, 72.995, "Koparkhairane", 13.45),
    RegionLabelSpec(19.255, 72.985, "Dombivli", 13.45),
    RegionLabelSpec(19.165, 72.995, "Thane West", 13.65),
    RegionLabelSpec(19.115, 72.825, "Oshiwara", 13.65),
    RegionLabelSpec(19.055, 72.815, "Fort", 13.85),
    RegionLabelSpec(19.075, 72.820, "CST area", 13.85),
    RegionLabelSpec(19.045, 72.855, "Tardeo", 14.05),
    RegionLabelSpec(19.100, 72.865, "Santacruz East", 14.05),
)

private val delhiRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(28.632, 77.217, "Connaught Place", 9.0),
    RegionLabelSpec(28.535, 77.391, "Noida", 9.0),
    RegionLabelSpec(28.460, 77.027, "Gurugram", 9.0),
    RegionLabelSpec(28.592, 77.046, "Dwarka", 9.0),
    RegionLabelSpec(28.613, 77.230, "India Gate", 9.45),
    RegionLabelSpec(28.749, 77.117, "Rohini", 9.55),
    RegionLabelSpec(28.682, 77.223, "GTB Nagar", 9.65),
    RegionLabelSpec(28.652, 77.191, "Karol Bagh", 9.85),
    RegionLabelSpec(28.568, 77.243, "Hauz Khas", 9.85),
    RegionLabelSpec(28.525, 77.207, "Okhla", 10.05),
    RegionLabelSpec(28.704, 77.103, "Pitampura", 10.05),
    RegionLabelSpec(28.499, 77.409, "Greater Noida", 10.25),
    RegionLabelSpec(28.612, 77.205, "Pragati Maidan", 10.25),
    RegionLabelSpec(28.645, 77.215, "Kashmere Gate", 10.45),
    RegionLabelSpec(28.670, 77.230, "Civil Lines", 10.45),
    RegionLabelSpec(28.588, 77.255, "Lajpat Nagar", 10.65),
    RegionLabelSpec(28.555, 77.235, "Greater Kailash", 10.65),
    RegionLabelSpec(28.535, 77.265, "Nehru Place", 10.85),
    RegionLabelSpec(28.520, 77.185, "Vasant Kunj", 10.85),
    RegionLabelSpec(28.495, 77.175, "Chhatarpur", 11.05),
    RegionLabelSpec(28.720, 77.120, "Rohini Sector 18", 11.05),
    RegionLabelSpec(28.695, 77.205, "Model Town", 11.25),
    RegionLabelSpec(28.665, 77.195, "Kamla Nagar", 11.25),
    RegionLabelSpec(28.640, 77.175, "Patel Nagar", 11.45),
    RegionLabelSpec(28.600, 77.225, "Saket", 11.45),
    RegionLabelSpec(28.575, 77.210, "Chirag Delhi", 11.65),
    RegionLabelSpec(28.555, 77.195, "Defence Colony", 11.65),
    RegionLabelSpec(28.540, 77.255, "Kalkaji", 11.85),
    RegionLabelSpec(28.515, 77.305, "Mayur Vihar", 11.85),
    RegionLabelSpec(28.495, 77.335, "Patparganj", 12.05),
    RegionLabelSpec(28.475, 77.355, "Vasundhara Enclave", 12.05),
    RegionLabelSpec(28.450, 77.380, "Indirapuram", 12.25),
    RegionLabelSpec(28.420, 77.340, "Sector 62 Noida", 12.25),
    RegionLabelSpec(28.400, 77.320, "Sector 18 Noida", 12.45),
    RegionLabelSpec(28.380, 77.300, "Sector 15 Noida", 12.45),
    RegionLabelSpec(28.440, 77.050, "DLF Phase 1", 12.65),
    RegionLabelSpec(28.470, 77.080, "DLF Cyber City", 12.65),
    RegionLabelSpec(28.500, 77.100, "Udyog Vihar", 12.85),
    RegionLabelSpec(28.520, 77.120, "Sohna Road", 12.85),
    RegionLabelSpec(28.540, 77.140, "Golf Course Road", 13.05),
    RegionLabelSpec(28.560, 77.160, "Sector 29 Gurugram", 13.05),
    RegionLabelSpec(28.580, 77.070, "Palam", 13.25),
    RegionLabelSpec(28.610, 77.050, "Dwarka Sector 21", 13.25),
    RegionLabelSpec(28.620, 77.080, "Dwarka Sector 12", 13.45),
    RegionLabelSpec(28.630, 77.100, "Dwarka Sector 6", 13.45),
    RegionLabelSpec(28.640, 77.240, "Yamuna Bank", 13.65),
    RegionLabelSpec(28.660, 77.260, "Shastri Park", 13.65),
    RegionLabelSpec(28.680, 77.280, "Seelampur", 13.85),
    RegionLabelSpec(28.700, 77.300, "Welcome", 13.85),
    RegionLabelSpec(28.720, 77.320, "Maujpur", 14.05),
    RegionLabelSpec(28.740, 77.340, "Bhajanpura", 14.05),
)

private val chennaiRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(13.083, 80.271, "Chennai Central", 9.0),
    RegionLabelSpec(12.925, 80.130, "Tambaram", 9.0),
    RegionLabelSpec(13.035, 80.157, "Porur", 9.0),
    RegionLabelSpec(12.952, 80.241, "OMR", 9.0),
    RegionLabelSpec(13.042, 80.234, "T Nagar", 9.45),
    RegionLabelSpec(12.990, 80.249, "Adyar", 9.55),
    RegionLabelSpec(12.982, 80.218, "Velachery", 9.75),
    RegionLabelSpec(13.073, 80.261, "Egmore", 9.75),
    RegionLabelSpec(13.007, 80.221, "Guindy", 9.95),
    RegionLabelSpec(13.055, 80.200, "Anna Nagar", 9.95),
    RegionLabelSpec(13.020, 80.185, "Koyambedu", 10.15),
    RegionLabelSpec(13.100, 80.250, "Washermenpet", 10.15),
    RegionLabelSpec(13.065, 80.275, "Royapuram", 10.35),
    RegionLabelSpec(12.965, 80.255, "Besant Nagar", 10.35),
    RegionLabelSpec(12.940, 80.230, "Thiruvanmiyur", 10.55),
    RegionLabelSpec(12.920, 80.210, "Perungudi", 10.55),
    RegionLabelSpec(12.905, 80.185, "Sholinganallur", 10.75),
    RegionLabelSpec(12.890, 80.165, "Kelambakkam", 10.75),
    RegionLabelSpec(13.020, 80.240, "Nungambakkam", 10.95),
    RegionLabelSpec(13.005, 80.255, "Chetpet", 10.95),
    RegionLabelSpec(12.995, 80.270, "Shenoy Nagar", 11.15),
    RegionLabelSpec(13.045, 80.220, "Arumbakkam", 11.15),
    RegionLabelSpec(13.080, 80.230, "Perambur", 11.35),
    RegionLabelSpec(13.110, 80.245, "Tondiarpet", 11.35),
    RegionLabelSpec(13.130, 80.265, "Ennore", 11.55),
    RegionLabelSpec(12.975, 80.205, "Madipakkam", 11.55),
    RegionLabelSpec(12.955, 80.185, "Medavakkam", 11.75),
    RegionLabelSpec(12.935, 80.165, "Karappakkam", 11.75),
    RegionLabelSpec(13.010, 80.175, "Ashok Pillar", 11.95),
    RegionLabelSpec(13.025, 80.165, "Vadapalani", 11.95),
    RegionLabelSpec(13.040, 80.155, "Kodambakkam", 12.15),
    RegionLabelSpec(13.055, 80.145, "Saligramam", 12.15),
    RegionLabelSpec(13.070, 80.135, "Virugambakkam", 12.35),
    RegionLabelSpec(13.085, 80.125, "Valasaravakkam", 12.35),
    RegionLabelSpec(13.100, 80.115, "Poonamallee", 12.55),
    RegionLabelSpec(13.115, 80.105, "Iyyapanthangal", 12.55),
    RegionLabelSpec(12.970, 80.280, "Light House", 12.75),
    RegionLabelSpec(12.955, 80.265, "Marina North", 12.75),
    RegionLabelSpec(12.940, 80.250, "Mylapore Tank", 12.95),
    RegionLabelSpec(12.925, 80.235, "Mandaveli", 12.95),
    RegionLabelSpec(13.000, 80.285, "George Town", 13.15),
    RegionLabelSpec(13.015, 80.295, "Parrys", 13.15),
    RegionLabelSpec(13.180, 80.280, "Minjur", 13.35),
    RegionLabelSpec(13.150, 80.260, "Red Hills", 13.35),
    RegionLabelSpec(13.120, 80.240, "Ambattur", 13.55),
    RegionLabelSpec(13.090, 80.220, "Avadi", 13.55),
    RegionLabelSpec(13.060, 80.200, "Pattabiram", 13.75),
    RegionLabelSpec(13.030, 80.180, "Tiruvallur Road", 13.75),
    RegionLabelSpec(12.910, 80.145, "Vandalur", 13.95),
    RegionLabelSpec(12.895, 80.125, "Guduvanchery", 13.95),
    RegionLabelSpec(12.880, 80.105, "Maraimalai Nagar", 14.15),
    RegionLabelSpec(12.865, 80.095, "Singaperumal Koil", 14.15),
)

private val hyderabadRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(17.385, 78.487, "Charminar", 9.0),
    RegionLabelSpec(17.447, 78.376, "HITEC City", 9.0),
    RegionLabelSpec(17.440, 78.349, "Gachibowli", 9.0),
    RegionLabelSpec(17.407, 78.477, "Secunderabad", 9.0),
    RegionLabelSpec(17.493, 78.400, "Kukatpally", 9.45),
    RegionLabelSpec(17.403, 78.560, "Uppal", 9.45),
    RegionLabelSpec(17.361, 78.475, "Malakpet", 9.65),
    RegionLabelSpec(17.255, 78.551, "Shamshabad", 9.65),
    RegionLabelSpec(17.425, 78.420, "Begumpet", 9.85),
    RegionLabelSpec(17.415, 78.440, "Somajiguda", 9.85),
    RegionLabelSpec(17.400, 78.485, "Abids", 10.05),
    RegionLabelSpec(17.385, 78.505, "Chaderghat", 10.05),
    RegionLabelSpec(17.370, 78.520, "Dilsukhnagar", 10.25),
    RegionLabelSpec(17.355, 78.535, "LB Nagar", 10.25),
    RegionLabelSpec(17.340, 78.550, "Vanasthalipuram", 10.45),
    RegionLabelSpec(17.325, 78.565, "Hayathnagar", 10.45),
    RegionLabelSpec(17.460, 78.390, "Miyapur", 10.65),
    RegionLabelSpec(17.475, 78.375, "Bachupally", 10.65),
    RegionLabelSpec(17.490, 78.360, "Nizampet", 10.85),
    RegionLabelSpec(17.505, 78.345, "Pragathi Nagar", 10.85),
    RegionLabelSpec(17.435, 78.365, "Financial District", 11.05),
    RegionLabelSpec(17.420, 78.355, "Nanakramguda", 11.05),
    RegionLabelSpec(17.405, 78.345, "Manikonda", 11.25),
    RegionLabelSpec(17.390, 78.335, "Narsingi", 11.25),
    RegionLabelSpec(17.375, 78.325, "Kokapet", 11.45),
    RegionLabelSpec(17.360, 78.315, "Tellapur", 11.45),
    RegionLabelSpec(17.345, 78.305, "Serilingampally", 11.65),
    RegionLabelSpec(17.330, 78.295, "BHEL", 11.65),
    RegionLabelSpec(17.315, 78.285, "Lingampally", 11.85),
    RegionLabelSpec(17.300, 78.275, "Chandanagar", 11.85),
    RegionLabelSpec(17.430, 78.500, "Trimulgherry", 12.05),
    RegionLabelSpec(17.445, 78.515, "Bolaram", 12.05),
    RegionLabelSpec(17.460, 78.530, "Alwal", 12.25),
    RegionLabelSpec(17.475, 78.545, "Yapral", 12.25),
    RegionLabelSpec(17.490, 78.560, "Kapra", 12.45),
    RegionLabelSpec(17.505, 78.575, "ECIL", 12.45),
    RegionLabelSpec(17.520, 78.590, "Kushaiguda", 12.65),
    RegionLabelSpec(17.535, 78.605, "Nagole", 12.65),
    RegionLabelSpec(17.550, 78.620, "Bandlaguda", 12.85),
    RegionLabelSpec(17.565, 78.635, "Meerpet", 12.85),
    RegionLabelSpec(17.575, 78.650, "Saroornagar", 13.05),
    RegionLabelSpec(17.570, 78.665, "Champapet", 13.05),
    RegionLabelSpec(17.250, 78.535, "Shamshabad Airport", 13.25),
    RegionLabelSpec(17.265, 78.520, "RGIA Cargo", 13.25),
    RegionLabelSpec(17.280, 78.505, "Old City South", 13.45),
    RegionLabelSpec(17.295, 78.490, "Rajendranagar", 13.45),
    RegionLabelSpec(17.310, 78.475, "Attapur", 13.65),
    RegionLabelSpec(17.325, 78.460, "Falaknuma", 13.65),
    RegionLabelSpec(17.340, 78.445, "Chandrayangutta", 13.85),
    RegionLabelSpec(17.355, 78.430, "Mailardevpally", 13.85),
    RegionLabelSpec(17.370, 78.415, "Katedan", 14.05),
    RegionLabelSpec(17.385, 78.400, "Shivrampally", 14.05),
    RegionLabelSpec(17.400, 78.385, "Gandipet Lake", 14.25),
    RegionLabelSpec(17.415, 78.370, "Neknampur", 14.25),
    RegionLabelSpec(17.430, 78.355, "Kokapet Junction", 14.45),
    RegionLabelSpec(17.445, 78.340, "Financial Dist ORR", 14.45),
    RegionLabelSpec(17.460, 78.325, "Gachibowli ORR", 14.65),
    RegionLabelSpec(17.475, 78.310, "Nanakramguda ORR", 14.65),
    RegionLabelSpec(17.490, 78.295, "Narsingi ORR", 14.85),
    RegionLabelSpec(17.505, 78.280, "Tellapur ORR", 14.85),
)

private val kolkataRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(22.573, 88.364, "Esplanade", 9.0),
    RegionLabelSpec(22.586, 88.411, "Salt Lake", 9.0),
    RegionLabelSpec(22.654, 88.447, "New Town", 9.0),
    RegionLabelSpec(22.611, 88.400, "Howrah", 9.0),
    RegionLabelSpec(22.545, 88.353, "Park Street", 9.45),
    RegionLabelSpec(22.508, 88.396, "Behala", 9.45),
    RegionLabelSpec(22.477, 88.319, "Jadavpur", 9.65),
    RegionLabelSpec(22.533, 88.370, "Ballygunge", 9.65),
    RegionLabelSpec(22.560, 88.380, "Gariahat", 9.85),
    RegionLabelSpec(22.520, 88.370, "Kalighat", 9.85),
    RegionLabelSpec(22.585, 88.350, "Sealdah", 10.05),
    RegionLabelSpec(22.600, 88.365, "Phoolbagan", 10.05),
    RegionLabelSpec(22.620, 88.385, "EM Bypass", 10.25),
    RegionLabelSpec(22.640, 88.405, "Sector V", 10.25),
    RegionLabelSpec(22.660, 88.425, "Karunamoyee", 10.45),
    RegionLabelSpec(22.680, 88.445, "City Centre 2", 10.45),
    RegionLabelSpec(22.700, 88.465, "Eco Park", 10.65),
    RegionLabelSpec(22.720, 88.485, "Rajarhat", 10.65),
    RegionLabelSpec(22.540, 88.340, "Chowringhee", 10.85),
    RegionLabelSpec(22.525, 88.330, "Hastings", 10.85),
    RegionLabelSpec(22.510, 88.320, "Alipore", 11.05),
    RegionLabelSpec(22.495, 88.310, "Taratala", 11.05),
    RegionLabelSpec(22.480, 88.300, "Thakurpukur", 11.25),
    RegionLabelSpec(22.465, 88.290, "Budge Budge", 11.25),
    RegionLabelSpec(22.450, 88.280, "Diamond Harbour Road", 11.45),
    RegionLabelSpec(22.435, 88.270, "Pujali", 11.45),
    RegionLabelSpec(22.570, 88.420, "Lake Town", 11.65),
    RegionLabelSpec(22.555, 88.410, "Bangur", 11.65),
    RegionLabelSpec(22.540, 88.400, "Dum Dum", 11.85),
    RegionLabelSpec(22.525, 88.390, "Nagerbazar", 11.85),
    RegionLabelSpec(22.510, 88.380, "Baguiati", 12.05),
    RegionLabelSpec(22.495, 88.370, "Kestopur", 12.05),
    RegionLabelSpec(22.480, 88.360, "Teghoria", 12.25),
    RegionLabelSpec(22.465, 88.350, "Haldiram", 12.25),
    RegionLabelSpec(22.450, 88.340, "Airport Gate 1", 12.45),
    RegionLabelSpec(22.435, 88.330, "Airport South", 12.45),
    RegionLabelSpec(22.620, 88.390, "Ruby Hospital", 12.65),
    RegionLabelSpec(22.605, 88.375, "Garia", 12.65),
    RegionLabelSpec(22.590, 88.360, "Sonarpur", 12.85),
    RegionLabelSpec(22.575, 88.345, "Baruipur", 12.85),
    RegionLabelSpec(22.560, 88.330, "Mukundapur", 13.05),
    RegionLabelSpec(22.545, 88.315, "Kasba", 13.05),
    RegionLabelSpec(22.530, 88.300, "Ruby Crossing", 13.25),
    RegionLabelSpec(22.515, 88.285, "E M Bypass South", 13.25),
    RegionLabelSpec(22.500, 88.270, "Southern Avenue", 13.45),
    RegionLabelSpec(22.485, 88.255, "Dhakuria", 13.45),
    RegionLabelSpec(22.470, 88.240, "Golf Green", 13.65),
    RegionLabelSpec(22.455, 88.225, "Jodhpur Park", 13.65),
    RegionLabelSpec(22.445, 88.215, "Lake Gardens", 13.85),
    RegionLabelSpec(22.430, 88.205, "Tollygunge", 13.85),
    RegionLabelSpec(22.415, 88.195, "Ranikuthi", 14.05),
    RegionLabelSpec(22.405, 88.185, "Netaji Nagar", 14.05),
)

private val puneRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(18.520, 73.857, "Shivajinagar", 9.0),
    RegionLabelSpec(18.600, 73.762, "Hinjewadi", 9.0),
    RegionLabelSpec(18.506, 73.944, "Viman Nagar", 9.0),
    RegionLabelSpec(18.414, 73.883, "Katraj", 9.0),
    RegionLabelSpec(18.536, 73.894, "Koregaon Park", 9.45),
    RegionLabelSpec(18.458, 73.851, "Kothrud", 9.45),
    RegionLabelSpec(18.496, 73.858, "Swargate", 9.65),
    RegionLabelSpec(18.535, 73.875, "Camp", 9.65),
    RegionLabelSpec(18.505, 73.830, "Deccan", 9.85),
    RegionLabelSpec(18.485, 73.815, "Erandwane", 9.85),
    RegionLabelSpec(18.465, 73.800, "Karve Nagar", 10.05),
    RegionLabelSpec(18.445, 73.785, "Warje", 10.05),
    RegionLabelSpec(18.425, 73.770, "Bavdhan", 10.25),
    RegionLabelSpec(18.405, 73.755, "Baner", 10.25),
    RegionLabelSpec(18.385, 73.740, "Aundh", 10.45),
    RegionLabelSpec(18.365, 73.725, "Pashan", 10.45),
    RegionLabelSpec(18.345, 73.710, "Sus", 10.65),
    RegionLabelSpec(18.325, 73.725, "Wakad", 10.65),
    RegionLabelSpec(18.305, 73.735, "Tathawade", 10.85),
    RegionLabelSpec(18.285, 73.745, "Ravet", 10.85),
    RegionLabelSpec(18.615, 73.768, "Phase 1 Hinjewadi", 11.05),
    RegionLabelSpec(18.595, 73.752, "Phase 2 Hinjewadi", 11.05),
    RegionLabelSpec(18.575, 73.738, "Phase 3 Hinjewadi", 11.25),
    RegionLabelSpec(18.555, 73.725, "Maan", 11.25),
    RegionLabelSpec(18.535, 73.712, "Marunji", 11.45),
    RegionLabelSpec(18.515, 73.720, "Nande", 11.45),
    RegionLabelSpec(18.555, 73.920, "Kharadi", 11.65),
    RegionLabelSpec(18.535, 73.935, "Lohegaon", 11.65),
    RegionLabelSpec(18.515, 73.950, "Dhanori", 11.85),
    RegionLabelSpec(18.495, 73.965, "Tingre Nagar", 11.85),
    RegionLabelSpec(18.475, 73.980, "Vishrantwadi", 12.05),
    RegionLabelSpec(18.455, 73.995, "Yerawada", 12.05),
    RegionLabelSpec(18.435, 73.970, "Bund Garden", 12.25),
    RegionLabelSpec(18.415, 73.955, "Magarpatta", 12.25),
    RegionLabelSpec(18.395, 73.940, "Hadapsar", 12.45),
    RegionLabelSpec(18.375, 73.925, "Fatima Nagar", 12.45),
    RegionLabelSpec(18.355, 73.910, "Kondhwa", 12.65),
    RegionLabelSpec(18.335, 73.895, "Undri", 12.65),
    RegionLabelSpec(18.315, 73.880, "Mohammadwadi", 12.85),
    RegionLabelSpec(18.295, 73.865, "Handewadi", 12.85),
    RegionLabelSpec(18.275, 73.850, "Uruli", 13.05),
    RegionLabelSpec(18.255, 73.835, "Saswad Road", 13.05),
    RegionLabelSpec(18.235, 73.820, "Phursungi", 13.25),
    RegionLabelSpec(18.215, 73.805, "Manjri", 13.25),
    RegionLabelSpec(18.195, 73.790, "Loni Kalbhor", 13.45),
    RegionLabelSpec(18.175, 73.775, "Wagholi", 13.45),
    RegionLabelSpec(18.155, 73.760, "Shikrapur", 13.65),
    RegionLabelSpec(18.135, 73.745, "Kesnand", 13.65),
    RegionLabelSpec(18.440, 73.740, "Perne", 13.85),
    RegionLabelSpec(18.460, 73.735, "Sanaswadi", 13.85),
    RegionLabelSpec(18.425, 73.745, "Chakan fringe", 14.05),
    RegionLabelSpec(18.415, 73.755, "Talegaon fringe", 14.05),
    RegionLabelSpec(18.425, 73.905, "Dhankawadi", 14.25),
    RegionLabelSpec(18.405, 73.890, "Bibwewadi", 14.25),
    RegionLabelSpec(18.385, 73.875, "Market Yard", 14.45),
    RegionLabelSpec(18.365, 73.860, "Gultekdi", 14.45),
    RegionLabelSpec(18.345, 73.845, "Sahakar Nagar", 14.65),
    RegionLabelSpec(18.325, 73.830, "Parvati", 14.65),
    RegionLabelSpec(18.305, 73.815, "Sinhgad Road", 14.85),
    RegionLabelSpec(18.285, 73.800, "Dhayari", 14.85),
)

private val ahmedabadRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(23.023, 72.571, "Navrangpura", 9.0),
    RegionLabelSpec(23.107, 72.637, "Gandhinagar", 9.0),
    RegionLabelSpec(22.997, 72.638, "Maninagar", 9.0),
    RegionLabelSpec(23.012, 72.508, "SG Highway", 9.0),
    RegionLabelSpec(23.040, 72.566, "Vastrapur", 9.45),
    RegionLabelSpec(23.091, 72.624, "Bopal", 9.45),
    RegionLabelSpec(23.055, 72.545, "Satellite", 9.65),
    RegionLabelSpec(23.070, 72.530, "Bodakdev", 9.65),
    RegionLabelSpec(23.085, 72.515, "Thaltej", 9.85),
    RegionLabelSpec(23.100, 72.500, "Gota", 9.85),
    RegionLabelSpec(23.115, 72.485, "Chandkheda", 10.05),
    RegionLabelSpec(23.130, 72.470, "Motera", 10.05),
    RegionLabelSpec(23.145, 72.455, "Sabarmati", 10.25),
    RegionLabelSpec(23.160, 72.440, "Ranip", 10.25),
    RegionLabelSpec(23.005, 72.595, "Paldi", 10.45),
    RegionLabelSpec(22.990, 72.610, "Ellisbridge", 10.45),
    RegionLabelSpec(22.975, 72.625, "Lal Darwaza", 10.65),
    RegionLabelSpec(22.960, 72.640, "Dariapur", 10.65),
    RegionLabelSpec(22.945, 72.655, "Kalupur", 10.85),
    RegionLabelSpec(22.930, 72.670, "Shahpur", 10.85),
    RegionLabelSpec(22.915, 72.685, "Sarangpur", 11.05),
    RegionLabelSpec(22.900, 72.700, "Khadia", 11.05),
    RegionLabelSpec(22.885, 72.715, "Raipur", 11.25),
    RegionLabelSpec(22.870, 72.730, "Danilimda", 11.25),
    RegionLabelSpec(22.855, 72.745, "Vatva", 11.45),
    RegionLabelSpec(22.840, 72.760, "Isanpur", 11.45),
    RegionLabelSpec(22.825, 72.775, "Naroda", 11.65),
    RegionLabelSpec(22.810, 72.790, "Odhav", 11.65),
    RegionLabelSpec(22.795, 72.705, "Nikol", 11.85),
    RegionLabelSpec(22.780, 72.690, "Kathwada", 11.85),
    RegionLabelSpec(22.765, 72.675, "Vastral", 12.05),
    RegionLabelSpec(22.750, 72.660, "Ramol", 12.05),
    RegionLabelSpec(22.735, 72.645, "Hatkeshwar", 12.25),
    RegionLabelSpec(22.720, 72.630, "Amraiwadi", 12.25),
    RegionLabelSpec(22.705, 72.615, "Civil Hospital", 12.45),
    RegionLabelSpec(22.690, 72.600, "Lal Darwaza South", 12.45),
    RegionLabelSpec(22.675, 72.585, "Gomtipur", 12.65),
    RegionLabelSpec(22.660, 72.570, "Geeta Mandir", 12.65),
    RegionLabelSpec(22.645, 72.555, "Kankaria", 12.85),
    RegionLabelSpec(22.630, 72.540, "Maninagar East", 12.85),
    RegionLabelSpec(22.615, 72.525, "Vatva GIDC", 13.05),
    RegionLabelSpec(22.600, 72.510, "Isanpur Ring", 13.05),
    RegionLabelSpec(22.585, 72.495, "Odhav GIDC", 13.25),
    RegionLabelSpec(22.570, 72.480, "Naroda ST", 13.25),
    RegionLabelSpec(22.555, 72.465, "Nikol East", 13.45),
    RegionLabelSpec(22.540, 72.450, "Vastral Road", 13.45),
    RegionLabelSpec(22.995, 72.565, "Riverfront West", 13.65),
    RegionLabelSpec(22.980, 72.550, "Riverfront East", 13.65),
    RegionLabelSpec(23.010, 72.535, "Shahibaug", 13.85),
    RegionLabelSpec(23.025, 72.520, "Civil Court", 13.85),
    RegionLabelSpec(23.040, 72.505, "Wadaj", 14.05),
    RegionLabelSpec(23.055, 72.490, "Naranpura", 14.05),
)

private val jaipurRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(26.912, 75.787, "Pink City", 9.0),
    RegionLabelSpec(26.957, 75.849, "Mansarovar", 9.0),
    RegionLabelSpec(26.902, 75.734, "Vaishali Nagar", 9.0),
    RegionLabelSpec(26.936, 75.797, "C-Scheme", 9.0),
    RegionLabelSpec(26.853, 75.805, "Malviya Nagar", 9.45),
    RegionLabelSpec(26.929, 75.828, "Raja Park", 9.45),
    RegionLabelSpec(26.890, 75.770, "Tonk Road", 9.65),
    RegionLabelSpec(26.875, 75.755, "Sanganer", 9.65),
    RegionLabelSpec(26.860, 75.740, "Sitapura", 9.85),
    RegionLabelSpec(26.845, 75.725, "Pratap Nagar", 9.85),
    RegionLabelSpec(26.830, 75.710, "Jagatpura", 10.05),
    RegionLabelSpec(26.815, 75.695, "Malviya Industrial", 10.05),
    RegionLabelSpec(26.800, 75.680, "Gopalpura", 10.25),
    RegionLabelSpec(26.785, 75.665, "Triveni Nagar", 10.25),
    RegionLabelSpec(26.770, 75.650, "Shastri Nagar", 10.45),
    RegionLabelSpec(26.755, 75.635, "Vidhyadhar Nagar", 10.45),
    RegionLabelSpec(26.755, 75.655, "Jhotwara", 10.65),
    RegionLabelSpec(26.748, 75.682, "Amer Road", 10.65),
    RegionLabelSpec(26.892, 75.748, "Chomu House", 10.85),
    RegionLabelSpec(26.868, 75.722, "Subhash Nagar", 10.85),
    RegionLabelSpec(26.835, 75.698, "Shastri Nagar West", 11.05),
    RegionLabelSpec(26.812, 75.672, "Bani Park", 11.05),
    RegionLabelSpec(26.948, 75.885, "Sindhi Camp", 11.25),
    RegionLabelSpec(26.925, 75.858, "MI Road", 11.25),
    RegionLabelSpec(26.902, 75.832, "Johari Bazaar", 11.45),
    RegionLabelSpec(26.878, 75.805, "Tripolia", 11.45),
    RegionLabelSpec(26.855, 75.782, "Chandpole", 11.65),
    RegionLabelSpec(26.832, 75.758, "Nahargarh Fort Road", 11.65),
    RegionLabelSpec(26.965, 75.830, "Amer Fort", 11.85),
    RegionLabelSpec(26.782, 75.665, "Kukas Road", 11.85),
    RegionLabelSpec(26.765, 75.648, "Jaisinghpura", 12.05),
    RegionLabelSpec(26.948, 75.668, "Shastri Nagar Ext", 12.25),
    RegionLabelSpec(26.885, 75.698, "Triveni East", 12.25),
    RegionLabelSpec(26.862, 75.672, "Vidhyadhar Ext", 12.45),
    RegionLabelSpec(26.838, 75.648, "Jhotwara Bypass", 12.45),
    RegionLabelSpec(26.978, 75.798, "Amer Corridor", 12.65),
    RegionLabelSpec(26.752, 75.662, "Khora Bisal", 12.65),
    RegionLabelSpec(26.908, 75.888, "Mahindra SEZ", 12.85),
    RegionLabelSpec(26.824, 75.688, "Airport Road", 12.85),
    RegionLabelSpec(26.785, 75.708, "Heerapura", 13.05),
    RegionLabelSpec(26.768, 75.695, "200ft Road", 13.05),
    RegionLabelSpec(26.972, 75.875, "Patrika Gate", 13.25),
    RegionLabelSpec(26.798, 75.718, "WTP Area", 13.25),
    RegionLabelSpec(26.915, 75.748, "Gopalpura Bypass", 13.45),
    RegionLabelSpec(26.888, 75.722, "Durgapura", 13.45),
    RegionLabelSpec(26.874, 75.768, "Sanganer Industrial", 13.65),
    RegionLabelSpec(26.856, 75.752, "Sitapura Ext", 13.65),
    RegionLabelSpec(26.842, 75.738, "RIICO", 13.85),
    RegionLabelSpec(26.826, 75.722, "SEZ South", 13.85),
    RegionLabelSpec(26.952, 75.702, "Chokhi Dhani", 14.05),
    RegionLabelSpec(26.768, 75.678, "Bagru Link Road", 14.05),
)

private val lucknowRegionLabelSpecs: List<RegionLabelSpec> = listOf(
    RegionLabelSpec(26.847, 80.946, "Hazratganj", 9.0),
    RegionLabelSpec(26.888, 80.996, "Gomti Nagar", 9.0),
    RegionLabelSpec(26.874, 80.915, "Alambagh", 9.0),
    RegionLabelSpec(26.935, 80.946, "Indira Nagar", 9.0),
    RegionLabelSpec(26.832, 80.923, "Aishbagh", 9.45),
    RegionLabelSpec(26.901, 81.005, "Mahanagar", 9.45),
    RegionLabelSpec(26.860, 80.930, "Chowk", 9.65),
    RegionLabelSpec(26.845, 80.915, "Nishatganj", 9.65),
    RegionLabelSpec(26.830, 80.900, "Kapoorthala", 9.85),
    RegionLabelSpec(26.815, 80.885, "Rajajipuram", 9.85),
    RegionLabelSpec(26.800, 80.870, "Krishna Nagar", 10.05),
    RegionLabelSpec(26.785, 80.855, "Ashiana", 10.05),
    RegionLabelSpec(26.770, 80.840, "Jankipuram", 10.25),
    RegionLabelSpec(26.758, 80.888, "Sitapur Road", 10.25),
    RegionLabelSpec(26.748, 80.910, "Kursi Road", 10.45),
    RegionLabelSpec(26.892, 80.918, "Polytechnic", 10.45),
    RegionLabelSpec(26.878, 80.902, "IT Crossing", 10.65),
    RegionLabelSpec(26.752, 80.872, "Engineering College", 10.65),
    RegionLabelSpec(26.865, 80.948, "Telibagh", 10.85),
    RegionLabelSpec(26.838, 80.928, "Amar Shaheed Path", 10.85),
    RegionLabelSpec(26.965, 80.995, "Sushant Golf City", 11.05),
    RegionLabelSpec(26.812, 80.898, "Sultanpur Road", 11.05),
    RegionLabelSpec(26.922, 80.972, "Shaheed Path", 11.25),
    RegionLabelSpec(26.788, 80.886, "Vrindavan Yojna", 11.25),
    RegionLabelSpec(26.948, 81.018, "Arjunganj", 11.45),
    RegionLabelSpec(26.768, 80.868, "South City", 11.45),
    RegionLabelSpec(26.902, 80.958, "Ruchi Khand", 11.65),
    RegionLabelSpec(26.758, 80.912, "Asharfabad", 11.65),
    RegionLabelSpec(26.978, 81.032, "Bijnor Road", 11.85),
    RegionLabelSpec(26.778, 80.882, "Chinhat", 11.85),
    RegionLabelSpec(26.932, 80.988, "Faizabad Road", 12.05),
    RegionLabelSpec(26.748, 80.902, "Barabanki Road", 12.05),
    RegionLabelSpec(26.888, 80.942, "Tedhi Pulia", 12.25),
    RegionLabelSpec(26.762, 80.876, "Ring Road", 12.25),
    RegionLabelSpec(26.918, 81.012, "Daliganj", 12.45),
    RegionLabelSpec(26.798, 80.918, "Thakurganj", 12.45),
    RegionLabelSpec(26.952, 80.998, "Balaganj", 12.65),
    RegionLabelSpec(26.772, 80.890, "Aminabad", 12.65),
    RegionLabelSpec(26.862, 80.936, "Chowk Bazar", 12.85),
    RegionLabelSpec(26.748, 80.868, "Kaiserbagh", 12.85),
    RegionLabelSpec(26.905, 80.972, "Lalbagh Lucknow", 13.05),
    RegionLabelSpec(26.785, 80.905, "Charbagh", 13.05),
    RegionLabelSpec(26.938, 81.022, "Mawaiya", 13.25),
    RegionLabelSpec(26.758, 80.892, "Alambagh Bus Stand", 13.25),
    RegionLabelSpec(26.875, 80.928, "Cantt", 13.45),
    RegionLabelSpec(26.748, 80.878, "Dilkusha", 13.45),
    RegionLabelSpec(26.912, 80.988, "La Martiniere", 13.65),
    RegionLabelSpec(26.768, 80.902, "KD Singh Stadium", 13.65),
    RegionLabelSpec(26.958, 81.028, "Janeshwar Mishra Park", 13.85),
    RegionLabelSpec(26.782, 80.918, "Ekana Stadium", 13.85),
    RegionLabelSpec(26.885, 80.948, "SGPGI", 14.05),
    RegionLabelSpec(26.752, 80.888, "Kapoorthala Ext", 14.05),
)

private fun regionLabelSpecsForCity(city: String): List<RegionLabelSpec> = when (city) {
    "BENGALURU" -> bengaluruRegionLabelSpecs
    "MUMBAI" -> mumbaiRegionLabelSpecs
    "DELHI" -> delhiRegionLabelSpecs
    "CHENNAI" -> chennaiRegionLabelSpecs
    "HYDERABAD" -> hyderabadRegionLabelSpecs
    "KOLKATA" -> kolkataRegionLabelSpecs
    "PUNE" -> puneRegionLabelSpecs
    "AHMEDABAD" -> ahmedabadRegionLabelSpecs
    "JAIPUR" -> jaipurRegionLabelSpecs
    "LUCKNOW" -> lucknowRegionLabelSpecs
    else -> emptyList()
}

/** Dim outside city + semi-dark red perimeter (no black stroke). */
private fun syncCityOutlineDecorations(
    map: MapView,
    selectedCity: String,
    decor: CityOutlineDecor,
    mapSeverityFilter: PotholeSeverity?,
    heatEnabled: Boolean = false,
) {
    decor.regionLabels?.let { map.overlays.remove(it) }
    decor.regionLabels = null
    decor.cityName?.let { map.overlays.remove(it) }
    decor.cityName = null
    decor.clusters?.let { map.overlays.remove(it) }
    decor.clusters = null
    removeAreaHeatOverlay(map, decor)
    decor.dim?.let { map.overlays.remove(it) }
    decor.edge?.let { map.overlays.remove(it) }
    decor.dim = null
    decor.edge = null
    val verts = visualOutlineVerticesForCity(selectedCity) ?: return
    val hole = closedGeoRing(verts)
    val density = map.resources.displayMetrics.density
    val outer = dimOuterEnvelopeForCity(selectedCity)
    val dim = Polygon(map).apply {
        setPoints(outer)
        setHoles(listOf(hole))
        getFillPaint().color = AndroidColor.argb(44, 210, 210, 215)
        getFillPaint().isAntiAlias = false
        getOutlinePaint().color = AndroidColor.TRANSPARENT
        getOutlinePaint().strokeWidth = 0f
        setInfoWindow(null)
        setOnClickListener { _, _, _ -> false }
    }
    val edge = Polyline(map, false, true).apply {
        setPoints(ArrayList(verts))
        getOutlinePaint().apply {
            color = AndroidColor.parseColor("#9B2338")
            strokeWidth = 1.3f * density
            style = Paint.Style.STROKE
            pathEffect = null
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            // Aliased stroke is cheaper while panning at high zoom; line is hairline on most densities.
            isAntiAlias = false
        }
        setInfoWindow(null)
        setOnClickListener { _, _, _ -> false }
    }
    decor.dim = dim
    decor.edge = edge
    map.overlays.add(0, dim)
    map.overlays.add(1, edge)
    val labelSpecs = regionLabelSpecsForCity(selectedCity)
    if (labelSpecs.isNotEmpty()) {
        val labels = RegionNameLabelsOverlay(map, labelSpecs)
        decor.regionLabels = labels
        map.overlays.add(2, labels)
    }
    val cityLabel = CityNameOverlay(
        map,
        visualOutlineCentroidForCity(selectedCity),
        formatCityDisplayName(selectedCity),
    )
    decor.cityName = cityLabel
    map.overlays.add(3, cityLabel)
    attachReportClusterOverlay(
        map = map,
        selectedCity = selectedCity,
        decor = decor,
        mapSeverityFilter = mapSeverityFilter,
        heatEnabled = heatEnabled,
    )
    applyCityMapChromeVisibility(decor, visible = false)
    map.invalidate()
}

/** Red outline + dim + region labels â€” only for city overview zoom. */
private fun applyCityMapChromeVisibility(decor: CityOutlineDecor, visible: Boolean) {
    decor.dim?.isEnabled = visible
    decor.edge?.isEnabled = visible
    decor.regionLabels?.isEnabled = visible
}

private data class MetroMapRegion(
    val bbox: BoundingBox?,
    val center: GeoPoint,
    val zoom: Double,
    val minZoom: Double,
)

/** Returns the configured city whose metro boundary contains [location], or null if none. */
private fun cityForLocation(location: Location): String? =
    CityMetroLocation.resolveMetroCity(location.latitude, location.longitude)

private fun metroRegionForCity(selectedCity: String, userLocation: Location?): MetroMapRegion {
    val bbox = metroBboxForCity(selectedCity)
    val fallbackCenter = cityCenterForKey(selectedCity)
    if (bbox == null) {
        return MetroMapRegion(
            bbox = null,
            center = fallbackCenter,
            zoom = 11.0,
            minZoom = 4.0,
        )
    }
    val loc = userLocation
    val userInside = loc != null &&
        CityMetroLocation.coordinatesInMetroCity(selectedCity, loc.latitude, loc.longitude)
    val center = if (loc != null && userInside) {
        GeoPoint(loc.latitude, loc.longitude)
    } else {
        fallbackCenter
    }
    val zoom = if (userInside) 14.0 else 12.5
    return MetroMapRegion(
        bbox = bbox,
        center = center,
        zoom = zoom,
        /** Low enough that "city overview" zoom is not immediately clamped back inward. */
        minZoom = 8.25,
    )
}

/** After a map-drag pick, keep pinch-zoom; only sync bounds/center. */
private fun shouldPreserveZoomForMapPick(selectedCity: String, userLocation: Location?): Boolean {
    val loc = userLocation ?: return false
    if (loc.provider != "map") return false
    return CityMetroLocation.coordinatesInMetroCity(selectedCity, loc.latitude, loc.longitude)
}

private fun MapView.applyMetroPanBounds(selectedCity: String) {
    val metro = metroBboxForCity(selectedCity)
    if (metro != null) {
        setScrollableAreaLimitDouble(metro)
    } else {
        resetScrollableAreaLimitLatitude()
        resetScrollableAreaLimitLongitude()
        setScrollableAreaLimitDouble(null)
    }
}

/** Keeps the visible viewport inside the selected city's metro bounding box at any zoom. */
private fun MapView.clampViewportToMetroBounds(selectedCity: String) {
    val metro = metroBboxForCity(selectedCity) ?: return
    val visible = boundingBox ?: return
    val center = mapCenter

    val visLatSpan = visible.latNorth - visible.latSouth
    val visLonSpan = visible.lonEast - visible.lonWest
    val metroLatSpan = metro.latNorth - metro.latSouth
    val metroLonSpan = metro.lonEast - metro.lonWest

    var newLat = center.latitude
    var newLon = center.longitude

    if (visLatSpan >= metroLatSpan) {
        newLat = (metro.latNorth + metro.latSouth) / 2.0
    } else {
        val halfLat = visLatSpan / 2.0
        newLat = center.latitude.coerceIn(metro.latSouth + halfLat, metro.latNorth - halfLat)
    }

    if (visLonSpan >= metroLonSpan) {
        newLon = (metro.lonEast + metro.lonWest) / 2.0
    } else {
        val halfLon = visLonSpan / 2.0
        newLon = center.longitude.coerceIn(metro.lonWest + halfLon, metro.lonEast - halfLon)
    }

    if (abs(newLat - center.latitude) > 1e-7 || abs(newLon - center.longitude) > 1e-7) {
        controller.setCenter(GeoPoint(newLat, newLon))
    }
}

private fun MapView.applyMetroRegion(
    selectedCity: String,
    region: MetroMapRegion,
    preserveZoom: Boolean = false,
) {
    applyMetroPanBounds(selectedCity)
    val cityViewFloor = minZoomLevel
    minZoomLevel = maxOf(region.minZoom, cityViewFloor)
    maxZoomLevel = 19.0
    if (!preserveZoom) controller.setZoom(region.zoom)
    controller.setCenter(region.center)
    clampViewportToMetroBounds(selectedCity)
    invalidate()
}

/**
 * Applies center/zoom once the view has been measured. Retries when layout is not ready yet.
 */
private fun MapView.scheduleApplyMetroRegion(
    selectedCity: String,
    userLocation: Location?,
    preserveZoom: Boolean,
    attempt: Int = 0,
) {
    val maxAttempts = 15
    post {
        if (width <= 0 || height <= 0) {
            if (attempt < maxAttempts) {
                postDelayed({
                    scheduleApplyMetroRegion(selectedCity, userLocation, preserveZoom, attempt + 1)
                }, 40L)
            }
            return@post
        }
        val region = metroRegionForCity(selectedCity, userLocation)
        applyMetroRegion(selectedCity, region, preserveZoom = preserveZoom)
    }
}

/** Zoom/frame so the city outline fills the map tile (tight margins). */
private fun MapView.fitMetroBoundingBoxOverview(selectedCity: String, restoreSlot: Array<Runnable?>, attempt: Int = 0) {
    val bbox = cityOverviewBoundingBox(selectedCity)
    if (bbox == null) {
        scheduleApplyMetroRegion(selectedCity, userLocation = null, preserveZoom = false)
        return
    }
    restoreSlot[0]?.let { removeCallbacks(it) }
    restoreSlot[0] = null
    val maxAttempts = 15
    post {
        if (width <= 0 || height <= 0) {
            if (attempt < maxAttempts) {
                postDelayed({
                    fitMetroBoundingBoxOverview(selectedCity, restoreSlot, attempt + 1)
                }, 40L)
            }
            return@post
        }
        val savedMin = minZoomLevel
        minZoomLevel = 0.0
        val d = resources.displayMetrics.density
        val padPx = (5f * d).toInt().coerceIn(6, 18)
        zoomToBoundingBox(bbox, false, padPx)
        clampViewportToMetroBounds(selectedCity)
        val cityViewZoom = zoomLevelDouble
        val restore = Runnable {
            // "-" zoom cannot go wider than this city overview level.
            minZoomLevel = cityViewZoom
            invalidate()
            restoreSlot[0] = null
        }
        restoreSlot[0] = restore
        postDelayed(restore, 120)
    }
}

/** Zoom no more than this above [MapView.minZoomLevel] counts as city overview (bbox fit level). */
private const val CITY_VIEW_ZOOM_EPSILON = 0.22

/** Street-level (locate). */
private const val STREET_VIEW_ZOOM = 14.0

/** One ZoomOutMap tap = this many "-" zoom-outs (max zoom-out for the button). */
private const val ZOOM_OUT_MAP_MINUS_STEPS = 4

private fun MapView.clampZoomToCityViewFloor() {
    val floor = minZoomLevel
    if (zoomLevelDouble + 1e-6 < floor) {
        controller.setZoom(floor)
    }
}

private fun MapView.isAtMaxZoom(): Boolean =
    zoomLevelDouble >= maxZoomLevel - 0.05

/** One "-" step: zoom out once, never wider than city-view floor. */
private fun MapView.zoomOutWithinCityView() {
    val floor = minZoomLevel
    if (zoomLevelDouble <= floor + 1e-6) {
        invalidate()
        return
    }
    controller.zoomOut()
    clampZoomToCityViewFloor()
    invalidate()
}

/** Four zoom levels down from max in one tap (same as four "-" presses); stops at city-view floor. */
private fun MapView.applyMetroDefaultZoom(): Boolean {
    if (!isAtMaxZoom()) return false
    val cityFloor = minZoomLevel
    val target = (zoomLevelDouble - ZOOM_OUT_MAP_MINUS_STEPS).coerceAtLeast(cityFloor)
    if (target >= zoomLevelDouble - 1e-6) return false
    controller.setZoom(target)
    clampZoomToCityViewFloor()
    invalidate()
    return true
}

/** Trace to GPS: center on fix and zoom in to street level â€” never zoom out to city view. */
private fun MapView.scheduleFocusOnGps(selectedCity: String, location: Location, attempt: Int = 0) {
    val maxAttempts = 15
    post {
        if (width <= 0 || height <= 0) {
            if (attempt < maxAttempts) {
                postDelayed({ scheduleFocusOnGps(selectedCity, location, attempt + 1) }, 40L)
            }
            return@post
        }
        controller.setCenter(GeoPoint(location.latitude, location.longitude))
        if (abs(zoomLevelDouble - STREET_VIEW_ZOOM) > 1e-6) {
            controller.setZoom(STREET_VIEW_ZOOM)
        }
        clampViewportToMetroBounds(selectedCity)
        invalidate()
    }
}

/** Default city view: fit the red outline edge-to-edge inside the map card. */
private fun MapView.scheduleFitCityOverview(
    selectedCity: String,
    restoreSlot: Array<Runnable?>,
    attempt: Int = 0,
) {
    val maxAttempts = 15
    post {
        if (width <= 0 || height <= 0) {
            if (attempt < maxAttempts) {
                postDelayed({
                    scheduleFitCityOverview(selectedCity, restoreSlot, attempt + 1)
                }, 40L)
            }
            return@post
        }
        fitMetroBoundingBoxOverview(selectedCity, restoreSlot)
    }
}

private fun shouldFocusUserGps(selectedCity: String, userLocation: Location?): Boolean {
    val loc = userLocation ?: return false
    if (loc.provider == "map") return false
    return CityMetroLocation.coordinatesInMetroCity(selectedCity, loc.latitude, loc.longitude)
}

/** Lighter basemap without place-name clutter; reduces busy dashed admin styling vs `light_all`. */
private val CartoPositronNoLabels: XYTileSource = XYTileSource(
    "CartoPositronNoLabels",
    0,
    19,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/light_nolabels/",
        "https://b.basemaps.cartocdn.com/light_nolabels/",
        "https://c.basemaps.cartocdn.com/light_nolabels/"
    ),
    "\u00a9 OpenStreetMap contributors \u00b7 \u00a9 CARTO"
)

/** Small solid green dot for GPS (replaces default osmdroid marker). */
private fun buildGpsPinDrawable(context: Context): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val size = (11f * d).toInt().coerceIn(10, 26)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f
    val r = (size / 2f - 0.5f * d).coerceAtLeast(3f)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#4ADE80")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, r, fill)
    return BitmapDrawable(context.resources, bmp).apply { setBounds(0, 0, size, size) }
}

private fun cancelGpsPinAutoHide(map: MapView, hideRunnableSlot: Array<Runnable?>) {
    hideRunnableSlot[0]?.let { map.removeCallbacks(it) }
    hideRunnableSlot[0] = null
}

private fun scheduleGpsPinAutoHide(
    map: MapView,
    markerSlot: Array<Marker?>,
    hideRunnableSlot: Array<Runnable?>,
) {
    cancelGpsPinAutoHide(map, hideRunnableSlot)
    val r = Runnable {
        markerSlot[0]?.isEnabled = false
        map.invalidate()
        hideRunnableSlot[0] = null
    }
    hideRunnableSlot[0] = r
    map.postDelayed(r, 10_000L)
}

@Composable
private fun OsmDensityMap(
    selectedCity: String,
    userLocation: Location?,
    gpsPinLocation: Location?,
    gpsAccuracyText: String,
    mapSeverityFilter: PotholeSeverity?,
    areaHeatEnabled: Boolean,
    onLocateMe: () -> Unit,
    onMapViewControlUsed: () -> Unit,
    onMapTouchChanged: (Boolean) -> Unit,
    onMapPanEndedAtCenter: (Double, Double) -> Unit,
    mapLocateEpoch: Int,
    lastConsumedMapLocateEpoch: Int,
    onMapLocateEpochConsumed: (Int) -> Unit,
    mapCameraUserPositioned: Boolean,
    savedMapCenterLat: Double?,
    savedMapCenterLon: Double?,
    savedMapZoom: Double?,
    onMapCameraSnapshot: (Double, Double, Double) -> Unit,
    onResetMapCamera: () -> Unit,
    recentReportsEpoch: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapHolder = remember { object { var map: MapView? = null } }
    val overviewRestore = remember { arrayOfNulls<Runnable>(1) }
    val mapViewRef = remember { arrayOfNulls<MapView>(1) }
    val labelOverlayRef = remember { arrayOfNulls<RegionNameLabelsOverlay>(1) }
    val cityOutlineDecor = remember { CityOutlineDecor() }
    /** Mirrors city-overview zoom state; touch resume reads this instead of always re-enabling chrome. */
    val cityMapChromeVisibleRef = remember { booleanArrayOf(false) }
    /** Set in [MapView] factory; used to cancel pending overlay work on dispose. */
    val mapTouchIdleRunnables = remember { arrayOfNulls<Runnable>(2) }
    val lastOutlineCity = remember { mutableStateOf<String?>(null) }
    val lastSyncedReportEpoch = remember { mutableIntStateOf(-1) }
    val lastMapSeverityFilter = remember { mutableStateOf<PotholeSeverity?>(null) }
    val lastAreaHeatEnabled = remember { mutableStateOf(false) }
    val areaHeatEnabledHolder = remember { object { var enabled: Boolean = false } }
    areaHeatEnabledHolder.enabled = areaHeatEnabled
    val gpsMarkerRef = remember { arrayOfNulls<Marker>(1) }
    val gpsPinHideRunnable = remember { arrayOfNulls<Runnable>(1) }
    /** Latest GPS fix for map touch hit-test (factory listener reads this each frame). */
    val latestGpsPinForTouch = remember { arrayOfNulls<Location>(1) }
    val gpsTouchRevealTick = remember { mutableIntStateOf(0) }
    val lastTouchRevealConsumed = remember { mutableIntStateOf(0) }
    val lastGpsPinScheduleKey = remember { mutableStateOf<String?>(null) }
    val lastMapLocateEpochForGpsPin = remember { mutableIntStateOf(0) }
    val selectedCityHolder = remember { object { var city: String = selectedCity } }
    selectedCityHolder.city = selectedCity
    val mapSeverityFilterHolder = remember { object { var filter: PotholeSeverity? = mapSeverityFilter } }
    mapSeverityFilterHolder.filter = mapSeverityFilter
    /** Bumps whenever this map composable enters the tree (e.g. returning to REPORT & TRACK). */
    var mapSessionId by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        mapSessionId++
        onDispose {
            mapHolder.map?.let { m ->
                overviewRestore[0]?.let { m.removeCallbacks(it) }
                overviewRestore[0] = null
                mapTouchIdleRunnables[0]?.let { r ->
                    m.removeCallbacks(r)
                    mapTouchIdleRunnables[0] = null
                }
                mapTouchIdleRunnables[1]?.let { r ->
                    m.removeCallbacks(r)
                    mapTouchIdleRunnables[1] = null
                }
                gpsPinHideRunnable[0]?.let { r ->
                    m.removeCallbacks(r)
                    gpsPinHideRunnable[0] = null
                }
                cityOutlineDecor.regionLabels?.let { m.overlays.remove(it) }
                cityOutlineDecor.cityName?.let { m.overlays.remove(it) }
                cityOutlineDecor.clusters?.let { m.overlays.remove(it) }
                cityOutlineDecor.areaHeat?.let { m.overlays.remove(it) }
                cityOutlineDecor.dim?.let { m.overlays.remove(it) }
                cityOutlineDecor.edge?.let { m.overlays.remove(it) }
                cityOutlineDecor.regionLabels = null
                cityOutlineDecor.cityName = null
                cityOutlineDecor.clusters = null
                cityOutlineDecor.areaHeat = null
                cityOutlineDecor.dim = null
                cityOutlineDecor.edge = null
            }
        }
    }
    /**
     * Apply heat toggle immediately. Relying only on [AndroidView] update can miss the change
     * until the next map touch (which also refreshes overlays).
     */
    LaunchedEffect(areaHeatEnabled, mapSessionId) {
        var map = mapHolder.map
        var tries = 0
        while (map == null && tries < 30) {
            delay(16)
            map = mapHolder.map
            tries++
        }
        map ?: return@LaunchedEffect
        cityOutlineDecor.areaHeat?.drawSuppressed = false
        refreshReportClusterOverlayOnly(
            map = map,
            selectedCity = selectedCity,
            decor = cityOutlineDecor,
            mapSeverityFilter = mapSeverityFilter,
            heatEnabled = areaHeatEnabled,
        )
        lastAreaHeatEnabled.value = areaHeatEnabled
        map.invalidate()
        map.postInvalidate()
    }
    val zoomHideJob = remember { object { var job: Job? = null } }
    val programmaticCameraMoveRef = remember { booleanArrayOf(false) }
    fun runProgrammaticCamera(action: (MapView) -> Unit) {
        val map = mapHolder.map ?: return
        programmaticCameraMoveRef[0] = true
        action(map)
        map.post { programmaticCameraMoveRef[0] = false }
    }
    val touchCallbacks = remember {
        object {
            var onMapTouchChanged: (Boolean) -> Unit = {}
            var onMapPanEndedAtCenter: (Double, Double) -> Unit = { _, _ -> }
            var onMapZoomChanged: (MapView) -> Unit = {}
            var onMapOrientationChanged: (Float) -> Unit = {}
            var onCityViewZoomChanged: (Boolean) -> Unit = {}
            var onCityMapViewIdleCheck: (MapView) -> Unit = {}
        }
    }
    var showZoomControls by remember { mutableStateOf(false) }
    /** Map rotation in degrees (osmdroid); used by the compass rose. */
    var mapOrientationDeg by remember { mutableFloatStateOf(0f) }
    /** True when zoom is at the city overview floor (user has not zoomed in past city view). */
    var atCityOverviewZoom by remember { mutableStateOf(false) }
    /** True while a finger is on the map (pan / pinch / rotate). */
    var mapUserInteracting by remember { mutableStateOf(false) }
    /** City title in the center; shown only at city zoom when idle. */
    var revealCityLabel by remember { mutableStateOf(false) }
    /** After a successful ZoomOutMap tap; cleared when zoom reaches max again (pinch or "+"). */
    var zoomOutMapUsedSinceMax by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scheduleHideZoomControls: () -> Unit = {
        zoomHideJob.job?.cancel()
        zoomHideJob.job = coroutineScope.launch {
            delay(2400)
            showZoomControls = false
        }
    }
    val runFastMapAction: ((MapView) -> Unit) -> Unit = fun(action: (MapView) -> Unit) {
        val map = mapHolder.map ?: return
        // Temporarily suppress heavier overlays to make map-control taps feel snappier.
        cityOutlineDecor.regionLabels?.isEnabled = false
        cityOutlineDecor.cityName?.drawSuppressed = true
        labelOverlayRef[0]?.drawSuppressed = true
        cityOutlineDecor.areaHeat?.drawSuppressed = true
        action(map)
        map.invalidate()
        map.postDelayed({
            val chrome = cityMapChromeVisibleRef[0]
            cityOutlineDecor.regionLabels?.isEnabled = chrome
            cityOutlineDecor.cityName?.drawSuppressed = false
            labelOverlayRef[0]?.drawSuppressed = false
            cityOutlineDecor.areaHeat?.drawSuppressed = false
            map.invalidate()
        }, 72L)
    }
    touchCallbacks.onMapTouchChanged = { active ->
        mapUserInteracting = active
        onMapTouchChanged(active)
        zoomHideJob.job?.cancel()
        if (active) {
            showZoomControls = true
        } else {
            scheduleHideZoomControls()
        }
    }
    touchCallbacks.onMapPanEndedAtCenter = panEnd@{ lat, lng ->
        val map = mapHolder.map
        if (map != null) {
            map.clampViewportToMetroBounds(selectedCityHolder.city)
            if (!programmaticCameraMoveRef[0]) {
                onMapCameraSnapshot(map.mapCenter.latitude, map.mapCenter.longitude, map.zoomLevelDouble)
            }
            if (map.isAtCityOverviewZoom()) {
                if (!mapCameraUserPositioned && map.needsCityOverviewReframe(selectedCityHolder.city)) {
                    runProgrammaticCamera { it.scheduleFitCityOverview(selectedCityHolder.city, overviewRestore) }
                    return@panEnd
                }
            }
            onMapPanEndedAtCenter(map.mapCenter.latitude, map.mapCenter.longitude)
            return@panEnd
        }
        onMapPanEndedAtCenter(lat, lng)
    }
    touchCallbacks.onCityMapViewIdleCheck = { map ->
        map.clampViewportToMetroBounds(selectedCityHolder.city)
        if (!mapCameraUserPositioned &&
            map.isAtCityOverviewZoom() &&
            map.needsCityOverviewReframe(selectedCityHolder.city)
        ) {
            runProgrammaticCamera { it.scheduleFitCityOverview(selectedCityHolder.city, overviewRestore) }
        }
    }
    touchCallbacks.onMapZoomChanged = { map ->
        if (!programmaticCameraMoveRef[0]) {
            onMapCameraSnapshot(map.mapCenter.latitude, map.mapCenter.longitude, map.zoomLevelDouble)
        }
        if (map.isAtMaxZoom()) {
            zoomOutMapUsedSinceMax = false
        }
        refreshReportClusterOverlayOnly(
            map = map,
            selectedCity = selectedCityHolder.city,
            decor = cityOutlineDecor,
            mapSeverityFilter = mapSeverityFilterHolder.filter,
            heatEnabled = areaHeatEnabledHolder.enabled,
        )
    }
    touchCallbacks.onMapOrientationChanged = { mapOrientationDeg = it }
    touchCallbacks.onCityViewZoomChanged = { atCity ->
        atCityOverviewZoom = atCity
        cityMapChromeVisibleRef[0] = atCity
        applyCityMapChromeVisibility(cityOutlineDecor, atCity)
        mapHolder.map?.invalidate()
    }

    LaunchedEffect(mapUserInteracting, atCityOverviewZoom, selectedCity) {
        if (!atCityOverviewZoom || mapUserInteracting) {
            revealCityLabel = false
            return@LaunchedEffect
        }
        delay(400)
        if (atCityOverviewZoom && !mapUserInteracting) {
            revealCityLabel = true
        }
    }

    LaunchedEffect(Unit) {
        val config = Configuration.getInstance()
        config.load(
            context.applicationContext,
            context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )
        config.userAgentValue = context.packageName
        config.tileDownloadThreads = 4
        config.tileFileSystemThreads = 4
        config.setCacheMapTileCount(64)
        config.setCacheMapTileOvershoot(128)
        config.setDebugMapView(false)
        config.setDebugTileProviders(false)
        config.setDebugMode(false)
        config.save(
            context.applicationContext,
            context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )
    }

    /** City picker change â†’ full city bbox view (not street/locate). */
    LaunchedEffect(selectedCity, mapSessionId) {
        zoomOutMapUsedSinceMax = false
        val map = mapHolder.map ?: return@LaunchedEffect
        if (mapLocateEpoch > lastConsumedMapLocateEpoch) return@LaunchedEffect
        if (mapCameraUserPositioned) return@LaunchedEffect
        runProgrammaticCamera { it.scheduleFitCityOverview(selectedCity, overviewRestore) }
    }

    /** Locate button â†’ street-level track view (no city chrome). */
    LaunchedEffect(mapLocateEpoch, lastConsumedMapLocateEpoch) {
        if (mapLocateEpoch <= lastConsumedMapLocateEpoch) return@LaunchedEffect
        val map = mapHolder.map ?: return@LaunchedEffect
        onMapLocateEpochConsumed(mapLocateEpoch)
        if (!shouldFocusUserGps(selectedCity, userLocation)) return@LaunchedEffect
        val loc = userLocation ?: return@LaunchedEffect
        runProgrammaticCamera { it.scheduleFocusOnGps(selectedCity, loc) }
    }

    /** Map drag pick: keep zoom, only sync center/bounds. */
    LaunchedEffect(
        userLocation?.latitude,
        userLocation?.longitude,
        userLocation?.provider,
    ) {
        val map = mapHolder.map ?: return@LaunchedEffect
        if (!shouldPreserveZoomForMapPick(selectedCity, userLocation)) return@LaunchedEffect
        if (mapCameraUserPositioned) return@LaunchedEffect
        runProgrammaticCamera {
            it.scheduleApplyMetroRegion(selectedCity, userLocation, preserveZoom = true)
        }
    }

    Box(
        modifier = modifier
            .background(Color.White)
            .clipToBounds()
    ) {
        val mapFabGap = 8.dp
        val mapFabShape = RoundedCornerShape(10.dp)
        val mapFabIcon = 16.dp
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(AndroidColor.WHITE)
                    setTileSource(CartoPositronNoLabels)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = false
                    setHorizontalMapRepetitionEnabled(false)
                    setVerticalMapRepetitionEnabled(false)
                    maxZoomLevel = 19.0
                    @Suppress("DEPRECATION")
                    setBuiltInZoomControls(false)
                    applyMetroPanBounds(selectedCityHolder.city)
                    overlayManager.tilesOverlay.apply {
                        setLoadingBackgroundColor(AndroidColor.WHITE)
                        setLoadingLineColor(AndroidColor.TRANSPARENT)
                        setColorFilter(null)
                    }
                    val slop = ViewConfiguration.get(ctx).scaledTouchSlop
                    val slopSq = slop * slop
                    val gpsTouchRevealHitSq = (46f * ctx.resources.displayMetrics.density).let { it * it }
                    var panDownX = 0f
                    var panDownY = 0f
                    var panStartLat = 0.0
                    var panStartLon = 0.0
                    var panExceededSlop = false
                    /** True if 2+ fingers touched in this gesture â€” avoids treating pinch-zoom as a pan. */
                    var gestureHadMultiplePointers = false
                    var scrollParentNotified = false
                    fun suspendHeavyOverlaysFromTouch() {
                        cityOutlineDecor.dim?.isEnabled = false
                        cityOutlineDecor.edge?.isEnabled = false
                        cityOutlineDecor.regionLabels?.isEnabled = false
                        cityOutlineDecor.clusters?.isEnabled = false
                        cityOutlineDecor.cityName?.drawSuppressed = true
                        labelOverlayRef[0]?.drawSuppressed = true
                        cityOutlineDecor.areaHeat?.drawSuppressed = true
                    }
                    val resumeHeavyOverlaysRunnable = Runnable {
                        val chrome = cityMapChromeVisibleRef[0]
                        cityOutlineDecor.dim?.isEnabled = chrome
                        cityOutlineDecor.edge?.isEnabled = chrome
                        cityOutlineDecor.regionLabels?.isEnabled = chrome
                        cityOutlineDecor.clusters?.isEnabled = true
                        cityOutlineDecor.cityName?.drawSuppressed = false
                        labelOverlayRef[0]?.drawSuppressed = false
                        cityOutlineDecor.areaHeat?.drawSuppressed = false
                        invalidate()
                    }
                    val cityReframeAfterIdle = Runnable {
                        touchCallbacks.onCityMapViewIdleCheck(this@apply)
                    }
                    val endParentScrollRunnable = Runnable {
                        if (!scrollParentNotified) return@Runnable
                        scrollParentNotified = false
                        touchCallbacks.onMapTouchChanged(false)
                        removeCallbacks(cityReframeAfterIdle)
                        postDelayed(cityReframeAfterIdle, 160L)
                    }
                    mapTouchIdleRunnables[0] = resumeHeavyOverlaysRunnable
                    mapTouchIdleRunnables[1] = endParentScrollRunnable
                    setOnTouchListener { v, event ->
                        val mapView = v as MapView
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                gestureHadMultiplePointers = false
                                panDownX = event.x
                                panDownY = event.y
                                val c = mapView.mapCenter
                                panStartLat = c.latitude
                                panStartLon = c.longitude
                                panExceededSlop = false
                            }
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                gestureHadMultiplePointers = true
                                panExceededSlop = false
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (event.pointerCount > 1) gestureHadMultiplePointers = true
                                if (event.pointerCount == 1) {
                                    val dx = event.x - panDownX
                                    val dy = event.y - panDownY
                                    if (dx * dx + dy * dy > slopSq) panExceededSlop = true
                                }
                            }
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> {
                                if (event.actionMasked == MotionEvent.ACTION_UP &&
                                    !gestureHadMultiplePointers &&
                                    event.pointerCount == 1
                                ) {
                                    val end = mapView.mapCenter
                                    if (mapView.isAtCityOverviewZoom()) {
                                        touchCallbacks.onMapPanEndedAtCenter(end.latitude, end.longitude)
                                    } else if (panExceededSlop) {
                                        val centerMoved =
                                            abs(end.latitude - panStartLat) > 1e-5 ||
                                                abs(end.longitude - panStartLon) > 1e-5
                                        if (centerMoved) {
                                            touchCallbacks.onMapPanEndedAtCenter(end.latitude, end.longitude)
                                        }
                                    }
                                }
                                if (event.actionMasked == MotionEvent.ACTION_UP &&
                                    !gestureHadMultiplePointers &&
                                    event.pointerCount == 1
                                ) {
                                    val gLoc = latestGpsPinForTouch[0]
                                    if (gLoc != null) {
                                        val p = Point()
                                        mapView.projection.toPixels(GeoPoint(gLoc.latitude, gLoc.longitude), p)
                                        val tdx = event.x - p.x
                                        val tdy = event.y - p.y
                                        if (tdx * tdx + tdy * tdy <= gpsTouchRevealHitSq) {
                                            gpsTouchRevealTick.intValue++
                                        }
                                    }
                                }
                            }
                        }
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                v.removeCallbacks(resumeHeavyOverlaysRunnable)
                                v.removeCallbacks(endParentScrollRunnable)
                                suspendHeavyOverlaysFromTouch()
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                if (!scrollParentNotified) {
                                    scrollParentNotified = true
                                    touchCallbacks.onMapTouchChanged(true)
                                }
                            }
                            MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                                v.removeCallbacks(resumeHeavyOverlaysRunnable)
                                v.postDelayed(resumeHeavyOverlaysRunnable, 88L)
                                if (scrollParentNotified) {
                                    v.removeCallbacks(endParentScrollRunnable)
                                    v.postDelayed(endParentScrollRunnable, 44L)
                                }
                            }
                        }
                        false
                    }
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            clampViewportToMetroBounds(selectedCityHolder.city)
                            if (!programmaticCameraMoveRef[0]) {
                                onMapCameraSnapshot(
                                    mapCenter.latitude,
                                    mapCenter.longitude,
                                    zoomLevelDouble,
                                )
                            }
                            if (isAtCityOverviewZoom()) {
                                removeCallbacks(cityReframeAfterIdle)
                                postDelayed(cityReframeAfterIdle, 180L)
                            }
                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            clampViewportToMetroBounds(selectedCityHolder.city)
                            touchCallbacks.onMapZoomChanged(this@apply)
                            val atCity = isAtCityOverviewZoom()
                            touchCallbacks.onCityViewZoomChanged(atCity)
                            if (atCity) {
                                post { touchCallbacks.onCityMapViewIdleCheck(this@apply) }
                            }
                            return false
                        }
                    })
                    val choreographer = Choreographer.getInstance()
                    val orientationFrame = object : Choreographer.FrameCallback {
                        private var lastSent = Float.NaN
                        private var lastCityView: Boolean? = null
                        override fun doFrame(frameTimeNanos: Long) {
                            if (!isAttachedToWindow) return
                            val o = mapOrientation
                            if (lastSent.isNaN() || abs(o - lastSent) > 0.08f) {
                                lastSent = o
                                touchCallbacks.onMapOrientationChanged(o)
                            }
                            val z = zoomLevelDouble
                            val floor = minZoomLevel
                            val atCity = (z - floor) <= CITY_VIEW_ZOOM_EPSILON
                            if (lastCityView != atCity) {
                                lastCityView = atCity
                                touchCallbacks.onCityViewZoomChanged(atCity)
                            }
                            choreographer.postFrameCallback(this)
                        }
                    }
                    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            choreographer.postFrameCallback(orientationFrame)
                        }

                        override fun onViewDetachedFromWindow(v: View) {
                            choreographer.removeFrameCallback(orientationFrame)
                        }
                    })
                    if (isAttachedToWindow) {
                        choreographer.postFrameCallback(orientationFrame)
                    }
                    mapHolder.map = this
                    mapViewRef[0] = this
                    val restoreLat = savedMapCenterLat
                    val restoreLon = savedMapCenterLon
                    val restoreZoom = savedMapZoom
                    if (mapCameraUserPositioned && restoreLat != null && restoreLon != null && restoreZoom != null) {
                        programmaticCameraMoveRef[0] = true
                        controller.setCenter(GeoPoint(restoreLat, restoreLon))
                        controller.setZoom(restoreZoom)
                        post { programmaticCameraMoveRef[0] = false }
                    } else {
                        programmaticCameraMoveRef[0] = true
                        scheduleFitCityOverview(selectedCity, overviewRestore)
                        post { programmaticCameraMoveRef[0] = false }
                    }
                }
            },
            update = { view ->
                view.applyMetroPanBounds(selectedCityHolder.city)
                if (lastOutlineCity.value != selectedCity) {
                    syncCityOutlineDecorations(
                        map = view,
                        selectedCity = selectedCity,
                        decor = cityOutlineDecor,
                        mapSeverityFilter = mapSeverityFilter,
                        heatEnabled = areaHeatEnabledHolder.enabled,
                    )
                    refreshAreaHeatOverlay(
                        map = view,
                        selectedCity = selectedCity,
                        decor = cityOutlineDecor,
                        mapSeverityFilter = mapSeverityFilter,
                        heatEnabled = areaHeatEnabledHolder.enabled,
                    )
                    cityOutlineDecor.clusters?.let { clusters ->
                        view.overlays.remove(clusters)
                        view.overlays.add(clusters)
                    }
                    view.clampViewportToMetroBounds(selectedCity)
                    lastOutlineCity.value = selectedCity
                    lastSyncedReportEpoch.intValue = recentReportsEpoch
                    lastMapSeverityFilter.value = mapSeverityFilter
                    lastAreaHeatEnabled.value = areaHeatEnabledHolder.enabled
                } else if (
                    lastSyncedReportEpoch.intValue != recentReportsEpoch ||
                    lastMapSeverityFilter.value != mapSeverityFilter
                ) {
                    refreshReportClusterOverlayOnly(
                        map = view,
                        selectedCity = selectedCity,
                        decor = cityOutlineDecor,
                        mapSeverityFilter = mapSeverityFilter,
                        heatEnabled = areaHeatEnabledHolder.enabled,
                    )
                    lastSyncedReportEpoch.intValue = recentReportsEpoch
                    lastMapSeverityFilter.value = mapSeverityFilter
                }
                // Backup path if AndroidView update runs; primary apply is LaunchedEffect(areaHeatEnabled).
                if (areaHeatEnabled != lastAreaHeatEnabled.value) {
                    cityOutlineDecor.areaHeat?.drawSuppressed = false
                    refreshReportClusterOverlayOnly(
                        map = view,
                        selectedCity = selectedCity,
                        decor = cityOutlineDecor,
                        mapSeverityFilter = mapSeverityFilter,
                        heatEnabled = areaHeatEnabled,
                    )
                    lastAreaHeatEnabled.value = areaHeatEnabled
                    view.invalidate()
                    view.postInvalidate()
                }
                labelOverlayRef[0] = cityOutlineDecor.regionLabels
                cityOutlineDecor.cityName?.let { cityLabel ->
                    val show = revealCityLabel && selectedCity.isNotBlank()
                    if (cityLabel.labelVisible != show) {
                        cityLabel.labelVisible = show
                        view.invalidate()
                    }
                }
                val gps = gpsPinLocation
                val slot = gpsMarkerRef[0]
                when {
                    gps == null -> {
                        cancelGpsPinAutoHide(view, gpsPinHideRunnable)
                        lastGpsPinScheduleKey.value = null
                        lastMapLocateEpochForGpsPin.intValue = mapLocateEpoch
                        if (slot != null) {
                            view.overlays.remove(slot)
                            gpsMarkerRef[0] = null
                            view.invalidate()
                        }
                    }
                    slot == null -> {
                        val m = Marker(view).apply {
                            position = GeoPoint(gps.latitude, gps.longitude)
                            title = "Your location (GPS)"
                            snippet = "From device GPS"
                            setIcon(buildGpsPinDrawable(view.context))
                            setInfoWindow(null)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            isEnabled = true
                        }
                        gpsMarkerRef[0] = m
                        view.overlays.add(m)
                        view.invalidate()
                    }
                    else -> {
                        val next = GeoPoint(gps.latitude, gps.longitude)
                        if (abs(slot.position.latitude - next.latitude) > 1e-7 ||
                            abs(slot.position.longitude - next.longitude) > 1e-7
                        ) {
                            slot.position = next
                            view.invalidate()
                        }
                    }
                }
                latestGpsPinForTouch[0] = gps?.let { Location(it) }
                if (gps != null) {
                    val marker = gpsMarkerRef[0]
                    if (marker != null) {
                        var scheduled = false
                        if (mapLocateEpoch != lastMapLocateEpochForGpsPin.intValue) {
                            lastMapLocateEpochForGpsPin.intValue = mapLocateEpoch
                            marker.isEnabled = true
                            scheduled = true
                        }
                        if (gpsTouchRevealTick.intValue > lastTouchRevealConsumed.intValue) {
                            lastTouchRevealConsumed.intValue = gpsTouchRevealTick.intValue
                            marker.isEnabled = true
                            scheduled = true
                        }
                        val key = String.format(Locale.US, "%.5f,%.5f", gps.latitude, gps.longitude)
                        if (key != lastGpsPinScheduleKey.value) {
                            lastGpsPinScheduleKey.value = key
                            marker.isEnabled = true
                            scheduled = true
                        }
                        if (scheduled) {
                            scheduleGpsPinAutoHide(view, gpsMarkerRef, gpsPinHideRunnable)
                        }
                    }
                }
            }
        )

        val mapCompassSize = 38.dp
        val mapCompassLabelN = 9.sp
        val mapCompassLabelOther = 7.sp
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 5.dp, top = 5.dp)
                .size(mapCompassSize)
                .semantics {
                    contentDescription =
                        "Map compass; letters show north, east, south, and west relative to the map."
                },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.94f),
            shadowElevation = 3.dp,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp, vertical = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = -mapOrientationDeg },
                ) {
                    Text(
                        "N",
                        modifier = Modifier.align(Alignment.TopCenter),
                        fontSize = mapCompassLabelN,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C),
                        lineHeight = mapCompassLabelN,
                    )
                    Text(
                        "S",
                        modifier = Modifier.align(Alignment.BottomCenter),
                        fontSize = mapCompassLabelOther,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = mapCompassLabelOther,
                    )
                    Text(
                        "W",
                        modifier = Modifier.align(Alignment.CenterStart),
                        fontSize = mapCompassLabelOther,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = mapCompassLabelOther,
                    )
                    Text(
                        "E",
                        modifier = Modifier.align(Alignment.CenterEnd),
                        fontSize = mapCompassLabelOther,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = mapCompassLabelOther,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 5.dp, end = 5.dp)
                .semantics {
                    contentDescription = "GPS accuracy: $gpsAccuracyText"
                },
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.94f),
            shadowElevation = 3.dp,
        ) {
            Text(
                gpsAccuracyText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4B5563),
                maxLines = 1,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(mapFabGap),
        ) {
            AnimatedVisibility(
                visible = showZoomControls,
                enter = fadeIn(animationSpec = tween(180)) +
                    scaleIn(initialScale = 0.72f, animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(160)) +
                    scaleOut(targetScale = 0.72f, animationSpec = tween(180)),
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(mapFabGap),
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            runFastMapAction { it.controller.zoomIn() }
                            scheduleHideZoomControls()
                        },
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = mapFabShape,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Zoom in",
                            modifier = Modifier.size(mapFabIcon),
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            runFastMapAction { it.zoomOutWithinCityView() }
                            scheduleHideZoomControls()
                        },
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = mapFabShape,
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Zoom out (to city view at most)",
                            modifier = Modifier.size(mapFabIcon),
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            onMapViewControlUsed()
                            onResetMapCamera()
                            runProgrammaticCamera { it.scheduleFitCityOverview(selectedCity, overviewRestore) }
                            scheduleHideZoomControls()
                        },
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = mapFabShape,
                    ) {
                        Icon(
                            Icons.Outlined.Map,
                            contentDescription = "Show full city map",
                            modifier = Modifier.size(mapFabIcon),
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            val map = mapHolder.map
                            if (map == null || zoomOutMapUsedSinceMax || !map.isAtMaxZoom()) {
                                scheduleHideZoomControls()
                                return@SmallFloatingActionButton
                            }
                            if (run {
                                    var applied = false
                                    runFastMapAction {
                                        applied = it.applyMetroDefaultZoom()
                                    }
                                    applied
                                }
                            ) {
                                zoomOutMapUsedSinceMax = true
                            }
                            scheduleHideZoomControls()
                        },
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = mapFabShape,
                    ) {
                        Icon(
                            Icons.Outlined.ZoomOutMap,
                            contentDescription = "Zoom out four steps (once until fully zoomed in)",
                            modifier = Modifier.size(mapFabIcon),
                        )
                    }
                }
            }
            SmallFloatingActionButton(
                onClick = { onLocateMe() },
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = mapFabShape,
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Trace to current GPS location",
                    modifier = Modifier.size(mapFabIcon),
                )
            }
        }


    }
}


private enum class MyReportsStatFilter {
    TOTAL,
    COMPLETED,
    IN_PROGRESS,
    OPEN,
}

private fun PersistedPotholeReport.matchesMyReportsFilter(filter: MyReportsStatFilter): Boolean =
    when (filter) {
        MyReportsStatFilter.TOTAL -> true
        MyReportsStatFilter.COMPLETED -> status == PotholeReportStatus.COMPLETED
        MyReportsStatFilter.IN_PROGRESS -> status == PotholeReportStatus.IN_PROGRESS
        MyReportsStatFilter.OPEN -> status == PotholeReportStatus.OPEN
    }

@Composable
private fun MyReportsSection(
    reporterUserId: String,
    recentReportsEpoch: Int,
    onReportsMutated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<PersistedPotholeReport?>(null) }
    var detailReport by remember { mutableStateOf<PersistedPotholeReport?>(null) }
    var statFilter by rememberSaveable { mutableStateOf(MyReportsStatFilter.TOTAL) }
    val titleColor = MaterialTheme.colorScheme.onBackground
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardColor = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outlineVariant
    val emptyCardColor = MaterialTheme.colorScheme.surfaceVariant
    var resolvedReporterId by remember(reporterUserId) { mutableStateOf(reporterUserId) }

    LaunchedEffect(reporterUserId) {
        if (reporterUserId.isBlank()) return@LaunchedEffect
        val stableId = withContext(Dispatchers.IO) {
            val stable = com.example.potholereport.data.remote.CitizenProfileRepository
                .resolveReporterUserId(reporterUserId)
            if (stable != reporterUserId) {
                RecentReportsRepository.migrateReporterUserId(reporterUserId, stable)
            }
            stable
        }
        resolvedReporterId = stableId
        val changed = withContext(Dispatchers.IO) {
            RecentReportsRepository.syncSignedInReportsFromSupabase(stableId)
        }
        if (changed) onReportsMutated()
    }

    val myReports = remember(recentReportsEpoch, resolvedReporterId) {
        RecentReportsRepository.signedInReportsOrdered(resolvedReporterId)
    }
    val total = myReports.size
    val completedCount = myReports.count { it.status == PotholeReportStatus.COMPLETED }
    val inProgressCount = myReports.count { it.status == PotholeReportStatus.IN_PROGRESS }
    val openCount = myReports.count { it.status == PotholeReportStatus.OPEN }
    val filteredReports = remember(myReports, statFilter) {
        myReports.filter { it.matchesMyReportsFilter(statFilter) }
    }
    val resolutionPct = if (total == 0) 0 else (completedCount * 100) / total

    detailReport?.let { report ->
        ReportDetailDialog(
            report = report,
            onDismiss = { detailReport = null },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this report?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "This removes the photo and report from your device only. " +
                        "It stays in the municipality system if it was already uploaded.",
                    fontSize = 13.sp,
                    color = subtitleColor,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = target.id
                        deleteTarget = null
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                RecentReportsRepository.deleteSignedInReport(id, resolvedReporterId)
                            }
                            if (ok) onReportsMutated()
                        }
                    },
                ) {
                    Text("Delete", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        StatCard(
            title = "TOTAL",
            value = total.toString(),
            color = Color(0xFF14233D),
            selected = statFilter == MyReportsStatFilter.TOTAL,
            onClick = { statFilter = MyReportsStatFilter.TOTAL },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        StatCard(
            title = "COMPLETED",
            value = completedCount.toString(),
            color = Color(0xFF0B7A42),
            selected = statFilter == MyReportsStatFilter.COMPLETED,
            onClick = { statFilter = MyReportsStatFilter.COMPLETED },
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        StatCard(
            title = "IN PROGRESS",
            value = inProgressCount.toString(),
            color = Color(0xFFBC7A1E),
            selected = statFilter == MyReportsStatFilter.IN_PROGRESS,
            onClick = { statFilter = MyReportsStatFilter.IN_PROGRESS },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        StatCard(
            title = "OPEN",
            value = openCount.toString(),
            color = Color(0xFFA12A2A),
            selected = statFilter == MyReportsStatFilter.OPEN,
            onClick = { statFilter = MyReportsStatFilter.OPEN },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(12.dp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, cardBorder),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RESOLUTION RATE", fontWeight = FontWeight.Medium, color = subtitleColor, fontSize = 11.sp)
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(10.dp))
            Text("$resolutionPct%", fontWeight = FontWeight.Bold, color = titleColor, fontSize = 11.sp)
        }
    }

    Spacer(Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("YOUR REPORTS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = titleColor)
        Text(
            when {
                total == 0 -> "NONE YET"
                statFilter == MyReportsStatFilter.TOTAL -> "$total Â· NEWEST FIRST"
                else -> "${filteredReports.size} SHOWN"
            },
            fontSize = 12.sp,
            color = subtitleColor
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cardBorder)
    Spacer(Modifier.height(12.dp))

    if (myReports.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, cardBorder),
            colors = CardDefaults.cardColors(containerColor = emptyCardColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(12.dp))
                Text("You haven't reported any potholes yet.", color = titleColor, fontSize = 14.sp)
                Text(
                    "Reports submitted while signed in will appear here.",
                    color = subtitleColor,
                    fontSize = 12.sp
                )
            }
        }
    } else if (filteredReports.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, cardBorder),
            colors = CardDefaults.cardColors(containerColor = emptyCardColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No reports in this category.",
                    color = titleColor,
                    fontSize = 14.sp,
                )
                Text(
                    "Tap another stat box to view other reports.",
                    color = subtitleColor,
                    fontSize = 12.sp,
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(filteredReports, key = { it.id }) { report ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(118.dp)) {
                        RecentReportThumbnail(
                            report,
                            onClick = { detailReport = report },
                        )
                        IconButton(
                            onClick = { deleteTarget = report },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(28.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xCC1F2937), shape = RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete report",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun HomeTabBannerAndTabs(
    isSignedIn: Boolean,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "REPORT ANONYMOUSLY",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Spot it. Snap it. Submit it.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        TopTab(
            title = "REPORT & TRACK",
            selected = selectedTabIndex == 0,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(0) },
        )
        TopTab(
            title = "ACCOUNTABILITY",
            selected = selectedTabIndex == 1,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(1) },
        )
        if (isSignedIn) {
            TopTab(
                title = "MY REPORTS",
                selected = selectedTabIndex == 2,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(2) },
            )
        }
    }
}

@Composable
private fun statAccentColor(accent: Color): Color {
    if (!isSystemInDarkTheme()) return accent
    val luminance = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    if (luminance >= 0.4f) return accent
    return Color(
        red = accent.red + (1f - accent.red) * 0.62f,
        green = accent.green + (1f - accent.green) * 0.62f,
        blue = accent.blue + (1f - accent.blue) * 0.62f,
        alpha = accent.alpha,
    )
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accent = statAccentColor(color)
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                accent.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = accent)
            Text(
                title, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), 
                letterSpacing = 1.sp, 
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MyReportsTabFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFFE2E2E2),
        )
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "REPORTS STAY ANONYMOUS TO OTHER USERS â€¢ ONLY YOU SEE THIS VIEW",
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = Color(0xFF7B7B7B),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "CITY GRID â€¢ CIVIC REPORTING",
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = Color(0xFF7B7B7B),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = Color(0xFF7B7B7B),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun isDeviceLocationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

private fun openLocationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun locationQualityScore(location: Location): Float {
    val ageSec = ((System.currentTimeMillis() - location.time).coerceAtLeast(0L) / 1000f)
        .coerceAtMost(240f)
    // Lower score is better: prioritize accuracy, then freshness.
    return location.accuracy.coerceAtLeast(1f) + ageSec * 0.30f
}

private fun shouldSeekMorePrecision(location: Location?): Boolean {
    if (location == null) return true
    val ageMs = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
    return location.accuracy > 16f || ageMs > 15_000L
}

private fun chooseBetterLocation(current: Location?, candidate: Location?): Location? {
    if (candidate == null) return current
    if (current == null) return candidate
    return if (locationQualityScore(candidate) < locationQualityScore(current)) candidate else current
}

@SuppressLint("MissingPermission")
private suspend fun requestOneFreshLocation(
    context: Context,
    locationManager: LocationManager,
    provider: String,
    timeoutMs: Long,
): Location? {
    return withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            val cancellationSignal = CancellationSignal()
            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    if (!cont.isCompleted) cont.resume(location)
                }
            } catch (_: Exception) {
                if (!cont.isCompleted) cont.resume(null)
            }
            cont.invokeOnCancellation {
                runCatching { cancellationSignal.cancel() }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun fetchCalibratedLocation(context: Context): Location? = withContext(Dispatchers.Default) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return@withContext null

        var best: Location? = null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        for (provider in providers) {
            val last = runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            best = chooseBetterLocation(best, last)
        }

        if (runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
            best = chooseBetterLocation(
                best,
                requestOneFreshLocation(
                    context = context,
                    locationManager = lm,
                    provider = LocationManager.GPS_PROVIDER,
                    timeoutMs = 6500L,
                ),
            )
        }
        if (runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
            best = chooseBetterLocation(
                best,
                requestOneFreshLocation(
                    context = context,
                    locationManager = lm,
                    provider = LocationManager.NETWORK_PROVIDER,
                    timeoutMs = 3000L,
                ),
            )
        }

        // If still coarse/stale, take one extra GPS refinement pass.
        if (shouldSeekMorePrecision(best) &&
            runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        ) {
            best = chooseBetterLocation(
                best,
                requestOneFreshLocation(
                    context = context,
                    locationManager = lm,
                    provider = LocationManager.GPS_PROVIDER,
                    timeoutMs = 2800L,
                ),
            )
        }
        best
    } catch (_: Exception) {
        null
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PotholeReportTheme {
        HomeScreen(
            onEmergencyClick = {},
            onOpenNewReport = { _ -> },
            isSignedIn = false,
            signedInEmail = "",
            anonymousUserId = "",
            avatarId = "neutral",
            onSignOut = {},
            onReportModalSignIn = { _, _ -> LoginResult.NO_ACCOUNT },
            onReportModalAuthSuccess = { _, _ -> },
            onReportModalStartSignup = { _, _, _ -> SignupStartResult.FAILED },
            onReportModalVerifyCode = { _, _ -> SignupVerifyResult.FAILED },
            onProfileSaved = { _, _ -> },
            onProfileStartEmailChange = { EmailChangeStartResult.FAILED },
            onProfileVerifyEmailChange = { _, _, _ -> EmailChangeVerifyResult.FAILED },
        )
    }
}
