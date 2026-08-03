package aman.zurutial.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiPiece(
    val angleDeg: Float,
    val speed: Float,
    val color: Color,
    val size: Float,
    val spin: Float
)

/**
 * A short-lived confetti burst, triggered by bumping [trigger]. Draws a ring of
 * small rectangles bursting outward and fading — used on "Room joined".
 */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    var progress by remember { mutableStateOf(1f) } // 1f = finished/hidden
    val palette = listOf(
        Color(0xFFB388FF), Color(0xFF7FE0D6), Color(0xFFFFB25E), Color(0xFF6FCF7A), Color(0xFFFF6E6E)
    )
    val pieces = remember(trigger) {
        List(28) {
            ConfettiPiece(
                angleDeg = Random.nextFloat() * 360f,
                speed = 0.6f + Random.nextFloat() * 0.6f,
                color = palette[Random.nextInt(palette.size)],
                size = 6f + Random.nextFloat() * 6f,
                spin = Random.nextFloat() * 360f
            )
        }
    }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        progress = 0f
        val start = System.currentTimeMillis()
        val durationMs = 900L
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            progress = (elapsed / durationMs.toFloat()).coerceAtMost(1f)
            if (progress >= 1f) break
            kotlinx.coroutines.delay(16)
        }
    }

    if (progress < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.28f)
            val maxDist = size.minDimension * 0.55f
            pieces.forEach { piece ->
                val dist = maxDist * piece.speed * progress
                val rad = Math.toRadians(piece.angleDeg.toDouble())
                val x = center.x + (cos(rad) * dist).toFloat()
                val y = center.y + (sin(rad) * dist).toFloat() + (progress * progress * 140f) // gravity
                val alpha = (1f - progress).coerceIn(0f, 1f)
                rotate(piece.spin * progress, pivot = Offset(x, y)) {
                    drawRect(
                        color = piece.color.copy(alpha = alpha),
                        topLeft = Offset(x - piece.size / 2f, y - piece.size / 2f),
                        size = androidx.compose.ui.geometry.Size(piece.size, piece.size * 0.6f)
                    )
                }
            }
        }
    }
}
