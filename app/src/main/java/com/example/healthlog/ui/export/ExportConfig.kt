package com.example.healthlog.ui.export

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.history.filter.DateFilter

// The target file format for exporting medical records.
enum class ExportFormat {
    PDF,
    CSV,
}

// Whether to export only the currently viewed metric or all tracked vital signs.
enum class ExportScope {
    CURRENT_TYPE,
    ALL_TYPES,
}

// Configuration options selected by the user in the export dialog.
data class ExportConfig(
    val format: ExportFormat = ExportFormat.PDF,
    val scope: ExportScope = ExportScope.CURRENT_TYPE,
    val selectedType: MeasurementType,
    val dateFilter: DateFilter? = DateFilter.Last30Days,
)

// Filters measurements according to the selected export configuration.
fun filterExportMeasurements(
    measurementsByType: Map<MeasurementType, List<Measurement>>,
    config: ExportConfig,
): List<Measurement> {
    val typesToInclude = when (config.scope) {
        ExportScope.CURRENT_TYPE -> listOf(config.selectedType)
        ExportScope.ALL_TYPES -> MeasurementType.entries
    }

    return typesToInclude
        .flatMap { measurementsByType[it].orEmpty() }
        .filter { measurement ->
            config.dateFilter?.includes(measurement.timestamp) ?: true
        }
        .sortedByDescending { it.timestamp }
}
