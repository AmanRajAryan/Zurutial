package aman.zurutial.sync

import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer

data class PlaybackBroadcast(
    val positionMs: Long,
    val isPlaying: Boolean,
    val playbackSpeed: Float,
    val hostMeasuredAtServerTime: Long,
    val seekVersion: Long,
    val lastActionBy: String = "",
    val actionType: String = "",
    val lastActionId: String = ""
)

class PlaybackSyncEngine(
    private val player: ExoPlayer,
    private val clockSync: ClockSync,
    private val deviceId: String,
    private val isWebStream: Boolean = false,
    private val onLog: (String) -> Unit = {}
) {
    private var lastSeekVersion = -1L
    private var lastSelfInitiatedActionId = ""
    private var lastBroadcast: PlaybackBroadcast? = null
    private var lastHardSeekTimeMs = 0L

    // Tunables
    private val HARD_SEEK_THRESHOLD_MS = if (isWebStream) 2500L else 400L // loose for web keyframes, tight for local
    private val NUDGE_DEADZONE_MS = 30L             // drift below this: do nothing, avoid micro-jitter chasing
    private val MAX_RATE_ADJUST = 0.07f             // ±7% speed change, inaudible pitch-wise

    fun markSelfInitiated(actionId: String) {
        lastSelfInitiatedActionId = actionId
    }

    fun getLastActionId(): String = lastBroadcast?.lastActionId ?: ""

    fun onBroadcastReceived(broadcast: PlaybackBroadcast, isHost: Boolean = false) {
        lastBroadcast = broadcast
        onLog("Rx [${broadcast.actionType}] play=${broadcast.isPlaying} pos=${broadcast.positionMs}")
        // Ignore our own action echoing back
        if (broadcast.lastActionId == lastSelfInitiatedActionId && broadcast.lastActionId.isNotEmpty()) {
            onLog("-> Ignored local echo")
            if (broadcast.seekVersion > lastSeekVersion) {
                lastSeekVersion = broadcast.seekVersion
            }
            return
        }

        // Stale/out-of-order write check
        if (broadcast.seekVersion < lastSeekVersion) {
            onLog("-> Ignored stale write")
            return
        }
        val isNewAction = broadcast.seekVersion > lastSeekVersion
        lastSeekVersion = broadcast.seekVersion

        val rawTarget = computeTargetPosition(broadcast)
        val duration = player.duration
        val maxPos = if (duration > 0) duration else Long.MAX_VALUE
        val targetPosition = rawTarget.coerceIn(0L, maxPos)
        val currentPosition = player.currentPosition
        val drift = targetPosition - currentPosition // positive = we're behind

        onLog("-> Target: $targetPosition, Cur: $currentPosition, Drift: $drift")

        if (isNewAction && (broadcast.actionType == "seek" || broadcast.actionType == "speed")) {
            // A real seek/action happened — always hard seek, no smoothing
            onLog("-> Hard seeking (isSeek=true or speed changed)")
            hardSeek(targetPosition, broadcast.isPlaying, broadcast.playbackSpeed)
            return
        }

        if (broadcast.isPlaying != player.playWhenReady) {
            onLog("-> Updating playWhenReady: ${broadcast.isPlaying}")
            player.playWhenReady = broadcast.isPlaying
        }

        if (isHost) {
            onLog("-> Host received action. Skipping drift evaluation.")
            return
        }

        evaluateDrift()
    }

    fun evaluateDrift() {
        val b = lastBroadcast ?: return
        
        if (System.currentTimeMillis() - lastHardSeekTimeMs < 4000) {
            // Give the player time to buffer and settle after a hard seek
            return
        }
        
        // Tough Love: If a member's phone is too slow and enters buffering,
        // DO NOT force seek them. Seeking flushes the buffer and causes an infinite buffering loop.
        // Let them buffer in peace. When they finish, they will snap forward to catch up.
        if (player.playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
            onLog("-> Buffering... Skipping drift sync.")
            return
        }

        val rawTarget = computeTargetPosition(b)
        val duration = player.duration
        val maxPos = if (duration > 0) duration else Long.MAX_VALUE
        val targetPosition = rawTarget.coerceIn(0L, maxPos)
        val currentPosition = player.currentPosition
        val drift = targetPosition - currentPosition // positive = we're behind

        val baseSpeed = b.playbackSpeed
        if (!b.isPlaying) {
            resetToTargetSpeed(baseSpeed)
            if (kotlin.math.abs(drift) > NUDGE_DEADZONE_MS) {
                // When paused, we don't nudge, we just hard snap if off
                hardSeek(targetPosition, false, baseSpeed)
            }
            return
        }

        when {
            kotlin.math.abs(drift) > HARD_SEEK_THRESHOLD_MS -> {
                onLog("-> Hard seeking (drift > 400ms)")
                hardSeek(targetPosition, b.isPlaying, baseSpeed)
            }
            kotlin.math.abs(drift) > NUDGE_DEADZONE_MS -> {
                if (player.playbackParameters.speed == baseSpeed) {
                    onLog("-> Applying rate nudge")
                }
                val correctionFactor = (drift.toFloat() / 1000f).coerceIn(-MAX_RATE_ADJUST, MAX_RATE_ADJUST)
                player.playbackParameters = PlaybackParameters(baseSpeed + correctionFactor)
            }
            else -> {
                if (player.playbackParameters.speed != baseSpeed) {
                    onLog("-> Normal speed")
                }
                resetToTargetSpeed(baseSpeed)
            }
        }
    }

    private fun computeTargetPosition(b: PlaybackBroadcast): Long {
        val nowServerTime = clockSync.toServerTime(System.currentTimeMillis())
        val elapsedSinceHostMeasured = nowServerTime - b.hostMeasuredAtServerTime
        return if (b.isPlaying) {
            b.positionMs + (elapsedSinceHostMeasured * b.playbackSpeed).toLong()
        } else {
            b.positionMs
        }
    }

    private fun resetToTargetSpeed(targetSpeed: Float) {
        if (player.playbackParameters.speed != targetSpeed) {
            player.playbackParameters = PlaybackParameters(targetSpeed)
        }
    }

    private fun hardSeek(positionMs: Long, isPlaying: Boolean, targetSpeed: Float) {
        lastHardSeekTimeMs = System.currentTimeMillis()
        player.seekTo(positionMs)
        player.playWhenReady = isPlaying
        resetToTargetSpeed(targetSpeed)
    }

    fun forceResync() {
        val b = lastBroadcast ?: return
        onLog("-> Forcing resync after app resume")
        val rawTarget = computeTargetPosition(b)
        val duration = player.duration
        val maxPos = if (duration > 0) duration else Long.MAX_VALUE
        val targetPosition = rawTarget.coerceIn(0L, maxPos)
        hardSeek(targetPosition, b.isPlaying, b.playbackSpeed)
    }
}
