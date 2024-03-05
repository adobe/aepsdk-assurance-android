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
 * A notification mechanism for components that have the need to be aware of creation and
 * destruction of an {@code AssuranceSession}.
 */
internal interface AssuranceSessionStatusListener {
    /** Callback indicating that the AssuranceSession is connected.  */
    fun onSessionConnected()

    /**
     * Callback indicating that the AssuranceSession is disconnected. Implementers should
     * be aware that the session may be re-established after this callback.
     *
     * @param error an optional `AssuranceConnectionError` if the session was disconnected
     * due to an error.
     */
    fun onSessionDisconnected(error: AssuranceConnectionError?)

    /**
     * Callback indicating that the AssuranceSession is terminated and a connection will not be
     * re-established for the same sessoion.
     *
     * @param error an optional `AssuranceConnectionError` if the session was terminated
     * due to an error.
     */
    fun onSessionTerminated(error: AssuranceConnectionError?)
}
