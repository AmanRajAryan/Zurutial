package aman.zurutial.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import aman.zurutial.media.FileFingerprint
import aman.zurutial.ui.viewmodel.RoomUiState
import aman.zurutial.ui.viewmodel.RoomViewModel

enum class HomeMode {
    INITIAL, CREATE, JOIN
}

@Composable
fun HomeScreen(
    viewModel: RoomViewModel = viewModel(),
    onEnteredRoom: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var mode by remember { mutableStateOf(HomeMode.INITIAL) }
    var pickedFileName by remember { mutableStateOf<String?>(null) }
    var pickedFileUri by remember { mutableStateOf<Uri?>(null) }
    var roomCodeInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

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
        if (uiState is RoomUiState.InRoom) onEnteredRoom()
        if (uiState is RoomUiState.Error) errorText = (uiState as RoomUiState.Error).message
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Watch Together", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            when (mode) {
                HomeMode.INITIAL -> {
                    Button(
                        onClick = { mode = HomeMode.CREATE },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Room")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { mode = HomeMode.JOIN },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Join Room")
                    }
                }
                HomeMode.CREATE -> {
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("video/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = pickedFileName ?: "Pick a video file")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.createRoom() },
                        enabled = pickedFileUri != null && uiState !is RoomUiState.Connecting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Create Room")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.TextButton(
                        onClick = { mode = HomeMode.INITIAL },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back")
                    }
                }
                HomeMode.JOIN -> {
                    if (uiState is RoomUiState.JoinRoomFileSelection) {
                        val roomToJoin = (uiState as RoomUiState.JoinRoomFileSelection).room
                        Text(text = "Currently playing: ${roomToJoin.fileName}", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("video/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = pickedFileName ?: "Pick matching video file")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.joinRoom(roomToJoin) },
                            enabled = pickedFileUri != null && uiState !is RoomUiState.Connecting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Enter Room")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { 
                                mode = HomeMode.INITIAL
                                viewModel.resetState() 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    } else {
                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.uppercase() },
                            label = { Text("Room Code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.verifyRoomForJoin(roomCodeInput.trim()) },
                            enabled = roomCodeInput.isNotBlank() && uiState !is RoomUiState.Connecting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Find Room")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { mode = HomeMode.INITIAL },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState is RoomUiState.Connecting) {
                CircularProgressIndicator()
            }

            errorText?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Text(
            "Setup Logs:",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(top = 8.dp)
        ) {
            SelectionContainer {
                Text(
                    text = logs.joinToString("\n"),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }
}
