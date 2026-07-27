package com.example.potholereport.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.potholereport.data.CitizenUiSettingsRepository
import com.example.potholereport.data.MapCursorColor
import com.example.potholereport.data.MapCursorStyle

/**
 * Profile → Settings. Holds small UI preferences; more sections can be added later.
 */
@Composable
fun CitizenSettingsDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    var cursorStyle by remember {
        mutableStateOf(CitizenUiSettingsRepository.mapCursorStyle(context))
    }
    var cursorColor by remember {
        mutableStateOf(CitizenUiSettingsRepository.mapCursorColor(context))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Text(
                    "Minor UI preferences for your device. More options will appear here later.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Map cursor",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    "Shown on the trip map while navigating.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text("Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MapCursorStyle.entries.forEach { style ->
                        val selected = cursorStyle == style
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (selected) Color(0xFFB91C1C) else Color(0xFFD1D5DB),
                            ),
                            color = if (selected) Color(0xFFFEE2E2) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    cursorStyle = style
                                    CitizenUiSettingsRepository.setMapCursorStyle(context, style)
                                    onSettingsChanged()
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (style == MapCursorStyle.VEHICLE) {
                                        Icons.Outlined.DirectionsCar
                                    } else {
                                        Icons.Filled.Navigation
                                    },
                                    contentDescription = null,
                                    tint = if (selected) Color(0xFFB91C1C) else Color(0xFF374151),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    style.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Color", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MapCursorColor.entries.forEach { color ->
                        val selected = cursorColor == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(color.argb))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
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
                Spacer(Modifier.height(6.dp))
                Text(
                    cursorColor.label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "More settings coming soon.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
