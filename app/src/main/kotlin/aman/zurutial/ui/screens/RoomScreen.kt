package aman.zurutial.ui.screens

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Divider
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
        Box(modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false // we're building custom controls below
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {

            val roomCode = (uiState as? RoomUiState.InRoom)?.room?.roomCode ?: ""
            if (roomCode.isNotEmpty()) {
                Text(text = "Room Code: $roomCode", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            }

            val activeCount = members.count {
                System.currentTimeMillis() - it.lastSeen < 30_000
            }
            Text(text = "$activeCount watching")

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Server: ${serverPing}ms", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                if (e2ePing.isNotEmpty()) {
                    Text(text = "Peer: ${e2ePing.values.maxOrNull() ?: 0}ms", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
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
            RoomCreatorControls(uiState = uiState, viewModel = viewModel)

            Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun RoomCreatorControls(uiState: RoomUiState, viewModel: RoomViewModel) {
    val room = (uiState as? RoomUiState.InRoom)?.room ?: return
    val isCreator = room.roomCreatorId == viewModel.getDeviceId()
    if (!isCreator) return

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
