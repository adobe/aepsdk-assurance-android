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


import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@SuppressWarnings({"unused"})
final class AssuranceWebViewSocket {
    public enum SocketReadyState {
        CONNECTING,
        OPEN,
        CLOSING,
        CLOSED,
        UNKNOWN
    }

    private static final String LOG_TAG = "AssuranceWebViewSocket";
    private static final String WEBSOCKET_HTML_PATH = "file:///android_asset/WebviewSocket.html";

    static final int MAX_DATA_LENGTH = 1024 * 32; // 32kb max packet length
    private final ExecutorService webViewExecutor;
    private final Semaphore initSemaphore;
    private final Semaphore mainThreadJoinSemaphore;
    private final AssuranceWebViewSocketHandler handler;

    private WebView webView;
    private SocketReadyState state;
    private String connectionURL;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    AssuranceWebViewSocket(final AssuranceWebViewSocketHandler handler) {
        this(handler, null);
    }

    @SuppressWarnings({"SetJavascriptEnabled", "AddJavaScriptInterface", "WeakerAccess"})
    AssuranceWebViewSocket(final AssuranceWebViewSocketHandler handler, final WebView webView) {
        this.handler = handler;
        setState(SocketReadyState.UNKNOWN);

        this.webViewExecutor = Executors.newSingleThreadExecutor();
        this.initSemaphore = new Semaphore(0);
        this.mainThreadJoinSemaphore = new Semaphore(1);
    }

    /**
     * Use this method to run javascript to make a connection to the provided webSocket URL.
     *
     * @param url {@link String} A valid socket connection URL
     */
    void connect(final String url) {
        if (!AssuranceUtil.isSafe(url)) {
            Log.warning(
                    Assurance.LOG_TAG, LOG_TAG, "URL is malformed, will not attempt to connect.");
            return;
        }

        setState(SocketReadyState.CONNECTING);
        runJavascript("connect('" + url + "')");
        connectionURL = url;
    }

    /** Use this method to run javascript to close the active webSocket connection. */
    void disconnect() {
        setState(SocketReadyState.CLOSING);
        runJavascript("disconnect()");
        connectionURL = null;
    }

