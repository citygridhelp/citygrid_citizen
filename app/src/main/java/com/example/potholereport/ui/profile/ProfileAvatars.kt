package com.example.potholereport.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.potholereport.data.ProfileAvatarIds

@Composable
fun CartoonAvatar(
    avatarId: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarBackground(avatarId)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.88f)) {
            when (avatarId) {
                ProfileAvatarIds.MAN -> drawManCartoon()
                ProfileAvatarIds.WOMAN -> drawWomanCartoon()
                ProfileAvatarIds.ELDER -> drawElderCartoon()
                ProfileAvatarIds.YOUTH -> drawYouthCartoon()
                ProfileAvatarIds.STUDENT -> drawStudentCartoon()
                ProfileAvatarIds.PILOT -> drawPilotCartoon()
                ProfileAvatarIds.CHEF -> drawChefCartoon()
                ProfileAvatarIds.ARTIST -> drawArtistCartoon()
                ProfileAvatarIds.SPORT -> drawSportCartoon()
                ProfileAvatarIds.GLASSES -> drawGlassesCartoon()
                ProfileAvatarIds.CURLY -> drawCurlyCartoon()
                ProfileAvatarIds.PONYTAIL -> drawPonytailCartoon()
                ProfileAvatarIds.BEANIE -> drawBeanieCartoon()
                ProfileAvatarIds.BRAIDS -> drawBraidsCartoon()
                ProfileAvatarIds.WORKER -> drawWorkerCartoon()
                ProfileAvatarIds.COOL -> drawCoolCartoon()
                ProfileAvatarIds.BLOSSOM -> drawBlossomCartoon()
                ProfileAvatarIds.WAVE -> drawWaveCartoon()
                ProfileAvatarIds.SUNSET -> drawSunsetCartoon()
                ProfileAvatarIds.VIOLET -> drawVioletCartoon()
                ProfileAvatarIds.CORAL -> drawCoralCartoon()
                ProfileAvatarIds.FOREST -> drawForestCartoon()
                ProfileAvatarIds.AMBER -> drawAmberCartoon()
                ProfileAvatarIds.SLATE -> drawSlateCartoon()
                ProfileAvatarIds.NEON -> drawNeonCartoon()
                ProfileAvatarIds.MOHAWK -> drawMohawkCartoon()
                ProfileAvatarIds.HEADPHONES -> drawHeadphonesCartoon()
                ProfileAvatarIds.STAR -> drawStarCartoon()
                ProfileAvatarIds.CYBER -> drawCyberCartoon()
                ProfileAvatarIds.DISCO -> drawDiscoCartoon()
                ProfileAvatarIds.GRAFFITI -> drawGraffitiCartoon()
                ProfileAvatarIds.FLAME -> drawFlameCartoon()
                ProfileAvatarIds.GALAXY -> drawGalaxyCartoon()
                ProfileAvatarIds.CHROME -> drawChromeCartoon()
                else -> drawNeutralCartoon()
            }
        }
    }
}

@Composable
fun SelectableCartoonAvatar(
    avatarId: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = modifier
            .size(62.dp)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CartoonAvatar(avatarId = avatarId, size = 52.dp)
    }
}

