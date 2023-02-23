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
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AssuranceExtensionTest {
    private static final String START_URL_QUERY_KEY_SESSION_ID = "adb_validation_sessionid";

    static final String EXTENSION_NAME = "com.adobe.assurance";
    private Map<String, Object> SAMPLE_STATE_DATA;
    private Map<String, Object> SAMPLE_XDM_STATE_DATA;

    @Mock private ExtensionApi mockApi;

    @Mock private Application mockApplication;

    @Mock private AssuranceSession mockSession;

    @Mock private Uri mockUri;

    @Mock SharedPreferences mockSharedPreference;

    @Mock SharedPreferences.Editor mockSharedPreferenceEditor;

    @Mock AssuranceStateManager mockAssuranceStateManager;

    @Mock AssuranceConnectionDataStore mockAssuranceConnectionDataStore;

    @Mock AssuranceSessionOrchestrator mockAssuranceSessionOrchestrator;

    private MockedStatic<Uri> mockedStaticUri;
    private MockedStatic<MobileCore> mockedStaticMobileCore;

    AssuranceExtension assuranceExtension;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        SAMPLE_STATE_DATA = new HashMap<>();
        SAMPLE_STATE_DATA.put("stateKey", "stateValue");

        SAMPLE_XDM_STATE_DATA = new HashMap<>();
        SAMPLE_XDM_STATE_DATA.put("xdmStateKey", "xdmStateValue");

        mockedStaticUri = Mockito.mockStatic(Uri.class);
        mockedStaticUri.when(() -> Uri.parse(anyString())).thenReturn(mockUri);

        MobileCore.setApplication(mockApplication);

        mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class);
        mockedStaticMobileCore.when(MobileCore::getApplication).thenReturn(mockApplication);
        Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
        Mockito.when(
                        mockApplication.getSharedPreferences(
                                AssuranceConstants.DataStoreKeys.DATASTORE_NAME,
                                Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);

        assuranceExtension =
                new AssuranceExtension(
                        mockApi,
                        mockAssuranceStateManager,
                        mockAssuranceConnectionDataStore,
                        mockAssuranceSessionOrchestrator);
        assuranceExtension.onRegistered();
    }

    @Test
    public void test_AssuranceExtensionShutsDownAfter5Seconds() throws Exception {
        // wait for 5 seconds
        Thread.sleep(TimeUnit.SECONDS.toMillis(6L));

        verify(mockAssuranceSessionOrchestrator, times(1)).terminateSession();
    }

    @Test
    public void test_AssuranceDoesNotShutDown_When_StartSessionAPICalled() throws Exception {
        when(mockAssuranceSessionOrchestrator.getActiveSession()).thenReturn(null);
        when(mockUri.getQueryParameter(START_URL_QUERY_KEY_SESSION_ID))
                .thenReturn(("6b55294e-32d4-49e8-9279-e3fe12a9d309"));

        assuranceExtension.startSession(
                "griffon://?adb_validation_sessionid=6b55294e-32d4-49e8-9279-e3fe12a9d309");
        Thread.sleep(TimeUnit.SECONDS.toMillis(6L));

        // verify that the extension is still running
        verify(mockAssuranceSessionOrchestrator, never()).terminateSession();
        verify(mockAssuranceSessionOrchestrator, times(1))
                .createSession(
                        "6b55294e-32d4-49e8-9279-e3fe12a9d309",
                        AssuranceConstants.AssuranceEnvironment.PROD,
                        null);
    }

    @Test
    public void test_StartSession_InvalidSessionID() throws Exception {
        // setup
        when(mockAssuranceSessionOrchestrator.getActiveSession()).thenReturn(null);
        when(mockUri.getQueryParameter(START_URL_QUERY_KEY_SESSION_ID)).thenReturn((null));
        final String DEEPLINK1 = "griffon://?adb_validation_sessionid=";
        final String DEEPLINK2 = "";

        // test
        assuranceExtension.startSession(DEEPLINK1);
        assuranceExtension.startSession(DEEPLINK2);

        // verify that shared state is not created
        verify(mockAssuranceSessionOrchestrator, times(0))
                .createSession(
                        anyString(),
                        any(AssuranceConstants.AssuranceEnvironment.class),
                        anyString());
    }

    @Test
    public void test_StartSession_SessionAlreadyExists() throws Exception {
        // setup
        when(mockAssuranceSessionOrchestrator.getActiveSession()).thenReturn(mockSession);
        final String DEEPLINK = "griffon://?adb_validation_sessionid=12345";

        // test
        assuranceExtension.startSession(DEEPLINK);

        // verify that shared state is not created
        verify(mockAssuranceSessionOrchestrator, times(0))
                .createSession(
                        anyString(),
                        any(AssuranceConstants.AssuranceEnvironment.class),
                        anyString());
    }

    @Test
    public void testStartSessionBogusUrl() {
        try {
            Assurance.startSession("adb_validation_sessionid");
        } catch (Exception e) {
            fail(); // should not throw
        }
    }

    @Test
    public void testProcessWildcardEvent() {
        // setup
        when(mockAssuranceSessionOrchestrator.getActiveSession()).thenReturn(null);
        final ArgumentCaptor<AssuranceEvent> assuranceEventCaptor =
                ArgumentCaptor.forClass(AssuranceEvent.class);
        Event event =
                new Event.Builder("Mars landing event", EventType.ACQUISITION, EventSource.OS)
                        .build();

        // test
        assuranceExtension.handleWildcardEvent(event);

        // verify if assurance event is queued to the session
        verify(mockAssuranceSessionOrchestrator, times(1))
                .queueEvent(assuranceEventCaptor.capture());
        AssuranceEvent capturedEvent = assuranceEventCaptor.getValue();
        assertNotNull(capturedEvent);

        // verify assurance event has correct vendor and type
        assertEquals(AssuranceTestConstants.VENDOR_ASSURANCE_MOBILE, capturedEvent.vendor);
        assertEquals(AssuranceTestConstants.AssuranceEventType.GENERIC, capturedEvent.type);
        assertNotNull(capturedEvent.eventID);

        // Verify the assurance event payload
        assertEquals(
                "Mars landing event",
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME));
        assertEquals(
                EventType.ACQUISITION.toLowerCase(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE));
        assertEquals(
                EventSource.OS.toLowerCase(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE));
        assertEquals(
                event.getUniqueIdentifier(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey
                                .ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER));
        assertEquals(
                event.getEventData(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA));
    }

    @Test
    public void testProcessSharedStateEvent_RegularSharedState() throws Exception {
        // setup
        HashMap<String, Object> sharedStateChangeOwner = new HashMap<String, Object>();
        sharedStateChangeOwner.put("stateowner", "superextension");
        when(mockAssuranceSessionOrchestrator.getActiveSession()).thenReturn(mockSession);
        Mockito.doReturn(new SharedStateResult(SharedStateStatus.SET, SAMPLE_STATE_DATA))
                .when(mockApi)
                .getSharedState(
                        anyString(),
                        nullable(Event.class),
                        any(Boolean.class),
                        any(SharedStateResolution.class));
        final ArgumentCaptor<AssuranceEvent> assuranceEventCaptor =
                ArgumentCaptor.forClass(AssuranceEvent.class);
        Event event =
                new Event.Builder(
                                AssuranceConstants.SDKEventName.SHARED_STATE_CHANGE,
                                EventType.HUB,
                                EventSource.SHARED_STATE)
                        .setEventData(sharedStateChangeOwner)
                        .build();

        // test
        assuranceExtension.handleWildcardEvent(event);

        // verify if assurance event is queued to the session
        verify(mockAssuranceSessionOrchestrator, times(1))
                .queueEvent(assuranceEventCaptor.capture());
        AssuranceEvent capturedEvent = assuranceEventCaptor.getValue();
        assertNotNull(capturedEvent);

        // verify assurance event has correct vendor and type
        assertEquals(AssuranceTestConstants.VENDOR_ASSURANCE_MOBILE, capturedEvent.vendor);
        assertEquals(AssuranceTestConstants.AssuranceEventType.GENERIC, capturedEvent.type);
        assertNotNull(capturedEvent.eventID);

        // Verify the shared state content event payload
        assertEquals(
                "Shared state change",
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME));
        assertEquals(
                EventType.HUB.toLowerCase(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE));
        assertEquals(
                EventSource.SHARED_STATE.toLowerCase(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE));
        assertEquals(
                event.getUniqueIdentifier(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey
                                .ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER));
        assertEquals(
                sharedStateChangeOwner,
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA));

        // unpack and verify shared state content in assurance event
        HashMap<String, Object> payloadMetadata =
                (HashMap<String, Object>)
                        capturedEvent.payload.get(AssuranceTestConstants.PayloadDataKeys.METADATA);
        HashMap<String, Object> sharedStateContent =
                (HashMap<String, Object>)
                        payloadMetadata.get(AssuranceTestConstants.PayloadDataKeys.STATE_CONTENTS);
        assertEquals(SAMPLE_STATE_DATA, sharedStateContent);
    }

    @Test
    public void testProcessSharedStateEvent_XDMSharedState() throws Exception {
        // setup
        HashMap<String, Object> sharedStateChangeOwner = new HashMap<String, Object>();
        sharedStateChangeOwner.put("stateowner", "superextension");
        when(mockAssuranceSessionOrchestrator.getActiveSession()).thenReturn(mockSession);
        doReturn(new SharedStateResult(SharedStateStatus.SET, SAMPLE_STATE_DATA))
                .when(mockApi)
                .getXDMSharedState(
                        anyString(),
                        nullable(Event.class),
                        any(Boolean.class),
                        any(SharedStateResolution.class));
        final ArgumentCaptor<AssuranceEvent> assuranceEventCaptor =
                ArgumentCaptor.forClass(AssuranceEvent.class);
        Event event =
                new Event.Builder(
                                AssuranceConstants.SDKEventName.XDM_SHARED_STATE_CHANGE,
                                EventType.HUB,
                                EventSource.SHARED_STATE)
                        .setEventData(sharedStateChangeOwner)
                        .build();

        // test
        assuranceExtension.handleWildcardEvent(event);

        // verify if assurance event is queued to the session
        verify(mockAssuranceSessionOrchestrator, times(1))
                .queueEvent(assuranceEventCaptor.capture());
        AssuranceEvent capturedEvent = assuranceEventCaptor.getValue();
        assertNotNull(capturedEvent);

        // verify assurance event has correct vendor and type
        assertEquals(AssuranceTestConstants.VENDOR_ASSURANCE_MOBILE, capturedEvent.vendor);
        assertEquals(AssuranceTestConstants.AssuranceEventType.GENERIC, capturedEvent.type);
        assertNotNull(capturedEvent.eventID);

        // Verify the shared state content event payload
        assertEquals(
                "Shared state change (XDM)",
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME));
        assertEquals(
                EventType.HUB.toLowerCase(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE));
        assertEquals(
                EventSource.SHARED_STATE.toLowerCase(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE));
        assertEquals(
                event.getUniqueIdentifier(),
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey
                                .ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER));
        assertEquals(
                sharedStateChangeOwner,
                capturedEvent.payload.get(
                        AssuranceTestConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA));

        // unpack and verify shared state content in assurance event
        HashMap<String, Object> payloadMetadata =
                (HashMap<String, Object>)
                        capturedEvent.payload.get(AssuranceTestConstants.PayloadDataKeys.METADATA);
        HashMap<String, Object> sharedStateContent =
                (HashMap<String, Object>)
                        payloadMetadata.get(AssuranceTestConstants.PayloadDataKeys.XDM_STATE_DATA);
        assertEquals(SAMPLE_STATE_DATA, sharedStateContent);
    }

    @Test
    public void testProcessSharedStateEvent_WhenInvalidSharedStateEvent() {
        // setup
        HashMap<String, Object> invalidStateChangeOwnerData = new HashMap<String, Object>();
        invalidStateChangeOwnerData.put("InvalidOwnerKey", "superextension");
        when(mockAssuranceSessionOrchestrator.getActiveSession()).thenReturn(null);
        Event event =
                new Event.Builder("Shared State Event", EventType.HUB, EventSource.SHARED_STATE)
                        .setEventData(invalidStateChangeOwnerData)
                        .build(); // no shared state change owner is provided

        // test
        assuranceExtension.handleWildcardEvent(event);

        // verify if assurance event is queued to the session
        verify(mockAssuranceSessionOrchestrator, times(0)).queueEvent(any(AssuranceEvent.class));
    }

    @Test
    public void testProcessSharedStateEvent_invalidSharedStateEvent_noOwner() {
        // setup
        Event event =
                new Event.Builder("Shared State Event", EventType.HUB, EventSource.SHARED_STATE)
                        .setEventData(null)
                        .build(); // no shared state change owner is provided

        // test
        assuranceExtension.handleWildcardEvent(event);

        // verify if assurance event is queued to the session
        verify(mockAssuranceSessionOrchestrator, times(0)).queueEvent(any(AssuranceEvent.class));
    }

    @Test
    public void test_logLocalUI() {
        // prepare
        when(mockAssuranceSessionOrchestrator.getActiveSession()).thenReturn(mockSession);

        // test
        assuranceExtension.logLocalUI(AssuranceConstants.UILogColorVisibility.HIGH, "dummy");

        // verify
        verify(mockSession, times(1))
                .logLocalUI(AssuranceConstants.UILogColorVisibility.HIGH, "dummy");
    }

    @Test
    public void test_OnRegistered_WhenSessionIDAvailable() {
        // setup
        when(mockAssuranceStateManager.getSessionId()).thenReturn("SampleSessionId");

        assuranceExtension.onRegistered();

        // verify that the shared state is shared
        verify(mockAssuranceStateManager, times(1)).shareAssuranceSharedState("SampleSessionId");
    }

    @Test
    public void test_OnRegistered_WhenSessionNotAvailable() {
        // setup
        when(mockAssuranceStateManager.getSessionId()).thenReturn(null);

        assuranceExtension.onRegistered();
        // verify that the shared state is not shared
        verify(mockAssuranceStateManager, times(0)).shareAssuranceSharedState(anyString());
    }

    @After
    public void teardown() {
        mockedStaticMobileCore.close();
        mockedStaticUri.close();
    }
}
