/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal.ui.quickconnect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState

@Composable
internal fun ActionButtonRow(quickConnectState: ConnectionState, onAction: (QuickConnectScreenAction) -> Unit) {
    // Cancel and Connect buttons for triggering the connection
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Cancel button
        OutlinedButton(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp, minWidth = 80.dp)
                .testTag(QuickConnectScreenTestTags.CANCEL_BUTTON),
            onClick = {
                onAction(QuickConnectScreenAction.Cancel)
            },
            border = BorderStroke(2.dp, Color.White),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
                backgroundColor = Color.Transparent
            )
        ) {
            Text(
                text = stringResource(id = R.string.quick_connect_button_cancel),
                fontFamily = FontFamily.SansSerif,
                style = TextStyle(color = Color.White, fontSize = 14.sp)
            )
        }

        // Connect/Retry button
        val buttonState = ButtonState.from(quickConnectState)
        ProgressButton(
            buttonState = buttonState,
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp, minWidth = 80.dp)
        ) {
            val action = if (buttonState is ButtonState.Idle) {
                QuickConnectScreenAction.Connect
            } else {
                QuickConnectScreenAction.Retry
            }
            onAction(action)
        }
    }
}
