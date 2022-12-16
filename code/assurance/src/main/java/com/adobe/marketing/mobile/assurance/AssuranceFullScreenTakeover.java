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
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.RequiresApi;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import java.lang.ref.WeakReference;

class AssuranceFullScreenTakeover {
    private static final String LOG_TAG = "AssuranceFullScreenTakeover";
    private static final String BASE_URL = "file:///android_asset/";
    private static final String MIME_TYPE = "text/html";

    private final MessageFullScreenWebViewClient webViewClient;
    private final FullScreenTakeoverCallbacks callbacks;

    private int orientationWhenShown;
    private WebView webView;
    private boolean isVisible;

    WeakReference<AssuranceFullScreenTakeoverActivity> messageFullScreenActivity;
    ViewGroup rootViewGroup;

    @SuppressWarnings("SetJavascriptEnabled")
    AssuranceFullScreenTakeover(
            final Context appContext,
            final String html,
            final FullScreenTakeoverCallbacks callbacks) {
        this.callbacks = callbacks;
        this.webViewClient = new MessageFullScreenWebViewClient();

        // Initialize webview on main thread.
        new Handler(Looper.getMainLooper())
                .post(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    webView = new WebView(appContext);
                                    webView.getSettings().setJavaScriptEnabled(true);
                                    webView.setVerticalScrollBarEnabled(false);
                                    webView.setHorizontalScrollBarEnabled(false);
                                    webView.setBackgroundColor(Color.TRANSPARENT);
                                    webView.setWebViewClient(webViewClient);
                                    webView.getSettings().setDefaultTextEncodingName("UTF-8");
                                    webView.loadDataWithBaseURL(
                                            BASE_URL, html, MIME_TYPE, "UTF-8", null);
                                } catch (final Exception ex) {
                                    Log.error(
                                            Assurance.LOG_TAG,
                                            LOG_TAG,
                                            String.format(
                                                    "Unable to create webview: %s",
                                                    ex.getLocalizedMessage()));
                                }
                            }
                        });
    }

    void show(final Activity currentActivity) {
        if (currentActivity == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Failed to show fullscreen takeover, current activity is null.");
            return;
        }

        try {
            final Intent fullscreen =
                    new Intent(
                            currentActivity.getApplicationContext(),
                            AssuranceFullScreenTakeoverActivity.class);
            fullscreen.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            fullscreen.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            AssuranceFullScreenTakeoverActivity.setFullscreenMessage(this);
            currentActivity.startActivity(fullscreen);
            currentActivity.overridePendingTransition(0, 0);
        } catch (ActivityNotFoundException ex) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Failed to show fullscreen takeover, could not start activity. Error %s",
                    ex.getLocalizedMessage());
        }
    }

    /** Dismisses the message. */
    void remove() {
        new Handler(Looper.getMainLooper())
                .post(
                        new Runnable() {
                            @Override
                            public void run() {
                                Log.trace(
                                        Assurance.LOG_TAG,
                                        LOG_TAG,
                                        "Dismissing the fullscreen takeover");
                                removeFromRootViewGroup();
                                AssuranceFullScreenTakeoverActivity.setFullscreenMessage(null);
                            }
                        });
        callbacks.onDismiss(this);
        isVisible = false;
    }

    /**
     * Runs a js fragment within the WebView context.
     *
     * @param jsFragment a non-terminated (no semicolon) javascript statement to run in the WebView
     *     context.
     */
    void runJavascript(final String jsFragment) {
        new Handler(Looper.getMainLooper())
                .post(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (webView != null) {
                                    Log.trace(
                                            Assurance.LOG_TAG,
                                            LOG_TAG,
                                            "FullScreenTakeOver runJavascript invoked with: %s",
                                            jsFragment);
                                    webView.loadUrl("javascript: " + jsFragment);
                                }
                            }
                        });
    }

    /**
     * Creates and adds the {@link #webView} to the root view group of {@link
     * #messageFullScreenActivity}.
     */
    void showInRootViewGroup() {
        if (rootViewGroup == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Failed to show fullscreen takeover in rootViewGroup because rootViewGroup is"
                            + " null.");
            return;
        }

        final int currentOrientation = rootViewGroup.getResources().getConfiguration().orientation;

        if (isVisible && orientationWhenShown == currentOrientation) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Failed to show fullscreen takeover in rootViewGroup because it is already"
                            + " visible.");
            return;
        }

        orientationWhenShown = currentOrientation;
        // run on main thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new MessageFullScreenRunner(this));
    }

    /** Removes the {@link #webView} from the activity. */
    private void removeFromRootViewGroup() {
        if (rootViewGroup == null) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Failed to dismiss fullscreen takeover, could not find root view group.");
            return;
        }

        if (messageFullScreenActivity != null) {
            final AssuranceFullScreenTakeoverActivity activity = messageFullScreenActivity.get();

            if (activity != null) {
                activity.finish();
            }

            messageFullScreenActivity = null;
        }

        rootViewGroup.removeView(webView);
    }

    /** Gets called after the message is successfully shown. */
    private void viewed() {
        isVisible = true;

        if (callbacks != null) {
            callbacks.onShow(this);
        }
    }

    private class MessageFullScreenRunner implements Runnable {
        private final AssuranceFullScreenTakeover message;

        MessageFullScreenRunner(AssuranceFullScreenTakeover message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {
                if (message.rootViewGroup == null) {
                    Log.error(
                            Assurance.LOG_TAG,
                            LOG_TAG,
                            "Failed to show fullscreen takeover, could not find root view group.");
                    message.remove();
                    return;
                }

                int width = message.rootViewGroup.getMeasuredWidth();
                int height = message.rootViewGroup.getMeasuredHeight();

                // problem now with trying to show the message when our rootview hasn't been
                // measured yet
                if (width == 0 || height == 0) {
                    Log.error(
                            Assurance.LOG_TAG,
                            LOG_TAG,
                            "Failed to show fullscreen takeover, could not measure root view"
                                    + " group.");
                    message.remove();
                    return;
                }

                message.rootViewGroup.addView(message.webView, width, height);
            } catch (final Exception ex) {
                Log.trace(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Failed to show fullscreen takeover due to exception: "
                                + ex.getLocalizedMessage());
                message.remove();
            }
        }
    }

    /**
     * Implements {@link WebViewClient} to intercept the url href being clicked and determine the
     * action based on the url.
     */
    private class MessageFullScreenWebViewClient extends WebViewClient {
        @RequiresApi(14)
        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
            return handleUrl(url);
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(
                final WebView view, final WebResourceRequest request) {
            final Uri uri = request.getUrl();
            return handleUrl(uri.toString());
        }

        private boolean handleUrl(final String url) {
            if (callbacks != null) {
                return callbacks.onURLTriggered(url);
            }

            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // call viewed when page has finished loading... This may fire prior to the presentation
            // animation showing,
            // but should avoid a race condition in running JS on page load.
            viewed();
        }
    }

    public interface FullScreenTakeoverCallbacks {
        boolean onURLTriggered(final String url);

        void onShow(final AssuranceFullScreenTakeover takeover);

        void onDismiss(final AssuranceFullScreenTakeover takeover);
    }
}
