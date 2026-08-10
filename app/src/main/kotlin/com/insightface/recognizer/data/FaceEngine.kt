package com.insightface.recognizer.data

import android.content.Context
import android.util.Log
import com.insightface.sdk.inspireface.InspireFace
import com.insightface.sdk.inspireface.base.CustomParameter
import com.insightface.sdk.inspireface.base.Session

/**
 * Process-wide InspireFace lifecycle. A direct Kotlin port of the official Android example's
 * `FaceEngine`, using the exact same official API calls:
 *  - [InspireFace.GlobalLaunch] / [InspireFace.GlobalTerminate]
 *  - [InspireFace.CreateCustomParameter] + [InspireFace.CreateSession]
 *
 * The model packs ("Pikachu" / "Megatron") are bundled inside the JitPack AAR and are
 * unpacked by GlobalLaunch on first run — no manual model download is required.
 */
object FaceEngine {

    private const val TAG = "FaceEngine"
    const val DEFAULT_MODEL = "Megatron"
    const val LIGHT_MODEL = "Pikachu"

    private const val IMAGE_MAX_FACES = 10
    private const val DEFAULT_INPUT_PX = 640

    @Volatile private var launched = false
    private var launchedModel: String? = null
    private var activeSessions = 0
    private val lock = Any()

    /** Loads [model] globally; switching models terminates the previous one first. */
    fun ensureLaunched(context: Context, model: String = DEFAULT_MODEL): Boolean = synchronized(lock) {
        if (launched && model == launchedModel) return true
        if (launched) {
            if (activeSessions > 0) {
                Log.e(TAG, "Cannot switch model while $activeSessions session(s) are active")
                return false
            }
            if (InspireFace.GlobalTerminate().not()) {
                Log.e(TAG, "GlobalTerminate failed for $launchedModel")
                return false
            }
            launched = false
            launchedModel = null
        }
        launched = java.lang.Boolean.TRUE ==
                InspireFace.GlobalLaunch(context.applicationContext, model)
        launchedModel = if (launched) model else null
        Log.i(TAG, "GlobalLaunch($model) -> $launched")
        return launched
    }

    val isLaunched: Boolean get() = launched

    /**
     * Still-image session with recognition + attribute + quality + mask models enabled, so a
     * single Session can both detect+extract features and run the attribute pipeline.
     * Uses [InspireFace.DETECT_MODE_ALWAYS_DETECT] for deterministic per-image results.
     */
    fun createRecognitionSession(
        maxFaces: Int = IMAGE_MAX_FACES,
        inputPx: Int = DEFAULT_INPUT_PX,
        minFacePx: Int = 0,
    ): Session? = synchronized(lock) {
        if (!launched) return null
        val parameter: CustomParameter = InspireFace.CreateCustomParameter()
            .enableRecognition(true)
            .enableMaskDetect(true)
            .enableFaceQuality(true)
            .enableFaceAttribute(true)
            .enableInteractionLiveness(true)
        val session = InspireFace.CreateSession(
            parameter, InspireFace.DETECT_MODE_ALWAYS_DETECT,
            maxFaces.coerceAtLeast(1), inputPx, -1
        ) ?: return null
        activeSessions++
        InspireFace.SetTrackPreviewSize(session, if (inputPx > 0) inputPx else DEFAULT_INPUT_PX)
        InspireFace.SetFaceDetectThreshold(session, 0.5f)
        InspireFace.SetFilterMinimumFacePixelSize(session, minFacePx.coerceAtLeast(0))
        session
    }

    fun releaseSession(session: Session?) = synchronized(lock) {
        if (session == null) return@synchronized
        InspireFace.ReleaseSession(session)
        if (activeSessions > 0) activeSessions--
    }
}
