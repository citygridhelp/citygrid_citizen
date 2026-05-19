package com.example.potholereport.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.potholereport.data.PersistedPotholeReport
import com.example.potholereport.data.PotholeReportStatus
import com.example.potholereport.data.RecentReportsRepository
import com.example.potholereport.data.formatRecentReportCaption
import java.util.Locale

@Composable
fun AccountabilitySection(
    recentReportsEpoch: Int,
    modifier: Modifier = Modifier,
) {
    val reports = remember(recentReportsEpoch) {
        RecentReportsRepository.reportsForAccountability()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "ACCOUNTABILITY",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Each report is routed to the municipal zone officer responsible for road maintenance in that area. " +
                "Officer details are taken from official corporation directories.",
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
        )
        Spacer(Modifier.height(14.dp))

        if (reports.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, Color(0xFFD7D7D7)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "No assigned reports yet.",
                        fontSize = 14.sp,
                        color = Color(0xFF5A5A5A),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Submit a pothole report with location to see the assigned municipal officer here.",
                        fontSize = 12.sp,
                        color = Color(0xFF7B7B7B),
                    )
                }
            }
        } else {
            reports.forEachIndexed { index, report ->
                AccountabilityReportCard(report = report)
                if (index < reports.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Source: BBMP, BMC, MCD, Greater Chennai Corporation, and GHMC public officer directories.",
            fontSize = 9.sp,
            lineHeight = 12.sp,
            color = Color(0xFF7B7B7B),
        )
    }
}

@Composable
private fun AccountabilityReportCard(report: PersistedPotholeReport) {
    val caption = remember(report.id, report.areaLabel) {
        formatRecentReportCaption(report.createdAtMs, report.areaLabel)
    }
    val statusColor = when (report.status) {
        PotholeReportStatus.OPEN -> Color(0xFFA12A2A)
        PotholeReportStatus.IN_PROGRESS -> Color(0xFFBC7A1E)
        PotholeReportStatus.COMPLETED -> Color(0xFF0B7A42)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        report.cityKey,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        caption,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    report.status.displayLabel.uppercase(Locale.US),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
            }

            if (report.hasCoordinates()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        String.format(Locale.US, "%.5f, %.5f", report.latitude, report.longitude),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Text(
                "ASSIGNED TO",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                report.assigneeName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                report.assigneePosition,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                report.assigneeCorporation,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
            Text(
                report.assigneeZone,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                report.assigneeOfficeAddress,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}