private fun avatarBackground(avatarId: String): Brush = when (avatarId) {
    ProfileAvatarIds.NEON -> Brush.linearGradient(
        listOf(Color(0xFF042F2E), Color(0xFF06B6D4), Color(0xFF22D3EE)),
    )
    ProfileAvatarIds.MOHAWK -> Brush.linearGradient(
        listOf(Color(0xFF1E1B4B), Color(0xFFBE185D), Color(0xFFF97316)),
    )
    ProfileAvatarIds.HEADPHONES -> Brush.linearGradient(
        listOf(Color(0xFF0F172A), Color(0xFF4C1D95), Color(0xFF7C3AED)),
    )
    ProfileAvatarIds.STAR -> Brush.radialGradient(
        listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFEA580C)),
    )
    ProfileAvatarIds.CYBER -> Brush.linearGradient(
        listOf(Color(0xFF020617), Color(0xFF0E7490), Color(0xFF22D3EE)),
    )
    ProfileAvatarIds.DISCO -> Brush.linearGradient(
        listOf(Color(0xFF4C1D95), Color(0xFFDB2777), Color(0xFFF472B6)),
    )
    ProfileAvatarIds.GRAFFITI -> Brush.linearGradient(
        listOf(Color(0xFF14532D), Color(0xFFEAB308), Color(0xFFEC4899)),
    )
    ProfileAvatarIds.FLAME -> Brush.linearGradient(
        listOf(Color(0xFF450A0A), Color(0xFFDC2626), Color(0xFFFBBF24)),
    )
    ProfileAvatarIds.GALAXY -> Brush.linearGradient(
        listOf(Color(0xFF1E1B4B), Color(0xFF6D28D9), Color(0xFF38BDF8)),
    )
    ProfileAvatarIds.CHROME -> Brush.linearGradient(
        listOf(Color(0xFF334155), Color(0xFF94A3B8), Color(0xFFE2E8F0)),
    )
    ProfileAvatarIds.COOL -> Brush.linearGradient(
        listOf(Color(0xFF0F172A), Color(0xFF334155)),
    )
    ProfileAvatarIds.ARTIST -> Brush.linearGradient(
        listOf(Color(0xFF7F1D1D), Color(0xFFF472B6)),
    )
    ProfileAvatarIds.WOMAN -> Brush.linearGradient(
        listOf(Color(0xFFFCE7F3), Color(0xFFF472B6)),
    )
    else -> SolidColor(avatarBackdropColor(avatarId))
}

private fun avatarBackdropColor(avatarId: String): Color = when (avatarId) {
    ProfileAvatarIds.MAN -> Color(0xFFDBEAFE)
    ProfileAvatarIds.WOMAN -> Color(0xFFFCE7F3)
    ProfileAvatarIds.ELDER -> Color(0xFFE7E5E4)
    ProfileAvatarIds.YOUTH -> Color(0xFFD1FAE5)
    ProfileAvatarIds.STUDENT -> Color(0xFFDCFCE7)
    ProfileAvatarIds.PILOT -> Color(0xFFE0F2FE)
    ProfileAvatarIds.CHEF -> Color(0xFFFFF7ED)
    ProfileAvatarIds.ARTIST -> Color(0xFFFEE2E2)
    ProfileAvatarIds.SPORT -> Color(0xFFFFEDD5)
    ProfileAvatarIds.GLASSES -> Color(0xFFE0E7FF)
    ProfileAvatarIds.CURLY -> Color(0xFFFEF3C7)
    ProfileAvatarIds.PONYTAIL -> Color(0xFFF3E8FF)
    ProfileAvatarIds.BEANIE -> Color(0xFFFFE4E6)
    ProfileAvatarIds.BRAIDS -> Color(0xFFECFDF5)
    ProfileAvatarIds.WORKER -> Color(0xFFFEF9C3)
    ProfileAvatarIds.COOL -> Color(0xFFE2E8F0)
    ProfileAvatarIds.BLOSSOM -> Color(0xFFFCE7F3)
    ProfileAvatarIds.WAVE -> Color(0xFFCCFBF1)
    ProfileAvatarIds.SUNSET -> Color(0xFFFFEDD5)
    ProfileAvatarIds.VIOLET -> Color(0xFFEDE9FE)
    ProfileAvatarIds.CORAL -> Color(0xFFFFE4E6)
    ProfileAvatarIds.FOREST -> Color(0xFFD1FAE5)
    ProfileAvatarIds.AMBER -> Color(0xFFFEF3C7)
    ProfileAvatarIds.SLATE -> Color(0xFFE5E7EB)
    ProfileAvatarIds.NEON -> Color(0xFF042F2E)
    ProfileAvatarIds.MOHAWK -> Color(0xFF1E1B4B)
    ProfileAvatarIds.HEADPHONES -> Color(0xFF0F172A)
    ProfileAvatarIds.STAR -> Color(0xFFFEF08A)
    ProfileAvatarIds.CYBER -> Color(0xFF020617)
    ProfileAvatarIds.DISCO -> Color(0xFF4C1D95)
    ProfileAvatarIds.GRAFFITI -> Color(0xFF14532D)
    ProfileAvatarIds.FLAME -> Color(0xFF450A0A)
    ProfileAvatarIds.GALAXY -> Color(0xFF1E1B4B)
    ProfileAvatarIds.CHROME -> Color(0xFF334155)
    else -> Color(0xFFE0E7FF)
}

