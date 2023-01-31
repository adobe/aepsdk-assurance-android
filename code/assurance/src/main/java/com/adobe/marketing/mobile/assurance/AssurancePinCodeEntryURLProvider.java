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


import android.net.Uri;
import android.os.Handler;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

class AssurancePinCodeEntryURLProvider
        implements AssuranceFullScreenTakeover.FullScreenTakeoverCallbacks {
    private static final String LOG_TAG = "AssurancePinCodeEntryURLProvider";
    private static final String MESSAGE_HOST_CANCEL = "cancel";
    private static final String MESSAGE_HOST_CONFIRM = "confirm";
    private static final String HTML_QUERY_KEY_PIN_CODE = "code";

    private final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle;
    Runnable deferredActivityRunnable;
    private final AssuranceStateManager assuranceStateManager;
    private AssuranceFullScreenTakeover pinCodeTakeover;
    private AssuranceSessionOrchestrator.SessionUIOperationHandler uiOperationHandler;
    private boolean isDisplayed;

    AssurancePinCodeEntryURLProvider(
            final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle,
            final AssuranceSessionOrchestrator.SessionUIOperationHandler uiOperationHandler,
            final AssuranceStateManager assuranceStateManager) {
        this.applicationHandle = applicationHandle;
        this.assuranceStateManager = assuranceStateManager;
        this.deferredActivityRunnable = null;
        this.uiOperationHandler = uiOperationHandler;
    }

    public boolean isDisplayed() {
        return isDisplayed;
    }

    void launchPinDialog() {
        // if we already have a pincode takeover, we should exit.
        if (pinCodeTakeover != null) {
            return;
        }

        final AssurancePinCodeEntryURLProvider thisRef = this;

        // Load and launch pin code entry dialog
        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // load html and such
                                    final ClassLoader cl = this.getClass().getClassLoader();

                                    if (cl == null) {
                                        Log.error(
                                                Assurance.LOG_TAG,
                                                LOG_TAG,
                                                "Pin code entry unable to get class loader.");
                                        return;
                                    }

                                    final InputStream is =
                                            cl.getResourceAsStream("assets/PinDialog.html");
                                    final Scanner s = new Scanner(is).useDelimiter("\\A");
                                    final String html = s.next();
                                    is.close();

                                    if (html == null || html.length() == 0) {
                                        Log.error(
                                                Assurance.LOG_TAG,
                                                LOG_TAG,
                                                "Unable to load ui for pin dialog, assets might be"
                                                        + " unavailable.");
                                        return;
                                    }

                                    thisRef.pinCodeTakeover =
                                            new AssuranceFullScreenTakeover(
                                                    applicationHandle.getAppContext(),
                                                    html,
                                                    thisRef);

                                    if (thisRef.applicationHandle != null) {
                                        Log.trace(
                                                Assurance.LOG_TAG,
                                                LOG_TAG,
                                                "Attempting to display the PinCode Screen.");

                                        // use main thread to get current activity and start the
                                        // pinCodeEntry activity
                                        // keeping this in main thread will synchronize and minimize
                                        // the race condition in getting the correct current
                                        // activity
                                        Handler mainHandler =
                                                new Handler(
                                                        applicationHandle
                                                                .getAppContext()
                                                                .getMainLooper());
                                        mainHandler.post(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (thisRef != null
                                                                && thisRef.applicationHandle
                                                                        != null) {

                                                            // have a runnable to defer the showing
                                                            // of pinCodeEntry screen, until the
                                                            // current activity is set
                                                            final Runnable providerRunnable =
                                                                    new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            thisRef.pinCodeTakeover
                                                                                    .show(
                                                                                            thisRef
                                                                                                    .applicationHandle
                                                                                                    .getCurrentActivity());
                                                                        }
                                                                    };

                                                            if (thisRef.applicationHandle
                                                                            .getCurrentActivity()
                                                                    == null) {
                                                                Log.debug(
                                                                        Assurance.LOG_TAG,
                                                                        LOG_TAG,
                                                                        "No activity reference,"
                                                                                + " deferring"
                                                                                + " connection"
                                                                                + " dialog");
                                                                deferredActivityRunnable =
                                                                        providerRunnable;
                                                            } else {
                                                                providerRunnable.run();
                                                            }
                                                        }
                                                    }
                                                });
                                    } else {
                                        Log.error(
                                                Assurance.LOG_TAG,
                                                LOG_TAG,
                                                "Unable to show PinDialog, parent session is"
                                                        + " null.");
                                    }
                                } catch (final IOException ex) {
                                    Log.error(
                                            Assurance.LOG_TAG,
                                            LOG_TAG,
                                            String.format(
                                                    "Unable to read assets/PinDialog.html: %s",
                                                    ex.getLocalizedMessage()));
                                }
                            }
                        })
                .start();
    }

    public void onConnecting() {
        if (pinCodeTakeover != null) {
            pinCodeTakeover.runJavascript("showLoading()");
        }
    }

    public void onConnectionSucceeded() {
        if (pinCodeTakeover != null) {
            pinCodeTakeover.remove();
        }
    }

    public void onConnectionFinished() {
        if (pinCodeTakeover != null) {
            pinCodeTakeover.remove();
        }
    }

    public void onConnectionFailed(
            final AssuranceConstants.AssuranceSocketError socketError,
            final boolean shouldShowRetry) {
        pinCodeTakeover.runJavascript(
                "showError('"
                        + socketError.getError()
                        + "', '"
                        + socketError.getErrorDescription()
                        + "', "
                        + shouldShowRetry
                        + ")");
        Log.warning(
                Assurance.LOG_TAG,
                LOG_TAG,
                String.format(
                        "Assurance connection closed. Reason: %s, Description: %s",
                        socketError.getError(), socketError.getErrorDescription()));
    }

    @Override
    public boolean onURLTriggered(final String url) {
        if (url == null) {
            Log.debug(Assurance.LOG_TAG, LOG_TAG, "[onURLTriggered] Failed because of url is null");
            return true;
        }

        final Uri uri = Uri.parse(url);

        if (uri == null) {
            Log.warning(Assurance.LOG_TAG, LOG_TAG, "Could not parse uri: " + url);
            return true;
        } else if (MESSAGE_HOST_CANCEL.equals(uri.getHost())) {
            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Cancel Button clicked. Dismissing the PinCode Screen.");
            pinCodeTakeover.remove();
            uiOperationHandler.onCancel();
        } else if (MESSAGE_HOST_CONFIRM.equals(uri.getHost())) {

            final String orgId = assuranceStateManager.getOrgId(true);

            if (orgId.isEmpty()) {
                Log.debug(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        String.format(
                                "%s %s",
                                AssuranceConstants.AssuranceSocketError.NO_ORGID.getError(),
                                AssuranceConstants.AssuranceSocketError.NO_ORGID.getError()));
                onConnectionFailed(AssuranceConstants.AssuranceSocketError.NO_ORGID, true);
                return true;
            }

            new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    Log.debug(
                                            Assurance.LOG_TAG,
                                            LOG_TAG,
                                            "Connect Button clicked. Making a socket connection.");
                                    uiOperationHandler.onConnect(
                                            uri.getQueryParameter(HTML_QUERY_KEY_PIN_CODE));
                                }
                            })
                    .start();
        } else {
            Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format("Unknown url for pin-code entry received: %s", url));
        }

        // check url and fire callback if needed
        return true;
    }

    @Override
    public void onDismiss(final AssuranceFullScreenTakeover takeover) {
        isDisplayed = false;
    }

    @Override
    public void onShow(final AssuranceFullScreenTakeover takeover) {
        isDisplayed = true;
    }
}
