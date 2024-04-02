/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal.ui.pin.dialpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme

/**
 * Displays the input feedback for the pin entered using the pinpad.
 * It repurposes [BasicTextField] to display the input feedback. Renders the input text as a row of
 * 4 characters. If the input is less than 4 characters, the remaining characters are rendered as
 * empty boxes. If the input is more than 4 characters, only the first 4 characters are rendered.
 * @param input the input text to render
 */
@Composable
internal fun InputFeedbackRow(input: String) {
    BasicTextField(
        value = TextFieldValue(input, selection = TextRange(input.length)),
        enabled = false, // disables IME input in favor of on screen pin pad
        onValueChange = { /* no-op */ },
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().testTag(AssuranceUiTestTags.PinScreen.INPUT_FEEDBACK_ROW)
            ) {
                repeat(4) { index ->
                    CharHolder(
                        character = input.getOrNull(index) ?: ' '
                    )
                }
            }
        }
    )
}

/**
 * Renders a single character in the input feedback row.
 * @param character the character to render
 */
@Composable
private fun CharHolder(character: Char) {
    Text(
        text = character.toString(),
        modifier = Modifier
            .width(48.dp)
            .padding(
                vertical = AssuranceTheme.dimensions.padding.small,
                horizontal = AssuranceTheme.dimensions.padding.xSmall
            )
            .background(Color.Transparent)
            .drawBehind {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 8f

                )
            },
        fontFamily = FontFamily.SansSerif,
        textAlign = TextAlign.Center,
        fontSize = 36.sp,
        color = Color.White
    )
}
