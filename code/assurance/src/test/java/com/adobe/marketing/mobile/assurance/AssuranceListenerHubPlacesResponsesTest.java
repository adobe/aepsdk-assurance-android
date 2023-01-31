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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class AssuranceListenerHubPlacesResponsesTest {

    private static final String REGION_NAME_KEY = "regionname";
    private static final String USER_WITHIN_KEY = "useriswithin";
    private static final String NEARBY_POI_KEY = "nearbypois";
    private static final String TRIGGERING_REGION_KEY = "triggeringregion";
    private static final String REGION_EVENT_TYPE_KEY = "regioneventtype";

    private static final String NEARBY_PLACES_EVENT_NAME = "responsegetnearbyplaces";
    private static final String REGION_EVENT_EVENT_NAME = "responseprocessregionevent";

    AssuranceListenerHubPlacesResponses listener;

    @Mock private AssuranceExtension mockAssuranceExtension;

    @Before
    public void setup() {
        mockAssuranceExtension = Mockito.mock(AssuranceExtension.class);
        listener = new AssuranceListenerHubPlacesResponses(mockAssuranceExtension);
    }

    @Test
    public void testHear_when_NearByPOIResponseEvent() {
        // test
        listener.hear(NEAR_BY_PLACES_EVENT());

        // verify
        verify(mockAssuranceExtension, times(1))
                .logLocalUI(
                        AssuranceConstants.UILogColorVisibility.NORMAL,
                        "Places - Found 2 nearby POIs:\n"
                                + "\t- Adobe (inside)\n"
                                + "\t- Peets coffee shop");
    }

    @Test
    public void testHear_when_RegionResponseEvent() {
        // test
        listener.hear(REGION_EVENT());

        // verify
        verify(mockAssuranceExtension, times(1))
                .logLocalUI(
                        AssuranceConstants.UILogColorVisibility.HIGH,
                        "Places - Processed exit for region \"Adobe\".");
    }

    @Test
    public void testHear_when_InvalidEvent() {
        Event event =
                new Event.Builder(null, EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();

        // test
        listener.hear(event);

        // verify
        verify(mockAssuranceExtension, times(0))
                .logLocalUI(any(AssuranceConstants.UILogColorVisibility.class), anyString());
    }

    private Event REGION_EVENT() {
        HashMap<String, Object> region = new HashMap<String, Object>();
        region.put(REGION_NAME_KEY, "Adobe");

        HashMap<String, Object> eventData = new HashMap<String, Object>();
        eventData.put(REGION_EVENT_TYPE_KEY, "exit");
        eventData.put(TRIGGERING_REGION_KEY, region);
        Event event =
                new Event.Builder(
                                REGION_EVENT_EVENT_NAME,
                                EventType.PLACES,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(eventData)
                        .build();
        return event;
    }

    private Event NEAR_BY_PLACES_EVENT() {
        Map<String, Object> poi1 = new HashMap<>();
        poi1.put(REGION_NAME_KEY, "Adobe");
        poi1.put(USER_WITHIN_KEY, true);

        Map<String, Object> poi2 = new HashMap<>();
        poi2.put(REGION_NAME_KEY, "Peets coffee shop");
        poi2.put(USER_WITHIN_KEY, false);

        List<Map<String, Object>> nearByPOIs = new ArrayList<>();
        nearByPOIs.add(poi1);
        nearByPOIs.add(poi2);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put(NEARBY_POI_KEY, nearByPOIs);
        Event event =
                new Event.Builder(
                                NEARBY_PLACES_EVENT_NAME,
                                EventType.PLACES,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(eventData)
                        .build();

        return event;
    }
}
