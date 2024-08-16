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

package com.adobe.marketing.mobile.assurance.internal.ui.pin.loading

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PinConnectingViewTests {
    @get: Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        MobileCore.setApplication(InstrumentationRegistry.getInstrumentation().context.applicationContext as Application)
    }

    @Test
    fun testPinConnectingView() {
        // Setup
        composeTestRule.setContent {
            PinConnectingView()
        }

        composeTestRule.waitForIdle()

        // Verify
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.PinScreen.PIN_CONNECTING_LOADING_INDICATOR)
            .assertExists()
            .assertIsDisplayed()
    }
}
