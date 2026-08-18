@file:Suppress("SpellCheckingInspection")

package com.example.potholereport.ui.emergency

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PregnantWoman
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.potholereport.data.CitizenRuntimePermissions
import com.example.potholereport.data.SosAssistNavRequest
import com.example.potholereport.data.SosNearbyRepository
import com.example.potholereport.data.TripSessionRepository
import com.example.potholereport.data.remote.NearbyOsmPoi
import com.example.potholereport.data.remote.NearbyPoiKind
import com.example.potholereport.data.remote.formatNearbyDistance
import com.example.potholereport.data.remote.formatNearbyEta
import com.example.potholereport.ui.theme.CitizenBorder
import com.example.potholereport.ui.theme.CitizenHeader
import com.example.potholereport.ui.theme.CitizenRadius
import com.example.potholereport.ui.theme.PotholeReportTheme
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import android.os.Looper

/** While SOS is foreground: sample GPS this often. */
private const val SOS_GPS_SAMPLE_MS = 40_000L

/** Only re-geocode city/country after this much movement. */
private const val SOS_GECODE_MIN_MOVE_M = 2_000.0

private data class HelplineItem(
    val title: String,
    val subtitle: String,
    /** Null = no dial row (e.g. roadside shop without a phone, or no demo fallback). */
    val number: String? = null,
    val placeLat: Double? = null,
    val placeLon: Double? = null,
    val placeLabel: String? = null,
    val keepTitleSingleLine: Boolean = false,
    val icon: @Composable () -> Unit,
)

private data class HelplineSection(
    val title: String,
    val accent: Color,
    val items: List<HelplineItem>
)

