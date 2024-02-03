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

package com.adobe.marketing.mobile.assurance;

import androidx.annotation.NonNull;

/**
 * A callback that allows components integrating with {@link QuickConnectManager} to receive a
 * notification about the status of the QuickConnect connection request.
 */
interface QuickConnectCallback {

    /**
     * Invoked when an error occurs when during QuickConnect connection workflow.
     *
     * @param error an {@code AssuranceConnectionError} that occurred resulting in quick connect
     *     workflow cancellation
     */
    void onError(@NonNull AssuranceConstants.AssuranceConnectionError error);

    /**
     * Invoked with the quick connect session details when the QuickConnect workflow is successful.
     * These details can be used to establish a connection with the session.
     *
     * @param sessionUUID the sessionId associated with the quick connect session
     * @param token the authorizing token for the quick connect session
     */
    void onSuccess(@NonNull String sessionUUID, @NonNull String token);
}
