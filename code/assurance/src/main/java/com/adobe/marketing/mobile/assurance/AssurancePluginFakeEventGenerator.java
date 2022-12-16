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

package com.adobe.marketing.mobile.assurance;


import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import java.util.HashMap;
import java.util.Map;

class AssurancePluginFakeEventGenerator implements AssurancePlugin {
    private static final String LOG_TAG = "AssurancePluginFakeEventGenerator";

    @Override
    public String getVendor() {
        return AssuranceConstants.VENDOR_ASSURANCE_MOBILE;
    }

    @Override
    public String getControlType() {
        return AssuranceConstants.ControlType.FAKE_EVENT;
    }

    /** This method will be invoked only if the control event is of type "fakeEvent" */
    @Override
    public void onEventReceived(final AssuranceEvent event) {
        final HashMap<String, Object> fakeEventDetails = event.getControlDetail();

        if (fakeEventDetails == null || fakeEventDetails.isEmpty()) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "empty details obtained, Ignoring to generate fake event to eventHub");
            return;
        }

        if (!(fakeEventDetails.get("eventName") instanceof String)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Event name is invalid, Ignoring to generate fake event to eventHub");
            return;
        }

        if (!(fakeEventDetails.get("eventType") instanceof String)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Event type is invalid, Ignoring to generate fake event to eventHub");
            return;
        }

        if (!(fakeEventDetails.get("eventSource") instanceof String)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Event source is invalid, Ignoring to generate fake event to eventHub");
            return;
        }

        Map<String, Object> eventData = new HashMap<>();

        if (fakeEventDetails.get("eventData") instanceof Map) {
            eventData = (Map) fakeEventDetails.get("eventData");
        }

        final Event fakeEvent =
                new Event.Builder(
                                (String) fakeEventDetails.get("eventName"),
                                (String) fakeEventDetails.get("eventType"),
                                (String) fakeEventDetails.get("eventSource"))
                        .setEventData(eventData)
                        .build();
        MobileCore.dispatchEvent(fakeEvent);
    }

    @Override
    public void onRegistered(final AssuranceSession parentSession) {
        /* no-op */
    }

    @Override
    public void onSessionConnected() {
        /* no-op */
    }

    @Override
    public void onSessionDisconnected(final int code) {
        /* no-op */
    }

    @Override
    public void onSessionTerminated() {
        /* no-op */
    }
}
