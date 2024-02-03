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

package com.adobe.marketing.mobile.assurance;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * An orchestrating component that manages the creation and teardown of sessions in response to
 * different events or work flows (scanning QR code, disconnection from Pin screen, shake gesture
 * for QuickConnect etc).
 *
 * <p>Acts as the source of truth for all operations related to active session.
 */
class AssuranceSessionOrchestrator {
    private static final String LOG_TAG = "AssuranceSessionOrchestrator";
    private final HostAppActivityLifecycleObserver activityLifecycleObserver;
    private final AssuranceStateManager assuranceStateManager;
    private final List<AssurancePlugin> plugins;
    private final AssuranceConnectionDataStore connectionURLStore;
    private final AssuranceSessionCreator sessionCreator;
    /** Represents the current active session if one exists. */
    private AssuranceSession session;

    /**
     * A buffer for holding the events until the initial Assurance session associated with the app
     * launch happens. This is emptied once a session has been connected.
     */
    private List<AssuranceEvent> outboundEventBuffer;

    private final SessionUIOperationHandler sessionUIOperationHandler;

    /**
     * Responsible for listening to the state of the session connection. Required for releasing
     * resources associated with the session (if any).
     */
    private final AssuranceSessionStatusListener sessionStatusListener =
            new AssuranceSessionStatusListener() {
                @Override
                public void onSessionConnected() {
                    if (outboundEventBuffer == null) {
                        return;
                    }

                    // Once a session has been connected, orchestrator is no longer required to
                    // hold the buffer of events.
                    outboundEventBuffer.clear();
                    outboundEventBuffer = null;
                }

                @Override
                public void onSessionDisconnected(
                        final AssuranceConstants.AssuranceConnectionError error) {
                    // Do nothing here. This callback is invoked when the session is disconnected
                    // There may be a reconnect attempt later, so wait for the session to be
                    // terminated.
                }

                @Override
                public void onSessionTerminated(
                        @Nullable AssuranceConstants.AssuranceConnectionError error) {
                    // In case of a user initiated AssuranceSessionOrchestrator#terminateSession()
                    // will unregister the listener against the session
                    // before disconnecting it. So this callback is never invoked in that flow.
                    // However, in case of a disconnection initiated by the service, we need to
                    // cleanup references to the
                    // active session to release resources.
                    terminateSession(true);
                }
            };

    AssuranceSessionOrchestrator(
            final Application application,
            final AssuranceStateManager assuranceStateManager,
            final List<AssurancePlugin> plugins,
            final AssuranceConnectionDataStore connectionURLStore) {
        this(
                application,
                assuranceStateManager,
                plugins,
                connectionURLStore,
                new AssuranceSessionCreator());
    }

    @VisibleForTesting
    AssuranceSessionOrchestrator(
            final Application application,
            final AssuranceStateManager assuranceStateManager,
            final List<AssurancePlugin> plugins,
            final AssuranceConnectionDataStore connectionURLStore,
            final AssuranceSessionCreator sessionCreator) {
        this.assuranceStateManager = assuranceStateManager;
        this.plugins = plugins;
        this.connectionURLStore = connectionURLStore;
        this.activityLifecycleObserver = new HostAppActivityLifecycleObserver(this);
        this.outboundEventBuffer = new ArrayList<>();
        this.sessionCreator = sessionCreator;
        this.sessionUIOperationHandler = new SessionUIOperationHandler(this);

        application.registerActivityLifecycleCallbacks(activityLifecycleObserver);
        AssuranceComponentRegistry.INSTANCE.initialize(
                assuranceStateManager, sessionUIOperationHandler);
    }

    /**
     * Creates a new {@code AssuranceSession} , registers the {@code sessionStatusListener} against
     * it and also shares the shared state for the extension.
     *
     * @param sessionId the session id that the {@code AssuranceSession} should be connected to.
     * @param environment the {@code AssuranceEnvironment} the {@code AssuranceSession} should be
     *     connected to.
     * @param code the Pin code with which the {@code AssuranceSession} should be authenticated
     *     with.
     * @param statusListener an optional status listener that can be attached to the session
     * @param authorizingPresentationType the type of the authorization UI to be shown for the
     *     session
     */
    synchronized void createSession(
            @NonNull final String sessionId,
            @NonNull final AssuranceConstants.AssuranceEnvironment environment,
            @NonNull final String code,
            @Nullable final AssuranceSessionStatusListener statusListener,
            @NonNull final SessionAuthorizingPresentationType authorizingPresentationType) {
        if (session != null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "An active session already exists. Cannot create a new one.");
            return;
        }

        // create a new session
        session =
                sessionCreator.create(
                        sessionId,
                        code,
                        environment,
                        assuranceStateManager,
                        plugins,
                        connectionURLStore,
                        outboundEventBuffer,
                        statusListener,
                        authorizingPresentationType);

        // register the session status listener for orchestrator to manage the outboundEventBuffer.
        session.registerStatusListener(sessionStatusListener);

        // Immediately share the extension state.
        assuranceStateManager.shareAssuranceSharedState(sessionId);

