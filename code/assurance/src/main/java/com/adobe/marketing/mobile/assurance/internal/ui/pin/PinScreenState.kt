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

import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState

/**
 * Data class for storing the Pin Screen state.
 * @param pin the pin entered via the UI if any
 * @param connectionState the current connection state as tracked by the Pin Screen
 */
internal data class PinScreenState(
    val pin: String = "",
    val connectionState: ConnectionState = ConnectionState.Disconnected(null)
)
