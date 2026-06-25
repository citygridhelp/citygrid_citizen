package com.example.potholereport.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

/**
 * Relative monocular depth produced by an external TFLite model
 * (e.g. MiDaS-small or Depth-Anything-v2-small).
 *
 * The depth map is stored at the model's native output resolution. Larger
 * values mean *closer* surfaces for MiDaS-style models; the analyzer only
 * cares about local contrast (std / mean) inside the pothole mask, so we
 * normalize globally to 0..1.
 */
internal data class DepthMap(
    val width: Int,
    val height: Int,
    val data: FloatArray,
    val sourceImageWidth: Int,
    val sourceImageHeight: Int,
) {
    fun sample(imageX: Int, imageY: Int): Float {
        if (sourceImageWidth <= 0 || sourceImageHeight <= 0) return 0f
        val dx = (imageX.toFloat() / sourceImageWidth * width)
            .toInt()
            .coerceIn(0, width - 1)
        val dy = (imageY.toFloat() / sourceImageHeight * height)
            .toInt()
            .coerceIn(0, height - 1)
        return data[dy * width + dx]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DepthMap) return false
        return width == other.width &&
            height == other.height &&
            sourceImageWidth == other.sourceImageWidth &&
            sourceImageHeight == other.sourceImageHeight &&
            data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + sourceImageWidth
        result = 31 * result + sourceImageHeight
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Optional monocular depth estimator. Loads only if
 * `assets/ml/depth.tflite` is present — otherwise [tryLoad] returns null and
 * callers fall back to luminance-based depth heuristics.
 *
 * Supported preprocessing: FP32 in [0,1] (the default for most depth model
 * TFLite exports). For models that need ImageNet mean/std, retrain with
 * normalization baked into the graph or extend this class.
 */
internal class MonocularDepthEstimator private constructor(
    private val interpreter: Interpreter,
    private val inputWidth: Int,
    private val inputHeight: Int,
    private val outputWidth: Int,
    private val outputHeight: Int,
) {

    fun close() {
        try {
            interpreter.close()
        } catch (_: Exception) {
        }
    }

    fun estimate(bitmap: Bitmap): DepthMap? {
        if (inputWidth <= 0 || inputHeight <= 0 || outputWidth <= 0 || outputHeight <= 0) {
            return null
        }
        return try {
            val input = preprocess(bitmap)
            val raw = Array(1) { Array(outputHeight) { FloatArray(outputWidth) } }
            try {
                interpreter.run(input, raw)
            } catch (_: Exception) {
                // Some depth exports use shape [1, 1, h, w] or [1, h, w, 1].
                val alt = Array(1) { Array(1) { Array(outputHeight) { FloatArray(outputWidth) } } }
                interpreter.run(input, alt)
                return finalize(alt[0][0], bitmap)
            }
            finalize(raw[0], bitmap)
        } catch (_: Exception) {
            null
        }
    }

    private fun finalize(grid: Array<FloatArray>, bitmap: Bitmap): DepthMap {
        val flat = FloatArray(outputHeight * outputWidth)
        var minV = Float.POSITIVE_INFINITY
        var maxV = Float.NEGATIVE_INFINITY
        for (y in 0 until outputHeight) {
            for (x in 0 until outputWidth) {
                val v = grid[y][x]
                flat[y * outputWidth + x] = v
                if (v < minV) minV = v
                if (v > maxV) maxV = v
            }
        }
        val span = (maxV - minV).coerceAtLeast(1e-6f)
        for (i in flat.indices) {
            flat[i] = ((flat[i] - minV) / span).coerceIn(0f, 1f)
        }
        return DepthMap(
            width = outputWidth,
            height = outputHeight,
            data = flat,
            sourceImageWidth = bitmap.width,
            sourceImageHeight = bitmap.height,
        )
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val buf = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
        buf.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputWidth * inputHeight)
        scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        if (scaled != bitmap) scaled.recycle()
        for (p in pixels) {
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            buf.putFloat(r)
            buf.putFloat(g)
            buf.putFloat(b)
        }
        buf.rewind()
        return buf
    }

    companion object {
        private const val ASSET_DIR = "ml"
        private const val MODEL_FILE_NAME = "depth.tflite"
        private const val MODEL_ASSET_PATH = "$ASSET_DIR/$MODEL_FILE_NAME"

        fun tryLoad(context: Context): MonocularDepthEstimator? {
            return try {
                val assetManager = context.assets
                val files = assetManager.list(ASSET_DIR) ?: return null
                if (MODEL_FILE_NAME !in files) return null
                val fd = assetManager.openFd(MODEL_ASSET_PATH)
                val buffer: MappedByteBuffer = FileInputStream(fd.fileDescriptor).use { stream ->
                    stream.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fd.startOffset,
                        fd.declaredLength,
                    )
                }
                val interp = Interpreter(buffer)
                val inT = interp.getInputTensor(0)
                val outT = interp.getOutputTensor(0)
                val inShape = inT.shape() // [1, h, w, 3] expected
                val outShape = outT.shape()
                if (inShape.size < 3) {
                    interp.close()
                    return null
                }
                val inH = inShape[1]
                val inW = inShape[2]
                // Output may be [1, h, w], [1, 1, h, w] or [1, h, w, 1].
                val (outH, outW) = when (outShape.size) {
                    3 -> outShape[1] to outShape[2]
                    4 -> {
                        if (outShape[3] == 1) outShape[1] to outShape[2]
                        else outShape[2] to outShape[3]
                    }
                    else -> return null.also { interp.close() }
                }
                if (inH <= 0 || inW <= 0 || outH <= 0 || outW <= 0) {
                    interp.close()
                    return null
                }
                val safeOutH = max(1, min(outH, 1024))
                val safeOutW = max(1, min(outW, 1024))
                MonocularDepthEstimator(interp, inW, inH, safeOutW, safeOutH)
            } catch (_: Exception) {
                null
            }
        }
    }
}
