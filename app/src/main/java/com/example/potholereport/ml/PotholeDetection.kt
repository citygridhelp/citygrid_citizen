package com.example.potholereport.ml

/**
 * Best detection for class "Pothole" (normalized box 0..1).
 * Mask geometry fields are populated when the risk analyzer runs an adaptive
 * mask over the detection box — providing richer width/depth signals.
 */
internal data class PotholeDetection(
    val score: Float,
    val ymin: Float,
    val xmin: Float,
    val ymax: Float,
    val xmax: Float,
    val maskAreaFraction: Float? = null,
    val maskShapeIrregularity: Float? = null,
    val maskMeanLuminance: Float? = null,
    val maskDarkFraction: Float? = null,
    /**
     * Segmentation mask emitted by a YOLOv8-Seg model. When present, the risk
     * analyzer prefers this over its adaptive in-box thresholding mask.
     */
    val mask: PotholeMask? = null,
) {
    val areaFraction: Float get() = (ymax - ymin).coerceAtLeast(0f) * (xmax - xmin).coerceAtLeast(0f)

    fun isCentralCircularHazard(): Boolean {
        val cx = (xmin + xmax) / 2f
        val cy = (ymin + ymax) / 2f
        val inCenter = cx in 0.28f..0.72f && cy in 0.28f..0.72f
        val w = (xmax - xmin).coerceAtLeast(0.01f)
        val h = (ymax - ymin).coerceAtLeast(0.01f)
        val aspect = w / h
        val roundish = aspect in 0.65f..1.55f
        return inCenter && roundish && areaFraction >= 0.08f
    }

    fun hasMaskInfo(): Boolean = maskAreaFraction != null
}
