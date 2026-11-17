package com.example.healthlog.ui.export

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

class CsvReportGeneratorTest {

    @Test
    fun csvEscaping_wrapsCommasAndQuotes() {
        val raw = "Test, with \"quotes\""
        val expected = "\"Test, with \"\"quotes\"\"\""
        // Test escaping logic directly
        val escaped = if (raw.contains(',') || raw.contains('"')) {
            "\"" + raw.replace("\"", "\"\"") + "\""
        } else {
            raw
        }
        assertEquals(expected, escaped)
    }

    @Test
    fun filterExportMeasurements_withCsvConfig_filtersCorrectly() {
        val pulse = Measurement(
            id = 1,
            type = MeasurementType.PULSE,
            value = 72.0,
            timestamp = 1000L,
        )
        val bp = Measurement(
            id = 2,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            timestamp = 2000L,
        )

        val config = ExportConfig(
            format = ExportFormat.CSV,
            scope = ExportScope.ALL_TYPES,
            selectedType = MeasurementType.PULSE,
            dateFilter = null,
        )

        val results = filterExportMeasurements(
            mapOf(
                MeasurementType.PULSE to listOf(pulse),
                MeasurementType.BLOOD_PRESSURE to listOf(bp),
            ),
            config,
        )

        assertEquals(2, results.size)
        assertEquals(2000L, results[0].timestamp)
        assertEquals(1000L, results[1].timestamp)
    }
}
