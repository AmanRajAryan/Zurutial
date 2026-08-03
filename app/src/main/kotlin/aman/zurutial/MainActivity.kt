package aman.zurutial

import aman.zurutial.data.RecentRoomsManager
import aman.zurutial.data.SettingsManager
import aman.zurutial.ui.components.AppBottomNav
import aman.zurutial.ui.components.HomeTab
import aman.zurutial.ui.screens.CreateRoomScreen
import aman.zurutial.ui.screens.HomeScreen
import aman.zurutial.ui.screens.JoinRoomScreen
import aman.zurutial.ui.screens.OnboardingScreen
import aman.zurutial.ui.screens.RoomScreen
import aman.zurutial.ui.screens.RoomsScreen
import aman.zurutial.ui.screens.SettingsScreen
import aman.zurutial.ui.theme.ComposeEmptyActivityTheme
import aman.zurutial.ui.theme.Motion
import aman.zurutial.ui.viewmodel.RoomUiState
import aman.zurutial.ui.viewmodel.RoomViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

private sealed class TopScreen {
    object Main : TopScreen()
    object CreateRoom : TopScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            var pureBlack by remember { mutableStateOf(SettingsManager.getPureBlackEnabled(context)) }
            var dynamicColor by remember { mutableStateOf(SettingsManager.getDynamicColorEnabled(context)) }
            var darkMode by remember { mutableStateOf(SettingsManager.getDarkThemeMode(context)) }

            ComposeEmptyActivityTheme(
                darkTheme = if (darkMode == "system") androidx.compose.foundation.isSystemInDarkTheme() else darkMode == "dark",
                dynamicColor = dynamicColor,
                pureBlack = pureBlack
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: RoomViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    var hasOnboarded by remember {
                        mutableStateOf(SettingsManager.getDisplayName(context).isNotBlank())
                    }
                    var displayName by remember { mutableStateOf(SettingsManager.getDisplayName(context)) }
                    var tab by remember { mutableStateOf(HomeTab.HOME) }
                    var topScreen by remember { mutableStateOf<TopScreen>(TopScreen.Main) }
                    var joinFlowActive by remember { mutableStateOf(false) }
                    var reconnectCode by remember { mutableStateOf("") }

                    val recentRooms = remember(uiState) {
                        RecentRoomsManager.getRecentRoomDetails(context)
                    }

                    val route = when {
                        !hasOnboarded -> "onboarding"
                        uiState is RoomUiState.InRoom -> "room"
                        uiState is RoomUiState.JoinRoomFileSelection || joinFlowActive -> "join"
                        topScreen is TopScreen.CreateRoom -> "create"
                        else -> "main"
                    }

                    AnimatedContent(
                        targetState = route,
                        transitionSpec = Motion.sharedAxisY(),
                        label = "topLevelNav",
                        modifier = Modifier.fillMaxSize()
                    ) { current ->
                        when (current) {
                            "onboarding" -> OnboardingScreen(onContinue = { name ->
                                SettingsManager.setDisplayName(context, name)
                                displayName = name
                                hasOnboarded = true
                            })

                            "room" -> RoomScreen(
                                viewModel = viewModel,
                                onExit = {
                                    joinFlowActive = false
                                    topScreen = TopScreen.Main
                                    tab = HomeTab.HOME
                                }
                            )

                            "join" -> JoinRoomScreen(
                                viewModel = viewModel,
                                initialRoomCode = reconnectCode,
                                onBack = {
                                    viewModel.resetState()
                                    joinFlowActive = false
                                    reconnectCode = ""
                                }
                            )

                            "create" -> CreateRoomScreen(
                                viewModel = viewModel,
                                onBack = {
                                    viewModel.resetState()
                                    topScreen = TopScreen.Main
                                }
                            )

                            else -> Scaffold(
                                bottomBar = {
                                    AppBottomNav(current = tab, onSelect = { tab = it })
                                }
                            ) { padding ->
                                AnimatedContent(
                                    targetState = tab,
                                    label = "tabSwitch",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = padding.calculateBottomPadding())
                                ) { selectedTab ->
                                    when (selectedTab) {
                                        HomeTab.HOME -> HomeScreen(
                                            displayName = displayName,
                                            recentRooms = recentRooms,
                                            onCreateRoom = { topScreen = TopScreen.CreateRoom },
                                            onJoinRoom = {
                                                reconnectCode = ""
                                                joinFlowActive = true
                                            },
                                            onReconnect = { code ->
                                                reconnectCode = code
                                                joinFlowActive = true
                                            },
                                            onSeeAllRooms = { tab = HomeTab.ROOMS }
                                        )

                                        HomeTab.ROOMS -> RoomsScreen(
                                            recentRooms = recentRooms,
                                            onReconnect = { code ->
                                                reconnectCode = code
                                                joinFlowActive = true
                                            },
                                            onCreateRoom = { topScreen = TopScreen.CreateRoom }
                                        )

                                        HomeTab.SETTINGS -> SettingsScreen(
                                            displayName = displayName,
                                            onDisplayNameChanged = {
                                                SettingsManager.setDisplayName(context, it)
                                                displayName = it
                                            },
                                            onExportLogs = {
                                                try {
                                                    val dir = context.getExternalFilesDir(null)
                                                    val file = java.io.File(dir, "zurutial_logs_${System.currentTimeMillis()}.txt")
                                                    file.writeText(viewModel.logs.value.joinToString("\n"))
                                                    android.widget.Toast.makeText(context, "Saved to ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onResetConnection = {
                                                viewModel.forceResync()
                                                viewModel.forceHeartbeat()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
