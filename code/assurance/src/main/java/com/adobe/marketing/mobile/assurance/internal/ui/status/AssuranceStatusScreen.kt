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

package com.adobe.marketing.mobile.assurance.internal.ui.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.AssuranceComponentRegistry
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceHeader
import com.adobe.marketing.mobile.assurance.internal.ui.findActivity
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme.backgroundColor

@Composable
internal fun AssuranceStatusScreen() {
    val activity = LocalContext.current.findActivity() ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Assurance header and close button
        Box {
            AssuranceHeader()
            CloseButton(modifier = Modifier.align(Alignment.TopEnd)) { activity.finish() }
        }

        // Assurance status text view
        val logs = remember {
            AssuranceComponentRegistry.appState.statusLogs
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .background(Color(4281743682))
        ) {
            // Lazy column to display the logs
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(AssuranceTheme.dimensions.padding.small)
            ) {
                items(logs.value.size) {
                    val message = logs.value[it].message
                    val color = logs.value[it].level.toColor()
                    Text(
                        text = message,
                        fontFamily = AssuranceTheme.typography.font.family,
                        style = TextStyle(color = color, fontSize = AssuranceTheme.typography.font.size.small.sp)
                    )
                }
            }
        }

        // Clear and Disconnect buttons displayed at the bottom of the screen
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = { AssuranceComponentRegistry.appState.clearLogs() }) {
                Text(
                    text = stringResource(id = R.string.status_screen_button_clear_log),
                    fontFamily = FontFamily.SansSerif,
                    style = TextStyle(color = Color.White, fontSize = 14.sp)
                )
            }

            OutlinedButton(
                onClick = {
                    AssuranceComponentRegistry.sessionUIOperationHandler?.onDisconnect()
                    activity.finish()
                },
                border = BorderStroke(2.dp, Color.Red),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    backgroundColor = Color.Transparent
                )
            ) {
                Text(
                    text = stringResource(id = R.string.status_screen_button_disconnect),
                    fontFamily = FontFamily.SansSerif,
                    style = TextStyle(color = Color.Red, fontSize = 14.sp)
                )
            }
        }
    }
}

/**
 * Close button displayed at the top right corner of the screen. Cheaper than importing a whole
 * icon pack!
 * @param modifier [Modifier] to be applied to the button
 * @param onClick callback to be invoked when the button is clicked
 */
@Composable
private fun CloseButton(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .size(32.dp)
            .wrapContentSize()
            .clickable { onClick() }
            .border(
                BorderStroke(1.dp, Color.White),
                CircleShape
            )
            .aspectRatio(1f)
            .then(modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u2715", // UTF-8 for X symbol
            fontFamily = FontFamily.SansSerif,
            style = TextStyle(color = Color.White, fontSize = 14.sp)
        )
    }
}

/**
 * Converts the [AssuranceConstants.UILogColorVisibility] to a [Color] for display in the UI.
 * @return [Color] for the given [AssuranceConstants.UILogColorVisibility]
 */
private fun AssuranceConstants.UILogColorVisibility.toColor(): Color {
    return when (this) {
        AssuranceConstants.UILogColorVisibility.LOW -> Color.LightGray
        AssuranceConstants.UILogColorVisibility.NORMAL -> Color.DarkGray
        AssuranceConstants.UILogColorVisibility.HIGH -> Color.Yellow
        AssuranceConstants.UILogColorVisibility.CRITICAL -> Color.Red
    }
}
