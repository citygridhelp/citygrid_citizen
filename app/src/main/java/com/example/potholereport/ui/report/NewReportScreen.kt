@file:Suppress("SpellCheckingInspection")

package com.example.potholereport.ui.report

import com.example.potholereport.ui.theme.CitizenBorder
import com.example.potholereport.ui.theme.CitizenInk
import com.example.potholereport.ui.theme.CitizenPrivacyCopy
import com.example.potholereport.ui.theme.CitizenSpace
import com.example.potholereport.ui.theme.CitizenType
import com.example.potholereport.ui.theme.CitizenHeader
import com.example.potholereport.ui.theme.CitizenRadius
import com.example.potholereport.ui.theme.CitizenSelection
import com.example.potholereport.ui.theme.CitizenIcon
import com.example.potholereport.ui.neighbourhood.NeighbourhoodPalette

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.potholereport.data.AddReportResult
import com.example.potholereport.data.CitizenGps
import com.example.potholereport.data.CitizenRuntimePermissions
import com.example.potholereport.data.BengaluruMunicipalRouting
import com.example.potholereport.data.CityMetroLocation
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.PotholeDuplicateGuard
import com.example.potholereport.data.PotholePosition
import com.example.potholereport.data.CivicIssueType
import com.example.potholereport.data.IncidentTimeOfDay
import com.example.potholereport.data.ManholeCondition
import com.example.potholereport.data.PotholeSeverity
import com.example.potholereport.data.RecentReportsRepository
import com.example.potholereport.data.TrafficFacingMode
import com.example.potholereport.data.remote.ReportSyncRepository
import com.example.potholereport.data.remote.SupabaseClientProvider
import com.example.potholereport.ml.PhotoCaptureKind
import com.example.potholereport.ml.PhotoValidationResult
import com.example.potholereport.ml.PotholePhotoValidator
import com.example.potholereport.ml.PotholeRiskAnalyzer
import com.example.potholereport.ml.PotholeRiskInsight
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AccentRed = Color(0xFFB74233)
private val DarkBlue = Color(0xFF1A1A2E)
private val OrangeSelect = Color(0xFFF58220)
private val GrayDisabled = CitizenInk.Disabled

private const val GPS_SUBMIT_WARN_ACCURACY_METERS = 50f
private const val GPS_SUBMIT_BLOCK_ACCURACY_METERS = 100f

@Composable
private fun SectionHelper(text: String) {
    Text(
        text,
        style = CitizenType.Helper,
        color = CitizenInk.Helper,
    )
}

