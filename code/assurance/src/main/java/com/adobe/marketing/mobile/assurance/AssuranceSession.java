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
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;

/**
 * Represents an Assurance session. Responsible for managing the socket connection to the session,
 * event queue workers, plugins and the presentation elements associated with the session. The
 * lifecycle of this class is equivalent to the lifecycle of the socket connection maintained by it.
 * An {@code AssuranceSession} is "valid" if an instance exists (either waiting for a connection to
 * be established, or one which is already connected).
 */
@SuppressWarnings("unused")
class AssuranceSession implements AssuranceWebViewSocketHandler {
    private static final String LOG_TAG = "AssuranceSession";
    private static final String CONNECTION_URL_FORMAT =
            "wss://connect%s.griffon.adobe.com/client/v1"
                    + "?sessionId=%s&token=%s&orgId=%s&clientId=%s";
    private static final long SOCKET_RECONNECT_TIME_DELAY = TimeUnit.SECONDS.toMillis(5);

    private final AssuranceStateManager assuranceStateManager;
    private final AssuranceConstants.AssuranceEnvironment assuranceEnvironment;
    private final String sessionId;
    private final OutboundEventQueueWorker outboundEventQueueWorker;
    private final InboundEventQueueWorker inboundEventQueueWorker;
    private final AssuranceWebViewSocket socket;
    private final HandlerThread socketReconnectThread =
            new HandlerThread("com.adobe.assurance.mobile.socketreconnectworker");
    private final Handler socketReconnectHandler;
    private final AssurancePluginManager pluginManager;
    private final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle;
    private final AssuranceSessionPresentationManager assuranceSessionPresentationManager;
    private final Set<AssuranceSessionStatusListener> sessionStatusListeners;
    private final AssuranceConnectionDataStore connectionDataStore;

    private final InboundEventQueueWorker.InboundQueueEventListener inboundQueueEventListener =
            new InboundEventQueueWorker.InboundQueueEventListener() {
                @Override
                public void onInboundEvent(final AssuranceEvent event) {
                    // Handle "startEventForwarding" event separately. It is not necessary to notify
                    // the plugins
                    // about this event.
                    if (AssuranceConstants.ControlType.START_EVENT_FORWARDING.equals(
                            event.getControlType())) {
                        onStartForwardingEvent();
                        return;
                    }

                    pluginManager.onAssuranceEvent(event);
                }
            };

    private boolean isAttemptingToReconnect = false;
    private boolean didClearBootEvents = false;

    /**
     * A notification mechanism for components that have the need to be aware of creation and
     * destruction of an {@code AssuranceSession}.
     */
    interface AssuranceSessionStatusListener {
        /** Callback indicating that the AssuranceSession is connected. */
        void onSessionConnected();

        /** Callback indicating that the AssuranceSession is disconnected. */
        void onSessionTerminated();
    }

    AssuranceSession(
            final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle,
            final AssuranceStateManager assuranceStateManager,
            final String sessionId,
            final AssuranceConstants.AssuranceEnvironment assuranceEnvironment,
            final AssuranceConnectionDataStore connectionDataStore,
            final AssuranceSessionOrchestrator.SessionUIOperationHandler uiOperationHandler,
            final List<AssurancePlugin> plugins,
            final List<AssuranceEvent> bufferedEvents) {

        this.assuranceStateManager = assuranceStateManager;
        this.applicationHandle = applicationHandle;
        this.assuranceEnvironment = assuranceEnvironment;
        this.sessionId = sessionId;
        this.sessionStatusListeners = new HashSet<>();
        this.connectionDataStore = connectionDataStore;

        assuranceSessionPresentationManager =
                new AssuranceSessionPresentationManager(
                        assuranceStateManager, uiOperationHandler, applicationHandle);

        pluginManager = new AssurancePluginManager(this);

        socketReconnectThread.start();
        Looper socketLooper = socketReconnectThread.getLooper();
        socketReconnectHandler = new Handler(socketLooper);

        socket = new AssuranceWebViewSocket(this);

        // Initialize EventQueue workers.
        outboundEventQueueWorker =
                new OutboundEventQueueWorker(
                        Executors.newSingleThreadExecutor(), socket, new AssuranceClientInfo());
        inboundEventQueueWorker =
                new InboundEventQueueWorker(
                        Executors.newSingleThreadExecutor(), inboundQueueEventListener);

        // Enqueue stored events.
        if (bufferedEvents != null) {
            final List<AssuranceEvent> buffer = new ArrayList<>(bufferedEvents);

            for (final AssuranceEvent event : buffer) {
                queueOutboundEvent(event);
            }
        } else {
            didClearBootEvents = true;
        }

        // Add plugins.
        if (plugins != null) {
            for (final AssurancePlugin plugin : plugins) {
                addPlugin(plugin);
            }
        }
    }

