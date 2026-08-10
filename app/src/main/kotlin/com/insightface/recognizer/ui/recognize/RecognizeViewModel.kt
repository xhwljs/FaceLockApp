package com.insightface.recognizer.ui.recognize

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.insightface.recognizer.App
import com.insightface.recognizer.data.FaceAnalyzer
import com.insightface.recognizer.data.FaceManager
import com.insightface.recognizer.data.FaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecognizeViewModel : ViewModel() {

    private val faceManager: FaceManager = App.get().faceManager

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data class Ready(val bitmap: Bitmap, val result: FaceAnalyzer.Result) : UiState
        data class Error(val message: String) : UiState
    }

    sealed interface RegisterState {
        data object Idle : RegisterState
        data object Registering : RegisterState
        data class Success(val name: String) : RegisterState
        data class Failed(val message: String) : RegisterState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    fun analyze(uri: Uri) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            if (!faceManager.isReady) {
                _state.value = UiState.Error("InspireFace 引擎未就绪，请稍候")
                return@launch
            }
            val bitmap = faceManager.decodeBitmap(uri)
            if (bitmap == null) {
                _state.value = UiState.Error("无法读取图片")
                return@launch
            }
            val result = faceManager.analyze(bitmap)
            _state.value = UiState.Ready(bitmap, result)
        }
    }

    /** Registers [face]'s feature + crop into the local FeatureHub under [name]. */
    fun registerFace(face: FaceAnalyzer.Face, name: String) {
        val feature = face.feature ?: run {
            _registerState.value = RegisterState.Failed("未提取到人脸特征，无法注册")
            return
        }
        val crop = face.crop ?: run {
            _registerState.value = RegisterState.Failed("未获取到人脸裁剪图，无法注册")
            return
        }
        _registerState.value = RegisterState.Registering
        viewModelScope.launch {
            val res: FaceRepository.InsertResult = faceManager.register(name, feature, crop)
            if (res.success) {
                // Re-run analysis so recognition reflects the newly registered face.
                val current = _state.value
                if (current is UiState.Ready) {
                    val refreshed = faceManager.analyze(current.bitmap)
                    _state.value = UiState.Ready(current.bitmap, refreshed)
                }
                _registerState.value = RegisterState.Success(name)
            } else {
                _registerState.value = RegisterState.Failed(
                    "注册失败，请确保引擎已就绪且人脸特征已提取"
                )
            }
        }
    }

    fun clearRegisterState() {
        _registerState.value = RegisterState.Idle
    }

    fun reset() {
        val current = _state.value
        if (current is UiState.Ready) current.bitmap.recycle()
        _state.value = UiState.Idle
    }
}
