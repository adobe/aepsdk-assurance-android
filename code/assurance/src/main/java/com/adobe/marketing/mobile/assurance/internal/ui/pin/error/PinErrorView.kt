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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceConnectionError
import com.adobe.marketing.mobile.assurance.internal.ui.common.AssuranceHeader
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenAction
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme.backgroundColor

/**
 * The error landing page for the PinScreen.
 * Displayed when an error occurs during the Assurance connection via pin based
 * authorization.
 * @param assuranceConnectionError the error that occurred during the Assurance connection
 * @param onAction the callback invoked when an action is taken on the error screen
 */
@Composable
internal fun PinErrorView(
    assuranceConnectionError: AssuranceConnectionError,
    onAction: (action: PinScreenAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AssuranceHeader()
            PinAuthErrorMessageHeader(text = assuranceConnectionError.error)
            PinAuthErrorMessageContent(text = assuranceConnectionError.description)
        }

        ActionButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(bottom = 20.dp),
            error = assuranceConnectionError,
            onAction = { action -> onAction(action) }
        )
    }
}
