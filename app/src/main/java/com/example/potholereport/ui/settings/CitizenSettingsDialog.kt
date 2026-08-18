package com.example.potholereport.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.potholereport.R
import com.example.potholereport.data.CitizenUiSettingsRepository
import com.example.potholereport.data.MapCursorColor
import com.example.potholereport.data.MapCursorStyle
import com.example.potholereport.data.TripCaptureCabinSource
import com.example.potholereport.data.TripCaptureCameraMapLayout
import com.example.potholereport.data.TripCaptureDisplayMode
import com.example.potholereport.data.TripCaptureSource
import com.example.potholereport.data.TripFeatureGates
import com.example.potholereport.data.TripCompanionPairing
import com.example.potholereport.data.TripCompanionUsbLan
import com.example.potholereport.data.TripDeviceRole
import com.example.potholereport.data.TripUsbCameraInventory
import com.example.potholereport.data.WeatherAlertCadence
import com.example.potholereport.data.resolveTripCaptureRoles
import com.example.potholereport.ui.theme.CitizenBorder
import com.example.potholereport.ui.theme.CitizenIcon
import com.example.potholereport.ui.theme.CitizenInk
import com.example.potholereport.ui.theme.CitizenRadius
import com.example.potholereport.ui.theme.CitizenSelection
import com.example.potholereport.ui.theme.CitizenSpace
import com.example.potholereport.ui.theme.CitizenLightSurface
import com.example.potholereport.ui.theme.CitizenType

/**
 * Profile → Settings. Holds small UI preferences; more sections can be added later.
 */
