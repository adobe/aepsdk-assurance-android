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

package com.adobe.marketing.mobile.assurance.internal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.compose.rememberNavController
import com.adobe.marketing.mobile.assurance.internal.AssuranceComponentRegistry
import com.adobe.marketing.mobile.assurance.internal.ui.theme.AssuranceTheme.backgroundColor

/**
 * Activity that hosts all of the Assurance UI.
 */
class AssuranceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val connectionPhase = AssuranceComponentRegistry.appState.sessionPhase.value

        setContent {
            MaterialTheme(
                content = {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize(),
                        color = backgroundColor
                    ) {
                        Box(
                            modifier = Modifier
                                .safeDrawingPadding()
                                .fillMaxSize(),
                            contentAlignment = TopCenter
                        ) {
                            // Set the status bar and navigation bar colors to be the same as the
                            // background color of Assurance screens. This is to simulate an edge to edge
                            // experience while the Assurance UI is active.
                            // TODO:
                            //  When Assurance SDK's target SDK is updated to 35 (where the
                            //  window.statusBarColor, window.navigationBarColor APIs are deprecated),
                            //  update activity-compose dependency and use enableEdgeToEdge API.
                            SideEffect {
                                with(this@AssuranceActivity) {
                                    window.statusBarColor = backgroundColor.toArgb()
                                    window.navigationBarColor = backgroundColor.toArgb()
                                }
                            }

                            // The AssuranceNavHost composable which is the entry point for the Assurance UI.
                            AssuranceNavHost(rememberNavController(), connectionPhase)
                        }
                    }
                }
            )
        }
    }
}
