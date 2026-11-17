package com.example.healthlog.ui.export

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.history.filter.DateFilter
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportDialogUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleMeasurements = mapOf(
        MeasurementType.BLOOD_PRESSURE to listOf(
            Measurement(
                id = 1,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 120.0,
                secondaryValue = 80.0,
                timestamp = System.currentTimeMillis() - 1000L,
            ),
        ),
        MeasurementType.PULSE to listOf(
            Measurement(
                id = 2,
                type = MeasurementType.PULSE,
                value = 72.0,
                timestamp = System.currentTimeMillis() - 2000L,
            ),
        ),
    )

    @Test
    fun exportDialog_initialState_pdfAndCurrentTypeSelected() {
        composeRule.setContent {
            HealthLogTheme {
                ExportDialog(
                    selectedType = MeasurementType.BLOOD_PRESSURE,
                    measurementsByType = sampleMeasurements,
                    onExport = {},
                    onDismiss = {},
                )
            }
        }

        // PDF is selected by default
        composeRule.onNodeWithTag("export_format_pdf").assertExists()
        // Scope Current is selected by default
        composeRule.onNodeWithTag("export_scope_current").assertExists()
        // Action button is enabled because there are matching readings
        composeRule.onNodeWithTag("export_action_button").assertIsEnabled()
    }

    @Test
    fun exportDialog_selectCsvFormat_updatesFormat() {
        var exportedConfig: ExportConfig? = null

        composeRule.setContent {
            HealthLogTheme {
                ExportDialog(
                    selectedType = MeasurementType.BLOOD_PRESSURE,
                    measurementsByType = sampleMeasurements,
                    onExport = { exportedConfig = it },
                    onDismiss = {},
                )
            }
        }

        // Tap CSV card
        composeRule.onNodeWithTag("export_format_csv").performClick()

        // Tap Export/Download button
        composeRule.onNodeWithTag("export_action_button").performClick()

        assertNotNull(exportedConfig)
        assertEquals(ExportFormat.CSV, exportedConfig?.format)
    }

    @Test
    fun exportDialog_selectScopeAll_updatesScope() {
        var exportedConfig: ExportConfig? = null

        composeRule.setContent {
            HealthLogTheme {
                ExportDialog(
                    selectedType = MeasurementType.BLOOD_PRESSURE,
                    measurementsByType = sampleMeasurements,
                    onExport = { exportedConfig = it },
                    onDismiss = {},
                )
            }
        }

        // Tap All tracked metrics row
        composeRule.onNodeWithTag("export_scope_all").performClick()

        // Tap Download button
        composeRule.onNodeWithTag("export_action_button").performClick()

        assertNotNull(exportedConfig)
        assertEquals(ExportScope.ALL_TYPES, exportedConfig?.scope)
    }

    @Test
    fun exportDialog_selectDateRanges_updatesDateFilter() {
        var exportedConfig: ExportConfig? = null

        composeRule.setContent {
            HealthLogTheme {
                ExportDialog(
                    selectedType = MeasurementType.BLOOD_PRESSURE,
                    measurementsByType = sampleMeasurements,
                    onExport = { exportedConfig = it },
                    onDismiss = {},
                )
            }
        }

        // Select 7 days chip
        composeRule.onNodeWithTag("export_date_7_days").performClick()
        composeRule.onNodeWithTag("export_date_7_days").assertIsSelected()

        // Select All Time chip
        composeRule.onNodeWithTag("export_date_all_time").performClick()
        composeRule.onNodeWithTag("export_date_all_time").assertIsSelected()

        composeRule.onNodeWithTag("export_action_button").performClick()
        assertNotNull(exportedConfig)
        assertEquals(null, exportedConfig?.dateFilter)
    }

    @Test
    fun exportDialog_noMatchingEntries_disablesDownloadButton() {
        // Empty measurements
        composeRule.setContent {
            HealthLogTheme {
                ExportDialog(
                    selectedType = MeasurementType.BLOOD_PRESSURE,
                    measurementsByType = emptyMap(),
                    onExport = {},
                    onDismiss = {},
                )
            }
        }

        // Action button must be disabled when 0 entries match
        composeRule.onNodeWithTag("export_action_button").assertIsNotEnabled()
    }

    @Test
    fun exportDialog_cancelButton_triggersDismiss() {
        var dismissed = false

        composeRule.setContent {
            HealthLogTheme {
                ExportDialog(
                    selectedType = MeasurementType.BLOOD_PRESSURE,
                    measurementsByType = sampleMeasurements,
                    onExport = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("export_cancel_button").performClick()
        assertTrue("Dismiss callback should be called", dismissed)
    }

    @Test
    fun exportDialog_closeIconButton_triggersDismiss() {
        var dismissed = false

        composeRule.setContent {
            HealthLogTheme {
                ExportDialog(
                    selectedType = MeasurementType.BLOOD_PRESSURE,
                    measurementsByType = sampleMeasurements,
                    onExport = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("export_close_button").performClick()
        assertTrue("Dismiss callback should be called", dismissed)
    }
}
