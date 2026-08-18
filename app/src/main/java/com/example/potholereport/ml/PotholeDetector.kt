package com.example.potholereport.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max

/**
 * Pothole detector that auto-detects the loaded TFLite model format:
 *  - SSD (legacy: 4 output tensors — boxes, classes, scores, num)
 *  - YOLOv8 detection (1 output: [1, 4 + nc, anchors] or [1, anchors, 4 + nc])
 *  - YOLOv8-Seg (2 outputs: detection head + proto masks)
 *
 * Load via [tryLoad] / [PotholeDetectorHolder] — missing `pothole_detect.tflite`
 * is a no-op (heuristics-only validator).
 *
 * For YOLO-Seg we currently use the detection head only — masks are derived in
 * [PotholeRiskAnalyzer] from the detection box (adaptive in-box thresholding),
 * which keeps inference simple and works for SSD/YOLO/YOLO-Seg uniformly.
 */
internal class PotholeDetector private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val inputWidth: Int,
    private val inputHeight: Int,
    private val isFloatingModel: Boolean,
    private val modelKind: ModelKind,
    private val ssdBoxesIndex: Int,
    private val ssdClassesIndex: Int,
    private val ssdScoresIndex: Int,
    private val ssdPotholeClassIndex: Int,
    private val yoloOutputIndex: Int,
    private val yoloChannelFirst: Boolean,
    private val yoloChannels: Int,
    private val yoloAnchors: Int,
    private val protoOutputIndex: Int,
    private val protoHeight: Int,
    private val protoWidth: Int,
    private val protoChannels: Int,
    private val protoChannelFirst: Boolean,
) {

    private enum class ModelKind { SSD, YOLO_V8, YOLO_V8_SEG }

    fun maxPotholeScore(bitmap: Bitmap): Float = bestPotholeDetection(bitmap)?.score ?: 0f

    fun bestPotholeDetection(bitmap: Bitmap): PotholeDetection? = try {
        when (modelKind) {
            ModelKind.SSD -> runSsd(bitmap)
            ModelKind.YOLO_V8, ModelKind.YOLO_V8_SEG -> runYolo(bitmap)
        }
    } catch (_: Exception) {
        null
    }

    fun close() {
        try {
            interpreter.close()
        } catch (_: Exception) {
        }
    }

    // ---------------- SSD path (legacy, unchanged) ----------------

    private fun runSsd(bitmap: Bitmap): PotholeDetection? {
        val input = preprocessSsd(bitmap)
        val detectionCount = interpreter.getOutputTensor(ssdScoresIndex).shape()[1]
        val boxes = Array(1) { Array(detectionCount) { FloatArray(4) } }
        val classes = Array(1) { FloatArray(detectionCount) }
        val scores = Array(1) { FloatArray(detectionCount) }
        val outputs = mapOf(
            ssdBoxesIndex to boxes,
            ssdClassesIndex to classes,
            ssdScoresIndex to scores,
        )
        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        var best: PotholeDetection? = null
        val scoreArr = scores[0]
        val classArr = classes[0]
        val boxArr = boxes[0]
        for (i in scoreArr.indices) {
            val score = scoreArr[i]
            if (score <= 0f || score > 1f) continue
            val classIdx = classArr[i].toInt()
            if (classIdx != ssdPotholeClassIndex) continue
            val box = boxArr[i]
            val candidate = PotholeDetection(
                score = score,
                ymin = box[0],
                xmin = box[1],
                ymax = box[2],
                xmax = box[3],
            )
            if (best == null || candidate.score > best!!.score) {
                best = candidate
            }
        }
        return best
    }

    private fun preprocessSsd(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val buffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputWidth * inputHeight)
        scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        if (scaled != bitmap) scaled.recycle()
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)
            if (isFloatingModel) {
                buffer.putFloat((r - 127.5f) / 127.5f)
                buffer.putFloat((g - 127.5f) / 127.5f)
                buffer.putFloat((b - 127.5f) / 127.5f)
            } else {
                buffer.put(r.toByte())
                buffer.put(g.toByte())
                buffer.put(b.toByte())
            }
        }
        buffer.rewind()
        return buffer
    }

    // ---------------- YOLOv8 detection path ----------------

    private fun runYolo(bitmap: Bitmap): PotholeDetection? {
        if (yoloOutputIndex < 0 || yoloChannels < 5 || yoloAnchors <= 0) return null
        val input = preprocessYolo(bitmap)
        val shape = interpreter.getOutputTensor(yoloOutputIndex).shape()
        val output = Array(1) { Array(shape[1]) { FloatArray(shape[2]) } }

        // Optionally collect proto-mask tensor for YOLOv8-Seg.
        val protoBuf: Any? = if (modelKind == ModelKind.YOLO_V8_SEG && protoOutputIndex >= 0) {
            allocateGenericOutputBuffer(interpreter.getOutputTensor(protoOutputIndex).shape())
        } else null

        if (interpreter.outputTensorCount == 1) {
            interpreter.run(input, output)
        } else {
            val outs = mutableMapOf<Int, Any>(yoloOutputIndex to output)
            for (i in 0 until interpreter.outputTensorCount) {
                if (i == yoloOutputIndex) continue
                outs[i] = if (i == protoOutputIndex && protoBuf != null) {
                    protoBuf
                } else {
                    allocateGenericOutputBuffer(interpreter.getOutputTensor(i).shape())
                }
            }
            interpreter.runForMultipleInputsOutputs(arrayOf(input), outs)
        }
        val out = output[0]

        // Determine class / mask-coefficient split for the detection head.
        val numMaskCoeffs = if (modelKind == ModelKind.YOLO_V8_SEG) protoChannels else 0
        val nClasses = (yoloChannels - 4 - numMaskCoeffs).coerceAtLeast(1)

        var bestScore = 0f
        var bestIdx = -1
        for (n in 0 until yoloAnchors) {
            var topScore = 0f
            for (c in 0 until nClasses) {
                val s = if (yoloChannelFirst) out[4 + c][n] else out[n][4 + c]
                if (s > topScore) topScore = s
            }
            if (topScore > bestScore) {
                bestScore = topScore
                bestIdx = n
            }
        }
        if (bestIdx < 0 || bestScore < PotholeDetectorGate.YOLO_MIN_SCORE) return null

        val cx = if (yoloChannelFirst) out[0][bestIdx] else out[bestIdx][0]
        val cy = if (yoloChannelFirst) out[1][bestIdx] else out[bestIdx][1]
        val bw = if (yoloChannelFirst) out[2][bestIdx] else out[bestIdx][2]
        val bh = if (yoloChannelFirst) out[3][bestIdx] else out[bestIdx][3]

        val maxCoord = maxOf(cx, cy, bw, bh)
        val needsScale = maxCoord > 1.5f
        val ncx = if (needsScale) cx / inputWidth else cx
        val ncy = if (needsScale) cy / inputHeight else cy
        val nbw = if (needsScale) bw / inputWidth else bw
        val nbh = if (needsScale) bh / inputHeight else bh

        val xmin = (ncx - nbw / 2f).coerceIn(0f, 1f)
        val ymin = (ncy - nbh / 2f).coerceIn(0f, 1f)
        val xmax = (ncx + nbw / 2f).coerceIn(0f, 1f)
        val ymax = (ncy + nbh / 2f).coerceIn(0f, 1f)

        // Decode segmentation mask (YOLOv8-Seg only).
        var segMask: PotholeMask? = null
        if (numMaskCoeffs > 0 && protoBuf != null) {
            val coeffs = FloatArray(numMaskCoeffs)
            for (c in 0 until numMaskCoeffs) {
                val channelIdx = 4 + nClasses + c
                coeffs[c] = if (yoloChannelFirst) out[channelIdx][bestIdx] else out[bestIdx][channelIdx]
            }
            segMask = decodeYoloSegMask(
                coefficients = coeffs,
                protoBuffer = protoBuf,
                boxXmin = xmin,
                boxYmin = ymin,
                boxXmax = xmax,
                boxYmax = ymax,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
            )
        }

        return PotholeDetection(
            score = bestScore,
            ymin = ymin,
            xmin = xmin,
            ymax = ymax,
            xmax = xmax,
            mask = segMask,
        )
    }

    /**
     * Multiplies mask coefficients with the proto tensor, sigmoids the result
     * and thresholds at 0.5 — clipped to the detection box — producing a
     * binary mask at proto resolution.
     */
    private fun decodeYoloSegMask(
        coefficients: FloatArray,
        protoBuffer: Any,
        boxXmin: Float,
        boxYmin: Float,
        boxXmax: Float,
        boxYmax: Float,
        imageWidth: Int,
        imageHeight: Int,
    ): PotholeMask? {
        if (protoHeight <= 0 || protoWidth <= 0 || protoChannels <= 0) return null
        val mh = protoHeight
        val mw = protoWidth
        val nm = protoChannels

        @Suppress("UNCHECKED_CAST")
        val proto = protoBuffer as? Array<*> ?: return null
        // proto[0] is the batch (size 1).
        @Suppress("UNCHECKED_CAST")
        val batch = proto[0] as? Array<*> ?: return null

        val data = BooleanArray(mh * mw)
        val bx0 = (boxXmin * mw).toInt().coerceIn(0, mw - 1)
        val by0 = (boxYmin * mh).toInt().coerceIn(0, mh - 1)
        val bx1 = (boxXmax * mw).toInt().coerceIn(bx0 + 1, mw)
        val by1 = (boxYmax * mh).toInt().coerceIn(by0 + 1, mh)

        if (protoChannelFirst) {
            // [1, nm, mh, mw]: batch[c] is Array<FloatArray> of [mh][mw].
            for (y in by0 until by1) {
                for (x in bx0 until bx1) {
                    var dot = 0f
                    for (c in 0 until nm) {
                        @Suppress("UNCHECKED_CAST")
                        val plane = batch[c] as Array<FloatArray>
                        dot += coefficients[c] * plane[y][x]
                    }
                    if (sigmoid(dot) > 0.5f) data[y * mw + x] = true
                }
            }
        } else {
            // [1, mh, mw, nm]: batch[y] is Array<FloatArray> of [mw][nm].
            for (y in by0 until by1) {
                @Suppress("UNCHECKED_CAST")
                val row = batch[y] as Array<FloatArray>
                for (x in bx0 until bx1) {
                    val cell = row[x]
                    var dot = 0f
                    for (c in 0 until nm) {
                        dot += coefficients[c] * cell[c]
                    }
                    if (sigmoid(dot) > 0.5f) data[y * mw + x] = true
                }
            }
        }

        return PotholeMask(
            width = mw,
            height = mh,
            data = data,
            sourceImageWidth = imageWidth,
            sourceImageHeight = imageHeight,
        )
    }

    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

    private fun preprocessYolo(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val buffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputWidth * inputHeight)
        scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        if (scaled != bitmap) scaled.recycle()
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)
            if (isFloatingModel) {
                buffer.putFloat(r / 255f)
                buffer.putFloat(g / 255f)
                buffer.putFloat(b / 255f)
            } else {
                buffer.put(r.toByte())
                buffer.put(g.toByte())
                buffer.put(b.toByte())
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun allocateGenericOutputBuffer(shape: IntArray): Any = when (shape.size) {
        1 -> FloatArray(shape[0])
        2 -> Array(shape[0]) { FloatArray(shape[1]) }
        3 -> Array(shape[0]) { Array(shape[1]) { FloatArray(shape[2]) } }
        4 -> Array(shape[0]) { Array(shape[1]) { Array(shape[2]) { FloatArray(shape[3]) } } }
        else -> FloatArray(shape.fold(1) { acc, v -> acc * v })
    }

    companion object {
        private const val ASSET_DIR = "ml"
        private const val MODEL_FILE_NAME = "pothole_detect.tflite"
        private const val MODEL_FILE = "$ASSET_DIR/$MODEL_FILE_NAME"
        private const val LABELS_FILE = "$ASSET_DIR/pothole_labels.txt"

        /**
         * Returns a detector when `assets/ml/pothole_detect.tflite` is present
         * and loads cleanly; otherwise null (heuristics-only).
         */
        fun tryLoad(context: Context): PotholeDetector? {
            return try {
                val assetManager = context.assets
                val files = assetManager.list(ASSET_DIR) ?: return null
                if (MODEL_FILE_NAME !in files) return null
                val modelBuffer = loadModelFile(assetManager.openFd(MODEL_FILE))
                val interpreter = Interpreter(modelBuffer)
                val labels = try {
                    assetManager.open(LABELS_FILE).bufferedReader().useLines { lines ->
                        lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
                    }
                } catch (_: Exception) {
                    listOf("Pothole")
                }
                val inputTensor = interpreter.getInputTensor(0)
                val inputHeight = inputTensor.shape()[1]
                val inputWidth = inputTensor.shape()[2]
                val isFloatingModel = inputTensor.dataType().name == "FLOAT32"
                val modelKind = autoDetectModelKind(interpreter)

                var boxes = 0
                var classes = 1
                var scores = 2
                var isTf2 = false
                for (i in 0 until interpreter.outputTensorCount) {
                    val name = interpreter.getOutputTensor(i).name()
                    if (name.contains("StatefulPartitionedCall")) isTf2 = true
                    when {
                        name.contains("scores", ignoreCase = true) -> scores = i
                        name.contains("classes", ignoreCase = true) -> classes = i
                        name.contains("boxes", ignoreCase = true) ||
                            name.contains("locations", ignoreCase = true) -> boxes = i
                    }
                }
                if (isTf2) {
                    scores = 0
                    boxes = 1
                    classes = 3
                }
                val ssdPotholeClassIndex = labels.indexOfFirst {
                    it.equals("Pothole", ignoreCase = true)
                }.takeIf { it >= 0 } ?: 0

                var yoloIdx = -1
                var channelFirst = true
                var channels = 0
                var anchors = 0
                if (modelKind == ModelKind.YOLO_V8 || modelKind == ModelKind.YOLO_V8_SEG) {
                    for (i in 0 until interpreter.outputTensorCount) {
                        val shape = interpreter.getOutputTensor(i).shape()
                        if (shape.size == 3 && shape[0] == 1) {
                            val a = shape[1]
                            val b = shape[2]
                            if (a in 5..120 && b > a * 4) {
                                yoloIdx = i
                                channelFirst = true
                                channels = a
                                anchors = b
                                break
                            }
                            if (b in 5..120 && a > b * 4) {
                                yoloIdx = i
                                channelFirst = false
                                channels = b
                                anchors = a
                                break
                            }
                        }
                    }
                }

                var protoIdx = -1
                var ph = 0
                var pw = 0
                var pc = 0
                var protoCF = false
                if (modelKind == ModelKind.YOLO_V8_SEG) {
                    for (i in 0 until interpreter.outputTensorCount) {
                        val shape = interpreter.getOutputTensor(i).shape()
                        if (shape.size == 4 && shape[0] == 1) {
                            protoIdx = i
                            val a = shape[1]
                            val b = shape[2]
                            val c = shape[3]
                            if (a in 8..96 && b > a * 2 && c > a * 2) {
                                protoCF = true
                                pc = a; ph = b; pw = c
                            } else {
                                protoCF = false
                                ph = a; pw = b; pc = c
                            }
                            break
                        }
                    }
                }

                PotholeDetector(
                    interpreter = interpreter,
                    labels = labels,
                    inputWidth = inputWidth,
                    inputHeight = inputHeight,
                    isFloatingModel = isFloatingModel,
                    modelKind = modelKind,
                    ssdBoxesIndex = boxes,
                    ssdClassesIndex = classes,
                    ssdScoresIndex = scores,
                    ssdPotholeClassIndex = ssdPotholeClassIndex,
                    yoloOutputIndex = yoloIdx,
                    yoloChannelFirst = channelFirst,
                    yoloChannels = channels,
                    yoloAnchors = anchors,
                    protoOutputIndex = protoIdx,
                    protoHeight = ph,
                    protoWidth = pw,
                    protoChannels = pc,
                    protoChannelFirst = protoCF,
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun autoDetectModelKind(interpreter: Interpreter): ModelKind {
            val outCount = interpreter.outputTensorCount
            if (outCount >= 4) return ModelKind.SSD
            if (outCount == 2) {
                val a = interpreter.getOutputTensor(0).shape()
                val b = interpreter.getOutputTensor(1).shape()
                if (a.size == 4 || b.size == 4) return ModelKind.YOLO_V8_SEG
            }
            if (outCount == 1) {
                val shape = interpreter.getOutputTensor(0).shape()
                if (shape.size == 3 && shape[0] == 1) return ModelKind.YOLO_V8
            }
            return ModelKind.SSD
        }

        private fun loadModelFile(fd: android.content.res.AssetFileDescriptor): MappedByteBuffer {
            FileInputStream(fd.fileDescriptor).use { input ->
                val channel = input.channel
                return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }
}

/**
 * Process-wide optional detector. Loads once on first use; safe when the
 * TFLite asset is missing.
 */
internal object PotholeDetectorHolder {
    @Volatile
    private var detector: PotholeDetector? = null

    @Volatile
    private var loadAttempted = false

    fun get(context: Context): PotholeDetector? {
        if (loadAttempted) return detector
        synchronized(this) {
            if (loadAttempted) return detector
            detector = PotholeDetector.tryLoad(context.applicationContext)
            loadAttempted = true
            return detector
        }
    }

    /** Test / debug: clear cached instance. */
    fun resetForTests() {
        synchronized(this) {
            detector?.close()
            detector = null
            loadAttempted = false
            PotholeDetectionCache.clear()
        }
    }
}

/** Caches last close-up detection so risk analysis can reuse the bbox/mask. */
internal object PotholeDetectionCache {
    @Volatile
    private var uriKey: String? = null

    @Volatile
    private var detection: PotholeDetection? = null

    fun put(uri: Uri, det: PotholeDetection?) {
        uriKey = uri.toString()
        detection = det
    }

    fun get(uri: Uri): PotholeDetection? {
        return if (uriKey == uri.toString()) detection else null
    }

    fun clear() {
        uriKey = null
        detection = null
    }
}

internal fun decodeBitmapFromUri(context: Context, uri: Uri, maxSide: Int = 1280): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, bounds)
            val sample = max(1, max(bounds.outWidth, bounds.outHeight) / maxSide)
            context.contentResolver.openInputStream(uri)?.use { stream2 ->
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeStream(stream2, null, opts)
            }
        }
    } catch (_: Exception) {
        null
    }
}
