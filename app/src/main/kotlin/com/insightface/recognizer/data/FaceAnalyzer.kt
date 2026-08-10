package com.insightface.recognizer.data

import android.graphics.Bitmap
import android.graphics.RectF
import com.insightface.sdk.inspireface.InspireFace
import com.insightface.sdk.inspireface.base.FaceAttributeResult
import com.insightface.sdk.inspireface.base.FaceInteractionState
import com.insightface.sdk.inspireface.base.FaceInteractionsActions
import com.insightface.sdk.inspireface.base.FaceMaskConfidence
import com.insightface.sdk.inspireface.base.FaceQualityConfidence
import com.insightface.sdk.inspireface.base.FaceRect
import com.insightface.sdk.inspireface.base.FaceFeature
import com.insightface.sdk.inspireface.base.ImageStream
import com.insightface.sdk.inspireface.base.MultipleFaceData
import com.insightface.sdk.inspireface.base.Point2f
import com.insightface.sdk.inspireface.base.Session

/**
 * Runs the full InspireFace still-image pipeline in one stream lifetime and materializes
 * EVERY item the SDK returns: detection box, 106-point dense landmarks, 512-d embedding
 * feature, aligned crop, mask confidence, age bracket, race, gender, image quality, jaw
 * open state and left/right eye state — plus an optional 1:N FeatureHub search result.
 *
 * This is a Kotlin consolidation of the official example's `FaceImageProcessor` +
 * `FaceAttributeProcessor`. All SDK calls are the verified official API.
 *
 * IMPORTANT: every method here must run on the single thread that owns [session].
 */
object FaceAnalyzer {

    enum class Status { READY, NO_FACE, PROCESS_FAILED }

    /** A single detected face with all SDK-provided information. */
    data class Face(
        val index: Int,
        val rect: RectF,
        val token: Long,
        val denseLandmarks: List<Point2f>?,
        val feature: FaceFeature?,
        val crop: Bitmap?,
        val attributes: Attributes,
        /** Recognition against the local FeatureHub. Null when no feature was extracted. */
        val recognition: Recognition?,
    )

    /** All attribute items the SDK exposes for a face. */
    data class Attributes(
        val maskConfidence: Float,
        val ageBracket: Int,
        val race: Int,
        val gender: Int,
        val qualityScore: Float,
        val jawOpen: Int,
        val leftEyeConfidence: Float,
        val rightEyeConfidence: Float,
    )

    data class Recognition(
        val matched: Boolean,
        val identityId: Long,
        val confidence: Float,
        val threshold: Float,
    )

    data class Result(
        val status: Status,
        val faces: List<Face>,
    )

    private const val INTERACTION_WARMUP_CALLS = 10

    /**
     * Detects every face in [bitmap], extracts features + dense landmarks + aligned crop,
     * runs the attribute pipeline and (when [repository] is open) searches each face's
     * embedding against the local FeatureHub.
     */
    fun analyze(
        session: Session,
        bitmap: Bitmap,
        repository: FaceRepository? = null,
    ): Result {
        val stream: ImageStream = InspireFace.CreateImageStreamFromBitmap(
            bitmap, InspireFace.CAMERA_ROTATION_0
        ) ?: return Result(Status.PROCESS_FAILED, emptyList())

        try {
            val faces: MultipleFaceData = InspireFace.ExecuteFaceTrack(session, stream)
                ?: return Result(Status.PROCESS_FAILED, emptyList())
            if (faces.detectedNum <= 0) return Result(Status.NO_FACE, emptyList())

            // --- Attribute pipeline (mask / quality / demographic / interaction) ---
            val attributeParam = InspireFace.CreateCustomParameter()
                .enableMaskDetect(true)
                .enableFaceQuality(true)
                .enableFaceAttribute(true)
                .enableInteractionLiveness(true)
            if (!InspireFace.MultipleFacePipelineProcess(session, stream, faces, attributeParam)) {
                return Result(Status.PROCESS_FAILED, emptyList())
            }
            val masks: FaceMaskConfidence? = InspireFace.GetFaceMaskConfidence(session)
            val qualities: FaceQualityConfidence? = InspireFace.GetFaceQualityConfidence(session)
            val faceAttrs: FaceAttributeResult? = InspireFace.GetFaceAttributeResult(session)

            // The interaction module has a short warm-up; reuse the same still frame so the
            // eye/jaw state stabilizes deterministically (mirrors the official example).
            val interactionParam = InspireFace.CreateCustomParameter()
                .enableInteractionLiveness(true)
            repeat(INTERACTION_WARMUP_CALLS - 1) {
                InspireFace.MultipleFacePipelineProcess(session, stream, faces, interactionParam)
            }
            val eyeStates: FaceInteractionState? = InspireFace.GetFaceInteractionStateResult(session)
            val actions: FaceInteractionsActions? = InspireFace.GetFaceInteractionActionsResult(session)

            val threshold = InspireFace.GetRecommendedCosineThreshold()
            val out = ArrayList<Face>(faces.detectedNum)
            for (i in 0 until faces.detectedNum) {
                val fr: FaceRect = faces.rects[i]
                val rect = RectF(fr.x, fr.y, fr.x + fr.width, fr.y + fr.height)
                val token = faces.tokens[i]

                val feature = InspireFace.ExtractFaceFeature(session, stream, token)
                val landmarks = InspireFace.GetFaceDenseLandmarkFromFaceToken(token)
                var crop = InspireFace.GetFaceAlignmentImage(session, stream, token)
                if (crop != null && crop === bitmap) {
                    // Keep crop ownership independent from the source image.
                    crop = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                }

                val attrs = Attributes(
                    maskConfidence = floatAt(masks?.confidence, i),
                    ageBracket = intAt(faceAttrs?.ageBracket, i),
                    race = intAt(faceAttrs?.race, i),
                    gender = intAt(faceAttrs?.gender, i),
                    qualityScore = floatAt(qualities?.confidence, i),
                    jawOpen = intAt(actions?.jawOpen, i),
                    leftEyeConfidence = floatAt(eyeStates?.leftEyeStatusConfidence, i),
                    rightEyeConfidence = floatAt(eyeStates?.rightEyeStatusConfidence, i),
                )

                val recognition = if (repository != null && feature != null) {
                    repository.search(feature, threshold)
                } else null

                out.add(Face(i, rect, token, landmarks?.toList(), feature, crop, attrs, recognition))
            }
            return Result(Status.READY, out)
        } finally {
            InspireFace.ReleaseImageStream(stream)
        }
    }

    private fun floatAt(values: FloatArray?, index: Int): Float =
        if (values != null && index in values.indices) values[index] else Float.NaN

    private fun intAt(values: IntArray?, index: Int): Int =
        if (values != null && index in values.indices) values[index] else -1
}
