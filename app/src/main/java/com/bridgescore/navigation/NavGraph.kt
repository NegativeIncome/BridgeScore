package com.bridgescore.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bridgescore.ui.screens.BoardEntryScreen
import com.bridgescore.ui.screens.SessionSetupScreen
import com.bridgescore.ui.screens.SummaryScreen
import com.bridgescore.ui.viewmodel.BridgeViewModel

object Routes {
    const val SETUP = "setup"
    const val ENTRY = "entry"
    const val SUMMARY = "summary"
}

@Composable
fun BridgeNavGraph(navController: NavHostController, viewModel: BridgeViewModel) {
    NavHost(navController = navController, startDestination = Routes.SETUP) {

        composable(Routes.SETUP) {
            SessionSetupScreen(
                viewModel = viewModel,
                onSessionStarted = { navController.navigate(Routes.ENTRY) },
                onOpenSession = { id ->
                    viewModel.loadSession(id)
                    navController.navigate(Routes.ENTRY)
                }
            )
        }

        composable(Routes.ENTRY) {
            BoardEntryScreen(
                viewModel = viewModel,
                onNavigateSummary = { navController.navigate(Routes.SUMMARY) }
            )
        }

        composable(Routes.SUMMARY) {
            SummaryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEditBoard = { boardNumber ->
                    viewModel.navigateToBoard(boardNumber)
                    navController.popBackStack()
                }
            )
        }
    }
}
