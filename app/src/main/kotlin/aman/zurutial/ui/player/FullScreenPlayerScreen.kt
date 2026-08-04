package aman.zurutial.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import aman.zurutial.ui.theme.ExtraShapes
import aman.zurutial.ui.viewmodel.RoomViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** How the video frame is scaled to fill the surface — mirrors PlayerView's own resize modes. */
enum class ScreenFitMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom")
}

private enum class TrackSheetType { AUDIO, SUBTITLE }

/**
 * A completely new full-screen player interface built around the existing
 * ExoPlayer instance from [RoomViewModel]. No playback, sync, or networking
 * logic lives here — this only reads player state and forwards user intent
 * to the same [RoomViewModel] methods the inline player already uses.
 */
@Composable
fun FullScreenPlayerScreen(
    viewModel: RoomViewModel,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val player = viewModel.player
    val canControl by viewModel.canControlPlayback.collectAsState()
    val playbackSpeed by viewModel.targetPlaybackSpeed.collectAsState()

    // ---- Playback state (polled locally, same pattern as the inline player) ----
    var positionMs by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableFloatStateOf(1f) }
    var bufferedMs by remember { mutableFloatStateOf(0f) }
    var isPlayingUi by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var isDraggingSeek by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            player?.let {
                if (!isDraggingSeek) positionMs = it.currentPosition.toFloat()
                durationMs = if (it.duration > 0) it.duration.toFloat() else 1f
                bufferedMs = it.bufferedPosition.toFloat()
                isPlayingUi = it.playWhenReady
            }
            delay(200)
        }
    }

    // ---- Immersive edge-to-edge while this screen is up ----
    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(enabled = true) { onExitFullscreen() }

    // ---- Controls visibility / auto-hide ----
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }

    LaunchedEffect(controlsVisible, isPlayingUi, locked, isDraggingSeek) {
        if (controlsVisible && isPlayingUi && !locked && !isDraggingSeek) {
            delay(4000)
            controlsVisible = false
        }
    }
    fun poke() { controlsVisible = true }

    // ---- Screen fit / rotation ----
    var fitMode by remember { mutableStateOf(ScreenFitMode.FIT) }
    var orientationLocked by remember { mutableStateOf(false) }

    // ---- Sheets ----
    var speedSheetOpen by remember { mutableStateOf(false) }
    var trackSheet by remember { mutableStateOf<TrackSheetType?>(null) }
    var adjustSheetOpen by remember { mutableStateOf(false) }

    // ---- Brightness / volume gestures ----
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeLevel by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    var brightnessLevel by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.5f
        )
    }
    var indicatorType by remember { mutableStateOf<GestureIndicator?>(null) }

    LaunchedEffect(indicatorType) {
        if (indicatorType != null) {
            delay(900)
            indicatorType = null
        }
    }

    fun applyBrightness(value: Float) {
        brightnessLevel = value.coerceIn(0.02f, 1f)
        activity?.window?.let { w ->
            val params: WindowManager.LayoutParams = w.attributes
            params.screenBrightness = brightnessLevel
            w.attributes = params
        }
        indicatorType = GestureIndicator.Brightness(brightnessLevel)
    }

    fun applyVolume(value: Float) {
        volumeLevel = value.coerceIn(0f, 1f)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volumeLevel * maxVolume).roundToInt(), 0)
        indicatorType = GestureIndicator.Volume(volumeLevel)
    }

    // ---- Seek helpers (routed through the same ViewModel as the inline player) ----
    fun seekTo(target: Float) {
        if (!canControl) return
        viewModel.onSeek(target.toLong().coerceIn(0, durationMs.toLong()))
    }
    fun seekRelative(deltaMs: Long) {
        if (!canControl) return
        val target = (positionMs + deltaMs).coerceIn(0f, durationMs)
        positionMs = target
        viewModel.onSeek(target.toLong())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ---- Video surface ----
        val resizeMode = when (fitMode) {
            ScreenFitMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            ScreenFitMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            ScreenFitMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    this.player = player
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            update = { it.resizeMode = resizeMode; it.player = player },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize()
        )

        // ---- Gesture zones (only active when unlocked) ----
        if (!locked) {
            Row(modifier = Modifier.fillMaxSize()) {
                GestureZone(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onSingleTap = { poke(); if (controlsVisible) controlsVisible = false },
                    onDoubleTap = { seekRelative(-10_000); indicatorType = GestureIndicator.Seek(false) },
                    onVerticalDrag = { delta -> applyBrightness(brightnessLevel - delta) }
                )
                GestureZone(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onSingleTap = { poke(); if (controlsVisible) controlsVisible = false },
                    onDoubleTap = { seekRelative(10_000); indicatorType = GestureIndicator.Seek(true) },
                    onVerticalDrag = { delta -> applyVolume(volumeLevel - delta) }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .tapModifier { poke() }
            )
        }

        // ---- Buffering spinner ----
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                color = Color.White
            )
        }

        // ---- Transient brightness/volume/seek indicator ----
        AnimatedVisibility(
            visible = indicatorType != null,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            indicatorType?.let { GestureIndicatorBubble(it) }
        }

        // ---- Lock affordance ----
        AnimatedVisibility(
            visible = locked,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.CenterStart).padding(24.dp)
        ) {
            GlassIconButton(icon = Icons.Filled.LockOpen, contentDescription = "Unlock controls") {
                locked = false
                poke()
            }
        }

        // ---- Top bar ----
        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { -it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopBar(
                onBack = onExitFullscreen,
                onLock = { locked = true },
                onFit = {
                    fitMode = when (fitMode) {
                        ScreenFitMode.FIT -> ScreenFitMode.FILL
                        ScreenFitMode.FILL -> ScreenFitMode.ZOOM
                        ScreenFitMode.ZOOM -> ScreenFitMode.FIT
                    }
                },
                fitMode = fitMode,
                onRotate = {
                    activity?.let { act ->
                        orientationLocked = !orientationLocked
                        act.requestedOrientation = if (orientationLocked) {
                            if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }
                },
                onAdjust = { adjustSheetOpen = true; poke() }
            )
        }

        // ---- Center transport controls ----
        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 48.dp else 32.dp)
            ) {
                GlassIconButton(
                    icon = Icons.Filled.Replay10,
                    contentDescription = "Back 10 seconds",
                    size = 56.dp,
                    iconSize = 28.dp,
                    enabled = canControl
                ) { seekRelative(-10_000); poke() }

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .then(
                            if (canControl) Modifier.tapModifier {
                                viewModel.onPlayPause(); poke()
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlayingUi) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlayingUi) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(42.dp)
                    )
                }

                GlassIconButton(
                    icon = Icons.Filled.Forward10,
                    contentDescription = "Forward 10 seconds",
                    size = 56.dp,
                    iconSize = 28.dp,
                    enabled = canControl
                ) { seekRelative(10_000); poke() }
            }
        }

        // ---- Bottom bar ----
        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomBar(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                enabled = canControl,
                playbackSpeed = playbackSpeed,
                onScrubStart = { isDraggingSeek = true; poke() },
                onScrub = { positionMs = it },
                onScrubEnd = { seekTo(positionMs); isDraggingSeek = false },
                onSpeedClick = { speedSheetOpen = true; poke() },
                onSubtitleClick = { trackSheet = TrackSheetType.SUBTITLE; poke() },
                onAudioTrackClick = { trackSheet = TrackSheetType.AUDIO; poke() },
                isLandscape = isLandscape
            )
        }

        if (!canControl) {
            AnimatedVisibility(
                visible = controlsVisible && !locked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (isLandscape) 96.dp else 118.dp)
            ) {
                Text(
                    "Only the host can control playback",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    if (speedSheetOpen) {
        SpeedSheet(
            current = playbackSpeed,
            onSelect = { viewModel.onSpeedChanged(it); speedSheetOpen = false },
            onDismiss = { speedSheetOpen = false }
        )
    }

    trackSheet?.let { type ->
        TrackSelectionSheet(
            type = type,
            player = player,
            onDismiss = { trackSheet = null }
        )
    }

    if (adjustSheetOpen) {
        PictureAdjustmentsSheet(
            brightness = brightnessLevel,
            onBrightnessChange = { applyBrightness(it) },
            onDismiss = { adjustSheetOpen = false }
        )
    }
}

// ============================= Sub-components =============================

private sealed class GestureIndicator {
    data class Brightness(val level: Float) : GestureIndicator()
    data class Volume(val level: Float) : GestureIndicator()
    data class Seek(val forward: Boolean) : GestureIndicator()
}

@Composable
private fun GestureIndicatorBubble(indicator: GestureIndicator) {
    val (icon, text) = when (indicator) {
        is GestureIndicator.Brightness -> Icons.Filled.BrightnessMedium to "${(indicator.level * 100).roundToInt()}%"
        is GestureIndicator.Volume -> (if (indicator.level <= 0f) Icons.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp) to "${(indicator.level * 100).roundToInt()}%"
        is GestureIndicator.Seek -> (if (indicator.forward) Icons.Filled.Forward10 else Icons.Filled.Replay10) to (if (indicator.forward) "+10s" else "-10s")
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun GestureZone(
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onVerticalDrag: (deltaFraction: Float) -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onSingleTap() }, onDoubleTap = { onDoubleTap() })
            }
            .pointerInput(Unit) {
                var heightPx = size.height.toFloat().coerceAtLeast(1f)
                detectVerticalDragGestures(
                    onDragStart = { heightPx = size.height.toFloat().coerceAtLeast(1f) },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        onVerticalDrag(dragAmount / heightPx)
                    }
                )
            }
    )
}

