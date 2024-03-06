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

package com.adobe.marketing.mobile.assurance.internal

import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants.AssuranceConnectionError

/**
 * A callback that allows components integrating with [QuickConnectManager] to receive a
 * notification about the status of the QuickConnect connection request.
 */
internal interface QuickConnectCallback {
    /**
     * Invoked when an error occurs when during QuickConnect connection workflow.
     *
     * @param error an `AssuranceConnectionError` that occurred resulting in quick connect
     * workflow cancellation
     */
    fun onError(error: AssuranceConnectionError)

    /**
     * Invoked with the quick connect session details when the QuickConnect workflow is successful.
     * These details can be used to establish a connection with the session.
     *
     * @param sessionUUID the sessionId associated with the quick connect session
     * @param token the authorizing token for the quick connect session
     */
    fun onSuccess(sessionUUID: String, token: String)
}
