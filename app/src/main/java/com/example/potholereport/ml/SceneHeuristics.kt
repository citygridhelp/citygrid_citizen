package com.example.potholereport.ml

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Rejects indoor floors, bins, walls, and other common false positives before ML scores can pass.
 */
internal data class SceneAnalysis(
    val roadRatio: Float,
    val asphaltRatio: Float,
    val indoorFloorRatio: Float,
    val woodPanelRatio: Float,
    val verticalStructureRatio: Float,
    val skyBandRatio: Float,
    val highSaturationRatio: Float,
    val circularVesselScore: Float,
    val horizontalRoadLineScore: Float,
)

internal object SceneHeuristics {

    fun analyze(bitmap: Bitmap): SceneAnalysis {
        val w = 72
        val h = 72
        val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
        var road = 0
        var asphalt = 0
        var indoorFloor = 0
        var woodPanel = 0
        var verticalStructure = 0
        var sky = 0
        var saturated = 0
        val total = w * h

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = small.getPixel(x, y)
                when {
                    isIndoorFloorTilePixel(c) -> indoorFloor++
                    isWoodPanelPixel(c) -> woodPanel++
                    isAsphaltPixel(c) -> {
                        asphalt++
                        road++
                    }
                    isRoadLikePixel(c) -> road++
                    isHighSaturationPixel(c) -> saturated++
                }
                // Y-bounds guard added: original code accessed (x, y-1) /
                // (x, y+1) without checking y, which throws on Android
                // Bitmap.getPixel and silently rejected every photo via the
                // outer try/catch in PotholePhotoValidator.validate().
                if (x in 1 until w - 1 && y in 1 until h - 1) {
                    val lum = luminance(c)
                    val lumLeft = luminance(small.getPixel(x - 1, y))
                    val lumRight = luminance(small.getPixel(x + 1, y))
                    val lumUp = luminance(small.getPixel(x, y - 1))
                    val lumDown = luminance(small.getPixel(x, y + 1))
                    val verticalGrad = abs(lum - lumUp) + abs(lum - lumDown)
                    val horizontalGrad = abs(lum - lumLeft) + abs(lum - lumRight)
                    if (verticalGrad > horizontalGrad * 1.35f && verticalGrad > 28f) {
                        verticalStructure++
                    }
                }
                if (y < h / 4 && isSkyLikePixel(c)) sky++
            }
        }

        val circularVessel = circularVesselScore(small)
        val horizontalLines = horizontalRoadLineScore(small)
        if (small != bitmap) small.recycle()

        return SceneAnalysis(
            roadRatio = road.toFloat() / total,
            asphaltRatio = asphalt.toFloat() / total,
            indoorFloorRatio = indoorFloor.toFloat() / total,
            woodPanelRatio = woodPanel.toFloat() / total,
            verticalStructureRatio = verticalStructure.toFloat() / total,
            skyBandRatio = sky.toFloat() / (total / 4),
            highSaturationRatio = saturated.toFloat() / total,
            circularVesselScore = circularVessel,
            horizontalRoadLineScore = horizontalLines,
        )
    }

    /** Bins, buckets, indoor corners — not a road pothole close-up. */
    fun isLikelyNonRoadFalsePositive(analysis: SceneAnalysis): Boolean {
        if (analysis.circularVesselScore >= 0.55f && analysis.roadRatio < 0.38f) return true
        // Calibrated 0.50 / 0.30: true indoor floors saturate the indoor-tile
        // detector well above 0.50, while real Indian close-ups stayed
        // under 0.42 with road indicators above 0.37 in the calibration set.
        if (analysis.indoorFloorRatio >= 0.50f && analysis.roadRatio < 0.30f) return true
        if (analysis.woodPanelRatio >= 0.18f && analysis.asphaltRatio < 0.16f) return true
        if (analysis.verticalStructureRatio >= 0.18f && analysis.asphaltRatio < 0.12f) return true
        if (analysis.highSaturationRatio >= 0.07f && analysis.asphaltRatio < 0.12f) return true
        if (analysis.asphaltRatio < 0.05f && analysis.horizontalRoadLineScore < 0.05f && analysis.roadRatio < 0.24f) return true
        return false
    }

    /** Wide shot should look like an outdoor Indian street/highway scene. */
    fun looksLikeOutdoorIndianRoad(analysis: SceneAnalysis): Boolean {
        if (analysis.indoorFloorRatio >= 0.24f && analysis.roadRatio < 0.42f) return false
        if (analysis.verticalStructureRatio >= 0.22f && analysis.roadRatio < 0.30f) return false
        if (analysis.roadRatio < 0.24f) return false
        if (analysis.asphaltRatio < 0.05f && analysis.roadRatio < 0.34f) return false
        val outdoorCue = analysis.skyBandRatio >= 0.03f ||
            analysis.horizontalRoadLineScore >= 0.08f ||
            analysis.roadRatio >= 0.32f
        return outdoorCue
    }

    fun nonRoadRejectionReason(analysis: SceneAnalysis): String? = when {
        analysis.circularVesselScore >= 0.55f && analysis.roadRatio < 0.38f ->
            "This looks like a garbage bin, bucket, or round container — not a pothole on a road."
        analysis.indoorFloorRatio >= 0.50f && analysis.roadRatio < 0.30f ->
            "This looks like an indoor floor (tiles or room), not a road surface."
        analysis.woodPanelRatio >= 0.18f && analysis.asphaltRatio < 0.16f ->
            "Indoor walls or cabinetry detected. Photograph the pothole outdoors on the street."
        analysis.verticalStructureRatio >= 0.18f && analysis.asphaltRatio < 0.12f ->
            "Walls or vertical objects dominate the frame — photograph the pothole on the road."
        analysis.highSaturationRatio >= 0.07f && analysis.asphaltRatio < 0.12f ->
            "Colourful indoor objects detected. Frame only the road damage."
        analysis.asphaltRatio < 0.05f && analysis.horizontalRoadLineScore < 0.05f && analysis.roadRatio < 0.24f ->
            "Not enough asphalt or street surface visible in the photo."
        else -> null
    }

    fun wideRoadRejectionReason(analysis: SceneAnalysis): String? {
        nonRoadRejectionReason(analysis)?.let { return it }
        if (!looksLikeOutdoorIndianRoad(analysis)) {
            return "Capture a wide view of an Indian street, road, or highway with the pothole visible. " +
                "Avoid indoor areas, rooms, footpaths only, or close-ups."
        }
        return null
    }

    private fun circularVesselScore(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        val cx = w / 2
        val cy = h / 2
        val radius = min(w, h) / 5
        var darkCenter = 0
        var ringContrast = 0
        var ringSamples = 0
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = cx + dx
                val y = cy + dy
                if (x !in 0 until w || y !in 0 until h) continue
                val dist = hypot(dx.toDouble(), dy.toDouble())
                if (dist > radius) continue
                val lum = luminance(bitmap.getPixel(x, y))
                if (dist < radius * 0.45 && lum < 55f) darkCenter++
                if (dist in radius * 0.75..radius * 1.0) {
                    ringSamples++
                    val outerLum = lum
                    val innerLum = luminance(bitmap.getPixel(cx, cy))
                    if (outerLum - innerLum > 35f) ringContrast++
                }
            }
        }
        val centerArea = (radius * 0.45 * radius * 0.45 * Math.PI).toInt().coerceAtLeast(1)
        val darkFrac = darkCenter.toFloat() / centerArea
        val ringFrac = if (ringSamples == 0) 0f else ringContrast.toFloat() / ringSamples
        return (darkFrac * 0.65f + ringFrac * 0.35f).coerceIn(0f, 1f)
    }

    private fun horizontalRoadLineScore(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        var strong = 0
        var samples = 0
        for (y in h / 3 until h * 2 / 3) {
            for (x in 1 until w - 1) {
                val lum = luminance(bitmap.getPixel(x, y))
                val lumLeft = luminance(bitmap.getPixel(x - 1, y))
                val lumRight = luminance(bitmap.getPixel(x + 1, y))
                val horiz = abs(lum - lumLeft) + abs(lum - lumRight)
                val vert = abs(lum - luminance(bitmap.getPixel(x, y - 1))) +
                    abs(lum - luminance(bitmap.getPixel(x, y + 1)))
                if (horiz > vert * 1.2f) strong++
                samples++
            }
        }
        return if (samples == 0) 0f else strong.toFloat() / samples
    }

    private fun isAsphaltPixel(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val lum = luminance(color)
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
        return lum in 35f..190f && sat < 0.42f && abs(r - g) < 45 && abs(g - b) < 45
    }

    private fun isIndoorFloorTilePixel(color: Int): Boolean {
        // Calibrated against 37 real Indian close-up pothole photos:
        // bright sunlit asphalt sits at lum 165..195 with sat<0.20, which
        // the previous rule misclassified as indoor floor. Tightened by
        // raising the lower luminance bound and shrinking the colour
        // tolerance — true tile / lacquered floor is well within 195..245.
        val lum = luminance(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
        return lum in 195f..245f && sat < 0.18f && abs(r - g) < 18 && abs(g - b) < 18
    }

    private fun isWoodPanelPixel(color: Int): Boolean {
        val lum = luminance(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
        return lum in 35f..125f && sat < 0.45f && r >= g - 8 && r >= b - 12 && g >= b - 18
    }

    private fun isSkyLikePixel(color: Int): Boolean {
        val lum = luminance(color)
        val b = Color.blue(color)
        val r = Color.red(color)
        return lum > 165f && b >= r - 15
    }

    private fun isHighSaturationPixel(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
        return sat > 0.42f && maxC > 80
    }

    private fun isRoadLikePixel(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
        val lum = luminance(color)
        // Exclude very bright smooth indoor tiles but allow sunlit dusty roads.
        if (lum > 175f && sat < 0.18f) return false
        return lum in 30f..210f && sat < 0.52f && abs(r - g) < 55 && abs(g - b) < 55
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
