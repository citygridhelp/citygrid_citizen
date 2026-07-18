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
            Text("$selectedCity - POTHOLE DENSITY", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
            "GPS +/-${loc.accuracy.toInt().coerceAtLeast(1)}m"
        } else {
            "GPS searching..."
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

/** Dim outside city + semi-dark red perimeter (no black stroke). */
private fun syncCityOutlineDecorations(
    map: MapView,
    selectedCity: String,
    decor: CityOutlineDecor,
    mapSeverityFilter: PotholeSeverity?,
    heatEnabled: Boolean = false,
) {
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
    // Place / street / water names come from CARTO light_all tiles (no seeded overlay).
    val cityLabel = CityNameOverlay(
        map,
        visualOutlineCentroidForCity(selectedCity),
        formatCityDisplayName(selectedCity),
    )
    decor.cityName = cityLabel
    map.overlays.add(2, cityLabel)
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

/** Red outline + dim only at city overview zoom. Area/street names come from basemap tiles. */
private fun applyCityMapChromeVisibility(decor: CityOutlineDecor, visible: Boolean) {
    decor.dim?.isEnabled = visible
    decor.edge?.isEnabled = visible
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
    maxZoomLevel = CartoBasemapUsefulMaxZoom
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

/** CARTO Positron with built-in place / street / water labels (no seeded coords). */
private val CartoPositronLabeled: XYTileSource = XYTileSource(
    "CartoPositronLabeled18",
    0,
    CartoBasemapUsefulMaxZoom.toInt(),
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/light_all/",
        "https://b.basemaps.cartocdn.com/light_all/",
        "https://c.basemaps.cartocdn.com/light_all/"
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
                cityOutlineDecor.cityName?.let { m.overlays.remove(it) }
                cityOutlineDecor.clusters?.let { m.overlays.remove(it) }
                cityOutlineDecor.areaHeat?.let { m.overlays.remove(it) }
                cityOutlineDecor.dim?.let { m.overlays.remove(it) }
                cityOutlineDecor.edge?.let { m.overlays.remove(it) }
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
        cityOutlineDecor.cityName?.drawSuppressed = true
        cityOutlineDecor.areaHeat?.drawSuppressed = true
        action(map)
        map.invalidate()
        map.postDelayed({
            cityOutlineDecor.cityName?.drawSuppressed = false
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
                    setTileSource(CartoPositronLabeled)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = false
                    setHorizontalMapRepetitionEnabled(false)
                    setVerticalMapRepetitionEnabled(false)
                    maxZoomLevel = CartoBasemapUsefulMaxZoom
                    @Suppress("DEPRECATION")
                    setBuiltInZoomControls(false)
                    applyMetroPanBounds(selectedCityHolder.city)
                    installCartoLabelDarkeningTilesOverlay()
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
                        cityOutlineDecor.clusters?.isEnabled = false
                        cityOutlineDecor.cityName?.drawSuppressed = true
                        cityOutlineDecor.areaHeat?.drawSuppressed = true
                    }
                    val resumeHeavyOverlaysRunnable = Runnable {
                        val chrome = cityMapChromeVisibleRef[0]
                        cityOutlineDecor.dim?.isEnabled = chrome
                        cityOutlineDecor.edge?.isEnabled = chrome
                        cityOutlineDecor.clusters?.isEnabled = true
                        cityOutlineDecor.cityName?.drawSuppressed = false
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
                statFilter == MyReportsStatFilter.TOTAL -> "$total - NEWEST FIRST"
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
                "REPORTS STAY ANONYMOUS TO OTHER USERS - ONLY YOU SEE THIS VIEW",
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = Color(0xFF7B7B7B),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "CITY GRID - CIVIC REPORTING",
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

