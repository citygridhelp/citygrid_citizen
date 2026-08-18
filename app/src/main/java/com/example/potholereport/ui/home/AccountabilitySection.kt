package com.example.potholereport.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.potholereport.data.AccountabilityViewMode
import com.example.potholereport.data.CityLaunchConfig
import com.example.potholereport.data.CivicIssueType
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.RecentReportsRepository
import com.example.potholereport.data.ReportsStripSort
import com.example.potholereport.data.aggregateAccountabilityChart
import com.example.potholereport.data.formatRecentReportCaption
import com.example.potholereport.data.isMeaningfulAreaName
import com.example.potholereport.data.matchesCivicFilters
import com.example.potholereport.data.sortReportsForStrip
import com.example.potholereport.ui.theme.CitizenAccountabilityCopy
import com.example.potholereport.ui.theme.CitizenBorder
import com.example.potholereport.ui.theme.CitizenHeader
import com.example.potholereport.ui.theme.CitizenIcon
import com.example.potholereport.ui.theme.CitizenInk
import com.example.potholereport.ui.theme.CitizenLightSurface
import com.example.potholereport.ui.theme.CitizenRadius
import com.example.potholereport.ui.theme.CitizenSelection
import com.example.potholereport.ui.theme.CitizenSpace
import com.example.potholereport.ui.theme.CitizenType
import com.example.potholereport.ui.theme.citizenReportStatusColor
import java.util.Locale

private const val GBA_DIRECTORY_URL = "https://bbmp.gov.in/"

