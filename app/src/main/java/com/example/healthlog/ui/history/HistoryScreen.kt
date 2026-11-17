package com.example.healthlog.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.example.healthlog.R
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.export.CsvReportGenerator
import com.example.healthlog.ui.export.ExportConfig
import com.example.healthlog.ui.export.ExportFileManager
import com.example.healthlog.ui.export.ExportFormat
import com.example.healthlog.ui.export.PdfReportGenerator
import com.example.healthlog.ui.export.filterExportMeasurements
import com.example.healthlog.ui.history.chart.ChartPeriod
import kotlinx.coroutines.launch
import java.io.File

// Connector: reads the HistoryViewModel's state, shows its messages,
// and hands everything to the layout as plain values and callbacks.
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    // The type whose page is shown, and the callback when another page is shown.
    // Kept by HealthLogNavHost, so it is remembered when the history is opened again.
    selectedType: MeasurementType,
    onSelectType: (MeasurementType) -> Unit,
    onOpenDetails: (Long) -> Unit = {},
    onExport: (ExportConfig) -> Unit = {},
    onBack: () -> Unit,
) {
    // The raw list of every type, for charts.
    val measurementsByType = MeasurementType.entries.associateWith { type ->
        viewModel.measurementsOf(type).collectAsState().value
    }
    // The filtered list of every type, for the entry list.
    val filteredMeasurementsByType = MeasurementType.entries.associateWith { type ->
        viewModel.filteredMeasurementsOf(type).collectAsState().value
    }
    // Filter criteria per type.
    val filterStateByType = MeasurementType.entries.associateWith { type ->
        viewModel.filterStateOf(type).collectAsState().value
    }

    val measurementToDelete by viewModel.measurementToDelete.collectAsState()

    // List or chart. Kept here, inside the history screen: it stays while the history
    // is open (also on rotation and when changing tabs), and the history always opens
    // on the list.
    var showChart by rememberSaveable { mutableStateOf(value = false) }

    // The period of the chart. All when the history opens; back to All when another tab
    // is shown (selectedType is its key), but kept on rotation.
    var period by rememberSaveable(selectedType) { mutableStateOf(ChartPeriod.ALL) }

    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val latestHeight by viewModel.latestHeight.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { messageId ->
            snackbarHostState.showSnackbar(resources.getString(messageId))
        }
    }

    HistoryContent(
        measurementsByType = measurementsByType,
        filteredMeasurementsByType = filteredMeasurementsByType,
        filterStateByType = filterStateByType,
        selectedType = selectedType,
        showChart = showChart,
        period = period,
        measurementToDelete = measurementToDelete,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelectType = onSelectType,
        onToggleChart = { showChart = !showChart },
        onSelectPeriod = { period = it },
        onSetFilters = viewModel::setFilters,
        onRemoveStageFilter = viewModel::removeStageFilter,
        onRemoveConditionFilter = viewModel::removeConditionFilter,
        onRemoveDateFilter = viewModel::clearDateFilter,
        onClearFilters = viewModel::clearFilters,
        onOpenDetails = onOpenDetails,
        onExport = { config ->
            val measurements = filterExportMeasurements(measurementsByType, config)
            if (measurements.isNotEmpty()) {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val timestamp = System.currentTimeMillis()

                val (exportFile, mimeType, successResId) = if (config.format == ExportFormat.PDF) {
                    val file = File(exportDir, "HealthLog_Report_$timestamp.pdf")
                    PdfReportGenerator(context).generateReport(
                        config = config,
                        measurements = measurements,
                        outputFile = file,
                        latestHeightCm = latestHeight,
                    )
                    Triple(file, "application/pdf", R.string.export_pdf_saved_to_downloads)
                } else {
                    val file = File(exportDir, "HealthLog_Data_$timestamp.csv")
                    CsvReportGenerator(context).generateReport(
                        measurements = measurements,
                        outputFile = file,
                        latestHeightCm = latestHeight,
                    )
                    Triple(file, "text/csv", R.string.export_csv_saved_to_downloads)
                }

                val uri = ExportFileManager.saveToDownloads(
                    context = context,
                    file = exportFile,
                    mimeType = mimeType,
                )
                scope.launch {
                    val message = if (uri != null) {
                        resources.getString(successResId)
                    } else {
                        resources.getString(R.string.export_save_failed)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        resources.getString(R.string.export_no_entries),
                    )
                }
            }
            onExport(config)
        },
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
    )
}
