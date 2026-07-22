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
import android.os.SystemClock
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Point
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.potholereport.data.TRIP_POTHOLE_NOTIFY_WITHIN_M
import com.example.potholereport.data.TripNavigationMatcher
import com.example.potholereport.data.TripDeviceHeadingTracker
import com.example.potholereport.data.TripPlaceLabels
import com.example.potholereport.data.TripPotholeAlert
import com.example.potholereport.data.fuseNavHeadingDeg
import com.example.potholereport.data.withFusedNavHeading
import com.example.potholereport.data.TripRouteProgress
import com.example.potholereport.data.TripTurnGuidance
import com.example.potholereport.data.TripRecentPlace
import com.example.potholereport.data.TripRecentPlacesRepository
import com.example.potholereport.data.TripVoiceGuidance
import com.example.potholereport.data.formatRecentReportCaption
import com.example.potholereport.data.formatTripPlaceAge
import com.example.potholereport.data.formatTripPotholeAlertLine
import com.example.potholereport.data.notifyTripPotholeAlert
import com.example.potholereport.data.remote.OsrmRouteClient
import com.example.potholereport.data.remote.OsrmRouteStep
import com.example.potholereport.data.remote.TripVehicle
import com.example.potholereport.data.isMeaningfulAreaName
import com.example.potholereport.data.resolveAreaLabel
import com.example.potholereport.data.SignupStartResult
import com.example.potholereport.data.SignupVerifyResult
import com.example.potholereport.data.tripPotholeAlertAccentArgb
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

