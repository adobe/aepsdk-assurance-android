/*
 * Copyright 2023 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance

import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceEnvironment
import com.adobe.marketing.mobile.services.Log

/**
 * Handles the UI operations for the Assurance session. Acts as a bridge between the UI and the
 * [AssuranceSession]
 * @param orchestrator the [AssuranceSessionOrchestrator] instance to control the active session.
 */
internal class SessionUIOperationHandler(private val orchestrator: AssuranceSessionOrchestrator) {
    internal companion object {
        private const val LOG_TAG = "SessionUIOperationHandler"
    }

    /**
     * Attempts a connection to Assurance with the provided details. If there is an active session,
     * it will be disconnected and a new session will be created. If no active session exists, a
     * new session will be created.
     * @param sessionId the session id for the Assurance session
     * @param token the pin for the Assurance session
     * @param environment the environment for the Assurance session
     * @param listener the [AssuranceSessionStatusListener] to receive the session status updates about the session
     * @param authorizingPresentationType the [SessionAuthorizingPresentationType] for the session
     */
    internal fun onConnect(
        sessionId: String,
        token: String,
        environment: AssuranceEnvironment,
        listener: AssuranceSessionStatusListener,
        authorizingPresentationType: SessionAuthorizingPresentationType
    ) {
        val activeSession = orchestrator.activeSession

        // Check if there is an active session.
        activeSession?.let {
            if (activeSession.authorizingPresentationType != authorizingPresentationType) {
                // This means that the active session exists and differs from the requested type.
                // This should never happen in an ideal situation.
                Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Cannot start $authorizingPresentationType session. A ${activeSession.authorizingPresentationType} session exists."
                )
                listener.onSessionDisconnected(
                    AssuranceConstants.AssuranceConnectionError.UNEXPECTED_ERROR
                )
            } else {
                // This is a retry scenario. Disconnect existing session without clearing the
                // buffered events for this retry scenario and connect again.
                Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Disconnecting active session of and recreating."
                )
                orchestrator.terminateSession(false)
            }
        }

        orchestrator.createSession(sessionId, environment, token, listener, authorizingPresentationType)
    }

    /**
     * Disconnects the active session. Typically corresponds to the "Disconnect" button in the UI.
     */
    internal fun onDisconnect() {
        Log.debug(
            Assurance.LOG_TAG,
            LOG_TAG,
            "On Disconnect clicked. Disconnecting session."
        )
        orchestrator.terminateSession(true)
    }

    /**
     * Disconnects from the active session. Typically corresponds to the "Cancel" button in the UI.
     */
    internal fun onCancel() {
        Log.debug(
            Assurance.LOG_TAG,
            LOG_TAG,
            "On Cancel Clicked. Disconnecting session."
        )
        orchestrator.terminateSession(true)
    }
}
