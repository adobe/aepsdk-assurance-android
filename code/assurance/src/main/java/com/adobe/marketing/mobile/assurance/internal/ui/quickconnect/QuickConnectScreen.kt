/*
 * Copyright 2024 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance.internal.ui.quickconnect

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adobe.marketing.mobile.assurance.AssuranceComponentRegistry
import com.adobe.marketing.mobile.assurance.AssuranceConstants
import com.adobe.marketing.mobile.assurance.AssuranceStateManager
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState
import com.adobe.marketing.mobile.assurance.internal.ui.findActivity

@Composable
internal fun QuickConnectScreen(environment: AssuranceConstants.AssuranceEnvironment) {
    val activity: Activity? = LocalContext.current.findActivity()

    val assuranceStateManager: AssuranceStateManager =
        AssuranceComponentRegistry.assuranceStateManager ?: run {
            Log.e(
                "QuickConnectScreen",
                "AssuranceStateManager is not initialized. Cannot proceed with Quick Connect."
            )
            activity?.finish()
            return
        }

    val quickConnectViewModel: QuickConnectViewModel = viewModel(
        factory = QuickConnectScreenViewModelFactory(
            assuranceStateManager = assuranceStateManager,
            environment = environment
        )
    )

    val quickConnectState: State<ConnectionState> = remember { quickConnectViewModel.state }

    // If the session is already connected, close the activity.
    if (quickConnectState.value is ConnectionState.Connected) {
        activity?.finish()
    }

    // QuickConnect Authorization view
    QuickConnectView(quickConnectState = quickConnectState) {
        quickConnectViewModel.onAction(it)

        if (it is QuickConnectScreenAction.Cancel) {
            activity?.finish()
        }
    }
}

@Composable
@Preview
fun QuickConnectScreenPreview() {
    QuickConnectScreen(environment = AssuranceConstants.AssuranceEnvironment.PROD)
}
