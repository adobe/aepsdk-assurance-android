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

package com.adobe.marketing.mobile.assurance

/**
 * Data class for storing the Assurance shared state.
 * @param clientId the client id
 * @param sessionId the session id
 */
internal data class AssuranceSharedState(val clientId: String, val sessionId: String) {
    private val integrationId: String
        get() = if (clientId.isNotBlank() && sessionId.isNotBlank()) {
            "$sessionId|$clientId"
        } else {
            ""
        }

    internal fun asMap(): Map<String, Any> = mutableMapOf<String, Any>().apply {
        if (clientId.isNotBlank()) {
            this[AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_CLIENT_ID] = clientId
        }

        if (sessionId.isNotBlank()) {
            this[AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_SESSION_ID] = sessionId
        }

        if (integrationId.isNotBlank()) {
            this[AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_INTEGRATION_ID] = integrationId
        }
    }
}