@Composable
fun NewReportScreen(
    reportCityKey: String,
    reporterUserId: String,
    onClose: () -> Unit,
    onReportPersisted: () -> Unit = {},
    onSyncMessage: (String) -> Unit = {},
    submittedWhileSignedIn: Boolean = false,
    issueType: CivicIssueType = CivicIssueType.POTHOLE,
    /** Prefill close-up from trip road still (#32 Phase 6); validated on open. */
    initialCloseUpUri: Uri? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var closeUpUri by remember { mutableStateOf<Uri?>(null) }
    var wideUri by remember { mutableStateOf<Uri?>(null) }
    var closeUpVerified by remember { mutableStateOf(false) }
    var wideVerified by remember { mutableStateOf(false) }
    var pendingWideUri by remember { mutableStateOf<Uri?>(null) }
    var showWideLaneConfirm by remember { mutableStateOf(false) }
    var photoValidating by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var photoValidationTitle by remember { mutableStateOf<String?>(null) }
    var photoValidationMessage by remember { mutableStateOf<String?>(null) }
    var duplicateBlockingReport by remember { mutableStateOf<PersistedPotholeReport?>(null) }
    var showDuplicateDialog by remember { mutableStateOf(false) }

    var selectedSeverity by remember { mutableStateOf(PotholeSeverity.MODERATE) }
    var selectedManholeCondition by remember { mutableStateOf(ManholeCondition.OPEN) }
    var selectedIncidentTime by remember { mutableStateOf(IncidentTimeOfDay.now()) }
    var selectedPosition by remember { mutableStateOf(PotholePosition.MIDDLE) }
    var aiRiskInsight by remember { mutableStateOf<PotholeRiskInsight?>(null) }

    var note by remember { mutableStateOf("") }
    val noteMax = 240

    var deviceLat by remember { mutableStateOf<Double?>(null) }
    var deviceLng by remember { mutableStateOf<Double?>(null) }
    var deviceLocationAccuracyM by remember { mutableStateOf<Float?>(null) }
    var deviceBearingDeg by remember { mutableStateOf<Float?>(null) }
    var trafficFacing by remember { mutableStateOf(TrafficFacingMode.UNKNOWN) }
    var manualCoordLat by remember { mutableStateOf<Double?>(null) }
    var manualCoordLng by remember { mutableStateOf<Double?>(null) }
    var useManualLocation by remember { mutableStateOf(false) }

    var mapsUrlInput by remember { mutableStateOf("") }
    var lngFieldInput by remember { mutableStateOf("") }

    var locationLoading by remember { mutableStateOf(false) }
    var locationBlockedReason by remember { mutableStateOf<String?>(null) }

    var photoChoiceTarget by remember { mutableIntStateOf(1) }

    var showEnableGpsDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    fun locationPermitted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun refreshGpsLocation() {
        if (!locationPermitted()) {
            locationBlockedReason = "PERMISSION"
            deviceLat = null
            deviceLng = null
            deviceLocationAccuracyM = null
            deviceBearingDeg = null
            return
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gpsOn) {
            locationBlockedReason = "GPS_OFF"
            deviceLat = null
            deviceLng = null
            deviceLocationAccuracyM = null
            deviceBearingDeg = null
            return
        }
        scope.launch {
            locationLoading = true
            if (!useManualLocation) locationBlockedReason = null
            val loc = withContext(Dispatchers.Default) {
                CitizenGps.fetchForReport(context)
            }
            locationLoading = false
            if (loc != null) {
                deviceLat = loc.latitude
                deviceLng = loc.longitude
                deviceLocationAccuracyM = loc.accuracy.takeIf { it > 0f }
                deviceBearingDeg = loc.bearing.takeIf { loc.hasBearing() }
                if (!useManualLocation) locationBlockedReason = null
            } else if (!useManualLocation) {
                locationBlockedReason = "NO_FIX"
                deviceLat = null
                deviceLng = null
                deviceLocationAccuracyM = null
                deviceBearingDeg = null
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            showPermissionRationale = false
            refreshGpsLocation()
        } else {
            locationBlockedReason = "PERMISSION"
            showPermissionRationale = true
        }
    }

    LaunchedEffect(Unit) {
        if (locationPermitted()) {
            refreshGpsLocation()
        } else {
            locationBlockedReason = "PERMISSION"
        }
    }

    val gatePhotos = issueType.needsPotholePhotoGate
    val usesLaneCapture = issueType.usesLaneCapture
    val requiresWideShot = issueType.requiresWideShot
    val strayForm = issueType.usesIncidentTimeForm

    // Trip road still → close-up (#32 Phase 6).
    LaunchedEffect(initialCloseUpUri, issueType) {
        val prefill = initialCloseUpUri ?: return@LaunchedEffect
        if (!gatePhotos) {
            closeUpUri = prefill
            closeUpVerified = true
            wideUri = null
            wideVerified = false
            aiRiskInsight = null
            return@LaunchedEffect
        }
        photoValidating = true
        val result = withContext(Dispatchers.Default) {
            PotholePhotoValidator.validate(
                context = context,
                uri = prefill,
                kind = PhotoCaptureKind.CLOSE_UP,
                closeUpUri = null,
            )
        }
        photoValidating = false
        when (result) {
            is PhotoValidationResult.Accepted -> {
                val insight = withContext(Dispatchers.Default) {
                    PotholeRiskAnalyzer.analyze(
                        context = context,
                        closeUpUri = prefill,
                        position = selectedPosition,
                        userSeverity = selectedSeverity,
                    )
                }
                closeUpUri = prefill
                closeUpVerified = true
                wideUri = null
                wideVerified = false
                aiRiskInsight = insight
                insight?.let { selectedSeverity = it.suggestedSeverity }
            }
            is PhotoValidationResult.Rejected -> {
                photoValidationTitle = result.title
                photoValidationMessage = result.message
                closeUpUri = null
                closeUpVerified = false
                aiRiskInsight = null
            }
        }
    }

    // Re-run the risk analyzer whenever the user changes the lane (left /
    // middle / right). The analyzer constrains its dark-blob search to that
    // lane, so the suggested width / depth / severity numbers update.
    LaunchedEffect(selectedPosition, closeUpVerified, issueType) {
        val captured = closeUpUri ?: return@LaunchedEffect
        if (!closeUpVerified || !gatePhotos) return@LaunchedEffect
        val insight = withContext(Dispatchers.Default) {
            PotholeRiskAnalyzer.analyze(
                context = context,
                closeUpUri = captured,
                position = selectedPosition,
                userSeverity = selectedSeverity,
            )
        }
        aiRiskInsight = insight
    }

    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = cameraPhotoUri
        if (!success || capturedUri == null) return@rememberLauncherForActivityResult
        val kind = if (photoChoiceTarget == 1) PhotoCaptureKind.CLOSE_UP else PhotoCaptureKind.WIDE_ROAD
        photoValidating = true
        scope.launch {
            if (!gatePhotos) {
                photoValidating = false
                if (kind == PhotoCaptureKind.CLOSE_UP) {
                    closeUpUri = capturedUri
                    closeUpVerified = true
                    wideUri = null
                    wideVerified = false
                    aiRiskInsight = null
                } else if (usesLaneCapture) {
                    pendingWideUri = capturedUri
                    showWideLaneConfirm = true
                } else {
                    wideUri = capturedUri
                    wideVerified = true
                    pendingWideUri = null
                    showWideLaneConfirm = false
                }
                return@launch
            }
            val result = withContext(Dispatchers.Default) {
                PotholePhotoValidator.validate(
                    context = context,
                    uri = capturedUri,
                    kind = kind,
                    closeUpUri = if (kind == PhotoCaptureKind.WIDE_ROAD) closeUpUri else null,
                )
            }
            photoValidating = false
            when (result) {
                is PhotoValidationResult.Accepted -> {
                    if (kind == PhotoCaptureKind.CLOSE_UP) {
                        val insight = withContext(Dispatchers.Default) {
                            PotholeRiskAnalyzer.analyze(
                                context = context,
                                closeUpUri = capturedUri,
                                position = selectedPosition,
                                userSeverity = selectedSeverity,
                            )
                        }
                        closeUpUri = capturedUri
                        closeUpVerified = true
                        wideUri = null
                        wideVerified = false
                        aiRiskInsight = insight
                        insight?.let { selectedSeverity = it.suggestedSeverity }
                    } else {
                        pendingWideUri = capturedUri
                        showWideLaneConfirm = true
                    }
                }
                is PhotoValidationResult.Rejected -> {
                    photoValidationTitle = result.title
                    photoValidationMessage = result.message
                    if (kind == PhotoCaptureKind.CLOSE_UP) {
                        closeUpUri = null
                        closeUpVerified = false
                        aiRiskInsight = null
                    } else {
                        wideUri = null
                        wideVerified = false
                    }
                }
            }
        }
    }

    fun startCameraCapture() {
        val file = createImageFile(context)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        cameraPhotoUri = uri
        takePicture.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCameraCapture()
    }

    fun launchCamera(target: Int) {
        photoChoiceTarget = target
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> startCameraCapture()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showEnableGpsDialog) {
        AlertDialog(
            onDismissRequest = { showEnableGpsDialog = false },
            title = { Text("Turn on location") },
            text = { Text("Location is off. Open Location settings, or enter coordinates manually below.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableGpsDialog = false
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                ) { Text("Location settings") }
            },
            dismissButton = {
                TextButton(onClick = { showEnableGpsDialog = false }) { Text("Cancel") }
            }
        )
    }

    photoValidationTitle?.let { title ->
        AlertDialog(
            onDismissRequest = {
                photoValidationTitle = null
                photoValidationMessage = null
            },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    photoValidationMessage.orEmpty(),
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        photoValidationTitle = null
                        photoValidationMessage = null
                    },
                ) {
                    Text("Retake photo", fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Location permission") },
            text = { Text("Allow location access to auto-tag this report, or use manual maps link below.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                ) { Text("App settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) { Text("Cancel") }
            }
        )
    }

    val pendingWide = pendingWideUri
    if (showWideLaneConfirm && pendingWide != null) {
        WideShotLaneConfirmDialog(
            photoUri = pendingWide,
            onDismiss = {
                showWideLaneConfirm = false
                pendingWideUri = null
            },
            onConfirm = { position, facing ->
                wideUri = pendingWide
                wideVerified = true
                selectedPosition = position
                trafficFacing = facing
                showWideLaneConfirm = false
                pendingWideUri = null
            },
        )
    }

    val latEffective = if (useManualLocation) manualCoordLat else deviceLat
    val lngEffective = if (useManualLocation) manualCoordLng else deviceLng
    val gpsOk = latEffective != null && lngEffective != null
    val gpsAccuracyM = if (useManualLocation) null else deviceLocationAccuracyM
    val lowGpsAccuracyBlock = gpsOk && !useManualLocation &&
        gpsAccuracyM != null && gpsAccuracyM > GPS_SUBMIT_BLOCK_ACCURACY_METERS
    val lowGpsAccuracyWarn = gpsOk && !useManualLocation &&
        gpsAccuracyM != null &&
        gpsAccuracyM > GPS_SUBMIT_WARN_ACCURACY_METERS &&
        gpsAccuracyM <= GPS_SUBMIT_BLOCK_ACCURACY_METERS

    val metroSubmitBlock = remember(reportCityKey, latEffective, lngEffective) {
        val lat = latEffective
        val lng = lngEffective
        if (lat != null && lng != null) {
            CityMetroLocation.validateSubmitLocation(reportCityKey, lat, lng)
        } else {
            null
        }
    }
    val metroBlock = metroSubmitBlock != null

    LaunchedEffect(reportCityKey, reporterUserId, latEffective, lngEffective, gpsAccuracyM, issueType) {
        val lat = latEffective
        val lng = lngEffective
        duplicateBlockingReport = if (lat != null && lng != null && reporterUserId.isNotBlank()) {
            withContext(Dispatchers.Default) {
                RecentReportsRepository.findActiveDuplicateForReporter(
                    reporterUserId = reporterUserId,
                    cityKey = reportCityKey,
                    latitude = lat,
                    longitude = lng,
                    gpsAccuracyM = if (useManualLocation) null else gpsAccuracyM,
                    issueType = issueType,
                )
            }
        } else {
            null
        }
    }

    val duplicateBlock = duplicateBlockingReport != null
    val formBusy = photoValidating || submitting

    val missingClose = closeUpUri == null || !closeUpVerified
    val missingWide = requiresWideShot && (wideUri == null || !wideVerified)
    val missingLoc = !gpsOk

    val footerLabel = when {
        submitting -> "SUBMITTING…"
        photoValidating -> "CHECKING PHOTO WITH AI..."
        duplicateBlock -> {
            val status = duplicateBlockingReport?.status?.displayLabel?.uppercase() ?: "OPEN"
            "ALREADY REPORTED HERE ($status)"
        }
        metroBlock -> "LOCATION OUTSIDE CITY"
        lowGpsAccuracyBlock -> "GPS TOO INACCURATE — RETRY OR MANUAL"
        missingClose -> if (strayForm) "ADD PHOTO TO CONTINUE" else "ADD VALID CLOSE-UP PHOTO TO CONTINUE"
        missingWide -> "ADD VALID WIDE SHOT TO CONTINUE"
        missingLoc -> "ATTACH LOCATION TO CONTINUE"
        else -> "SUBMIT REPORT"
    }
    val readyToSubmit = !missingClose && !missingWide && !missingLoc && !formBusy &&
        !duplicateBlock && !metroBlock && !lowGpsAccuracyBlock
    val submitButtonActive = readyToSubmit || submitting

    if (showDuplicateDialog) {
        val blocking = duplicateBlockingReport
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Already reported", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (blocking != null) {
                        "You already have an open report within about ${PotholeDuplicateGuard.userFacingRadiusMeters()} m of this spot. " +
                            "Status: ${blocking.status.displayLabel}. " +
                            "You can submit a new report here only after it is marked Completed (resolved)."
                    } else {
                        "You already have an open report within about ${PotholeDuplicateGuard.userFacingRadiusMeters()} m of this spot."
                    },
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeighbourhoodPalette.Page)
    ) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(NeighbourhoodPalette.Page)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = CitizenHeader.Horizontal, end = CitizenHeader.Horizontal, top = CitizenHeader.Vertical)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            issueType.reportTitle,
                            color = NeighbourhoodPalette.Ink,
                            style = CitizenType.ScreenTitle,
                        )
                        IconButton(
                            onClick = onClose,
                            enabled = !submitting,
                            modifier = Modifier.size(CitizenIcon.Tap),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = NeighbourhoodPalette.Navy,
                                modifier = Modifier.size(CitizenIcon.Header),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(CitizenSpace.Sm))

                    SectionTitle(
                        when {
                            gatePhotos -> "01 CLOSE-UP OF POTHOLE"
                            issueType == CivicIssueType.MANHOLE -> "01 CLOSE-UP OF MANHOLE"
                            strayForm -> "01 PHOTO OF PLACE / DOGS"
                            else -> "01 CLOSE-UP PHOTO"
                        },
                    )
                    SectionHelper(
                        when {
                            gatePhotos -> "Frame the pothole clearly. Used for depth and area estimate."
                            issueType == CivicIssueType.MANHOLE ->
                                "Frame the manhole clearly so cover state is visible."
                            strayForm ->
                                "Photo of the dogs, or the place where the incident happened."
                            else -> "Frame the issue clearly so others can recognise it."
                        },
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    PhotoDropZone(
                        enabled = !formBusy,
                        label = when {
                            photoValidating && photoChoiceTarget == 1 -> "ANALYZING PHOTO..."
                            closeUpVerified && strayForm -> "PHOTO ADDED · TAP TO RETAKE"
                            closeUpVerified -> "CLOSE-UP VERIFIED · TAP TO RETAKE"
                            else -> "TAP TO OPEN CAMERA"
                        },
                        filled = closeUpVerified,
                        onClick = { launchCamera(1) },
                    )

                    Spacer(modifier = Modifier.height(CitizenSpace.Sm))

                    if (requiresWideShot) {
                        SectionTitle("02 WIDE SHOT OF ROAD")
                        SectionHelper(
                            if (usesLaneCapture) {
                                "Step back and show the full road. After capture, tap L/M/R on the photo."
                            } else {
                                "Step back and show the street or area around the issue."
                            },
                        )
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        PhotoDropZone(
                            enabled = closeUpVerified && !formBusy,
                            label = when {
                                !closeUpVerified -> "COMPLETE STEP 01 FIRST"
                                photoValidating && photoChoiceTarget == 2 -> "ANALYZING PHOTO..."
                                showWideLaneConfirm -> "CONFIRM LANES ON PHOTO…"
                                wideVerified -> "WIDE SHOT VERIFIED · TAP TO RETAKE"
                                else -> "TAP TO OPEN CAMERA"
                            },
                            filled = wideVerified,
                            onClick = { if (closeUpVerified) launchCamera(2) },
                        )

                        if (wideVerified && usesLaneCapture) {
                            Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                            WideShotSummaryRow(
                                position = selectedPosition,
                                facing = trafficFacing,
                                enabled = !formBusy,
                                onAdjust = {
                                    val uri = wideUri ?: return@WideShotSummaryRow
                                    pendingWideUri = uri
                                    showWideLaneConfirm = true
                                },
                            )
                        }

                        Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                    }

                    SectionTitle(if (strayForm) "02 LOCATION" else "03 ATTACH LOCATION")
                    SectionHelper(
                        when {
                            gatePhotos ->
                                "Auto-detected from GPS. Used only to tag the pothole, not linked to you."
                            issueType == CivicIssueType.MANHOLE ->
                                "Auto-detected from GPS. Used only to tag the manhole, not linked to you."
                            strayForm ->
                                "Auto-detected from GPS. Used only to tag this sighting, not linked to you."
                            else ->
                                "Auto-detected from GPS. Used only to tag this report, not linked to you."
                        },
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    LocationBlock(
                        loading = locationLoading,
                        blockedReason = locationBlockedReason,
                        gpsOk = gpsOk,
                        gpsAccuracyM = gpsAccuracyM,
                        usingManualLocation = useManualLocation,
                        latDisplay = latEffective,
                        lngDisplay = lngEffective,
                        onRetryGps = {
                            if (!locationPermitted()) {
                                val extra = CitizenRuntimePermissions.forLocationAndTrip(context)
                                if (extra.isNotEmpty()) {
                                    locationPermissionLauncher.launch(extra)
                                } else {
                                    refreshGpsLocation()
                                }
                            } else {
                                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val on = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                                if (!on) showEnableGpsDialog = true
                                else refreshGpsLocation()
                            }
                        },
                        onOpenGpsSettings = { showEnableGpsDialog = true },
                    )

                    if (lowGpsAccuracyWarn) {
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        Text(
                            "GPS is imprecise (±${gpsAccuracyM!!.toInt()}m). Move outdoors for a better fix, or use manual entry below.",
                            style = CitizenType.Helper,
                            color = CitizenInk.Warning,
                        )
                    }

                    if (lowGpsAccuracyBlock) {
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        Text(
                            "GPS too weak (±${gpsAccuracyM!!.toInt()}m) to tag. Retry GPS, enable location, or paste coordinates below.",
                            style = CitizenType.Helper,
                            color = CitizenInk.Error,
                        )
                    }

                    if (duplicateBlock) {
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        Text(
                            "Open report within ~${PotholeDuplicateGuard.userFacingRadiusMeters()} m " +
                                "(${duplicateBlockingReport?.status?.displayLabel}). Submit again only after it is resolved.",
                            style = CitizenType.Helper,
                            color = CitizenInk.Warning,
                        )
                    }

                    if (metroBlock && metroSubmitBlock != null) {
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        Text(
                            metroSubmitBlock,
                            style = CitizenType.Helper,
                            color = CitizenInk.Warning,
                        )
                    }

                    if (!gpsOk && !locationLoading) {
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        Text("MANUAL ENTRY", style = CitizenType.Label, fontFamily = FontFamily.Monospace, color = DarkBlue)
                        SectionHelper("Paste a Google Maps URL or coordinates.")
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = mapsUrlInput,
                                onValueChange = {
                                    mapsUrlInput = it
                                    useManualLocation = false
                                    manualCoordLat = null
                                    manualCoordLng = null
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !submitting,
                                placeholder = { Text("Maps URL", fontSize = 10.sp) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 10.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DarkBlue,
                                    unfocusedBorderColor = DarkBlue,
                                )
                            )
                            Spacer(modifier = Modifier.width(CitizenSpace.Xs))
                            OutlinedTextField(
                                value = lngFieldInput,
                                onValueChange = {
                                    lngFieldInput = it
                                    useManualLocation = false
                                    manualCoordLat = null
                                    manualCoordLng = null
                                },
                                modifier = Modifier.width(88.dp),
                                enabled = !submitting,
                                placeholder = { Text("lng", fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = TextStyle(fontSize = 10.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DarkBlue,
                                    unfocusedBorderColor = DarkBlue,
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        Button(
                            onClick = {
                                val parsed = parseMapsInput(mapsUrlInput, lngFieldInput)
                                if (parsed != null) {
                                    useManualLocation = true
                                    manualCoordLat = parsed.first
                                    manualCoordLng = parsed.second
                                    locationBlockedReason = null
                                }
                            },
                            enabled = !submitting,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue, contentColor = Color.White),
                            shape = CitizenRadius.Card,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("USE THIS LOCATION", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(CitizenSpace.Sm))

                    when {
                        strayForm -> {
                            SectionTitle("03 TIME OF DAY")
                            SectionHelper("When did you see the dogs or when did the incident happen? Change if it was earlier.")
                            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs)) {
                                IncidentTimeTile(
                                    Modifier.weight(1f),
                                    IncidentTimeOfDay.MORNING,
                                    selectedIncidentTime,
                                    enabled = !submitting,
                                ) { selectedIncidentTime = it }
                                IncidentTimeTile(
                                    Modifier.weight(1f),
                                    IncidentTimeOfDay.AFTERNOON,
                                    selectedIncidentTime,
                                    enabled = !submitting,
                                ) { selectedIncidentTime = it }
                            }
                            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs)) {
                                IncidentTimeTile(
                                    Modifier.weight(1f),
                                    IncidentTimeOfDay.EVENING,
                                    selectedIncidentTime,
                                    enabled = !submitting,
                                ) { selectedIncidentTime = it }
                                IncidentTimeTile(
                                    Modifier.weight(1f),
                                    IncidentTimeOfDay.NIGHT,
                                    selectedIncidentTime,
                                    enabled = !submitting,
                                ) { selectedIncidentTime = it }
                            }
                        }
                        issueType == CivicIssueType.MANHOLE -> {
                            SectionTitle("04 MANHOLE STATE")
                            SectionHelper("Is the manhole open, closed, broken, or overflowing?")
                            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs)) {
                                ManholeConditionTile(
                                    Modifier.weight(1f),
                                    ManholeCondition.OPEN,
                                    selectedManholeCondition,
                                    enabled = !submitting,
                                ) { selectedManholeCondition = it }
                                ManholeConditionTile(
                                    Modifier.weight(1f),
                                    ManholeCondition.CLOSED,
                                    selectedManholeCondition,
                                    enabled = !submitting,
                                ) { selectedManholeCondition = it }
                            }
                            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs)) {
                                ManholeConditionTile(
                                    Modifier.weight(1f),
                                    ManholeCondition.BROKEN,
                                    selectedManholeCondition,
                                    enabled = !submitting,
                                ) { selectedManholeCondition = it }
                                ManholeConditionTile(
                                    Modifier.weight(1f),
                                    ManholeCondition.OVERFLOWING,
                                    selectedManholeCondition,
                                    enabled = !submitting,
                                ) { selectedManholeCondition = it }
                            }
                        }
                        else -> {
                            SectionTitle("04 HOW BAD IS IT?")
                            SectionHelper(
                                if (gatePhotos) {
                                    "Pick the closest match. AI may suggest a severity after photos."
                                } else {
                                    "Same severity scale for every issue."
                                },
                            )
                            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs)) {
                                SeverityTile(
                                    Modifier.weight(1f),
                                    PotholeSeverity.MINOR,
                                    selectedSeverity,
                                    enabled = !submitting,
                                    blurb = issueType.severityBlurb(PotholeSeverity.MINOR),
                                ) { selectedSeverity = it }
                                SeverityTile(
                                    Modifier.weight(1f),
                                    PotholeSeverity.MODERATE,
                                    selectedSeverity,
                                    enabled = !submitting,
                                    blurb = issueType.severityBlurb(PotholeSeverity.MODERATE),
                                ) { selectedSeverity = it }
                            }
                            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs)) {
                                SeverityTile(
                                    Modifier.weight(1f),
                                    PotholeSeverity.SEVERE,
                                    selectedSeverity,
                                    enabled = !submitting,
                                    blurb = issueType.severityBlurb(PotholeSeverity.SEVERE),
                                ) { selectedSeverity = it }
                                SeverityTile(
                                    Modifier.weight(1f),
                                    PotholeSeverity.CRITICAL,
                                    selectedSeverity,
                                    enabled = !submitting,
                                    blurb = issueType.severityBlurb(PotholeSeverity.CRITICAL),
                                ) { selectedSeverity = it }
                            }

                            if (gatePhotos) aiRiskInsight?.let { insight ->
                                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                                AiRiskSuggestionCard(
                                    insight = insight,
                                    enabled = !submitting,
                                    onUseSuggested = { selectedSeverity = insight.suggestedSeverity },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(CitizenSpace.Sm))

                    SectionTitle(if (strayForm) "04 LANDMARK" else "05 NOTE (OPTIONAL)")
                    SectionHelper(
                        if (strayForm) {
                            "Park gate, shop, school, junction — anything a catcher can find."
                        } else {
                            "Add a location clue if you can: landmark, road name, street number, junction, or nearby shop."
                        },
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    val noteSuggestions = remember(strayForm) {
                        if (strayForm) {
                            listOf(
                                "Near park gate...",
                                "Opposite the bakery...",
                                "Next to school wall...",
                                "Behind the bus stop...",
                                "Junction of ... and ...",
                            )
                        } else {
                            listOf(
                                "Near ...",
                                "On ... Road...",
                                "Street no. ...",
                                "Landmark: ...",
                                "Near MG Road metro, opp. Cafe Coffee Day...",
                            )
                        }
                    }
                    var noteSuggestionIndex by remember { mutableIntStateOf(0) }
                    LaunchedEffect(note.isEmpty(), submitting) {
                        if (note.isNotEmpty() || submitting) return@LaunchedEffect
                        while (true) {
                            delay(2_500)
                            noteSuggestionIndex =
                                (noteSuggestionIndex + 1) % noteSuggestions.size
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= noteMax) note = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !submitting,
                        placeholder = {
                            Text(
                                noteSuggestions[noteSuggestionIndex],
                                fontSize = 10.sp,
                            )
                        },
                        maxLines = 2,
                        textStyle = TextStyle(fontSize = 11.sp),
                        supportingText = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("${note.length}/$noteMax", style = CitizenType.Caption, color = CitizenInk.Muted)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
                            unfocusedBorderColor = DarkBlue,
                            focusedTextColor = DarkBlue,
                            unfocusedTextColor = DarkBlue,
                            cursorColor = DarkBlue,
                            focusedPlaceholderColor = CitizenInk.Helper,
                            unfocusedPlaceholderColor = CitizenInk.Helper,
                        )
                    )
                }

                Column(
                    modifier = Modifier.padding(
                        start = CitizenSpace.Lg,
                        end = CitizenSpace.Lg,
                        bottom = CitizenSpace.Sm,
                    )
                ) {
                    Button(
                        onClick = {
                            if (!readyToSubmit || submitting) return@Button
                            val closeUri = closeUpUri ?: return@Button
                            val wideShotUri = if (requiresWideShot) {
                                wideUri ?: return@Button
                            } else {
                                null
                            }
                            val lat = latEffective ?: return@Button
                            val lng = lngEffective ?: return@Button
                            val metroError = CityMetroLocation.validateSubmitLocation(reportCityKey, lat, lng)
                            if (metroError != null) return@Button
                            submitting = true
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        RecentReportsRepository.addReport(
                                            cityKey = reportCityKey,
                                            closeUpUri = closeUri,
                                            wideUri = wideShotUri,
                                            latitude = lat,
                                            longitude = lng,
                                            severity = when {
                                                issueType == CivicIssueType.MANHOLE ->
                                                    selectedManholeCondition.toSeverity()
                                                else -> selectedSeverity
                                            },
                                            issueType = issueType,
                                            note = note,
                                            reporterUserId = reporterUserId,
                                            submittedSignedIn = submittedWhileSignedIn,
                                            potholePosition = selectedPosition,
                                            reportBearingDeg = deviceBearingDeg ?: Float.NaN,
                                            trafficFacing = trafficFacing,
                                            manholeCondition = selectedManholeCondition.takeIf {
                                                issueType == CivicIssueType.MANHOLE
                                            },
                                            incidentTimeOfDay = selectedIncidentTime.takeIf { strayForm },
                                        )
                                    }
                                    when (result) {
                                        is AddReportResult.Success -> {
                                            val routingMsg =
                                                BengaluruMunicipalRouting.formatSubmitRoutingMessage(result.report)
                                            if (submittedWhileSignedIn) {
                                                if (!SupabaseClientProvider.isConfigured) {
                                                    onSyncMessage(
                                                        "$routingMsg Saved on this device only — " +
                                                            "rebuild with SUPABASE_URL and SUPABASE_ANON_KEY in local.properties.",
                                                    )
                                                } else {
                                                    // Local save is enough to close; cloud push runs in
                                                    // app scope so a slow upload cannot hang or cancel mid-trip.
                                                    ReportSyncRepository.enqueuePush(result.report)
                                                    onSyncMessage(
                                                        "$routingMsg Saved — uploading to cloud in the background.",
                                                    )
                                                }
                                            } else {
                                                onSyncMessage(routingMsg)
                                            }
                                            onReportPersisted()
                                            onClose()
                                        }
                                        is AddReportResult.DuplicateActive -> {
                                            duplicateBlockingReport = result.existing
                                            showDuplicateDialog = true
                                        }
                                        AddReportResult.Failed -> {
                                            onSyncMessage("Couldn't save the report. Check storage or network, then tap Submit again.")
                                        }
                                    }
                                } finally {
                                    submitting = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = CitizenIcon.Tap + CitizenSpace.Md),
                        enabled = readyToSubmit && !submitting,
                        contentPadding = PaddingValues(
                            horizontal = CitizenSpace.Xl,
                            vertical = CitizenSpace.Md,
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (submitButtonActive) DarkBlue else CitizenInk.Disabled,
                            contentColor = Color.White,
                            disabledContainerColor = if (submitting) DarkBlue else CitizenInk.Disabled,
                            disabledContentColor = Color.White.copy(alpha = 0.85f),
                        ),
                        shape = CitizenRadius.Capsule,
                        border = BorderStroke(
                            CitizenBorder.Thin,
                            if (submitButtonActive || submitting) Color(0xFF0F0F1A) else CitizenInk.Disabled,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(CitizenIcon.Chip),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(CitizenIcon.Chip),
                                )
                            }
                            Spacer(modifier = Modifier.width(CitizenSpace.Sm))
                            Text(
                                footerLabel,
                                style = CitizenType.Cta,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    Text(
                        CitizenPrivacyCopy.FOOTER_CAPS,
                        style = CitizenType.CtaSub,
                        color = CitizenInk.Muted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(AccentRed)
            )
        }
    }
}

