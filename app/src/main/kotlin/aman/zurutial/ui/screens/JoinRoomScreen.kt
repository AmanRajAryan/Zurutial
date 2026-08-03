package aman.zurutial.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aman.zurutial.media.FileFingerprint
import aman.zurutial.ui.theme.ExtraShapes
import aman.zurutial.ui.viewmodel.RoomUiState
import aman.zurutial.ui.viewmodel.RoomViewModel

/**
 * Home-tab join flow: room-code entry with a large rounded input. When the
 * viewModel resolves the room and needs a matching local file, this same
 * screen advances in place to the file-selection step.
 */
@Composable
fun JoinRoomScreen(
    viewModel: RoomViewModel,
    initialRoomCode: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var roomCodeInput by remember { mutableStateOf(initialRoomCode) }
    var pickedFileName by remember { mutableStateOf<String?>(null) }
    var pickedFileUri by remember { mutableStateOf<Uri?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (initialRoomCode.isNotBlank()) {
            viewModel.verifyRoomForJoin(initialRoomCode)
        }
    }

    BackHandler(enabled = uiState !is RoomUiState.Connecting) {
        if (uiState is RoomUiState.JoinRoomFileSelection) viewModel.resetState()
        onBack()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val picked = FileFingerprint.inspect(context, uri)
        if (picked == null) {
            errorText = "Couldn't read that file — try a different one"
            return@rememberLauncherForActivityResult
        }
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
            IconButton(onClick = {
                if (uiState is RoomUiState.JoinRoomFileSelection) viewModel.resetState()
                onBack()
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Join Room", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            val fileSelectState = uiState as? RoomUiState.JoinRoomFileSelection

            if (fileSelectState == null) {
                Text(
                    "Enter the room code shared with you",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = roomCodeInput,
                    onValueChange = { roomCodeInput = it.uppercase() },
                    label = { Text("Room Code") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 28.sp),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val room = fileSelectState.room
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Now playing in this room", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(room.fileName, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    onClick = { filePickerLauncher.launch(arrayOf("video/*")) },
                    shape = MaterialTheme.shapes.large,
                    color = if (pickedFileUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                        Text(pickedFileName ?: "Pick the matching video file", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            errorText?.let {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState is RoomUiState.Connecting) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                    Text("Connecting...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            val fileSelectState = uiState as? RoomUiState.JoinRoomFileSelection
            if (fileSelectState == null) {
                Button(
                    onClick = { viewModel.verifyRoomForJoin(roomCodeInput.trim()) },
                    enabled = roomCodeInput.isNotBlank() && uiState !is RoomUiState.Connecting,
                    shape = ExtraShapes.pill,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Find Room", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Button(
                    onClick = { viewModel.joinRoom(fileSelectState.room) },
                    enabled = pickedFileUri != null && uiState !is RoomUiState.Connecting,
                    shape = ExtraShapes.pill,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enter Room", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
