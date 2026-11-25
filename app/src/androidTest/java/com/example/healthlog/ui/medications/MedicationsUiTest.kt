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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MedicationsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun prescriptionsView_emptyState_rendersEmptyMessageAndAddButton() {
        var addClicked = false

        composeRule.setContent {
            HealthLogTheme {
                PrescriptionsView(
                    medications = emptyList(),
                    todayDoses = emptyList(),
                    onEdit = {},
                    onToggleActive = {},
                    onDelete = {},
                    onAddMedication = { addClicked = true }
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.medications_empty_prescriptions)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.medications_action_add)).performClick()

        assertTrue(addClicked)
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
        var savedDate: LocalDate? = null
        var savedOneTimePair: Pair<Int, Int>? = null
        val targetDate = LocalDate.of(2026, 11, 25)

        composeRule.setContent {
            HealthLogTheme {
                AddEditMedicationSheet(
                    editingItem = null,
                    initialDate = targetDate,
                    initialTime = 14 to 30,
                    onDismiss = {},
                    onSave = { name, _, _, _, _, _, _, _, date, oneTimePair ->
                        savedName = name
                        savedDate = date
                        savedOneTimePair = oneTimePair
                    }
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.med_form_title_add)).assertIsDisplayed()

        // Switch to As-Needed / One-Time so no alarm permission is needed
        composeRule.onNodeWithText(context.getString(R.string.med_freq_as_needed)).performClick()
        composeRule.onNodeWithText("14:30").assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.med_form_name_label)).performTextInput("Cardura")
        composeRule.onNodeWithText(context.getString(R.string.med_form_dosage_label)).performTextInput("2")

        composeRule.onNodeWithText(context.getString(R.string.med_form_action_log_dose)).performScrollTo().performClick()

        assertEquals("Cardura", savedName)
        assertEquals(targetDate, savedDate)
        assertEquals(14 to 30, savedOneTimePair)
    }

    @Test
    fun reminderAlertsContent_rendersOptions_andInvokesCallbacks() {
        var followUpValue: Boolean? = null
        var keepRingingValue: Boolean? = null
        var openSoundSettingsClicked = false

        composeRule.setContent {
            HealthLogTheme {
                ReminderAlertsContent(
                    followUpEnabled = true,
                    keepRingingEnabled = false,
                    onFollowUpChanged = { followUpValue = it },
                    onKeepRingingChanged = { keepRingingValue = it },
                    onOpenSoundSettings = { openSoundSettingsClicked = true }
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.reminder_alerts_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.reminder_alerts_subtitle)).assertIsDisplayed()

        // Toggle follow-up OFF (was true -> expects false)
        composeRule.onNodeWithText(context.getString(R.string.reminder_alerts_follow_up_title))
            .assertIsDisplayed()
            .performClick()
        assertEquals(false, followUpValue)

        // Toggle keep-ringing ON (was false -> expects true)
        composeRule.onNodeWithText(context.getString(R.string.reminder_alerts_keep_ringing_title))
            .assertIsDisplayed()
            .performClick()
        assertEquals(true, keepRingingValue)

        // Tap system sound & DND settings row
        composeRule.onNodeWithText(context.getString(R.string.reminder_alerts_system_sound_title))
            .assertIsDisplayed()
            .performClick()
        assertTrue(openSoundSettingsClicked)
    }
}
