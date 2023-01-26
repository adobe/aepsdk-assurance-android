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

package com.adobe.marketing.mobile.assurance.testapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.assurance.testapp.AssuranceTestAppConstants

internal class AssuranceTestAppViewModel : ViewModel() {


    /**
     * Send an event with the payload read from the resourceName provided.
     */
    fun sendEvent(resourceName: String) {
        val contentToSend = this.javaClass.classLoader?.getResource(resourceName)?.readText()
        if (contentToSend != null) {
            MobileCore.dispatchEvent(
                Event.Builder(
                    AssuranceTestAppConstants.CHUNKED_EVENT_NAME,
                    AssuranceTestAppConstants.CHUNKED_EVENT_TYPE,
                    AssuranceTestAppConstants.CHUNKED_EVENT_SOURCE).setEventData(
                    mapOf(
                        AssuranceTestAppConstants.CHUNKED_EVENT_PAYLOAD_KEY to contentToSend
                    )
                ).build()
            )
        } else {
            Log.e(AssuranceTestAppConstants.TAG, "Cannot send event. Failed to read content from: $resourceName")
        }
    }
}