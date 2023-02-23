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

import static com.adobe.marketing.mobile.assurance.AssuranceTestUtils.setInternalState;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AssuranceSessionOrchestratorTest {

    @Mock private Application mockApplication;
    @Mock private AssuranceSessionOrchestrator.ApplicationHandle mockApplicationHandle;

    @Mock private AssuranceStateManager mockAssuranceStateManager;
    @Mock private List<AssurancePlugin> mockPluginList;
    @Mock private AssuranceConnectionDataStore mockAssuranceConnectionDataStore;
    @Mock private AssuranceSessionOrchestrator.AssuranceSessionCreator mockAssuranceSessionCreator;
    @Mock private AssuranceSession mockAssuranceSession;

    private AssuranceSessionOrchestrator.HostAppActivityLifecycleObserver
            hostAppActivityLifecycleObserver;

    private AssuranceSessionOrchestrator assuranceSessionOrchestrator;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        assuranceSessionOrchestrator =
                new AssuranceSessionOrchestrator(
                        mockApplication,
                        mockAssuranceStateManager,
                        mockPluginList,
                        mockAssuranceConnectionDataStore,
                        mockApplicationHandle,
                        mockAssuranceSessionCreator);

        ArgumentCaptor<AssuranceSessionOrchestrator.HostAppActivityLifecycleObserver>
                lifecycleObserverArgumentCaptor =
                        ArgumentCaptor.forClass(
                                AssuranceSessionOrchestrator.HostAppActivityLifecycleObserver
                                        .class);

        verify(mockApplication)
                .registerActivityLifecycleCallbacks(lifecycleObserverArgumentCaptor.capture());
        hostAppActivityLifecycleObserver = lifecycleObserverArgumentCaptor.getValue();
    }

    @Test
    public void testCreateSession_WithoutPin() {
        when(mockAssuranceSessionCreator.create(
                        eq("SessionID"),
                        eq(AssuranceConstants.AssuranceEnvironment.PROD),
                        eq(assuranceSessionOrchestrator.getSessionUIOperationHandler()),
                        eq(mockAssuranceStateManager),
                        eq(mockPluginList),
                        eq(mockAssuranceConnectionDataStore),
                        eq(mockApplicationHandle),
                        ArgumentMatchers.<List<AssuranceEvent>>any()))
                .thenReturn(mockAssuranceSession);
        Assert.assertNull(assuranceSessionOrchestrator.getActiveSession());

        assuranceSessionOrchestrator.createSession(
                "SessionID", AssuranceConstants.AssuranceEnvironment.PROD, null);

        Assert.assertNotNull(assuranceSessionOrchestrator.getActiveSession());

        verify(mockAssuranceSession)
                .registerStatusListener(
                        assuranceSessionOrchestrator.getAssuranceSessionStatusListener());
        verify(mockAssuranceStateManager).shareAssuranceSharedState("SessionID");
        verify(mockAssuranceSession).connect(null);
    }

    @Test
    public void testCreateSession_WithPin() {
        when(mockAssuranceSessionCreator.create(
                        eq("SessionID"),
                        eq(AssuranceConstants.AssuranceEnvironment.PROD),
                        eq(assuranceSessionOrchestrator.getSessionUIOperationHandler()),
                        eq(mockAssuranceStateManager),
                        eq(mockPluginList),
                        eq(mockAssuranceConnectionDataStore),
                        eq(mockApplicationHandle),
                        ArgumentMatchers.<List<AssuranceEvent>>any()))
                .thenReturn(mockAssuranceSession);
        Assert.assertNull(assuranceSessionOrchestrator.getActiveSession());

        assuranceSessionOrchestrator.createSession(
                "SessionID", AssuranceConstants.AssuranceEnvironment.PROD, "1234");

        Assert.assertNotNull(assuranceSessionOrchestrator.getActiveSession());

        verify(mockAssuranceSession)
                .registerStatusListener(
                        assuranceSessionOrchestrator.getAssuranceSessionStatusListener());
        verify(mockAssuranceStateManager).shareAssuranceSharedState("SessionID");
        verify(mockAssuranceSession).connect("1234");
    }

    @Test
    public void testTerminateSession() {
        // prepare
        when(mockAssuranceSessionCreator.create(
                        eq("SessionID"),
                        eq(AssuranceConstants.AssuranceEnvironment.PROD),
                        eq(assuranceSessionOrchestrator.getSessionUIOperationHandler()),
                        eq(mockAssuranceStateManager),
                        eq(mockPluginList),
                        eq(mockAssuranceConnectionDataStore),
                        eq(mockApplicationHandle),
                        ArgumentMatchers.<List<AssuranceEvent>>any()))
                .thenReturn(mockAssuranceSession);

        assuranceSessionOrchestrator.createSession(
                "SessionID", AssuranceConstants.AssuranceEnvironment.PROD, "1234");

        // test
        assuranceSessionOrchestrator.terminateSession();

        verify(mockAssuranceStateManager).clearAssuranceSharedState();
        verify(mockAssuranceSession)
                .unregisterStatusListener(
                        assuranceSessionOrchestrator.getAssuranceSessionStatusListener());
        verify(mockAssuranceSession).disconnect();
        Assert.assertNull(assuranceSessionOrchestrator.getActiveSession());
    }

    @Test
    public void testReconnectToStoredSession_NoStoredSessionURL() {
        when(mockAssuranceConnectionDataStore.getStoredConnectionURL()).thenReturn(null);

        Assert.assertFalse(assuranceSessionOrchestrator.reconnectToStoredSession());
    }

    @Test
    public void testReconnectToStoredSession_BadStoredSessionURL() throws Exception {
        when(mockAssuranceConnectionDataStore.getStoredConnectionURL()).thenReturn("");
        Assert.assertFalse(assuranceSessionOrchestrator.reconnectToStoredSession());

        final String uriWithoutSessionId =
                "wss://connect.griffon.adobe.com/client/v1?token=9004&orgId=972C898555E9F7BC7F000101%40AdobeOrg&clientId=89942ef1-11c2-46fb-bcc7-bb797c84e638";
        when(mockAssuranceConnectionDataStore.getStoredConnectionURL())
                .thenReturn(uriWithoutSessionId);
        Assert.assertFalse(assuranceSessionOrchestrator.reconnectToStoredSession());

        final String uriWithoutToken =
                "wss://connect.griffon.adobe.com/client/v1?sessionId=5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758&orgId=972C898555E9F7BC7F000101%40AdobeOrg&clientId=89942ef1-11c2-46fb-bcc7-bb797c84e638";
        when(mockAssuranceConnectionDataStore.getStoredConnectionURL()).thenReturn(uriWithoutToken);
        Assert.assertFalse(assuranceSessionOrchestrator.reconnectToStoredSession());
    }

    @Test
    public void testReconnectToStoredSession_HasStoredSessionURL() throws Exception {
        final String connectionUrl =
                "wss://connect.griffon.adobe.com/client/v1?sessionId=5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758&token=9004&orgId=972C898555E9F7BC7F000101%40AdobeOrg&clientId=89942ef1-11c2-46fb-bcc7-bb797c84e638";
        when(mockAssuranceSessionCreator.create(
                        anyString(),
                        eq(AssuranceConstants.AssuranceEnvironment.PROD),
                        eq(assuranceSessionOrchestrator.getSessionUIOperationHandler()),
                        eq(mockAssuranceStateManager),
                        eq(mockPluginList),
                        eq(mockAssuranceConnectionDataStore),
                        eq(mockApplicationHandle),
                        ArgumentMatchers.<List<AssuranceEvent>>any()))
                .thenReturn(mockAssuranceSession);
        when(mockAssuranceConnectionDataStore.getStoredConnectionURL()).thenReturn(connectionUrl);

        Assert.assertTrue(assuranceSessionOrchestrator.reconnectToStoredSession());
        verify(mockAssuranceSessionCreator)
                .create(
                        eq("5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758"),
                        eq(AssuranceConstants.AssuranceEnvironment.PROD),
                        eq(assuranceSessionOrchestrator.getSessionUIOperationHandler()),
                        eq(mockAssuranceStateManager),
                        eq(mockPluginList),
                        eq(mockAssuranceConnectionDataStore),
                        eq(mockApplicationHandle),
                        ArgumentMatchers.<List<AssuranceEvent>>any());
    }

    @Test
    public void testQueueEvent_SessionNotEstablished() {
        final List<AssuranceEvent> mockBuffer = Mockito.mock(List.class);
        setInternalState(assuranceSessionOrchestrator, "outboundEventBuffer", mockBuffer);

        AssuranceEvent event = new AssuranceEvent("EventName", Collections.EMPTY_MAP);
        assuranceSessionOrchestrator.queueEvent(event);

        verify(mockBuffer).add(event);
    }

    @Test
    public void testQueueEvent_SessionEstablished_NotConnected() {
        final List<AssuranceEvent> mockBuffer = Mockito.mock(List.class);
        setInternalState(assuranceSessionOrchestrator, "outboundEventBuffer", mockBuffer);
        setInternalState(assuranceSessionOrchestrator, "session", mockAssuranceSession);

        AssuranceEvent event = new AssuranceEvent("EventName", Collections.EMPTY_MAP);
        assuranceSessionOrchestrator.queueEvent(event);

        verify(mockBuffer).add(event);
        verify(mockAssuranceSession).queueOutboundEvent(event);
    }

    @Test
    public void testCanProcessSDKEvents() {
        setInternalState(assuranceSessionOrchestrator, "outboundEventBuffer", (Object[]) null);
        setInternalState(assuranceSessionOrchestrator, "session", (Object[]) null);
        Assert.assertFalse(assuranceSessionOrchestrator.canProcessSDKEvents());

        final List<AssuranceEvent> mockBuffer = Mockito.mock(List.class);
        setInternalState(assuranceSessionOrchestrator, "outboundEventBuffer", mockBuffer);
        Assert.assertTrue(assuranceSessionOrchestrator.canProcessSDKEvents());

        setInternalState(assuranceSessionOrchestrator, "outboundEventBuffer", (Object[]) null);
        setInternalState(assuranceSessionOrchestrator, "session", mockAssuranceSession);
        Assert.assertTrue(assuranceSessionOrchestrator.canProcessSDKEvents());

        setInternalState(assuranceSessionOrchestrator, "outboundEventBuffer", mockBuffer);
        setInternalState(assuranceSessionOrchestrator, "session", mockAssuranceSession);
        Assert.assertTrue(assuranceSessionOrchestrator.canProcessSDKEvents());
    }

    @Test
    public void testSessionUIOperationHandler_OnConnect() {
        setInternalState(assuranceSessionOrchestrator, "session", mockAssuranceSession);

        AssuranceSessionOrchestrator.SessionUIOperationHandler sessionUIOperationHandler =
                assuranceSessionOrchestrator.getSessionUIOperationHandler();
        Assert.assertNotNull(sessionUIOperationHandler);

        sessionUIOperationHandler.onConnect("1234");

        verify(mockAssuranceSession).connect("1234");
    }

    @Test
    public void testSessionUIOperationHandler_OnDisconnect() {
        setInternalState(assuranceSessionOrchestrator, "session", mockAssuranceSession);
        AssuranceSessionOrchestrator.SessionUIOperationHandler sessionUIOperationHandler =
                assuranceSessionOrchestrator.getSessionUIOperationHandler();
        Assert.assertNotNull(sessionUIOperationHandler);

        sessionUIOperationHandler.onDisconnect();

        verify(mockAssuranceSession)
                .unregisterStatusListener(
                        assuranceSessionOrchestrator.getAssuranceSessionStatusListener());
        verify(mockAssuranceSession).disconnect();
        Assert.assertNull(assuranceSessionOrchestrator.getActiveSession());
    }
}
