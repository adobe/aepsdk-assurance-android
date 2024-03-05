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
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceActivity
import com.adobe.marketing.mobile.assurance.internal.ui.floatingbutton.AssuranceFloatingButton
import com.adobe.marketing.mobile.services.AppContextService
import com.adobe.marketing.mobile.services.ServiceProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssuranceSessionPresentationManagerTest {
    @Mock
    private lateinit var mockAssuranceFloatingButton: AssuranceFloatingButton

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    @Mock
    private lateinit var mockAppContextService: AppContextService

    @Mock
    private lateinit var mockActivity: Activity

    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        mockedStaticServiceProvider = mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<ServiceProvider> { ServiceProvider.getInstance() }
            .thenReturn(mockServiceProvider)
        mockedStaticServiceProvider.`when`<AppContextService> { mockServiceProvider.appContextService }
            .thenReturn(mockAppContextService)
    }

    @Test
    fun `Test #onSessionConnected`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.PIN,
            mockAssuranceFloatingButton
        )

        // Test
        assuranceSessionPresentationManager.onSessionConnected()

        // Verify
        assertEquals(
            AssuranceAppState.SessionPhase.Connected,
            AssuranceComponentRegistry.appState.sessionPhase.value
        )
        verify(mockAssuranceFloatingButton).show()
        verify(mockAssuranceFloatingButton).updateGraphic(true)
    }

    @Test
    fun `Test #onSessionDisconnected with Normal close code`() {
        // Setup
        `when`(mockAppContextService.currentActivity).thenReturn(mockActivity)
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.PIN,
            mockAssuranceFloatingButton
        )

        // Test
        assuranceSessionPresentationManager.onSessionDisconnected(AssuranceConstants.SocketCloseCode.NORMAL)

        // Verify
        assertEquals(
            AssuranceAppState.SessionPhase.Disconnected(null),
            AssuranceComponentRegistry.appState.sessionPhase.value
        )
        verify(mockAssuranceFloatingButton).remove()
        verify(mockActivity, never()).startActivity(any())
    }

    @Test
    fun `Test #onSessionDisconnected with error close code and active presentation`() {
        // Setup
        `when`(mockAppContextService.currentActivity).thenReturn(mockActivity)
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        // Simulate active QuickConnect presentation
        val credentials =
            AssuranceAppState.AssuranceAuthorization.QuickConnect(AssuranceConstants.AssuranceEnvironment.PROD)
        AssuranceComponentRegistry.appState.onSessionPhaseChange(
            AssuranceAppState.SessionPhase.Authorizing(
                credentials
            )
        )
        assertTrue { assuranceSessionPresentationManager.isAuthorizingPresentationActive() }

        // Test
        assuranceSessionPresentationManager.onSessionDisconnected(AssuranceConstants.SocketCloseCode.ORG_MISMATCH)

        // Verify

        // Session phase should remain in Authorizing phase because the UI is active
        assertEquals(
            AssuranceAppState.SessionPhase.Authorizing(credentials),
            AssuranceComponentRegistry.appState.sessionPhase.value
        )

        // no changes in ui elements
        verify(mockAssuranceFloatingButton, never()).remove()
        verify(mockActivity, never()).startActivity(any())
    }

    @Test
    fun `Test #onSessionDisconnected with error close code EVENT_LIMIT and inactive presentation`() {
        // Setup
        `when`(mockAppContextService.currentActivity).thenReturn(mockActivity)
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        // Test
        assuranceSessionPresentationManager.onSessionDisconnected(AssuranceConstants.SocketCloseCode.EVENT_LIMIT)

        // Verify
        assertFalse { assuranceSessionPresentationManager.isAuthorizingPresentationActive() }
        assertEquals(
            AssuranceAppState.SessionPhase.Disconnected(AssuranceConstants.AssuranceConnectionError.EVENT_LIMIT),
            AssuranceComponentRegistry.appState.sessionPhase.value
        )
        verify(mockActivity).startActivity(any())
    }

    @Test
    fun `Test #onSessionDisconnected with error close code CLIENT ERROR and inactive presentation`() {
        // Setup
        `when`(mockAppContextService.currentActivity).thenReturn(mockActivity)
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        // Test
        assuranceSessionPresentationManager.onSessionDisconnected(AssuranceConstants.SocketCloseCode.CLIENT_ERROR)

        // Verify
        assertFalse { assuranceSessionPresentationManager.isAuthorizingPresentationActive() }
        assertEquals(
            AssuranceAppState.SessionPhase.Disconnected(AssuranceConstants.AssuranceConnectionError.CLIENT_ERROR),
            AssuranceComponentRegistry.appState.sessionPhase.value
        )
        verify(mockActivity).startActivity(any())
    }

    @Test
    fun `Test #onSessionDisconnected with error close code SESSION_DELETED and inactive presentation`() {
        // Setup
        `when`(mockAppContextService.currentActivity).thenReturn(mockActivity)
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        // Test
        assuranceSessionPresentationManager.onSessionDisconnected(AssuranceConstants.SocketCloseCode.SESSION_DELETED)

        // Verify
        assertFalse { assuranceSessionPresentationManager.isAuthorizingPresentationActive() }
        assertEquals(
            AssuranceAppState.SessionPhase.Disconnected(AssuranceConstants.AssuranceConnectionError.SESSION_DELETED),
            AssuranceComponentRegistry.appState.sessionPhase.value
        )
        verify(mockActivity).startActivity(any())
    }

    @Test
    fun `Test #onSessionReconnecting`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        // Test
        assuranceSessionPresentationManager.onSessionReconnecting()

        // Verify
        verify(mockAssuranceFloatingButton).updateGraphic(false)
        val currentPhase = AssuranceComponentRegistry.appState.sessionPhase.value
        assertTrue { currentPhase is AssuranceAppState.SessionPhase.Disconnected }
        assertTrue { (currentPhase as AssuranceAppState.SessionPhase.Disconnected).reconnecting }
    }

    @Test
    fun `Test #onSessionStateChange updates the Floating button graphic`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        // Test
        assuranceSessionPresentationManager.onSessionStateChange(AssuranceWebViewSocket.SocketReadyState.CLOSED)
        verify(mockAssuranceFloatingButton).updateGraphic(false)
        reset(mockAssuranceFloatingButton)

        assuranceSessionPresentationManager.onSessionStateChange(AssuranceWebViewSocket.SocketReadyState.OPEN)
        verify(mockAssuranceFloatingButton).updateGraphic(true)
        reset(mockAssuranceFloatingButton)

        assuranceSessionPresentationManager.onSessionStateChange(AssuranceWebViewSocket.SocketReadyState.CLOSING)
        verify(mockAssuranceFloatingButton).updateGraphic(false)
        reset(mockAssuranceFloatingButton)

        assuranceSessionPresentationManager.onSessionStateChange(AssuranceWebViewSocket.SocketReadyState.CONNECTING)
        verify(mockAssuranceFloatingButton).updateGraphic(false)
        reset(mockAssuranceFloatingButton)

        assuranceSessionPresentationManager.onSessionStateChange(AssuranceWebViewSocket.SocketReadyState.UNKNOWN)
        verify(mockAssuranceFloatingButton).updateGraphic(false)
        reset(mockAssuranceFloatingButton)
    }

    @Test
    fun `Test #onActivityResumed with Assurance Activity, active FloatingButton, active presentation`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        val mockAssuranceActivity = mock(AssuranceActivity::class.java)
        `when`(mockAppContextService.currentActivity).thenReturn(mock(AssuranceActivity::class.java))
        `when`(mockAssuranceFloatingButton.isActive()).thenReturn(true)

        // Test
        assuranceSessionPresentationManager.onActivityResumed(mockAssuranceActivity)

        // Verify
        verify(mockAssuranceFloatingButton).hide()
    }

    @Test
    fun `Test #onActivityResumed with Assurance Activity, inactive FloatingButton`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        val mockAssuranceActivity = mock(AssuranceActivity::class.java)
        `when`(mockAppContextService.currentActivity).thenReturn(mock(AssuranceActivity::class.java))
        `when`(mockAssuranceFloatingButton.isActive()).thenReturn(false)

        // Test
        assuranceSessionPresentationManager.onActivityResumed(mockAssuranceActivity)

        // Verify
        verify(mockAssuranceFloatingButton, never()).hide()
    }

    @Test
    fun `Test #onActivityResumed with non Assurance Activity, active FloatingButton`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        `when`(mockAppContextService.currentActivity).thenReturn(mockActivity)
        `when`(mockAssuranceFloatingButton.isActive()).thenReturn(true)

        // Test
        assuranceSessionPresentationManager.onActivityResumed(mockActivity)

        // Verify
        verify(mockAssuranceFloatingButton).show()
    }

    @Test
    fun `Test #onActivityResumed with non Assurance Activity, inactive FloatingButton`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        `when`(mockAppContextService.currentActivity).thenReturn(mockActivity)
        `when`(mockAssuranceFloatingButton.isActive()).thenReturn(false)

        // Test
        assuranceSessionPresentationManager.onActivityResumed(mockActivity)

        // Verify
        verify(mockAssuranceFloatingButton, never()).show()
    }

    @Test
    fun `Test #isAuthorisingPresentationActive when phase is Pin connect`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.PIN,
            mockAssuranceFloatingButton
        )

        val credentials = AssuranceAppState.AssuranceAuthorization.PinConnect("1234")
        AssuranceComponentRegistry.appState.onSessionPhaseChange(
            AssuranceAppState.SessionPhase.Authorizing(
                credentials
            )
        )

        // Test
        assertTrue { assuranceSessionPresentationManager.isAuthorizingPresentationActive() }
    }

    @Test
    fun `Test #isAuthorisingPresentationActive when phase is Quick Connect`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        val credentials = AssuranceAppState.AssuranceAuthorization.QuickConnect(AssuranceConstants.AssuranceEnvironment.PROD)
        AssuranceComponentRegistry.appState.onSessionPhaseChange(
            AssuranceAppState.SessionPhase.Authorizing(
                credentials
            )
        )

        // Test
        assertTrue { assuranceSessionPresentationManager.isAuthorizingPresentationActive() }
    }

    @Test
    fun `Test #isAuthorisingPresentationActive when phase is not Authorizing`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )

        AssuranceComponentRegistry.appState.onSessionPhaseChange(
            AssuranceAppState.SessionPhase.Connected
        )

        // Test
        assertFalse { assuranceSessionPresentationManager.isAuthorizingPresentationActive() }
    }

    @Test
    fun `Test #logLocalUI updates app status logs`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )
        val message = "Assurance disconnected, attempting to reconnect ..."

        // Test
        assuranceSessionPresentationManager.logLocalUI(
            AssuranceConstants.UILogColorVisibility.HIGH,
            message
        )

        // Verify
        assertEquals(
            AssuranceAppState.StatusLog(
                AssuranceConstants.UILogColorVisibility.HIGH,
                message
            ),
            AssuranceComponentRegistry.appState.statusLogs.value[0]
        )
    }

    @Test
    fun `Test #logLocalUI updates when message is null`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )
        val message = null

        // Test
        assuranceSessionPresentationManager.logLocalUI(
            AssuranceConstants.UILogColorVisibility.HIGH,
            message
        )

        // Verify
        assertTrue { AssuranceComponentRegistry.appState.statusLogs.value.isEmpty() }
    }

    @Test
    fun `Test #logLocalUI updates when log level is null`() {
        // Setup
        val assuranceSessionPresentationManager = AssuranceSessionPresentationManager(
            SessionAuthorizingPresentationType.QUICK_CONNECT,
            mockAssuranceFloatingButton
        )
        val message = "Some message"

        // Test
        assuranceSessionPresentationManager.logLocalUI(null, message)

        // Verify
        assertTrue { AssuranceComponentRegistry.appState.statusLogs.value.isEmpty() }
    }

    @After
    fun teardown() {
        mockedStaticServiceProvider.close()
        AssuranceComponentRegistry.appState.onSessionPhaseChange(
            AssuranceAppState.SessionPhase.Disconnected(
                null
            )
        )
        AssuranceComponentRegistry.appState.clearLogs()
    }
}
