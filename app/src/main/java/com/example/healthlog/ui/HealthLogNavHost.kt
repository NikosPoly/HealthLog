package com.example.healthlog.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.ui.history.HistoryScreen
import com.example.healthlog.ui.history.HistoryViewModel
import com.example.healthlog.ui.home.HomeScreen
import com.example.healthlog.ui.home.HomeViewModel

// The names of the app's screens.
private object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
}

// Decides which screen is shown and how to move between them.
@Composable
fun HealthLogNavHost(
    repository: MeasurementRepository
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(repository)
            )

            HomeScreen(
                viewModel = viewModel,
                onOpenHistory = {
                    // launchSingleTop: a double tap doesn't open the history twice.
                    navController.navigate(Routes.HISTORY) { launchSingleTop = true }
                }
            )
        }

        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(repository)
            )

            HistoryScreen(
                viewModel = viewModel,
                onBack = {
                    // Go back to Home, but never remove Home itself (e.g. on a double tap).
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
