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

import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceConnectionError
import com.adobe.marketing.mobile.assurance.AssuranceSession.AssuranceSessionStatusListener
import com.adobe.marketing.mobile.services.ServiceProvider
import java.lang.ref.WeakReference

/**
 * Manages authorizing UI for QuickConnect. Due to the authorization happening before session
 * creation for QuickConnect, this class is a proxy for relaying the session status to the
 * AssuranceQuickConnectActivity.
 */
internal class QuickConnectAuthorizingPresentation(
    sessionStatusListener: AssuranceSessionStatusListener?
) : SessionAuthorizingPresentation {

    /**
     * A weak reference to the status listener of the AssuranceQuickConnectActivity.
     */
    private val presentationDelegate: WeakReference<AssuranceSessionStatusListener> =
        WeakReference(sessionStatusListener)

    override fun isDisplayed(): Boolean {
        return ServiceProvider.getInstance().appContextService.currentActivity is AssuranceQuickConnectActivity
    }

    override fun reorderToFront() {
        // no-op quick connect UI is always launched on demand
        // so there is no reason to re-order
    }

    override fun showAuthorization() {
        // no-op
        // Unlike a PIN authorized session (where the session and its sessionId exists before
        // authorization) the quick connect authorization happens before a session is created.
    }

    override fun onConnecting() {
        // no op - managed internally by the AssuranceQuickConnectActivity
    }

    override fun onConnectionSucceeded() {
        presentationDelegate.get()?.onSessionConnected()
    }

    override fun onConnectionFailed(connectionError: AssuranceConnectionError, shouldShowRetry: Boolean) {
        presentationDelegate.get()?.onSessionTerminated(connectionError)
    }
}
