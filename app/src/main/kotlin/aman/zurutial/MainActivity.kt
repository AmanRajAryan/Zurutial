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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
          var inRoom by remember { mutableStateOf(false) }

          if (inRoom) {
            RoomScreen(viewModel = viewModel)
          } else {
            HomeScreen(
              viewModel = viewModel,
              onEnteredRoom = { inRoom = true }
            )
          }
        }
      }
    }
  }
}
