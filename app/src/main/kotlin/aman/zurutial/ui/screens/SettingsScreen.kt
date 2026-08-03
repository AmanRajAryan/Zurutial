package aman.zurutial.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aman.zurutial.data.SettingsManager

@Composable
fun SettingsScreen(
    displayName: String,
    onDisplayNameChanged: (String) -> Unit,
    onExportLogs: () -> Unit,
    onResetConnection: () -> Unit
) {
    val context = LocalContext.current

    var dynamicColor by remember { mutableStateOf(SettingsManager.getDynamicColorEnabled(context)) }
    var darkThemeMode by remember { mutableStateOf(SettingsManager.getDarkThemeMode(context)) }
    var pureBlack by remember { mutableStateOf(SettingsManager.getPureBlackEnabled(context)) }
    var autoSync by remember { mutableStateOf(SettingsManager.getAutoSyncEnabled(context)) }
    var seekSensitivity by remember { mutableStateOf(SettingsManager.getSeekSensitivity(context)) }
    var debugLogsEnabled by remember { mutableStateOf(SettingsManager.getDebugLogsEnabled(context)) }
    var showNameDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        item { SectionHeader("Profile") }
        item {
            SettingsRow(
                icon = Icons.Filled.Person,
                title = "Display Name",
                subtitle = displayName.ifBlank { "Not set" },
                onClick = { showNameDialog = true }
            )
        }

        item { SectionHeader("Appearance") }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.Palette,
                title = "Dynamic Color",
                subtitle = "Use colors from your wallpaper (Material You)",
                checked = dynamicColor,
                onCheckedChange = {
                    dynamicColor = it
                    SettingsManager.setDynamicColorEnabled(context, it)
                }
            )
        }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.DarkMode,
                title = "Dark Theme",
                subtitle = "Follow system, or force dark",
                checked = darkThemeMode != "light",
                onCheckedChange = {
                    val mode = if (it) "dark" else "light"
                    darkThemeMode = mode
                    SettingsManager.setDarkThemeMode(context, mode)
                }
            )
        }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.DarkMode,
                title = "Pure Black Mode",
                subtitle = "True AMOLED black backgrounds",
                checked = pureBlack,
                onCheckedChange = {
                    pureBlack = it
                    SettingsManager.setPureBlackEnabled(context, it)
                }
            )
        }

        item { SectionHeader("Playback") }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.Sync,
                title = "Auto Sync",
                subtitle = "Automatically correct drift during playback",
                checked = autoSync,
                onCheckedChange = {
                    autoSync = it
                    SettingsManager.setAutoSyncEnabled(context, it)
                }
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text("Seek Sensitivity", style = MaterialTheme.typography.titleSmall)
                }
                Slider(
                    value = seekSensitivity,
                    onValueChange = {
                        seekSensitivity = it
                        SettingsManager.setSeekSensitivity(context, it)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { SectionHeader("Network") }
        item {
            SettingsRow(
                icon = Icons.Filled.Refresh,
                title = "Reset Connection",
                subtitle = "Force a fresh sync with the room",
                onClick = onResetConnection
            )
        }

        item { SectionHeader("Advanced") }
        item { SectionHeader("Developer Options", isSubheader = true) }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.BugReport,
                title = "Enable Debug Logs",
                subtitle = "Show a floating log viewer during playback",
                checked = debugLogsEnabled,
                onCheckedChange = {
                    debugLogsEnabled = it
                    SettingsManager.setDebugLogsEnabled(context, it)
                }
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.Download,
                title = "Export Logs",
                subtitle = "Save the current session log to a file",
                onClick = onExportLogs
            )
        }

        item { SectionHeader("About") }
        item {
            SettingsRow(icon = Icons.Filled.Info, title = "Version", subtitle = "1.0", onClick = null)
        }
        item {
            SettingsRow(icon = Icons.Filled.Code, title = "GitHub", subtitle = "View source", onClick = null)
        }
        item {
            SettingsRow(icon = Icons.Filled.Description, title = "Licenses", subtitle = "Open source licenses", onClick = null)
        }
    }

    if (showNameDialog) {
        var tempName by remember { mutableStateOf(displayName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Display Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = tempName.trim()
                    if (trimmed.isNotEmpty()) onDisplayNameChanged(trimmed)
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, isSubheader: Boolean = false) {
    Text(
        text = title,
        style = if (isSubheader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
        color = if (isSubheader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Surface(
        onClick = onClick ?: {},
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
