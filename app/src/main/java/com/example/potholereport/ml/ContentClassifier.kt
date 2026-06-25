package com.example.potholereport.ml

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pixel-level heuristic classifier that rejects photos which are obviously
 * NOT a road pothole — people, animals, plants, food, household objects,
 * photos of photos, phone/monitor screens.
 *
 * Pure heuristics, no ML — used while the on-device detector is disabled
 * and before the cloud AI pipeline is wired up.
 *
 * Designed to be **conservative**: never aggressively reject a real pothole
 * photo, only catch the unmistakeable non-road content.
 */
internal data class ContentAnalysis(
    val skinRatio: Float,
    val vegetationRatio: Float,
    val animalFurRatio: Float,
    val foodOrObjectColorRatio: Float,
    val rectFrameLikelihood: Float,
    val flatScreenLikelihood: Float,
    val saturatedNonRoadRatio: Float,
)

internal object ContentClassifier {

    private const val SAMPLE_DIM = 72

    fun classify(bitmap: Bitmap): ContentAnalysis {
        val small = Bitmap.createScaledBitmap(bitmap, SAMPLE_DIM, SAMPLE_DIM, true)
        val w = small.width
        val h = small.height
        val total = w * h

        var skin = 0
        var vegetation = 0
        var fur = 0
        var foodObject = 0
        var saturatedNonRoad = 0

        var sumLum = 0f
        var sumLumSq = 0f
        var saturatedSum = 0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = small.getPixel(x, y)
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)
                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                sumLum += lum
                sumLumSq += lum * lum
                if (sat > 0.30f && maxC > 70) saturatedSum++