private data class LocationUiState(
    val city: String? = null,
    val countryName: String = "India",
    val countryCode: String = "IN",
    val statusText: String = "LOCATION UNAVAILABLE - USING DEFAULT",
    val hasPermission: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Composable
fun EmergencyScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var locationState by remember {
        mutableStateOf(LocationUiState(hasPermission = hasLocationPermission(context)))
    }
    var swipeEligible by remember { mutableStateOf(false) }
    var swipeDistance by remember { mutableStateOf(0f) }
    var pendingNavigatePoi by remember { mutableStateOf<NearbyOsmPoi?>(null) }
    /** Bumped on manual “Use my location” to force an immediate nearby refetch. */
    var roadsideForceEpoch by remember { mutableIntStateOf(0) }
    // Medical + Roadside are kept warm by [SosNearbyRepository] from Home GPS —
    // SOS opens onto the latest cache instead of waiting for a fresh Overpass round-trip.
    val sosNearby by SosNearbyRepository.state.collectAsState()
    val nearestMechanic = sosNearby.mechanic
    val nearestPuncture = sosNearby.puncture
    val nearestGeneralHospital = sosNearby.generalHospital
    val nearestChildrenHospital = sosNearby.childrenHospital
    val nearestMaternityHospital = sosNearby.maternityHospital
    val roadsideLookupDone = sosNearby.lookupDone || sosNearby.mechanic != null || sosNearby.puncture != null
    val medicalLookupDone = sosNearby.lookupDone ||
        sosNearby.generalHospital != null ||
        sosNearby.childrenHospital != null ||
        sosNearby.maternityHospital != null

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationState = locationState.copy(
            hasPermission = granted,
            statusText = if (granted) {
                "LOCATION DETECTED - SHOWING LOCAL NUMBERS"
            } else {
                "LOCATION PERMISSION DENIED - USING DEFAULT"
            }
        )
    }

    // Live GPS only while SOS is open AND the app is resumed (not minimized).
    DisposableEffect(lifecycleOwner, locationState.hasPermission) {
        if (!locationState.hasPermission) {
            return@DisposableEffect onDispose { }
        }
        val locationManager = context.getSystemService(LocationManager::class.java)
            ?: return@DisposableEffect onDispose { }
        val listener = LocationListenerCompat { location ->
            applyForegroundLocationFix(
                context = context,
                location = location,
                current = locationState,
                onUpdate = { locationState = it },
            )
            SosNearbyRepository.refreshIfNeeded(location.latitude, location.longitude)
        }
        @SuppressLint("MissingPermission")
        fun startUpdates() {
            runCatching {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        SOS_GPS_SAMPLE_MS,
                        25f,
                        listener,
                        Looper.getMainLooper(),
                    )
                }
            }
            runCatching {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        SOS_GPS_SAMPLE_MS,
                        25f,
                        listener,
                        Looper.getMainLooper(),
                    )
                }
            }
        }
        fun stopUpdates() {
            runCatching { locationManager.removeUpdates(listener) }
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startUpdates()
                Lifecycle.Event.ON_PAUSE -> stopUpdates()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startUpdates()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopUpdates()
        }
    }

    // Seed city/country helplines + nudge the shared SOS nearby cache.
    LaunchedEffect(locationState.hasPermission, roadsideForceEpoch, lifecycleOwner) {
        if (!locationState.hasPermission) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val seeded = withContext(Dispatchers.IO) { resolveLocationState(context) }
            locationState = seeded
            val seedLat = seeded.latitude
            val seedLon = seeded.longitude
            if (seedLat != null && seedLon != null) {
                SosNearbyRepository.refreshIfNeeded(
                    seedLat,
                    seedLon,
                    force = roadsideForceEpoch > 0 || !sosNearby.lookupDone,
                )
            }
            while (isActive) {
                delay(SOS_GPS_SAMPLE_MS)
                val fix = withContext(Dispatchers.IO) { readBestLastKnownLocation(context) }
                if (fix != null) {
                    applyForegroundLocationFix(
                        context = context,
                        location = fix,
                        current = locationState,
                        onUpdate = { locationState = it },
                    )
                    SosNearbyRepository.refreshIfNeeded(fix.latitude, fix.longitude)
                } else {
                    val lat = locationState.latitude
                    val lon = locationState.longitude
                    if (lat != null && lon != null) {
                        SosNearbyRepository.refreshIfNeeded(lat, lon)
                    }
                }
            }
        }
    }

    val emergencySections = getHelplineSections(
        countryCode = locationState.countryCode,
        city = locationState.city,
        nearestMechanic = nearestMechanic,
        nearestPuncture = nearestPuncture,
        nearestGeneralHospital = nearestGeneralHospital,
        nearestChildrenHospital = nearestChildrenHospital,
        nearestMaternityHospital = nearestMaternityHospital,
        roadsideLookupDone = roadsideLookupDone,
        medicalLookupDone = medicalLookupDone,
    )

    fun goNavigateToPoi(poi: NearbyOsmPoi, mode: SosAssistNavRequest.Mode) {
        SosAssistNavRequest.offer(
            label = poi.name,
            latitude = poi.latitude,
            longitude = poi.longitude,
            mode = mode,
        )
        pendingNavigatePoi = null
        onClose()
    }

    fun onNavigatePlace(poi: NearbyOsmPoi) {
        if (TripSessionRepository.isNavigating()) {
            pendingNavigatePoi = poi
        } else {
            goNavigateToPoi(poi, SosAssistNavRequest.Mode.REPLACE_AS_DESTINATION)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(density) {
                val edgePx = with(density) { 24.dp.toPx() }
                detectHorizontalDragGestures(
                    onDragStart = { start ->
                        swipeEligible = start.x <= edgePx
                        swipeDistance = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (swipeEligible && dragAmount > 0) {
                            swipeDistance += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (swipeEligible && swipeDistance > 140f) {
                            onClose()
                        }
                        swipeEligible = false
                        swipeDistance = 0f
                    },
                    onDragCancel = {
                        swipeEligible = false
                        swipeDistance = 0f
                    }
                )
            }
            .background(Color(0xFFB11B1B))
            .padding(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CitizenRadius.Card,
            color = Color(0xFFF0F0EE),
            border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB11B1B))
                        .padding(CitizenHeader.Padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EMERGENCY\nHELPLINES",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            lineHeight = 16.sp
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                CountryInfoCard(
                    countryName = locationState.countryName,
                    cityName = locationState.city,
                    statusText = locationState.statusText,
                    onUseLocationClick = {
                        if (locationState.hasPermission) {
                            locationState = locationState.copy(
                                statusText = "REFRESHING LOCATION..."
                            )
                            locationState = resolveLocationState(context)
                            val lat = locationState.latitude
                            val lon = locationState.longitude
                            if (lat != null && lon != null) {
                                SosNearbyRepository.refreshIfNeeded(lat, lon, force = true)
                            }
                            roadsideForceEpoch++
                        } else {
                            val extra = CitizenRuntimePermissions.forLocationAndTrip(context)
                            if (extra.isNotEmpty()) {
                                permissionLauncher.launch(extra)
                            }
                        }
                    }
                )

                emergencySections.forEach { section ->
                    HelplineSectionCard(
                        section = section,
                        onNavigatePlace = { lat, lon, label ->
                            val poi = listOfNotNull(
                                nearestMechanic,
                                nearestPuncture,
                                nearestGeneralHospital,
                                nearestChildrenHospital,
                                nearestMaternityHospital,
                            ).firstOrNull {
                                it.latitude == lat && it.longitude == lon
                            }
                            if (poi != null) {
                                onNavigatePlace(poi)
                            } else {
                                // Synthetic navigate for labeled coords.
                                if (TripSessionRepository.isNavigating()) {
                                    pendingNavigatePoi = NearbyOsmPoi(
                                        id = "manual",
                                        name = label,
                                        kind = NearbyPoiKind.MECHANIC,
                                        latitude = lat,
                                        longitude = lon,
                                    )
                                } else {
                                    SosAssistNavRequest.offer(
                                        label = label,
                                        latitude = lat,
                                        longitude = lon,
                                        mode = SosAssistNavRequest.Mode.REPLACE_AS_DESTINATION,
                                    )
                                    onClose()
                                }
                            }
                        },
                    )
                }

                DisclaimerCard()
            }
        }
    }

    pendingNavigatePoi?.let { poi ->
        AlertDialog(
            onDismissRequest = { pendingNavigatePoi = null },
            title = { Text("Active trip") },
            text = {
                Text(
                    "You’re already on a trip. Cancel it and navigate to ${poi.name}, " +
                        "or add this stop to the current navigation?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        goNavigateToPoi(poi, SosAssistNavRequest.Mode.REPLACE_AS_DESTINATION)
                    },
                ) {
                    Text("Cancel trip & go")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            goNavigateToPoi(poi, SosAssistNavRequest.Mode.ADD_AS_STOP)
                        },
                    ) {
                        Text("Add as stop")
                    }
                    TextButton(onClick = { pendingNavigatePoi = null }) {
                        Text("Keep trip")
                    }
                }
            },
        )
    }
}