    /** Use this method to run javascript to disconnect the webSocket. */
    void sendData(final byte[] data) {
        final String encodedData = Base64.encodeToString(data, Base64.NO_WRAP | Base64.NO_PADDING);

        if (encodedData.length() > MAX_DATA_LENGTH) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to send data packet, payload was "
                            + encodedData.length()
                            + " bytes, maximum is "
                            + MAX_DATA_LENGTH
                            + ".");
            return;
        }

        runJavascript("sendData('" + encodedData + "')");
    }

    /**
     * Getter for the active socket connection URL.
     *
     * @return A {@link String} representing an active webSocket connection URL
     */
    String getConnectionURL() {
        return this.connectionURL;
    }

    /**
     * Gets the current state of Web socket connection.
     *
     * @return {@link SocketReadyState} representing current state
     */
    SocketReadyState getState() {
        return this.state;
    }

    /**
     * Sets the state of the Web socket connection.
     *
     * @param newState {@link SocketReadyState} representing current state
     */
    private void setState(final SocketReadyState newState) {
        this.state = newState;

        if (this.handler != null) {
            this.handler.onSocketStateChange(this, newState);
        }
    }

    /**
     * Run the provided javascript in the webView. Uses mainThreadJoinSemaphore to execute
     * javascript on socket thread one at a time.
     *
     * @param jsString A {@link String} representing javascript to be run on the web socket
     */
    private void runJavascript(final String jsString) {
        runOnSocketThread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (webView == null) {
                                // this method captures main thread to initialize WebView
                                initializeWebView();
                                // block the socket thread until the webview is full initialized.
                                // initSemaphore is initialized with permit 0
                                initSemaphore.acquire();
                            }

                            // acquire this socket thread until javascript execution is completed in
                            // the mainthread. This ensures the javascript execution of websocket
                            // happens one by one.
                            // mainThreadJoinSemaphore is initialized with permit 1
                            mainThreadJoinSemaphore.acquire();
                        } catch (final InterruptedException ex) {
                            Log.error(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    String.format(
                                            "Socket unable to wait for JS semaphore: %s",
                                            ex.getLocalizedMessage()));
                        }

                        runOnMainThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (webView != null) {
                                            webView.loadUrl("javascript: " + jsString);
                                        } else {
                                            Log.error(
                                                    Assurance.LOG_TAG,
                                                    LOG_TAG,
                                                    "WebView is null, unable to execute JS for"
                                                            + " socket communication.");
                                        }

                                        mainThreadJoinSemaphore.release();
                                    }
                                });
                    }
                });
    }

    /** Initializes the webView that runs socket connection. Thread : MainThread */
    // Takes over the main thread to initialize the webView
    private void initializeWebView() {
        final WeakReference<AssuranceWebViewSocket> weakThisReference = new WeakReference<>(this);

        runOnMainThread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {

                            final AssuranceWebViewSocket currentSocket = weakThisReference.get();

                            if (currentSocket == null) {
                                Log.error(Assurance.LOG_TAG, LOG_TAG, "Current Socket is null");
                                return;
                            }

                            final ClassLoader cl = currentSocket.getClass().getClassLoader();

                            if (cl == null) {
                                Log.error(
                                        Assurance.LOG_TAG,
                                        LOG_TAG,
                                        "Socket unable to get class loader.");
                                return;
                            }

                            currentSocket.webView =
                                    webView == null
                                            ? new WebView(MobileCore.getApplication())
                                            : webView;
                            currentSocket.webView.getSettings().setJavaScriptEnabled(true);
                            currentSocket.webView.setWebViewClient(new WebViewSocketClient());
                            currentSocket.webView.setWebChromeClient(
                                    new WebChromeClient() {
                                        @Override
                                        public boolean onConsoleMessage(
                                                ConsoleMessage consoleMessage) {
                                            if (consoleMessage.messageLevel()
                                                    == ConsoleMessage.MessageLevel.ERROR) {
                                                Log.error(
                                                        Assurance.LOG_TAG,
                                                        LOG_TAG,
                                                        consoleMessage.message());
                                            }

                                            return super.onConsoleMessage(consoleMessage);
                                        }
                                    });
                            currentSocket.webView.addJavascriptInterface(
                                    new WebViewJavascriptInterface(currentSocket), "nativeCode");
                            currentSocket.webView.loadUrl(WEBSOCKET_HTML_PATH);
                        } catch (final Exception ex) {
                            Log.error(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "Unexpected exception while initializing webview: "
                                            + ex.getLocalizedMessage());
                        }
                    }
                });
    }

    /** Helper method to execute {@link Runnable} on socket thread. */
    private void runOnSocketThread(final Runnable r) {
        webViewExecutor.submit(r);
    }

    /** Helper method to execute {@link Runnable} on main thread. */
    private void runOnMainThread(final Runnable r) {
        mainThreadHandler.post(r);
    }

    private final class WebViewJavascriptInterface {
        private WeakReference<AssuranceWebViewSocket> parentSocket;

        WebViewJavascriptInterface(final AssuranceWebViewSocket parentSocket) {
            this.parentSocket = new WeakReference<>(parentSocket);
        }

        @JavascriptInterface
        public void onMessageReceived(final String data) {
            if (handler != null) {
                handler.onSocketDataReceived(parentSocket.get(), data);
            }
        }

        @JavascriptInterface
        public void onSocketOpened() {
            setState(SocketReadyState.OPEN);

            if (handler != null) {
                handler.onSocketConnected(parentSocket.get());
            }
        }

        @JavascriptInterface
        public void onSocketClosed(
                final String reason, final short closeCode, final boolean wasClean) {
            setState(SocketReadyState.CLOSED);

            if (handler != null) {
                handler.onSocketDisconnected(parentSocket.get(), reason, closeCode, wasClean);
            }
        }

        @JavascriptInterface
        public void onSocketError() {
            setState(SocketReadyState.CLOSED);

            if (handler != null) {
                handler.onSocketError(parentSocket.get());
            }
        }

        @JavascriptInterface
        public void log(final String logMsg) {
            Log.trace(Assurance.LOG_TAG, LOG_TAG, "JSLog: " + logMsg);
        }
    }

    private final class WebViewSocketClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView wv, String url) {
            Log.trace(Assurance.LOG_TAG, LOG_TAG, "Socket web content finished loading.");
            initSemaphore.release();
        }

        @Override
        public void onReceivedError(
                WebView view, WebResourceRequest request, WebResourceError error) {
            Log.debug(Assurance.LOG_TAG, LOG_TAG, "Socket encountered page error: %s", error);
        }
    }
}
