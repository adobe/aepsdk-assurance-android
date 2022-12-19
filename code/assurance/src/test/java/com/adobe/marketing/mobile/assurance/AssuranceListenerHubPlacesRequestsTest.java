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
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class AssuranceListenerHubPlacesRequestsTest {

    private static final String COUNT_KEY = "count";
    private static final String LATITUDE_KEY = "latitude";
    private static final String LONGITUDE_KEY = "longitude";

    private static final String GET_NEARBY_PLACES_EVENT_NAME = "requestgetnearbyplaces";
    private static final String RESET_PLACES_EVENT_NAME = "requestreset";

    AssuranceListenerHubPlacesRequests listener;
    @Mock private AssuranceExtension mockAssuranceExtension;

    @Before
    public void setup() {
        mockAssuranceExtension = Mockito.mock(AssuranceExtension.class);
        listener = new AssuranceListenerHubPlacesRequests(mockAssuranceExtension);
    }

    @Test
    public void testHear_GetNearByPOIEvent() {
        HashMap<String, Object> eventData = new HashMap<String, Object>();
        eventData.put(COUNT_KEY, 5);
        eventData.put(LATITUDE_KEY, 22.22);
        eventData.put(LONGITUDE_KEY, 33.33);

        Event event =
                new Event.Builder(
                                GET_NEARBY_PLACES_EVENT_NAME,
                                EventType.PLACES,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        // test
        listener.hear(event);

        // verify log message
        verify(mockAssuranceExtension, times(1))
                .logLocalUI(
                        AssuranceConstants.UILogColorVisibility.NORMAL,
                        "Places - Requesting 5 nearby POIs from (22.220000, 33.330000)");
    }

    @Test
    public void testHear_ResetPOIEvent() {
        // setup
        HashMap<String, Object> eventData = new HashMap<String, Object>();
        Event event =
                new Event.Builder(
                                RESET_PLACES_EVENT_NAME,
                                EventType.PLACES,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        // test
        listener.hear(event);

        // verify log message
        verify(mockAssuranceExtension, times(1))
                .logLocalUI(
                        AssuranceConstants.UILogColorVisibility.CRITICAL,
                        "Places - Resetting Location");
    }

    @Test
    public void testHear_GetNearByPOIEvent_whenNoEventData() {
        // setup
        Event event =
                new Event.Builder(
                                GET_NEARBY_PLACES_EVENT_NAME,
                                EventType.PLACES,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();

        // test
        listener.hear(event);

        // verify log message
        verify(mockAssuranceExtension, times(0))
                .logLocalUI(any(AssuranceConstants.UILogColorVisibility.class), anyString());
    }

    @Test
    public void testHear_whenInvalidEventData() {
        HashMap<String, Object> eventData = new HashMap<String, Object>();
        eventData.put(COUNT_KEY, "Invalid Value");
        eventData.put(LATITUDE_KEY, 22.22);
        eventData.put(LONGITUDE_KEY, 33.33);
        Event event =
                new Event.Builder(
                                GET_NEARBY_PLACES_EVENT_NAME,
                                EventType.PLACES,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        // test
        listener.hear(event);

        // verify log message
        verify(mockAssuranceExtension, times(0))
                .logLocalUI(any(AssuranceConstants.UILogColorVisibility.class), anyString());
    }
}
