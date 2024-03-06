/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import com.adobe.marketing.mobile.assurance.internal.AssuranceTestUtils.setInternalState
import com.adobe.marketing.mobile.services.AppContextService
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.ui.FloatingButton
import com.adobe.marketing.mobile.services.ui.Presentable
import com.adobe.marketing.mobile.services.ui.UIService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.eq
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AssuranceSessionTest {

    @Mock
    private lateinit var mockAssuranceStateManager: AssuranceStateManager

    @Mock
    private lateinit var mockAssuranceConnectionDataStore: AssuranceConnectionDataStore

    @Mock
    private lateinit var mockAssuranceSessionPresentationManager: AssuranceSessionPresentationManager

    @Mock
    private lateinit var mockAuthorizingPresentationDelegate: AssuranceSessionStatusListener

    @Mock
    private lateinit var mockSocket: AssuranceWebViewSocket

    @Mock
    private lateinit var mockInboundEventQueueWorker: InboundEventQueueWorker

    @Mock
    private lateinit var mockOutboundEventQueueWorker: OutboundEventQueueWorker

    @Mock
    private lateinit var mockAssurancePluginManager: AssurancePluginManager

    @Mock
    private lateinit var mockBitmap: Bitmap

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    @Mock
    private lateinit var mockUIService: UIService

    @Mock
    private lateinit var mockAppContextService: AppContextService

    @Mock
    private lateinit var mockFloatingButton: Presentable<FloatingButton>

    @Mock
    private lateinit var mockHandler: Handler

    private lateinit var mockedStaticBitmap: MockedStatic<Bitmap>
    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>
    private lateinit var mockedStaticUri: MockedStatic<Uri>

    private lateinit var assuranceSession: AssuranceSession

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        setupPreRequisites()
    }

    @Test
    fun `AssuranceSession#connect() fails when orgId is unavailable`() {
        // Simulate null org Id from both AssuranceStateManager and storedConnectionURL
        val uri = setupUri("SESSION_ID", "SESSION_PIN", null, "CLIENT_ID")
        `when`(mockAssuranceStateManager.getOrgId(true)).thenReturn(null)
        `when`(mockAssuranceStateManager.getClientId()).thenReturn("CLIENT_ID")
        `when`(mockAssuranceConnectionDataStore.storedConnectionURL).thenReturn(null)

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.connect()

        // verify
        verify(mockSocket, never()).connect(uri)
    }

    @Test
    fun `AssuranceSession#connect() uses stored sessionId when unavailable from AssuranceStateManager`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        `when`(mockAssuranceStateManager.getOrgId(true)).thenReturn(null)
        `when`(mockAssuranceStateManager.getClientId()).thenReturn("CLIENT_ID")

        val uri = setupUri(
            "STORED_SESSION_ID",
            "STORED_SESSION_PIN",
            "STORED_ORG_ID",
            "STORED_CLIENT_ID"
        )
        `when`(mockAssuranceConnectionDataStore.storedConnectionURL).thenReturn(uri)

        // test
        assuranceSession.connect()

        // verify that everything is called with argument values except for ORG_ID
        verify(mockSocket).connect("wss://connect.griffon.adobe.com/client/v1?sessionId=SESSION_ID&token=SESSION_PIN&orgId=STORED_ORG_ID&clientId=CLIENT_ID")
    }

    @Test
    fun `AssuranceSession#connect() uses provided sessionId when available from AssuranceStateManager`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        `when`(mockAssuranceStateManager.getOrgId(true)).thenReturn("ORG_ID")
        `when`(mockAssuranceStateManager.getClientId()).thenReturn("CLIENT_ID")

        val uri = setupUri(
            "SESSION_ID",
            "SESSION_PIN",
            "ORG_ID",
            "CLIENT_ID"
        )

        // test
        assuranceSession.connect()

        // verify that everything is called with argument values
        verify(mockSocket).connect(uri)
        // verify no interactions with connectionDataStore
        verify(mockAssuranceConnectionDataStore, never()).storedConnectionURL
    }

    @Test
    fun `AssuranceSession#disconnect() disconnects socket and clears the session`() {
        // setup
        `when`(mockSocket.state).thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN)

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.disconnect()

        // verify
        verify(mockSocket).disconnect()

        verify(mockOutboundEventQueueWorker).stop()
        verify(mockInboundEventQueueWorker).stop()
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null)
        verify(mockAssuranceStateManager).clearAssuranceSharedState()

        verify(mockAssurancePluginManager).onSessionTerminated()
    }

    @Test
    fun `AssuranceSession#queueOutboundEvent queues the event to the worker`() {
        // setup
        val event = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.GENERIC,
            mutableMapOf(),
            mutableMapOf(),
            300L
        )

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.queueOutboundEvent(event)

        // verify
        verify(mockOutboundEventQueueWorker).offer(event)
    }

    @Test
    fun `AssuranceSession#queueOutboundEvent does not queue null events to the worker`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.queueOutboundEvent(null)

        // verify
        verify(mockOutboundEventQueueWorker, never()).offer(any())
    }

    @Test
    fun `AssuranceSession#addPlugin adds the plugin to the plugin manager`() {
        // setup
        val mockPlugin = Mockito.mock(AssurancePlugin::class.java)
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.addPlugin(mockPlugin)

        // verify
        verify(mockAssurancePluginManager).addPlugin(mockPlugin)
    }

    @Test
    fun `AssuranceSession#logLocalUI logs the message to the AssuranceSessionPresentationManager`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.logLocalUI(AssuranceConstants.UILogColorVisibility.HIGH, "Test Message")

        // verify
        verify(mockAssuranceSessionPresentationManager).logLocalUI(
            AssuranceConstants.UILogColorVisibility.HIGH,
            "Test Message"
        )
    }

    @Test
    fun `Test AssuranceSession#onSocketConnected on initial connection`() {
        // setup
        val socketUrl = "wss://connect.griffon.adobe.com/client/v1?someQueryParams"
        `when`(mockSocket.connectionURL).thenReturn(socketUrl)

        // simulate initial start for outbound queue workers
        `when`(mockOutboundEventQueueWorker.start()).thenReturn(true)

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketConnected(mockSocket)

        // verify
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(socketUrl)
        verify(mockInboundEventQueueWorker).start()
        verify(mockOutboundEventQueueWorker).start()

        // first connection will automatically send client info event. Should not be called again
        verify(mockOutboundEventQueueWorker, never()).sendClientInfoEvent()
    }

    @Test
    fun `Test AssuranceSession#onSocketConnected on re-connection`() {
        // setup
        val socketUrl = "wss://connect.griffon.adobe.com/client/v1?someQueryParams"
        `when`(mockSocket.connectionURL).thenReturn(socketUrl)

        // simulate reconnect/re-start for outbound queue workers
        `when`(mockOutboundEventQueueWorker.start()).thenReturn(false)

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketConnected(mockSocket)

        // verify
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(socketUrl)
        verify(mockInboundEventQueueWorker).start()
        verify(mockOutboundEventQueueWorker).start()
        verify(mockOutboundEventQueueWorker, times(1)).sendClientInfoEvent()
    }

    @Test
    fun `Test AssuranceSession#onSocketDataReceived on valid AssuranceEvent`() {
        // setup
        val event = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.GENERIC,
            mutableMapOf(),
            mutableMapOf(),
            300L
        )
        val eventJson = event.jsonRepresentation

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDataReceived(mockSocket, eventJson)

        // verify
        verify(mockInboundEventQueueWorker).offer(any<AssuranceEvent>())
    }

    @Test
    fun `Test AssuranceSession#onSocketDataReceived on an in-valid AssuranceEvent`() {
        // setup
        val eventJson = "{invalidJson}"

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDataReceived(mockSocket, eventJson)

        // verify
        verify(mockInboundEventQueueWorker, never()).offer(any<AssuranceEvent>())
    }

    @Test
    fun `Test AssuranceSession#onSocketDisconnected on NORMAL disconnection`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Normal Disconnection",
            AssuranceConstants.SocketCloseCode.NORMAL,
            true
        )

        // verify
        verify(mockAssuranceSessionPresentationManager).onSessionDisconnected(AssuranceConstants.SocketCloseCode.NORMAL)
        verify(mockAssurancePluginManager).onSessionTerminated()
        mockAuthorizingPresentationDelegate.onSessionDisconnected(null)

        verify(mockOutboundEventQueueWorker).stop()
        verify(mockInboundEventQueueWorker).stop()
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null)
        verify(mockAssuranceStateManager).clearAssuranceSharedState()
    }

    @Test
    fun `Test AssuranceSession#onSocketDisconnected on ABNORMAL disconnection never retries when authorizing presentation is active`() {
        // setup
        `when`(mockAssuranceSessionPresentationManager.isAuthorizingPresentationActive()).thenReturn(false)

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Abnormal Disconnection",
            AssuranceConstants.SocketCloseCode.ABNORMAL,
            true
        )

        // verify
        verify(mockOutboundEventQueueWorker).block()
        verify(mockAssurancePluginManager).onSessionDisconnected(AssuranceConstants.SocketCloseCode.ABNORMAL)

        // verify that the delegate is notified of the disconnection
        verify(mockAuthorizingPresentationDelegate).onSessionDisconnected(
            AssuranceConstants.SocketCloseCode.toAssuranceConnectionError(
                AssuranceConstants.SocketCloseCode.ABNORMAL
            )
        )

        verify(mockAssuranceSessionPresentationManager).onSessionReconnecting()
        verify(mockHandler).postDelayed(any(), eq(5000L))
    }

    @Test
    fun `Test AssuranceSession#onSocketDisconnected on ABNORMAL disconnection for the first time when authorizing presentation is not active`() {
        // setup
        `when`(mockAssuranceSessionPresentationManager.isAuthorizingPresentationActive()).thenReturn(true)

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Abnormal Disconnection",
            AssuranceConstants.SocketCloseCode.ABNORMAL,
            true
        )

        // verify
        verify(mockOutboundEventQueueWorker).block()
        verify(mockAssurancePluginManager).onSessionDisconnected(AssuranceConstants.SocketCloseCode.ABNORMAL)

        // verify that the delegate is notified of the disconnection
        verify(mockAuthorizingPresentationDelegate).onSessionDisconnected(
            AssuranceConstants.SocketCloseCode.toAssuranceConnectionError(
                AssuranceConstants.SocketCloseCode.ABNORMAL
            )
        )

        verify(mockAssuranceSessionPresentationManager, never()).onSessionReconnecting()
        verify(mockHandler, never()).postDelayed(any(), eq(5000L))
    }

    @Test
    fun `test AssuranceSession#onSocketDisconnected on ABNORMAL disconnection for the second time when Authorizing presentation is not visible`() {
        // setup
        `when`(mockAssuranceSessionPresentationManager.isAuthorizingPresentationActive()).thenReturn(false)
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Abnormal Disconnection",
            AssuranceConstants.SocketCloseCode.ABNORMAL,
            false
        )
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Abnormal Disconnection",
            AssuranceConstants.SocketCloseCode.ABNORMAL,
            false
        )

        // verify
        verify(mockOutboundEventQueueWorker, times(1)).block()
        verify(mockAssurancePluginManager, times(1)).onSessionDisconnected(AssuranceConstants.SocketCloseCode.ABNORMAL)

        // verify that the delegate is notified of the disconnection
        verify(mockAuthorizingPresentationDelegate, times(1)).onSessionDisconnected(
            AssuranceConstants.SocketCloseCode.toAssuranceConnectionError(
                AssuranceConstants.SocketCloseCode.ABNORMAL
            )
        )

        verify(mockAssuranceSessionPresentationManager, times(1)).onSessionReconnecting()
        verify(mockHandler, times(2)).postDelayed(any(), eq(5000L))
    }

    @Test
    fun `Test AssuranceSession#onSocketDisconnected on ORG_MISMATCH`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Org Mismatch",
            AssuranceConstants.SocketCloseCode.ORG_MISMATCH,
            true
        )

        // verify
        verify(mockAssuranceSessionPresentationManager).onSessionDisconnected(AssuranceConstants.SocketCloseCode.ORG_MISMATCH)
        verify(mockAssurancePluginManager).onSessionTerminated()
        mockAuthorizingPresentationDelegate.onSessionDisconnected(AssuranceConstants.AssuranceConnectionError.ORG_ID_MISMATCH)

        verify(mockOutboundEventQueueWorker).stop()
        verify(mockInboundEventQueueWorker).stop()
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null)
        verify(mockAssuranceStateManager).clearAssuranceSharedState()
    }

    @Test
    fun `Test AssuranceSession#onSocketDisconnected on CLIENT_ERROR`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Session Expired",
            AssuranceConstants.SocketCloseCode.CLIENT_ERROR,
            true
        )

        // verify
        verify(mockAssuranceSessionPresentationManager).onSessionDisconnected(AssuranceConstants.SocketCloseCode.CLIENT_ERROR)
        verify(mockAssurancePluginManager).onSessionTerminated()
        mockAuthorizingPresentationDelegate.onSessionDisconnected(AssuranceConstants.AssuranceConnectionError.CLIENT_ERROR)

        verify(mockOutboundEventQueueWorker).stop()
        verify(mockInboundEventQueueWorker).stop()
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null)
        verify(mockAssuranceStateManager).clearAssuranceSharedState()
    }

    @Test
    fun `Test AssuranceSession#onSocketDisconnected on CONNECTION_LIMIT`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Connection Limit",
            AssuranceConstants.SocketCloseCode.CONNECTION_LIMIT,
            true
        )

        // verify
        verify(mockAssuranceSessionPresentationManager).onSessionDisconnected(AssuranceConstants.SocketCloseCode.CONNECTION_LIMIT)
        verify(mockAssurancePluginManager).onSessionTerminated()
        mockAuthorizingPresentationDelegate.onSessionDisconnected(AssuranceConstants.AssuranceConnectionError.CONNECTION_LIMIT)

        verify(mockOutboundEventQueueWorker).stop()
        verify(mockInboundEventQueueWorker).stop()
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null)
        verify(mockAssuranceStateManager).clearAssuranceSharedState()
    }

    @Test
    fun `Test AssuranceSession#onSocketDisconnected on EVENT_LIMIT`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Event Limit",
            AssuranceConstants.SocketCloseCode.EVENT_LIMIT,
            true
        )

        // verify
        verify(mockAssuranceSessionPresentationManager).onSessionDisconnected(AssuranceConstants.SocketCloseCode.EVENT_LIMIT)
        verify(mockAssurancePluginManager).onSessionTerminated()
        mockAuthorizingPresentationDelegate.onSessionDisconnected(AssuranceConstants.AssuranceConnectionError.EVENT_LIMIT)

        verify(mockOutboundEventQueueWorker).stop()
        verify(mockInboundEventQueueWorker).stop()
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null)
        verify(mockAssuranceStateManager).clearAssuranceSharedState()
    }

    @Test
    fun `Test AssuranceSession#onSocketDisconnected on SESSION_EXPIRED`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onSocketDisconnected(
            mockSocket,
            "Session Deleted",
            AssuranceConstants.SocketCloseCode.SESSION_DELETED,
            true
        )

        // verify
        verify(mockAssuranceSessionPresentationManager).onSessionDisconnected(AssuranceConstants.SocketCloseCode.SESSION_DELETED)
        verify(mockAssurancePluginManager).onSessionTerminated()
        mockAuthorizingPresentationDelegate.onSessionDisconnected(AssuranceConstants.AssuranceConnectionError.SESSION_DELETED)

        verify(mockOutboundEventQueueWorker).stop()
        verify(mockInboundEventQueueWorker).stop()
        verify(mockAssuranceConnectionDataStore).saveConnectionURL(null)
        verify(mockAssuranceStateManager).clearAssuranceSharedState()
    }

    @Test
    fun `Test AssuranceSession#onSocketStateChange notifies AssurancePresentationManager`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)
        val states = AssuranceWebViewSocket.SocketReadyState.values()

        // test
        states.forEach {
            assuranceSession.onSocketStateChange(mockSocket, it)

            // verify
            verify(mockAssuranceSessionPresentationManager).onSessionStateChange(it)
        }
    }

    @Test
    fun `AssuranceSession#onActivityResumed() notifies AssurancePresentationManager`() {
        // setup
        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.onActivityResumed(Mockito.mock(Activity::class.java))

        // verify
        verify(mockAssuranceSessionPresentationManager).onActivityResumed(any())
    }

    @Test
    fun `Test that InboundEventQueueWorker handles START_EVENT_FORWARDING event`() {
        // setup
        val startEventForwardingEvent = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mutableMapOf(),
            mutableMapOf<String, Any?>(
                AssuranceConstants.PayloadDataKeys.TYPE to AssuranceConstants.ControlType.START_EVENT_FORWARDING
            ),
            300L
        )

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.inboundQueueEventListener.onInboundEvent(startEventForwardingEvent)

        // verify
        verify(mockOutboundEventQueueWorker).unblock()
        verify(mockAssuranceSessionPresentationManager).onSessionConnected()
        verify(mockAuthorizingPresentationDelegate).onSessionConnected()
        verify(mockAssurancePluginManager).onSessionConnected()
        // verify that the start event forwarding event is not forwarded to the plugin manager
        verify(mockAssurancePluginManager, never()).onAssuranceEvent(startEventForwardingEvent)
    }

    @Test
    fun `Test that InboundEventQueueWorker forwards events to plugin manager`() {
        // setup
        val startEventForwardingEvent = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mutableMapOf(),
            mutableMapOf(),
            300L
        )

        assuranceSession = setupAssuranceSession(SessionAuthorizingPresentationType.PIN)

        // test
        assuranceSession.inboundQueueEventListener.onInboundEvent(startEventForwardingEvent)

        // verify
        verify(mockOutboundEventQueueWorker, never()).unblock()
        verify(mockAssuranceSessionPresentationManager, never()).onSessionConnected()
        verify(mockAuthorizingPresentationDelegate, never()).onSessionConnected()
        verify(mockAssurancePluginManager, never()).onSessionConnected()
        // verify that the start event forwarding event is not forwarded to the plugin manager
        verify(mockAssurancePluginManager, times(1)).onAssuranceEvent(startEventForwardingEvent)
    }

    @After
    fun teardown() {
        if (::mockedStaticBitmap.isInitialized) mockedStaticBitmap.close()
        if (::mockedStaticServiceProvider.isInitialized) mockedStaticServiceProvider.close()
        if (::mockedStaticUri.isInitialized) mockedStaticUri.close()
    }

    private fun setupPreRequisites() {
        // Required for Bitmap.createBitmap for mocking AssuranceSessionPresentationManager
        mockedStaticBitmap = Mockito.mockStatic(Bitmap::class.java)
        mockedStaticBitmap.`when`<Any> { Bitmap.createBitmap(any(), any(), any()) }
            .thenReturn(mockBitmap)

        // Required for setting up FloatingButtonPresentation
        mockedStaticServiceProvider = Mockito.mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }
            .thenReturn(mockServiceProvider)
        `when`(mockServiceProvider.uiService).thenReturn(mockUIService)
        `when`(mockServiceProvider.appContextService).thenReturn(mockAppContextService)
        `when`(mockUIService.create(any<FloatingButton>(), any())).thenReturn(mockFloatingButton)
    }

    private fun setupUri(
        sessionId: String?,
        token: String?,
        orgId: String?,
        clientId: String?,
    ): String {
        val uri = "wss://connect.griffon.adobe.com/client/v1?" +
            "sessionId=${sessionId ?: ""}" +
            "&token=${token ?: ""}" +
            "&orgId=${orgId ?: ""}" +
            "&clientId=${clientId ?: ""}"
        return uri
    }

    private fun setupAssuranceSession(sessionAuthorizingPresentationType: SessionAuthorizingPresentationType): AssuranceSession {
        val assuranceSession = AssuranceSession(
            mockAssuranceStateManager,
            "SESSION_ID",
            "SESSION_PIN",
            AssuranceConstants.AssuranceEnvironment.PROD,
            mockAssuranceConnectionDataStore,
            mutableListOf<AssurancePlugin>(),
            mutableListOf<AssuranceEvent>(),
            sessionAuthorizingPresentationType,
            mockAuthorizingPresentationDelegate
        )

        setInternalState(
            assuranceSession,
            "assuranceSessionPresentationManager",
            mockAssuranceSessionPresentationManager
        )
        setInternalState(assuranceSession, "inboundEventQueueWorker", mockInboundEventQueueWorker)
        setInternalState(assuranceSession, "outboundEventQueueWorker", mockOutboundEventQueueWorker)
        setInternalState(assuranceSession, "socket", mockSocket)
        setInternalState(assuranceSession, "pluginManager", mockAssurancePluginManager)
        setInternalState(assuranceSession, "socketReconnectHandler", mockHandler)
        return assuranceSession
    }
}
