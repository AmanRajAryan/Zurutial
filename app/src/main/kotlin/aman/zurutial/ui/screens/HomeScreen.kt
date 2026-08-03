package aman.zurutial.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aman.zurutial.data.RecentRoom
import aman.zurutial.ui.components.GradientHeroCard
import aman.zurutial.ui.components.LiftCard
import aman.zurutial.ui.theme.ZurutialTheme

@Composable
fun HomeScreen(
    displayName: String,
    recentRooms: List<RecentRoom>,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onReconnect: (String) -> Unit,
    onSeeAllRooms: () -> Unit
) {
    val colors = ZurutialTheme.extendedColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Zurutial",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Watch together anywhere",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

        GradientHeroCard(
            title = "Create Room",
            subtitle = "Host a synchronized watch party",
            icon = Icons.Filled.Videocam,
            gradient = colors.heroGradient,
            onClick = onCreateRoom
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

        GradientHeroCard(
            title = "Join Room",
            subtitle = "Enter a code to hop into a party",
            icon = Icons.Filled.Groups,
            gradient = colors.heroGradientSecondary,
            onClick = onJoinRoom
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))

        if (recentRooms.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Rooms", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onSeeAllRooms) { Text("See all") }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            recentRooms.take(3).forEach { room ->
                RecentRoomRow(room = room, onClick = { onReconnect(room.roomCode) })
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(10.dp))
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RecentRoomRow(room: RecentRoom, onClick: () -> Unit) {
    LiftCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(room.roomCode, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (room.videoName.isNotBlank()) room.videoName else "Watch room",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                formatRelativeDate(room.lastUsedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatRelativeDate(timestampMs: Long): String {
    if (timestampMs <= 0L) return ""
    val diff = System.currentTimeMillis() - timestampMs
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d ago"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.US).format(java.util.Date(timestampMs))
    }
}
