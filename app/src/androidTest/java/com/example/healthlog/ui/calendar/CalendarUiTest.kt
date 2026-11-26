package com.example.healthlog.ui.calendar

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.model.ScheduledDoseStatus
import com.example.healthlog.model.SymptomSeverity
import com.example.healthlog.model.SymptomType
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CalendarUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val formatLocale: Locale
        get() = Locale.forLanguageTag(context.getString(R.string.format_locale))

    @Test
    fun calendarContent_emptyDay_rendersHeaderLegendAndEmptyState() {
        val month = YearMonth.of(2026, 11)
        val selectedDate = LocalDate.of(2026, 11, 10)
        var jumpTodayClicked = false
        var addSymptomClicked = false

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = selectedDate,
                        daySummaries = emptyMap()
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = { jumpTodayClicked = true },
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = { addSymptomClicked = true },
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {}
                )
            }
        }

        // Top bar title and Today button
        composeRule.onNodeWithText(context.getString(R.string.calendar_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_jump_today))
            .assertIsDisplayed()
            .performClick()
        assertTrue(jumpTodayClicked)

        // Legend items
        composeRule.onNodeWithText(context.getString(R.string.calendar_legend_vitals)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_legend_symptoms)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_legend_meds)).assertIsDisplayed()

        // Empty day message
        val emptyText = context.getString(R.string.calendar_day_empty)
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText(emptyText))
        composeRule.onNodeWithText(emptyText).assertIsDisplayed()

        // FAB triggers add symptom
        composeRule.onNodeWithContentDescription(context.getString(R.string.calendar_action_log_symptom))
            .performClick()
        assertTrue(addSymptomClicked)
    }

    @Test
    fun calendarContent_monthNavigationAndMonthYearPicker_triggerCallbacks() {
        val initialMonth = YearMonth.of(2026, 11)
        val selectedDate = LocalDate.of(2026, 11, 10)
        var prevClicked = false
        var nextClicked = false
        var pickedMonthYear: YearMonth? = null

        val locale = formatLocale
        val expectedMonthTitle = initialMonth.format(
            DateTimeFormatter.ofPattern("LLLL yyyy", locale)
        ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

        val targetMonthLabel = Month.of(3)
            .getDisplayName(TextStyle.SHORT, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = initialMonth,
                        selectedDate = selectedDate,
                        daySummaries = emptyMap()
                    ),
                    onPreviousMonth = { prevClicked = true },
                    onNextMonth = { nextClicked = true },
                    onJumpToToday = {},
                    onSelectMonthYear = { pickedMonthYear = it },
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {}
                )
            }
        }

        // Previous / Next month chevron clicks
        composeRule.onNodeWithContentDescription(context.getString(R.string.calendar_prev_month)).performClick()
        assertTrue(prevClicked)

        composeRule.onNodeWithContentDescription(context.getString(R.string.calendar_next_month)).performClick()
        assertTrue(nextClicked)

        // Open Month & Year picker by clicking the month title
        composeRule.onNodeWithText(expectedMonthTitle).performClick()
        composeRule.onNodeWithText(context.getString(R.string.calendar_select_month_year)).assertIsDisplayed()

        // Advance year from 2026 -> 2027 and pick March
        composeRule.onNodeWithContentDescription(context.getString(R.string.calendar_next_year)).performClick()
        composeRule.onNodeWithText("2027").assertIsDisplayed()
        composeRule.onNodeWithText(targetMonthLabel).performClick()

        // Confirm selection
        composeRule.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        assertEquals(YearMonth.of(2027, 3), pickedMonthYear)
    }

    @Test
    fun calendarContent_daySelectionAndUnifiedDetails_renderAndHandleInteractions() {
        val month = YearMonth.of(2026, 11)
        val selectedDate = LocalDate.of(2026, 11, 10)
        val dayTimestamp = selectedDate.atTime(9, 30)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val symptom = SymptomEntry(
            id = 5L,
            symptomType = SymptomType.HEADACHE.name,
            severity = 4,
            timestamp = dayTimestamp,
            notes = "After work"
        )
        val measurement = Measurement(
            id = 42L,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 128.0,
            secondaryValue = 82.0,
            tertiaryValue = 72.0,
            timestamp = dayTimestamp
        )
        val dose = ScheduledDoseItem(
            medicationId = 1L,
            medicationName = "Amlodipine",
            dosage = "5",
            unit = "mg",
            instructions = "With breakfast",
            scheduledDate = "2026-11-10",
            scheduledTime = "08:00",
            hour = 8,
            minute = 0,
            status = ScheduledDoseStatus.TAKEN,
            isRecurring = true,
            reminderId = 10L,
            doseLogId = 7L
        )

        val summary = CalendarDaySummary(
            date = selectedDate,
            measurements = listOf(measurement),
            scheduledDoses = listOf(dose),
            symptoms = listOf(symptom),
            highestVitalRank = VitalClinicalRank.NORMAL,
            maxSymptomSeverity = SymptomSeverity.SEVERE
        )

        var newlySelectedDate: LocalDate? = null
        var openedMeasurementId: Long? = null
        var editedSymptom: SymptomEntry? = null
        var deletedSymptomRequest: SymptomEntry? = null
        var undoneDose: ScheduledDoseItem? = null

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = selectedDate,
                        daySummaries = mapOf(selectedDate to summary)
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = { newlySelectedDate = it },
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = { editedSymptom = it },
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = { deletedSymptomRequest = it },
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = { openedMeasurementId = it },
                    onUndoDoseAction = { undoneDose = it },
                    today = selectedDate
                )
            }
        }

        // Tap another day in the grid (Nov 15)
        composeRule.onNodeWithText("15").performClick()
        assertEquals(LocalDate.of(2026, 11, 15), newlySelectedDate)

        // Verify Symptoms section & actions
        val headacheLabel = context.getString(R.string.symptom_headache)
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText(headacheLabel))
        composeRule.onNodeWithText(headacheLabel).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.severity_severe)).assertIsDisplayed()

        composeRule.onNodeWithTag("calendar_list")
            .performScrollToNode(hasContentDescription(context.getString(R.string.symptom_action_edit)))
        composeRule.onNodeWithContentDescription(context.getString(R.string.symptom_action_edit)).performClick()
        assertEquals(symptom, editedSymptom)

        composeRule.onNodeWithContentDescription(context.getString(R.string.symptom_action_delete)).performClick()
        assertEquals(symptom, deletedSymptomRequest)

        // Verify Vitals section & click opens measurement details
        val bpValue = "128/82 mmHg · 72 bpm"
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText(bpValue))
        composeRule.onNodeWithText(bpValue).assertIsDisplayed().performClick()
        assertEquals(42L, openedMeasurementId)

        // Verify Medications section shows real medication name, status badge, and Undo button.
        // Scroll all the way to the bottom spacer so the FloatingActionButton does not obscure the Undo button.
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(androidx.compose.ui.test.hasTestTag("calendar_bottom_spacing"))
        composeRule.onNodeWithText("Amlodipine").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_status_taken)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_undo)).performClick()
        assertEquals(dose, undoneDose)
    }

    @Test
    fun calendarContent_pendingDoseOnToday_showsTakeAndSkipAndMovedHint() {
        val today = LocalDate.of(2026, 11, 10)
        val month = YearMonth.from(today)
        val pendingDose = ScheduledDoseItem(
            medicationId = 2L,
            medicationName = "Metformin",
            dosage = "500",
            unit = "mg",
            instructions = "After lunch",
            scheduledDate = today.toString(),
            scheduledTime = "13:30",
            hour = 13,
            minute = 30,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 20L,
            overrideId = 3L,
            originalTime = "12:00"
        )

        var takenDose: ScheduledDoseItem? = null
        var skippedDose: ScheduledDoseItem? = null

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = today,
                        daySummaries = mapOf(today to CalendarDaySummary(date = today, scheduledDoses = listOf(pendingDose)))
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    onMarkDoseTaken = { takenDose = it },
                    onMarkDoseSkipped = { skippedDose = it },
                    today = today
                )
            }
        }

        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Metformin"))
        composeRule.onNodeWithText("Metformin").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_moved_from, "12:00")).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_taken)).performClick()
        assertEquals(pendingDose, takenDose)

        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_skipped)).performClick()
        assertEquals(pendingDose, skippedDose)
    }

    @Test
    fun calendarContent_pendingDoseOnFutureDate_showsScheduledBadgeWithoutTakeOrSkip() {
        val today = LocalDate.of(2026, 11, 10)
        val futureDate = LocalDate.of(2026, 11, 15)
        val month = YearMonth.from(today)
        val futureDose = ScheduledDoseItem(
            medicationId = 3L,
            medicationName = "Vitamin D",
            dosage = "1000",
            unit = "mg",
            instructions = null,
            scheduledDate = futureDate.toString(),
            scheduledTime = "09:00",
            hour = 9,
            minute = 0,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 30L
        )

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = futureDate,
                        daySummaries = mapOf(futureDate to CalendarDaySummary(date = futureDate, scheduledDoses = listOf(futureDose)))
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    today = today
                )
            }
        }

        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Vitamin D"))
        composeRule.onNodeWithText("Vitamin D").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_status_scheduled)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_taken)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_skipped)).assertDoesNotExist()
    }

    @Test
    fun calendarContent_deleteSymptomDialog_displaysDedicatedStringsAndConfirms() {
        val symptom = SymptomEntry(
            id = 11L,
            symptomType = SymptomType.DIZZINESS.name,
            severity = 3,
            timestamp = 1763475600000L
        )
        var confirmDeleted = false

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = YearMonth.of(2026, 11),
                        selectedDate = LocalDate.of(2026, 11, 18),
                        symptomToDelete = symptom
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = { confirmDeleted = true },
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {}
                )
            }
        }

        val expectedTitle = context.getString(R.string.symptom_delete_confirm_title)
        val expectedDesc = context.getString(
            R.string.symptom_delete_confirm_desc,
            context.getString(R.string.symptom_dizziness)
        )

        composeRule.onNodeWithText(expectedTitle).assertIsDisplayed()
        composeRule.onNodeWithText(expectedDesc).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.medications_action_delete)).performClick()
        assertTrue(confirmDeleted)
    }

    @Test
    fun addEditSymptomSheet_addPredefinedSymptom_savesTypeSeverityAndNotes() {
        var savedType = ""
        var savedCustomName: String? = "unexpected"
        var savedSeverity = -1
        var savedNotes: String? = null

        composeRule.setContent {
            HealthLogTheme {
                AddEditSymptomSheet(
                    editingItem = null,
                    defaultDate = LocalDate.of(2026, 11, 18),
                    onDismiss = {},
                    onSave = { type, customName, severity, _, notes ->
                        savedType = type
                        savedCustomName = customName
                        savedSeverity = severity
                        savedNotes = notes
                    }
                )
            }
        }

        // Verify Add title
        composeRule.onNodeWithText(context.getString(R.string.symptom_form_title_add)).assertIsDisplayed()

        // Pick Dizziness chip
        composeRule.onNodeWithText(context.getString(R.string.symptom_dizziness)).performClick()

        // Type notes
        composeRule.onNodeWithText(context.getString(R.string.symptom_form_notes_label))
            .performScrollTo()
            .performTextInput("  After standing up  ")

        // Save
        composeRule.onNodeWithText(context.getString(R.string.symptom_form_save))
            .performScrollTo()
            .performClick()

        assertEquals(SymptomType.DIZZINESS.name, savedType)
        assertNull(savedCustomName)
        assertEquals(3, savedSeverity)
        assertEquals("After standing up", savedNotes)
    }

    @Test
    fun addEditSymptomSheet_customSymptom_validatesRequiredNameBeforeSaving() {
        var savedType = ""
        var savedCustomName: String? = null

        composeRule.setContent {
            HealthLogTheme {
                AddEditSymptomSheet(
                    editingItem = null,
                    defaultDate = LocalDate.of(2026, 11, 18),
                    onDismiss = {},
                    onSave = { type, customName, _, _, _ ->
                        savedType = type
                        savedCustomName = customName
                    }
                )
            }
        }

        // Choose Custom chip
        composeRule.onNodeWithText(context.getString(R.string.symptom_custom)).performClick()

        // Save button must be disabled when custom name is blank
        composeRule.onNodeWithText(context.getString(R.string.symptom_form_save))
            .performScrollTo()
            .assertIsNotEnabled()

        // Enter custom name
        composeRule.onNodeWithText(context.getString(R.string.symptom_form_custom_label))
            .performScrollTo()
            .performTextInput("Lower back pain")

        // Save button is now enabled and submits custom symptom
        composeRule.onNodeWithText(context.getString(R.string.symptom_form_save))
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals(SymptomType.CUSTOM.name, savedType)
        assertEquals("Lower back pain", savedCustomName)
    }

    @Test
    fun addEditSymptomSheet_editExistingSymptom_prefillsAndSavesUpdatedNotes() {
        val existing = SymptomEntry(
            id = 9L,
            symptomType = SymptomType.FATIGUE.name,
            severity = 5,
            timestamp = LocalDate.of(2026, 11, 18)
                .atTime(14, 15)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            notes = "Initial fatigue"
        )

        var savedType = ""
        var savedSeverity = -1
        var savedNotes: String? = null

        composeRule.setContent {
            HealthLogTheme {
                AddEditSymptomSheet(
                    editingItem = existing,
                    defaultDate = LocalDate.of(2026, 11, 18),
                    onDismiss = {},
                    onSave = { type, _, severity, _, notes ->
                        savedType = type
                        savedSeverity = severity
                        savedNotes = notes
                    }
                )
            }
        }

        // Edit title and pre-filled notes
        composeRule.onNodeWithText(context.getString(R.string.symptom_form_title_edit)).assertIsDisplayed()
        composeRule.onNodeWithText("Initial fatigue").performScrollTo().assertIsDisplayed()

        // Update notes
        composeRule.onNodeWithText("Initial fatigue").performTextClearance()
        composeRule.onNodeWithText(context.getString(R.string.symptom_form_notes_label))
            .performTextInput("Improved after rest")

        // Save
        composeRule.onNodeWithText(context.getString(R.string.symptom_form_save))
            .performScrollTo()
            .performClick()

        assertEquals(SymptomType.FATIGUE.name, savedType)
        assertEquals(5, savedSeverity)
        assertEquals("Improved after rest", savedNotes)
    }

    @Test
    fun calendarContent_scheduledDose_moveAndDeleteButtonsTriggerCallbacks() {
        val today = LocalDate.of(2026, 11, 10)
        val month = YearMonth.from(today)
        val dose = ScheduledDoseItem(
            medicationId = 5L,
            medicationName = "Lipitor",
            dosage = "20",
            unit = "mg",
            instructions = "At bedtime",
            scheduledDate = today.toString(),
            scheduledTime = "21:00",
            hour = 21,
            minute = 0,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 50L
        )

        var movedDose: ScheduledDoseItem? = null
        var deletedDose: ScheduledDoseItem? = null

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = today,
                        daySummaries = mapOf(today to CalendarDaySummary(date = today, scheduledDoses = listOf(dose)))
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    onRequestMoveDose = { movedDose = it },
                    onRequestDeleteDose = { deletedDose = it },
                    today = today
                )
            }
        }

        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Lipitor"))
        composeRule.onNodeWithText("Lipitor").assertIsDisplayed()

        // Move time button
        composeRule.onNodeWithContentDescription(context.getString(R.string.dose_action_move_time))
            .assertIsDisplayed()
            .performClick()
        assertEquals(dose, movedDose)

        // Delete dose button
        composeRule.onNodeWithContentDescription(context.getString(R.string.dose_action_delete))
            .assertIsDisplayed()
            .performClick()
        assertEquals(dose, deletedDose)
    }

    @Test
    fun calendarContent_oneTimeDose_showsDeleteButtonWithoutUndoButton() {
        val today = LocalDate.of(2026, 11, 10)
        val month = YearMonth.from(today)
        val oneTimeDose = ScheduledDoseItem(
            medicationId = 6L,
            medicationName = "Paracetamol",
            dosage = "500",
            unit = "mg",
            instructions = "As needed",
            scheduledDate = today.toString(),
            scheduledTime = "14:00",
            hour = 14,
            minute = 0,
            status = ScheduledDoseStatus.TAKEN,
            isRecurring = false,
            reminderId = null,
            doseLogId = 123L
        )

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = today,
                        daySummaries = mapOf(today to CalendarDaySummary(date = today, scheduledDoses = listOf(oneTimeDose)))
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    today = today
                )
            }
        }

        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Paracetamol"))
        composeRule.onNodeWithText("Paracetamol").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_status_taken)).assertIsDisplayed()

        // Delete button is present
        composeRule.onNodeWithContentDescription(context.getString(R.string.dose_action_delete)).assertIsDisplayed()

        // Undo button does NOT exist for one-time medicine
        composeRule.onNodeWithText(context.getString(R.string.dose_action_undo)).assertDoesNotExist()
    }

    @Test
    fun calendarContent_pastUnloggedDose_showsUnloggedBadgeAndTakeSkipButtons() {
        val today = LocalDate.of(2026, 11, 10)
        val pastDate = LocalDate.of(2026, 11, 8)
        val month = YearMonth.from(today)
        val unloggedDose = ScheduledDoseItem(
            medicationId = 7L,
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
            doseLogId = 200L
        )

        var takenClicked = false
        var skippedClicked = false

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = pastDate,
                        daySummaries = mapOf(pastDate to CalendarDaySummary(date = pastDate, scheduledDoses = listOf(unloggedDose)))
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    onMarkDoseTaken = { takenClicked = true },
                    onMarkDoseSkipped = { skippedClicked = true },
                    today = today
                )
            }
        }

        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Amlodipine"))
        composeRule.onNodeWithText("Amlodipine").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_status_unlogged)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_taken))
            .assertIsDisplayed()
            .performClick()
        assertTrue(takenClicked)

        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_skipped))
            .assertIsDisplayed()
            .performClick()
        assertTrue(skippedClicked)
    }

    @Test
    fun calendarContent_pastScheduledTakenAndSkipped_showsSwitchActionInsteadOfUndo() {
        val today = LocalDate.of(2026, 11, 10)
        val pastDate = LocalDate.of(2026, 11, 8)
        val month = YearMonth.from(today)
        val takenDose = ScheduledDoseItem(
            medicationId = 8L,
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
            doseLogId = 201L
        )
        val skippedDose = ScheduledDoseItem(
            medicationId = 9L,
            medicationName = "Losartan",
            dosage = "50",
            unit = "mg",
            instructions = null,
            scheduledDate = pastDate.toString(),
            scheduledTime = "20:00",
            hour = 20,
            minute = 0,
            status = ScheduledDoseStatus.SKIPPED,
            isRecurring = false,
            canToggleStatus = true,
            doseLogId = 202L
        )

        var switchedToSkipped: ScheduledDoseItem? = null
        var switchedToTaken: ScheduledDoseItem? = null

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = pastDate,
                        daySummaries = mapOf(
                            pastDate to CalendarDaySummary(
                                date = pastDate,
                                scheduledDoses = listOf(takenDose, skippedDose)
                            )
                        )
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    onMarkDoseTaken = { switchedToTaken = it },
                    onMarkDoseSkipped = { switchedToSkipped = it },
                    today = today
                )
            }
        }

        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Metformin"))
        composeRule.onNodeWithText(context.getString(R.string.dose_action_undo)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.dose_action_switch_to_skipped))
            .assertIsDisplayed()
            .performClick()
        assertEquals(takenDose, switchedToSkipped)

        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Losartan"))
        composeRule.onNodeWithText(context.getString(R.string.dose_action_switch_to_taken))
            .assertIsDisplayed()
            .performClick()
        assertEquals(skippedDose, switchedToTaken)
    }

    @Test
    fun calendarContent_notificationsDisabled_showsMutedBadgeOnlyOnPendingRecurringDose() {
        val today = LocalDate.of(2026, 11, 10)
        val month = YearMonth.from(today)
        val pendingRecurring = ScheduledDoseItem(
            medicationId = 10L,
            medicationName = "Metformin",
            dosage = "500",
            unit = "mg",
            instructions = null,
            scheduledDate = today.toString(),
            scheduledTime = "08:00",
            hour = 8,
            minute = 0,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 100L
        )
        val takenRecurring = ScheduledDoseItem(
            medicationId = 11L,
            medicationName = "Losartan",
            dosage = "50",
            unit = "mg",
            instructions = null,
            scheduledDate = today.toString(),
            scheduledTime = "09:00",
            hour = 9,
            minute = 0,
            status = ScheduledDoseStatus.TAKEN,
            isRecurring = true,
            reminderId = 101L,
            doseLogId = 301L
        )

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = today,
                        daySummaries = mapOf(
                            today to CalendarDaySummary(
                                date = today,
                                scheduledDoses = listOf(pendingRecurring, takenRecurring)
                            )
                        )
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    today = today,
                    notificationsEnabled = false
                )
            }
        }

        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Metformin"))
        composeRule.onNodeWithText(context.getString(R.string.dose_alerts_disabled_badge)).assertIsDisplayed()
        composeRule.onAllNodesWithTag("dose_alerts_muted_badge").assertCountEquals(1)
    }

    @Test
    fun calendarContent_notificationsEnabled_hidesMutedBadge() {
        val today = LocalDate.of(2026, 11, 10)
        val month = YearMonth.from(today)
        val pendingRecurring = ScheduledDoseItem(
            medicationId = 10L,
            medicationName = "Metformin",
            dosage = "500",
            unit = "mg",
            instructions = null,
            scheduledDate = today.toString(),
            scheduledTime = "08:00",
            hour = 8,
            minute = 0,
            status = ScheduledDoseStatus.PENDING,
            isRecurring = true,
            reminderId = 100L
        )

        composeRule.setContent {
            HealthLogTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        displayedMonth = month,
                        selectedDate = today,
                        daySummaries = mapOf(
                            today to CalendarDaySummary(
                                date = today,
                                scheduledDoses = listOf(pendingRecurring)
                            )
                        )
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onJumpToToday = {},
                    onSelectMonthYear = {},
                    onSelectDate = {},
                    onOpenAddSymptom = {},
                    onOpenEditSymptom = {},
                    onDismissAddEditSheet = {},
                    onSaveSymptom = { _, _, _, _, _ -> },
                    onRequestDeleteSymptom = {},
                    onConfirmDeleteSymptom = {},
                    onDismissDeleteDialog = {},
                    onOpenMeasurementDetails = {},
                    today = today,
                    notificationsEnabled = true
                )
            }
        }

        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Metformin"))
        composeRule.onNodeWithTag("dose_alerts_muted_badge").assertDoesNotExist()
    }
}
