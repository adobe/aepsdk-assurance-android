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

package com.adobe.marketing.mobile.assurance.internal.ui.pin.error

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenAction
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PinErrorViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val actions = mutableListOf<PinScreenAction>()

    @Before
    fun setUp() {
        MobileCore.setApplication(InstrumentationRegistry.getInstrumentation().context.applicationContext as Application)
    }

    @Test
    fun testPinErrorViewWithRetryableError() {

        composeTestRule.setContent {
            PinErrorView(assuranceConnectionError = AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR) {
                actions.add(it)
            }
        }

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.PIN_ERROR_VIEW,
            useUnmergedTree = true
        )
            .assertExists().assertIsDisplayed()

        val pinErrorView = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.PIN_ERROR_VIEW,
            useUnmergedTree = true
        )

        // Verify the header exists
        pinErrorView.onChildren().filter(hasTestTag(AssuranceUiTestTags.ASSURANCE_HEADER))

        // Verify the error message header exists
        pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_HEADER))
            .assertCountEquals(1)
            .apply {
                get(0).assertIsDisplayed()
                    .assertTextEquals(AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR.error)
            }

        // Verify the error message content exists
        pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_CONTENT))
            .assertCountEquals(1)
            .apply {
                get(0).assertIsDisplayed()
                    .assertTextEquals(AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR.description)
            }

        // Verify the action button row exists
        val actionButtonRow = pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_ACTION_BUTTON_ROW))
            .assertCountEquals(1)

        // Verify the cancel button exists
        actionButtonRow[0].onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_CANCEL_BUTTON))
            .assertCountEquals(1)

        // Verify the retry button exists
        actionButtonRow[0].onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_RETRY_BUTTON))
            .assertCountEquals(1)
    }

    @Test
    fun testPinErrorViewWithNonRetryableError() {
        composeTestRule.setContent {
            PinErrorView(assuranceConnectionError = AssuranceConstants.AssuranceConnectionError.EVENT_LIMIT) {
                actions.add(it)
            }
        }

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.PIN_ERROR_VIEW,
            useUnmergedTree = true
        )
            .assertExists().assertIsDisplayed()

        val pinErrorView = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.PIN_ERROR_VIEW,
            useUnmergedTree = true
        )

        // Verify the header exists
        pinErrorView.onChildren().filter(hasTestTag(AssuranceUiTestTags.ASSURANCE_HEADER))

        // Verify the error message header exists
        pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_HEADER))
            .assertCountEquals(1)
            .apply {
                get(0).assertIsDisplayed()
                    .assertTextEquals(AssuranceConstants.AssuranceConnectionError.EVENT_LIMIT.error)
            }

        // Verify the error message content exists
        pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_CONTENT))
            .assertCountEquals(1)
            .apply {
                get(0).assertIsDisplayed()
                    .assertTextEquals(AssuranceConstants.AssuranceConnectionError.EVENT_LIMIT.description)
            }

        // Verify the action button row exists
        val actionButtonRow = pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_ACTION_BUTTON_ROW))
            .assertCountEquals(1)
        actionButtonRow[0].onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_CANCEL_BUTTON))
            .assertCountEquals(1)
        actionButtonRow[0].onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_RETRY_BUTTON))
            .assertCountEquals(0)
    }

    @Test
    fun testActionButtonRowCancelClickAction() {
        composeTestRule.setContent {
            PinErrorView(assuranceConnectionError = AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR) {
                actions.add(it)
            }
        }

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.PIN_ERROR_VIEW,
            useUnmergedTree = true
        )
            .assertExists().assertIsDisplayed()

        val pinErrorView = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.PIN_ERROR_VIEW,
            useUnmergedTree = true
        )

        // Verify the header exists
        pinErrorView.onChildren().filter(hasTestTag(AssuranceUiTestTags.ASSURANCE_HEADER))

        // Verify the error message header exists
        pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_HEADER))
            .assertCountEquals(1)
            .apply {
                get(0).assertIsDisplayed()
                    .assertTextEquals(AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR.error)
            }

        // Verify the error message content exists
        pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_CONTENT))
            .assertCountEquals(1)
            .apply {
                get(0).assertIsDisplayed()
                    .assertTextEquals(AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR.description)
            }

        // Verify the action button row exists
        val actionButtonRow = pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_ACTION_BUTTON_ROW))
            .assertCountEquals(1)

        // Verify the cancel button exists
        val cancelButton = actionButtonRow[0].onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_CANCEL_BUTTON))
            .assertCountEquals(1)[0]
        cancelButton.performClick()

        assert(actions[0] == PinScreenAction.Cancel)
    }

    @Test
    fun testActionButtonRowRetryClickAction() {
        composeTestRule.setContent {
            PinErrorView(assuranceConnectionError = AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR) {
                actions.add(it)
            }
        }

        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.PIN_ERROR_VIEW,
            useUnmergedTree = true
        )
            .assertExists().assertIsDisplayed()

        val pinErrorView = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.PIN_ERROR_VIEW,
            useUnmergedTree = true
        )

        // Verify the header exists
        pinErrorView.onChildren().filter(hasTestTag(AssuranceUiTestTags.ASSURANCE_HEADER))

        // Verify the error message header exists
        pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_HEADER))
            .assertCountEquals(1)
            .apply {
                get(0).assertIsDisplayed()
                    .assertTextEquals(AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR.error)
            }

        // Verify the error message content exists
        pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_CONTENT))
            .assertCountEquals(1)
            .apply {
                get(0).assertIsDisplayed()
                    .assertTextEquals(AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR.description)
            }

        // Verify the action button row exists
        val actionButtonRow = pinErrorView.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_ACTION_BUTTON_ROW))
            .assertCountEquals(1)

        // Verify the cancel button exists
        val retryButton = actionButtonRow[0].onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.PIN_ERROR_RETRY_BUTTON))
            .assertCountEquals(1)[0]
        retryButton.performClick()

        assert(actions[0] == PinScreenAction.Retry)
    }
}
