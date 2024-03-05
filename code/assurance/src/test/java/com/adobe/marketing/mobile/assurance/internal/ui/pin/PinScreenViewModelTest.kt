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

package com.adobe.marketing.mobile.assurance.internal.ui.pin

import com.adobe.marketing.mobile.assurance.AssuranceComponentRegistry
import com.adobe.marketing.mobile.assurance.AssuranceConstants
import com.adobe.marketing.mobile.assurance.AssuranceSessionStatusListener
import com.adobe.marketing.mobile.assurance.AssuranceStateManager
import com.adobe.marketing.mobile.assurance.SessionAuthorizingPresentationType
import com.adobe.marketing.mobile.assurance.SessionUIOperationHandler
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertEquals

class PinScreenViewModelTest {

    companion object {
        private const val SESSION_ID = "SOME_SESSION_ID"
    }

    @Mock
    private lateinit var mockAssuranceSessionUIOperationHandler: SessionUIOperationHandler

    @Mock
    private lateinit var mockAssuranceStateManager: AssuranceStateManager

    private lateinit var pinScreenViewModel: PinScreenViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Initialize AssuranceComponentRegistry with mock AssuranceStateManager and mock AssuranceSessionUIOperationHandler
        AssuranceComponentRegistry.initialize(mockAssuranceStateManager, mockAssuranceSessionUIOperationHandler)

