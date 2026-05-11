package com.example.streamcontrol.features.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.streamcontrol.core.storage.CsvFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FilesViewModel(
    private val csvFileManager: CsvFileManager
) : ViewModel() {

    private val _state = MutableStateFlow(FilesState())
    val state: StateFlow<FilesState> = _state.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val files = csvFileManager.getExistingFiles().map { fileInfo ->
                    SavedFile(
                        name = fileInfo.name,
                        path = fileInfo.path,
                        createdAt = fileInfo.lastModified.time
                    )
                }
                _state.update { it.copy(files = files, isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar archivos: ${e.message}"
                    )
                }
            }
        }
    }

    fun showDeleteConfirmation(file: SavedFile) {
        _state.update { it.copy(showDeleteConfirmation = file) }
    }

    fun hideDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = null) }
    }

    fun confirmDelete() {
        val fileToDelete = _state.value.showDeleteConfirmation ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = csvFileManager.deleteFile(fileToDelete.path)
            result.fold(
                onSuccess = {
                    _state.update { currentState ->
                        currentState.copy(
                            files = currentState.files.filter { it.path != fileToDelete.path },
                            showDeleteConfirmation = null,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            errorMessage = "Error al eliminar: ${error.message}",
                            showDeleteConfirmation = null,
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    fun shareFile(file: SavedFile) {
        _state.update { it.copy(fileToShare = file) }
    }

    fun clearFileToShare() {
        _state.update { it.copy(fileToShare = null) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val csvFileManager: CsvFileManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FilesViewModel::class.java)) {
                return FilesViewModel(csvFileManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
