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
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.assurance.AssuranceConstants.UILogColorVisibility;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
class AssuranceListenerHubPlacesRequests implements ExtensionEventListener {
    private static final String LOG_TAG = "AssuranceListenerHubPlacesRequests";
    private static final String COUNT_KEY = "count";
    private static final String LATITUDE_KEY = "latitude";
    private static final String LONGITUDE_KEY = "longitude";

    private static final String GET_NEARBY_PLACES_EVENT_NAME = "requestgetnearbyplaces";
    private static final String RESET_PLACES_EVENT_NAME = "requestreset";

    private final AssuranceExtension parent;

    AssuranceListenerHubPlacesRequests(AssuranceExtension extension) {
        this.parent = extension;
    }

    @Override
    public void hear(final Event event) {
        final String eventName = event.getName();
        final Map<String, Object> eventData = event.getEventData();

        if (eventName == null) {
            Log.debug(Assurance.LOG_TAG, LOG_TAG, "[hear] Event name is null");
            return;
        }

        if (eventName.equals(GET_NEARBY_PLACES_EVENT_NAME)) {
            if (AssuranceUtil.isNullOrEmpty(eventData)) {
                Log.debug(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "[hear] for event requestgetnearbyplaces - Event data is null");
                return;
            }

            try {
                final int count = DataReader.getInt(eventData, COUNT_KEY);
                final double latitude = DataReader.getDouble(eventData, LATITUDE_KEY);
                final double longitude = DataReader.getDouble(eventData, LONGITUDE_KEY);
                final String logMessage =
                        String.format(
                                Locale.US,
                                "Places - Requesting %d nearby POIs from (%.6f, %.6f)",
                                count,
                                latitude,
                                longitude);
                parent.logLocalUI(UILogColorVisibility.NORMAL, logMessage);
            } catch (final DataReaderException ex) {
                Log.warning(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Unable to log-local Places event: " + ex.getLocalizedMessage());
            }
        } else if (eventName.equals(RESET_PLACES_EVENT_NAME)) {
            parent.logLocalUI(UILogColorVisibility.CRITICAL, "Places - Resetting Location");
        }
    }
}