private fun DrawScope.drawFace(
    skin: Color,
    eyeY: Float,
    smile: Boolean = true,
    drawEyes: DrawScope.(Float, Float, Float) -> Unit = { cx, r, y ->
        val eyeR = r * 0.12f
        drawCircle(Color(0xFF1F2937), eyeR, Offset(cx - r * 0.35f, y))
        drawCircle(Color(0xFF1F2937), eyeR, Offset(cx + r * 0.35f, y))
    },
) {
    val cx = size.width / 2f
    val cy = size.height * 0.52f
    val r = size.minDimension * 0.28f
    drawCircle(skin, r, Offset(cx, cy))
    drawEyes(cx, r, eyeY)
    if (smile) {
        drawArc(
            color = Color(0xFF374151),
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx - r * 0.45f, cy + r * 0.05f),
            size = Size(r * 0.9f, r * 0.55f),
            style = Stroke(width = r * 0.09f),
        )
    }
}

private fun DrawScope.drawTorso(cx: Float, color: Color, widthFrac: Float = 0.64f, heightFrac: Float = 0.22f) {
    drawRect(
        color = color,
        topLeft = Offset(cx - size.width * widthFrac / 2f, size.height * 0.72f),
        size = Size(size.width * widthFrac, size.height * heightFrac),
    )
}

private fun DrawScope.drawHairCap(cx: Float, color: Color, radius: Float, y: Float) {
    drawCircle(color, radius, Offset(cx, y))
}

private fun DrawScope.drawManCartoon() {
    val cx = size.width / 2f
    drawTorso(cx, Color(0xFF2563EB))
    drawHairCap(cx, Color(0xFF4B5563), size.width * 0.34f, size.height * 0.28f)
    drawFace(Color(0xFFFCD9B6), size.height * 0.48f)
}

private fun DrawScope.drawWomanCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFF9D174D), size.width * 0.40f, Offset(cx, size.height * 0.34f))
    drawCircle(Color(0xFFF472B6), size.width * 0.32f, Offset(cx - size.width * 0.24f, size.height * 0.20f))
    drawCircle(Color(0xFFF472B6), size.width * 0.32f, Offset(cx + size.width * 0.24f, size.height * 0.20f))
    drawCircle(Color(0xFFFBCFE8), size.width * 0.10f, Offset(cx - size.width * 0.30f, size.height * 0.38f))
    drawCircle(Color(0xFFFBCFE8), size.width * 0.10f, Offset(cx + size.width * 0.30f, size.height * 0.38f))
    drawTorso(cx, Color(0xFFDB2777), widthFrac = 0.58f)
    drawFace(Color(0xFFFFE4C7), size.height * 0.50f) { faceCx, r, y ->
        drawCircle(Color(0xFFBE185D), r * 0.10f, Offset(faceCx - r * 0.35f, y + r * 0.02f))
        drawCircle(Color(0xFFBE185D), r * 0.10f, Offset(faceCx + r * 0.35f, y + r * 0.02f))
    }
}

private fun DrawScope.drawElderCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFFD6D3D1), size.width * 0.36f, size.height * 0.30f)
    drawCircle(Color(0xFF9CA3AF), size.width * 0.08f, Offset(cx - size.width * 0.18f, size.height * 0.26f))
    drawCircle(Color(0xFF9CA3AF), size.width * 0.08f, Offset(cx + size.width * 0.18f, size.height * 0.26f))
    drawTorso(cx, Color(0xFF78716C))
    drawFace(Color(0xFFF5D0A8), size.height * 0.50f)
}

private fun DrawScope.drawYouthCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF059669), size.width * 0.34f, size.height * 0.26f)
    drawTorso(cx, Color(0xFF10B981), widthFrac = 0.56f, heightFrac = 0.18f)
    drawFace(Color(0xFFFFE0BD), size.height * 0.46f)
}

