/*
 * Copyright 2023 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance.internal.ui.pin.error

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceConnectionError
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.ui.findActivity
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenAction

/**
 * Represents the action buttons for cancelling or retrying the connection when an error occurs
 * during the pin authorization flow.
 * @param modifier the modifier to be applied to the layout
 * @param error the error that occurred during the pin authorization flow
 * @param onAction the callback to be invoked when a button is clicked
 */
@Composable
internal fun ActionButtonRow(
    modifier: Modifier = Modifier,
    error: AssuranceConnectionError?,
    onAction: (action: PinScreenAction) -> Unit
) {
    val activity = LocalContext.current.findActivity() ?: return

    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = {
            onAction(PinScreenAction.Cancel)
            activity.finish()
        }) {
            Text(
                text = stringResource(id = R.string.pin_connect_button_cancel),
                fontFamily = FontFamily.SansSerif,
                style = TextStyle(color = Color.White, fontSize = 24.sp)
            )
        }

        if (error != null && error.isRetryable) {
            TextButton(onClick = { onAction(PinScreenAction.Retry) }) {
                Text(
                    text = stringResource(id = R.string.pin_connect_button_retry),
                    fontFamily = FontFamily.SansSerif,
                    style = TextStyle(color = Color.White, fontSize = 24.sp)
                )
            }
        }
    }
}