/** Shared single-tap modifier used by every glass button / row in this screen. */
private fun Modifier.tapModifier(onTap: () -> Unit): Modifier =
    this.pointerInput(onTap) {
        detectTapGestures(onTap = { onTap() })
    }

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (enabled) 0.38f else 0.2f))
            .then(if (enabled) Modifier.tapModifier(onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    onLock: () -> Unit,
    onFit: () -> Unit,
    fitMode: ScreenFitMode,
    onRotate: () -> Unit,
    onAdjust: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Exit full screen", onClick = onBack)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassIconButton(icon = Icons.Filled.Lock, contentDescription = "Lock controls", onClick = onLock)
            GlassIconButton(icon = Icons.Filled.AspectRatio, contentDescription = "Screen fit: ${fitMode.label}", onClick = onFit)
            GlassIconButton(icon = Icons.Filled.ScreenRotation, contentDescription = "Rotate", onClick = onRotate)
            GlassIconButton(icon = Icons.Filled.Tune, contentDescription = "Picture adjustments", onClick = onAdjust)
        }
    }
}

@Composable
private fun BottomBar(
    positionMs: Float,
    durationMs: Float,
    bufferedMs: Float,
    enabled: Boolean,
    playbackSpeed: Float,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onAudioTrackClick: () -> Unit,
    isLandscape: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column {
                ExpressiveSeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    bufferedMs = bufferedMs,
                    enabled = enabled,
                    onScrubStart = onScrubStart,
                    onScrub = onScrub,
                    onScrubEnd = onScrubEnd
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatTime(positionMs.toLong()), color = Color.White, style = MaterialTheme.typography.labelMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 18.dp else 10.dp)) {
                        GlassTextButton(text = "${formatSpeed(playbackSpeed)}x", icon = Icons.Filled.Speed, onClick = onSpeedClick)
                        GlassIconButton(icon = Icons.Filled.ClosedCaption, contentDescription = "Subtitles", size = 38.dp, iconSize = 20.dp, onClick = onSubtitleClick)
                        GlassIconButton(icon = Icons.Filled.Audiotrack, contentDescription = "Audio track", size = 38.dp, iconSize = 20.dp, onClick = onAudioTrackClick)
                    }

                    Text(
                        "-${formatTime((durationMs - positionMs).toLong().coerceAtLeast(0))}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTextButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
            .tapModifier(onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

/**
 * A custom seek bar (Material3's Slider doesn't expose a buffered-range
 * track) showing played / buffered / remaining state, with a live time
 * bubble while scrubbing. The thumb tracks progress via a fractional-width
 * inner box rather than manual pixel math, so it can't drift out of sync.
 */
@Composable
private fun ExpressiveSeekBar(
    positionMs: Float,
    durationMs: Float,
    bufferedMs: Float,
    enabled: Boolean,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit
) {
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(positionMs) }

    val displayValue = if (isScrubbing) scrubValue else positionMs
    val safeDuration = durationMs.coerceAtLeast(1f)
    val playedFraction = (displayValue / safeDuration).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedMs / safeDuration).coerceIn(0f, 1f)

    Column {
        AnimatedVisibility(visible = isScrubbing, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(formatTime(scrubValue.toLong()), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                isScrubbing = true
                                scrubValue = (down.position.x / trackWidthPx * safeDuration).coerceIn(0f, safeDuration)
                                onScrubStart()
                                horizontalDrag(down.id) { change ->
                                    scrubValue = (change.position.x / trackWidthPx * safeDuration).coerceIn(0f, safeDuration)
                                    onScrub(scrubValue)
                                    change.consume()
                                }
                                isScrubbing = false
                                onScrubEnd()
                            }
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                val h = size.height
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.25f),
                    size = Size(size.width, h),
                    cornerRadius = CornerRadius(h / 2, h / 2)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    size = Size(size.width * bufferedFraction, h),
                    cornerRadius = CornerRadius(h / 2, h / 2)
                )
                drawRoundRect(
                    color = Color.White,
                    size = Size(size.width * playedFraction, h),
                    cornerRadius = CornerRadius(h / 2, h / 2)
                )
            }

            val thumbScale by animateFloatAsState(if (isScrubbing) 1.3f else 1f, label = "thumbScale")
            Box(
                modifier = Modifier.fillMaxWidth(playedFraction.coerceIn(0.0001f, 1f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp * thumbScale)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(current: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = ExtraShapes.bottomSheet) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
            Text("Playback speed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                val selected = abs(speed - current) < 0.01f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .tapModifier { onSelect(speed) }
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (speed == 1f) "Normal" else "${formatSpeed(speed)}x",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (selected) {
                        Icon(Icons.Filled.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackSelectionSheet(
    type: TrackSheetType,
    player: ExoPlayer?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val trackType = if (type == TrackSheetType.AUDIO) C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT
    val groups = player?.currentTracks?.groups?.filter { it.type == trackType } ?: emptyList()
    val subsCurrentlyDisabled = player?.trackSelectionParameters?.disabledTrackTypes?.contains(C.TRACK_TYPE_TEXT) ?: true
    val anySubtitleSelected = groups.any { g -> (0 until g.length).any { g.isTrackSelected(it) } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = ExtraShapes.bottomSheet) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
            Text(
                if (type == TrackSheetType.AUDIO) "Audio track" else "Subtitles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            if (type == TrackSheetType.SUBTITLE) {
                TrackRow(label = "Off", selected = subsCurrentlyDisabled || !anySubtitleSelected) {
                    player?.let { p ->
                        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    }
                    onDismiss()
                }
            }

            if (groups.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "No ${if (type == TrackSheetType.AUDIO) "alternate audio tracks" else "subtitle tracks"} found in this stream",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                groups.forEachIndexed { gi, group ->
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val label = format.label ?: format.language?.uppercase() ?: "Track ${gi + 1}.${i + 1}"
                        val selected = group.isTrackSelected(i)
                        TrackRow(label = label, selected = selected) {
                            player?.let { p ->
                                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                    .setTrackTypeDisabled(trackType, false)
                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                                    .build()
                            }
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .tapModifier(onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        if (selected) {
            Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Picture adjustments panel. Brightness is real (it drives the same window
 * brightness as the gesture). Contrast/Saturation are presented as a
 * complete, on-brand UI but intentionally don't alter frames yet — Media3's
 * color-effects pipeline isn't wired into the existing player setup, and
 * this redesign is scoped to UI only, not the playback pipeline. Wiring
 * them up is a small, isolated follow-up if you want real color grading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PictureAdjustmentsSheet(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var contrast by remember { mutableFloatStateOf(0.5f) }
    var saturation by remember { mutableFloatStateOf(0.5f) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = ExtraShapes.bottomSheet) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
            Text("Picture adjustments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Brightness applies immediately. Contrast and saturation are previewed here and will take effect once the video color pipeline is enabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            AdjustSliderRow(icon = Icons.Filled.BrightnessMedium, label = "Brightness", value = brightness, onChange = onBrightnessChange)
            AdjustSliderRow(icon = Icons.Filled.Tune, label = "Contrast", value = contrast, onChange = { contrast = it })
            AdjustSliderRow(icon = Icons.Filled.Tune, label = "Saturation", value = saturation, onChange = { saturation = it })
        }
    }
}

@Composable
private fun AdjustSliderRow(
    icon: ImageVector,
    label: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
    }
}

// ============================= Formatting helpers =============================

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString()
