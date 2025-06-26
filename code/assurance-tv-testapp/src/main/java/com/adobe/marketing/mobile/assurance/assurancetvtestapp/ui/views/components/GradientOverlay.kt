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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.assurance.assurancetvtestapp.ui.views.components.theme.AppColors

@Composable
fun GradientOverlay(
    modifier: Modifier = Modifier,
    gradient: Brush
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    )
}

object GradientPresets {
    // App Background Gradients
    val AppBackground = Brush.verticalGradient(
        colors = listOf(
            AppColors.AppBackground,
            AppColors.DarkBackground
        )
    )
    
    // Sidebar Background Gradient
    val SidebarBackground = Brush.verticalGradient(
        colors = listOf(
            AppColors.SidebarDark,
            AppColors.SidebarMedium,
            AppColors.SidebarLight
        )
    )
    
    // Video Card Overlay (for better text visibility)
    val VideoCardOverlay = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            AppColors.OverlayDark
        ),
        startY = 0f,
        endY = 150f
    )
    
    // Video Player Overlay
    val VideoPlayerOverlay = Brush.verticalGradient(
        colors = listOf(
            AppColors.OverlayLight,
            Color.Transparent,
            Color.Transparent,
            AppColors.OverlayLight
        )
    )
    
    // Adobe Brand Accent Gradient
    val AdobeAccent = Brush.horizontalGradient(
        colors = listOf(
            AppColors.AdobeRed,
            AppColors.AdobeRedLight
        )
    )
}

// Convenience composables for common gradients
@Composable
fun AppBackgroundGradient(modifier: Modifier = Modifier) {
    GradientOverlay(modifier = modifier, gradient = GradientPresets.AppBackground)
}

@Composable
fun SidebarBackgroundGradient(modifier: Modifier = Modifier) {
    GradientOverlay(modifier = modifier, gradient = GradientPresets.SidebarBackground)
}

@Composable
fun VideoCardOverlay(modifier: Modifier = Modifier) {
    GradientOverlay(modifier = modifier, gradient = GradientPresets.VideoCardOverlay)
}

@Composable
fun VideoPlayerOverlay(modifier: Modifier = Modifier) {
    GradientOverlay(modifier = modifier, gradient = GradientPresets.VideoPlayerOverlay)
} 