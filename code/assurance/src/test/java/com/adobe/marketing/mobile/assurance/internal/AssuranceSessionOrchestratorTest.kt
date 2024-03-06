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

import android.app.Application
import com.adobe.marketing.mobile.assurance.internal.AssuranceSessionOrchestrator.AssuranceSessionCreator
import com.adobe.marketing.mobile.assurance.internal.AssuranceSessionOrchestrator.HostAppActivityLifecycleObserver
import com.adobe.marketing.mobile.assurance.internal.AssuranceTestUtils.setInternalState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AssuranceSessionOrchestratorTest {
    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockAssuranceStateManager: AssuranceStateManager

    @Mock
    private lateinit var mockAssuranceConnectionDataStore: AssuranceConnectionDataStore

    @Mock
    private lateinit var mockAssuranceSessionCreator: AssuranceSessionCreator

    @Mock
    private lateinit var mockAssuranceSession: AssuranceSession

    @Mock
    private lateinit var mockBufferedEvents: MutableList<AssuranceEvent>

    private lateinit var assuranceSessionOrchestrator: AssuranceSessionOrchestrator
    private lateinit var activityLifecycleObserver: HostAppActivityLifecycleObserver

    companion object {
        private const val TEST_SESSION_ID = "SESSION_ID"
        private const val TEST_TOKEN = "1234"
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        assuranceSessionOrchestrator = AssuranceSessionOrchestrator(
            mockApplication,
            mockAssuranceStateManager,
            mutableListOf(),
            mockAssuranceConnectionDataStore,
            mockAssuranceSessionCreator
        )
        setInternalState(assuranceSessionOrchestrator, "outboundEventBuffer", mockBufferedEvents)

        val activityLifecycleObserverCaptor: KArgumentCaptor<HostAppActivityLifecycleObserver> =
            argumentCaptor()
        verify(mockApplication).registerActivityLifecycleCallbacks(activityLifecycleObserverCaptor.capture())
        activityLifecycleObserver = activityLifecycleObserverCaptor.firstValue
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#createSession`() {
        val sessionStatusListener = mock(AssuranceSessionStatusListener::class.java)
        `when`(
            mockAssuranceSessionCreator.create(
                anyString(),
                anyString(),
                any(),
                eq(mockAssuranceStateManager),
                any(),
                eq(mockAssuranceConnectionDataStore),
                any(),
                eq(sessionStatusListener),
                any()
            )
        ).thenReturn(mockAssuranceSession)

        assuranceSessionOrchestrator.createSession(
            TEST_SESSION_ID,
            AssuranceConstants.AssuranceEnvironment.PROD,
            TEST_TOKEN,
            sessionStatusListener,
            SessionAuthorizingPresentationType.QUICK_CONNECT
        )

        verify(mockAssuranceSession).registerStatusListener(assuranceSessionOrchestrator.assuranceSessionStatusListener)
        verify(mockAssuranceStateManager).shareAssuranceSharedState(TEST_SESSION_ID)
        verify(mockAssuranceSession).connect()
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#terminateSession with purged buffer`() {
        // Setup a session
        val sessionStatusListener = mock(AssuranceSessionStatusListener::class.java)
        `when`(
            mockAssuranceSessionCreator.create(
                anyString(),
                anyString(),
                any(),
                eq(mockAssuranceStateManager),
                any(),
                eq(mockAssuranceConnectionDataStore),
                any(),
                eq(sessionStatusListener),
                any()
            )
        ).thenReturn(mockAssuranceSession)

        assuranceSessionOrchestrator.createSession(
            TEST_SESSION_ID,
            AssuranceConstants.AssuranceEnvironment.PROD,
            TEST_TOKEN,
            sessionStatusListener,
            SessionAuthorizingPresentationType.QUICK_CONNECT
        )

        // Terminate the session
        assuranceSessionOrchestrator.terminateSession(true)

        // Verify the session is disconnected and the buffer is cleared
        verify(mockBufferedEvents).clear()
        verify(mockAssuranceSession).unregisterStatusListener(assuranceSessionOrchestrator.assuranceSessionStatusListener)
        verify(mockAssuranceSession).disconnect()
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#terminateSession without purging buffered events`() {
        // Setup a session
        val sessionStatusListener = mock(AssuranceSessionStatusListener::class.java)
        `when`(
            mockAssuranceSessionCreator.create(
                anyString(),
                anyString(),
                any(),
                eq(mockAssuranceStateManager),
                any(),
                eq(mockAssuranceConnectionDataStore),
                any(),
                eq(sessionStatusListener),
                any()
            )
        ).thenReturn(mockAssuranceSession)

        assuranceSessionOrchestrator.createSession(
            TEST_SESSION_ID,
            AssuranceConstants.AssuranceEnvironment.PROD,
            TEST_TOKEN,
            sessionStatusListener,
            SessionAuthorizingPresentationType.QUICK_CONNECT
        )

        // Terminate the session
        assuranceSessionOrchestrator.terminateSession(false)

        // Verify the session is disconnected and the buffer is not cleared
        verify(mockBufferedEvents, never()).clear()
        verify(mockAssuranceSession).unregisterStatusListener(assuranceSessionOrchestrator.assuranceSessionStatusListener)
        verify(mockAssuranceSession).disconnect()
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#reconnectToStoredSession() when stored session is not available`() {
        `when`(mockAssuranceConnectionDataStore.storedConnectionURL).thenReturn(null)

        assuranceSessionOrchestrator.reconnectToStoredSession()

        verifyNoInteractions(mockAssuranceSessionCreator)
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#reconnectToStoredSession() when stored session is empty`() {
        `when`(mockAssuranceConnectionDataStore.storedConnectionURL).thenReturn("")

        assuranceSessionOrchestrator.reconnectToStoredSession()

        verifyNoInteractions(mockAssuranceSessionCreator)
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#reconnectToStoredSession() when stored sessionIs is not available`() {
        // val testStoredSession = wss://connect.griffon.adobe.com/client/v1?sessionId=5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758&token=9004&orgId=972C898555E9F7BC7F000101%40AdobeOrg&clientId=89942ef1-11c2-46fb-bcc7-bb797c84e638

        val testStoredSession =
            "wss://connect.griffon.adobe.com/client/v1?token=9004&orgId=972C898555E9F7BC7F000101%40AdobeOrg&clientId=89942ef1-11c2-46fb-bcc7-bb797c84e638"
        `when`(mockAssuranceConnectionDataStore.storedConnectionURL).thenReturn(testStoredSession)

        assuranceSessionOrchestrator.reconnectToStoredSession()

        verifyNoInteractions(mockAssuranceSessionCreator)
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#reconnectToStoredSession() when stored token is not available`() {
        // val testStoredSession = wss://connect.griffon.adobe.com/client/v1?sessionId=5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758&token=9004&orgId=972C898555E9F7BC7F000101%40AdobeOrg&clientId=89942ef1-11c2-46fb-bcc7-bb797c84e638

        val testStoredSession =
            "wss://connect.griffon.adobe.com/client/v1?sessionId=5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758&orgId=972C898555E9F7BC7F000101%40AdobeOrg&clientId=89942ef1-11c2-46fb-bcc7-bb797c84e638"
        `when`(mockAssuranceConnectionDataStore.storedConnectionURL).thenReturn(testStoredSession)

        assuranceSessionOrchestrator.reconnectToStoredSession()

        verifyNoInteractions(mockAssuranceSessionCreator)
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#reconnectToStoredSession() when stored url is valid`() {
        val testStoredSession =
            "wss://connect.griffon.adobe.com/client/v1?sessionId=5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758&token=9004&orgId=972C898555E9F7BC7F000101%40AdobeOrg&clientId=89942ef1-11c2-46fb-bcc7-bb797c84e638"
        `when`(mockAssuranceConnectionDataStore.storedConnectionURL).thenReturn(testStoredSession)
        `when`(
            mockAssuranceSessionCreator.create(
                anyString(),
                anyString(),
                any(),
                eq(mockAssuranceStateManager),
                any(),
                eq(mockAssuranceConnectionDataStore),
                any(),
                any(),
                any()
            )
        ).thenReturn(mockAssuranceSession)

        assuranceSessionOrchestrator.reconnectToStoredSession()

        verify(mockAssuranceSessionCreator, times(1)).create(
            eq("5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758"), // verify the session id is extracted from the stored url
            eq("9004"), // verify the token is extracted from the stored url
            eq(AssuranceConstants.AssuranceEnvironment.PROD),
            eq(mockAssuranceStateManager),
            any(),
            eq(mockAssuranceConnectionDataStore),
            any(),
            eq(null), // reconnecting to stored session should not have a presentation session delegate
            any()
        )
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#reconnectToStoredSession() extracts environment correctly`() {
        val testStoredSession =
            "wss://connect-stage.griffon.adobe.com/client/v1?sessionId=5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758&token=9004&orgId=972C898555E9F7BC7F000101%40AdobeOrg&clientId=89942ef1-11c2-46fb-bcc7-bb797c84e638"
        `when`(mockAssuranceConnectionDataStore.storedConnectionURL).thenReturn(testStoredSession)
        `when`(
            mockAssuranceSessionCreator.create(
                anyString(),
                anyString(),
                any(),
                eq(mockAssuranceStateManager),
                any(),
                eq(mockAssuranceConnectionDataStore),
                any(),
                any(),
                any()
            )
        ).thenReturn(mockAssuranceSession)

        assuranceSessionOrchestrator.reconnectToStoredSession()

        verify(mockAssuranceSessionCreator, times(1)).create(
            eq("5ccd5a20-1c00-4d6e-bf77-bbe85bc0c758"), // verify the session id is extracted from the stored url
            eq("9004"), // verify the token is extracted from the stored url
            eq(AssuranceConstants.AssuranceEnvironment.STAGE), // verify the environment is extracted from the stored url
            eq(mockAssuranceStateManager),
            any(),
            eq(mockAssuranceConnectionDataStore),
            any(),
            eq(null), // reconnecting to stored session should not have a presentation session delegate
            any()
        )
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#queueEvent() when session is not connected`() {
        val event = AssuranceEvent("EventName", mapOf("key" to "value"))

        assuranceSessionOrchestrator.queueEvent(event)

        verify(mockBufferedEvents).add(event)
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#queueEvent() when session is initialized but not connceted`() {
        // Setup a session
        val sessionStatusListener = mock(AssuranceSessionStatusListener::class.java)
        `when`(
            mockAssuranceSessionCreator.create(
                anyString(),
                anyString(),
                any(),
                eq(mockAssuranceStateManager),
                any(),
                eq(mockAssuranceConnectionDataStore),
                any(),
                eq(sessionStatusListener),
                any()
            )
        ).thenReturn(mockAssuranceSession)

        assuranceSessionOrchestrator.createSession(
            TEST_SESSION_ID,
            AssuranceConstants.AssuranceEnvironment.PROD,
            TEST_TOKEN,
            sessionStatusListener,
            SessionAuthorizingPresentationType.QUICK_CONNECT
        )

        val event = AssuranceEvent("EventName", mapOf("key" to "value"))
        assuranceSessionOrchestrator.queueEvent(event)

        verify(mockAssuranceSession).queueOutboundEvent(event)
        // verify that the event is added to the buffer because connection is not established yet
        verify(mockBufferedEvents, times(1)).add(event)
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#queueEvent() when session is connected`() {
        val sessionStatusListener = mock(AssuranceSessionStatusListener::class.java)
        `when`(
            mockAssuranceSessionCreator.create(
                anyString(),
                anyString(),
                any(),
                eq(mockAssuranceStateManager),
                any(),
                eq(mockAssuranceConnectionDataStore),
                any(),
                eq(sessionStatusListener),
                any()
            )
        ).thenReturn(mockAssuranceSession)

        // Setup a session
        assuranceSessionOrchestrator.createSession(
            TEST_SESSION_ID,
            AssuranceConstants.AssuranceEnvironment.PROD,
            TEST_TOKEN,
            sessionStatusListener,
            SessionAuthorizingPresentationType.QUICK_CONNECT
        )

        // Simulate the session is connected
        assuranceSessionOrchestrator.assuranceSessionStatusListener.onSessionConnected()

        // Test the event being queued
        val event = AssuranceEvent("EventName", mapOf("key" to "value"))
        assuranceSessionOrchestrator.queueEvent(event)

        verify(mockAssuranceSession).queueOutboundEvent(event)
        // verify that the event is not added to the buffer because connection is established
        verify(mockBufferedEvents, never()).add(event)
        verify(mockBufferedEvents).clear()
    }

    @Test
    fun `Test AssuranceSessionOrchestrator#queueEvent() when session is connected and event is null`() {
        val sessionStatusListener = mock(AssuranceSessionStatusListener::class.java)
        `when`(
            mockAssuranceSessionCreator.create(
                anyString(),
                anyString(),
                any(),
                eq(mockAssuranceStateManager),
                any(),
                eq(mockAssuranceConnectionDataStore),
                any(),
                eq(sessionStatusListener),
                any()
            )
        ).thenReturn(mockAssuranceSession)

        // Setup a session
        assuranceSessionOrchestrator.createSession(
            TEST_SESSION_ID,
            AssuranceConstants.AssuranceEnvironment.PROD,
            TEST_TOKEN,
            sessionStatusListener,
            SessionAuthorizingPresentationType.QUICK_CONNECT
        )

        // Simulate the session is connected
        assuranceSessionOrchestrator.assuranceSessionStatusListener.onSessionConnected()

        // Test the event being queued
        assuranceSessionOrchestrator.queueEvent(null)

        verify(mockAssuranceSession, never()).queueOutboundEvent(any())
    }
}
