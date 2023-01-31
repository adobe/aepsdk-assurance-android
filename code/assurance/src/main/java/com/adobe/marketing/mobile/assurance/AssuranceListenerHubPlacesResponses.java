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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
class AssuranceListenerHubPlacesResponses implements ExtensionEventListener {
    private static final String LOG_TAG = "AssuranceListenerHubPlacesResponses";
    private static final String REGION_NAME_KEY = "regionname";
    private static final String USER_WITHIN_KEY = "useriswithin";
    private static final String NEARBY_POI_KEY = "nearbypois";
    private static final String TRIGGERING_REGION_KEY = "triggeringregion";
    private static final String REGION_EVENT_TYPE_KEY = "regioneventtype";

    private static final String NEARBY_PLACES_EVENT_NAME = "responsegetnearbyplaces";
    private static final String REGION_EVENT_EVENT_NAME = "responseprocessregionevent";

    private final AssuranceExtension parent;

    AssuranceListenerHubPlacesResponses(AssuranceExtension extension) {
        this.parent = extension;
    }

    @Override
    public void hear(final Event event) {
        final String eventName = event.getName();
        final Map<String, Object> eventData = event.getEventData();

        if (eventName == null || eventData == null) {
            Log.debug(Assurance.LOG_TAG, LOG_TAG, "[hear] Event data is null");
            return;
        }

        if (eventName.equals(NEARBY_PLACES_EVENT_NAME)) {
            final List<Map<?, ?>> nearbyPois =
                    DataReader.optTypedList(Map.class, eventData, NEARBY_POI_KEY, new ArrayList());
            final StringBuilder sb = new StringBuilder();
            sb.append(
                    String.format(
                            Locale.US,
                            "Places - Found %d nearby POIs%s",
                            nearbyPois.size(),
                            !nearbyPois.isEmpty() ? ":" : "."));

            for (final Map<?, ?> poiMap : nearbyPois) {
                if (AssuranceUtil.isStringMap(poiMap)) {
                    final Map<String, Object> poi = (Map<String, Object>) poiMap;
                    final String region = DataReader.optString(poi, REGION_NAME_KEY, null);

                    if (region != null) {
                        final boolean userIsInside =
                                DataReader.optBoolean(poi, USER_WITHIN_KEY, false);
                        sb.append(
                                String.format(
                                        Locale.US,
                                        "\n\t- %s%s",
                                        region,
                                        userIsInside ? " (inside)" : ""));
                    }
                }
            }

            parent.logLocalUI(UILogColorVisibility.NORMAL, sb.toString());
        } else if (eventName.equals(REGION_EVENT_EVENT_NAME)) {
            final Map<String, Object> triggeringRegion =
                    DataReader.optTypedMap(
                            Object.class, eventData, TRIGGERING_REGION_KEY, new HashMap());
            final String regionName = DataReader.optString(triggeringRegion, REGION_NAME_KEY, null);

            if (regionName != null) {
                final String regionEventType =
                        DataReader.optString(eventData, REGION_EVENT_TYPE_KEY, "");
                final String logMessage =
                        String.format(
                                Locale.US,
                                "Places - Processed %s for region \"%s\".",
                                regionEventType,
                                regionName != null ? regionName : "");
                parent.logLocalUI(UILogColorVisibility.HIGH, logMessage);
            }
        }
    }
}
