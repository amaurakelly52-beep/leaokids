package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LeaoViewModel
import com.example.ui.screens.LeaoMainContent
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val application = applicationContext as Application
        val viewModel: LeaoViewModel = viewModel(
          factory = LeaoViewModel.provideFactory(application)
        )
        LeaoMainContent(viewModel = viewModel)
      }
    }
  }
}
