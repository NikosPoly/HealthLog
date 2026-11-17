package com.example.healthlog.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MeasurementArm
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.history.chart.ChartPeriod
import com.example.healthlog.ui.history.filter.ClinicalStageFilter
import com.example.healthlog.ui.history.filter.DateFilter
import com.example.healthlog.ui.history.filter.HistoryFilterState
import com.example.healthlog.ui.history.filter.MeasurementConditionFilter
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented UI tests for Milestone 2.2 History Filtering:
// Tests the FilterBar, FilterBottomSheet, deferred apply, live counter, chip removal, and reset.
@RunWith(AndroidJUnit4::class)
class HistoryFilterUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // Sample data: Normal BP and Grade 1 Hypertension BP
    private val normalBp = Measurement(
        id = 1,
        type = MeasurementType.BLOOD_PRESSURE,
        value = 120.0,
        secondaryValue = 80.0,
        arm = MeasurementArm.LEFT.name,
        bodyPosition = BodyPosition.SITTING.name,
    )
    private val stage1Bp = Measurement(
        id = 2,
        type = MeasurementType.BLOOD_PRESSURE,
        value = 145.0,
        secondaryValue = 92.0,
        arm = MeasurementArm.RIGHT.name,
        bodyPosition = BodyPosition.SITTING.name,
    )

    private val sampleData = mapOf(
        MeasurementType.BLOOD_PRESSURE to listOf(normalBp, stage1Bp),
    )

    // Callbacks verification state
    private var appliedStages: Set<ClinicalStageFilter>? = null
    private var appliedConditions: Set<MeasurementConditionFilter>? = null
    private var appliedDateFilter: DateFilter? = null
    private var removedStage: ClinicalStageFilter? = null
    private var clearedType: MeasurementType? = null

    private fun openHistory(
        initialFilterState: HistoryFilterState = HistoryFilterState(),
    ) {
        composeRule.setContent {
            var filterState by remember { mutableStateOf(initialFilterState) }
            var showChart by remember { mutableStateOf(false) }

            HealthLogTheme {
                HistoryContent(
                    measurementsByType = sampleData,
                    filteredMeasurementsByType = sampleData,
                    filterStateByType = mapOf(MeasurementType.BLOOD_PRESSURE to filterState),
                    selectedType = MeasurementType.BLOOD_PRESSURE,
                    showChart = showChart,
                    period = ChartPeriod.ALL,
                    measurementToDelete = null,
                    snackbarHostState = remember { SnackbarHostState() },
                    onBack = {},
                    onSelectType = {},
                    onToggleChart = { showChart = !showChart },
                    onSelectPeriod = {},
                    onSetFilters = { _, stages, conditions, dateFilter ->
                        appliedStages = stages
                        appliedConditions = conditions
                        appliedDateFilter = dateFilter
                        filterState = filterState.copy(
                            stages = stages,
                            conditions = conditions,
                            dateFilter = dateFilter,
                        )
                    },
                    onRemoveStageFilter = { _, stage ->
                        removedStage = stage
                        filterState = filterState.copy(stages = filterState.stages - stage)
                    },
                    onRemoveConditionFilter = { _, condition ->
                        filterState = filterState.copy(conditions = filterState.conditions - condition)
                    },
                    onRemoveDateFilter = {
                        filterState = filterState.copy(dateFilter = null)
                    },
                    onClearFilters = { type ->
                        clearedType = type
                        filterState = HistoryFilterState()
                    },
                    onRequestDelete = {},
                    onConfirmDelete = {},
                    onCancelDelete = {},
                )
            }
        }
    }

    @Test
    fun filterBar_isDisplayedInListMode_andHiddenInChartMode() {
        openHistory()

        // In list mode, the Filter button is visible
        composeRule.onNodeWithText(context.getString(R.string.action_filter)).assertIsDisplayed()

        // Switch to chart mode
        composeRule.onNodeWithText(context.getString(R.string.history_show_chart)).performClick()
        composeRule.waitForIdle()

        // Filter button is not shown in chart mode
        composeRule.onNodeWithText(context.getString(R.string.action_filter)).assertDoesNotExist()

        // Switch back to list mode
        composeRule.onNodeWithText(context.getString(R.string.history_show_entries)).performClick()
        composeRule.waitForIdle()

        // Filter button is visible again
        composeRule.onNodeWithText(context.getString(R.string.action_filter)).assertIsDisplayed()
    }

    @Test
    fun filterButton_opensBottomSheet_withAllSections() {
        openHistory()

        // Open bottom sheet
        composeRule.onNodeWithText(context.getString(R.string.action_filter)).performClick()
        composeRule.waitForIdle()

        // Verify bottom sheet title and section headers
        composeRule.onNodeWithText(context.getString(R.string.filter_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.filter_section_stage)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.filter_section_conditions)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.filter_section_date)).assertIsDisplayed()

        // Verify initial live result count button: "Show all (2)"
        val expectedButtonText = context.getString(R.string.filter_show_all, 2)
        composeRule.onNodeWithText(expectedButtonText).assertIsDisplayed()
    }

    @Test
    fun bottomSheet_deferredApply_onlyAppliesOnButtonClick() {
        openHistory()

        // Open bottom sheet
        composeRule.onNodeWithText(context.getString(R.string.action_filter)).performClick()
        composeRule.waitForIdle()

        // Select "Grade 1 Hypertension" chip in bottom sheet
        val grade1Text = context.getString(R.string.bp_stage_grade_1)
        composeRule.onNodeWithText(grade1Text).performClick()
        composeRule.waitForIdle()

        // Verifying deferred apply: callback should NOT have fired yet!
        assertEquals(null, appliedStages)

        // Live counter button should now show 1 matching entry: "Show 1 entry"
        val expectedButtonText = context.resources.getQuantityString(R.plurals.filter_show_entries, 1, 1)
        composeRule.onNodeWithText(expectedButtonText).performClick()
        composeRule.waitForIdle()

        // Now callback must have been invoked with Grade 1
        assertNotNull(appliedStages)
        assertEquals(
            setOf(ClinicalStageFilter.BloodPressure(BloodPressureStage.GRADE_1_HYPERTENSION)),
            appliedStages,
        )
    }

    @Test
    fun activeFilters_appearInFilterBar_andCanBeRemovedIndividually() {
        val grade1Stage = ClinicalStageFilter.BloodPressure(BloodPressureStage.GRADE_1_HYPERTENSION)
        val initialFilter = HistoryFilterState(stages = setOf(grade1Stage))

        openHistory(initialFilterState = initialFilter)

        // Active stage chip should appear in FilterBar
        val grade1Label = context.getString(R.string.bp_stage_grade_1)
        composeRule.onNodeWithText(grade1Label).assertIsDisplayed()

        // Click the remove icon on the active stage chip
        val removeDesc = context.getString(R.string.filter_stage_chip_desc)
        composeRule.onNodeWithContentDescription(removeDesc).performClick()
        composeRule.waitForIdle()

        // Callback was triggered
        assertEquals(grade1Stage, removedStage)
    }

    @Test
    fun activeFilters_quickResetInFilterBar_clearsAllFilters() {
        val initialFilter = HistoryFilterState(
            stages = setOf(ClinicalStageFilter.BloodPressure(BloodPressureStage.NORMAL)),
        )

        openHistory(initialFilterState = initialFilter)

        // Quick reset button is displayed
        val resetText = context.getString(R.string.filter_reset)
        composeRule.onNodeWithText(resetText).performClick()
        composeRule.waitForIdle()

        // Callback was triggered for BLOOD_PRESSURE
        assertEquals(MeasurementType.BLOOD_PRESSURE, clearedType)
    }

    @Test
    fun bottomSheet_liveCounter_updatesDynamicallyAsSelectionsChange() {
        openHistory()

        // Open bottom sheet
        composeRule.onNodeWithText(context.getString(R.string.action_filter)).performClick()
        composeRule.waitForIdle()

        // Initially: "Show all (2)"
        val showAllText = context.getString(R.string.filter_show_all, 2)
        composeRule.onNodeWithText(showAllText).assertIsDisplayed()

        // Select "Normal" -> matching count is 1 -> "Show 1 entry"
        val normalText = context.getString(R.string.bp_stage_normal)
        composeRule.onNodeWithText(normalText).performClick()
        composeRule.waitForIdle()

        val show1Text = context.resources.getQuantityString(R.plurals.filter_show_entries, 1, 1)
        composeRule.onNodeWithText(show1Text).assertIsDisplayed()

        // Select "Grade 1 Hypertension" as well (multi-selection) -> count becomes 2 -> "Show 2 entries"
        val grade1Text = context.getString(R.string.bp_stage_grade_1)
        composeRule.onNodeWithText(grade1Text).performClick()
        composeRule.waitForIdle()

        val show2Text = context.resources.getQuantityString(R.plurals.filter_show_entries, 2, 2)
        composeRule.onNodeWithText(show2Text).assertIsDisplayed()

        // Select "All Stages" -> resets stage selections -> button returns to "Show all (2)"
        composeRule.onNodeWithText(context.getString(R.string.filter_all_stages)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(showAllText).assertIsDisplayed()
    }
}
