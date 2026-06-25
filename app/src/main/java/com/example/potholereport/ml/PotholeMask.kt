package com.example.potholereport.ml

/**
 * A binary segmentation mask attached to a [PotholeDetection].
 *
 * The mask is stored at its native (low) resolution — usually the YOLOv8-Seg
 * proto grid (e.g. 160×160) — together with the source image dimensions, so
 * callers can sample mask membership for any image pixel without resizing.
 */
internal data class PotholeMask(
    val width: Int,
    val height: Int,
    val data: BooleanArray,
    val sourceImageWidth: Int,
    val sourceImageHeight: Int,
) {
    fun containsImagePixel(imageX: Int, imageY: Int): Boolean {
        if (sourceImageWidth <= 0 || sourceImageHeight <= 0) return false
        val mx = (imageX.toFloat() / sourceImageWidth * width)
            .toInt()
            .coerceIn(0, width - 1)
        val my = (imageY.toFloat() / sourceImageHeight * height)
            .toInt()
            .coerceIn(0, height - 1)
        return data[my * width + mx]
    }

    /** Total mask pixel count (proto resolution). */
    fun activePixels(): Int {
        var n = 0
        for (b in data) if (b) n++
        return n
    }

    // Auto-generated equals / hashCode are needed because BooleanArray is an array.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PotholeMask) return false
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
