package com.example.potholereport.ml

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

/**
 * Validates close-up and wide road photos using **only heuristics**:
 *  - [SceneHeuristics] for road / asphalt / indoor / circular-vessel signatures.
 *  - [ContentClassifier] to reject people, animals, vegetation, food, photos
 *    of photos, phone/monitor screens, and other obvious non-road content.
 *
 * The on-device TFLite detector is intentionally not invoked in this build —
 * pothole confirmation will move to the cloud pipeline (see
 * [CloudPotholeAnalysisClient]).
 */
object PotholePhotoValidator {

    // Calibrated against 37 real Indian close-up pothole photos: roadRatio
    // distribution after the rule fixes was min=0.370, p10=0.453, mean=0.611.
    // Threshold kept conservatively low at 0.13 to leave dim/wet-asphalt
    // headroom while still rejecting frames with no visible road surface.
    private const val CLOSE_UP_MIN_ROAD_RATIO = 0.13f
    private const val CLOSE_UP_MIN_ASPHALT_RATIO = 0.06f
    private const val CLOSE_UP_MIN_ROAD_LINE_SCORE = 0.05f

    private const val WIDE_MIN_ROAD_RATIO = 0.24f
    private const val MIN_HASH_DISTANCE = 8

    // Blob-based rejection thresholds derived from a head-to-head sweep of
    // 34 unique real Indian close-up potholes vs 53 negative samples
    // (cement pipelines, garbage, manholes, road blockers, sign boards,
    // trees, wall boundaries). See tools/calibrate/blob_survey.csv.
    //
    // The rule below is the conservative "validator" version - tuned to
    // avoid false rejections of real potholes (relaxed thresholds). The
    // lane-aware risk analyzer applies tighter thresholds because it has
    // the user's left/middle/right hint to constrain the search.
    private const val BLOB_MAX_AREA_PCT = 30.0f      // pos max 18; margin
    private const val BLOB_MAX_CONTRAST = 0.80f      // pos max 0.74; margin
    private const val BLOB_MID_AREA_PCT = 18.0f
    private const val BLOB_MID_INSIDE_ASPHALT = 0.50f
    private const val BLOB_HARD_INSIDE_ASPHALT = 0.18f

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
            val content = ContentClassifier.classify(bitmap)

            ContentClassifier.rejectionReason(content)?.let { reason ->
                return PhotoValidationResult.Rejected(
                    title = "Not a road pothole",
                    message = reason,
                )
            }

            when (kind) {
                PhotoCaptureKind.CLOSE_UP -> validateCloseUp(bitmap, scene)
                PhotoCaptureKind.WIDE_ROAD -> validateWide(context, bitmap, scene, closeUpUri)
            }
        } catch (e: Exception) {
            PhotoValidationResult.Rejected(
                title = "Photo check failed",
                message = "Photo validation could not run. Please try again. (${e.message ?: "unknown"})",
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun validateCloseUp(
        bitmap: Bitmap,
        scene: SceneAnalysis,
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

        // Bin / drain-cover guard from scene heuristics alone (no ML).
        if (scene.circularVesselScore >= 0.62f && scene.roadRatio < 0.42f) {
            return PhotoValidationResult.Rejected(
                title = "Not a road pothole",
                message = "A round opening (bin, drain cover, bucket) was detected — not a pothole on a road.",
            )
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

        // Final blob-based pass: looks for the largest dark blob in the
        // frame and rejects if its size, contrast, or asphalt-overlap
        // signature matches one of the negative-sample categories
        // (cement pipelines, garbage, manholes, road blockers, sign
        // boards, trees, wall boundaries). See thresholds above.
        val blob = PotholeBlobLocator.analyze(bitmap, position = null)
        if (blob.detected) {
            blobRejectionReason(blob)?.let { reason ->
                return PhotoValidationResult.Rejected(
                    title = "Not a road pothole",
                    message = reason,
                )
            }
        }

        // Confidence is derived from how road-like the frame is.
        val confidence = (scene.roadRatio * 0.6f + scene.asphaltRatio * 0.3f +
            scene.horizontalRoadLineScore * 0.1f).coerceIn(0f, 1f)
        return PhotoValidationResult.Accepted(
            confidence = confidence,
            summary = "Road surface accepted. Keep the pothole centred in the frame.",
        )
    }

    /**
     * Map a triggered blob-rejection rule to a user-facing message that
     * tells the photographer why their photo is not a road pothole.
     */
    private fun blobRejectionReason(blob: PotholeBlobAnalysis): String? = when {
        blob.areaPct >= BLOB_MAX_AREA_PCT ->
            "The dark area takes up too much of the frame. Photograph an actual " +
                "pothole on the road, not a wall, garbage pile, or other large object."
        blob.contrast >= BLOB_MAX_CONTRAST ->
            "Photo contrast is too extreme for a road pothole. Frame the damaged " +
                "asphalt squarely without strong shadows or backlit objects."
        blob.areaPct >= BLOB_MID_AREA_PCT &&
            blob.blobInsideAsphaltFrac < BLOB_MID_INSIDE_ASPHALT ->
            "This dark area is not on a typical asphalt road (looks like a cement " +
                "pipe, wall, or other concrete object)."
        blob.blobInsideAsphaltFrac < BLOB_HARD_INSIDE_ASPHALT &&
            blob.areaPct >= 4.0f ->
            "The dark region in the photo is not asphalt. Frame the damaged road " +
                "surface only."
        else -> null
    }

    private fun validateWide(
        context: Context,
        bitmap: Bitmap,
        scene: SceneAnalysis,
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
                    val closeContent = ContentClassifier.classify(closeBitmap)
                    ContentClassifier.rejectionReason(closeContent)?.let {
                        return PhotoValidationResult.Rejected(
                            title = "Close-up not valid",
                            message = "The close-up photo does not look like a road pothole. Retake step 01 outdoors on the street.",
                        )
                    }
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

        return PhotoValidationResult.Accepted(
            confidence = scene.roadRatio,
            summary = "Outdoor road scene accepted. Keep the pothole visible in the frame.",
        )
    }
}