private fun DrawScope.drawNeutralCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFF818CF8), size.width * 0.36f, Offset(cx, size.height * 0.28f))
    drawCircle(Color(0xFF6366F1), size.width * 0.28f, Offset(cx, size.height * 0.22f))
    drawTorso(cx, Color(0xFF4F46E5), widthFrac = 0.58f)
    drawRect(
        color = Color(0xFFC7D2FE),
        topLeft = Offset(cx - size.width * 0.10f, size.height * 0.74f),
        size = Size(size.width * 0.20f, size.height * 0.04f),
    )
    drawFace(Color(0xFFFAD9B0), size.height * 0.49f)
}

private fun DrawScope.drawStudentCartoon() {
    val cx = size.width / 2f
    drawRect(
        color = Color(0xFF16A34A),
        topLeft = Offset(cx - size.width * 0.34f, size.height * 0.10f),
        size = Size(size.width * 0.68f, size.height * 0.12f),
    )
    drawHairCap(cx, Color(0xFF3F2E1F), size.width * 0.33f, size.height * 0.30f)
    drawTorso(cx, Color(0xFF15803D), widthFrac = 0.58f)
    drawFace(Color(0xFFFFDBAC), size.height * 0.48f)
}

private fun DrawScope.drawPilotCartoon() {
    val cx = size.width / 2f
    drawRect(
        color = Color(0xFF1E3A8A),
        topLeft = Offset(cx - size.width * 0.36f, size.height * 0.12f),
        size = Size(size.width * 0.72f, size.height * 0.11f),
    )
    drawCircle(Color(0xFF1E40AF), size.width * 0.05f, Offset(cx, size.height * 0.17f))
    drawHairCap(cx, Color(0xFF1F2937), size.width * 0.32f, size.height * 0.30f)
    drawTorso(cx, Color(0xFF1D4ED8))
    drawFace(Color(0xFFF5D0A8), size.height * 0.49f)
}

private fun DrawScope.drawChefCartoon() {
    val cx = size.width / 2f
    drawRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(cx - size.width * 0.22f, size.height * 0.06f),
        size = Size(size.width * 0.44f, size.height * 0.16f),
    )
    drawHairCap(cx, Color(0xFF57534E), size.width * 0.30f, size.height * 0.32f)
    drawTorso(cx, Color(0xFFF8FAFC), widthFrac = 0.62f)
    drawRect(
        color = Color(0xFFDC2626),
        topLeft = Offset(cx - size.width * 0.30f, size.height * 0.80f),
        size = Size(size.width * 0.60f, size.height * 0.14f),
    )
    drawFace(Color(0xFFFFE4C7), size.height * 0.50f)
}

private fun DrawScope.drawArtistCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFFDC2626), size.width * 0.20f, Offset(cx - size.width * 0.10f, size.height * 0.13f))
    drawCircle(Color(0xFF22C55E), size.width * 0.08f, Offset(cx + size.width * 0.20f, size.height * 0.12f))
    drawCircle(Color(0xFF3B82F6), size.width * 0.08f, Offset(cx + size.width * 0.08f, size.height * 0.20f))
    drawCircle(Color(0xFFEAB308), size.width * 0.07f, Offset(cx - size.width * 0.22f, size.height * 0.18f))
    drawHairCap(cx, Color(0xFF431407), size.width * 0.34f, size.height * 0.30f)
    drawTorso(cx, Color(0xFF7C3AED), widthFrac = 0.56f)
    drawFace(Color(0xFFFFD7BA), size.height * 0.49f)
}

private fun DrawScope.drawSportCartoon() {
    val cx = size.width / 2f
    drawRect(
        color = Color(0xFFEA580C),
        topLeft = Offset(cx - size.width * 0.34f, size.height * 0.16f),
        size = Size(size.width * 0.68f, size.height * 0.06f),
    )
    drawHairCap(cx, Color(0xFF1C1917), size.width * 0.32f, size.height * 0.28f)
    drawTorso(cx, Color(0xFFF97316))
    drawFace(Color(0xFFE8B88A), size.height * 0.47f)
}

