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

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class})
public class AssuranceTest {

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
    }

    @Test
    public void test_ExtensionVersion() {
        // test
        TestCase.assertEquals(
                AssuranceTestConstants.EXTENSION_VERSION, Assurance.extensionVersion());
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
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(dispatchedEventCaptor.capture());
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
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class));
    }

    @Test
    public void test_StartSession_NullUrl() {
        // test
        Assurance.startSession(null);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class));
    }

    @Test
    public void test_RegisterExtension() {
        // prepare
        final ArgumentCaptor<ExtensionErrorCallback> extensionErrorCallbackCaptor =
                ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        Assurance.registerExtension();

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.registerExtension(any(Class.class), extensionErrorCallbackCaptor.capture());

        // test 2 - ErrorCallback is handled without crashing
        extensionErrorCallbackCaptor.getValue().error(ExtensionError.UNEXPECTED_ERROR);
    }
}
