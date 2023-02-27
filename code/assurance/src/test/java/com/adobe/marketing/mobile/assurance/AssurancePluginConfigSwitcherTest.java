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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.SharedPreferences;
import com.adobe.marketing.mobile.MobileCore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AssurancePluginConfigSwitcherTest {

    private static final String EVENT_TYPE_CONFIG_UPDATE = "configUpdate";
    private static final String PREF_KEY_MODIFIED_CONFIG_KEYS = "modifiedConfigKeys";
    AssurancePluginConfigSwitcher assurancePluginConfigSwitcher;

    @Mock Application application;

    @Mock SharedPreferences preferences;

    @Mock SharedPreferences.Editor editor;

    @Mock AssuranceSession mockSession;

    private MockedStatic<MobileCore> mockedStaticMobileCore;

    @Before
    public void testSetup() {

        MockitoAnnotations.openMocks(this);
        mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class);
        mockedStaticMobileCore.when(MobileCore::getApplication).thenReturn(application);

        // mock shared preference
        Mockito.when(application.getSharedPreferences(anyString(), ArgumentMatchers.anyInt()))
                .thenReturn(preferences);
        Mockito.when(preferences.edit()).thenReturn(editor);

        // create plugin instance to test
        assurancePluginConfigSwitcher = new AssurancePluginConfigSwitcher();
        assurancePluginConfigSwitcher.onRegistered(mockSession);
    }

    @Test
    public void test_getVendorName() {
        // test
        String vendor = assurancePluginConfigSwitcher.getVendor();
        assertEquals(vendor, AssuranceConstants.VENDOR_ASSURANCE_MOBILE);
    }

    @Test
    public void test_getControlType() {
        // test
        String vendor = assurancePluginConfigSwitcher.getControlType();
        assertEquals(vendor, AssuranceTestConstants.ControlType.CONFIG_UPDATE);
    }

    @Test
    public void test_onEventReceived() {
        // setup
        HashMap<String, Object> configUpdateDetails = new HashMap<>();
        configUpdateDetails.put("key3", "best rsids");
        configUpdateDetails.put(
                "key4",
                new HashMap<String, Object>() {
                    {
                        put("lib1", "id1");
                    }
                });

        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.CONFIG_UPDATE);
        payload.put("detail", configUpdateDetails);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);
        Set<String> previouslyUpdatedConfig = new HashSet<>();
        previouslyUpdatedConfig.add("key1");
        previouslyUpdatedConfig.add("key2");
        when(preferences.getStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, null))
                .thenReturn(previouslyUpdatedConfig);
        final ArgumentCaptor<Set> savedConfigKeysCaptor = ArgumentCaptor.forClass(Set.class);

        // test
        assurancePluginConfigSwitcher.onEventReceived(event);

        // verify
        verify(preferences, times(1)).getStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, null);
        verify(editor, times(1)).putStringSet(anyString(), savedConfigKeysCaptor.capture());

        // check if the correct keys are saved to persistence
        assertEquals(4, savedConfigKeysCaptor.getValue().size());
        assertTrue(savedConfigKeysCaptor.getValue().contains("key1"));
        assertTrue(savedConfigKeysCaptor.getValue().contains("key2"));
        assertTrue(savedConfigKeysCaptor.getValue().contains("key3"));
        assertTrue(savedConfigKeysCaptor.getValue().contains("key4"));

        // verify updateConfiguration method call
        mockedStaticMobileCore.verify(
                () -> MobileCore.updateConfiguration(configUpdateDetails), times(1));
        // MobileCore.updateConfiguration(configUpdateDetails);

        // verify that config change is logged in the clientUI
        verify(mockSession, times(1))
                .logLocalUI(eq(AssuranceConstants.UILogColorVisibility.HIGH), any(String.class));
    }

    @Test
    public void test_onEventReceived_WhenSharedPreferenceisNull() {
        // setup
        HashMap<String, Object> configUpdateDetails = new HashMap<String, Object>();
        configUpdateDetails.put("key3", "best rsids");
        HashMap<String, Object> payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.CONFIG_UPDATE);
        payload.put("detail", configUpdateDetails);

        Mockito.when(application.getSharedPreferences(anyString(), ArgumentMatchers.anyInt()))
                .thenReturn(null);
        assurancePluginConfigSwitcher = new AssurancePluginConfigSwitcher();
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        assurancePluginConfigSwitcher.onEventReceived(event);

        // verify updateConfiguration method call
        mockedStaticMobileCore.verify(
                () -> MobileCore.updateConfiguration(configUpdateDetails), times(1));
    }

    @Test
    public void test_onEventReceived_with_emptyDetails() {
        // setup
        HashMap<String, Object> payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.CONFIG_UPDATE);
        payload.put("detail", null);

        // test
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);
        assurancePluginConfigSwitcher.onEventReceived(event);

        // verify
        verify(preferences, times(0)).getStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, null);

        // verify updateConfiguration method is not called
        mockedStaticMobileCore.verify(
                () -> MobileCore.updateConfiguration(any(Map.class)), times(0));
    }

    @Test
    public void test_onGriffonUIRemoved() {
        // setup
        Set<String> tobeRemovedKeys = new HashSet<>();
        tobeRemovedKeys.add("key1");
        tobeRemovedKeys.add("key2");
        when(preferences.getStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, null))
                .thenReturn(tobeRemovedKeys);
        final ArgumentCaptor<Map> configCaptor = ArgumentCaptor.forClass(Map.class);

        // test
        assurancePluginConfigSwitcher.onSessionTerminated();

        // verify updateConfiguration method call
        mockedStaticMobileCore.verify(
                () -> MobileCore.updateConfiguration(configCaptor.capture()), times(1));
        assertEquals(2, configCaptor.getValue().size());
        assertTrue(configCaptor.getValue().containsKey("key1"));
        assertTrue(configCaptor.getValue().containsKey("key2"));
        assertNull(configCaptor.getValue().get("key1"));
        assertNull(configCaptor.getValue().get("key2"));

        // verify
        verify(preferences, times(1)).getStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, null);
        verify(editor, times(1)).remove(PREF_KEY_MODIFIED_CONFIG_KEYS);
    }

    @Test
    public void test_onGriffonUIRemoved_whenNoConfigChange() {
        // setup
        when(preferences.getStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, null)).thenReturn(null);

        // test
        assurancePluginConfigSwitcher.onSessionTerminated();

        // verify updateConfiguration method call
        mockedStaticMobileCore.verify(
                () -> MobileCore.updateConfiguration(any(Map.class)), times(0));

        // verify
        verify(preferences, times(1)).getStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, null);
        verify(editor, times(1)).remove(PREF_KEY_MODIFIED_CONFIG_KEYS);
    }

    @Test
    public void test_onGriffonUIRemoved_whenSharedPreferenceNull() {
        // setup
        Mockito.when(application.getSharedPreferences(anyString(), ArgumentMatchers.anyInt()))
                .thenReturn(null);
        assurancePluginConfigSwitcher = new AssurancePluginConfigSwitcher();

        // test
        assurancePluginConfigSwitcher.onSessionTerminated();

        // verify updateConfiguration method call
        mockedStaticMobileCore.verify(
                () -> MobileCore.updateConfiguration(any(Map.class)), times(0));
    }

    @Test
    public void test_noOpMethods_ShouldNotCrash() {
        // test
        assurancePluginConfigSwitcher.onSessionConnected();
        assurancePluginConfigSwitcher.onSessionDisconnected(0);
        assurancePluginConfigSwitcher.onRegistered(null);
    }

    @After
    public void teardown() {
        mockedStaticMobileCore.close();
    }
}
