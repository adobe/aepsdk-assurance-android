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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.assurance.AssuranceConstants
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProgressButtonTests {
    @get: Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testProgressButtonWhenIdle() {

        var clicked = false
        composeTestRule.setContent {
            ProgressButton(
                buttonState = ButtonState.Idle(),
                onClick = { clicked = true }
            )
        }

        // Verify the progress button exists and is displayed
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON)
            .assertExists().assertIsDisplayed()

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren().assertAny(
                // Verify that the progress indicator is not displayed
                !hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_INDICATOR)
            )
            .assertAny(
                // Verify that the progress button text is "Connect"
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON_TEXT).and(
                    hasText(
                        "Connect"
                    )
                )
            )

        // Click the progress button
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON)
            .performClick()

        // Verify the progress button click was registered and relayed to the callback
        assertTrue(clicked)
    }

    @Test
    fun testProgressButtonWhenWaiting() {

        var clicked = false
        composeTestRule.setContent {
            ProgressButton(
                buttonState = ButtonState.Waiting(),
                onClick = { clicked = true }
            )
        }

        // Verify the progress button exists and is displayed
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON)
            .assertExists().assertIsDisplayed()

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren().assertAny(
                // Verify that the progress indicator is displayed
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_INDICATOR)
            )
            .assertAny(
                // Verify that the progress button text is not displayed
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON_TEXT).and(
                    hasText(
                        "Waiting.."
                    )
                )
            )

        // Click the progress button
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON)
            .performClick()

        // Verify the progress button click was not registered and not relayed to the callback
        assertFalse(clicked)
    }

    @Test
    fun testProgressButtonWhenRetry() {

        var clicked = false
        composeTestRule.setContent {
            ProgressButton(
                buttonState = ButtonState.Retry(),
                onClick = { clicked = true }
            )
        }

        // Verify the progress button exists and is displayed
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON)
            .assertExists().assertIsDisplayed()

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON,
            useUnmergedTree = true
        )
            .onChildren().assertAny(
                // Verify that the progress indicator is not displayed
                !hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_INDICATOR)
            )
            .assertAny(
                // Verify that the progress button text is "Retry"
                hasTestTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON_TEXT).and(
                    hasText(
                        "Retry"
                    )
                )
            )

        // Click the progress button
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON)
            .performClick()

        // Verify the progress button click was registered and relayed to the callback
        assertTrue(clicked)
    }

    @Test
    fun testButtonStates() {
        val idle = ButtonState.Idle()
        assertTrue(idle.clickable)
        assertTrue(idle.backgroundColor == ButtonState.activeBackgroundColor)
        assertTrue(idle.foregroundColor == Color.White)

        val waiting = ButtonState.Waiting()
        assertFalse(waiting.clickable)
        assertTrue(waiting.backgroundColor == ButtonState.inactiveBackgroundColor)
        assertTrue(waiting.foregroundColor == Color.White)

        val retry = ButtonState.Retry()
        assertTrue(retry.clickable)
        assertTrue(retry.backgroundColor == ButtonState.activeBackgroundColor)
        assertTrue(retry.foregroundColor == Color.White)
    }

    @Test
    fun testButtonStateFromConnectionState() {
        val disconnected = ConnectionState.Disconnected(null)
        val idle = ButtonState.from(disconnected)
        assertTrue(idle is ButtonState.Idle)

        val connecting = ConnectionState.Connecting
        val waiting = ButtonState.from(connecting)
        assertTrue(waiting is ButtonState.Waiting)

        val connected = ConnectionState.Connected
        val idleAgain = ButtonState.from(connected)
        assertTrue(idleAgain is ButtonState.Idle)

        val disconnectedWithError = ConnectionState.Disconnected(AssuranceConstants.AssuranceConnectionError.CONNECTION_LIMIT)
        val retry = ButtonState.from(disconnectedWithError)
        assertTrue(retry is ButtonState.Retry)
    }
}
