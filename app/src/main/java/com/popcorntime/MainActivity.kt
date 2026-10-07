package com.popcorntime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.popcorntime.ui.screens.MainScreen
import com.popcorntime.ui.theme.PopcornTimeTheme
import com.popcorntime.viewmodel.PopcornViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PopcornViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming magnet link intents
        intent?.data?.let { uri ->
            if (uri.scheme == "magnet") {
                viewModel.importMagnet("Shared Torrent", uri.toString())
            }
        }

        setContent {
            PopcornTimeTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
