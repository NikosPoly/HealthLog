package com.example.healthlog.ui.export

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportConfigPdfTest {

    @Test
    fun filterExportMeasurements_withPdfConfig_correctlyFiltersAndOrders() {
        val bp1 = Measurement(
            id = 1,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            timestamp = 1000L,
        )
        val bp2 = Measurement(
            id = 2,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 140.0,
            secondaryValue = 90.0,
            timestamp = 2000L,
        )

        val config = ExportConfig(
            format = ExportFormat.PDF,
            scope = ExportScope.CURRENT_TYPE,
            selectedType = MeasurementType.BLOOD_PRESSURE,
            dateFilter = null,
        )

        val filtered = filterExportMeasurements(
            mapOf(MeasurementType.BLOOD_PRESSURE to listOf(bp1, bp2)),
            config,
        )

        assertEquals(2, filtered.size)
        // Ordered newest first
        assertEquals(2000L, filtered[0].timestamp)
        assertEquals(1000L, filtered[1].timestamp)
    }

    @Test
    fun exportFormat_pdfIsDefaultFormat() {
        val config = ExportConfig(selectedType = MeasurementType.BLOOD_PRESSURE)
        assertEquals(ExportFormat.PDF, config.format)
    }
}
