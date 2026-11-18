package com.example.healthlog.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.ui.calendar.CalendarPlaceholderScreen
import com.example.healthlog.ui.medications.MedicationsPlaceholderScreen
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavigationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun bottomBar_rendersAllThreeTabs_andHandlesSelection() {
        var currentTab by mutableStateOf(NavigationTab.VITALS)

        composeRule.setContent {
            HealthLogTheme {
                HealthLogBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        }

        // Verify all 3 tabs are visible with localized text
        composeRule.onNodeWithText(context.getString(R.string.nav_vitals)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.nav_medications)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.nav_calendar)).assertIsDisplayed()

        // Tap Medications
        composeRule.onNodeWithTag("tab_medications").performClick()
        assertEquals(NavigationTab.MEDICATIONS, currentTab)

        // Tap Calendar
        composeRule.onNodeWithTag("tab_calendar").performClick()
        assertEquals(NavigationTab.CALENDAR, currentTab)

        // Tap Vitals
        composeRule.onNodeWithTag("tab_vitals").performClick()
        assertEquals(NavigationTab.VITALS, currentTab)
    }

    @Test
    fun medicationsPlaceholder_rendersExpectedContent() {
        composeRule.setContent {
            HealthLogTheme {
                MedicationsPlaceholderScreen()
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.medications_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.medications_placeholder_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.medications_placeholder_badge)).assertIsDisplayed()
    }

    @Test
    fun calendarPlaceholder_rendersExpectedContent() {
        composeRule.setContent {
            HealthLogTheme {
                CalendarPlaceholderScreen()
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.calendar_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_placeholder_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_placeholder_badge)).assertIsDisplayed()
    }
}
