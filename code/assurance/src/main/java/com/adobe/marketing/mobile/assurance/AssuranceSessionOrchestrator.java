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
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An orchestrating component that manages the creation and teardown of sessions in response to
 * different events or work flows (scanning QR code, disconnection from PIN screen, shake gesture
 * for QuickConnect etc).
 *
 * <p>Acts as the source of truth for all operations related to active session.
 */
class AssuranceSessionOrchestrator {
    private static final String LOG_TAG = "AssuranceSessionOrchestrator";
    private final ApplicationHandle applicationHandle;
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

    /**
     * Acts as a glue between the session presentation layer and session control layer. Responsible
     * for relaying the UI operations relating to session management from the user back to the
     * orchestrator.
     */
    private final SessionUIOperationHandler sessionUIOperationHandler =
            new SessionUIOperationHandler() {
                @Override
                public void onConnect(final String pin) {
                    if (session == null) {
                        Log.error(
                                Assurance.LOG_TAG,
                                LOG_TAG,
                                "PIN Confirmation without active session!");
                        return;
                    }

                    if (StringUtils.isNullOrEmpty(pin)) {
                        Log.error(
                                Assurance.LOG_TAG,
                                LOG_TAG,
                                "Null/Empty PIN recorded. Cannot connect to session.");
                        terminateSession();
                        return;
                    }

                    session.connect(pin);
                }

                @Override
                public void onDisconnect() {
                    Log.debug(
                            Assurance.LOG_TAG,
                            LOG_TAG,
                            "On Disconnect clicked. Disconnecting session.");
                    terminateSession();
                }

                @Override
                public void onCancel() {
                    Log.debug(
                            Assurance.LOG_TAG,
                            LOG_TAG,
                            "On Cancel Clicked. Disconnecting session.");
                    terminateSession();
                }
            };

    /**
     * Responsible for listening to the state of the session connection. Required for releasing
     * resources associated with the session (if any).
     */
    private final AssuranceSession.AssuranceSessionStatusListener sessionStatusListener =
            new AssuranceSession.AssuranceSessionStatusListener() {
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
                public void onSessionTerminated() {
                    // In case of a user initiated AssuranceSessionOrchestrator#terminateSession()
                    // will unregister the listener against the session
                    // before disconnecting it. So this callback is never invoked in that flow.
                    // However, in case of a disconnection initiated by the service, we need to
                    // cleanup references to the
                    // active session to release resources.
                    terminateSession();
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
                new ApplicationHandle(
                        application,
                        ServiceProvider.getInstance().getAppContextService().getCurrentActivity()),
                new AssuranceSessionCreator());
    }

    @VisibleForTesting
    AssuranceSessionOrchestrator(
            final Application application,
            final AssuranceStateManager assuranceStateManager,
            final List<AssurancePlugin> plugins,
            final AssuranceConnectionDataStore connectionURLStore,
            final ApplicationHandle applicationHandle,
            final AssuranceSessionCreator sessionCreator) {
        this.applicationHandle = applicationHandle;
        this.assuranceStateManager = assuranceStateManager;
        this.plugins = plugins;
        this.connectionURLStore = connectionURLStore;
        this.activityLifecycleObserver =
                new HostAppActivityLifecycleObserver(applicationHandle, this);
        this.outboundEventBuffer = new ArrayList<>();
        this.sessionCreator = sessionCreator;

        application.registerActivityLifecycleCallbacks(activityLifecycleObserver);
    }

    /**
     * Creates a new {@code AssuranceSession} , registers the {@code sessionStatusListener} against
     * it and also shares the shared state for the extension.
     *
     * @param sessionId the session id that the {@code AssuranceSession} should be connected to.
     * @param environment the {@code AssuranceEnvironment} the {@code AssuranceSession} should be
     *     connected to.
     * @param code the PIN code with which the {@code AssuranceSession} should be authenticated
     *     with.
     */
    synchronized void createSession(
            final String sessionId,
            final AssuranceConstants.AssuranceEnvironment environment,
            final String code) {
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
                        environment,
                        sessionUIOperationHandler,
                        assuranceStateManager,
                        plugins,
                        connectionURLStore,
                        applicationHandle,
                        outboundEventBuffer);

        // register the session status listener to manage the outboundEventBuffer.
        session.registerStatusListener(sessionStatusListener);

        // Immediately share the extension state.
        assuranceStateManager.shareAssuranceSharedState(sessionId);

