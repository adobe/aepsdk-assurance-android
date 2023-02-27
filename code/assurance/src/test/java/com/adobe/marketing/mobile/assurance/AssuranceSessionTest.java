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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import com.adobe.marketing.mobile.MobileCore;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AssuranceSessionTest {

    AssuranceSession assuranceSession;

    @Mock Application mockApplication;

    @Mock AssuranceStateManager mockAssuranceStateManager;

    @Mock SharedPreferences mockPreferences;

    @Mock SharedPreferences.Editor mockEditor;

    @Mock OutboundEventQueueWorker mockOutboundEventQueueWorker;

    @Mock InboundEventQueueWorker mockInboundEventQueueWorker;

    @Mock AssuranceSessionOrchestrator.ApplicationHandle mockApplicationHandle;

    @Mock AssuranceConnectionDataStore mockAssuranceConnectionDataStore;

    @Mock AssuranceSessionOrchestrator.SessionUIOperationHandler mockSessionUIOperationHandler;

    @Mock AssuranceSessionPresentationManager mockAssuranceSessionPresentationManager;

    @Mock AssuranceWebViewSocket mockAssuranceWebViewSocket;

    @Mock AssurancePluginManager mockAssurancePluginManager;

    @Mock Uri mockUri;

    private MockedStatic<MobileCore> mockedStaticMobileCore;
    private MockedStatic<Uri> mockedStaticUri;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class);
        mockedStaticMobileCore.when(MobileCore::getApplication).thenReturn(mockApplication);

        mockedStaticUri = Mockito.mockStatic(Uri.class);
        mockedStaticUri.when(() -> Uri.parse(anyString())).thenReturn(mockUri);

        // mock shared preference
        when(mockApplication.getSharedPreferences(anyString(), ArgumentMatchers.anyInt()))
                .thenReturn(mockPreferences);
        when(mockPreferences.edit()).thenReturn(mockEditor);

        final String mockOrgId = "sampleOrgID";
        final String mockClientId = "sampleClientID";
        when(mockAssuranceStateManager.getOrgId(true)).thenReturn(mockOrgId);
        when(mockAssuranceStateManager.getClientId()).thenReturn(mockClientId);

        // create the instance of the griffon session
        assuranceSession =
                new AssuranceSession(
                        mockApplicationHandle,
                        mockAssuranceStateManager,
                        "SampleSessionID",
                        AssuranceConstants.AssuranceEnvironment.PROD,
                        mockAssuranceConnectionDataStore,
                        mockSessionUIOperationHandler,
                        Collections.EMPTY_LIST,
                        Collections.EMPTY_LIST);

        // Assign mocks to private fields instantiated inside the constructor
        setInternalState(
                assuranceSession,
                "assuranceSessionPresentationManager",
                mockAssuranceSessionPresentationManager);
        setInternalState(assuranceSession, "inboundEventQueueWorker", mockInboundEventQueueWorker);
        setInternalState(
                assuranceSession, "outboundEventQueueWorker", mockOutboundEventQueueWorker);
        setInternalState(assuranceSession, "socket", mockAssuranceWebViewSocket);
        setInternalState(assuranceSession, "pluginManager", mockAssurancePluginManager);
    }

    @Test
    public void test_connectWithoutPin() {
        assuranceSession.connect(null);

        verify(mockAssuranceSessionPresentationManager).onSessionInitialized();
    }

    @Test
    public void test_connect() {
        assuranceSession.connect("1234");

        String expectedURL =
                String.format(
                        "wss://connect.griffon.adobe.com/client/v1?sessionId=%s&token=%s&orgId=%s&clientId=%s",
                        "SampleSessionID",
                        "1234",
                        mockAssuranceStateManager.getOrgId(true),
                        mockAssuranceStateManager.getClientId());

        verify(mockAssuranceSessionPresentationManager).onSessionConnecting();
        verify(mockAssuranceWebViewSocket).connect(expectedURL);
    }

    @Test
    public void test_connect_orgIdFromConfigStateUnavailable() throws Exception {
        when(mockAssuranceStateManager.getOrgId(true)).thenReturn("");
        when(mockAssuranceConnectionDataStore.getStoredConnectionURL())
                .thenReturn(
                        "wss://connect.griffon.adobe.com/client/v1?"
                            + "sessionId=SampleSessionId&token=1234&orgId=StoredOrg@AdobeOrg&clientId=sampleClientID");
        when(mockUri.getQueryParameter("orgId")).thenReturn("StoredOrg@AdobeOrg");

        assuranceSession.connect("1234");

        String expectedURL =
                String.format(
                        "wss://connect.griffon.adobe.com/client/v1?sessionId=%s&token=%s&orgId=%s&clientId=%s",
                        "SampleSessionID", "1234", "StoredOrg@AdobeOrg", "sampleClientID");

        verify(mockAssuranceSessionPresentationManager).onSessionConnecting();
        verify(mockAssuranceWebViewSocket).connect(expectedURL);
    }

    @Test
    public void test_disconnect() {
        assuranceSession.disconnect();

        verify(mockAssuranceWebViewSocket).disconnect();
        verify(mockAssurancePluginManager).onSessionTerminated();
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null);
        verify(mockOutboundEventQueueWorker).stop();
        verify(mockInboundEventQueueWorker).stop();
        verify(mockAssuranceStateManager).clearAssuranceSharedState();
    }

    @Test
    public void test_onSocketConnected_InitialConnection() {
        when(mockAssuranceWebViewSocket.getConnectionURL()).thenReturn("sample.url.com");
        assuranceSession.onSocketConnected(mockAssuranceWebViewSocket);

        verify(mockAssuranceConnectionDataStore).saveConnectionURL("sample.url.com");
        verify(mockInboundEventQueueWorker).start();
        verify(mockInboundEventQueueWorker).start();
    }

    @Test
    public void test_onSocketConnected_Reconnection() {
        when(mockOutboundEventQueueWorker.start()).thenReturn(false);
        when(mockAssuranceWebViewSocket.getConnectionURL()).thenReturn("sample.url.com");
        assuranceSession.onSocketConnected(mockAssuranceWebViewSocket);

        verify(mockAssuranceConnectionDataStore).saveConnectionURL("sample.url.com");
        verify(mockOutboundEventQueueWorker).start();
        verify(mockInboundEventQueueWorker).start();
        verify(mockOutboundEventQueueWorker).sendClientInfoEvent();
    }

    @Test
    public void test_onSocketDataReceived() {
        final String sampleSocketEventData =
                "{\"eventID\":\"12345\", \"vendor\":\"\", \"type\":\"control\"}";
        assuranceSession.onSocketDataReceived(mockAssuranceWebViewSocket, sampleSocketEventData);

        verify(mockInboundEventQueueWorker, times(1)).offer(any(AssuranceEvent.class));
    }

    @Test
    public void test_onSocketDisconnected_NORMAL() {
        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.NORMAL,
                true);

        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null);
        verify(mockOutboundEventQueueWorker).stop();
        verify(mockInboundEventQueueWorker).stop();
        verify(mockAssuranceStateManager).clearAssuranceSharedState();
        verify(mockAssuranceSessionPresentationManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.NORMAL);
        verify(mockAssurancePluginManager).onSessionTerminated();
    }

    @Test
    public void test_onSocketDisconnected_ORG_MISMATCH() {
        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.ORG_MISMATCH,
                true);

        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null);
        verify(mockOutboundEventQueueWorker).stop();
        verify(mockInboundEventQueueWorker).stop();
        verify(mockAssuranceStateManager).clearAssuranceSharedState();
        verify(mockAssuranceSessionPresentationManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.ORG_MISMATCH);
        verify(mockAssurancePluginManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.ORG_MISMATCH);
        verify(mockAssurancePluginManager).onSessionTerminated();
    }

    @Test
    public void test_onSocketDisconnected_CLIENT_ERROR() {
        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.CLIENT_ERROR,
                false);

        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null);
        verify(mockOutboundEventQueueWorker).stop();
        verify(mockInboundEventQueueWorker).stop();
        verify(mockAssuranceStateManager).clearAssuranceSharedState();
        verify(mockAssuranceSessionPresentationManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.CLIENT_ERROR);
        verify(mockAssurancePluginManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.CLIENT_ERROR);
        verify(mockAssurancePluginManager).onSessionTerminated();
    }

    @Test
    public void test_onSocketDisconnected_CONNECTION_LIMIT() {
        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.CONNECTION_LIMIT,
                false);

        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null);
        verify(mockOutboundEventQueueWorker).stop();
        verify(mockInboundEventQueueWorker).stop();
        verify(mockAssuranceStateManager).clearAssuranceSharedState();
        verify(mockAssuranceSessionPresentationManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.CONNECTION_LIMIT);
        verify(mockAssurancePluginManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.CONNECTION_LIMIT);
        verify(mockAssurancePluginManager).onSessionTerminated();
    }

    @Test
    public void test_onSocketDisconnected_EVENT_LIMIT() {
        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.EVENT_LIMIT,
                false);

        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null);
        verify(mockOutboundEventQueueWorker).stop();
        verify(mockInboundEventQueueWorker).stop();
        verify(mockAssuranceStateManager).clearAssuranceSharedState();
        verify(mockAssuranceSessionPresentationManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.EVENT_LIMIT);
        verify(mockAssurancePluginManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.EVENT_LIMIT);
        verify(mockAssurancePluginManager).onSessionTerminated();
    }

    @Test
    public void test_onSocketDisconnected_SESSION_DELETED() {
        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.SESSION_DELETED,
                false);

        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null);
        verify(mockOutboundEventQueueWorker).stop();
        verify(mockInboundEventQueueWorker).stop();
        verify(mockAssuranceStateManager).clearAssuranceSharedState();
        verify(mockAssuranceSessionPresentationManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.SESSION_DELETED);
        verify(mockAssurancePluginManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.SESSION_DELETED);
        verify(mockAssurancePluginManager).onSessionTerminated();
    }

    @Test
    public void test_onSocketDisconnected_ABNORMAL() throws Exception {
        final Handler mockHandler = Mockito.mock(Handler.class);
        setInternalState(assuranceSession, "socketReconnectHandler", mockHandler);
        doAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocationOnMock)
                                    throws Throwable {
                                Runnable runnable = (Runnable) invocationOnMock.getArgument(0);
                                runnable.run();
                                return null;
                            }
                        })
                .when(mockHandler)
                .postDelayed(any(Runnable.class), anyLong());

        final String sessionID = "sampleSessionId";
        final String token = "1234";
        final String orgId = "sampleOrgId";
        final String clientID = "sampleClientId";
        final String mockConnectionURL = buildURL(sessionID, token, orgId, clientID);
        when(mockAssuranceConnectionDataStore.getStoredConnectionURL())
                .thenReturn(mockConnectionURL);

        when(mockUri.getQueryParameter("sessionId")).thenReturn((sessionID));
        when(mockUri.getQueryParameter("token")).thenReturn((token));
        when(mockUri.getQueryParameter("orgId")).thenReturn(orgId);
        when(mockUri.getQueryParameter("clientId")).thenReturn((orgId));

        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.ABNORMAL,
                true);

        verify(mockOutboundEventQueueWorker).block();
        verify(mockAssuranceSessionPresentationManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.ABNORMAL);
        verify(mockAssurancePluginManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.ABNORMAL);
        verify(mockAssuranceSessionPresentationManager).onSessionReconnecting();
        verify(mockAssuranceWebViewSocket).connect(anyString());
    }

    @Test
    public void test_onSocketDisconnected_ABNORMAL_retry() throws Exception {
        final Handler mockHandler = Mockito.mock(Handler.class);
        setInternalState(assuranceSession, "socketReconnectHandler", mockHandler);
        doAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocationOnMock)
                                    throws Throwable {
                                Runnable runnable = (Runnable) invocationOnMock.getArgument(0);
                                runnable.run();
                                return null;
                            }
                        })
                .when(mockHandler)
                .postDelayed(any(Runnable.class), anyLong());

        final String sessionID = "sampleSessionId";
        final String token = "1234";
        final String orgId = "sampleOrgId";
        final String clientID = "sampleClientId";
        final String mockConnectionURL = buildURL(sessionID, token, orgId, clientID);
        when(mockAssuranceConnectionDataStore.getStoredConnectionURL())
                .thenReturn(mockConnectionURL);
        when(mockUri.getQueryParameter("sessionId")).thenReturn((sessionID));
        when(mockUri.getQueryParameter("token")).thenReturn((token));
        when(mockUri.getQueryParameter("orgId")).thenReturn(orgId);
        when(mockUri.getQueryParameter("clientId")).thenReturn((orgId));

        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.ABNORMAL,
                true);
        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.ABNORMAL,
                true);

        verify(mockOutboundEventQueueWorker, times(2)).block();
        verify(mockAssuranceSessionPresentationManager, times(2))
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.ABNORMAL);
        verify(mockAssuranceWebViewSocket, times(2)).connect(anyString());

        // Plugins should only be notified once
        verify(mockAssurancePluginManager, times(1))
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.ABNORMAL);
        // Reconnecting message should only be printed once.
        verify(mockAssuranceSessionPresentationManager, times(1)).onSessionReconnecting();
    }

    @Test
    public void test_onSocketDisconnected_ABNORMAL_badStoredURL() throws Exception {
        final Handler mockHandler = Mockito.mock(Handler.class);
        setInternalState(assuranceSession, "socketReconnectHandler", mockHandler);
        doAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocationOnMock)
                                    throws Throwable {
                                Runnable runnable = (Runnable) invocationOnMock.getArgument(0);
                                runnable.run();
                                return null;
                            }
                        })
                .when(mockHandler)
                .postDelayed(any(Runnable.class), anyLong());

        final String sessionID = "sampleSessionId";
        final String token = null;
        final String orgId = "sampleOrgId";
        final String clientID = "sampleClientId";
        final String mockConnectionURL = buildURL(sessionID, token, orgId, clientID);
        when(mockAssuranceConnectionDataStore.getStoredConnectionURL())
                .thenReturn(mockConnectionURL);
        when(mockUri.getQueryParameter("sessionId")).thenReturn((sessionID));
        when(mockUri.getQueryParameter("token")).thenReturn((token));
        when(mockUri.getQueryParameter("orgId")).thenReturn(orgId);
        when(mockUri.getQueryParameter("clientId")).thenReturn((orgId));

        assuranceSession.onSocketDisconnected(
                mockAssuranceWebViewSocket,
                "SampleReason",
                AssuranceConstants.SocketCloseCode.ABNORMAL,
                true);

        verify(mockOutboundEventQueueWorker).block();
        verify(mockAssuranceSessionPresentationManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.ABNORMAL);
        verify(mockAssurancePluginManager)
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.ABNORMAL);
        verify(mockAssuranceSessionPresentationManager).onSessionReconnecting();
        verify(mockAssuranceSessionPresentationManager).onSessionInitialized();
    }

    @Test
    public void test_ActivityDelegates() {
        final Activity mockActivity = Mockito.mock(Activity.class);
        assuranceSession.onActivityDestroyed(mockActivity);
        verify(mockAssuranceSessionPresentationManager).onActivityDestroyed(mockActivity);

        assuranceSession.onActivityResumed(mockActivity);
        verify(mockAssuranceSessionPresentationManager).onActivityResumed(mockActivity);

        assuranceSession.onActivityStarted(mockActivity);
        verify(mockAssuranceSessionPresentationManager).onActivityStarted(mockActivity);
    }

    @After
    public void teardown() {
        mockedStaticUri.close();
        mockedStaticMobileCore.close();
    }

    private String buildURL(
            final String sessionID, final String token, final String orgId, final String clientID)
            throws Exception {
        return String.format(
                "wss://connect.griffon.adobe.com/client/v1?sessionId=%s&token=%s&orgId=%s&clientId=%s",
                (sessionID == null ? "" : sessionID),
                (token == null ? "" : token),
                (orgId == null ? "" : orgId),
                (clientID == null ? "" : clientID));
    }
}
