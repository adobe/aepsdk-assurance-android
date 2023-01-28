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

package com.adobe.marketing.mobile.assurance.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.rememberNavController
import com.adobe.marketing.mobile.assurance.testapp.ui.views.DrawerLayout
import com.adobe.marketing.mobile.assurance.testapp.ui.navigation.NavRoutes
import com.adobe.marketing.mobile.assurance.testapp.ui.theme.AepsdkassuranceandroidTheme
import com.adobe.marketing.mobile.assurance.testapp.ui.navigation.AppNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AssuranceTestAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AepsdkassuranceandroidTheme {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()
                val drawerItems = listOf(
                    NavRoutes.AssuranceRoute,
                    NavRoutes.CoreRoute
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MenuScaffold(
                        scaffoldState = scaffoldState,
                        coroutineScope = scope,
                        drawerContent = {
                            DrawerLayout(
                                scope = scope,
                                scaffoldState = scaffoldState,
                                navController = navController,
                                drawerItems = drawerItems,
                            )
                        },
                        landingContent = { AppNavigation(scope, navController) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuScaffold(
    scaffoldState: ScaffoldState,
    coroutineScope: CoroutineScope,
    drawerContent: @Composable () -> Unit = {},
    landingContent: @Composable () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        topBar = {
            AssuranceTestAppTopBar {
                coroutineScope.launch { scaffoldState.drawerState.open() }
            }
        },
        drawerContent = { drawerContent() }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) { landingContent() }
    }
}


@Composable
private fun AssuranceTestAppTopBar(onNavIconClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.app_top_bar_title),
                textAlign = TextAlign.Center
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    onNavIconClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(id = R.string.app_top_bar_description)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}