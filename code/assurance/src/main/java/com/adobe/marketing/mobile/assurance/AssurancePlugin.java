/*
 * Copyright 2022 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance;

interface AssurancePlugin {

    /**
     * Returns the vendor name
     *
     * <p>The Plugin will only receive the Control Events from the provided vendor.
     *
     * @return the vendor name
     */
    String getVendor();

    /**
     * Returns the control type
     *
     * <p>For {@link AssuranceEvent} events, controlType is available in the payload under the key
     * "type" The Plugin will only receive the Control Events with the provided ControlType.
     *
     * @return the control type
     */
    String getControlType();

    /**
     * Invoked when a AssuranceEvent is received for a specific vendor and specific control type
     *
     * @param event is AssuranceEvent
     */
    void onEventReceived(final AssuranceEvent event);

    /**
     * Invoked when plugin is successfully registered to the AssuranceSession
     *
     * @param parentSession an instance of the active AssuranceSession
     */
    void onRegistered(final AssuranceSession parentSession);

    /** Invoked when a successful Assurance web socket connection is established */
    void onSessionConnected();

    /**
     * Invoked when an Assurance session is disconnected. More information about various close code
     * could be found here : https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent
     *
     * @param code an integer value representing the reason for websocket session disconnect
     */
    void onSessionDisconnected(final int code);

    /**
     * Invocation of this method guarantees that the Assurance session is completely terminated and
     * the Assurance extension will not reconnect the session on the next app launch.
     */
    void onSessionTerminated();
}
