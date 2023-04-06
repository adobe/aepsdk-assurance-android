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


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.View;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;

/** Manages the UI elements required for an Assurance Session. */
class AssuranceSessionPresentationManager {
    private static final String LOG_TAG = "AssuranceSessionPresentationManager";
    private static final String FULLSCREEN_TAKEOVER_ACTIVITY_CLASSNAME =
            AssuranceFullScreenTakeoverActivity.class.getSimpleName();
    private final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle;
    private AssuranceFloatingButton button;
    private AssuranceConnectionStatusUI statusUI;
    private SessionAuthorizingPresentation authorizingPresentation;

    AssuranceSessionPresentationManager(
            final AssuranceStateManager assuranceStateManager,
            final AssuranceSessionOrchestrator.SessionUIOperationHandler uiOperationHandler,
            final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle,
            final SessionAuthorizingPresentation.Type authorizingPresentationType,
            final AssuranceSession.AssuranceSessionStatusListener authorizingPresentationDelegate) {
        this.applicationHandle = applicationHandle;

        statusUI = new AssuranceConnectionStatusUI(uiOperationHandler, applicationHandle);

        button =
                new AssuranceFloatingButton(
                        applicationHandle,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (statusUI != null) {
                                    statusUI.show();
                                }
                            }
                        });

        if (authorizingPresentationType == SessionAuthorizingPresentation.Type.PIN) {
            authorizingPresentation =
                    new AssurancePinCodeEntryProvider(
                            applicationHandle, uiOperationHandler, assuranceStateManager);
        } else {
            authorizingPresentation =
                    new QuickConnectAuthorizingPresentation(authorizingPresentationDelegate);
        }
    }

    /**
     * Logs a message on the Assurance Session Status UI view.
     *
     * @param visibility the level with with the {@param message} needs to be logged
     * @param message the message that needs to be logged
     */
    void logLocalUI(
            final AssuranceConstants.UILogColorVisibility visibility, final String message) {
        if (statusUI != null) {
            statusUI.addUILog(visibility, message);
        }
    }

    /** Shows the UI elements that are required when a session is initialized. */
    void onSessionInitialized() {
        if (authorizingPresentation != null) {
            authorizingPresentation.showAuthorization();
        }
    }

    /** Shows the UI elements that are required when a session connection is in progress. */
    void onSessionConnecting() {
        if (authorizingPresentation != null) {
            authorizingPresentation.onConnecting();
        }
    }

    /**
     * Shows the UI elements (currently connecting screen) that are required when a session
     * connection has been successfully established.
     */
    void onSessionConnected() {
        if (authorizingPresentation != null) {
            authorizingPresentation.onConnectionSucceeded();
        }

        if (button != null) {
            button.setCurrentGraphic(AssuranceFloatingButtonView.Graphic.CONNECTED);
            button.display();
        }

        logLocalUI(
                AssuranceConstants.UILogColorVisibility.LOW, "Assurance connection established.");
    }

    /** Shows the UI elements that are required when a session connection has been disconnected. */
    void onSessionDisconnected(final int closeCode) {
        if (closeCode == AssuranceConstants.SocketCloseCode.NORMAL) {
            cleanupUIElements();
            return;
        }

        final AssuranceConstants.AssuranceConnectionError assuranceConnectionError =
                AssuranceConstants.SocketCloseCode.toAssuranceConnectionError(closeCode);

        if (assuranceConnectionError != null) {
            displayError(assuranceConnectionError, closeCode);
        } else {
            displayError(
                    AssuranceConstants.AssuranceConnectionError.GENERIC_ERROR,
                    AssuranceConstants.SocketCloseCode.ABNORMAL);
        }
    }

    /**
     * Shows the UI elements that are required when a session is re-connecting (implicitly) after
     * encountering an error.
     */
    void onSessionReconnecting() {
        if (button != null) {
            button.setCurrentGraphic(AssuranceFloatingButtonView.Graphic.DISCONNECTED);
            button.display();
        }

        logLocalUI(
                AssuranceConstants.UILogColorVisibility.HIGH,
                "Assurance disconnected, attempting to reconnect ...");
    }

    /**
     * Manages the UI elements that are required when a session connectivity chanage has occurred.
     *
     * @param newState the new connection state of the socket
     */
    void onSessionStateChange(final AssuranceWebViewSocket.SocketReadyState newState) {
        if (button != null) {
            button.setCurrentGraphic(
                    newState == AssuranceWebViewSocket.SocketReadyState.OPEN
                            ? AssuranceFloatingButtonView.Graphic.CONNECTED
                            : AssuranceFloatingButtonView.Graphic.DISCONNECTED);
        }
    }

    /**
     * Acts on the notification about an activity of the host app starting. Needed to manage edge
     * cases that arise from activity launch sequences.
     *
     * @param activity the activity of the host application that has started
     */
    void onActivityStarted(final Activity activity) {
        // If the Assurance pinpad screen is displayed and the Application tries to start a new
        // activity
        // then reorder the Assurance pinpad activity to display on top
        if (AssuranceFullScreenTakeoverActivity.isDisplayed
                && !(FULLSCREEN_TAKEOVER_ACTIVITY_CLASSNAME.equals(
                        activity.getClass().getSimpleName()))) {

            // bring activity to front
            final Intent fullscreen =
                    new Intent(activity, AssuranceFullScreenTakeoverActivity.class);
            fullscreen.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            fullscreen.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(fullscreen);
            activity.overridePendingTransition(0, 0);
        }
    }

    /**
     * Notification about an activity of the host app resuming. Needed to manage button placement on
     * the current activity as well as launching a pin screen as necessary.
     *
     * @param activity the activity of the host application that has resumed
     */
    void onActivityResumed(final Activity activity) {
        if (button != null) {
            button.onActivityResumed(activity);
        }

        if (authorizingPresentation != null) {
            authorizingPresentation.reorderToFront();
        }
    }

    /**
     * Notification about an activity of the host app being destroyed. Needed to manage button
     * destruction.
     *
     * @param activity the activity of the host application that has been destroyed
     */
    void onActivityDestroyed(final Activity activity) {
        if (button != null) {
            button.onActivityDestroyed(activity);
        }
    }

    private void displayError(
            final AssuranceConstants.AssuranceConnectionError socketError, final int closeCode) {
        if (authorizingPresentation != null && authorizingPresentation.isDisplayed()) {
            // If this is an unhandled/abnormal error while the authorizing screen is on,
            // set the retry flag to true.
            // In case of a non retryable error the "Cancel" button on the authorizing screen will
            // allow socket disconnection and cleaning up of the UI Elements.
            authorizingPresentation.onConnectionFailed(
                    socketError, closeCode == AssuranceConstants.SocketCloseCode.ABNORMAL);
        } else {
            if (closeCode == AssuranceConstants.SocketCloseCode.ABNORMAL) {
                // Do not do anything for Abnormal close code as Assurance SDK currently assumes
                // that all abnormal errors can be re-tried and hence a retry attempt will enforce
                // UI changes.
                return;
            }

            cleanupUIElements();
            showErrorDisplay(socketError);
        }
    }

    /** Releases all the UI elements that are being managed by this class. */
    private void cleanupUIElements() {
        if (button != null) {
            button.remove();
            button = null;
        }

        if (authorizingPresentation != null) {
            authorizingPresentation = null;
        }

        if (statusUI != null) {
            statusUI.dismiss();
            statusUI = null;
        }
    }

    private void showErrorDisplay(final AssuranceConstants.AssuranceConnectionError socketError) {
        final Activity currentActivity = applicationHandle.getCurrentActivity();

        if (currentActivity == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Failed to show fullscreen takeover, current activity is null.");
            return;
        }

        try {
            final Intent errorScreen =
                    new Intent(currentActivity, AssuranceErrorDisplayActivity.class);
            errorScreen.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            errorScreen.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            errorScreen.putExtra(
                    AssuranceConstants.IntentExtraKey.ERROR_NAME, socketError.getError());
            errorScreen.putExtra(
                    AssuranceConstants.IntentExtraKey.ERROR_DESCRIPTION,
                    socketError.getDescription());
            currentActivity.startActivity(errorScreen);
            currentActivity.overridePendingTransition(0, 0);
        } catch (final ActivityNotFoundException ex) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Failed to show fullscreen takeover, could not start activity. Error %s",
                    ex.getLocalizedMessage());
        }
    }

    boolean isAuthorizingPresentationActive() {
        if (authorizingPresentation != null) {
            return authorizingPresentation.isDisplayed();
        }
        return false;
    }
}
