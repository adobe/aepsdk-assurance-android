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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceHeader
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceSubHeader
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenAction
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenState
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme.backgroundColor

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 64.dp)
            .testTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_VIEW)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AssuranceHeader()
            AssuranceSubHeader(text = stringResource(id = R.string.pin_connect_enter_pin_text))
            InputFeedbackRow(input = pinScreenState.value.pin)
            NumberRow(listOf("1", "2", "3"), onClick = { action -> onAction(action) })
            NumberRow(listOf("4", "5", "6"), onClick = { action -> onAction(action) })
            NumberRow(listOf("7", "8", "9"), onClick = { action -> onAction(action) })
            SymbolRow(onClick = { action -> onAction(action) })
        }

        ActionButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(bottom = 20.dp),
            pinScreenState = pinScreenState,
            onAction = { action -> onAction(action) }
        )
    }
}
