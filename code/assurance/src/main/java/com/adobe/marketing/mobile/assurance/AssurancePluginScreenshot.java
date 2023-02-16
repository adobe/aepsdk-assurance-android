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
import android.graphics.Bitmap;
import android.view.View;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.assurance.AssuranceConstants.UILogColorVisibility;
import com.adobe.marketing.mobile.services.Log;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

class AssurancePluginScreenshot implements AssurancePlugin {
    private static final String LOG_TAG = "AssurancePluginScreenshot";
    private static final String PAYLOAD_BLOBID = "blobId";
    private static final String PAYLOAD_MIMETYPE = "mimeType";
    private static final String PAYLOAD_ERROR = "error";
    private AssuranceSession parentSession = null;

    private CaptureScreenShotListener listener;

    @Override
    public String getVendor() {
        return AssuranceConstants.VENDOR_ASSURANCE_MOBILE;
    }

    @Override
    public String getControlType() {
        return AssuranceConstants.ControlType.SCREENSHOT;
    }

    /** This method will be invoked only if the control event is of type "screenshot" */
    @Override
    public void onEventReceived(final AssuranceEvent event) {
        // we don't need to verify any controlDetails for this event
        listener =
                new CaptureScreenShotListener() {
                    @Override
                    public void onCaptureScreenshot(Bitmap bitmap) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int quality = 100;
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                        sendScreenshot(baos);
                    }
                };

        getCurrentScreenShot(listener);
    }

    @Override
    public void onRegistered(final AssuranceSession parentSession) {
        this.parentSession = parentSession;
    }

    @Override
    public void onSessionConnected() {
        /* no-op */
    }

    @Override
    public void onSessionDisconnected(final int code) {
        listener = null;
    }

    @Override
    public void onSessionTerminated() {
        parentSession = null;
    }

    private void getCurrentScreenShot(final CaptureScreenShotListener captureScreenShotListener) {
        if (parentSession == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to take screenshot, Assurance session instance unavailable.");
            return;
        }

        // create bitmap screen capture
        final Activity currentActivity = parentSession.getCurrentActivity();

        if (currentActivity != null) {
            currentActivity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            View currentWindow =
                                    currentActivity.getWindow().getDecorView().getRootView();
                            currentWindow.setDrawingCacheEnabled(true);
                            Bitmap bitmap = Bitmap.createBitmap(currentWindow.getDrawingCache());
                            currentWindow.setDrawingCacheEnabled(false);

                            if (captureScreenShotListener != null) {
                                captureScreenShotListener.onCaptureScreenshot(bitmap);
                            }
                        }
                    });
        }
    }

    private void sendScreenshot(final ByteArrayOutputStream baos) {
        if (parentSession == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to send screenshot, Assurance session instance unavailable");
            return;
        }

        AssuranceBlob.upload(
                baos.toByteArray(),
                "image/jpeg",
                parentSession,
                new AssuranceBlob.BlobUploadCallback() {
                    @Override
                    public void onSuccess(final String blobID) {
                        final Map<String, Object> responsePayload = new HashMap<>();
                        responsePayload.put(PAYLOAD_BLOBID, blobID);
                        responsePayload.put(PAYLOAD_MIMETYPE, "image/png");
                        final AssuranceEvent screenshotEvent =
                                new AssuranceEvent(
                                        AssuranceConstants.AssuranceEventType.BLOB,
                                        responsePayload);

                        if (parentSession != null) {
                            parentSession.logLocalUI(UILogColorVisibility.LOW, "Screenshot taken");
                            parentSession.queueOutboundEvent(screenshotEvent);
                        } else {
                            Log.warning(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "Assurance Session instance is null for"
                                        + " AssurancePluginScreenshot, Cannot send the screenshot"
                                        + " event.");
                        }
                    }

                    @Override
                    public void onFailure(final String reason) {
                        final Map<String, Object> responsePayload = new HashMap<>();
                        responsePayload.put(PAYLOAD_BLOBID, "");
                        responsePayload.put(PAYLOAD_ERROR, reason);
                        final AssuranceEvent screenshotFailEvent =
                                new AssuranceEvent(
                                        AssuranceConstants.AssuranceEventType.BLOB,
                                        responsePayload);
                        String error =
                                String.format(
                                        "Error while taking screenshot - Description: %s", reason);
                        Log.error(Assurance.LOG_TAG, LOG_TAG, error);

                        if (parentSession != null) {
                            parentSession.logLocalUI(UILogColorVisibility.LOW, error);
                            parentSession.queueOutboundEvent(screenshotFailEvent);
                        }
                    }
                });
    }

    /**
     * Returns the session associated with this plugin.
     *
     * @return the {@code AssuranceSession} associated with this plugin.
     */
    @VisibleForTesting
    AssuranceSession getParentSession() {
        return parentSession;
    }

    /**
     * Returns the screenshot listener associated with this plugin.
     *
     * @return the {@code CaptureScreenShotListener} for this plugin.
     */
    @VisibleForTesting
    CaptureScreenShotListener getCaptureScreenShotListener() {
        return listener;
    }

    interface CaptureScreenShotListener {
        void onCaptureScreenshot(Bitmap bitmap);
    }
}