        pinScreenViewModel =
            PinScreenViewModel(SESSION_ID, AssuranceConstants.AssuranceEnvironment.DEV)
    }

    @Test
    fun `Test #onAction with PinScreenAction Number`() {
        // Test #onAction with PinScreenAction Number once
        pinScreenViewModel.onAction(PinScreenAction.Number("1"))

        // Verify
        assert(pinScreenViewModel.state.value.pin == "1")

        // simulate another number
        pinScreenViewModel.onAction(PinScreenAction.Number("4"))

        // Verify
        assert(pinScreenViewModel.state.value.pin == "14")
    }

    @Test
    fun `Test #onAction with PinScreenAction Number with more than 4 numbers`() {
        // Test #onAction with PinScreenAction Number once
        pinScreenViewModel.onAction(PinScreenAction.Number("1"))
        pinScreenViewModel.onAction(PinScreenAction.Number("4"))
        pinScreenViewModel.onAction(PinScreenAction.Number("5"))
        pinScreenViewModel.onAction(PinScreenAction.Number("6"))
        pinScreenViewModel.onAction(PinScreenAction.Number("3"))

        // Verify
        assert(pinScreenViewModel.state.value.pin == "1456")
    }

    @Test
    fun `Test #onAction with PinScreenAction Delete`() {
        pinScreenViewModel.onAction(PinScreenAction.Number("1"))
        pinScreenViewModel.onAction(PinScreenAction.Number("2"))
        pinScreenViewModel.onAction(PinScreenAction.Number("3"))
        pinScreenViewModel.onAction(PinScreenAction.Number("4"))

        // Test #onAction with PinScreenAction Delete once
        pinScreenViewModel.onAction(PinScreenAction.Delete)

        // Verify
        assert(pinScreenViewModel.state.value.pin == "123")
    }

    @Test
    fun `Test #onAction with PinScreenAction Delete with no numbers`() {
        pinScreenViewModel.onAction(PinScreenAction.Delete)

        // Verify
        assert(pinScreenViewModel.state.value.pin == "")
    }

    @Test
    fun `Test #onAction with PinScreenAction Connect when sessionId is blank`() {
        val pinScreenViewModel = PinScreenViewModel("", AssuranceConstants.AssuranceEnvironment.DEV)
        pinScreenViewModel.onAction(PinScreenAction.Connect("1234"))

        // Verify
        verify(mockAssuranceSessionUIOperationHandler, never()).onConnect(
            any(),
            any(),
            any(),
            any(),
            any()
        )
        assertEquals(
            pinScreenViewModel.state.value.connectionState,
            ConnectionState.Disconnected(
                AssuranceConstants.AssuranceConnectionError.UNEXPECTED_ERROR
            )
        )
    }

    @Test
    fun `Test #onAction with PinScreenAction Connect with valid sessionId`() {
        pinScreenViewModel.onAction(PinScreenAction.Connect("1234"))

        // Verify
        verify(mockAssuranceSessionUIOperationHandler).onConnect(
            eq(SESSION_ID),
            eq("1234"),
            eq(AssuranceConstants.AssuranceEnvironment.DEV),
            any<AssuranceSessionStatusListener>(),
            any<SessionAuthorizingPresentationType>()
        )
    }

    @Test
    fun `Test #onAction with PinScreenAction Cancel`() {
        // Test #onAction with PinScreenAction Cancel once
        pinScreenViewModel.onAction(PinScreenAction.Cancel)

        // Verify
        verify(mockAssuranceSessionUIOperationHandler).onCancel()
    }

    @Test
    fun `Test #onAction with PinScreenAction Retry`() {
        // Test #onAction with PinScreenAction Retry once
        pinScreenViewModel.onAction(PinScreenAction.Retry)

        // Verify
        assert(pinScreenViewModel.state.value.pin == "")
    }

    @Test
    fun `Test SessionConnection callbacks on SessionConnected`() {
        // Simulate a connection to capture the status listener
        pinScreenViewModel.onAction(PinScreenAction.Connect("1234"))

        val statusListenerCaptor: KArgumentCaptor<AssuranceSessionStatusListener> = argumentCaptor()
        // Verify
        verify(mockAssuranceSessionUIOperationHandler).onConnect(
            eq(SESSION_ID),
            eq("1234"),
            eq(AssuranceConstants.AssuranceEnvironment.DEV),
            statusListenerCaptor.capture(),
            any<SessionAuthorizingPresentationType>()
        )

        val capturedStatusListener = statusListenerCaptor.firstValue
        capturedStatusListener.onSessionConnected()

        // Verify
        assert(pinScreenViewModel.state.value.connectionState is ConnectionState.Connected)
    }

    @Test
    fun `Test SessionConnection callbacks on SessionDisconnected`() {
        // Simulate a connection to capture the status listener
        pinScreenViewModel.onAction(PinScreenAction.Connect("1234"))

        val statusListenerCaptor: KArgumentCaptor<AssuranceSessionStatusListener> = argumentCaptor()
        // Verify
        verify(mockAssuranceSessionUIOperationHandler).onConnect(
            eq(SESSION_ID),
            eq("1234"),
            eq(AssuranceConstants.AssuranceEnvironment.DEV),
            statusListenerCaptor.capture(),
            any<SessionAuthorizingPresentationType>()
        )

        val capturedStatusListener = statusListenerCaptor.firstValue
        capturedStatusListener.onSessionDisconnected(AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR)

        // Verify
        assert(pinScreenViewModel.state.value.connectionState is ConnectionState.Disconnected)
        assertEquals(
            AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR,
            (pinScreenViewModel.state.value.connectionState as ConnectionState.Disconnected).error
        )
    }

    @Test
    fun `Test SessionConnection callbacks on SessionTerminated`() {
        // Simulate a connection to capture the status listener
        pinScreenViewModel.onAction(PinScreenAction.Connect("1234"))

        val statusListenerCaptor: KArgumentCaptor<AssuranceSessionStatusListener> = argumentCaptor()
        // Verify
        verify(mockAssuranceSessionUIOperationHandler).onConnect(
            eq(SESSION_ID),
            eq("1234"),
            eq(AssuranceConstants.AssuranceEnvironment.DEV),
            statusListenerCaptor.capture(),
            any<SessionAuthorizingPresentationType>()
        )

        val capturedStatusListener = statusListenerCaptor.firstValue
        capturedStatusListener.onSessionTerminated(AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR)

        // Verify
        assert(pinScreenViewModel.state.value.connectionState is ConnectionState.Disconnected)
    }

    @After
    fun tearDown() {
        AssuranceComponentRegistry.reset()
    }
}