private fun DrawScope.drawGlassesCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF44403C), size.width * 0.33f, size.height * 0.28f)
    drawTorso(cx, Color(0xFF6366F1), widthFrac = 0.60f)
    drawFace(Color(0xFFFAD9B0), size.height * 0.49f) { faceCx, r, y ->
        val eyeR = r * 0.12f
        val frameR = r * 0.22f
        val stroke = Stroke(width = r * 0.07f)
        drawCircle(Color(0xFF374151), frameR, Offset(faceCx - r * 0.35f, y), style = stroke)
        drawCircle(Color(0xFF374151), frameR, Offset(faceCx + r * 0.35f, y), style = stroke)
        drawLine(Color(0xFF374151), Offset(faceCx - r * 0.13f, y), Offset(faceCx + r * 0.13f, y), strokeWidth = r * 0.06f)
        drawCircle(Color(0xFF1F2937), eyeR, Offset(faceCx - r * 0.35f, y))
        drawCircle(Color(0xFF1F2937), eyeR, Offset(faceCx + r * 0.35f, y))
    }
}

private fun DrawScope.drawCurlyCartoon() {
    val cx = size.width / 2f
    val c = Color(0xFF92400E)
    drawCircle(c, size.width * 0.12f, Offset(cx - size.width * 0.20f, size.height * 0.18f))
    drawCircle(c, size.width * 0.12f, Offset(cx, size.height * 0.12f))
    drawCircle(c, size.width * 0.12f, Offset(cx + size.width * 0.20f, size.height * 0.18f))
    drawCircle(c, size.width * 0.11f, Offset(cx - size.width * 0.10f, size.height * 0.28f))
    drawCircle(c, size.width * 0.11f, Offset(cx + size.width * 0.10f, size.height * 0.28f))
    drawTorso(cx, Color(0xFFD97706), widthFrac = 0.58f)
    drawFace(Color(0xFF8D5524), size.height * 0.50f)
}

private fun DrawScope.drawPonytailCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF6B21A8), size.width * 0.34f, size.height * 0.30f)
    drawCircle(Color(0xFF7C3AED), size.width * 0.14f, Offset(cx + size.width * 0.30f, size.height * 0.34f))
    drawTorso(cx, Color(0xFFA855F7), widthFrac = 0.58f)
    drawFace(Color(0xFFFFE0BD), size.height * 0.49f)
}

private fun DrawScope.drawBeanieCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFFEA580C), size.width * 0.36f, Offset(cx, size.height * 0.24f))
    drawRect(
        color = Color(0xFFF97316),
        topLeft = Offset(cx - size.width * 0.36f, size.height * 0.30f),
        size = Size(size.width * 0.72f, size.height * 0.06f),
    )
    drawTorso(cx, Color(0xFF0F766E), widthFrac = 0.58f)
    drawFace(Color(0xFFFFD7BA), size.height * 0.50f)
}

private fun DrawScope.drawBraidsCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF422006), size.width * 0.32f, size.height * 0.30f)
    drawCircle(Color(0xFF422006), size.width * 0.09f, Offset(cx - size.width * 0.34f, size.height * 0.42f))
    drawCircle(Color(0xFF422006), size.width * 0.09f, Offset(cx + size.width * 0.34f, size.height * 0.42f))
    drawTorso(cx, Color(0xFF059669), widthFrac = 0.58f)
    drawFace(Color(0xFF6B4423), size.height * 0.49f)
}

private fun DrawScope.drawWorkerCartoon() {
    val cx = size.width / 2f
    drawRect(
        color = Color(0xFFFACC15),
        topLeft = Offset(cx - size.width * 0.36f, size.height * 0.10f),
        size = Size(size.width * 0.72f, size.height * 0.12f),
    )
    drawHairCap(cx, Color(0xFF292524), size.width * 0.30f, size.height * 0.32f)
    drawTorso(cx, Color(0xFFCA8A04), widthFrac = 0.64f)
    drawFace(Color(0xFFE8B88A), size.height * 0.50f)
}