    /**
     * Makes a socket connection with the provided pin. If the pin is empty, it triggers the
     * presentation to launch a screen to retrieve the pin.
     *
     * @param pin token required for making a socket connection.
     */
    void connect(final String pin) {
        // Verify if a pin is already available.
        if (StringUtils.isNullOrEmpty(pin)) {
            // If PIN in absent, show the PIN prompt.
            Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "No stored code. Launching PIN screen by initializing session.");
            initializeSession();
            return;
        }

        // If PIN is available, we want to connect directly without PIN prompt.
        Log.debug(Assurance.LOG_TAG, LOG_TAG, "Found stored. Connecting session directly");
        assuranceSessionPresentationManager.onSessionConnecting();

        final String envString = AssuranceUtil.getURLFormatForEnvironment(assuranceEnvironment);
        String orgId = assuranceStateManager.getOrgId(true);

        if (StringUtils.isNullOrEmpty(orgId)) {
            // if configuration does not have org-id, try extracting it from stored connection url
            final String reconnectionURL = connectionDataStore.getStoredConnectionURL();
            if (reconnectionURL == null) {
                Log.debug(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Cannot connect. No orgId from Configuration state or stored url.");
                return;
            }

            final Uri uri = Uri.parse(reconnectionURL);
            orgId = uri.getQueryParameter(AssuranceConstants.SocketURLKeys.ORG_ID);
            Log.debug(Assurance.LOG_TAG, LOG_TAG, "Using orgId from stored reconnection url.");
        }

        final String connectionString =
                String.format(
                        CONNECTION_URL_FORMAT,
                        envString,
                        sessionId,
                        pin,
                        orgId,
                        assuranceStateManager.getClientId());
        Log.debug(
                Assurance.LOG_TAG, LOG_TAG, "Connecting to session with URL: " + connectionString);
        socket.connect(connectionString);
    }

    /** Disconnects the socket connection and releases all the resources held. */
    void disconnect() {
        if (socket != null && socket.getState() != AssuranceWebViewSocket.SocketReadyState.CLOSED) {
            socket.disconnect();
        }

        clearSessionData();

        pluginManager.onSessionTerminated();
    }

    /**
     * This method queues the passed event in {@link OutboundEventQueueWorker}.
     *
     * @param event A {@link AssuranceEvent} to be queued and sent to Assurance
     */
    void queueOutboundEvent(final AssuranceEvent event) {
        if (event == null) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Assurance cannot send event, event cannot be null.");
            return;
        }

