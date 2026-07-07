@file:Suppress("SpellCheckingInspection")

package com.example.potholereport.ui.emergency

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
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
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.potholereport.ui.theme.PotholeReportTheme
import java.util.Locale

private data class HelplineItem(
    val title: String,
    val subtitle: String,
    val number: String,
    val icon: @Composable () -> Unit
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
    val hasPermission: Boolean = false
)

@Composable
fun EmergencyScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var locationState by remember {
        mutableStateOf(LocationUiState(hasPermission = hasLocationPermission(context)))
    }
    var swipeEligible by remember { mutableStateOf(false) }
    var swipeDistance by remember { mutableStateOf(0f) }

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

    LaunchedEffect(locationState.hasPermission) {
        if (locationState.hasPermission) {
            locationState = resolveLocationState(context)
        }
    }

    val emergencySections = getHelplineSections(
        countryCode = locationState.countryCode,
        city = locationState.city
    )

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
            shape = RoundedCornerShape(0.dp),
            color = Color(0xFFF0F0EE),
            border = BorderStroke(1.dp, Color(0xFF2B2B2B))
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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        Spacer(Modifier.width(8.dp))
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
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )

                emergencySections.forEach { section ->
                    HelplineSectionCard(section = section)
                }

                DisclaimerCard()
            }
        }
    }
}

private fun getHelplineSections(countryCode: String, city: String?): List<HelplineSection> {
    val regionSubtitle = city?.let { "City Police ($it)" } ?: "City Police"
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
                    }
                )
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
        "GB" -> listOf(
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
                    }
                )
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
                    }
                )
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
                    HelplineItem("BREAKDOWN HELP", "Demo provider • 24x7", "1800-200-7777") {
                        Icon(Icons.Outlined.Build, contentDescription = null)
                    },
                    HelplineItem("TWO-WHEELER ASSIST", "Demo provider • puncture", "1800-200-8888") {
                        Icon(Icons.Outlined.Build, contentDescription = null)
                    }
                )
            )
        )
    }
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
private fun resolveLocationState(context: Context): LocationUiState {
    val locationManager = context.getSystemService(LocationManager::class.java) ?: return LocationUiState(
        statusText = "LOCATION SERVICE UNAVAILABLE - USING DEFAULT",
        hasPermission = true
    )

    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        ?: return LocationUiState(
            statusText = "LOCATION NOT FOUND - USING DEFAULT",
            hasPermission = true
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
            hasPermission = true
        )
    } catch (_: Exception) {
        LocationUiState(
            statusText = "LOCATION LOOKUP FAILED - USING DEFAULT",
            hasPermission = true
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
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F6)),
        border = BorderStroke(1.dp, Color(0xFF3A3A3A))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = Color(0xFF0E4AA2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SHOWING NUMBERS FOR ${countryName.uppercase(Locale.ROOT)}",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1D2A3B),
                    fontSize = 13.sp
                )
            }
            if (!cityName.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "DETECTED CITY: ${cityName.uppercase(Locale.ROOT)}",
                    color = Color(0xFF444444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = statusText,
                color = Color(0xFF666666),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                border = BorderStroke(1.dp, Color(0xFF1D1D1D)),
                shape = RoundedCornerShape(0.dp),
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
private fun HelplineSectionCard(section: HelplineSection) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F6)),
        border = BorderStroke(1.dp, Color(0xFF3A3A3A))
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
                HelplineRow(item = item)
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
private fun HelplineRow(item: HelplineItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = RoundedCornerShape(0.dp),
            color = Color(0xFFE4E4E1),
            border = BorderStroke(1.dp, Color(0xFF313131))
        ) {
            Box(contentAlignment = Alignment.Center) {
                item.icon()
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = Color(0xFF202020)
            )
            Text(
                text = item.subtitle,
                fontSize = 10.sp,
                color = Color(0xFF6D6D6D)
            )
        }

        Text(
            text = item.number,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1F2733),
            fontSize = if (item.number.length > 8) 20.sp else 30.sp
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.Call,
            contentDescription = "Call icon",
            tint = Color(0xFFABABAB),
            modifier = Modifier.size(16.dp)
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
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F6)),
        border = BorderStroke(1.dp, Color(0xFF3A3A3A))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "Location is used only to choose a relevant emergency number set. " +
                    "If location is unavailable, app falls back to India defaults. " +
                    "Verify regional numbers before use while travelling.",
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = Color(0xFF333333),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Government helpline sources: india.gov.in/directory/helpline · " +
                    "mha.gov.in (ERSS/112). Roadside 1800 numbers are demo providers, not government.",
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
