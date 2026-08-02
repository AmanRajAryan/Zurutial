package aman.zurutial.ui.screens

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Divider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import aman.zurutial.data.model.Member
import aman.zurutial.ui.viewmodel.RoomUiState
import aman.zurutial.ui.viewmodel.RoomViewModel
import kotlinx.coroutines.delay

@Composable
fun RoomScreen(
    viewModel: RoomViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val canControl by viewModel.canControlPlayback.collectAsState()
    val members by viewModel.members.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val serverPing by viewModel.serverPing.collectAsState()
    val e2ePing by viewModel.e2ePing.collectAsState()
    val player = viewModel.player

    val lifecycleOwner = LocalLifecycleOwner.current
    var isMuted by remember { mutableStateOf(false) }
    var sliderPositionMs by remember { mutableFloatStateOf(0f) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var isPlayingUi by remember { mutableStateOf(false) }
    var durationMs by remember { mutableFloatStateOf(1f) }
    var showAdvanced by remember { mutableStateOf(false) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }

    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val rawRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    // Cap it to 0.8 minimum so extreme vertical videos (9:16) don't 
                    // become infinitely tall and push all UI controls off the screen.
                    videoAspectRatio = rawRatio.coerceIn(0.8f, 3.0f)
                }
            }
        }
        player?.addListener(listener)
        // Also check if size is already known
        player?.videoSize?.let {
            if (it.width > 0 && it.height > 0) {
                videoAspectRatio = (it.width.toFloat() / it.height.toFloat()).coerceIn(0.8f, 3.0f)
            }
        }
        onDispose {
            player?.removeListener(listener)
        }
    }

    // Poll player position periodically to drive the slider (ExoPlayer has no built-in position Flow)
    LaunchedEffect(player) {
        while (true) {
            player?.let {
                if (!isDraggingSlider) {
                    sliderPositionMs = it.currentPosition.toFloat()
                }
                durationMs = if (it.duration > 0) it.duration.toFloat() else 1f
                isPlayingUi = it.playWhenReady
            }
            delay(500)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // Don't pause playback on backgrounding — sync must keep running for others.
                // Only mute locally if you want to save battery; leaving as-is for correctness.
            } else if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.forceResync()
                viewModel.forceHeartbeat()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspectRatio)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { context ->
                    androidx.media3.ui.PlayerView(context).apply {
                        useController = false // we're building custom controls below
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        this.player = viewModel.player
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {

            val roomCode = (uiState as? RoomUiState.InRoom)?.room?.roomCode ?: ""
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            if (roomCode.isNotEmpty()) {
                Text(
                    text = "Room Code: $roomCode (Tap to copy)", 
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(roomCode))
                        android.widget.Toast.makeText(context, "Room code copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    }.padding(vertical = 4.dp)
                )
            }

            val activeMembers = members.filter { System.currentTimeMillis() - it.lastSeen < 30_000 }
            val names = activeMembers.joinToString(", ") { it.displayName }
            Text(text = "Watching: $names", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sliderPositionMs,
                onValueChange = {
                    isDraggingSlider = true
                    sliderPositionMs = it
                },
                onValueChangeFinished = {
                    if (canControl) {
                        viewModel.onSeek(sliderPositionMs.toLong())
                    }
                    isDraggingSlider = false
                },
                valueRange = 0f..durationMs,
                enabled = canControl,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canControl) {
                    Button(onClick = { viewModel.onPlayPause() }) {
                        Text(text = if (isPlayingUi) "Pause" else "Play")
                    }
                } else {
                    Button(onClick = {
                        viewModel.toggleLocalMute()
                        isMuted = !isMuted
                    }) {
                        Text(text = if (isMuted) "Unmute" else "Mute")
                    }
                }
            }

            if (!canControl) {
                Text(text = "Only the room creator can control playback right now")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            RoomCreatorControls(
                uiState = uiState, 
                viewModel = viewModel, 
                isActingHost = viewModel.isActingHost()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.leaveRoom() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            ) {
                Text("Leave Room")
            }

            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (showAdvanced) "Hide Advanced Info" else "Show Advanced Info")
            }

            if (showAdvanced) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "Server: ${serverPing}ms", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    if (e2ePing.isNotEmpty()) {
                        Text(text = "Peer: ${e2ePing.values.maxOrNull() ?: 0}ms", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Text(
                    "Debug Logs:",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
    }
}

@Composable
private fun RoomCreatorControls(uiState: RoomUiState, viewModel: RoomViewModel, isActingHost: Boolean) {
    val room = (uiState as? RoomUiState.InRoom)?.room ?: return
    if (!isActingHost) return

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Allow everyone to control playback")
        Switch(
            checked = room.canMembersControlPlayback,
            onCheckedChange = { viewModel.setMemberControlAllowed(it) }
        )
    }
}
