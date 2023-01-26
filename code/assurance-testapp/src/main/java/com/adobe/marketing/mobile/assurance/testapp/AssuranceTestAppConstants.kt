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

object AssuranceTestAppConstants {
    const val TAG = "AssuranceTestApp"
    const val SMALL_EVENT_PAYLOAD_FILE = "assets/assurance_event_payload_key_value_5KB.txt"
    const val LARGE_EVENT_PAYLOAD_FILE = "assets/assurance_large_event_payload_key_value_40KB.txt"
    const val LARGE_HTML_PAYLOAD_FILE = "assets/assurance_large_event_payload_key_value_html.txt"
    const val CHUNKED_EVENT_NAME = "AssuranceChunkingEvent"
    const val CHUNKED_EVENT_SOURCE = "AssuranceChunking"
    const val CHUNKED_EVENT_TYPE = "AssuranceChunking"
    const val CHUNKED_EVENT_PAYLOAD_KEY = "largePayloadKey"
    const val TRACK_ACTION_NAME = "TrackActionClicked"
    const val TRACK_STATE_NAME = "SampleState"
    const val TEST_EVENT_NAME = "TestEvent"
    const val TEST_EVENT_SOURCE = "TestSource"
    const val TEST_EVENT_TYPE = "TestType"
}