        // Attempt connection to the session
        session.connect();
    }

    /**
     * Dissolve the active session (if one exists) and its associated states.
     *
     * @param purgeBuffer flag indicating whether or not to clear buffered events.
     */
    synchronized void terminateSession(final boolean purgeBuffer) {
        Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Terminating active session purging Assurance shared state");

        if (purgeBuffer && outboundEventBuffer != null) {
            Log.debug(Assurance.LOG_TAG, LOG_TAG, "Clearing the queued events.");

            outboundEventBuffer.clear();
            outboundEventBuffer = null;
        }

        assuranceStateManager.clearAssuranceSharedState();

        if (session != null) {
            session.unregisterStatusListener(sessionStatusListener);
            session.disconnect();
            session = null;
        }
    }

    /**
     * Attempt to reconnect to a previously connected session that has not been explicitly
     * disconnected by the user via a valid connection url.
     *
     * @return true if there exists a valid connection url that can be reconnected to, false if a
     *     valid connection url does not exist.
     */
    boolean reconnectToStoredSession() {
        final String connectionURL = connectionURLStore.getStoredConnectionURL();
        Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Attempting to reconnect to stored URL: " + connectionURL);

        if (StringUtils.isNullOrEmpty(connectionURL)) {
            return false;
        }

        final Uri uri = Uri.parse(connectionURL);
        final String sessionId = uri.getQueryParameter(AssuranceConstants.SocketURLKeys.SESSION_ID);

        if (StringUtils.isNullOrEmpty(sessionId)) {
            return false;
        }

        final String pin = uri.getQueryParameter(AssuranceConstants.SocketURLKeys.TOKEN);

        if (StringUtils.isNullOrEmpty(pin)) {
            return false;
        }

        final AssuranceConstants.AssuranceEnvironment environment =
                AssuranceUtil.getEnvironmentFromSocketUri(uri);

        Log.trace(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Initializing Assurance session. %s using stored connection details:%s ",
                sessionId,
                connectionURL);
        createSession(sessionId, environment, pin, null, SessionAuthorizingPresentationType.PIN);
        return true;
    }

    /**
     * Retrieves the active session if one exists.
     *
     * @return the active {@code AssuranceSession} if one exists, null otherwise
     */
    AssuranceSession getActiveSession() {
        return session;
    }

    void queueEvent(final AssuranceEvent event) {
        if (event == null) {
            return;
        }

        // Queue this event to the active session if one exists.
        if (session != null) {
            session.queueOutboundEvent(event);
        }

        // We still want to queue the events to the buffer until the session is connected.
        // This ensures that even a session cancellation will allow sending the buffered events
        // in forthcoming session that successfully connects.
        if (outboundEventBuffer != null) {
            outboundEventBuffer.add(event);
        }

        // Drop the event otherwise.
    }

    /**
     * Check if the Assurance extension is capable of handling events. Extension is capable of
     * handling events as long as it is waiting for the first session to be established on launch or
     * if an active session exists. This is inferred by the existence of an active session or the
     * existence of an outboundEventBuffer.
     *
     * @return true if extension is waiting for the first session to be established on launch
     *     (before shutting down) or, if an active session exists. false if extension is shutdown or
     *     no active session exists.
     */
    @VisibleForTesting
    boolean canProcessSDKEvents() {
        return session != null || outboundEventBuffer != null;
    }

    /**
     * Responsible for observing the activity lifecycle of the host application that this extension
     * is registered against. Acts as the primary source of deeplink based session initiation. This
     * class also relays the details of the current activity for use by other components of the
     * extension.
     */
    static class HostAppActivityLifecycleObserver
            implements Application.ActivityLifecycleCallbacks {
        private final AssuranceSessionOrchestrator sessionOrchestrator;

        HostAppActivityLifecycleObserver(final AssuranceSessionOrchestrator sessionOrchestrator) {
            this.sessionOrchestrator = sessionOrchestrator;
        }

        @Override
        public void onActivityCreated(
                @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            final Intent intent = activity.getIntent();
            final Uri data = intent.getData();

            if (data != null) {
                Assurance.startSession(data.toString());
            }

            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Session Activity Hook - onActivityCreated called "
                            + activity.getClass().getCanonicalName());
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Session Activity Hook - onActivityResumed called "
                            + activity.getClass().getCanonicalName());
            final AssuranceSession activeSession = sessionOrchestrator.getActiveSession();

            if (activeSession != null) {
                activeSession.onActivityResumed(activity);
            }
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {}

        @Override
        public void onActivityPaused(@NonNull Activity activity) {}

        @Override
        public void onActivityStopped(@NonNull Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(
                @NonNull Activity activity, @NonNull Bundle outState) {}

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {}
    }

    /** Exists ONLY for test convenience. */
    @VisibleForTesting
    SessionUIOperationHandler getSessionUIOperationHandler() {
        return sessionUIOperationHandler;
    }

    /** Exists ONLY for test convenience. */
    @VisibleForTesting
    AssuranceSessionStatusListener getAssuranceSessionStatusListener() {
        return sessionStatusListener;
    }

    /**
     * A convenience class for mocking session creation during tests. Exists ONLY for test
     * convenience.
     */
    @VisibleForTesting
    static class AssuranceSessionCreator {
        AssuranceSession create(
                final String sessionId,
                final String pin,
                final AssuranceConstants.AssuranceEnvironment environment,
                final AssuranceStateManager assuranceStateManager,
                final List<AssurancePlugin> plugins,
                final AssuranceConnectionDataStore connectionURLStore,
                final List<AssuranceEvent> outboundEventBuffer,
                final AssuranceSessionStatusListener authorizingPresentationListener,
                final SessionAuthorizingPresentationType authorizingPresentationType) {
            return new AssuranceSession(
                    assuranceStateManager,
                    sessionId,
                    pin,
                    environment,
                    connectionURLStore,
                    plugins,
                    outboundEventBuffer,
                    authorizingPresentationType,
                    authorizingPresentationListener);
        }
    }
}
