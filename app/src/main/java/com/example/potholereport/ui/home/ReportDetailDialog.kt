package com.example.potholereport.ui.home

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.CivicIssueType
import com.example.potholereport.data.IncidentTimeOfDay
import com.example.potholereport.data.ManholeCondition
import com.example.potholereport.data.TrafficFacingMode
import com.example.potholereport.data.formatRecentReportCaption
import com.example.potholereport.data.isMeaningfulAreaName
import com.example.potholereport.data.resolveAreaLabel
import com.example.potholereport.ui.theme.CitizenBorder
import com.example.potholereport.ui.theme.CitizenIcon
import com.example.potholereport.ui.theme.CitizenInk
import com.example.potholereport.ui.theme.CitizenRadius
import com.example.potholereport.ui.theme.CitizenSpace
import com.example.potholereport.ui.theme.CitizenType
import com.example.potholereport.ui.theme.citizenReportStatusColor
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Vertical gap that avoids Spacer(modifier=...) named-arg issues in this module. */
@Composable
private fun Gap(height: Dp) {
    Box(modifier = Modifier.height(height))
}

@Composable
fun ReportDetailDialog(
    report: PersistedPotholeReport,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var closeBitmap by remember(report.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var wideBitmap by remember(report.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var areaLabel by remember(report.id) {
        mutableStateOf(report.areaLabel.takeIf { isMeaningfulAreaName(it) }.orEmpty())
    }
    var showLocationMap by remember(report.id) { mutableStateOf(false) }

    LaunchedEffect(report.id) {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
    }

    LaunchedEffect(report.id, report.photoPath, report.widePhotoPath, report.storageClosePath, report.storageWidePath) {
        closeBitmap = com.example.potholereport.data.remote.ReportPhotoCache.loadCloseBitmap(context, report)
        wideBitmap = com.example.potholereport.data.remote.ReportPhotoCache.loadWideBitmap(context, report)
    }
    LaunchedEffect(report.id, report.areaLabel, report.latitude, report.longitude) {
        if (isMeaningfulAreaName(areaLabel)) return@LaunchedEffect
        if (!report.hasCoordinates()) return@LaunchedEffect
        val resolved = withContext(Dispatchers.IO) {
            resolveAreaLabel(context, report.latitude, report.longitude)
        }
        if (isMeaningfulAreaName(resolved)) areaLabel = resolved
    }

    val whenLabel = remember(report.createdAtMs, areaLabel) {
        formatRecentReportCaption(report.createdAtMs, areaLabel)
    }
    val cityName = formatCityDisplayNameForReport(report.cityKey)
    val coordsText = if (report.hasCoordinates()) {
        String.format(Locale.US, "%.5f, %.5f", report.latitude, report.longitude)
    } else {
        "Not recorded"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CitizenRadius.Card,
            border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(CitizenSpace.Md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "REPORT DETAILS",
                        style = CitizenType.ScreenTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(CitizenIcon.Tap)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(CitizenIcon.Header),
                        )
                    }
                }
                Text(
                    text = whenLabel,
                    style = CitizenType.Chip,
                    color = CitizenInk.Helper,
                )
                Gap(CitizenSpace.Md)

                if (report.submittedSignedIn) {
                    DetailSectionTitle("STATUS")
                    Text(
                        text = report.status.displayLabelCaps,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = citizenReportStatusColor(report.status),
                    )
                    Gap(CitizenSpace.Xs)
                    Text(
                        text = "Updated by the municipal response team.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    Gap(CitizenSpace.Md)
                }

                if (report.hasAssignee()) {
                    DetailSectionTitle("ACCOUNTABILITY (REFERENCE ONLY)")
                    Text(
                        text = ReportAssigneeDisplay.routingDisclaimer(report),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    Text(
                        text = com.example.potholereport.ui.theme.CitizenAccountabilityCopy.CARD_DISCLAIMER,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Gap(CitizenSpace.Sm)
                    DetailLine(ReportAssigneeDisplay.authorityLabel(), report.assigneeCorporation)
                    DetailLine(
                        ReportAssigneeDisplay.municipalUnitLabel(report),
                        report.assigneeZone,
                    )
                    ReportAssigneeDisplay.wardLabel(report)?.let { ward ->
                        DetailLine("Ward", ward)
                    }
                    DetailLine("Officer", report.assigneeName)
                    DetailLine("Role", report.assigneePosition)
                    DetailLine("Office", report.assigneeOfficeAddress)
                    Gap(CitizenSpace.Md)
                }

                val strayPlaceOnly = report.issueType.usesIncidentTimeForm &&
                    report.widePhotoPath.isBlank() &&
                    report.storageWidePath.isBlank()
                if (strayPlaceOnly) {
                    ReportPhotoPanel(
                        label = "PLACE / DOGS",
                        bitmap = closeBitmap,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReportPhotoPanel(
                            label = "CLOSE-UP",
                            bitmap = closeBitmap,
                            modifier = Modifier.weight(1f),
                        )
                        ReportPhotoPanel(
                            label = "WIDE CONTEXT",
                            bitmap = wideBitmap,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Gap(14.dp)
                DetailSectionTitle("LOCATION")
                DetailLine(
                    "Area",
                    areaLabel.takeIf { isMeaningfulAreaName(it) } ?: "—",
                )
                DetailLine("City", cityName)
                DetailLine(
                    label = "Coordinates",
                    value = coordsText,
                    clickable = report.hasCoordinates(),
                    onClick = {
                        if (report.hasCoordinates()) {
                            showLocationMap = !showLocationMap
                        }
                    },
                )
                if (report.hasCoordinates()) {
                    Text(
                        text = if (showLocationMap) {
                            "Tap coordinates to hide map"
                        } else {
                            "Tap coordinates to view on map"
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                    )
                }
                if (showLocationMap && report.hasCoordinates()) {
                    ReportLocationMapPreview(
                        latitude = report.latitude,
                        longitude = report.longitude,
                        areaHint = areaLabel.takeIf { isMeaningfulAreaName(it) },
                        onOpenExternalMap = {
                            val label = areaLabel.takeIf { isMeaningfulAreaName(it) } ?: "Pothole report"
                            val uri = Uri.parse(
                                "geo:${report.latitude},${report.longitude}" +
                                    "?q=${report.latitude},${report.longitude}(${Uri.encode(label)})",
                            )
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        },
                    )
                }

                Gap(10.dp)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Gap(10.dp)

                if (report.issueType == CivicIssueType.MANHOLE) {
                    val condition = report.manholeConditionEnum()
                    DetailSectionTitle("MANHOLE STATE")
                    Text(
                        text = condition?.displayLabel ?: "Not recorded",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = condition?.blurb ?: "Open, closed, broken, or overflowing was not saved.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                } else if (report.issueType.usesIncidentTimeForm) {
                    val period = report.incidentTimeOfDayEnum()
                    DetailSectionTitle("TIME OF DAY")
                    Text(
                        text = period?.displayLabel ?: "Not recorded",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    period?.let {
                        Text(
                            text = it.blurb,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                } else {
                    DetailSectionTitle("SEVERITY")
                    Text(
                        text = report.severity.displayLabel,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = report.severity.blurb,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "For drivers: ${report.severity.driverPhrase}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
                }

                if (report.issueType.usesLaneCapture) {
                    Gap(10.dp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Gap(10.dp)
                    DetailSectionTitle("POSITION ON ROAD")
                    val facingLabel = when (report.trafficFacingMode()) {
                        TrafficFacingMode.FACING_CAMERA -> "Toward camera"
                        TrafficFacingMode.AWAY_FROM_CAMERA -> "Against traffic"
                        TrafficFacingMode.UNKNOWN -> null
                    }
                    Text(
                        text = "${report.potholePositionEnum().code} · ${report.potholePositionEnum().displayLabel}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    facingLabel?.let { facing ->
                        Text(
                            text = "Traffic: $facing",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                Gap(10.dp)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Gap(10.dp)

                DetailSectionTitle(
                    if (report.issueType.usesIncidentTimeForm) "LANDMARK" else "NOTE",
                )
                val noteBody = IncidentTimeOfDay.stripFromNote(
                    ManholeCondition.stripFromNote(report.note),
                ).trim()
                Text(
                    text = noteBody.ifBlank {
                        if (report.issueType.usesIncidentTimeForm) {
                            "No landmark provided."
                        } else {
                            "No note provided."
                        }
                    },
                    fontSize = 13.sp,
                    color = if (noteBody.isBlank()) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun ReportPhotoPanel(
    label: String,
    bitmap: android.graphics.Bitmap?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp,
        )
        Gap(CitizenSpace.Xs)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .background(Color(0xFFE8E8E8)),
            contentAlignment = Alignment.Center,
        ) {
            when (val b = bitmap) {
                null -> Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = CitizenInk.Disabled,
                    modifier = Modifier.size(32.dp),
                )
                else -> Image(
                    bitmap = b.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        style = CitizenType.Caption,
        fontWeight = FontWeight.Bold,
        color = CitizenInk.Muted,
    )
    Gap(CitizenSpace.Xs)
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    clickable: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = "$label: ",
            style = CitizenType.Chip,
            fontWeight = FontWeight.SemiBold,
            color = CitizenInk.Helper,
        )
        Text(
            text = value,
            style = CitizenType.Chip,
            color = if (clickable) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (clickable) TextDecoration.Underline else null,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (clickable) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

internal val ReportDetailMapTiles: XYTileSource = XYTileSource(
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

@Composable
internal fun ReportLocationMapPreview(
    latitude: Double,
    longitude: Double,
    areaHint: String?,
    onOpenExternalMap: () -> Unit,
) {
    val mapHolder = remember { object { var map: MapView? = null } }
    val point = remember(latitude, longitude) { GeoPoint(latitude, longitude) }

    DisposableEffect(Unit) {
        onDispose {
            mapHolder.map?.onPause()
            mapHolder.map = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .background(Color(0xFFE8E8E8)),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(ReportDetailMapTiles)
                    setMultiTouchControls(true)
                    isHorizontalMapRepetitionEnabled = false
                    isVerticalMapRepetitionEnabled = false
                    maxZoomLevel = CartoBasemapUsefulMaxZoom
                    minZoomLevel = 5.0
                    installCartoLabelDarkeningTilesOverlay()
                    controller.setZoom(16.0)
                    controller.setCenter(point)
                    overlays.add(
                        Marker(this).apply {
                            position = point
                            title = areaHint ?: "Report location"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        },
                    )
                    mapHolder.map = this
                    onResume()
                }
            },
            update = { map ->
                map.controller.setCenter(point)
                mapHolder.map = map
            },
        )
        Text(
            text = "Open in Maps app",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onOpenExternalMap),
        )
    }
}

private fun decodeReportBitmap(path: String): android.graphics.Bitmap? {
    if (path.isBlank() || !File(path).exists()) return null
    return runCatching {
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        BitmapFactory.decodeFile(path, opts)
    }.getOrNull()
}

private fun formatCityDisplayNameForReport(cityKey: String): String =
    cityKey.trim()
        .split(' ', '_')
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
