package aman.zurutial.ui.screens

import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Divider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import aman.zurutial.data.SettingsManager
import aman.zurutial.data.model.Member
import aman.zurutial.ui.components.ConfettiBurst
import aman.zurutial.ui.components.ConnectedUsersRow
import aman.zurutial.ui.components.CopyFeedbackIconButton
import aman.zurutial.ui.components.DebugLogsCollapsedPill
import aman.zurutial.ui.components.DebugLogsSheet
import aman.zurutial.ui.components.EmptyState
import aman.zurutial.ui.components.LiftCard
import aman.zurutial.ui.components.MemberDisplay
import aman.zurutial.ui.components.MemberPresence
import aman.zurutial.ui.components.SyncLevel
import aman.zurutial.ui.components.SyncStatus
import aman.zurutial.ui.components.SyncStatusChip
import aman.zurutial.ui.player.FullScreenPlayerScreen
import aman.zurutial.ui.theme.ExtraShapes
import aman.zurutial.ui.viewmodel.RoomUiState
import aman.zurutial.ui.viewmodel.RoomViewModel
import kotlinx.coroutines.delay

@Composable
fun RoomScreen(
    viewModel: RoomViewModel,
    onExit: () -> Unit,
    isInPip: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val canControl by viewModel.canControlPlayback.collectAsState()
    val members by viewModel.members.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val audioIssue by viewModel.audioIssue.collectAsState()
    val serverPing by viewModel.serverPing.collectAsState()
    val e2ePing by viewModel.e2ePing.collectAsState()
    val player = viewModel.player

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isMuted by remember { mutableStateOf(false) }
    var localVolume by remember { mutableFloatStateOf(1f) }
    var sliderPositionMs by remember { mutableFloatStateOf(0f) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var isPlayingUi by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var durationMs by remember { mutableFloatStateOf(1f) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var recentActionAtMs by remember { mutableStateOf(0L) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var debugSheetOpen by remember { mutableStateOf(false) }
    var fullscreenOpen by remember { mutableStateOf(false) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var hasCelebrated by remember { mutableStateOf(false) }
    val debugLogsEnabled = remember { SettingsManager.getDebugLogsEnabled(context) }

    LaunchedEffect(Unit) {
        if (!hasCelebrated) {
            hasCelebrated = true
            confettiTrigger++
        }
    }

    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val rawRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    videoAspectRatio = rawRatio.coerceIn(0.8f, 3.0f)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING
            }
        }
        player?.addListener(listener)
        player?.videoSize?.let {
            if (it.width > 0 && it.height > 0) {
                videoAspectRatio = (it.width.toFloat() / it.height.toFloat()).coerceIn(0.8f, 3.0f)
            }
        }
        onDispose { player?.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            player?.let {
                if (!isDraggingSlider) sliderPositionMs = it.currentPosition.toFloat()
                durationMs = if (it.duration > 0) it.duration.toFloat() else 1f
                isPlayingUi = it.playWhenReady
            }
            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.forceResync()
                viewModel.forceHeartbeat()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val room = (uiState as? RoomUiState.InRoom)?.room
    val playbackSpeed by viewModel.targetPlaybackSpeed.collectAsState()
    val nowServer = viewModel.clockSync?.toServerTime(System.currentTimeMillis()) ?: System.currentTimeMillis()
    val activeMembers = members.filter { nowServer - it.lastSeen < 30_000 }
    val isActingHost = viewModel.isActingHost()

    val activePeerIds = activeMembers.map { it.deviceId }
    val activePings = e2ePing.filterKeys { it in activePeerIds }
    val maxPing = if (activeMembers.size <= 1) serverPing else (activePings.values.maxOrNull() ?: serverPing)
    val syncStatus = when {
        room != null && activeMembers.none { it.deviceId == room.roomCreatorId } && !isActingHost && activeMembers.size <= 1 ->
            SyncStatus(SyncLevel.HOST_DISCONNECTED, "Host disconnected")
        isBuffering -> SyncStatus(SyncLevel.RECOVERING, "Recovering...")
        maxPing > 400 -> SyncStatus(SyncLevel.HIGH_LATENCY, "High latency")
        System.currentTimeMillis() - recentActionAtMs < 1500 -> SyncStatus(SyncLevel.SYNCING, "Syncing...")
        else -> SyncStatus(SyncLevel.PERFECT, "Perfect Sync")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoAspectRatio)
                    .background(Color.Black)
                    .clip(ExtraShapes.thumbnail)
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS)
                        }
                    },
                    // A single ExoPlayer can only have one attached video surface at a
                    // time. While the full-screen player owns it, this inline surface
                    // releases its reference instead of fighting over it; it reclaims
                    // the player automatically the moment fullscreenOpen flips back.
                    update = { it.player = if (fullscreenOpen) null else player },
                    modifier = Modifier.fillMaxSize()
                )

                if (!isPlayingUi && !isBuffering) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.leaveRoom(); onExit() },
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Leave room", tint = Color.White)
                }

                SyncStatusChip(
                    status = syncStatus,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                )

                IconButton(
                    onClick = { fullscreenOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "Full screen", tint = Color.White)
                }

                ConfettiBurst(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())
            }

            Column(modifier = Modifier.padding(20.dp)) {
                // ---- Room card ----
                LiftCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "ROOM CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    room?.roomCode ?: "",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row {
                                CopyFeedbackIconButton(onCopy = {
                                    clipboardManager.setText(AnnotatedString(room?.roomCode ?: ""))
                                })
                                IconButton(onClick = {
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            android.content.Intent.EXTRA_TEXT,
                                            "Join my Zurutial watch party! Room code: ${room?.roomCode}"
                                        )
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share room code"))
                                }) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (activeMembers.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.Share,
                                title = "No users",
                                description = "Invite someone using your room code."
                            )
                        } else {
                            ConnectedUsersRow(
                                members = activeMembers.map { member ->
                                    MemberDisplay(
                                        deviceId = member.deviceId,
                                        displayName = member.displayName,
                                        isHost = member.deviceId == viewModel.getActingHostId(),
                                        presence = if (member.deviceId == viewModel.getDeviceId() && !isPlayingUi) MemberPresence.PAUSED
                                            else if (System.currentTimeMillis() - member.lastSeen > 15_000) MemberPresence.AWAY
                                            else MemberPresence.SYNCED
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val currentHostId = viewModel.getActingHostId()
                        val peerPingDisplay = when {
                            activeMembers.size <= 1 -> "--"
                            activePings.isEmpty() -> "--"
                            isActingHost -> "${activePings.values.maxOrNull() ?: 0}ms"
                            currentHostId != null && activePings.containsKey(currentHostId) -> "${activePings[currentHostId]}ms"
                            else -> "${activePings.values.maxOrNull() ?: 0}ms"
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Server: ${serverPing}ms",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Peer: $peerPingDisplay",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Link: ${if (maxPing < 150) "Excellent" else if (maxPing < 400) "Good" else "Poor"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Playback card ----
                LiftCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Slider(
                            value = sliderPositionMs.coerceIn(0f, durationMs),
                            onValueChange = {
                                isDraggingSlider = true
                                sliderPositionMs = it
                            },
                            onValueChangeFinished = {
                                if (canControl) {
                                    viewModel.onSeek(sliderPositionMs.toLong())
                                    recentActionAtMs = System.currentTimeMillis()
                                }
                                isDraggingSlider = false
                            },
                            valueRange = 0f..durationMs,
                            enabled = canControl,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(sliderPositionMs.toLong()), style = MaterialTheme.typography.labelMedium)
                            Text(
                                "-${formatTime((durationMs - sliderPositionMs).toLong().coerceAtLeast(0))}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canControl) {
                                IconButton(onClick = {
                                    viewModel.onSeek((sliderPositionMs - 10_000).toLong().coerceAtLeast(0))
                                    recentActionAtMs = System.currentTimeMillis()
                                }) {
                                    Icon(Icons.Filled.Replay10, contentDescription = "Back 10 seconds", modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                            }

                            FloatingActionButton(
                                onClick = {
                                    if (canControl) {
                                        viewModel.onPlayPause()
                                        recentActionAtMs = System.currentTimeMillis()
                                    } else {
                                        viewModel.toggleLocalMute()
                                        isMuted = !isMuted
                                    }
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    if (!canControl) {
                                        if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp
                                    } else if (isPlayingUi) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (canControl) "Play/Pause" else "Mute",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            if (canControl) {
                                Spacer(modifier = Modifier.width(20.dp))
                                IconButton(onClick = {
                                    viewModel.onSeek((sliderPositionMs + 10_000).toLong())
                                    recentActionAtMs = System.currentTimeMillis()
                                }) {
                                    Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds", modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        if (!canControl) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Only the host can control playback right now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val exoPlayer = player
                                if (exoPlayer != null) {
                                    localVolume = if (localVolume > 0f) 0f else 1f
                                    exoPlayer.volume = localVolume
                                }
                            }) {
                                Icon(if (localVolume > 0f) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, contentDescription = "Volume")
                            }
                            Slider(
                                value = localVolume,
                                onValueChange = {
                                    localVolume = it
                                    player?.volume = it
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Box {
                                TextButton(onClick = { speedMenuOpen = true }) {
                                    Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${playbackSpeed}x")
                                }
                                DropdownMenu(expanded = speedMenuOpen, onDismissRequest = { speedMenuOpen = false }) {
                                    listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text("${speed}x") },
                                            onClick = {
                                                viewModel.onSpeedChanged(speed)
                                                speedMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isActingHost && room != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LiftCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Allow everyone to control playback", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "When off, only you can play, pause, or seek",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = room.canMembersControlPlayback,
                                onCheckedChange = { viewModel.setMemberControlAllowed(it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.leaveRoom(); onExit() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Leave Room")
                }

                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        if (debugLogsEnabled) {
            DebugLogsCollapsedPill(
                onExpand = { debugSheetOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }

        // While the system has shrunk us into native Picture-in-Picture, cover
        // everything else with just the bare video surface — no controls, no chrome,
        // matching what Android's PiP API expects (no custom floating player).
        if (isInPip) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (debugSheetOpen) {
        DebugLogsSheet(
            logs = logs,
            onDismiss = { debugSheetOpen = false },
            onClear = { /* logs are append-only in the viewmodel by design */ }
        )
    }

    if (audioIssue != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissAudioIssue() },
            icon = { Icon(Icons.Filled.VolumeOff, contentDescription = null) },
            title = { Text("Audio format issue") },
            text = { Text(audioIssue ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAudioIssue() }) { Text("Got it") }
            }
        )
    }

    if (fullscreenOpen) {
        FullScreenPlayerScreen(
            viewModel = viewModel,
            fileName = room?.fileName ?: "",
            syncStatus = syncStatus,
            onExitFullscreen = { fullscreenOpen = false }
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}
