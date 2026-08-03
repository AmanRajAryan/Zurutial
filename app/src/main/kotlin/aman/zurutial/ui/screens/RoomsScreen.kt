package aman.zurutial.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aman.zurutial.data.RecentRoom
import aman.zurutial.ui.components.EmptyState
import aman.zurutial.ui.components.LiftCard

@Composable
fun RoomsScreen(
    recentRooms: List<RecentRoom>,
    onReconnect: (String) -> Unit,
    onCreateRoom: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Rooms",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        if (recentRooms.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.VideoLibrary,
                title = "No Rooms",
                description = "Create your first room to start a synchronized watch party.",
                actionLabel = "Create your first room",
                onAction = onCreateRoom,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                items(recentRooms, key = { it.roomCode }) { room ->
                    LiftCard(onClick = { onReconnect(room.roomCode) }, modifier = Modifier.padding(bottom = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(room.roomCode, style = MaterialTheme.typography.titleMedium)
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
            }
        }
    }
}
