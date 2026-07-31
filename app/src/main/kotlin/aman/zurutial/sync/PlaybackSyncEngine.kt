package aman.zurutial.sync

import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer

data class PlaybackBroadcast(
    val positionMs: Long,
    val isPlaying: Boolean,
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
    private val onLog: (String) -> Unit = {}
) {
    private var lastSeekVersion = -1L
    private var lastSelfInitiatedActionId = ""
    private var lastBroadcast: PlaybackBroadcast? = null

    // Tunables
    private val HARD_SEEK_THRESHOLD_MS = 250L      // drift beyond this: snap, don't nudge
    private val NUDGE_DEADZONE_MS = 30L             // drift below this: do nothing, avoid micro-jitter chasing
    private val MAX_RATE_ADJUST = 0.05f             // ±5% speed change, inaudible pitch-wise

    fun markSelfInitiated(actionId: String) {
        lastSelfInitiatedActionId = actionId
    }

    fun onBroadcastReceived(broadcast: PlaybackBroadcast) {
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

        if (isNewAction && broadcast.actionType == "seek") {
            // A real seek/action happened — always hard seek, no smoothing
            onLog("-> Hard seeking (isSeek=true)")
            hardSeek(targetPosition, broadcast.isPlaying)
            return
        }

        if (broadcast.isPlaying != player.playWhenReady) {
            onLog("-> Updating playWhenReady: ${broadcast.isPlaying}")
            player.playWhenReady = broadcast.isPlaying
        }

        when {
            kotlin.math.abs(drift) > HARD_SEEK_THRESHOLD_MS -> {
                onLog("-> Hard seeking (drift > 400ms)")
                hardSeek(targetPosition, broadcast.isPlaying)
            }
            kotlin.math.abs(drift) > NUDGE_DEADZONE_MS -> {
                onLog("-> Applying rate nudge")
                applyRateNudge(drift)
            }
            else -> {
                onLog("-> Normal speed")
                resetToNormalSpeed()
            }
        }
    }

    private fun computeTargetPosition(b: PlaybackBroadcast): Long {
        val nowServerTime = clockSync.toServerTime(System.currentTimeMillis())
        val elapsedSinceHostMeasured = nowServerTime - b.hostMeasuredAtServerTime
        return if (b.isPlaying) {
            b.positionMs + elapsedSinceHostMeasured
        } else {
            b.positionMs
        }
    }

    private fun applyRateNudge(driftMs: Long) {
        // Behind (positive drift) -> speed up to catch up. Ahead -> slow down.
        val correctionFactor = (driftMs.toFloat() / 1000f).coerceIn(-MAX_RATE_ADJUST, MAX_RATE_ADJUST)
        val newSpeed = 1.0f + correctionFactor
        player.playbackParameters = PlaybackParameters(newSpeed)
    }

    private fun resetToNormalSpeed() {
        if (player.playbackParameters.speed != 1.0f) {
            player.playbackParameters = PlaybackParameters(1.0f)
        }
    }

    private fun hardSeek(positionMs: Long, isPlaying: Boolean) {
        player.seekTo(positionMs)
        player.playWhenReady = isPlaying
        resetToNormalSpeed()
    }

    fun forceResync() {
        val b = lastBroadcast ?: return
        onLog("-> Forcing resync after app resume")
        val rawTarget = computeTargetPosition(b)
        val duration = player.duration
        val maxPos = if (duration > 0) duration else Long.MAX_VALUE
        val targetPosition = rawTarget.coerceIn(0L, maxPos)
        hardSeek(targetPosition, b.isPlaying)
    }
}
