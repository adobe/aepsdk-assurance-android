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

package com.adobe.marketing.mobile.assurance.internal.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object AssuranceTheme {
    internal val backgroundColor = Color(0xFF1C1F28)
    internal val statusLogBackgroundColor = Color(4281743682)
    internal val dimensions = Dimensions
    internal val typography = Fonts
}

/**
 * A collection of dimensions used in the Assurance UI.
 */
internal object Dimensions {

    internal class Padding(
        val xSmall: Dp,
        val small: Dp,
        val medium: Dp,
        val large: Dp,
        val xLarge: Dp,
        val xxLarge: Dp
    )

    internal class Spacing(
        val small: Dp,
        val medium: Dp,
        val large: Dp,
        val xLarge: Dp
    )

    internal class Size(
        val small: Int,
        val medium: Int,
        val large: Int,
        val xLarge: Int,
        val xxLarge: Int
    )

    internal class Button(
        val height: Size,
        val width: Size
    )

    internal val padding = Padding(
        xSmall = 4.dp,
        small = 8.dp,
        medium = 16.dp,
        large = 24.dp,
        xLarge = 32.dp,
        xxLarge = 64.dp
    )

    internal val spacing = Spacing(
        small = 20.dp,
        medium = 24.dp,
        large = 32.dp,
        xLarge = 64.dp
    )

    internal val button = Button(
        height = Size(
            small = 32,
            medium = 40,
            large = 48,
            xLarge = 56,
            xxLarge = 64
        ),
        width = Size(
            small = 40,
            medium = 80,
            large = 120,
            xLarge = 160,
            xxLarge = 200
        )
    )
}

internal object Fonts {
    internal class Font(
        val size: Dimensions.Size,
        val family: FontFamily,
    )

    internal val font = Font(
        size = Dimensions.Size(
            small = 12,
            medium = 14,
            large = 16,
            xLarge = 18,
            xxLarge = 20
        ),
        family = FontFamily.SansSerif
    )
}
