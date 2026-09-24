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

package com.adobe.marketing.mobile.assurance.internal.ui.pin

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags

internal object PinScreenVerificationUtils {

    private const val ASSURANCE_HEADER_TEXT = "Assurance"
    private const val ASSURANCE_SUB_HEADER_TEXT = "Enter the 4 digit PIN to continue"

    internal fun verifyDialPadIdleSetup(composeTestRule: ComposeContentTestRule, isScrollable: Boolean = true) {
        // Verify the dial pad view exists and is displayed
        composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.DIAL_PAD_VIEW,
            useUnmergedTree = true
        )
            .assertExists().assertIsDisplayed()

        // Verify the header and sub-header exist and are displayed
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_HEADER).assertExists()
            .assertIsDisplayed()
            .assertTextEquals(ASSURANCE_HEADER_TEXT)
        composeTestRule.onNodeWithTag(AssuranceUiTestTags.ASSURANCE_SUB_HEADER).assertExists()
            .assertIsDisplayed()
            .assertTextEquals(ASSURANCE_SUB_HEADER_TEXT)

        // Verify the input feedback row exists and is displayed
        val inputFeedbackRow = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.INPUT_FEEDBACK_ROW,
            useUnmergedTree = true
        )
        verifyInputFeedback(inputFeedbackRow, "") // four empty spaces

        // Verify the number row exists and is displayed.
        // Use onAllNodesWithTag rather than DIAL_PAD_VIEW.onChildren() so the lookup is
        // independent of how deeply the rows are nested - the portrait and landscape adaptive
        // layouts wrap the rows at different depths under DIAL_PAD_VIEW.
        val numberRows = composeTestRule.onAllNodesWithTag(
            AssuranceUiTestTags.PinScreen.NUMBER_ROW,
            useUnmergedTree = true
        )
        verifyDialPad(numberRows, isScrollable)

        // Verify the symbol row exists and is displayed
        val symbolRow = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.SYMBOL_ROW,
            useUnmergedTree = true
        )
        verifySymbolRow(symbolRow, isScrollable)

        // Verify the action button row exists and is displayed
        val actionButtonRow = composeTestRule.onNodeWithTag(
            AssuranceUiTestTags.PinScreen.DIAL_PAD_ACTION_BUTTON_ROW,
            useUnmergedTree = true
        )
        actionButtonRow.assertExists()
            .apply { if (isScrollable) performScrollTo() }
            .assertIsDisplayed()

        // Verify the action button row buttons
        actionButtonRow.onChildren()
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_CANCEL_BUTTON))
            .assertCountEquals(1)
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

    private fun verifyDialPad(numberRows: SemanticsNodeInteractionCollection, isScrollable: Boolean) {
        // Verify the number row exists and is displayed
        numberRows.assertCountEquals(3)

        // Verify the number row buttons
        // - Each number row should have 3 buttons
        // - Each button should have a text child
        numberRows[0]
            .apply { if (isScrollable) performScrollTo() }
            .onChildren().assertCountEquals(3)
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_BUTTON))
            .apply {
                get(0).onChildren()
                    .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT))
                    .assertAny(hasText("1"))
                    .childrenDisplayed(1)
                get(1).onChildren()
                    .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT))
                    .assertAny(hasText("2"))
                    .childrenDisplayed(1)
                get(2).onChildren()
                    .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT))
                    .assertAny(hasText("3"))
                    .childrenDisplayed(1)
            }

        numberRows[1]
            .apply { if (isScrollable) performScrollTo() }
            .onChildren()
            .assertCountEquals(3)
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_BUTTON))
            .apply {
                get(0)
                    .onChildren()
                    .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT))
                    .assertAny(hasText("4"))
                    .childrenDisplayed(1)
                get(1).onChildren()
                    .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT))
                    .assertAny(hasText("5"))
                    .childrenDisplayed(1)
                get(2).onChildren()
                    .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT))
                    .assertAny(hasText("6"))
                    .childrenDisplayed(1)
            }

        numberRows[2]
            .apply { if (isScrollable) performScrollTo() }
            .onChildren()
            .assertCountEquals(3)
            .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_BUTTON))
            .apply {
                get(0).onChildren()
                    .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT))
                    .assertAny(hasText("7"))
                    .childrenDisplayed(1)
                get(1).onChildren()
                    .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT))
                    .assertAny(hasText("8"))
                    .childrenDisplayed(1)
                get(2).onChildren()
                    .filter(hasTestTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT))
                    .assertAny(hasText("9"))
                    .childrenDisplayed(1)
            }
    }

    private fun verifyInputFeedback(
        inputFeedbackRow: SemanticsNodeInteraction,
        expectedValue: String
    ) {
        inputFeedbackRow.assertExists().assertIsDisplayed()
        inputFeedbackRow.onChildren().assertCountEquals(4)
        (0..3).forEach {
            val expectedChar = if (it < expectedValue.length) expectedValue[it] else ' '
            inputFeedbackRow.onChildren()[it].assertTextEquals(expectedChar.toString())
        }
    }

    private fun verifySymbolRow(symbolRow: SemanticsNodeInteraction, isScrollable: Boolean) {

        symbolRow.assertExists()
            .apply { if (isScrollable) performScrollTo() }
            .assertIsDisplayed()
        symbolRow
            .apply { if (isScrollable) performScrollTo() }
            .onChildren()
            .apply {
                get(0).onChildren()[0].assertTextEquals("")
                get(1).onChildren().childrenDisplayed(1)[0].assertTextEquals("0")
                get(2).onChildren().childrenDisplayed(1).assertAny(
                    hasContentDescription("Delete")
                )
            }
    }

    internal fun SemanticsNodeInteractionCollection.childrenDisplayed(count: Int): SemanticsNodeInteractionCollection {
        this.assertCountEquals(count)
        (0..count - 1).forEach { this[it].assertIsDisplayed() }
        return this
    }
}
