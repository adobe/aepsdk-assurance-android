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

import static com.adobe.marketing.mobile.assurance.AssuranceConstants.PayloadDataKeys.STATE_DATA;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.PayloadDataKeys.XDM_STATE_DATA;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Responsible for managing the Assurance shared state and fetching shared states for other
 * extensions.
 */
class AssuranceStateManager {
    private static final String LOG_TAG = "AssuranceStateManager";

    private final ExtensionApi extensionApi;
    private final AssuranceSharedState assuranceSharedState;

    /**
     * Most recent event received from the EventHub. Internally used for fetching states of other
     * extensions.
     */
    private Event lastSDKEvent = null;

    AssuranceStateManager(final ExtensionApi extensionApi, final Application application) {
        this.extensionApi = extensionApi;
        this.assuranceSharedState = new AssuranceSharedState(application);
    }

    /**
     * Updates the AssuranceStateManager about the latest event from the EventHub.
     *
     * @param event latest event from the EventHub
     */
    void onSDKEvent(final Event event) {
        lastSDKEvent = event;
    }

    /**
     * Updates the shared state of Assurance Extension. Calling this method will retrieve and update
     * the latest sessionId from the current session. Assurance shares sessionId, clientId and
     * integrationId as its state.
     *
     * @param sessionID the current assurance session identifier
     */
    void shareAssuranceSharedState(final String sessionID) {
        assuranceSharedState.setSessionId(sessionID);

        final Map<String, Object> sharedState = assuranceSharedState.getAssuranceSharedState();
        Log.debug(Assurance.LOG_TAG, LOG_TAG, "Assurance shared state updated: \n %s", sharedState);
        extensionApi.createSharedState(sharedState, lastSDKEvent);
    }

    /** Clears the shared state of Assurance Extension. */
    void clearAssuranceSharedState() {
        assuranceSharedState.setSessionId(null);

        // Sets latest shared state to null.
        extensionApi.createSharedState(new HashMap<>(), null);
        Log.debug(Assurance.LOG_TAG, LOG_TAG, "Assurance shared state cleared");
    }

    /**
     * Fetches the active sessionId if one exists.
     *
     * @return sessionId of the active session if one exists, null otherwise
     */
    String getSessionId() {
        return assuranceSharedState.getSessionId();
    }

    /**
     * Fetches the id for the client.
     *
     * @return the id for the client
     */
    String getClientId() {
        return assuranceSharedState.getClientId();
    }

    /**
     * Fetches the orgID from the latest shared state of the configuration extension.
     *
     * @param urlEncoded true if the orgID needs to be encoded to URL supported format
     * @return OrgId configured for the app, Empty string if orgID is unavailable
     */
    String getOrgId(final boolean urlEncoded) {
        final SharedStateResult latestConfigSharedStateResult =
                extensionApi.getSharedState(
                        AssuranceConstants.SDKSharedStateName.CONFIGURATION,
                        lastSDKEvent,
                        false,
                        SharedStateResolution.ANY);

        if (!isSharedStateSet(latestConfigSharedStateResult)) {
            Log.error(
                    Assurance.LOG_TAG, LOG_TAG, "SDK configuration is not available to read OrgId");
            return "";
        }

        final Map<String, Object> latestConfigSharedState =
                latestConfigSharedStateResult.getValue();

        if (AssuranceUtil.isNullOrEmpty(latestConfigSharedState)) {
            Log.error(
                    Assurance.LOG_TAG, LOG_TAG, "SDK configuration is not available to read OrgId");
            return "";
        }

        final String orgId =
                DataReader.optString(
                        latestConfigSharedState, AssuranceConstants.SDKConfigurationKey.ORG_ID, "");

        if (StringUtils.isNullOrEmpty(orgId)) {
            Log.debug(Assurance.LOG_TAG, LOG_TAG, "Org id is null or empty");
            return "";
        }

        if (!urlEncoded) {
            return orgId;
        }

        return urlEncode(orgId);
    }

