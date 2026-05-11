package com.example.streamcontrol.features.files

data class FilesState(
    val files: List<SavedFile> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteConfirmation: SavedFile? = null,
    val fileToShare: SavedFile? = null
)

data class SavedFile(
    val name: String,
    val path: String,
    val createdAt: Long
)
