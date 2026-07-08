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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 *
 * Switches between a single-column layout (portrait) and a two-pane layout (landscape /
 * unfolded foldables) based on the available width and height, so the dial pad and action
 * buttons remain reachable regardless of orientation or fold state.
 * @param pinScreenState the state of the pin screen
 * @param onAction the callback invoked when an action is performed on the pin screen
 */
@Composable
internal fun DialPadView(
    pinScreenState: State<PinScreenState>,
    onAction: (action: PinScreenAction) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag(AssuranceUiTestTags.PinScreen.DIAL_PAD_VIEW)
    ) {
        if (maxWidth > maxHeight) {
            DialPadLandscapeContent(pinScreenState, onAction)
        } else {
            DialPadPortraitContent(pinScreenState, onAction)
        }
    }
}

/**
 * Single-column dial pad layout used in portrait orientation.
 */
@Composable
private fun DialPadPortraitContent(
    pinScreenState: State<PinScreenState>,
    onAction: (action: PinScreenAction) -> Unit
) {
    val scrollState = rememberScrollState()
    val width = LocalConfiguration.current.screenWidthDp

    Box(
        modifier = Modifier
            .fillMaxHeight()
            // Set the width to a minimum of 600dp or the screen width.
            // This is to ensure that the dial pad buttons are not too large on squarish aspect ratios
            .widthIn(max = min(600f, (width.toFloat())).dp)
            .verticalScroll(scrollState)
            .padding(horizontal = AssuranceTheme.dimensions.padding.xxLarge),

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

/**
 * Two-pane dial pad layout used in landscape orientation and on unfolded foldables, where the
 * available width exceeds the available height. The left pane shows the branding, and the right
 * pane shows the pin feedback, dial pad and action buttons, so nothing is pushed out of view.
 */
@Composable
private fun DialPadLandscapeContent(
    pinScreenState: State<PinScreenState>,
    onAction: (action: PinScreenAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AssuranceTheme.dimensions.padding.xxLarge)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.height(AssuranceTheme.dimensions.spacing.xLarge)
            )
            Spacer(modifier = Modifier.height(AssuranceTheme.dimensions.spacing.medium))
            AssuranceHeader()
            AssuranceSubHeader(text = stringResource(id = R.string.pin_screen_header))
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = AssuranceTheme.dimensions.padding.large),
            contentAlignment = Alignment.Center
        ) {
            val rowSpacing = AssuranceTheme.dimensions.spacing.small
            // Rough space needed for the pin feedback row and action button row, so the 4 button
            // rows in between can be sized to use up the rest of the available height. Reserving
            // this space up front - rather than measuring it - lets both panes share the same
            // centered-on-full-height alignment, so they line up regardless of device height.
            val feedbackRowHeight = 56.dp
            val actionRowHeight = 64.dp
            val gridHeight = maxHeight - feedbackRowHeight - actionRowHeight - rowSpacing * 5
            val gridWidth = maxWidth - rowSpacing * 2
            val buttonSize = minOf(gridHeight / 4, gridWidth / 3).coerceAtLeast(24.dp)

            Column(
                verticalArrangement = Arrangement.spacedBy(rowSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InputFeedbackRow(input = pinScreenState.value.pin)
                NumberRow(listOf("1", "2", "3"), buttonSize = buttonSize, onClick = { action -> onAction(action) })
                NumberRow(listOf("4", "5", "6"), buttonSize = buttonSize, onClick = { action -> onAction(action) })
                NumberRow(listOf("7", "8", "9"), buttonSize = buttonSize, onClick = { action -> onAction(action) })
                SymbolRow(buttonSize = buttonSize, onClick = { action -> onAction(action) })
                ActionButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                    pinScreenState = pinScreenState,
                    onAction = { action -> onAction(action) }
                )
            }
        }
    }
}
