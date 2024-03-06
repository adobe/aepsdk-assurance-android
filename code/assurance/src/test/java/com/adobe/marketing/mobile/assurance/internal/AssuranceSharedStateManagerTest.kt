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

package com.adobe.marketing.mobile.assurance.internal

import com.adobe.marketing.mobile.services.DataStoring
import com.adobe.marketing.mobile.services.NamedCollection
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.reset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssuranceSharedStateManagerTest {
    companion object {
        private const val MOCK_CLIENT_ID = "mockClientId"
        private const val MOCK_SESSION_ID = "mockSessionId"
    }

    @Mock
    private lateinit var mockDataStoreService: DataStoring

    @Mock
    private lateinit var mockAssuranceNamedCollection: NamedCollection

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `Test initialization with valid client and session id's`() {
        // Setup data store service to return the named collection for assurance
        `when`(mockDataStoreService.getNamedCollection(AssuranceConstants.DataStoreKeys.DATASTORE_NAME)).thenReturn(
            mockAssuranceNamedCollection
        )

        `when`(
            mockAssuranceNamedCollection.getString(
                AssuranceConstants.DataStoreKeys.CLIENT_ID,
                ""
            )
        ).thenReturn(MOCK_CLIENT_ID)

        `when`(
            mockAssuranceNamedCollection.getString(
                AssuranceConstants.DataStoreKeys.SESSION_ID,
                ""
            )
        ).thenReturn(MOCK_SESSION_ID)

        // Test
        val assuranceSharedStateManager = AssuranceSharedStateManager(mockDataStoreService)
        val initialAssuranceSharedState = assuranceSharedStateManager.assuranceSharedState

        // Verify that the initial assurance shared state is initialized with the expected values
        assertEquals(MOCK_CLIENT_ID, initialAssuranceSharedState.clientId)
        assertEquals(MOCK_SESSION_ID, initialAssuranceSharedState.sessionId)

        // Verify that the state is persisted
        verify(mockAssuranceNamedCollection).setString(AssuranceConstants.DataStoreKeys.CLIENT_ID, MOCK_CLIENT_ID)
        verify(mockAssuranceNamedCollection).setString(AssuranceConstants.DataStoreKeys.SESSION_ID, MOCK_SESSION_ID)
    }

    @Test
    fun `Test initialization with null Assurance data store`() {
        // Setup data store service to return null named collection for assurance
        `when`(mockDataStoreService.getNamedCollection(AssuranceConstants.DataStoreKeys.DATASTORE_NAME)).thenReturn(
            null
        )

        // Test
        val assuranceSharedStateManager = AssuranceSharedStateManager(mockDataStoreService)
        val initialAssuranceSharedState = assuranceSharedStateManager.assuranceSharedState

        // Verify that the initial assurance shared state is initialized with a valid client id
        // and an empty session id
        assertNotNull(initialAssuranceSharedState.clientId)
        assertTrue { initialAssuranceSharedState.clientId.isNotBlank() }
        assertEquals("", initialAssuranceSharedState.sessionId)
    }

    @Test
    fun `Test initialization with null client and sessionId`() {
        // Setup data store service to return the named collection for assurance
        `when`(mockDataStoreService.getNamedCollection(AssuranceConstants.DataStoreKeys.DATASTORE_NAME)).thenReturn(
            mockAssuranceNamedCollection
        )

        `when`(
            mockAssuranceNamedCollection.getString(
                AssuranceConstants.DataStoreKeys.CLIENT_ID,
                ""
            )
        ).thenReturn("")

        `when`(
            mockAssuranceNamedCollection.getString(
                AssuranceConstants.DataStoreKeys.SESSION_ID,
                ""
            )
        ).thenReturn("")

        // Test
        val assuranceSharedStateManager = AssuranceSharedStateManager(mockDataStoreService)
        val initialAssuranceSharedState = assuranceSharedStateManager.assuranceSharedState

        // Verify that the initial assurance shared state is initialized with a valid client id
        // and an empty session id
        assertTrue { initialAssuranceSharedState.clientId.isNotBlank() }
        assertEquals("", initialAssuranceSharedState.sessionId)

        // Verify that the clientId is persisted and sessionId is removed from the data store
        verify(mockAssuranceNamedCollection).setString(AssuranceConstants.DataStoreKeys.CLIENT_ID, initialAssuranceSharedState.clientId)
        verify(mockAssuranceNamedCollection).remove(AssuranceConstants.DataStoreKeys.SESSION_ID)
    }

    @Test
    fun `Test #setSessionId when sessionId is null`() {
        // Setup data store service to return the named collection for assurance
        `when`(mockDataStoreService.getNamedCollection(AssuranceConstants.DataStoreKeys.DATASTORE_NAME)).thenReturn(
            mockAssuranceNamedCollection
        )

        `when`(
            mockAssuranceNamedCollection.getString(
                AssuranceConstants.DataStoreKeys.CLIENT_ID,
                ""
            )
        ).thenReturn(MOCK_CLIENT_ID)

        `when`(
            mockAssuranceNamedCollection.getString(
                AssuranceConstants.DataStoreKeys.SESSION_ID,
                ""
            )
        ).thenReturn(MOCK_SESSION_ID)

        val assuranceSharedStateManager = AssuranceSharedStateManager(mockDataStoreService)
        val initialAssuranceSharedState = assuranceSharedStateManager.assuranceSharedState

        // Verify that the initial assurance shared state is initialized with the expected values
        assertEquals(MOCK_CLIENT_ID, initialAssuranceSharedState.clientId)
        assertEquals(MOCK_SESSION_ID, initialAssuranceSharedState.sessionId)
        reset(mockAssuranceNamedCollection) // reset the mock to clear the previous invocations

        // Test
        assuranceSharedStateManager.setSessionId(null)

        val newAssuranceSharedState = assuranceSharedStateManager.assuranceSharedState

        // Verify that the session id is removed and the client id is retained
        assertEquals(MOCK_CLIENT_ID, newAssuranceSharedState.clientId)
        assertEquals("", newAssuranceSharedState.sessionId)

        // Verify that the sessionId is removed from the data store and the clientId is persisted
        // in the data store
        verify(mockAssuranceNamedCollection).remove(AssuranceConstants.DataStoreKeys.SESSION_ID)
        verify(mockAssuranceNamedCollection).setString(AssuranceConstants.DataStoreKeys.CLIENT_ID, MOCK_CLIENT_ID)
    }

    @Test
    fun `Test #setSessionId when sessionId is not null`() {
        // Setup data store service to return the named collection for assurance
        `when`(mockDataStoreService.getNamedCollection(AssuranceConstants.DataStoreKeys.DATASTORE_NAME)).thenReturn(
            mockAssuranceNamedCollection
        )

        `when`(
            mockAssuranceNamedCollection.getString(
                AssuranceConstants.DataStoreKeys.CLIENT_ID,
                ""
            )
        ).thenReturn(MOCK_CLIENT_ID)

        `when`(
            mockAssuranceNamedCollection.getString(
                AssuranceConstants.DataStoreKeys.SESSION_ID,
                ""
            )
        ).thenReturn(MOCK_SESSION_ID)

        val assuranceSharedStateManager = AssuranceSharedStateManager(mockDataStoreService)
        val initialAssuranceSharedState = assuranceSharedStateManager.assuranceSharedState

        // Verify that the initial assurance shared state is initialized with the expected values
        assertEquals(MOCK_CLIENT_ID, initialAssuranceSharedState.clientId)
        assertEquals(MOCK_SESSION_ID, initialAssuranceSharedState.sessionId)
        reset(mockAssuranceNamedCollection) // reset the mock to clear the previous invocations

        // Test
        val newSessionId = UUID.randomUUID().toString()
        assuranceSharedStateManager.setSessionId(newSessionId)

        val newAssuranceSharedState = assuranceSharedStateManager.assuranceSharedState

        // Verify that the session id is updated and the client id is retained
        assertEquals(MOCK_CLIENT_ID, newAssuranceSharedState.clientId)
        assertEquals(newSessionId, newAssuranceSharedState.sessionId)

        verify(mockAssuranceNamedCollection).setString(AssuranceConstants.DataStoreKeys.SESSION_ID, newSessionId)
        verify(mockAssuranceNamedCollection).setString(AssuranceConstants.DataStoreKeys.CLIENT_ID, MOCK_CLIENT_ID)
    }
}
