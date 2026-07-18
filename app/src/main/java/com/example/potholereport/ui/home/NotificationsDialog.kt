package com.example.potholereport.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.potholereport.data.AppUpdateChecker
import com.example.potholereport.data.CitizenNotification
import com.example.potholereport.data.CitizenNotificationsRepository

@Composable
fun NotificationsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var refreshToken by remember { mutableIntStateOf(0) }
    val notifications = remember(refreshToken) { CitizenNotificationsRepository.all() }

    fun closeDialog() {
        CitizenNotificationsRepository.applyReadDeletionsOnClose()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = { closeDialog() },
        title = {
            Text("Notifications", fontWeight = FontWeight.Bold)
        },
        text = {
            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No notifications yet. You'll see app updates and ticket status changes here.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(notifications, key = { it.id }) { item ->
                        NotificationRow(
                            item = item,
                            onOpen = {
                                CitizenNotificationsRepository.markRead(item.id)
                                refreshToken++
                                if (item.type == CitizenNotification.Type.APP_UPDATE &&
                                    item.id.startsWith("app-update-available-")
                                ) {
                                    AppUpdateChecker.openPlayStoreListing(context)
                                    closeDialog()
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    CitizenNotificationsRepository.markAllRead()
                    CitizenNotificationsRepository.applyReadDeletionsOnClose()
                    refreshToken++
                    onDismiss()
                },
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun NotificationRow(
    item: CitizenNotification,
    onOpen: () -> Unit,
) {
    val accent = when (item.type) {
        CitizenNotification.Type.APP_UPDATE -> Color(0xFF0369A1)
        CitizenNotification.Type.STATUS_CHANGE -> Color(0xFFB74233)
        CitizenNotification.Type.GENERAL -> MaterialTheme.colorScheme.primary
    }
    val icon = when (item.type) {
        CitizenNotification.Type.APP_UPDATE -> Icons.Outlined.SystemUpdate
        CitizenNotification.Type.STATUS_CHANGE -> Icons.Outlined.TrackChanges
        CitizenNotification.Type.GENERAL -> Icons.Outlined.Notifications
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (item.read) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                accent.copy(alpha = 0.08f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    item.body,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    CitizenNotificationsRepository.formatTimestamp(item.createdAtMs),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}
