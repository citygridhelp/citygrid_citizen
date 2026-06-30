@file:Suppress("SpellCheckingInspection")

package com.example.potholereport.ui.report

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
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
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.example.potholereport.data.AddReportResult
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.PotholePosition
import com.example.potholereport.data.PotholeSeverity
import com.example.potholereport.data.RecentReportsRepository
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private val AccentRed = Color(0xFFB74233)
private val DarkBlue = Color(0xFF1A1A2E)
private val OffWhite = Color(0xFFF5F2E8)
private val OrangeSelect = Color(0xFFF58220)
private val GrayDisabled = Color(0xFF9E9E9E)
private val GrayButton = Color(0xFF757575)

@Composable
fun NewReportScreen(
    reportCityKey: String,
    reporterUserId: String,
    onClose: () -> Unit,
    onReportPersisted: () -> Unit = {},
    onSyncMessage: (String) -> Unit = {},
    submittedWhileSignedIn: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var closeUpUri by remember { mutableStateOf<Uri?>(null) }
    var wideUri by remember { mutableStateOf<Uri?>(null) }
    var closeUpVerified by remember { mutableStateOf(false) }
    var wideVerified by remember { mutableStateOf(false) }
    var photoValidating by remember { mutableStateOf(false) }
    var photoValidationTitle by remember { mutableStateOf<String?>(null) }
    var photoValidationMessage by remember { mutableStateOf<String?>(null) }
    var duplicateBlockingReport by remember { mutableStateOf<PersistedPotholeReport?>(null) }
    var showDuplicateDialog by remember { mutableStateOf(false) }

    var selectedSeverity by remember { mutableStateOf(PotholeSeverity.MODERATE) }
    var selectedPosition by remember { mutableStateOf(PotholePosition.MIDDLE) }
    var aiRiskInsight by remember { mutableStateOf<PotholeRiskInsight?>(null) }

    var note by remember { mutableStateOf("") }
    val noteMax = 240

    var deviceLat by remember { mutableStateOf<Double?>(null) }
    var deviceLng by remember { mutableStateOf<Double?>(null) }
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
            return
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gpsOn) {
            locationBlockedReason = "GPS_OFF"
            deviceLat = null
            deviceLng = null
            return
        }
        scope.launch {
            locationLoading = true
            if (!useManualLocation) locationBlockedReason = null
            val loc = withContext(Dispatchers.Default) {
                fetchBestCalibratedLocation(context)
            }
            locationLoading = false
            if (loc != null) {
                deviceLat = loc.latitude
                deviceLng = loc.longitude
                if (!useManualLocation) locationBlockedReason = null
            } else if (!useManualLocation) {
                locationBlockedReason = "NO_FIX"
                deviceLat = null
                deviceLng = null
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

    // Re-run the risk analyzer whenever the user changes the lane (left /
    // middle / right). The analyzer constrains its dark-blob search to that
    // lane, so the suggested width / depth / severity numbers update.
    LaunchedEffect(selectedPosition, closeUpVerified) {
        val captured = closeUpUri ?: return@LaunchedEffect
        if (!closeUpVerified) return@LaunchedEffect
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
                        wideUri = capturedUri
                        wideVerified = true
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
            text = { Text("Enable device location (GPS) so we can tag the pothole. You can also use manual entry below.") },
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

    val latEffective = if (useManualLocation) manualCoordLat else deviceLat
    val lngEffective = if (useManualLocation) manualCoordLng else deviceLng
    val gpsOk = latEffective != null && lngEffective != null

    LaunchedEffect(reportCityKey, reporterUserId, latEffective, lngEffective) {
        val lat = latEffective
        val lng = lngEffective
        duplicateBlockingReport = if (lat != null && lng != null && reporterUserId.isNotBlank()) {
            withContext(Dispatchers.Default) {
                RecentReportsRepository.findActiveDuplicateForReporter(
                    reporterUserId = reporterUserId,
                    cityKey = reportCityKey,
                    latitude = lat,
                    longitude = lng,
                )
            }
        } else {
            null
        }
    }

    val duplicateBlock = duplicateBlockingReport != null

    val missingClose = closeUpUri == null || !closeUpVerified
    val missingWide = wideUri == null || !wideVerified
    val missingLoc = !gpsOk

    val footerLabel = when {
        photoValidating -> "CHECKING PHOTO WITH AI..."
        duplicateBlock -> {
            val status = duplicateBlockingReport?.status?.displayLabel?.uppercase() ?: "OPEN"
            "ALREADY REPORTED HERE ($status)"
        }
        missingClose -> "ADD VALID CLOSE-UP PHOTO TO CONTINUE"
        missingWide -> "ADD VALID WIDE SHOT TO CONTINUE"
        missingLoc -> "ATTACH LOCATION TO CONTINUE"
        else -> "SUBMIT REPORT"
    }
    val readyToSubmit = !missingClose && !missingWide && !missingLoc && !photoValidating && !duplicateBlock

    if (showDuplicateDialog) {
        val blocking = duplicateBlockingReport
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Already reported", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (blocking != null) {
                        "You already reported this pothole at this location. " +
                            "Status: ${blocking.status.displayLabel}. " +
                            "You can submit a new report here only after it is marked Completed (resolved)."
                    } else {
                        "You already have an open report for this pothole nearby."
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
            .background(DarkBlue)
    ) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(OffWhite)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 6.dp, end = 4.dp, top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "NEW REPORT",
                            color = AccentRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentRed)
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkBlue)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    SectionTitle("01 CLOSE-UP OF POTHOLE")
                    Text(
                        "FRAME THE HOLE CLEARLY - USED FOR DEPTH AND AREA ESTIMATION",
                        color = Color.Gray,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    PhotoDropZone(
                        enabled = !photoValidating,
                        label = when {
                            photoValidating && photoChoiceTarget == 1 -> "ANALYZING PHOTO..."
                            closeUpVerified -> "CLOSE-UP VERIFIED · TAP TO RETAKE"
                            else -> "TAP TO OPEN CAMERA"
                        },
                        filled = closeUpVerified,
                        onClick = { launchCamera(1) },
                    )

                    Spacer(Modifier.height(8.dp))

                    SectionTitle("02 WIDE SHOT OF ROAD")
                    Text(
                        "STEP BACK - CAPTURE THE FULL ROAD WIDTH WITH POTHOLE VISIBLE - USED FOR LANE-POSITION",
                        color = Color.Gray,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    PhotoDropZone(
                        enabled = closeUpVerified && !photoValidating,
                        label = when {
                            !closeUpVerified -> "COMPLETE STEP 01 FIRST"
                            photoValidating && photoChoiceTarget == 2 -> "ANALYZING PHOTO..."
                            wideVerified -> "WIDE SHOT VERIFIED · TAP TO RETAKE"
                            else -> "TAP TO OPEN CAMERA"
                        },
                        filled = wideVerified,
                        onClick = { if (closeUpVerified) launchCamera(2) },
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Photos are checked on-device with TensorFlow Lite before you can submit.",
                        color = Color(0xFF5A5A5A),
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                    )

                    Spacer(Modifier.height(8.dp))

                    SectionTitle("03 ATTACH LOCATION")
                    Text(
                        "AUTO-DETECTED FROM GPS - USED ONLY TO TAG THE POTHOLE - NOT LINKED TO YOU",
                        color = Color.Gray,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    LocationBlock(
                        loading = locationLoading,
                        blockedReason = locationBlockedReason,
                        gpsOk = gpsOk,
                        latDisplay = latEffective,
                        lngDisplay = lngEffective,
                        onRetryGps = {
                            if (!locationPermitted()) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
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

                    if (duplicateBlock) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "You already reported this pothole here (${duplicateBlockingReport?.status?.displayLabel}). " +
                                "Submit again only after it is resolved (Completed).",
                            color = Color(0xFFB45309),
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                        )
                    }

                    if (!gpsOk && !locationLoading) {
                        Spacer(Modifier.height(4.dp))
                        Text("MANUAL ENTRY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkBlue)
                        Text(
                            "Paste a Google Maps URL or coordinates.",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                        Spacer(Modifier.height(2.dp))
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
                                placeholder = { Text("Maps URL", fontSize = 10.sp) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 10.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DarkBlue,
                                    unfocusedBorderColor = DarkBlue,
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            OutlinedTextField(
                                value = lngFieldInput,
                                onValueChange = {
                                    lngFieldInput = it
                                    useManualLocation = false
                                    manualCoordLat = null
                                    manualCoordLng = null
                                },
                                modifier = Modifier.width(88.dp),
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
                        Spacer(Modifier.height(4.dp))
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
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("USE THIS LOCATION", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    SectionTitle("04 POTHOLE POSITION IN PHOTO")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap the lane where the pothole sits — it helps the analyzer focus on the right area.",
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PositionTile(Modifier.weight(1f), PotholePosition.LEFT, selectedPosition) { selectedPosition = it }
                        PositionTile(Modifier.weight(1f), PotholePosition.MIDDLE, selectedPosition) { selectedPosition = it }
                        PositionTile(Modifier.weight(1f), PotholePosition.RIGHT, selectedPosition) { selectedPosition = it }
                    }

                    Spacer(Modifier.height(6.dp))

                    SectionTitle("05 HOW BAD IS IT?")
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SeverityTile(Modifier.weight(1f), PotholeSeverity.MINOR, selectedSeverity) { selectedSeverity = it }
                        SeverityTile(Modifier.weight(1f), PotholeSeverity.MODERATE, selectedSeverity) { selectedSeverity = it }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SeverityTile(Modifier.weight(1f), PotholeSeverity.SEVERE, selectedSeverity) { selectedSeverity = it }
                        SeverityTile(Modifier.weight(1f), PotholeSeverity.CRITICAL, selectedSeverity) { selectedSeverity = it }
                    }

                    aiRiskInsight?.let { insight ->
                        Spacer(Modifier.height(6.dp))
                        AiRiskSuggestionCard(
                            insight = insight,
                            onUseSuggested = { selectedSeverity = insight.suggestedSeverity },
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    SectionTitle("06 NOTE (OPTIONAL)")
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= noteMax) note = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Near bus stop, gets worse after rain...", fontSize = 10.sp) },
                        maxLines = 2,
                        textStyle = TextStyle(fontSize = 11.sp),
                        supportingText = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("${note.length}/$noteMax", fontSize = 9.sp, color = Color.Gray)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
                            unfocusedBorderColor = DarkBlue,
                            focusedTextColor = DarkBlue,
                            unfocusedTextColor = DarkBlue,
                            cursorColor = DarkBlue,
                            focusedPlaceholderColor = Color(0xFF757575),
                            unfocusedPlaceholderColor = Color(0xFF757575),
                        )
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 6.dp, end = 4.dp, bottom = 6.dp)
                ) {
                    Button(
                        onClick = {
                            if (!readyToSubmit) return@Button
                            val closeUri = closeUpUri ?: return@Button
                            val wideShotUri = wideUri ?: return@Button
                            val lat = latEffective ?: return@Button
                            val lng = lngEffective ?: return@Button
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    RecentReportsRepository.addReport(
                                        cityKey = reportCityKey,
                                        closeUpUri = closeUri,
                                        wideUri = wideShotUri,
                                        latitude = lat,
                                        longitude = lng,
                                        severity = selectedSeverity,
                                        note = note,
                                        reporterUserId = reporterUserId,
                                        submittedSignedIn = submittedWhileSignedIn,
                                    )
                                }
                                when (result) {
                                    is AddReportResult.Success -> {
                                        if (submittedWhileSignedIn) {
                                            if (!SupabaseClientProvider.isConfigured) {
                                                onSyncMessage(
                                                    "Report saved on this device only. " +
                                                        "Rebuild with SUPABASE_URL and SUPABASE_ANON_KEY in local.properties.",
                                                )
                                            } else {
                                                val pushed = withContext(Dispatchers.IO) {
                                                    ReportSyncRepository.pushReport(result.report)
                                                }
                                                if (pushed) {
                                                    onSyncMessage("Report submitted to the municipality.")
                                                } else {
                                                    ReportSyncRepository.enqueuePush(result.report)
                                                    onSyncMessage(
                                                        "Report saved on device. Cloud upload failed — " +
                                                            "sign in with your Supabase account and try again.",
                                                    )
                                                }
                                            }
                                        }
                                        onReportPersisted()
                                        onClose()
                                    }
                                    is AddReportResult.DuplicateActive -> {
                                        duplicateBlockingReport = result.existing
                                        showDuplicateDialog = true
                                    }
                                    AddReportResult.Failed -> { /* keep form open */ }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = readyToSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (readyToSubmit) DarkBlue else GrayButton,
                            contentColor = if (readyToSubmit) Color.White else Color.White.copy(alpha = 0.85f),
                            disabledContainerColor = GrayButton,
                            disabledContentColor = Color.White.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(footerLabel, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "NO EMAIL · NO ACCOUNT · NO IP STORED",
                        color = Color.Gray,
                        fontSize = 8.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
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
            .border(BorderStroke(1.dp, severityColor))
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
        Spacer(Modifier.height(2.dp))
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
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onUseSuggested,
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
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        color = DarkBlue,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
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
            .height(72.dp)
            .border(
                BorderStroke(2.dp, if (enabled) DarkBlue else GrayDisabled),
                RoundedCornerShape(0.dp)
            )
            .background(if (enabled) Color.White.copy(alpha = 0.5f) else Color(0xFFE0E0E0))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = if (enabled) DarkBlue else GrayDisabled,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) DarkBlue else GrayDisabled,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            if (filled) {
                Text("✓ Added", fontSize = 8.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocationBlock(
    loading: Boolean,
    blockedReason: String?,
    gpsOk: Boolean,
    latDisplay: Double?,
    lngDisplay: Double?,
    onRetryGps: () -> Unit,
    onOpenGpsSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, DarkBlue))
            .background(Color.White)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFFB71C1C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            when {
                loading -> Text("LOCATING…", color = DarkBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                gpsOk -> {
                    Text("LOCATION ATTACHED", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${"%.5f".format(latDisplay!!)}, ${"%.5f".format(lngDisplay!!)}",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
                blockedReason == "GPS_OFF" -> {
                    Text("GPS DISABLED", color = Color(0xFFB71C1C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Turn on location services.", fontSize = 8.sp, color = Color.Gray, maxLines = 2)
                }
                blockedReason == "NO_FIX" -> {
                    Text("LOCATION PENDING", color = Color(0xFFE65100), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Move outdoors and retry.", fontSize = 8.sp, color = Color.Gray, maxLines = 2)
                }
                else -> {
                    Text("LOCATION BLOCKED", color = Color(0xFFB71C1C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Allow location permission. On a deployed build you will be prompted to enable GPS.",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        maxLines = 3
                    )
                }
            }
        }
        if (!gpsOk && !loading) {
            Column {
                Button(
                    onClick = onRetryGps,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = DarkBlue),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text("RETRY GPS", fontWeight = FontWeight.Black, fontSize = 9.sp)
                }
                TextButton(onClick = onOpenGpsSettings, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                    Text("ENABLE GPS", fontSize = 8.sp, color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SeverityTile(
    modifier: Modifier,
    severity: PotholeSeverity,
    selected: PotholeSeverity,
    onSelect: (PotholeSeverity) -> Unit,
) {
    val sel = severity == selected
    Box(
        modifier = modifier
            .height(52.dp)
            .border(BorderStroke(1.dp, DarkBlue))
            .background(if (sel) OrangeSelect else Color.White.copy(alpha = 0.85f))
            .clickable { onSelect(severity) }
            .padding(4.dp)
    ) {
        Column {
            Text(
                "${severity.code} ${severity.title}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                color = if (sel) Color.White else DarkBlue
            )
            Text(
                severity.blurb,
                fontSize = 7.sp,
                lineHeight = 9.sp,
                color = if (sel) Color.White.copy(alpha = 0.95f) else Color.DarkGray,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PositionTile(
    modifier: Modifier,
    position: PotholePosition,
    selected: PotholePosition,
    onSelect: (PotholePosition) -> Unit,
) {
    val sel = position == selected
    Box(
        modifier = modifier
            .height(40.dp)
            .border(BorderStroke(1.dp, DarkBlue))
            .background(if (sel) OrangeSelect else Color.White.copy(alpha = 0.85f))
            .clickable { onSelect(position) }
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                position.displayLabel.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = if (sel) Color.White else DarkBlue,
            )
        }
    }
}

private fun createImageFile(context: Context): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return File.createTempFile("JPEG_${time}_", ".jpg", context.cacheDir)
}

private fun locationQualityScore(location: Location): Float {
    val ageSec = ((System.currentTimeMillis() - location.time).coerceAtLeast(0L) / 1000f)
        .coerceAtMost(180f)
    // Lower is better: prioritize accuracy, then freshness.
    return location.accuracy.coerceAtLeast(1f) + ageSec * 0.35f
}

private fun pickBetterLocation(current: Location?, candidate: Location?): Location? {
    if (candidate == null) return current
    if (current == null) return candidate
    return if (locationQualityScore(candidate) < locationQualityScore(current)) candidate else current
}

@SuppressLint("MissingPermission")
private suspend fun requestSingleUpdate(
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
private suspend fun fetchBestCalibratedLocation(context: Context): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var best: Location? = null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )

    for (provider in providers) {
        val last = runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        best = pickBetterLocation(best, last)
    }

    if (runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
        val gpsFresh = requestSingleUpdate(
            context = context,
            locationManager = lm,
            provider = LocationManager.GPS_PROVIDER,
            timeoutMs = 6500L,
        )
        best = pickBetterLocation(best, gpsFresh)
    }

    if (runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
        val networkFresh = requestSingleUpdate(
            context = context,
            locationManager = lm,
            provider = LocationManager.NETWORK_PROVIDER,
            timeoutMs = 3500L,
        )
        best = pickBetterLocation(best, networkFresh)
    }

    return best
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
