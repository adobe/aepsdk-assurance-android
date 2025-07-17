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
import android.graphics.Canvas;
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
    private static final int MAX_SCREENSHOT_SIZE =
            4096; // Prevent OutOfMemoryError on very large screens

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

        if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT <= 25) {
            // Android 5.0-7.1 (API 21-25): Use Canvas fallback with hardware bitmap detection
            try {
                // Success for software bitmap
                captureScreenshotWithCanvas(currentActivity);
            } catch (Exception e) {
                // Fail here for hardware bitmap or other issues
                Log.error(
                        Assurance.LOG_TAG, LOG_TAG, "Canvas screenshot failed: " + e.getMessage());
                sendErrorEvent("Canvas screenshot failed: " + e.getMessage());
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ (API 26+): Use PixelCopy for full hardware + software support
            captureScreenshotWithPixelCopy(currentActivity);
        } else {
            // Below API 21: Not supported
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Screenshot not supported on Android versions below 5.0 (API 21)");
            if (parentSession != null) {
                parentSession.logLocalUI(
                        UILogColorVisibility.LOW,
                        "Screenshot not supported on Android versions below 5.0 (API 21)");
                sendErrorEvent("Screenshot not supported on Android versions below 5.0 (API 21)");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void captureScreenshotWithPixelCopy(final Activity currentActivity) {
        currentActivity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final View view =
                                    currentActivity.getWindow().getDecorView().getRootView();

                            int width = view.getWidth();
                            int height = view.getHeight();

                            // Apply size limits like Canvas method for consistency
                            if (width > MAX_SCREENSHOT_SIZE || height > MAX_SCREENSHOT_SIZE) {
                                float scale =
                                        Math.min(
                                                (float) MAX_SCREENSHOT_SIZE / width,
                                                (float) MAX_SCREENSHOT_SIZE / height);
                                width = (int) (width * scale);
                                height = (int) (height * scale);
                                Log.debug(
                                        Assurance.LOG_TAG,
                                        LOG_TAG,
                                        "Scaling PixelCopy screenshot to: " + width + "x" + height);
                            }

                            final Bitmap bitmap =
                                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                            final int[] locationOnScreen = new int[2];
                            view.getLocationOnScreen(locationOnScreen);

                            final Rect sourceRect =
                                    new Rect(
                                            locationOnScreen[0],
                                            locationOnScreen[1],
                                            locationOnScreen[0] + view.getWidth(),
                                            locationOnScreen[1] + view.getHeight());

                            PixelCopy.request(
                                    currentActivity.getWindow(),
                                    sourceRect,
                                    bitmap,
                                    new PixelCopy.OnPixelCopyFinishedListener() {
                                        @Override
                                        public void onPixelCopyFinished(int result) {
                                            if (result == PixelCopy.SUCCESS) {
                                                // Compress and upload
                                                final ByteArrayOutputStream baos =
                                                        new ByteArrayOutputStream();
                                                bitmap.compress(
                                                        Bitmap.CompressFormat.PNG, 100, baos);
                                                sendScreenshot(baos);
                                            } else {
                                                Log.error(
                                                        Assurance.LOG_TAG,
                                                        LOG_TAG,
                                                        "PixelCopy failed with result: " + result);
                                                sendErrorEvent(
                                                        "PixelCopy failed with result: " + result);
                                            }
                                        }
                                    },
                                    new Handler(Looper.getMainLooper()));
                        } catch (Exception e) {
                            Log.error(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "PixelCopy setup failed: " + e.getMessage());
                            sendErrorEvent("PixelCopy setup failed: " + e.getMessage());
                        }
                    }
                });
    }

    /** Canvas-based screenshot capture for API 21-25 with hardware bitmap detection */
    private void captureScreenshotWithCanvas(final Activity currentActivity) {
        currentActivity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = null;
                        try {
                            View view = currentActivity.getWindow().getDecorView().getRootView();

                            int width = view.getWidth();
                            int height = view.getHeight();

                            // Limit bitmap size to prevent OutOfMemoryError
                            float scale = 1.0f;
                            if (width > MAX_SCREENSHOT_SIZE || height > MAX_SCREENSHOT_SIZE) {
                                scale =
                                        Math.min(
                                                (float) MAX_SCREENSHOT_SIZE / width,
                                                (float) MAX_SCREENSHOT_SIZE / height);
                                width = (int) (width * scale);
                                height = (int) (height * scale);
                                Log.debug(
                                        Assurance.LOG_TAG,
                                        LOG_TAG,
                                        "Scaling Canvas screenshot to: " + width + "x" + height);
                            }

                            // Create bitmap with scaled dimensions
                            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                            // Create canvas backed by the bitmap
                            Canvas canvas = new Canvas(bitmap);

                            // Scale the canvas if needed
                            if (scale < 1.0f) {
                                canvas.scale(scale, scale);
                            }

                            // Draw the view onto the canvas
                            view.draw(canvas);

                            // Success - compress and upload
                            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            sendScreenshot(baos);

                            Log.debug(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "Canvas screenshot successful for API "
                                            + Build.VERSION.SDK_INT);

                        } catch (IllegalArgumentException e) {
                            // This can happen with hardware-accelerated content that can't be drawn
                            // to Canvas
                            Log.error(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "Canvas screenshot failed - likely due to hardware-accelerated"
                                            + " content: "
                                            + e.getMessage());
                            sendErrorEvent(
                                    "Screenshot failed: hardware-accelerated content detected."
                                        + " Upgrade to Android 8.0+ for full screenshot support.");
                        } catch (OutOfMemoryError e) {
                            Log.error(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "Canvas screenshot failed - out of memory: " + e.getMessage());
                            sendErrorEvent("Screenshot failed: insufficient memory");
                        } catch (Exception e) {
                            Log.error(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "Canvas screenshot failed: " + e.getMessage());
                            sendErrorEvent("Screenshot failed: " + e.getMessage());
                        }
                    }
                });
    }

    private void sendErrorEvent(final String errorMessage) {
        final Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put(PAYLOAD_BLOBID, "");
        responsePayload.put(PAYLOAD_ERROR, errorMessage);
        final AssuranceEvent screenshotFailEvent =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.BLOB, responsePayload);

        if (parentSession != null) {
            parentSession.logLocalUI(UILogColorVisibility.LOW, errorMessage);
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
