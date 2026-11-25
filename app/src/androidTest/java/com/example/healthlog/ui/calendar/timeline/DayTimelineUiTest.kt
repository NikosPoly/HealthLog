package com.example.healthlog.ui.calendar.timeline

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.model.ScheduledDoseStatus
import com.example.healthlog.ui.medications.MedicationActions
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class DayTimelineUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun dayTimelineContent_rendersHeaderAndSlotsCorrectly() {
        val testDate = LocalDate.of(2026, 11, 20)
        val todayDate = testDate.plusDays(1) // Different from testDate so "Today" button is visible
        var backClicked = false
        var prevDayClicked = false
        var nextDayClicked = false
        var jumpTodayClicked = false
        var selectedFilter: TimelineFilter? = null

        val slot8 = TimelineHourSlot(hour = 8, timeLabel = "08:00")
        val slot9 = TimelineHourSlot(
            hour = 9,
            timeLabel = "09:00",
            events = listOf(
                TimelineEvent.Vital(
                    measurement = Measurement(
                        id = 1L,
                        type = MeasurementType.BLOOD_PRESSURE,
                        value = 120.0,
                        secondaryValue = 80.0,
                        timestamp = 1763622000000L
                    ),
                    hour = 9,
                    minute = 0,
                    timeString = "09:00"
                )
            )
        )

        val slots = (0..23).map { hour ->
            when (hour) {
                8 -> slot8
                9 -> slot9
                else -> TimelineHourSlot(hour = hour, timeLabel = String.format("%02d:00", hour))
            }
        }

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = testDate,
                        hourSlots = slots,
                        totalEventCount = 1,
                        vitalsCount = 1
                    ),
                    onBack = { backClicked = true },
                    onPreviousDay = { prevDayClicked = true },
                    onNextDay = { nextDayClicked = true },
                    onJumpToToday = { jumpTodayClicked = true },
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
                    today = todayDate
                )
            }
        }

        // Title and Date
        composeRule.onNodeWithText(context.getString(R.string.day_timeline_title)).assertIsDisplayed()

        // Filter chips
        composeRule.onNodeWithText(context.getString(R.string.timeline_filter_all) + " (1)").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_legend_vitals) + " (1)").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_legend_meds) + " (0)").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_legend_symptoms) + " (0)").assertIsDisplayed()

        // Navigation controls
        composeRule.onNodeWithContentDescription(context.getString(R.string.day_timeline_prev_day)).performClick()
        assertTrue(prevDayClicked)

        composeRule.onNodeWithContentDescription(context.getString(R.string.day_timeline_next_day)).performClick()
        assertTrue(nextDayClicked)

        composeRule.onNodeWithText(context.getString(R.string.calendar_jump_today)).performClick()
        assertTrue(jumpTodayClicked)

        // Filter selection callback
        composeRule.onNodeWithText(context.getString(R.string.calendar_legend_vitals) + " (1)").performClick()
        assertEquals(TimelineFilter.VITALS, selectedFilter)
    }

    @Test
    fun dayTimelineContent_interactiveMedicationAndSymptomCards_triggerCallbacks() {
        val testDate = LocalDate.of(2026, 11, 20)
        var takenDose: ScheduledDoseItem? = null
        var skippedDose: ScheduledDoseItem? = null
        var undoneDose: ScheduledDoseItem? = null
        var movedDose: ScheduledDoseItem? = null
        var deletedDose: ScheduledDoseItem? = null
        var editedSymptom: SymptomEntry? = null
        var deletedSymptom: SymptomEntry? = null

        val pendingDose = ScheduledDoseItem(
            medicationId = 1L,
            medicationName = "Amlodipine",
            dosage = "5",
            unit = "mg",
            instructions = "Morning",
            scheduledDate = testDate.toString(),
            scheduledTime = "08:00",
            hour = 8,
            minute = 0,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 1L
        )

        val takenDoseItem = ScheduledDoseItem(
            medicationId = 2L,
            medicationName = "Metformin",
            dosage = "500",
            unit = "mg",
            instructions = null,
            scheduledDate = testDate.toString(),
            scheduledTime = "12:00",
            hour = 12,
            minute = 0,
            status = ScheduledDoseStatus.TAKEN,
            isRecurring = true,
            reminderId = 2L,
            doseLogId = 10L
        )

        val symptom = SymptomEntry(
            id = 5L,
            symptomType = "HEADACHE",
            customName = null,
            severity = 3,
            timestamp = 1763640000000L,
            notes = "Mild headache"
        )

        val slots = listOf(
            TimelineHourSlot(
                hour = 8,
                timeLabel = "08:00",
                events = listOf(TimelineEvent.Medication(pendingDose, 8, 0, "08:00"))
            ),
            TimelineHourSlot(
                hour = 12,
                timeLabel = "12:00",
                events = listOf(TimelineEvent.Medication(takenDoseItem, 12, 0, "12:00"))
            ),
            TimelineHourSlot(
                hour = 14,
                timeLabel = "14:00",
                events = listOf(TimelineEvent.Symptom(symptom, 14, 0, "14:00"))
            )
        )

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = testDate,
                        hourSlots = slots,
                        totalEventCount = 3,
                        medsCount = 2,
                        symptomsCount = 1
                    ),
                    onBack = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onJumpToToday = {},
                    onSelectFilter = {},
                    onMarkDoseTaken = { takenDose = it },
                    onMarkDoseSkipped = { skippedDose = it },
                    onUndoDose = { undoneDose = it },
                    onRequestMoveDose = { movedDose = it },
                    onRequestDeleteDose = { deletedDose = it },
                    onOpenEditSymptom = { editedSymptom = it },
                    onDismissAddEditSymptomSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = { deletedSymptom = it },
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    today = testDate
                )
            }
        }

        // Pending medication Take / Skip / Move / Delete
        composeRule.onNodeWithText("Amlodipine").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_taken)).performClick()
        assertEquals(pendingDose, takenDose)

        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_skipped)).performClick()
        assertEquals(pendingDose, skippedDose)

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.dose_action_move_time))[0].performClick()
        assertEquals(pendingDose, movedDose)

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.dose_action_delete))[0].performClick()
        assertEquals(pendingDose, deletedDose)

        // Taken medication Undo
        composeRule.onNodeWithText("Metformin").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_undo)).performClick()
        assertEquals(takenDoseItem, undoneDose)

        // Symptom Edit / Delete
        composeRule.onNodeWithText(context.getString(R.string.symptom_headache)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.symptom_action_edit)).performClick()
        assertEquals(symptom, editedSymptom)

        composeRule.onNodeWithContentDescription(context.getString(R.string.symptom_action_delete)).performClick()
        assertEquals(symptom, deletedSymptom)
    }

    @Test
    fun dayTimelineContent_slotChooserDialog_dispatchesSelection() {
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

    @Test
    fun dayTimelineContent_greekLocale_rendersTimelineStringsCorrectly() {
        val greekContext = InstrumentationRegistry.getInstrumentation().targetContext.createConfigurationContext(
            android.content.res.Configuration().apply {
                setLocale(Locale("el"))
            }
        )
        val testDate = LocalDate.of(2026, 11, 20)
        val dose = ScheduledDoseItem(
            medicationId = 15L,
            medicationName = "Aspirin",
            dosage = "100",
            unit = "mg",
            instructions = null,
            scheduledDate = testDate.toString(),
            scheduledTime = "12:00",
            hour = 12,
            minute = 0,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 1L
        )
        val slot = TimelineHourSlot(
            hour = 12,
            timeLabel = "12:00",
            events = listOf(TimelineEvent.Medication(dose, 12, 0, "12:00"))
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

        // Verify Greek localized strings render on timeline
        composeRule.onNodeWithText(greekContext.getString(R.string.day_timeline_title)).assertIsDisplayed()
        composeRule.onNodeWithText(greekContext.getString(R.string.dose_action_mark_taken)).assertIsDisplayed()
        composeRule.onNodeWithText(greekContext.getString(R.string.dose_action_mark_skipped)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(greekContext.getString(R.string.dose_action_move_time)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(greekContext.getString(R.string.dose_action_delete)).assertIsDisplayed()
    }

    @Test
    fun dayTimelineContent_pastUnloggedDose_rendersUnloggedBadgeAndActions() {
        val today = LocalDate.of(2026, 11, 20)
        val pastDate = LocalDate.of(2026, 11, 18)
        val unloggedDose = ScheduledDoseItem(
            medicationId = 20L,
            medicationName = "Amlodipine",
            dosage = "5",
            unit = "mg",
            instructions = null,
            scheduledDate = pastDate.toString(),
            scheduledTime = "08:00",
            hour = 8,
            minute = 0,
            status = ScheduledDoseStatus.UNLOGGED,
            isRecurring = false,
            canToggleStatus = true,
            doseLogId = 300L
        )
        val slot = TimelineHourSlot(
            hour = 8,
            timeLabel = "08:00",
            events = listOf(TimelineEvent.Medication(unloggedDose, 8, 0, "08:00"))
        )

        var takenDose: ScheduledDoseItem? = null
        var skippedDose: ScheduledDoseItem? = null

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = pastDate,
                        hourSlots = listOf(slot)
                    ),
                    onBack = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onJumpToToday = {},
                    onSelectFilter = {},
                    onMarkDoseTaken = { takenDose = it },
                    onMarkDoseSkipped = { skippedDose = it },
                    onUndoDose = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSymptomSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    today = today
                )
            }
        }

        composeRule.onNodeWithText("Amlodipine").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_status_unlogged)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_taken))
            .assertIsDisplayed()
            .performClick()
        assertEquals(unloggedDose, takenDose)

        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_skipped))
            .assertIsDisplayed()
            .performClick()
        assertEquals(unloggedDose, skippedDose)
    }

    @Test
    fun dayTimelineContent_pastScheduledTakenDose_rendersMarkSkippedSwitchWithoutUndo() {
        val today = LocalDate.of(2026, 11, 20)
        val pastDate = LocalDate.of(2026, 11, 18)
        val takenDose = ScheduledDoseItem(
            medicationId = 21L,
            medicationName = "Metformin",
            dosage = "500",
            unit = "mg",
            instructions = null,
            scheduledDate = pastDate.toString(),
            scheduledTime = "08:00",
            hour = 8,
            minute = 0,
            status = ScheduledDoseStatus.TAKEN,
            isRecurring = false,
            canToggleStatus = true,
            doseLogId = 301L
        )
        val slot = TimelineHourSlot(
            hour = 8,
            timeLabel = "08:00",
            events = listOf(TimelineEvent.Medication(takenDose, 8, 0, "08:00"))
        )

        var switchedToSkipped: ScheduledDoseItem? = null

        composeRule.setContent {
            HealthLogTheme {
                DayTimelineContent(
                    uiState = DayTimelineUiState(
                        selectedDate = pastDate,
                        hourSlots = listOf(slot)
                    ),
                    onBack = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onJumpToToday = {},
                    onSelectFilter = {},
                    onMarkDoseTaken = {},
                    onMarkDoseSkipped = { switchedToSkipped = it },
                    onUndoDose = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSymptomSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    today = today
                )
            }
        }

        composeRule.onNodeWithText("Metformin").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_undo)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_switch_to_skipped))
            .assertIsDisplayed()
            .performClick()
        assertEquals(takenDose, switchedToSkipped)
    }
}
