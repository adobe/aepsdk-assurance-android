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

package com.adobe.marketing.mobile.assurance.internal.ui.error

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AssuranceErrorViewTests {
    @get: Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        MobileCore.setApplication(InstrumentationRegistry.getInstrumentation().context.applicationContext as Application)
    }

    @Test
    fun testAssuranceErrorScreen() {
        composeTestRule.setContent {
            AssuranceErrorScreen(assuranceConnectionError = AssuranceConstants.AssuranceConnectionError.NO_ORG_ID)
        }

        composeTestRule.waitForIdle()

        // Verify
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.ERROR_VIEW)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.ERROR_TITLE)
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals(AssuranceConstants.AssuranceConnectionError.NO_ORG_ID.error)

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.ERROR_DESCRIPTION)
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals(AssuranceConstants.AssuranceConnectionError.NO_ORG_ID.description)

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.DISMISS_BUTTON)
            .assertExists()
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .assertTextEquals("Dismiss")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.DISMISS_BUTTON)
    }

    @Test
    fun testAssuranceErrorScreen_DismissClick() {
        composeTestRule.setContent {
            AssuranceErrorScreen(assuranceConnectionError = AssuranceConstants.AssuranceConnectionError.ORG_ID_MISMATCH)
        }

        composeTestRule.waitForIdle()

        // Verify initial state
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.ERROR_VIEW)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.ERROR_TITLE)
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals(AssuranceConstants.AssuranceConnectionError.ORG_ID_MISMATCH.error)

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.ERROR_DESCRIPTION)
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals(AssuranceConstants.AssuranceConnectionError.ORG_ID_MISMATCH.description)

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.DISMISS_BUTTON)
            .assertExists()
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .assertTextEquals("Dismiss")

        // Click the dismiss button
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.DISMISS_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Verify the error screen is dismissed
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ErrorScreen.ERROR_VIEW)
            .assertDoesNotExist()
    }
}
