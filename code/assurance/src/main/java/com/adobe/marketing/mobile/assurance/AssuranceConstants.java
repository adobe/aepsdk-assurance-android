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


import java.util.HashMap;
import java.util.Map;

final class AssuranceConstants {
    static final String VENDOR_ASSURANCE_MOBILE = "com.adobe.griffon.mobile";

    static final class DeeplinkURLKeys {
        static final String START_URL_QUERY_KEY_SESSION_ID = "adb_validation_sessionid";
        static final String START_URL_QUERY_KEY_ENVIRONMENT = "env";

        private DeeplinkURLKeys() {}
    }

    static final class GenericEventPayloadKey {
        static final String ACP_EXTENSION_EVENT_TYPE = "ACPExtensionEventType";
        static final String ACP_EXTENSION_EVENT_SOURCE = "ACPExtensionEventSource";
        static final String ACP_EXTENSION_EVENT_NAME = "ACPExtensionEventName";
        static final String ACP_EXTENSION_EVENT_DATA = "ACPExtensionEventData";
        static final String ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER =
                "ACPExtensionEventUniqueIdentifier";
        static final String ACP_EXTENSION_EVENT_NUMBER = "ACPExtensionEventNumber";

        private GenericEventPayloadKey() {}
    }

    static final class SDKEventDataKey {
        static final String START_SESSION_URL = "startSessionURL";
        static final String EXTENSIONS = "extensions";
        static final String STATE_OWNER = "stateowner";
        static final String FRIENDLY_NAME = "friendlyName";

        private SDKEventDataKey() {}
    }

    static final class SDKEventName {
        static final String XDM_SHARED_STATE_CHANGE = "Shared state change (XDM)";
        static final String SHARED_STATE_CHANGE = "Shared state change";

        private SDKEventName() {}
    }

    static final class SDKSharedStateName {
        static final String CONFIGURATION = "com.adobe.module.configuration";
        static final String EVENTHUB = "com.adobe.module.eventhub";

        private SDKSharedStateName() {}
    }

    static final class SDKConfigurationKey {
        static final String ORG_ID = "experienceCloud.org";

        private SDKConfigurationKey() {}
    }

    static final class AssuranceEventType {
        static final String GENERIC = "generic";
        static final String LOG = "log";
        static final String CONTROL = "control";
        static final String CLIENT = "client";
        static final String BLOB = "blob";

        private AssuranceEventType() {}
    }

    static final class ControlType {
        static final String START_EVENT_FORWARDING = "startEventForwarding";
        static final String SCREENSHOT = "screenshot";
        static final String LOG_FORWARDING = "logForwarding";
        static final String FAKE_EVENT = "fakeEvent";
        static final String CONFIG_UPDATE = "configUpdate";
        static final String NONE = "none";
        static final String WILDCARD = "wildcard";

        private ControlType() {}
    }

    static final class AssuranceEventKeys {
        static final String EVENT_ID = "eventID";
        static final String VENDOR = "vendor";
        static final String TYPE = "type";
        static final String PAYLOAD = "payload";
        static final String METADATA = "metadata";
        static final String TIMESTAMP = "timestamp";
        static final String EVENT_NUMBER = "eventNumber";
        static final String CHUNK_DATA = "chunkData";
        static final String CHUNK_ID = "chunkId";
        static final String CHUNK_TOTAL = "chunkTotal";
        static final String CHUNK_SEQUENCE_NUMBER = "chunkSequenceNumber";

        private AssuranceEventKeys() {}
    }

    static final class SharedStateKeys {
        static final String ASSURANCE_STATE_SESSION_ID = "sessionid";
        static final String ASSURANCE_STATE_CLIENT_ID = "clientid";
        static final String ASSURANCE_STATE_INTEGRATION_ID = "integrationid";

        private SharedStateKeys() {}
    }

    static final class DataStoreKeys {
        static final String DATASTORE_NAME = "com.adobe.assurance.preferences";
        static final String SESSION_URL = "reconnection.url";
        static final String ENVIRONMENT = "environment";
        static final String CLIENT_ID = "clientid";
        static final String SESSION_ID = "sessionid";
        static final String TOKEN = "token";

        private DataStoreKeys() {}
    }

    static final class BlobKeys {
        static final String UPLOAD_ENDPOINT_FORMAT = "https://blob%s.griffon.adobe.com";
        static final String UPLOAD_PATH_API = "api";
        static final String UPLOAD_PATH_FILEUPLOAD = "FileUpload";
        static final String UPLOAD_QUERY_KEY = "validationSessionId";
        static final String UPLOAD_HTTP_METHOD = "POST";
        static final String UPLOAD_HEADER_KEY_CONTENT_TYPE = "Content-Type";
        static final String UPLOAD_HEADER_KEY_FILE_CONTENT_TYPE = "File-Content-Type";
        static final String UPLOAD_HEADER_KEY_CONTENT_LENGTH = "Content-Length";
        static final String UPLOAD_HEADER_KEY_ACCEPT = "Accept";
        static final String RESPONSE_KEY_BLOB_ID = "id";
        static final String RESPONSE_KEY_ERROR = "error";

        private BlobKeys() {}
    }

    static final class PayloadDataKeys {
        static final String ANALYTICS_DEBUG_API_ENABLED = "analytics.debugApiEnabled";
        static final String STATE_DATA = "state.data";
        static final String XDM_STATE_DATA = "xdm.state.data";
        static final String METADATA = "metadata";
        static final String TYPE = "type";
        static final String DETAIL = "detail";

        private PayloadDataKeys() {}
    }

    static final class ClientInfoKeys {
        static final String VERSION = "version";
        static final String DEVICE_INFO = "deviceInfo";
        static final String APP_SETTINGS = "appSettings";

