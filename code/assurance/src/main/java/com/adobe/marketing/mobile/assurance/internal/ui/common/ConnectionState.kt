/*
 * Copyright 2024 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance.internal.ui.common

import com.adobe.marketing.mobile.assurance.AssuranceConstants

/**
 * Represents the current state of the Assurance connection attempt as tracked by the Pin Screen or
 * Quick Connect Screen. Currently, both screens use the same state due to them having the same
 * phases of connection. If this changes in the future, this class can be split into individual
 * state holders.
 */
internal sealed class ConnectionState {
    /**
     * Represents the state of Assurance connection being disconnected.
     * @param error the error that caused the disconnection
     */
    data class Disconnected(val error: AssuranceConstants.AssuranceConnectionError?) :
        ConnectionState()

    /**
     * Represents the state of Assurance connection attempt in progress.
     */
    object Connecting : ConnectionState()

    /**
     * Represents the state of Assurance connection being connected successfully.
     */
    object Connected : ConnectionState()
}
