package com.example.potholereport.ml

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

internal object RoadSceneHeuristics {

    /** Fraction of pixels that look like asphalt / road surface. */
    fun roadSurfaceRatio(bitmap: Bitmap): Float {
        val w = 64
        val h = 64
        val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
        var roadPixels = 0
        val total = w * h
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = small.getPixel(x, y)
                if (isRoadLikePixel(c)) roadPixels++
            }
        }
        if (small != bitmap) small.recycle()
        return roadPixels.toFloat() / total
    }

    /** Wide shots usually show more upper-frame detail (sky/horizon band). */
    fun upperBandEdgeScore(bitmap: Bitmap): Float {
        val w = 48
        val h = 48
        val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
        var edgeSum = 0f
        val yEnd = h / 3
        for (y in 1 until yEnd) {
            for (x in 1 until w - 1) {
                val c = luminance(small.getPixel(x, y))
                val dx = abs(c - luminance(small.getPixel(x + 1, y)))
                val dy = abs(c - luminance(small.getPixel(x, y + 1)))
                edgeSum += dx + dy
            }
        }
        if (small != bitmap) small.recycle()
        return edgeSum / (w * yEnd)
    }

    fun perceptualHash(bitmap: Bitmap): Long {
        val size = 8
        val small = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val gray = FloatArray(size * size)
        var sum = 0f
        var i = 0
        for (y in 0 until size) {
            for (x in 0 until size) {
                val lum = luminance(small.getPixel(x, y))
                gray[i++] = lum
                sum += lum
            }
        }
        val avg = sum / gray.size
        var hash = 0L
        gray.forEachIndexed { index, value ->
            if (value >= avg) {
                hash = hash or (1L shl index)
            }
        }
        if (small != bitmap) small.recycle()
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int =
        java.lang.Long.bitCount(a xor b)

    private fun isRoadLikePixel(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
        val lum = luminance(color)
        return lum in 35f..185f && sat < 0.45f && abs(r - g) < 35 && abs(g - b) < 35
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
