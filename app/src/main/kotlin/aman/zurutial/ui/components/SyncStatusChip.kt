package aman.zurutial.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import aman.zurutial.ui.theme.ExtraShapes
import aman.zurutial.ui.theme.ZurutialTheme

enum class SyncLevel { PERFECT, SYNCING, RECOVERING, HIGH_LATENCY, HOST_DISCONNECTED }

data class SyncStatus(val level: SyncLevel, val label: String)

/**
 * The floating pill that reports live sync health. Pulses gently while
 * perfectly synced, and switches color by severity (green / orange / red).
 */
@Composable
fun SyncStatusChip(status: SyncStatus, modifier: Modifier = Modifier) {
    val colors = ZurutialTheme.extendedColors
    val (dot, container, content) = when (status.level) {
        SyncLevel.PERFECT -> Triple(colors.syncGreen, colors.syncGreenContainer, colors.syncGreen)
        SyncLevel.SYNCING -> Triple(colors.syncOrange, colors.syncOrangeContainer, colors.syncOrange)
        SyncLevel.RECOVERING -> Triple(colors.syncOrange, colors.syncOrangeContainer, colors.syncOrange)
        SyncLevel.HIGH_LATENCY -> Triple(colors.syncOrange, colors.syncOrangeContainer, colors.syncOrange)
        SyncLevel.HOST_DISCONNECTED -> Triple(colors.syncRed, colors.syncRedContainer, colors.syncRed)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status.level == SyncLevel.PERFECT) 1.35f else 1f,
        animationSpec = infiniteRepeatable(tween(1100), repeatMode = RepeatMode.Reverse),
        label = "dotPulse"
    )

    Surface(
        modifier = modifier,
        shape = ExtraShapes.pill,
        color = container,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(pulse)
                    .background(dot, CircleShape)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelLarge,
                color = content
            )
        }
    }
}
