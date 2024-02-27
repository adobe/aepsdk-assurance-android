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

package com.adobe.marketing.mobile.assurance.internal.ui.pin.dialpad

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceUiTestTags
import com.adobe.marketing.mobile.assurance.internal.ui.pin.PinScreenAction
import com.adobe.marketing.mobile.util.StreamUtils

/**
 * Represents a row of miscellaneous symbols and numbers. Used to display the 0, delete, and empty
 * buttons i.e the last row of the dial pad.
 * @param onClick the callback invoked when the button is clicked
 */
@Composable
internal fun SymbolRow(onClick: (PinScreenAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .testTag(AssuranceUiTestTags.PinScreen.SYMBOL_ROW),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Intentionally empty content button for filling the grid evenly
        DialPadButton(
            content = { Text(text = "") },
            borderColor = Color.Transparent,
            modifier = Modifier
                .aspectRatio(1f)
                .weight(1f)
        ) { /* no-op */ }

        // 0 Button
        DialPadButton(
            content = {
                Text(
                    text = "0",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            borderColor = Color.White,
            modifier = Modifier
                .aspectRatio(1f)
                .weight(1f)
        ) {
            onClick(
                PinScreenAction.Number("0")
            )
        }

        // Delete Button
        val context = LocalContext.current
        val deleteButtonBase64 = remember { StreamUtils.readAsString(context.assets.open("PinPadDeleteIcon.txt")) }
        val imageBytes = remember { Base64.decode(deleteButtonBase64, 0) }
        val bitmap = remember { BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) }
        DialPadButton(
            content = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Delete"
                )
            },
            borderColor = Color.Transparent,
            modifier = Modifier
                .aspectRatio(1f)
                .weight(1f)
        ) { onClick(PinScreenAction.Delete) }
    }
}
