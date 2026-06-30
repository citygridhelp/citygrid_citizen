package com.example.potholereport.ui.home

import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.PotholeReportStatus
import com.example.potholereport.data.formatRecentReportCaption
import com.example.potholereport.data.isMeaningfulAreaName
import com.example.potholereport.data.resolveAreaLabel
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "REPORT DETAILS",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Text(
                    whenLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))

                if (report.submittedSignedIn) {
                    DetailSectionTitle("STATUS")
                    Text(
                        report.status.displayLabel.uppercase(Locale.US),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = reportStatusColor(report.status),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Updated by the municipal response team.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (report.hasAssignee()) {
                    DetailSectionTitle("ASSIGNED OFFICER")
                    DetailLine("Name", report.assigneeName)
                    DetailLine("Position", report.assigneePosition)
                    DetailLine("Corporation", report.assigneeCorporation)
                    DetailLine("Zone", report.assigneeZone)
                    DetailLine("Office", report.assigneeOfficeAddress)
                    Spacer(Modifier.height(12.dp))
                }

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

                Spacer(Modifier.height(14.dp))
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
                        if (showLocationMap) "Tap coordinates to hide map" else "Tap coordinates to view on map",
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

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))

                DetailSectionTitle("SEVERITY")
                Text(
                    report.severity.displayLabel,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    report.severity.blurb,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))

                DetailSectionTitle("NOTE")
                Text(
                    report.note.trim().ifBlank { "No note provided." },
                    fontSize = 13.sp,
                    color = if (report.note.isBlank()) {
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
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .background(Color(0xFFE8E8E8)),
            contentAlignment = Alignment.Center,
        ) {
            when (val b = bitmap) {
                null -> Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(32.dp),
                )
                else -> Image(
                    bitmap = b.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier.fillMaxWidth().height(128.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

private fun reportStatusColor(status: PotholeReportStatus): Color = when (status) {
    PotholeReportStatus.OPEN -> Color(0xFFA12A2A)
    PotholeReportStatus.IN_PROGRESS -> Color(0xFFBC7A1E)
    PotholeReportStatus.COMPLETED -> Color(0xFF0B7A42)
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
    )
    Spacer(Modifier.height(6.dp))
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
            "$label: ",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            value,
            fontSize = 12.sp,
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

private val ReportDetailMapTiles: XYTileSource = XYTileSource(
    "CartoPositronNoLabels",
    0,
    19,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/light_nolabels/",
        "https://b.basemaps.cartocdn.com/light_nolabels/",
        "https://c.basemaps.cartocdn.com/light_nolabels/",
    ),
    "\u00a9 OpenStreetMap contributors \u00b7 \u00a9 CARTO",
)

@Composable
private fun ReportLocationMapPreview(
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
                    maxZoomLevel = 19.0
                    minZoomLevel = 5.0
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
            "Open in Maps app",
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
