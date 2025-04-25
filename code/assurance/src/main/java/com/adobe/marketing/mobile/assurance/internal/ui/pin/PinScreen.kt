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

package com.adobe.marketing.mobile.assurance.internal.ui.pin

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants.AssuranceEnvironment
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState
import com.adobe.marketing.mobile.assurance.internal.ui.findActivity
import com.adobe.marketing.mobile.assurance.internal.ui.pin.dialpad.DialPadView
import com.adobe.marketing.mobile.assurance.internal.ui.pin.error.PinErrorView
import com.adobe.marketing.mobile.assurance.internal.ui.pin.loading.PinConnectingView

/**
 * The screen that encompasses the entire Assurance PIN flow.
 * @param sessionId the sessionID for the Assurance session
 * @param environment the Assurance environment
 */
@Composable
internal fun PinScreen(sessionId: String, environment: AssuranceEnvironment) {
    val activity = LocalContext.current.findActivity() ?: return

    val viewModel: PinScreenViewModel = viewModel(
        factory = PinScreenViewModelFactory(
            sessionId = sessionId,
            environment = environment
        )
    )

    // Handle back button press
    BackHandler {
        viewModel.onAction(PinScreenAction.Cancel)
        activity.finish()
    }

    // Display the appropriate screen based on the connection state
    val dialPadState = viewModel.state.value
    when (dialPadState.connectionState) {
        is ConnectionState.Disconnected -> {
            val disconnected = dialPadState.connectionState
            if (disconnected.error == null) {
                // If there is no error, show the dial pad. This is the default state.
                DialPadView(viewModel.state) { viewModel.onAction(it) }
            } else {
                // else show the error screen.
                PinErrorView(assuranceConnectionError = disconnected.error) { viewModel.onAction(it) }
            }
        }

        is ConnectionState.Connecting -> PinConnectingView()

        is ConnectionState.Connected -> activity.finish()
    }
}
