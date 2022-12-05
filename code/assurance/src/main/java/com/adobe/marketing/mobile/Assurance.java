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

import com.adobe.marketing.mobile.assurance.AssuranceExtension;
import com.adobe.marketing.mobile.services.Log;

import java.util.HashMap;
import java.util.Map;

public class Assurance {

	public static final Class<? extends Extension> EXTENSION = AssuranceExtension.class;
	public static final String LOG_TAG = "Assurance";
	public static final String EXTENSION_VERSION = "2.0.0";
	public static final String EXTENSION_NAME = "com.adobe.assurance";

	private static final String DEEPLINK_SESSION_ID_KEY = "adb_validation_sessionid";
	private static final String START_SESSION_URL = "startSessionURL";

	// ========================================================================================
	// Public APIs
	// ========================================================================================

	/**
	 * Returns the current version of the Assurance extension.
	 *
	 * @return A {@link String} representing Assurance extension version
	 */
	public static String extensionVersion() {
		return EXTENSION_VERSION;
	}

	/**
	 * Register Assurance extension with {@code MobileCore}
	 * <p>
	 * This will allow the extension to send and receive events to and from the {@code MobileCore}.
	 *
	 * @return returns an boolean as a result of the Assurance extension registration.
	 */
	@Deprecated
	public static boolean registerExtension() {
		ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(final ExtensionError adbExtensionError) {
				Log.error(LOG_TAG, LOG_TAG,
						  String.format("Assurance registration failed with error %s. " +
										"For more details refer to" +
										" https://aep-sdks.gitbook.io/docs/beta/project-griffon/set-up-project-griffon#register-griffon-with-mobile-core",
										adbExtensionError.getErrorName()));
			}
		};
		return MobileCore.registerExtension(AssuranceExtension.class, errorCallback);
	}

	/**
	 * Starts a Project Assurance session with the provided URL
	 *
	 * Calling this method when a session has already been started will result in a no-op.
	 * It will attempt to initiate a new Project Assurance session if no session is active.
	 *
	 * @param url a valid Project Assurance deeplink URL to start a session
	 */
	public static void startSession(final String url) {
		// validate the obtained URL
		if (!url.contains(DEEPLINK_SESSION_ID_KEY)) {
			Log.warning(LOG_TAG, LOG_TAG,
						String.format("Not a valid Assurance deeplink, Ignorning start session API call. URL : %s", url));
			return;
		}

		final Map<String, Object> startSessionEventData = new HashMap();
		startSessionEventData.put(START_SESSION_URL, url);

		final Event startSessionEvent = new Event.Builder("Assurance Start Session",
				EventType.ASSURANCE,
				EventSource.REQUEST_CONTENT)
		.setEventData(startSessionEventData).build();
		MobileCore.dispatchEvent(startSessionEvent);
	}
}
