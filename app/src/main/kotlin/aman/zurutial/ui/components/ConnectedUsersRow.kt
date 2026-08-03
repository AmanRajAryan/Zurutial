package aman.zurutial.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import aman.zurutial.ui.theme.ZurutialTheme

enum class MemberPresence { SYNCED, PAUSED, AWAY }

data class MemberDisplay(
    val deviceId: String,
    val displayName: String,
    val isHost: Boolean,
    val presence: MemberPresence
)

/** Beautiful horizontal scroller of participant avatars with live status dots. */
@Composable
fun ConnectedUsersRow(members: List<MemberDisplay>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(members, key = { it.deviceId }) { member ->
            MemberAvatarItem(member)
        }
    }
}

@Composable
private fun MemberAvatarItem(member: MemberDisplay) {
    val colors = ZurutialTheme.extendedColors
    val dotColor = when (member.presence) {
        MemberPresence.SYNCED -> colors.syncGreen
        MemberPresence.PAUSED -> colors.syncOrange
        MemberPresence.AWAY -> colors.syncRed
    }
    val statusText = when (member.presence) {
        MemberPresence.SYNCED -> "Synced"
        MemberPresence.PAUSED -> "Paused"
        MemberPresence.AWAY -> "Away"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 76.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (member.isHost) colors.heroGradient else colors.heroGradientSecondary
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.displayName.take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            if (member.isHost) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Host",
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 6.dp))
        Text(
            text = member.displayName,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
