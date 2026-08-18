package com.example.potholereport.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WaterDrop
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.potholereport.data.AppUpdateChecker
import com.example.potholereport.data.CitizenNotification
import com.example.potholereport.data.CitizenNotificationsRepository
import com.example.potholereport.ui.theme.CitizenBorder
import com.example.potholereport.ui.theme.CitizenIcon
import com.example.potholereport.ui.theme.CitizenInk
import com.example.potholereport.ui.theme.CitizenLightSurface
import com.example.potholereport.ui.theme.CitizenRadius
import com.example.potholereport.ui.theme.CitizenSelection
import com.example.potholereport.ui.theme.CitizenSpace
import com.example.potholereport.ui.theme.CitizenType

@Composable
fun NotificationsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var refreshToken by remember { mutableIntStateOf(0) }
    val notifications = remember(refreshToken) { CitizenNotificationsRepository.all() }

    fun closeDialog() {
        CitizenNotificationsRepository.applyReadDeletionsOnClose()
        onDismiss()
    }

    CitizenLightSurface {
    AlertDialog(
        onDismissRequest = { closeDialog() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Notifications", style = CitizenType.ScreenTitle)
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
                        tint = CitizenInk.Disabled,
                        modifier = Modifier.size(CitizenIcon.Fab),
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Md))
                    Text(
                        "No notifications yet. You'll see app updates and ticket status changes here.",
                        style = CitizenType.Helper,
                        color = CitizenInk.Helper,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
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
                Text("Done", style = CitizenType.Label)
            }
        },
    )
    }
}

@Composable
private fun NotificationRow(
    item: CitizenNotification,
    onOpen: () -> Unit,
) {
    val accent = when (item.type) {
        CitizenNotification.Type.APP_UPDATE -> Color(0xFF0369A1)
        CitizenNotification.Type.STATUS_CHANGE -> CitizenSelection.Active
        CitizenNotification.Type.GENERAL -> MaterialTheme.colorScheme.primary
        CitizenNotification.Type.WEATHER -> Color(0xFF0369A1)
        CitizenNotification.Type.UNCONFIRMED -> CitizenInk.Warning
    }
    val icon = when (item.type) {
        CitizenNotification.Type.APP_UPDATE -> Icons.Outlined.SystemUpdate
        CitizenNotification.Type.STATUS_CHANGE -> Icons.Outlined.TrackChanges
        CitizenNotification.Type.GENERAL -> Icons.Outlined.Notifications
        CitizenNotification.Type.WEATHER -> Icons.Outlined.WaterDrop
        CitizenNotification.Type.UNCONFIRMED -> Icons.Outlined.HelpOutline
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CitizenRadius.Card)
            .border(
                BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary),
                CitizenRadius.Card,
            )
            .clickable(onClick = onOpen),
        shape = CitizenRadius.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (item.read) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                CitizenSelection.ActiveSoft
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(CitizenSpace.Md),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(CitizenIcon.Chip),
            )
            Spacer(modifier = Modifier.size(CitizenSpace.Sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = CitizenType.Label,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                Text(
                    item.body,
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
                Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                Text(
                    CitizenNotificationsRepository.formatTimestamp(item.createdAtMs),
                    style = CitizenType.Caption,
                    color = CitizenInk.Muted,
                )
            }
        }
    }
}
