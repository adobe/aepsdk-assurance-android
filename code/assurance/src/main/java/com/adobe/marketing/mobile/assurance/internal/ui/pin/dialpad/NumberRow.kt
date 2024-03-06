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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenAction

/**
 * A row of dial pad buttons that are digits.
 * @param contents the contents of the row
 * @param onClick the callback invoked when a number button is clicked
 */
@Composable
internal fun NumberRow(contents: List<String>, onClick: (PinScreenAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AssuranceUiTestTags.PinScreen.NUMBER_ROW),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        contents.forEach { symbol ->
            DialPadButton(
                content = {
                    Text(
                        text = symbol,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.testTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_NUMERIC_BUTTON_TEXT)
                    )
                },
                borderColor = Color.White,
                modifier = Modifier
                    .aspectRatio(1f)
                    .weight(1f)
            ) {
                onClick(PinScreenAction.Number(symbol))
            }
        }
    }
}
