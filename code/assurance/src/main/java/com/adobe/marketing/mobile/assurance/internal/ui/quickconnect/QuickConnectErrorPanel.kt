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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme

/**
 * Composable to [AssuranceConstants.AssuranceConnectionError] details during QuickConnect authorization
 * process.
 * @param error the error details to be displayed
 */
@Composable
internal fun QuickConnectErrorPanel(error: AssuranceConstants.AssuranceConnectionError) {
    // This is a row with a columnar content inside it.
    Row(modifier = Modifier.testTag(AssuranceUiTestTags.QuickConnectScreen.CONNECTION_ERROR_PANEL)) {
        Column {
            // Text for the connection error if any
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        top = AssuranceTheme.dimensions.padding.small,
                        bottom = AssuranceTheme.dimensions.padding.xSmall
                    )
                    .testTag(AssuranceUiTestTags.QuickConnectScreen.CONNECTION_ERROR_TEXT),
                text = error.error,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = Color.White,
                    fontSize = AssuranceTheme.typography.font.size.large.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )

            // Text for connection description
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        top = AssuranceTheme.dimensions.padding.small,
                        bottom = AssuranceTheme.dimensions.padding.xSmall
                    )
                    .testTag(AssuranceUiTestTags.QuickConnectScreen.CONNECTION_ERROR_DESCRIPTION),
                text = error.description,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = Color.White,
                    fontSize = AssuranceTheme.typography.font.size.large.sp
                )
            )
        }
    }
}