        private ClientInfoKeys() {}
    }

    static final class DeviceInfoKeys {
        static final String PLATFORM_NAME = "Canonical platform name";
        static final String DEVICE_NAME = "Device name";
        static final String DEVICE_MANUFACTURER = "Device manufacturer";
        static final String OPERATING_SYSTEM = "Operating system";
        static final String CARRIER_NAME = "Carrier name";
        static final String DEVICE_TYPE = "Device type";
        static final String MODEL = "Model";
        static final String SCREEN_SIZE = "Screen size";
        static final String LOCATION_SERVICE_ENABLED = "Location service enabled";
        static final String LOCATION_AUTHORIZATION_STATUS = "Location authorization status";
        static final String LOW_POWER_BATTERY_ENABLED = "Low power mode enabled";
        static final String BATTERY_LEVEL = "Battery level";
    }

    static final class SocketURLKeys {
        static final String SESSION_ID = "sessionId";
        static final String CLIENT_ID = "clientId";
        static final String ORG_ID = "orgId";
        static final String TOKEN = "token";
    }

    static final class SocketCloseCode {
        /**
         * Represents an the result of a successful intentional closure. Example: the user clicking
         * the disconnect button in the UI.
         */
        static final int NORMAL = 1000;

        /** Represents a class of unexpected errors. */
        static final int ABNORMAL = 1006;

        /**
         * Represents a client error during socket connection resulting due the socket connection
         * specifying an incorrect organization identifier.
         */
        static final int ORG_MISMATCH = 4900;

        /**
         * Represents a client error during socket connection Example: If clientInfoEvent is not the
         * first event to socket or, if there are any missing parameters in the socket URL.
         */
        static final int CLIENT_ERROR = 4400;

        /**
         * Represents an error resulting due to the number of connections per session exceeding the
         * default value - 200.
         */
        static final int CONNECTION_LIMIT = 4901;

        /**
         * Represents an error resulting due to the number of Assurance events sent per minute. The
         * default limit is 10k events per minute.
         */
        static final int EVENT_LIMIT = 4902;

        /**
         * Represents an error resulting due to the client attempting to connect to a session that
         * has been deleted (session may have been deleted manually, or due to exceeding the 30 day
         * limit)
         */
        static final int SESSION_DELETED = 4903;

        private SocketCloseCode() {}
    }

    static final class IntentExtraKey {
        static final String ERROR_NAME = "errorName";
        static final String ERROR_DESCRIPTION = "errorDescription";

        private IntentExtraKey() {}
    }

    // ========================================================================================
    // Enums
    // ========================================================================================

    enum AssuranceSocketError {
        GENERIC_ERROR(
                "Connection Error",
                "The connection may be failing due to a network issue or an incorrect PIN. "
                        + "Please verify internet connectivity or the PIN and try again."),
        NO_ORGID(
                "Invalid Configuration",
                "The Experience Cloud organization identifier is unavailable from the SDK. Ensure"
                    + " SDK configuration is setup correctly. See documentation for more detail."),
        ORGID_MISMATCH(
                "Unauthorized Access",
                "The Experience Cloud organization identifier does not match with that of the"
                    + " Assurance session. Ensure the right Experience Cloud organization is being"
                    + " used. See documentation for more detail."),
        CONNECTION_LIMIT(
                "Connection Limit Reached",
                "You have reached the maximum number of connected devices allowed for a session. "
                        + "Please disconnect another device and try again."),
        EVENT_LIMIT(
                "Event Limit Reached",
                "You have reached the maximum number of events that can be sent per minute."),
        CLIENT_ERROR(
                "Client Disconnected",
                "This client has been disconnected due to an unexpected error. Error Code 4400."),
        SESSION_DELETED(
                "Session Deleted",
                "The session client connected to has been deleted. Error Code 4903.");

        private final String error;
        private final String errorDescription;

        private AssuranceSocketError(final String error, final String description) {
            this.error = error;
            this.errorDescription = description;
        }

        String getErrorDescription() {
            return errorDescription;
        }

        String getError() {
            return error;
        }

        @Override
        public String toString() {
            return error + ": " + errorDescription;
        }
    }

    enum UILogColorVisibility {
        LOW(0),
        NORMAL(1),
        HIGH(2),
        CRITICAL(3);
        private final int intValue;

        UILogColorVisibility(final int val) {
            this.intValue = val;
        }

        public int getValue() {
            return intValue;
        }
    }

    enum AssuranceEnvironment {
        PROD("prod"),
        STAGE("stage"),
        QA("qa"),
        DEV("dev");

        private final String environmentString;

        /**
         * Returns {@link AssuranceEnvironment} value of the provided string.
         *
         * <p>Returns null if the provided string is not a valid {@link AssuranceEnvironment} enum
         * value
         *
         * @return {@link AssuranceEnvironment} value for provided status string
         */
        AssuranceEnvironment(final String environmentString) {
            this.environmentString = environmentString;
        }

        public String stringValue() {
            return environmentString;
        }

        /**
         * Returns {@link AssuranceEnvironment} enum value for the provided string.
         *
         * <p>Defaults to PROD if the provided string is not a valid {@link AssuranceEnvironment}
         * enum value
         *
         * @return {@link AssuranceEnvironment} value for provided status string
         */
        public static AssuranceEnvironment get(final String statusString) {
            AssuranceEnvironment enumValue = lookup.get(statusString);
            return (enumValue == null) ? AssuranceEnvironment.PROD : enumValue;
        }

        // generate look up table on load time
        private static final Map<String, AssuranceEnvironment> lookup = new HashMap<>();

        static {
            for (AssuranceEnvironment env : AssuranceEnvironment.values()) {
                lookup.put(env.stringValue(), env);
            }
        }
    }
}
