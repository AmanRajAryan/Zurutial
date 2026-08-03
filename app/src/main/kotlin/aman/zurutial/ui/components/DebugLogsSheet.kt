package aman.zurutial.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Collapsed "Debug Logs — Tap to expand" pill. Only ever shown when the user has
 * enabled Settings -> Developer Options -> Debug Logs; never occupies permanent
 * screen space otherwise.
 */
@Composable
fun DebugLogsCollapsedPill(onExpand: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onExpand,
        modifier = modifier,
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
            ) {
                Text("🐞", fontSize = 16.sp)
            }
            Column {
                Text("Debug Logs", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Tap to expand",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogsSheet(
    logs: List<String>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var query by remember { mutableStateOf("") }

    val filtered = remember(logs, query) {
        if (query.isBlank()) logs else logs.filter { it.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Debug Logs", style = MaterialTheme.typography.titleLarge)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search logs") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LogActionButton(Icons.Filled.ContentCopy, "Copy") {
                    clipboard.setText(AnnotatedString(filtered.joinToString("\n")))
                }
                LogActionButton(Icons.Filled.Share, "Share") {
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, filtered.joinToString("\n"))
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share logs"))
                }
                LogActionButton(Icons.Filled.Download, "Export") {
                    try {
                        val dir = context.getExternalFilesDir(null)
                        val file = java.io.File(dir, "zurutial_logs_${System.currentTimeMillis()}.txt")
                        file.writeText(filtered.joinToString("\n"))
                        android.widget.Toast.makeText(context, "Saved to ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                LogActionButton(Icons.Filled.Clear, "Clear") { onClear() }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            SelectionContainer {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    items(filtered) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LogActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(onClick = onClick, colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}
