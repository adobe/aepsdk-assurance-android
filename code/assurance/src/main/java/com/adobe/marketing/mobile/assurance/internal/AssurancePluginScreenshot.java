/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.View;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants.UILogColorVisibility;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

class AssurancePluginScreenshot implements AssurancePlugin {
    private static final String LOG_TAG = "AssurancePluginScreenshot";
    private static final String PAYLOAD_BLOBID = "blobId";
    private static final String PAYLOAD_MIMETYPE = "mimeType";
    private static final String PAYLOAD_ERROR = "error";
    private AssuranceSession parentSession = null;

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
        manageScreenShot();
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
        /* no-op */
    }

    @Override
    public void onSessionTerminated() {
        parentSession = null;
    }

    private void manageScreenShot() {
        if (parentSession == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to take screenshot, Assurance session instance unavailable.");
            return;
        }

        // Get current activity
        final Activity currentActivity =
                ServiceProvider.getInstance().getAppContextService().getCurrentActivity();

        if (currentActivity == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to take screenshot, current activity is null.");
            return;
        }

        // Check if device supports PixelCopy API (Android 8.0 and above)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Screenshot not supported on Android versions below 8.0 (API 26)");
            if (parentSession != null) {
                parentSession.logLocalUI(UILogColorVisibility.LOW, "Screenshot not supported on Android versions below 8.0 (API 26)");
                sendErrorEvent("Screenshot not supported on Android versions below 8.0 (API 26)");
            }
            return;
        }

        try {
            captureScreenshotWithPixelCopy(currentActivity);
        } catch (Exception e) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Error while taking screenshot: " + e.getMessage());
            sendErrorEvent("Screenshot capture failed: " + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void captureScreenshotWithPixelCopy(final Activity currentActivity) {
        final View view = currentActivity.getWindow().getDecorView().getRootView();
        final Bitmap bitmap = Bitmap.createBitmap(
                view.getWidth(),
                view.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        final int[] locationOnScreen = new int[2];
        view.getLocationOnScreen(locationOnScreen);
        
        final Rect sourceRect = new Rect(
                locationOnScreen[0],
                locationOnScreen[1],
                locationOnScreen[0] + view.getWidth(),
                locationOnScreen[1] + view.getHeight()
        );

        final Handler handler = new Handler(Looper.getMainLooper());

        PixelCopy.request(
                currentActivity.getWindow(),
                sourceRect,
                bitmap,
                new PixelCopy.OnPixelCopyFinishedListener() {
                    @Override
                    public void onPixelCopyFinished(int result) {
                        if (result == PixelCopy.SUCCESS) {
                            // Compress to PNG
                            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            sendScreenshot(baos);
                        } else {
                            Log.error(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "PixelCopy failed with result: " + result);
                            sendErrorEvent("PixelCopy failed with result: " + result);
                        }
                    }
                },
                handler
        );
    }

    private void sendErrorEvent(final String errorMessage) {
        final Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put(PAYLOAD_BLOBID, "");
        responsePayload.put(PAYLOAD_ERROR, errorMessage);
        final AssuranceEvent screenshotFailEvent =
                new AssuranceEvent(
                        AssuranceConstants.AssuranceEventType.BLOB,
                        responsePayload);
        
        if (parentSession != null) {
            parentSession.logLocalUI(UILogColorVisibility.LOW, "Screenshot capture failed");
            parentSession.queueOutboundEvent(screenshotFailEvent);
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
                "image/png",
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
}