/*
 * Copyright 2022 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance.testapp.ui.views

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.MobilePrivacyStatus
import com.adobe.marketing.mobile.assurance.testapp.ui.theme.Purple200

@Composable
fun CoreScreen() {
    Column {

        Row(
            Modifier.align(Alignment.CenterHorizontally).padding(16.dp)) {
            Text(
                text = "Privacy Status",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textDecoration = TextDecoration.Underline
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Button(
                onClick = { MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = "Opt In")
            }

            Button(
                onClick = { MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_OUT) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = "Opt Out")
            }

            Button(
                onClick = { MobileCore.setPrivacyStatus(MobilePrivacyStatus.UNKNOWN) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = "Unknown")
            }
        }

        Row {
            Divider(color = Purple200, thickness = 2.dp)
        }

        Row(
            Modifier.align(Alignment.CenterHorizontally).padding(16.dp)) {
            Text(
                text = "",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textDecoration = TextDecoration.Underline
            )
        }

    }
}