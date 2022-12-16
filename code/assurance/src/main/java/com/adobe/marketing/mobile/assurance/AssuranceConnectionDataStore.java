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
import com.adobe.marketing.mobile.services.Log;

/**
 * Responsible for storing and retrieving the Assurance connection URL required for connecting to a
 * session. Typically used for storing and auto-reconnecting to a session that has not been
 * explicitly disconnected (by the user disconnection or due to an error).
 */
class AssuranceConnectionDataStore {
    private static final String LOG_TAG = "AssuranceConnectionDataStore";
    private final SharedPreferences sharedPreferences;

    AssuranceConnectionDataStore(final Context context) {
        sharedPreferences =
                context == null
                        ? null
                        : context.getSharedPreferences(
                                AssuranceConstants.DataStoreKeys.DATASTORE_NAME,
                                Context.MODE_PRIVATE);
    }

    /**
     * Retrieve the previously stored connection URL for the socket if any.
     *
     * @return the previously stored connection URL for the socket if any
     */
    String getStoredConnectionURL() {
        if (sharedPreferences == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to get connection URL from persistence, SharedPreference instance is"
                            + " null");
            return null;
        }

        return sharedPreferences.getString(AssuranceConstants.DataStoreKeys.SESSION_URL, null);
    }

    /**
     * Persist a new socket connection url.
     *
     * @param url new socket connection url. Note that {@code null} is equivalent to erasing a
     *     previously stored value
     */
    void saveConnectionURL(final String url) {
        if (sharedPreferences == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to get connection URL from persistence, SharedPreference instance is"
                            + " null");
            return;
        }

        final SharedPreferences.Editor editor = sharedPreferences.edit();

        if (editor == null) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to get connection URL from persistence, SharedPreference Editor"
                            + " instance is null");
            return;
        }

        Log.trace(Assurance.LOG_TAG, LOG_TAG, "Session URL stored is:" + url);

        editor.putString(AssuranceConstants.DataStoreKeys.SESSION_URL, url);
        editor.apply();
    }
}
