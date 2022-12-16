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


import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
class AssurancePluginLogForwarder implements AssurancePlugin {
    private static final String LOG_TAG = "AssurancePluginLogForwarder";
    private static final Pattern HEADER_MESSAGE =
            Pattern.compile(
                    "^\\[ \\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d.\\d\\d\\d {1,}\\d+: {0,}\\d+"
                            + " [VDIWEAF]/[^ ]+ {1,}]$");

    private volatile boolean backgroundThreadRunning = false;
    private boolean logEnabled = false;
    private final AtomicReference<AssuranceSession> parentSession;

    AssurancePluginLogForwarder() {
        parentSession = new AtomicReference<>(null);
    }

    boolean isBackgroundThreadRunning() {
        return backgroundThreadRunning;
    }

    @Override
    public String getVendor() {
        return AssuranceConstants.VENDOR_ASSURANCE_MOBILE;
    }

    @Override
    public String getControlType() {
        return AssuranceConstants.ControlType.LOG_FORWARDING;
    }

    /** This method will be invoked only if the control event is of type "logForwarding" */
    @Override
    public void onEventReceived(final AssuranceEvent event) {
        final HashMap<String, Object> logForwardingDetails = event.getControlDetail();

        if (AssuranceUtil.isNullOrEmpty(logForwardingDetails)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Invalid details in payload. Ignoring to enable/disable logs.");
            return;
        }

        final Object enabled = logForwardingDetails.get("enable");

        if (!(enabled instanceof Boolean)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to forward the log, logForwardingValue is invalid");
            return;
        }

        logEnabled = (Boolean) enabled;
        final AssuranceSession session = parentSession.get();

        if (logEnabled) {
            if (session != null) {
                session.logLocalUI(
                        AssuranceConstants.UILogColorVisibility.HIGH,
                        "Received Assurance command to start forwarding logs");
            }

            if (!backgroundThreadRunning) {
                backgroundThreadRunning = true;
                Thread logForwardThread = new Thread(new LogForwardThread());
                logForwardThread.start();
            }
        } else {
            if (session != null) {
                session.logLocalUI(
                        AssuranceConstants.UILogColorVisibility.HIGH,
                        "Received Assurance command to stop forwarding logs");
            }
        }
    }

    @Override
    public void onRegistered(final AssuranceSession parentSession) {
        // does nothing
        this.parentSession.set(parentSession);
    }

    @Override
    public void onSessionConnected() {
        /* no-op */
    }

    @Override
    public void onSessionDisconnected(final int code) {
        logEnabled = false;
    }

    @Override
    public void onSessionTerminated() {
        parentSession.set(null);
    }

    private final class LogForwardThread implements Runnable {
        @Override
        public void run() {
            try {
                final Process procRemoveUnecessaryLogs =
                        new ProcessBuilder().command("logcat", "-P", "").start();
                final String processIdCommand =
                        String.format("--pid=%s", android.os.Process.myPid());
                final Process proc =
                        new ProcessBuilder()
                                .command("logcat", processIdCommand, "-bmain", "-vlong")
                                .start();
                final BufferedReader reader =
                        new BufferedReader(new InputStreamReader(proc.getInputStream()));
                final StringBuilder logLines = new StringBuilder();
                boolean isFirstLog = true;

                while (logEnabled && !Thread.interrupted()) {
                    try {
                        final String logLine = reader.readLine();

                        // hack to avoid infinite looping on log forwarding.
                        if (logLine != null && logLine.contains(Assurance.LOG_TAG)) {
                            continue;
                        }

                        if (logLine != null && checkIfHeader(logLine)) {
                            // When the first log is the header log
                            if (isFirstLog) {
                                isFirstLog = false;
                                continue;
                            }

                            if (checkIfLogLinesAreEmpty(logLines)) {
                                continue;
                            }

                            final Map<String, Object> eventPayload = new HashMap<>();
                            eventPayload.put("logline", logLines.toString());
                            final AssuranceEvent logEvent =
                                    new AssuranceEvent(
                                            AssuranceConstants.AssuranceEventType.LOG,
                                            eventPayload);
                            final AssuranceSession session = parentSession.get();

                            if (session != null) {
                                session.queueOutboundEvent(logEvent);
                            }

                            logLines.setLength(0);
                        }

                        if (logLine != null && !logLine.isEmpty()) {
                            logLines.append(logLine).append("\n");
                        }
                    } catch (final Exception ex) {
                        Log.error(
                                Assurance.LOG_TAG,
                                LOG_TAG,
                                String.format(
                                        "Log forwarding error reading line: %s",
                                        ex.getLocalizedMessage()));
                    }
                }

                procRemoveUnecessaryLogs.destroy();
                proc.destroy();
            } catch (final Exception ex) {
                // handle exception
                Log.error(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        String.format(
                                "Log forwarding error while sending logs: %s"
                                        + ex.getLocalizedMessage()));
            }

            backgroundThreadRunning = false;
        }
    }

    private boolean checkIfHeader(String message) {
        final Matcher matcher = HEADER_MESSAGE.matcher(message);
        return matcher.matches();
    }

    private boolean checkIfLogLinesAreEmpty(StringBuilder sb) {
        String[] a = sb.toString().split("\n");

        if (a.length < 2) {
            return true;
        }

        return a[1].equals("");
    }
}
