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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MobileCore.class)
public class AssurancePluginFakeEventGeneratorTest {

    private static final String EVENT_TYPE_FAKE_EVENT_GENERATOR = "fakeEvent";

    AssurancePluginFakeEventGenerator griffonPluginFakeEventGenerator;

    @Before
    public void testSetup() {
        griffonPluginFakeEventGenerator = new AssurancePluginFakeEventGenerator();
        PowerMockito.mockStatic(MobileCore.class);
    }

    @Test
    public void test_getVendorName() {
        String vendor = griffonPluginFakeEventGenerator.getVendor();
        assertEquals(vendor, AssuranceConstants.VENDOR_ASSURANCE_MOBILE);
    }

    @Test
    public void test_onEventReceived_Happy() {
        // setup
        HashMap<String, Object> fakeEventDetails = new HashMap<>();
        fakeEventDetails.put("eventName", "fakeEventName");
        fakeEventDetails.put("eventType", "fakeEventType");
        fakeEventDetails.put("eventSource", "fakeEventSource");
        fakeEventDetails.put(
                "eventData",
                new HashMap<String, Object>() {
                    {
                        put("payloadKey", "payloadValue");
                    }
                });

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        payload.put("detail", fakeEventDetails);

        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<ExtensionErrorCallback> extensionErrorCallbackArgumentCaptor =
                ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        griffonPluginFakeEventGenerator.onEventReceived(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture());

        assertEquals("fakeEventName", eventCaptor.getValue().getName());
        assertEquals("fakeEventType", eventCaptor.getValue().getType());
        assertEquals("fakeEventSource", eventCaptor.getValue().getSource());
        assertEquals(1, eventCaptor.getValue().getEventData().size());
        assertEquals("payloadValue", eventCaptor.getValue().getEventData().get("payloadKey"));
    }

    @Test
    public void test_onEventReceived_NoEventName() {
        // setup
        HashMap fakeEventDetails = new HashMap<String, Object>();
        fakeEventDetails.put("eventType", "fakeEventType");
        fakeEventDetails.put("eventSource", "fakeEventSource");
        fakeEventDetails.put(
                "eventData",
                new HashMap<String, Object>() {
                    {
                        put("payloadKey", "payloadValue");
                    }
                });

        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        payload.put("detail", fakeEventDetails);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        griffonPluginFakeEventGenerator.onEventReceived(event);

        // verify no event dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_onEventReceived_NoEventType() {
        // setup
        HashMap fakeEventDetails = new HashMap<String, Object>();
        fakeEventDetails.put("eventName", "fakeEventName");
        fakeEventDetails.put("eventSource", "fakeEventSource");
        fakeEventDetails.put(
                "eventData",
                new HashMap<String, Object>() {
                    {
                        put("payloadKey", "payloadValue");
                    }
                });

        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        payload.put("detail", fakeEventDetails);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        griffonPluginFakeEventGenerator.onEventReceived(event);

        // verify no event dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_onEventReceived_NoEventSource() {
        // setup
        HashMap fakeEventDetails = new HashMap<String, Object>();
        fakeEventDetails.put("eventName", "fakeEventName");
        fakeEventDetails.put("eventType", "fakeEventType");
        fakeEventDetails.put(
                "eventData",
                new HashMap<String, Object>() {
                    {
                        put("payloadKey", "payloadValue");
                    }
                });

        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        payload.put("detail", fakeEventDetails);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        griffonPluginFakeEventGenerator.onEventReceived(event);

        // verify no event dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_onEventReceived_GriffonEventWithNoControlDetails() {
        // setup
        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        griffonPluginFakeEventGenerator.onEventReceived(event);

        // verify no event dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_NoOpMethods_DoesNotCrash() {
        griffonPluginFakeEventGenerator.onRegistered(null);
        griffonPluginFakeEventGenerator.onSessionConnected();
        griffonPluginFakeEventGenerator.onSessionDisconnected(0);
        griffonPluginFakeEventGenerator.onSessionTerminated();
    }
}
