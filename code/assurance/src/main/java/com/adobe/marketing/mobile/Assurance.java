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

package com.adobe.marketing.mobile;


import androidx.annotation.NonNull;
import com.adobe.marketing.mobile.assurance.AssuranceExtension;
import com.adobe.marketing.mobile.services.Log;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Assurance {

    public static final Class<? extends Extension> EXTENSION = AssuranceExtension.class;
    public static final String LOG_TAG = "Assurance";
    public static final String EXTENSION_VERSION = "2.2.0";
    public static final String EXTENSION_NAME = "com.adobe.assurance";
    public static final String EXTENSION_FRIENDLY_NAME = "Assurance";

    private static final String DEEPLINK_SESSION_ID_KEY = "adb_validation_sessionid";
    private static final String START_SESSION_URL = "startSessionURL";
    private static final String IS_QUICK_CONNECT = "quickConnect";

    // ========================================================================================
    // Public APIs
    // ========================================================================================

    /**
     * Returns the current version of the Assurance extension.
     *
     * @return A {@link String} representing Assurance extension version
     */
    @NonNull
    public static String extensionVersion() {
        return EXTENSION_VERSION;
    }

    /**
     * Starts a Project Assurance session with the provided URL
     *
     * <p>Calling this method when a session has already been started will result in a no-op. It
     * will attempt to initiate a new Project Assurance session if no session is active.
     *
     * @param url a valid Project Assurance deeplink URL to start a session
     */
    public static void startSession(@NonNull final String url) {
        // validate the obtained URL
        if (url == null || !url.contains(DEEPLINK_SESSION_ID_KEY)) {
            Log.warning(
                    LOG_TAG,
                    LOG_TAG,
                    String.format(
                            "Not a valid Assurance deeplink, Ignorning start session API call. URL"
                                    + " : %s",
                            url));
            return;
        }

        final Map<String, Object> startSessionEventData = new HashMap<>();
        startSessionEventData.put(START_SESSION_URL, url);

        final Event startSessionEvent =
                new Event.Builder(
                                "Assurance Start Session",
                                EventType.ASSURANCE,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(startSessionEventData)
                        .build();
        MobileCore.dispatchEvent(startSessionEvent);
    }

    /**
     * Starts an Assurance session via quick flow. Invoking this method on a non-debuggable build,
     * or when a session already exists will result in a no-op.
     */
    public static void startSession() {
        Log.debug(LOG_TAG, LOG_TAG, "QuickConnect api triggered.");
        // Send a quick connect start session event irrespective of the build here.
        // Validation will be done when the extension handles this event.
        final Event startSessionEvent =
                new Event.Builder(
                                "Assurance Start Session (Quick Connect)",
                                EventType.ASSURANCE,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(Collections.singletonMap(IS_QUICK_CONNECT, true))
                        .build();
        MobileCore.dispatchEvent(startSessionEvent);
    }
}
