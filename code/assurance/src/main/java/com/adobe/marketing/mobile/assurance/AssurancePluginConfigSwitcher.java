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


import android.content.Context;
import android.content.SharedPreferences;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.assurance.AssuranceConstants.UILogColorVisibility;
import com.adobe.marketing.mobile.services.Log;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class AssurancePluginConfigSwitcher implements AssurancePlugin {
    private static final String LOG_TAG = "AssurancePluginConfigSwitcher";
    private static final String PREF_KEY_MODIFIED_CONFIG_KEYS = "modifiedConfigKeys";

    private AssuranceSession session;
    private SharedPreferences pref =
            MobileCore.getApplication()
                    .getSharedPreferences(
                            AssuranceConstants.DataStoreKeys.DATASTORE_NAME, Context.MODE_PRIVATE);

    @Override
    public String getVendor() {
        return AssuranceConstants.VENDOR_ASSURANCE_MOBILE;
    }

    @Override
    public String getControlType() {
        return AssuranceConstants.ControlType.CONFIG_UPDATE;
    }

    /** This method will be invoked only if the control event is of type "configUpdate" */
    @Override
    public void onEventReceived(final AssuranceEvent event) {
        final Map<String, Object> controlDetails = event.getControlDetail();

        if (AssuranceUtil.isNullOrEmpty(controlDetails)) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "ConfigUpdate Control event details is empty, Ignoring to update config.");
            return;
        }

        Log.debug(Assurance.LOG_TAG, LOG_TAG, "Updating the configuration.");

        MobileCore.updateConfiguration(controlDetails);
        saveModifiedKeys(controlDetails.keySet());
    }

    @Override
    public void onRegistered(final AssuranceSession parentSession) {
        session = parentSession;
    }

    @Override
    public void onSessionConnected() {
        /* no-op */
    }

    @Override
    public void onSessionDisconnected(final int code) {
        /* no-op */
    }

    @Override
    public void onSessionTerminated() {
        // Get the modified keys from pref
        if (pref == null) {
            return;
        }

        final Set<String> savedKeys = pref.getStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, null);

        if (savedKeys != null) {
            final Map<String, Object> config = new HashMap<>();

            // Iterate over the keys and update the config
            for (final String key : savedKeys) {
                config.put(key, null);
            }

            // Use coreAPI to revert back the configuration
            MobileCore.updateConfiguration(config);
        }

        // remove them from persistence
        clearModifiedKeys();
        session = null;
    }

    private void saveModifiedKeys(final Set<String> payload) {
        if (pref == null) {
            return;
        }

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("ConfigUpdate - Configuration modified for keys");

        // Get the stored keys
        Set<String> savedKeys = pref.getStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, null);

        // retrieved contents of shared preferences should not be modified - so create a copy
        Set<String> modifiedKeys = savedKeys == null ? new HashSet<>() : new HashSet<>(savedKeys);

        // Add the new keys to the savedKeys and update the PREF_KEY_MODIFIED_CONFIG_KEYS
        modifiedKeys.addAll(payload);

        SharedPreferences.Editor editor = pref.edit();
        editor.putStringSet(PREF_KEY_MODIFIED_CONFIG_KEYS, modifiedKeys);
        editor.apply();

        // Append each modified log
        for (String s : payload) {
            logBuilder.append("\n ").append(s);
        }

        // Log in the Local UI
        if (session != null) {
            session.logLocalUI(UILogColorVisibility.HIGH, logBuilder.toString());
        }
    }

    private void clearModifiedKeys() {
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(PREF_KEY_MODIFIED_CONFIG_KEYS);
        editor.apply();
    }
}