private fun getHelplineSections(
    countryCode: String,
    city: String?,
    nearestMechanic: NearbyOsmPoi? = null,
    nearestPuncture: NearbyOsmPoi? = null,
    nearestGeneralHospital: NearbyOsmPoi? = null,
    nearestChildrenHospital: NearbyOsmPoi? = null,
    nearestMaternityHospital: NearbyOsmPoi? = null,
    roadsideLookupDone: Boolean = false,
    medicalLookupDone: Boolean = false,
): List<HelplineSection> {
    val regionSubtitle = city?.let { "City Police ($it)" } ?: "City Police"
    fun medicalHospitalItems(): List<HelplineItem> = listOf(
        roadsideAssistItem(
            title = "GENERAL HOSPITAL",
            roleHint = "Nearest emergency hospital",
            poi = nearestGeneralHospital,
            lookupDone = medicalLookupDone,
            emptyHint = "No nearby hospital found",
        ) {
            Icon(Icons.Outlined.LocalHospital, contentDescription = null)
        },
        roadsideAssistItem(
            title = "CHILDREN'S HOSPITAL",
            roleHint = "Nearest paediatric care",
            poi = nearestChildrenHospital,
            lookupDone = medicalLookupDone,
            emptyHint = "No nearby children's hospital found",
        ) {
            Icon(Icons.Outlined.ChildCare, contentDescription = null)
        },
        roadsideAssistItem(
            title = "MATERNITY HOSPITAL",
            roleHint = "Nearest pregnancy / labour care",
            poi = nearestMaternityHospital,
            lookupDone = medicalLookupDone,
            emptyHint = "No nearby maternity hospital found",
        ) {
            Icon(Icons.Outlined.PregnantWoman, contentDescription = null)
        },
    )
    return when (countryCode.uppercase(Locale.ROOT)) {
        "US" -> listOf(
            HelplineSection(
                title = "EMERGENCY",
                accent = Color(0xFFB11B1B),
                items = listOf(
                    HelplineItem("NATIONAL EMERGENCY", "Police • Fire • Ambulance", "911") {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                    },
                    HelplineItem("POLICE", "Local emergency dispatch", "911") {
                        Icon(Icons.Outlined.LocalPolice, contentDescription = null)
                    }
                )
            ),
            HelplineSection(
                title = "MEDICAL",
                accent = Color(0xFFD8342A),
                items = listOf(
                    HelplineItem("AMBULANCE", "Emergency medical services", "911") {
                        Icon(Icons.Outlined.HealthAndSafety, contentDescription = null)
                    },
                ) + medicalHospitalItems(),
            ),
            HelplineSection(
                title = "FIRE & ROAD",
                accent = Color(0xFFF39800),
                items = listOf(
                    HelplineItem("FIRE & RESCUE", "Fire emergencies", "911") {
                        Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null)
                    },
                    HelplineItem("HIGHWAY SAFETY", "Road and traffic updates", "511") {
                        Icon(Icons.Outlined.DirectionsCar, contentDescription = null)
                    }
                )
            )
        )
        "GB", "UK" -> listOf(
            HelplineSection(
                title = "EMERGENCY",
                accent = Color(0xFFB11B1B),
                items = listOf(
                    HelplineItem("NATIONAL EMERGENCY", "Police • Fire • Ambulance", "999") {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                    },
                    HelplineItem("POLICE (NON-EMERGENCY)", "For non-urgent police help", "101") {
                        Icon(Icons.Outlined.LocalPolice, contentDescription = null)
                    }
                )
            ),
            HelplineSection(
                title = "MEDICAL",
                accent = Color(0xFFD8342A),
                items = listOf(
                    HelplineItem("NHS NON-EMERGENCY", "Medical advice", "111") {
                        Icon(Icons.Outlined.HealthAndSafety, contentDescription = null)
                    },
                ) + medicalHospitalItems(),
            ),
            HelplineSection(
                title = "FIRE & ROAD",
                accent = Color(0xFFF39800),
                items = listOf(
                    HelplineItem("FIRE & RESCUE", "Fire emergencies", "999") {
                        Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null)
                    }
                )
            )
        )
        else -> listOf(
            HelplineSection(
                title = "EMERGENCY",
                accent = Color(0xFFB11B1B),
                items = listOf(
                    HelplineItem("NATIONAL EMERGENCY", "Police • Fire • Ambulance", "112") {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                    },
                    HelplineItem("POLICE", regionSubtitle, "100") {
                        Icon(Icons.Outlined.LocalPolice, contentDescription = null)
                    }
                )
            ),
            HelplineSection(
                title = "MEDICAL",
                accent = Color(0xFFD8342A),
                items = listOf(
                    HelplineItem("AMBULANCE", "Free state-run service", "102") {
                        Icon(Icons.Outlined.HealthAndSafety, contentDescription = null)
                    },
                    HelplineItem("EMERGENCY MEDICAL", "Arogya Vahaka • 24x7", "108") {
                        Icon(Icons.Outlined.HealthAndSafety, contentDescription = null)
                    },
                ) + medicalHospitalItems(),
            ),
            HelplineSection(
                title = "FIRE & ROAD",
                accent = Color(0xFFF39800),
                items = listOf(
                    HelplineItem("FIRE & RESCUE", "State Fire Service", "101") {
                        Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null)
                    },
                    HelplineItem("HIGHWAY HELPLINE", "NHAI • Road incidents", "1033") {
                        Icon(Icons.Outlined.DirectionsCar, contentDescription = null)
                    }
                )
            ),
            HelplineSection(
                title = "ROADSIDE ASSIST",
                accent = Color(0xFF121212),
                items = listOf(
                    roadsideAssistItem(
                        title = "BREAKDOWN HELP",
                        roleHint = "Mechanic • breakdown",
                        poi = nearestMechanic,
                        lookupDone = roadsideLookupDone,
                    ) {
                        Icon(Icons.Outlined.Build, contentDescription = null)
                    },
                    roadsideAssistItem(
                        title = "TWO-WHEELER ASSIST",
                        roleHint = "Puncture • tube / tubeless",
                        poi = nearestPuncture,
                        lookupDone = roadsideLookupDone,
                    ) {
                        Icon(Icons.Outlined.Build, contentDescription = null)
                    },
                )
            )
        )
    }
}

