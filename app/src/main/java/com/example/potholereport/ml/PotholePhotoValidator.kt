package com.example.potholereport.ml

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

/**
 * Validates close-up and wide road photos using TensorFlow Lite pothole detection
 * plus scene heuristics (rejects bins, indoor floors, walls, etc.).
 */
object PotholePhotoValidator {

    /** ML must exceed this after scene checks pass. */
    private const val CLOSE_UP_MIN_ML_SCORE = 0.35f
    private const val CLOSE_UP_MIN_ROAD_RATIO = 0.20f
    private const val CLOSE_UP_MIN_ASPHALT_RATIO = 0.06f
    private const val CLOSE_UP_MIN_ROAD_LINE_SCORE = 0.05f

    private const val WIDE_MIN_ML_SCORE = 0.18f
    private const val WIDE_MIN_ROAD_RATIO = 0.24f
    private const val MIN_HASH_DISTANCE = 8

    @Volatile
    private var detector: PotholeDetector? = null

    fun validate(
        context: Context,
        uri: Uri,
        kind: PhotoCaptureKind,
        closeUpUri: Uri? = null,
    ): PhotoValidationResult {
        val bitmap = decodeBitmapFromUri(context, uri)
            ?: return PhotoValidationResult.Rejected(
                title = "Could not read photo",
                message = "Try taking the picture again.",
            )

        return try {
            val scene = SceneHeuristics.analyze(bitmap)
            val det = obtainDetector(context)
            val detection = det.bestPotholeDetection(bitmap)
            val potholeScore = detection?.score ?: 0f
            when (kind) {
                PhotoCaptureKind.CLOSE_UP -> validateCloseUp(bitmap, scene, detection, potholeScore)
                PhotoCaptureKind.WIDE_ROAD -> validateWide(context, bitmap, scene, potholeScore, closeUpUri)
            }
        } catch (e: Exception) {
            PhotoValidationResult.Rejected(
                title = "Photo check failed",
                message = "AI validation could not run. Please try again. (${e.message ?: "unknown"})",
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun validateCloseUp(
        bitmap: Bitmap,
        scene: SceneAnalysis,
        detection: PotholeDetection?,
        potholeScore: Float,
    ): PhotoValidationResult {
        SceneHeuristics.nonRoadRejectionReason(scene)?.let { reason ->
            return PhotoValidationResult.Rejected(
                title = "Not a road pothole",
                message = reason,
            )
        }

        if (SceneHeuristics.isLikelyNonRoadFalsePositive(scene)) {
            return PhotoValidationResult.Rejected(
                title = "Not a road pothole",
                message = SceneHeuristics.nonRoadRejectionReason(scene)
                    ?: "This does not look like road damage. Photograph the pothole on asphalt or concrete.",
            )
        }

        detection?.let { det ->
            // Keep bin/manhole false-positive guard, but do not reject genuine round potholes on road.
            if (det.isCentralCircularHazard() &&
                scene.circularVesselScore >= 0.62f &&
                scene.roadRatio < 0.42f
            ) {
                return PhotoValidationResult.Rejected(
                    title = "Not a road pothole",
                    message = "A round opening (bin, drain cover, bucket) was detected — not a pothole on a road.",
                )
            }
        }

        if (scene.asphaltRatio < CLOSE_UP_MIN_ASPHALT_RATIO &&
            scene.horizontalRoadLineScore < CLOSE_UP_MIN_ROAD_LINE_SCORE
        ) {
            return PhotoValidationResult.Rejected(
                title = "Not enough street surface",
                message = "Show clear asphalt or concrete on an Indian road. Bins, rooms, and indoor floors are not accepted.",
            )
        }

        if (scene.roadRatio < CLOSE_UP_MIN_ROAD_RATIO) {
            return PhotoValidationResult.Rejected(
                title = "Road surface not visible",
                message = "Fill the frame with the damaged road area. Avoid indoor floors, bins, or walls.",
            )
        }

        if (potholeScore < CLOSE_UP_MIN_ML_SCORE) {
            return PhotoValidationResult.Rejected(
                title = "Not recognized as a pothole",
                message = "Frame a clear pothole, crack, or road damage on the street surface. " +
                    "Avoid garbage bins, indoor objects, and plain floors.",
            )
        }

        return PhotoValidationResult.Accepted(
            confidence = potholeScore,
            summary = "Pothole on road detected (${(potholeScore * 100).toInt()}% confidence).",
        )
    }

    private fun validateWide(
        context: Context,
        bitmap: Bitmap,
        scene: SceneAnalysis,
        potholeScore: Float,
        closeUpUri: Uri?,
    ): PhotoValidationResult {
        SceneHeuristics.wideRoadRejectionReason(scene)?.let { reason ->
            return PhotoValidationResult.Rejected(
                title = "Not a road scene",
                message = reason,
            )
        }

        closeUpUri?.let { closeUri ->
            val closeBitmap = decodeBitmapFromUri(context, closeUri, maxSide = 640)
            if (closeBitmap != null) {
                try {
                    val closeScene = SceneHeuristics.analyze(closeBitmap)
                    if (SceneHeuristics.isLikelyNonRoadFalsePositive(closeScene)) {
                        return PhotoValidationResult.Rejected(
                            title = "Close-up not valid",
                            message = "The close-up photo does not look like a road pothole. Retake step 01 outdoors on the street.",
                        )
                    }
                    val dist = RoadSceneHeuristics.hammingDistance(
                        RoadSceneHeuristics.perceptualHash(bitmap),
                        RoadSceneHeuristics.perceptualHash(closeBitmap),
                    )
                    if (dist < MIN_HASH_DISTANCE) {
                        return PhotoValidationResult.Rejected(
                            title = "Use a wider road shot",
                            message = "This looks like the same close-up photo. Step back so the full road width " +
                                "and surroundings are visible.",
                        )
                    }
                } finally {
                    closeBitmap.recycle()
                }
            }
        }

        if (scene.roadRatio < WIDE_MIN_ROAD_RATIO) {
            return PhotoValidationResult.Rejected(
                title = "Not a wide road view",
                message = "Capture an Indian street, road, or highway with enough pavement visible.",
            )
        }

        if (!SceneHeuristics.looksLikeOutdoorIndianRoad(scene)) {
            return PhotoValidationResult.Rejected(
                title = "Does not look like a road",
                message = "Show a wide outdoor view of a street or highway in India with the pothole visible. " +
                    "Indoor rooms, tiled floors, and footpaths alone are not accepted.",
            )
        }

        if (potholeScore >= WIDE_MIN_ML_SCORE) {
            return PhotoValidationResult.Accepted(
                confidence = potholeScore,
                summary = "Wide road view with pothole (${(potholeScore * 100).toInt()}% confidence).",
            )
        }

        return PhotoValidationResult.Accepted(
            confidence = scene.roadRatio,
            summary = "Outdoor road scene accepted. Keep the pothole visible in the frame.",
        )
    }

    private fun obtainDetector(context: Context): PotholeDetector {
        detector?.let { return it }
        return synchronized(this) {
            detector ?: PotholeDetector(context.applicationContext).also { detector = it }
        }
    }
}
