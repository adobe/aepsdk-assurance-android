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
package com.adobe.marketing.mobile.assurance

import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.SharedStateStatus
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.util.DataReader
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**
 * Responsible for managing the Assurance shared state (via [AssuranceSharedStateManager])
 * as well as fetching shared states for other extensions.
 */
internal class AssuranceStateManager(private val extensionApi: ExtensionApi) {
    internal companion object {
        private const val LOG_TAG = "AssuranceStateManager"
    }

    private val assuranceSharedStateManager: AssuranceSharedStateManager =
        AssuranceSharedStateManager(
            ServiceProvider.getInstance().dataStoreService
        )

    /**
     * Most recent event received from the EventHub. Internally used for fetching states of other
     * extensions.
     */
    private var lastSDKEvent: Event? = null

    @JvmName("getSessionId")
    internal fun getSessionId() = assuranceSharedStateManager.assuranceSharedState.sessionId

    @JvmName("getClientId")
    internal fun getClientId() = assuranceSharedStateManager.assuranceSharedState.clientId

    /**
     * Updates the AssuranceStateManager about the latest event from the EventHub.
     *
     * @param event latest event from the EventHub
     */
    @JvmName("onSDKEvent")
    internal fun onSDKEvent(event: Event?) {
        lastSDKEvent = event
    }

    /**
     * Updates the shared state of Assurance Extension. Calling this method will retrieve and update
     * the latest sessionId from the current session. Assurance shares sessionId, clientId and
     * integrationId as its state.
     *
     * @param sessionID the current assurance session identifier
     */
    @JvmName("shareAssuranceSharedState")
    internal fun shareAssuranceSharedState(sessionID: String?) {
        assuranceSharedStateManager.setSessionId(sessionID)
        val sharedState: Map<String, Any> = assuranceSharedStateManager.assuranceSharedState.asMap()
        Log.debug(Assurance.LOG_TAG, LOG_TAG, "Assurance shared state updated: \n $sharedState")
        extensionApi.createSharedState(sharedState, lastSDKEvent)
    }

    /** Clears the shared state of Assurance Extension.  */
    @JvmName("clearAssuranceSharedState")
    internal fun clearAssuranceSharedState() {
        assuranceSharedStateManager.setSessionId(null)

        // Sets latest shared state to null.
        extensionApi.createSharedState(mapOf(), null)
        Log.debug(Assurance.LOG_TAG, LOG_TAG, "Assurance shared state cleared")
    }

    /**
     * Fetches the orgID from the latest shared state of the configuration extension.
     *
     * @param urlEncoded true if the orgID needs to be encoded to URL supported format
     * @return OrgId configured for the app, Empty string if orgID is unavailable
     */
    @JvmName("getOrgId")
    internal fun getOrgId(urlEncoded: Boolean): String {
        val latestConfigSharedStateResult = extensionApi.getSharedState(
            AssuranceConstants.SDKSharedStateName.CONFIGURATION,
            lastSDKEvent,
            false,
            SharedStateResolution.ANY
        )
        if (!isSharedStateSet(latestConfigSharedStateResult)) {
            Log.error(
                Assurance.LOG_TAG,
                LOG_TAG,
                "SDK configuration is not available to read OrgId"
            )
            return ""
        }

        val latestConfigSharedState: Map<String, Any?>? = latestConfigSharedStateResult?.value
        if (latestConfigSharedState.isNullOrEmpty()) {
            Log.error(
                Assurance.LOG_TAG,
                LOG_TAG,
                "SDK configuration is not available to read OrgId"
            )
            return ""
        }
        val orgId = DataReader.optString(
            latestConfigSharedState,
            AssuranceConstants.SDKConfigurationKey.ORG_ID,
            ""
        )
        if (orgId.isNullOrEmpty()) {
            Log.debug(Assurance.LOG_TAG, LOG_TAG, "Org id is null or empty")
            return ""
        }
        return if (!urlEncoded) {
            orgId
        } else {
            urlEncode(orgId)
        }
    }

