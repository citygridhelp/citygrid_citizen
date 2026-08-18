package com.example.potholereport.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.example.potholereport.data.PotholePosition
import com.example.potholereport.data.PotholeSeverity
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PotholeRiskInsight(
    val estimatedWidthCm: Int,
    val estimatedDepthCm: Int,
    val suggestedSeverity: PotholeSeverity,
    val suggestedSpeedLimitKmph: Int,
    val criticalityLabel: String,
) {
    val advisoryLine: String
        get() = "Estimated width ${estimatedWidthCm}cm, depth ${estimatedDepthCm}cm · " +
            "Suggested max speed ${suggestedSpeedLimitKmph} km/h."
}

/**
 * Hybrid risk analyzer. Prefers an on-device YOLO/SSD bbox (Phase A) when
 * [PotholeDetectionCache] has a strong detection for this close-up; otherwise
 * uses [PotholeBlobLocator] in the selected lane.
 *
 * Analysis pipeline:
 *
 *   1. If a [CloudPotholeAnalysisClient] is registered, defer to it.
 *   2. Prefer cached [PotholeDetection] (score ≥ [PotholeDetectorGate.YOLO_MIN_SCORE])
 *      → [enrichWithMaskGeometry] for width/depth signals.
 *   3. Else [PotholeBlobLocator] in the user-selected lane.
 *   4. Risk score → severity + speed advisory.
 *
 * Numbers are advisory only - not engineering measurements.
 */
object PotholeRiskAnalyzer {

    /**
     * Assumed in-frame surface area when the user has framed a typical
     * close-up of a pothole (~80 cm x 60 cm of road in the photo).
     * Same constant as the reference Pothole Watch web pipeline.
     */
    private const val APPROX_FRAME_AREA_CM2 = 4800f

