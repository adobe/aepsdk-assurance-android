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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceHeader
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceSubHeader
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenAction
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenState
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme.backgroundColor
import kotlin.math.min

/**
 * DialPadView is the landing view for the Pin screen. It displays the pin feedback row,
 * dial pad and action buttons for operating on the session.
 * @param pinScreenState the state of the pin screen
 * @param onAction the callback invoked when an action is performed on the pin screen
 */
@Composable
internal fun DialPadView(
    pinScreenState: State<PinScreenState>,
    onAction: (action: PinScreenAction) -> Unit
) {

    val scrollState = rememberScrollState()
    val width = LocalConfiguration.current.screenWidthDp

    Box(
        modifier = Modifier
            .fillMaxHeight()
            // Set the width to a maximum of 600dp or the screen width, whichever is smaller
            // This is to ensure that the dial pad buttons are not too large on squarish aspect ratios
            .widthIn(max = min(600f, (width.toFloat())).dp)
            .verticalScroll(scrollState)
            .background(backgroundColor)
            .padding(horizontal = AssuranceTheme.dimensions.padding.xxLarge)
            .testTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_VIEW),

    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AssuranceTheme.dimensions.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AssuranceHeader()
            AssuranceSubHeader(text = stringResource(id = R.string.pin_screen_header))
            InputFeedbackRow(input = pinScreenState.value.pin)
            NumberRow(listOf("1", "2", "3"), onClick = { action -> onAction(action) })
            NumberRow(listOf("4", "5", "6"), onClick = { action -> onAction(action) })
            NumberRow(listOf("7", "8", "9"), onClick = { action -> onAction(action) })
            SymbolRow(onClick = { action -> onAction(action) })
            Spacer(modifier = Modifier.height(AssuranceTheme.dimensions.spacing.xLarge))
        }

        ActionButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(bottom = AssuranceTheme.dimensions.padding.medium),
            pinScreenState = pinScreenState,
            onAction = { action -> onAction(action) }
        )
    }
}
