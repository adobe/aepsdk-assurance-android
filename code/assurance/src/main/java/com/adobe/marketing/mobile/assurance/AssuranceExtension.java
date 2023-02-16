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

import static com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceEnvironment;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.PayloadDataKeys.STATE_DATA;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.PayloadDataKeys.XDM_STATE_DATA;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.SDKEventName.XDM_SHARED_STATE_CHANGE;

import android.net.Uri;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.assurance.AssuranceConstants.GenericEventPayloadKey;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class AssuranceExtension extends Extension {
    private static final String LOG_TAG = "AssuranceExtension";
    private static final long ASSURANCE_SHUTDOWN_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

    private final AssuranceStateManager assuranceStateManager;
    private final AssuranceSessionOrchestrator assuranceSessionOrchestrator;
    private final AssuranceConnectionDataStore assuranceConnectionDataStore;
    private static boolean shouldUnregisterOnTimeout =
            true; // keep the unregister timer active on launch

    // ========================================================================================
    //  Constructor
    // ========================================================================================
    /**
     * Constructor.
     *
     * <p>This method is called by MobileCore during the registration of Assurance extension with
     * core. All the Assurance Extension listeners are registered. Thread : Background thread
     * created by MobileCore
     *
     * @param extensionApi an instance of {@link ExtensionApi} with set of API's to interact with
     *     {@code MobileCore}.
     */
    AssuranceExtension(final ExtensionApi extensionApi) {
        this(
                extensionApi,
                new AssuranceStateManager(extensionApi, MobileCore.getApplication()),
                new AssuranceConnectionDataStore(MobileCore.getApplication()),
                Collections.unmodifiableList(
                        Arrays.asList(
                                new AssurancePluginLogForwarder(),
                                new AssurancePluginScreenshot(),
                                new AssurancePluginConfigSwitcher(),
                                new AssurancePluginFakeEventGenerator())));
    }

    /**
     * Cascading constructor for facilitating dependency injection of components needed for tests.
     */
    @VisibleForTesting
    AssuranceExtension(
            final ExtensionApi extensionApi,
            final AssuranceStateManager assuranceStateManager,
            final AssuranceConnectionDataStore assuranceConnectionDataStore,
            final List<AssurancePlugin> plugins) {
        this(
                extensionApi,
                assuranceStateManager,
                assuranceConnectionDataStore,
                new AssuranceSessionOrchestrator(
                        MobileCore.getApplication(),
                        assuranceStateManager,
                        plugins,
                        assuranceConnectionDataStore));
    }

    /**
     * Cascading constructor for facilitating dependency injection of components needed for tests.
     */
    @VisibleForTesting
    AssuranceExtension(
            final ExtensionApi extensionApi,
            final AssuranceStateManager assuranceStateManager,
            final AssuranceConnectionDataStore assuranceConnectionDataStore,
            final AssuranceSessionOrchestrator assuranceSessionOrchestrator) {
        super(extensionApi);

        this.assuranceStateManager = assuranceStateManager;
        this.assuranceConnectionDataStore = assuranceConnectionDataStore;
        this.assuranceSessionOrchestrator = assuranceSessionOrchestrator;
    }

    // ========================================================================================
    // Public API handler
    // ========================================================================================

    /**
     * Starts a Project Assurance session with the provided URL
     *
     * <p>Calling this method when a session has already been started will result in a no-op. It
     * will attempt to initiate a new Project Assurance session if no session is active.
     *
     * @param deeplink a valid Project Assurance deeplink URL to start a session
     */
    void startSession(final String deeplink) {
        // validate the session instance
        shouldUnregisterOnTimeout = false;

        if (assuranceSessionOrchestrator == null) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to start Assurance session. Make sure Assurance.registerExtension()is"
                        + " called before starting the session. For more details refer to"
                        + " https://aep-sdks.gitbook.io/docs/foundation-extensions/adobe-experience-platform-assurance#register-aepassurance-with-mobile-core");
            return;
        }

        if (assuranceSessionOrchestrator.getActiveSession() != null) {
            Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to start Assurance session. Session already exists");
            return;
        }

        // validate the deeplink
        if (StringUtils.isNullOrEmpty(deeplink)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to start Assurance session. Obtained null or empty deeplink url");
            return;
        }

        final Uri uri = Uri.parse(deeplink);
        final String sessionId = AssuranceUtil.getValidSessionIDFromUri(uri);

        // validate the assurance sessionId from deeplink
        if (StringUtils.isNullOrEmpty(sessionId)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format(
                            "Unable to start Assurance session. The assurance sessionId obtained"
                                    + " deeplink is invalid. Deeplink : %s",
                            deeplink));
            return;
        }

        final AssuranceEnvironment environment =
                AssuranceUtil.getEnvironmentFromQueryValue(
                        uri.getQueryParameter(
                                AssuranceConstants.DeeplinkURLKeys
                                        .START_URL_QUERY_KEY_ENVIRONMENT));

        // Create a new session. Note that new session creation via new deeplink will never have a
        // PIN and
        // will go through the PIN flow. So at this time it is OK to pass a null pin.
        assuranceSessionOrchestrator.createSession(sessionId, environment, null);
        Log.trace(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Received sessionID. Initializing Assurance session. %s",
                sessionId);
    }

    // ========================================================================================
    // overridden methods - Extension class
    // ========================================================================================
    @Override
    protected String getName() {
        return Assurance.EXTENSION_NAME;
    }

    @Override
    protected String getFriendlyName() {
        return Assurance.EXTENSION_FRIENDLY_NAME;
    }

    @Override
    protected String getVersion() {
        return Assurance.EXTENSION_VERSION;
    }

    @Override
    protected void onRegistered() {
        super.onRegistered();
        getApi().registerEventListener(
                        EventType.WILDCARD, EventSource.WILDCARD, this::handleWildcardEvent);
        getApi().registerEventListener(
                        EventType.ASSURANCE,
                        EventSource.REQUEST_CONTENT,
                        this::handleAssuranceRequestContent);
        getApi().registerEventListener(
                        EventType.PLACES,
                        EventSource.REQUEST_CONTENT,
                        new AssuranceListenerHubPlacesRequests(this));
        getApi().registerEventListener(
                        EventType.PLACES,
                        EventSource.RESPONSE_CONTENT,
                        new AssuranceListenerHubPlacesResponses(this));

        publishAssuranceSharedState();

        // If assurance was already connected, do not start the timer
        if (attemptReconnect()) {
            return;
        }

        // if assurance was not already connected, start timer to shutdown assurance, if
        // startSession API is not called within 5 seconds
        new Timer()
                .schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                // this code executes in timer thread
                                if (shouldUnregisterOnTimeout) {
                                    shutDownAssurance();
                                }
                            }
                        },
                        ASSURANCE_SHUTDOWN_TIMEOUT);

        Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                String.format(
                        "Assurance extension version %s is successfully registered",
                        Assurance.EXTENSION_VERSION));
    }

    @Override
    protected void onUnregistered() {
        super.onUnregistered();
    }

    @Override
    public boolean readyForEvent(final Event event) {
        // Assurance is always ready for processing events as long as it is registered.
        // The decision to queue/process or drop the event is made based on the session connectivity
        // status in AssuranceSessionOrchestrator.queueEvent()
        return true;
    }
    // ========================================================================================
    // Handlers for listened events
    // ========================================================================================

    /**
     * Processes events trapped by AssuranceListenerHubWildcard and converts them into Assurance
     * Events
     *
     * @param event V5 Event object containing the trapped event.
     */
    void handleWildcardEvent(final Event event) {
        // keep track of the last SDK event to create shared state for Assurance
        assuranceStateManager.onSDKEvent(event);
        final Map<String, Object> payload = new HashMap<>();
        payload.put(GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME, event.getName());
        payload.put(GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE, event.getType().toLowerCase());
        payload.put(
                GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE, event.getSource().toLowerCase());
        payload.put(
                GenericEventPayloadKey.ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER,
                event.getUniqueIdentifier());
        payload.put(GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA, event.getEventData());

        // if the event is a shared state change event process differently
        if (EventSource.SHARED_STATE.equalsIgnoreCase(event.getSource())) {
            processSharedStateEvent(event, payload);
            return;
        }

        final AssuranceEvent assuranceEvent =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.GENERIC, payload);
        assuranceSessionOrchestrator.queueEvent(assuranceEvent);
    }

    void handleAssuranceRequestContent(Event event) {
        String sessionURL =
                DataReader.optString(
                        event.getEventData(),
                        AssuranceConstants.SDKEventDataKey.START_SESSION_URL,
                        "");

        if ("".equals(sessionURL)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to process start session event. could find start session URL in the"
                            + " event");
            return;
        }

        startSession(sessionURL);
    }

    // ========================================================================================
    // private methods
    // ========================================================================================

    /** Shares the initial shared state for Assurance. */
    private void publishAssuranceSharedState() {
        final String sessionID = assuranceStateManager.getSessionId();

        // share the state only if the session is available
        if (!StringUtils.isNullOrEmpty(sessionID)) {
            assuranceStateManager.shareAssuranceSharedState(sessionID);
        }
    }

    /**
     * Processes shared state change events trapped by AssuranceListenerHubWildcard Listener.
     *
     * @param event V5 Event object containing the shared state change event that triggered this
     *     update.
     */
    private void processSharedStateEvent(final Event event, Map<String, Object> payload) {
        final Map<String, Object> eventData = event.getEventData();

        if (AssuranceUtil.isNullOrEmpty(eventData)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "EventData for shared state change event is null. Ignoring event");
            return;
        }

        try {
            final String stateOwner =
                    DataReader.getString(eventData, AssuranceConstants.SDKEventDataKey.STATE_OWNER);
            final SharedStateResult sharedStateResult;
            final String stateDataKey;

            // Differentiate the type of shared state using the event name and get the state content
            // accordingly
            // Event Name for XDM shared 		= "Shared state content"
            // Event Name for Regular  shared 	= "Shared state content (XDM)"
            if (XDM_SHARED_STATE_CHANGE.equals(event.getName())) {
                sharedStateResult =
                        getApi().getXDMSharedState(
                                        stateOwner, event, false, SharedStateResolution.ANY);
                stateDataKey = XDM_STATE_DATA;
            } else {
                sharedStateResult =
                        getApi().getSharedState(
                                        stateOwner, event, false, SharedStateResolution.ANY);
                stateDataKey = STATE_DATA;
            }

            // Assurance should log only Shared staes which have been set
            if (sharedStateResult == null
                    || sharedStateResult.getStatus() != SharedStateStatus.SET) {
                return;
            }

            // edit the Assurance event payload to add the shared state content
            payload.put(
                    AssuranceConstants.PayloadDataKeys.METADATA,
                    new HashMap<String, Object>() {
                        {
                            put(stateDataKey, sharedStateResult.getValue());
                        }
                    });

            // prepare AssuranceEvent with shared state data
            assuranceSessionOrchestrator.queueEvent(
                    new AssuranceEvent(AssuranceConstants.AssuranceEventType.GENERIC, payload));
        } catch (final DataReaderException ex) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to extract state owner from shared state change event: "
                            + ex.getLocalizedMessage());
        }
    }

    /**
     * Call this methods to Unregsiter Assurance extension with the {@link MobileCore} This method
     * clears the already queued events waiting to be sent on successful session connect. The shared
     * state of Assurance extension is purged.
     */
    private void shutDownAssurance() {
        Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Timeout - Assurance did not receive deeplink to start Assurance session within 5"
                        + " seconds. Shutting down Assurance extension");
        assuranceSessionOrchestrator.terminateSession();
    }

    /**
     * Attempts to reconnect to a Project Assurance session that is already running.
     *
     * <p>Intended to be called on every app start to ensure that an already running session can be
     * re-connected to.
     *
     * @return {@code boolean} indicating if a there exists a saved session URL with which a
     *     connection can be attempted.
     */
    private boolean attemptReconnect() {
        return assuranceSessionOrchestrator.reconnectToStoredSession();
    }

    /**
     * Adds a log message to the local Assurance UI
     *
     * @param visibility Visibility level of the log message
     * @param message Log message
     */
    void logLocalUI(
            final AssuranceConstants.UILogColorVisibility visibility, final String message) {
        final AssuranceSession session = assuranceSessionOrchestrator.getActiveSession();

        if (session != null) {
            session.logLocalUI(visibility, message);
        }
    }
}
