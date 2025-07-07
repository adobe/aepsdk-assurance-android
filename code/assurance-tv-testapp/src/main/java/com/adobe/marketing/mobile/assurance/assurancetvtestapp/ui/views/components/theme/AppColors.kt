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

package com.adobe.marketing.mobile.assurance.assurancetvtestapp.ui.views.components.theme

import androidx.compose.ui.graphics.Color

object AppColors {
    // Adobe Brand Colors
    val AdobeRed = Color(0xFFFA0F00)
    val AdobeRedLight = Color(0xFFFF6B35)
    
    // Background Colors
    val CardBackground = Color(0xFF2A2A2A)
    val DarkCardBackground = Color(0xFF121212)
    val AppBackground = Color(0xFF1A1A1A)
    val DarkBackground = Color(0xFF0A0A0A)
    
    // Navigation Colors
    val SidebarDark = Color(0xFF2D2D2D)
    val SidebarMedium = Color(0xFF1F1F1F)
    val SidebarLight = Color(0xFF0F0F0F)
    
    // Text Colors
    val PrimaryText = Color.White
    val SecondaryText = Color.White.copy(alpha = 0.8f)
    val TertiaryText = Color.White.copy(alpha = 0.7f)
    
    // State Colors
    val ErrorColor = AdobeRed
    val LoadingColor = AdobeRed
    
    // Overlay Colors
    val OverlayDark = Color.Black.copy(alpha = 0.7f)
    val OverlayLight = Color.Black.copy(alpha = 0.5f)
} 