    /**
     * Retrieves a list of [AssuranceEvent] with payloads containing regular and XDM shared
     * state of registered extension. Extension with null or empty states are ignored.
     */
    @JvmName("getAllExtensionStateData")
    internal fun getAllExtensionStateData(): List<AssuranceEvent> {
        val states: MutableList<AssuranceEvent> = mutableListOf()
        val eventHubSharedStateResult = extensionApi.getSharedState(
            AssuranceConstants.SDKSharedStateName.EVENTHUB,
            lastSDKEvent,
            false,
            SharedStateResolution.ANY
        )
        if (!isSharedStateSet(eventHubSharedStateResult)) {
            return states
        }

        val registeredExtensions: Map<String, Any?>? = eventHubSharedStateResult?.value
        // bail out early if the event hub shared state does not contain any registered extension
        // details
        if (registeredExtensions.isNullOrEmpty()) {
            return states
        }

        // Add eventHub shared state details
        states.addAll(
            getStateForExtension(
                AssuranceConstants.SDKSharedStateName.EVENTHUB,
                "EventHub State"
            )
        )
        val extensionsMap = DataReader.optTypedMap(
            Any::class.java,
            registeredExtensions,
            AssuranceConstants.SDKEventDataKey.EXTENSIONS,
            null
        ) ?: return states

        // loop through the registered extensions and add their states
        extensionsMap.keys.forEach { extensionName ->
            val friendlyName = getFriendlyExtensionName(extensionsMap, extensionName)
            states.addAll(
                getStateForExtension(
                    extensionName,
                    "$friendlyName State"
                )
            ) // an example of AssuranceEvent name is "UserProfile State"
        }

        return states
    }

    /**
     * Creates a list of [AssuranceEvent] with the latest shared state contents for the
     * provided stateOwner.
     *
     *
     * Both Regular and XDM Shared state data are fetched. Shared states with null or empty data
     * are ignored.
     *
     * @param stateOwner [String] representing the shared state owner for which the content
     * has to be fetched
     * @param eventName `String` representing the eventName for the Assurance Event
     * @return {@List} of `AssuranceEvents`
     */
    private fun getStateForExtension(
        stateOwner: String,
        eventName: String
    ): List<AssuranceEvent> {
        val stateEvents: MutableList<AssuranceEvent> = mutableListOf()

        // create an event if the extension has a regular shared state
        val regularSharedState = extensionApi.getSharedState(
            stateOwner,
            lastSDKEvent,
            false,
            SharedStateResolution.ANY
        )
        if (isSharedStateSet(regularSharedState) && !regularSharedState?.value.isNullOrEmpty()) {
            stateEvents.add(
                prepareSharedStateEvent(
                    stateOwner,
                    eventName,
                    regularSharedState?.value,
                    AssuranceConstants.PayloadDataKeys.STATE_DATA
                )
            )
        }

        // create an event if the extension has a xdm shared state
        val xdmSharedState = extensionApi.getXDMSharedState(
            stateOwner,
            lastSDKEvent,
            false,
            SharedStateResolution.ANY
        )
        if (isSharedStateSet(xdmSharedState) && !xdmSharedState?.value.isNullOrEmpty()) {
            stateEvents.add(
                prepareSharedStateEvent(
                    stateOwner,
                    eventName,
                    xdmSharedState?.value,
                    AssuranceConstants.PayloadDataKeys.XDM_STATE_DATA
                )
            )
        }
        return stateEvents
    }

    /**
     * Creates an [AssuranceEvent] for shared state data with the provided information.
     *
     * @param owner the owner of the shared state
     * @param eventName the `AssuranceEvent` name
     * @param stateContent a [Map] of the shared state content
     * @param stateType the type of shared state key. Should be either XDM (xdm.state.data) or
     * Regular (state.data)
     * @return an `AssuranceEvent`
     */
    private fun prepareSharedStateEvent(
        owner: String,
        eventName: String,
        stateContent: Map<String, Any?>?,
        stateType: String
    ): AssuranceEvent {
        val payload: MutableMap<String, Any> = HashMap()
        payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME] = eventName
        payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE] = EventType.HUB
        payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE] = EventSource.SHARED_STATE
        payload[AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA] = mapOf(
            AssuranceConstants.SDKEventDataKey.STATE_OWNER to owner
        )

        val stateData: MutableMap<String, Any?> = HashMap()
        stateData[stateType] = stateContent
        payload[AssuranceConstants.PayloadDataKeys.METADATA] = stateData
        return AssuranceEvent(AssuranceConstants.AssuranceEventType.GENERIC, payload)
    }

    /**
     * Helper method to retrieve the friendly extension name from the extension details map.
     *
     * @param extensionsMap a [Map] of eventHub's shared state data
     * @param extensionName the name of the extension whose friendly name is to be retrieved
     * @return a `String` representing the friendly name for the extension, if the extension
     * details map does not contain a friendly name then extensionName is returned
     */
    private fun getFriendlyExtensionName(
        extensionsMap: Map<String, Any?>,
        extensionName: String
    ): String {
        return try {
            val extensionDetails = extensionsMap[extensionName] as Map<String, Any?>
            extensionDetails[AssuranceConstants.SDKEventDataKey.FRIENDLY_NAME] as String
        } catch (ignored: Exception) {
            extensionName
        }
    }

    private fun urlEncode(content: String): String {
        return try {
            URLEncoder.encode(content, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Error while encoding the content. Error %s",
                e.localizedMessage
            )
            ""
        }
    }

    private fun isSharedStateSet(result: SharedStateResult?): Boolean {
        return result != null && result.status == SharedStateStatus.SET
    }
}
