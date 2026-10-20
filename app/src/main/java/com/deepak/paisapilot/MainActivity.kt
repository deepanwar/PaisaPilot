package com.deepak.paisapilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.deepak.paisapilot.spike.SpikeViewModel
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaisaPilotTheme {
                val viewModel: SpikeViewModel = hiltViewModel()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(
                        text = viewModel.message,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
