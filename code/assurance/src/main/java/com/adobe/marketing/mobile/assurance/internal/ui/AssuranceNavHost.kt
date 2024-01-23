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

package com.adobe.marketing.mobile.assurance.internal.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adobe.marketing.mobile.assurance.AssuranceAppState
import com.adobe.marketing.mobile.assurance.internal.ui.error.AssuranceErrorScreen
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreen
import com.adobe.marketing.mobile.assurance.internal.ui.quickconnect.QuickConnectScreen
import com.adobe.marketing.mobile.assurance.internal.ui.status.AssuranceStatusScreen

/**
 * Navigation host for the Assurance UI. Controls the navigation between the different screens
 * based on the current session phase.
 * @param navHostController the nav host controller
 * @param sessionPhase the current Assurance session phase
 */
@Composable
internal fun AssuranceNavHost(
    navHostController: NavHostController = rememberNavController(),
    sessionPhase: AssuranceAppState.SessionPhase
) {
    // Infer the start destination based on the current session phase
    val activity = LocalContext.current as? Activity

    val startDestination: AssuranceDestination = AssuranceDestination.fromSessionPhase(sessionPhase)

    NavHost(navController = navHostController, startDestination = startDestination.route.path) {
        composable(AssuranceNavRoute.PinCodeRoute.path) {
            val pinConnect = (startDestination as? AssuranceDestination.PinDestination)?.pinConnect
                ?: kotlin.run {
                    // PinCode route should always be mapped to PinDestination
                    activity?.finish()
                    return@composable
                }

            PinScreen(sessionId = pinConnect.sessionId, environment = pinConnect.environment)
        }

        composable(AssuranceNavRoute.QuickConnectRoute.path) {
            val quickConnect = (startDestination as? AssuranceDestination.QuickConnectDestination)?.quickConnect
                ?: kotlin.run {
                    // QuickConnect route should always be mapped to QuickConnectDestination
                    activity?.finish()
                    return@composable
                }

            QuickConnectScreen(environment = quickConnect.environment)
        }

        composable(AssuranceNavRoute.StatusRoute.path) {
            AssuranceStatusScreen()
        }

        composable(AssuranceNavRoute.ErrorRoute.path) {
            val errorDestination =
                (startDestination as? AssuranceDestination.ErrorDestination) ?: kotlin.run {
                    // Error route should always be mapped to Disconnection
                    activity?.finish()
                    return@composable
                }

            val error = errorDestination.disconnected.error ?: return@composable
            AssuranceErrorScreen(assuranceConnectionError = error)
        }

        composable(AssuranceNavRoute.UnknownRoute.path) {
            activity?.finish()
        }
    }
}

internal sealed class AssuranceNavRoute(val path: String) {
    object QuickConnectRoute : AssuranceNavRoute("QuickConnect")
    object PinCodeRoute : AssuranceNavRoute("PinCode")
    object StatusRoute : AssuranceNavRoute("Status")
    object ErrorRoute : AssuranceNavRoute("Error")
    object UnknownRoute : AssuranceNavRoute("Unknown")
}
