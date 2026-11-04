package com.example.healthlog.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.history.chart.ChartPage
import com.example.healthlog.ui.history.chart.ChartPeriod
import com.example.healthlog.ui.history.chart.PeriodChips
import com.example.healthlog.ui.history.chart.ScreenTap
import com.example.healthlog.ui.history.chart.watchTaps
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.history.list.MeasurementHistoryList
import com.example.healthlog.ui.theme.HealthLogTheme
import kotlinx.coroutines.launch

// Layout only: the history screen, drawn from the values it receives;
// user actions are reported through the callbacks.
// One page per measurement type, chosen with the tabs or by swiping left and right.
// The button above the pages switches every page between the list and the chart.
// In chart mode, the chips below it choose the period of the chart.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    // The list of every type, newest first.
    measurementsByType: Map<MeasurementType, List<Measurement>>,
    // The type whose page is shown.
    selectedType: MeasurementType,
    // True: the pages show the chart; false: the list. The same for every tab.
    showTrend: Boolean,
    // How far back the chart looks. Only used in chart mode.
    period: ChartPeriod,
    measurementToDelete: Measurement?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    // Called when another page is shown, by tapping a tab or by swiping.
    onSelectType: (MeasurementType) -> Unit,
    // Called by the button that switches between the list and the chart.
    onToggleTrend: () -> Unit,
    onSelectPeriod: (ChartPeriod) -> Unit,
    onRequestDelete: (Measurement) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit
) {
    // The pages, in the same order as on the home screen.
    val types = MeasurementType.entries
    val pagerState = rememberPagerState(initialPage = types.indexOf(selectedType)) { types.size }
    val scope = rememberCoroutineScope()

    // The latest tap anywhere on the screen. The chart pages use it to close the box of
    // a tapped dot when the tap is outside the chart.
    var screenTap by remember { mutableStateOf<ScreenTap?>(null) }

    // Reports the shown page, whether it changed by swiping or by tapping a tab.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onSelectType(types[page])
        }
    }

    Scaffold(
        modifier = Modifier.watchTaps { screenTap = ScreenTap(it) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // One tab per type. The row scrolls sideways when the names don't fit.
            PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage,
                // No extra space before the first and after the last tab, so the row
                // starts at the left edge and stops at the first and last tab.
                edgePadding = 3.dp) {
                types.forEachIndexed { index, type ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(type.label)) }
                    )
                }
            }

            // Outside the pages, so it stays in place while swiping.
            TrendToggle(
                showTrend = showTrend,
                onToggle = onToggleTrend
            )

            // Only for the chart; the list always shows every measurement.
            if (showTrend) {
                PeriodChips(
                    selected = period,
                    onSelect = onSelectPeriod,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val type = types[page]
                val measurements = measurementsByType[type].orEmpty()

                if (showTrend) {
                    ChartPage(
                        type = type,
                        measurements = measurements,
                        period = period,
                        screenTap = screenTap
                    )
                } else {
                    MeasurementHistoryList(
                        measurements = measurements,
                        onRequestDelete = onRequestDelete
                    )
                }
            }
        }
    }

    // Shown outside the list, so it doesn't depend on the card being visible.
    measurementToDelete?.let { measurement ->
        DeleteConfirmationDialog(
            details = stringResource(
                R.string.delete_dialog_details,
                stringResource(measurement.type.label),
                formatMeasurementValue(measurement, decimalSeparator())
            ),
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete
        )
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
                        tertiaryValue = 72.0
                    ),
                    Measurement(
                        id = 2,
                        type = MeasurementType.BLOOD_PRESSURE,
                        value = 130.0,
                        secondaryValue = 85.0,
                        tertiaryValue = 75.0
                    )
                )
            ),
            selectedType = MeasurementType.BLOOD_PRESSURE,
            showTrend = false,
            period = ChartPeriod.ALL,
            measurementToDelete = null,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onSelectType = {},
            onToggleTrend = {},
            onSelectPeriod = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {}
        )
    }
}
