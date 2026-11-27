package com.example.healthlog.ui.export

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.history.filter.DateFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExportConfigTest {

    @Test
    fun filterExportMeasurements_currentType_filtersOnlySelectedMetric() {
        val bp1 = Measurement(id = 1, type = MeasurementType.BLOOD_PRESSURE, value = 120.0, timestamp = 1000L)
        val pulse1 = Measurement(id = 2, type = MeasurementType.PULSE, value = 75.0, timestamp = 2000L)

        val measurementsByType = mapOf(
            MeasurementType.BLOOD_PRESSURE to listOf(bp1),
            MeasurementType.PULSE to listOf(pulse1),
        )

        val config = ExportConfig(
            format = ExportFormat.PDF,
            scope = ExportScope.CURRENT_TYPE,
            selectedType = MeasurementType.BLOOD_PRESSURE,
            dateFilter = null,
        )

        val result = filterExportMeasurements(measurementsByType, config)
        assertEquals(1, result.size)
        assertEquals(MeasurementType.BLOOD_PRESSURE, result[0].type)
    }

    @Test
    fun filterExportMeasurements_allTypes_includesAllMetricsSortedNewestFirst() {
        val bp = Measurement(id = 1, type = MeasurementType.BLOOD_PRESSURE, value = 120.0, timestamp = 1000L)
        val pulse = Measurement(id = 2, type = MeasurementType.PULSE, value = 75.0, timestamp = 3000L)
        val glucose = Measurement(id = 3, type = MeasurementType.BLOOD_SUGAR, value = 95.0, timestamp = 2000L)

        val measurementsByType = mapOf(
            MeasurementType.BLOOD_PRESSURE to listOf(bp),
            MeasurementType.PULSE to listOf(pulse),
            MeasurementType.BLOOD_SUGAR to listOf(glucose),
        )

        val config = ExportConfig(
            format = ExportFormat.CSV,
            scope = ExportScope.ALL_TYPES,
            selectedType = MeasurementType.BLOOD_PRESSURE,
            dateFilter = null,
        )

        val result = filterExportMeasurements(measurementsByType, config)
        assertEquals(3, result.size)
        assertEquals(3000L, result[0].timestamp)
        assertEquals(2000L, result[1].timestamp)
        assertEquals(1000L, result[2].timestamp)
    }

    @Test
    fun filterExportMeasurements_withCustomDateFilter_excludesOutOfRange() {
        val inRangeDate = LocalDate.of(2026, 11, 10)
        val outOfRangeDate = LocalDate.of(2026, 10, 1)

        val zone = java.time.ZoneId.systemDefault()
        val inRangeMillis = inRangeDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val outOfRangeMillis = outOfRangeDate.atStartOfDay(zone).toInstant().toEpochMilli()

        val m1 = Measurement(id = 1, type = MeasurementType.BLOOD_PRESSURE, value = 120.0, timestamp = inRangeMillis)
        val m2 = Measurement(id = 2, type = MeasurementType.BLOOD_PRESSURE, value = 130.0, timestamp = outOfRangeMillis)

        val measurementsByType = mapOf(
            MeasurementType.BLOOD_PRESSURE to listOf(m1, m2),
        )

        val config = ExportConfig(
            format = ExportFormat.PDF,
            scope = ExportScope.CURRENT_TYPE,
            selectedType = MeasurementType.BLOOD_PRESSURE,
            dateFilter = DateFilter.CustomRange(
                startDate = LocalDate.of(2026, 11, 1),
                endDate = LocalDate.of(2026, 11, 15),
            ),
        )

        val result = filterExportMeasurements(measurementsByType, config)
        assertEquals(1, result.size)
        assertEquals(inRangeMillis, result[0].timestamp)
    }

    @Test
    fun exportConfig_defaultsAreClinicallySensible() {
        val config = ExportConfig(selectedType = MeasurementType.BLOOD_PRESSURE)
        assertEquals(ExportFormat.PDF, config.format)
        assertEquals(ExportScope.CURRENT_TYPE, config.scope)
        assertEquals(DateFilter.Last30Days, config.dateFilter)
        assertTrue(config.includeProfileDetails)
    }
}