        if (!outboundEventQueueWorker.offer(event)) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Assurance cannot send event, problem queuing event in outBoundEventQueue");
        }
    }

    void addPlugin(final AssurancePlugin plugin) {
        pluginManager.addPlugin(plugin);
    }

    /**
     * Logs a message on the Assurance Session Status UI view managed by this session.
     *
     * @param visibility the level with with the {@param message} needs to be logged
     * @param message the message that needs to be logged
     */
    void logLocalUI(
            final AssuranceConstants.UILogColorVisibility visibility, final String message) {
        assuranceSessionPresentationManager.logLocalUI(visibility, message);
    }

    Activity getCurrentActivity() {
        return applicationHandle.getCurrentActivity();
    }

    /**
     * Getter for the assuranceEnvironment variable.
     *
     * <p>Defines the assuranceEnvironment in which the socket connection is made to. This value is
     * available for all the SDK plugins. A valid value during a active AssuranceSession. Its value
     * will be null when the session is terminated.
     *
     * @return A {@link String} value representing the assuranceEnvironment of the active Assurance
     *     session
     */
    AssuranceConstants.AssuranceEnvironment getAssuranceEnvironment() {
        return assuranceEnvironment;
    }

    /**
     * Retrieves the session identifier for this {@code AssuranceSession}.
     *
     * @return the session identifier for this {@code AssuranceSession}
     */
    String getSessionId() {
        return sessionId;
    }

    @Override
    public void onSocketConnected(final AssuranceWebViewSocket socket) {
        Log.debug(Assurance.LOG_TAG, LOG_TAG, "Websocket connected.");

        // reset flags
        isAttemptingToReconnect = false;

        // save the connection url
        connectionDataStore.saveConnectionURL(socket.getConnectionURL());

        // On successful socket connection, send clientInfoEvent
        // and then wait to receive "startForwarding" control event to kickoff sending queued events

        // Until the startForwarding event is received
        // 1. Do not remove the WebView UI nor show the floating button
        // 2. Do not notify plugins on successful connect
        // 3. Do not share Assurance shared state
        inboundEventQueueWorker.start();

        // If the outbound worker was already started, re-send the clientInfoEvent to trigger
        // unblocking of event queue processing.
        final boolean outboundWorkerAlreadyStarted = !outboundEventQueueWorker.start();

        if (outboundWorkerAlreadyStarted) {
            outboundEventQueueWorker.sendClientInfoEvent();
        }
    }

    @Override
    public void onSocketDataReceived(final AssuranceWebViewSocket socket, final String message) {
        try {
            AssuranceEvent event = new AssuranceEvent(message);

            // only nonnull events should be queued to the inbound worker thread
            if (event == null) {
                Log.warning(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        String.format(
                                "Unidentified event obtained from Assurance server. Ignoring the"
                                        + " event with json -%s",
                                message));
                return;
            }

            if (!inboundEventQueueWorker.offer(event)) {
                Log.warning(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Cannnot process the inbound Assurance event from server, problem queuing"
                                + " event in inboundEventsQueue");
            }
        } catch (final UnsupportedCharsetException ex) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format(
                            "Unable to marshal inbound event due to encoding. Error - %s",
                            ex.getLocalizedMessage()));
        } catch (final JSONException ex) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format(
                            "Unable to marshal inbound event due to json format. Error - %s",
                            ex.getLocalizedMessage()));
        }
    }

    @Override
    public void onSocketDisconnected(
            final AssuranceWebViewSocket socket,
            final String errorReason,
            final int closeCode,
            final boolean wasClean) {
        switch (closeCode) {
            case AssuranceConstants.SocketCloseCode.NORMAL:
                clearSessionData();
                assuranceSessionPresentationManager.onSessionDisconnected(closeCode);
                pluginManager.onSessionTerminated();
                notifyTerminationAndRemoveStatusListeners();
                break;

            case AssuranceConstants.SocketCloseCode.ORG_MISMATCH:
            case AssuranceConstants.SocketCloseCode.CLIENT_ERROR:
            case AssuranceConstants.SocketCloseCode.CONNECTION_LIMIT:
            case AssuranceConstants.SocketCloseCode.EVENT_LIMIT:
            case AssuranceConstants.SocketCloseCode.SESSION_DELETED:
                clearSessionData();
                assuranceSessionPresentationManager.onSessionDisconnected(closeCode);
                // We can notify plugins for SessionTermination now, since we don't give retry
                // option and UI will be dismissed anyhow
                pluginManager.onSessionDisconnected(closeCode);
                pluginManager.onSessionTerminated();
                notifyTerminationAndRemoveStatusListeners();
                break;

            default:
                Log.warning(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        String.format(
                                "Abnornmal closure of websocket. Reason - %s and closeCode - %s",
                                errorReason, closeCode));
                outboundEventQueueWorker.block();
                assuranceSessionPresentationManager.onSessionDisconnected(closeCode);
                long delayBeforeReconnect =
                        isAttemptingToReconnect ? SOCKET_RECONNECT_TIME_DELAY : 0L;

                // If the disconnect happens because of abnormal close code. And if we are
                // attempting to reconnect for the first time then,
                // 1. Make an appropriate UI log.
                // 2. Change the button graphics to gray out.
                // 3. Notify plugins on disconnect with abnormal close code.
                // 4. Attempt to reconnect with appropriate time delay.
                if (!isAttemptingToReconnect) {
                    isAttemptingToReconnect = true;
                    assuranceSessionPresentationManager.onSessionReconnecting();
                    pluginManager.onSessionDisconnected(closeCode);
                    Log.warning(
                            Assurance.LOG_TAG,
                            LOG_TAG,
                            "Assurance disconnected, attempting to reconnect..");
                }

                // attempt to reconnect after a certain delay through reconnect handler
                socketReconnectHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                attemptReconnect();
                            }
                        },
                        delayBeforeReconnect);
        }
    }

    @Override
    public void onSocketError(final AssuranceWebViewSocket socket) {
        // to handle error on webSocket
        // Currently no-operation and its not designed to receive any specific error message from
        // server
        // everytime an error occurs websocketDidDisconnect method is also called with appropriate
        // close code
        // hence the error is handled in that method
    }

    @Override
    public void onSocketStateChange(
            final AssuranceWebViewSocket socket,
            final AssuranceWebViewSocket.SocketReadyState state) {
        assuranceSessionPresentationManager.onSessionStateChange(state);
    }

    /**
     * Notification about an activity of the host app being started. Needed to alter presentation
     * behavior.
     *
     * @param activity the activity of the host application that has started
     */
    void onActivityStarted(final Activity activity) {
        assuranceSessionPresentationManager.onActivityStarted(activity);
    }

    /**
     * Notification about an activity of the host app that has resumed. Needed to alter presentation
     * behavior.
     *
     * @param activity the activity of the host application that has resumed
     */
    void onActivityResumed(final Activity activity) {
        assuranceSessionPresentationManager.onActivityResumed(activity);
    }

    /**
     * Notification about an activity of the host app that was destroyed. Needed to alter
     * presentation behavior.
     *
     * @param activity the activity of the host application that has been destroyed
     */
    void onActivityDestroyed(final Activity activity) {
        assuranceSessionPresentationManager.onActivityDestroyed(activity);
    }

    /**
     * Register a {@code AssuranceSessionStatusListener} against this session to get notified about
     * the current state of the session.
     *
     * @param sessionStatusListener session status listener that is to be registered.
     */
    void registerStatusListener(final AssuranceSessionStatusListener sessionStatusListener) {
        if (sessionStatusListener != null) {
            sessionStatusListeners.add(sessionStatusListener);
        }
    }

    /**
     * Unregister a {@code AssuranceSessionStatusListener} against this session to stop geting
     * notified about the current state of the session.
     *
     * @param sessionStatusListener session status listener that is to be unregistered.
     */
    void unregisterStatusListener(final AssuranceSessionStatusListener sessionStatusListener) {
        if (sessionStatusListener != null) {
            sessionStatusListeners.remove(sessionStatusListener);
        }
    }

    /** Prepares the components required for starting a session. */
    private void initializeSession() {
        assuranceSessionPresentationManager.onSessionInitialized();
    }

    /** Attempts to reconnect to an Assurance session that is already running. */
    private void attemptReconnect() {
        final String connectionUrl = connectionDataStore.getStoredConnectionURL();

        if (StringUtils.isNullOrEmpty(connectionUrl)) {
            // An internal attempt to reconnect is missing a previously stored valid session URL.
            // This, if encountered, is likely a logic error.
            Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Attempt to reconnect is missing a previously "
                            + "stored valid connection URL.");
            initializeSession();
            return;
        }

        final Uri uri = Uri.parse(connectionUrl);
        final String pin = uri.getQueryParameter(AssuranceConstants.DataStoreKeys.TOKEN);

        if (StringUtils.isNullOrEmpty(pin)) {
            Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Attempt to reconnect is missing a previously " + "stored valid token.");
            initializeSession();
            return;
        }

        Log.trace(
                Assurance.LOG_TAG,
                LOG_TAG,
                String.format(
                        "Assurance Session was already connected during "
                                + "previous app launch. Attempting to reconnect. URL: %s",
                        connectionUrl));
        connect(pin);
    }

    /**
     * Handles {@code AssuranceConstants.ControlType.START_EVENT_FORWARDING} event by doing the
     * following :
     *
     * <ol>
     *   <li>Unblock the outbound queue worker.
     *   <li>Remove the WebView UI and display the floating button.
     *   <li>Share the Assurance shared state as necessary.
     *   <li>Notify the client plugins on successful connection.
     * </ol>
     */
    private void onStartForwardingEvent() {
        outboundEventQueueWorker.unblock();
        assuranceSessionPresentationManager.onSessionConnected();

        for (AssuranceSessionStatusListener listener : sessionStatusListeners) {
            if (listener != null) {
                listener.onSessionConnected();
            }
        }

        // If the initial SDK events were cleared because of Assurance shutting down after 5 second
        // timeout
        // then populate the griffon session with all the available shared state details (Both XDM
        // and Regular)
        if (didClearBootEvents) {
            for (final AssuranceEvent stateEvent :
                    assuranceStateManager.getAllExtensionStateData()) {
                queueOutboundEvent(stateEvent);
            }
        }

        pluginManager.onSessionConnected();
    }

    /**
     * Notifies {@code AssuranceSessionStatusListener}'s of session being disconnected and also
     * unregisters the listener. This is to eliminate the possibility of preventing garbage
     * collection of AssuranceSession due to any references to the listener exceeding session
     * lifespan.
     */
    private void notifyTerminationAndRemoveStatusListeners() {
        for (final AssuranceSessionStatusListener listener : sessionStatusListeners) {
            if (listener != null) {
                listener.onSessionTerminated();
                unregisterStatusListener(listener);
            }
        }
    }

    /**
     * Stops the inbound and outbound event workers, clears any state flags and Assurance shared
     * state.
     */
    private void clearSessionData() {
        outboundEventQueueWorker.stop();
        inboundEventQueueWorker.stop();
        socketReconnectThread.quit();
        didClearBootEvents = true;
        connectionDataStore.saveConnectionURL(null);
        assuranceStateManager.clearAssuranceSharedState();
    }
}
