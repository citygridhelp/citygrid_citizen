package com.example.potholereport.ml

import android.graphics.Bitmap
import android.graphics.Color
import com.example.potholereport.data.PotholePosition
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Finds the largest connected dark region in a close-up photo using the
 * reference Pothole Watch flood-fill approach, with two extensions:
 *   1. The search can be restricted to a vertical lane (left/middle/right)
 *      based on where the user said the pothole sits.
 *   2. We measure two extra signals that the calibration sweep showed are
 *      essential to separate real potholes from cement pipelines, walls,
 *      garbage piles, etc.:
 *
 *        - blobInsideAsphaltFrac: % of the blob's pixels that match the
 *          asphalt color profile. Real potholes ride on asphalt and inherit
 *          its hue; concrete-pipe interiors / shadowed walls do not.
 *        - ringAsphaltFrac: % of the box-perimeter ring classified asphalt.
 *
 * See tools/calibrate/blob_survey.csv for the per-photo measurements that
 * drove the rejection thresholds in [PotholeBlobAnalysis].
 */
internal data class PotholeBlobAnalysis(
    val detected: Boolean,
    /** Largest dark blob area as percent of the entire frame (0..100). */
    val areaPct: Float,
    /** (nonBlobMean - blobMean) / nonBlobMean, clamped 0..1. */
    val contrast: Float,
    /** Bounding box, normalised 0..1 image coords. */
    val xmin: Float,
    val ymin: Float,
    val xmax: Float,
    val ymax: Float,
    val aspect: Float,
    val edgeTouches: Int,
    val ringAsphaltFrac: Float,
    val blobInsideAsphaltFrac: Float,
)

internal object PotholeBlobLocator {

    /** Working resolution for the blob finder (matches reference code). */
    private const val TARGET_EDGE = 240

    /** Empty result used when the bitmap is too small or no blob qualifies. */
    private val EMPTY = PotholeBlobAnalysis(
        detected = false,
        areaPct = 0f, contrast = 0f,
        xmin = 0f, ymin = 0f, xmax = 0f, ymax = 0f,
        aspect = 1f, edgeTouches = 0,
        ringAsphaltFrac = 0f, blobInsideAsphaltFrac = 0f,
    )

