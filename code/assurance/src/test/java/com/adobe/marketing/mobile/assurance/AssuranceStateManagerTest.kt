/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance

import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.SharedStateStatus
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import kotlin.test.assertEquals

class AssuranceStateManagerTest {

    companion object {
        const val CONFIGURATION_EXTENSION_STATE_NAME = "com.adobe.module.configuration"
        const val EDGE_EXTENSION_STATE_NAME = "com.adobe.edge"
        const val EDGE_IDENTITY_EXTENSION_STATE_NAME = "com.adobe.edge.identity"
        const val EDGE_CONSENT_EXTENSION_STATE_NAME = "com.adobe.module.consent"
        const val EVENT_HUB_EXTENSION_STATE_NAME = "com.adobe.module.eventhub"

        private val extensionDetails =
            """
            {
              "extensions": {
                "com.adobe.module.configuration": {
                  "version": "3.0.0",
                  "friendlyName": "Configuration"
                },
                "com.adobe.edge": {
                  "version": "3.0.0",
                  "friendlyName": "Edge"
                },
                "com.adobe.edge.identity": {
                  "version": "3.0.0",
                  "friendlyName": "Edge Identity"
                },
                "com.adobe.module.consent": {
                  "version": "3.0.0",
                  "friendlyName": "Edge Consent"
                }
              },
              "wrapper": {
                "type": "N",
                "friendlyName": "None"
              },
              "version": "3.0.0"
            }
            """.trimIndent()

        private val mockSharedStates: Map<String, SharedStateResult> = mutableMapOf(
            CONFIGURATION_EXTENSION_STATE_NAME to SharedStateResult(
                SharedStateStatus.SET,
                mutableMapOf<String, Any?>("orgId" to "orgId")
            ),
            EDGE_EXTENSION_STATE_NAME to SharedStateResult(
                SharedStateStatus.SET,
                mutableMapOf<String, Any?>("edgeKey" to "edgeValue")
            ),
            EDGE_IDENTITY_EXTENSION_STATE_NAME to SharedStateResult(
                SharedStateStatus.SET,
                mutableMapOf<String, Any?>("edgeIdentityKey" to "edgeIdentityValue")
            ),
            EVENT_HUB_EXTENSION_STATE_NAME to SharedStateResult(
                SharedStateStatus.SET,
                JSONUtils.toMap(JSONObject(extensionDetails))
            )
        )

        private val mockXDMSharedStates: Map<String, SharedStateResult> = mutableMapOf(
            EDGE_CONSENT_EXTENSION_STATE_NAME to SharedStateResult(
                SharedStateStatus.SET,
                mutableMapOf<String, Any?>("consentKey" to "consentValue")
            )
        )

        val expectedEventHubStateEvent = mapOf<String, Any?>(
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME to "EventHub State",
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE to EventType.HUB,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE to EventSource.SHARED_STATE,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA to mapOf(
                AssuranceConstants.SDKEventDataKey.STATE_OWNER to EVENT_HUB_EXTENSION_STATE_NAME,
            ),
            AssuranceConstants.PayloadDataKeys.METADATA to mapOf<String, Any?>(
                AssuranceConstants.PayloadDataKeys.STATE_DATA to mockSharedStates.getValue(
                    EVENT_HUB_EXTENSION_STATE_NAME
                ).value
            )
        )

        val expectedConfigurationStateEvent = mapOf<String, Any?>(
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME to "Configuration State",
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE to EventType.HUB,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE to EventSource.SHARED_STATE,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA to mapOf(
                AssuranceConstants.SDKEventDataKey.STATE_OWNER to CONFIGURATION_EXTENSION_STATE_NAME,
            ),
            AssuranceConstants.PayloadDataKeys.METADATA to mapOf<String, Any?>(
                AssuranceConstants.PayloadDataKeys.STATE_DATA to mockSharedStates.getValue(
                    CONFIGURATION_EXTENSION_STATE_NAME
                ).value
            )
        )

        val expectedEdgeStateEvent = mapOf<String, Any?>(
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME to "Edge State",
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE to EventType.HUB,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE to EventSource.SHARED_STATE,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA to mapOf(
                AssuranceConstants.SDKEventDataKey.STATE_OWNER to EDGE_EXTENSION_STATE_NAME,
            ),
            AssuranceConstants.PayloadDataKeys.METADATA to mapOf<String, Any?>(
                AssuranceConstants.PayloadDataKeys.STATE_DATA to mockSharedStates.getValue(
                    EDGE_EXTENSION_STATE_NAME
                ).value
            )
        )

        val expectedEdgeIdentityStateEvent = mapOf<String, Any?>(
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME to "Edge Identity State",
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE to EventType.HUB,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE to EventSource.SHARED_STATE,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA to mapOf(
                AssuranceConstants.SDKEventDataKey.STATE_OWNER to EDGE_IDENTITY_EXTENSION_STATE_NAME,
            ),
            AssuranceConstants.PayloadDataKeys.METADATA to mapOf<String, Any?>(
                AssuranceConstants.PayloadDataKeys.STATE_DATA to mockSharedStates.getValue(
                    EDGE_IDENTITY_EXTENSION_STATE_NAME
                ).value
            )
        )

        val expectedConsentStateEvent = mapOf<String, Any?>(
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_NAME to "Edge Consent State",
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_TYPE to EventType.HUB,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_SOURCE to EventSource.SHARED_STATE,
            AssuranceConstants.GenericEventPayloadKey.ACP_EXTENSION_EVENT_DATA to mapOf(
                AssuranceConstants.SDKEventDataKey.STATE_OWNER to EDGE_CONSENT_EXTENSION_STATE_NAME,
            ),
            AssuranceConstants.PayloadDataKeys.METADATA to mapOf<String, Any?>(
                AssuranceConstants.PayloadDataKeys.XDM_STATE_DATA to mockXDMSharedStates.getValue(
                    EDGE_CONSENT_EXTENSION_STATE_NAME
                ).value
            )
        )
    }

