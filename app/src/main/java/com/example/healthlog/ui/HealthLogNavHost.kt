package com.example.healthlog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.healthlog.repository.MedicationRepository
import com.example.healthlog.ui.calendar.CalendarPlaceholderScreen
import com.example.healthlog.ui.details.MeasurementDetailsScreen
import com.example.healthlog.ui.details.MeasurementDetailsViewModel
import com.example.healthlog.ui.history.HistoryScreen
import com.example.healthlog.ui.history.HistoryViewModel
import com.example.healthlog.ui.home.HomeScreen
import com.example.healthlog.ui.home.HomeViewModel
import com.example.healthlog.ui.medications.MedicationsPlaceholderScreen
import com.example.healthlog.ui.navigation.HealthLogBottomBar
import com.example.healthlog.ui.navigation.NavigationTab

// The names of the app's screens.
private object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val DETAILS = "details/{measurementId}"
    const val GUIDE = "guide/{type}"
    const val DAY_TIMELINE = "day_timeline/{date}"
    const val PROFILE = "profile"

    fun details(measurementId: Long) = "details/$measurementId"
    fun guide(type: MeasurementType) = "guide/${type.name}"
    fun dayTimeline(date: java.time.LocalDate) = "day_timeline/$date"
}

// Decides which screen is shown and how to move between them.
@Composable
fun HealthLogNavHost(
    repository: MeasurementRepository,
    medicationRepository: MedicationRepository? = null,
    symptomRepository: com.example.healthlog.repository.SymptomRepository? = null,
    userProfileRepository: com.example.healthlog.repository.UserProfileRepository? = null,
    appLockManager: com.example.healthlog.security.AppLockManager? = null,
) {
    val navController = rememberNavController()

    // The active bottom navigation tab on the home destination.
    var selectedTab by rememberSaveable { mutableStateOf(NavigationTab.VITALS) }

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

            // When user is on Medications or Calendar tab, pressing Android back returns to Vitals tab.
            BackHandler(enabled = isHomeCurrent && (selectedTab != NavigationTab.VITALS)) {
                selectedTab = NavigationTab.VITALS
            }

            val bottomBarComposable: @Composable () -> Unit = {
                HealthLogBottomBar(
                    currentTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            val context = androidx.compose.ui.platform.LocalContext.current

            when (selectedTab) {
                NavigationTab.VITALS -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenHistory = {
                            // launchSingleTop: a double tap doesn't open the history twice.
                            navController.navigate(Routes.HISTORY) { launchSingleTop = true }
                        },
                        onOpenProfile = {
                            navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                        },
                        onOpenGuide = { type ->
                            navController.navigate(Routes.guide(type)) { launchSingleTop = true }
                        },
                        isDialogVisible = isHomeCurrent,
                        bottomBar = bottomBarComposable
                    )
                }

                NavigationTab.MEDICATIONS -> {
                    if (medicationRepository != null) {
                        val medicationsViewModel: com.example.healthlog.ui.medications.MedicationsViewModel = viewModel(
                            factory = com.example.healthlog.ui.medications.MedicationsViewModel.factory(
                                medicationRepository,
                                context
                            )
                        )
                        com.example.healthlog.ui.medications.MedicationsScreen(
                            viewModel = medicationsViewModel,
                            bottomBar = bottomBarComposable
                        )
                    } else {
                        MedicationsPlaceholderScreen(
                            bottomBar = bottomBarComposable
                        )
                    }
                }

                NavigationTab.CALENDAR -> {
                    if (medicationRepository != null && symptomRepository != null) {
                        val calendarViewModel: com.example.healthlog.ui.calendar.CalendarViewModel = viewModel(
                            factory = com.example.healthlog.ui.calendar.CalendarViewModel.factory(
                                measurementRepository = repository,
                                medicationRepository = medicationRepository,
                                symptomRepository = symptomRepository,
                                appContext = context
                            )
                        )
                        com.example.healthlog.ui.calendar.CalendarScreen(
                            viewModel = calendarViewModel,
                            onOpenMeasurementDetails = { measurementId ->
                                navController.navigate(Routes.details(measurementId)) { launchSingleTop = true }
                            },
                            onOpenDayTimeline = { date ->
                                navController.navigate(Routes.dayTimeline(date)) { launchSingleTop = true }
                            },
                            bottomBar = bottomBarComposable
                        )
                    } else {
                        CalendarPlaceholderScreen(
                            bottomBar = bottomBarComposable
                        )
                    }
                }
            }
        }

        composable(Routes.HISTORY) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val profileRepo = userProfileRepository ?: remember {
                val db = com.example.healthlog.database.HealthDatabase.getDatabase(context.applicationContext)
                com.example.healthlog.repository.UserProfileRepository(db.userProfileDao())
            }

            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(
                    repository = repository,
                    userProfileRepository = profileRepo,
                )
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

        composable(
            route = Routes.DAY_TIMELINE,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("date")
            val date = dateStr?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                ?: java.time.LocalDate.now()

            if (medicationRepository != null && symptomRepository != null) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val timelineViewModel: com.example.healthlog.ui.calendar.timeline.DayTimelineViewModel = viewModel(
                    factory = com.example.healthlog.ui.calendar.timeline.DayTimelineViewModel.factory(
                        date = date,
                        measurementRepository = repository,
                        medicationRepository = medicationRepository,
                        symptomRepository = symptomRepository,
                        appContext = context
                    )
                )

                com.example.healthlog.ui.calendar.timeline.DayTimelineScreen(
                    viewModel = timelineViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenMeasurementDetails = { measurementId ->
                        navController.navigate(Routes.details(measurementId)) { launchSingleTop = true }
                    }
                )
            }
        }

        composable(Routes.PROFILE) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val profileRepo = userProfileRepository ?: remember {
                val db = com.example.healthlog.database.HealthDatabase.getDatabase(context.applicationContext)
                com.example.healthlog.repository.UserProfileRepository(db.userProfileDao())
            }
            val lockManager = appLockManager ?: remember {
                com.example.healthlog.security.AppLockManager.getInstance(context.applicationContext)
            }

            val profileViewModel: com.example.healthlog.ui.profile.ProfileViewModel = viewModel(
                factory = com.example.healthlog.ui.profile.ProfileViewModel.factory(
                    repository = profileRepo,
                    appLockManager = lockManager
                )
            )

            com.example.healthlog.ui.profile.ProfileScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