    /**
     * Retrieves a list of {@link AssuranceEvent} with payloads containing regular and XDM shared
     * state of registered extension. Extension with null or empty states are ignored.
     */
    List<AssuranceEvent> getAllExtensionStateData() {
        final List<AssuranceEvent> states = new ArrayList<>();
        final SharedStateResult eventHubSharedStateResult =
                extensionApi.getSharedState(
                        AssuranceConstants.SDKSharedStateName.EVENTHUB,
                        lastSDKEvent,
                        false,
                        SharedStateResolution.ANY);

        if (!isSharedStateSet(eventHubSharedStateResult)) {
            return states;
        }

        final Map<String, Object> registeredExtensions = eventHubSharedStateResult.getValue();

        // bail out early if the event hub shared state does not contain any registered extension
        // details
        if (AssuranceUtil.isNullOrEmpty(registeredExtensions)) {
            return states;
        }

        // Add eventHub shared state details
        states.addAll(
                getStateForExtension(
                        AssuranceConstants.SDKSharedStateName.EVENTHUB, "EventHub State"));

        final Map<String, Object> extensionsMap =
                DataReader.optTypedMap(
                        Object.class,
                        registeredExtensions,
                        AssuranceConstants.SDKEventDataKey.EXTENSIONS,
                        null);

        if (extensionsMap == null) {
            return states;
        }

        // loop through the registered extensions and add their states
        for (final String extensionName : extensionsMap.keySet()) {
            final String friendlyName = getFriendlyExtensionName(extensionsMap, extensionName);
            states.addAll(
                    getStateForExtension(
                            extensionName,
                            String.format(
                                    "%s State",
                                    friendlyName))); // an example of AssuranceEvent name is
            // "UserProfile State"
        }

        return states;
    }

    /**
     * Creates a list of {@link AssuranceEvent} with the latest shared state contents for the
     * provided stateOwner.
     *
     * <p>Both Regular and XDM Shared state data are fetched. Shared states with null or empty data
     * are ignored.
     *
     * @param stateOwner {@link String} representing the shared state owner for which the content
     *     has to be fetched
     * @param eventName {@code String} representing the eventName for the Assurance Event
     * @return {@List} of {@code AssuranceEvents}
     */
    private List<AssuranceEvent> getStateForExtension(
            final String stateOwner, final String eventName) {
        final List<AssuranceEvent> stateEvents = new ArrayList<>();

        // create an event if the extension has a regular shared state
        final SharedStateResult regularSharedState =
                extensionApi.getSharedState(
                        stateOwner, lastSDKEvent, false, SharedStateResolution.ANY);

        if (isSharedStateSet(regularSharedState)
                && !AssuranceUtil.isNullOrEmpty(regularSharedState.getValue())) {
            stateEvents.add(
                    prepareSharedStateEvent(
                            stateOwner, eventName, regularSharedState.getValue(), STATE_DATA));
        }

        // create an event if the extension has a xdm shared state
        final SharedStateResult xdmSharedState =
                extensionApi.getXDMSharedState(
                        stateOwner, lastSDKEvent, false, SharedStateResolution.ANY);

        if (isSharedStateSet(xdmSharedState)
                && !AssuranceUtil.isNullOrEmpty(xdmSharedState.getValue())) {
            stateEvents.add(
                    prepareSharedStateEvent(
                            stateOwner, eventName, xdmSharedState.getValue(), XDM_STATE_DATA));
        }

        return stateEvents;
    }