private fun roadsideAssistItem(
    title: String,
    roleHint: String,
    poi: NearbyOsmPoi?,
    lookupDone: Boolean,
    emptyHint: String = "No nearby shop found",
    icon: @Composable () -> Unit,
): HelplineItem {
    if (poi == null) {
        val subtitle = when {
            !lookupDone -> "Looking up nearby…"
            else -> emptyHint
        }
        return HelplineItem(
            title = title,
            subtitle = "$roleHint · $subtitle",
            number = null,
            keepTitleSingleLine = true,
            icon = icon,
        )
    }
    val dist = formatNearbyDistance(
        poi.carDistanceMeters
            ?: poi.bikeDistanceMeters
            ?: poi.straightLineMeters,
    )
    val eta = formatNearbyEta(poi.carEtaSeconds ?: poi.bikeEtaSeconds)
    val subtitle = "${poi.name} · $dist · $eta"
    return HelplineItem(
        title = title,
        subtitle = subtitle,
        number = poi.phone?.takeIf { it.isNotBlank() },
        placeLat = poi.latitude,
        placeLon = poi.longitude,
        placeLabel = poi.name,
        keepTitleSingleLine = true,
        icon = icon,
    )
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@SuppressLint("MissingPermission")
private fun readBestLastKnownLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val locationManager = context.getSystemService(LocationManager::class.java) ?: return null
    val candidates = listOfNotNull(
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER),
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
        locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER),
    )
    return candidates.minWithOrNull(
        compareBy<Location> { if (it.hasAccuracy()) it.accuracy else Float.MAX_VALUE }
            .thenByDescending { it.time },
    )
}