        // Attempt connection to the session
        session.connect(code);
    }

    /** Dissolve the active session (if one exists) and its associated states. */
    synchronized void terminateSession() {
        Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Terminating active session. Clearing the queued"
                        + "events and purging Assurance shared state");

        if (outboundEventBuffer != null) {
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
     *     valist connection url does not exist.
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
        final String sessionId = uri.getQueryParameter("sessionId");

        if (StringUtils.isNullOrEmpty(sessionId)) {
            return false;
        }

        final AssuranceConstants.AssuranceEnvironment environment =
                AssuranceUtil.getEnvironmentFromQueryValue(
                        uri.getQueryParameter(
                                AssuranceConstants.DeeplinkURLKeys
                                        .START_URL_QUERY_KEY_ENVIRONMENT));
        final String pin = uri.getQueryParameter(AssuranceConstants.DataStoreKeys.TOKEN);

        if (StringUtils.isNullOrEmpty(pin)) {
            return false;
        }

        Log.trace(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Initializing Assurance session. %s using stored connection details:%s ",
                sessionId,
                connectionURL);
        createSession(sessionId, environment, pin);
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
     * Interface that acts as a glue between the session presentation layer and session control
     * layer.
     */
    interface SessionUIOperationHandler {
        /** Invoked when an UI operation corresponding to session connection attempt is made. */
        void onConnect(final String pin);

        /** Invoked when an UI operation corresponding to session disconnect is made. */
        void onDisconnect();

        /** Invoked when an UI operation related to abandoning a session is made. */
        void onCancel();
    }

    /**
     * Serves as a handle to retrieve the context information about the host application that this
     * extension is registered against. Intentionally a separate class than the {@code
     * HostAppActivityLifecycleObserver} to limit access to the publicly overridden methods of an
     * activity lifecycle observer.
     */
    static class ApplicationHandle {
        private final AtomicReference<WeakReference<Activity>> currentActivity;
        private final WeakReference<Application> application;

        ApplicationHandle(final Application application, final Activity activity) {
            this.application = new WeakReference<>(application);
            this.currentActivity = new AtomicReference<>(new WeakReference<Activity>(activity));
        }

        private void setCurrentActivity(final Activity activity) {
            currentActivity.set(new WeakReference<>(activity));
        }

        /**
         * @return the {@code Application} {@code Context} that the extension is registered against
         */
        Context getAppContext() {
            return application.get();
        }

        /**
         * @return current foreground {@code Activity} of the application that this extension is
         *     registered against, if one exists, null otherwise
         */
        Activity getCurrentActivity() {
            final WeakReference<Activity> activityReference = currentActivity.get();
            return activityReference == null ? null : activityReference.get();
        }
    }

    /**
     * Responsible for observing the activity lifecycle of the host application that this extension
     * is registered against. Acts as the primary source of deeplink based session initiation. This
     * class also relays the details of the current activity for use by other components of the
     * extension via the {@code ApplicationHandle} and the active {@code AssuranceSession}
     */
    static class HostAppActivityLifecycleObserver
            implements Application.ActivityLifecycleCallbacks {
        private final ApplicationHandle applicationHandle;
        private final AssuranceSessionOrchestrator sessionOrchestrator;

        HostAppActivityLifecycleObserver(
                final ApplicationHandle applicationHandle,
                final AssuranceSessionOrchestrator sessionOrchestrator) {
            this.applicationHandle = applicationHandle;
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
        public void onActivityStarted(@NonNull Activity activity) {
            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Session Activity Hook - onActivityStarted called "
                            + activity.getClass().getCanonicalName());

            final AssuranceSession activeSession = sessionOrchestrator.getActiveSession();

            if (activeSession != null) {
                activeSession.onActivityStarted(activity);
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Session Activity Hook - onActivityResumed called "
                            + activity.getClass().getCanonicalName());

            applicationHandle.setCurrentActivity(activity);
            final AssuranceSession activeSession = sessionOrchestrator.getActiveSession();

            if (activeSession != null) {
                activeSession.onActivityResumed(activity);
            }
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Session Activity Hook - onActivityPaused called "
                            + activity.getClass().getCanonicalName());
            applicationHandle.setCurrentActivity(null);
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Session Activity Hook - onActivityStopped called "
                            + activity.getClass().getCanonicalName());
        }

        @Override
        public void onActivitySaveInstanceState(
                @NonNull Activity activity, @NonNull Bundle outState) {}

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Session Activity Hook - onActivityDestroyed called "
                            + activity.getClass().getCanonicalName());

            final AssuranceSession activeSession = sessionOrchestrator.getActiveSession();

            if (activeSession != null) {
                activeSession.onActivityDestroyed(activity);
            }
        }
    }

    /** Exists ONLY for test convenience. */
    @VisibleForTesting
    SessionUIOperationHandler getSessionUIOperationHandler() {
        return sessionUIOperationHandler;
    }

    /** Exists ONLY for test convenience. */
    @VisibleForTesting
    AssuranceSession.AssuranceSessionStatusListener getAssuranceSessionStatusListener() {
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
                final AssuranceConstants.AssuranceEnvironment environment,
                final SessionUIOperationHandler sessionUIOperationHandler,
                final AssuranceStateManager assuranceStateManager,
                final List<AssurancePlugin> plugins,
                final AssuranceConnectionDataStore connectionURLStore,
                final ApplicationHandle applicationHandle,
                final List<AssuranceEvent> outboundEventBuffer) {
            return new AssuranceSession(
                    applicationHandle,
                    assuranceStateManager,
                    sessionId,
                    environment,
                    connectionURLStore,
                    sessionUIOperationHandler,
                    plugins,
                    outboundEventBuffer);
        }
    }
}
