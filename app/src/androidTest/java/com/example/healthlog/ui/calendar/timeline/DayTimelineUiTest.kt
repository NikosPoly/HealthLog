package com.example.healthlog.ui.calendar.timeline

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.model.ScheduledDoseStatus
import com.example.healthlog.model.SymptomType
import com.example.healthlog.ui.medications.MedicationActions
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DayTimelineUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun dayTimelineContent_rendersHeaderFiltersAndHourlySlots() {
        val testDate = LocalDate.of(2026, 11, 20)
        var backClicked = false
        var nextDayClicked = false
        var prevDayClicked = false
        var selectedFilter: TimelineFilter? = null
        var clickedEmptyHour: Int? = null

        val emptySlots = (0..23).map { hour ->
            TimelineHourSlot(
                hour = hour,
                timeLabel = String.format("%02d:00", hour),
                events = emptyList()
            )
        }

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = testDate,
                        hourSlots = emptySlots
                    ),
                    onBack = { backClicked = true },
                    onPreviousDay = { prevDayClicked = true },
                    onNextDay = { nextDayClicked = true },
                    onJumpToToday = {},
                    onSelectFilter = { selectedFilter = it },
                    onMarkDoseTaken = {},
                    onMarkDoseSkipped = {},
                    onUndoDose = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSymptomSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    onEmptyHourClick = { clickedEmptyHour = it },
                    today = testDate
                )
            }
        }

        // Title and Back button
        composeRule.onNodeWithText(context.getString(R.string.day_timeline_title)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.action_back)).performClick()
        assertTrue(backClicked)

        // Day navigation
        composeRule.onNodeWithContentDescription(context.getString(R.string.day_timeline_prev_day)).performClick()
        assertTrue(prevDayClicked)
        composeRule.onNodeWithContentDescription(context.getString(R.string.day_timeline_next_day)).performClick()
        assertTrue(nextDayClicked)

        // Filter chips
        composeRule.onNodeWithText("${context.getString(R.string.calendar_legend_vitals)} (0)").performClick()
        assertEquals(TimelineFilter.VITALS, selectedFilter)

        // Click empty slot 10:00
        composeRule.onNodeWithTag("day_timeline_list")
            .performScrollToNode(hasTestTag("timeline_empty_slot_10"))
        composeRule.onNodeWithTag("timeline_empty_slot_10").performClick()
        assertEquals(10, clickedEmptyHour)
    }

    @Test
    fun dayTimelineContent_rendersEvents_interactsWithActionsAndVitalsClick() {
        val testDate = LocalDate.of(2026, 11, 20)
        var openedMeasurementId: Long? = null
        var takenDose: ScheduledDoseItem? = null
        var skippedDose: ScheduledDoseItem? = null
        var movedDoseRequest: ScheduledDoseItem? = null
        var deletedDoseRequest: ScheduledDoseItem? = null
        var editedSymptom: SymptomEntry? = null

        val measurement = Measurement(
            id = 55L,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 125.0,
            secondaryValue = 82.0,
            tertiaryValue = 72.0,
            timestamp = 1000L
        )
        val dose = ScheduledDoseItem(
            medicationId = 10L,
            medicationName = "Amlodipine",
            dosage = "5",
            unit = "mg",
            instructions = "Morning",
            scheduledDate = testDate.toString(),
            scheduledTime = "08:30",
            hour = 8,
            minute = 30,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 1L
        )
        val symptom = SymptomEntry(
            id = 20L,
            symptomType = SymptomType.HEADACHE.name,
            severity = 3,
            timestamp = 2000L,
            notes = "Mild headache"
        )

        val slots = (0..23).map { hour ->
            when (hour) {
                8 -> TimelineHourSlot(
                    hour = 8,
                    timeLabel = "08:00",
                    events = listOf(
                        TimelineEvent.Vital(measurement, 8, 15, "08:15"),
                        TimelineEvent.Medication(dose, 8, 30, "08:30")
                    )
                )
                14 -> TimelineHourSlot(
                    hour = 14,
                    timeLabel = "14:00",
                    events = listOf(
                        TimelineEvent.Symptom(symptom, 14, 45, "14:45")
                    )
                )
                else -> TimelineHourSlot(hour = hour, timeLabel = String.format("%02d:00", hour))
            }
        }

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = testDate,
                        hourSlots = slots,
                        totalEventCount = 3,
                        vitalsCount = 1,
                        medsCount = 1,
                        symptomsCount = 1
                    ),
                    onBack = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onJumpToToday = {},
                    onSelectFilter = {},
                    onMarkDoseTaken = { takenDose = it },
                    onMarkDoseSkipped = { skippedDose = it },
                    onUndoDose = {},
                    onRequestMoveDose = { movedDoseRequest = it },
                    onRequestDeleteDose = { deletedDoseRequest = it },
                    onOpenEditSymptom = { editedSymptom = it },
                    onDismissAddEditSymptomSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = { openedMeasurementId = it },
                    today = testDate
                )
            }
        }

        // Tap Vital card opens measurement details
        val bpText = "125/82 mmHg · 72 bpm"
        composeRule.onNodeWithTag("day_timeline_list").performScrollToNode(hasText(bpText))
        composeRule.onNodeWithText(bpText).assertIsDisplayed().performClick()
        assertEquals(55L, openedMeasurementId)

        // Medication Take, Skip, Move, and Delete actions
        composeRule.onNodeWithTag("day_timeline_list").performScrollToNode(hasText("Amlodipine"))
        composeRule.onNodeWithText("Amlodipine").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_taken)).performClick()
        assertEquals(dose, takenDose)
        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_skipped)).performClick()
        assertEquals(dose, skippedDose)
        composeRule.onNodeWithContentDescription(context.getString(R.string.dose_action_move_time)).performClick()
        assertEquals(dose, movedDoseRequest)
        composeRule.onNodeWithContentDescription(context.getString(R.string.dose_action_delete)).performClick()
        assertEquals(dose, deletedDoseRequest)

        // Edit symptom action
        composeRule.onNodeWithTag("day_timeline_list").performScrollToNode(hasText(context.getString(R.string.symptom_headache)))
        composeRule.onNodeWithText(context.getString(R.string.symptom_headache)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.symptom_action_edit)).performClick()
        assertEquals(symptom, editedSymptom)
    }

    @Test
    fun dayTimelineContent_slotChooserDialog_triggersSymptomAndMedicationActions() {
        val testDate = LocalDate.of(2026, 11, 20)
        var choseSymptom = false
        var choseMedication = false

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = testDate,
                        slotChooserHour = 14
                    ),
                    onBack = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onJumpToToday = {},
                    onSelectFilter = {},
                    onMarkDoseTaken = {},
                    onMarkDoseSkipped = {},
                    onUndoDose = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSymptomSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    onChooseLogSymptomFromSlot = { choseSymptom = true },
                    onChooseAddMedicationFromSlot = { choseMedication = true },
                    today = testDate
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.day_timeline_slot_chooser_title, "14:00")).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.day_timeline_slot_chooser_symptom)).performClick()
        assertTrue(choseSymptom)
        composeRule.onNodeWithText(context.getString(R.string.day_timeline_slot_chooser_medication)).performClick()
        assertTrue(choseMedication)
    }

    @Test
    fun dayTimelineContent_moveAndDeleteScopeDialogs_dispatchSelectedScope() {
        val testDate = LocalDate.of(2026, 11, 20)
        val recurringDose = ScheduledDoseItem(
            medicationId = 10L,
            medicationName = "Amlodipine",
            dosage = "5",
            unit = "mg",
            instructions = null,
            scheduledDate = testDate.toString(),
            scheduledTime = "08:00",
            hour = 8,
            minute = 0,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 1L
        )
        var moveScope: MedicationActions.RecurringDoseScope? = null

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = testDate,
                        pendingDoseMove = PendingDoseMove(recurringDose, 10, 30)
                    ),
                    onBack = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onJumpToToday = {},
                    onSelectFilter = {},
                    onMarkDoseTaken = {},
                    onMarkDoseSkipped = {},
                    onUndoDose = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSymptomSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    onConfirmMoveScope = { moveScope = it },
                    today = testDate
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.dose_move_scope_title, "10:30")).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_scope_this_day)).performClick()
        assertEquals(MedicationActions.RecurringDoseScope.THIS_DAY_ONLY, moveScope)
        composeRule.onNodeWithText(context.getString(R.string.dose_scope_this_and_future)).performClick()
        assertEquals(MedicationActions.RecurringDoseScope.THIS_AND_FUTURE, moveScope)
    }

    @Test
    fun dayTimelineContent_deleteRecurringScopeDialog_dispatchesSelectedScope() {
        val testDate = LocalDate.of(2026, 11, 20)
        val recurringDose = ScheduledDoseItem(
            medicationId = 10L,
            medicationName = "Amlodipine",
            dosage = "5",
            unit = "mg",
            instructions = null,
            scheduledDate = testDate.toString(),
            scheduledTime = "08:00",
            hour = 8,
            minute = 0,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 1L
        )
        var deleteScope: MedicationActions.RecurringDoseScope? = null

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = testDate,
                        doseToDelete = recurringDose
                    ),
                    onBack = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onJumpToToday = {},
                    onSelectFilter = {},
                    onMarkDoseTaken = {},
                    onMarkDoseSkipped = {},
                    onUndoDose = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSymptomSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    onConfirmDeleteDose = { deleteScope = it },
                    today = testDate
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.dose_delete_scope_title, "Amlodipine")).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_scope_this_day)).performClick()
        assertEquals(MedicationActions.RecurringDoseScope.THIS_DAY_ONLY, deleteScope)
        composeRule.onNodeWithText(context.getString(R.string.dose_scope_this_and_future)).performClick()
        assertEquals(MedicationActions.RecurringDoseScope.THIS_AND_FUTURE, deleteScope)
    }

    @Test
    fun dayTimelineContent_oneTimeDose_doesNotShowUndoButton() {
        val testDate = LocalDate.of(2026, 11, 20)
        val oneTimeDose = ScheduledDoseItem(
            medicationId = 15L,
            medicationName = "Aspirin",
            dosage = "100",
            unit = "mg",
            instructions = null,
            scheduledDate = testDate.toString(),
            scheduledTime = "12:00",
            hour = 12,
            minute = 0,
            status = ScheduledDoseStatus.TAKEN,
            isRecurring = false,
            reminderId = null,
            doseLogId = 99L
        )
        val slot = TimelineHourSlot(
            hour = 12,
            timeLabel = "12:00",
            events = listOf(TimelineEvent.Medication(oneTimeDose, 12, 0, "12:00"))
        )

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = testDate,
                        hourSlots = listOf(slot)
                    ),
                    onBack = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onJumpToToday = {},
                    onSelectFilter = {},
                    onMarkDoseTaken = {},
                    onMarkDoseSkipped = {},
                    onUndoDose = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSymptomSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    today = testDate
                )
            }
        }

        composeRule.onNodeWithText("Aspirin").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_status_taken)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.dose_action_delete)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_undo)).assertDoesNotExist()
    }
}
