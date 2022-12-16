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
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.assurance.AssuranceConstants.UILogColorVisibility;
import com.adobe.marketing.mobile.services.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Scanner;

class AssuranceConnectionStatusUI
        implements AssuranceFullScreenTakeover.FullScreenTakeoverCallbacks {
    private static final String LOG_TAG = "AssuranceConnectionStatusUI";
    private final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle;
    private final AssuranceSessionOrchestrator.SessionUIOperationHandler uiOperationHandler;
    private AssuranceFullScreenTakeover statusTakeover;

    AssuranceConnectionStatusUI(
            final AssuranceSessionOrchestrator.SessionUIOperationHandler uiOperationHandler,
            final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle) {
        this.applicationHandle = applicationHandle;
        this.uiOperationHandler = uiOperationHandler;

        final AssuranceConnectionStatusUI thisRef = this;

        // load html on background thread
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
                                            cl.getResourceAsStream("assets/StatusInfo.html");

                                    if (is == null) {
                                        Log.error(
                                                Assurance.LOG_TAG,
                                                LOG_TAG,
                                                "Unable to open StatusInfo.html");
                                        return;
                                    }

                                    final Scanner s = new Scanner(is).useDelimiter("\\A");
                                    final String html = s.next();
                                    is.close();

                                    thisRef.statusTakeover =
                                            new AssuranceFullScreenTakeover(
                                                    applicationHandle.getAppContext(),
                                                    html,
                                                    thisRef);
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

    /** Displays the {@code AssuranceFullScreenTakeover} if it has not already been removed. */
    void show() {
        if (statusTakeover != null) {
            this.statusTakeover.show(applicationHandle.getCurrentActivity());
        }
    }

    /** Removes the {@code AssuranceFullScreenTakeover} if it is being shown. */
    void dismiss() {
        if (this.statusTakeover != null) {
            this.statusTakeover.remove();
        }
    }

    void addUILog(final UILogColorVisibility visibility, final String message) {
        final AssuranceFullScreenTakeover currentTakeover = statusTakeover;

        if (currentTakeover != null && message != null && visibility != null) {
            final String cleanMessage =
                    message.replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "<br>")
                            .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Client Side Logging (%s) - %s",
                    visibility.getValue(),
                    message);
            final String js =
                    String.format(
                            Locale.US, "addLog(%d, \"%s\");", visibility.getValue(), cleanMessage);
            this.statusTakeover.runJavascript(js);
        } else {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Status update failed due to lack of statusTakeover reference");
        }
    }

    @Override
    public boolean onURLTriggered(final String url) {
        final Uri triggeredUrl = Uri.parse(url);

        if ("disconnect".equals(triggeredUrl.getHost())) {
            uiOperationHandler.onDisconnect();
            statusTakeover.remove();
        } else if ("cancel".equals(triggeredUrl.getHost())) {
            statusTakeover.remove();
        } else {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format(
                            "Unknown url coming from status takeover redirect: Url - %s", url));
        }

        return true;
    }

    /** Clearing the logs from the status UI */
    void clearLogs() {
        // Calling clearLog function from the StatusInfo.html
        statusTakeover.runJavascript("clearLog()");
    }

    @Override
    public void onShow(final AssuranceFullScreenTakeover takeover) {}

    @Override
    public void onDismiss(final AssuranceFullScreenTakeover takeover) {}
}