/**
 * Update SOS coordinates from a live fix. Re-geocode city only after meaningful movement
 * so we do not hammer Geocoder on every GPS tick.
 */
private fun applyForegroundLocationFix(
    context: Context,
    location: Location,
    current: LocationUiState,
    onUpdate: (LocationUiState) -> Unit,
) {
    val prevLat = current.latitude
    val prevLon = current.longitude
    val movedM = if (prevLat != null && prevLon != null) {
        haversineMeters(prevLat, prevLon, location.latitude, location.longitude)
    } else {
        Double.POSITIVE_INFINITY
    }
    if (movedM < 20.0 && prevLat != null) return
    if (movedM >= SOS_GECODE_MIN_MOVE_M || current.city.isNullOrBlank()) {
        onUpdate(geocodeLocation(context, location))
    } else {
        onUpdate(
            current.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                statusText = "LOCATION DETECTED - SHOWING LOCAL NUMBERS",
                hasPermission = true,
            ),
        )
    }
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val p1 = Math.toRadians(lat1)
    val p2 = Math.toRadians(lat2)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(p1) * cos(p2) * sin(dLon / 2) * sin(dLon / 2)
    return 2.0 * r * atan2(sqrt(h), sqrt(1 - h))
}

@SuppressLint("MissingPermission")
private fun resolveLocationState(context: Context): LocationUiState {
    val location = readBestLastKnownLocation(context)
        ?: return LocationUiState(
            statusText = "LOCATION NOT FOUND - USING DEFAULT",
            hasPermission = true,
        )
    return geocodeLocation(context, location)
}

private fun geocodeLocation(context: Context, location: Location): LocationUiState {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()

        val countryCode = address?.countryCode ?: "IN"
        val countryName = address?.countryName ?: "India"
        val city = address?.locality ?: address?.subAdminArea
        LocationUiState(
            city = city,
            countryName = countryName,
            countryCode = countryCode,
            statusText = "LOCATION DETECTED - SHOWING LOCAL NUMBERS",
            hasPermission = true,
            latitude = location.latitude,
            longitude = location.longitude,
        )
    } catch (_: Exception) {
        LocationUiState(
            statusText = "LOCATION LOOKUP FAILED - USING DEFAULT",
            hasPermission = true,
            latitude = location.latitude,
            longitude = location.longitude,
        )
    }
}