private fun DrawScope.drawCoolCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF0F172A), size.width * 0.33f, size.height * 0.28f)
    drawTorso(cx, Color(0xFF1E293B), widthFrac = 0.58f)
    drawRect(
        color = Color(0xFF94A3B8),
        topLeft = Offset(cx - size.width * 0.22f, size.height * 0.76f),
        size = Size(size.width * 0.44f, size.height * 0.03f),
    )
    drawFace(Color(0xFFF5D0A8), size.height * 0.49f, smile = false) { faceCx, r, y ->
        drawRect(
            color = Color(0xFF111827),
            topLeft = Offset(faceCx - r * 0.54f, y - r * 0.10f),
            size = Size(r * 0.46f, r * 0.16f),
        )
        drawRect(
            color = Color(0xFF111827),
            topLeft = Offset(faceCx + r * 0.08f, y - r * 0.10f),
            size = Size(r * 0.46f, r * 0.16f),
        )
    }
}

private fun DrawScope.drawBlossomCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFFF472B6), size.width * 0.08f, Offset(cx - size.width * 0.24f, size.height * 0.16f))
    drawCircle(Color(0xFFF472B6), size.width * 0.08f, Offset(cx + size.width * 0.24f, size.height * 0.16f))
    drawHairCap(cx, Color(0xFF9D174D), size.width * 0.34f, size.height * 0.30f)
    drawTorso(cx, Color(0xFFEC4899), widthFrac = 0.58f)
    drawFace(Color(0xFFFFE4C7), size.height * 0.50f)
}

private fun DrawScope.drawWaveCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF0E7490), size.width * 0.34f, size.height * 0.28f)
    drawTorso(cx, Color(0xFF0891B2), widthFrac = 0.60f)
    drawFace(Color(0xFFFFD7BA), size.height * 0.49f)
}

private fun DrawScope.drawSunsetCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFFFB923C), size.width * 0.36f, Offset(cx, size.height * 0.26f))
    drawHairCap(cx, Color(0xFF7C2D12), size.width * 0.30f, size.height * 0.32f)
    drawTorso(cx, Color(0xFFF97316), widthFrac = 0.58f)
    drawFace(Color(0xFF8D5524), size.height * 0.50f)
}

private fun DrawScope.drawVioletCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF5B21B6), size.width * 0.34f, size.height * 0.28f)
    drawTorso(cx, Color(0xFF7C3AED), widthFrac = 0.60f)
    drawFace(Color(0xFFFFE0BD), size.height * 0.49f)
}

private fun DrawScope.drawCoralCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF9F1239), size.width * 0.33f, size.height * 0.28f)
    drawTorso(cx, Color(0xFFE11D48), widthFrac = 0.58f)
    drawFace(Color(0xFFFFD7BA), size.height * 0.50f)
}

private fun DrawScope.drawForestCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF14532D), size.width * 0.34f, size.height * 0.28f)
    drawTorso(cx, Color(0xFF166534), widthFrac = 0.60f)
    drawFace(Color(0xFFE8B88A), size.height * 0.49f)
}

private fun DrawScope.drawAmberCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFFB45309), size.width * 0.34f, size.height * 0.28f)
    drawTorso(cx, Color(0xFFF59E0B), widthFrac = 0.60f)
    drawFace(Color(0xFFFAD9B0), size.height * 0.49f)
}

private fun DrawScope.drawSlateCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF374151), size.width * 0.32f, size.height * 0.28f)
    drawTorso(cx, Color(0xFF4B5563), widthFrac = 0.62f)
    drawFace(Color(0xFFFFE4C7), size.height * 0.49f)
}

private fun DrawScope.drawNeonCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFF22D3EE), size.width * 0.38f, Offset(cx, size.height * 0.26f), style = Stroke(width = size.width * 0.04f))
    drawCircle(Color(0xFF06B6D4), size.width * 0.32f, Offset(cx, size.height * 0.28f))
    drawTorso(cx, Color(0xFF0891B2), widthFrac = 0.56f)
    drawFace(Color(0xFF5EEAD4), size.height * 0.49f) { faceCx, r, y ->
        drawCircle(Color(0xFFA7F3D0), r * 0.14f, Offset(faceCx - r * 0.35f, y))
        drawCircle(Color(0xFFA7F3D0), r * 0.14f, Offset(faceCx + r * 0.35f, y))
        drawCircle(Color(0xFF022C22), r * 0.06f, Offset(faceCx - r * 0.35f, y))
        drawCircle(Color(0xFF022C22), r * 0.06f, Offset(faceCx + r * 0.35f, y))
    }
}

