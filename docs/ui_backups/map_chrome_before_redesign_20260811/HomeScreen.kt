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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
import androidx.lifecycle.repeatOnLifecycle
import com.example.potholereport.BuildConfig
import com.example.potholereport.data.BengaluruGbaBoundary
import com.example.potholereport.data.BengaluruGbaWards
import com.example.potholereport.data.CitizenNotificationsRepository
import com.example.potholereport.data.CitizenUiSettingsRepository
import com.example.potholereport.data.CityLaunchConfig
import com.example.potholereport.data.CityMetroKeys
import com.example.potholereport.data.CityMetroLocation
import com.example.potholereport.data.EmailChangeStartResult
import com.example.potholereport.data.EmailChangeVerifyResult
import com.example.potholereport.data.LoginResult
import com.example.potholereport.data.CityWeatherForecast
import com.example.potholereport.data.CityWeatherRainGrid
import com.example.potholereport.data.CityWeatherRepository
import com.example.potholereport.data.WeatherHorizon
import com.example.potholereport.data.DeviceReporterId
import com.example.potholereport.data.MapCursorColor
import com.example.potholereport.data.MapCursorStyle
import com.example.potholereport.data.TripCaptureCabinSource
import com.example.potholereport.data.TripCaptureCameraMapLayout
import com.example.potholereport.data.TripCaptureDisplayMode
import com.example.potholereport.data.TripCaptureSource
import com.example.potholereport.data.NetworkReachability
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.PotholeSeverity
import com.example.potholereport.data.PotholeReportStatus
import com.example.potholereport.data.RainCriticality
import com.example.potholereport.data.RecentReportsRepository
import com.example.potholereport.data.ReportsStripSort
import com.example.potholereport.data.JourneyStartEta
import com.example.potholereport.data.TripAlertScheduler
import com.example.potholereport.data.TripDangerBlink
import com.example.potholereport.data.TripNavigationMatcher
import com.example.potholereport.data.TripDeviceHeadingTracker
import com.example.potholereport.data.TripDeadReckoning
import com.example.potholereport.data.TripImuBumpAnalyzer
import com.example.potholereport.data.TripImuBumpDetector
import com.example.potholereport.data.TripImuBumpEvent
import com.example.potholereport.data.TripImuBumpLog
import com.example.potholereport.data.TripImuBumpUserResponse
import com.example.potholereport.data.TripRoadKeyframeBuffer
import com.example.potholereport.data.TripAvoidRejoinAnalyzer
import com.example.potholereport.data.TripCrowdHazardStore
import com.example.potholereport.data.TripHazardKind
import com.example.potholereport.data.TripCompanionLan
import com.example.potholereport.data.TripUnconfirmedDetectionStore
import com.example.potholereport.ml.TripBumpVisionGate
import com.example.potholereport.ml.TripBumpVisionPipeline
import com.example.potholereport.ml.TripBumpVisionTier
import com.example.potholereport.data.TripPlaceLabels
import com.example.potholereport.data.TripPotholeAlert
import com.example.potholereport.data.fuseNavHeadingDeg
import com.example.potholereport.data.withFusedNavHeading
import com.example.potholereport.data.TripRouteProgress
import com.example.potholereport.data.TripDeviationAction
import com.example.potholereport.data.TripTurnGuidance
import com.example.potholereport.data.TripPlaceSearch
import com.example.potholereport.data.TripPlaceSearchBounds
import com.example.potholereport.data.TripPlaceSuggestion
import com.example.potholereport.data.TripRecentPlace
import com.example.potholereport.data.TripRecentPlacesRepository
import com.example.potholereport.data.TripSessionRepository
import com.example.potholereport.data.TripRouteOfflineCache
import com.example.potholereport.data.TripSessionSnapshot
import com.example.potholereport.data.SosAssistNavRequest
import com.example.potholereport.data.SosNearbyRepository
import com.example.potholereport.data.TripCameraMode
import com.example.potholereport.data.TripPlayMotionTracker
import com.example.potholereport.data.TripRoutePotholeRanking
import com.example.potholereport.data.TripStreetCamera
import com.example.potholereport.data.TripVoiceGuidance
import com.example.potholereport.data.TripDeviceRole
import com.example.potholereport.ui.settings.CitizenSettingsDialog
import com.example.potholereport.data.formatRecentReportCaption
import com.example.potholereport.data.formatTripPlaceAge
import com.example.potholereport.data.formatTripPotholeAlertLine
import com.example.potholereport.data.formatTripPotholeSideLetter
import com.example.potholereport.data.notifyTripPotholeAlert
import com.example.potholereport.data.tripPotholeAlertAccentArgb
import com.example.potholereport.data.tripPotholeAlertInkArgb
import com.example.potholereport.data.remote.OsrmRouteClient
import com.example.potholereport.data.remote.OsrmRouteResult
import com.example.potholereport.data.remote.OsrmRouteStep
import com.example.potholereport.data.remote.OverpassNearbyClient
import com.example.potholereport.data.remote.NearbyOsmPoi
import com.example.potholereport.data.remote.pickNonOverlappingPois
import com.example.potholereport.data.remote.ReportSyncRepository
import com.example.potholereport.data.remote.TripVehicle
import com.example.potholereport.data.isMeaningfulAreaName
import com.example.potholereport.data.resolveAreaLabel
import com.example.potholereport.data.SignupStartResult
import com.example.potholereport.data.SignupVerifyResult
import com.example.potholereport.ui.profile.CartoonAvatar
import com.example.potholereport.ui.profile.ProfileDialog
import com.example.potholereport.ui.report.NewReportScreen
import com.example.potholereport.ui.theme.PotholeReportTheme
import com.example.potholereport.ui.theme.CitizenBorder
import com.example.potholereport.ui.theme.CitizenSpace
import com.example.potholereport.ui.theme.CitizenType
import com.example.potholereport.ui.theme.CitizenInk
import com.example.potholereport.ui.theme.CitizenSelection
import com.example.potholereport.ui.theme.CitizenIcon
import com.example.potholereport.ui.theme.CitizenHeader
import com.example.potholereport.ui.theme.CitizenRadius
import com.example.potholereport.ui.theme.CitizenMapChrome
import com.example.potholereport.ui.theme.CitizenAccountabilityCopy
import com.example.potholereport.ui.theme.CitizenPrivacyCopy
import com.example.potholereport.ui.theme.CitizenStatTotal
import com.example.potholereport.ui.theme.citizenReportStatusColor
import com.example.potholereport.ui.theme.CitizenSectionHeader
import com.example.potholereport.ui.theme.rememberMapUiScale
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
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    /** Short snackbar confirmations (submit, delete, trip). */
    onFeedbackMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiScale = rememberMapUiScale()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showReportSignInModal by rememberSaveable { mutableStateOf(false) }
    var pendingModalSignupName by rememberSaveable { mutableStateOf("") }
    var pendingModalSignupEmail by rememberSaveable { mutableStateOf("") }
    var pendingModalSignupPassword by rememberSaveable { mutableStateOf("") }
    var pendingModalSignupVerification by rememberSaveable { mutableStateOf(false) }
    /** Fullscreen trip Report opens in-composition (never Nav + MapView teardown). */
    var showInlineNewReport by remember { mutableStateOf(false) }
    var inlineReportCloseUpUri by remember { mutableStateOf<android.net.Uri?>(null) }

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
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showUnconfirmedDialog by remember { mutableStateOf(false) }
    var showGpsEnableDialog by remember { mutableStateOf(false) }
    var mapCursorStyle by remember {
        mutableStateOf(CitizenUiSettingsRepository.mapCursorStyle(context))
    }
    var mapCursorColor by remember {
        mutableStateOf(CitizenUiSettingsRepository.mapCursorColor(context))
    }
    var tripCaptureDisplay by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCaptureDisplayMode(context))
    }
    var tripCaptureLayout by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCaptureCameraMapLayout(context))
    }
    var tripCaptureRoad by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCaptureRoadSource(context))
    }
    var tripCaptureCabin by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCaptureCabinSource(context))
    }
    var tripCompanionPairingCode by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCompanionPairingCode(context))
    }
    var imuBumpEnabled by remember {
        mutableStateOf(CitizenUiSettingsRepository.imuBumpDetectionEnabled(context))
    }
    var swerveDetectionEnabled by remember {
        mutableStateOf(CitizenUiSettingsRepository.swerveDetectionEnabled(context))
    }
    var roadVideoAutoDetectEnabled by remember {
        mutableStateOf(CitizenUiSettingsRepository.roadVideoAutoDetectEnabled(context))
    }
    var showCompanionCamera by remember { mutableStateOf(false) }
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
        // Apply the fix immediately — do not wait on geocode (fails / hangs offline).
        lastGpsLocation = Location(location)
        savedGpsLat = location.latitude
        savedGpsLon = location.longitude
        savedGpsAccuracyM = if (location.hasAccuracy()) location.accuracy else null
        userLocation = Location(location)
        gpsMetroCity = cityForLocation(location)
        SosNearbyRepository.refreshIfNeeded(location.latitude, location.longitude)
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
        // Fresh precise pin → start routing immediately (no 10s GPS wait).
        if (live != null && live.isUsableTripFix(maxAgeMs = 5_000L, maxAccuracyM = 20f)) {
            return live
        }
        val refined = fetchHighAccuracyGpsLocation(context, timeoutMs = 2_500L)
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
     * to the last known pin. Used so Trip "My location" start matches the green locate pin.
     */
    suspend fun refreshCalibratedGpsPin(bumpMapLocate: Boolean = true): Location? {
        val live = lastGpsLocation?.let { Location(it) }
        val fresh = fetchHighAccuracyGpsLocation(context, timeoutMs = 3_000L)
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

    // Keep SOS Medical + Roadside places warm from live GPS — do not wait for SOS open.
    val sosPrefetchLifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lastGpsLocation?.latitude, lastGpsLocation?.longitude) {
        val loc = lastGpsLocation ?: return@LaunchedEffect
        SosNearbyRepository.refreshIfNeeded(loc.latitude, loc.longitude)
    }
    LaunchedEffect(sosPrefetchLifecycle) {
        sosPrefetchLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                val loc = lastGpsLocation
                if (loc != null) {
                    SosNearbyRepository.refreshIfNeeded(loc.latitude, loc.longitude)
                } else if (hasLocationPermission(context) && isDeviceLocationEnabled(context)) {
                    val fresh = withContext(Dispatchers.IO) {
                        runCatching { fetchCalibratedLocation(context) }.getOrNull()
                    }
                    if (fresh != null) {
                        lastGpsLocation = Location(fresh)
                        SosNearbyRepository.refreshIfNeeded(fresh.latitude, fresh.longitude)
                    }
                }
                delay(45_000L)
            }
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
                    if (!shouldAcceptNavigationLocation(
                            current = lastGpsLocation,
                            candidate = location,
                            navigationActive = tripNavigationActive,
                        )
                    ) {
                        return
                    }
                    val accepted = Location(location)
                    lastGpsLocation = accepted
                    savedGpsLat = accepted.latitude
                    savedGpsLon = accepted.longitude
                    savedGpsAccuracyM = if (accepted.hasAccuracy()) accepted.accuracy else null
                    userLocation = Location(accepted)
                    gpsMetroCity = cityForLocation(accepted)
                    SosNearbyRepository.refreshIfNeeded(accepted.latitude, accepted.longitude)
                }
            }
            // Satellite GPS is the offline-capable source — poll it tightly during trips.
            if (runCatching {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                }.getOrDefault(false)
            ) {
                runCatching {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        250L,
                        0.5f,
                        listener,
                    )
                }
            }
            // Network / passive are assist only (often dead without internet).
            if (runCatching {
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                }.getOrDefault(false)
            ) {
                runCatching {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        8_000L,
                        12f,
                        listener,
                    )
                }
            }
            if (runCatching {
                    locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
                }.getOrDefault(false)
            ) {
                runCatching {
                    locationManager.requestLocationUpdates(
                        LocationManager.PASSIVE_PROVIDER,
                        1_000L,
                        1f,
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
    // Back-to-exit-fullscreen is handled inside ReportAndTrackSection so trip can
    // turn off when collapsing without an active navigation session.

    // Always consume system back while a report overlay is up — otherwise a bottom
    // edge tap can fall through to Activity finish (looks like the app "minimized").
    BackHandler(enabled = showInlineNewReport) {
        showInlineNewReport = false
        inlineReportCloseUpUri = null
    }
    BackHandler(enabled = showReportSignInModal) {
        showReportSignInModal = false
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!mapFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CitizenHeader.Padding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CITY GRID",
                    style = CitizenType.Brand,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.weight(1f))
                Spacer(modifier = Modifier.width(CitizenSpace.Sm))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSignedIn) {
                        NotificationsHeaderBell(
                            unreadCount = unreadNotificationCount,
                            onClick = { showNotificationsDialog = true },
                        )
                        Spacer(modifier = Modifier.width(CitizenSpace.Xs))
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
                        onOpenSettings = {
                            if (isSignedIn) showSettingsDialog = true
                        },
                        onOpenUnconfirmed = {
                            if (isSignedIn) showUnconfirmedDialog = true
                        },
                        onShowAbout = { showAboutDialog = true },
                    )

                    Spacer(modifier = Modifier.width(CitizenSpace.Sm))

                    Button(
                        onClick = onEmergencyClick,
                        modifier = Modifier
                            .height(CitizenIcon.CapsuleCompact)
                            .defaultMinSize(minWidth = 56.dp, minHeight = CitizenIcon.CapsuleCompact),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB74233)),
                        shape = CitizenRadius.Capsule,
                        contentPadding = PaddingValues(
                            horizontal = CitizenSpace.Md,
                            vertical = 0.dp,
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(CitizenIcon.Dense),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(CitizenSpace.Xs))
                        Text(
                            "SOS",
                            style = CitizenType.ChipCompact,
                            fontWeight = FontWeight.Bold,
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
                                    // Keep scroll always enabled — toggling enabled mid-gesture
                                    // recomposes this Column and stutters MapLibre pan/zoom.
                                    // MapView requestDisallowInterceptTouchEvent owns the gesture.
                                    Modifier.verticalScroll(scrollState)
                                },
                            ),
                    ) {
                        if (!mapFullscreen) Spacer(modifier = Modifier.height(CitizenSpace.Md))
                        ReportAndTrackSection(
                            isSignedIn = isSignedIn,
                            onReportPotholeGuest = { showReportSignInModal = true },
                            onReportPotholeSignedIn = { onOpenNewReport(selectedCity) },
                            onRequestReportFromFullscreenMap = {
                                // Open immediately on top of the live map. Do not tear down
                                // MapView or delay — that left a BackHandler gap that finished
                                // the Activity (app looked like it minimized).
                                if (isSignedIn) {
                                    coroutineScope.launch {
                                        inlineReportCloseUpUri = resolveTripRoadCloseUpUri(
                                            context,
                                            tripCaptureRoad,
                                        )
                                        showInlineNewReport = true
                                    }
                                } else {
                                    showReportSignInModal = true
                                }
                            },
                            suppressFullscreenBack = showInlineNewReport || showReportSignInModal,
                            mapCoveredByOverlay = showInlineNewReport || showReportSignInModal,
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
                                        // Immediate feedback: snap to last known GPS first, then refine.
                                        // Only bump locate once — a second bump after calibration felt like
                                        // an automatic zoom a few seconds later.
                                        val hadPin = lastGpsLocation != null
                                        lastGpsLocation?.let { last ->
                                            userLocation = Location(last)
                                            val city = cityForLocation(last)
                                            gpsMetroCity = city
                                            if (city != null && city != selectedCity && CityLaunchConfig.isCityEnabled(city)) {
                                                selectedCity = city
                                            }
                                            mapLocateEpoch++
                                        }
                                        refreshCalibratedGpsPin(bumpMapLocate = !hadPin)
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
                            onMapTouchChanged = { },
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
                            onFeedbackMessage = onFeedbackMessage,
                            mapCursorStyle = mapCursorStyle,
                            mapCursorColor = mapCursorColor,
                            tripCaptureDisplay = tripCaptureDisplay,
                            tripCaptureLayout = tripCaptureLayout,
                            tripCaptureRoad = tripCaptureRoad,
                            tripCaptureCabin = tripCaptureCabin,
                            tripCompanionPairingCode = tripCompanionPairingCode,
                            imuBumpEnabled = imuBumpEnabled,
                            swerveDetectionEnabled = swerveDetectionEnabled,
                            roadVideoAutoDetectEnabled = roadVideoAutoDetectEnabled,
                            reporterUserId = if (isSignedIn) anonymousUserId else "",
                            onNotificationsMutated = { notificationsBadgeEpoch++ },
                        )
                        if (!mapFullscreen) Spacer(modifier = Modifier.height(CitizenSpace.Lg))
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
                            Spacer(modifier = Modifier.height(CitizenSpace.Md))
                            MyReportsSection(
                                reporterUserId = anonymousUserId,
                                recentReportsEpoch = recentReportsEpoch,
                                onReportsMutated = onRecentReportsMutated,
                                onFeedbackMessage = onFeedbackMessage,
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
                            .verticalScroll(scrollState),
                    ) {
                        Spacer(modifier = Modifier.height(CitizenSpace.Md))
                        AccountabilitySection(recentReportsEpoch = recentReportsEpoch)
                        Spacer(modifier = Modifier.height(CitizenSpace.Lg))
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
            onDismiss = {
                showReportSignInModal = false
            },
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

    if (showInlineNewReport) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(50f)
                .background(MaterialTheme.colorScheme.background),
        ) {
            NewReportScreen(
                reportCityKey = selectedCity,
                reporterUserId = if (isSignedIn && anonymousUserId.isNotBlank()) {
                    anonymousUserId
                } else {
                    DeviceReporterId.getOrCreate(context)
                },
                submittedWhileSignedIn = isSignedIn,
                initialCloseUpUri = inlineReportCloseUpUri,
                onClose = {
                    showInlineNewReport = false
                    inlineReportCloseUpUri = null
                },
                onReportPersisted = onRecentReportsMutated,
                onSyncMessage = onFeedbackMessage,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showGpsEnableDialog) {
        AlertDialog(
            onDismissRequest = { showGpsEnableDialog = false },
            title = { Text("Turn on location", style = CitizenType.ScreenTitle) },
            text = {
                Text(
                    "Location is off. Tap Enable to allow location and turn on GPS so we can place you on the map and tag reports.",
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
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
                    Text("Enable", style = CitizenType.Label)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsEnableDialog = false }) {
                    Text("Not now", style = CitizenType.Label, color = CitizenInk.Helper)
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

    if (showUnconfirmedDialog) {
        UnconfirmedDetectionsDialog(
            cityKey = selectedCity,
            reporterUserId = anonymousUserId,
            signedIn = isSignedIn,
            onDismiss = { showUnconfirmedDialog = false },
            onReportsMutated = onRecentReportsMutated,
            onFeedbackMessage = onFeedbackMessage,
        )
    }

    if (showSettingsDialog) {
        CitizenSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onSettingsChanged = {
                mapCursorStyle = CitizenUiSettingsRepository.mapCursorStyle(context)
                mapCursorColor = CitizenUiSettingsRepository.mapCursorColor(context)
                tripCaptureDisplay = CitizenUiSettingsRepository.tripCaptureDisplayMode(context)
                tripCaptureLayout = CitizenUiSettingsRepository.tripCaptureCameraMapLayout(context)
                tripCaptureRoad = CitizenUiSettingsRepository.tripCaptureRoadSource(context)
                tripCaptureCabin = CitizenUiSettingsRepository.tripCaptureCabinSource(context)
                tripCompanionPairingCode =
                    CitizenUiSettingsRepository.tripCompanionPairingCode(context)
                imuBumpEnabled = CitizenUiSettingsRepository.imuBumpDetectionEnabled(context)
                swerveDetectionEnabled =
                    CitizenUiSettingsRepository.swerveDetectionEnabled(context)
                roadVideoAutoDetectEnabled =
                    CitizenUiSettingsRepository.roadVideoAutoDetectEnabled(context)
            },
            onOpenCompanionCamera = {
                showSettingsDialog = false
                showCompanionCamera = true
            },
        )
    }

    if (showCompanionCamera) {
        TripCompanionCameraDialog(onDismiss = { showCompanionCamera = false })
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About City Grid", style = CitizenType.ScreenTitle) },
            text = {
                Text(
                    "City Grid helps citizens report potholes and other road hazards with photos and location. " +
                        "When you are signed in, you can track your submissions in My Reports.\n\n" +
                        "${CitizenPrivacyCopy.DETAIL}\n\n" +
                        "${CitizenAccountabilityCopy.ABOUT_INDEPENDENT} " +
                        "Reporting and municipal zone officers are available for Bengaluru (GBA / BBMP) in this release.\n\n" +
                        "BBMP directory: https://bbmp.gov.in/\n\n" +
                        "Version ${BuildConfig.VERSION_NAME}",
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", style = CitizenType.Label)
                }
            },
        )
    }
    } // Box root — hosts in-composition ReportSignInModal overlay above map
}

@Composable
private fun NotificationsHeaderBell(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.size(CitizenIcon.CapsuleCompact)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(CitizenIcon.CapsuleCompact),
            )
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .background(CitizenSelection.Active, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    style = CitizenType.Caption,
                    color = CitizenSelection.ActiveContent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
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
    onOpenSettings: () -> Unit,
    onOpenUnconfirmed: () -> Unit,
    onShowAbout: () -> Unit,
) {
    Box {
        if (isSignedIn) {
            Box(
                modifier = Modifier
                    .size(CitizenIcon.CapsuleCompact)
                    .clip(CircleShape)
                    .clickable { onMenuExpandedChange(true) },
                contentAlignment = Alignment.Center,
            ) {
                CartoonAvatar(avatarId = avatarId, size = CitizenIcon.CapsuleCompact)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(CitizenIcon.CapsuleCompact)
                    .border(CitizenBorder.Thin, MaterialTheme.colorScheme.primary, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onRequestSignIn),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Sign in",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(CitizenIcon.Chip),
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
                    text = {
                        val n = TripUnconfirmedDetectionStore.count()
                        Text(if (n > 0) "Unconfirmed ($n)" else "Unconfirmed")
                    },
                    onClick = {
                        onMenuExpandedChange(false)
                        onOpenUnconfirmed()
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = null)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onOpenSettings()
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Settings, contentDescription = null)
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
    val uiScale = rememberMapUiScale()
    val compactChrome = LocalConfiguration.current.screenWidthDp < 370
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(
                horizontal = uiScale.dp(if (compactChrome) 2f else 4f),
                vertical = uiScale.dp(if (compactChrome) 6f else 8f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CitizenIcon.Tap),
        ) {
            Text(
                text = title,
                style = if (selected) CitizenType.Tab else CitizenType.Tab.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    CitizenInk.Helper
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(if (selected) CitizenSelection.Active else Color.Transparent)
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
        null -> CitizenInk.Helper
    }

    Text(
        text = when {
            selectedCity.isBlank() -> "Weather loads after city is set"
            loading -> "Loading today's weather..."
            forecast != null -> forecast!!.summaryLine
            else -> "Weather forecast unavailable"
        },
        style = CitizenType.Helper,
        color = if (forecast != null) rainColor else CitizenInk.Helper,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ReportAndTrackSection(
    isSignedIn: Boolean,
    onReportPotholeGuest: () -> Unit,
    onReportPotholeSignedIn: () -> Unit,
    onRequestReportFromFullscreenMap: () -> Unit,
    suppressFullscreenBack: Boolean = false,
    mapCoveredByOverlay: Boolean = false,
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
    onFeedbackMessage: (String) -> Unit = {},
    mapFullscreen: Boolean,
    onMapFullscreenChange: (Boolean) -> Unit,
    mapCursorStyle: MapCursorStyle = MapCursorStyle.CAR,
    mapCursorColor: MapCursorColor = MapCursorColor.BLUE,
    tripCaptureDisplay: TripCaptureDisplayMode = TripCaptureDisplayMode.MAP_ONLY,
    tripCaptureLayout: TripCaptureCameraMapLayout = TripCaptureCameraMapLayout.SWITCH,
    tripCaptureRoad: TripCaptureSource = TripCaptureSource.PHONE_BACK,
    tripCaptureCabin: TripCaptureCabinSource = TripCaptureCabinSource.OFF,
    tripCompanionPairingCode: String = "",
    imuBumpEnabled: Boolean = true,
    swerveDetectionEnabled: Boolean = true,
    roadVideoAutoDetectEnabled: Boolean = true,
    reporterUserId: String = "",
    onNotificationsMutated: () -> Unit = {},
) {
    val context = LocalContext.current
    val tripScope = rememberCoroutineScope()
    val uiScale = rememberMapUiScale()
    val compactChrome = LocalConfiguration.current.screenWidthDp < 370
    var tripNavigatePreparing by remember { mutableStateOf(false) }
    var mapDestPickEpoch by remember { mutableIntStateOf(0) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    if (!mapFullscreen) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CitizenSpace.Lg)
            .clickable {
                if (isSignedIn) onReportPotholeSignedIn()
                else onReportPotholeGuest()
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB74233)),
        shape = CitizenRadius.Capsule,
        border = BorderStroke(1.dp, Color(0xFF8B2F24)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CitizenIcon.Tap)
                .padding(
                    horizontal = uiScale.dp(if (compactChrome) 18f else 24f),
                    vertical = uiScale.dp(if (compactChrome) 10f else 12f),
                ),
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
                    modifier = Modifier.size(CitizenIcon.Chip),
                )
                Spacer(modifier = Modifier.width(uiScale.dp(8f)))
                Text(
                    "REPORT A POTHOLE",
                    color = Color.White,
                    style = CitizenType.Cta,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(uiScale.dp(4f)))
            Text(
                "Signed in but report anonymously",
                color = Color.White.copy(alpha = 0.95f),
                style = CitizenType.CtaSub,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    Spacer(modifier = Modifier.height(uiScale.dp(if (compactChrome) 18f else 24f)))
    }

    var expanded by remember { mutableStateOf(false) }
    /** null = all severities on the map. */
    var mapSeverityFilterKey by rememberSaveable { mutableStateOf<String?>(null) }
    val mapSeverityFilter = remember(mapSeverityFilterKey) {
        mapSeverityFilterKey?.let { key -> PotholeSeverity.entries.find { it.name == key } }
    }
    var severityMenuExpanded by remember { mutableStateOf(false) }
    var areaHeatEnabled by rememberSaveable { mutableStateOf(false) }
    var weatherMapEnabled by rememberSaveable { mutableStateOf(false) }
    var weatherHorizonName by rememberSaveable { mutableStateOf(WeatherHorizon.NEXT_3H.name) }
    val weatherHorizon = remember(weatherHorizonName) {
        WeatherHorizon.entries.find { it.name == weatherHorizonName } ?: WeatherHorizon.NEXT_3H
    }
    var weatherRainGrid by remember { mutableStateOf<CityWeatherRainGrid?>(null) }
    var tripModeEnabled by rememberSaveable { mutableStateOf(false) }
    var tripDestLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var tripDestLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var tripDestLabel by rememberSaveable { mutableStateOf("") }
    /** Bumped when destination is chosen so the map can fly to that area. */
    var tripDestFocusEpoch by remember { mutableIntStateOf(0) }
    /** Bumped when Direction is pressed — fit the full route in view. */
    var tripRouteOverviewEpoch by remember { mutableIntStateOf(0) }
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
    /** False until user taps Navigate — this starts turn-by-turn. */
    var tripNavActive by rememberSaveable { mutableStateOf(false) }
    var pendingBumpEvent by remember { mutableStateOf<TripImuBumpEvent?>(null) }
    val pendingBumpHolder = remember { arrayOfNulls<TripImuBumpEvent>(1) }
    pendingBumpHolder[0] = pendingBumpEvent
    var bumpAnalyzing by remember { mutableStateOf(false) }
    var bumpVisionSummary by remember { mutableStateOf<String?>(null) }
    var bumpVisionTier by remember { mutableStateOf<TripBumpVisionTier?>(null) }
    var bumpReportCreated by remember { mutableStateOf(false) }
    var bumpUnconfirmedSaved by remember { mutableStateOf(false) }
    var bumpLocationOnly by remember { mutableStateOf(false) }
    var bumpUnconfirmedId by remember { mutableStateOf<Long?>(null) }
    var pendingAvoidPrompt by remember { mutableStateOf(false) }
    var avoidPromptLat by remember { mutableStateOf<Double?>(null) }
    var avoidPromptLon by remember { mutableStateOf<Double?>(null) }
    val bumpGpsHolder = remember { arrayOfNulls<Location>(1) }
    bumpGpsHolder[0] = gpsPinLocation
    val bumpVehicleHolder = remember { arrayOfNulls<TripVehicle>(1) }
    bumpVehicleHolder[0] = tripVehicle
    val lastBumpEpochHolder = remember { longArrayOf(0L) }
    val navActiveHolder = remember { booleanArrayOf(false) }
    navActiveHolder[0] = tripNavActive
    val avoidWasOffRouteHolder = remember { booleanArrayOf(false) }
    val avoidOffRouteStartedHolder = remember { longArrayOf(0L) }
    val avoidPeakOffRouteHolder = remember { doubleArrayOf(0.0) }
    val lastAvoidPromptHolder = remember { longArrayOf(0L) }
    // Session toggle for #32 Camera / Camera+map (Switch). Reset when Navigate starts.
    var tripCaptureShowCamera by rememberSaveable { mutableStateOf(false) }
    /**
     * Nav-screen road-video master switch (bottom-right).
     * Off = map-only navigate (no camera / “Checking road video”).
     * Bump sensing still runs from IMU + GPS → Unconfirmed.
     * On = capture per Settings (Camera / Camera+map / sources).
     */
    var tripRoadVideoEnabled by rememberSaveable { mutableStateOf(false) }
    val tripRoadVideoHolder = remember { booleanArrayOf(false) }
    tripRoadVideoHolder[0] = tripRoadVideoEnabled
    val tripCaptureSplitActive =
        tripCaptureDisplay == TripCaptureDisplayMode.CAMERA_AND_MAP &&
            tripCaptureLayout == TripCaptureCameraMapLayout.SPLIT
    val tripCaptureSwitchEnabled =
        tripCaptureDisplay == TripCaptureDisplayMode.CAMERA ||
            (tripCaptureDisplay == TripCaptureDisplayMode.CAMERA_AND_MAP &&
                tripCaptureLayout == TripCaptureCameraMapLayout.SWITCH)
    fun applyRoadVideoFromSettings(enabled: Boolean) {
        tripRoadVideoEnabled = enabled
        if (!enabled) {
            tripCaptureShowCamera = false
            TripRoadKeyframeBuffer.clear()
            return
        }
        // Resume using current Settings capture mode.
        tripCaptureShowCamera = when {
            tripCaptureSplitActive -> false
            tripCaptureDisplay == TripCaptureDisplayMode.CAMERA -> true
            tripCaptureDisplay == TripCaptureDisplayMode.CAMERA_AND_MAP &&
                tripCaptureLayout == TripCaptureCameraMapLayout.SWITCH -> true
            else -> false
        }
    }
    LaunchedEffect(tripNavActive) {
        if (!tripNavActive) {
            tripRoadVideoEnabled = false
            tripCaptureShowCamera = false
        }
    }
    LaunchedEffect(tripCaptureDisplay, tripCaptureLayout, tripRoadVideoEnabled) {
        // If settings change while video is on, re-apply display layout.
        if (tripNavActive && tripRoadVideoEnabled) {
            applyRoadVideoFromSettings(true)
        }
    }
    /** After destination is set, routes/ETA/alts stay hidden until Direction is pressed. */
    var tripDirectionsRevealed by rememberSaveable { mutableStateOf(false) }
    var tripPendingRouteOverview by remember { mutableStateOf(false) }
    /** Intermediate stops between start and destination (Direction mode). */
    var tripStops by remember { mutableStateOf<List<TripStopPlace>>(emptyList()) }
    /** Trip map camera: Street / Map / Play (cycle on the interchangeable control). */
    var tripCameraModeName by rememberSaveable { mutableStateOf(TripCameraMode.STREET.name) }
    val tripCameraMode = TripCameraMode.fromStorage(tripCameraModeName)
    /** True for Street + Play (heading-up follow); false for Map overview. */
    val tripStreetFollow = tripCameraMode.isHeadingUp
    /** Device compass heading (0–360°); turns the vehicle cursor when the phone turns. */
    var tripDeviceHeadingDeg by remember { mutableStateOf<Float?>(null) }

    /**
     * Collapse fullscreen map.
     * Keep Trip “on” (red chip) only after Navigate has started; otherwise end
     * planning so the home Trip chip is not red without a live trip.
     */
    fun exitMapFullscreen() {
        if (!tripNavActive) {
            tripModeEnabled = false
        }
        onMapFullscreenChange(false)
    }
    BackHandler(enabled = mapFullscreen && !imeVisible && !suppressFullscreenBack) {
        exitMapFullscreen()
    }

    DisposableEffect(tripModeEnabled, tripNavActive, swerveDetectionEnabled) {
        // Need compass for heading-up while navigating (and trip map orientation).
        // Heading crooks alone no longer enqueue Avoid — lane-change overtakes looked
        // identical. Avoid prompts come from leave-route → rejoin + bump signal.
        if (!tripModeEnabled && !tripNavActive) {
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
    // #33 Phase C — IMU bump sensing while Navigate is on (camera optional; GPS required).
    DisposableEffect(tripNavActive, imuBumpEnabled, tripVehicle) {
        val deviceRole = CitizenUiSettingsRepository.tripDeviceRole(context)
        val allow =
            tripNavActive &&
                imuBumpEnabled &&
                deviceRole != TripDeviceRole.CAMERA_ONLY &&
                TripImuBumpAnalyzer.peakThresholdMps2(tripVehicle) != null
        if (!allow) {
            onDispose { }
        } else {
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val detector = TripImuBumpDetector(
                context = context,
                vehicleProvider = { bumpVehicleHolder[0] ?: TripVehicle.CAR },
                speedMpsProvider = {
                    val gps = bumpGpsHolder[0]
                    gps?.speed?.takeIf { gps.hasSpeed() && it.isFinite() }
                },
                onBumpPeak = { peak, now ->
                    mainExecutor.execute {
                        if (pendingBumpHolder[0] != null) return@execute
                        lastBumpEpochHolder[0] = now
                        if (tripRoadVideoHolder[0]) {
                            TripRoadKeyframeBuffer.beginBurst(now)
                        }
                        val gps = bumpGpsHolder[0]
                        val vehicle = bumpVehicleHolder[0] ?: TripVehicle.CAR
                        val event = TripImuBumpEvent(
                            id = TripImuBumpLog.allocateId(),
                            detectedAtEpochMs = now,
                            peakMps2 = peak,
                            lat = gps?.latitude,
                            lon = gps?.longitude,
                            speedMps = gps?.speed?.takeIf { gps.hasSpeed() && it.isFinite() },
                            accuracyM = gps?.accuracy?.takeIf { gps.hasAccuracy() && it.isFinite() },
                            vehicle = vehicle,
                        )
                        TripImuBumpLog.add(event)
                        pendingBumpEvent = event
                    }
                },
            )
            detector.start()
            onDispose { detector.stop() }
        }
    }
    LaunchedEffect(tripNavActive) {
        if (!tripNavActive) {
            pendingBumpEvent = null
            bumpAnalyzing = false
            bumpVisionSummary = null
            bumpVisionTier = null
            bumpReportCreated = false
            bumpUnconfirmedSaved = false
            bumpLocationOnly = false
            bumpUnconfirmedId = null
            pendingAvoidPrompt = false
            TripRoadKeyframeBuffer.clear()
        }
    }
    // #33 Phase D — analyze ~15 s road keyframe window around the bump (or GPS-only).
    LaunchedEffect(pendingBumpEvent?.id) {
        val event = pendingBumpEvent ?: return@LaunchedEffect
        bumpAnalyzing = true
        bumpVisionSummary = null
        bumpVisionTier = null
        bumpReportCreated = false
        bumpUnconfirmedSaved = false
        bumpLocationOnly = !tripRoadVideoEnabled
        bumpUnconfirmedId = null
        val outcome = TripBumpVisionPipeline.analyzeBump(
            context = context,
            event = event,
            cityKey = selectedCity,
            reporterUserId = reporterUserId.takeIf { it.isNotBlank() },
            signedIn = isSignedIn,
        )
        if (pendingBumpEvent?.id != event.id) return@LaunchedEffect
        bumpAnalyzing = false
        bumpVisionTier = outcome.vision.tier
        bumpReportCreated = outcome.reportCreated
        bumpUnconfirmedSaved = outcome.unconfirmedId != null
        bumpUnconfirmedId = outcome.unconfirmedId
        bumpLocationOnly = outcome.vision.tier == TripBumpVisionTier.NO_FRAMES ||
            outcome.vision.tier == TripBumpVisionTier.NO_MODEL ||
            outcome.vision.frameCount <= 0
        bumpVisionSummary = buildString {
            append(outcome.vision.summary)
            outcome.blockedReason?.let { append(' ').append(it) }
        }
        if (outcome.reportCreated) {
            onReportsMutated()
            CitizenNotificationsRepository.notifyVisionCapture(
                title = "Pothole auto-captured",
                body = "Bump + road video drafted a report. ${outcome.vision.summary}",
            )
            onNotificationsMutated()
            onFeedbackMessage("Auto-drafted pothole report from road video")
        } else if (outcome.unconfirmedId != null) {
            CitizenNotificationsRepository.notifyVisionCapture(
                title = "Bump saved (Unconfirmed)",
                body = outcome.vision.summary.ifBlank {
                    "A bump was saved for your review."
                },
            )
            onNotificationsMutated()
            onFeedbackMessage("Bump saved to Unconfirmed")
        }
        delay(18_000L)
        if (pendingBumpEvent?.id == event.id) {
            TripImuBumpLog.updateResponse(event.id, TripImuBumpUserResponse.DISMISSED)
            pendingBumpEvent = null
        }
    }
    // #33 continuous vision — while Navigate + road video ON, YOLO-score newest frames
    // and auto-capture potholes (High → report, Medium → Unconfirmed). No IMU required.
    val visionHitCooldown = remember {
        object {
            var lastHitEpochMs = 0L
            var lastHitLat = Double.NaN
            var lastHitLon = Double.NaN
        }
    }
    LaunchedEffect(
        tripNavActive,
        tripRoadVideoEnabled,
        roadVideoAutoDetectEnabled,
    ) {
        if (!tripNavActive || !tripRoadVideoEnabled || !roadVideoAutoDetectEnabled) return@LaunchedEffect
        while (true) {
            delay(TripBumpVisionGate.VISION_SCAN_INTERVAL_MS)
            if (!tripNavActive || !tripRoadVideoEnabled || !roadVideoAutoDetectEnabled) break
            if (pendingBumpEvent != null || bumpAnalyzing) continue
            if (TripRoadKeyframeBuffer.size() < TripBumpVisionGate.VISION_MIN_FRAMES) continue
            val gps = bumpGpsHolder[0] ?: continue
            val lat = gps.latitude.takeIf { it.isFinite() } ?: continue
            val lon = gps.longitude.takeIf { it.isFinite() } ?: continue
            val now = System.currentTimeMillis()
            if (now - visionHitCooldown.lastHitEpochMs < TripBumpVisionGate.VISION_HIT_COOLDOWN_MS) {
                val dLat = visionHitCooldown.lastHitLat
                val dLon = visionHitCooldown.lastHitLon
                if (dLat.isFinite() && dLon.isFinite()) {
                    val movedM = TripNavigationMatcher.haversineM(dLat, dLon, lat, lon)
                    if (movedM < TripBumpVisionGate.VISION_HIT_COOLDOWN_M) continue
                } else {
                    continue
                }
            }
            val outcome = TripBumpVisionPipeline.analyzeRoadVideo(
                context = context,
                aroundEpochMs = now,
                lat = lat,
                lon = lon,
                cityKey = selectedCity,
                reporterUserId = reporterUserId.takeIf { it.isNotBlank() },
                signedIn = isSignedIn,
            )
            when {
                outcome.reportCreated -> {
                    visionHitCooldown.lastHitEpochMs = now
                    visionHitCooldown.lastHitLat = lat
                    visionHitCooldown.lastHitLon = lon
                    onReportsMutated()
                    val score = outcome.vision.maxScore
                    val scorePart = score?.let {
                        " YOLO ${"%.2f".format(java.util.Locale.US, it)}"
                    }.orEmpty()
                    CitizenNotificationsRepository.notifyVisionCapture(
                        title = "Pothole auto-captured",
                        body = "Road video drafted a report near your route.$scorePart",
                    )
                    onNotificationsMutated()
                    onFeedbackMessage("Pothole auto-captured from road video")
                }
                outcome.unconfirmedId != null -> {
                    visionHitCooldown.lastHitEpochMs = now
                    visionHitCooldown.lastHitLat = lat
                    visionHitCooldown.lastHitLon = lon
                    val score = outcome.vision.maxScore
                    val scorePart = score?.let {
                        " Score ${"%.2f".format(java.util.Locale.US, it)}."
                    }.orEmpty()
                    CitizenNotificationsRepository.notifyVisionCapture(
                        title = "Possible pothole (Unconfirmed)",
                        body = "Road video saved a candidate for your review.$scorePart",
                    )
                    onNotificationsMutated()
                    onFeedbackMessage("Possible pothole saved to Unconfirmed")
                }
            }
        }
    }
    // Companion road feed: keep LAN navigator alive whenever Navigate + road video
    // is ON (even if the map is showing and the camera preview is hidden).
    LaunchedEffect(
        tripNavActive,
        tripModeEnabled,
        mapFullscreen,
        tripRoadVideoEnabled,
        tripCaptureRoad,
        tripCompanionPairingCode,
    ) {
        val wantCompanion =
            tripNavActive &&
                tripModeEnabled &&
                mapFullscreen &&
                tripRoadVideoEnabled &&
                tripCaptureRoad == TripCaptureSource.COMPANION
        if (!wantCompanion) {
            TripCompanionLan.stopNavigator()
            return@LaunchedEffect
        }
        TripCompanionLan.startNavigator(context, tripCompanionPairingCode)
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            TripCompanionLan.stopNavigator()
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
            // Keep fine GPS if the user left Home mid-trip (e.g. report a pothole).
            if (!TripSessionRepository.isNavigating()) {
                onTripNavigationActiveChanged(false)
            }
        }
    }
    // Keep the display awake during turn-by-turn; power button still turns the screen off.
    val tripKeepAwakeView = LocalView.current
    DisposableEffect(tripNavActive) {
        tripKeepAwakeView.keepScreenOn = tripNavActive
        onDispose { tripKeepAwakeView.keepScreenOn = false }
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
    val weatherMapAvailable = CityWeatherRepository.supportsWeatherMap(selectedCity)
    LaunchedEffect(heatAvailable) {
        if (!heatAvailable) areaHeatEnabled = false
    }
    LaunchedEffect(weatherMapAvailable) {
        if (!weatherMapAvailable) {
            weatherMapEnabled = false
            weatherRainGrid = null
        }
    }
    LaunchedEffect(weatherMapEnabled, selectedCity, weatherHorizon) {
        if (!weatherMapEnabled || !CityWeatherRepository.supportsWeatherMap(selectedCity)) {
            weatherRainGrid = null
            return@LaunchedEffect
        }
        // Keep the rain grid fresh while Weather stays on: fetch now, then on each
        // clock-hour boundary (and at least every 15 minutes as a fallback).
        var lastHourStamp = -1
        while (true) {
            val nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))
            val hourStamp = nowIst.hour + nowIst.dayOfYear * 24
            val force = hourStamp != lastHourStamp && lastHourStamp >= 0
            weatherRainGrid = withContext(Dispatchers.IO) {
                CityWeatherRepository.fetchRainGrid(
                    cityKey = selectedCity,
                    horizon = weatherHorizon,
                    forceRefresh = force,
                )
            }
            lastHourStamp = hourStamp
            val minutes = nowIst.minute
            val seconds = nowIst.second
            // Sleep until the next hour (+5s slack), capped at 15 minutes.
            val msToNextHour =
                ((60 - minutes) * 60 - seconds).coerceAtLeast(30) * 1000L
            delay(minOf(msToNextHour, 15 * 60 * 1000L))
        }
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
            tripDirectionsRevealed = false
            tripPendingRouteOverview = false
            tripStops = emptyList()
            TripSessionRepository.clear(context)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CitizenSpace.Lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    selectedCity,
                    style = CitizenType.Section,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    " - ",
                    style = CitizenType.Section,
                )
                Text(
                    "POTHOLE DENSITY",
                    style = CitizenType.SectionTrailing,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            CityWeatherSubline(selectedCity = selectedCity)
        }

        Box {
            AssistChip(
                onClick = {
                    citySearchQuery = ""
                    searchedPlaces = emptyList()
                    expanded = true
                },
                modifier = Modifier.height(CitizenIcon.CapsuleCompact),
                label = {
                    Text(
                        "CHANGE CITY",
                        style = CitizenType.ChipCompact,
                    )
                },
                shape = CitizenRadius.Capsule,
                border = BorderStroke(CitizenBorder.Thin, CitizenSelection.InactiveBorder),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onBackground,
                ),
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
                Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                if (citySearchQuery.isBlank()) {
                    Text(
                        "More cities coming soon",
                        modifier = Modifier.padding(horizontal = CitizenSpace.Lg, vertical = CitizenSpace.Xs),
                        style = CitizenType.Caption,
                        color = CitizenInk.Muted,
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
                                        style = CitizenType.Caption,
                                        color = CitizenInk.Muted,
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

    Spacer(modifier = Modifier.height(CitizenSpace.Sm))
    }

    val mapSeverityFilterLabel = mapSeverityFilter?.title ?: "ALL"
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
            .padding(horizontal = CitizenSpace.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                "Severity",
                style = CitizenType.Label,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.width(CitizenSpace.Sm))
            Box {
                AssistChip(
                    onClick = { severityMenuExpanded = true },
                    modifier = Modifier.height(CitizenIcon.CapsuleCompact),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                mapSeverityFilterLabel,
                                style = CitizenType.Chip,
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(CitizenIcon.Chip),
                                tint = if (mapSeverityFilter != null) {
                                    CitizenSelection.Active
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                },
                            )
                        }
                    },
                    shape = CitizenRadius.Capsule,
                    border = BorderStroke(
                        CitizenBorder.Thin,
                        if (mapSeverityFilter != null) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                    ),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (mapSeverityFilter != null) {
                            CitizenSelection.ActiveSoft
                        } else {
                            Color.Transparent
                        },
                        labelColor = if (mapSeverityFilter != null) {
                            CitizenSelection.Active
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
                        text = { Text("ALL") },
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
            // Red only while an active Navigate session exists (dest set + Navigate pressed).
            selected = tripNavActive,
            onClick = {
                when {
                    // Resume minimized turn-by-turn (chip is red).
                    tripNavActive -> onMapFullscreenChange(true)
                    // Open planner; chip stays unselected on home until Navigate.
                    else -> {
                        tripModeEnabled = true
                        onMapFullscreenChange(true)
                    }
                }
            },
            label = {
                Text("Trip", style = CitizenType.Chip)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(CitizenIcon.Chip),
                )
            },
            shape = CitizenRadius.Capsule,
            border = BorderStroke(
                CitizenBorder.Thin,
                if (tripNavActive) CitizenSelection.Active else CitizenSelection.InactiveBorder,
            ),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = MaterialTheme.colorScheme.onBackground,
                iconColor = MaterialTheme.colorScheme.onBackground,
                selectedContainerColor = CitizenSelection.Active,
                selectedLabelColor = CitizenSelection.ActiveContent,
                selectedLeadingIconColor = CitizenSelection.ActiveContent,
            ),
            modifier = Modifier
                .height(CitizenIcon.CapsuleCompact)
                .padding(end = CitizenSpace.Sm),
        )
        if (heatAvailable) {
            FilterChip(
                selected = areaHeatEnabled,
                modifier = Modifier
                    .height(CitizenIcon.CapsuleCompact)
                    .padding(end = CitizenSpace.Sm),
                onClick = {
                    areaHeatEnabled = !areaHeatEnabled
                    if (areaHeatEnabled) weatherMapEnabled = false
                },
                label = {
                    Text(
                        "Heat",
                        style = CitizenType.Chip,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = null,
                        modifier = Modifier.size(CitizenIcon.Chip),
                    )
                },
                shape = CitizenRadius.Capsule,
                border = BorderStroke(
                    CitizenBorder.Thin,
                    if (areaHeatEnabled) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    iconColor = MaterialTheme.colorScheme.onBackground,
                    selectedContainerColor = CitizenSelection.Active,
                    selectedLabelColor = CitizenSelection.ActiveContent,
                    selectedLeadingIconColor = CitizenSelection.ActiveContent,
                ),
            )
        }
        if (weatherMapAvailable) {
            FilterChip(
                selected = weatherMapEnabled,
                modifier = Modifier
                    .height(CitizenIcon.CapsuleCompact)
                    .padding(end = CitizenSpace.Sm),
                onClick = {
                    weatherMapEnabled = !weatherMapEnabled
                    if (weatherMapEnabled) areaHeatEnabled = false
                },
                label = {
                    Text(
                        "Weather",
                        style = CitizenType.Chip,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(CitizenIcon.Chip),
                    )
                },
                shape = CitizenRadius.Capsule,
                border = BorderStroke(
                    CitizenBorder.Thin,
                    if (weatherMapEnabled) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    iconColor = MaterialTheme.colorScheme.onBackground,
                    selectedContainerColor = CitizenSelection.Active,
                    selectedLabelColor = CitizenSelection.ActiveContent,
                    selectedLeadingIconColor = CitizenSelection.ActiveContent,
                ),
            )
            if (weatherMapEnabled) {
                WeatherHorizon.entries.forEach { horizon ->
                    FilterChip(
                        selected = weatherHorizon == horizon,
                        modifier = Modifier
                            .height(CitizenIcon.CapsuleCompact)
                            .padding(end = CitizenSpace.Sm),
                        onClick = { weatherHorizonName = horizon.name },
                        label = {
                            Text(
                                horizon.chipLabel,
                                style = CitizenType.Chip,
                            )
                        },
                        shape = CitizenRadius.Capsule,
                        border = BorderStroke(
                            CitizenBorder.Thin,
                            if (weatherHorizon == horizon) {
                                CitizenSelection.Active
                            } else {
                                CitizenSelection.InactiveBorder
                            },
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onBackground,
                            selectedContainerColor = CitizenSelection.Active,
                            selectedLabelColor = CitizenSelection.ActiveContent,
                        ),
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(CitizenSpace.Sm))
    }

    var tripAlerts by remember { mutableStateOf<List<TripPotholeAlert>>(emptyList()) }
    var tripRoutePoints by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var tripRouteSteps by remember { mutableStateOf<List<OsrmRouteStep>>(emptyList()) }
    var tripRouteOptions by remember { mutableStateOf<List<OsrmRouteResult>>(emptyList()) }
    /** #23.2 chip hints aligned with [tripRouteOptions] (e.g. safer-road label). */
    var tripRouteSaferHints by remember { mutableStateOf<List<String?>>(emptyList()) }
    /**
     * Background OSRM result filled as soon as origin + destination are known,
     * before Direction / Navigation — so those taps can paint instantly.
     */
    var tripRoutePrefetch by remember { mutableStateOf<TripRoutePrefetchCache?>(null) }
    /** Preview corridors retained after Navigate for deviation “tap to use”. */
    var tripKnownAlternates by remember { mutableStateOf<List<OsrmRouteResult>>(emptyList()) }
    var tripSelectedRouteIndex by remember { mutableIntStateOf(0) }
    var tripRouteUserPicked by remember { mutableStateOf(false) }
    var tripNextTurn by remember { mutableStateOf<TripTurnGuidance?>(null) }
    var tripRouteProgress by remember { mutableStateOf<TripRouteProgress?>(null) }
    var tripRouteDurationSec by remember { mutableStateOf<Double?>(null) }
    var tripRouteStatus by remember { mutableStateOf(TripRouteStatus.IDLE) }
    var tripArrived by remember { mutableStateOf(false) }
    /** Nearby shops / localities shown after arrival (non-overlapping labels + card). */
    var tripArrivalPlaces by remember { mutableStateOf<List<NearbyOsmPoi>>(emptyList()) }
    var showTripArrivalCard by remember { mutableStateOf(false) }
    var tripArrivalLookupDone by remember { mutableStateOf(false) }

    /**
     * Offline-capable cursor: coasts from last GPS + gyro/compass heading along the
     * known route when satellite updates pause (no internet / weak A-GPS).
     */
    var tripCursorLocation by remember { mutableStateOf<Location?>(gpsPinLocation) }
    LaunchedEffect(
        tripNavActive,
        gpsPinLocation?.latitude,
        gpsPinLocation?.longitude,
        gpsPinLocation?.speed,
        gpsPinLocation?.bearing,
        gpsPinLocation?.elapsedRealtimeNanos,
        tripDeviceHeadingDeg,
        tripRoutePoints,
    ) {
        if (!tripNavActive) {
            tripCursorLocation = gpsPinLocation
            return@LaunchedEffect
        }
        while (isActive) {
            tripCursorLocation = TripDeadReckoning.displayLocation(
                lastFix = gpsPinLocation,
                deviceHeadingDeg = tripDeviceHeadingDeg,
                routePoints = tripRoutePoints,
            )
            delay(200)
        }
    }
    /** Map / follow / progress use this; raw [gpsPinLocation] stays for online reroute. */
    val tripMapGps = tripCursorLocation ?: gpsPinLocation
    var tripRerouting by remember { mutableStateOf(false) }
    var tripRouteRequestEpoch by remember { mutableIntStateOf(0) }
    var mapConnectivityEpoch by remember { mutableIntStateOf(0) }
    var tripNavOnline by remember {
        mutableStateOf(NetworkReachability.hasInternet(context))
    }
    var rerouteOriginLat by remember { mutableStateOf<Double?>(null) }
    var rerouteOriginLon by remember { mutableStateOf<Double?>(null) }
    var lastRerouteAtMs by remember { mutableStateOf(0L) }
    var tripStartCorrectionDone by remember { mutableStateOf(false) }
    /** Dashed return-to-route polyline while hybrid prefers rejoin. */
    var tripRejoinPoints by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    /** Index into [tripKnownAlternates] briefly offered (legacy tap UI); usually null — alts auto-adopt. */
    var tripOfferedAlternateIndex by remember { mutableStateOf<Int?>(null) }
    val announcedPotholeIds = remember { mutableSetOf<Long>() }
    val potholeSpeakCounts = remember { mutableMapOf<Long, Int>() }
    val potholeLastSpeakAtMs = remember { mutableMapOf<Long, Long>() }
    val bbox = remember(selectedCity) { metroBboxForCity(selectedCity) }

    // Holders so the connectivity callback always sees the latest trip/GPS state.
    val tripNavOnlineHolder = remember {
        object {
            var navActive = false
            var arrived = false
            var modeEnabled = false
            var directionsRevealed = false
            var hasDest = false
            var gps: Location? = null
        }
    }
    tripNavOnlineHolder.navActive = tripNavActive
    tripNavOnlineHolder.arrived = tripArrived
    tripNavOnlineHolder.modeEnabled = tripModeEnabled
    tripNavOnlineHolder.directionsRevealed = tripDirectionsRevealed
    tripNavOnlineHolder.hasDest = tripDestLat != null && tripDestLon != null
    tripNavOnlineHolder.gps = gpsPinLocation

    // Connectivity: offline chip during nav; on reconnect refresh tiles + sync routes.
    DisposableEffect(Unit) {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val onlineSnap = object {
            var online = NetworkReachability.hasInternet(context)
        }
        onlineSnap.online = tripNavOnline
        val unregister = NetworkReachability.registerConnectivityListener(context) { online ->
            mainHandler.post {
                val wasOffline = !onlineSnap.online
                onlineSnap.online = online
                tripNavOnline = online
                if (!online || !wasOffline) return@post
                mapConnectivityEpoch++
                val holder = tripNavOnlineHolder
                if (holder.navActive && !holder.arrived) {
                    val gps = holder.gps
                    if (gps != null) {
                        lastRerouteAtMs = SystemClock.elapsedRealtime()
                        rerouteOriginLat = gps.latitude
                        rerouteOriginLon = gps.longitude
                        tripRerouting = true
                        tripRouteRequestEpoch++
                    }
                } else if (holder.modeEnabled && holder.hasDest) {
                    // Planning / Direction: drop warm packs so OSRM re-fetches from live start.
                    tripRoutePrefetch = null
                    tripRouteRequestEpoch++
                }
            }
        }
        onDispose { unregister() }
    }

    fun selectTripRouteOption(index: Int) {
        val option = tripRouteOptions.getOrNull(index) ?: return
        tripSelectedRouteIndex = index
        tripRoutePoints = option.points
        tripRouteSteps = option.steps
        tripRouteDurationSec = option.durationSeconds
        tripRouteStatus = TripRouteStatus.ACTIVE
        tripRouteUserPicked = true
    }

    fun clearTripDeviationUi() {
        tripRejoinPoints = emptyList()
        tripOfferedAlternateIndex = null
    }

    fun adoptKnownAlternate(index: Int) {
        val option = tripKnownAlternates.getOrNull(index) ?: return
        // Already navigating this corridor — don't re-apply or toast-spam.
        if (TripNavigationMatcher.routesLookAlike(option.points, tripRoutePoints)) {
            if (tripRejoinPoints.isNotEmpty() || tripOfferedAlternateIndex != null) {
                clearTripDeviationUi()
            }
            return
        }
        tripRoutePoints = option.points
        tripRouteSteps = option.steps
        tripRouteDurationSec = option.durationSeconds
        tripRouteStatus = TripRouteStatus.ACTIVE
        tripSelectedRouteIndex = 0
        tripOfferedAlternateIndex = null
        tripRejoinPoints = emptyList()
        tripRerouting = false
        rerouteOriginLat = null
        rerouteOriginLon = null
        lastRerouteAtMs = SystemClock.elapsedRealtime()
        onFeedbackMessage("Continuing on alternate route")
    }

    fun confirmOfferedKnownAlternate() {
        val idx = tripOfferedAlternateIndex ?: return
        adoptKnownAlternate(idx)
    }

    /** Paint planner / map from a background prefetch (no network). */
    fun applyTripPrefetchToPlanner(
        cache: TripRoutePrefetchCache,
        preferPrevious: OsrmRouteResult? = null,
    ): Boolean {
        if (cache.routes.isEmpty()) return false
        val selectedIndex = if (preferPrevious != null) {
            cache.routes.indexOfFirst { osrmRoutesLookAlike(it, preferPrevious) }
                .takeIf { it >= 0 } ?: 0
        } else {
            0
        }
        tripRouteOptions = cache.routes
        tripRouteSaferHints = cache.hints
        tripSelectedRouteIndex = selectedIndex
        val selected = cache.routes[selectedIndex]
        tripRoutePoints = selected.points
        tripRouteSteps = selected.steps
        tripRouteDurationSec = selected.durationSeconds
        tripRouteStatus = TripRouteStatus.ACTIVE
        tripArrived = false
        return true
    }

    fun currentPlannerOriginLatLon(): Pair<Double, Double>? {
        val lat = tripStartLat ?: gpsPinLocation?.latitude
        val lon = tripStartLon ?: gpsPinLocation?.longitude
        if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite()) return null
        return lat to lon
    }

    fun tryApplyMatchingPrefetch(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        viaPoints: List<Pair<Double, Double>>,
        preferPrevious: OsrmRouteResult? = null,
    ): Boolean {
        val cache = tripRoutePrefetch ?: return false
        if (!tripRoutePrefetchMatches(
                cache,
                originLat,
                originLon,
                destLat,
                destLon,
                tripVehicle,
                viaPoints,
            )
        ) {
            return false
        }
        return applyTripPrefetchToPlanner(cache, preferPrevious)
    }

    /** Persist a successful OSRM pack for offline Direction after start drift. */
    fun rememberTripRouteOffline(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        viaPoints: List<Pair<Double, Double>>,
        routes: List<OsrmRouteResult>,
        hints: List<String?>,
    ) {
        TripRouteOfflineCache.remember(
            context = context,
            originLat = originLat,
            originLon = originLon,
            destLat = destLat,
            destLon = destLon,
            vehicle = tripVehicle,
            viaPoints = viaPoints,
            routes = routes,
            hints = hints,
        )
    }

    /**
     * Offline Direction: destination already known + cached route pack.
     * [looseOrigin] allows GPS / start updates while offline.
     */
    fun tryApplyOfflineRouteCache(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        viaPoints: List<Pair<Double, Double>>,
        preferPrevious: OsrmRouteResult? = null,
        looseOrigin: Boolean = true,
    ): Boolean {
        val entry = TripRouteOfflineCache.findBest(
            context = context,
            originLat = originLat,
            originLon = originLon,
            destLat = destLat,
            destLon = destLon,
            vehicle = tripVehicle,
            viaPoints = viaPoints,
            looseOrigin = looseOrigin,
        ) ?: return false
        val cache = TripRoutePrefetchCache(
            originLat = entry.originLat,
            originLon = entry.originLon,
            destLat = entry.destLat,
            destLon = entry.destLon,
            vehicle = entry.vehicle,
            viaPoints = entry.viaPoints,
            routes = entry.routes,
            hints = entry.hints,
        )
        tripRoutePrefetch = cache
        return applyTripPrefetchToPlanner(cache, preferPrevious)
    }

    fun tryApplyAnyCachedRoute(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        viaPoints: List<Pair<Double, Double>>,
        preferPrevious: OsrmRouteResult? = null,
        offlineLooseOrigin: Boolean = false,
    ): Boolean {
        if (
            tryApplyMatchingPrefetch(
                originLat, originLon, destLat, destLon, viaPoints, preferPrevious,
            )
        ) {
            return true
        }
        return tryApplyOfflineRouteCache(
            originLat = originLat,
            originLon = originLon,
            destLat = destLat,
            destLon = destLon,
            viaPoints = viaPoints,
            preferPrevious = preferPrevious,
            looseOrigin = offlineLooseOrigin,
        )
    }

    // Restore trip after leaving Home mid-navigation (report / tab / process recreate).
    var tripSessionRestored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        ReportSyncRepository.flushPendingUploads()
        if (tripSessionRestored) return@LaunchedEffect
        tripSessionRestored = true
        val snap = TripSessionRepository.restore(context) ?: return@LaunchedEffect
        if (!snap.tripModeEnabled) return@LaunchedEffect
        tripModeEnabled = true
        tripCameraModeName = TripCameraMode.fromStorage(snap.tripCameraMode, snap.tripStreetFollow).name
        tripVehicleName = snap.vehicleName
        tripDestLat = snap.destLat
        tripDestLon = snap.destLon
        tripDestLabel = snap.destLabel
        tripStartLat = snap.startLat
        tripStartLon = snap.startLon
        tripStartLabel = snap.startLabel
        tripNavOriginLat = snap.navOriginLat
        tripNavOriginLon = snap.navOriginLon
        if (snap.routePoints.size >= 2) {
            tripRoutePoints = snap.routePoints
            tripRouteDurationSec = snap.routeDurationSec
            tripRouteStatus = TripRouteStatus.ACTIVE
            tripDirectionsRevealed = true
        }
        tripNavActive = snap.tripNavActive
        if (snap.tripNavActive) tripDirectionsRevealed = true
        onTripNavigationActiveChanged(snap.tripModeEnabled || snap.tripNavActive)
        // Trip UI lives on the fullscreen map — reopen it when restoring a session.
        onMapFullscreenChange(true)
    }

    // SOS Roadside Assist → navigate here / add stop (after Emergency closes).
    val sosAssistLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(sosAssistLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            val req = SosAssistNavRequest.consume() ?: return@LifecycleEventObserver
            when (req.mode) {
                SosAssistNavRequest.Mode.REPLACE_AS_DESTINATION -> {
                    tripModeEnabled = true
                    tripStops = emptyList()
                    tripDestLat = req.latitude
                    tripDestLon = req.longitude
                    tripDestLabel = req.label
                    tripArrived = false
                    showTripArrivalCard = false
                    tripArrivalPlaces = emptyList()
                    tripDirectionsRevealed = true
                    tripRouteUserPicked = false
                    tripPendingRouteOverview = false
                    tripRoutePrefetch = null
                    tripStartCorrectionDone = false
                    onMapFullscreenChange(true)
                    tripScope.launch {
                        val gps = onEnsureFreshGpsForTrip() ?: gpsPinLocation
                        tripNavOriginLat = gps?.latitude
                        tripNavOriginLon = gps?.longitude
                        tripNavActive = true
                        tripRouteRequestEpoch++
                        onFeedbackMessage("Navigating to ${req.label}")
                    }
                }
                SosAssistNavRequest.Mode.ADD_AS_STOP -> {
                    if (tripDestLat == null || tripDestLon == null) {
                        // No destination yet — treat as new destination.
                        tripModeEnabled = true
                        tripDestLat = req.latitude
                        tripDestLon = req.longitude
                        tripDestLabel = req.label
                        tripDirectionsRevealed = true
                        tripRouteUserPicked = false
                        tripPendingRouteOverview = true
                        tripRoutePrefetch = null
                        onMapFullscreenChange(true)
                        tripRouteRequestEpoch++
                        onFeedbackMessage("Added ${req.label} as destination")
                    } else {
                        tripModeEnabled = true
                        val stop = TripStopPlace(
                            label = req.label,
                            latitude = req.latitude,
                            longitude = req.longitude,
                        )
                        tripStops = if (tripStops.size >= MAX_TRIP_STOPS) {
                            tripStops.drop(1) + stop
                        } else {
                            tripStops + stop
                        }
                        tripRouteUserPicked = false
                        tripDirectionsRevealed = true
                        tripRoutePrefetch = null
                        onMapFullscreenChange(true)
                        tripRouteRequestEpoch++
                        onFeedbackMessage("Added ${req.label} as a stop")
                    }
                }
            }
        }
        sosAssistLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { sosAssistLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(
        tripModeEnabled,
        tripNavActive,
        tripStreetFollow,
        tripCameraModeName,
        tripVehicleName,
        tripDestLat,
        tripDestLon,
        tripDestLabel,
        tripStartLat,
        tripStartLon,
        tripStartLabel,
        tripNavOriginLat,
        tripNavOriginLon,
        tripRoutePoints,
        tripRouteDurationSec,
    ) {
        if (!tripModeEnabled) return@LaunchedEffect
        TripSessionRepository.save(
            context,
            TripSessionSnapshot(
                tripModeEnabled = tripModeEnabled,
                tripNavActive = tripNavActive,
                tripStreetFollow = tripStreetFollow,
                tripCameraMode = tripCameraMode.name,
                vehicleName = tripVehicleName,
                destLat = tripDestLat,
                destLon = tripDestLon,
                destLabel = tripDestLabel,
                startLat = tripStartLat,
                startLon = tripStartLon,
                startLabel = tripStartLabel,
                navOriginLat = tripNavOriginLat,
                navOriginLon = tripNavOriginLon,
                routePoints = tripRoutePoints,
                routeDurationSec = tripRouteDurationSec,
            ),
        )
    }

    /**
     * As soon as start + destination are known (search or map tap), warm OSRM
     * in the background so Direction / Navigation can show instantly.
     */
    LaunchedEffect(
        tripModeEnabled,
        tripNavActive,
        tripDirectionsRevealed,
        tripStartLat,
        tripStartLon,
        tripDestLat,
        tripDestLon,
        tripVehicle,
        tripStops,
        if (tripStartUsesMyLocation) {
            gpsPinLocation?.latitude?.let { (it * 200).toInt() }
        } else {
            null
        },
        if (tripStartUsesMyLocation) {
            gpsPinLocation?.longitude?.let { (it * 200).toInt() }
        } else {
            null
        },
    ) {
        if (!tripModeEnabled || tripNavActive || tripDirectionsRevealed) return@LaunchedEffect
        val destLat = tripDestLat
        val destLon = tripDestLon
        if (destLat == null || destLon == null) {
            tripRoutePrefetch = null
            return@LaunchedEffect
        }
        val origin = currentPlannerOriginLatLon()
        if (origin == null) {
            tripRoutePrefetch = null
            return@LaunchedEffect
        }
        val (originLat, originLon) = origin
        val viaPoints = tripStops.map { it.latitude to it.longitude }
        val existing = tripRoutePrefetch
        if (
            existing != null &&
            tripRoutePrefetchMatches(
                existing,
                originLat,
                originLon,
                destLat,
                destLon,
                tripVehicle,
                viaPoints,
            )
        ) {
            return@LaunchedEffect
        }
        delay(180)
        if (!NetworkReachability.hasInternet(context)) {
            // Offline: warm in-memory prefetch from durable cache so Direction still works
            // when the destination is known and the start / GPS has moved.
            val offline = TripRouteOfflineCache.findBest(
                context = context,
                originLat = originLat,
                originLon = originLon,
                destLat = destLat,
                destLon = destLon,
                vehicle = tripVehicle,
                viaPoints = viaPoints,
                looseOrigin = true,
            )
            if (offline != null) {
                tripRoutePrefetch = TripRoutePrefetchCache(
                    originLat = offline.originLat,
                    originLon = offline.originLon,
                    destLat = offline.destLat,
                    destLon = offline.destLon,
                    vehicle = offline.vehicle,
                    viaPoints = offline.viaPoints,
                    routes = offline.routes,
                    hints = offline.hints,
                )
            }
            return@LaunchedEffect
        }
        val gps = gpsPinLocation
        val snapRadius = OsrmRouteClient.snapRadiusForAccuracy(
            gps?.takeIf { it.hasAccuracy() }?.accuracy,
        )
        val routes = runCatching {
            OsrmRouteClient.fetchRoutes(
                originLat,
                originLon,
                destLat,
                destLon,
                tripVehicle,
                snapRadiusM = snapRadius,
                viaPoints = viaPoints,
            )
        }.getOrNull().orEmpty()
        if (routes.isEmpty()) return@LaunchedEffect
        val ranked = rankTripRoutesByPotholeRisk(selectedCity, routes)
        tripRoutePrefetch = TripRoutePrefetchCache(
            originLat = originLat,
            originLon = originLon,
            destLat = destLat,
            destLon = destLon,
            vehicle = tripVehicle,
            viaPoints = viaPoints,
            routes = ranked.routes,
            hints = ranked.hints,
        )
        rememberTripRouteOffline(
            originLat = originLat,
            originLon = originLon,
            destLat = destLat,
            destLon = destLon,
            viaPoints = viaPoints,
            routes = ranked.routes,
            hints = ranked.hints,
        )
    }

    /** Fetch route for preview (start+dest) and again while navigating / rerouting. */
    LaunchedEffect(
        tripModeEnabled,
        tripNavActive,
        tripStartLat,
        tripStartLon,
        tripDestLat,
        tripDestLon,
        tripVehicle,
        tripStops,
        // Coarse GPS bucket (~500 m) so preview does not refetch/reset the user's
        // chosen alternate on every small location jitter.
        if (tripStartUsesMyLocation) {
            gpsPinLocation?.latitude?.let { (it * 200).toInt() }
        } else {
            null
        },
        if (tripStartUsesMyLocation) {
            gpsPinLocation?.longitude?.let { (it * 200).toInt() }
        } else {
            null
        },
        tripRouteRequestEpoch,
        tripNavOriginLat,
        tripNavOriginLon,
        tripDirectionsRevealed,
    ) {
        if (!tripModeEnabled) {
            tripRoutePoints = emptyList()
            tripRouteSteps = emptyList()
            tripRouteOptions = emptyList()
            tripRouteSaferHints = emptyList()
            tripRoutePrefetch = null
            tripKnownAlternates = emptyList()
            tripSelectedRouteIndex = 0
            tripRouteUserPicked = false
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
            clearTripDeviationUi()
            announcedPotholeIds.clear()
            potholeSpeakCounts.clear()
            potholeLastSpeakAtMs.clear()
            return@LaunchedEffect
        }
        val destLat = tripDestLat
        val destLon = tripDestLon
        if (destLat == null || destLon == null) {
            tripRoutePoints = emptyList()
            tripRouteSteps = emptyList()
            tripRouteOptions = emptyList()
            tripRouteSaferHints = emptyList()
            tripRoutePrefetch = null
            tripKnownAlternates = emptyList()
            tripSelectedRouteIndex = 0
            tripNextTurn = null
            tripRouteProgress = null
            tripRouteDurationSec = null
            tripRouteStatus = TripRouteStatus.IDLE
            tripAlerts = emptyList()
            tripRerouting = false
            clearTripDeviationUi()
            return@LaunchedEffect
        }
        // Planning: keep route hidden until Direction, but leave [tripRoutePrefetch] warm.
        if (!tripNavActive && !tripDirectionsRevealed) {
            tripRoutePoints = emptyList()
            tripRouteSteps = emptyList()
            tripRouteOptions = emptyList()
            tripRouteSaferHints = emptyList()
            tripKnownAlternates = emptyList()
            tripSelectedRouteIndex = 0
            tripRouteUserPicked = false
            tripNextTurn = null
            tripRouteProgress = null
            tripRouteDurationSec = null
            tripRouteStatus = TripRouteStatus.IDLE
            tripAlerts = emptyList()
            tripRerouting = false
            clearTripDeviationUi()
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
            // Only true for mid-trip off-route / start-correction refetches — not the
            // initial Navigate press (that must keep the user's chosen alternate).
            val isRerouteRequest = rerouteOriginLat != null
            val routeSnapRadius = OsrmRouteClient.snapRadiusForAccuracy(
                gps?.takeIf { it.hasAccuracy() }?.accuracy,
            )
            val stopVias = tripStops.map { it.latitude to it.longitude }
            // Offline: keep the current polyline and live GPS cursor — never hang on OSRM.
            if (isRerouteRequest && !NetworkReachability.hasInternet(context)) {
                tripRerouting = false
                tripRouteStatus = if (tripRoutePoints.size >= 2) {
                    TripRouteStatus.ACTIVE
                } else {
                    tripRouteStatus
                }
                rerouteOriginLat = null
                rerouteOriginLon = null
                return@LaunchedEffect
            }
            tripRerouting = tripNavActive && isRerouteRequest
            tripRouteStatus = when {
                tripRerouting -> TripRouteStatus.REROUTING
                tripNavActive && tripRoutePoints.size >= 2 && tripRouteSteps.isNotEmpty() ->
                    TripRouteStatus.ACTIVE
                else -> TripRouteStatus.FINDING
            }
            if (tripNavActive) {
                val keepChosenPreview =
                    !isRerouteRequest &&
                        tripRoutePoints.size >= 2 &&
                        tripRouteSteps.isNotEmpty()
                if (keepChosenPreview) {
                    // Navigate with the route the user already selected (incl. alternate).
                    // Keep preview corridors for mid-trip deviation “tap to use”.
                    if (tripRouteOptions.isNotEmpty()) {
                        tripKnownAlternates = tripRouteOptions
                    }
                    tripRouteOptions = emptyList()
                    tripRouteSaferHints = emptyList()
                    tripRouteStatus = TripRouteStatus.ACTIVE
                    tripRerouting = false
                    clearTripDeviationUi()
                } else {
                    // Prefer warm prefetch / offline pack for the first Navigate tap (no Direction first).
                    if (
                        !isRerouteRequest &&
                        tryApplyAnyCachedRoute(
                            originLat,
                            originLon,
                            destLat,
                            destLon,
                            stopVias,
                            offlineLooseOrigin = !NetworkReachability.hasInternet(context),
                        )
                    ) {
                        tripKnownAlternates = tripRouteOptions
                        tripRouteOptions = emptyList()
                        tripRouteSaferHints = emptyList()
                        tripRouteStatus = TripRouteStatus.ACTIVE
                        tripRerouting = false
                        clearTripDeviationUi()
                        rerouteOriginLat = null
                        rerouteOriginLon = null
                    } else if (!NetworkReachability.hasInternet(context)) {
                        // Offline with no usable pack — keep geometry if any; else NO_ROUTE.
                        clearTripDeviationUi()
                        if (preserveCurrentRouteOnFailure) {
                            tripRouteStatus = TripRouteStatus.ACTIVE
                        } else {
                            tripRouteStatus = TripRouteStatus.NO_ROUTE
                        }
                        tripRerouting = false
                        rerouteOriginLat = null
                        rerouteOriginLon = null
                    } else {
                    // Mid-trip refetch: prefer a route that matches the corridor in use.
                    clearTripDeviationUi()
                    val preferredPoints = tripRoutePoints
                    val routes = runCatching {
                        OsrmRouteClient.fetchRoutes(
                            originLat, originLon, destLat, destLon, tripVehicle,
                            snapRadiusM = routeSnapRadius,
                            viaPoints = stopVias,
                        )
                    }.getOrNull().orEmpty()
                    val route = pickOsrmRouteMatchingPoints(routes, preferredPoints)
                    if (route != null && route.points.size >= 2) {
                        tripRouteOptions = emptyList()
                        tripRouteSaferHints = emptyList()
                        // Refresh known alts from the new fetch so future detours can offer them.
                        if (routes.size > 1) {
                            tripKnownAlternates = rankTripRoutesByPotholeRisk(selectedCity, routes).routes
                        }
                        tripSelectedRouteIndex = 0
                        tripRoutePoints = route.points
                        tripRouteSteps = route.steps
                        tripRouteDurationSec = route.durationSeconds
                        tripRouteStatus = TripRouteStatus.ACTIVE
                        rememberTripRouteOffline(
                            originLat = originLat,
                            originLon = originLon,
                            destLat = destLat,
                            destLon = destLon,
                            viaPoints = stopVias,
                            routes = if (routes.isNotEmpty()) {
                                rankTripRoutesByPotholeRisk(selectedCity, routes).routes
                            } else {
                                listOf(route)
                            },
                            hints = emptyList(),
                        )
                        rerouteOriginLat = null
                        rerouteOriginLon = null
                    } else if (!preserveCurrentRouteOnFailure) {
                        tripRoutePoints = emptyList()
                        tripRouteSteps = emptyList()
                        tripRouteOptions = emptyList()
                        tripRouteSaferHints = emptyList()
                        tripSelectedRouteIndex = 0
                        tripNextTurn = null
                        tripRouteDurationSec = null
                        tripRouteStatus = TripRouteStatus.NO_ROUTE
                    } else {
                        // Keep previous geometry + steps so turn guidance does not blank out.
                        tripRouteStatus = TripRouteStatus.ACTIVE
                        rerouteOriginLat = null
                        rerouteOriginLon = null
                    }
                    }
                }
            } else {
                val previous = tripRouteOptions.getOrNull(tripSelectedRouteIndex)
                if (
                    tryApplyAnyCachedRoute(
                        originLat,
                        originLon,
                        destLat,
                        destLon,
                        stopVias,
                        preferPrevious = if (tripRouteUserPicked) previous else null,
                        offlineLooseOrigin = !NetworkReachability.hasInternet(context),
                    )
                ) {
                    // Instant Direction paint from warm / offline cache — no OSRM round-trip.
                    tripArrived = false
                    rerouteOriginLat = null
                    rerouteOriginLon = null
                } else if (!NetworkReachability.hasInternet(context)) {
                    if (preserveCurrentRouteOnFailure) {
                        tripRouteStatus = TripRouteStatus.ACTIVE
                    } else {
                        tripRoutePoints = emptyList()
                        tripRouteSteps = emptyList()
                        tripRouteOptions = emptyList()
                        tripRouteSaferHints = emptyList()
                        tripSelectedRouteIndex = 0
                        tripNextTurn = null
                        tripRouteDurationSec = null
                        tripRouteStatus = TripRouteStatus.NO_ROUTE
                    }
                } else {
                val routes = runCatching {
                    OsrmRouteClient.fetchRoutes(
                        originLat, originLon, destLat, destLon, tripVehicle,
                        snapRadiusM = routeSnapRadius,
                        viaPoints = stopVias,
                    )
                }.getOrNull().orEmpty()
                if (routes.isNotEmpty()) {
                    val ranked = rankTripRoutesByPotholeRisk(selectedCity, routes)
                    tripRouteOptions = ranked.routes
                    tripRouteSaferHints = ranked.hints
                    tripRoutePrefetch = TripRoutePrefetchCache(
                        originLat = originLat,
                        originLon = originLon,
                        destLat = destLat,
                        destLon = destLon,
                        vehicle = tripVehicle,
                        viaPoints = stopVias,
                        routes = ranked.routes,
                        hints = ranked.hints,
                    )
                    rememberTripRouteOffline(
                        originLat = originLat,
                        originLon = originLon,
                        destLat = destLat,
                        destLon = destLon,
                        viaPoints = stopVias,
                        routes = ranked.routes,
                        hints = ranked.hints,
                    )
                    val selectedIndex = when {
                        tripRouteUserPicked && previous != null -> {
                            ranked.routes.indexOfFirst { osrmRoutesLookAlike(it, previous) }
                                .takeIf { it >= 0 }
                                ?: tripSelectedRouteIndex.coerceIn(0, ranked.routes.lastIndex)
                        }
                        else -> 0
                    }
                    if (!tripRouteUserPicked) {
                        tripSelectedRouteIndex = 0
                    } else {
                        tripSelectedRouteIndex = selectedIndex
                    }
                    val selected = ranked.routes[tripSelectedRouteIndex]
                    tripRoutePoints = selected.points
                    tripRouteSteps = selected.steps
                    tripRouteDurationSec = selected.durationSeconds
                    tripRouteStatus = TripRouteStatus.ACTIVE
                    tripArrived = false
                    rerouteOriginLat = null
                    rerouteOriginLon = null
                } else if (!preserveCurrentRouteOnFailure) {
                    tripRoutePoints = emptyList()
                    tripRouteSteps = emptyList()
                    tripRouteOptions = emptyList()
                    tripRouteSaferHints = emptyList()
                    tripSelectedRouteIndex = 0
                    tripNextTurn = null
                    tripRouteDurationSec = null
                    tripRouteStatus = TripRouteStatus.NO_ROUTE
                } else {
                    tripRouteStatus = TripRouteStatus.ACTIVE
                }
                }
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
        tripMapGps?.latitude,
        tripMapGps?.longitude,
        tripMapGps?.bearing,
        tripMapGps?.speed,
        tripMapGps?.accuracy,
        recentReportsEpoch,
        mapSeverityFilter,
    ) {
        if (!tripModeEnabled || !tripNavActive || tripRoutePoints.size < 2) {
            tripRouteProgress = null
            tripNextTurn = null
            tripAlerts = emptyList()
            clearTripDeviationUi()
            return@LaunchedEffect
        }
        val gps = tripMapGps
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
                val base = RecentReportsRepository.reportsForMapInMetro(selectedCity, metroBbox)
                    .filter { report ->
                        mapSeverityFilter == null || report.severity == mapSeverityFilter
                    }
                val crowd = TripCrowdHazardStore.syntheticReportsForMap(
                    context = context,
                    cityKey = selectedCity,
                    viewerUserId = reporterUserId.ifBlank { "local" },
                ).filter { report ->
                    mapSeverityFilter == null || report.severity == mapSeverityFilter
                }
                base + crowd
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
        val knownAltPoints = tripKnownAlternates.map { it.points }
        val destLat = tripDestLat ?: tripRoutePoints.lastOrNull()?.first
        val destLon = tripDestLon ?: tripRoutePoints.lastOrNull()?.second
        // On a known alternate from preview → auto-continue that path (no tap, no rejoin dash).
        val preferredAlt = TripNavigationMatcher.findPreferredKnownAlternateIndex(
            knownAlternates = knownAltPoints,
            activeRoute = tripRoutePoints,
            driverLat = gps.latitude,
            driverLon = gps.longitude,
            activeOffRouteMeters = progress.offRouteMeters,
            destLat = destLat,
            destLon = destLon,
        ) ?: TripNavigationMatcher.findPreferredKnownAlternateIndex(
            knownAlternates = knownAltPoints,
            activeRoute = tripRoutePoints,
            driverLat = gps.latitude,
            driverLon = gps.longitude,
            activeOffRouteMeters = progress.offRouteMeters,
            minImprovementM = 0.0,
            destLat = destLat,
            destLon = destLon,
        )
        if (preferredAlt != null) {
            if (tripRejoinPoints.isNotEmpty() || tripOfferedAlternateIndex != null) {
                clearTripDeviationUi()
            }
            adoptKnownAlternate(preferredAlt)
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
                    if (!NetworkReachability.hasInternet(context)) {
                        return@LaunchedEffect
                    }
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
        val offM = progress.offRouteMeters
        val wallNow = System.currentTimeMillis()
        if (offM > TripAvoidRejoinAnalyzer.MIN_OFF_ROUTE_M) {
            if (!avoidWasOffRouteHolder[0]) {
                avoidWasOffRouteHolder[0] = true
                avoidOffRouteStartedHolder[0] = wallNow
                avoidPeakOffRouteHolder[0] = offM
            } else if (offM > avoidPeakOffRouteHolder[0]) {
                avoidPeakOffRouteHolder[0] = offM
            }
        } else if (avoidWasOffRouteHolder[0]) {
            val peak = avoidPeakOffRouteHolder[0]
            val shouldAsk = swerveDetectionEnabled &&
                TripAvoidRejoinAnalyzer.shouldPrompt(
                    wasDodging = true,
                    nowOffRouteMeters = offM,
                    peakOffRouteMeters = peak,
                    recentBumpEpochMs = lastBumpEpochHolder[0].takeIf { it > 0L },
                    dodgeStartedAtMs = avoidOffRouteStartedHolder[0].takeIf { it > 0L },
                    nowEpochMs = wallNow,
                    lastPromptEpochMs = lastAvoidPromptHolder[0],
                )
            avoidWasOffRouteHolder[0] = false
            avoidOffRouteStartedHolder[0] = 0L
            avoidPeakOffRouteHolder[0] = 0.0
            if (shouldAsk && pendingBumpEvent == null && !pendingAvoidPrompt) {
                lastAvoidPromptHolder[0] = wallNow
                avoidPromptLat = gps.latitude
                avoidPromptLon = gps.longitude
                pendingAvoidPrompt = true
            }
        }
        if (progress.offRouteMeters <= accuracyAwareThreshold) {
            // Back on the active corridor — drop rejoin / offer chrome.
            if (tripRejoinPoints.isNotEmpty() || tripOfferedAlternateIndex != null) {
                clearTripDeviationUi()
            }
            return@LaunchedEffect
        }
        // Parallel / intentional detours: reroute quickly. Tiny GPS wobble: wait a bit longer.
        val rerouteCooldownMs = when {
            progress.offRouteMeters >= 45.0 -> 2_500L
            else -> 6_000L
        }
        if (now - lastRerouteAtMs < rerouteCooldownMs) {
            return@LaunchedEffect
        }
        // No internet → local adopt / rejoin-to-dest / wait for reconnect to U-turn-reroute.
        val speed = gps.speed.takeIf { gps.hasSpeed() }
        if (!NetworkReachability.hasInternet(context)) {
            val offlinePlan = TripNavigationMatcher.planDeviation(
                routePoints = tripRoutePoints,
                knownAlternates = knownAltPoints,
                driverLat = gps.latitude,
                driverLon = gps.longitude,
                progress = progress,
                speedMps = speed,
                fullRerouteDurationSec = null,
                destLat = destLat,
                destLon = destLon,
                vehicle = tripVehicle,
                vehicleNetworkSnapM = null,
            )
            when (offlinePlan.action) {
                TripDeviationAction.ADOPT_KNOWN_ALTERNATE -> {
                    offlinePlan.knownAlternateIndex?.let { idx ->
                        adoptKnownAlternate(idx)
                    }
                }
                TripDeviationAction.SHOW_REJOIN -> {
                    tripRejoinPoints = offlinePlan.rejoinPoints
                    tripOfferedAlternateIndex = null
                    lastRerouteAtMs = now
                }
                TripDeviationAction.SUGGEST_UTURN,
                TripDeviationAction.FULL_REROUTE,
                -> {
                    // Offline: cannot rematch vehicle network — keep dashed rejoin if short.
                    val rejoin = TripNavigationMatcher.buildRejoinPolyline(
                        tripRoutePoints,
                        gps.latitude,
                        gps.longitude,
                    )
                    if (
                        rejoin != null &&
                        rejoin.second <= TripNavigationMatcher.REJOIN_PREF_MAX_RETURN_M
                    ) {
                        tripRejoinPoints = rejoin.first
                        tripOfferedAlternateIndex = null
                        lastRerouteAtMs = now
                    }
                }
                TripDeviationAction.NONE -> Unit
            }
            return@LaunchedEffect
        }

        // Online: probe OSRM snap on this vehicle's network (car vs bike/foot paths).
        val networkProbe = if (destLat != null && destLon != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    OsrmRouteClient.fetchRoute(
                        gps.latitude,
                        gps.longitude,
                        destLat,
                        destLon,
                        tripVehicle,
                        snapRadiusM = OsrmRouteClient.snapRadiusForAccuracy(
                            if (gps.hasAccuracy()) gps.accuracy else null,
                        ),
                    )
                }.getOrNull()
            }
        } else {
            null
        }
        val plan = TripNavigationMatcher.planDeviation(
            routePoints = tripRoutePoints,
            knownAlternates = knownAltPoints,
            driverLat = gps.latitude,
            driverLon = gps.longitude,
            progress = progress,
            speedMps = speed,
            fullRerouteDurationSec = networkProbe?.durationSeconds,
            destLat = destLat,
            destLon = destLon,
            vehicle = tripVehicle,
            vehicleNetworkSnapM = networkProbe?.startSnapMeters,
        )
        when (plan.action) {
            TripDeviationAction.ADOPT_KNOWN_ALTERNATE -> {
                plan.knownAlternateIndex?.let { idx ->
                    adoptKnownAlternate(idx)
                }
                return@LaunchedEffect
            }
            TripDeviationAction.SHOW_REJOIN -> {
                tripRejoinPoints = plan.rejoinPoints
                tripOfferedAlternateIndex = null
                lastRerouteAtMs = now
                return@LaunchedEffect
            }
            TripDeviationAction.SUGGEST_UTURN -> {
                clearTripDeviationUi()
                onFeedbackMessage(
                    "U-turn — this path isn't for ${tripVehicle.label.lowercase()}",
                )
                // Fall through to full vehicle-network reroute.
            }
            TripDeviationAction.FULL_REROUTE,
            TripDeviationAction.NONE,
            -> {
                clearTripDeviationUi()
            }
        }

        lastRerouteAtMs = now
        clearTripDeviationUi()
        rerouteOriginLat = gps.latitude
        rerouteOriginLon = gps.longitude
        tripRerouting = true
        tripRouteRequestEpoch++
    }
    val nextTripAlert = tripAlerts.firstOrNull()
    LaunchedEffect(tripPendingRouteOverview, tripDirectionsRevealed, tripRoutePoints, tripRouteStatus) {
        if (!tripPendingRouteOverview || !tripDirectionsRevealed) return@LaunchedEffect
        if (tripRoutePoints.size >= 2 && tripRouteStatus == TripRouteStatus.ACTIVE) {
            tripRouteOverviewEpoch++
            tripPendingRouteOverview = false
        }
    }
    LaunchedEffect(tripNavActive) {
        if (tripNavActive) onFeedbackMessage("Navigation started")
    }
    // Arrival: end navigation, zoom destination neighborhood, load nearby places card.
    LaunchedEffect(tripArrived) {
        if (!tripArrived) return@LaunchedEffect
        if (!tripNavActive) return@LaunchedEffect
        onFeedbackMessage("You have arrived")
        val destLat = tripDestLat ?: tripRoutePoints.lastOrNull()?.first
        val destLon = tripDestLon ?: tripRoutePoints.lastOrNull()?.second
        delay(800)
        tripNavActive = false
        tripNavOriginLat = null
        tripNavOriginLon = null
        tripRouteStatus = TripRouteStatus.ARRIVED
        showTripArrivalCard = true
        tripArrivalPlaces = emptyList()
        tripArrivalLookupDone = false
        // Destination neighborhood (not full city overview).
        tripDestFocusEpoch++
        if (destLat != null && destLon != null && destLat.isFinite() && destLon.isFinite()) {
            val raw = withContext(Dispatchers.IO) {
                OverpassNearbyClient.fetchArrivalNearby(destLat, destLon)
            }
            tripArrivalPlaces = pickNonOverlappingPois(raw)
        }
        tripArrivalLookupDone = true
    }

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
            potholeSpeakCounts.clear()
            potholeLastSpeakAtMs.clear()
            announcedPotholeIds.clear()
            tripVoice.stop()
        }
    }

    // #29 Turn-aware + speed-scaled pothole / turn voice (shared scheduler).
    LaunchedEffect(
        tripNavActive,
        tripArrived,
        tripVoiceMuted,
        nextTripAlert?.report?.id,
        nextTripAlert?.distanceMeters,
        nextTripAlert?.alongRouteMeters,
        tripNextTurn?.instruction,
        tripNextTurn?.distanceMeters,
        tripRouteProgress?.traveledMeters,
        gpsPinLocation?.speed,
        gpsPinLocation?.hasSpeed(),
    ) {
        if (!tripNavActive || tripArrived) return@LaunchedEffect
        while (true) {
            val gps = gpsPinLocation
            val speed = gps?.speed?.takeIf { gps.hasSpeed() && it.isFinite() }
            val result = TripAlertScheduler.evaluate(
                traveledMeters = tripRouteProgress?.traveledMeters ?: 0.0,
                speedMps = speed,
                nextTurn = tripNextTurn,
                alerts = tripAlerts,
                nowElapsedMs = android.os.SystemClock.elapsedRealtime(),
                potholeSpeakCounts = potholeSpeakCounts.toMap(),
                potholeLastSpeakAtMs = potholeLastSpeakAtMs.toMap(),
                spokenTurnKeys = spokenTurnKeys.toSet(),
            )
            for (cue in result.cues) {
                when (cue) {
                    is TripAlertScheduler.Cue.Combined -> {
                        val id = cue.alert.report.id
                        announcedPotholeIds.add(id)
                        potholeSpeakCounts[id] = (potholeSpeakCounts[id] ?: 0) + 1
                        potholeLastSpeakAtMs[id] = android.os.SystemClock.elapsedRealtime()
                        spokenTurnKeys.add(cue.turnKey)
                        spokenTurnKeys.add(TripAlertScheduler.turnKey(cue.guidance, "far"))
                        if (cue.haptic) {
                            notifyTripPotholeAlert(context, cue.alert.report.severity)
                        }
                        if (!tripVoiceMuted) tripVoice.speak(cue.speak)
                    }
                    is TripAlertScheduler.Cue.Pothole -> {
                        val id = cue.alert.report.id
                        announcedPotholeIds.add(id)
                        potholeSpeakCounts[id] = (potholeSpeakCounts[id] ?: 0) + 1
                        potholeLastSpeakAtMs[id] = android.os.SystemClock.elapsedRealtime()
                        if (cue.haptic) {
                            notifyTripPotholeAlert(context, cue.alert.report.severity)
                        }
                        if (!tripVoiceMuted) tripVoice.speak(cue.speak)
                    }
                    is TripAlertScheduler.Cue.Turn -> {
                        if (!spokenTurnKeys.add(cue.key)) continue
                        if (!tripVoiceMuted) tripVoice.speak(cue.speak)
                    }
                }
            }
            delay(750)
        }
    }

    @Composable
    fun MapSection(modifier: Modifier, showExpand: Boolean) {
        @Composable
        fun DensityMap(mapModifier: Modifier) {
            if (CitizenMapTiler.isConfigured) {
                MapLibreDensityMap(
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
                    gpsPinLocation = tripMapGps,
                    gpsAccuracyText = gpsAccuracyText,
                    mapSeverityFilter = mapSeverityFilter,
                    areaHeatEnabled = areaHeatEnabled && !(tripModeEnabled && mapFullscreen),
                    weatherRainEnabled = weatherMapEnabled && !(tripModeEnabled && mapFullscreen),
                    weatherRainGrid = weatherRainGrid.takeIf {
                        weatherMapEnabled && !(tripModeEnabled && mapFullscreen)
                    },
                    tripModeEnabled = tripModeEnabled && mapFullscreen,
                    tripNavActive = tripNavActive && mapFullscreen,
                    tripStreetFollow = tripStreetFollow,
                    tripCameraMode = tripCameraMode,
                    tripDeviceHeadingDeg = tripDeviceHeadingDeg,
                    tripVehicle = tripVehicle,
                    mapCursorStyle = mapCursorStyle,
                    mapCursorColor = mapCursorColor,
                    tripRoutePoints = tripRoutePoints,
                    tripRouteTraveledMeters = if (tripNavActive) {
                        tripRouteProgress?.traveledMeters ?: 0.0
                    } else {
                        0.0
                    },
                    tripRouteOptions = tripRouteOptions.map { it.points },
                    tripRouteEtas = tripRouteOptions.mapIndexed { index, option ->
                        val base =
                            "${formatTripEta(option.durationSeconds)}\n${formatTripDistance(option.distanceMeters)}"
                        val hint = tripRouteSaferHints.getOrNull(index)
                        if (hint.isNullOrBlank()) base else "$base\n$hint"
                    },
                    tripSelectedRouteIndex = tripSelectedRouteIndex,
                    onSelectTripRouteIndex = { selectTripRouteOption(it) },
                    tripRejoinPoints = tripRejoinPoints,
                    tripOfferedAlternatePoints = tripOfferedAlternateIndex
                        ?.let { tripKnownAlternates.getOrNull(it)?.points }
                        .orEmpty(),
                    onConfirmOfferedAlternate = { confirmOfferedKnownAlternate() },
                    tripAlerts = tripAlerts,
                    arrivalPlaces = tripArrivalPlaces,
                    tripStartLat = tripRoutePoints.firstOrNull()?.first
                        ?: tripStartLat
                        ?: gpsPinLocation?.latitude,
                    tripStartLon = tripRoutePoints.firstOrNull()?.second
                        ?: tripStartLon
                        ?: gpsPinLocation?.longitude,
                    tripDestinationLat = tripRoutePoints.lastOrNull()?.first ?: tripDestLat,
                    tripDestinationLon = tripRoutePoints.lastOrNull()?.second ?: tripDestLon,
                    tripStopPoints = tripStops.map { it.latitude to it.longitude },
                    recentReportsEpoch = recentReportsEpoch,
                    mapConnectivityEpoch = mapConnectivityEpoch,
                    mapFullscreen = mapFullscreen,
                    onExitFullscreen = { exitMapFullscreen() },
                    onEnterFullscreen = { onMapFullscreenChange(true) },
                    onReportPothole = onRequestReportFromFullscreenMap,
                    tripRoadVideoEnabled = tripRoadVideoEnabled,
                    onToggleTripRoadVideo = {
                        applyRoadVideoFromSettings(!tripRoadVideoEnabled)
                    },
                    onMapPickDestination = { lat, lon ->
                        if (tripModeEnabled && !tripNavActive) {
                            tripDestLat = lat
                            tripDestLon = lon
                            tripDestLabel = "Dropped pin"
                            tripNavActive = false
                            tripRouteUserPicked = false
                            tripDirectionsRevealed = false
                            tripPendingRouteOverview = false
                            mapDestPickEpoch++
                            tripDestFocusEpoch++
                            tripScope.launch {
                                val label = withContext(Dispatchers.IO) {
                                    reverseGeocodeTripPlace(context, lat, lon).ifBlank {
                                        String.format(Locale.US, "%.5f, %.5f", lat, lon)
                                    }
                                }
                                if (tripDestLat == lat && tripDestLon == lon) {
                                    tripDestLabel = label
                                    TripRecentPlacesRepository.rememberDestination(
                                        context,
                                        label,
                                        lat,
                                        lon,
                                    )
                                }
                            }
                        }
                    },
                    mapCoveredByOverlay = mapCoveredByOverlay,
                    tripDestFocusEpoch = tripDestFocusEpoch,
                    tripRouteOverviewEpoch = tripRouteOverviewEpoch,
                    modifier = mapModifier,
                )
            } else {
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
                gpsPinLocation = tripMapGps,
                gpsAccuracyText = gpsAccuracyText,
                mapSeverityFilter = mapSeverityFilter,
                areaHeatEnabled = areaHeatEnabled && !(tripModeEnabled && mapFullscreen),
                weatherRainEnabled = weatherMapEnabled && !(tripModeEnabled && mapFullscreen),
                weatherRainGrid = weatherRainGrid.takeIf {
                    weatherMapEnabled && !(tripModeEnabled && mapFullscreen)
                },
                // Home map is reports-only; trip overlays only on the fullscreen trip surface.
                tripModeEnabled = tripModeEnabled && mapFullscreen,
                tripNavActive = tripNavActive && mapFullscreen,
                tripStreetFollow = tripStreetFollow,
                tripCameraMode = tripCameraMode,
                tripDeviceHeadingDeg = tripDeviceHeadingDeg,
                tripVehicle = tripVehicle,
                mapCursorStyle = mapCursorStyle,
                mapCursorColor = mapCursorColor,
                tripRoutePoints = tripRoutePoints,
                tripRouteTraveledMeters = if (tripNavActive) {
                    tripRouteProgress?.traveledMeters ?: 0.0
                } else {
                    0.0
                },
                tripRouteOptions = tripRouteOptions.map { it.points },
                tripRouteEtas = tripRouteOptions.mapIndexed { index, option ->
                    val base =
                        "${formatTripEta(option.durationSeconds)}\n${formatTripDistance(option.distanceMeters)}"
                    val hint = tripRouteSaferHints.getOrNull(index)
                    if (hint.isNullOrBlank()) base else "$base\n$hint"
                },
                tripSelectedRouteIndex = tripSelectedRouteIndex,
                onSelectTripRouteIndex = { selectTripRouteOption(it) },
                tripRejoinPoints = tripRejoinPoints,
                tripOfferedAlternatePoints = tripOfferedAlternateIndex
                    ?.let { tripKnownAlternates.getOrNull(it)?.points }
                    .orEmpty(),
                onConfirmOfferedAlternate = { confirmOfferedKnownAlternate() },
                tripAlerts = tripAlerts,
                arrivalPlaces = tripArrivalPlaces,
                // Start/end dots follow the road-snapped route when available (not off-road GPS).
                tripStartLat = tripRoutePoints.firstOrNull()?.first
                    ?: tripStartLat
                    ?: gpsPinLocation?.latitude,
                tripStartLon = tripRoutePoints.firstOrNull()?.second
                    ?: tripStartLon
                    ?: gpsPinLocation?.longitude,
                tripDestinationLat = tripRoutePoints.lastOrNull()?.first ?: tripDestLat,
                tripDestinationLon = tripRoutePoints.lastOrNull()?.second ?: tripDestLon,
                tripStopPoints = tripStops.map { it.latitude to it.longitude },
                recentReportsEpoch = recentReportsEpoch,
                mapConnectivityEpoch = mapConnectivityEpoch,
                mapFullscreen = mapFullscreen,
                onExitFullscreen = { exitMapFullscreen() },
                onReportPothole = onRequestReportFromFullscreenMap,
                tripRoadVideoEnabled = tripRoadVideoEnabled,
                onToggleTripRoadVideo = {
                    applyRoadVideoFromSettings(!tripRoadVideoEnabled)
                },
                onMapPickDestination = { lat, lon ->
                    if (tripModeEnabled && !tripNavActive) {
                        tripDestLat = lat
                        tripDestLon = lon
                        tripDestLabel = "Dropped pin"
                        tripNavActive = false
                        tripRouteUserPicked = false
                        tripDirectionsRevealed = false
                        tripPendingRouteOverview = false
                        mapDestPickEpoch++
                        tripDestFocusEpoch++
                        tripScope.launch {
                            val label = withContext(Dispatchers.IO) {
                                reverseGeocodeTripPlace(context, lat, lon).ifBlank {
                                    String.format(Locale.US, "%.5f, %.5f", lat, lon)
                                }
                            }
                            if (tripDestLat == lat && tripDestLon == lon) {
                                tripDestLabel = label
                                TripRecentPlacesRepository.rememberDestination(
                                    context,
                                    label,
                                    lat,
                                    lon,
                                )
                            }
                        }
                    }
                },
                mapCoveredByOverlay = mapCoveredByOverlay,
                modifier = mapModifier,
            )
            }
        }

        fun startTripNavigation() {
            // Stay fullscreen when leaving the planner for turn-by-turn / overview.
            onMapFullscreenChange(true)
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
                    tripArrived = false
                    showTripArrivalCard = false
                    tripArrivalPlaces = emptyList()
                } finally {
                    tripNavigatePreparing = false
                }
            }
        }

        @Composable
        fun TripPlanner(plannerModifier: Modifier) {
            val hasTripEndpoints = tripDestLat != null && tripDestLon != null &&
                (tripStartLat != null || gpsPinLocation != null)
            val directionsVisible = tripDirectionsRevealed || tripNavActive
            TripPlannerPanel(
                modifier = plannerModifier,
                selectedCity = selectedCity,
                startText = if (tripStartUsesMyLocation) tripMyLocationLabel else tripStartLabel,
                destText = tripDestLabel,
                selectedStartLat = tripStartLat,
                selectedStartLon = tripStartLon,
                selectedDestLat = tripDestLat,
                selectedDestLon = tripDestLon,
                destExternalPickEpoch = mapDestPickEpoch,
                vehicle = tripVehicle,
                startUsesMyLocation = tripStartUsesMyLocation,
                showOriginFields = tripDirectionsRevealed,
                stops = tripStops,
                // Either action is available once endpoints exist — Direction previews,
                // Navigation starts turn-by-turn (and fetches the route if needed).
                canNavigate = hasTripEndpoints && !tripNavigatePreparing,
                canShowDirections = hasTripEndpoints && !tripNavigatePreparing,
                navigatePreparing = tripNavigatePreparing,
                routeStatus = if (directionsVisible) tripRouteStatus else TripRouteStatus.IDLE,
                routeDurationSeconds = if (directionsVisible) {
                    tripRouteOptions.getOrNull(tripSelectedRouteIndex)?.durationSeconds
                } else {
                    null
                },
                routeDistanceMeters = if (directionsVisible) {
                    tripRouteOptions.getOrNull(tripSelectedRouteIndex)?.distanceMeters
                } else {
                    null
                },
                routeOptionLabels = if (directionsVisible) {
                    tripRouteOptions.mapIndexed { index, option ->
                        val journey = JourneyStartEta.estimate(
                            option.durationSeconds,
                            cityKey = selectedCity,
                        )
                        val etaPart = if (journey != null && journey.congested) {
                            JourneyStartEta.formatShortLeaveNow(journey)
                        } else {
                            formatTripEta(option.durationSeconds)
                        }
                        val base =
                            "$etaPart · ${formatTripDistance(option.distanceMeters)}"
                        val hint = tripRouteSaferHints.getOrNull(index)
                        if (hint.isNullOrBlank()) base else "$base · $hint"
                    }
                } else {
                    emptyList()
                },
                selectedRouteIndex = tripSelectedRouteIndex,
                onSelectRouteIndex = { selectTripRouteOption(it) },
                onVehicleChange = {
                    tripVehicleName = it.name
                    tripRouteUserPicked = false
                },
                onStartSelected = { label, lat, lon ->
                    tripStartLat = lat
                    tripStartLon = lon
                    tripStartLabel = label
                    tripNavActive = false
                    tripRouteUserPicked = false
                    TripRecentPlacesRepository.rememberStart(context, label, lat, lon)
                },
                onDestSelected = { label, lat, lon ->
                    tripDestLat = lat
                    tripDestLon = lon
                    tripDestLabel = label
                    tripNavActive = false
                    tripRouteUserPicked = false
                    tripDirectionsRevealed = false
                    tripPendingRouteOverview = false
                    tripDestFocusEpoch++
                },
                onUseMyLocation = {
                    tripStartLat = null
                    tripStartLon = null
                    tripStartLabel = ""
                    tripNavActive = false
                    tripNavOriginLat = null
                    tripNavOriginLon = null
                    tripRouteUserPicked = false
                },
                onClearStart = {
                    tripStartLat = null
                    tripStartLon = null
                    tripStartLabel = ""
                    tripMyLocationLabel = ""
                    tripNavActive = false
                    tripNavOriginLat = null
                    tripNavOriginLon = null
                    tripRouteUserPicked = false
                },
                onClearDestination = {
                    tripDestLat = null
                    tripDestLon = null
                    tripDestLabel = ""
                    tripNavActive = false
                    tripRouteUserPicked = false
                    tripDirectionsRevealed = false
                    tripPendingRouteOverview = false
                    tripStops = emptyList()
                    // No destination → no “live trip” chip state after leaving fullscreen.
                    if (!mapFullscreen) {
                        tripModeEnabled = false
                    }
                },
                onStopsChanged = { tripStops = it },
                onSwapStartDestination = {
                    val gps = gpsPinLocation
                    val sLat = tripStartLat ?: gps?.latitude ?: return@TripPlannerPanel
                    val sLon = tripStartLon ?: gps?.longitude ?: return@TripPlannerPanel
                    val sLabel = if (tripStartUsesMyLocation) {
                        tripMyLocationLabel.ifBlank { "My location" }
                    } else {
                        tripStartLabel.ifBlank { "Start" }
                    }
                    val dLat = tripDestLat ?: return@TripPlannerPanel
                    val dLon = tripDestLon ?: return@TripPlannerPanel
                    val dLabel = tripDestLabel.ifBlank { "Destination" }
                    tripStartLat = dLat
                    tripStartLon = dLon
                    tripStartLabel = dLabel
                    tripDestLat = sLat
                    tripDestLon = sLon
                    tripDestLabel = sLabel
                    tripStops = tripStops.asReversed()
                    tripNavActive = false
                    tripRouteUserPicked = false
                    tripDirectionsRevealed = true
                    tripPendingRouteOverview = true
                },
                onWaypointsReordered = { newStops, newDest ->
                    tripStops = newStops
                    tripDestLat = newDest.latitude
                    tripDestLon = newDest.longitude
                    tripDestLabel = newDest.label
                    tripNavActive = false
                    tripRouteUserPicked = false
                    tripPendingRouteOverview = true
                },
                onDirections = {
                    // Quick GPS refine when the pin is stale/coarse, then reveal route.
                    tripScope.launch {
                        if (tripStartUsesMyLocation) {
                            val live = gpsPinLocation
                            if (live == null || !live.isUsableTripFix(5_000L, 25f)) {
                                onEnsureFreshGpsForTrip()
                            }
                        }
                        // Paint warm prefetch / offline cache immediately so Direction feels instant.
                        val origin = currentPlannerOriginLatLon()
                        val dLat = tripDestLat
                        val dLon = tripDestLon
                        if (origin != null && dLat != null && dLon != null) {
                            val online = NetworkReachability.hasInternet(context)
                            tryApplyAnyCachedRoute(
                                originLat = origin.first,
                                originLon = origin.second,
                                destLat = dLat,
                                destLon = dLon,
                                viaPoints = tripStops.map { it.latitude to it.longitude },
                                offlineLooseOrigin = !online,
                            )
                        }
                        if (!tripDirectionsRevealed) {
                            tripDirectionsRevealed = true
                            tripPendingRouteOverview = true
                        } else if (tripRoutePoints.size >= 2) {
                            tripRouteOverviewEpoch++
                        } else {
                            tripPendingRouteOverview = true
                        }
                    }
                },
                onNavigate = {
                    // Turn-by-turn; may start without a prior Direction press.
                    val origin = currentPlannerOriginLatLon()
                    val dLat = tripDestLat
                    val dLon = tripDestLon
                    if (origin != null && dLat != null && dLon != null) {
                        val online = NetworkReachability.hasInternet(context)
                        tryApplyAnyCachedRoute(
                            originLat = origin.first,
                            originLon = origin.second,
                            destLat = dLat,
                            destLon = dLon,
                            viaPoints = tripStops.map { it.latitude to it.longitude },
                            offlineLooseOrigin = !online,
                        )
                    }
                    tripDirectionsRevealed = true
                    tripCameraModeName = TripCameraMode.STREET.name
                    startTripNavigation()
                },
            )
        }

        @Composable
        fun BoxScope.TripNavChrome(topPadding: Dp) {
            val uiScale = rememberMapUiScale()
            val railBtn = uiScale.dp(44f)
            val railIcon = uiScale.dp(20f)
            val turnIcon = uiScale.dp(22f)
            val speedKmh = remember(gpsPinLocation?.speed, gpsPinLocation?.hasSpeed()) {
                val gps = gpsPinLocation
                if (gps != null && gps.hasSpeed() && gps.speed.isFinite() && gps.speed >= 0f) {
                    (gps.speed * 3.6f).roundToInt().coerceAtLeast(0)
                } else {
                    null
                }
            }
            val turn = tripNextTurn
            val turnKind = remember(turn?.modifier, turn?.maneuverType, turn?.instruction) {
                classifyTripTurnDirection(turn)
            }
            val potholeAlert = nextTripAlert
            val potholeSideLetter = remember(potholeAlert?.sideLabel) {
                formatTripPotholeSideLetter(potholeAlert?.sideLabel)
            }
            val selectedRoute = tripRouteOptions.getOrNull(tripSelectedRouteIndex)
            val etaSeconds = remember(selectedRoute?.durationSeconds, tripRouteProgress?.completionFraction) {
                remainingTripSeconds(selectedRoute?.durationSeconds, tripRouteProgress)
            }
            val remainingMeters = tripRouteProgress?.remainingMeters
            val progressLabel = when {
                tripArrived -> "Arrived"
                tripRerouting -> "Route lost. Finding a new path…"
                etaSeconds != null -> formatTripEta(etaSeconds)
                remainingMeters != null -> formatTripDistance(remainingMeters)
                else -> null
            }
            // Top capsule: speed | turn | pothole, then ETA / progress.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = CitizenSpace.Sm)
                    .padding(top = topPadding)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CitizenSpace.Xs),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CitizenRadius.Capsule,
                    color = CitizenMapChrome.Panel,
                    shadowElevation = 3.dp,
                    border = BorderStroke(CitizenBorder.Thin, CitizenMapChrome.PanelBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = CitizenSpace.Md, vertical = CitizenSpace.Sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs),
                        ) {
                            Icon(
                                imageVector = turnKind.icon,
                                contentDescription = turnKind.label,
                                tint = DarkBlue,
                                modifier = Modifier.size(turnIcon),
                            )
                            if (turn != null) {
                                Text(
                                    text = formatNavDistanceMeters(turn.distanceMeters),
                                    style = if (turnKind == TripNavTurnKind.ARRIVE) {
                                        CitizenType.Label
                                    } else {
                                        CitizenType.ScreenTitle
                                    },
                                    color = if (turnKind == TripNavTurnKind.ARRIVE) {
                                        CitizenInk.Helper
                                    } else {
                                        Color(0xFF111827)
                                    },
                                    maxLines = 1,
                                )
                            } else {
                                Text(
                                    text = "—",
                                    style = CitizenType.ScreenTitle,
                                    color = DarkBlue,
                                    maxLines = 1,
                                )
                            }
                        }
                        TripNavCapsuleDivider()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = speedKmh?.toString() ?: "—",
                                style = CitizenType.Label,
                                color = DarkBlue,
                            )
                            Text(
                                text = "km/h",
                                style = CitizenType.ChipCompact,
                                color = CitizenInk.Helper,
                            )
                        }
                        TripNavCapsuleDivider()
                        if (potholeAlert != null) {
                            val severity = potholeAlert.report.severity
                            val ink = Color(tripPotholeAlertInkArgb(severity))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs),
                            ) {
                                Text(
                                    text = "Pothole",
                                    style = CitizenType.ChipCompact,
                                    color = CitizenInk.Helper,
                                    maxLines = 1,
                                )
                                Text(
                                    text = formatNavDistanceMeters(potholeAlert.distanceMeters),
                                    style = CitizenType.Label,
                                    color = Color(0xFF111827),
                                    maxLines = 1,
                                )
                                if (potholeSideLetter != null) {
                                    Text(
                                        text = potholeSideLetter,
                                        style = CitizenType.Label,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ink,
                                        maxLines = 1,
                                    )
                                }
                                Text(
                                    text = severity.driverShort,
                                    style = CitizenType.ChipCompact,
                                    fontWeight = FontWeight.Bold,
                                    color = ink,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            Text(
                                text = "No pothole ahead",
                                style = CitizenType.Chip,
                                color = CitizenInk.Helper,
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (progressLabel != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CitizenRadius.Capsule,
                        color = CitizenMapChrome.PanelSoft,
                        shadowElevation = 2.dp,
                        border = BorderStroke(CitizenBorder.Thin, CitizenMapChrome.PanelBorder),
                    ) {
                        Text(
                            text = progressLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CitizenSpace.Md, vertical = CitizenSpace.Xs),
                            style = CitizenType.Chip,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                tripArrived -> CitizenInk.Success
                                tripRerouting -> CitizenInk.Warning
                                else -> DarkBlue
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
                if (!tripNavOnline && !tripArrived) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CitizenRadius.Capsule,
                        color = Color(0xFFFFF7ED),
                        shadowElevation = 1.dp,
                        border = BorderStroke(CitizenBorder.Thin, Color(0xFFFDBA74)),
                    ) {
                        Text(
                            text = "Offline — GPS continues · map updates when online",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CitizenSpace.Md, vertical = CitizenSpace.Xs),
                            style = CitizenType.ChipCompact,
                            fontWeight = FontWeight.SemiBold,
                            color = CitizenInk.Warning,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            // Left rail — same FAB size language as locate / zoom / exit.
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = CitizenSpace.Sm, top = topPadding + uiScale.dp(72f)),
                verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        tripNavActive = false
                        tripNavOriginLat = null
                        tripNavOriginLon = null
                    },
                    modifier = Modifier.size(railBtn),
                    containerColor = CitizenSelection.Active,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Stop navigation",
                        modifier = Modifier.size(railIcon),
                    )
                }
                SmallFloatingActionButton(
                    onClick = {
                        val next = !tripVoiceMuted
                        tripVoiceMuted = next
                        TripVoiceGuidance.setMuted(context, next)
                        if (next) tripVoice.stop()
                    },
                    modifier = Modifier.size(railBtn),
                    containerColor = Color.White,
                    contentColor = DarkBlue,
                    shape = CircleShape,
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
                        modifier = Modifier.size(railIcon),
                    )
                }
                SmallFloatingActionButton(
                    onClick = {
                        tripCameraModeName = tripCameraMode.next().name
                    },
                    modifier = Modifier
                        .size(railBtn)
                        .semantics {
                            contentDescription = when (tripCameraMode) {
                                TripCameraMode.STREET -> "Street view. Tap for map overview"
                                TripCameraMode.MAP -> "Map overview. Tap for play mode"
                                TripCameraMode.PLAY -> "Play mode. Tap for street view"
                            }
                        },
                    containerColor = if (tripCameraMode != TripCameraMode.MAP) DarkBlue else Color.White,
                    contentColor = if (tripCameraMode != TripCameraMode.MAP) Color.White else DarkBlue,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = when (tripCameraMode) {
                            TripCameraMode.STREET -> Icons.Filled.Navigation
                            TripCameraMode.MAP -> Icons.Outlined.Map
                            TripCameraMode.PLAY -> Icons.Filled.SportsEsports
                        },
                        contentDescription = null,
                        modifier = Modifier.size(railIcon),
                    )
                }
                // #32 Phase 2a: Switch camera ↔ map (only while road-video toggle is ON).
                if (tripRoadVideoEnabled && tripCaptureSwitchEnabled) {
                    SmallFloatingActionButton(
                        onClick = { tripCaptureShowCamera = !tripCaptureShowCamera },
                        modifier = Modifier
                            .size(railBtn)
                            .semantics {
                                contentDescription = if (tripCaptureShowCamera) {
                                    "Show map"
                                } else {
                                    "Show road camera"
                                }
                            },
                        containerColor = if (tripCaptureShowCamera) {
                            Color(0xFFB74233)
                        } else {
                            Color.White
                        },
                        contentColor = if (tripCaptureShowCamera) Color.White else DarkBlue,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = if (tripCaptureShowCamera) {
                                Icons.Outlined.Map
                            } else {
                                Icons.Filled.Videocam
                            },
                            contentDescription = null,
                            modifier = Modifier.size(railIcon),
                        )
                    }
                }
            }
            // When full road camera covers the map, keep Report reachable.
            if (
                tripRoadVideoEnabled &&
                tripCaptureShowCamera &&
                tripCaptureSwitchEnabled
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 72.dp)
                        .systemGestureExclusion()
                        .height(CitizenIcon.Fab)
                        .width(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB74233))
                        .border(1.dp, Color(0xFF8B2F24), CircleShape)
                        .clickable(onClick = onRequestReportFromFullscreenMap)
                        .semantics { contentDescription = "Report a pothole" },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(CitizenIcon.FabGlyph),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Report",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // One layout tree for map so expand/collapse never remounts MapView (that was
        // backgrounding / "minimizing" the activity when leaving fullscreen).
        Column(
            modifier = modifier.background(Color(0xFFFDFCF9)),
        ) {
            if (mapFullscreen && tripModeEnabled && !tripNavActive) {
                TripPlanner(
                    plannerModifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth(),
            ) {
                val config = LocalConfiguration.current
                val splitLandscape =
                    tripRoadVideoEnabled &&
                        tripCaptureSplitActive &&
                        mapFullscreen &&
                        tripModeEnabled &&
                        tripNavActive &&
                        config.screenWidthDp >= config.screenHeightDp
                val splitPortrait =
                    tripRoadVideoEnabled &&
                        tripCaptureSplitActive &&
                        mapFullscreen &&
                        tripModeEnabled &&
                        tripNavActive &&
                        !splitLandscape &&
                        config.screenWidthDp < config.screenHeightDp
                val splitNow = splitLandscape || splitPortrait

                // #32 Phase 2b: camera pane (map stays a single DensityMap call below).
                if (splitNow) {
                    Box(
                        modifier = if (splitLandscape) {
                            Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                        } else {
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(0.5f)
                        },
                    ) {
                        TripRoadCameraPreview(
                            roadSource = tripCaptureRoad,
                            cabinSource = tripCaptureCabin,
                            companionPairingCode = tripCompanionPairingCode,
                            showDetectionOverlay = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Hairline between panes.
                    Box(
                        modifier = if (splitLandscape) {
                            Modifier
                                .align(Alignment.Center)
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(Color(0xFF111827).copy(alpha = 0.35f))
                        } else {
                            Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF111827).copy(alpha = 0.35f))
                        },
                    )
                }

                Box(
                    modifier = when {
                        splitLandscape -> Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                        splitPortrait -> Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                        else -> Modifier.fillMaxSize()
                    },
                ) {
                    DensityMap(mapModifier = Modifier.fillMaxSize())
                }

                // #32 Phase 1–2a: full-bleed road camera (Switch / Camera modes).
                if (!splitNow &&
                    mapFullscreen &&
                    tripModeEnabled &&
                    tripNavActive &&
                    tripRoadVideoEnabled &&
                    tripCaptureSwitchEnabled &&
                    tripCaptureShowCamera
                ) {
                    TripRoadCameraPreview(
                        roadSource = tripCaptureRoad,
                        cabinSource = tripCaptureCabin,
                        companionPairingCode = tripCompanionPairingCode,
                        showDetectionOverlay = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Settings = Map only, but video toggle ON → headless capture for bump analysis.
                if (!splitNow &&
                    mapFullscreen &&
                    tripModeEnabled &&
                    tripNavActive &&
                    tripRoadVideoEnabled &&
                    tripCaptureDisplay == TripCaptureDisplayMode.MAP_ONLY
                ) {
                    TripRoadCameraPreview(
                        roadSource = tripCaptureRoad,
                        cabinSource = tripCaptureCabin,
                        companionPairingCode = tripCompanionPairingCode,
                        showDetectionOverlay = false,
                        modifier = Modifier
                            .size(1.dp)
                            .alpha(0f),
                    )
                }
                // Trip planner / nav chrome live only on the fullscreen trip map.
                if (mapFullscreen && tripModeEnabled && tripNavActive) {
                    val dangerSpeedMps = remember(
                        gpsPinLocation?.speed,
                        gpsPinLocation?.hasSpeed(),
                    ) {
                        val gps = gpsPinLocation
                        gps?.speed?.takeIf { gps.hasSpeed() && it.isFinite() && it >= 0f }
                    }
                    val dangerBlink = remember(
                        tripAlerts,
                        tripRouteProgress?.traveledMeters,
                        dangerSpeedMps,
                        tripArrived,
                    ) {
                        if (tripArrived) {
                            null
                        } else {
                            TripDangerBlink.activeBlink(
                                zones = TripDangerBlink.buildZones(tripAlerts),
                                traveledMeters = tripRouteProgress?.traveledMeters ?: 0.0,
                                speedMps = dangerSpeedMps,
                            )
                        }
                    }
                    TripDangerEdgeBlink(
                        state = dangerBlink,
                        speedMps = dangerSpeedMps,
                        daytime = TripDangerBlink.isDaytime(),
                    )
                    TripNavChrome(topPadding = CitizenSpace.Sm)
                }
                if (showTripArrivalCard && mapFullscreen) {
                    TripArrivalPlacesCard(
                        destinationLabel = tripDestLabel,
                        places = tripArrivalPlaces,
                        loading = !tripArrivalLookupDone,
                        onDismiss = {
                            showTripArrivalCard = false
                            tripArrivalPlaces = emptyList()
                            tripArrivalLookupDone = false
                            tripArrived = false
                            tripModeEnabled = false
                            exitMapFullscreen()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                } else {
                    val bump = pendingBumpEvent
                    if (
                        bump != null &&
                        mapFullscreen &&
                        tripModeEnabled &&
                        tripNavActive
                    ) {
                        TripBumpConfirmCard(
                            peakLabel = String.format(java.util.Locale.US, "%.1f m/s²", bump.peakMps2),
                            analyzing = bumpAnalyzing,
                            resultSummary = bumpVisionSummary,
                            resultTier = bumpVisionTier,
                            reportCreated = bumpReportCreated,
                            locationOnly = bumpLocationOnly,
                            unconfirmedSaved = bumpUnconfirmedSaved,
                            onYes = {
                                TripImuBumpLog.updateResponse(
                                    bump.id,
                                    TripImuBumpUserResponse.YES,
                                )
                            },
                            onNo = {
                                TripImuBumpLog.updateResponse(
                                    bump.id,
                                    TripImuBumpUserResponse.NO,
                                )
                                pendingBumpEvent = null
                            },
                            onHazardKind = { kind ->
                                val id = bumpUnconfirmedId
                                val item = id?.let { TripUnconfirmedDetectionStore.get(it) }
                                if (item != null) {
                                    val result = TripBumpVisionPipeline.confirmUnconfirmed(
                                        context = context,
                                        item = item,
                                        cityKey = selectedCity,
                                        reporterUserId = reporterUserId.takeIf { it.isNotBlank() },
                                        signedIn = isSignedIn,
                                        hazardKind = kind,
                                    )
                                    if (result.created) {
                                        onReportsMutated()
                                        CitizenNotificationsRepository.notifyVisionCapture(
                                            title = "Report filed from Unconfirmed",
                                            body = result.reason
                                                ?: "Check My Reports — uploading to cloud.",
                                            reportId = result.reportId,
                                        )
                                        onNotificationsMutated()
                                    } else if (result.crowdRecorded) {
                                        onReportsMutated()
                                    }
                                    onFeedbackMessage(
                                        result.reason
                                            ?: if (result.created) {
                                                "Report filed — check My Reports"
                                            } else if (result.crowdRecorded) {
                                                "Map mark only — not in My Reports"
                                            } else {
                                                "Could not confirm"
                                            },
                                    )
                                }
                                TripImuBumpLog.updateResponse(
                                    bump.id,
                                    TripImuBumpUserResponse.YES,
                                )
                                pendingBumpEvent = null
                            },
                            onDismiss = {
                                TripImuBumpLog.updateResponse(
                                    bump.id,
                                    TripImuBumpUserResponse.DISMISSED,
                                )
                                pendingBumpEvent = null
                            },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    } else if (
                        pendingAvoidPrompt &&
                        mapFullscreen &&
                        tripModeEnabled &&
                        tripNavActive &&
                        swerveDetectionEnabled
                    ) {
                        TripAvoidConfirmCard(
                            onHazardKind = { kind ->
                                if (kind.mapsAsCrowdHazard) {
                                    TripCrowdHazardStore.recordConfirm(
                                        context = context,
                                        cityKey = selectedCity,
                                        lat = avoidPromptLat,
                                        lon = avoidPromptLon,
                                        kind = kind,
                                        confirmerUserId = reporterUserId.ifBlank { "local" },
                                    )
                                    onReportsMutated()
                                    onFeedbackMessage("${kind.label} saved to your map")
                                } else {
                                    onFeedbackMessage("OK — ignored as ${kind.label.lowercase()}")
                                }
                                pendingAvoidPrompt = false
                            },
                            onDismiss = { pendingAvoidPrompt = false },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
                if (showExpand) {
                    SmallFloatingActionButton(
                        onClick = { onMapFullscreenChange(true) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 44.dp, end = CitizenSpace.Sm)
                            .size(CitizenIcon.Fab),
                        containerColor = CitizenMapChrome.Panel,
                        contentColor = DarkBlue,
                        shape = CircleShape,
                    ) {
                        Icon(
                            Icons.Filled.Fullscreen,
                            contentDescription = "Expand map",
                            modifier = Modifier.size(CitizenIcon.FabGlyph),
                        )
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
                .padding(horizontal = CitizenSpace.Lg)
        },
    ) {
        MapSection(
            modifier = Modifier.fillMaxSize(),
            showExpand = !mapFullscreen && !CitizenMapTiler.isConfigured,
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
 * On-map trip planner: destination search, Car / Bike / Walk, Direction, and Navigation.
 * Start defaults to the device GPS location.
 * After Direction, start + optional stops appear with a vertical + / swap capsule.
 */
private data class TripStopPlace(
    val label: String,
    val latitude: Double,
    val longitude: Double,
)

private const val MAX_TRIP_STOPS = 3

@Composable
private fun TripPlannerPanel(
    modifier: Modifier = Modifier,
    selectedCity: String,
    startText: String,
    destText: String,
    selectedStartLat: Double? = null,
    selectedStartLon: Double? = null,
    selectedDestLat: Double? = null,
    selectedDestLon: Double? = null,
    /** Bumped when destination is set from the map so the search field clears. */
    destExternalPickEpoch: Int = 0,
    vehicle: TripVehicle,
    startUsesMyLocation: Boolean,
    /** After Direction — show start field, stops, and +/swap capsule. */
    showOriginFields: Boolean = false,
    stops: List<TripStopPlace> = emptyList(),
    canNavigate: Boolean,
    canShowDirections: Boolean = false,
    navigatePreparing: Boolean = false,
    routeStatus: TripRouteStatus = TripRouteStatus.IDLE,
    routeDurationSeconds: Double? = null,
    routeDistanceMeters: Double? = null,
    routeOptionLabels: List<String> = emptyList(),
    selectedRouteIndex: Int = 0,
    onSelectRouteIndex: (Int) -> Unit = {},
    onVehicleChange: (TripVehicle) -> Unit,
    onStartSelected: (label: String, lat: Double, lon: Double) -> Unit,
    onDestSelected: (label: String, lat: Double, lon: Double) -> Unit,
    onUseMyLocation: () -> Unit,
    onClearStart: () -> Unit,
    onClearDestination: () -> Unit,
    onStopsChanged: (List<TripStopPlace>) -> Unit = {},
    onSwapStartDestination: () -> Unit = {},
    /** Reorder stops + destination: [newStops] then [newDestination] as final leg. */
    onWaypointsReordered: (newStops: List<TripStopPlace>, newDestination: TripStopPlace) -> Unit = { _, _ -> },
    onDirections: () -> Unit,
    onNavigate: () -> Unit,
) {
    val context = LocalContext.current
    val uiScale = rememberMapUiScale()
    val fieldFont = uiScale.sp(12f)
    val fieldIcon = uiScale.dp(18f)
    var startQuery by remember { mutableStateOf("") }
    var destQuery by remember { mutableStateOf("") }
    var startResults by remember { mutableStateOf<List<TripPlaceSuggestion>>(emptyList()) }
    var destResults by remember { mutableStateOf<List<TripPlaceSuggestion>>(emptyList()) }
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
    /** 0 = none, 1 = start, 2 = destination, 10+i = stop i. */
    var activeField by remember { mutableIntStateOf(0) }
    var placePendingSave by remember { mutableStateOf<TripRecentPlace?>(null) }
    /** Local queries while typing over an existing stop label. */
    var stopEditQueries by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    /** Empty slot awaiting a place pick after tapping +. */
    var addingStop by remember { mutableStateOf(false) }
    var newStopQuery by remember { mutableStateOf("") }
    var stopResults by remember { mutableStateOf<List<TripPlaceSuggestion>>(emptyList()) }
    var stopSearching by remember { mutableStateOf(false) }
    var stopSearchAttempted by remember { mutableStateOf(false) }

    fun requestSavePlace(place: TripRecentPlace) {
        placePendingSave = place
    }

    fun stopFieldId(index: Int) = 10 + index
    val newStopFieldId = 9

    LaunchedEffect(destExternalPickEpoch) {
        if (destExternalPickEpoch <= 0) return@LaunchedEffect
        destQuery = ""
        destResults = emptyList()
        destSearchAttempted = false
        activeField = 0
        recentDestinations = TripRecentPlacesRepository.destinations(context)
    }

    LaunchedEffect(startUsesMyLocation) {
        if (startUsesMyLocation) {
            startQuery = ""
            startResults = emptyList()
            if (activeField == 1) activeField = 0
        }
    }

    LaunchedEffect(stops) {
        // Drop edit buffers for removed indices after swap/clear.
        stopEditQueries = stopEditQueries.filterKeys { it in stops.indices }
        if (stops.size >= MAX_TRIP_STOPS) addingStop = false
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
        delay(220)
        startSearching = true
        startResults = searchTripPlaces(
            context = context,
            query = q,
            selectedCity = selectedCity,
            recentPlaces = recentStarts + recentDestinations,
            limit = 20,
        )
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
        delay(220)
        destSearching = true
        destResults = searchTripPlaces(
            context = context,
            query = q,
            selectedCity = selectedCity,
            recentPlaces = recentDestinations + recentStarts,
            limit = 20,
        )
        destSearching = false
        destSearchAttempted = true
    }
    LaunchedEffect(newStopQuery, stopEditQueries, activeField, selectedCity, addingStop) {
        val q = when {
            activeField == newStopFieldId -> newStopQuery.trim()
            activeField >= 10 -> stopEditQueries[activeField - 10]?.trim().orEmpty()
            else -> return@LaunchedEffect
        }
        if (q.length < 2) {
            stopResults = emptyList()
            stopSearching = false
            stopSearchAttempted = false
            return@LaunchedEffect
        }
        delay(220)
        stopSearching = true
        stopResults = searchTripPlaces(
            context = context,
            query = q,
            selectedCity = selectedCity,
            recentPlaces = recentDestinations + recentStarts,
            limit = 20,
        )
        stopSearching = false
        stopSearchAttempted = true
    }

    val fieldText = DarkBlue
    val placeholderColor = CitizenInk.Helper
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

    fun sameTripPlace(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
        val dLat = (lat1 - lat2) * 111_320.0
        val dLon = (lon1 - lon2) * 111_320.0 *
            kotlin.math.cos(Math.toRadians((lat1 + lat2) / 2.0))
        return kotlin.math.sqrt(dLat * dLat + dLon * dLon) < 45.0
    }

    fun refreshStopSuggestionPools() {
        recentStarts = TripRecentPlacesRepository.starts(context)
        recentDestinations = TripRecentPlacesRepository.destinations(context)
    }

    /** Recent starts + destinations (and saved places) for stop fields. */
    fun stopSuggestionPlaces(excludeStopIndex: Int?): List<TripRecentPlace> {
        fun occupied(lat: Double, lon: Double): Boolean {
            val sLat = selectedStartLat
            val sLon = selectedStartLon
            if (sLat != null && sLon != null && sameTripPlace(lat, lon, sLat, sLon)) return true
            val dLat = selectedDestLat
            val dLon = selectedDestLon
            if (dLat != null && dLon != null && sameTripPlace(lat, lon, dLat, dLon)) return true
            stops.forEachIndexed { i, stop ->
                if (excludeStopIndex != null && i == excludeStopIndex) return@forEachIndexed
                if (sameTripPlace(lat, lon, stop.latitude, stop.longitude)) return true
            }
            return false
        }
        val seen = LinkedHashSet<String>()
        val out = ArrayList<TripRecentPlace>()
        fun addAll(places: List<TripRecentPlace>) {
            for (place in places) {
                if (occupied(place.latitude, place.longitude)) continue
                val key = "${(place.latitude * 1e4).toInt()}_${(place.longitude * 1e4).toInt()}"
                if (!seen.add(key)) continue
                out += place
            }
        }
        // Saved first, then recent destinations, then starts — same pool user already searched.
        addAll(recentDestinations.filter { it.isFavorite })
        addAll(recentDestinations.filterNot { it.isFavorite })
        addAll(recentStarts)
        return out
    }

    fun applyStopPick(index: Int?, label: String, lat: Double, lon: Double) {
        val place = TripStopPlace(label = label, latitude = lat, longitude = lon)
        if (index == null) {
            onStopsChanged(stops + place)
            addingStop = false
            newStopQuery = ""
        } else {
            onStopsChanged(stops.toMutableList().also { it[index] = place })
            stopEditQueries = stopEditQueries - index
        }
        stopResults = emptyList()
        activeField = 0
        recentDestinations = TripRecentPlacesRepository.rememberDestination(context, label, lat, lon)
    }

    val density = LocalDensity.current
    val waypointRowHeightPx = with(density) { 56.dp.toPx() }
    var draggingWaypointIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val destWaypoint = remember(destText, selectedDestLat, selectedDestLon) {
        val lat = selectedDestLat
        val lon = selectedDestLon
        if (
            lat != null && lon != null &&
            lat.isFinite() && lon.isFinite() &&
            destText.isNotBlank()
        ) {
            TripStopPlace(label = destText, latitude = lat, longitude = lon)
        } else {
            null
        }
    }
    val orderedWaypoints = remember(stops, destWaypoint) {
        buildList {
            addAll(stops)
            destWaypoint?.let { add(it) }
        }
    }
    val canReorderWaypoints = showOriginFields && orderedWaypoints.size >= 2

    fun commitWaypointOrder(ordered: List<TripStopPlace>) {
        if (ordered.isEmpty()) return
        val newDest = ordered.last()
        val newStops = ordered.dropLast(1).take(MAX_TRIP_STOPS)
        destQuery = ""
        stopEditQueries = emptyMap()
        activeField = 0
        addingStop = false
        newStopQuery = ""
        stopResults = emptyList()
        onWaypointsReordered(newStops, newDest)
    }

    fun moveWaypoint(from: Int, to: Int) {
        if (!canReorderWaypoints) return
        if (from == to || from !in orderedWaypoints.indices) return
        val list = orderedWaypoints.toMutableList()
        val item = list.removeAt(from)
        list.add(to.coerceIn(0, list.size), item)
        commitWaypointOrder(list)
    }

    @Composable
    fun WaypointReorderChrome(index: Int, content: @Composable () -> Unit) {
        val total = orderedWaypoints.size
        val isDragging = draggingWaypointIndex == index
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(if (isDragging) 2f else 0f)
                .offset {
                    IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { moveWaypoint(index, index - 1) },
                    enabled = index > 0,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (index > 0) DarkBlue else Color(0xFFD1D5DB),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = "Long-press and drag to reorder",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier
                        .size(22.dp)
                        .pointerInput(index, total, canReorderWaypoints) {
                            if (!canReorderWaypoints) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingWaypointIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffsetY += amount.y
                                },
                                onDragEnd = {
                                    val from = draggingWaypointIndex
                                    if (from != null) {
                                        val shift = (dragOffsetY / waypointRowHeightPx).roundToInt()
                                        val target = (from + shift).coerceIn(0, total - 1)
                                        if (target != from) moveWaypoint(from, target)
                                    }
                                    draggingWaypointIndex = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingWaypointIndex = null
                                    dragOffsetY = 0f
                                },
                            )
                        },
                )
                IconButton(
                    onClick = { moveWaypoint(index, index + 1) },
                    enabled = index < total - 1,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (index < total - 1) DarkBlue else Color(0xFFD1D5DB),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.97f),
        shadowElevation = 4.dp,
        contentColor = DarkBlue,
    ) {
        Column(Modifier.padding(8.dp)) {
            // Default origin is current GPS until the user picks another start.
            LaunchedEffect(Unit) { onUseMyLocation() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (showOriginFields) {
                        OutlinedTextField(
                            value = startQuery,
                            onValueChange = {
                                startQuery = it
                                activeField = 1
                                startSearchAttempted = false
                            },
                            singleLine = true,
                            placeholder = {
                                Text(
                                    startText.ifBlank {
                                        if (startUsesMyLocation) "My location" else "Starting point"
                                    },
                                    fontSize = fieldFont,
                                    color = placeholderColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = fieldText,
                                fontSize = fieldFont,
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.MyLocation,
                                    contentDescription = "Start",
                                    tint = DarkBlue,
                                    modifier = Modifier.size(fieldIcon),
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!startUsesMyLocation || startQuery.isNotBlank() || startText.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                startQuery = ""
                                                startResults = emptyList()
                                                startSearchAttempted = false
                                                activeField = 1
                                                onUseMyLocation()
                                            },
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Icon(
                                                Icons.Outlined.Close,
                                                contentDescription = "Use my location",
                                                tint = Color(0xFF4B5563),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
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
                            TripSuggestionList(startResults) { suggestion ->
                                startQuery = suggestion.label
                                startResults = emptyList()
                                activeField = 0
                                recentStarts = TripRecentPlacesRepository.rememberStart(
                                    context,
                                    suggestion.label,
                                    suggestion.latitude,
                                    suggestion.longitude,
                                )
                                onStartSelected(
                                    suggestion.label,
                                    suggestion.latitude,
                                    suggestion.longitude,
                                )
                            }
                        } else if (activeField == 1 && startSearchAttempted) {
                            TripSearchStatus("No matching places found. Try a landmark, area, or postcode.")
                        } else if (activeField == 1 && startQuery.isBlank() && recentStarts.isNotEmpty()) {
                            TripRecentPlacesPanel(
                                title = "Recent starts",
                                places = recentStarts,
                                showFavorite = false,
                                onPick = { place ->
                                    startQuery = place.displayName
                                    activeField = 0
                                    recentStarts = TripRecentPlacesRepository.rememberStart(
                                        context, place.label, place.latitude, place.longitude,
                                    )
                                    onStartSelected(place.displayName, place.latitude, place.longitude)
                                },
                                onToggleFavorite = null,
                                onRemove = { place ->
                                    recentStarts = TripRecentPlacesRepository.removeStart(
                                        context, place.latitude, place.longitude,
                                    )
                                },
                            )
                        }

                        stops.forEachIndexed { index, stop ->
                            Spacer(modifier = Modifier.height(4.dp))
                            val stopValue = stopEditQueries[index] ?: stop.label
                            val stopField = @Composable {
                                OutlinedTextField(
                                    value = stopValue,
                                    onValueChange = {
                                        stopEditQueries = stopEditQueries + (index to it)
                                        activeField = stopFieldId(index)
                                        stopSearchAttempted = false
                                    },
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            "Stop ${index + 1}",
                                            fontSize = fieldFont,
                                            color = placeholderColor,
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = fieldText,
                                        fontSize = fieldFont,
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = "Stop",
                                            tint = Color(0xFFEA580C),
                                            modifier = Modifier.size(fieldIcon),
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                onStopsChanged(stops.filterIndexed { i, _ -> i != index })
                                                stopEditQueries = stopEditQueries
                                                    .filterKeys { it != index }
                                                    .mapKeys { (k, _) -> if (k > index) k - 1 else k }
                                                activeField = 0
                                            },
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Icon(
                                                Icons.Outlined.Close,
                                                contentDescription = "Remove stop",
                                                tint = Color(0xFF4B5563),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    },
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged {
                                            if (it.isFocused) {
                                                activeField = stopFieldId(index)
                                                refreshStopSuggestionPools()
                                            }
                                        },
                                )
                            }
                            if (canReorderWaypoints) {
                                WaypointReorderChrome(index = index, content = stopField)
                            } else {
                                stopField()
                            }
                        }
                        if (addingStop) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = newStopQuery,
                                onValueChange = {
                                    newStopQuery = it
                                    activeField = newStopFieldId
                                    stopSearchAttempted = false
                                },
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        "Search or pick a stop",
                                        fontSize = fieldFont,
                                        color = placeholderColor,
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = fieldText,
                                    fontSize = fieldFont,
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(fieldIcon),
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            addingStop = false
                                            newStopQuery = ""
                                            stopResults = emptyList()
                                            activeField = 0
                                        },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Cancel stop",
                                            tint = Color(0xFF4B5563),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                },
                                colors = fieldColors,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            activeField = newStopFieldId
                                            refreshStopSuggestionPools()
                                        }
                                    },
                            )
                        }
                        if (activeField == newStopFieldId || activeField >= 10) {
                            val editingIdx = if (activeField >= 10) activeField - 10 else null
                            val stopQueryLen = when {
                                activeField == newStopFieldId -> newStopQuery.trim().length
                                editingIdx != null ->
                                    stopEditQueries[editingIdx]?.trim()?.length
                                        ?: 0 // focused existing stop → show recents until typing
                                else -> 0
                            }
                            val showStopRecents = stopQueryLen < 2
                            val stopRecents = if (showStopRecents) {
                                stopSuggestionPlaces(excludeStopIndex = editingIdx)
                            } else {
                                emptyList()
                            }
                            if (stopSearching) {
                                TripSearchStatus("Searching places…")
                            } else if (stopResults.isNotEmpty()) {
                                TripSuggestionList(stopResults) { suggestion ->
                                    applyStopPick(
                                        editingIdx,
                                        suggestion.label,
                                        suggestion.latitude,
                                        suggestion.longitude,
                                    )
                                }
                            } else if (stopSearchAttempted) {
                                TripSearchStatus("No matching places found. Try a landmark, area, or postcode.")
                            } else if (stopRecents.isNotEmpty()) {
                                val favorites = stopRecents.filter { it.isFavorite }
                                val recentOnly = stopRecents.filterNot { it.isFavorite }
                                if (favorites.isNotEmpty()) {
                                    TripRecentPlacesPanel(
                                        title = "Saved places",
                                        places = favorites,
                                        showFavorite = true,
                                        onPick = { place ->
                                            applyStopPick(
                                                editingIdx,
                                                place.displayName,
                                                place.latitude,
                                                place.longitude,
                                            )
                                        },
                                        onToggleFavorite = { place -> requestSavePlace(place) },
                                        onRemove = { place ->
                                            recentDestinations =
                                                TripRecentPlacesRepository.removeDestination(
                                                    context,
                                                    place.latitude,
                                                    place.longitude,
                                                )
                                        },
                                    )
                                }
                                if (recentOnly.isNotEmpty()) {
                                    TripRecentPlacesPanel(
                                        title = "Recent places",
                                        places = recentOnly,
                                        showFavorite = true,
                                        onPick = { place ->
                                            applyStopPick(
                                                editingIdx,
                                                place.displayName,
                                                place.latitude,
                                                place.longitude,
                                            )
                                        },
                                        onToggleFavorite = { place -> requestSavePlace(place) },
                                        onRemove = { place ->
                                            recentDestinations =
                                                TripRecentPlacesRepository.removeDestination(
                                                    context,
                                                    place.latitude,
                                                    place.longitude,
                                                )
                                            recentStarts =
                                                TripRecentPlacesRepository.removeStart(
                                                    context,
                                                    place.latitude,
                                                    place.longitude,
                                                )
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val destField = @Composable {
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
                                destText.ifBlank { "Search or tap map" },
                                fontSize = fieldFont,
                                color = placeholderColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = fieldText,
                            fontSize = fieldFont,
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Navigation,
                                contentDescription = "Destination",
                                tint = Color(0xFFB91C1C),
                                modifier = Modifier.size(fieldIcon),
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val destLat = selectedDestLat
                                val destLon = selectedDestLon
                                if (
                                    destText.isNotBlank() &&
                                    destLat != null &&
                                    destLon != null &&
                                    destLat.isFinite() &&
                                    destLon.isFinite()
                                ) {
                                    val alreadySaved = recentDestinations.any {
                                        it.isFavorite &&
                                            kotlin.math.abs(it.latitude - destLat) < 0.0001 &&
                                            kotlin.math.abs(it.longitude - destLon) < 0.0001
                                    }
                                    IconButton(
                                        onClick = {
                                            val match = recentDestinations.firstOrNull {
                                                kotlin.math.abs(it.latitude - destLat) < 0.0001 &&
                                                    kotlin.math.abs(it.longitude - destLon) < 0.0001
                                            }
                                            requestSavePlace(
                                                match?.copy(label = destText.ifBlank { match.label })
                                                    ?: TripRecentPlace(
                                                        label = destText,
                                                        latitude = destLat,
                                                        longitude = destLon,
                                                    ),
                                            )
                                        },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (alreadySaved) {
                                                Icons.Filled.Star
                                            } else {
                                                Icons.Outlined.StarBorder
                                            },
                                            contentDescription = if (alreadySaved) {
                                                "Edit saved place"
                                            } else {
                                                "Save destination as…"
                                            },
                                            tint = if (alreadySaved) Color(0xFFCA8A04) else CitizenInk.Helper,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                if (destQuery.isNotBlank() || destText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            destQuery = ""
                                            destResults = emptyList()
                                            destSearchAttempted = false
                                            recentDestinations = TripRecentPlacesRepository.destinations(context)
                                            activeField = 2
                                            onClearDestination()
                                        },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Clear destination",
                                            tint = Color(0xFF4B5563),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
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
                    }
                    if (canReorderWaypoints) {
                        WaypointReorderChrome(index = stops.size, content = destField)
                        Text(
                            "↑↓ or long-press ☰ to reorder stops & destination",
                            fontSize = 9.sp,
                            color = CitizenInk.Helper,
                            modifier = Modifier.padding(start = 28.dp, top = 2.dp),
                        )
                    } else {
                        destField()
                    }
                    if (destText.isBlank() && destQuery.isBlank()) {
                        Text(
                            "Or tap a place on the map below",
                            fontSize = 10.sp,
                            color = CitizenInk.Helper,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        )
                    }
                    if (activeField == 2 && destSearching) {
                        TripSearchStatus("Searching places…")
                    } else if (activeField == 2 && destResults.isNotEmpty()) {
                        TripSuggestionList(destResults) { suggestion ->
                            destQuery = suggestion.label
                            destResults = emptyList()
                            activeField = 0
                            recentDestinations = TripRecentPlacesRepository.rememberDestination(
                                context,
                                suggestion.label,
                                suggestion.latitude,
                                suggestion.longitude,
                            )
                            onDestSelected(
                                suggestion.label,
                                suggestion.latitude,
                                suggestion.longitude,
                            )
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
                                title = "Saved places",
                                places = favorites,
                                showFavorite = true,
                                onPick = { place ->
                                    destQuery = place.displayName
                                    activeField = 0
                                    recentDestinations = TripRecentPlacesRepository.rememberDestination(
                                        context,
                                        place.label,
                                        place.latitude,
                                        place.longitude,
                                    )
                                    onDestSelected(place.displayName, place.latitude, place.longitude)
                                },
                                onToggleFavorite = { place -> requestSavePlace(place) },
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
                                    destQuery = place.displayName
                                    activeField = 0
                                    recentDestinations = TripRecentPlacesRepository.rememberDestination(
                                        context,
                                        place.label,
                                        place.latitude,
                                        place.longitude,
                                    )
                                    onDestSelected(place.displayName, place.latitude, place.longitude)
                                },
                                onToggleFavorite = { place -> requestSavePlace(place) },
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
                }

                if (showOriginFields) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CitizenRadius.Capsule,
                        color = Color(0xFFF3F4F6),
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    if (stops.size >= MAX_TRIP_STOPS) return@IconButton
                                    addingStop = true
                                    newStopQuery = ""
                                    stopResults = emptyList()
                                    stopSearchAttempted = false
                                    refreshStopSuggestionPools()
                                    activeField = newStopFieldId
                                },
                                enabled = stops.size < MAX_TRIP_STOPS && !addingStop,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Add a stop",
                                    tint = if (stops.size < MAX_TRIP_STOPS && !addingStop) {
                                        DarkBlue
                                    } else {
                                        Color(0xFF9CA3AF)
                                    },
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier
                                    .width(18.dp)
                                    .padding(vertical = 2.dp),
                                color = Color(0xFFD1D5DB),
                            )
                            IconButton(
                                onClick = {
                                    // TextFields bind to local queries (not startText/destText).
                                    // Mirror the swap into those queries so names update immediately.
                                    val nextStartQuery = destText
                                    val nextDestQuery = startText
                                    startQuery = nextStartQuery
                                    destQuery = nextDestQuery
                                    startResults = emptyList()
                                    destResults = emptyList()
                                    startSearchAttempted = false
                                    destSearchAttempted = false
                                    activeField = 0
                                    onSwapStartDestination()
                                },
                                enabled = selectedDestLat != null && selectedDestLon != null,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Filled.SwapVert,
                                    contentDescription = "Swap start and destination",
                                    tint = if (selectedDestLat != null) DarkBlue else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TripVehicle.entries.forEach { option ->
                    val selected = vehicle == option
                    FilterChip(
                        selected = selected,
                        onClick = { onVehicleChange(option) },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                option.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
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
                        shape = CitizenRadius.Capsule,
                        border = BorderStroke(1.dp, if (selected) DarkBlue else Color(0xFF9CA3AF)),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = DarkBlue,
                            iconColor = DarkBlue,
                            selectedContainerColor = DarkBlue,
                            selectedLabelColor = CitizenSelection.ActiveContent,
                            selectedLeadingIconColor = CitizenSelection.ActiveContent,
                        ),
                    )
                }
            }
            val eta = routeDurationSeconds?.takeIf { it.isFinite() && it > 0.0 }
            val dist = routeDistanceMeters?.takeIf { it.isFinite() && it > 0.0 }
            val journeyEta = remember(eta, selectedCity) {
                eta?.let { JourneyStartEta.estimate(it, cityKey = selectedCity) }
            }
            if (eta != null || dist != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    buildString {
                        if (eta != null) append(formatTripEta(eta))
                        if (eta != null && dist != null) append("  ·  ")
                        if (dist != null) append(formatTripDistance(dist))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkBlue,
                    textAlign = TextAlign.Center,
                )
                // #31 Phase A — journey-start ETA (no live traffic overlay).
                if (journeyEta != null) {
                    Text(
                        JourneyStartEta.formatComparison(journeyEta),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (journeyEta.congested) Color(0xFFB45309) else CitizenInk.Helper,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            when (routeStatus) {
                TripRouteStatus.FINDING -> {
                    Text(
                        "Looking for routes. Wait a moment…",
                        fontSize = 11.sp,
                        color = CitizenInk.Helper,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                TripRouteStatus.NO_ROUTE -> {
                    Text(
                        "No route between these points. Change start or destination, then try again.",
                        fontSize = 11.sp,
                        color = Color(0xFFB91C1C),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                else -> Unit
            }
            if (routeOptionLabels.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Alternate routes",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CitizenInk.Helper,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    routeOptionLabels.forEachIndexed { index, label ->
                        val selected = index == selectedRouteIndex
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectRouteIndex(index) },
                            label = {
                                Text(
                                    label,
                                    fontSize = uiScale.sp(11f),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            shape = CitizenRadius.Capsule,
                            border = BorderStroke(
                                1.dp,
                                if (selected) Color(0xFFEA580C) else Color(0xFFD1D5DB),
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = DarkBlue,
                                selectedContainerColor = Color(0xFFFFEDD5),
                                selectedLabelColor = Color(0xFF9A3412),
                            ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onDirections,
                    enabled = canShowDirections,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CitizenRadius.Capsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB91C1C),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFB91C1C).copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.55f),
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    Text(
                        "Direction",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = onNavigate,
                    enabled = canNavigate,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CitizenRadius.Capsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue,
                        contentColor = Color.White,
                        disabledContainerColor = DarkBlue.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.55f),
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    Text(
                        if (navigatePreparing) "GPS…" else "Navigation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    val pending = placePendingSave
    if (pending != null) {
        TripSavePlaceDialog(
            place = pending,
            onDismiss = { placePendingSave = null },
            onSave = { name ->
                recentDestinations = TripRecentPlacesRepository.saveNamedDestination(
                    context,
                    pending.label,
                    pending.latitude,
                    pending.longitude,
                    name,
                )
                placePendingSave = null
            },
            onRemove = {
                recentDestinations = TripRecentPlacesRepository.toggleFavoriteDestination(
                    context,
                    pending.label,
                    pending.latitude,
                    pending.longitude,
                )
                placePendingSave = null
            },
        )
    }
}

@Composable
private fun TripSavePlaceDialog(
    place: TripRecentPlace,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var nameText by remember(place.latitude, place.longitude, place.savedName) {
        mutableStateOf(place.savedName.orEmpty().ifBlank { "Home" })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (place.isFavorite) "Rename saved place" else "Save place as",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm)) {
                Text(
                    place.label,
                    fontSize = 12.sp,
                    color = CitizenInk.Helper,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs),
                ) {
                    TripRecentPlacesRepository.SAVE_NAME_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = nameText.equals(preset, ignoreCase = true),
                            onClick = { nameText = preset },
                            label = {
                                Text(preset, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            },
                            shape = CitizenRadius.Capsule,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkBlue,
                                selectedLabelColor = CitizenSelection.ActiveContent,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it.take(24) },
                    singleLine = true,
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Home") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(nameText.trim()) },
                enabled = nameText.trim().isNotEmpty(),
            ) {
                Text(if (place.isFavorite) "Update" else "Save")
            }
        },
        dismissButton = {
            Row {
                if (place.isFavorite) {
                    TextButton(onClick = onRemove) {
                        Text("Remove", color = Color(0xFFB91C1C))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
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
            tint = CitizenInk.Helper,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
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
                        text = place.displayName,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = DarkBlue,
                        fontWeight = FontWeight.Medium,
                    )
                    val subtitle = when {
                        !place.savedName.isNullOrBlank() &&
                            !place.savedName.equals(place.label, ignoreCase = true) -> place.label
                        else -> formatTripPlaceAge(place.usedAtMs)
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            fontSize = 10.sp,
                            color = CitizenInk.Helper,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                                "Edit or remove saved place"
                            } else {
                                "Save place as…"
                            },
                            tint = if (place.isFavorite) Color(0xFFCA8A04) else CitizenInk.Helper,
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
    results: List<TripPlaceSuggestion>,
    onPick: (TripPlaceSuggestion) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .background(Color(0xFFF8F9FA), RoundedCornerShape(6.dp)),
    ) {
        results.take(14).forEach { suggestion ->
            val title = suggestion.title.ifBlank { suggestion.label }
            val subtitle = buildList {
                suggestion.kindLabel?.takeIf { it.isNotBlank() }?.let { add(it) }
                suggestion.subtitle
                    .takeIf { it.isNotBlank() && it != suggestion.kindLabel }
                    ?.let { add(it) }
            }.distinct().joinToString(" · ")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(suggestion) }
                    .padding(horizontal = CitizenSpace.Sm, vertical = CitizenSpace.Sm),
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
                        color = CitizenInk.Helper,
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
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var stripSortName by rememberSaveable { mutableStateOf(ReportsStripSort.RECENT.name) }
    val stripSort = ReportsStripSort.entries.find { it.name == stripSortName }
        ?: ReportsStripSort.RECENT
    val bbox = metroBboxForCity(selectedCity)
    LaunchedEffect(selectedCity, recentReportsEpoch) {
        val changed = withContext(Dispatchers.IO) {
            RecentReportsRepository.syncPublicCityReportsFromSupabase(selectedCity)
        }
        if (changed) onReportsMutated()
    }
    val reports = remember(selectedCity, recentReportsEpoch, stripSort) {
        RecentReportsRepository.recentForCityInMetro(selectedCity, bbox, limit = 5, sort = stripSort)
    }
    detailReport?.let { report ->
        ReportDetailDialog(report = report, onDismiss = { detailReport = null })
    }
    Spacer(modifier = Modifier.height(CitizenSpace.Xl))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CitizenHeader.Horizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "REPORTS",
                style = CitizenType.Section,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                " - ",
                style = CitizenType.Section,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                formatCityDisplayName(selectedCity),
                style = CitizenType.SectionTrailing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Spacer(modifier = Modifier.width(CitizenSpace.Sm))
        Box {
            AssistChip(
                onClick = { sortMenuExpanded = true },
                modifier = Modifier.height(CitizenIcon.CapsuleCompact),
                label = {
                    Text(
                        "Sort: ${stripSort.chipLabel}",
                        style = CitizenType.ChipCompact,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(CitizenIcon.Chip),
                    )
                },
                shape = CitizenRadius.Capsule,
                border = BorderStroke(CitizenBorder.Thin, CitizenSelection.InactiveBorder),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    trailingIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
            ) {
                ReportsStripSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (option.enabled) {
                                    option.menuLabel
                                } else {
                                    "${option.menuLabel} · Coming soon"
                                },
                                color = if (option.enabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        },
                        enabled = option.enabled,
                        onClick = {
                            stripSortName = option.name
                            sortMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(CitizenSpace.Md))
    if (reports.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CitizenSpace.Lg),
            shape = CitizenRadius.Card,
            border = BorderStroke(CitizenBorder.Thin, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ),
        ) {
            Row(
                Modifier.padding(CitizenSpace.Md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = CitizenInk.Disabled,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(CitizenSpace.Md))
                Text(
                    when (stripSort) {
                        ReportsStripSort.LONGEST_PENDING ->
                            "No pending reports in this city."
                        else ->
                            "No reports in this city yet. Submit one with the button above."
                    },
                    style = CitizenType.Tagline,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Md),
            contentPadding = PaddingValues(horizontal = CitizenSpace.Lg),
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
        shape = CitizenRadius.Card,
        border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary),
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
            val statusColor = citizenReportStatusColor(report.status)
            Text(
                report.status.displayLabelCaps,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(CitizenSpace.Xs)
                    .background(Color.White, CitizenRadius.Capsule)
                    .border(CitizenBorder.Thin, statusColor, CitizenRadius.Capsule)
                    .padding(horizontal = CitizenSpace.Xs, vertical = 2.dp),
                style = CitizenType.ChipCompact,
                color = statusColor,
                maxLines = 1,
            )
            Text(
                caption,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = CitizenSpace.Xs, vertical = CitizenSpace.Xs),
                style = CitizenType.Caption,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
            val label = TripPlaceLabels.formatCitySearchLabel(address)
            if (label.length < 2 || TripPlaceLabels.isAdministrativeUnitName(label)) continue
            val lat = address.latitude
            val lon = address.longitude
            if (!address.hasLatitude() || !address.hasLongitude()) continue
            // Keep display casing; city key is canonicalized on select.
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

/**
 * Silent OSRM result computed while the user is still picking Destination /
 * waiting to tap Direction or Navigation.
 */
private data class TripRoutePrefetchCache(
    val originLat: Double,
    val originLon: Double,
    val destLat: Double,
    val destLon: Double,
    val vehicle: TripVehicle,
    val viaPoints: List<Pair<Double, Double>>,
    val routes: List<OsrmRouteResult>,
    val hints: List<String?>,
)

/** Allow small GPS drift between prefetch and Direction/Navigate. */
private const val TRIP_PREFETCH_ORIGIN_MATCH_M = 80.0
private const val TRIP_PREFETCH_DEST_MATCH_M = 25.0

private fun tripRoutePrefetchMatches(
    cache: TripRoutePrefetchCache,
    originLat: Double,
    originLon: Double,
    destLat: Double,
    destLon: Double,
    vehicle: TripVehicle,
    viaPoints: List<Pair<Double, Double>>,
): Boolean {
    if (cache.vehicle != vehicle) return false
    if (cache.viaPoints.size != viaPoints.size) return false
    for (i in viaPoints.indices) {
        val a = cache.viaPoints[i]
        val b = viaPoints[i]
        if (TripNavigationMatcher.haversineM(a.first, a.second, b.first, b.second) > 20.0) {
            return false
        }
    }
    if (
        TripNavigationMatcher.haversineM(
            cache.destLat, cache.destLon, destLat, destLon,
        ) > TRIP_PREFETCH_DEST_MATCH_M
    ) {
        return false
    }
    if (
        TripNavigationMatcher.haversineM(
            cache.originLat, cache.originLon, originLat, originLon,
        ) > TRIP_PREFETCH_ORIGIN_MATCH_M
    ) {
        return false
    }
    return cache.routes.isNotEmpty()
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

/** Keep the user's chosen alternate when OSRM returns a refreshed similar set. */
private fun osrmRoutesLookAlike(a: OsrmRouteResult, b: OsrmRouteResult): Boolean {
    if (kotlin.math.abs(a.distanceMeters - b.distanceMeters) > 150.0) return false
    if (kotlin.math.abs(a.durationSeconds - b.durationSeconds) > 90.0) return false
    val am = a.points.getOrNull(a.points.size / 2) ?: return false
    val bm = b.points.getOrNull(b.points.size / 2) ?: return false
    return kotlin.math.abs(am.first - bm.first) < 0.002 &&
        kotlin.math.abs(am.second - bm.second) < 0.002
}

/** #23.2 — reorder ETA-close corridors by severity-weighted pothole risk. */
private fun rankTripRoutesByPotholeRisk(
    cityKey: String,
    routes: List<OsrmRouteResult>,
): TripRoutePotholeRanking.Result {
    if (routes.size < 2) {
        return TripRoutePotholeRanking.Result(routes, List(routes.size) { null })
    }
    val bbox = metroBboxForCity(cityKey)
    val reports = if (bbox != null) {
        RecentReportsRepository.reportsForMapInMetro(cityKey, bbox)
    } else {
        emptyList()
    }
    return TripRoutePotholeRanking.rank(routes, reports)
}

/**
 * Prefer a refreshed OSRM option that follows the same corridor as [preferredPoints]
 * (e.g. the alternate the user already chose). Falls back to the fastest option.
 */
private fun pickOsrmRouteMatchingPoints(
    routes: List<OsrmRouteResult>,
    preferredPoints: List<Pair<Double, Double>>,
): OsrmRouteResult? {
    if (routes.isEmpty()) return null
    if (preferredPoints.size < 2) return routes.first()
    val preferred = OsrmRouteResult(
        points = preferredPoints,
        durationSeconds = 0.0,
        distanceMeters = 0.0,
    )
    val best = routes.minByOrNull { OsrmRouteClient.maxSampleGapM(it, preferred) }
        ?: return routes.first()
    val gap = OsrmRouteClient.maxSampleGapM(best, preferred)
    // ~180 m mid-sample gap ⇒ same corridor; otherwise take the fastest fresh route.
    return if (gap <= 180.0) best else routes.first()
}

private enum class TripNavTurnKind(
    val label: String,
    val icon: ImageVector,
) {
    LEFT("Left", Icons.Filled.TurnLeft),
    RIGHT("Right", Icons.Filled.TurnRight),
    STRAIGHT("Straight", Icons.Filled.Straight),
    UTURN("U-turn", Icons.Filled.UTurnLeft),
    ARRIVE("Arrive", Icons.Filled.Flag),
}

private fun classifyTripTurnDirection(turn: TripTurnGuidance?): TripNavTurnKind {
    if (turn == null) return TripNavTurnKind.STRAIGHT
    val type = turn.maneuverType.lowercase()
    if (type == "arrive") return TripNavTurnKind.ARRIVE
    val blob = "${turn.modifier} ${turn.instruction} $type".lowercase()
    return when {
        blob.contains("uturn") || blob.contains("u-turn") || blob.contains("u turn") ->
            TripNavTurnKind.UTURN
        blob.contains("left") -> TripNavTurnKind.LEFT
        blob.contains("right") -> TripNavTurnKind.RIGHT
        blob.contains("straight") || blob.contains("continue") || type == "new name" ->
            TripNavTurnKind.STRAIGHT
        else -> TripNavTurnKind.STRAIGHT
    }
}

/** Capsule distances: meters below 1 km, otherwise km (e.g. 1200 → 1.2 km). */
private fun formatNavDistanceMeters(meters: Double): String {
    val m = meters.toInt().coerceAtLeast(0)
    if (m < 1000) return "$m m"
    val km = m / 1000.0
    val whole = km.toInt()
    return if (km == whole.toDouble()) {
        "$whole km"
    } else {
        String.format(Locale.US, "%.1f km", km)
    }
}


@Composable
private fun TripNavCapsuleDivider() {
    Text(
        text = "|",
        style = CitizenType.Label,
        fontWeight = FontWeight.Light,
        color = CitizenInk.Disabled,
    )
}

/**
 * Trip start/destination/stop autocomplete: city metro bias + partial typing
 * + clickable landmarks (petrol, hospital, locality, PG, …) from live OSM / Photon.
 */
private suspend fun searchTripPlaces(
    context: Context,
    query: String,
    selectedCity: String,
    recentPlaces: List<TripRecentPlace> = emptyList(),
    limit: Int = 20,
): List<TripPlaceSuggestion> {
    val bbox = metroBboxForCity(selectedCity)
    val bounds = bbox?.let {
        TripPlaceSearchBounds(
            latNorth = it.latNorth,
            lonEast = it.lonEast,
            latSouth = it.latSouth,
            lonWest = it.lonWest,
        )
    }
    return TripPlaceSearch.search(
        context = context,
        query = query,
        selectedCity = selectedCity,
        cityLabel = formatCityDisplayName(selectedCity),
        bounds = bounds,
        recentPlaces = recentPlaces,
        limit = limit,
    )
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
        (5.75f * density).coerceIn(5.25f, 9f)
    } else {
        (9f + sqrt(count.toFloat()).coerceAtMost(18f)) * density
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
            // Keep digit size proportional to the disk so it stays inside (no oversized floor).
            textPaint.textSize = (rPx * 1.0f).coerceIn(6.25f * d, 16f * d)
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
    /** Trip map uses black-spot alert markers instead of numbered density disks. */
    suppressForTripMap: Boolean = false,
) {
    decor.clusters?.let { map.overlays.remove(it) }
    decor.clusters = null
    if (suppressForTripMap) return
    val bbox = metroBboxForCity(selectedCity) ?: return
    val reports = reportsForMapSeverityFilter(
        RecentReportsRepository.reportsForMapInMetro(selectedCity, bbox) +
            TripCrowdHazardStore.syntheticReportsForMap(
                context = map.context,
                cityKey = selectedCity,
                viewerUserId = "local",
            ).filter { bbox.contains(it.latitude, it.longitude) },
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
    var weatherRain: CityWeatherRainOverlay? = null,
    var cityName: CityNameOverlay? = null,
    var clusters: ReportClusterMarkersOverlay? = null,
    var tripNav: TripNavigationOverlay? = null,
)

private fun removeAreaHeatOverlay(map: MapView, decor: CityOutlineDecor) {
    decor.areaHeat?.let { map.overlays.remove(it) }
    decor.areaHeat = null
}

private fun removeWeatherRainOverlay(map: MapView, decor: CityOutlineDecor) {
    decor.weatherRain?.let { map.overlays.remove(it) }
    decor.weatherRain = null
}

/**
 * Soft rain-risk grid under clusters. Hidden when Heat is on (parent exclusivity).
 */
private fun refreshWeatherRainOverlay(
    map: MapView,
    decor: CityOutlineDecor,
    enabled: Boolean,
    grid: CityWeatherRainGrid?,
) {
    val cells = if (enabled) grid?.cells.orEmpty() else emptyList()
    if (cells.isEmpty()) {
        removeWeatherRainOverlay(map, decor)
        return
    }
    val existing = decor.weatherRain
    if (existing != null) {
        existing.updateCells(cells)
        return
    }
    val overlay = CityWeatherRainOverlay(mapView = map, cells = cells)
    decor.weatherRain = overlay
    val heatIdx = decor.areaHeat?.let { map.overlays.indexOf(it) } ?: -1
    val dimIdx = decor.dim?.let { map.overlays.indexOf(it) } ?: -1
    when {
        heatIdx >= 0 -> map.overlays.add(heatIdx + 1, overlay)
        dimIdx >= 0 -> map.overlays.add(dimIdx + 1, overlay)
        else -> map.overlays.add(0, overlay)
    }
    decor.clusters?.let { clusters ->
        map.overlays.remove(clusters)
        map.overlays.add(clusters)
    }
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
    suppressForTripMap: Boolean = false,
) {
    attachReportClusterOverlay(
        map = map,
        selectedCity = selectedCity,
        decor = decor,
        mapSeverityFilter = mapSeverityFilter,
        heatEnabled = heatEnabled,
        suppressForTripMap = suppressForTripMap,
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
    streetFollow: Boolean = true,
    routeMovingFactor: Float = 0f,
    navigationActive: Boolean = false,
    alternateRouteOptions: List<List<Pair<Double, Double>>> = emptyList(),
    alternateRouteEtas: List<String> = emptyList(),
    selectedRouteIndex: Int = 0,
    previewSelecting: Boolean = false,
    onSelectRouteIndex: ((Int) -> Unit)? = null,
    rejoinPoints: List<Pair<Double, Double>> = emptyList(),
    offeredAlternatePoints: List<Pair<Double, Double>> = emptyList(),
    onConfirmOfferedAlternate: (() -> Unit)? = null,
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
    overlay.onAlternateRouteClick = onSelectRouteIndex
    overlay.onOfferedAlternateClick = onConfirmOfferedAlternate
    overlay.showSidePlacement = true
    if (previewSelecting && alternateRouteOptions.isNotEmpty()) {
        overlay.completedRoutePoints = emptyList()
        overlay.remainingRoutePoints = routePoints.map { (lat, lon) -> GeoPoint(lat, lon) }
        overlay.routeLatLons = routePoints
        overlay.alerts = emptyList()
        overlay.applyRouteStrokeWidths(
            movingFactor = 0f,
            mapWidthPx = map.width,
            navigationActive = false,
            mapZoom = map.zoomLevelDouble,
        )
        overlay.setPreviewAlternates(
            map,
            alternateRouteOptions.map { pts -> pts.map { (lat, lon) -> GeoPoint(lat, lon) } },
            selectedRouteIndex,
            etas = alternateRouteEtas,
        )
        overlay.clearRejoinPath(map)
        overlay.clearOfferedAlternate(map)
    } else {
        overlay.setPreviewAlternates(map, emptyList(), 0)
        val (_, remaining) = TripNavigationMatcher.splitRouteByTraveled(
            routePoints,
            traveledMeters,
        )
        overlay.routeLatLons = routePoints
        // Hide covered distance — only remaining ahead is drawn on the map.
        overlay.completedRoutePoints = emptyList()
        overlay.remainingRoutePoints = remaining.map { (lat, lon) -> GeoPoint(lat, lon) }
        overlay.alerts = TripNavigationMatcher.alertsForMapDisplay(alerts, streetFollow)
        // Thick ribbon only after Navigate; preview stays thin for clear curves.
        overlay.applyRouteStrokeWidths(
            movingFactor = if (navigationActive && streetFollow) routeMovingFactor else 0f,
            mapWidthPx = map.width,
            navigationActive = navigationActive,
            mapZoom = map.zoomLevelDouble,
        )
        if (rejoinPoints.size >= 2) {
            overlay.setRejoinPath(
                map,
                rejoinPoints.map { (lat, lon) -> GeoPoint(lat, lon) },
            )
        } else {
            overlay.clearRejoinPath(map)
        }
        if (offeredAlternatePoints.size >= 2) {
            overlay.setOfferedAlternate(
                map,
                offeredAlternatePoints.map { (lat, lon) -> GeoPoint(lat, lon) },
            )
        } else {
            overlay.clearOfferedAlternate(map)
        }
    }
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
        destinationBalloon: Boolean = false,
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
                if (destinationBalloon) {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setIcon(buildGoogleDestBalloonDrawable(map.context))
                } else {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setIcon(buildTripDotDrawable(map.context, colorHex))
                }
            }
            slot[0] = marker
            map.overlays.add(marker)
        } else {
            existing.position = point
            if (destinationBalloon) {
                existing.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                existing.setIcon(buildGoogleDestBalloonDrawable(map.context))
            } else if (existing.icon == null) {
                existing.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                existing.setIcon(buildTripDotDrawable(map.context, colorHex))
            }
        }
    }
    syncDot(startSlot, startLat, startLon, "Start", "#22C55E")
    syncDot(
        destSlot,
        destLat,
        destLon,
        "Destination",
        "#EF4444",
        destinationBalloon = true,
    )
}

/** Intermediate stop markers (numbered orange discs). */
private fun syncTripStopMarkers(
    map: MapView,
    slots: MutableList<Marker>,
    show: Boolean,
    stops: List<Pair<Double, Double>>,
) {
    val usable = if (show) {
        stops.filter { it.first.isFinite() && it.second.isFinite() }
    } else {
        emptyList()
    }
    while (slots.size > usable.size) {
        val removed = slots.removeAt(slots.lastIndex)
        map.overlays.remove(removed)
    }
    usable.forEachIndexed { index, (lat, lon) ->
        val point = GeoPoint(lat, lon)
        val title = "Stop ${index + 1}"
        if (index < slots.size) {
            val existing = slots[index]
            existing.position = point
            existing.title = title
            existing.setIcon(TripMapMarkerIcons.stopDotDrawable(map.context, index + 1))
        } else {
            val marker = Marker(map).apply {
                position = point
                this.title = title
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setIcon(TripMapMarkerIcons.stopDotDrawable(map.context, index + 1))
            }
            slots += marker
            map.overlays.add(marker)
        }
    }
}

/** Arrival neighborhood POI labels (non-overlapping Overpass set). */
private fun syncArrivalPlaceMarkers(
    map: MapView,
    slots: MutableList<Marker>,
    places: List<com.example.potholereport.data.remote.NearbyOsmPoi>,
) {
    val usable = places.filter {
        it.latitude.isFinite() && it.longitude.isFinite() && it.name.isNotBlank()
    }
    while (slots.size > usable.size) {
        val removed = slots.removeAt(slots.lastIndex)
        map.overlays.remove(removed)
    }
    usable.forEachIndexed { index, poi ->
        val point = GeoPoint(poi.latitude, poi.longitude)
        val label = poi.name.take(28)
        if (index < slots.size) {
            val existing = slots[index]
            existing.position = point
            existing.title = label
            existing.setIcon(TripMapMarkerIcons.arrivalPlaceDrawable(map.context, label))
            existing.setAnchor(Marker.ANCHOR_LEFT, Marker.ANCHOR_CENTER)
        } else {
            val marker = Marker(map).apply {
                position = point
                title = label
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_LEFT, Marker.ANCHOR_CENTER)
                setIcon(TripMapMarkerIcons.arrivalPlaceDrawable(map.context, label))
            }
            slots += marker
            map.overlays.add(marker)
        }
    }
}

/** #25.2 street / place name labels at high zoom. */
private fun syncStreetNameMarkers(
    map: MapView,
    slots: MutableList<Marker>,
    labels: List<com.example.potholereport.data.remote.StreetNameOverlayClient.StreetLabel>,
) {
    val usable = labels.filter {
        it.latitude.isFinite() && it.longitude.isFinite() && it.name.isNotBlank()
    }
    while (slots.size > usable.size) {
        val removed = slots.removeAt(slots.lastIndex)
        map.overlays.remove(removed)
    }
    usable.forEachIndexed { index, label ->
        val point = GeoPoint(label.latitude, label.longitude)
        val title = label.name.take(28)
        if (index < slots.size) {
            val existing = slots[index]
            existing.position = point
            existing.title = title
            existing.setIcon(TripMapMarkerIcons.arrivalPlaceDrawable(map.context, title))
            existing.setAnchor(Marker.ANCHOR_LEFT, Marker.ANCHOR_CENTER)
        } else {
            val marker = Marker(map).apply {
                position = point
                this.title = title
                setInfoWindow(null)
                setAnchor(Marker.ANCHOR_LEFT, Marker.ANCHOR_CENTER)
                setIcon(TripMapMarkerIcons.arrivalPlaceDrawable(map.context, title))
            }
            slots += marker
            map.overlays.add(marker)
        }
    }
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
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
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

private fun MapView.applyMetroPanBounds(selectedCity: String, freePan: Boolean = false) {
    if (freePan) {
        resetScrollableAreaLimitLatitude()
        resetScrollableAreaLimitLongitude()
        setScrollableAreaLimitDouble(null)
        maxZoomLevel = TRIP_MAP_MAX_ZOOM
        return
    }
    maxZoomLevel = CartoBasemapUsefulMaxZoom
    // No hard scrollable limit for home pan: allow brief overshoot past the city
    // border, then release animates the viewport back to the border edge.
    resetScrollableAreaLimitLatitude()
    resetScrollableAreaLimitLongitude()
    setScrollableAreaLimitDouble(null)
}

/** Bounding box used to keep the home Report & Track map inside the city. */
private fun homePanBoundingBox(selectedCity: String): BoundingBox? =
    cityOverviewBoundingBox(selectedCity) ?: metroBboxForCity(selectedCity)

/** Visible rubber-band duration when snapping back to the city border after release. */
private const val CITY_BORDER_SNAP_BACK_MS = 320L

/**
 * Home map (card + fullscreen): one-/multi-finger pan is always allowed while not in
 * trip free-pan. Drag may overshoot the city box; on release the map eases back so the
 * viewport sits flush against the border (never settles outside).
 */
private fun MapView.applyHomePanPolicy(
    selectedCity: String,
    freePan: Boolean,
    animateClamp: Boolean = false,
) {
    applyMetroPanBounds(selectedCity, freePan = freePan)
    if (!freePan) {
        clampViewportToMetroBounds(selectedCity, freePan = false, animate = animateClamp)
    }
}

/**
 * Center that keeps the full viewport inside [homePanBoundingBox], or null if already inside.
 */
private fun MapView.cityBorderClampCenter(selectedCity: String): GeoPoint? {
    val metro = homePanBoundingBox(selectedCity) ?: return null
    val visible = boundingBox ?: return null
    val center = mapCenter

    val visLatSpan = visible.latNorth - visible.latSouth
    val visLonSpan = visible.lonEast - visible.lonWest
    val metroLatSpan = metro.latNorth - metro.latSouth
    val metroLonSpan = metro.lonEast - metro.lonWest

    var newLat = center.latitude
    var newLon = center.longitude

    if (visLatSpan >= metroLatSpan - 1e-9) {
        newLat = (metro.latNorth + metro.latSouth) / 2.0
    } else {
        val halfLat = visLatSpan / 2.0
        newLat = center.latitude.coerceIn(metro.latSouth + halfLat, metro.latNorth - halfLat)
    }

    if (visLonSpan >= metroLonSpan - 1e-9) {
        newLon = (metro.lonEast + metro.lonWest) / 2.0
    } else {
        val halfLon = visLonSpan / 2.0
        newLon = center.longitude.coerceIn(metro.lonWest + halfLon, metro.lonEast - halfLon)
    }

    if (abs(newLat - center.latitude) <= 1e-7 && abs(newLon - center.longitude) <= 1e-7) {
        return null
    }
    return GeoPoint(newLat, newLon)
}

/**
 * Stop any fling and ease the viewport back flush to the city border.
 * No-op when already inside so normal in-city flings stay smooth.
 */
private fun MapView.snapBackToCityBorder(selectedCity: String, freePan: Boolean) {
    if (freePan) return
    val point = cityBorderClampCenter(selectedCity) ?: return
    controller.stopAnimation(/* jumpToTarget = */ false)
    controller.animateTo(point, zoomLevelDouble, CITY_BORDER_SNAP_BACK_MS)
}

/**
 * Keeps the entire visible viewport inside the city pan box (not only the center).
 * At city overview (viewport ≥ city) the center is locked — map stays framed.
 */
private fun MapView.clampViewportToMetroBounds(
    selectedCity: String,
    freePan: Boolean = false,
    animate: Boolean = false,
) {
    if (freePan) return
    val point = cityBorderClampCenter(selectedCity) ?: return
    if (animate) {
        controller.animateTo(point, zoomLevelDouble, CITY_BORDER_SNAP_BACK_MS)
    } else {
        controller.setCenter(point)
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
            applyHomePanPolicy(selectedCity, freePan = false)
            invalidate()
            restoreSlot[0] = null
        }
        restoreSlot[0] = restore
        postDelayed(restore, 120)
        applyHomePanPolicy(selectedCity, freePan = false)
    }
}

/** Zoom no more than this above [MapView.minZoomLevel] counts as city overview (bbox fit level). */
private const val CITY_VIEW_ZOOM_EPSILON = 0.22

/** Street-level (locate). */
private const val STREET_VIEW_ZOOM = 14.0

/** Live navigation: street-level zoom when Navigate starts / still stopped. */
private const val TRIP_FOLLOW_ZOOM = TripStreetCamera.ZOOM_SLOW

/** Wider north-up zoom when street follow is off during navigation. */
private const val TRIP_NAV_OVERVIEW_ZOOM = 15.5

/** Closest allowed zoom on the fullscreen trip map (used while moving). */
private const val TRIP_MAP_MAX_ZOOM = TripStreetCamera.ZOOM_FAST

/** OSM follow zoom for the active camera mode. */
private fun tripFollowZoomForMode(
    mode: TripCameraMode,
    speedMps: Float?,
    playIntensity: Float = 0f,
    playMoving: Boolean = playIntensity > 0.001f,
): Double = when (mode) {
    TripCameraMode.STREET -> TripStreetCamera.zoomForSpeedMps(speedMps)
    TripCameraMode.PLAY -> TripStreetCamera.playZoomForIntensity(playIntensity, playMoving)
    TripCameraMode.MAP -> TRIP_NAV_OVERVIEW_ZOOM
}


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

/** Smoothly follows the latest GPS fix; street-follow uses bottom-biased center + speed zoom. */
private fun MapView.scheduleFollowTripLocation(
    selectedCity: String,
    location: Location,
    headingUp: Boolean = true,
    zoom: Double = TRIP_FOLLOW_ZOOM,
    lookAheadMeters: Double = 0.0,
    smoothZoom: Boolean = false,
    attempt: Int = 0,
) {
    val maxAttempts = 15
    post {
        if (width <= 0 || height <= 0) {
            if (attempt < maxAttempts) {
                postDelayed({
                    scheduleFollowTripLocation(
                        selectedCity,
                        location,
                        headingUp,
                        zoom,
                        lookAheadMeters,
                        smoothZoom,
                        attempt + 1,
                    )
                }, 40L)
            }
            return@post
        }
        applyStreetFollowVehicleBottomOffset(enabled = headingUp)
        val targetZoom = zoom
        val nextZoom = if (smoothZoom) {
            zoomLevelDouble + (targetZoom - zoomLevelDouble) * TripStreetCamera.ZOOM_SMOOTH
        } else {
            targetZoom
        }
        if (abs(zoomLevelDouble - nextZoom) > 0.02) {
            controller.setZoom(nextZoom)
        }
        val speedMps = location.speed.takeIf { location.hasSpeed() && it.isFinite() }
        if (headingUp && location.hasBearing()) {
            applyTripHeadingUp(location.bearing, speedMps)
        } else if (!headingUp) {
            resetMapOrientationNorthUp()
        }
        val center = if (
            headingUp &&
            lookAheadMeters > 0.0 &&
            location.hasBearing() &&
            location.bearing.isFinite()
        ) {
            val (alat, alon) = TripNavigationMatcher.destinationPoint(
                location.latitude,
                location.longitude,
                location.bearing,
                lookAheadMeters,
            )
            GeoPoint(alat, alon)
        } else {
            GeoPoint(location.latitude, location.longitude)
        }
        controller.animateTo(center)
        clampViewportToMetroBounds(selectedCity)
        invalidate()
    }
}

/**
 * Animate a visible zoom-in into street follow (flat 2D top-down).
 * Uses osmdroid's built-in ValueAnimator so zoom + pan render smoothly in one pass
 * (avoids restarting setZoom/animateTo every frame, which looked stuttery).
 */
private suspend fun MapView.animateStreetViewZoomIn(
    selectedCity: String,
    location: Location,
    targetZoom: Double,
    headingUp: Boolean,
    durationMs: Long = TripStreetCamera.INTRO_ZOOM_MS,
) {
    var tries = 0
    while ((width <= 0 || height <= 0) && tries < 40) {
        delay(16)
        tries++
    }
    val endZoom = targetZoom.coerceIn(minZoomLevel, maxZoomLevel)
    applyStreetFollowVehicleBottomOffset(enabled = headingUp)
    val speedMpsOrNull = location.speed.takeIf { location.hasSpeed() && it.isFinite() }
    val lookAhead = TripStreetCamera.lookAheadMetersForSpeed(speedMpsOrNull)
    val center = if (
        headingUp &&
        lookAhead > 0.0 &&
        location.hasBearing() &&
        location.bearing.isFinite()
    ) {
        val (alat, alon) = TripNavigationMatcher.destinationPoint(
            location.latitude,
            location.longitude,
            location.bearing,
            lookAhead,
        )
        GeoPoint(alat, alon)
    } else {
        GeoPoint(location.latitude, location.longitude)
    }
    val speedMps = speedMpsOrNull ?: 0f
    val orientation = if (
        headingUp &&
        location.hasBearing() &&
        location.bearing.isFinite() &&
        speedMps >= TripStreetCamera.HEADING_LOCK_MIN_SPEED_MPS
    ) {
        ((-location.bearing) % 360f + 360f) % 360f
    } else if (headingUp) {
        // Keep current orientation when stopped — don't snap from noisy compass.
        mapOrientation
    } else {
        null
    }
    // Stop any in-flight pan/zoom so the intro owns the camera.
    controller.stopAnimation(/* jumpToTarget = */ false)
    if (abs(zoomLevelDouble - endZoom) <= 0.08 &&
        abs(mapCenter.latitude - center.latitude) < 1e-5 &&
        abs(mapCenter.longitude - center.longitude) < 1e-5
    ) {
        controller.setCenter(center)
        controller.setZoom(endZoom)
        if (orientation != null) {
            mapOrientation = orientation
        } else if (!headingUp) {
            resetMapOrientationNorthUp()
        }
        clampViewportToMetroBounds(selectedCity)
        invalidate()
        return
    }
    // One hardware-timed animation: center + zoom (+ heading) together.
    controller.animateTo(center, endZoom, durationMs, orientation)
    delay(durationMs + 60L)
    // Settle exactly on the end state (animation may stop a hair early).
    controller.stopAnimation(/* jumpToTarget = */ true)
    controller.setCenter(center)
    controller.setZoom(endZoom)
    if (orientation != null) {
        mapOrientation = orientation
    }
    clampViewportToMetroBounds(selectedCity)
    invalidate()
}

/** North-up overview that fits the whole preview route (and alternates) in the map card. */
private fun MapView.scheduleFitRoutePreview(
    routePoints: List<Pair<Double, Double>>,
    selectedCity: String,
    attempt: Int = 0,
    extraRoutePoints: List<List<Pair<Double, Double>>> = emptyList(),
) {
    if (routePoints.size < 2 && extraRoutePoints.none { it.size >= 2 }) return
    val maxAttempts = 15
    post {
        if (width <= 0 || height <= 0) {
            if (attempt < maxAttempts) {
                postDelayed({
                    scheduleFitRoutePreview(
                        routePoints,
                        selectedCity,
                        attempt + 1,
                        extraRoutePoints,
                    )
                }, 40L)
            }
            return@post
        }
        resetMapOrientationNorthUp()
        var north = Double.NEGATIVE_INFINITY
        var south = Double.POSITIVE_INFINITY
        var east = Double.NEGATIVE_INFINITY
        var west = Double.POSITIVE_INFINITY
        fun include(lat: Double, lon: Double) {
            if (!lat.isFinite() || !lon.isFinite()) return
            north = maxOf(north, lat)
            south = minOf(south, lat)
            east = maxOf(east, lon)
            west = minOf(west, lon)
        }
        for ((lat, lon) in routePoints) include(lat, lon)
        for (alt in extraRoutePoints) {
            for ((lat, lon) in alt) include(lat, lon)
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
private fun MapView.applyTripHeadingUp(bearingDeg: Float, speedMps: Float? = null) {
    if (!bearingDeg.isFinite()) return
    val speed = speedMps?.takeIf { it.isFinite() } ?: 0f
    // Freeze orientation when nearly stopped — stops upside-down compass/GPS flips.
    if (speed < TripStreetCamera.HEADING_LOCK_MIN_SPEED_MPS) return

    val target = ((-bearingDeg) % 360f + 360f) % 360f
    val current = ((mapOrientation % 360f) + 360f) % 360f
    var delta = target - current
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f

    // Reject single-sample inverted snaps (common GPS glitch).
    if (abs(delta) >= TripStreetCamera.HEADING_FLIP_REJECT_DEG) return
    if (abs(delta) < TripStreetCamera.HEADING_UPDATE_MIN_DEG) return

    // Ease large turns instead of jumping (real curves still catch up within a few ticks).
    val step = delta.coerceIn(
        -TripStreetCamera.HEADING_MAX_STEP_DEG,
        TripStreetCamera.HEADING_MAX_STEP_DEG,
    )
    mapOrientation = ((current + step) % 360f + 360f) % 360f
}

/** Pin the vehicle near the bottom of the screen in street follow; clear in overview. */
private fun MapView.applyStreetFollowVehicleBottomOffset(enabled: Boolean) {
    if (!enabled) {
        if (mapCenterOffsetX != 0 || mapCenterOffsetY != 0) {
            setMapCenterOffset(0, 0)
        }
        return
    }
    if (height <= 0) return
    // Slightly less than max so the cursor clears the bottom Report / Video FABs.
    val y = (height * TripStreetCamera.VEHICLE_BOTTOM_OFFSET_FRAC_WITH_NAV_CHROME).roundToInt()
        .coerceAtLeast(0)
    if (mapCenterOffsetX != 0 || mapCenterOffsetY != y) {
        setMapCenterOffset(0, y)
    }
}

private fun MapView.resetMapOrientationNorthUp() {
    applyStreetFollowVehicleBottomOffset(enabled = false)
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
 * Tile max is [CartoBasemapNativeMaxZoom] so trip street-zoom can fetch native z19–20
 * instead of stretching z18 (which makes road edges look blocky).
 */
private val CartoVoyagerLabeled: XYTileSource = XYTileSource(
    "CartoVoyagerLabeled20",
    0,
    CartoBasemapNativeMaxZoom.toInt(),
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

/**
 * Destination marker: glossy red pin asset ([R.drawable.ic_trip_dest_pin]).
 * Tip is at the bottom — use [Marker.ANCHOR_BOTTOM].
 */
private fun buildGoogleDestBalloonDrawable(context: Context): BitmapDrawable =
    TripMapMarkerIcons.destPinDrawable(context)


/**
 * Pre-navigation “you are here”: blue disc + soft heading sector (approximate facing).
 * Sector points toward the top of the bitmap — rotate the marker with device heading.
 */
private fun buildPreNavMyLocationDrawable(context: Context): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val size = (84f * d).toInt().coerceIn(72, 128)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f
    val sectorR = size * 0.48f
    val halfAngleDeg = 68f
    val sector = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#4285F4")
        style = Paint.Style.FILL
        alpha = 88
    }
    // Fan upward (270° in Android canvas = up), ±halfAngle.
    canvas.drawArc(
        cx - sectorR,
        cy - sectorR,
        cx + sectorR,
        cy + sectorR,
        270f - halfAngleDeg,
        halfAngleDeg * 2f,
        true,
        sector,
    )
    // Soft outer edge ring so the sector reads clearly over map pins.
    val sectorEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#4285F4")
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * d
        alpha = 110
    }
    canvas.drawArc(
        cx - sectorR,
        cy - sectorR,
        cx + sectorR,
        cy + sectorR,
        270f - halfAngleDeg,
        halfAngleDeg * 2f,
        false,
        sectorEdge,
    )
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#4285F4")
        style = Paint.Style.FILL
    }
    val coreR = 7.5f * d
    canvas.drawCircle(cx, cy, coreR + 2.4f * d, ring)
    canvas.drawCircle(cx, cy, coreR, fill)
    return BitmapDrawable(context.resources, bmp).apply { setBounds(0, 0, size, size) }
}

/** Keep the live location marker above route / pothole pins when they overlap. */
private fun bringMarkerToFront(map: MapView, marker: Marker?) {
    if (marker == null) return
    val overlays = map.overlays
    if (overlays.lastOrNull() === marker) return
    overlays.remove(marker)
    overlays.add(marker)
}

/** Solid colored map dot for trip start (destination uses [buildGoogleDestBalloonDrawable]). */
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
 * Moving-trip pointer: Settings → Map cursor (Arrow / rear Car / rear Bike).
 * Used with [Marker.setFlat] + GPS bearing so the icon tracks travel direction.
 */
private fun buildTripNavPointerDrawable(
    context: Context,
    vehicle: TripVehicle,
    style: MapCursorStyle,
    color: MapCursorColor,
): BitmapDrawable {
    // [vehicle] kept for call-site compatibility; style now selects Arrow/Car/Bike art.
    @Suppress("UNUSED_PARAMETER")
    val unusedVehicle = vehicle
    return TripNavCursorIcons.drawableForStyle(context, style, color)
}

/** Classic nav chevron; tip toward top (forward). */
private fun drawNavArrowPointer(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    d: Float,
    fill: Paint,
    stroke: Paint,
) {
    val arrow = android.graphics.Path().apply {
        moveTo(cx, cy - 11f * d)
        lineTo(cx + 8.5f * d, cy + 9f * d)
        lineTo(cx, cy + 4.5f * d)
        lineTo(cx - 8.5f * d, cy + 9f * d)
        close()
    }
    canvas.drawPath(arrow, fill)
    canvas.drawPath(arrow, stroke)
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

/** Top-down motorbike; front wheel toward top (forward). */
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
        strokeWidth = 2.2f * d
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    val bodyFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fill.color
        style = Paint.Style.FILL
    }
    val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#111827")
        style = Paint.Style.FILL
    }
    // Front wheel (top / forward) and rear wheel
    canvas.drawCircle(cx, cy - 7.5f * d, 3.6f * d, stroke)
    canvas.drawCircle(cx, cy - 7.5f * d, 1.6f * d, dark)
    canvas.drawCircle(cx, cy + 7.2f * d, 4.0f * d, stroke)
    canvas.drawCircle(cx, cy + 7.2f * d, 1.8f * d, dark)
    // Fuel tank / body (elongated motorbike silhouette)
    val body = android.graphics.Path().apply {
        moveTo(cx - 2.2f * d, cy - 3.5f * d)
        lineTo(cx + 2.2f * d, cy - 3.5f * d)
        lineTo(cx + 3.4f * d, cy + 1.5f * d)
        lineTo(cx + 2.4f * d, cy + 5.0f * d)
        lineTo(cx - 2.4f * d, cy + 5.0f * d)
        lineTo(cx - 3.4f * d, cy + 1.5f * d)
        close()
    }
    canvas.drawPath(body, bodyFill)
    canvas.drawPath(body, stroke)
    // Seat
    canvas.drawRoundRect(
        cx - 2.0f * d,
        cy + 1.2f * d,
        cx + 2.0f * d,
        cy + 4.2f * d,
        1.2f * d,
        1.2f * d,
        dark,
    )
    // Fork + handlebars
    canvas.drawLine(cx, cy - 4.0f * d, cx, cy - 7.5f * d, frame)
    canvas.drawLine(cx - 4.2f * d, cy - 5.2f * d, cx + 4.2f * d, cy - 5.2f * d, frame)
    // Swingarm to rear wheel
    canvas.drawLine(cx, cy + 3.5f * d, cx, cy + 7.2f * d, frame)
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
    map.postDelayed(r, 5_000L)
}

@Composable
private fun OsmDensityMap(
    selectedCity: String,
    userLocation: Location?,
    gpsPinLocation: Location?,
    gpsAccuracyText: String,
    mapSeverityFilter: PotholeSeverity?,
    areaHeatEnabled: Boolean,
    weatherRainEnabled: Boolean = false,
    weatherRainGrid: CityWeatherRainGrid? = null,
    tripModeEnabled: Boolean,
    tripNavActive: Boolean,
    tripStreetFollow: Boolean = true,
    tripCameraMode: TripCameraMode = TripCameraMode.STREET,
    tripDeviceHeadingDeg: Float? = null,
    tripVehicle: TripVehicle = TripVehicle.CAR,
    mapCursorStyle: MapCursorStyle = MapCursorStyle.CAR,
    mapCursorColor: MapCursorColor = MapCursorColor.BLUE,
    tripRoutePoints: List<Pair<Double, Double>>,
    tripRouteTraveledMeters: Double = 0.0,
    tripRouteOptions: List<List<Pair<Double, Double>>> = emptyList(),
    tripRouteEtas: List<String> = emptyList(),
    tripSelectedRouteIndex: Int = 0,
    onSelectTripRouteIndex: (Int) -> Unit = {},
    tripRejoinPoints: List<Pair<Double, Double>> = emptyList(),
    tripOfferedAlternatePoints: List<Pair<Double, Double>> = emptyList(),
    onConfirmOfferedAlternate: () -> Unit = {},
    tripAlerts: List<TripPotholeAlert>,
    arrivalPlaces: List<com.example.potholereport.data.remote.NearbyOsmPoi> = emptyList(),
    tripStartLat: Double?,
    tripStartLon: Double?,
    tripDestinationLat: Double?,
    tripDestinationLon: Double?,
    /** Intermediate stops (lat, lon) shown as numbered markers. */
    tripStopPoints: List<Pair<Double, Double>> = emptyList(),
    tripDestFocusEpoch: Int = 0,
    tripRouteOverviewEpoch: Int = 0,
    onLocateMe: () -> Unit,
    onMapViewControlUsed: () -> Unit,
    onMapTouchChanged: (Boolean) -> Unit,
    onMapPanEndedAtCenter: (Double, Double) -> Unit,
    /** When trip planning (not navigating), map tap sets destination. */
    onMapPickDestination: ((lat: Double, lon: Double) -> Unit)? = null,
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
    mapConnectivityEpoch: Int = 0,
    mapFullscreen: Boolean = false,
    onExitFullscreen: (() -> Unit)? = null,
    onReportPothole: (() -> Unit)? = null,
    tripRoadVideoEnabled: Boolean = false,
    onToggleTripRoadVideo: (() -> Unit)? = null,
    mapCoveredByOverlay: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapHolder = remember { object { var map: MapView? = null } }
    val playMotionTracker = remember { TripPlayMotionTracker() }
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
    val lastWeatherRainEnabled = remember { mutableStateOf(false) }
    val lastWeatherRainCellCount = remember { mutableIntStateOf(-1) }
    val lastTripModeEnabled = remember { mutableStateOf(false) }
    val lastTripStreetFollow = remember { mutableStateOf(true) }
    val lastTripNavActive = remember { mutableStateOf(false) }
    val lastTripRouteSize = remember { mutableIntStateOf(-1) }
    val lastTripSelectedRouteIndex = remember { mutableIntStateOf(-1) }
    val lastTripOptionsSize = remember { mutableIntStateOf(-1) }
    val lastTripAlertsSize = remember { mutableIntStateOf(-1) }
    val lastTripTraveledMeters = remember { mutableFloatStateOf(-1f) }
    val lastTripRejoinSize = remember { mutableIntStateOf(-1) }
    val lastTripOfferedAltSize = remember { mutableIntStateOf(-1) }
    val areaHeatEnabledHolder = remember { object { var enabled: Boolean = false } }
    areaHeatEnabledHolder.enabled = areaHeatEnabled
    val weatherRainEnabledHolder = remember { object { var enabled: Boolean = false } }
    weatherRainEnabledHolder.enabled = weatherRainEnabled
    val weatherRainGridHolder = remember { object { var grid: CityWeatherRainGrid? = null } }
    weatherRainGridHolder.grid = weatherRainGrid
    val tripDestMarkerRef = remember { arrayOfNulls<Marker>(1) }
    val tripStartMarkerRef = remember { arrayOfNulls<Marker>(1) }
    val tripStopMarkersRef = remember { mutableListOf<Marker>() }
    val tripArrivalMarkersRef = remember { mutableListOf<Marker>() }
    val lastArrivalPlacesSize = remember { mutableIntStateOf(-1) }
    val streetNameMarkersRef = remember { mutableListOf<Marker>() }
    val tripMovingPointerActive = remember { booleanArrayOf(false) }
    val tripPreNavLocationActive = remember { booleanArrayOf(false) }
    val tripPointerVehicleSlot = remember { arrayOfNulls<TripVehicle>(1) }
    val tripPointerStyleSlot = remember { arrayOfNulls<MapCursorStyle>(1) }
    val tripPointerColorSlot = remember { arrayOfNulls<MapCursorColor>(1) }
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
    // Fullscreen trip map may pan outside the city; home map stays metro-clamped.
    val freePanHolder = remember { object { var enabled: Boolean = false } }
    freePanHolder.enabled = mapFullscreen && tripModeEnabled
    /** Trip planning: tap map to set destination (holder updated each composition). */
    val tripMapPickDestHolder = remember {
        object {
            var enabled: Boolean = false
            var onPick: (Double, Double) -> Unit = { _, _ -> }
        }
    }
    tripMapPickDestHolder.enabled =
        mapFullscreen && tripModeEnabled && !tripNavActive && onMapPickDestination != null
    tripMapPickDestHolder.onPick = { lat, lon ->
        onMapPickDestination?.invoke(lat, lon)
    }
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
                cityOutlineDecor.weatherRain?.let { m.overlays.remove(it) }
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
                tripStopMarkersRef.forEach { m.overlays.remove(it) }
                tripStopMarkersRef.clear()
                cityOutlineDecor.cityName = null
                cityOutlineDecor.clusters = null
                cityOutlineDecor.areaHeat = null
                cityOutlineDecor.weatherRain = null
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

    // Internet restored — redraw so CARTO tiles refill without restarting navigation.
    LaunchedEffect(mapConnectivityEpoch) {
        if (mapConnectivityEpoch <= 0) return@LaunchedEffect
        val map = mapHolder.map ?: return@LaunchedEffect
        map.invalidate()
        map.postInvalidate()
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
            suppressForTripMap = tripModeEnabled,
        )
        lastAreaHeatEnabled.value = areaHeatEnabled
        map.invalidate()
        map.postInvalidate()
    }

    LaunchedEffect(weatherRainEnabled, weatherRainGrid, mapSessionId) {
        var map = mapHolder.map
        var tries = 0
        while (map == null && tries < 30) {
            delay(16)
            map = mapHolder.map
            tries++
        }
        map ?: return@LaunchedEffect
        cityOutlineDecor.weatherRain?.drawSuppressed = false
        refreshWeatherRainOverlay(
            map = map,
            decor = cityOutlineDecor,
            enabled = weatherRainEnabled,
            grid = weatherRainGrid,
        )
        lastWeatherRainEnabled.value = weatherRainEnabled
        lastWeatherRainCellCount.intValue = weatherRainGrid?.cells?.size ?: 0
        map.invalidate()
        map.postInvalidate()
    }

    LaunchedEffect(
        tripModeEnabled,
        tripNavActive,
        tripStreetFollow,
        tripRoutePoints,
        tripRouteTraveledMeters,
        tripRouteOptions,
        tripRouteEtas,
        tripSelectedRouteIndex,
        tripAlerts,
        tripRejoinPoints,
        tripOfferedAlternatePoints,
        tripStartLat,
        tripStartLon,
        tripDestinationLat,
        tripDestinationLon,
        tripStopPoints,
        mapFullscreen,
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
        freePanHolder.enabled = mapFullscreen && tripModeEnabled
        map.applyHomePanPolicy(selectedCity, freePan = freePanHolder.enabled)
        if (!mapFullscreen) {
            map.resetMapOrientationNorthUp()
        }
        refreshTripNavigationOverlay(
            map = map,
            decor = cityOutlineDecor,
            tripModeEnabled = tripModeEnabled,
            routePoints = tripRoutePoints,
            traveledMeters = tripRouteTraveledMeters,
            alerts = tripAlerts,
            streetFollow = tripNavActive && tripStreetFollow,
            routeMovingFactor = if (tripNavActive && tripStreetFollow) {
                TripStreetCamera.movingFactor(gpsPinLocation?.speed?.takeIf {
                    gpsPinLocation?.hasSpeed() == true
                })
            } else {
                0f
            },
            navigationActive = tripNavActive,
            alternateRouteOptions = tripRouteOptions,
            alternateRouteEtas = tripRouteEtas,
            selectedRouteIndex = tripSelectedRouteIndex,
            previewSelecting = tripModeEnabled && !tripNavActive,
            onSelectRouteIndex = onSelectTripRouteIndex,
            rejoinPoints = tripRejoinPoints,
            offeredAlternatePoints = tripOfferedAlternatePoints,
            onConfirmOfferedAlternate = onConfirmOfferedAlternate,
        )
        // Numbered density disks look like "pothole 1/2/3" on trip map — hide them.
        refreshReportClusterOverlayOnly(
            map = map,
            selectedCity = selectedCity,
            decor = cityOutlineDecor,
            mapSeverityFilter = mapSeverityFilter,
            heatEnabled = areaHeatEnabled,
            suppressForTripMap = tripModeEnabled,
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
        syncTripStopMarkers(
            map = map,
            slots = tripStopMarkersRef,
            show = tripModeEnabled,
            stops = tripStopPoints,
        )
        bringMarkerToFront(map, gpsMarkerRef[0])
        lastTripModeEnabled.value = tripModeEnabled
        lastTripStreetFollow.value = tripStreetFollow
        lastTripNavActive.value = tripNavActive
        lastTripRouteSize.intValue = tripRoutePoints.size
        lastTripSelectedRouteIndex.intValue = tripSelectedRouteIndex
        lastTripOptionsSize.intValue = tripRouteOptions.size
        lastTripAlertsSize.intValue = tripAlerts.size
        lastTripTraveledMeters.floatValue = tripRouteTraveledMeters.toFloat()
        lastTripRejoinSize.intValue = tripRejoinPoints.size
        lastTripOfferedAltSize.intValue = tripOfferedAlternatePoints.size
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

    /** Trip Navigate → street-level 2D top-down (heading-up), with zoom-in transition. */
    var tripStreetFocusDone by remember { mutableStateOf(false) }
    var tripFollowEnabled by remember { mutableStateOf(false) }
    /** True while the Navigate / street-view zoom-in intro is playing. */
    var tripStreetZoomIntroActive by remember { mutableStateOf(false) }
    LaunchedEffect(tripNavActive) {
        if (!tripNavActive) {
            tripStreetFocusDone = false
            tripFollowEnabled = false
            tripStreetZoomIntroActive = false
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
        val street = tripStreetFollow
        val heading = street && fuseNavHeadingDeg(tripDeviceHeadingDeg, focus) != null
        val focusLoc = focus.withFusedNavHeading(tripDeviceHeadingDeg)
        if (street) {
            tripStreetZoomIntroActive = true
            programmaticCameraMoveRef[0] = true
            map.animateStreetViewZoomIn(
                selectedCity = selectedCity,
                location = focusLoc,
                targetZoom = tripFollowZoomForMode(tripCameraMode, null),
                headingUp = heading,
            )
            programmaticCameraMoveRef[0] = false
            tripStreetZoomIntroActive = false
        } else {
            runProgrammaticCamera {
                it.scheduleFollowTripLocation(
                    selectedCity,
                    focusLoc,
                    headingUp = false,
                    zoom = TRIP_NAV_OVERVIEW_ZOOM,
                    lookAheadMeters = 0.0,
                    smoothZoom = false,
                )
            }
        }
        onMapCameraSnapshot(
            focus.latitude,
            focus.longitude,
            tripFollowZoomForMode(tripCameraMode, null),
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
        val street = tripStreetFollow
        val heading = street && fuseNavHeadingDeg(tripDeviceHeadingDeg, gps) != null
        val focusLoc = gps.withFusedNavHeading(tripDeviceHeadingDeg)
        if (street) {
            tripStreetZoomIntroActive = true
            programmaticCameraMoveRef[0] = true
            map.animateStreetViewZoomIn(
                selectedCity = selectedCity,
                location = focusLoc,
                targetZoom = tripFollowZoomForMode(tripCameraMode, null),
                headingUp = heading,
            )
            programmaticCameraMoveRef[0] = false
            tripStreetZoomIntroActive = false
        } else {
            runProgrammaticCamera {
                it.scheduleFollowTripLocation(
                    selectedCity,
                    focusLoc,
                    headingUp = false,
                    zoom = TRIP_NAV_OVERVIEW_ZOOM,
                    lookAheadMeters = 0.0,
                    smoothZoom = false,
                )
            }
        }
        onMapCameraSnapshot(
            gps.latitude,
            gps.longitude,
            tripFollowZoomForMode(tripCameraMode, null),
        )
        tripStreetFocusDone = true
    }
    // If still on city overview when the user begins moving, force closer street follow.
    LaunchedEffect(
        tripNavActive,
        tripStreetZoomIntroActive,
        gpsPinLocation?.speed,
        gpsPinLocation?.bearing,
    ) {
        if (!tripNavActive || tripStreetZoomIntroActive) return@LaunchedEffect
        val gps = gpsPinLocation ?: return@LaunchedEffect
        if (!gps.isTripMoving()) return@LaunchedEffect
        val map = mapHolder.map ?: return@LaunchedEffect
        if (!map.isAtCityOverviewZoom() && tripStreetFocusDone && map.zoomLevelDouble >= TRIP_FOLLOW_ZOOM - 0.2) {
            // Already close enough; heading-up is applied by the follow LaunchedEffect.
            return@LaunchedEffect
        }
        runProgrammaticCamera {
            val street = tripStreetFollow
            val speed = gps.speed.takeIf { gps.hasSpeed() }
            val playMotion = if (tripCameraMode == TripCameraMode.PLAY) {
                playMotionTracker.update(
                    speedMps = speed,
                    elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                )
            } else {
                null
            }
            val speedZoom = tripFollowZoomForMode(
                tripCameraMode,
                speed,
                playIntensity = playMotion?.dive ?: 0f,
                playMoving = playMotion?.moving == true,
            )
            it.scheduleFollowTripLocation(
                selectedCity,
                gps.withFusedNavHeading(tripDeviceHeadingDeg),
                headingUp = street,
                zoom = if (street) speedZoom else TRIP_NAV_OVERVIEW_ZOOM,
                lookAheadMeters = if (street) {
                    TripStreetCamera.lookAheadMetersForSpeed(speed)
                } else {
                    0.0
                },
                smoothZoom = street,
            )
        }
        onMapCameraSnapshot(
            gps.latitude,
            gps.longitude,
            if (tripStreetFollow) {
                tripFollowZoomForMode(tripCameraMode, gps.speed.takeIf { gps.hasSpeed() })
            } else {
                TRIP_NAV_OVERVIEW_ZOOM
            },
        )
        tripStreetFocusDone = true
    }

    /** Before Navigate: north-up overview that fits the full planned route (+ alternates). */
    LaunchedEffect(
        tripNavActive,
        tripModeEnabled,
        tripRoutePoints,
        tripRouteOptions,
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
            it.scheduleFitRoutePreview(
                tripRoutePoints,
                selectedCity,
                extraRoutePoints = tripRouteOptions,
            )
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
        cityOutlineDecor.weatherRain?.drawSuppressed = true
        action(map)
        map.invalidate()
        map.postDelayed({
            cityOutlineDecor.cityName?.drawSuppressed = false
            cityOutlineDecor.areaHeat?.drawSuppressed = false
            cityOutlineDecor.weatherRain?.drawSuppressed = false
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
            map.applyHomePanPolicy(
                selectedCityHolder.city,
                freePan = freePanHolder.enabled,
                animateClamp = true,
            )
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
        map.applyMetroPanBounds(selectedCityHolder.city, freePan = freePanHolder.enabled)
        map.snapBackToCityBorder(selectedCityHolder.city, freePan = freePanHolder.enabled)
        if (!programmaticCameraMoveRef[0]) {
            onMapCameraSnapshot(
                map.mapCenter.latitude,
                map.mapCenter.longitude,
                map.zoomLevelDouble,
            )
        }
        // Pinch-out to city floor → snap to full city overview (same as FAB city map).
        // Soften: only reframe when clearly off city framing, not on tiny drifts.
        if (!freePanHolder.enabled && map.isAtCityOverviewZoom()) {
            if (map.needsCityOverviewReframe(selectedCityHolder.city) || !mapCameraUserPositioned) {
                runProgrammaticCamera { it.scheduleFitCityOverview(selectedCityHolder.city, overviewRestore) }
            }
        } else if (!mapCameraUserPositioned &&
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
        map.applyHomePanPolicy(selectedCityHolder.city, freePan = freePanHolder.enabled)
        if (map.isAtMaxZoom()) {
            zoomOutMapUsedSinceMax = false
        }
        refreshReportClusterOverlayOnly(
            map = map,
            selectedCity = selectedCityHolder.city,
            decor = cityOutlineDecor,
            mapSeverityFilter = mapSeverityFilterHolder.filter,
            heatEnabled = areaHeatEnabledHolder.enabled,
            suppressForTripMap = tripModeEnabled,
        )
        // Dots ↔ L/M/R placement when zoom crosses the road-detail threshold.
        if (tripModeEnabled) {
            cityOutlineDecor.tripNav?.syncMarkers(map)
            map.invalidate()
        }
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

    // #25.2 — OSM street/place name markers at street zoom (hidden at city overview / heat).
    LaunchedEffect(mapSessionId, areaHeatEnabled, atCityOverviewZoom, mapUserInteracting) {
        if (areaHeatEnabled || atCityOverviewZoom) {
            val map = mapHolder.map
            if (map != null) {
                syncStreetNameMarkers(map, streetNameMarkersRef, emptyList())
                map.invalidate()
            }
            return@LaunchedEffect
        }
        while (true) {
            delay(3_000)
            if (mapUserInteracting) continue
            val map = mapHolder.map ?: continue
            if (map.isAtCityOverviewZoom() || map.zoomLevelDouble < 15.0) {
                syncStreetNameMarkers(map, streetNameMarkersRef, emptyList())
                map.invalidate()
                continue
            }
            val center = map.mapCenter
            val labels = withContext(Dispatchers.IO) {
                com.example.potholereport.data.remote.StreetNameOverlayClient.fetchLabels(
                    lat = center.latitude,
                    lon = center.longitude,
                    radiusM = if (map.zoomLevelDouble >= 16.5) 220 else 380,
                    maxCount = if (map.zoomLevelDouble >= 16.5) 24 else 14,
                )
            }
            syncStreetNameMarkers(map, streetNameMarkersRef, labels)
            map.invalidate()
        }
    }

    /** Keep the GPS pointer centered; street-follow = 2D heading-up + speed zoom. */
    LaunchedEffect(
        tripNavActive,
        tripFollowEnabled,
        tripStreetFollow,
        tripCameraMode,
        tripStreetZoomIntroActive,
        mapUserInteracting,
        gpsPinLocation?.latitude,
        gpsPinLocation?.longitude,
        gpsPinLocation?.bearing,
        gpsPinLocation?.speed,
        tripDeviceHeadingDeg,
    ) {
        if (!tripNavActive || mapUserInteracting || tripStreetZoomIntroActive) return@LaunchedEffect
        val gps = gpsPinLocation ?: return@LaunchedEffect
        val map = mapHolder.map ?: return@LaunchedEffect
        if (tripStreetFollow) {
            if (!tripFollowEnabled) return@LaunchedEffect
            val speed = gps.speed.takeIf { gps.hasSpeed() }
            val playMotion = if (tripCameraMode == TripCameraMode.PLAY) {
                playMotionTracker.update(
                    speedMps = speed,
                    elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                )
            } else {
                playMotionTracker.reset()
                null
            }
            val speedZoom = tripFollowZoomForMode(
                tripCameraMode,
                speed,
                playIntensity = playMotion?.dive ?: 0f,
                playMoving = playMotion?.moving == true,
            )
            val movingFactor = TripStreetCamera.movingFactor(speed)
            cityOutlineDecor.tripNav?.applyRouteStrokeWidths(
                movingFactor = movingFactor,
                mapWidthPx = map.width,
                navigationActive = true,
                mapZoom = speedZoom,
            )
            map.invalidate()
            runProgrammaticCamera {
                map.scheduleFollowTripLocation(
                    selectedCity,
                    gps.withFusedNavHeading(tripDeviceHeadingDeg)
                        .forTripMapDisplay(
                            tripRoutePoints,
                            enableSnap = true,
                            preferredAlongMeters = tripRouteTraveledMeters.takeIf { it > 0.0 },
                        ),
                    headingUp = true,
                    zoom = speedZoom,
                    lookAheadMeters = TripStreetCamera.lookAheadMetersForSpeed(speed),
                    smoothZoom = true,
                )
            }
        } else {
            cityOutlineDecor.tripNav?.applyRouteStrokeWidths(
                movingFactor = 0f,
                mapWidthPx = map.width,
                navigationActive = true,
                mapZoom = TRIP_NAV_OVERVIEW_ZOOM,
            )
            runProgrammaticCamera {
                map.scheduleFollowTripLocation(
                    selectedCity,
                    gps.withFusedNavHeading(tripDeviceHeadingDeg)
                        .forTripMapDisplay(
                            tripRoutePoints,
                            enableSnap = true,
                            preferredAlongMeters = tripRouteTraveledMeters.takeIf { it > 0.0 },
                        ),
                    headingUp = false,
                    zoom = TRIP_NAV_OVERVIEW_ZOOM,
                    lookAheadMeters = 0.0,
                    smoothZoom = false,
                )
            }
        }
    }

    LaunchedEffect(tripCameraMode, tripStreetFollow, tripNavActive) {
        if (!tripNavActive) {
            tripStreetZoomIntroActive = false
            playMotionTracker.reset()
            return@LaunchedEffect
        }
        if (tripCameraMode != TripCameraMode.PLAY) {
            playMotionTracker.reset()
        }
        if (tripStreetFollow) {
            tripFollowEnabled = true
            // Initial Navigate intro is owned by tripNavActive effect; this only
            // re-plays when the user turns street view back on mid-trip.
            if (!tripStreetFocusDone) return@LaunchedEffect
            val gps = gpsPinLocation ?: return@LaunchedEffect
            val map = mapHolder.map ?: return@LaunchedEffect
            tripStreetZoomIntroActive = true
            programmaticCameraMoveRef[0] = true
            map.animateStreetViewZoomIn(
                selectedCity = selectedCity,
                location = gps.withFusedNavHeading(tripDeviceHeadingDeg),
                targetZoom = tripFollowZoomForMode(tripCameraMode, null),
                headingUp = fuseNavHeadingDeg(tripDeviceHeadingDeg, gps) != null,
            )
            programmaticCameraMoveRef[0] = false
            tripStreetZoomIntroActive = false
        } else {
            tripStreetZoomIntroActive = false
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

    /** Destination search / pick → ease map to that area. */
    LaunchedEffect(tripDestFocusEpoch) {
        if (tripDestFocusEpoch <= 0 || !tripModeEnabled) return@LaunchedEffect
        val lat = tripDestinationLat ?: return@LaunchedEffect
        val lon = tripDestinationLon ?: return@LaunchedEffect
        if (!lat.isFinite() || !lon.isFinite()) return@LaunchedEffect
        val map = mapHolder.map ?: return@LaunchedEffect
        runProgrammaticCamera {
            it.controller.animateTo(GeoPoint(lat, lon), 14.0, 750L)
        }
    }

    /** Direction → zoom out to fit the full planned route. */
    LaunchedEffect(tripRouteOverviewEpoch) {
        if (tripRouteOverviewEpoch <= 0 || !tripModeEnabled) return@LaunchedEffect
        val map = mapHolder.map ?: return@LaunchedEffect
        if (tripRoutePoints.size < 2 && tripRouteOptions.none { it.size >= 2 }) return@LaunchedEffect
        runProgrammaticCamera {
            it.scheduleFitRoutePreview(
                routePoints = tripRoutePoints,
                selectedCity = selectedCity,
                extraRoutePoints = tripRouteOptions,
            )
        }
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
        val uiScale = rememberMapUiScale()
        val mapFabGap = uiScale.dp(8f)
        val mapFabSize = uiScale.dp(44f)
        val mapFabShape = CircleShape
        val mapFabIcon = uiScale.dp(20f)
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
                    installCartoLabelDarkeningTilesOverlay()
                    @Suppress("DEPRECATION")
                    setBuiltInZoomControls(false)
                    applyHomePanPolicy(selectedCityHolder.city, freePan = freePanHolder.enabled)
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
                        cityOutlineDecor.weatherRain?.drawSuppressed = true
                    }
                    val resumeHeavyOverlaysRunnable = Runnable {
                        val chrome = cityMapChromeVisibleRef[0]
                        cityOutlineDecor.dim?.isEnabled = chrome
                        cityOutlineDecor.edge?.isEnabled = chrome
                        cityOutlineDecor.clusters?.isEnabled = true
                        cityOutlineDecor.cityName?.drawSuppressed = false
                        cityOutlineDecor.areaHeat?.drawSuppressed = false
                        cityOutlineDecor.weatherRain?.drawSuppressed = false
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
                        // Longer idle before city snap — less yank after pinch/pan.
                        postDelayed(cityReframeAfterIdle, 320L)
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
                                    if (!panExceededSlop && tripMapPickDestHolder.enabled) {
                                        val geo = mapView.projection.fromPixels(
                                            event.x.toInt(),
                                            event.y.toInt(),
                                        )
                                        if (geo.latitude.isFinite() && geo.longitude.isFinite()) {
                                            tripMapPickDestHolder.onPick(
                                                geo.latitude,
                                                geo.longitude,
                                            )
                                        }
                                    }
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
                                // Resume chrome after gesture settles (smoother pan/pinch).
                                v.postDelayed(resumeHeavyOverlaysRunnable, 220L)
                                // Rubber-band: after MapView finishes this UP (may start a fling),
                                // ease back to the city border if overshot. A delayed pass catches
                                // flings that only exit the city after release.
                                if (!freePanHolder.enabled) {
                                    removeCallbacks(cityReframeAfterIdle)
                                    post {
                                        snapBackToCityBorder(
                                            selectedCityHolder.city,
                                            freePan = false,
                                        )
                                    }
                                    postDelayed(cityReframeAfterIdle, 300L)
                                } else if (scrollParentNotified) {
                                    v.removeCallbacks(endParentScrollRunnable)
                                    v.postDelayed(endParentScrollRunnable, 80L)
                                }
                                if (scrollParentNotified && !freePanHolder.enabled) {
                                    scrollParentNotified = false
                                    touchCallbacks.onMapTouchChanged(false)
                                }
                            }
                        }
                        false
                    }
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            // Avoid Compose snapshot writes mid-fling — they stutter pan.
                            // While the finger is down, allow overshoot (no snap). After release /
                            // during coasting fling, snap back if still outside the city.
                            if (!freePanHolder.enabled) {
                                if (!scrollParentNotified) {
                                    removeCallbacks(cityReframeAfterIdle)
                                    postDelayed(cityReframeAfterIdle, 280L)
                                }
                            } else if (isAtCityOverviewZoom()) {
                                removeCallbacks(cityReframeAfterIdle)
                                postDelayed(cityReframeAfterIdle, 320L)
                            }
                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            // Soften: defer metro clamp / city snap to idle, not every zoom tick.
                            touchCallbacks.onMapZoomChanged(this@apply)
                            val atCity = isAtCityOverviewZoom()
                            touchCallbacks.onCityViewZoomChanged(atCity)
                            if ((!freePanHolder.enabled || atCity) && !scrollParentNotified) {
                                removeCallbacks(cityReframeAfterIdle)
                                postDelayed(cityReframeAfterIdle, 280L)
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
                // Keep MapView attached but inert under report/sign-in overlays.
                // Removing it from composition previously finished the Activity on some devices.
                if (mapCoveredByOverlay) {
                    if (view.visibility != View.GONE) {
                        runCatching { view.onPause() }
                        view.visibility = View.GONE
                    }
                } else if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                    runCatching { view.onResume() }
                }
                view.applyHomePanPolicy(selectedCityHolder.city, freePan = freePanHolder.enabled)
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
                    refreshWeatherRainOverlay(
                        map = view,
                        decor = cityOutlineDecor,
                        enabled = weatherRainEnabledHolder.enabled,
                        grid = weatherRainGridHolder.grid,
                    )
                    cityOutlineDecor.clusters?.let { clusters ->
                        view.overlays.remove(clusters)
                        view.overlays.add(clusters)
                    }
                    view.applyHomePanPolicy(selectedCity, freePan = freePanHolder.enabled)
                    lastOutlineCity.value = selectedCity
                    lastSyncedReportEpoch.intValue = recentReportsEpoch
                    lastMapSeverityFilter.value = mapSeverityFilter
                    lastAreaHeatEnabled.value = areaHeatEnabledHolder.enabled
                    lastWeatherRainEnabled.value = weatherRainEnabledHolder.enabled
                    lastWeatherRainCellCount.intValue = weatherRainGridHolder.grid?.cells?.size ?: 0
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
                        suppressForTripMap = tripModeEnabled,
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
                        suppressForTripMap = tripModeEnabled,
                    )
                    lastAreaHeatEnabled.value = areaHeatEnabled
                    view.invalidate()
                    view.postInvalidate()
                }
                val weatherCellCount = weatherRainGrid?.cells?.size ?: 0
                if (weatherRainEnabled != lastWeatherRainEnabled.value ||
                    weatherCellCount != lastWeatherRainCellCount.intValue
                ) {
                    cityOutlineDecor.weatherRain?.drawSuppressed = false
                    refreshWeatherRainOverlay(
                        map = view,
                        decor = cityOutlineDecor,
                        enabled = weatherRainEnabled,
                        grid = weatherRainGrid,
                    )
                    lastWeatherRainEnabled.value = weatherRainEnabled
                    lastWeatherRainCellCount.intValue = weatherCellCount
                    view.invalidate()
                    view.postInvalidate()
                }
                if (
                    tripModeEnabled != lastTripModeEnabled.value ||
                    tripNavActive != lastTripNavActive.value ||
                    tripStreetFollow != lastTripStreetFollow.value ||
                    tripRoutePoints.size != lastTripRouteSize.intValue ||
                    tripSelectedRouteIndex != lastTripSelectedRouteIndex.intValue ||
                    tripRouteOptions.size != lastTripOptionsSize.intValue ||
                    tripAlerts.size != lastTripAlertsSize.intValue ||
                    tripRejoinPoints.size != lastTripRejoinSize.intValue ||
                    tripOfferedAlternatePoints.size != lastTripOfferedAltSize.intValue ||
                    arrivalPlaces.size != lastArrivalPlacesSize.intValue ||
                    kotlin.math.abs(tripRouteTraveledMeters.toFloat() - lastTripTraveledMeters.floatValue) > 2f
                ) {
                    refreshTripNavigationOverlay(
                        map = view,
                        decor = cityOutlineDecor,
                        tripModeEnabled = tripModeEnabled,
                        routePoints = tripRoutePoints,
                        traveledMeters = tripRouteTraveledMeters,
                        alerts = tripAlerts,
                        streetFollow = tripNavActive && tripStreetFollow,
                        routeMovingFactor = if (tripNavActive && tripStreetFollow) {
                            TripStreetCamera.movingFactor(
                                gpsPinLocation?.speed?.takeIf { gpsPinLocation?.hasSpeed() == true },
                            )
                        } else {
                            0f
                        },
                        navigationActive = tripNavActive,
                        alternateRouteOptions = tripRouteOptions,
                        alternateRouteEtas = tripRouteEtas,
                        selectedRouteIndex = tripSelectedRouteIndex,
                        previewSelecting = tripModeEnabled && !tripNavActive,
                        onSelectRouteIndex = onSelectTripRouteIndex,
                        rejoinPoints = tripRejoinPoints,
                        offeredAlternatePoints = tripOfferedAlternatePoints,
                        onConfirmOfferedAlternate = onConfirmOfferedAlternate,
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
                    syncTripStopMarkers(
                        map = view,
                        slots = tripStopMarkersRef,
                        show = tripModeEnabled,
                        stops = tripStopPoints,
                    )
                    syncArrivalPlaceMarkers(
                        map = view,
                        slots = tripArrivalMarkersRef,
                        places = arrivalPlaces,
                    )
                    bringMarkerToFront(view, gpsMarkerRef[0])
                    lastTripModeEnabled.value = tripModeEnabled
                    lastTripNavActive.value = tripNavActive
                    lastTripStreetFollow.value = tripStreetFollow
                    lastTripRouteSize.intValue = tripRoutePoints.size
                    lastTripSelectedRouteIndex.intValue = tripSelectedRouteIndex
                    lastTripOptionsSize.intValue = tripRouteOptions.size
                    lastTripAlertsSize.intValue = tripAlerts.size
                    lastTripTraveledMeters.floatValue = tripRouteTraveledMeters.toFloat()
                    lastTripRejoinSize.intValue = tripRejoinPoints.size
                    lastTripOfferedAltSize.intValue = tripOfferedAlternatePoints.size
                    lastArrivalPlacesSize.intValue = arrivalPlaces.size
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
                    syncTripStopMarkers(
                        map = view,
                        slots = tripStopMarkersRef,
                        show = tripModeEnabled,
                        stops = tripStopPoints,
                    )
                    syncArrivalPlaceMarkers(
                        map = view,
                        slots = tripArrivalMarkersRef,
                        places = arrivalPlaces,
                    )
                    lastArrivalPlacesSize.intValue = arrivalPlaces.size
                    bringMarkerToFront(view, gpsMarkerRef[0])
                } else if (arrivalPlaces.size != lastArrivalPlacesSize.intValue) {
                    syncArrivalPlaceMarkers(
                        map = view,
                        slots = tripArrivalMarkersRef,
                        places = arrivalPlaces,
                    )
                    lastArrivalPlacesSize.intValue = arrivalPlaces.size
                    view.invalidate()
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
                // Trip planner (before Navigate): keep green current-position pin — no blue sector.
                // Blue Google-style pre-nav disc is reserved for after navigation starts (MapLibre).
                val showPreNavLocation = false
                val keepGreenVisible = tripModeEnabled && !tripNavActive && gps != null
                val mapDisplayGps = gps?.forTripMapDisplay(
                    routePoints = tripRoutePoints,
                    enableSnap = showMovePointer || keepGreenVisible,
                    preferredAlongMeters = tripRouteTraveledMeters.takeIf { it > 0.0 },
                )
                val moveIconVehicle = tripVehicle
                val fusedHeading = fuseNavHeadingDeg(tripDeviceHeadingDeg, gps)
                // Pre-nav sector follows the phone; while navigating prefer fused course.
                val pointerBearing = when {
                    showPreNavLocation -> {
                        val h = tripDeviceHeadingDeg?.takeIf { it.isFinite() }
                            ?: fusedHeading
                        if (h != null) {
                            tripPointerLastBearing[0] = h
                            h
                        } else if (tripPointerLastBearing[0].isFinite()) {
                            tripPointerLastBearing[0]
                        } else {
                            0f
                        }
                    }
                    fusedHeading != null -> {
                        tripPointerLastBearing[0] = fusedHeading
                        fusedHeading
                    }
                    tripPointerLastBearing[0].isFinite() -> tripPointerLastBearing[0]
                    else -> 0f
                }
                fun applyVehiclePointer(marker: Marker, vehicle: TripVehicle, bearingDeg: Float) {
                    val needsIcon =
                        tripPointerVehicleSlot[0] != vehicle ||
                            tripPointerStyleSlot[0] != mapCursorStyle ||
                            tripPointerColorSlot[0] != mapCursorColor ||
                            !tripMovingPointerActive[0]
                    if (needsIcon) {
                        marker.setIcon(
                            buildTripNavPointerDrawable(
                                view.context,
                                vehicle,
                                mapCursorStyle,
                                mapCursorColor,
                            ),
                        )
                        tripPointerVehicleSlot[0] = vehicle
                        tripPointerStyleSlot[0] = mapCursorStyle
                        tripPointerColorSlot[0] = mapCursorColor
                    }
                    marker.setFlat(true)
                    marker.setRotation(bearingDeg)
                    marker.isEnabled = true
                    tripMovingPointerActive[0] = true
                    tripPreNavLocationActive[0] = false
                    cancelGpsPinAutoHide(view, gpsPinHideRunnable)
                    bringMarkerToFront(view, marker)
                }
                fun applyPreNavLocation(marker: Marker, headingDeg: Float) {
                    if (!tripPreNavLocationActive[0] || tripMovingPointerActive[0]) {
                        marker.setIcon(buildPreNavMyLocationDrawable(view.context))
                        tripPreNavLocationActive[0] = true
                        tripMovingPointerActive[0] = false
                        tripPointerVehicleSlot[0] = null
                        tripPointerStyleSlot[0] = null
                        tripPointerColorSlot[0] = null
                    }
                    marker.setFlat(true)
                    marker.setRotation(headingDeg)
                    marker.isEnabled = true
                    cancelGpsPinAutoHide(view, gpsPinHideRunnable)
                    bringMarkerToFront(view, marker)
                }
                fun applyIdleGpsPin(marker: Marker) {
                    marker.setIcon(buildGpsPinDrawable(view.context))
                    marker.setFlat(false)
                    marker.setRotation(0f)
                    tripMovingPointerActive[0] = false
                    tripPreNavLocationActive[0] = false
                    tripPointerVehicleSlot[0] = null
                    tripPointerStyleSlot[0] = null
                    tripPointerColorSlot[0] = null
                }
                when {
                    gps == null -> {
                        cancelGpsPinAutoHide(view, gpsPinHideRunnable)
                        lastGpsPinScheduleKey.value = null
                        lastMapLocateEpochForGpsPin.intValue = mapLocateEpoch
                        tripMovingPointerActive[0] = false
                        tripPreNavLocationActive[0] = false
                        tripPointerVehicleSlot[0] = null
                        tripPointerStyleSlot[0] = null
                        tripPointerColorSlot[0] = null
                        if (slot != null) {
                            view.overlays.remove(slot)
                            gpsMarkerRef[0] = null
                            view.invalidate()
                        }
                    }
                    slot == null -> {
                        val display = mapDisplayGps ?: gps
                        val m = Marker(view).apply {
                            position = GeoPoint(display.latitude, display.longitude)
                            title = "Your location (GPS)"
                            snippet = "From device GPS"
                            setInfoWindow(null)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            isEnabled = true
                            when {
                                showMovePointer ->
                                    applyVehiclePointer(this, moveIconVehicle, pointerBearing)
                                showPreNavLocation ->
                                    applyPreNavLocation(this, pointerBearing)
                                else -> applyIdleGpsPin(this)
                            }
                        }
                        gpsMarkerRef[0] = m
                        view.overlays.add(m)
                        bringMarkerToFront(view, m)
                        view.invalidate()
                    }
                    else -> {
                        val display = mapDisplayGps ?: gps
                        val next = GeoPoint(display.latitude, display.longitude)
                        if (abs(slot.position.latitude - next.latitude) > 1e-7 ||
                            abs(slot.position.longitude - next.longitude) > 1e-7
                        ) {
                            slot.position = next
                        }
                        when {
                            showMovePointer ->
                                applyVehiclePointer(slot, moveIconVehicle, pointerBearing)
                            showPreNavLocation ->
                                applyPreNavLocation(slot, pointerBearing)
                            tripMovingPointerActive[0] || tripPreNavLocationActive[0] ->
                                applyIdleGpsPin(slot)
                        }
                        if (showMovePointer || showPreNavLocation) {
                            bringMarkerToFront(view, slot)
                        }
                        view.invalidate()
                    }
                }
                latestGpsPinForTouch[0] = gps?.let { Location(it) }
                if (gps != null && !showMovePointer && !keepGreenVisible) {
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
                } else if (showMovePointer || keepGreenVisible) {
                    lastGpsPinScheduleKey.value = null
                    cancelGpsPinAutoHide(view, gpsPinHideRunnable)
                    gpsMarkerRef[0]?.isEnabled = true
                    if (keepGreenVisible && slot != null && !tripMovingPointerActive[0]) {
                        applyIdleGpsPin(slot)
                    }
                }
            }
        )

        val mapCompassSize = uiScale.dp(44f)
        val mapCompassLabelN = uiScale.sp(9f)
        val mapCompassLabelOther = uiScale.sp(7f)
        val gpsChipFont = if (mapFullscreen) uiScale.sp(10f) else uiScale.sp(9f)
        val gpsChipPadH = if (mapFullscreen) uiScale.dp(8f) else uiScale.dp(6f)
        val gpsChipPadV = if (mapFullscreen) uiScale.dp(5f) else uiScale.dp(4f)
        val bottomChromePad = uiScale.dp(8f)
        val bottomChromeInset = Modifier.padding(bottom = bottomChromePad)

        @Composable
        fun MapCompassRose(modifier: Modifier) {
            Surface(
                modifier = modifier
                    .size(mapCompassSize)
                    .semantics {
                        contentDescription =
                            "Map compass; letters show north, east, south, and west relative to the map."
                    },
                shape = CircleShape,
                color = CitizenMapChrome.Panel,
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
                            maxLines = 1,
                        )
                        Text(
                            "S",
                            modifier = Modifier.align(Alignment.BottomCenter),
                            fontSize = mapCompassLabelOther,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = mapCompassLabelOther,
                            maxLines = 1,
                        )
                        Text(
                            "W",
                            modifier = Modifier.align(Alignment.CenterStart),
                            fontSize = mapCompassLabelOther,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = mapCompassLabelOther,
                            maxLines = 1,
                        )
                        Text(
                            "E",
                            modifier = Modifier.align(Alignment.CenterEnd),
                            fontSize = mapCompassLabelOther,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = mapCompassLabelOther,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        @Composable
        fun GpsAccuracyChip(modifier: Modifier) {
            Surface(
                modifier = modifier.semantics {
                    contentDescription = "GPS accuracy: $gpsAccuracyText"
                },
                shape = RoundedCornerShape(6.dp),
                color = CitizenMapChrome.Panel,
                shadowElevation = 3.dp,
            ) {
                Text(
                    gpsAccuracyText,
                    modifier = Modifier.padding(horizontal = gpsChipPadH, vertical = gpsChipPadV),
                    fontSize = gpsChipFont,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4B5563),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        @Composable
        fun LocateFab() {
            val followActive = tripNavActive && tripFollowEnabled
            val needsRecenter = tripNavActive && !tripFollowEnabled
            SmallFloatingActionButton(
                onClick = {
                    if (tripNavActive) {
                        tripFollowEnabled = true
                        gpsPinLocation?.let { gps ->
                            runProgrammaticCamera {
                                val street = tripStreetFollow
                                val speed = gps.speed.takeIf { gps.hasSpeed() }
                                val playMotion = if (tripCameraMode == TripCameraMode.PLAY) {
                                    playMotionTracker.update(
                                        speedMps = speed,
                                        elapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                                    )
                                } else {
                                    null
                                }
                                val speedZoom = tripFollowZoomForMode(
                                    tripCameraMode,
                                    speed,
                                    playIntensity = playMotion?.dive ?: 0f,
                                    playMoving = playMotion?.moving == true,
                                )
                                it.scheduleFollowTripLocation(
                                    selectedCity,
                                    gps.withFusedNavHeading(tripDeviceHeadingDeg)
                                        .forTripMapDisplay(
                                            tripRoutePoints,
                                            enableSnap = true,
                                            preferredAlongMeters = tripRouteTraveledMeters.takeIf { meters ->
                                                meters > 0.0
                                            },
                                        ),
                                    headingUp = street,
                                    zoom = if (street) speedZoom else TRIP_NAV_OVERVIEW_ZOOM,
                                    lookAheadMeters = if (street) {
                                        TripStreetCamera.lookAheadMetersForSpeed(speed)
                                    } else {
                                        0.0
                                    },
                                    smoothZoom = street,
                                )
                            }
                        }
                    } else {
                        onLocateMe()
                    }
                },
                modifier = Modifier.size(mapFabSize),
                containerColor = if (needsRecenter) Color(0xFF1D4ED8) else Color.White,
                contentColor = when {
                    needsRecenter -> Color.White
                    followActive -> Color(0xFF15803D)
                    else -> MaterialTheme.colorScheme.primary
                },
                shape = mapFabShape,
            ) {
                Icon(
                    imageVector = if (needsRecenter) {
                        Icons.Filled.Navigation
                    } else {
                        Icons.Default.MyLocation
                    },
                    contentDescription = if (tripNavActive) {
                        if (followActive) "Following" else "Recenter on route"
                    } else {
                        "Trace to current GPS location"
                    },
                    modifier = Modifier.size(mapFabIcon),
                )
            }
        }

        // Zoom stack sits above the shared bottom chrome row.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(bottomChromeInset)
                .padding(end = 10.dp, bottom = mapFabSize + mapFabGap + 4.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(mapFabGap),
        ) {
            // Zoom extras only on demand — keeps the stack short so FABs do not collide.
            AnimatedVisibility(
                visible = showZoomControls && !tripNavActive,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(160)),
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
                        modifier = Modifier.size(mapFabSize),
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
                        modifier = Modifier.size(mapFabSize),
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
                        modifier = Modifier.size(mapFabSize),
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
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(160)),
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
                            modifier = Modifier.size(mapFabSize),
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
                            modifier = Modifier.size(mapFabSize),
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
        }

        // Landing map: compass top-left, GPS top-right (compact).
        if (!mapFullscreen) {
            MapCompassRose(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 6.dp),
            )
            GpsAccuracyChip(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp),
            )
        }

        // Bottom chrome flush to the map bottom.
        // Fullscreen nav: fixed left / center / right slots so Report never overlaps Minimize.
        if (mapFullscreen) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(bottomChromeInset)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                ) {
                    MapCompassRose(modifier = Modifier)
                    GpsAccuracyChip(modifier = Modifier)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .height(mapFabSize),
                    contentAlignment = Alignment.Center,
                ) {
                    // Show as soon as Trip is on (planning + navigating), not only after Navigate.
                    val showNavReport = tripModeEnabled && onReportPothole != null
                    if (showNavReport) {
                        // Pill → camera circle, kept mid; width shrinks from both ends.
                        var reportExpanded by remember(tripModeEnabled, mapFullscreen) {
                            mutableStateOf(true)
                        }
                        LaunchedEffect(tripModeEnabled, mapFullscreen) {
                            reportExpanded = true
                            delay(5_000)
                            reportExpanded = false
                        }
                        val reportWidth by animateDpAsState(
                            targetValue = if (reportExpanded) 110.dp else mapFabSize,
                            animationSpec = tween(durationMillis = 200),
                            label = "reportChromeWidth",
                        )
                        val reportClick = onReportPothole
                        // Mid-aligned tablet: width collapses toward center from both ends.
                        Box(
                            modifier = Modifier
                                .systemGestureExclusion()
                                .height(mapFabSize)
                                .width(reportWidth)
                                .clip(CircleShape)
                                .background(Color(0xFFB74233))
                                .border(1.dp, Color(0xFF8B2F24), CircleShape)
                                .clickable(onClick = { reportClick?.invoke() })
                                .semantics { contentDescription = "Report a pothole" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(
                                    horizontal = if (reportExpanded) 8.dp else 0.dp,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(mapFabIcon),
                                )
                                AnimatedVisibility(
                                    visible = reportExpanded,
                                    enter = fadeIn(tween(160)),
                                    exit = fadeOut(tween(140)),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            "Report",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.wrapContentWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(mapFabGap),
                ) {
                    if (onExitFullscreen != null) {
                        SmallFloatingActionButton(
                            onClick = {
                                // Collapse map only — never finish Activity / send app to background.
                                onExitFullscreen.invoke()
                            },
                            modifier = Modifier
                                .size(mapFabSize),
                            containerColor = Color.White,
                            contentColor = DarkBlue,
                            shape = mapFabShape,
                        ) {
                            Icon(
                                Icons.Filled.FullscreenExit,
                                contentDescription = "Exit fullscreen",
                                modifier = Modifier.size(mapFabIcon),
                            )
                        }
                    }
                    LocateFab()
                    // Far-right: road-video on/off (Report stays centered).
                    if (tripModeEnabled && tripNavActive && onToggleTripRoadVideo != null) {
                        SmallFloatingActionButton(
                            onClick = { onToggleTripRoadVideo.invoke() },
                            modifier = Modifier
                                .size(mapFabSize)
                                .semantics {
                                    contentDescription = if (tripRoadVideoEnabled) {
                                        "Turn off road video"
                                    } else {
                                        "Turn on road video"
                                    }
                                },
                            containerColor = if (tripRoadVideoEnabled) {
                                Color(0xFFB74233)
                            } else {
                                Color.White
                            },
                            contentColor = if (tripRoadVideoEnabled) Color.White else DarkBlue,
                            shape = mapFabShape,
                        ) {
                            Icon(
                                imageVector = if (tripRoadVideoEnabled) {
                                    Icons.Filled.Videocam
                                } else {
                                    Icons.Filled.VideocamOff
                                },
                                contentDescription = null,
                                modifier = Modifier.size(mapFabIcon),
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(bottomChromeInset)
                    .padding(horizontal = 10.dp),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(mapFabGap),
                ) {
                    LocateFab()
                }
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
    onFeedbackMessage: (String) -> Unit = {},
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
            title = { Text("Delete this report?", style = CitizenType.ScreenTitle) },
            text = {
                Text(
                    "This removes the photo and report from your device only. " +
                        "It stays in the municipality system if it was already uploaded.",
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
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
                            if (ok) {
                                onReportsMutated()
                                onFeedbackMessage("Report removed from this device")
                            } else {
                                onFeedbackMessage("Couldn't delete the report. Try again.")
                            }
                        }
                    },
                ) {
                    Text("Delete", style = CitizenType.Label, color = CitizenInk.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = CitizenSpace.Lg)) {
        StatCard(
            title = "TOTAL",
            value = total.toString(),
            color = CitizenStatTotal,
            selected = statFilter == MyReportsStatFilter.TOTAL,
            onClick = { statFilter = MyReportsStatFilter.TOTAL },
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(CitizenSpace.Md))
        StatCard(
            title = PotholeReportStatus.COMPLETED.displayLabelCaps,
            value = completedCount.toString(),
            color = citizenReportStatusColor(PotholeReportStatus.COMPLETED),
            selected = statFilter == MyReportsStatFilter.COMPLETED,
            onClick = { statFilter = MyReportsStatFilter.COMPLETED },
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(CitizenSpace.Md))
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = CitizenSpace.Lg)) {
        StatCard(
            title = PotholeReportStatus.IN_PROGRESS.displayLabelCaps,
            value = inProgressCount.toString(),
            color = citizenReportStatusColor(PotholeReportStatus.IN_PROGRESS),
            selected = statFilter == MyReportsStatFilter.IN_PROGRESS,
            onClick = { statFilter = MyReportsStatFilter.IN_PROGRESS },
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(CitizenSpace.Md))
        StatCard(
            title = PotholeReportStatus.OPEN.displayLabelCaps,
            value = openCount.toString(),
            color = citizenReportStatusColor(PotholeReportStatus.OPEN),
            selected = statFilter == MyReportsStatFilter.OPEN,
            onClick = { statFilter = MyReportsStatFilter.OPEN },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(CitizenSpace.Md))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CitizenSpace.Lg),
        shape = CitizenRadius.Card,
        border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CitizenSpace.Md, vertical = CitizenSpace.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RESOLUTION RATE", style = CitizenType.Caption, color = CitizenInk.Helper)
            Spacer(modifier = Modifier.width(CitizenSpace.Md))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(CitizenSpace.Sm)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CitizenRadius.Capsule)
            )
            Spacer(modifier = Modifier.width(CitizenSpace.Sm))
            Text("$resolutionPct%", style = CitizenType.Caption, fontWeight = FontWeight.Bold, color = titleColor)
        }
    }

    Spacer(modifier = Modifier.height(CitizenSpace.Md))

    CitizenSectionHeader(
        title = "YOUR REPORTS",
        trailing = when {
            total == 0 -> "NONE YET"
            statFilter == MyReportsStatFilter.TOTAL -> "$total - NEWEST FIRST"
            else -> "${filteredReports.size} SHOWN"
        },
        modifier = Modifier.padding(horizontal = CitizenSpace.Lg),
    )
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = CitizenSpace.Lg),
        color = CitizenBorder.Secondary,
    )
    Spacer(modifier = Modifier.height(CitizenSpace.Md))

    if (myReports.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CitizenSpace.Lg),
            shape = CitizenRadius.Card,
            border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary),
            colors = CardDefaults.cardColors(containerColor = emptyCardColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = CitizenSpace.Xl, vertical = CitizenSpace.Xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = CitizenInk.Disabled,
                    modifier = Modifier.size(CitizenIcon.Fab),
                )
                Spacer(modifier = Modifier.height(CitizenSpace.Md))
                Text(
                    "You haven't reported any potholes yet.",
                    style = CitizenType.Label,
                    color = titleColor,
                )
                Text(
                    "Reports submitted while signed in will appear here.",
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
            }
        }
    } else if (filteredReports.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CitizenSpace.Lg),
            shape = CitizenRadius.Card,
            border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary),
            colors = CardDefaults.cardColors(containerColor = emptyCardColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = CitizenSpace.Xl, vertical = CitizenSpace.Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No reports in this category.",
                    style = CitizenType.Label,
                    color = titleColor,
                )
                Text(
                    "Tap another stat box to view other reports.",
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Md),
            contentPadding = PaddingValues(horizontal = CitizenSpace.Lg),
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
                                .size(CitizenIcon.Tap),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xCC1F2937), shape = CitizenRadius.Card),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete report",
                                    tint = Color.White,
                                    modifier = Modifier.size(CitizenIcon.Dense),
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
    val uiScale = rememberMapUiScale()
    val compactChrome = LocalConfiguration.current.screenWidthDp < 370
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CitizenSpace.Lg, vertical = CitizenSpace.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            tonalElevation = 1.dp,
            shape = CitizenRadius.Card,
            border = BorderStroke(CitizenBorder.Thin, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = uiScale.dp(if (compactChrome) 8f else 10f),
                    vertical = uiScale.dp(4f),
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = CitizenPrivacyCopy.BANNER_CAPS,
                    style = CitizenType.ChipCompact,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.width(uiScale.dp(if (compactChrome) 8f else 12f)))
        Text(
            "Spot it. Snap it. Submit it.",
            style = CitizenType.Tagline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compactChrome) 4.dp else 8.dp),
    ) {
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
        shape = CitizenRadius.Card,
        border = BorderStroke(
            width = CitizenBorder.Thin,
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
        Column(modifier = Modifier.padding(CitizenSpace.Md)) {
            Text(value, style = CitizenType.ScreenTitle, color = accent)
            Text(
                title,
                style = CitizenType.Caption,
                color = CitizenInk.Helper,
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
            modifier = Modifier.padding(horizontal = CitizenSpace.Lg),
            color = CitizenBorder.Secondary,
        )
        Spacer(modifier = Modifier.height(CitizenSpace.Md))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CitizenSpace.Lg),
        ) {
            Text(
                "REPORTS STAY ANONYMOUS TO OTHER USERS - ONLY YOU SEE THIS VIEW",
                style = CitizenType.Caption,
                color = CitizenInk.Muted,
            )
            Spacer(modifier = Modifier.height(CitizenSpace.Sm))
            Text(
                "CITY GRID - CIVIC REPORTING",
                style = CitizenType.Caption,
                color = CitizenInk.Muted,
            )
            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = CitizenType.Caption,
                color = CitizenInk.Muted,
            )
        }
        Spacer(modifier = Modifier.height(CitizenSpace.Md))
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
 * Map cursor / camera: when a trip route exists, pull slightly drifted GPS onto the
 * polyline so the marker stays on the road instead of over nearby buildings.
 * Progress / reroute / alerts still use the raw [gpsPinLocation].
 */
private fun Location.forTripMapDisplay(
    routePoints: List<Pair<Double, Double>>,
    enableSnap: Boolean,
    preferredAlongMeters: Double? = null,
): Location {
    if (!enableSnap || routePoints.size < 2) return this
    val accuracy = if (hasAccuracy()) accuracy else null
    val snapped = TripNavigationMatcher.snapDriverToRoute(
        routePoints = routePoints,
        driverLat = latitude,
        driverLon = longitude,
        maxSnapMeters = TripNavigationMatcher.mapSnapLimitM(accuracy),
        preferredAlongMeters = preferredAlongMeters,
    ) ?: return this
    return Location(this).apply {
        latitude = snapped.first
        longitude = snapped.second
    }
}

/**
 * Rejects stale, very coarse, or physically implausible navigation fixes.
 * Fresh GPS fixes are strongly preferred over network fixes.
 *
 * While [navigationActive], GPS accuracy limits are relaxed so the cursor keeps
 * moving when A-GPS assist is unavailable (no internet) and reported accuracy spikes.
 */
private fun shouldAcceptNavigationLocation(
    current: Location?,
    candidate: Location,
    navigationActive: Boolean = false,
): Boolean {
    if (!candidate.latitude.isFinite() || !candidate.longitude.isFinite()) return false
    val isGps = candidate.provider == LocationManager.GPS_PROVIDER
    // Some OEM GPS streams omit accuracy; still accept GPS while navigating.
    if (!candidate.hasAccuracy()) {
        if (!(navigationActive && isGps)) return false
    } else {
        // Offline / weak A-GPS often reports coarse accuracy — keep cursor alive on GPS.
        val maxGpsM = if (navigationActive) 200f else 40f
        val maxNetworkM = if (navigationActive) 80f else 25f
        if (candidate.accuracy > if (isGps) maxGpsM else maxNetworkM) return false
    }
    val candidateAgeMs = locationAgeMs(candidate)
    val maxAgeMs = if (navigationActive) 20_000L else 8_000L
    if (candidateAgeMs > maxAgeMs) return false
    current ?: return true

    val currentAgeMs = locationAgeMs(current)
    val currentIsGps = current.provider == LocationManager.GPS_PROVIDER
    // Never let network/passive replace a recent GPS fix.
    if (currentIsGps && !isGps && currentAgeMs < 12_000L) return false
    if (candidate.time < current.time && currentAgeMs < 5_000L) {
        // Prefer elapsedRealtime when both have it.
        val candNanos = candidate.elapsedRealtimeNanos
        val curNanos = current.elapsedRealtimeNanos
        if (candNanos <= 0L || curNanos <= 0L || candNanos < curNanos) return false
    }

    val elapsedSeconds = (candidateAgeDeltaMs(current, candidate).coerceAtLeast(250L) / 1_000.0)
    val distanceMeters = current.distanceTo(candidate).toDouble()
    val accuracyPad = (if (candidate.hasAccuracy()) candidate.accuracy else 40f) +
        (if (current.hasAccuracy()) current.accuracy.coerceAtLeast(0f) else 0f)
    val speedPad = if (navigationActive) 70.0 else 55.0
    val plausibleJumpMeters = 20.0 + accuracyPad + elapsedSeconds * speedPad
    if (distanceMeters > plausibleJumpMeters) return false

    // Prefer continuous satellite updates; accept coarser fixes only if GPS went stale.
    if (isGps) return true
    return !candidate.hasAccuracy() ||
        candidate.accuracy <= 25f ||
        currentAgeMs >= 6_000L
}

/** Prefer [Location.getElapsedRealtimeNanos] so clock skew / offline NTP does not reject fixes. */
private fun locationAgeMs(location: Location): Long {
    val nanos = location.elapsedRealtimeNanos
    if (nanos > 0L) {
        return ((SystemClock.elapsedRealtimeNanos() - nanos) / 1_000_000L).coerceAtLeast(0L)
    }
    return (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
}

private fun candidateAgeDeltaMs(older: Location, newer: Location): Long {
    val a = older.elapsedRealtimeNanos
    val b = newer.elapsedRealtimeNanos
    if (a > 0L && b > 0L) {
        return ((b - a) / 1_000_000L).coerceAtLeast(0L)
    }
    return (newer.time - older.time).coerceAtLeast(0L)
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
                timeoutMs = (timeoutMs * 0.4).toLong().coerceIn(700L, 1_200L),
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
        var best = fetchHighAccuracyGpsLocation(context, timeoutMs = 3_000L)
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



