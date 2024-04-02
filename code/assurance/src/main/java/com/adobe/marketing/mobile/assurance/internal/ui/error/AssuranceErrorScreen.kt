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

package com.adobe.marketing.mobile.assurance.internal.ui.error

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceHeader
import com.adobe.marketing.mobile.assurance.internal.ui.findActivity
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme

/**
 * The error landing page for no-retryable the Assurance errors when authorization UI is not already
 * displayed.
 * @param assuranceConnectionError the error that occurred during the Assurance connection
 */
@Composable
internal fun AssuranceErrorScreen(assuranceConnectionError: AssuranceConstants.AssuranceConnectionError) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AssuranceTheme.backgroundColor)
            .padding(horizontal = AssuranceTheme.dimensions.padding.xLarge)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AssuranceTheme.dimensions.spacing.medium)
        ) {
            AssuranceHeader()

            // Error title
            Text(
                text = assuranceConnectionError.error,
                style = TextStyle(
                    color = Color.Red,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
            )

            // Error description
            Text(
                text = assuranceConnectionError.description,
                style = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Justify
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
            )
        }

        // Dismiss button. This the the only button that is displayed on this screen becuase the
        // error is not retryable and session is already terminated at this point.
        val activity = LocalContext.current.findActivity()
        TextButton(
            onClick = {
                activity?.finish()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = stringResource(id = R.string.pin_connect_button_dismiss),
                fontFamily = FontFamily.SansSerif,
                style = TextStyle(color = Color.White, fontSize = 24.sp)
            )
        }
    }
}
