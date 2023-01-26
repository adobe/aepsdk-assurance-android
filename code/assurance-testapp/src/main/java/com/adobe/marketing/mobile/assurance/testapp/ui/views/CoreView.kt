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

package com.adobe.marketing.mobile.assurance.testapp.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.MobilePrivacyStatus
import com.adobe.marketing.mobile.assurance.testapp.AssuranceTestAppConstants
import com.adobe.marketing.mobile.assurance.testapp.R

@Composable
internal fun CoreScreen() {
    Column {

        Row() {
            PrivacySection()
        }

        Row() {
            EventSection()
        }

    }
}

@Composable
private fun PrivacySection() {
    Column() {
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.privacy_status_section_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textDecoration = TextDecoration.Underline
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Button(
                onClick = { MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = stringResource(id = R.string.privacy_status_button_opt_in))
            }

            Button(
                onClick = { MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_OUT) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = stringResource(id = R.string.privacy_status_button_opt_out))
            }

            Button(
                onClick = { MobileCore.setPrivacyStatus(MobilePrivacyStatus.UNKNOWN) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = stringResource(id = R.string.privacy_status_button_unknown))
            }
        }
    }
}

@Composable
private fun EventSection() {
    Column() {
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.event_section_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textDecoration = TextDecoration.Underline
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Button(
                onClick = {
                    MobileCore.trackAction(
                        AssuranceTestAppConstants.TRACK_ACTION_NAME,
                        mapOf("sampleKey" to "sampleValue")
                    )
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = stringResource(id = R.string.track_action_button))
            }

            Button(
                onClick = {
                    MobileCore.trackState(
                        AssuranceTestAppConstants.TRACK_STATE_NAME,
                        mapOf("sampleKey" to "sampleValue")
                    )
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = stringResource(id = R.string.track_state_button))
            }

            Button(
                onClick = {
                    MobileCore.dispatchEvent(
                        Event.Builder(
                            AssuranceTestAppConstants.TEST_EVENT_NAME,
                            AssuranceTestAppConstants.TEST_EVENT_TYPE,
                            AssuranceTestAppConstants.TEST_EVENT_SOURCE
                        )
                            .setEventData(mapOf("sampleKey" to "sampleValue"))
                            .build()
                    )
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = stringResource(id = R.string.dispatch_event_button))
            }
        }
    }
}

