package com.insightface.recognizer.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Single owner of the InspireFace native lifecycle and the [FaceRepository]. All SDK calls
 * run on one dedicated thread (the SDK requires session/stream access to be single-threaded),
 * exposed here as suspend functions that hop to that thread.
 *
 * Held by [com.insightface.recognizer.App] so every screen shares one engine + one face DB.
 */
class FaceManager(private val app: Context) {

    enum class State { NOT_LAUNCHED, LAUNCHING, READY, FAILED }

    private val _state = MutableStateFlow(State.NOT_LAUNCHED)
    val state: StateFlow<State> = _state.asStateFlow()

    // The single SDK owner thread. All InspireFace + FeatureHub calls happen here — the SDK
    // requires session/stream access to be single-threaded.
    private val sdkDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "inspireface-sdk").apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private val sdkScope = CoroutineScope(SupervisorJob() + sdkDispatcher)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var repository: FaceRepository? = null

    val isReady: Boolean get() = _state.value == State.READY

    /** Launches the SDK and opens the local FeatureHub. Safe to call repeatedly. */
    fun launch(model: String = FaceEngine.DEFAULT_MODEL) {
        if (_state.value == State.LAUNCHING || _state.value == State.READY) return
        _state.value = State.LAUNCHING
        sdkScope.launch {
            val ok = FaceEngine.ensureLaunched(app, model)
            if (ok) {
                val repo = FaceRepository(app, model)
                if (repo.open()) repository = repo
            }
            _state.value = if (ok) State.READY else State.FAILED
        }
    }

    /** Re-launches with a different model (terminates the previous one first). */
    fun switchModel(model: String) {
        sdkScope.launch {
            repository?.close()
            repository = null
            FaceEngine.ensureLaunched(app, model)
            val repo = FaceRepository(app, model)
            if (repo.open()) repository = repo
            _state.value = if (FaceEngine.isLaunched) State.READY else State.FAILED
        }
    }

    /** Runs the full still-image analysis pipeline. Must not be called before READY. */
    suspend fun analyze(bitmap: Bitmap): FaceAnalyzer.Result = withContext(sdkScope.coroutineContext) {
        val session = FaceEngine.createRecognitionSession()
        try {
            if (session == null) {
                FaceAnalyzer.Result(FaceAnalyzer.Status.PROCESS_FAILED, emptyList())
            } else {
                FaceAnalyzer.analyze(session, bitmap, repository)
            }
        } finally {
            FaceEngine.releaseSession(session)
        }
    }

    suspend fun listFaces(keyword: String? = null): List<FaceRepository.FaceRecord> =
        withContext(sdkScope.coroutineContext) {
            repository?.query(keyword) ?: emptyList()
        }

    suspend fun register(name: String, feature: com.insightface.sdk.inspireface.base.FaceFeature, crop: Bitmap) =
        withContext(sdkScope.coroutineContext) {
            repository?.insert(name, feature, crop) ?: FaceRepository.InsertResult(false, null)
        }

    suspend fun rename(id: Long, name: String) = withContext(sdkScope.coroutineContext) {
        repository?.update(id, name) ?: false
    }

    suspend fun delete(id: Long) = withContext(sdkScope.coroutineContext) {
        repository?.delete(id) ?: false
    }

    suspend fun decodeBitmap(uri: android.net.Uri, maxDimension: Int = 2048): Bitmap? =
        withContext(ioScope.coroutineContext) {
            runCatching { BitmapDecoder.decode(app, uri, maxDimension) }.getOrNull()
        }
}