    /**
     * Creates an {@link AssuranceEvent} for shared state data with the provided information.
     *
     * @param owner the owner of the shared state
     * @param eventName the {@code AssuranceEvent} name
     * @param stateContent a {@link Map} of the shared state content
     * @param stateType the type of shared state key. Should be either XDM (xdm.state.data) or
     *     Regular (state.data)
     * @return an {@code AssuranceEvent}
     */
    private AssuranceEvent prepareSharedStateEvent(
            final String owner,
            final String eventName,
            final Map<String, Object> stateContent,
            final String stateType) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put(AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME, eventName);
        payload.put(
                AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE, EventType.HUB);
        payload.put(
                AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE,
                EventSource.SHARED_STATE);
        payload.put(
                AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA,
                new HashMap<String, String>() {
                    {
                        put(AssuranceConstants.SDKEventDataKey.STATE_OWNER, owner);
                    }
                });
        final Map<String, Object> stateData = new HashMap<>();
        stateData.put(stateType, stateContent);
        payload.put(AssuranceConstants.PayloadDataKeys.METADATA, stateData);
        return new AssuranceEvent(AssuranceConstants.AssuranceEventType.GENERIC, payload);
    }

    /**
     * Helper method to retrieve the friendly extension name from the extension details map.
     *
     * @param extensionsMap a {@link Map} of eventHub's shared state data
     * @param extensionName the name of the extension whose friendly name is to be retrieved
     * @return a {@code String} representing the friendly name for the extension, if the extension
     *     details map does not contain a friendly name then extensionName is returned
     */
    private String getFriendlyExtensionName(
            final Map<String, Object> extensionsMap, final String extensionName) {
        String friendlyName = extensionName;

        try {
            final Map<String, Object> extensionDetails =
                    (Map<String, Object>) extensionsMap.get(extensionName);
            friendlyName =
                    (String) extensionDetails.get(AssuranceConstants.SDKEventDataKey.FRIENDLY_NAME);
        } catch (final Exception ignored) {
        }

        return friendlyName;
    }

    private String urlEncode(final String content) {
        try {
            return URLEncoder.encode(content, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Error while encoding the content. Error %s",
                    e.getLocalizedMessage());
        }

        return "";
    }

    /**
     * Represents the shared state for the Assurance Extension. Responsible for loading, saving
     * entities comprising the shared state.
     */
    @VisibleForTesting
    static class AssuranceSharedState {
        private final AtomicReference<String> clientId = new AtomicReference<>();
        private final AtomicReference<String> sessionId = new AtomicReference<>();
        private final SharedPreferences sharedPreferences;

        @VisibleForTesting
        AssuranceSharedState(final Application application) {
            sharedPreferences =
                    (application != null)
                            ? application.getSharedPreferences(
                                    AssuranceConstants.DataStoreKeys.DATASTORE_NAME,
                                    Context.MODE_PRIVATE)
                            : null;
            load();
        }

        /**
         * Retrieves the most recent shared state for Assurance extension.
         *
         * @return shared state for the Assurance extension.
         */
        @VisibleForTesting
        Map<String, Object> getAssuranceSharedState() {
            final HashMap<String, Object> state = new HashMap<>();
            final String activeClientId = clientId.get();
            final String activeSessionId = sessionId.get();
            final boolean isValidClientId = !StringUtils.isNullOrEmpty(activeClientId);
            final boolean isValidSessionId = !StringUtils.isNullOrEmpty(activeSessionId);

            if (isValidClientId) {
                state.put(
                        AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_CLIENT_ID,
                        activeClientId);
            }

            if (isValidSessionId) {
                state.put(
                        AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_SESSION_ID,
                        activeSessionId);
            }

            if (isValidClientId && isValidSessionId) {
                state.put(
                        AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_INTEGRATION_ID,
                        String.format("%s|%s", activeSessionId, activeClientId));
            }

            return state;
        }

        /**
         * Updates and persists the sessionId key of the AssuranceShared state.
         *
         * @param sessionId new session identifier that the Assurance shared state needs to be
         *     updated with
         */
        @VisibleForTesting
        void setSessionId(final String sessionId) {
            this.sessionId.set(sessionId);
            save();
        }

        @VisibleForTesting
        String getSessionId() {
            return sessionId.get();
        }

        @VisibleForTesting
        String getClientId() {
            return clientId.get();
        }

        /**
         * Loads the keys corresponding to the shared state of Assurance Extension from shared
         * preferences.
         */
        private void load() {
            if (sharedPreferences == null) {
                Log.warning(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Unable to access persistence to load ClientUUID, creating a new client"
                                + " UUID");
                clientId.set(UUID.randomUUID().toString());
                sessionId.set("");
                return;
            }

            // read clientId and sessionId from persistence to memory
            final String persistedClientId =
                    sharedPreferences.getString(AssuranceConstants.DataStoreKeys.CLIENT_ID, "");
            final String persistedSessionId =
                    sharedPreferences.getString(AssuranceConstants.DataStoreKeys.SESSION_ID, "");
            Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format(
                            "Assurance state loaded, sessionID : \"%s\" and clientId \"%s\" from"
                                    + " persistence.",
                            persistedSessionId, persistedClientId));

            // If the clientId loaded is empty from persistence, then create one and save it to
            // persistence
            clientId.set(
                    StringUtils.isNullOrEmpty(persistedClientId)
                            ? UUID.randomUUID().toString()
                            : persistedClientId);

            sessionId.set(persistedSessionId);

            save();
        }

        /**
         * Stores the keys corresponding to the shared state of Assurance Extension to shared
         * preferences.
         */
        private void save() {
            if (sharedPreferences == null) {
                Log.warning(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Unable to save sessionId and clientId in persistence, Shared Preference"
                                + " is null");
                return;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (editor == null) {
                Log.warning(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Unable to save sessionId and clientId in persistence, Shared Preference"
                                + " editor is null");
                return;
            }

            // save sessionId to persistence
            if (!StringUtils.isNullOrEmpty(sessionId.get())) {
                editor.putString(AssuranceConstants.DataStoreKeys.SESSION_ID, sessionId.get());
            } else {
                editor.remove(AssuranceConstants.DataStoreKeys.SESSION_ID);
            }

            // save clientId to persistence
            if (!StringUtils.isNullOrEmpty(clientId.get())) {
                editor.putString(AssuranceConstants.DataStoreKeys.CLIENT_ID, clientId.get());
            } else {
                editor.remove(AssuranceConstants.DataStoreKeys.CLIENT_ID);
            }

            editor.apply();
        }
    }

    private boolean isSharedStateSet(SharedStateResult result) {
        return result != null && result.getStatus() == SharedStateStatus.SET;
    }
}