private val DarkBlue = Color(0xFF1A1A2E)

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
    /** Last fix from the device GPS pipeline only — never updated by map drag. */
    var lastGpsLocation by remember { mutableStateOf<Location?>(null) }
    /** Survives process death / Activity recreate so an active trip can resume routing. */
    var savedGpsLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedGpsLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedGpsAccuracyM by rememberSaveable { mutableStateOf<Float?>(null) }
    /** Enables frequent high-accuracy updates only while turn-by-turn map navigation is active. */
    var tripNavigationActive by remember { mutableStateOf(false) }
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
        savedGpsLat = location.latitude
        savedGpsLon = location.longitude
        savedGpsAccuracyM = if (location.hasAccuracy()) location.accuracy else null
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

    /**
     * Resolve My-location trip origin with utmost accuracy:
     * prefer the live map pin when it is a fresh GPS fix; refine with GPS-only
     * (never let a coarse network one-shot replace a good live pin).
     */
    suspend fun resolveMyLocationTripOrigin(): Location? {
        val live = lastGpsLocation?.let { Location(it) }
        val refined = fetchHighAccuracyGpsLocation(context, timeoutMs = 10_000L)
        val best = pickBestTripOrigin(livePin = live, freshGps = refined)
        if (best != null) {
            lastGpsLocation = Location(best)
            savedGpsLat = best.latitude
            savedGpsLon = best.longitude
            savedGpsAccuracyM = if (best.hasAccuracy()) best.accuracy else null
            userLocation = Location(best)
            gpsMetroCity = cityForLocation(best)
            mapLocateEpoch++
        }
        return best
    }

    /**
     * Same GPS path as the Locate button: prefer a fresh calibrated fix, then fall back
     * to the last known pin. Used so Trip "My location" start matches the blue locate pin.
     */
    suspend fun refreshCalibratedGpsPin(bumpMapLocate: Boolean = true): Location? {
        val live = lastGpsLocation?.let { Location(it) }
        val fresh = fetchHighAccuracyGpsLocation(context, timeoutMs = 8_000L)
            ?: fetchCalibratedLocation(context)
        val gps = pickBestTripOrigin(livePin = live, freshGps = fresh)
            ?: live
        if (gps != null) {
            lastGpsLocation = Location(gps)
            savedGpsLat = gps.latitude
            savedGpsLon = gps.longitude
            savedGpsAccuracyM = if (gps.hasAccuracy()) gps.accuracy else null
            userLocation = Location(gps)
            gpsMetroCity = cityForLocation(gps)
            val gpsCity = gpsMetroCity
            if (gpsCity != null && gpsCity != selectedCity && CityLaunchConfig.isCityEnabled(gpsCity)) {
                selectedCity = gpsCity
            }
            if (bumpMapLocate) mapLocateEpoch++
        }
        return gps
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
        // Restore last GPS after process death so an active trip can keep routing.
        if (lastGpsLocation == null) {
            val lat = savedGpsLat
            val lon = savedGpsLon
            if (lat != null && lon != null) {
                val restored = Location("saved").apply {
                    latitude = lat
                    longitude = lon
                    savedGpsAccuracyM?.let { accuracy = it }
                }
                lastGpsLocation = restored
                if (userLocation == null) userLocation = Location(restored)
                if (gpsMetroCity == null) gpsMetroCity = cityForLocation(restored)
            }
        }
        if (!hasLocationPermission(context) || !isDeviceLocationEnabled(context)) {
            showGpsEnableDialog = true
        } else {
            applyLocationFromDevice()
        }
    }

    @SuppressLint("MissingPermission")
    DisposableEffect(tripNavigationActive) {
        if (!tripNavigationActive || !hasLocationPermission(context) || !isDeviceLocationEnabled(context)) {
            onDispose { }
        } else {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListenerCompat {
                override fun onLocationChanged(location: Location) {
                    if (!shouldAcceptNavigationLocation(lastGpsLocation, location)) return
                    val accepted = Location(location)
                    lastGpsLocation = accepted
                    savedGpsLat = accepted.latitude
                    savedGpsLon = accepted.longitude
                    savedGpsAccuracyM = if (accepted.hasAccuracy()) accepted.accuracy else null
                    userLocation = Location(accepted)
                    gpsMetroCity = cityForLocation(accepted)
                }
            }
            if (runCatching {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                }.getOrDefault(false)
            ) {
                runCatching {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        500L,
                        0.5f,
                        listener,
                    )
                }
            }
            if (runCatching {
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                }.getOrDefault(false)
            ) {
                runCatching {
                    // Network is fallback-only; keep it slow so it cannot dominate the pin.
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5_000L,
                        8f,
                        listener,
                    )
                }
            }
            onDispose {
                runCatching { locationManager.removeUpdates(listener) }
            }
        }
    }

    LaunchedEffect(isSignedIn) {
        if (!isSignedIn && selectedTabIndex > 1) selectedTabIndex = 0
    }

    val scrollState = rememberScrollState()
    val myReportsScrollState = rememberScrollState()
    val isMyReportsTab = isSignedIn && selectedTabIndex == 2
    /** In-composition overlay — not a Dialog window (Dialog was minimizing / dismissing the app). */
    var mapFullscreen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = mapFullscreen) { mapFullscreen = false }

    Column(modifier = modifier.fillMaxSize()) {
        if (!mapFullscreen) {
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
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (!mapFullscreen) {
                HomeTabBannerAndTabs(
                    isSignedIn = isSignedIn,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                )
            }

            when {
                // Keep ReportAndTrack in one composition slot for expand/collapse so trip state survives.
                mapFullscreen || selectedTabIndex == 0 -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(
                                if (mapFullscreen) {
                                    Modifier
                                } else {
                                    Modifier.verticalScroll(scrollState, enabled = !mapTouchActive)
                                },
                            ),
                    ) {
                        if (!mapFullscreen) Spacer(Modifier.height(12.dp))
                        ReportAndTrackSection(
                            isSignedIn = isSignedIn,
                            onReportPotholeGuest = { showReportSignInModal = true },
                            onReportPotholeSignedIn = { onOpenNewReport(selectedCity) },
                            selectedCity = selectedCity,
                            onCitySelected = { selectedCity = it },
                            gpsMetroCity = gpsMetroCity,
                            userLocation = userLocation,
                            recentReportsEpoch = recentReportsEpoch,
                            onReportsMutated = onRecentReportsMutated,
                            mapFullscreen = mapFullscreen,
                            onMapFullscreenChange = { mapFullscreen = it },
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
                                        refreshCalibratedGpsPin(bumpMapLocate = true)
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
                            onEnsureFreshGpsForTrip = { resolveMyLocationTripOrigin() },
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
                            onTripNavigationActiveChanged = { tripNavigationActive = it },
                        )
                        if (!mapFullscreen) Spacer(Modifier.height(16.dp))
                    }
                }
                isMyReportsTab -> {
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
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState, enabled = !mapTouchActive),
                    ) {
                        Spacer(Modifier.height(12.dp))
                        AccountabilitySection(recentReportsEpoch = recentReportsEpoch)
                        Spacer(Modifier.height(16.dp))
                    }
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
    /** Same calibrated GPS refresh as Locate — used when trip start is "My location". */
    onEnsureFreshGpsForTrip: suspend () -> Location?,
    onTripNavigationActiveChanged: (Boolean) -> Unit,
    recentReportsEpoch: Int,
    onReportsMutated: () -> Unit = {},
    mapFullscreen: Boolean,
    onMapFullscreenChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val tripScope = rememberCoroutineScope()
    var tripNavigatePreparing by remember { mutableStateOf(false) }
    if (!mapFullscreen) {
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
    }

    var expanded by remember { mutableStateOf(false) }
    /** null = all severities on the map. */
    var mapSeverityFilterKey by rememberSaveable { mutableStateOf<String?>(null) }
    val mapSeverityFilter = remember(mapSeverityFilterKey) {
        mapSeverityFilterKey?.let { key -> PotholeSeverity.entries.find { it.name == key } }
    }
    var severityMenuExpanded by remember { mutableStateOf(false) }
    var areaHeatEnabled by rememberSaveable { mutableStateOf(false) }
    var tripModeEnabled by rememberSaveable { mutableStateOf(false) }
    var tripDestLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var tripDestLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var tripDestLabel by rememberSaveable { mutableStateOf("") }
    /** null lat/lon = start from current GPS location. */
    var tripStartLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var tripStartLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var tripStartLabel by rememberSaveable { mutableStateOf("") }
    /** Reverse-geocoded label for GPS start (“My location”). */
    var tripMyLocationLabel by remember { mutableStateOf("") }
    var tripVehicleName by rememberSaveable { mutableStateOf(TripVehicle.CAR.name) }
    val tripVehicle = remember(tripVehicleName) {
        TripVehicle.entries.find { it.name == tripVehicleName } ?: TripVehicle.CAR
    }
    /** False until user taps Navigate — preview route can show earlier; this starts turn-by-turn. */
    var tripNavActive by rememberSaveable { mutableStateOf(false) }
    /** Street-follow POV while navigating (close zoom, heading-up). 3D tilt deferred. */
    var tripStreetFollow by rememberSaveable { mutableStateOf(true) }
    /** Device compass heading (0–360°); turns the vehicle cursor when the phone turns. */
    var tripDeviceHeadingDeg by remember { mutableStateOf<Float?>(null) }
    DisposableEffect(tripNavActive) {
        if (!tripNavActive) {
            tripDeviceHeadingDeg = null
            onDispose { }
        } else {
            val tracker = TripDeviceHeadingTracker(context) { heading ->
                tripDeviceHeadingDeg = heading
            }
            tracker.start()
            onDispose {
                tracker.stop()
                tripDeviceHeadingDeg = null
            }
        }
    }
    /**
     * Locked origin for the active trip (usually the Locate-calibrated GPS at Navigate).
     * Prefer this over OSRM’s snapped first point so the start matches the user marker.
     */
    var tripNavOriginLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var tripNavOriginLon by rememberSaveable { mutableStateOf<Double?>(null) }
    // Warm high-accuracy GPS as soon as Trip mode is on so Navigate
    // starts from the same live pin the user already sees.
    DisposableEffect(tripModeEnabled, tripNavActive) {
        val needFineGps = tripModeEnabled || tripNavActive
        onTripNavigationActiveChanged(needFineGps)
        onDispose {
            onTripNavigationActiveChanged(false)
        }
    }
    val tripLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(tripLifecycleOwner, tripModeEnabled, tripNavActive) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && (tripModeEnabled || tripNavActive)) {
                onTripNavigationActiveChanged(true)
            }
        }
        tripLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { tripLifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val tripStartUsesMyLocation = tripStartLat == null && tripStartLon == null
    val heatAvailable = CityMetroKeys.canonical(selectedCity) == "BENGALURU"
    LaunchedEffect(heatAvailable) {
        if (!heatAvailable) areaHeatEnabled = false
    }
    LaunchedEffect(tripModeEnabled) {
        if (!tripModeEnabled) {
            tripDestLat = null
            tripDestLon = null
            tripDestLabel = ""
            tripStartLat = null
            tripStartLon = null
            tripStartLabel = ""
            tripMyLocationLabel = ""
            tripNavOriginLat = null
            tripNavOriginLon = null
            tripNavActive = false
        }
    }
    LaunchedEffect(
        tripModeEnabled,
        tripStartUsesMyLocation,
        gpsPinLocation?.latitude,
        gpsPinLocation?.longitude,
    ) {
        if (!tripModeEnabled || !tripStartUsesMyLocation) return@LaunchedEffect
        val gps = gpsPinLocation
        if (gps == null) {
            tripMyLocationLabel = "My location (searching…)"
            return@LaunchedEffect
        }
        val label = withContext(Dispatchers.IO) {
            reverseGeocodeTripPlace(context, gps.latitude, gps.longitude)
        }
        tripMyLocationLabel = label.ifBlank { "My location" }
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

    if (!mapFullscreen) {
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
    }

    val mapSeverityFilterLabel = mapSeverityFilter?.title ?: "All"
    val gpsAccuracyText = remember(gpsPinLocation?.accuracy, gpsPinLocation?.time) {
        val loc = gpsPinLocation
        if (loc != null && loc.hasAccuracy()) {
            "GPS +/-${loc.accuracy.toInt().coerceAtLeast(1)}m"
        } else {
            "GPS searching..."
        }
    }
    if (!mapFullscreen) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
        FilterChip(
            selected = tripModeEnabled,
            onClick = { tripModeEnabled = !tripModeEnabled },
            label = {
                Text("Trip", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            shape = RoundedCornerShape(50),
            border = BorderStroke(
                1.dp,
                if (tripModeEnabled) Color(0xFFB91C1C) else Color(0xFF9CA3AF),
            ),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = MaterialTheme.colorScheme.onBackground,
                iconColor = MaterialTheme.colorScheme.onBackground,
                selectedContainerColor = Color(0xFFB91C1C),
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White,
            ),
            modifier = Modifier.padding(end = 8.dp),
        )
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
    }

    var tripAlerts by remember { mutableStateOf<List<TripPotholeAlert>>(emptyList()) }
    var tripRoutePoints by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var tripRouteSteps by remember { mutableStateOf<List<OsrmRouteStep>>(emptyList()) }
    var tripNextTurn by remember { mutableStateOf<TripTurnGuidance?>(null) }
    var tripRouteProgress by remember { mutableStateOf<TripRouteProgress?>(null) }
    var tripRouteDurationSec by remember { mutableStateOf<Double?>(null) }
    var tripRouteStatus by remember { mutableStateOf(TripRouteStatus.IDLE) }
    var tripArrived by remember { mutableStateOf(false) }
    var tripRerouting by remember { mutableStateOf(false) }
    var tripRouteRequestEpoch by remember { mutableIntStateOf(0) }
    var rerouteOriginLat by remember { mutableStateOf<Double?>(null) }
    var rerouteOriginLon by remember { mutableStateOf<Double?>(null) }
    var lastRerouteAtMs by remember { mutableStateOf(0L) }
    var tripStartCorrectionDone by remember { mutableStateOf(false) }
    val announcedPotholeIds = remember { mutableSetOf<Long>() }
    val bbox = remember(selectedCity) { metroBboxForCity(selectedCity) }

    /** Fetch route for preview (start+dest) and again while navigating / rerouting. */
    LaunchedEffect(
        tripModeEnabled,
        tripNavActive,
        tripStartLat,
        tripStartLon,
        tripDestLat,
        tripDestLon,
        tripVehicle,
        // Bucket GPS so tiny jitter does not cancel an in-flight OSRM request.
        if (tripStartUsesMyLocation) {
            gpsPinLocation?.latitude?.let { (it * 5_000).toInt() }
        } else {
            null
        },
        if (tripStartUsesMyLocation) {
            gpsPinLocation?.longitude?.let { (it * 5_000).toInt() }
        } else {
            null
        },
        tripRouteRequestEpoch,
        tripNavOriginLat,
        tripNavOriginLon,
    ) {
        if (!tripModeEnabled) {
            tripRoutePoints = emptyList()
            tripRouteSteps = emptyList()
            tripNextTurn = null
            tripRouteProgress = null
            tripRouteDurationSec = null
            tripRouteStatus = TripRouteStatus.IDLE
            tripArrived = false
            tripAlerts = emptyList()
            tripRerouting = false
            rerouteOriginLat = null
            rerouteOriginLon = null
            lastRerouteAtMs = 0L
            tripStartCorrectionDone = false
            announcedPotholeIds.clear()
            return@LaunchedEffect
        }
        val destLat = tripDestLat
        val destLon = tripDestLon
        if (destLat == null || destLon == null) {
            tripRoutePoints = emptyList()
            tripRouteSteps = emptyList()
            tripNextTurn = null
            tripRouteProgress = null
            tripRouteDurationSec = null
            tripRouteStatus = TripRouteStatus.IDLE
            tripAlerts = emptyList()
            tripRerouting = false
            return@LaunchedEffect
        }
        val gps = gpsPinLocation
        // Preview uses planned start / GPS; active trip prefers locked Navigate origin + reroutes.
        val originLat = if (tripNavActive) {
            rerouteOriginLat ?: tripNavOriginLat ?: tripStartLat ?: gps?.latitude
        } else {
            tripStartLat ?: gps?.latitude
        }
        val originLon = if (tripNavActive) {
            rerouteOriginLon ?: tripNavOriginLon ?: tripStartLon ?: gps?.longitude
        } else {
            tripStartLon ?: gps?.longitude
        }
        if (originLat != null && originLon != null) {
            val preserveCurrentRouteOnFailure = tripRoutePoints.size >= 2
            tripRerouting = tripNavActive && preserveCurrentRouteOnFailure
            tripRouteStatus = if (tripRerouting) {
                TripRouteStatus.REROUTING
            } else {
                TripRouteStatus.FINDING
            }
            val route = runCatching {
                OsrmRouteClient.fetchRoute(
                    originLat, originLon, destLat, destLon, tripVehicle,
                )
            }.getOrNull()
            if (route != null && route.points.size >= 2) {
                // OSRM returns network-snapped geometry for the selected mode.
                tripRoutePoints = route.points
                tripRouteSteps = route.steps
                tripRouteDurationSec = route.durationSeconds
                tripRouteStatus = TripRouteStatus.ACTIVE
                if (!tripNavActive) {
                    tripArrived = false
                }
                rerouteOriginLat = null
                rerouteOriginLon = null
            } else if (!preserveCurrentRouteOnFailure) {
                tripRoutePoints = emptyList()
                tripRouteSteps = emptyList()
                tripNextTurn = null
                tripRouteDurationSec = null
                tripRouteStatus = TripRouteStatus.NO_ROUTE
            } else {
                // Keep the last good polyline (preview or active) if a refresh fails.
                tripRouteStatus = if (tripNavActive) TripRouteStatus.ACTIVE else TripRouteStatus.ACTIVE
            }
            tripRerouting = false
        } else {
            // GPS / origin not ready yet. Keep an existing preview polyline if we have one.
            if (tripRoutePoints.size < 2) {
                tripRouteStatus = TripRouteStatus.FINDING
            }
        }
    }

    /** Project every GPS fix onto the route for progress, alerts, and off-route detection. */
    LaunchedEffect(
        tripModeEnabled,
        tripNavActive,
        tripRoutePoints,
        gpsPinLocation?.latitude,
        gpsPinLocation?.longitude,
        gpsPinLocation?.bearing,
        gpsPinLocation?.speed,
        gpsPinLocation?.accuracy,
        recentReportsEpoch,
        mapSeverityFilter,
    ) {
        if (!tripModeEnabled || !tripNavActive || tripRoutePoints.size < 2) {
            tripRouteProgress = null
            tripNextTurn = null
            tripAlerts = emptyList()
            return@LaunchedEffect
        }
        val gps = gpsPinLocation
        val driverLat = gps?.latitude ?: tripStartLat ?: tripRoutePoints.first().first
        val driverLon = gps?.longitude ?: tripStartLon ?: tripRoutePoints.first().second
        val progress = TripNavigationMatcher.routeProgress(
            tripRoutePoints,
            driverLat,
            driverLon,
        )
        tripRouteProgress = progress
        if (progress != null) {
            val arrivalThresholdM = maxOf(
                25.0,
                gps?.accuracy?.takeIf { gps.hasAccuracy() }?.toDouble() ?: 0.0,
            )
            if (progress.remainingMeters <= arrivalThresholdM) {
                tripArrived = true
                tripRouteStatus = TripRouteStatus.ARRIVED
                tripNextTurn = null
            } else if (tripRouteStatus == TripRouteStatus.ARRIVED) {
                tripArrived = false
                tripRouteStatus = TripRouteStatus.ACTIVE
            }
            if (!tripArrived) {
                tripNextTurn = TripNavigationMatcher.nextTurnGuidance(
                    tripRouteSteps,
                    progress.traveledMeters,
                )
            }
        } else {
            tripNextTurn = null
        }
        val reports = bbox?.let { metroBbox ->
            withContext(Dispatchers.IO) {
                RecentReportsRepository.reportsForMapInMetro(selectedCity, metroBbox)
                    .filter { report ->
                        mapSeverityFilter == null || report.severity == mapSeverityFilter
                    }
            }
        }.orEmpty()
        tripAlerts = if (tripArrived) {
            emptyList()
        } else {
            TripNavigationMatcher.matchAlongRoute(
                tripRoutePoints,
                reports,
                driverLat,
                driverLon,
                gps?.let {
                    fuseNavHeadingDeg(tripDeviceHeadingDeg, it)
                } ?: gps?.bearing?.takeIf { gps.hasBearing() },
                gps?.accuracy?.takeIf { gps.hasAccuracy() },
            )
        }

        if (
            gps == null ||
            !gps.isTripMoving() ||
            progress == null ||
            tripRerouting ||
            tripArrived
        ) {
            return@LaunchedEffect
        }
        if (
            !tripStartCorrectionDone &&
            gps.hasAccuracy() &&
            gps.accuracy <= 30f
        ) {
            tripStartCorrectionDone = true
            if (
                TripNavigationMatcher.routeStartNeedsCorrection(
                    tripRoutePoints,
                    gps.latitude,
                    gps.longitude,
                    gps.accuracy,
                )
            ) {
                lastRerouteAtMs = SystemClock.elapsedRealtime()
                rerouteOriginLat = gps.latitude
                rerouteOriginLon = gps.longitude
                tripRerouting = true
                tripRouteRequestEpoch++
                return@LaunchedEffect
            }
        }
        val accuracyAwareThreshold = maxOf(
            TripNavigationMatcher.OFF_ROUTE_REROUTE_M,
            gps.accuracy.takeIf { gps.hasAccuracy() }?.times(2.0f)?.toDouble() ?: 0.0,
        )
        val now = SystemClock.elapsedRealtime()
        if (
            progress.offRouteMeters > accuracyAwareThreshold &&
            now - lastRerouteAtMs >= 12_000L
        ) {
            lastRerouteAtMs = now
            rerouteOriginLat = gps.latitude
            rerouteOriginLon = gps.longitude
            tripRerouting = true
            tripRouteRequestEpoch++
        }
    }
    val nextTripAlert = tripAlerts.firstOrNull()

    var tripVoiceMuted by rememberSaveable {
        mutableStateOf(TripVoiceGuidance.isMuted(context))
    }
    val tripVoice = remember { TripVoiceGuidance(context) }
    DisposableEffect(tripVoice) {
        onDispose { tripVoice.shutdown() }
    }
    val spokenTurnKeys = remember { mutableSetOf<String>() }
    LaunchedEffect(tripNavActive, tripRouteRequestEpoch) {
        if (!tripNavActive) {
            spokenTurnKeys.clear()
            tripVoice.stop()
        }
    }

    // One vibration + tone (+ optional voice) per report id while navigating.
    LaunchedEffect(tripNavActive, tripArrived, nextTripAlert?.report?.id, nextTripAlert?.distanceMeters, tripVoiceMuted) {
        if (!tripNavActive || tripArrived) return@LaunchedEffect
        val alert = nextTripAlert ?: return@LaunchedEffect
        if (alert.distanceMeters > TRIP_POTHOLE_NOTIFY_WITHIN_M) return@LaunchedEffect
        val id = alert.report.id
        if (!announcedPotholeIds.add(id)) return@LaunchedEffect
        notifyTripPotholeAlert(context, alert.report.severity)
        if (!tripVoiceMuted) {
            tripVoice.speak(formatTripPotholeAlertLine(alert))
        }
    }

    // Speak upcoming turns (far ~200 m, near ~50 m); skip when muted.
    LaunchedEffect(
        tripNavActive,
        tripArrived,
        tripVoiceMuted,
        tripNextTurn?.instruction,
        tripNextTurn?.distanceMeters,
    ) {
        if (!tripNavActive || tripArrived || tripVoiceMuted) return@LaunchedEffect
        val turn = tripNextTurn ?: return@LaunchedEffect
        val phase = when {
            turn.distanceMeters <= 50.0 -> "near"
            turn.distanceMeters <= 200.0 -> "far"
            else -> return@LaunchedEffect
        }
        val key = "${turn.maneuverType}|${turn.instruction}|$phase"
        if (!spokenTurnKeys.add(key)) return@LaunchedEffect
        val spoken = if (phase == "near") {
            "In ${turn.distanceMeters.toInt().coerceAtLeast(1)} meters, ${turn.instruction}"
        } else {
            turn.instruction
        }
        tripVoice.speak(spoken)
    }

    @Composable
    fun MapSection(modifier: Modifier, showExpand: Boolean, showCollapse: Boolean = false) {
        Box(modifier = modifier.background(Color(0xFFFDFCF9))) {
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
                areaHeatEnabled = areaHeatEnabled && !tripModeEnabled,
                tripModeEnabled = tripModeEnabled,
                tripNavActive = tripNavActive,
                tripStreetFollow = tripStreetFollow,
                tripDeviceHeadingDeg = tripDeviceHeadingDeg,
                tripVehicle = tripVehicle,
                tripRoutePoints = tripRoutePoints,
                tripRouteTraveledMeters = if (tripNavActive) {
                    tripRouteProgress?.traveledMeters ?: 0.0
                } else {
                    0.0
                },
                tripAlerts = tripAlerts,
                // Start/end dots follow the road-snapped route when available (not off-road GPS).
                tripStartLat = tripRoutePoints.firstOrNull()?.first
                    ?: tripStartLat
                    ?: gpsPinLocation?.latitude,
                tripStartLon = tripRoutePoints.firstOrNull()?.second
                    ?: tripStartLon
                    ?: gpsPinLocation?.longitude,
                tripDestinationLat = tripRoutePoints.lastOrNull()?.first ?: tripDestLat,
                tripDestinationLon = tripRoutePoints.lastOrNull()?.second ?: tripDestLon,
                recentReportsEpoch = recentReportsEpoch,
                modifier = Modifier.fillMaxSize(),
            )
            if (tripModeEnabled && !tripNavActive) {
                TripPlannerPanel(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        // Clear of TopStart compass rose.
                        .padding(start = 8.dp, top = 48.dp, end = 52.dp),
                    selectedCity = selectedCity,
                    startText = if (tripStartUsesMyLocation) tripMyLocationLabel else tripStartLabel,
                    destText = tripDestLabel,
                    vehicle = tripVehicle,
                    startUsesMyLocation = tripStartUsesMyLocation,
                    canNavigate = tripDestLat != null && tripDestLon != null &&
                        (tripStartLat != null || gpsPinLocation != null) &&
                        !tripNavigatePreparing,
                    navigatePreparing = tripNavigatePreparing,
                    onVehicleChange = { tripVehicleName = it.name },
                    onStartSelected = { label, lat, lon ->
                        tripStartLat = lat
                        tripStartLon = lon
                        tripStartLabel = label
                        tripNavActive = false
                    },
                    onDestSelected = { label, lat, lon ->
                        tripDestLat = lat
                        tripDestLon = lon
                        tripDestLabel = label
                        tripNavActive = false
                    },
                    onUseMyLocation = {
                        tripStartLat = null
                        tripStartLon = null
                        tripStartLabel = ""
                        tripNavActive = false
                        tripNavOriginLat = null
                        tripNavOriginLon = null
                    },
                    onClearStart = {
                        tripStartLat = null
                        tripStartLon = null
                        tripStartLabel = ""
                        tripMyLocationLabel = ""
                        tripNavActive = false
                        tripNavOriginLat = null
                        tripNavOriginLon = null
                    },
                    onClearDestination = {
                        tripDestLat = null
                        tripDestLon = null
                        tripDestLabel = ""
                        tripNavActive = false
                    },
                    onNavigate = {
                        tripScope.launch {
                            tripNavigatePreparing = true
                            try {
                                if (tripStartUsesMyLocation) {
                                    // Refresh GPS for the pin; OSRM nearest-snaps onto Car/Bike/Walk network.
                                    val gps = onEnsureFreshGpsForTrip() ?: gpsPinLocation
                                    tripNavOriginLat = gps?.latitude
                                    tripNavOriginLon = gps?.longitude
                                    rerouteOriginLat = null
                                    rerouteOriginLon = null
                                    tripStartCorrectionDone = false
                                } else {
                                    tripNavOriginLat = tripStartLat
                                    tripNavOriginLon = tripStartLon
                                }
                                tripNavActive = true
                            } finally {
                                tripNavigatePreparing = false
                            }
                        }
                    },
                )
            }
            if (tripModeEnabled && tripNavActive) {
                // Single left rail under the compass — keeps the map face clear.
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 6.dp, top = 50.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconButton(
                        onClick = {
                            tripNavActive = false
                            tripNavOriginLat = null
                            tripNavOriginLon = null
                        },
                        modifier = Modifier
                            .background(Color(0xFFB91C1C), CircleShape)
                            .size(42.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Stop navigation",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            val next = !tripVoiceMuted
                            tripVoiceMuted = next
                            TripVoiceGuidance.setMuted(context, next)
                            if (next) tripVoice.stop()
                        },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.92f), CircleShape)
                            .size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (tripVoiceMuted) {
                                Icons.Filled.VolumeOff
                            } else {
                                Icons.Filled.VolumeUp
                            },
                            contentDescription = if (tripVoiceMuted) {
                                "Unmute voice guidance"
                            } else {
                                "Mute voice guidance"
                            },
                            tint = DarkBlue,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = { tripStreetFollow = !tripStreetFollow },
                        modifier = Modifier
                            .background(
                                if (tripStreetFollow) {
                                    DarkBlue.copy(alpha = 0.92f)
                                } else {
                                    Color.White.copy(alpha = 0.92f)
                                },
                                CircleShape,
                            )
                            .size(36.dp)
                            .semantics {
                                contentDescription = if (tripStreetFollow) {
                                    "Switch to map overview"
                                } else {
                                    "Switch to street follow"
                                }
                            },
                    ) {
                        Icon(
                            imageVector = if (tripStreetFollow) {
                                Icons.Filled.Navigation
                            } else {
                                Icons.Outlined.Map
                            },
                            contentDescription = null,
                            tint = if (tripStreetFollow) Color.White else DarkBlue,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (showCollapse) {
                IconButton(
                    onClick = { onMapFullscreenChange(false) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 44.dp, end = 6.dp)
                        .background(Color.White.copy(alpha = 0.92f), CircleShape)
                        .size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.FullscreenExit,
                        contentDescription = "Exit fullscreen",
                        tint = DarkBlue,
                    )
                }
            }
            if (showExpand) {
                IconButton(
                    onClick = { onMapFullscreenChange(true) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 44.dp, end = 6.dp)
                        .background(Color.White.copy(alpha = 0.92f), CircleShape)
                        .size(36.dp),
                ) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "Expand map", tint = DarkBlue)
                }
            }
            if (tripModeEnabled && tripNavActive) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .heightIn(max = 96.dp),
                    color = Color(0xFF1F2937).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        val progress = tripRouteProgress
                        val etaSeconds = remainingTripSeconds(
                            totalDurationSec = tripRouteDurationSec,
                            progress = progress,
                        )
                        Text(
                            text = when {
                                tripRouteStatus == TripRouteStatus.ARRIVED || tripArrived ->
                                    "Arrived at destination"
                                tripRerouting || tripRouteStatus == TripRouteStatus.REROUTING ->
                                    "Off route · Finding a new route…"
                                tripRouteStatus == TripRouteStatus.NO_ROUTE ->
                                    "No route found. Try another destination."
                                tripRouteStatus == TripRouteStatus.FINDING || progress == null ->
                                    "Finding route…"
                                else -> buildString {
                                    append(formatTripDistance(progress.remainingMeters))
                                    append(" remaining")
                                    etaSeconds?.let {
                                        append(" · ETA ")
                                        append(formatTripEta(it))
                                    }
                                    append(" · ")
                                    append((progress.completionFraction * 100).toInt())
                                    append("% complete")
                                }
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (
                            progress != null &&
                            !tripArrived &&
                            tripRouteStatus != TripRouteStatus.NO_ROUTE
                        ) {
                            Spacer(Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (tripArrived) 1f else progress.completionFraction.toFloat()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF60A5FA),
                                trackColor = Color.White.copy(alpha = 0.25f),
                            )
                        }
                        if (!tripArrived) {
                            tripNextTurn?.let { turn ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${formatTripDistance(turn.distanceMeters)} · ${turn.instruction}",
                                    color = Color(0xFFBFDBFE),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            nextTripAlert?.let { alert ->
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    formatTripPotholeAlertLine(alert),
                                    color = Color(tripPotholeAlertAccentArgb(alert.report.severity)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Single MapView — expand is an in-composition fill (no Dialog window).
    Box(
        modifier = if (mapFullscreen) {
            Modifier
                .fillMaxSize()
                .background(Color(0xFFFDFCF9))
        } else {
            Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(horizontal = 16.dp)
        },
    ) {
        MapSection(
            modifier = Modifier.fillMaxSize(),
            showExpand = !mapFullscreen,
            showCollapse = mapFullscreen,
        )
    }

    if (!mapFullscreen) {
        RecentReportsStrip(
            selectedCity = selectedCity,
            recentReportsEpoch = recentReportsEpoch,
            onReportsMutated = onReportsMutated,
        )
    }
}

/**
 * On-map trip planner: start + destination search, Car / Bike / Walk, and Navigate.
 * Start defaults to the device GPS location when left blank.
 * A road route preview appears once start + destination are set; [onNavigate] enters street follow.
 */
@Composable
private fun TripPlannerPanel(
    modifier: Modifier = Modifier,
    selectedCity: String,
    startText: String,
    destText: String,
    vehicle: TripVehicle,
    startUsesMyLocation: Boolean,
    canNavigate: Boolean,
    navigatePreparing: Boolean = false,
    onVehicleChange: (TripVehicle) -> Unit,
    onStartSelected: (label: String, lat: Double, lon: Double) -> Unit,
    onDestSelected: (label: String, lat: Double, lon: Double) -> Unit,
    onUseMyLocation: () -> Unit,
    onClearStart: () -> Unit,
    onClearDestination: () -> Unit,
    onNavigate: () -> Unit,
) {
    val context = LocalContext.current
    var startQuery by remember { mutableStateOf("") }
    var destQuery by remember { mutableStateOf("") }
    var startResults by remember { mutableStateOf<List<Pair<String, GeoPoint>>>(emptyList()) }
    var destResults by remember { mutableStateOf<List<Pair<String, GeoPoint>>>(emptyList()) }
    var recentDestinations by remember {
        mutableStateOf(TripRecentPlacesRepository.destinations(context))
    }
    var recentStarts by remember {
        mutableStateOf(TripRecentPlacesRepository.starts(context))
    }
    var startSearching by remember { mutableStateOf(false) }
    var destSearching by remember { mutableStateOf(false) }
    var startSearchAttempted by remember { mutableStateOf(false) }
    var destSearchAttempted by remember { mutableStateOf(false) }
    /** 0 = none, 1 = start, 2 = destination. */
    var activeField by remember { mutableIntStateOf(0) }

    LaunchedEffect(startUsesMyLocation) {
        if (startUsesMyLocation) {
            startQuery = ""
            startResults = emptyList()
            activeField = 0
        }
    }

    LaunchedEffect(startQuery, activeField, selectedCity) {
        if (activeField != 1) return@LaunchedEffect
        val q = startQuery.trim()
        if (q.length < 2) {
            startResults = emptyList()
            startSearching = false
            startSearchAttempted = false
            return@LaunchedEffect
        }
        delay(280)
        startSearching = true
        startResults = withContext(Dispatchers.IO) {
            searchTripPlaces(context, q, selectedCity, limit = 18)
        }
        startSearching = false
        startSearchAttempted = true
    }
    LaunchedEffect(destQuery, activeField, selectedCity) {
        if (activeField != 2) return@LaunchedEffect
        val q = destQuery.trim()
        if (q.length < 2) {
            destResults = emptyList()
            destSearching = false
            destSearchAttempted = false
            return@LaunchedEffect
        }
        delay(280)
        destSearching = true
        destResults = withContext(Dispatchers.IO) {
            searchTripPlaces(context, q, selectedCity, limit = 18)
        }
        destSearching = false
        destSearchAttempted = true
    }

    val fieldText = DarkBlue
    val placeholderColor = Color(0xFF6B7280)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = fieldText,
        unfocusedTextColor = fieldText,
        disabledTextColor = fieldText,
        focusedBorderColor = DarkBlue,
        unfocusedBorderColor = Color(0xFFB0B4BD),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = DarkBlue,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        focusedLeadingIconColor = DarkBlue,
        unfocusedLeadingIconColor = DarkBlue,
        focusedTrailingIconColor = Color(0xFF4B5563),
        unfocusedTrailingIconColor = Color(0xFF4B5563),
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.97f),
        shadowElevation = 4.dp,
        contentColor = DarkBlue,
    ) {
        Column(Modifier.padding(10.dp)) {
            OutlinedTextField(
                value = when {
                    startQuery.isNotBlank() -> startQuery
                    startUsesMyLocation && startText.isNotBlank() -> startText
                    else -> ""
                },
                onValueChange = {
                    startQuery = it
                    activeField = 1
                    startSearchAttempted = false
                },
                singleLine = true,
                placeholder = {
                    Text(
                        when {
                            startUsesMyLocation -> "My location"
                            startText.isNotBlank() -> startText
                            else -> "Choose start"
                        },
                        fontSize = 12.sp,
                        color = placeholderColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall.copy(color = fieldText),
                leadingIcon = {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = if (startUsesMyLocation) {
                            "My location active"
                        } else {
                            "Custom start location"
                        },
                        tint = if (startUsesMyLocation) Color(0xFF15803D) else Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    val canClearStart =
                        startQuery.isNotBlank() ||
                            !startUsesMyLocation ||
                            (startUsesMyLocation && startText.isNotBlank())
                    if (canClearStart) {
                        IconButton(onClick = {
                            startQuery = ""
                            startResults = emptyList()
                            startSearchAttempted = false
                            activeField = 0
                            onClearStart()
                        }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Clear start",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused) activeField = 1
                    },
            )
            if (activeField == 1 && startSearching) {
                TripSearchStatus("Searching places…")
            } else if (activeField == 1 && startResults.isNotEmpty()) {
                TripSuggestionList(startResults) { label, gp ->
                    startQuery = label
                    startResults = emptyList()
                    activeField = 0
                    recentStarts = TripRecentPlacesRepository.rememberStart(
                        context,
                        label,
                        gp.latitude,
                        gp.longitude,
                    )
                    onStartSelected(label, gp.latitude, gp.longitude)
                }
            } else if (activeField == 1 && startSearchAttempted) {
                TripSearchStatus("No matching places found. Try a landmark, area, or postcode.")
            } else if (
                activeField == 1 &&
                startQuery.isBlank() &&
                recentStarts.isNotEmpty()
            ) {
                TripRecentPlacesPanel(
                    title = "Recent starts",
                    places = recentStarts,
                    showFavorite = false,
                    onPick = { place ->
                        startQuery = place.label
                        activeField = 0
                        recentStarts = TripRecentPlacesRepository.rememberStart(
                            context,
                            place.label,
                            place.latitude,
                            place.longitude,
                        )
                        onStartSelected(place.label, place.latitude, place.longitude)
                    },
                    onToggleFavorite = null,
                    onRemove = null,
                )
            }

            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = destQuery,
                onValueChange = {
                    destQuery = it
                    activeField = 2
                    destSearchAttempted = false
                },
                singleLine = true,
                placeholder = {
                    Text(
                        destText.ifBlank { "Choose destination" },
                        fontSize = 12.sp,
                        color = placeholderColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall.copy(color = fieldText),
                leadingIcon = {
                    Icon(Icons.Filled.Navigation, contentDescription = "Destination", tint = Color(0xFFB91C1C), modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (destQuery.isNotBlank() || destText.isNotBlank()) {
                        IconButton(onClick = {
                            destQuery = ""
                            destResults = emptyList()
                            destSearchAttempted = false
                            recentDestinations = TripRecentPlacesRepository.destinations(context)
                            activeField = 0
                            onClearDestination()
                        }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Clear destination",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused) activeField = 2
                    },
            )
            if (activeField == 2 && destSearching) {
                TripSearchStatus("Searching places…")
            } else if (activeField == 2 && destResults.isNotEmpty()) {
                TripSuggestionList(destResults) { label, gp ->
                    destQuery = label
                    destResults = emptyList()
                    activeField = 0
                    recentDestinations = TripRecentPlacesRepository.rememberDestination(
                        context,
                        label,
                        gp.latitude,
                        gp.longitude,
                    )
                    onDestSelected(label, gp.latitude, gp.longitude)
                }
            } else if (activeField == 2 && destSearchAttempted) {
                TripSearchStatus("No matching places found. Try a landmark, area, or postcode.")
            } else if (
                activeField == 2 &&
                destQuery.isBlank() &&
                recentDestinations.isNotEmpty()
            ) {
                val favorites = recentDestinations.filter { it.isFavorite }
                val recentOnly = recentDestinations.filterNot { it.isFavorite }
                if (favorites.isNotEmpty()) {
                    TripRecentPlacesPanel(
                        title = "Saved destinations",
                        places = favorites,
                        showFavorite = true,
                        onPick = { place ->
                            destQuery = place.label
                            activeField = 0
                            recentDestinations = TripRecentPlacesRepository.rememberDestination(
                                context,
                                place.label,
                                place.latitude,
                                place.longitude,
                            )
                            onDestSelected(place.label, place.latitude, place.longitude)
                        },
                        onToggleFavorite = { place ->
                            recentDestinations = TripRecentPlacesRepository.toggleFavoriteDestination(
                                context,
                                place.label,
                                place.latitude,
                                place.longitude,
                            )
                        },
                        onRemove = { place ->
                            recentDestinations = TripRecentPlacesRepository.removeDestination(
                                context,
                                place.latitude,
                                place.longitude,
                            )
                        },
                    )
                }
                if (recentOnly.isNotEmpty()) {
                    TripRecentPlacesPanel(
                        title = "Recent destinations",
                        places = recentOnly,
                        showFavorite = true,
                        onPick = { place ->
                            destQuery = place.label
                            activeField = 0
                            recentDestinations = TripRecentPlacesRepository.rememberDestination(
                                context,
                                place.label,
                                place.latitude,
                                place.longitude,
                            )
                            onDestSelected(place.label, place.latitude, place.longitude)
                        },
                        onToggleFavorite = { place ->
                            recentDestinations = TripRecentPlacesRepository.toggleFavoriteDestination(
                                context,
                                place.label,
                                place.latitude,
                                place.longitude,
                            )
                        },
                        onRemove = { place ->
                            recentDestinations = TripRecentPlacesRepository.removeDestination(
                                context,
                                place.latitude,
                                place.longitude,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TripVehicle.entries.forEach { option ->
                    val selected = vehicle == option
                    FilterChip(
                        selected = selected,
                        onClick = { onVehicleChange(option) },
                        label = { Text(option.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (option) {
                                    TripVehicle.CAR -> Icons.Filled.DirectionsCar
                                    TripVehicle.BIKE -> Icons.Filled.TwoWheeler
                                    TripVehicle.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, if (selected) DarkBlue else Color(0xFF9CA3AF)),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = DarkBlue,
                            iconColor = DarkBlue,
                            selectedContainerColor = DarkBlue,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onNavigate,
                enabled = canNavigate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB91C1C),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFD1D5DB),
                    disabledContentColor = Color(0xFF6B7280),
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Filled.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (navigatePreparing) "Getting GPS…" else "Navigate",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TripSearchStatus(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (message.startsWith("Searching")) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = DarkBlue,
            )
        }
        Text(
            text = message,
            color = Color(0xFF4B5563),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun TripRecentPlacesPanel(
    title: String,
    places: List<TripRecentPlace>,
    showFavorite: Boolean,
    onPick: (TripRecentPlace) -> Unit,
    onToggleFavorite: ((TripRecentPlace) -> Unit)?,
    onRemove: ((TripRecentPlace) -> Unit)?,
) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 7.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (title.startsWith("Saved")) Icons.Filled.Star else Icons.Outlined.History,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            title,
            color = Color(0xFF4B5563),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .background(Color(0xFFF8F9FA), RoundedCornerShape(6.dp)),
    ) {
        places.take(8).forEach { place ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(place) }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.label,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = DarkBlue,
                        fontWeight = FontWeight.Medium,
                    )
                    val age = formatTripPlaceAge(place.usedAtMs)
                    if (age.isNotBlank()) {
                        Text(
                            age,
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280),
                        )
                    }
                }
                if (showFavorite && onToggleFavorite != null) {
                    IconButton(
                        onClick = { onToggleFavorite(place) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = if (place.isFavorite) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarBorder
                            },
                            contentDescription = if (place.isFavorite) {
                                "Remove from saved"
                            } else {
                                "Save destination"
                            },
                            tint = if (place.isFavorite) Color(0xFFCA8A04) else Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (onRemove != null) {
                    IconButton(
                        onClick = { onRemove(place) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Remove place",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFE5E7EB))
        }
    }
}

@Composable
private fun TripSuggestionList(
    results: List<Pair<String, GeoPoint>>,
    onPick: (String, GeoPoint) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .background(Color(0xFFF8F9FA), RoundedCornerShape(6.dp)),
    ) {
        results.take(8).forEach { (label, gp) ->
            val title = label.substringBefore(" · ").ifBlank { label }
            val subtitle = label.substringAfter(" · ", missingDelimiterValue = "").trim()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(label, gp) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = DarkBlue,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF6B7280),
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFE5E7EB))
        }
    }
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

/** Richer place label for trip search: name/institution first, then area context. */
private fun resolveTripPlaceLabel(address: android.location.Address, queryHint: String = ""): String =
    TripPlaceLabels.formatSearchResult(address, queryHint)

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

/** Reverse-geocode lat/lng into a readable trip place label. */
private fun reverseGeocodeTripPlace(context: Context, lat: Double, lon: Double): String {
    if (!lat.isFinite() || !lon.isFinite()) return ""
    return runCatching {
        val geocoder = Geocoder(context, Locale.Builder().setLanguage("en").setRegion("IN").build())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lon, 1) ?: emptyList()
        val address = addresses.firstOrNull() ?: return ""
        resolveTripPlaceLabel(address)
    }.getOrDefault("")
}

private enum class TripRouteStatus {
    IDLE,
    FINDING,
    ACTIVE,
    REROUTING,
    NO_ROUTE,
    ARRIVED,
}

private fun formatTripDistance(meters: Double): String =
    if (meters < 1_000.0) {
        "${meters.toInt().coerceAtLeast(0)} m"
    } else {
        String.format(Locale.US, "%.1f km", meters / 1_000.0)
    }

private fun remainingTripSeconds(
    totalDurationSec: Double?,
    progress: TripRouteProgress?,
): Double? {
    val total = totalDurationSec?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    val fractionLeft = progress
        ?.completionFraction
        ?.coerceIn(0.0, 1.0)
        ?.let { 1.0 - it }
        ?: 1.0
    return (total * fractionLeft).coerceAtLeast(0.0)
}

private fun formatTripEta(seconds: Double): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes} min"
        else -> "<1 min"
    }
}

/**
 * Trip start/destination search: city-biased, keeps distinct street/landmark results.
 * Tries city-scoped query first, then India-wide to fill gaps.
 */
private fun searchTripPlaces(
    context: Context,
    query: String,
    selectedCity: String,
    limit: Int = 18,
): List<Pair<String, GeoPoint>> {
    val q = query.trim()
    if (q.length < 2) return emptyList()
    parseTripCoordinates(q)?.let { return listOf("Dropped pin (${it.latitude}, ${it.longitude})" to it) }
    if (!Geocoder.isPresent()) return emptyList()

    val geocoder = Geocoder(context, Locale.Builder().setLanguage("en").setRegion("IN").build())
    val cityLabel = formatCityDisplayName(selectedCity)
    val cityAliases = when (CityMetroKeys.canonical(selectedCity)) {
        "BENGALURU" -> listOf(cityLabel, "Bangalore")
        else -> listOf(cityLabel)
    }
    val queries = buildList {
        cityAliases.forEach { city -> add("$q, $city, India") }
        add("$q, $selectedCity, India")
        add("$q, India")
        add(q)
    }.distinct()
    val out = linkedMapOf<String, GeoPoint>()
    val seenCoords = mutableSetOf<String>()
    for (search in queries) {
        if (out.size >= limit) break
        val addresses = runCatching {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(search, limit)
        }.getOrNull().orEmpty()
        for (address in addresses) {
            if (!address.hasLatitude() || !address.hasLongitude()) continue
            val lat = address.latitude
            val lon = address.longitude
            val coordKey = String.format(Locale.US, "%.4f,%.4f", lat, lon)
            if (!seenCoords.add(coordKey)) continue
            val label = resolveTripPlaceLabel(address, q)
            if (label.length < 3) continue
            // Prefer first (usually city-biased) hit for a given display label.
            out.putIfAbsent(label, GeoPoint(lat, lon))
            if (out.size >= limit) break
        }
    }
    return out.entries.map { it.key to it.value }
}

/** Accepts pasted "latitude, longitude" as a deterministic search fallback. */
private fun parseTripCoordinates(query: String): GeoPoint? {
    val match = Regex("""^\s*(-?\d{1,2}(?:\.\d+)?)\s*[, ]\s*(-?\d{1,3}(?:\.\d+)?)\s*$""")
        .matchEntire(query)
        ?: return null
    val latitude = match.groupValues[1].toDoubleOrNull() ?: return null
    val longitude = match.groupValues[2].toDoubleOrNull() ?: return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return GeoPoint(latitude, longitude)
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
    var tripNav: TripNavigationOverlay? = null,
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

private fun refreshTripNavigationOverlay(
    map: MapView,
    decor: CityOutlineDecor,
    tripModeEnabled: Boolean,
    routePoints: List<Pair<Double, Double>>,
    traveledMeters: Double,
    alerts: List<TripPotholeAlert>,
) {
    if (!tripModeEnabled) {
        decor.tripNav?.let { overlay ->
            overlay.clear(map)
            overlay.detachFrom(map)
        }
        decor.tripNav = null
        return
    }
    val overlay = decor.tripNav ?: TripNavigationOverlay().also { decor.tripNav = it }
    overlay.attachTo(map)
    val (completed, remaining) = TripNavigationMatcher.splitRouteByTraveled(
        routePoints,
        traveledMeters,
    )
    overlay.completedRoutePoints = completed.map { (lat, lon) -> GeoPoint(lat, lon) }
    overlay.remainingRoutePoints = remaining.map { (lat, lon) -> GeoPoint(lat, lon) }
    overlay.alerts = alerts
    overlay.syncMarkers(map)
    decor.clusters?.let { clusters ->
        map.overlays.remove(clusters)
        map.overlays.add(clusters)
    }
    map.invalidate()
}

private fun syncTripEndpointDots(
    map: MapView,
    startSlot: Array<Marker?>,
    destSlot: Array<Marker?>,
    show: Boolean,
    startLat: Double?,
    startLon: Double?,
    destLat: Double?,
    destLon: Double?,
) {
    fun syncDot(
        slot: Array<Marker?>,
        lat: Double?,
        lon: Double?,
        title: String,
        colorHex: String,
    ) {
        val existing = slot[0]
        if (!show || lat == null || lon == null) {
            existing?.let { map.overlays.remove(it) }
            slot[0] = null
            return
        }
        val point = GeoPoint(lat, lon)
        if (existing == null) {
            val marker = Marker(map).apply {
                position = point
                this.title = title
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setIcon(buildTripDotDrawable(map.context, colorHex))
            }
            slot[0] = marker
            map.overlays.add(marker)
        } else {
            existing.position = point
            if (existing.icon == null) {
                existing.setIcon(buildTripDotDrawable(map.context, colorHex))
            }
        }
    }
    syncDot(startSlot, startLat, startLon, "Start", "#22C55E")
    syncDot(destSlot, destLat, destLon, "Destination", "#EF4444")
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
    // Place / street / water names come from CARTO Voyager tiles (no seeded overlay).
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

/** Live navigation: closer top-down “riding on the road” zoom. */
private const val TRIP_FOLLOW_ZOOM = 17.5

/** Wider north-up zoom when street follow is off during navigation. */
private const val TRIP_NAV_OVERVIEW_ZOOM = 15.0

/** Ignore tiny GPS bearing jitter when rotating the map (degrees). */
private const val TRIP_HEADING_UPDATE_MIN_DEG = 5f

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

/** Smoothly follows the latest GPS fix; close zoom + optional heading-up street POV. */
private fun MapView.scheduleFollowTripLocation(
    selectedCity: String,
    location: Location,
    headingUp: Boolean = true,
    zoom: Double = TRIP_FOLLOW_ZOOM,
    attempt: Int = 0,
) {
    val maxAttempts = 15
    post {
        if (width <= 0 || height <= 0) {
            if (attempt < maxAttempts) {
                postDelayed({
                    scheduleFollowTripLocation(selectedCity, location, headingUp, zoom, attempt + 1)
                }, 40L)
            }
            return@post
        }
        if (abs(zoomLevelDouble - zoom) > 0.15) {
            controller.setZoom(zoom)
        }
        if (headingUp && location.hasBearing()) {
            applyTripHeadingUp(location.bearing)
        } else if (!headingUp) {
            resetMapOrientationNorthUp()
        }
        controller.animateTo(GeoPoint(location.latitude, location.longitude))
        clampViewportToMetroBounds(selectedCity)
        invalidate()
    }
}

/** North-up overview that fits the whole preview route in the map card. */
private fun MapView.scheduleFitRoutePreview(
    routePoints: List<Pair<Double, Double>>,
    selectedCity: String,
    attempt: Int = 0,
) {
    if (routePoints.size < 2) return
    val maxAttempts = 15
    post {
        if (width <= 0 || height <= 0) {
            if (attempt < maxAttempts) {
                postDelayed({ scheduleFitRoutePreview(routePoints, selectedCity, attempt + 1) }, 40L)
            }
            return@post
        }
        resetMapOrientationNorthUp()
        var north = Double.NEGATIVE_INFINITY
        var south = Double.POSITIVE_INFINITY
        var east = Double.NEGATIVE_INFINITY
        var west = Double.POSITIVE_INFINITY
        for ((lat, lon) in routePoints) {
            if (!lat.isFinite() || !lon.isFinite()) continue
            north = maxOf(north, lat)
            south = minOf(south, lat)
            east = maxOf(east, lon)
            west = minOf(west, lon)
        }
        if (!north.isFinite() || !south.isFinite()) return@post
        // Tiny padding so a short route still has context around it.
        val latPad = ((north - south) * 0.12).coerceAtLeast(0.002)
        val lonPad = ((east - west) * 0.12).coerceAtLeast(0.002)
        val box = BoundingBox(
            north + latPad,
            east + lonPad,
            south - latPad,
            west - lonPad,
        )
        val padPx = (minOf(width, height) * 0.12).toInt().coerceIn(48, 120)
        zoomToBoundingBox(box, false, padPx)
        clampViewportToMetroBounds(selectedCity)
        invalidate()
    }
}

/** Rotate map so travel direction points toward the top of the screen (heading-up). */
private fun MapView.applyTripHeadingUp(bearingDeg: Float) {
    if (!bearingDeg.isFinite()) return
    val target = ((-bearingDeg) % 360f + 360f) % 360f
    val current = ((mapOrientation % 360f) + 360f) % 360f
    var delta = target - current
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    if (abs(delta) < TRIP_HEADING_UPDATE_MIN_DEG) return
    mapOrientation = target
}

private fun MapView.resetMapOrientationNorthUp() {
    if (abs(mapOrientation) < 0.5f) return
    mapOrientation = 0f
    invalidate()
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

/**
 * CARTO Voyager — colorful basemap with blue lakes/waterbodies and green parks,
 * plus place / street labels (OpenStreetMap data).
 */
private val CartoVoyagerLabeled: XYTileSource = XYTileSource(
    "CartoVoyagerLabeled18",
    0,
    CartoBasemapUsefulMaxZoom.toInt(),
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
    ),
    "\u00a9 OpenStreetMap contributors \u00b7 \u00a9 CARTO",
)

/** Small solid green dot for GPS (replaces default osmdroid marker). */
private fun buildGpsPinDrawable(context: Context): BitmapDrawable =
    buildTripDotDrawable(context, "#4ADE80", sizeDp = 11f)

/** Solid colored map dot for trip start / destination. */
private fun buildTripDotDrawable(
    context: Context,
    colorHex: String,
    sizeDp: Float = 14f,
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val size = (sizeDp * d).toInt().coerceIn(12, 32)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f
    val r = (size / 2f - 0.6f * d).coerceAtLeast(3.5f)
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor(colorHex)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, r + 1.2f * d, ring)
    canvas.drawCircle(cx, cy, r, fill)
    return BitmapDrawable(context.resources, bmp).apply { setBounds(0, 0, size, size) }
}

/**
 * Moving-trip pointer: top-down car / bike / footsteps with forward at the top of the bitmap.
 * Used with [Marker.setFlat] + GPS bearing so the icon tracks travel direction while navigating.
 */
private fun buildTripVehiclePointerDrawable(
    context: Context,
    vehicle: TripVehicle,
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val size = (34f * d).toInt().coerceIn(28, 72)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f
    val accent = when (vehicle) {
        TripVehicle.CAR -> AndroidColor.parseColor("#1D4ED8")
        TripVehicle.BIKE -> AndroidColor.parseColor("#0F766E")
        TripVehicle.WALK -> AndroidColor.parseColor("#A16207")
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * d
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
        alpha = 235
    }
    // Soft disc so the icon stays readable on dark / busy tiles.
    canvas.drawCircle(cx, cy, size * 0.47f, soft)
    canvas.drawCircle(cx, cy, size * 0.47f, stroke)

    when (vehicle) {
        TripVehicle.CAR -> drawTopDownCarPointer(canvas, cx, cy, d, fill, stroke)
        TripVehicle.BIKE -> drawTopDownBikePointer(canvas, cx, cy, d, fill, stroke)
        TripVehicle.WALK -> drawFootstepsPointer(canvas, cx, cy, d, fill, stroke)
    }
    return BitmapDrawable(context.resources, bmp).apply { setBounds(0, 0, size, size) }
}

/** Top-down car; hood toward top (forward). */
private fun drawTopDownCarPointer(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    d: Float,
    fill: Paint,
    stroke: Paint,
) {
    val body = android.graphics.Path().apply {
        // Slightly tapered hood at top.
        moveTo(cx - 4.2f * d, cy - 9.5f * d)
        lineTo(cx + 4.2f * d, cy - 9.5f * d)
        lineTo(cx + 6.2f * d, cy - 3.5f * d)
        lineTo(cx + 6.2f * d, cy + 8.5f * d)
        lineTo(cx - 6.2f * d, cy + 8.5f * d)
        lineTo(cx - 6.2f * d, cy - 3.5f * d)
        close()
    }
    canvas.drawPath(body, fill)
    canvas.drawPath(body, stroke)
    val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
        alpha = 200
    }
    canvas.drawRoundRect(
        cx - 3.6f * d,
        cy - 6.5f * d,
        cx + 3.6f * d,
        cy - 1.5f * d,
        1.2f * d,
        1.2f * d,
        glass,
    )
    canvas.drawRoundRect(
        cx - 3.6f * d,
        cy + 1.5f * d,
        cx + 3.6f * d,
        cy + 5.5f * d,
        1.2f * d,
        1.2f * d,
        glass,
    )
    // Wheels
    val wheel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#111827")
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cx - 7.4f * d, cy - 5.5f * d, cx - 5.6f * d, cy - 1.5f * d, d, d, wheel)
    canvas.drawRoundRect(cx + 5.6f * d, cy - 5.5f * d, cx + 7.4f * d, cy - 1.5f * d, d, d, wheel)
    canvas.drawRoundRect(cx - 7.4f * d, cy + 2.5f * d, cx - 5.6f * d, cy + 6.5f * d, d, d, wheel)
    canvas.drawRoundRect(cx + 5.6f * d, cy + 2.5f * d, cx + 7.4f * d, cy + 6.5f * d, d, d, wheel)
}

/** Simple bike silhouette facing up. */
private fun drawTopDownBikePointer(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    d: Float,
    fill: Paint,
    stroke: Paint,
) {
    val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fill.color
        style = Paint.Style.STROKE
        strokeWidth = 2.1f * d
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    // Wheels
    canvas.drawCircle(cx - 5.5f * d, cy + 5f * d, 4.4f * d, stroke)
    canvas.drawCircle(cx + 5.5f * d, cy + 5f * d, 4.4f * d, stroke)
    canvas.drawCircle(cx - 5.5f * d, cy + 5f * d, 2.2f * d, fill)
    canvas.drawCircle(cx + 5.5f * d, cy + 5f * d, 2.2f * d, fill)
    // Frame + handlebar (forward / top)
    canvas.drawLine(cx - 5.5f * d, cy + 5f * d, cx, cy - 1f * d, frame)
    canvas.drawLine(cx, cy - 1f * d, cx + 5.5f * d, cy + 5f * d, frame)
    canvas.drawLine(cx - 5.5f * d, cy + 5f * d, cx + 5.5f * d, cy + 5f * d, frame)
    canvas.drawLine(cx, cy - 1f * d, cx, cy - 7.5f * d, frame)
    canvas.drawLine(cx - 3.5f * d, cy - 7.5f * d, cx + 3.5f * d, cy - 7.5f * d, frame)
    canvas.drawCircle(cx, cy - 1f * d, 1.5f * d, fill)
}

/** Pair of footprints; toes toward top (forward). */
private fun drawFootstepsPointer(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    d: Float,
    fill: Paint,
    stroke: Paint,
) {
    fun foot(centerX: Float, centerY: Float, angleDeg: Float) {
        canvas.save()
        canvas.rotate(angleDeg, centerX, centerY)
        val sole = android.graphics.Path().apply {
            addOval(
                centerX - 2.6f * d,
                centerY - 5.5f * d,
                centerX + 2.6f * d,
                centerY + 2.2f * d,
                android.graphics.Path.Direction.CW,
            )
        }
        canvas.drawPath(sole, fill)
        canvas.drawPath(sole, stroke)
        // Heel pad
        canvas.drawCircle(centerX, centerY + 3.6f * d, 2.1f * d, fill)
        canvas.drawCircle(centerX, centerY + 3.6f * d, 2.1f * d, stroke)
        canvas.restore()
    }
    foot(cx - 4.2f * d, cy - 1.5f * d, -12f)
    foot(cx + 4.2f * d, cy + 2.5f * d, 12f)
}

private fun Location.isTripMoving(): Boolean {
    if (!hasSpeed()) return false
    // ~1.5 km/h — walking or faster counts as moving.
    return speed >= 0.4f
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
    tripModeEnabled: Boolean,
    tripNavActive: Boolean,
    tripStreetFollow: Boolean = true,
    tripDeviceHeadingDeg: Float? = null,
    tripVehicle: TripVehicle = TripVehicle.CAR,
    tripRoutePoints: List<Pair<Double, Double>>,
    tripRouteTraveledMeters: Double = 0.0,
    tripAlerts: List<TripPotholeAlert>,
    tripStartLat: Double?,
    tripStartLon: Double?,
    tripDestinationLat: Double?,
    tripDestinationLon: Double?,
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
    val lastTripModeEnabled = remember { mutableStateOf(false) }
    val lastTripRouteSize = remember { mutableIntStateOf(-1) }
    val lastTripAlertsSize = remember { mutableIntStateOf(-1) }
    val lastTripTraveledMeters = remember { mutableFloatStateOf(-1f) }
    val areaHeatEnabledHolder = remember { object { var enabled: Boolean = false } }
    areaHeatEnabledHolder.enabled = areaHeatEnabled
    val tripDestMarkerRef = remember { arrayOfNulls<Marker>(1) }
    val tripStartMarkerRef = remember { arrayOfNulls<Marker>(1) }
    val tripMovingPointerActive = remember { booleanArrayOf(false) }
    val tripPointerVehicleSlot = remember { arrayOfNulls<TripVehicle>(1) }
    val tripPointerLastBearing = remember { floatArrayOf(Float.NaN) }
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
    val mapLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(mapLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val map = mapHolder.map ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        mapLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { mapLifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
                cityOutlineDecor.tripNav?.let { overlay ->
                    overlay.clear(m)
                    overlay.detachFrom(m)
                }
                tripDestMarkerRef[0]?.let { m.overlays.remove(it) }
                tripDestMarkerRef[0] = null
                tripStartMarkerRef[0]?.let { m.overlays.remove(it) }
                tripStartMarkerRef[0] = null
                cityOutlineDecor.cityName = null
                cityOutlineDecor.clusters = null
                cityOutlineDecor.areaHeat = null
                cityOutlineDecor.dim = null
                cityOutlineDecor.edge = null
                cityOutlineDecor.tripNav = null
                m.onPause()
                m.onDetach()
            }
            mapHolder.map = null
            mapViewRef[0] = null
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
    LaunchedEffect(
        tripModeEnabled,
        tripNavActive,
        tripRoutePoints,
        tripRouteTraveledMeters,
        tripAlerts,
        tripStartLat,
        tripStartLon,
        tripDestinationLat,
        tripDestinationLon,
        mapSessionId,
    ) {
        var map = mapHolder.map
        var tries = 0
        while (map == null && tries < 30) {
            delay(16)
            map = mapHolder.map
            tries++
        }
        map ?: return@LaunchedEffect
        refreshTripNavigationOverlay(
            map = map,
            decor = cityOutlineDecor,
            tripModeEnabled = tripModeEnabled,
            routePoints = tripRoutePoints,
            traveledMeters = tripRouteTraveledMeters,
            alerts = tripAlerts,
        )
        syncTripEndpointDots(
            map = map,
            startSlot = tripStartMarkerRef,
            destSlot = tripDestMarkerRef,
            show = tripModeEnabled &&
                (tripRoutePoints.size >= 2 || tripDestinationLat != null),
            startLat = tripStartLat,
            startLon = tripStartLon,
            destLat = tripDestinationLat,
            destLon = tripDestinationLon,
        )
        lastTripModeEnabled.value = tripModeEnabled
        lastTripRouteSize.intValue = tripRoutePoints.size
        lastTripAlertsSize.intValue = tripAlerts.size
        lastTripTraveledMeters.floatValue = tripRouteTraveledMeters.toFloat()
        map.invalidate()
        map.postInvalidate()
    }

    val zoomHideJob = remember { object { var job: Job? = null } }
    val programmaticCameraMoveRef = remember { booleanArrayOf(false) }
    fun runProgrammaticCamera(action: (MapView) -> Unit) {
        val map = mapHolder.map ?: return
        programmaticCameraMoveRef[0] = true
        action(map)
        map.postDelayed({ programmaticCameraMoveRef[0] = false }, 450L)
    }

    /** Trip Navigate → street-level heading-up POV (not city overview). */
    var tripStreetFocusDone by remember { mutableStateOf(false) }
    var tripFollowEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(tripNavActive) {
        if (!tripNavActive) {
            tripStreetFocusDone = false
            tripFollowEnabled = false
            mapHolder.map?.resetMapOrientationNorthUp()
            return@LaunchedEffect
        }
        tripFollowEnabled = true
        var map = mapHolder.map
        var tries = 0
        while (map == null && tries < 40) {
            delay(16)
            map = mapHolder.map
            tries++
        }
        map ?: return@LaunchedEffect
        delay(80)
        if (!tripNavActive || tripStreetFocusDone) return@LaunchedEffect
        val focus = gpsPinLocation
            ?: tripStartLat?.let { lat ->
                tripStartLon?.let { lon ->
                    Location("trip-start").apply {
                        latitude = lat
                        longitude = lon
                    }
                }
            }
            ?: return@LaunchedEffect
        runProgrammaticCamera {
            val street = tripStreetFollow
            it.scheduleFollowTripLocation(
                selectedCity,
                focus.withFusedNavHeading(tripDeviceHeadingDeg),
                headingUp = street && fuseNavHeadingDeg(tripDeviceHeadingDeg, focus) != null,
                zoom = if (street) TRIP_FOLLOW_ZOOM else TRIP_NAV_OVERVIEW_ZOOM,
            )
        }
        onMapCameraSnapshot(
            focus.latitude,
            focus.longitude,
            if (tripStreetFollow) TRIP_FOLLOW_ZOOM else TRIP_NAV_OVERVIEW_ZOOM,
        )
        tripStreetFocusDone = true
    }
    // If Navigate ran before GPS was ready, enter street POV once a fix arrives.
    LaunchedEffect(
        tripNavActive,
        tripStreetFocusDone,
        gpsPinLocation?.latitude,
        gpsPinLocation?.longitude,
    ) {
        if (!tripNavActive || tripStreetFocusDone) return@LaunchedEffect
        val gps = gpsPinLocation ?: return@LaunchedEffect
        var map = mapHolder.map
        var tries = 0
        while (map == null && tries < 30) {
            delay(16)
            map = mapHolder.map
            tries++
        }
        map ?: return@LaunchedEffect
        runProgrammaticCamera {
            val street = tripStreetFollow
            it.scheduleFollowTripLocation(
                selectedCity,
                gps.withFusedNavHeading(tripDeviceHeadingDeg),
                headingUp = street && fuseNavHeadingDeg(tripDeviceHeadingDeg, gps) != null,
                zoom = if (street) TRIP_FOLLOW_ZOOM else TRIP_NAV_OVERVIEW_ZOOM,
            )
        }
        onMapCameraSnapshot(
            gps.latitude,
            gps.longitude,
            if (tripStreetFollow) TRIP_FOLLOW_ZOOM else TRIP_NAV_OVERVIEW_ZOOM,
        )
        tripStreetFocusDone = true
    }
    // If still on city overview when the user begins moving, force closer street follow.
    LaunchedEffect(
        tripNavActive,
        gpsPinLocation?.speed,
        gpsPinLocation?.bearing,
    ) {
        if (!tripNavActive) return@LaunchedEffect
        val gps = gpsPinLocation ?: return@LaunchedEffect
        if (!gps.isTripMoving()) return@LaunchedEffect
        val map = mapHolder.map ?: return@LaunchedEffect
        if (!map.isAtCityOverviewZoom() && tripStreetFocusDone && map.zoomLevelDouble >= TRIP_FOLLOW_ZOOM - 0.2) {
            // Already close enough; heading-up is applied by the follow LaunchedEffect.
            return@LaunchedEffect
        }
        runProgrammaticCamera {
            it.scheduleFollowTripLocation(
                selectedCity,
                gps.withFusedNavHeading(tripDeviceHeadingDeg),
                headingUp = tripStreetFollow,
                zoom = if (tripStreetFollow) TRIP_FOLLOW_ZOOM else TRIP_NAV_OVERVIEW_ZOOM,
            )
        }
        onMapCameraSnapshot(
            gps.latitude,
            gps.longitude,
            if (tripStreetFollow) TRIP_FOLLOW_ZOOM else TRIP_NAV_OVERVIEW_ZOOM,
        )
        tripStreetFocusDone = true
    }

    /** Before Navigate: north-up overview that fits the full planned route. */
    LaunchedEffect(
        tripNavActive,
        tripModeEnabled,
        tripRoutePoints,
        mapSessionId,
    ) {
        if (tripNavActive || !tripModeEnabled || tripRoutePoints.size < 2) return@LaunchedEffect
        var map = mapHolder.map
        var tries = 0
        while (map == null && tries < 40) {
            delay(16)
            map = mapHolder.map
            tries++
        }
        map ?: return@LaunchedEffect
        delay(60)
        if (tripNavActive) return@LaunchedEffect
        runProgrammaticCamera {
            it.scheduleFitRoutePreview(tripRoutePoints, selectedCity)
        }
        val mid = tripRoutePoints[tripRoutePoints.size / 2]
        onMapCameraSnapshot(mid.first, mid.second, map.zoomLevelDouble)
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
                if (tripNavActive) tripFollowEnabled = false
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
            if (tripNavActive && mapUserInteracting) tripFollowEnabled = false
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

    /** Keep the GPS pointer centered; street-follow = close zoom + heading-up. */
    LaunchedEffect(
        tripNavActive,
        tripFollowEnabled,
        tripStreetFollow,
        mapUserInteracting,
        gpsPinLocation?.latitude,
        gpsPinLocation?.longitude,
        gpsPinLocation?.bearing,
        gpsPinLocation?.speed,
        tripDeviceHeadingDeg,
    ) {
        if (!tripNavActive || mapUserInteracting) return@LaunchedEffect
        val gps = gpsPinLocation ?: return@LaunchedEffect
        val map = mapHolder.map ?: return@LaunchedEffect
        if (tripStreetFollow) {
            if (!tripFollowEnabled) return@LaunchedEffect
            runProgrammaticCamera {
                map.scheduleFollowTripLocation(
                    selectedCity,
                    gps.withFusedNavHeading(tripDeviceHeadingDeg),
                    headingUp = true,
                    zoom = TRIP_FOLLOW_ZOOM,
                )
            }
        } else {
            runProgrammaticCamera {
                map.scheduleFollowTripLocation(
                    selectedCity,
                    gps.withFusedNavHeading(tripDeviceHeadingDeg),
                    headingUp = false,
                    zoom = TRIP_NAV_OVERVIEW_ZOOM,
                )
            }
        }
    }

    LaunchedEffect(tripStreetFollow, tripNavActive) {
        if (!tripNavActive) return@LaunchedEffect
        if (tripStreetFollow) {
            tripFollowEnabled = true
        } else {
            mapHolder.map?.resetMapOrientationNorthUp()
        }
    }

    LaunchedEffect(tripFollowEnabled, tripNavActive, tripStreetFollow) {
        if (tripNavActive && tripStreetFollow && !tripFollowEnabled) {
            mapHolder.map?.resetMapOrientationNorthUp()
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
                    setTileSource(CartoVoyagerLabeled)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = false
                    setHorizontalMapRepetitionEnabled(false)
                    setVerticalMapRepetitionEnabled(false)
                    maxZoomLevel = CartoBasemapUsefulMaxZoom
                    @Suppress("DEPRECATION")
                    setBuiltInZoomControls(false)
                    applyMetroPanBounds(selectedCityHolder.city)
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
                    onResume()
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
                if (
                    tripModeEnabled != lastTripModeEnabled.value ||
                    tripRoutePoints.size != lastTripRouteSize.intValue ||
                    tripAlerts.size != lastTripAlertsSize.intValue ||
                    kotlin.math.abs(tripRouteTraveledMeters.toFloat() - lastTripTraveledMeters.floatValue) > 2f
                ) {
                    refreshTripNavigationOverlay(
                        map = view,
                        decor = cityOutlineDecor,
                        tripModeEnabled = tripModeEnabled,
                        routePoints = tripRoutePoints,
                        traveledMeters = tripRouteTraveledMeters,
                        alerts = tripAlerts,
                    )
                    syncTripEndpointDots(
                        map = view,
                        startSlot = tripStartMarkerRef,
                        destSlot = tripDestMarkerRef,
                        show = tripModeEnabled &&
                            (tripRoutePoints.size >= 2 || tripDestinationLat != null),
                        startLat = tripStartLat,
                        startLon = tripStartLon,
                        destLat = tripDestinationLat,
                        destLon = tripDestinationLon,
                    )
                    lastTripModeEnabled.value = tripModeEnabled
                    lastTripRouteSize.intValue = tripRoutePoints.size
                    lastTripAlertsSize.intValue = tripAlerts.size
                    lastTripTraveledMeters.floatValue = tripRouteTraveledMeters.toFloat()
                    view.invalidate()
                } else if (tripModeEnabled) {
                    syncTripEndpointDots(
                        map = view,
                        startSlot = tripStartMarkerRef,
                        destSlot = tripDestMarkerRef,
                        show = tripRoutePoints.size >= 2 || tripDestinationLat != null,
                        startLat = tripStartLat,
                        startLon = tripStartLon,
                        destLat = tripDestinationLat,
                        destLon = tripDestinationLon,
                    )
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
                // Vehicle cursor for the whole navigation session (not only while speed > 0).
                val showMovePointer = tripNavActive && gps != null
                val moveIconVehicle = tripVehicle
                val fusedHeading = fuseNavHeadingDeg(tripDeviceHeadingDeg, gps)
                val pointerBearing = when {
                    fusedHeading != null -> {
                        tripPointerLastBearing[0] = fusedHeading
                        fusedHeading
                    }
                    tripPointerLastBearing[0].isFinite() -> tripPointerLastBearing[0]
                    else -> 0f
                }
                fun applyVehiclePointer(marker: Marker, vehicle: TripVehicle, bearingDeg: Float) {
                    if (tripPointerVehicleSlot[0] != vehicle || !tripMovingPointerActive[0]) {
                        marker.setIcon(buildTripVehiclePointerDrawable(view.context, vehicle))
                        tripPointerVehicleSlot[0] = vehicle
                    }
                    marker.setFlat(true)
                    marker.setRotation(bearingDeg)
                    marker.isEnabled = true
                    tripMovingPointerActive[0] = true
                    cancelGpsPinAutoHide(view, gpsPinHideRunnable)
                }
                fun applyIdleGpsPin(marker: Marker) {
                    marker.setIcon(buildGpsPinDrawable(view.context))
                    marker.setFlat(false)
                    marker.setRotation(0f)
                    tripMovingPointerActive[0] = false
                    tripPointerVehicleSlot[0] = null
                }
                when {
                    gps == null -> {
                        cancelGpsPinAutoHide(view, gpsPinHideRunnable)
                        lastGpsPinScheduleKey.value = null
                        lastMapLocateEpochForGpsPin.intValue = mapLocateEpoch
                        tripMovingPointerActive[0] = false
                        tripPointerVehicleSlot[0] = null
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
                            setInfoWindow(null)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            isEnabled = true
                            if (showMovePointer) {
                                applyVehiclePointer(this, moveIconVehicle, pointerBearing)
                            } else {
                                applyIdleGpsPin(this)
                            }
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
                        }
                        if (showMovePointer) {
                            applyVehiclePointer(slot, moveIconVehicle, pointerBearing)
                        } else if (tripMovingPointerActive[0]) {
                            applyIdleGpsPin(slot)
                        }
                        view.invalidate()
                    }
                }
                latestGpsPinForTouch[0] = gps?.let { Location(it) }
                if (gps != null && !showMovePointer) {
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
                } else if (showMovePointer) {
                    lastGpsPinScheduleKey.value = null
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
                .padding(
                    end = 10.dp,
                    bottom = if (tripNavActive) 112.dp else 10.dp,
                ),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(mapFabGap),
        ) {
            // Zoom extras only on demand — keeps the stack short so FABs do not collide.
            AnimatedVisibility(
                visible = showZoomControls && !tripNavActive,
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
                }
            }
            if (tripNavActive) {
                // Compact trip stack: zoom in/out only (no city / 4-step buttons).
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
                                contentDescription = "Zoom out",
                                modifier = Modifier.size(mapFabIcon),
                            )
                        }
                    }
                }
            }
            SmallFloatingActionButton(
                onClick = {
                    if (tripNavActive) {
                        tripFollowEnabled = true
                        gpsPinLocation?.let { gps ->
                            runProgrammaticCamera {
                                it.scheduleFollowTripLocation(
                                    selectedCity,
                                    gps.withFusedNavHeading(tripDeviceHeadingDeg),
                                    headingUp = tripStreetFollow,
                                    zoom = if (tripStreetFollow) {
                                        TRIP_FOLLOW_ZOOM
                                    } else {
                                        TRIP_NAV_OVERVIEW_ZOOM
                                    },
                                )
                            }
                        }
                    } else {
                        onLocateMe()
                    }
                },
                containerColor = Color.White,
                contentColor = if (tripNavActive && tripFollowEnabled) {
                    Color(0xFF15803D)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                shape = mapFabShape,
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = if (tripNavActive) {
                        if (tripFollowEnabled) "Following current GPS location" else "Resume following GPS"
                    } else {
                        "Trace to current GPS location"
                    },
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

/**
 * Rejects stale, very coarse, or physically implausible navigation fixes.
 * Fresh GPS fixes are strongly preferred over network fixes.
 */
private fun shouldAcceptNavigationLocation(current: Location?, candidate: Location): Boolean {
    if (!candidate.latitude.isFinite() || !candidate.longitude.isFinite()) return false
    if (!candidate.hasAccuracy()) return false
    val isGps = candidate.provider == LocationManager.GPS_PROVIDER
    // Tighter accuracy gate: GPS ≤ 40 m, network ≤ 25 m (network is a last resort).
    if (candidate.accuracy > if (isGps) 40f else 25f) return false
    val now = System.currentTimeMillis()
    val candidateAgeMs = (now - candidate.time).coerceAtLeast(0L)
    if (candidateAgeMs > 8_000L) return false
    current ?: return true

    val currentAgeMs = (now - current.time).coerceAtLeast(0L)
    val currentIsGps = current.provider == LocationManager.GPS_PROVIDER
    // Never let network/passive replace a recent GPS fix.
    if (currentIsGps && !isGps && currentAgeMs < 12_000L) return false
    if (candidate.time < current.time && currentAgeMs < 5_000L) return false

    val elapsedSeconds = ((candidate.time - current.time).coerceAtLeast(250L) / 1_000.0)
    val distanceMeters = current.distanceTo(candidate).toDouble()
    val plausibleJumpMeters =
        20.0 + current.accuracy.coerceAtLeast(0f) + candidate.accuracy + elapsedSeconds * 55.0
    if (distanceMeters > plausibleJumpMeters) return false

    // Prefer continuous satellite updates; accept coarser fixes only if GPS went stale.
    return candidate.accuracy <= 25f || currentAgeMs >= 6_000L || isGps
}

private fun locationQualityScore(location: Location): Float {
    val ageSec = ((System.currentTimeMillis() - location.time).coerceAtLeast(0L) / 1000f)
        .coerceAtMost(240f)
    val providerPenalty = when (location.provider) {
        LocationManager.GPS_PROVIDER -> 0f
        LocationManager.NETWORK_PROVIDER -> 18f
        LocationManager.PASSIVE_PROVIDER -> 22f
        else -> 10f
    }
    // Lower score is better: GPS provider first, then accuracy, then freshness.
    return providerPenalty + location.accuracy.coerceAtLeast(1f) + ageSec * 0.45f
}

private fun shouldSeekMorePrecision(location: Location?): Boolean {
    if (location == null) return true
    val ageMs = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
    val isGps = location.provider == LocationManager.GPS_PROVIDER
    return !isGps || location.accuracy > 12f || ageMs > 8_000L
}

private fun chooseBetterLocation(current: Location?, candidate: Location?): Location? {
    if (candidate == null) return current
    if (current == null) return candidate
    val now = System.currentTimeMillis()
    val currentAge = (now - current.time).coerceAtLeast(0L)
    val candidateAge = (now - candidate.time).coerceAtLeast(0L)
    val currentGps = current.provider == LocationManager.GPS_PROVIDER
    val candidateGps = candidate.provider == LocationManager.GPS_PROVIDER
    if (currentGps && !candidateGps && currentAge < 12_000L) return current
    if (candidateGps && !currentGps && candidateAge < 12_000L) return candidate
    return if (locationQualityScore(candidate) < locationQualityScore(current)) candidate else current
}

/**
 * Pick trip origin: never replace a fresh accurate live pin with a worse one-shot.
 */
private fun pickBestTripOrigin(livePin: Location?, freshGps: Location?): Location? {
    if (livePin == null) return freshGps
    if (freshGps == null) return livePin
    val now = System.currentTimeMillis()
    val liveAge = (now - livePin.time).coerceAtLeast(0L)
    val liveGps = livePin.provider == LocationManager.GPS_PROVIDER ||
        livePin.provider == null ||
        livePin.provider == "saved" ||
        livePin.provider == "gps"
    val liveAccurate = livePin.hasAccuracy() && livePin.accuracy <= 20f
    // Trust the on-screen pin when it is recent and precise.
    if (liveGps && liveAccurate && liveAge <= 10_000L) {
        // Only take fresh if it is clearly better GPS.
        val freshIsGps = freshGps.provider == LocationManager.GPS_PROVIDER
        val freshBetter = freshIsGps &&
            freshGps.hasAccuracy() &&
            freshGps.accuracy + 3f < livePin.accuracy
        return if (freshBetter) freshGps else livePin
    }
    return chooseBetterLocation(livePin, freshGps)
}

private fun Location.isUsableTripFix(maxAgeMs: Long = 10_000L, maxAccuracyM: Float = 25f): Boolean {
    if (!latitude.isFinite() || !longitude.isFinite()) return false
    if (!hasAccuracy() || accuracy > maxAccuracyM) return false
    val age = (System.currentTimeMillis() - time).coerceAtLeast(0L)
    return age <= maxAgeMs
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

/**
 * GPS-provider-only high-accuracy fix. Does not consult network location, which
 * often sits tens of meters away from the true pin in dense urban areas.
 */
@SuppressLint("MissingPermission")
private suspend fun fetchHighAccuracyGpsLocation(
    context: Context,
    timeoutMs: Long = 10_000L,
): Location? = withContext(Dispatchers.Default) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return@withContext null
        if (!runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
            return@withContext null
        }

        var best = runCatching {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull()?.takeIf { it.isUsableTripFix(maxAgeMs = 15_000L, maxAccuracyM = 35f) }

        val first = requestOneFreshLocation(
            context = context,
            locationManager = lm,
            provider = LocationManager.GPS_PROVIDER,
            timeoutMs = timeoutMs,
        )
        best = chooseBetterLocation(best, first)

        if (shouldSeekMorePrecision(best)) {
            val second = requestOneFreshLocation(
                context = context,
                locationManager = lm,
                provider = LocationManager.GPS_PROVIDER,
                timeoutMs = (timeoutMs * 0.45).toLong().coerceAtLeast(2_500L),
            )
            best = chooseBetterLocation(best, second)
        }

        best?.takeIf { it.provider == LocationManager.GPS_PROVIDER || it.hasAccuracy() }
    } catch (_: Exception) {
        null
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

        // Prefer GPS-only first.
        var best = fetchHighAccuracyGpsLocation(context, timeoutMs = 8_000L)
        if (best != null && best.isUsableTripFix(maxAgeMs = 12_000L, maxAccuracyM = 25f)) {
            return@withContext best
        }

        // Last-known GPS (may still beat network).
        val lastGps = runCatching {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull()
        best = chooseBetterLocation(best, lastGps)

        // Network only if GPS is missing or very poor.
        if (
            (best == null || !best.isUsableTripFix(maxAgeMs = 20_000L, maxAccuracyM = 40f)) &&
            runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        ) {
            val network = requestOneFreshLocation(
                context = context,
                locationManager = lm,
                provider = LocationManager.NETWORK_PROVIDER,
                timeoutMs = 2_500L,
            )
            // Accept network only when we have nothing usable from GPS.
            if (best == null) {
                best = network
            } else if (
                network != null &&
                network.hasAccuracy() &&
                network.accuracy + 15f < best.accuracy
            ) {
                best = network
            }
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

