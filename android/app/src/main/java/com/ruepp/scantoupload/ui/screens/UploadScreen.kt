package com.ruepp.scantoupload.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ruepp.scantoupload.ui.theme.Green
import com.ruepp.scantoupload.ui.theme.Red
import com.ruepp.scantoupload.viewmodel.FileUploadStatus
import com.ruepp.scantoupload.viewmodel.UploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    sharedUris: List<Uri>,
    onDisconnect: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    // Handle shared URIs on first composition
    LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) {
            viewModel.addFiles(sharedUris, contentResolver)
            viewModel.uploadAll(contentResolver)
        }
    }

    // Handle invalid token
    LaunchedEffect(uiState.tokenInvalid) {
        if (uiState.tokenInvalid) {
            onDisconnect()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // Take persistable permissions for picked files
            for (uri in uris) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { }
            }
            viewModel.addFiles(uris, contentResolver)
        }
    }

    val hasFiles = uiState.files.isNotEmpty()
    val hasPending = uiState.files.any {
        it.status == FileUploadStatus.PENDING || it.status == FileUploadStatus.ERROR
    }
    val hasCompleted = uiState.files.any { it.status == FileUploadStatus.SUCCESS }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan to Upload") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.disconnect()
                            onDisconnect()
                        },
                        enabled = !uiState.isUploading
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Disconnect")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { filePicker.launch(arrayOf("application/pdf")) },
                    enabled = !uiState.isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select PDF")
                }

                if (hasFiles) {
                    Button(
                        onClick = { viewModel.uploadAll(contentResolver) },
                        enabled = !uiState.isUploading && hasPending,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Upload All")
                    }
                }
            }

            if (hasCompleted) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { viewModel.clearCompleted() },
                    enabled = !uiState.isUploading
                ) {
                    Text("Clear completed")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasFiles) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No files selected.\nTap \"Select PDF\" or share a PDF from another app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.files, key = { it.uri.toString() }) { file ->
                        FileItemCard(
                            file = file,
                            onRemove = { viewModel.removeFile(file.uri) },
                            canRemove = !uiState.isUploading
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemCard(
    file: com.ruepp.scantoupload.viewmodel.FileItem,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (file.size > 0) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (file.status) {
                    FileUploadStatus.PENDING -> {
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FileUploadStatus.UPLOADING -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = file.progress / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${file.progress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FileUploadStatus.SUCCESS -> {
                        Text(
                            text = "Uploaded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Green
                        )
                    }
                    FileUploadStatus.ERROR -> {
                        Text(
                            text = file.errorMessage ?: "Upload failed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Red,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (canRemove && file.status != FileUploadStatus.UPLOADING) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
