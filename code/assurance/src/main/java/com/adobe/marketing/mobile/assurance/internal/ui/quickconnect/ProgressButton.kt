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

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState

/**
 * A button that can be in one of the three states: Idle, Waiting, or Retry.
 * The style and behavior of the button are governed by the state.
 *
 * @param modifier the [Modifier] to apply to the button container.
 * @param buttonState the current state of the button.
 * @param onClick the callback invoked when the button is clicked.
 */
@Composable
internal fun ProgressButton(
    modifier: Modifier = Modifier,
    buttonState: ButtonState,
    onClick: () -> Unit
) {
    val clickable = remember { mutableStateOf(buttonState.clickable) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(
                enabled = clickable.value,
                onClick = {
                    onClick()
                }
            )
            .background(buttonState.backgroundColor, shape = RoundedCornerShape(20.dp))
            .testTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON)
    ) {
        if (buttonState is ButtonState.Waiting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .testTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_INDICATOR)
                    .size(25.dp)
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF1A73E8)
            )
        }
        Text(
            modifier = Modifier
                .padding(8.dp)
                .testTag(AssuranceUiTestTags.QuickConnectScreen.PROGRESS_BUTTON_TEXT),
            text = stringResource(id = buttonState.text),
            color = buttonState.foregroundColor,
            fontFamily = FontFamily.SansSerif,
            style = TextStyle(color = Color.White, fontSize = 14.sp)
        )
    }
}

/**
 * Represents the behavior and style of the button.
 *
 * @param text the text to be displayed on the button.
 * @param backgroundColor the background color of the button.
 * @param foregroundColor the text color of the button.
 * @param clickable the flag to indicate if the button is clickable.
 */
internal sealed class ButtonState(
    @StringRes val text: Int,
    val backgroundColor: Color,
    val foregroundColor: Color,
    val clickable: Boolean = true
) {

    companion object {
        val activeBackgroundColor = Color(0xFF068CE4)
        val inactiveBackgroundColor = Color(0xFF484E50)

        /**
         * Maps the [ConnectionState] to a [ButtonState].
         * @param connectionState the current state of the connection.
         */
        fun from(connectionState: ConnectionState): ButtonState = when (connectionState) {
            is ConnectionState.Disconnected -> {
                connectionState.error?.let { Retry() } ?: Idle()
            }

            is ConnectionState.Connecting -> Waiting()
            else -> Idle()
        }
    }

    /**
     * Represents the button state when the button is idle.
     */
    internal class Idle(
        backgroundColor: Color = activeBackgroundColor,
        foregroundColor: Color = Color.White,
        clickable: Boolean = true
    ) : ButtonState(R.string.quick_connect_button_connect, backgroundColor, foregroundColor, clickable)

    /**
     * Represents the button state when the button is waiting for the connection to be established.
     */
    internal class Waiting(
        backgroundColor: Color = inactiveBackgroundColor,
        foregroundColor: Color = Color.White,
        clickable: Boolean = false
    ) : ButtonState(R.string.quick_connect_button_waiting, backgroundColor, foregroundColor, clickable)

    /**
     * Represents the button state when the button is in a retry state.
     */
    internal class Retry(
        backgroundColor: Color = activeBackgroundColor,
        foregroundColor: Color = Color.White,
        clickable: Boolean = true
    ) : ButtonState(R.string.quick_connect_button_retry, backgroundColor, foregroundColor, clickable)
}
