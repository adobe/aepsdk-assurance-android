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

package com.adobe.marketing.mobile.assurance.internal.ui.quickconnect

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.adobe.marketing.mobile.assurance.internal.AssuranceComponentRegistry
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants
import com.adobe.marketing.mobile.assurance.internal.AssuranceSessionStatusListener
import com.adobe.marketing.mobile.assurance.internal.AssuranceStateManager
import com.adobe.marketing.mobile.assurance.internal.QuickConnectCallback
import com.adobe.marketing.mobile.assurance.internal.QuickConnectManager
import com.adobe.marketing.mobile.assurance.internal.SessionAuthorizingPresentationType
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState
import java.util.concurrent.Executors

/**
 * A view model for the QuickConnect screen. This view model is responsible for bridging the UI
 * with [QuickConnectManager] and [SessionUIOperationHandler].
 * Note that the operations that result in state changes by view model are not synchronized because
 * they are serial by design. The UI button click events trigger the [QuickConnectManager] which
 * on success triggers the session connection via [SessionUIOperationHandler]. Any point of time
 * during this process, if the user cancels the operation, the activity is finished discarding the
 * state change.
 */
internal class QuickConnectViewModel : ViewModel {
    private val quickConnectManager: QuickConnectManager
    private val environment: AssuranceConstants.AssuranceEnvironment
    private val _state: MutableState<ConnectionState>
    internal val state: State<ConnectionState>
        get() = _state

    /**
     * Creates a new [QuickConnectViewModel].
     * @param assuranceStateManager the [AssuranceStateManager] to use for the session
     * @param environment the Assurance environment that the session is being connected to
     */
    internal constructor(
        assuranceStateManager: AssuranceStateManager,
        environment: AssuranceConstants.AssuranceEnvironment
    ) : this(
        mutableStateOf(ConnectionState.Disconnected(null)),
        assuranceStateManager,
        environment
    )

    /**
     * Creates a new [QuickConnectViewModel]. Exists only to allow cascading constructors for
     * testing.
     * @param state the state of the connection
     * @param assuranceStateManager the [AssuranceStateManager] to use for the session
     * @param environment the Assurance environment that the session is being connected to
     */
    private constructor(
        state: MutableState<ConnectionState>,
        assuranceStateManager: AssuranceStateManager,
        environment: AssuranceConstants.AssuranceEnvironment
    ) : this(state, AssuranceStatusListenerWrapper(state), assuranceStateManager, environment)

    /**
     * Creates a new [QuickConnectViewModel]. Exists only to allow cascading constructors for
     * testing.
     * @param state the initial state of the connection
     * @param assuranceStatusListenerWrapper the [AssuranceStatusListenerWrapper] to use for the session
     * @param assuranceStateManager the [AssuranceStateManager] to use for the session
     * @param environment the Assurance environment that the session is being connected to
     */
    @VisibleForTesting
    internal constructor(
        state: MutableState<ConnectionState>,
        assuranceStatusListenerWrapper: AssuranceStatusListenerWrapper,
        assuranceStateManager: AssuranceStateManager,
        environment: AssuranceConstants.AssuranceEnvironment
    ) : this(
        state = state,
        quickConnectManager = QuickConnectManager(
            assuranceStateManager,
            Executors.newSingleThreadScheduledExecutor(),
            if (environment == AssuranceConstants.AssuranceEnvironment.PROD) "" else environment.stringValue,
            object : QuickConnectCallback {
                override fun onError(error: AssuranceConstants.AssuranceConnectionError) {
                    state.value = ConnectionState.Disconnected(error)
                }

                override fun onSuccess(sessionUUID: String, token: String) {
                    AssuranceComponentRegistry.sessionUIOperationHandler?.onConnect(
                        sessionId = sessionUUID,
                        token = token,
                        environment = environment,
                        listener = assuranceStatusListenerWrapper,
                        authorizingPresentationType = SessionAuthorizingPresentationType.QUICK_CONNECT
                    )
                }
            }
        ),
        environment = environment
    )

    /**
     * Creates a new [QuickConnectViewModel]. Exists only to allow cascading constructors for
     * testing.
     * @param state the initial state of the connection
     * @param quickConnectManager the [QuickConnectManager] to manage device registration
     * @param environment the Assurance environment that the session is being connected to
     */
    @VisibleForTesting
    internal constructor(
        state: MutableState<ConnectionState>,
        quickConnectManager: QuickConnectManager,
        environment: AssuranceConstants.AssuranceEnvironment
    ) {
        this._state = state
        this.quickConnectManager = quickConnectManager
        this.environment = environment
    }

    /**
     * Handles the user's action on the QuickConnect screen.
     * @param quickConnectScreenAction the action the user took on the UI
     */
    internal fun onAction(quickConnectScreenAction: QuickConnectScreenAction) {
        when (quickConnectScreenAction) {
            is QuickConnectScreenAction.Cancel -> {
                quickConnectManager.cancel()
                _state.value = ConnectionState.Disconnected(null)
                quickConnectManager.cancel()
                AssuranceComponentRegistry.sessionUIOperationHandler?.onCancel()
            }

            is QuickConnectScreenAction.Retry -> {
                _state.value = ConnectionState.Connecting
                quickConnectManager.registerDevice()
            }

            is QuickConnectScreenAction.Connect -> {
                _state.value = ConnectionState.Connecting
                quickConnectManager.registerDevice()
            }
        }
    }
}

/**
 * Factory for creating a QuickConnect view model. Exists because this is the only way to pass parameters
 * to a ViewModel.
 *
 * @param assuranceStateManager the [AssuranceStateManager] to use for the session
 * @param environment the Assurance environment that the session is being connected to
 */
internal class QuickConnectScreenViewModelFactory(
    private val assuranceStateManager: AssuranceStateManager,
    private val environment: AssuranceConstants.AssuranceEnvironment
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickConnectViewModel::class.java)) {
            return QuickConnectViewModel(assuranceStateManager, environment) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * A convenience wrapper for [AssuranceSessionStatusListener] that updates the state of the connection.
 * Exists to allow making constructors simpler for the sake of testing.
 * @param quickConnectState the state of the connection
 */
internal class AssuranceStatusListenerWrapper(private val quickConnectState: MutableState<ConnectionState>) :
    AssuranceSessionStatusListener {
    override fun onSessionConnected() {
        quickConnectState.value = ConnectionState.Connected
    }

    override fun onSessionDisconnected(error: AssuranceConstants.AssuranceConnectionError?) {
        quickConnectState.value = ConnectionState.Disconnected(error)
    }

    override fun onSessionTerminated(error: AssuranceConstants.AssuranceConnectionError?) {
        quickConnectState.value = ConnectionState.Disconnected(error)
    }
}
