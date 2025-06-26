/*
 * Copyright 2025 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance.assurancetvtestapp.ui.views.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.adobe.marketing.mobile.assurance.assurancetvtestapp.ui.views.components.theme.AppColors
import com.adobe.marketing.mobile.assurance.assurancetvtestapp.ui.views.components.theme.AppSizes
import com.adobe.marketing.mobile.assurance.assurancetvtestapp.ui.views.components.theme.AppSpacing

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = AppSizes.LoadingIndicatorSize,
    color: Color = AppColors.LoadingColor,
    strokeWidth: Dp = AppSizes.LoadingIndicatorStroke,
    message: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = strokeWidth
        )
        
        if (message != null) {
            Spacer(modifier = Modifier.height(AppSpacing.SpacerLarge))
            androidx.tv.material3.Text(
                text = message,
                color = AppColors.PrimaryText,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CenteredLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = "Loading..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(message = message)
    }
} 