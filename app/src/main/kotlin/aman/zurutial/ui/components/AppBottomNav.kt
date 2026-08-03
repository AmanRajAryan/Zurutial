package aman.zurutial.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class HomeTab(val label: String) {
    HOME("Home"),
    ROOMS("Rooms"),
    SETTINGS("Settings")
}

@Composable
fun AppBottomNav(current: HomeTab, onSelect: (HomeTab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = current == HomeTab.HOME,
            onClick = { onSelect(HomeTab.HOME) },
            icon = {
                Icon(
                    if (current == HomeTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = HomeTab.HOME.label
                )
            },
            label = { Text(HomeTab.HOME.label) }
        )
        NavigationBarItem(
            selected = current == HomeTab.ROOMS,
            onClick = { onSelect(HomeTab.ROOMS) },
            icon = {
                Icon(
                    if (current == HomeTab.ROOMS) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                    contentDescription = HomeTab.ROOMS.label
                )
            },
            label = { Text(HomeTab.ROOMS.label) }
        )
        NavigationBarItem(
            selected = current == HomeTab.SETTINGS,
            onClick = { onSelect(HomeTab.SETTINGS) },
            icon = {
                Icon(
                    if (current == HomeTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = HomeTab.SETTINGS.label
                )
            },
            label = { Text(HomeTab.SETTINGS.label) }
        )
    }
}
