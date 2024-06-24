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
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.findActivity
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenAction
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenState

/**
 * Action button row for the Pin Screen composing the "Cancel" and "Connect" buttons to operate on
 * the Assurance session.
 * @param modifier the [Modifier] to apply to the Row holding the buttons.
 * @param pinScreenState the current state of the Pin Screen.
 * @param onAction the callback invoked when a button is clicked
 */
@Composable
internal fun ActionButtonRow(
    modifier: Modifier = Modifier,
    pinScreenState: State<PinScreenState>,
    onAction: (action: PinScreenAction) -> Unit
) {
    val activity = LocalContext.current.findActivity()

    Row(
        modifier = modifier.testTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_ACTION_BUTTON_ROW),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            modifier = Modifier.testTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_CANCEL_BUTTON),
            onClick = {
                onAction(PinScreenAction.Cancel)
                activity?.finish()
            }
        ) {
            Text(
                text = stringResource(id = R.string.pin_screen_button_cancel),
                fontFamily = FontFamily.SansSerif,
                style = TextStyle(color = Color.White, fontSize = 24.sp)
            )
        }

        val pin = pinScreenState.value.pin
        if (pin.length == 4) {
            TextButton(
                modifier = Modifier.testTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_CONNECT_BUTTON),
                onClick = { onAction(PinScreenAction.Connect(pin)) }
            ) {
                Text(
                    text = stringResource(id = R.string.pin_screen_button_connect),
                    fontFamily = FontFamily.SansSerif,
                    style = TextStyle(color = Color.White, fontSize = 24.sp)
                )
            }
        }
    }
}