    @Mock
    private lateinit var mockAssuranceSharedStateManager: AssuranceSharedStateManager

    @Mock
    private lateinit var mockExtensionApi: ExtensionApi

    private lateinit var assuranceStateManager: AssuranceStateManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        assuranceStateManager =
            AssuranceStateManager(mockExtensionApi, mockAssuranceSharedStateManager)
    }

    @Test
    fun `Test #getAllExtensionStateData() with Standard and XDM states`() {
        `when`(
            mockExtensionApi.getSharedState(
                any(),
                eq(null),
                eq(false),
                eq(SharedStateResolution.ANY)
            )
        ).doAnswer {
            val extensionName = it.arguments[0] as String
            mockSharedStates[extensionName]
        }

        `when`(
            mockExtensionApi.getXDMSharedState(
                any(),
                eq(null),
                eq(false),
                eq(SharedStateResolution.ANY)
            )
        ).doAnswer {
            val extensionName = it.arguments[0] as String
            mockXDMSharedStates[extensionName]
        }

        // Test
        val stateEvents = assuranceStateManager.getAllExtensionStateData()

        // Verify the number of state events are as expected
        assertEquals(5, stateEvents.size)

        // Prepare an expected list of state events to compare with the actual state events
        val expectedStateMaps = mutableListOf(
            expectedEventHubStateEvent.toMutableMap(),
            expectedConfigurationStateEvent.toMutableMap(),
            expectedEdgeStateEvent.toMutableMap(),
            expectedEdgeIdentityStateEvent.toMutableMap(),
            expectedConsentStateEvent.toMutableMap()
        )

        val actualStateMaps = stateEvents.map { it.payload }
        assertEquals(JSONArray(expectedStateMaps).toString(), JSONArray(actualStateMaps).toString())
    }

    @Test
    fun `Test #getAllExtensionStateData() with no XDM shared states`() {
        // Setup
        `when`(
            mockExtensionApi.getSharedState(
                any(),
                eq(null),
                eq(false),
                eq(SharedStateResolution.ANY)
            )
        ).doAnswer {
            val extensionName = it.arguments[0] as String
            mockSharedStates[extensionName]
        }

        // Do not return any XDM shared state
        `when`(
            mockExtensionApi.getXDMSharedState(
                any(),
                eq(null),
                eq(false),
                eq(SharedStateResolution.ANY)
            )
        ).doAnswer {
            SharedStateResult(SharedStateStatus.NONE, null)
        }

        // Test
        val stateEvents = assuranceStateManager.getAllExtensionStateData()

        // Verify the number of state events are as expected
        assertEquals(4, stateEvents.size)

        // Prepare an expected list of state events to compare with the actual state events
        val expectedStateMaps = mutableListOf(
            expectedEventHubStateEvent.toMutableMap(),
            expectedConfigurationStateEvent.toMutableMap(),
            expectedEdgeStateEvent.toMutableMap(),
            expectedEdgeIdentityStateEvent.toMutableMap()
            // No Edge Consent state event as there is no XDM shared state
        )

        val actualStateMaps = stateEvents.map { it.payload }
        assertEquals(JSONArray(expectedStateMaps).toString(), JSONArray(actualStateMaps).toString())
    }

    @Test
    fun `Test #getOrgId with url encoding`() {
        val orgId = "B974622245B1A30A490D4D@AdobeOrg"
        setConfigurationSharedStateWithOrgId(orgId)

        val result = assuranceStateManager.getOrgId(true)

        assertEquals("B974622245B1A30A490D4D%40AdobeOrg", result)
    }

    @Test
    fun `Test #getOrgId without url encoding`() {
        val orgId = "B974622245B1A30A490D4D@AdobeOrg"
        setConfigurationSharedStateWithOrgId(orgId)

        val result = assuranceStateManager.getOrgId(false)

        assertEquals(orgId, result)
    }

    @Test
    fun `Test #getOrgId with empty orgId`() {
        val orgId = ""
        setConfigurationSharedStateWithOrgId(orgId)

        val result = assuranceStateManager.getOrgId(false)

        assertEquals(orgId, result)
    }

    @Test
    fun `Test #getOrgId with no configuration state`() {
        `when`(mockExtensionApi.getSharedState(AssuranceConstants.SDKSharedStateName.CONFIGURATION, null, false, SharedStateResolution.ANY))
            .thenReturn(SharedStateResult(SharedStateStatus.NONE, null))

        val result = assuranceStateManager.getOrgId(false)

        assertEquals("", result)
    }

    @Test
    fun `Test #shareAssuranceSharedState() with null SessionId`() {
        `when`(mockAssuranceSharedStateManager.assuranceSharedState).thenReturn(AssuranceSharedState("clientId", "sessionId"))

        assuranceStateManager.shareAssuranceSharedState(null)

        verify(mockAssuranceSharedStateManager).setSessionId(null)
        verify(mockExtensionApi).createSharedState(anyOrNull(), anyOrNull())
    }

    @Test
    fun `Test #shareAssuranceSharedState() with empty SessionId`() {
        `when`(mockAssuranceSharedStateManager.assuranceSharedState).thenReturn(AssuranceSharedState("clientId", "sessionId"))

        assuranceStateManager.shareAssuranceSharedState("")

        verify(mockAssuranceSharedStateManager).setSessionId("")
        verify(mockExtensionApi).createSharedState(anyOrNull(), anyOrNull())
    }

    @Test
    fun `Test #clearAssuranceSharedState() with non-empty SessionId`() {
        assuranceStateManager.clearAssuranceSharedState()

        verify(mockAssuranceSharedStateManager).setSessionId(null)
        verify(mockExtensionApi).createSharedState(mapOf(), null)
    }

    private fun setConfigurationSharedStateWithOrgId(orgId: String) {
        val configurationSharedState = mutableMapOf<String, Any?>()

        orgId.let {
            configurationSharedState[AssuranceConstants.SDKConfigurationKey.ORG_ID] = it
        }

        val sharedStateResult = SharedStateResult(SharedStateStatus.SET, configurationSharedState)
        doReturn(sharedStateResult).`when`(mockExtensionApi).getSharedState(
            AssuranceConstants.SDKSharedStateName.CONFIGURATION,
            null,
            false,
            SharedStateResolution.ANY
        )
    }
}
