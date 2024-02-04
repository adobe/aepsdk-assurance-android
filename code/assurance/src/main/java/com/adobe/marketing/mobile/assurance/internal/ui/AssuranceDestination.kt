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

package com.adobe.marketing.mobile.assurance.internal.ui

import com.adobe.marketing.mobile.assurance.AssuranceAppState.AssuranceAuthorization
import com.adobe.marketing.mobile.assurance.AssuranceAppState.SessionPhase

/**
 * Defines various destination Assurance screens that can be navigated to based on the state of the current
 * Assurance session.
 * @param route the route for the destination to navigate to
 */
internal sealed class AssuranceDestination(val route: AssuranceNavRoute) {
    /**
     * Represents the Pin authorization screen destination.
     * @param pinConnect the pin authorization details
     */
    data class PinDestination(val pinConnect: AssuranceAuthorization.PinConnect) :
        AssuranceDestination(AssuranceNavRoute.PinCodeRoute)

    /**
     * Represents the Quick Connect authorization screen destination.
     * @param quickConnect the quick connect authorization details
     */
    data class QuickConnectDestination(val quickConnect: AssuranceAuthorization.QuickConnect) :
        AssuranceDestination(AssuranceNavRoute.QuickConnectRoute)

    /**
     * Represents the Status screen destination.
     */
    object StatusDestination : AssuranceDestination(AssuranceNavRoute.StatusRoute)

    /**
     * Represents the Error screen destination.
     * @param disconnected the disconnected phase details
     */
    data class ErrorDestination(val disconnected: SessionPhase.Disconnected) :
        AssuranceDestination(AssuranceNavRoute.ErrorRoute)

    /**
     * Fall-back destination in case of an unknown state.
     */
    object Unknown : AssuranceDestination(AssuranceNavRoute.UnknownRoute)

    companion object {
        /**
         * Maps an AssuranceDestination from the given AssuranceAppState.SessionPhase.
         * @param sessionPhase the session phase to map the destination from
         */
        internal fun fromSessionPhase(sessionPhase: SessionPhase): AssuranceDestination {
            return when (sessionPhase) {
                is SessionPhase.Disconnected -> mapDisconnectedPhase(sessionPhase)
                is SessionPhase.Authorizing -> mapAuthorizingPhase(sessionPhase)
                SessionPhase.Connected -> StatusDestination
            }
        }

        /**
         * Returns an AssuranceDestination based on the authorizing phase type.
         * @param authorizing authorizing details
         */
        private fun mapAuthorizingPhase(authorizing: SessionPhase.Authorizing): AssuranceDestination {
            return when (authorizing.assuranceAuthorization) {
                is AssuranceAuthorization.PinConnect -> {
                    val sessionId = authorizing.assuranceAuthorization.sessionId
                    val environment = authorizing.assuranceAuthorization.environment
                    PinDestination(AssuranceAuthorization.PinConnect(sessionId, environment))
                }

                is AssuranceAuthorization.QuickConnect -> {
                    val environment = authorizing.assuranceAuthorization.environment
                    QuickConnectDestination(AssuranceAuthorization.QuickConnect(environment))
                }
            }
        }

        /**
         * Returns an AssuranceDestination based on the disconnected phase details.
         * @param disconnected disconnected phase details
         */
        private fun mapDisconnectedPhase(disconnected: SessionPhase.Disconnected): AssuranceDestination {
            return if (disconnected.reconnecting) {
                StatusDestination
            } else if (disconnected.error != null) {
                ErrorDestination(disconnected)
            } else {
                // this case should never happen
                Unknown
            }
        }
    }
}