    suspend fun analyze(
        context: Context,
        closeUpUri: Uri,
        position: PotholePosition = PotholePosition.MIDDLE,
        userSeverity: PotholeSeverity? = null,
    ): PotholeRiskInsight? {
        // Cloud takes precedence when registered.
        CloudPotholeAnalysisClient.current()?.let { cloud ->
            val cloudInsight = try {
                cloud.analyze(context.applicationContext, closeUpUri)
            } catch (_: Exception) {
                null
            }
            if (cloudInsight != null) return cloudInsight
        }

        val bitmap = decodeBitmapFromUri(context, closeUpUri, maxSide = 960) ?: return null
        return try {
            val cached = PotholeDetectionCache.get(closeUpUri)
            val strongDet = cached?.takeIf { it.score >= PotholeDetectorGate.YOLO_MIN_SCORE }
            if (strongDet != null) {
                val enriched = enrichWithMaskGeometry(bitmap, strongDet)
                return insightFromDetection(enriched, userSeverity)
            }

            val blob = PotholeBlobLocator.analyze(bitmap, position)
            if (!blob.detected || blob.areaPct < 0.5f) {
                // Optional: run detector now if cache miss but model is loaded.
                val fresh = PotholeDetectorHolder.get(context)?.bestPotholeDetection(bitmap)
                if (fresh != null && fresh.score >= PotholeDetectorGate.YOLO_MIN_SCORE) {
                    PotholeDetectionCache.put(closeUpUri, fresh)
                    val enriched = enrichWithMaskGeometry(bitmap, fresh)
                    return insightFromDetection(enriched, userSeverity)
                }
                return null
            }

            val areaCm2 = ((blob.areaPct / 100f) * APPROX_FRAME_AREA_CM2)
                .coerceAtLeast(20f)
            val baseWidthCm = sqrt(areaCm2)
            val widthCm = (baseWidthCm * sqrt(blob.aspect.coerceIn(1f, 4f)))
                .coerceIn(8f, 180f)
                .roundToInt()
            val depthCm = (blob.contrast * 22f)
                .coerceIn(0.5f, 18f)
                .roundToInt()

            insightFromScores(
                widthCm = widthCm,
                depthCm = depthCm,
                areaPct = blob.areaPct,
                contrast = blob.contrast,
                userSeverity = userSeverity,
            )
        } catch (_: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun insightFromDetection(
        det: PotholeDetection,
        userSeverity: PotholeSeverity?,
    ): PotholeRiskInsight {
        val areaFrac = det.maskAreaFraction?.coerceIn(0.01f, 0.85f)
            ?: det.areaFraction.coerceIn(0.01f, 0.85f)
        val areaPct = areaFrac * 100f
        val areaCm2 = (areaFrac * APPROX_FRAME_AREA_CM2).coerceAtLeast(20f)
        val boxAspect = ((det.xmax - det.xmin) / (det.ymax - det.ymin).coerceAtLeast(0.05f))
            .coerceIn(0.4f, 4f)
        val widthCm = (sqrt(areaCm2) * sqrt(boxAspect))
            .coerceIn(8f, 180f)
            .roundToInt()
        val dark = det.maskDarkFraction ?: 0.45f
        val contrast = (1f - (det.maskMeanLuminance ?: 90f) / 180f)
            .coerceIn(0.15f, 0.95f)
            .let { c -> (c * 0.6f + dark * 0.4f).coerceIn(0.15f, 0.95f) }
        val depthCm = (contrast * 22f).coerceIn(0.5f, 18f).roundToInt()
        return insightFromScores(
            widthCm = widthCm,
            depthCm = depthCm,
            areaPct = areaPct,
            contrast = contrast,
            userSeverity = userSeverity,
        )
    }

    private fun insightFromScores(
        widthCm: Int,
        depthCm: Int,
        areaPct: Float,
        contrast: Float,
        userSeverity: PotholeSeverity?,
    ): PotholeRiskInsight {
        val sevForScore = when (userSeverity) {
            PotholeSeverity.MINOR -> 1
            PotholeSeverity.MODERATE -> 2
            PotholeSeverity.SEVERE -> 3
            PotholeSeverity.CRITICAL -> 4
            null -> 2
        }
        val depthScore = (depthCm * 10f).coerceAtMost(100f)
        val areaScore = (areaPct * 4f).coerceAtMost(100f)
        val contrastScore = (contrast * 180f).coerceAtMost(100f)
        val severityScore = (sevForScore * 25f)
        val riskScore = (
            depthScore * 0.30f +
                areaScore * 0.25f +
                contrastScore * 0.15f +
                severityScore * 0.30f
            ).roundToInt()

        val (severity, label, speedKmh) = when {
            riskScore >= 75 -> Triple(PotholeSeverity.CRITICAL, "CRITICAL", 20)
            riskScore >= 55 -> Triple(PotholeSeverity.SEVERE, "HIGH", 30)
            riskScore >= 35 -> Triple(PotholeSeverity.MODERATE, "MODERATE", 45)
            else -> Triple(PotholeSeverity.MINOR, "LOW", 60)
        }
        return PotholeRiskInsight(
            estimatedWidthCm = widthCm,
            estimatedDepthCm = depthCm,
            suggestedSeverity = severity,
            suggestedSpeedLimitKmph = speedKmh,
            criticalityLabel = label,
        )
    }

    // ------------------------------------------------------------------
    // Adaptive-baseline mask geometry helpers — used when YOLO/SSD
    // supplies a detection box (Phase A) or a seg mask.
    // ------------------------------------------------------------------

    internal fun enrichWithMaskGeometry(bitmap: Bitmap, det: PotholeDetection): PotholeDetection {
        return if (det.mask != null) statsFromSegMask(bitmap, det)
               else statsFromAdaptiveMask(bitmap, det)
    }

    private fun statsFromSegMask(bitmap: Bitmap, det: PotholeDetection): PotholeDetection {
        val mask = det.mask ?: return det
        val imgW = bitmap.width
        val imgH = bitmap.height

        val x0 = (det.xmin * imgW).roundToInt().coerceIn(0, imgW - 1)
        val y0 = (det.ymin * imgH).roundToInt().coerceIn(0, imgH - 1)
        val x1 = (det.xmax * imgW).roundToInt().coerceIn(x0 + 1, imgW)
        val y1 = (det.ymax * imgH).roundToInt().coerceIn(y0 + 1, imgH)
        val boxW = (x1 - x0).coerceAtLeast(2)
        val boxH = (y1 - y0).coerceAtLeast(2)

        val baseline = sampleAsphaltBaseline(bitmap, x0, y0, x1, y1)
        val baseLum = baseline.first.coerceAtLeast(40f)
        val baseStd = baseline.second.coerceAtLeast(8f)

        var maskCount = 0
        var sumDarkLum = 0f
        var darkCount = 0
        var sumLum = 0f
        for (py in y0 until y1) {
            for (px in x0 until x1) {
                val lum = luminance(bitmap.getPixel(px, py))
                sumLum += lum
                if (mask.containsImagePixel(px, py)) {
                    maskCount++
                    sumDarkLum += lum
                    if (lum < baseLum - 0.6f * baseStd) darkCount++
                }
            }
        }
        if (maskCount < 8) {
            return det.copy(
                maskAreaFraction = 0f,
                maskShapeIrregularity = 0f,
                maskMeanLuminance = sumLum / (boxW * boxH).toFloat(),
                maskDarkFraction = 0f,
            )
        }

        val boxAreaPx = (boxW * boxH).toFloat()
        val maskAreaFraction = (maskCount.toFloat() / boxAreaPx).coerceIn(0f, 1f)

        val mw = mask.width
        val mh = mask.height
        val mx0 = (det.xmin * mw).toInt().coerceIn(0, mw - 1)
        val my0 = (det.ymin * mh).toInt().coerceIn(0, mh - 1)
        val mx1 = (det.xmax * mw).toInt().coerceIn(mx0 + 1, mw)
        val my1 = (det.ymax * mh).toInt().coerceIn(my0 + 1, mh)
        var area = 0
        var perimeter = 0
        for (y in my0 until my1) {
            for (x in mx0 until mx1) {
                if (!mask.data[y * mw + x]) continue
                area++
                val left = x == 0 || !mask.data[y * mw + (x - 1)]
                val right = x == mw - 1 || !mask.data[y * mw + (x + 1)]
                val up = y == 0 || !mask.data[(y - 1) * mw + x]
                val down = y == mh - 1 || !mask.data[(y + 1) * mw + x]
                if (left || right || up || down) perimeter++
            }
        }
        val areaF = area.toFloat()
        val circularity = if (areaF > 0f) {
            (4f * PI.toFloat() * areaF) /
                (perimeter.toFloat() * perimeter.toFloat()).coerceAtLeast(1f)
        } else 0f
        val irregularity = (1f - circularity).coerceIn(0f, 1f)

        return det.copy(
            maskAreaFraction = maskAreaFraction,
            maskShapeIrregularity = irregularity,
            maskMeanLuminance = sumDarkLum / maskCount.toFloat(),
            maskDarkFraction = darkCount.toFloat() / maskCount.toFloat(),
        )
    }

    private fun statsFromAdaptiveMask(bitmap: Bitmap, det: PotholeDetection): PotholeDetection {
        val w = bitmap.width
        val h = bitmap.height
        val x0 = (det.xmin * w).roundToInt().coerceIn(0, w - 1)
        val y0 = (det.ymin * h).roundToInt().coerceIn(0, h - 1)
        val x1 = (det.xmax * w).roundToInt().coerceIn(x0 + 1, w)
        val y1 = (det.ymax * h).roundToInt().coerceIn(y0 + 1, h)
        val boxW = (x1 - x0).coerceAtLeast(2)
        val boxH = (y1 - y0).coerceAtLeast(2)

        val baseline = sampleAsphaltBaseline(bitmap, x0, y0, x1, y1)
        val baseLum = baseline.first
        val baseStd = baseline.second.coerceAtLeast(8f)

        val mask = BooleanArray(boxW * boxH)
        var maskCount = 0
        var sumLum = 0f
        var sumDarkLum = 0f
        var darkCount = 0
        val darknessThreshold = (baseLum - 0.65f * baseStd).coerceAtLeast(15f)
        for (yy in 0 until boxH) {
            val py = y0 + yy
            for (xx in 0 until boxW) {
                val px = x0 + xx
                val c = bitmap.getPixel(px, py)
                val lum = luminance(c)
                sumLum += lum
                val sat = saturation(c)
                val belongs = lum < darknessThreshold && sat < 0.55f
                if (belongs) {
                    mask[yy * boxW + xx] = true
                    maskCount++
                    sumDarkLum += lum
                    if (lum < darknessThreshold - 0.3f * baseStd) darkCount++
                }
            }
        }
        if (maskCount < 8) {
            return det.copy(
                maskAreaFraction = 0f,
                maskShapeIrregularity = 0f,
                maskMeanLuminance = sumLum / (boxW * boxH).toFloat(),
                maskDarkFraction = 0f,
            )
        }

        val maskAreaPx = maskCount.toFloat()
        val boxAreaPx = (boxW * boxH).toFloat()
        val maskAreaFraction = maskAreaPx / boxAreaPx

        var perimeter = 0
        for (yy in 0 until boxH) {
            for (xx in 0 until boxW) {
                if (!mask[yy * boxW + xx]) continue
                val left = xx == 0 || !mask[yy * boxW + (xx - 1)]
                val right = xx == boxW - 1 || !mask[yy * boxW + (xx + 1)]
                val up = yy == 0 || !mask[(yy - 1) * boxW + xx]
                val down = yy == boxH - 1 || !mask[(yy + 1) * boxW + xx]
                if (left || right || up || down) perimeter++
            }
        }
        val circularity = if (maskAreaPx > 0f) {
            (4f * PI.toFloat() * maskAreaPx) /
                (perimeter.toFloat() * perimeter.toFloat()).coerceAtLeast(1f)
        } else 0f
        val irregularity = (1f - circularity).coerceIn(0f, 1f)

        return det.copy(
            maskAreaFraction = maskAreaFraction,
            maskShapeIrregularity = irregularity,
            maskMeanLuminance = sumDarkLum / maskCount.toFloat(),
            maskDarkFraction = darkCount.toFloat() / maskCount.toFloat(),
        )
    }

    private fun sampleAsphaltBaseline(
        bitmap: Bitmap,
        x0: Int, y0: Int, x1: Int, y1: Int,
    ): Pair<Float, Float> {
        val w = bitmap.width
        val h = bitmap.height
        val pad = ((maxOf(x1 - x0, y1 - y0) * 0.20f).roundToInt()).coerceAtLeast(6)
        val rx0 = (x0 - pad).coerceAtLeast(0)
        val ry0 = (y0 - pad).coerceAtLeast(0)
        val rx1 = (x1 + pad).coerceAtMost(w)
        val ry1 = (y1 + pad).coerceAtMost(h)

        var sum = 0f
        var sumSq = 0f
        var n = 0
        for (yy in ry0 until ry1) {
            for (xx in rx0 until rx1) {
                val inBox = xx in x0 until x1 && yy in y0 until y1
                if (inBox) continue
                val lum = luminance(bitmap.getPixel(xx, yy))
                sum += lum
                sumSq += lum * lum
                n++
            }
        }
        if (n == 0) return 110f to 32f
        val mean = sum / n
        val variance = (sumSq / n - mean * mean).coerceAtLeast(0f)
        return mean to sqrt(variance.toDouble()).toFloat()
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    private fun saturation(color: Int): Float {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        if (maxC == 0) return 0f
        return (maxC - minC).toFloat() / maxC.toFloat()
    }
}
