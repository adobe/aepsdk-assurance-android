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

package com.adobe.marketing.mobile.assurance.internal.ui.status

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.assurance.internal.AssuranceComponentRegistry
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AssuranceStatusScreenTests {
    @get: Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        MobileCore.setApplication(InstrumentationRegistry.getInstrumentation().context.applicationContext as Application)
        AssuranceComponentRegistry.appState.clearLogs()
    }

    @Test
    fun testAssuranceStatusScreen() {
        // Setup
        val testLog1 = "Test Log 1"
        val testLog2 = "Test Log 2"

        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.LOW, testLog1)
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.HIGH, testLog2)

        composeTestRule.setContent {
            AssuranceStatusScreen()
        }

        composeTestRule.waitForIdle()

        // Verify
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_CLOSE_BUTTON)
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("\u2715")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_VIEW)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_PANEL)
            .assertExists()
            .assertIsDisplayed()

        val logContent = composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_CONTENT)
            .assertExists()
            .assertIsDisplayed()

        logContent.onChildren().assertCountEquals(2)
        val log1 = logContent.onChildren()[0]
        log1.assertTextEquals(testLog1)
        val log2 = logContent.onChildren()[1]
        log2.assertTextEquals(testLog2)

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.CLEAR_LOG_BUTTON)
            .assertExists()
            .assertTextEquals("Clear Log")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_DISCONNECT_BUTTON)
            .assertExists()
            .assertTextEquals("Disconnect")
    }

    @Test
    fun testAssuranceStatusScreenInLandscapeMode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        uiDevice.setOrientationLandscape()

        // Setup
        val testLog1 = "Test Log 1"
        val testLog2 = "Test Log 2"

        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.LOW, testLog1)
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.HIGH, testLog2)

        composeTestRule.setContent {
            AssuranceStatusScreen()
        }

        composeTestRule.waitForIdle()

        // Verify
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_CLOSE_BUTTON)
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("\u2715")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_VIEW)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_PANEL)
            .assertExists()
            .assertIsDisplayed()

        val logContent = composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_CONTENT)
            .assertExists()
            .assertIsDisplayed()

        logContent.onChildren().assertCountEquals(2)
        val log1 = logContent.onChildren()[0]
        log1.assertTextEquals(testLog1)
        val log2 = logContent.onChildren()[1]
        log2.assertTextEquals(testLog2)

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.CLEAR_LOG_BUTTON)
            .assertExists()
            .assertTextEquals("Clear Log")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_DISCONNECT_BUTTON)
            .assertExists()
            .assertTextEquals("Disconnect")

        uiDevice.setOrientationNatural()
    }

    @Test
    fun testStatusScreen_OnNewLogs() {
        // Setup
        val testLog1 = "Test Log 1"
        val testLog2 = "Test Log 2"

        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.LOW, testLog1)
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.HIGH, testLog2)

        composeTestRule.setContent {
            AssuranceStatusScreen()
        }

        composeTestRule.waitForIdle()

        // Verify
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_CLOSE_BUTTON)
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("\u2715")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_VIEW)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_PANEL)
            .assertExists()
            .assertIsDisplayed()

        val logContent = composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_CONTENT)
            .assertExists()
            .assertIsDisplayed()

        logContent.onChildren().assertCountEquals(2)
        val log1 = logContent.onChildren()[0]
        log1.assertTextEquals(testLog1)
        val log2 = logContent.onChildren()[1]
        log2.assertTextEquals(testLog2)

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.CLEAR_LOG_BUTTON)
            .assertExists()
            .assertTextEquals("Clear Log")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_DISCONNECT_BUTTON)
            .assertExists()
            .assertTextEquals("Disconnect")

        // Add New Logs
        val testLog3 = "Test Log 3"
        val testLog4 = "Test Log 4"
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.LOW, testLog3)
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.HIGH, testLog4)

        composeTestRule.waitForIdle()

        // Verify
        logContent.onChildren().assertCountEquals(4)
        val log3 = logContent.onChildren()[2]
        log3.assertTextEquals(testLog3)

        val log4 = logContent.onChildren()[3]
        log4.assertTextEquals(testLog4)
    }

    @Test
    fun testStatusScreen_OnNewLogsInLandscapeMode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        uiDevice.setOrientationLandscape()

        // Setup
        val testLog1 = "Test Log 1"
        val testLog2 = "Test Log 2"

        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.LOW, testLog1)
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.HIGH, testLog2)

        composeTestRule.setContent {
            AssuranceStatusScreen()
        }

        composeTestRule.waitForIdle()

        // Verify
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_CLOSE_BUTTON)
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("\u2715")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_VIEW)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_PANEL)
            .assertExists()
            .assertIsDisplayed()

        val logContent = composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_CONTENT)
            .assertExists()
            .assertIsDisplayed()

        logContent.onChildren().assertCountEquals(2)
        val log1 = logContent.onChildren()[0]
        log1.assertTextEquals(testLog1)
        val log2 = logContent.onChildren()[1]
        log2.assertTextEquals(testLog2)

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.CLEAR_LOG_BUTTON)
            .assertExists()
            .assertTextEquals("Clear Log")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_DISCONNECT_BUTTON)
            .assertExists()
            .assertTextEquals("Disconnect")

        // Add New Logs
        val testLog3 = "Test Log 3"
        val testLog4 = "Test Log 4"
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.LOW, testLog3)
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.HIGH, testLog4)

        composeTestRule.waitForIdle()

        // Verify
        logContent.onChildren().assertCountEquals(4)
        val log3 = logContent.onChildren()[2]
        log3.assertTextEquals(testLog3)

        val log4 = logContent.onChildren()[3]
        log4.assertTextEquals(testLog4)

        uiDevice.setOrientationNatural()
    }

    @Test
    fun testAssuranceStatusScreen_OnClearLogs() {
        // Setup
        val testLog1 = "Test Log 1"
        val testLog2 = "Test Log 2"

        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.LOW, testLog1)
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.HIGH, testLog2)

        composeTestRule.setContent {
            AssuranceStatusScreen()
        }

        composeTestRule.waitForIdle()

        // Verify
        val logContent = composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_CONTENT)
            .assertExists()
            .assertIsDisplayed()

        logContent.onChildren().assertCountEquals(2)
        val log1 = logContent.onChildren()[0]
        log1.assertTextEquals(testLog1)
        val log2 = logContent.onChildren()[1]
        log2.assertTextEquals(testLog2)

        // Clear Logs
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.CLEAR_LOG_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()

        // Verify
        Assert.assertTrue(AssuranceComponentRegistry.appState.statusLogs.value.isEmpty())
    }

    @Test
    fun testAssuranceStatusScreen_OnDisconnect() {
        // Setup
        val testLog1 = "Test Log 1"
        val testLog2 = "Test Log 2"

        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.LOW, testLog1)
        AssuranceComponentRegistry.appState.logStatus(AssuranceConstants.UILogColorVisibility.HIGH, testLog2)

        composeTestRule.setContent {
            AssuranceStatusScreen()
        }

        composeTestRule.waitForIdle()

        // Verify
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_CLOSE_BUTTON)
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("\u2715")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_VIEW)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_PANEL)
            .assertExists()
            .assertIsDisplayed()

        val logContent = composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.LOGS_CONTENT)
            .assertExists()
            .assertIsDisplayed()

        logContent.onChildren().assertCountEquals(2)
        val log1 = logContent.onChildren()[0]
        log1.assertTextEquals(testLog1)
        val log2 = logContent.onChildren()[1]
        log2.assertTextEquals(testLog2)

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.CLEAR_LOG_BUTTON)
            .assertExists()
            .assertTextEquals("Clear Log")

        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_DISCONNECT_BUTTON)
            .assertExists()
            .assertTextEquals("Disconnect")

        // Test
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_DISCONNECT_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()

        // Verify
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.StatusScreen.STATUS_VIEW)
            .assertDoesNotExist()
    }
}
