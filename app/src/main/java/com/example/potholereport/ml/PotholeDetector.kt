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
import kotlin.math.max

/**
 * TensorFlow Lite SSD detector trained for the "Pothole" class.
 * Model: Intelligent-Pothole-Detection-System (detect.tflite).
 */
internal class PotholeDetector(context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputWidth: Int
    private val inputHeight: Int
    private val isFloatingModel: Boolean
    private val boxesIndex: Int
    private val classesIndex: Int
    private val scoresIndex: Int
    private val potholeClassIndex: Int

    init {
        val assetManager = context.assets
        val modelBuffer = loadModelFile(assetManager.openFd(MODEL_FILE))
        interpreter = Interpreter(modelBuffer)
        labels = assetManager.open(LABELS_FILE).bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
        val inputTensor = interpreter.getInputTensor(0)
        inputHeight = inputTensor.shape()[1]
        inputWidth = inputTensor.shape()[2]
        isFloatingModel = inputTensor.dataType().name == "FLOAT32"

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
        boxesIndex = boxes
        classesIndex = classes
        scoresIndex = scores
        potholeClassIndex = labels.indexOfFirst { it.equals("Pothole", ignoreCase = true) }
            .takeIf { it >= 0 } ?: labels.lastIndex
    }

    fun maxPotholeScore(bitmap: Bitmap): Float = bestPotholeDetection(bitmap)?.score ?: 0f

    fun bestPotholeDetection(bitmap: Bitmap): PotholeDetection? {
        val input = preprocess(bitmap)
        val detectionCount = interpreter.getOutputTensor(scoresIndex).shape()[1]
        val boxes = Array(1) { Array(detectionCount) { FloatArray(4) } }
        val classes = Array(1) { FloatArray(detectionCount) }
        val scores = Array(1) { FloatArray(detectionCount) }
        val outputs = mapOf(
            boxesIndex to boxes,
            classesIndex to classes,
            scoresIndex to scores,
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
            if (classIdx != potholeClassIndex) continue
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

    fun close() {
        interpreter.close()
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
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

    private fun loadModelFile(fd: android.content.res.AssetFileDescriptor): MappedByteBuffer {
        FileInputStream(fd.fileDescriptor).use { input ->
            val channel = input.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    companion object {
        private const val MODEL_FILE = "ml/pothole_detect.tflite"
        private const val LABELS_FILE = "ml/pothole_labels.txt"
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