private fun DrawScope.drawMohawkCartoon() {
    val cx = size.width / 2f
    val pathColor = Color(0xFF22D3EE)
    drawLine(pathColor, Offset(cx, size.height * 0.42f), Offset(cx, size.height * 0.04f), strokeWidth = size.width * 0.08f)
    drawLine(Color(0xFFEC4899), Offset(cx - size.width * 0.06f, size.height * 0.10f), Offset(cx - size.width * 0.20f, size.height * 0.16f), strokeWidth = size.width * 0.05f)
    drawLine(Color(0xFFF97316), Offset(cx + size.width * 0.06f, size.height * 0.10f), Offset(cx + size.width * 0.20f, size.height * 0.16f), strokeWidth = size.width * 0.05f)
    drawHairCap(cx, Color(0xFF1C1917), size.width * 0.30f, size.height * 0.34f)
    drawTorso(cx, Color(0xFFBE185D), widthFrac = 0.56f)
    drawFace(Color(0xFFFFE0BD), size.height * 0.50f)
}

private fun DrawScope.drawHeadphonesCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF1E1B4B), size.width * 0.32f, size.height * 0.30f)
    drawCircle(Color(0xFF4C1D95), size.width * 0.14f, Offset(cx - size.width * 0.38f, size.height * 0.46f))
    drawCircle(Color(0xFF4C1D95), size.width * 0.14f, Offset(cx + size.width * 0.38f, size.height * 0.46f))
    drawCircle(Color(0xFFA78BFA), size.width * 0.09f, Offset(cx - size.width * 0.38f, size.height * 0.46f))
    drawCircle(Color(0xFFA78BFA), size.width * 0.09f, Offset(cx + size.width * 0.38f, size.height * 0.46f))
    drawLine(Color(0xFF7C3AED), Offset(cx - size.width * 0.24f, size.height * 0.22f), Offset(cx + size.width * 0.24f, size.height * 0.22f), strokeWidth = size.width * 0.04f)
    drawTorso(cx, Color(0xFF6D28D9), widthFrac = 0.56f)
    drawFace(Color(0xFFFFD7BA), size.height * 0.49f)
}

private fun DrawScope.drawStarCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF92400E), size.width * 0.34f, size.height * 0.28f)
    drawTorso(cx, Color(0xFFF59E0B), widthFrac = 0.58f)
    drawFace(Color(0xFFFFE4C7), size.height * 0.49f) { faceCx, r, y ->
        drawSparkleEye(Color(0xFFFBBF24), faceCx - r * 0.35f, y, r * 0.18f)
        drawSparkleEye(Color(0xFFFBBF24), faceCx + r * 0.35f, y, r * 0.18f)
    }
}

private fun DrawScope.drawCyberCartoon() {
    val cx = size.width / 2f
    drawHairCap(cx, Color(0xFF0F172A), size.width * 0.30f, size.height * 0.32f)
    drawRect(
        color = Color(0xFF22D3EE).copy(alpha = 0.85f),
        topLeft = Offset(cx - size.width * 0.30f, size.height * 0.40f),
        size = Size(size.width * 0.60f, size.height * 0.10f),
    )
    drawTorso(cx, Color(0xFF0E7490), widthFrac = 0.56f)
    drawFace(Color(0xFF67E8F9), size.height * 0.50f, smile = false)
}

private fun DrawScope.drawDiscoCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFF581C87), size.width * 0.40f, Offset(cx, size.height * 0.30f))
    drawCircle(Color(0xFFDB2777), size.width * 0.34f, Offset(cx - size.width * 0.12f, size.height * 0.22f))
    drawCircle(Color(0xFFF472B6), size.width * 0.34f, Offset(cx + size.width * 0.12f, size.height * 0.22f))
    drawCircle(Color(0xFFFBBF24), size.width * 0.06f, Offset(cx - size.width * 0.28f, size.height * 0.14f))
    drawCircle(Color(0xFF22D3EE), size.width * 0.06f, Offset(cx + size.width * 0.26f, size.height * 0.16f))
    drawTorso(cx, Color(0xFFBE185D), widthFrac = 0.56f)
    drawFace(Color(0xFF8D5524), size.height * 0.50f)
}

