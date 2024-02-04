/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

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
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
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
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.QuickConnectScreen.QUICK_CONNECT_VIEW).assertExists()

        // Verify the header and sub-header exist and are displayed
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_SUB_HEADER).assertIsDisplayed()

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.QUICK_CONNECT_VIEW,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.CANCEL_BUTTON)
            )
            .assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON)
            )

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON_TEXT)
            ).assertAny(
                !hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_INDICATOR)
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
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.QuickConnectScreen.QUICK_CONNECT_VIEW).assertExists()

        // Verify the header and sub-header exist and are displayed
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_SUB_HEADER).assertIsDisplayed()

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.QUICK_CONNECT_VIEW,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.CANCEL_BUTTON)
            )
            .assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON)
            )

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON_TEXT)
            ).assertAny(
                !hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_INDICATOR)
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
            AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON_TEXT).and(hasText("Waiting.."))
            ).assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_INDICATOR)
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
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_SUB_HEADER).assertIsDisplayed()

        // Verify that the progress button responds to Connecting state and shows the progress indicator
        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON,
            useUnmergedTree = true
        ).assertHasClickAction()
            .onChildren()
            .assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON_TEXT)
                    .and(hasText("Waiting.."))
            ).assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_INDICATOR)
            )

        // Update the state to disconnected with error
        val error = AssuranceConstants.AssuranceConnectionError.UNEXPECTED_ERROR
        quickConnectState.value = ConnectionState.Disconnected(error)
        composeTestRule.waitForIdle()

        // Verify that the QuickConnectErrorPanel is shown
        val errorPanelContent = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.CONNECTION_ERROR_PANEL,
            useUnmergedTree = true
        )
            .assertExists()
            .onChildren()

        errorPanelContent.filterToOne(hasTestTag(AssuranceUiTestTags.QuickConnectScreen.CONNECTION_ERROR_TEXT))
            .assertTextEquals(error.error)
        errorPanelContent.filterToOne(hasTestTag(AssuranceUiTestTags.QuickConnectScreen.CONNECTION_ERROR_DESCRIPTION))
            .assertTextEquals(error.description)

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren()
            .assertAny(
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON_TEXT).and(hasText("Retry"))
            ).assertAny(
                !hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_INDICATOR)
            )
    }
}