    /**
     * @param position user-selected lane, or null to search the full frame.
     */
    fun analyze(bitmap: Bitmap, position: PotholePosition? = null): PotholeBlobAnalysis {
        val srcW = bitmap.width
        val srcH = bitmap.height
        if (srcW < 16 || srcH < 16) return EMPTY
        val scale = min(1.0, TARGET_EDGE.toDouble() / max(srcW, srcH))
        val w = max(1, (srcW * scale).roundToInt())
        val h = max(1, (srcH * scale).roundToInt())
        val small = if (w == srcW && h == srcH) bitmap
                    else Bitmap.createScaledBitmap(bitmap, w, h, true)

        val total = w * h
        val px = IntArray(total)
        small.getPixels(px, 0, w, 0, 0, w, h)

        val gray = IntArray(total)
        val asphalt = BooleanArray(total)
        var brightSum = 0L
        for (i in 0 until total) {
            val c = px[i]
            val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            gray[i] = lum
            brightSum += lum
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
            asphalt[i] = lum in 35..190 && sat < 0.42f
                && abs(r - g) < 45 && abs(g - b) < 45
        }
        val mean = brightSum.toDouble() / total
        var varSum = 0.0
        for (i in 0 until total) {
            val d = gray[i] - mean
            varSum += d * d
        }
        val std = sqrt(varSum / total)
        val threshold = max(20.0, mean - 0.6 * std).toInt()

        // Lane bounds - we only ALLOW seed pixels within the user-selected
        // vertical band, but the flood fill can still extend into adjacent
        // pixels so a half-in-lane blob is still measured fully.
        val lane = position?.laneRange
        val laneStart = lane?.let { (it.start * w).roundToInt().coerceIn(0, w - 1) } ?: 0
        val laneEnd = lane?.let { (it.endInclusive * w).roundToInt().coerceIn(laneStart + 1, w) } ?: w

        val mask = BooleanArray(total)
        for (i in 0 until total) mask[i] = gray[i] < threshold

        val visited = BooleanArray(total)
        val stack = IntArray(total)
        var bestSize = 0
        var bestSum = 0.0
        var bx0 = 0; var by0 = 0; var bx1 = 0; var by1 = 0
        var bestAsphaltCount = 0

        for (y in 0 until h) {
            for (x in laneStart until laneEnd) {
                val start = y * w + x
                if (!mask[start] || visited[start]) continue
                var sp = 0
                stack[sp++] = start
                visited[start] = true
                var size = 0
                var darkSum = 0.0
                var minX = w; var minY = h; var maxX = 0; var maxY = 0
                var asphaltCount = 0
                while (sp > 0) {
                    val ci = stack[--sp]
                    size++
                    darkSum += gray[ci]
                    if (asphalt[ci]) asphaltCount++
                    val cy = ci / w; val cx = ci - cy * w
                    if (cx < minX) minX = cx
                    if (cy < minY) minY = cy
                    if (cx > maxX) maxX = cx
                    if (cy > maxY) maxY = cy
                    if (cy > 0)     { val n = ci - w; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n } }
                    if (cy < h - 1) { val n = ci + w; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n } }
                    if (cx > 0)     { val n = ci - 1; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n } }
                    if (cx < w - 1) { val n = ci + 1; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n } }
                }
                if (size > bestSize) {
                    bestSize = size
                    bestSum = darkSum
                    bx0 = minX; by0 = minY; bx1 = maxX; by1 = maxY
                    bestAsphaltCount = asphaltCount
                }
            }
        }

        if (small != bitmap) small.recycle()
        if (bestSize < 30) return EMPTY

        val areaPct = bestSize.toFloat() / total * 100f
        val componentMean = bestSum / bestSize
        val nonBlobCount = max(1, total - bestSize)
        var nonBlobMean = (brightSum - bestSum) / nonBlobCount
        if (nonBlobMean <= 0) nonBlobMean = mean
        val contrast = max(0f,
            ((nonBlobMean - componentMean) / max(1.0, nonBlobMean)).toFloat()
        ).coerceAtMost(1f)

        var edgeTouches = 0
        if (bx0 == 0) edgeTouches++
        if (by0 == 0) edgeTouches++
        if (bx1 == w - 1) edgeTouches++
        if (by1 == h - 1) edgeTouches++

        val boxW = max(1, bx1 - bx0 + 1)
        val boxH = max(1, by1 - by0 + 1)
        val aspect = max(boxW, boxH).toFloat() / min(boxW, boxH)

        // Asphalt fraction in a ring around (but outside) the blob's bbox.
        val padX = max(2, boxW / 4)
        val padY = max(2, boxH / 4)
        val rx0 = max(0, bx0 - padX)
        val ry0 = max(0, by0 - padY)
        val rx1 = min(w - 1, bx1 + padX)
        val ry1 = min(h - 1, by1 + padY)
        var ringAsphalt = 0
        var ringN = 0
        for (yy in ry0..ry1) {
            for (xx in rx0..rx1) {
                if (xx in bx0..bx1 && yy in by0..by1) continue
                ringN++
                if (asphalt[yy * w + xx]) ringAsphalt++
            }
        }
        val ringFrac = if (ringN > 0) ringAsphalt.toFloat() / ringN else 0f
        val insideFrac = bestAsphaltCount.toFloat() / bestSize

        return PotholeBlobAnalysis(
            detected = true,
            areaPct = areaPct,
            contrast = contrast,
            xmin = bx0.toFloat() / w,
            ymin = by0.toFloat() / h,
            xmax = (bx1 + 1).toFloat() / w,
            ymax = (by1 + 1).toFloat() / h,
            aspect = aspect,
            edgeTouches = edgeTouches,
            ringAsphaltFrac = ringFrac,
            blobInsideAsphaltFrac = insideFrac,
        )
    }
}
