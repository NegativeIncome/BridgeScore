package com.bridgescore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.bridgescore.navigation.BridgeNavGraph
import com.bridgescore.ui.theme.BridgeScoreTheme
import com.bridgescore.ui.viewmodel.BridgeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BridgeViewModel by viewModels {
        BridgeViewModel.Factory((application as BridgeScoreApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BridgeScoreTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    val navController = rememberNavController()
                    BridgeNavGraph(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}
