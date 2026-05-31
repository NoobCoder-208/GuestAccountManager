package com.chuong.guestmng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.chuong.guestmng.presentation.ui.MainAppScreen
import com.chuong.guestmng.presentation.viewmodel.GuestViewModel
import com.chuong.guestmng.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  
  private val guestViewModel: GuestViewModel by viewModels { GuestViewModel.Factory }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppScreen(viewModel = guestViewModel)
      }
    }
  }
}

