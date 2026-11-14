package com.example.healthlog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthlog.guidelines.protocols.MeasurementGuideScreen
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.ui.details.MeasurementDetailsScreen
import com.example.healthlog.ui.details.MeasurementDetailsViewModel
import com.example.healthlog.ui.history.HistoryScreen
import com.example.healthlog.ui.history.HistoryViewModel
import com.example.healthlog.ui.home.HomeScreen
import com.example.healthlog.ui.home.HomeViewModel

// The names of the app's screens.
private object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val DETAILS = "details/{measurementId}"
    const val GUIDE = "guide/{type}"

    fun details(measurementId: Long) = "details/$measurementId"
    fun guide(type: MeasurementType) = "guide/${type.name}"
}

// Decides which screen is shown and how to move between them.
@Composable
fun HealthLogNavHost(
    repository: MeasurementRepository
) {
    val navController = rememberNavController()

    // The history page last shown. Kept here, above the history screen, so it is
    // remembered when the history is closed and opened again while the app is running.
    // Blood pressure when the app starts.
    var historyType by rememberSaveable { mutableStateOf(MeasurementType.BLOOD_PRESSURE) }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(repository)
            )

            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val isHomeCurrent = currentBackStackEntry?.destination?.route == Routes.HOME

            HomeScreen(
                viewModel = viewModel,
                onOpenHistory = {
                    // launchSingleTop: a double tap doesn't open the history twice.
                    navController.navigate(Routes.HISTORY) { launchSingleTop = true }
                },
                onOpenGuide = { type ->
                    navController.navigate(Routes.guide(type)) { launchSingleTop = true }
                },
                isDialogVisible = isHomeCurrent
            )
        }

        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(repository)
            )

            HistoryScreen(
                viewModel = viewModel,
                selectedType = historyType,
                onSelectType = { historyType = it },
                onOpenDetails = { measurementId ->
                    navController.navigate(Routes.details(measurementId)) { launchSingleTop = true }
                },
                onBack = {
                    // Go back to Home, but never remove Home itself (e.g. on a double tap).
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(
            route = Routes.DETAILS,
            arguments = listOf(
                navArgument("measurementId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val measurementId = backStackEntry.arguments?.getLong("measurementId") ?: 0L
            val viewModel: MeasurementDetailsViewModel = viewModel(
                factory = MeasurementDetailsViewModel.factory(repository, measurementId)
            )

            MeasurementDetailsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.GUIDE,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val typeName = backStackEntry.arguments?.getString("type")
            val type = typeName?.let { runCatching { MeasurementType.valueOf(it) }.getOrNull() }
                ?: MeasurementType.BLOOD_PRESSURE

            MeasurementGuideScreen(
                type = type,
                showTabs = false,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