@Composable
private fun AiRiskSuggestionCard(
    insight: PotholeRiskInsight,
    onUseSuggested: () -> Unit,
    enabled: Boolean = true,
) {
    val severityColor = when (insight.suggestedSeverity) {
        PotholeSeverity.MINOR -> Color(0xFF1B5E20)
        PotholeSeverity.MODERATE -> Color(0xFFEF6C00)
        PotholeSeverity.SEVERE -> Color(0xFFC62828)
        PotholeSeverity.CRITICAL -> Color(0xFF8E0000)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(CitizenBorder.Thin, severityColor))
            .background(Color.White)
            .padding(6.dp),
    ) {
        Text(
            "AI ROAD RISK ADVISORY",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = severityColor,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
        Text(
            insight.advisoryLine,
            fontSize = 9.sp,
            color = Color(0xFF2A2A2A),
            lineHeight = 11.sp,
        )
        Text(
            "Criticality: ${insight.criticalityLabel} · Suggested severity: ${insight.suggestedSeverity.displayLabel}",
            fontSize = 9.sp,
            color = Color(0xFF2A2A2A),
            lineHeight = 11.sp,
        )
        Text(
            "Rider advice: keep speed below ${insight.suggestedSpeedLimitKmph} km/h near this pothole.",
            fontSize = 9.sp,
            color = severityColor,
            fontWeight = FontWeight.Bold,
            lineHeight = 11.sp,
        )
        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
        TextButton(
            onClick = onUseSuggested,
            enabled = enabled,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Use AI suggested severity", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = CitizenType.Label,
        fontFamily = FontFamily.Monospace,
        color = DarkBlue,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PhotoDropZone(
    enabled: Boolean,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(CitizenRadius.Card)
            .border(
                BorderStroke(CitizenBorder.Thin, if (enabled) DarkBlue else GrayDisabled),
                CitizenRadius.Card,
            )
            .background(
                if (enabled) Color.White.copy(alpha = 0.5f) else Color(0xFFE0E0E0),
                CitizenRadius.Card,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = if (enabled) DarkBlue else GrayDisabled,
                modifier = Modifier.size(CitizenIcon.Header)
            )
            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
            Text(
                label,
                style = CitizenType.ChipCompact,
                fontWeight = FontWeight.Bold,
                color = if (enabled) DarkBlue else CitizenInk.Disabled,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (filled) {
                Text(
                    "✓ Added",
                    style = CitizenType.Caption,
                    color = CitizenInk.Success,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LocationBlock(
    loading: Boolean,
    blockedReason: String?,
    gpsOk: Boolean,
    gpsAccuracyM: Float?,
    usingManualLocation: Boolean,
    latDisplay: Double?,
    lngDisplay: Double?,
    onRetryGps: () -> Unit,
    onOpenGpsSettings: () -> Unit,
) {
    val pinColor = when {
        loading -> CitizenInk.Helper
        gpsOk -> CitizenInk.Success
        blockedReason == "NO_FIX" -> CitizenInk.Warning
        else -> CitizenInk.Error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(CitizenBorder.Thin, CitizenSelection.InactiveBorder), CitizenRadius.Card)
            .background(Color.White, CitizenRadius.Card)
            .padding(CitizenSpace.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(CitizenIcon.Fab)
                .background(pinColor, CitizenRadius.Capsule),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(CitizenIcon.Chip),
            )
        }
        Spacer(modifier = Modifier.width(CitizenSpace.Sm))
        Column(modifier = Modifier.weight(1f)) {
            when {
                loading -> Text(
                    "LOCATING…",
                    style = CitizenType.Label,
                    color = DarkBlue,
                )
                gpsOk -> {
                    val title = if (usingManualLocation) "MANUAL LOCATION" else "LOCATION ATTACHED"
                    Text(title, style = CitizenType.Label, color = CitizenInk.Success)
                    Text(
                        "${"%.5f".format(latDisplay!!)}, ${"%.5f".format(lngDisplay!!)}",
                        style = CitizenType.Caption,
                        color = CitizenInk.Helper,
                    )
                    if (!usingManualLocation && gpsAccuracyM != null) {
                        Text(
                            "GPS +/-${gpsAccuracyM.toInt()}m",
                            style = CitizenType.Caption,
                            color = when {
                                gpsAccuracyM > GPS_SUBMIT_BLOCK_ACCURACY_METERS -> CitizenInk.Error
                                gpsAccuracyM > GPS_SUBMIT_WARN_ACCURACY_METERS -> CitizenInk.Warning
                                else -> CitizenInk.Success
                            },
                        )
                    }
                }
                blockedReason == "GPS_OFF" -> {
                    Text("GPS is off", style = CitizenType.Label, color = CitizenInk.Error)
                    Text(
                        "Turn on location, then tap RETRY GPS.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Muted,
                        maxLines = 2,
                    )
                }
                blockedReason == "NO_FIX" -> {
                    Text("No GPS fix yet", style = CitizenType.Label, color = CitizenInk.Warning)
                    Text(
                        "Move outdoors, then tap RETRY GPS.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Muted,
                        maxLines = 2,
                    )
                }
                else -> {
                    Text("Location permission needed", style = CitizenType.Label, color = CitizenInk.Error)
                    Text(
                        "Tap RETRY GPS to allow access.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Muted,
                        maxLines = 3,
                    )
                }
            }
        }
        if (!gpsOk && !loading) {
            Column {
                Button(
                    onClick = onRetryGps,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = DarkBlue,
                    ),
                    shape = CitizenRadius.Capsule,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) {
                    Text("RETRY GPS", style = CitizenType.ChipCompact, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onOpenGpsSettings,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) {
                    Text("ENABLE GPS", style = CitizenType.Caption, color = DarkBlue)
                }
            }
        }
    }
}

@Composable
private fun WideShotSummaryRow(
    position: PotholePosition,
    facing: TrafficFacingMode,
    enabled: Boolean,
    onAdjust: () -> Unit,
) {
    val facingLabel = when (facing) {
        TrafficFacingMode.FACING_CAMERA -> "Toward camera"
        TrafficFacingMode.AWAY_FROM_CAMERA -> "Against traffic (Flip)"
        TrafficFacingMode.UNKNOWN -> "Traffic unset"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CitizenRadius.Card)
            .border(BorderStroke(CitizenBorder.Thin, DarkBlue), CitizenRadius.Card)
            .background(Color.White.copy(alpha = 0.92f), CitizenRadius.Card)
            .clickable(enabled = enabled, onClick = onAdjust)
            .padding(CitizenSpace.Sm),
    ) {
        Text(
            "POSITION ${position.code} · ${position.displayLabel.uppercase()}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            color = DarkBlue,
        )
        Text(
            "$facingLabel · tap to adjust lanes / Flip",
            fontSize = 8.sp,
            color = CitizenInk.Muted,
        )
    }
}

@Composable
private fun IncidentTimeTile(
    modifier: Modifier,
    period: IncidentTimeOfDay,
    selected: IncidentTimeOfDay,
    enabled: Boolean = true,
    onSelect: (IncidentTimeOfDay) -> Unit,
) {
    val sel = period == selected
    Box(
        modifier = modifier
            .heightIn(min = CitizenIcon.Tap + CitizenSpace.Sm)
            .clip(CitizenRadius.Card)
            .border(
                BorderStroke(
                    CitizenBorder.Thin,
                    if (sel) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                ),
                CitizenRadius.Card,
            )
            .background(
                if (sel) CitizenSelection.Active else Color.White.copy(alpha = 0.85f),
                CitizenRadius.Card,
            )
            .clickable(enabled = enabled) { onSelect(period) }
            .padding(CitizenSpace.Xs),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                period.displayLabel.uppercase(),
                style = CitizenType.ChipCompact,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (sel) CitizenSelection.ActiveContent else DarkBlue,
                textAlign = TextAlign.Center,
            )
            Text(
                period.blurb,
                style = CitizenType.Caption,
                color = if (sel) {
                    CitizenSelection.ActiveContent.copy(alpha = 0.95f)
                } else {
                    CitizenInk.Helper
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ManholeConditionTile(
    modifier: Modifier,
    condition: ManholeCondition,
    selected: ManholeCondition,
    enabled: Boolean = true,
    onSelect: (ManholeCondition) -> Unit,
) {
    val sel = condition == selected
    Box(
        modifier = modifier
            .heightIn(min = CitizenIcon.Tap + CitizenSpace.Sm)
            .clip(CitizenRadius.Card)
            .border(
                BorderStroke(
                    CitizenBorder.Thin,
                    if (sel) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                ),
                CitizenRadius.Card,
            )
            .background(
                if (sel) CitizenSelection.Active else Color.White.copy(alpha = 0.85f),
                CitizenRadius.Card,
            )
            .clickable(enabled = enabled) { onSelect(condition) }
            .padding(CitizenSpace.Xs),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                condition.displayLabel.uppercase(),
                style = CitizenType.ChipCompact,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (sel) CitizenSelection.ActiveContent else DarkBlue,
                textAlign = TextAlign.Center,
            )
            Text(
                condition.blurb,
                style = CitizenType.Caption,
                color = if (sel) {
                    CitizenSelection.ActiveContent.copy(alpha = 0.95f)
                } else {
                    CitizenInk.Helper
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun SeverityTile(
    modifier: Modifier,
    severity: PotholeSeverity,
    selected: PotholeSeverity,
    enabled: Boolean = true,
    blurb: String = severity.blurb,
    onSelect: (PotholeSeverity) -> Unit,
) {
    val sel = severity == selected
    Box(
        modifier = modifier
            .heightIn(min = CitizenIcon.Tap + CitizenSpace.Sm)
            .clip(CitizenRadius.Card)
            .border(
                BorderStroke(
                    CitizenBorder.Thin,
                    if (sel) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                ),
                CitizenRadius.Card,
            )
            .background(
                if (sel) CitizenSelection.Active else Color.White.copy(alpha = 0.85f),
                CitizenRadius.Card,
            )
            .clickable(enabled = enabled) { onSelect(severity) }
            .padding(CitizenSpace.Xs),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "${severity.code} ${severity.title}",
                style = CitizenType.ChipCompact,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (sel) CitizenSelection.ActiveContent else DarkBlue,
                textAlign = TextAlign.Center,
            )
            Text(
                blurb,
                style = CitizenType.Caption,
                color = if (sel) {
                    CitizenSelection.ActiveContent.copy(alpha = 0.95f)
                } else {
                    CitizenInk.Helper
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PositionTile(
    modifier: Modifier,
    position: PotholePosition,
    selected: PotholePosition,
    enabled: Boolean = true,
    onSelect: (PotholePosition) -> Unit,
) {
    val sel = position == selected
    val isFullRoad = position == PotholePosition.FULL_WIDTH
    Box(
        modifier = modifier
            .heightIn(min = if (isFullRoad) CitizenIcon.Tap + CitizenSpace.Sm else CitizenIcon.Tap)
            .clip(CitizenRadius.Card)
            .border(
                BorderStroke(
                    CitizenBorder.Thin,
                    if (sel) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                ),
                CitizenRadius.Card,
            )
            .background(
                if (sel) CitizenSelection.Active else Color.White.copy(alpha = 0.85f),
                CitizenRadius.Card,
            )
            .clickable(enabled = enabled) { onSelect(position) }
            .padding(CitizenSpace.Xs),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (isFullRoad) "LMR · FULL ROAD" else position.displayLabel.uppercase(),
                style = CitizenType.ChipCompact,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (sel) CitizenSelection.ActiveContent else DarkBlue,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (isFullRoad) {
                Text(
                    "Entire road width damaged",
                    style = CitizenType.Caption,
                    color = if (sel) {
                        CitizenSelection.ActiveContent.copy(alpha = 0.95f)
                    } else {
                        CitizenInk.Helper
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TrafficDirectionConfirmRow(
    facing: TrafficFacingMode,
    enabled: Boolean,
    onSelect: (TrafficFacingMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(CitizenBorder.Thin, DarkBlue))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(8.dp),
    ) {
        Text(
            "WHICH WAY DO CARS TRAVEL IN THIS PHOTO?",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = DarkBlue,
        )
        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
        Text(
            "Look at the wide shot. Match the picture to how cars move in your photo. Used for Left/Right trip alerts.",
            fontSize = 8.sp,
            lineHeight = 10.sp,
            color = CitizenInk.Muted,
        )
        Spacer(modifier = Modifier.height(CitizenSpace.Sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Xs),
        ) {
            TrafficFacingTile(
                modifier = Modifier.weight(1f),
                title = "Toward camera",
                subtitle = "Cars coming at you",
                mode = TrafficFacingMode.FACING_CAMERA,
                selected = facing == TrafficFacingMode.FACING_CAMERA,
                enabled = enabled,
                onClick = { onSelect(TrafficFacingMode.FACING_CAMERA) },
            )
            TrafficFacingTile(
                modifier = Modifier.weight(1f),
                title = "Away",
                subtitle = "Cars going away",
                mode = TrafficFacingMode.AWAY_FROM_CAMERA,
                selected = facing == TrafficFacingMode.AWAY_FROM_CAMERA,
                enabled = enabled,
                onClick = { onSelect(TrafficFacingMode.AWAY_FROM_CAMERA) },
            )
            TrafficFacingTile(
                modifier = Modifier.weight(1f),
                title = "Not sure",
                subtitle = "Skip for now",
                mode = TrafficFacingMode.UNKNOWN,
                selected = facing == TrafficFacingMode.UNKNOWN,
                enabled = enabled,
                onClick = { onSelect(TrafficFacingMode.UNKNOWN) },
            )
        }
    }
}

@Composable
private fun TrafficFacingTile(
    modifier: Modifier,
    title: String,
    subtitle: String,
    mode: TrafficFacingMode,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val ink = if (selected) Color.White else DarkBlue
    val mute = if (selected) Color.White.copy(alpha = 0.95f) else Color.DarkGray
    Column(
        modifier = modifier
            .heightIn(min = 118.dp)
            .border(BorderStroke(CitizenBorder.Thin, DarkBlue))
            .background(if (selected) OrangeSelect else Color.White.copy(alpha = 0.85f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        TrafficFacingDiagram(
            mode = mode,
            ink = ink,
            accent = if (selected) Color.White else OrangeSelect,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )
        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
        Text(
            title,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            color = ink,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            subtitle,
            fontSize = 7.sp,
            lineHeight = 8.sp,
            color = mute,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Mini road + camera sketch so reporters can match traffic direction at a glance.
 * Camera sits at the bottom of the frame (photographer); arrows show car travel.
 */
@Composable
private fun TrafficFacingDiagram(
    mode: TrafficFacingMode,
    ink: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val roadTop = h * 0.08f
        val roadBottom = h * 0.62f
        val road = Path().apply {
            moveTo(w * 0.34f, roadTop)
            lineTo(w * 0.66f, roadTop)
            lineTo(w * 0.88f, roadBottom)
            lineTo(w * 0.12f, roadBottom)
            close()
        }
        drawPath(road, color = ink.copy(alpha = 0.12f))
        drawPath(road, color = ink, style = Stroke(width = 1.6f))
        val dashPaint = ink.copy(alpha = 0.55f)
        val midX = w * 0.5f
        var y = roadTop + 4f
        while (y < roadBottom - 6f) {
            drawLine(
                color = dashPaint,
                start = Offset(midX, y),
                end = Offset(midX, y + 5f),
                strokeWidth = 1.4f,
                cap = StrokeCap.Round,
            )
            y += 9f
        }
        val camW = w * 0.22f
        val camH = h * 0.22f
        val camLeft = (w - camW) / 2f
        val camTop = h * 0.72f
        drawRoundRect(
            color = ink,
            topLeft = Offset(camLeft, camTop),
            size = Size(camW, camH),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = 1.8f),
        )
        drawCircle(
            color = accent,
            radius = camH * 0.22f,
            center = Offset(w * 0.5f, camTop + camH * 0.5f),
        )
        when (mode) {
            TrafficFacingMode.FACING_CAMERA -> {
                drawTrafficCar(
                    center = Offset(midX, roadTop + (roadBottom - roadTop) * 0.28f),
                    bodyW = w * 0.16f,
                    bodyH = h * 0.12f,
                    ink = ink,
                    accent = accent,
                )
                drawTrafficArrow(
                    tip = Offset(midX, roadBottom - 4f),
                    from = Offset(midX, roadTop + (roadBottom - roadTop) * 0.48f),
                    color = accent,
                )
            }
            TrafficFacingMode.AWAY_FROM_CAMERA -> {
                drawTrafficCar(
                    center = Offset(midX, roadTop + (roadBottom - roadTop) * 0.72f),
                    bodyW = w * 0.16f,
                    bodyH = h * 0.12f,
                    ink = ink,
                    accent = accent,
                )
                drawTrafficArrow(
                    tip = Offset(midX, roadTop + 4f),
                    from = Offset(midX, roadTop + (roadBottom - roadTop) * 0.52f),
                    color = accent,
                )
            }
            TrafficFacingMode.UNKNOWN -> {
                drawTrafficArrow(
                    tip = Offset(midX - w * 0.12f, roadBottom - 6f),
                    from = Offset(midX - w * 0.12f, roadTop + 8f),
                    color = ink.copy(alpha = 0.35f),
                )
                drawTrafficArrow(
                    tip = Offset(midX + w * 0.12f, roadTop + 8f),
                    from = Offset(midX + w * 0.12f, roadBottom - 6f),
                    color = ink.copy(alpha = 0.35f),
                )
                drawCircle(
                    color = ink.copy(alpha = 0.15f),
                    radius = h * 0.14f,
                    center = Offset(midX, roadTop + (roadBottom - roadTop) * 0.45f),
                )
                val qCenter = Offset(midX, roadTop + (roadBottom - roadTop) * 0.42f)
                drawArc(
                    color = ink,
                    startAngle = 200f,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = Offset(qCenter.x - 7f, qCenter.y - 10f),
                    size = Size(14f, 14f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round),
                )
                drawCircle(color = ink, radius = 1.6f, center = Offset(qCenter.x, qCenter.y + 10f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrafficCar(
    center: Offset,
    bodyW: Float,
    bodyH: Float,
    ink: Color,
    accent: Color,
) {
    drawRoundRect(
        color = accent.copy(alpha = 0.85f),
        topLeft = Offset(center.x - bodyW / 2f, center.y - bodyH / 2f),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(2.5f, 2.5f),
    )
    drawRoundRect(
        color = ink,
        topLeft = Offset(center.x - bodyW / 2f, center.y - bodyH / 2f),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(2.5f, 2.5f),
        style = Stroke(width = 1.2f),
    )
    drawRect(
        color = ink.copy(alpha = 0.35f),
        topLeft = Offset(center.x - bodyW * 0.28f, center.y - bodyH * 0.28f),
        size = Size(bodyW * 0.56f, bodyH * 0.28f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrafficArrow(
    tip: Offset,
    from: Offset,
    color: Color,
) {
    drawLine(
        color = color,
        start = from,
        end = tip,
        strokeWidth = 2.4f,
        cap = StrokeCap.Round,
    )
    val dx = tip.x - from.x
    val dy = tip.y - from.y
    val len = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat().coerceAtLeast(1f)
    val ux = dx / len
    val uy = dy / len
    val head = 7f
    val left = Offset(
        tip.x - ux * head - uy * head * 0.55f,
        tip.y - uy * head + ux * head * 0.55f,
    )
    val right = Offset(
        tip.x - ux * head + uy * head * 0.55f,
        tip.y - uy * head - ux * head * 0.55f,
    )
    drawLine(color, tip, left, strokeWidth = 2.4f, cap = StrokeCap.Round)
    drawLine(color, tip, right, strokeWidth = 2.4f, cap = StrokeCap.Round)
}
private fun createImageFile(context: Context): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return File.createTempFile("JPEG_${time}_", ".jpg", context.cacheDir)
}

private fun parseMapsInput(url: String, lngExtra: String): Pair<Double, Double>? {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return null

    val atMatch = Regex("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)").find(trimmed)
    if (atMatch != null) {
        val la = atMatch.groupValues[1].toDoubleOrNull() ?: return null
        val lo = atMatch.groupValues[2].toDoubleOrNull() ?: return null
        return la to lo
    }
    val qMatch = Regex("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)").find(trimmed)
    if (qMatch != null) {
        val la = qMatch.groupValues[1].toDoubleOrNull() ?: return null
        val lo = qMatch.groupValues[2].toDoubleOrNull() ?: return null
        return la to lo
    }
    val comma = trimmed.split(",").map { it.trim() }
    if (comma.size == 2) {
        val la = comma[0].toDoubleOrNull() ?: return null
        val lo = comma[1].toDoubleOrNull() ?: return null
        return la to lo
    }
    val la = trimmed.toDoubleOrNull()
    val lo = lngExtra.trim().toDoubleOrNull()
    if (la != null && lo != null) return la to lo
    return null
}
