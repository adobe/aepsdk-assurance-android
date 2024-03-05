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
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.SharedStateStatus
import com.adobe.marketing.mobile.services.AppContextService
import com.adobe.marketing.mobile.services.ServiceProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssuranceExtensionTest {

    @Mock
    private lateinit var mockExtensionApi: ExtensionApi

    @Mock
    private lateinit var mockAssuranceStateManager: AssuranceStateManager

    @Mock
    private lateinit var mockAssuranceConnectionDataStore: AssuranceConnectionDataStore

    @Mock
    private lateinit var mockAssuranceSessionOrchestrator: AssuranceSessionOrchestrator

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    @Mock
    private lateinit var mockMobileCore: MobileCore

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockActivity: Activity

    @Mock
    private lateinit var mockUri: Uri

    @Mock
    private lateinit var mockAppContextService: AppContextService

    @Mock
    private lateinit var mockSession: AssuranceSession

    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>
    private lateinit var mockedStaticMobileCore: MockedStatic<MobileCore>
    private lateinit var mockedStaticUri: MockedStatic<Uri>
    private lateinit var assuranceExtension: AssuranceExtension

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        assuranceExtension = AssuranceExtension(
            mockExtensionApi,
            mockAssuranceStateManager,
            mockAssuranceConnectionDataStore,
            mockAssuranceSessionOrchestrator
        )

        mockedStaticMobileCore = mockStatic(MobileCore::class.java)

        mockedStaticUri = mockStatic(Uri::class.java)
        mockedStaticUri.`when`<Any> { Uri.parse(any()) }.thenReturn(mockUri)

        mockedStaticServiceProvider = mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }
            .thenReturn(mockServiceProvider)

        `when`(mockServiceProvider.appContextService).thenReturn(mockAppContextService)
        `when`(mockApplication.applicationContext).thenReturn(mockContext)
    }

    @Test
    fun `Deeplink based #startSession has no effect when null deeplink is provided`() {
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        assuranceExtension.startSession(null)

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Disconnected }
        verify(mockApplication, Mockito.never()).startActivity(any())
    }

    @Test
    fun `Deeplink based #startSession has no effect when empty deeplink is provided`() {
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        assuranceExtension.startSession("")

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Disconnected }
        verify(mockApplication, Mockito.never()).startActivity(any())
    }

    @Test
    fun `Deeplink based #startSession has no effect when empty session id is provided`() {
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        startSessionWithDeeplink("aepsdkassurance", "")

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Disconnected }
        verify(mockApplication, Mockito.never()).startActivity(any())
    }

    @Test
    fun `Deeplink based #startSession has no effect when invalid session id is provided`() {
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        startSessionWithDeeplink("aepsdkassurance", "some invalid session id")

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Disconnected }
        verify(mockApplication, Mockito.never()).startActivity(any())
    }

    @Test
    fun `Deeplink based #startSession has no effect when no host application exists`() {
        setup(
            withActiveSession = false,
            currentApplication = null,
            currentActivity = null,
            asDebugBuild = false
        )

        startSessionWithDeeplink("aepsdkassurance", "6b55294e-32d4-49e8-9279-e3fe12a9d309")

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Disconnected }
        verify(mockApplication, Mockito.never()).startActivity(any())
    }

    @Test
    fun `Deeplink based #startSession has no effect when session is already active`() {

        setup(
            withActiveSession = true,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        startSessionWithDeeplink("aepsdkassurance", "6b55294e-32d4-49e8-9279-e3fe12a9d309")

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Disconnected }
        verify(mockApplication, Mockito.never()).startActivity(any())
    }

    @Test
    fun `#startSession with deeplink initiates Pin based authorization`() {

        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        startSessionWithDeeplink("aepsdkassurance", "6b55294e-32d4-49e8-9279-e3fe12a9d309")

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Authorizing }
        val capturedSessionPhase =
            AssuranceComponentRegistry.appState.sessionPhase.value as AssuranceAppState.SessionPhase.Authorizing
        assertTrue { capturedSessionPhase.assuranceAuthorization is AssuranceAppState.AssuranceAuthorization.PinConnect }

        verify(mockApplication).startActivity(any<Intent>())
    }

    @Test
    fun `QuickConnect based #startSession has no effect when no host application exists`() {
        setup(
            withActiveSession = false,
            currentApplication = null,
            currentActivity = null,
            asDebugBuild = true
        )

        assuranceExtension.startSession()

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Disconnected }
        verify(mockApplication, Mockito.never()).startActivity(any())
    }

    @Test
    fun `QuickConnect based session has no effect when session is already active`() {
        setup(
            withActiveSession = true,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = true
        )

        assuranceExtension.startSession()

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Disconnected }
        verify(mockApplication, Mockito.never()).startActivity(any())
    }

    @Test
    fun `QuickConnect based session cannot be started on non-debuggable builds`() {
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        assuranceExtension.startSession()

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Disconnected }
        verify(mockApplication, Mockito.never()).startActivity(any())
    }

    @Test
    fun `QuickConnect based #startSession initiates QuickConnect based authorization on debug builds`() {
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = true
        )

        assuranceExtension.startSession()

        assertTrue { AssuranceComponentRegistry.appState.sessionPhase.value is AssuranceAppState.SessionPhase.Authorizing }
        val capturedSessionPhase =
            AssuranceComponentRegistry.appState.sessionPhase.value as AssuranceAppState.SessionPhase.Authorizing
        assertTrue { capturedSessionPhase.assuranceAuthorization is AssuranceAppState.AssuranceAuthorization.QuickConnect }

        verify(mockApplication).startActivity(any<Intent>())
    }

    @Test
    fun `Test that #onRegistered registers event listeners`() {
        assuranceExtension.onRegistered()

        verify(mockExtensionApi).registerEventListener(
            ArgumentMatchers.eq(EventType.WILDCARD),
            eq(EventSource.WILDCARD),
            any()
        )
        verify(mockExtensionApi).registerEventListener(
            ArgumentMatchers.eq(EventType.ASSURANCE),
            eq(EventSource.REQUEST_CONTENT),
            any()
        )
        verify(mockExtensionApi).registerEventListener(
            ArgumentMatchers.eq(EventType.PLACES),
            eq(EventSource.REQUEST_CONTENT),
            any<AssuranceListenerHubPlacesRequests>()
        )
        verify(mockExtensionApi).registerEventListener(
            ArgumentMatchers.eq(EventType.PLACES),
            eq(EventSource.RESPONSE_CONTENT),
            any<AssuranceListenerHubPlacesResponses>()
        )
    }

    @Test
    fun `Test that #onRegistered does not shared state only when valid session does not exist`() {
        `when`(mockAssuranceStateManager.getSessionId()).thenReturn(null)
        assuranceExtension.onRegistered()
        verify(mockAssuranceStateManager, never()).shareAssuranceSharedState(any())
    }

    @Test
    fun `Test that #readyForEvent always returns true`() {
        val event =
            Event.Builder("Test Event", EventType.ASSURANCE, EventSource.REQUEST_CONTENT).build()
        assertTrue { assuranceExtension.readyForEvent(event) }
    }

    @Test
    fun `Test that #onRegistered shares state when valid session exists`() {
        `when`(mockAssuranceStateManager.getSessionId()).thenReturn("some valid session id")
        assuranceExtension.onRegistered()
        verify(mockAssuranceStateManager).shareAssuranceSharedState(any())
    }

    @Test
    fun `Verify that #getName returns extension name`() {
        assertEquals("com.adobe.assurance", assuranceExtension.name)
    }

    @Test
    fun `Verify that #getFriendlyName returns extension friendly name`() {
        assertEquals("Assurance", assuranceExtension.friendlyName)
    }

    @Test
    fun `Test #handleAssuranceRequestContent when event data contains deeplink`() {
        // Simulate no active session
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        // Simulate a valid deeplink based assurance request
        val sessionId = "6b55294e-32d4-49e8-9279-e3fe12a9d309"
        val deeplink = "aepsdkassurance://?sessionid=$sessionId"
        val eventData = mutableMapOf<String, Any?>()
        eventData[AssuranceConstants.SDKEventDataKey.START_SESSION_URL] = deeplink

        val startSessionEvent = Event.Builder(
            "Assurance Start Session",
            EventType.ASSURANCE,
            EventSource.REQUEST_CONTENT
        )
            .setEventData(eventData)
            .build()

        setupUriParser("aepsdkassurance", sessionId)

        // Test the handleAssuranceRequestContent
        assuranceExtension.handleAssuranceRequestContent(startSessionEvent)

        // Verify that the session is started with the provided deeplink
        val sessionPhase = AssuranceComponentRegistry.appState.sessionPhase.value
        assertNotNull(sessionPhase)
        assertTrue { sessionPhase is AssuranceAppState.SessionPhase.Authorizing }

        val authorization =
            (sessionPhase as AssuranceAppState.SessionPhase.Authorizing).assuranceAuthorization
        assertTrue { authorization is AssuranceAppState.AssuranceAuthorization.PinConnect }
    }

    @Test
    fun `Test #handleAssuranceRequestContent when event data contains QuickConnect flag`() {
        // Simulate no active session
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = true
        )

        // Simulate a valid QuickConnect based assurance request
        val startSessionEvent = Event.Builder(
            "Assurance Start Session (Quick Connect)",
            EventType.ASSURANCE,
            EventSource.REQUEST_CONTENT
        )
            .setEventData(mapOf(AssuranceConstants.SDKEventDataKey.IS_QUICK_CONNECT to true))
            .build()

        // Test the handleAssuranceRequestContent
        assuranceExtension.handleAssuranceRequestContent(startSessionEvent)

        // Verify that the session is started with the provided deeplink
        val sessionPhase = AssuranceComponentRegistry.appState.sessionPhase.value
        assertNotNull(sessionPhase)
        assertTrue { sessionPhase is AssuranceAppState.SessionPhase.Authorizing }

        val authorization =
            (sessionPhase as AssuranceAppState.SessionPhase.Authorizing).assuranceAuthorization
        assertTrue { authorization is AssuranceAppState.AssuranceAuthorization.QuickConnect }
    }

    @Test
    fun `Test #handleAssuranceRequestContent when event data contains invalid deeplink`() {
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        // Simulate an invalid deeplink based assurance request
        val sessionId = "some invalid session id"
        val deeplink = "aepsdkassurance://?sessionid=$sessionId"
        val eventData = mutableMapOf<String, Any?>()
        eventData[AssuranceConstants.SDKEventDataKey.START_SESSION_URL] = deeplink

        val startSessionEvent = Event.Builder(
            "Assurance Start Session",
            EventType.ASSURANCE,
            EventSource.REQUEST_CONTENT
        )
            .setEventData(eventData)
            .build()

        assuranceExtension.handleAssuranceRequestContent(startSessionEvent)
        val sessionPhase = AssuranceComponentRegistry.appState.sessionPhase.value
        assertNotNull(sessionPhase)
        assertTrue { sessionPhase is AssuranceAppState.SessionPhase.Disconnected }
    }

    @Test
    fun `Test #handleAssuranceRequestContent when event data is unrelated`() {
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        val startSessionEvent = Event.Builder(
            "Assurance Start Session",
            EventType.ASSURANCE,
            EventSource.REQUEST_CONTENT
        )
            .setEventData(mapOf("some random key" to "some random value"))
            .build()

        assuranceExtension.handleAssuranceRequestContent(startSessionEvent)

        val sessionPhase = AssuranceComponentRegistry.appState.sessionPhase.value
        assertNotNull(sessionPhase)
        assertTrue { sessionPhase is AssuranceAppState.SessionPhase.Disconnected }
    }

    @Test
    fun `Test #handleAssuranceRequestContent when event data is null`() {
        setup(
            withActiveSession = false,
            currentApplication = mockApplication,
            currentActivity = mockActivity,
            asDebugBuild = false
        )

        val startSessionEvent = Event.Builder(
            "Assurance Start Session",
            EventType.ASSURANCE,
            EventSource.REQUEST_CONTENT
        )
            .build()

        assuranceExtension.handleAssuranceRequestContent(startSessionEvent)

        val sessionPhase = AssuranceComponentRegistry.appState.sessionPhase.value
        assertNotNull(sessionPhase)
        assertTrue { sessionPhase is AssuranceAppState.SessionPhase.Disconnected }
    }

    @Test
    fun `Test #handleWildCardEvent happy case`() {
        val eventName = "Mars Landing Event"
        val event = Event.Builder(eventName, EventType.EDGE, EventSource.OS)
            .setEventData(mapOf("hasLanded" to true)).build()

        assuranceExtension.handleWildcardEvent(event)

        // verify that the event is passed to the assurance state manager
        verify(mockAssuranceStateManager).onSDKEvent(event)

        val assuranceEventCaptor: KArgumentCaptor<AssuranceEvent> = argumentCaptor()
        verify(mockAssuranceSessionOrchestrator).queueEvent(assuranceEventCaptor.capture())

        val capturedAssuranceEvent = assuranceEventCaptor.firstValue

        assertEquals(
            eventName,
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME)
        )
        assertEquals(
            EventType.EDGE.lowercase(),
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE)
        )
        assertEquals(
            EventSource.OS.lowercase(),
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE)
        )
        assertEquals(
            event.uniqueIdentifier,
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER)
        )
        assertEquals(
            event.eventData,
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA)
        )
    }

    @Test
    fun `#handleWildCardEvent when parentId is available`() {
        val eventName = "Mars Landing Event"
        val parentEvent = Event.Builder("Parent Event", EventType.EDGE, EventSource.OS).build()
        val event = Event.Builder(eventName, EventType.EDGE, EventSource.OS)
            .setEventData(mapOf("hasLanded" to true))
            .chainToParentEvent(parentEvent)
            .build()

        assuranceExtension.handleWildcardEvent(event)

        // verify that the event is passed to the assurance state manager
        verify(mockAssuranceStateManager).onSDKEvent(event)

        val assuranceEventCaptor: KArgumentCaptor<AssuranceEvent> = argumentCaptor()
        verify(mockAssuranceSessionOrchestrator).queueEvent(assuranceEventCaptor.capture())

        val capturedAssuranceEvent = assuranceEventCaptor.firstValue

        assertEquals(
            eventName,
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME)
        )
        assertEquals(
            EventType.EDGE.lowercase(),
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE)
        )
        assertEquals(
            EventSource.OS.lowercase(),
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE)
        )
        assertEquals(
            event.uniqueIdentifier,
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER)
        )
        assertEquals(
            event.eventData,
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA)
        )
        assertEquals(
            parentEvent.uniqueIdentifier,
            capturedAssuranceEvent.payload.get(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_PARENT_IDENTIFIER)
        )
    }

    @Test
    fun `#handleWildCardEvent when the event is a regular shared state event`() {
        val extensionName = "com.adobe.module.someExtension"
        val sharedStateData = mapOf("somekey" to "someValue")

        val event = Event.Builder(
            AssuranceConstants.SDKEventName.SHARED_STATE_CHANGE,
            EventType.HUB,
            EventSource.SHARED_STATE
        )
            .setEventData(mapOf("stateowner" to extensionName))
            .build()
        `when`(
            mockExtensionApi.getSharedState(
                extensionName,
                event,
                false,
                SharedStateResolution.ANY
            )
        ).thenReturn(
            SharedStateResult(SharedStateStatus.SET, sharedStateData)
        )

        assuranceExtension.handleWildcardEvent(event)

        val assuranceEventCaptor: KArgumentCaptor<AssuranceEvent> = argumentCaptor()
        verify(mockAssuranceSessionOrchestrator).queueEvent(assuranceEventCaptor.capture())

        val capturedAssuranceEvent = assuranceEventCaptor.firstValue
        assertEquals(
            event.uniqueIdentifier,
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER]
        )
        assertEquals(
            AssuranceConstants.SDKEventName.SHARED_STATE_CHANGE,
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME]
        )
        assertEquals(
            EventType.HUB.lowercase(),
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE]
        )
        assertEquals(
            EventSource.SHARED_STATE.lowercase(),
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE]
        )
        assertEquals(
            event.eventData,
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA]
        )

        assertEquals(
            capturedAssuranceEvent.payload[AssuranceConstants.PayloadDataKeys.METADATA],
            mapOf(AssuranceConstants.PayloadDataKeys.STATE_DATA to sharedStateData)
        )
    }

    @Test
    fun `#handleWildCardEven when event is a XDM Shared State event`() {
        val extensionName = "com.adobe.module.someExtension"
        val sharedStateData = mapOf("somekey" to "someValue")

        val event = Event.Builder(
            AssuranceConstants.SDKEventName.XDM_SHARED_STATE_CHANGE,
            EventType.HUB,
            EventSource.SHARED_STATE
        )
            .setEventData(mapOf(AssuranceConstants.SDKEventDataKey.STATE_OWNER to extensionName))
            .build()
        `when`(
            mockExtensionApi.getXDMSharedState(
                extensionName,
                event,
                false,
                SharedStateResolution.ANY
            )
        ).thenReturn(
            SharedStateResult(SharedStateStatus.SET, sharedStateData)
        )

        // Test
        assuranceExtension.handleWildcardEvent(event)

        // Verify than an event is queued and capture it
        val assuranceEventCaptor: KArgumentCaptor<AssuranceEvent> = argumentCaptor()
        verify(mockAssuranceSessionOrchestrator).queueEvent(assuranceEventCaptor.capture())

        // Verify that the event is queued with the correct payload
        val capturedAssuranceEvent = assuranceEventCaptor.firstValue
        assertEquals(
            event.uniqueIdentifier,
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER]
        )
        assertEquals(
            AssuranceConstants.SDKEventName.XDM_SHARED_STATE_CHANGE,
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME]
        )
        assertEquals(
            EventType.HUB.lowercase(),
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE]
        )
        assertEquals(
            EventSource.SHARED_STATE.lowercase(),
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE]
        )
        assertEquals(
            event.eventData,
            capturedAssuranceEvent.payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA]
        )

        assertEquals(
            capturedAssuranceEvent.payload[AssuranceConstants.PayloadDataKeys.METADATA],
            mapOf(AssuranceConstants.PayloadDataKeys.XDM_STATE_DATA to sharedStateData)
        )
    }

    @Test
    fun `Test #handleWildCardEven when event is a invalid Shared State event`() {
        val event = Event.Builder(
            AssuranceConstants.SDKEventName.SHARED_STATE_CHANGE,
            EventType.HUB,
            EventSource.SHARED_STATE
        )
            .setEventData(mapOf("INVALID_STATE_OWNER_KEY" to "com.adobe.module.someExtension"))
            .build()

        // Test
        assuranceExtension.handleWildcardEvent(event)

        // Verify than event is never queued
        verify(mockAssuranceSessionOrchestrator, never()).queueEvent(any())
    }

    @Test
    fun `Test #logLocalUI invokes session when available`() {
        `when`(mockAssuranceSessionOrchestrator.activeSession).thenReturn(mockSession)
        val message = "my message"
        assuranceExtension.logLocalUI(AssuranceConstants.UILogColorVisibility.HIGH, message)
        verify(mockSession).logLocalUI(AssuranceConstants.UILogColorVisibility.HIGH, message)
    }

    @After
    fun teardown() {
        mockedStaticServiceProvider.close()
        mockedStaticMobileCore.close()
        mockedStaticUri.close()
        AssuranceComponentRegistry.appState.onSessionPhaseChange(AssuranceAppState.SessionPhase.Disconnected())
    }

    private fun startSessionWithDeeplink(scheme: String, sessionId: String) {
        setupUriParser(scheme, sessionId)
        assuranceExtension.startSession("$scheme://?${AssuranceConstants.DeeplinkURLKeys.START_URL_QUERY_KEY_SESSION_ID}=$sessionId")
    }

    private fun setupUriParser(scheme: String, sessionId: String) {
        `when`(mockUri.getQueryParameter(AssuranceConstants.DeeplinkURLKeys.START_URL_QUERY_KEY_SESSION_ID)).thenReturn(
            sessionId
        )
    }

    private fun setup(
        withActiveSession: Boolean,
        currentApplication: Application?,
        currentActivity: Activity?,
        asDebugBuild: Boolean
    ) {

        `when`(mockAssuranceSessionOrchestrator.activeSession).thenReturn(if (withActiveSession) mockSession else null)
        mockedStaticMobileCore.`when`<Any> { MobileCore.getApplication() }
            .thenReturn(currentApplication)
        `when`(mockAppContextService.application).thenReturn(currentApplication)

        `when`(mockAppContextService.currentActivity).thenReturn(currentActivity)

        if (asDebugBuild) {
            mockDebuggableApplication()
        } else {
            mockNonDebuggableApplication()
        }
    }

    private fun mockDebuggableApplication() {
        val mockApplicationInfo: ApplicationInfo = Mockito.mock(ApplicationInfo::class.java)
        // Set application flag to be debuggable
        mockApplicationInfo.flags =
            if ((mockApplicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                mockApplicationInfo.flags
            } else (mockApplicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE)
        `when`(mockContext.getApplicationInfo()).thenReturn(mockApplicationInfo)
    }

    private fun mockNonDebuggableApplication() {
        val mockApplicationInfo: ApplicationInfo = Mockito.mock(ApplicationInfo::class.java)
        // Set application flag to be non-debuggable
        mockApplicationInfo.flags =
            if ((mockApplicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
                mockApplicationInfo.flags
            } else (mockApplicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE)
        `when`(mockContext.getApplicationInfo()).thenReturn(mockApplicationInfo)
    }
}