@Composable
fun AccountabilitySection(
    recentReportsEpoch: Int,
    modifier: Modifier = Modifier,
    title: String = "ACCOUNTABILITY",
    reportsOverride: List<PersistedPotholeReport>? = null,
    showIssueTypeFilter: Boolean = false,
    emptyTitle: String = "No assigned reports yet.",
    emptyBody: String = "Submit a Bengaluru pothole report with location to see the assigned GBA corporation officer here.",
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortName by rememberSaveable { mutableStateOf(ReportsStripSort.RECENT.name) }
    val stripSort = ReportsStripSort.entries.find { it.name == sortName }
        ?: ReportsStripSort.RECENT
    var infoExpanded by rememberSaveable { mutableStateOf(false) }
    var viewModeName by rememberSaveable { mutableStateOf(AccountabilityViewMode.LIST.name) }
    val viewMode = AccountabilityViewMode.entries.find { it.name == viewModeName }
        ?: AccountabilityViewMode.LIST
    var expandedDivisionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var issueTypeKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedIssueType = remember(issueTypeKey) {
        issueTypeKey?.let { key -> CivicIssueType.entries.find { it.name == key } }
    }

    val reports = remember(
        recentReportsEpoch,
        stripSort,
        reportsOverride,
        selectedIssueType,
        showIssueTypeFilter,
    ) {
        val base = reportsOverride
            ?: RecentReportsRepository.reportsForAccountability()
                .filter { CityLaunchConfig.isCityEnabled(it.cityKey) }
        val typed = if (showIssueTypeFilter) {
            base.filter { it.matchesCivicFilters(issueType = selectedIssueType) }
        } else {
            base
        }
        sortReportsForStrip(typed, stripSort, limit = Int.MAX_VALUE)
    }
    val chartBuckets = remember(reports) { aggregateAccountabilityChart(reports) }
    val uriHandler = LocalUriHandler.current
    val resolvedEmptyTitle = if (showIssueTypeFilter && selectedIssueType != null) {
        "No ${selectedIssueType.filterLabel.lowercase(Locale.US)} reports yet."
    } else {
        emptyTitle
    }
    val resolvedEmptyBody = if (showIssueTypeFilter && selectedIssueType != null) {
        "Pick All issues, or tap + to file a ${selectedIssueType.hubTitle.lowercase(Locale.US)} report."
    } else {
        emptyBody
    }

    CitizenLightSurface {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CitizenHeader.Horizontal),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(CitizenIcon.Chip),
                )
                Spacer(modifier = Modifier.width(CitizenSpace.Sm))
                Text(
                    text = title,
                    style = CitizenType.Section,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AccountabilityInfoIcon(
                    expanded = infoExpanded,
                    onClick = { infoExpanded = !infoExpanded },
                )
            }
            Spacer(modifier = Modifier.width(CitizenSpace.Sm))
            Box {
                AssistChip(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.height(CitizenIcon.CapsuleCompact),
                    label = {
                        Text(
                            text = "Sort: ${stripSort.chipLabel}",
                            style = CitizenType.ChipCompact,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(CitizenIcon.Chip),
                        )
                    },
                    shape = CitizenRadius.Capsule,
                    border = BorderStroke(CitizenBorder.Thin, CitizenSelection.InactiveBorder),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onBackground,
                        trailingIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    ReportsStripSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (option.enabled) {
                                        option.menuLabel
                                    } else {
                                        "${option.menuLabel} · Coming soon"
                                    },
                                    color = if (option.enabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                                )
                            },
                            enabled = option.enabled,
                            onClick = {
                                sortName = option.name
                                sortMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = infoExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                Text(
                    text = CitizenAccountabilityCopy.SECTION_INTRO,
                    style = CitizenType.Helper,
                    color = CitizenInk.Helper,
                )
            }
        }

        if (showIssueTypeFilter) {
            Spacer(modifier = Modifier.height(CitizenSpace.Md))
            AccountabilityIssueTypeFilter(
                selected = selectedIssueType,
                onSelect = { issueTypeKey = it?.name },
            )
        }

        Spacer(modifier = Modifier.height(CitizenSpace.Md))
        AccountabilityViewModeSwitcher(
            selected = viewMode,
            onSelect = { viewModeName = it.name },
        )
        Spacer(modifier = Modifier.height(CitizenSpace.Md))

        if (reports.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CitizenRadius.Card,
                border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(CitizenSpace.Xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = resolvedEmptyTitle,
                        style = CitizenType.Label,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                    Text(
                        text = resolvedEmptyBody,
                        style = CitizenType.Helper,
                        color = CitizenInk.Muted,
                    )
                }
            }
        } else {
            when (viewMode) {
                AccountabilityViewMode.LIST -> {
                    reports.forEachIndexed { index, report ->
                        AccountabilityReportCard(report = report)
                        if (index < reports.lastIndex) {
                            Spacer(modifier = Modifier.height(CitizenSpace.Md))
                        }
                    }
                }
                AccountabilityViewMode.BAR -> {
                    AccountabilityHorizontalBarChart(
                        divisions = chartBuckets,
                        expandedDivisionKeys = expandedDivisionKey?.let { setOf(it) }.orEmpty(),
                        onToggleDivision = { key ->
                            expandedDivisionKey = if (expandedDivisionKey == key) null else key
                        },
                    )
                }
                AccountabilityViewMode.PIE -> {
                    AccountabilityPieChart(divisions = chartBuckets)
                }
            }
        }

        Spacer(modifier = Modifier.height(CitizenSpace.Md))
        Text(
            text = "Official source — Bengaluru (GBA / BBMP) city corporations:",
            style = CitizenType.Caption,
            color = CitizenInk.Muted,
        )
        TextButton(
            onClick = { uriHandler.openUri(GBA_DIRECTORY_URL) },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = CitizenSpace.Xs),
            modifier = Modifier.heightIn(min = CitizenIcon.Tap),
        ) {
            Text(
                text = GBA_DIRECTORY_URL,
                style = CitizenType.Caption,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = "More cities will list their corporation sources when enabled in a future release.",
            style = CitizenType.Caption,
            color = CitizenInk.Muted,
        )
    }
    }
}

@Composable
private fun AccountabilityIssueTypeFilter(
    selected: CivicIssueType?,
    onSelect: (CivicIssueType?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(CitizenSpace.Sm),
    ) {
        AccountabilityIssueTypeChip(
            label = "All issues",
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        CivicIssueType.filterEntries.forEach { type ->
            AccountabilityIssueTypeChip(
                label = type.filterLabel,
                selected = selected == type,
                onClick = { onSelect(type) },
            )
        }
    }
}

@Composable
private fun AccountabilityIssueTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        modifier = Modifier.height(CitizenIcon.CapsuleCompact),
        label = {
            Text(
                text = label,
                style = CitizenType.ChipCompact,
                maxLines = 1,
            )
        },
        shape = CitizenRadius.Capsule,
        border = BorderStroke(
            CitizenBorder.Thin,
            if (selected) MaterialTheme.colorScheme.primary else CitizenSelection.InactiveBorder,
        ),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                Color.Transparent
            },
            labelColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
        ),
    )
}

