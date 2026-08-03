package aman.zurutial.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import aman.zurutial.media.FileFingerprint
import aman.zurutial.media.PickedFile
import aman.zurutial.ui.theme.ExtraShapes
import aman.zurutial.ui.viewmodel.RoomUiState
import aman.zurutial.ui.viewmodel.RoomViewModel

@Composable
fun CreateRoomScreen(
    viewModel: RoomViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var pickedFileName by remember { mutableStateOf<String?>(null) }
    var pickedFileUri by remember { mutableStateOf<Uri?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = uiState !is RoomUiState.Connecting) { onBack() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val picked = FileFingerprint.inspect(context, uri)
        if (picked == null) {
            errorText = "Couldn't read that file — try a different one"
            return@rememberLauncherForActivityResult
        }
        urlInput = ""
        pickedFileName = picked.fileName
        pickedFileUri = uri
        viewModel.setPickedFile(picked)
        errorText = null
    }

    LaunchedEffect(uiState) {
        if (uiState is RoomUiState.Error) errorText = (uiState as RoomUiState.Error).message
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Create Room", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Pick a video to host the watch party",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))

            PickerOptionCard(
                icon = Icons.Filled.FileUpload,
                title = pickedFileName ?: "Pick a local video file",
                subtitle = "Everyone in the room needs this same file on their device",
                selected = pickedFileUri != null,
                onClick = {
                    urlInput = ""
                    filePickerLauncher.launch(arrayOf("video/*"))
                }
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    "  OR STREAM  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    if (it.isNotBlank()) {
                        pickedFileName = null
                        pickedFileUri = null
                        viewModel.setPickedFile(
                            PickedFile(
                                uri = Uri.parse(it.trim()),
                                fileName = "Web Stream",
                                sizeBytes = 0L,
                                durationMs = 0L,
                                customFingerprint = it.trim()
                            )
                        )
                    }
                },
                label = { Text("Video URL") },
                placeholder = { Text("https://...") },
                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            )

            errorText?.let {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState is RoomUiState.Connecting) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        logs.firstOrNull()?.substringAfter(" ") ?: "Connecting...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Button(
                onClick = { viewModel.createRoom() },
                enabled = (pickedFileUri != null || urlInput.isNotBlank()) && uiState !is RoomUiState.Connecting,
                shape = ExtraShapes.pill,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Room", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun PickerOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
