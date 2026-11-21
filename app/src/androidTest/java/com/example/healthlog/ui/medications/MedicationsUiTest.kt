package com.example.healthlog.ui.medications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationWithReminders
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun todayScheduleView_emptyState_rendersEmptyMessageAndAddButton() {
        var addClicked = false

        composeRule.setContent {
            HealthLogTheme {
                TodayScheduleView(
                    doses = emptyList(),
                    onMarkTaken = {},
                    onMarkSkipped = {},
                    onUndo = {},
                    onAddMedication = { addClicked = true }
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.medications_empty_schedule)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.medications_empty_schedule_sub)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.medications_action_add)).performClick()

        assertTrue(addClicked)
    }

    @Test
    fun todayScheduleView_withPendingDose_rendersDoseAndHandlesTakeAction() {
        val med = Medication(
            id = 1,
            name = "Amlodipine",
            dosage = "5",
            unit = "mg",
            frequency = "DAILY"
        )
        val dose = TodayDoseItem(
            logId = null,
            medication = med,
            reminder = null,
            scheduledTime = "08:00",
            scheduledDate = "2026-11-18",
            status = DoseStatus.PENDING
        )

        var takenDose: TodayDoseItem? = null

        composeRule.setContent {
            HealthLogTheme {
                TodayScheduleView(
                    doses = listOf(dose),
                    onMarkTaken = { takenDose = it },
                    onMarkSkipped = {},
                    onUndo = {},
                    onAddMedication = {}
                )
            }
        }

        // Verify medication name, dosage, and time are displayed
        composeRule.onNodeWithText("Amlodipine • 5 mg").assertIsDisplayed()
        composeRule.onNodeWithText("08:00").assertIsDisplayed()

        // Tap Take button
        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_taken)).performClick()
        assertEquals(dose, takenDose)
    }

    @Test
    fun todayScheduleView_withCompletedDose_rendersUndoAction() {
        val med = Medication(
            id = 1,
            name = "Aspirin",
            dosage = "100",
            unit = "mg",
            frequency = "DAILY"
        )
        val completedDose = TodayDoseItem(
            logId = 99L,
            medication = med,
            reminder = null,
            scheduledTime = "09:00",
            scheduledDate = "2026-11-18",
            status = DoseStatus.TAKEN,
            actionTimestamp = 1763475600000L
        )

        var undoneDose: TodayDoseItem? = null

        composeRule.setContent {
            HealthLogTheme {
                TodayScheduleView(
                    doses = listOf(completedDose),
                    onMarkTaken = {},
                    onMarkSkipped = {},
                    onUndo = { undoneDose = it },
                    onAddMedication = {}
                )
            }
        }

        composeRule.onNodeWithText("Aspirin • 100 mg").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dose_status_taken)).assertIsDisplayed()

        // Tap Undo action
        composeRule.onNodeWithText(context.getString(R.string.dose_action_undo)).performClick()
        assertEquals(completedDose, undoneDose)
    }

    @Test
    fun prescriptionsView_rendersMedications_andQuickLogAction() {
        val dailyMed = MedicationWithReminders(
            medication = Medication(id = 1, name = "Metformin", dosage = "500", unit = "mg", frequency = "DAILY", isActive = true),
            reminders = listOf(MedicationReminder(id = 10, medicationId = 1, hour = 8, minute = 0))
        )
        val asNeededMed = MedicationWithReminders(
            medication = Medication(id = 2, name = "Paracetamol", dosage = "500", unit = "mg", frequency = "AS_NEEDED", isActive = true),
            reminders = emptyList()
        )

        var quickLoggedMed: Medication? = null

        composeRule.setContent {
            HealthLogTheme {
                PrescriptionsView(
                    medications = listOf(dailyMed, asNeededMed),
                    todayDoses = emptyList(),
                    onEdit = {},
                    onToggleActive = {},
                    onDelete = {},
                    onAddMedication = {},
                    onQuickLog = { quickLoggedMed = it }
                )
            }
        }

        composeRule.onNodeWithText("Metformin").assertIsDisplayed()
        composeRule.onNodeWithText("Paracetamol").assertIsDisplayed()

        // Tap "+ Take" on as-needed medication
        composeRule.onNodeWithText(context.getString(R.string.dose_action_mark_taken)).performClick()
        assertEquals(asNeededMed.medication, quickLoggedMed)
    }

    @Test
    fun addEditMedicationSheet_rendersForm_andValidatesInput() {
        var savedName = ""

        composeRule.setContent {
            HealthLogTheme {
                AddEditMedicationSheet(
                    editingItem = null,
                    onDismiss = {},
                    onSave = { name, _, _, _, _, _, _, _ ->
                        savedName = name
                    }
                )
            }
        }

        // Verify title
        composeRule.onNodeWithText(context.getString(R.string.med_form_title_add)).assertIsDisplayed()

        // Switch to As-Needed so notification permission is not required to save
        composeRule.onNodeWithText(context.getString(R.string.med_freq_as_needed)).performClick()

        // Input name
        composeRule.onNodeWithText(context.getString(R.string.med_form_name_label)).performTextInput("Cardura")

        // Input dosage
        composeRule.onNodeWithText(context.getString(R.string.med_form_dosage_label)).performTextInput("2")

        // When as-needed and not saving as permanent prescription, button is "Log Dose Now"
        composeRule.onNodeWithText(context.getString(R.string.med_form_action_log_dose)).performScrollTo().performClick()

        assertEquals("Cardura", savedName)
    }
}