@Composable
fun CitizenSettingsDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit = {},
    onOpenCompanionCamera: () -> Unit = {},
) {
    val context = LocalContext.current
    var cursorStyle by remember {
        mutableStateOf(CitizenUiSettingsRepository.mapCursorStyle(context))
    }
    var cursorColor by remember {
        mutableStateOf(CitizenUiSettingsRepository.mapCursorColor(context))
    }
    var captureDisplay by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCaptureDisplayMode(context))
    }
    var captureLayout by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCaptureCameraMapLayout(context))
    }
    var roadSource by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCaptureRoadSource(context))
    }
    var cabinSource by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCaptureCabinSource(context))
    }
    var deviceRole by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripDeviceRole(context))
    }
    var pairingCode by remember {
        mutableStateOf(CitizenUiSettingsRepository.tripCompanionPairingCode(context))
    }
    var imuBumpEnabled by remember {
        mutableStateOf(CitizenUiSettingsRepository.imuBumpDetectionEnabled(context))
    }
    var weatherAlertsEnabled by remember {
        mutableStateOf(CitizenUiSettingsRepository.weatherAlertsEnabled(context))
    }
    var weatherAlertCadence by remember {
        mutableStateOf(CitizenUiSettingsRepository.weatherAlertCadence(context))
    }
    var emailNotificationsEnabled by remember {
        mutableStateOf(CitizenUiSettingsRepository.emailNotificationsEnabled(context))
    }

    fun refreshCaptureFromPrefs() {
        captureDisplay = CitizenUiSettingsRepository.tripCaptureDisplayMode(context)
        captureLayout = CitizenUiSettingsRepository.tripCaptureCameraMapLayout(context)
        roadSource = CitizenUiSettingsRepository.tripCaptureRoadSource(context)
        cabinSource = CitizenUiSettingsRepository.tripCaptureCabinSource(context)
        deviceRole = CitizenUiSettingsRepository.tripDeviceRole(context)
    }

    LaunchedEffect(Unit) {
        CitizenUiSettingsRepository.activateUsbCamera1ForTripCapture(context)
        refreshCaptureFromPrefs()
        onSettingsChanged()
        val webcam = TripUsbCameraInventory.listHostDevices(context).firstOrNull { device ->
            !device.permissionGranted &&
                !device.likelyPhone &&
                (device.likelyUvc || TripUsbCameraInventory.looksLikeCameraName(device.name))
        }
        webcam?.let { TripUsbCameraInventory.requestPermissionForDeviceId(context, it.deviceId) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CitizenLightSurface {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = CitizenSpace.Xl),
            shape = CitizenRadius.Card,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CitizenSpace.Lg, vertical = CitizenSpace.Md)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Settings",
                        style = CitizenType.ScreenTitle,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(CitizenIcon.Tap),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(CitizenIcon.Header),
                        )
                    }
                }
                Text(
                    "Minor UI preferences for your device. More options will appear here later.",
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
                Spacer(modifier = Modifier.height(CitizenSpace.Lg))
                Text("Map cursor", style = CitizenType.Section)
                Text(
                    "Shown on the trip map while navigating.",
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                Text("Style", style = CitizenType.Label)
                Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                Text(
                    "Arrow, or rear-view Car / Bike for Play and navigation.",
                    style = CitizenType.Caption,
                    color = CitizenInk.Muted,
                )
                Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                Row(horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm)) {
                    MapCursorStyle.entries.forEach { style ->
                        val selected = cursorStyle == style
                        Surface(
                            shape = CitizenRadius.Card,
                            border = BorderStroke(
                                CitizenBorder.Thin,
                                if (selected) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                            ),
                            color = if (selected) CitizenSelection.ActiveSoft else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    cursorStyle = style
                                    CitizenUiSettingsRepository.setMapCursorStyle(context, style)
                                    onSettingsChanged()
                                },
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(CitizenSpace.Sm),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                when (style) {
                                    MapCursorStyle.ARROW -> {
                                        Icon(
                                            imageVector = Icons.Filled.Navigation,
                                            contentDescription = null,
                                            tint = if (selected) {
                                                CitizenSelection.Active
                                            } else {
                                                Color(0xFF374151)
                                            },
                                            modifier = Modifier.size(CitizenIcon.FabGlyph),
                                        )
                                    }
                                    MapCursorStyle.CAR -> {
                                        Image(
                                            painter = painterResource(R.drawable.map_cursor_car_rear),
                                            contentDescription = "Car cursor",
                                            modifier = Modifier.size(40.dp),
                                        )
                                    }
                                    MapCursorStyle.BIKE -> {
                                        Image(
                                            painter = painterResource(R.drawable.map_cursor_bike_rear),
                                            contentDescription = "Bike cursor",
                                            modifier = Modifier.size(40.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                                Text(
                                    style.label,
                                    style = CitizenType.Chip,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(CitizenSpace.Md))
                Text("Color", style = CitizenType.Label)
                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                ) {
                    MapCursorColor.entries.forEach { color ->
                        val selected = cursorColor == color
                        Box(
                            modifier = Modifier
                                .size(CitizenIcon.Tap)
                                .clip(CircleShape)
                                .background(Color(color.argb))
                                .border(
                                    width = if (selected) 3.dp else CitizenBorder.Thin,
                                    color = if (selected) Color(0xFF111827) else Color.White,
                                    shape = CircleShape,
                                )
                                .clickable {
                                    cursorColor = color
                                    CitizenUiSettingsRepository.setMapCursorColor(context, color)
                                    onSettingsChanged()
                                },
                            contentAlignment = Alignment.Center,
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                Text(
                    cursorColor.label,
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )

                Spacer(modifier = Modifier.height(CitizenSpace.Lg))
                HorizontalDivider(color = CitizenBorder.Secondary)
                Spacer(modifier = Modifier.height(CitizenSpace.Lg))

                Text("Device role", style = CitizenType.Section)
                Text(
                    if (TripFeatureGates.COMPANION_CAMERA_ENABLED) {
                        "How this phone is used with City Grid. Two phones: set one to Nav + report " +
                            "and the other to Camera only, same pairing code. Link over Wi‑Fi, hotspot, " +
                            "or a USB cable (USB tethering on the camera phone)."
                    } else {
                        "How this phone is used with City Grid. Companion phone roles are work in progress — use Both for now."
                    },
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                ) {
                    TripDeviceRole.entries.forEach { role ->
                        val roleEnabled = TripFeatureGates.isDeviceRoleEnabled(role)
                        SettingsChoiceChip(
                            label = role.label,
                            selected = deviceRole == role,
                            enabled = roleEnabled,
                            comingSoon = !roleEnabled,
                            onClick = {
                                deviceRole = role
                                CitizenUiSettingsRepository.applyTripDeviceRole(context, role)
                                refreshCaptureFromPrefs()
                                onSettingsChanged()
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                Text(
                    deviceRole.helper,
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )

                if (deviceRole.showsTripCaptureSettings) {
                    Spacer(modifier = Modifier.height(CitizenSpace.Md))
                    Text("Bump detection", style = CitizenType.Label)
                    Text(
                        if (TripFeatureGates.IMU_BUMP_ENABLED) {
                            "While Navigate is on, sense jolts and save GPS to Unconfirmed " +
                                "(road video optional). With camera on, High confidence can draft a report."
                        } else {
                            "Work in progress — bump sensing is temporarily unavailable while we finish testing."
                        },
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                        verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    ) {
                        SettingsChoiceChip(
                            label = "On",
                            selected = imuBumpEnabled && TripFeatureGates.IMU_BUMP_ENABLED,
                            enabled = TripFeatureGates.IMU_BUMP_ENABLED,
                            comingSoon = !TripFeatureGates.IMU_BUMP_ENABLED,
                            onClick = {
                                imuBumpEnabled = true
                                CitizenUiSettingsRepository.setImuBumpDetectionEnabled(context, true)
                                onSettingsChanged()
                            },
                        )
                        SettingsChoiceChip(
                            label = "Off",
                            selected = !imuBumpEnabled || !TripFeatureGates.IMU_BUMP_ENABLED,
                            enabled = TripFeatureGates.IMU_BUMP_ENABLED,
                            onClick = {
                                imuBumpEnabled = false
                                CitizenUiSettingsRepository.setImuBumpDetectionEnabled(context, false)
                                onSettingsChanged()
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(CitizenSpace.Md))
                    Text("Auto-detect from road video", style = CitizenType.Label)
                    Text(
                        "While Navigate and the road-video toggle are on, score live camera " +
                            "frames for potholes (like the reference YOLO boxes). High → auto-report " +
                            "when signed in; Medium → Unconfirmed. First step instead of manual Report.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    var visionAuto by remember {
                        mutableStateOf(CitizenUiSettingsRepository.roadVideoAutoDetectEnabled(context))
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                        verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    ) {
                        SettingsChoiceChip(
                            label = "On",
                            selected = visionAuto,
                            enabled = true,
                            onClick = {
                                visionAuto = true
                                CitizenUiSettingsRepository.setRoadVideoAutoDetectEnabled(context, true)
                                onSettingsChanged()
                            },
                        )
                        SettingsChoiceChip(
                            label = "Off",
                            selected = !visionAuto,
                            enabled = true,
                            onClick = {
                                visionAuto = false
                                CitizenUiSettingsRepository.setRoadVideoAutoDetectEnabled(context, false)
                                onSettingsChanged()
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(CitizenSpace.Md))
                    Text("Avoid / swerve", style = CitizenType.Label)
                    Text(
                        "When you dodge under ~2 m off the route with a sensed bump/jolt, ask what it was. " +
                            "No photo → bump mark; ditch/pothole → that coordinate as a pothole. " +
                            "Overtaking without a jolt is ignored.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    var swerveEnabled by remember {
                        mutableStateOf(CitizenUiSettingsRepository.swerveDetectionEnabled(context))
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                        verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    ) {
                        SettingsChoiceChip(
                            label = "On",
                            selected = swerveEnabled,
                            enabled = true,
                            onClick = {
                                swerveEnabled = true
                                CitizenUiSettingsRepository.setSwerveDetectionEnabled(context, true)
                                onSettingsChanged()
                            },
                        )
                        SettingsChoiceChip(
                            label = "Off",
                            selected = !swerveEnabled,
                            enabled = true,
                            onClick = {
                                swerveEnabled = false
                                CitizenUiSettingsRepository.setSwerveDetectionEnabled(context, false)
                                onSettingsChanged()
                            },
                        )
                    }
                }

                if (deviceRole.usesCompanionPairing && TripFeatureGates.COMPANION_CAMERA_ENABLED) {
                    Spacer(modifier = Modifier.height(CitizenSpace.Md))
                    Text("Pairing code", style = CitizenType.Label)
                    Text(
                        "Enter the same code on both phones. Stills go over Wi‑Fi, hotspot, " +
                            "or USB tethering (plug the camera phone into this nav phone, then " +
                            "turn on USB tethering on the camera phone).",
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    ) {
                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { raw ->
                                val next = TripCompanionPairing.normalize(raw)
                                pairingCode = next
                                CitizenUiSettingsRepository.setTripCompanionPairingCode(context, next)
                                onSettingsChanged()
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                val next = TripCompanionPairing.generateCode()
                                pairingCode = next
                                CitizenUiSettingsRepository.setTripCompanionPairingCode(context, next)
                                onSettingsChanged()
                            },
                        ) {
                            Text("New")
                        }
                    }
                    TextButton(onClick = { TripCompanionUsbLan.openTetherSettings(context) }) {
                        Text("Open USB tethering settings")
                    }
                } else if (!TripFeatureGates.COMPANION_CAMERA_ENABLED) {
                    Spacer(modifier = Modifier.height(CitizenSpace.Md))
                    Text("Companion phone", style = CitizenType.Label)
                    Text(
                        "Work in progress — companion camera pairing is unavailable while testing continues.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Muted,
                    )
                }

                if (deviceRole == TripDeviceRole.CAMERA_ONLY && TripFeatureGates.COMPANION_CAMERA_ENABLED) {
                    Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                    TextButton(onClick = onOpenCompanionCamera) {
                        Text("Open companion camera", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Keep this open while navigating. When the main phone turns road video ON, " +
                            "stills auto-send over Wi‑Fi, hotspot, or USB tethering — no Capture button.",
                        style = CitizenType.Caption,
                        color = CitizenInk.Muted,
                    )
                }

                if (deviceRole.showsTripCaptureSettings) {
                    Spacer(modifier = Modifier.height(CitizenSpace.Lg))
                    HorizontalDivider(color = CitizenBorder.Secondary)
                    Spacer(modifier = Modifier.height(CitizenSpace.Lg))

                    Text("Trip capture", style = CitizenType.Section)
                    Text(
                        "Road and cabin camera sources for navigation. " +
                            "Normal Report still uses the camera when you submit. " +
                            "USB camera 1/2 is for a UVC webcam (OTG). " +
                            "Another phone is a second City Grid phone — not a USB webcam.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Md))

                    Text("During navigation", style = CitizenType.Label)
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                        verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    ) {
                        TripCaptureDisplayMode.entries.forEach { mode ->
                            SettingsChoiceChip(
                                label = mode.label,
                                selected = captureDisplay == mode,
                                enabled = true,
                                onClick = {
                                    captureDisplay = mode
                                    CitizenUiSettingsRepository.setTripCaptureDisplayMode(context, mode)
                                    onSettingsChanged()
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    Text(
                        captureDisplay.helper,
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                    )

                    if (captureDisplay == TripCaptureDisplayMode.CAMERA_AND_MAP) {
                        Spacer(modifier = Modifier.height(CitizenSpace.Md))
                        Text("Camera + map layout", style = CitizenType.Label)
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                            verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                        ) {
                            TripCaptureCameraMapLayout.entries.forEach { layout ->
                                SettingsChoiceChip(
                                    label = layout.label,
                                    selected = captureLayout == layout,
                                    enabled = layout.selectable,
                                    comingSoon = !layout.selectable,
                                    onClick = {
                                        captureLayout = layout
                                        CitizenUiSettingsRepository.setTripCaptureCameraMapLayout(
                                            context,
                                            layout,
                                        )
                                        onSettingsChanged()
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(CitizenSpace.Md))
                    Text("Road (rear)", style = CitizenType.Label)
                    Text(
                        "Feed used for pothole capture and analysis.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                        verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    ) {
                        TripCaptureSource.entries.forEach { source ->
                            val enabled = TripFeatureGates.isRoadSourceEnabled(source)
                            SettingsChoiceChip(
                                label = source.label,
                                selected = roadSource == source,
                                enabled = enabled,
                                comingSoon = !enabled,
                                onClick = {
                                    CitizenUiSettingsRepository.setTripCaptureRoadSource(context, source)
                                    refreshCaptureFromPrefs()
                                    onSettingsChanged()
                                },
                            )
                        }
                    }
                    UsbOtgCameraPanel(
                        roadSource = roadSource,
                        cabinSource = cabinSource,
                        onRoadSourceChange = { source ->
                            CitizenUiSettingsRepository.setTripCaptureRoadSource(context, source)
                            refreshCaptureFromPrefs()
                            onSettingsChanged()
                        },
                        onCabinSourceChange = { source ->
                            cabinSource = source
                            CitizenUiSettingsRepository.setTripCaptureCabinSource(context, source)
                            onSettingsChanged()
                        },
                    )

                    Spacer(modifier = Modifier.height(CitizenSpace.Md))
                    Text("Cabin (front)", style = CitizenType.Label)
                    Text(
                        "Optional second view (PIP). Use the other phone lens than Road.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                        verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    ) {
                        TripCaptureCabinSource.entries.forEach { source ->
                            val enabled = TripFeatureGates.isCabinSourceEnabled(source)
                            SettingsChoiceChip(
                                label = source.label,
                                selected = cabinSource == source,
                                enabled = enabled,
                                comingSoon = !enabled,
                                onClick = {
                                    cabinSource = source
                                    CitizenUiSettingsRepository.setTripCaptureCabinSource(context, source)
                                    onSettingsChanged()
                                },
                            )
                        }
                    }
                    val roleHint = remember(roadSource, cabinSource) {
                        resolveTripCaptureRoles(roadSource, cabinSource)
                    }
                    if (roleHint.cabinConflictWithRoad) {
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        Text(
                            "Cabin matches Road’s lens — cabin preview stays off until you pick the other camera.",
                            style = CitizenType.Caption,
                            color = CitizenInk.Warning,
                        )
                    }
                    if (deviceRole == TripDeviceRole.NAV_AND_REPORT &&
                        roadSource == TripCaptureSource.COMPANION
                    ) {
                        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                        Text(
                            "Road = Another phone — on the camera phone set Camera only, same pairing code, " +
                                "Open companion camera. USB cable: enable USB tethering on that camera phone.",
                            style = CitizenType.Caption,
                            color = CitizenInk.Muted,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(CitizenSpace.Lg))
                HorizontalDivider(color = CitizenBorder.Secondary)
                Spacer(modifier = Modifier.height(CitizenSpace.Lg))
                Text("Email notifications", style = CitizenType.Section)
                Text(
                    "Report confirmation to your linked email when you submit a pothole.",
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                ) {
                    SettingsChoiceChip(
                        label = "On",
                        selected = emailNotificationsEnabled,
                        enabled = true,
                        onClick = {
                            emailNotificationsEnabled = true
                            CitizenUiSettingsRepository.setEmailNotificationsEnabled(context, true)
                            onSettingsChanged()
                        },
                    )
                    SettingsChoiceChip(
                        label = "Off",
                        selected = !emailNotificationsEnabled,
                        enabled = true,
                        onClick = {
                            emailNotificationsEnabled = false
                            CitizenUiSettingsRepository.setEmailNotificationsEnabled(context, false)
                            onSettingsChanged()
                        },
                    )
                }
                Spacer(modifier = Modifier.height(CitizenSpace.Lg))
                HorizontalDivider(color = CitizenBorder.Secondary)
                Spacer(modifier = Modifier.height(CitizenSpace.Lg))
                Text("Weather alerts", style = CitizenType.Section)
                Text(
                    "Rain and weather updates on the Alerts page. Choose how often to refresh.",
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                ) {
                    SettingsChoiceChip(
                        label = "On",
                        selected = weatherAlertsEnabled,
                        enabled = true,
                        onClick = {
                            weatherAlertsEnabled = true
                            CitizenUiSettingsRepository.setWeatherAlertsEnabled(context, true)
                            onSettingsChanged()
                        },
                    )
                    SettingsChoiceChip(
                        label = "Off",
                        selected = !weatherAlertsEnabled,
                        enabled = true,
                        onClick = {
                            weatherAlertsEnabled = false
                            CitizenUiSettingsRepository.setWeatherAlertsEnabled(context, false)
                            onSettingsChanged()
                        },
                    )
                }
                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                    verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
                ) {
                    WeatherAlertCadence.alertEntries.forEach { option ->
                        SettingsChoiceChip(
                            label = option.chipLabel,
                            selected = weatherAlertCadence == option,
                            enabled = weatherAlertsEnabled,
                            onClick = {
                                weatherAlertCadence = option
                                CitizenUiSettingsRepository.setWeatherAlertCadence(context, option)
                                onSettingsChanged()
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(CitizenSpace.Md))
                HorizontalDivider(color = CitizenBorder.Secondary)
                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                Text(
                    "Stills for reports only (no continuous video yet). " +
                        "Modes apply while Navigate is on.",
                    style = CitizenType.Caption,
                    color = CitizenInk.Muted,
                )
            }
        }
        }
    }
}

@Composable
private fun SettingsChoiceChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    comingSoon: Boolean = false,
    onClick: () -> Unit,
) {
    val border = when {
        !enabled -> CitizenSelection.InactiveBorder.copy(alpha = 0.45f)
        selected -> CitizenSelection.Active
        else -> CitizenSelection.InactiveBorder
    }
    val bg = when {
        !enabled -> Color.Transparent
        selected -> CitizenSelection.ActiveSoft
        else -> Color.Transparent
    }
    Surface(
        shape = CitizenRadius.Card,
        border = BorderStroke(CitizenBorder.Thin, border),
        color = bg,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.55f)
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        Column(modifier = Modifier.padding(horizontal = CitizenSpace.Sm, vertical = CitizenSpace.Xs)) {
            Text(
                label,
                style = CitizenType.Chip,
                fontWeight = if (selected && enabled) FontWeight.SemiBold else FontWeight.Normal,
                color = if (enabled) Color.Unspecified else CitizenInk.Disabled,
            )
            if (comingSoon) {
                Text(
                    "Work in progress",
                    style = CitizenType.Caption,
                    color = CitizenInk.Muted,
                )
            }
        }
    }
}

@Composable
private fun UsbOtgCameraPanel(
    roadSource: TripCaptureSource,
    cabinSource: TripCaptureCabinSource,
    onRoadSourceChange: (TripCaptureSource) -> Unit,
    onCabinSourceChange: (TripCaptureCabinSource) -> Unit,
) {
    val context = LocalContext.current
    var scanEpoch by remember { mutableIntStateOf(0) }
    var permissionNote by remember { mutableStateOf<String?>(null) }
    val devices = remember(scanEpoch) {
        TripUsbCameraInventory.listHostDevices(context)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionNote = if (granted) {
            "Phone camera permission on. Allow USB next if Android asks."
        } else {
            "Camera permission is needed to preview a USB webcam."
        }
        scanEpoch++
    }

    DisposableEffect(Unit) {
        val unplug = TripUsbCameraInventory.registerHotplugReceiver(context) {
            scanEpoch++
            val webcam = TripUsbCameraInventory.listHostDevices(context).firstOrNull { device ->
                !device.permissionGranted &&
                    !device.likelyPhone &&
                    (device.likelyUvc || TripUsbCameraInventory.looksLikeCameraName(device.name))
            }
            webcam?.let {
                TripUsbCameraInventory.requestPermissionForDeviceId(context, it.deviceId)
            }
        }
        val unperm = TripUsbCameraInventory.registerPermissionReceiver(context) { _, granted ->
            permissionNote = if (granted) {
                "USB access allowed. Link it as USB camera 1 or 2 below."
            } else {
                "USB access denied. Tap Allow USB access and choose this app."
            }
            scanEpoch++
        }
        onDispose {
            unplug()
            unperm()
        }
    }

    Spacer(modifier = Modifier.height(CitizenSpace.Md))
    Text("USB camera (OTG webcam)", style = CitizenType.Label)
    Text(
        "A UVC webcam on OTG uses USB camera 1/2. A second Android phone on the same cable is not a webcam — use Device role Camera only + USB tethering instead.",
        style = CitizenType.Helper,
        color = CitizenInk.Helper,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
    ) {
        TextButton(
            onClick = {
                val needCamera = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) != PackageManager.PERMISSION_GRANTED
                if (needCamera) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    scanEpoch++
                    permissionNote = if (TripUsbCameraInventory.listHostDevices(context).isEmpty()) {
                        "No USB device yet. Connect OTG and tap Detect again."
                    } else {
                        "Found ${TripUsbCameraInventory.listHostDevices(context).size} USB device(s)."
                    }
                }
            },
        ) {
            Text("Detect USB camera")
        }
    }
    if (permissionNote != null) {
        Text(
            permissionNote.orEmpty(),
            style = CitizenType.Caption,
            color = CitizenInk.Helper,
        )
    }
    if (devices.isEmpty()) {
        Text(
            "No USB device detected. Use an OTG adapter, plug the camera in, then tap Detect USB camera.",
            style = CitizenType.Caption,
            color = CitizenInk.Warning,
        )
        return
    }
    val phones = devices.filter { it.likelyPhone }
    val webcams = devices.filter { !it.likelyPhone }
    phones.forEach { device ->
        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
        Text(
            "${device.name} · phone (not a webcam)",
            style = CitizenType.Caption,
            color = CitizenInk.Helper,
        )
        Text(
            "Use this as the Camera-only phone: same pairing code, Open companion camera, then USB tethering. Do not pick USB camera 1.",
            style = CitizenType.Caption,
            color = CitizenInk.Warning,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
            verticalArrangement = Arrangement.spacedBy(CitizenSpace.Xs),
        ) {
            TextButton(
                onClick = { onRoadSourceChange(TripCaptureSource.COMPANION) },
            ) {
                Text(
                    if (roadSource == TripCaptureSource.COMPANION) {
                        "Road = Another phone ✓"
                    } else {
                        "Use as Another phone"
                    },
                )
            }
            TextButton(onClick = { TripCompanionUsbLan.openTetherSettings(context) }) {
                Text("USB tethering settings")
            }
        }
    }
    webcams.forEachIndexed { index, device ->
        Spacer(modifier = Modifier.height(CitizenSpace.Xs))
        Text(
            buildString {
                append(device.name)
                if (device.likelyUvc) append(" · UVC")
                append(if (device.permissionGranted) " · USB allowed" else " · USB permission needed")
            },
            style = CitizenType.Caption,
            color = CitizenInk.Helper,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
            verticalArrangement = Arrangement.spacedBy(CitizenSpace.Xs),
        ) {
            if (!device.permissionGranted) {
                TextButton(
                    onClick = {
                        TripUsbCameraInventory.requestPermissionForDeviceId(context, device.deviceId)
                        permissionNote = "Waiting for USB permission…"
                    },
                ) {
                    Text("Allow USB access")
                }
            }
            val roadSlot = if (index == 0) TripCaptureSource.USB_1 else TripCaptureSource.USB_2
            val cabinSlot =
                if (index == 0) TripCaptureCabinSource.USB_1 else TripCaptureCabinSource.USB_2
            TextButton(
                onClick = { onRoadSourceChange(roadSlot) },
            ) {
                Text(if (roadSource == roadSlot) "Road camera ✓" else "Use as road camera")
            }
            TextButton(
                onClick = { onCabinSourceChange(cabinSlot) },
            ) {
                Text(if (cabinSource == cabinSlot) "Cabin camera ✓" else "Use as cabin camera")
            }
        }
    }
}
