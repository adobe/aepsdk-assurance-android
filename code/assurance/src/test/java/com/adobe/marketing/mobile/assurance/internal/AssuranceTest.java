/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.util.DataReader;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class AssuranceTest {

    private MockedStatic<MobileCore> mockedStaticMobileCore;

    @Before
    public void setup() {
        mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class);
    }

    @Test
    public void test_ExtensionVersion() {
        // test
        TestCase.assertEquals(Assurance.EXTENSION_VERSION, Assurance.extensionVersion());
    }

    @Test
    public void test_StartSession() {
        // prepare
        final String validSessionURL =
                "assurance://?adb_validation_sessionid=de2ec9c3-9664-4c80-9ec0-bed891dc9471";
        final ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        Assurance.startSession(validSessionURL);

        // verify
        mockedStaticMobileCore.verify(
                () -> MobileCore.dispatchEvent(dispatchedEventCaptor.capture()), times(1));
        // MobileCore.dispatchEvent(dispatchedEventCaptor.capture());
        final Event dispatchedEvent = dispatchedEventCaptor.getValue();
        TestCase.assertEquals(EventType.ASSURANCE, dispatchedEvent.getType());
        TestCase.assertEquals(EventSource.REQUEST_CONTENT, dispatchedEvent.getSource());
        assertEquals("Assurance Start Session", dispatchedEvent.getName());
        assertEquals(
                validSessionURL,
                dispatchedEvent
                        .getEventData()
                        .get(AssuranceConstants.SDKEventDataKey.START_SESSION_URL));
    }

    @Test
    public void test_StartSession_InvalidUrl() {
        // test
        Assurance.startSession("Invalid");

        // verify
        mockedStaticMobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), times(0));
    }

    @Test
    public void test_StartSession_NullUrl() {
        // test
        Assurance.startSession(null);

        // verify
        mockedStaticMobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), times(0));
    }

    @Test
    public void test_startSession_quickConnect() {
        // prepare
        final ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        Assurance.startSession();

        // verify
        mockedStaticMobileCore.verify(
                () -> MobileCore.dispatchEvent(dispatchedEventCaptor.capture()), times(1));

        final Event dispatchedEvent = dispatchedEventCaptor.getValue();
        assertEquals(EventType.ASSURANCE, dispatchedEvent.getType());
        assertEquals(EventSource.REQUEST_CONTENT, dispatchedEvent.getSource());
        assertEquals("Assurance Start Session (Quick Connect)", dispatchedEvent.getName());
        assertTrue(
                DataReader.optBoolean(
                        dispatchedEvent.getEventData(),
                        AssuranceConstants.SDKEventDataKey.IS_QUICK_CONNECT,
                        false));
    }

    @After
    public void teardown() {
        mockedStaticMobileCore.close();
    }
}
