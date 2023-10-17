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

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceEnvironment

/**
 * Represents and acts as a single source of truth for the Assurance workflow across the SDK.
 */
internal class AssuranceAppState {

    /**
     * Represents the data classes associated with an authorization phase of the Assurance session.
     */
    internal sealed class AssuranceAuthorization {
        /**
         * Represents pin-based authorization details.
         * @param sessionId the session id to connect to
         * @param environment the environment to connect to
         */
        data class PinConnect(
            val sessionId: String,
            val environment: AssuranceEnvironment = AssuranceEnvironment.PROD
        ) : AssuranceAuthorization()

        /**
         * Represents quick-connect authorization details.
         * @param environment the environment to connect to
         */
        data class QuickConnect(val environment: AssuranceEnvironment = AssuranceEnvironment.PROD) :
            AssuranceAuthorization()
    }

    /**
     * Represents the phases through which an assurance session can transition.
     */
    internal sealed class SessionPhase {
        /**
         * Represents the disconnected phase of the assurance session.
         * @param error the error that caused the session to disconnect if any
         * @param reconnecting whether the session is reconnecting implicitly
         */
        data class Disconnected(
            val error: AssuranceConstants.AssuranceConnectionError? = null,
            val reconnecting: Boolean = false
        ) : SessionPhase()

        /**
         * Represents the authorizing phase of the assurance session.
         * @param assuranceAuthorization the authorization details for the session
         */
        data class Authorizing(val assuranceAuthorization: AssuranceAuthorization) : SessionPhase()

        /**
         * Represents the connected phase of the assurance session.
         */
        object Connected : SessionPhase()
    }

    private val _sessionPhase = mutableStateOf<SessionPhase>(SessionPhase.Disconnected())

    /**
     * Represents the current phase of the assurance session.
     */
    val sessionPhase: State<SessionPhase> = _sessionPhase

    /**
     * Updates the current session phase to the given [sessionPhase].
     * @param sessionPhase the new session phase
     */
    @JvmName("onSessionPhaseChange")
    internal fun onSessionPhaseChange(sessionPhase: SessionPhase) {
        _sessionPhase.value = sessionPhase
    }
}
