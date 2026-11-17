package com.example.healthlog.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.history.chart.ChartPage
import com.example.healthlog.ui.history.chart.ChartPeriod
import com.example.healthlog.ui.history.chart.PeriodChips
import com.example.healthlog.ui.history.chart.ScreenTap
import com.example.healthlog.ui.history.chart.watchTaps
import com.example.healthlog.ui.history.filter.ClinicalStageFilter
import com.example.healthlog.ui.history.filter.FilterBar
import com.example.healthlog.ui.history.filter.FilterBottomSheet
import com.example.healthlog.ui.history.filter.HistoryFilterState
import com.example.healthlog.ui.history.filter.MeasurementConditionFilter
import com.example.healthlog.ui.history.list.EntryList
import com.example.healthlog.ui.theme.HealthLogTheme
import com.example.healthlog.ui.theme.TypeTheme
import kotlinx.coroutines.launch

// Layout only: the history screen, drawn from the values it receives;
// user actions are reported through the callbacks.
// One page per measurement type, chosen with the tabs or by swiping left and right.
// The button above the pages switches every page between the list and the chart.
// In chart mode, the chips below it choose the period of the chart.
// In list mode, a space-saving filter bar allows filtering by clinical stage and physical conditions.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    // The raw list of every type, newest first (used for charts).
    measurementsByType: Map<MeasurementType, List<Measurement>>,
    // The filtered list of every type, newest first (used for entry list).
    filteredMeasurementsByType: Map<MeasurementType, List<Measurement>> = measurementsByType,
    // The active filter state per type.
    filterStateByType: Map<MeasurementType, HistoryFilterState> = emptyMap(),
    // The type whose page is shown.
    selectedType: MeasurementType,
    // True: the pages show the chart; false: the list. The same for every tab.
    showChart: Boolean,
    // How far back the chart looks. Only used in chart mode.
    period: ChartPeriod,
    measurementToDelete: Measurement?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    // Called when another page is shown, by tapping a tab or by swiping.
    onSelectType: (MeasurementType) -> Unit,
    // Called by the button that switches between the list and the chart.
    onToggleChart: () -> Unit,
    onSelectPeriod: (ChartPeriod) -> Unit,
    onSetFilters: (MeasurementType, Set<ClinicalStageFilter>, Set<MeasurementConditionFilter>) -> Unit = { _, _, _ -> },
    onRemoveStageFilter: (MeasurementType, ClinicalStageFilter) -> Unit = { _, _ -> },
    onRemoveConditionFilter: (MeasurementType, MeasurementConditionFilter) -> Unit = { _, _ -> },
    onClearFilters: (MeasurementType) -> Unit = {},
    onOpenDetails: (Long) -> Unit = {},
    onRequestDelete: (Measurement) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    // The pages, in the same order as on the home screen.
    val types = MeasurementType.entries
    val pagerState = rememberPagerState(initialPage = types.indexOf(selectedType)) { types.size }
    val scope = rememberCoroutineScope()

    // The latest tap anywhere on the screen. The chart pages use it to close the box of
    // a tapped dot when the tap is outside the chart.
    var screenTap by remember { mutableStateOf<ScreenTap?>(null) }

    // Visibility state of the filter modal bottom sheet.
    var showFilterSheet by rememberSaveable { mutableStateOf(value = false) }

    // Reports the shown page once it has settled, whether it changed by swiping or by
    // tapping a tab. Not halfway through a swipe: the ViewModel only hears about it at
    // the end, so its update doesn't add work to the swipe.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            onSelectType(types[page])
        }
    }

    // The type whose page is shown. It changes halfway through a swipe.
    val shownType = types[pagerState.currentPage]

    // The whole screen takes the shown type's color. Each page below also has its own
    // type's color, so while swiping the two pages show their own colors side by side.
    TypeTheme(shownType) {
        Scaffold(
            modifier = Modifier.watchTaps { screenTap = ScreenTap(it) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back),
                                // Multicolored icon: keep its own colors.
                                tint = Color.Unspecified,
                            )
                        }
                    },
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // One tab per type (see TypeTabs).
                TypeTabs(
                    types = types,
                    selectedIndex = pagerState.currentPage,
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                )

                // Outside the pages, so it stays in place while swiping.
                ChartToggle(
                    showChart = showChart,
                    onToggle = onToggleChart,
                )

                // Only for the chart; the list shows the space-saving filter bar.
                if (showChart) {
                    PeriodChips(
                        selected = period,
                        onSelect = onSelectPeriod,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    val currentFilter = filterStateByType[shownType] ?: HistoryFilterState()
                    FilterBar(
                        filterState = currentFilter,
                        onOpenFilterSheet = { showFilterSheet = true },
                        onRemoveStage = { onRemoveStageFilter(shownType, it) },
                        onRemoveCondition = { onRemoveConditionFilter(shownType, it) },
                        onClearAll = { onClearFilters(shownType) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // Latest height is used to calculate healthy weight ranges and BMI target bands.
                val latestHeight = remember(measurementsByType) {
                    measurementsByType[MeasurementType.HEIGHT]?.firstOrNull()?.value
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val type = types[page]
                    val measurements = measurementsByType[type].orEmpty()
                    val filteredMeasurements = filteredMeasurementsByType[type].orEmpty()

                    TypeTheme(type) {
                        if (showChart) {
                            ChartPage(
                                type = type,
                                measurements = measurements,
                                period = period,
                                screenTap = screenTap,
                                latestHeight = latestHeight,
                                onOpenDetails = onOpenDetails,
                            )
                        } else {
                            EntryList(
                                measurements = filteredMeasurements,
                                onOpenDetails = onOpenDetails,
                                onRequestDelete = onRequestDelete,
                            )
                        }
                    }
                }
            }
        }

        // Filter modal bottom sheet for selecting clinical stages and physical conditions.
        if (showFilterSheet) {
            FilterBottomSheet(
                type = shownType,
                filterState = filterStateByType[shownType] ?: HistoryFilterState(),
                onApply = { stages, conditions ->
                    onSetFilters(shownType, stages, conditions)
                },
                onDismiss = { showFilterSheet = false },
            )
        }

        // Shown outside the list, so it doesn't depend on the card being visible.
        measurementToDelete?.let { measurement ->
            DeleteConfirmationDialog(
                details = stringResource(
                    R.string.delete_dialog_details,
                    stringResource(measurement.type.label),
                    formatMeasurementValue(measurement, decimalSeparator()),
                ),
                onConfirm = onConfirmDelete,
                onDismiss = onCancelDelete,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryContentPreview() {
    HealthLogTheme {
        HistoryContent(
            measurementsByType = mapOf(
                MeasurementType.BLOOD_PRESSURE to listOf(
                    Measurement(
                        id = 1,
                        type = MeasurementType.BLOOD_PRESSURE,
                        value = 120.0,
                        secondaryValue = 80.0,
                        tertiaryValue = 72.0,
                    ),
                    Measurement(
                        id = 2,
                        type = MeasurementType.BLOOD_PRESSURE,
                        value = 130.0,
                        secondaryValue = 85.0,
                        tertiaryValue = 75.0,
                    ),
                ),
            ),
            selectedType = MeasurementType.BLOOD_PRESSURE,
            showChart = false,
            period = ChartPeriod.ALL,
            measurementToDelete = null,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onSelectType = {},
            onToggleChart = {},
            onSelectPeriod = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {},
        )
    }
}
