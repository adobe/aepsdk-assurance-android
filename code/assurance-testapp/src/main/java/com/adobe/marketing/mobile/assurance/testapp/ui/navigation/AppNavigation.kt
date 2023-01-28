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

package com.adobe.marketing.mobile.assurance.testapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.adobe.marketing.mobile.assurance.testapp.ui.viewmodel.AssuranceTestAppViewModel
import com.adobe.marketing.mobile.assurance.testapp.ui.views.AssuranceScreen
import com.adobe.marketing.mobile.assurance.testapp.ui.views.CoreScreen
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun AppNavigation(
    scope: CoroutineScope,
    navHostController: NavHostController,
    assuranceTestAppViewModel: AssuranceTestAppViewModel = viewModel()
) {
    NavHost(navHostController, NavRoutes.AssuranceRoute.route) {
        composable(NavRoutes.AssuranceRoute.route) {
            AssuranceScreen(scope, assuranceTestAppViewModel)
        }

        composable(NavRoutes.CoreRoute.route) {
            CoreScreen()
        }
    }
}