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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AssuranceStateManagerTest extends TestCase {

    @Mock private ExtensionApi mockApi;

    @Mock private Application mockApplication;

    @Mock SharedPreferences mockSharedPreference;

    @Mock SharedPreferences.Editor mockSharedPreferenceEditor;

    private AssuranceStateManager assuranceStateManager;

    private Map<String, Object> SAMPLE_STATE_DATA;
    private Map<String, Object> SAMPLE_XDM_STATE_DATA;

    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        SAMPLE_STATE_DATA = new HashMap<String, Object>();
        SAMPLE_STATE_DATA.put("stateKey", "stateValue");

        SAMPLE_XDM_STATE_DATA = new HashMap<String, Object>();
        SAMPLE_XDM_STATE_DATA.put("xdmStateKey", "xdmStateValue");

        when(mockApplication.getSharedPreferences(
                        AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreference);
        when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);
        assuranceStateManager = new AssuranceStateManager(mockApi, mockApplication);
    }

    @Test
    public void test_ShareAssuranceState() {
        // prepare
        final Event lastSDKEvent = sampleEvent();
        final ArgumentCaptor<Map<String, Object>> sharedStateCaptor =
                ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<Event> lastEventCaptor = ArgumentCaptor.forClass(Event.class);
        // simulate last SDK event
        assuranceStateManager.onSDKEvent(lastSDKEvent);

        // test
        assuranceStateManager.shareAssuranceSharedState("SampleSessionId");

        // verify
        verify(mockApi, times(1))
                .createSharedState(sharedStateCaptor.capture(), lastEventCaptor.capture());
        assertEquals(
                "SampleSessionId",
                sharedStateCaptor
                        .getValue()
                        .get(AssuranceTestConstants.SharedStateKeys.ASSURANCE_STATE_SESSION_ID));
        assertNotNull(
                sharedStateCaptor
                        .getValue()
                        .get(AssuranceTestConstants.SharedStateKeys.ASSURANCE_STATE_CLIENT_ID));
        assertNotNull(
                sharedStateCaptor
                        .getValue()
                        .get(
                                AssuranceTestConstants.SharedStateKeys
                                        .ASSURANCE_STATE_INTEGRATION_ID));
        assertEquals(lastSDKEvent, lastEventCaptor.getValue());
    }

    @Test
    public void test_shareAssuranceSharedState_emptySessionID() {
        // prepare
        final Event lastSDKEvent = sampleEvent();
        final ArgumentCaptor<Map<String, Object>> sharedStateCaptor =
                ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<Event> lastEventCaptor = ArgumentCaptor.forClass(Event.class);
        // simulate last SDK event
        assuranceStateManager.onSDKEvent(lastSDKEvent);

        // test
        assuranceStateManager.shareAssuranceSharedState("");

        // verify
        verify(mockApi, times(1))
                .createSharedState(sharedStateCaptor.capture(), lastEventCaptor.capture());
        assertNull(
                sharedStateCaptor
                        .getValue()
                        .get(AssuranceTestConstants.SharedStateKeys.ASSURANCE_STATE_SESSION_ID));
        assertNull(
                sharedStateCaptor
                        .getValue()
                        .get(
                                AssuranceTestConstants.SharedStateKeys
                                        .ASSURANCE_STATE_INTEGRATION_ID));
    }

    @Test
    public void test_clearAssuranceState() {
        final ArgumentCaptor<Map<String, Object>> sharedStateCaptor =
                ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<Event> lastEventCaptor = ArgumentCaptor.forClass(Event.class);

        // simulate active session id
        assuranceStateManager.shareAssuranceSharedState("SampleSessionId");
        reset(mockApi);

        // test
        assuranceStateManager.clearAssuranceSharedState();

        // verify
        verify(mockApi, times(1))
                .createSharedState(sharedStateCaptor.capture(), lastEventCaptor.capture());
        assertEquals(new HashMap<>(), sharedStateCaptor.getValue());
        assertNull(lastEventCaptor.getValue());
        assertNull(assuranceStateManager.getSessionId());
    }

    @Test
    public void test_geURLEncodedOrgId_validOrgID() {
        // prepare
        setConfigurationSharedStateWithOrgId("B974622245B1A30A490D4D@AdobeOrg");
        // test & verify
        assertEquals("B974622245B1A30A490D4D%40AdobeOrg", assuranceStateManager.getOrgId(true));
    }

    @Test
    public void test_geURLEncodedOrgId_emptyOrgID() {
        // prepare
        setConfigurationSharedStateWithOrgId("");
        // test & verify
        assertEquals("", assuranceStateManager.getOrgId(true));
    }

    @Test
    public void test_geURLEncodedOrgId_nonStringOrgID() {
        // prepare
        setConfigurationSharedStateWithOrgId(423424);

        // test & verify
        assertEquals("", assuranceStateManager.getOrgId(true));
    }

    @Test
    public void test_GetAllExtensionStateData() throws Exception {
        // setup
        String jsonString =
                "{\n"
                        + "  \"extensions\": {\n"
                        + "    \"com.adobe.module.configuration\": {\n"
                        + "      \"version\": \"1.8.0\",\n"
                        + "      \"friendlyName\": \"Configuration\"\n"
                        + "    },\n"
                        + "    \"com.adobe.edge.consent\": {\n"
                        + "      \"version\": \"1.0.0\"\n"
                        + // not providing friendly name for consent
                        "    },\n"
                        + "    \"com.adobe.module.places\": {\n"
                        + "      \"version\": \"1.0.0\",\n"
                        + "      \"friendlyName\": \"com.adobe.module.places\"\n"
                        + "    }\n"
                        + "  },\n"
                        + "  \"version\": \"1.8.0\"\n"
                        + "}";
        final Gson gson = new Gson();
        final Map extensionDetails = gson.fromJson(jsonString, Map.class);

        doAnswer(
                        new Answer<SharedStateResult>() {
                            @Override
                            public SharedStateResult answer(InvocationOnMock invocation)
                                    throws Throwable {
                                String extension = invocation.getArgument(0, String.class);

                                if ("com.adobe.module.places".equals(extension)) {
                                    return null;
                                } else if ("com.adobe.module.configuration".equals(extension)) {
                                    return new SharedStateResult(
                                            SharedStateStatus.SET, SAMPLE_STATE_DATA);
                                } else if ("com.adobe.module.eventhub".equals(extension)) {
                                    return new SharedStateResult(
                                            SharedStateStatus.SET, extensionDetails);
                                } else {
                                    return null;
                                }
                            }
                        })
                .when(mockApi)
                .getSharedState(
                        any(String.class),
                        nullable(Event.class),
                        any(Boolean.class),
                        any(SharedStateResolution.class));

        doAnswer(
                        new Answer<SharedStateResult>() {
                            @Override
                            public SharedStateResult answer(InvocationOnMock invocation)
                                    throws Throwable {
                                String extension = invocation.getArgument(0, String.class);

                                if ("com.adobe.edge.consent".equals(extension)) {
                                    return new SharedStateResult(
                                            SharedStateStatus.SET, SAMPLE_XDM_STATE_DATA);
                                } else {
                                    return null;
                                }
                            }
                        })
                .when(mockApi)
                .getXDMSharedState(
                        any(String.class),
                        nullable(Event.class),
                        any(Boolean.class),
                        any(SharedStateResolution.class));

        // test
        List<AssuranceEvent> stateEvents = assuranceStateManager.getAllExtensionStateData();
        assertEquals(3, stateEvents.size());

        // verify state event 1  : EventHub state
        AssuranceEvent eventHubStateEvent = stateEvents.get(0);
        assertEquals(AssuranceConstants.AssuranceEventType.GENERIC, eventHubStateEvent.type);
        assertEquals(
                "EventHub State",
                eventHubStateEvent.payload.get(
                        AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME));
        assertEquals(
                EventType.HUB,
                eventHubStateEvent.payload.get(
                        AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE));
        assertEquals(
                EventSource.SHARED_STATE,
                eventHubStateEvent.payload.get(
                        AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE));
        assertEquals(
                "com.adobe.module.eventhub",
                ((Map)
                                eventHubStateEvent.payload.get(
                                        AssuranceConstants.GenericEventPayloadKey
                                                .ACP_EXTENSION_EVENT_DATA))
                        .get("stateowner"));
        assertEquals(
                extensionDetails,
                ((Map) eventHubStateEvent.payload.get(AssuranceConstants.PayloadDataKeys.METADATA))
                        .get("state.data"));

        // verify state event 2  : Configuration with regular shared state
        final AssuranceEvent configurationStateEvent = stateEvents.get(1);
        assertEquals(
                "com.adobe.module.configuration",
                ((Map)
                                configurationStateEvent.payload.get(
                                        AssuranceConstants.GenericEventPayloadKey
                                                .ACP_EXTENSION_EVENT_DATA))
                        .get("stateowner"));
        assertEquals(
                SAMPLE_STATE_DATA,
                ((Map)
                                configurationStateEvent.payload.get(
                                        AssuranceConstants.PayloadDataKeys.METADATA))
                        .get("state.data"));

        // verify state event 3  : Consent with XDM shared state
        final AssuranceEvent consentStateEvent = stateEvents.get(2);
        assertEquals(
                "com.adobe.edge.consent",
                ((Map)
                                consentStateEvent.payload.get(
                                        AssuranceConstants.GenericEventPayloadKey
                                                .ACP_EXTENSION_EVENT_DATA))
                        .get("stateowner"));
        assertEquals(
                SAMPLE_XDM_STATE_DATA,
                ((Map) consentStateEvent.payload.get(AssuranceConstants.PayloadDataKeys.METADATA))
                        .get("xdm.state.data"));
    }

    @Test
    public void test_getAllExtensionStateData_noRegisteredExtensions() throws Exception {
        // setup
        Map extensionDetails = Collections.EMPTY_MAP;
        SharedStateResult res = new SharedStateResult(SharedStateStatus.SET, extensionDetails);
        when(mockApi.getSharedState(
                        eq(AssuranceConstants.SDKSharedStateName.EVENTHUB),
                        any(Event.class),
                        any(Boolean.class),
                        any(SharedStateResolution.class)))
                .thenReturn(res);

        // test
        List<AssuranceEvent> stateEvents = assuranceStateManager.getAllExtensionStateData();
        assertEquals(0, stateEvents.size());
    }

    private Event sampleEvent() {
        return new Event.Builder("Mars landing event", EventType.ACQUISITION, EventSource.OS)
                .build();
    }

    private void setConfigurationSharedStateWithOrgId(final Object orgID) {
        final Map<String, Object> configSharedState = new HashMap<>();

        if (orgID != null) {
            configSharedState.put(AssuranceTestConstants.SDKConfigurationKey.ORG_ID, orgID);
        }

        SharedStateResult res = new SharedStateResult(SharedStateStatus.SET, configSharedState);
        doReturn(res)
                .when(mockApi)
                .getSharedState(
                        AssuranceTestConstants.SDKSharedStateName.CONFIGURATION,
                        null,
                        false,
                        SharedStateResolution.ANY);
    }
}
