package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class NexoraNavigationUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `dashboard renders real telemetry cards and navigates across all tabs`() {
        composeRule.waitForIdle()

        // Dashboard core cards
        composeRule.onNodeWithTag("dashboard_device_card").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_ram_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_battery_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_thermal_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_display_card").performScrollTo().assertIsDisplayed()

        // Navigate to Hidden Settings
        composeRule.onNodeWithTag("nav_hidden_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Hidden Settings").assertIsDisplayed()
        composeRule.onNodeWithTag("shortcut_filter_PERFORMANCE_DEV").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shortcut_card_dev_options").performScrollTo().assertIsDisplayed()

        // Navigate to Performance Monitor
        composeRule.onNodeWithTag("nav_performance_monitor").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Performance Monitor").assertIsDisplayed()
        composeRule.onNodeWithTag("monitor_ram_chart").performScrollTo().assertIsDisplayed()

        // Navigate to Game Center
        composeRule.onNodeWithTag("nav_game_center").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Game Center").assertIsDisplayed()
        composeRule.onNodeWithTag("games_search_input").assertIsDisplayed()

        // Navigate to Labs and switch between Network and Display tabs
        composeRule.onNodeWithTag("nav_labs").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("network_status_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("labs_tab_display").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("display_lab_card").performScrollTo().assertIsDisplayed()

        // Open Settings from TopAppBar and verify Back navigation
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_theme_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("nav_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("labs_tab_network").assertIsDisplayed()
    }

    @Test
    fun `game turbo screen opens, switches profiles, and tracks sessions`() {
        composeRule.waitForIdle()

        // Open Game Turbo from TopAppBar pill
        composeRule.onNodeWithTag("nav_game_turbo").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("turbo_hero_card").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("turbo_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("turbo_session_timer").assertIsDisplayed()
        composeRule.onNodeWithTag("turbo_metric_battery").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("turbo_metric_ram").performScrollTo().assertIsDisplayed()

        // Select Performance profile
        composeRule.onNodeWithTag("turbo_lazy_list").performScrollToKey("turbo_profiles")
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("turbo_profile_PERFORMANCE").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("turbo_profile_PERFORMANCE").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Start and end a manual gaming session (DataStore persists asynchronously on IO dispatcher)
        composeRule.onNodeWithTag("turbo_lazy_list").performScrollToKey("turbo_hero")
        composeRule.onNodeWithTag("turbo_start_session_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("turbo_end_session_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("turbo_end_session_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("turbo_end_session_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("turbo_start_session_btn").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Gaming Sidebar, Tools & Session History cards by scrolling LazyColumn to each key
        composeRule.onNodeWithTag("turbo_lazy_list").performScrollToKey("turbo_sidebar_capture")
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("turbo_sidebar_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("turbo_sidebar_card").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("turbo_lazy_list").performScrollToKey("turbo_tools_card")
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("turbo_tools_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("turbo_tools_card").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("turbo_lazy_list").performScrollToKey("turbo_session_history")
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("turbo_history_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("turbo_history_card").performScrollTo().assertIsDisplayed()
    }
}
