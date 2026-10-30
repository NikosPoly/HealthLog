package com.example.healthlog.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.chart.ChartPage
import com.example.healthlog.ui.components.DeleteConfirmationDialog
import com.example.healthlog.ui.components.MeasurementHistoryCard
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.theme.HealthLogTheme
import kotlinx.coroutines.launch

// Layout only: the history screen, drawn from the values it receives;
// user actions are reported through the callbacks.
// One page per measurement type, chosen with the tabs or by swiping left and right.
// The button above the pages switches every page between the list and the chart.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    // The list of every type, newest first.
    measurementsByType: Map<MeasurementType, List<Measurement>>,
    // The type whose page is shown.
    selectedType: MeasurementType,
    // True: the pages show the chart; false: the list. The same for every tab.
    showTrend: Boolean,
    measurementToDelete: Measurement?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    // Called when another page is shown, by tapping a tab or by swiping.
    onSelectType: (MeasurementType) -> Unit,
    // Called by the button that switches between the list and the chart.
    onToggleTrend: () -> Unit,
    onRequestDelete: (Measurement) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit
) {
    // The pages, in the same order as on the home screen.
    val types = MeasurementType.entries
    val pagerState = rememberPagerState(initialPage = types.indexOf(selectedType)) { types.size }
    val scope = rememberCoroutineScope()

    // Reports the shown page, whether it changed by swiping or by tapping a tab.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onSelectType(types[page])
        }
    }

    Scaffold(
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

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val type = types[page]

                if (showTrend) {
                    ChartPage(type = type)
                } else {
                    MeasurementList(
                        measurements = measurementsByType[type].orEmpty(),
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

// The button at the top center that switches between the list and the chart.
// It shows what it switches to: "See trend" on the list, "See history" on the chart.
@Composable
private fun TrendToggle(
    showTrend: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        OutlinedButton(onClick = onToggle) {
            Icon(
                painter = painterResource(
                    if (showTrend) R.drawable.ic_history else R.drawable.ic_trend
                ),
                // The text next to it already says what the button does.
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(
                stringResource(
                    if (showTrend) R.string.chart_show_history else R.string.chart_show_trend
                )
            )
        }
    }
}

// The list of one page: the cards of one type, or the empty text in the middle.
@Composable
private fun MeasurementList(
    measurements: List<Measurement>,
    onRequestDelete: (Measurement) -> Unit
) {
    if (measurements.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize()
            .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(stringResource(R.string.history_empty))
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 16.dp
        )
    ) {
        items(
            items = measurements,
            key = { it.id }
        ) { measurement ->
            MeasurementHistoryCard(
                measurement = measurement,
                onDelete = {
                    onRequestDelete(measurement)
                }
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
            measurementToDelete = null,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onSelectType = {},
            onToggleTrend = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {}
        )
    }
}
