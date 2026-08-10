package com.example.potholereport.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.potholereport.data.PotholeReportStatus

/** Shared radius / border / header chrome for the citizen app. */
object CitizenRadius {
    /** Chips, SOS, filter toggles, primary pill CTAs. */
    val Capsule = RoundedCornerShape(percent = 50)

    /** Cards, empty states, list surfaces. */
    val Card = RoundedCornerShape(12.dp)

    /** Rare sharp edges (dense forms / legacy panels). Prefer [Card] or [Capsule]. */
    val Sharp = RoundedCornerShape(0.dp)
}

object CitizenBorder {
    val Thin: Dp = 1.dp
    val Secondary: Color = Color(0xFF9CA3AF)
}

/**
 * Spacing rhythm for padding / gaps. Prefer these over one-off dp values.
 * Tiny icon padding may still use [Xs]; map overlay micro-offsets can stay local.
 */
object CitizenSpace {
    val Xs: Dp = 4.dp
    val Sm: Dp = 8.dp
    val Md: Dp = 12.dp
    val Lg: Dp = 16.dp
    val Xl: Dp = 24.dp
}

/** Semantic text / state colors used across helper copy, warnings, and disabled UI. */
object CitizenInk {
    /** Quiet supporting copy — darkened for outdoor / sunlight readability. */
    val Helper: Color = Color(0xFF4B5563)

    /** Soft secondary labels (footers, “coming soon”). */
    val Muted: Color = Color(0xFF6B7280)

    /** Caution / soft-gate / GPS warn. */
    val Warning: Color = Color(0xFFB45309)

    /** Hard block / destructive / error. */
    val Error: Color = Color(0xFFB91C1C)

    /** Positive confirmation (GPS attached, verified). */
    val Success: Color = Color(0xFF2E7D32)

    /** Disabled chips, icons, and inactive labels. */
    val Disabled: Color = Color(0xFF6B7280)
}

/** Icon drawing size + minimum tappable control size. */
object CitizenIcon {
    /** Minimum tap target for header / map icon buttons. */
    val Tap: Dp = 44.dp

    /**
     * Visual height for compact homepage capsules (Change City, filters, SOS).
     * Keep thinner than [Tap]; primary Report CTA stays taller on its own.
     */
    val CapsuleCompact: Dp = 28.dp

    /** Glyph inside header icon buttons. */
    val Header: Dp = 22.dp

    /** Leading icons inside chips / filters. */
    val Chip: Dp = 16.dp

    /** Compact glyphs (SOS, dense rows). */
    val Dense: Dp = 14.dp

    /** Signed-in avatar inside the header tap target. */
    val Avatar: Dp = 36.dp

    /** Map floating action buttons — same as [Tap] for a11y. */
    val Fab: Dp = Tap

    /** Glyph inside map FABs. */
    val FabGlyph: Dp = 20.dp
}

/** Active vs inactive selection chrome for tabs / chips / filters. */
object CitizenSelection {
    val Active: Color = Color(0xFFB91C1C)
    val ActiveContent: Color = Color.White
    val ActiveSoft: Color = Color(0xFFB91C1C).copy(alpha = 0.12f)
    val InactiveBorder: Color = CitizenBorder.Secondary
}

/**
 * App typography hierarchy. Prefer these roles over freehand fontSize/fontWeight.
 */
object CitizenType {
    /** Top brand mark (`CITY GRID`). */
    val Brand = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    )

    /** Full-screen / modal title (`NEW REPORT`). */
    val ScreenTitle = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )

    /** Section heading (`BENGALURU`, `REPORTS`). */
    val Section = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )

    /** Quieter part of a section line (`POTHOLE DENSITY`). */
    val SectionTrailing = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

    /** Row / filter field labels (`Severity`). */
    val Label = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    /** Chip / toggle text (`Trip`, `Heat`, `All`). */
    val Chip = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

    /** Compact chip / SOS label. */
    val ChipCompact = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )

    /** Top tabs. */
    val Tab = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
    )

    /** Supporting sentences and weather/helper copy. */
    val Helper = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

    /** Tagline under anonymity banner. */
    val Tagline = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 14.sp,
    )

    /** Tiny captions / footers. */
    val Caption = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
    )

    /** Primary CTA label (`REPORT A POTHOLE`). */
    val Cta = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 16.sp,
    )

    /** CTA subtitle under primary button. */
    val CtaSub = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )
}

object CitizenHeader {
    val Horizontal: Dp = CitizenSpace.Lg
    val Vertical: Dp = CitizenSpace.Lg
    val Padding = PaddingValues(horizontal = Horizontal, vertical = Vertical)
}

/** Shared ticket / report status colors used on My Reports, Accountability, and detail. */
fun citizenReportStatusColor(status: PotholeReportStatus): Color =
    when (status) {
        PotholeReportStatus.OPEN -> CitizenInk.Error
        PotholeReportStatus.IN_PROGRESS -> CitizenInk.Warning
        PotholeReportStatus.COMPLETED -> CitizenInk.Success
    }

/** Neutral accent for “TOTAL” / aggregate stats. */
val CitizenStatTotal: Color = Color(0xFF14233D)

/**
 * Section title used under the map (e.g. REPORTS) and similar blocks.
 * Matches the city heading pattern: strong title + quieter trailing label.
 */
@Composable
fun CitizenSectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = CitizenType.Section,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!trailing.isNullOrBlank()) {
            Text(
                " - ",
                style = CitizenType.Section,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                trailing,
                style = CitizenType.SectionTrailing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
