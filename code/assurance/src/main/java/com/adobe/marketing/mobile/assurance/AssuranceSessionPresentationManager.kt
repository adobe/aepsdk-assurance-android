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

import android.app.Activity
import android.content.Intent
import com.adobe.marketing.mobile.assurance.AssuranceConstants.SocketCloseCode
import com.adobe.marketing.mobile.assurance.AssuranceConstants.UILogColorVisibility
import com.adobe.marketing.mobile.assurance.AssuranceWebViewSocket.SocketReadyState
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceActivity
import com.adobe.marketing.mobile.assurance.internal.ui.floatingbutton.AssuranceFloatingButton
import com.adobe.marketing.mobile.services.ServiceProvider

/** Manages the UI elements required for an Assurance Session.  */
internal class AssuranceSessionPresentationManager(private val authorizingPresentationType: SessionAuthorizingPresentationType) {

    companion object {
        private const val LOG_TAG = "AssuranceSessionPresentationManager"
    }

    private val button: AssuranceFloatingButton =
        AssuranceFloatingButton(ServiceProvider.getInstance().appContextService)

    /**
     * Shows the UI elements (currently connecting screen) that are required when a session
     * connection has been successfully established.
     */
    @JvmName("onSessionConnected")
    internal fun onSessionConnected() {
        AssuranceComponentRegistry.appState.onSessionPhaseChange(AssuranceAppState.SessionPhase.Connected)
        button.apply {
            show()
            updateGraphic(true)
        }
        logLocalUI(
            UILogColorVisibility.LOW,
            "Assurance connection established."
        )
    }

    /** Shows the UI elements that are required when a session connection has been disconnected.  */
    @JvmName("onSessionDisconnected")
    internal fun onSessionDisconnected(closeCode: Int) {
        val error = SocketCloseCode.toAssuranceConnectionError(closeCode)
        if (error == null) {
            AssuranceComponentRegistry.appState.onSessionPhaseChange(
                AssuranceAppState.SessionPhase.Disconnected()
            )
            button.remove()
            logLocalUI(
                UILogColorVisibility.LOW,
                "Assurance disconnected."
            )
            return
        }

        if (isAuthorizingPresentationActive()) {
            // Stay in Authorizing phase since the UI is active and allow the UI action to change
            // the phase
        } else {
            // Show the error screen
            AssuranceComponentRegistry.appState.onSessionPhaseChange(
                AssuranceAppState.SessionPhase.Disconnected(error, false)
            )
            showAssuranceActivity(ServiceProvider.getInstance().appContextService.currentActivity)
        }
    }

    /**
     * Shows the UI elements that are required when a session is re-connecting (implicitly) after
     * encountering an error.
     */
    @JvmName("onSessionReconnecting")
    internal fun onSessionReconnecting() {
        button.updateGraphic(false)
        logLocalUI(
            UILogColorVisibility.HIGH,
            "Assurance disconnected, attempting to reconnect ..."
        )
        AssuranceComponentRegistry.appState.onSessionPhaseChange(
            AssuranceAppState.SessionPhase.Disconnected(
                reconnecting = true
            )
        )
    }

    /**
     * Manages the UI elements that are required when a session connectivity chanage has occurred.
     *
     * @param newState the new connection state of the socket
     */
    @JvmName("onSessionStateChange")
    internal fun onSessionStateChange(newState: SocketReadyState) {
        button.updateGraphic(newState == SocketReadyState.OPEN)
    }

    /**
     * Notification about an activity of the host app resuming. Needed to manage button placement on
     * the current activity as well as launching a pin screen as necessary.
     *
     * @param activity the activity of the host application that has resumed
     */
    @JvmName("onActivityResumed")
    internal fun onActivityResumed(activity: Activity) {
        if (AssuranceUtil.isAssuranceActivity(activity)) {
            if (button.isActive()) button.hide()
        } else {
            if (button.isActive()) button.show()
        }

        if (isAuthorizingPresentationActive()) {
            showAssuranceActivity(activity)
        }
    }

    @JvmName("isAuthorizingPresentationActive")
    internal fun isAuthorizingPresentationActive(): Boolean {
        val phase = AssuranceComponentRegistry.appState.sessionPhase.value
        return if (phase is AssuranceAppState.SessionPhase.Authorizing) {
            val isPinConnect =
                (authorizingPresentationType == SessionAuthorizingPresentationType.PIN) &&
                    (phase.assuranceAuthorization is AssuranceAppState.AssuranceAuthorization.PinConnect)
            val isQuickConnect =
                (authorizingPresentationType == SessionAuthorizingPresentationType.QUICK_CONNECT) &&
                    (phase.assuranceAuthorization is AssuranceAppState.AssuranceAuthorization.QuickConnect)
            return isPinConnect || isQuickConnect
        } else {
            false
        }
    }

    /**
     * Logs a message on the Assurance Session Status UI view.
     *
     * @param visibility the level with with the {@param message} needs to be logged
     * @param message the message that needs to be logged
     */
    @JvmName("logLocalUI")
    internal fun logLocalUI(visibility: UILogColorVisibility?, message: String?) {
        // TODO
    }

    private fun showAssuranceActivity(currentActivity: Activity?) {
        if (currentActivity == null) return
        val intent = Intent(currentActivity, AssuranceActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        currentActivity.startActivity(intent)
    }
}

/**
 * The type of presentation that is required for authorizing the Assurance Session.
 */
internal enum class SessionAuthorizingPresentationType {
    PIN, QUICK_CONNECT
}
