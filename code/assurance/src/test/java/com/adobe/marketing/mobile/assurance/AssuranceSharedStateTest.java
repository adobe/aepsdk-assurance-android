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

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AssuranceSharedStateTest {

    private AssuranceStateManager.AssuranceSharedState assuranceSharedState;

    @Mock private Application mockAppplication;

    @Mock private SharedPreferences mockSharedPreferences;

    @Mock private SharedPreferences.Editor mockPreferenceEditor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_Init_nullSharedPreferences() {
        // Prepare
        when(mockAppplication.getSharedPreferences(
                        AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE))
                .thenReturn(null);

        // Test
        assuranceSharedState = new AssuranceStateManager.AssuranceSharedState(mockAppplication);

        // Verify
        Assert.assertNotNull(assuranceSharedState.getClientId());
        Assert.assertNotEquals("", assuranceSharedState.getClientId());
        Assert.assertEquals("", assuranceSharedState.getSessionId());
        verifyNoMoreInteractions(mockSharedPreferences);
    }

    @Test
    public void test_Init_validSharedPreferences() {
        // Prepare
        when(mockAppplication.getSharedPreferences(
                        AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockPreferenceEditor);
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.CLIENT_ID, ""))
                .thenReturn("sampleClientID");
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.SESSION_ID, ""))
                .thenReturn("sampleSessionID");

        // Test
        assuranceSharedState = new AssuranceStateManager.AssuranceSharedState(mockAppplication);

        // Verify
        Assert.assertEquals("sampleClientID", assuranceSharedState.getClientId());
        Assert.assertEquals("sampleSessionID", assuranceSharedState.getSessionId());
        verify(mockPreferenceEditor)
                .putString(AssuranceConstants.DataStoreKeys.CLIENT_ID, "sampleClientID");
        verify(mockPreferenceEditor)
                .putString(AssuranceConstants.DataStoreKeys.SESSION_ID, "sampleSessionID");
    }

    @Test
    public void test_Init_emptyPersistedClientId() {
        // Prepare
        when(mockAppplication.getSharedPreferences(
                        AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockPreferenceEditor);
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.CLIENT_ID, ""))
                .thenReturn("");
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.SESSION_ID, ""))
                .thenReturn("sampleSessionID");

        // Test
        assuranceSharedState = new AssuranceStateManager.AssuranceSharedState(mockAppplication);

        // Verify
        Assert.assertNotEquals("", assuranceSharedState.getClientId());
        Assert.assertNotNull(assuranceSharedState.getClientId());
        Assert.assertEquals("sampleSessionID", assuranceSharedState.getSessionId());
        verify(mockPreferenceEditor)
                .putString(
                        AssuranceConstants.DataStoreKeys.CLIENT_ID,
                        assuranceSharedState.getClientId());
        verify(mockPreferenceEditor)
                .putString(AssuranceConstants.DataStoreKeys.SESSION_ID, "sampleSessionID");
    }

    @Test
    public void test_setSessionId_nonNullSessionId() {
        // Prepare
        when(mockAppplication.getSharedPreferences(
                        AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockPreferenceEditor);
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.CLIENT_ID, ""))
                .thenReturn("sampleClientID");
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.SESSION_ID, ""))
                .thenReturn("sampleSessionID");

        assuranceSharedState = new AssuranceStateManager.AssuranceSharedState(mockAppplication);

        // Verify preparation
        Assert.assertEquals("sampleSessionID", assuranceSharedState.getSessionId());
        verify(mockPreferenceEditor)
                .putString(
                        AssuranceConstants.DataStoreKeys.CLIENT_ID,
                        assuranceSharedState.getClientId());
        verify(mockPreferenceEditor)
                .putString(AssuranceConstants.DataStoreKeys.SESSION_ID, "sampleSessionID");
        reset(mockPreferenceEditor);

        // Test
        assuranceSharedState.setSessionId("aNewSessionId");

        // Verify
        verify(mockPreferenceEditor)
                .putString(AssuranceConstants.DataStoreKeys.SESSION_ID, "aNewSessionId");
        Assert.assertEquals("aNewSessionId", assuranceSharedState.getSessionId());
        verify(mockPreferenceEditor).apply();
    }

    @Test
    public void test_setSessionId_nullSessionId() {
        // Prepare
        when(mockAppplication.getSharedPreferences(
                        AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockPreferenceEditor);
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.CLIENT_ID, ""))
                .thenReturn("sampleClientID");
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.SESSION_ID, ""))
                .thenReturn("sampleSessionID");

        assuranceSharedState = new AssuranceStateManager.AssuranceSharedState(mockAppplication);

        // Verify preparation
        Assert.assertEquals("sampleSessionID", assuranceSharedState.getSessionId());
        verify(mockPreferenceEditor)
                .putString(
                        AssuranceConstants.DataStoreKeys.CLIENT_ID,
                        assuranceSharedState.getClientId());
        verify(mockPreferenceEditor)
                .putString(AssuranceConstants.DataStoreKeys.SESSION_ID, "sampleSessionID");
        reset(mockPreferenceEditor);

        // Test
        assuranceSharedState.setSessionId(null);

        // Verify
        verify(mockPreferenceEditor).remove(AssuranceConstants.DataStoreKeys.SESSION_ID);
        Assert.assertNull(assuranceSharedState.getSessionId());
        verify(mockPreferenceEditor).apply();
    }

    @Test
    public void test_setSessionId_nullSharedPreferencesEditor() {
        // Prepare
        when(mockAppplication.getSharedPreferences(
                        AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(null);
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.CLIENT_ID, ""))
                .thenReturn("sampleClientID");
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.SESSION_ID, ""))
                .thenReturn("sampleSessionID");

        assuranceSharedState = new AssuranceStateManager.AssuranceSharedState(mockAppplication);

        // Verify preparation
        Assert.assertEquals("sampleSessionID", assuranceSharedState.getSessionId());

        // Test
        assuranceSharedState.setSessionId(null);

        // Verify
        verifyNoMoreInteractions(mockPreferenceEditor);
    }

    @Test
    public void test_getAssuranceSharedState() {
        // Prepare
        when(mockAppplication.getSharedPreferences(
                        AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockPreferenceEditor);
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.CLIENT_ID, ""))
                .thenReturn("sampleClientID");
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.SESSION_ID, ""))
                .thenReturn("sampleSessionID");

        assuranceSharedState = new AssuranceStateManager.AssuranceSharedState(mockAppplication);

        // Test
        final Map<String, Object> sharedState = assuranceSharedState.getAssuranceSharedState();

        Assert.assertEquals(
                "sampleClientID",
                sharedState.get(AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_CLIENT_ID));
        Assert.assertEquals(
                "sampleSessionID",
                sharedState.get(AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_SESSION_ID));
        Assert.assertEquals(
                "sampleSessionID|sampleClientID",
                sharedState.get(AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_INTEGRATION_ID));
    }

    @Test
    public void test_getAssuranceSharedState_nullSessionId() {
        // Prepare
        when(mockAppplication.getSharedPreferences(
                        AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE))
                .thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockPreferenceEditor);
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.CLIENT_ID, ""))
                .thenReturn("sampleClientID");
        when(mockSharedPreferences.getString(AssuranceConstants.DataStoreKeys.SESSION_ID, ""))
                .thenReturn(null);

        assuranceSharedState = new AssuranceStateManager.AssuranceSharedState(mockAppplication);

        // Test
        final Map<String, Object> sharedState = assuranceSharedState.getAssuranceSharedState();

        Assert.assertEquals(
                "sampleClientID",
                sharedState.get(AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_CLIENT_ID));
        Assert.assertEquals(
                null,
                sharedState.get(AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_SESSION_ID));
        Assert.assertEquals(
                null,
                sharedState.get(AssuranceConstants.SharedStateKeys.ASSURANCE_STATE_INTEGRATION_ID));
    }
}