/**
 * Slow pulse on the info icon while collapsed so users notice they can tap for
 * the Bengaluru / citizen-reference explanation.
 */
@Composable
private fun AccountabilityInfoIcon(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "accountabilityInfoPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "accountabilityInfoAlpha",
    )
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "accountabilityInfoScale",
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(CitizenIcon.Tap),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = if (expanded) {
                "Hide accountability information"
            } else {
                "Why Accountability — tap for details"
            },
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(CitizenIcon.Chip)
                .then(
                    if (expanded) {
                        Modifier.alpha(1f)
                    } else {
                        Modifier
                            .alpha(pulse)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    },
                ),
        )
    }
}

@Composable
private fun AccountabilityReportCard(report: PersistedPotholeReport) {
    val context = LocalContext.current
    val caption = remember(report.id, report.areaLabel) {
        formatRecentReportCaption(report.createdAtMs, report.areaLabel)
    }
    val statusColor = citizenReportStatusColor(report.status)
    var showLocationMap by remember(report.id) { mutableStateOf(false) }
    val coordsText = remember(report.latitude, report.longitude) {
        String.format(Locale.US, "%.5f, %.5f", report.latitude, report.longitude)
    }
    val areaHint = report.areaLabel.takeIf { isMeaningfulAreaName(it) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CitizenRadius.Card,
        border = BorderStroke(CitizenBorder.Thin, CitizenBorder.Secondary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(CitizenSpace.Md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.cityKey.replace('_', ' ').lowercase(Locale.US)
                            .split(' ')
                            .joinToString(" ") { w ->
                                w.replaceFirstChar { c -> c.titlecase(Locale.US) }
                            },
                        style = CitizenType.Caption,
                        fontWeight = FontWeight.Bold,
                        color = CitizenInk.Helper,
                    )
                    Spacer(modifier = Modifier.height(CitizenSpace.Xs))
                    Text(
                        text = caption,
                        style = CitizenType.Label,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = report.status.displayLabelCaps,
                    style = CitizenType.Caption,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
            }

            if (report.hasCoordinates()) {
                Spacer(modifier = Modifier.height(CitizenSpace.Sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(CitizenIcon.Dense),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(CitizenSpace.Xs))
                    Text(
                        text = coordsText,
                        style = CitizenType.Caption,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { showLocationMap = !showLocationMap },
                    )
                }
                Text(
                    text = if (showLocationMap) {
                        "Tap coordinates to hide map"
                    } else {
                        "Tap coordinates to view on map"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                )
                if (showLocationMap) {
                    ReportLocationMapPreview(
                        latitude = report.latitude,
                        longitude = report.longitude,
                        areaHint = areaHint,
                        onOpenExternalMap = {
                            val label = areaHint ?: "Pothole report"
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
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = CitizenSpace.Sm),
                color = CitizenBorder.Secondary,
            )

            Text(
                text = "ASSIGNED FOR ACCOUNTABILITY",
                style = CitizenType.Caption,
                fontWeight = FontWeight.Bold,
                color = CitizenInk.Muted,
            )
            Text(
                text = CitizenAccountabilityCopy.CARD_DISCLAIMER,
                style = CitizenType.Caption,
                color = CitizenInk.Helper,
            )
            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
            Text(
                text = report.assigneeCorporation,
                style = CitizenType.Chip,
                fontWeight = FontWeight.SemiBold,
                color = CitizenInk.Helper,
            )
            Text(
                text = report.assigneeZone,
                style = CitizenType.Caption,
                color = CitizenInk.Muted,
            )
            ReportAssigneeDisplay.wardLabel(report)?.let { ward ->
                Text(
                    text = ward,
                    style = CitizenType.Caption,
                    color = CitizenInk.Muted,
                )
            }
            Spacer(modifier = Modifier.height(CitizenSpace.Sm))
            Text(
                text = report.assigneeName,
                style = CitizenType.Section,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
            Text(
                text = report.assigneePosition,
                style = CitizenType.Chip,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(CitizenSpace.Xs))
            Text(
                text = report.assigneeOfficeAddress,
                style = CitizenType.Caption,
                color = CitizenInk.Helper,
            )
        }
    }
}