                when {
                    isSkinPixel(r, g, b) -> skin++
                    isVegetationPixel(r, g, b) -> vegetation++
                    isAnimalFurPixel(r, g, b, sat, lum) -> fur++
                    isSaturatedNonRoadPixel(r, g, b, sat, lum) -> {
                        foodObject++
                        saturatedNonRoad++
                    }
                }
            }
        }

        val rectFrame = rectangularFrameScore(small)
        val flatScreen = flatScreenScore(sumLum, sumLumSq, total, saturatedSum)
        if (small != bitmap) small.recycle()

        return ContentAnalysis(
            skinRatio = skin.toFloat() / total,
            vegetationRatio = vegetation.toFloat() / total,
            animalFurRatio = fur.toFloat() / total,
            foodOrObjectColorRatio = foodObject.toFloat() / total,
            rectFrameLikelihood = rectFrame,
            flatScreenLikelihood = flatScreen,
            saturatedNonRoadRatio = saturatedNonRoad.toFloat() / total,
        )
    }

    /**
     * Returns a user-facing rejection reason if the content is clearly not a
     * road pothole, otherwise null.
     */
    fun rejectionReason(analysis: ContentAnalysis): String? = when {
        // Calibration thresholds derived from 37 real Indian close-up
        // pothole photos. See tools/train_pothole_model/CALIBRATION_NOTES.md.
        analysis.skinRatio >= 0.45f ->
            "This looks like a person. Photograph the pothole on the road only."
        analysis.vegetationRatio >= 0.32f ->
            "This looks like grass, plants, or trees. Photograph the pothole on the road surface."
        analysis.animalFurRatio >= 0.55f ->
            "This looks like an animal. Photograph the pothole on the road only."
        // Rectangular-frame "photo of photo" detector intentionally disabled
        // (threshold > 1.0): too many real road close-ups have strong edges
        // on all four sides (curbs, markings, tar lines). Cloud AI handles
        // this cleaner than pixel heuristics can.
        analysis.rectFrameLikelihood >= 1.05f ->
            "This looks like a photo of a photo or printed picture. Please take a fresh photo of the actual pothole."
        analysis.flatScreenLikelihood >= 0.62f ->
            "This looks like a phone or monitor screen. Please photograph the actual road pothole."
        analysis.foodOrObjectColorRatio >= 0.30f && analysis.saturatedNonRoadRatio >= 0.22f ->
            "This looks like an object, food, or sign — not road damage. Frame the damaged road only."
        else -> null
    }

    // ---------------- pixel classifiers ----------------

    /**
     * Kovac/Peer/Jones skin-tone rules + chroma & luminance constraints
     * calibrated against real Indian sunlit asphalt (which the original
     * Kovac rule misclassified as skin — skinRatio reached 0.835 in the
     * 37-photo calibration set). Tightened to require noticeably greater
     * R-G / G-B separations and a higher minimum luminance.
     */
    private fun isSkinPixel(r: Int, g: Int, b: Int): Boolean {
        if (r <= 110 || g <= 50 || b <= 25) return false
        if (max(r, max(g, b)) - min(r, min(g, b)) <= 25) return false
        if (r - g <= 25) return false
        if (g - b <= 18) return false
        if (r <= g || r <= b) return false
        if (r > 220 && g < 80 && b < 80) return false
        val lum = 0.299f * r + 0.587f * g + 0.114f * b
        if (lum < 95f) return false
        return true
    }

    private fun isVegetationPixel(r: Int, g: Int, b: Int): Boolean {
        // Green-dominant: 2G > R + B + small bias, plus enough chroma to not
        // be greenish concrete.
        if (g <= r + 8) return false
        if (g <= b + 8) return false
        val chroma = max(r, max(g, b)) - min(r, min(g, b))
        if (chroma < 22) return false
        // Avoid flagging shadowed road which can have a slight green cast.
        if (g < 60) return false
        return true
    }

    /**
     * Warm fur tones (tan, brown, golden, light cream) at mid luminance with
     * non-trivial chroma. Calibrated against real Indian sunlit asphalt
     * (animalFurRatio reached 0.700 in the 37-photo set with the original
     * rule). Tightened to require greater R-G / G-B separations and higher
     * saturation so warm asphalt no longer matches.
     */
    private fun isAnimalFurPixel(r: Int, g: Int, b: Int, sat: Float, lum: Float): Boolean {
        if (lum < 80f || lum > 230f) return false
        if (sat < 0.28f || sat > 0.55f) return false
        if (r <= g + 12 || g <= b + 12) return false
        if (r - b < 50 || r - b > 120) return false
        return true
    }

    private fun isSaturatedNonRoadPixel(r: Int, g: Int, b: Int, sat: Float, lum: Float): Boolean {
        if (sat < 0.42f) return false
        if (lum < 50f) return false
        // Any strongly-saturated colour is non-road. Asphalt sits at sat < 0.4.
        return true
    }

    // ---------------- structural cues ----------------

    /**
     * Detects a rectangular high-contrast frame near the image perimeter — a
     * common signature of "photo of a photo" or a printed picture.
     */
    private fun rectangularFrameScore(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        val band = max(2, min(w, h) / 14)
        var topEdges = 0
        var bottomEdges = 0
        var leftEdges = 0
        var rightEdges = 0

        for (x in 1 until w - 1) {
            for (y in 0 until band) {
                if (isStrongVerticalGradient(bitmap, x, y)) topEdges++
                val by = h - 1 - y
                if (isStrongVerticalGradient(bitmap, x, by)) bottomEdges++
            }
        }
        for (y in 1 until h - 1) {
            for (x in 0 until band) {
                if (isStrongHorizontalGradient(bitmap, x, y)) leftEdges++
                val rx = w - 1 - x
                if (isStrongHorizontalGradient(bitmap, rx, y)) rightEdges++
            }
        }

        val sideArea = band * w
        val vertSideArea = band * h
        val topF = topEdges.toFloat() / sideArea
        val botF = bottomEdges.toFloat() / sideArea
        val leftF = leftEdges.toFloat() / vertSideArea
        val rightF = rightEdges.toFloat() / vertSideArea

        // High when ALL FOUR sides have strong perpendicular gradients — a
        // road photo never produces that pattern.
        val sides = floatArrayOf(topF, botF, leftF, rightF)
        sides.sort()
        val minSide = sides[0]
        return (minSide * 6f).coerceIn(0f, 1f)
    }

    private fun isStrongVerticalGradient(bitmap: Bitmap, x: Int, y: Int): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (x !in 1 until w - 1 || y !in 1 until h - 1) return false
        val l = lum(bitmap.getPixel(x, y))
        val u = lum(bitmap.getPixel(x, y - 1))
        val d = lum(bitmap.getPixel(x, y + 1))
        return abs(l - u) + abs(l - d) > 70f
    }

    private fun isStrongHorizontalGradient(bitmap: Bitmap, x: Int, y: Int): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (x !in 1 until w - 1 || y !in 1 until h - 1) return false
        val l = lum(bitmap.getPixel(x, y))
        val left = lum(bitmap.getPixel(x - 1, y))
        val right = lum(bitmap.getPixel(x + 1, y))
        return abs(l - left) + abs(l - right) > 70f
    }

    /**
     * Phone/monitor screens tend to have very low global luminance variance
     * combined with non-trivial saturation (UI colours) — quite unlike a
     * textured asphalt photo which has high local variance.
     */
    private fun flatScreenScore(sumLum: Float, sumLumSq: Float, n: Int, saturatedCount: Int): Float {
        if (n == 0) return 0f
        val mean = sumLum / n
        val variance = (sumLumSq / n - mean * mean).coerceAtLeast(0f)
        val std = kotlin.math.sqrt(variance.toDouble()).toFloat()
        val flatness = (1f - (std / 60f)).coerceIn(0f, 1f)
        val saturationFrac = saturatedCount.toFloat() / n
        // Need both very flat AND coloured — pure asphalt is flat-ish but desaturated.
        val saturationScore = (saturationFrac * 4f).coerceIn(0f, 1f)
        return (flatness * 0.6f + saturationScore * 0.4f).coerceIn(0f, 1f)
    }

    private fun lum(color: Int): Float {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
