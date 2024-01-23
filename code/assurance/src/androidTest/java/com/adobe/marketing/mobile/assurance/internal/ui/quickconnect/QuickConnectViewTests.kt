package com.adobe.marketing.mobile.assurance.internal.ui.quickconnect

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import com.adobe.marketing.mobile.assurance.AssuranceConstants
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceCommonTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState
import org.junit.Rule
import org.junit.Test

class QuickConnectViewTests {
    @get: Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testQuickConnectViewWhenIdle() {
        val quickConnectState = mutableStateOf<ConnectionState>(ConnectionState.Disconnected(null))
        composeTestRule.setContent {
            QuickConnectView(
                quickConnectState = quickConnectState,
                onAction = { /* no-op */ }
            )
        }

        // Verify
        composeTestRule.onNodeWithTag(QuickConnectScreenTestTags.QUICK_CONNECT_VIEW).assertExists()

        // Verify the header and sub-header exist and are displayed
        composeTestRule.onNodeWithTag(AssuranceCommonTestTags.ASSURANCE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AssuranceCommonTestTags.ASSURANCE_SUB_HEADER).assertIsDisplayed()

        composeTestRule.onNodeWithTag(
            QuickConnectScreenTestTags.QUICK_CONNECT_VIEW,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(QuickConnectScreenTestTags.CANCEL_BUTTON)
            )
            .assertAny(
                hasTestTag(QuickConnectScreenTestTags.PROGRESS_BUTTON)
            )

        composeTestRule.onNodeWithTag(
            QuickConnectScreenTestTags.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(QuickConnectScreenTestTags.PROGRESS_BUTTON_TEXT)
            ).assertAny(
                !hasTestTag(QuickConnectScreenTestTags.PROGRESS_INDICATOR)
            )
    }

    @Test
    fun testQuickConnectViewWhenConnecting() {
        val quickConnectState = mutableStateOf<ConnectionState>(ConnectionState.Disconnected(null))
        composeTestRule.setContent {
            QuickConnectView(
                quickConnectState = quickConnectState,
                onAction = { /* no-op */ }
            )
        }

        // Verify the initial state of being idle and disconnected
        composeTestRule.onNodeWithTag(QuickConnectScreenTestTags.QUICK_CONNECT_VIEW).assertExists()

        // Verify the header and sub-header exist and are displayed
        composeTestRule.onNodeWithTag(AssuranceCommonTestTags.ASSURANCE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AssuranceCommonTestTags.ASSURANCE_SUB_HEADER).assertIsDisplayed()

        composeTestRule.onNodeWithTag(
            QuickConnectScreenTestTags.QUICK_CONNECT_VIEW,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(QuickConnectScreenTestTags.CANCEL_BUTTON)
            )
            .assertAny(
                hasTestTag(QuickConnectScreenTestTags.PROGRESS_BUTTON)
            )

        composeTestRule.onNodeWithTag(
            QuickConnectScreenTestTags.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(QuickConnectScreenTestTags.PROGRESS_BUTTON_TEXT)
            ).assertAny(
                !hasTestTag(QuickConnectScreenTestTags.PROGRESS_INDICATOR)
            )

        // Update the state to connecting
        quickConnectState.value = ConnectionState.Connecting
        composeTestRule.waitForIdle()

        // Verify that the progress button responds to Connecting state and shows the progress indicator
        // Note that ideally this test should also check that the progress button is not clickable.
        // However, the test framework does not checking for clickability of a Composable and
        // on the assertion of whether or not a click action can be performed.
        // Refer to the ProgressButtonTests.kt file for the click logic tests.
        composeTestRule.onNodeWithTag(
            QuickConnectScreenTestTags.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(QuickConnectScreenTestTags.PROGRESS_BUTTON_TEXT).and(hasText("Waiting.."))
            ).assertAny(
                hasTestTag(QuickConnectScreenTestTags.PROGRESS_INDICATOR)
            )
    }

    @Test
    fun testQuickConnectViewWhenDisconnectedWithError() {
        val quickConnectState = mutableStateOf<ConnectionState>(ConnectionState.Disconnected(null))
        composeTestRule.setContent {
            QuickConnectView(
                quickConnectState = quickConnectState,
                onAction = { /* no-op */ }
            )
        }

        // Update the state to connecting
        quickConnectState.value = ConnectionState.Connecting
        composeTestRule.waitForIdle()

        // Verify the header and sub-header exist and are displayed
        composeTestRule.onNodeWithTag(AssuranceCommonTestTags.ASSURANCE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AssuranceCommonTestTags.ASSURANCE_SUB_HEADER).assertIsDisplayed()

        // Verify that the progress button responds to Connecting state and shows the progress indicator
        composeTestRule.onNodeWithTag(
            QuickConnectScreenTestTags.PROGRESS_BUTTON,
            useUnmergedTree = true
        ).assertHasClickAction()
            .onChildren()
            .assertAny(
                hasTestTag(QuickConnectScreenTestTags.PROGRESS_BUTTON_TEXT)
                    .and(hasText("Waiting.."))
            ).assertAny(
                hasTestTag(QuickConnectScreenTestTags.PROGRESS_INDICATOR)
            )

        // Update the state to disconnected with error
        val error = AssuranceConstants.AssuranceConnectionError.UNEXPECTED_ERROR
        quickConnectState.value = ConnectionState.Disconnected(error)
        composeTestRule.waitForIdle()

        // Verify that the QuickConnectErrorPanel is shown
        val errorPanelContent = composeTestRule.onNodeWithTag(
            QuickConnectScreenTestTags.CONNECTION_ERROR_PANEL,
            useUnmergedTree = true
        )
            .assertExists()
            .onChildren()

        errorPanelContent.filterToOne(hasTestTag(QuickConnectScreenTestTags.CONNECTION_ERROR_TEXT))
            .assertTextEquals(error.error)
        errorPanelContent.filterToOne(hasTestTag(QuickConnectScreenTestTags.CONNECTION_ERROR_DESCRIPTION))
            .assertTextEquals(error.description)

        composeTestRule.onNodeWithTag(
            QuickConnectScreenTestTags.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(QuickConnectScreenTestTags.PROGRESS_BUTTON_TEXT).and(hasText("Retry"))
            ).assertAny(
                !hasTestTag(QuickConnectScreenTestTags.PROGRESS_INDICATOR)
            )
    }
}