@Composable
private fun CountryInfoCard(
    countryName: String,
    cityName: String?,
    statusText: String,
    onUseLocationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        shape = CitizenRadius.Card,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F6)),
        border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = Color(0xFF0E4AA2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SHOWING NUMBERS FOR ${countryName.uppercase(Locale.ROOT)}",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1D2A3B),
                    fontSize = 13.sp
                )
            }
            if (!cityName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DETECTED CITY: ${cityName.uppercase(Locale.ROOT)}",
                    color = Color(0xFF444444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusText,
                color = Color(0xFF666666),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary),
                shape = CitizenRadius.Card,
                color = Color.White,
                contentColor = Color(0xFF1D1D1D),
                modifier = Modifier.clickable { onUseLocationClick() }
            ) {
                Text(
                    text = "USE MY LOCATION",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color(0xFF1D1D1D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HelplineSectionCard(
    section: HelplineSection,
    onNavigatePlace: (lat: Double, lon: Double, label: String) -> Unit = { _, _, _ -> },
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        shape = CitizenRadius.Card,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F6)),
        border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(section.accent)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = section.title,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            section.items.forEachIndexed { index, item ->
                HelplineRow(item = item, onNavigatePlace = onNavigatePlace)
                if (index < section.items.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFD6D6D6))
                    )
                }
            }
        }
    }
}

@Composable
private fun HelplineRow(
    item: HelplineItem,
    onNavigatePlace: (lat: Double, lon: Double, label: String) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    val hasPlace = item.placeLat != null && item.placeLon != null &&
        item.placeLat.isFinite() && item.placeLon.isFinite()
    val hasNumber = !item.number.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    hasNumber -> Modifier.clickable { dialNumber(context, item.number!!) }
                    hasPlace -> Modifier.clickable {
                        onNavigatePlace(
                            item.placeLat!!,
                            item.placeLon!!,
                            item.placeLabel ?: item.title,
                        )
                    }
                    else -> Modifier
                },
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = CitizenRadius.Card,
            color = Color(0xFFE4E4E1),
            border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary)
        ) {
            Box(contentAlignment = Alignment.Center) {
                item.icon()
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Black,
                fontSize = if (item.keepTitleSingleLine) 12.sp else 14.sp,
                color = Color(0xFF202020),
                maxLines = 1,
                softWrap = !item.keepTitleSingleLine,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                fontSize = 10.sp,
                color = Color(0xFF6D6D6D),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (hasNumber) {
            Text(
                text = formatHelplineNumber(item.number!!),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
                color = Color(0xFF0E4AA2),
                fontSize = if (item.number!!.length > 8) 14.sp else 20.sp,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = "Call ${item.title}",
                tint = Color(0xFF0E4AA2),
                modifier = Modifier.size(16.dp)
            )
        }
        if (hasPlace) {
            if (hasNumber) Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    onNavigatePlace(
                        item.placeLat!!,
                        item.placeLon!!,
                        item.placeLabel ?: item.title,
                    )
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Navigation,
                    contentDescription = "Navigate to ${item.placeLabel ?: item.title}",
                    tint = Color(0xFF0E4AA2),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun formatHelplineNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() || it == '+' }
    return when {
        digits.startsWith("1800") && digits.length >= 10 ->
            "${digits.take(4)}-${digits.drop(4).take(3)}-${digits.drop(7)}"
        digits.length in 3..4 -> digits
        digits.length == 10 -> "${digits.take(3)} ${digits.drop(3).take(3)} ${digits.drop(6)}"
        else -> raw
    }
}

private fun dialNumber(context: Context, number: String) {
    val tel = number.filter { it.isDigit() || it == '+' }
    if (tel.isBlank()) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun DisclaimerCard() {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        shape = CitizenRadius.Card,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F6)),
        border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "Location is used to choose emergency numbers, nearby hospitals, and roadside shops. " +
                    "Medical and Roadside Assist keep updating from your location while the app is open — " +
                    "you do not need to open SOS first. Places refresh if you move ~400 m or after ~2.5 minutes. " +
                    "Hospital phones appear only when listed in map data — you can still navigate to the place. " +
                    "Updates pause when the app is minimized. " +
                    "Verify regional numbers before use while travelling.",
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = Color(0xFF333333),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Government helpline sources: india.gov.in/directory/helpline · " +
                    "mha.gov.in (ERSS/112). Roadside Assist shows nearby OSM mechanic / puncture " +
                    "shops with distance and time; phone numbers only when listed in OSM.",
                fontSize = 9.sp,
                lineHeight = 13.sp,
                color = Color(0xFF5A5A5A),
            )
            Text(
                text = "Open india.gov.in helpline directory",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFB74233),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable {
                        uriHandler.openUri("https://www.india.gov.in/directory/helpline")
                    },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmergencyScreenPreview() {
    PotholeReportTheme {
        EmergencyScreen(onClose = {})
    }
}
