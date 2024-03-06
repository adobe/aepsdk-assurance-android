/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal.ui.pin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.adobe.marketing.mobile.assurance.internal.AssuranceComponentRegistry
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants.AssuranceConnectionError
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants.AssuranceEnvironment
import com.adobe.marketing.mobile.assurance.internal.AssuranceSessionStatusListener
import com.adobe.marketing.mobile.assurance.internal.SessionAuthorizingPresentationType
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState

/**
 * ViewModel for the PinScreen.
 * @param sessionId the session id that the PIN flow is being used for
 * @param environment the Assurance environment that the session is being connected to
 */
internal class PinScreenViewModel(
    private val sessionId: String,
    private val environment: AssuranceEnvironment
) : ViewModel() {

    private val _state = mutableStateOf(PinScreenState())
    internal val state: State<PinScreenState> = _state

    /**
     * A listener for Assurance session status events. This listener is used to update the UI state
     * in response to Assurance session activity.
     */
    private val assuranceStatusListener = object : AssuranceSessionStatusListener {
        override fun onSessionConnected() {
            _state.value = state.value.copy(connectionState = ConnectionState.Connected)
        }

        override fun onSessionDisconnected(error: AssuranceConnectionError?) {
            _state.value = state.value.copy(connectionState = ConnectionState.Disconnected(error))
        }

        override fun onSessionTerminated(error: AssuranceConnectionError?) {
            _state.value = state.value.copy(connectionState = ConnectionState.Disconnected(error))
        }
    }

    /**
     * Handles an action from the PinScreen UI.
     * @param action the action to handle
     */
    internal fun onAction(action: PinScreenAction) {
        val text = state.value.pin
        when (action) {
            is PinScreenAction.Number -> {
                if (text.length >= 4) return
                _state.value = state.value.copy(pin = text + action.number)
            }

            is PinScreenAction.Delete -> {
                if (text.isBlank()) return
                _state.value = state.value.copy(pin = text.dropLast(1))
            }

            is PinScreenAction.Connect -> {
                connect(action.number)
            }

            is PinScreenAction.Cancel -> {
                AssuranceComponentRegistry.sessionUIOperationHandler?.onCancel()
            }

            is PinScreenAction.Retry -> {
                _state.value = PinScreenState()
            }
        }
    }

    private fun connect(pin: String) {
        val uiOperationHandler = AssuranceComponentRegistry.sessionUIOperationHandler

        if (sessionId.isBlank()) {
            _state.value = state.value.copy(
                connectionState = ConnectionState.Disconnected(
                    AssuranceConnectionError.UNEXPECTED_ERROR
                )
            )
            return
        }

        uiOperationHandler?.onConnect(
            sessionId,
            pin,
            environment,
            assuranceStatusListener,
            SessionAuthorizingPresentationType.PIN
        )
        _state.value = state.value.copy(connectionState = ConnectionState.Connecting)
    }
}

/**
 * Represents the actions that can be performed on the PIN screen.
 */
internal sealed class PinScreenAction {
    /**
     * Represents a number being clicked on the dial pad.
     * @param number the digit that was clicked
     */
    data class Number(val number: String) : PinScreenAction()

    /**
     * Represents the delete button being clicked.
     */
    object Delete : PinScreenAction()

    /**
     * Represents the cancel button being clicked.
     */
    object Cancel : PinScreenAction()

    /**
     * Represents the retry button being clicked.
     */
    object Retry : PinScreenAction()

    /**
     * Represents the connect button being clicked.
     */
    data class Connect(val number: String) : PinScreenAction()
}

/**
 * Factory for creating a PinScreenViewModel. Exists because this is the only way to pass parameters
 * to a ViewModel.
 * @param sessionId the session id that the PIN flow is being used for connecting to
 * @param environment the Assurance environment that the session is being connected to
 */
internal class PinScreenViewModelFactory(
    private val sessionId: String,
    private val environment: AssuranceEnvironment
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PinScreenViewModel::class.java)) {
            return PinScreenViewModel(sessionId, environment) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
