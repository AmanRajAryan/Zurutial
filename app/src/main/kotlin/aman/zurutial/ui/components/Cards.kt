package aman.zurutial.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A card that lifts (scales up + raises elevation) while pressed — the
 * "cards lift on press" interaction from the design spec. Wraps Material3
 * Card so callers keep normal Card ergonomics.
 */
@Composable
fun LiftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardLift"
    )
    val elevation by animateFloatAsState(
        targetValue = if (pressed) 8f else 1f,
        label = "cardElevation"
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale },
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
            interactionSource = interactionSource,
            content = content
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            content = content
        )
    }
}

/** Copy button that briefly swaps to a checkmark with a tiny scale-pop on success. */
@Composable
fun CopyFeedbackIconButton(onCopy: () -> Unit) {
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScopeSafe()

    IconButton(onClick = {
        onCopy()
        copied = true
        scope.launch {
            delay(1400)
            copied = false
        }
    }) {
        AnimatedContent(
            targetState = copied,
            transitionSpec = { (scaleIn() ) togetherWith (scaleOut()) },
            label = "copyIcon"
        ) { isCopied ->
            if (isCopied) {
                Icon(Icons.Filled.Check, contentDescription = "Copied", tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
            }
        }
    }
}

@Composable
private fun rememberCoroutineScopeSafe() = androidx.compose.runtime.rememberCoroutineScope()
