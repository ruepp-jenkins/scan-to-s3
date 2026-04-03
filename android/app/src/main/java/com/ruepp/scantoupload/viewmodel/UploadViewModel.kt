package com.ruepp.scantoupload.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruepp.scantoupload.data.api.ApiClient
import com.ruepp.scantoupload.data.api.ApiException
import com.ruepp.scantoupload.data.preferences.ServerConfig
import com.ruepp.scantoupload.data.preferences.TokenManager
import com.ruepp.scantoupload.util.ProgressRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FileUploadStatus {
    PENDING, UPLOADING, SUCCESS, ERROR
}

data class FileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val status: FileUploadStatus = FileUploadStatus.PENDING,
    val progress: Int = 0,
    val errorMessage: String? = null
)

data class UploadUiState(
    val files: List<FileItem> = emptyList(),
    val isUploading: Boolean = false,
    val sessionExpired: Boolean = false
)

class UploadViewModel(
    private val tokenManager: TokenManager,
    private val serverConfig: ServerConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private val apiClient = ApiClient(serverConfig, tokenManager)

    fun addFiles(uris: List<Uri>, contentResolver: ContentResolver) {
        val newFiles = uris.mapNotNull { uri ->
            val existing = _uiState.value.files.any { it.uri == uri }
            if (existing) return@mapNotNull null

            val name = getFileName(uri, contentResolver) ?: "document.pdf"
            val size = getFileSize(uri, contentResolver)
            FileItem(uri = uri, name = name, size = size)
        }
        _uiState.value = _uiState.value.copy(
            files = _uiState.value.files + newFiles
        )
    }

    fun removeFile(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            files = _uiState.value.files.filter { it.uri != uri }
        )
    }

    fun clearCompleted() {
        _uiState.value = _uiState.value.copy(
            files = _uiState.value.files.filter { it.status != FileUploadStatus.SUCCESS }
        )
    }

    fun uploadAll(contentResolver: ContentResolver) {
        if (_uiState.value.isUploading) return

        _uiState.value = _uiState.value.copy(isUploading = true)

        viewModelScope.launch(Dispatchers.IO) {
            val files = _uiState.value.files.toList()

            for (i in files.indices) {
                val file = files[i]
                if (file.status == FileUploadStatus.SUCCESS) continue

                updateFileStatus(file.uri, FileUploadStatus.UPLOADING, 0)

                val result = uploadSingleFile(file, contentResolver)

                result.fold(
                    onSuccess = {
                        updateFileStatus(file.uri, FileUploadStatus.SUCCESS, 100)
                    },
                    onFailure = { error ->
                        if (error is ApiException && error.isUnauthorized) {
                            updateFileStatus(file.uri, FileUploadStatus.ERROR, 0, "Session expired")
                            _uiState.value = _uiState.value.copy(
                                isUploading = false,
                                sessionExpired = true
                            )
                            return@launch
                        }
                        updateFileStatus(
                            file.uri,
                            FileUploadStatus.ERROR,
                            0,
                            error.message ?: "Upload failed"
                        )
                    }
                )
            }

            _uiState.value = _uiState.value.copy(isUploading = false)
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }

    private fun uploadSingleFile(
        file: FileItem,
        contentResolver: ContentResolver
    ): Result<Unit> {
        // Step 1: Get presigned URL
        val presignedResult = apiClient.getPresignedUrl(file.name)
        val presigned = presignedResult.getOrElse { return Result.failure(it) }

        // Step 2: Upload to S3
        val inputStream = contentResolver.openInputStream(file.uri)
            ?: return Result.failure(Exception("Cannot read file"))

        val requestBody = ProgressRequestBody(
            inputStream = inputStream,
            contentLength = file.size,
            onProgress = { progress ->
                updateFileStatus(file.uri, FileUploadStatus.UPLOADING, progress)
            }
        )

        return apiClient.uploadToS3(presigned.uploadUrl, presigned.headers, requestBody)
    }

    private fun updateFileStatus(
        uri: Uri,
        status: FileUploadStatus,
        progress: Int,
        error: String? = null
    ) {
        _uiState.value = _uiState.value.copy(
            files = _uiState.value.files.map { file ->
                if (file.uri == uri) {
                    file.copy(status = status, progress = progress, errorMessage = error)
                } else {
                    file
                }
            }
        )
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment
    }

    private fun getFileSize(uri: Uri, contentResolver: ContentResolver): Long {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getLong(sizeIndex)
            }
        }
        return -1L
    }
}