private fun DrawScope.drawGraffitiCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFF22C55E), size.width * 0.14f, Offset(cx - size.width * 0.18f, size.height * 0.14f))
    drawCircle(Color(0xFFEC4899), size.width * 0.12f, Offset(cx + size.width * 0.16f, size.height * 0.12f))
    drawCircle(Color(0xFFEAB308), size.width * 0.10f, Offset(cx, size.height * 0.08f))
    drawHairCap(cx, Color(0xFF1E293B), size.width * 0.32f, size.height * 0.30f)
    drawTorso(cx, Color(0xFF7C3AED), widthFrac = 0.56f)
    drawFace(Color(0xFFFFD7BA), size.height * 0.49f)
}

private fun DrawScope.drawFlameCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFFDC2626), size.width * 0.12f, Offset(cx, size.height * 0.08f))
    drawCircle(Color(0xFFF97316), size.width * 0.10f, Offset(cx - size.width * 0.10f, size.height * 0.12f))
    drawCircle(Color(0xFFFBBF24), size.width * 0.09f, Offset(cx + size.width * 0.10f, size.height * 0.11f))
    drawHairCap(cx, Color(0xFF7C2D12), size.width * 0.32f, size.height * 0.30f)
    drawTorso(cx, Color(0xFFEA580C), widthFrac = 0.56f)
    drawFace(Color(0xFFFFE0BD), size.height * 0.49f)
}

private fun DrawScope.drawGalaxyCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFF6D28D9), size.width * 0.36f, Offset(cx, size.height * 0.28f))
    drawCircle(Color(0xFF38BDF8), size.width * 0.05f, Offset(cx - size.width * 0.20f, size.height * 0.20f))
    drawCircle(Color(0xFFF472B6), size.width * 0.04f, Offset(cx + size.width * 0.18f, size.height * 0.24f))
    drawCircle(Color(0xFFF8FAFC), size.width * 0.03f, Offset(cx + size.width * 0.06f, size.height * 0.14f))
    drawTorso(cx, Color(0xFF4C1D95), widthFrac = 0.56f)
    drawFace(Color(0xFFE9D5FF), size.height * 0.49f) { faceCx, r, y ->
        drawCircle(Color(0xFF38BDF8), r * 0.13f, Offset(faceCx - r * 0.35f, y))
        drawCircle(Color(0xFFF472B6), r * 0.13f, Offset(faceCx + r * 0.35f, y))
    }
}

private fun DrawScope.drawChromeCartoon() {
    val cx = size.width / 2f
    drawCircle(Color(0xFFCBD5E1), size.width * 0.34f, Offset(cx, size.height * 0.28f))
    drawCircle(Color(0xFF94A3B8), size.width * 0.26f, Offset(cx, size.height * 0.24f))
    drawTorso(cx, Color(0xFF64748B), widthFrac = 0.58f)
    drawRect(
        color = Color(0xFFE2E8F0),
        topLeft = Offset(cx - size.width * 0.08f, size.height * 0.82f),
        size = Size(size.width * 0.16f, size.height * 0.05f),
    )
    drawFace(Color(0xFFFFE4C7), size.height * 0.49f)
}

private fun DrawScope.drawSparkleEye(color: Color, cx: Float, cy: Float, r: Float) {
    val stroke = r * 0.32f
    drawLine(color, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = stroke)
    drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = stroke)
    drawLine(color, Offset(cx - r * 0.7f, cy - r * 0.7f), Offset(cx + r * 0.7f, cy + r * 0.7f), strokeWidth = stroke * 0.8f)
    drawLine(color, Offset(cx - r * 0.7f, cy + r * 0.7f), Offset(cx + r * 0.7f, cy - r * 0.7f), strokeWidth = stroke * 0.8f)
    drawCircle(color, r * 0.18f, Offset(cx, cy))
}
