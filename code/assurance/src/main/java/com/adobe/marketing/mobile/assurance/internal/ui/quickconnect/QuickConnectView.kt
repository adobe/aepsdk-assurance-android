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

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceHeader
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceSubHeader
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme

/**
 * Represents the QuickConnect authorization view.
 * Exists separately from QuickConnectScreen to allow for easier testing.
 */
@Composable
internal fun QuickConnectView(
    quickConnectState: State<ConnectionState>,
    onAction: (QuickConnectScreenAction) -> Unit
) {
    val isTv = with(LocalConfiguration.current) {
        remember { (this.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION }
    }

    val isLandscape = with(LocalConfiguration.current) {
        remember { (this.orientation == Configuration.ORIENTATION_LANDSCAPE || this.orientation == Configuration.ORIENTATION_UNDEFINED) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AssuranceTheme.backgroundColor)
            .padding(horizontal = AssuranceTheme.dimensions.padding.large)
            .testTag(AssuranceUiTestTags.QuickConnectScreen.QUICK_CONNECT_VIEW)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag(AssuranceUiTestTags.QuickConnectScreen.QUICK_CONNECT_SCROLLVIEW),
            verticalArrangement = Arrangement.spacedBy(AssuranceTheme.dimensions.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AssuranceHeader()
            AssuranceSubHeader(text = stringResource(id = R.string.quick_connect_screen_header))

            // Quick Connect  flow image
            Image(
                painter = painterResource(id = R.drawable.img_quick_connect),
                contentDescription = "Quick Connect Flow",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(
                        horizontal = AssuranceTheme.dimensions.padding.xLarge,
                        vertical = AssuranceTheme.dimensions.padding.medium
                    )
                    // Restrict the image to a max of 1/3rd of the screen in landscape mode or TV mode
                    .fillMaxWidth(fraction = if (isTv || isLandscape) 0.3f else 1f)
                    .testTag(AssuranceUiTestTags.QuickConnectScreen.QUICK_CONNECT_LOGO)

            )

            if (quickConnectState.value is ConnectionState.Disconnected) {
                val disconnectedState = quickConnectState.value as ConnectionState.Disconnected
                if (disconnectedState.error == null) {
                    // If there is no error, this is the default state. Do nothing.
                } else {
                    // else show the error panel.
                    QuickConnectErrorPanel(disconnectedState.error)
                }
            }

            // Action buttons for triggering the connection
            ActionButtonRow(quickConnectState = quickConnectState.value, onAction = onAction)

            // Spacer to push the Adobe Logo at the bottom of the screen while being scrollable.
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.img_adobelogo),
                contentDescription = "Adobe Logo",
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth()
                    .padding(
                        bottom = AssuranceTheme.dimensions.padding.xSmall
                    )
                    .testTag(AssuranceUiTestTags.QuickConnectScreen.ADOBE_LOGO)

            )
        }
    }
}
