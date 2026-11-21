package com.example.healthlog.ui.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.MeasurementType
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
        val dose = MedicationDoseLog(
            id = 7L,
            medicationId = 1L,
            scheduledDate = "2026-11-10",
            scheduledTime = "08:00",
            status = "TAKEN"
        )

        val summary = CalendarDaySummary(
            date = selectedDate,
            measurements = listOf(measurement),
            doseLogs = listOf(dose),
            symptoms = listOf(symptom),
            highestVitalRank = VitalClinicalRank.NORMAL,
            maxSymptomSeverity = SymptomSeverity.SEVERE
        )

        var newlySelectedDate: LocalDate? = null
        var openedMeasurementId: Long? = null
        var editedSymptom: SymptomEntry? = null
        var deletedSymptomRequest: SymptomEntry? = null

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
                    onOpenMeasurementDetails = { openedMeasurementId = it }
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

        // Verify Medications section
        val medTitle = "${context.getString(R.string.nav_medications)} (08:00)"
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText(medTitle))
        composeRule.onNodeWithText(medTitle).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_status_taken)).assertIsDisplayed()
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
}
