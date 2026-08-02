package aman.zurutial

import aman.zurutial.ui.screens.HomeScreen
import aman.zurutial.ui.screens.RoomScreen
import aman.zurutial.ui.theme.ComposeEmptyActivityTheme
import aman.zurutial.ui.viewmodel.RoomViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      ComposeEmptyActivityTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          val viewModel: RoomViewModel = viewModel()
          val uiState by viewModel.uiState.collectAsState()

          if (uiState is aman.zurutial.ui.viewmodel.RoomUiState.InRoom) {
            RoomScreen(viewModel = viewModel)
          } else {
            HomeScreen(
              viewModel = viewModel,
              onEnteredRoom = {}
            )
          }
        }
      }
    }
  }
}
