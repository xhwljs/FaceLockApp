package com.insightface.recognizer.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.insightface.recognizer.App
import com.insightface.recognizer.data.FaceManager
import com.insightface.recognizer.data.FaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ManageViewModel : ViewModel() {

    private val faceManager: FaceManager = App.get().faceManager

    private val _records = MutableStateFlow<List<FaceRepository.FaceRecord>>(emptyList())
    val records: StateFlow<List<FaceRepository.FaceRecord>> = _records.asStateFlow()

    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword.asStateFlow()

    init { refresh() }

    fun setKeyword(value: String) {
        _keyword.value = value
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _records.value = faceManager.listFaces(_keyword.value.ifBlank { null })
        }
    }

    fun rename(id: Long, newName: String) {
        viewModelScope.launch {
            faceManager.rename(id, newName)
            refresh()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            faceManager.delete(id)
            refresh()
        }
    }
}
