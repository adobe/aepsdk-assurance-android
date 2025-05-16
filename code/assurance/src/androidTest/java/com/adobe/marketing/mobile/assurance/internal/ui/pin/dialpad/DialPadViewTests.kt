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

package com.adobe.marketing.mobile.assurance.internal.ui.pin.dialpad

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenAction
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenState
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenVerificationUtils.childrenDisplayed
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenVerificationUtils.verifyDialPadIdleSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DialPadViewTests {
    @get: Rule
    val composeTestRule = createComposeRule()

    private var pinScreenActions: MutableList<PinScreenAction> = mutableListOf()

    @Test
    fun testDialPadViewIdleSetup() {
        val pinScreenState = mutableStateOf(PinScreenState())
        composeTestRule.setContent {
            DialPadView(
                pinScreenState = pinScreenState,
                onAction = { action -> pinScreenActions += action }
            )
        }
        composeTestRule.waitForIdle()

        // Verify
        verifyDialPadIdleSetup(composeTestRule)
    }

    @Test
    fun testDialPadViewIdleSetupInLandscapeMode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        uiDevice.setOrientationLandscape()

        val pinScreenState = mutableStateOf(PinScreenState())
        composeTestRule.setContent {
            DialPadView(
                pinScreenState = pinScreenState,
                onAction = { action -> pinScreenActions += action }
            )
        }
        composeTestRule.waitForIdle()

        // Verify
        verifyDialPadIdleSetup(composeTestRule, scrollIfNecessary = true)

        // Reset orientation
        uiDevice.setOrientationNatural()
    }

    @Test
    fun testDialPadViewWhenPinIsEntered() {
        val pinScreenState = mutableStateOf(PinScreenState())
        composeTestRule.setContent {
            DialPadView(
                pinScreenState = pinScreenState,
                onAction = { action -> pinScreenActions += action }
            )
        }

        // Verify idle setup
        verifyDialPadIdleSetup(composeTestRule)

        assertTrue(pinScreenActions.isEmpty())

        val numberRows = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.DIAL_PAD_VIEW,
            useUnmergedTree = true
        )
            .onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.NUMBER_ROW))

        // Enter a pin 1358
        numberRows[0].onChildren()[0].performClick() // 1
        numberRows[0].onChildren()[2].performClick() // 3
        numberRows[1].onChildren()[1].performClick() // 5
        numberRows[2].onChildren()[1].performClick() // 8
        composeTestRule.waitForIdle()

        // Verify that the pin screen actions are recorded
        assertEquals(PinScreenAction.Number("1"), pinScreenActions[0])
        assertEquals(PinScreenAction.Number("3"), pinScreenActions[1])
        assertEquals(PinScreenAction.Number("5"), pinScreenActions[2])
        assertEquals(PinScreenAction.Number("8"), pinScreenActions[3])
    }

    @Test
    fun testDialPadViewWhenPinIsEnteredInLandscapeMode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        uiDevice.setOrientationLandscape()

        val pinScreenState = mutableStateOf(PinScreenState())
        composeTestRule.setContent {
            DialPadView(
                pinScreenState = pinScreenState,
                onAction = { action -> pinScreenActions += action }
            )
        }

        // Verify idle setup
        verifyDialPadIdleSetup(composeTestRule, scrollIfNecessary = true)

        assertTrue(pinScreenActions.isEmpty())

        val numberRows = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.DIAL_PAD_VIEW,
            useUnmergedTree = true
        ).onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.NUMBER_ROW))

        // Enter a pin 1358
        numberRows[0].performScrollTo().onChildren()[0].performClick() // 1
        numberRows[0].performScrollTo().onChildren()[2].performClick() // 3
        numberRows[1].performScrollTo().onChildren()[1].performClick() // 5
        numberRows[2].performScrollTo().onChildren()[1].performClick() // 8
        composeTestRule.waitForIdle()

        // Verify that the pin screen actions are recorded
        assertEquals(PinScreenAction.Number("1"), pinScreenActions[0])
        assertEquals(PinScreenAction.Number("3"), pinScreenActions[1])
        assertEquals(PinScreenAction.Number("5"), pinScreenActions[2])
        assertEquals(PinScreenAction.Number("8"), pinScreenActions[3])

        // Reset orientation
        uiDevice.setOrientationNatural()
    }

    @Test
    fun testDialPadViewWhenPinIsEnteredAndCleared() {
        val pinScreenState = mutableStateOf(PinScreenState())
        composeTestRule.setContent {
            DialPadView(
                pinScreenState = pinScreenState,
                onAction = { action -> pinScreenActions += action }
            )
        }

        // Verify idle setup
        verifyDialPadIdleSetup(composeTestRule)
        assertTrue(pinScreenActions.isEmpty())

        val numberRows = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.DIAL_PAD_VIEW,
            useUnmergedTree = true
        )
            .onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.NUMBER_ROW))

        // Enter a pin 1231
        numberRows[0].onChildren()[0].performClick() // 1
        numberRows[0].onChildren()[1].performClick() // 2
        numberRows[0].onChildren()[2].performClick() // 3
        numberRows[0].onChildren()[0].performClick() // 1

        val symbolRow = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.SYMBOL_ROW,
            useUnmergedTree = true
        ).performScrollTo()

        // Clear the pin with the delete button
        symbolRow.onChildren()[2].performScrollTo().performClick() // delete

        // Verify that the pin screen actions are recorded
        assertEquals(PinScreenAction.Number("1"), pinScreenActions[0])
        assertEquals(PinScreenAction.Number("2"), pinScreenActions[1])
        assertEquals(PinScreenAction.Number("3"), pinScreenActions[2])
        assertEquals(PinScreenAction.Number("1"), pinScreenActions[3])
        assertEquals(PinScreenAction.Delete, pinScreenActions[4])
    }

    @Test
    fun testDialPadViewWhenPinIsEnteredAndClearedInLandscapeMode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        uiDevice.setOrientationLandscape()

        val pinScreenState = mutableStateOf(PinScreenState())
        composeTestRule.setContent {
            DialPadView(
                pinScreenState = pinScreenState,
                onAction = { action -> pinScreenActions += action }
            )
        }

        // Verify idle setup
        verifyDialPadIdleSetup(composeTestRule, scrollIfNecessary = true)
        assertTrue(pinScreenActions.isEmpty())

        val numberRows = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.DIAL_PAD_VIEW,
            useUnmergedTree = true
        )
            .onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.NUMBER_ROW))

        // Enter a pin 1231
        numberRows[0].performScrollTo().onChildren()[0].performClick() // 1
        numberRows[0].performScrollTo().onChildren()[1].performClick() // 2
        numberRows[0].performScrollTo().onChildren()[2].performClick() // 3
        numberRows[0].performScrollTo().onChildren()[0].performClick() // 1

        val symbolRow = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.SYMBOL_ROW,
            useUnmergedTree = true
        )

        // Clear the pin with the delete button
        symbolRow.performScrollTo().onChildren()[2].performClick() // delete

        // Verify that the pin screen actions are recorded
        assertEquals(PinScreenAction.Number("1"), pinScreenActions[0])
        assertEquals(PinScreenAction.Number("2"), pinScreenActions[1])
        assertEquals(PinScreenAction.Number("3"), pinScreenActions[2])
        assertEquals(PinScreenAction.Number("1"), pinScreenActions[3])
        assertEquals(PinScreenAction.Delete, pinScreenActions[4])

        // Reset orientation
        uiDevice.setOrientationNatural()
    }

    @Test
    fun testDialPadViewWithPartialPinEntered() {
        // Simulate a pin screen with a pin partially entered
        val pinScreenState = mutableStateOf(PinScreenState(pin = "123"))
        composeTestRule.setContent {
            DialPadView(
                pinScreenState = pinScreenState,
                onAction = { action -> pinScreenActions += action }
            )
        }

        // Verify the action button row exists and is displayed
        val actionButtonRow = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.DIAL_PAD_ACTION_BUTTON_ROW,
            useUnmergedTree = true
        ).performScrollTo()

        actionButtonRow.assertExists().assertIsDisplayed()

        // Verify the action button row buttons
        actionButtonRow.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_CANCEL_BUTTON))
            .childrenDisplayed(1)
            .also {
                it[0].onChildren()
                    .assertAny(hasText("Cancel"))
                    .childrenDisplayed(1)
            }

        // Verify that the initial screen does not display the "Connect Button"
        actionButtonRow.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_CONNECT_BUTTON))
            .assertCountEquals(0)
    }

    @Test
    fun testDialPadViewWithFullPinEntered() {
        // Simulate a pin screen with a pin fully entered
        val pinScreenState = mutableStateOf(PinScreenState(pin = "1234"))
        composeTestRule.setContent {
            DialPadView(
                pinScreenState = pinScreenState,
                onAction = { action -> pinScreenActions += action }
            )
        }

        // Verify the action button row exists and is displayed
        val actionButtonRow = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.DIAL_PAD_ACTION_BUTTON_ROW,
            useUnmergedTree = true
        ).performScrollTo()

        actionButtonRow.assertExists().assertIsDisplayed()

        // Verify the action button row buttons
        actionButtonRow.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_CANCEL_BUTTON))
            .childrenDisplayed(1)
            .also {
                it[0].onChildren()
                    .assertAny(hasText("Cancel"))
            }

        // Verify the "Connect Button" is displayed
        actionButtonRow.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_CONNECT_BUTTON))
            .childrenDisplayed(1)
            .also {
                it[0].onChildren()
                    .assertAny(hasText("Connect"))
            }
    }

    @Test
    fun testDialPadViewWithFullPinEnteredInLandscapeMode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        uiDevice.setOrientationLandscape()

        // Simulate a pin screen with a pin fully entered
        val pinScreenState = mutableStateOf(PinScreenState(pin = "1234"))
        composeTestRule.setContent {
            DialPadView(
                pinScreenState = pinScreenState,
                onAction = { action -> pinScreenActions += action }
            )
        }

        // Verify the action button row exists and is displayed
        val actionButtonRow = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.DIAL_PAD_ACTION_BUTTON_ROW,
            useUnmergedTree = true
        ).performScrollTo()

        actionButtonRow.assertExists().performScrollTo().assertIsDisplayed()

        // Verify the action button row buttons
        actionButtonRow.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_CANCEL_BUTTON))
            .childrenDisplayed(1)
            .also {
                it[0].onChildren()
                    .assertAny(hasText("Cancel"))
            }

        // Verify the "Connect Button" is displayed
        actionButtonRow.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_CONNECT_BUTTON))
            .childrenDisplayed(1)
            .also {
                it[0].onChildren()
                    .assertAny(hasText("Connect"))
            }

        // Reset orientation
        uiDevice.setOrientationNatural()
    }
}
