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
import com.adobe.marketing.mobile.services.DataStoring
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.NamedCollection
import java.util.UUID

/**
 * A class that manages the persistence of Assurance shared state.
 * @param dataStoreService the data store service to use for persistence.
 */
internal class AssuranceSharedStateManager(dataStoreService: DataStoring) {
    internal companion object {
        private const val LOG_TAG = "AssuranceSharedStateManager"
    }

    private val assuranceDataStore: NamedCollection? =
        dataStoreService.getNamedCollection(AssuranceConstants.DataStoreKeys.DATASTORE_NAME)

    var assuranceSharedState: AssuranceSharedState

    init {
        assuranceSharedState = adjustPersistedState(loadFromPersistence())
    }

    /**
     * Updates and persists the sessionId.
     * @param sessionId the new session id
     */
    @JvmName("setSessionId")
    internal fun setSessionId(sessionId: String?) {
        assuranceSharedState = assuranceSharedState.copy(sessionId = sessionId ?: "")
        persist(assuranceSharedState)
    }

    /**
     * Loads the Assurance shared state from persistence if one was previously stored.
     * @return the Assurance shared state if one was previously stored, otherwise a creates a
     * new empty [AssuranceSharedState]
     */
    private fun loadFromPersistence(): AssuranceSharedState {
        if (assuranceDataStore == null) {
            return AssuranceSharedState("", "")
        }

        val persistedClientId =
            assuranceDataStore.getString(AssuranceConstants.DataStoreKeys.CLIENT_ID, "")
        val persistedSessionId =
            assuranceDataStore.getString(AssuranceConstants.DataStoreKeys.SESSION_ID, "")

        Log.debug(
            Assurance.LOG_TAG,
            LOG_TAG,
            "Assurance state loaded, sessionID : $persistedSessionId and clientId $persistedClientId from persistence."
        )
        return AssuranceSharedState(persistedClientId, persistedSessionId)
    }

    /**
     * Adjusts the persisted state if necessary with a new clientId and persists the state.
     * @param persistedState the persisted state to adjust
     */
    private fun adjustPersistedState(persistedState: AssuranceSharedState): AssuranceSharedState {
        return if (persistedState.clientId.isEmpty()) {
            Log.warning(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Assurance clientId persisted is empty, generating a new one."
            )
            return AssuranceSharedState(UUID.randomUUID().toString(), persistedState.sessionId)
        } else {
            persistedState
        }.also {
            persist(it)
        }
    }

    /**
     * Persists the Assurance shared state provided.
     * @param stateToPersist the Assurance shared state to persist
     */
    private fun persist(stateToPersist: AssuranceSharedState) {
        if (assuranceDataStore == null) {
            Log.warning(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Assurance datastore is null, unable to persist assurance state."
            )
            return
        }

        if (stateToPersist.clientId.isBlank()) {
            assuranceDataStore.remove(AssuranceConstants.DataStoreKeys.CLIENT_ID)
        } else {
            assuranceDataStore.setString(
                AssuranceConstants.DataStoreKeys.CLIENT_ID,
                stateToPersist.clientId
            )
        }

        if (stateToPersist.sessionId.isBlank()) {
            assuranceDataStore.remove(AssuranceConstants.DataStoreKeys.SESSION_ID)
        } else {
            assuranceDataStore.setString(
                AssuranceConstants.DataStoreKeys.SESSION_ID,
                stateToPersist.sessionId
            )
        }
    }
}
