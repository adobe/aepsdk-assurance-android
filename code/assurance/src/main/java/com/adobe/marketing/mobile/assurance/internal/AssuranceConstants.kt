/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal

import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.services.ServiceProvider
import java.util.concurrent.TimeUnit

internal object AssuranceConstants {
    const val VENDOR_ASSURANCE_MOBILE = "com.adobe.griffon.mobile"

    internal object DeeplinkURLKeys {
        const val START_URL_QUERY_KEY_SESSION_ID = "adb_validation_sessionid"
        const val START_URL_QUERY_KEY_ENVIRONMENT = "env"
    }

    internal object GenericEventPayloadKey {
        const val ACP_EXTENSION_EVENT_TYPE = "ACPExtensionEventType"
        const val ACP_EXTENSION_EVENT_SOURCE = "ACPExtensionEventSource"
        const val ACP_EXTENSION_EVENT_NAME = "ACPExtensionEventName"
        const val ACP_EXTENSION_EVENT_DATA = "ACPExtensionEventData"
        const val ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER = "ACPExtensionEventUniqueIdentifier"
        const val ACP_EXTENSION_EVENT_PARENT_IDENTIFIER = "ACPExtensionEventParentIdentifier"
    }

    internal object SDKEventDataKey {
        const val START_SESSION_URL = "startSessionURL"
        const val IS_QUICK_CONNECT = "quickConnect"
        const val END_SESSION = "endSession"
        const val EXTENSIONS = "extensions"
        const val STATE_OWNER = "stateowner"
        const val FRIENDLY_NAME = "friendlyName"
    }

    internal object SDKEventName {
        const val XDM_SHARED_STATE_CHANGE = "Shared state change (XDM)"
        const val SHARED_STATE_CHANGE = "Shared state change"
    }

    internal object SDKSharedStateName {
        const val CONFIGURATION = "com.adobe.module.configuration"
        const val EVENTHUB = "com.adobe.module.eventhub"
    }

    internal object SDKConfigurationKey {
        const val ORG_ID = "experienceCloud.org"
    }

    internal object AssuranceEventType {
        const val GENERIC = "generic"
        const val LOG = "log"
        const val CONTROL = "control"
        const val CLIENT = "client"
        const val BLOB = "blob"
    }

    internal object ControlType {
        const val START_EVENT_FORWARDING = "startEventForwarding"
        const val SCREENSHOT = "screenshot"
        const val LOG_FORWARDING = "logForwarding"
        const val FAKE_EVENT = "fakeEvent"
        const val CONFIG_UPDATE = "configUpdate"
        const val NONE = "none"
        const val WILDCARD = "wildcard"
    }

    internal object AssuranceEventKeys {
        const val EVENT_ID = "eventID"
        const val VENDOR = "vendor"
        const val TYPE = "type"
        const val PAYLOAD = "payload"
        const val METADATA = "metadata"
        const val TIMESTAMP = "timestamp"
        const val EVENT_NUMBER = "eventNumber"
        const val CHUNK_DATA = "chunkData"
        const val CHUNK_ID = "chunkId"
        const val CHUNK_TOTAL = "chunkTotal"
        const val CHUNK_SEQUENCE_NUMBER = "chunkSequenceNumber"
    }

    internal object SharedStateKeys {
        const val ASSURANCE_STATE_SESSION_ID = "sessionid"
        const val ASSURANCE_STATE_CLIENT_ID = "clientid"
        const val ASSURANCE_STATE_INTEGRATION_ID = "integrationid"
    }

    internal object DataStoreKeys {
        const val DATASTORE_NAME = "com.adobe.assurance.preferences"
        const val SESSION_URL = "reconnection.url"
        const val ENVIRONMENT = "environment"
        const val CLIENT_ID = "clientid"
        const val SESSION_ID = "sessionid"
        const val TOKEN = "token"
    }

    internal object BlobKeys {
        const val UPLOAD_ENDPOINT_FORMAT = "https://blob%s.griffon.adobe.com"
        const val UPLOAD_PATH_API = "api"
        const val UPLOAD_PATH_FILEUPLOAD = "FileUpload"
        const val UPLOAD_QUERY_KEY = "validationSessionId"
        const val UPLOAD_HTTP_METHOD = "POST"
        const val UPLOAD_HEADER_KEY_CONTENT_TYPE = "Content-Type"
        const val UPLOAD_HEADER_KEY_FILE_CONTENT_TYPE = "File-Content-Type"
        const val UPLOAD_HEADER_KEY_CONTENT_LENGTH = "Content-Length"
        const val UPLOAD_HEADER_KEY_ACCEPT = "Accept"
        const val RESPONSE_KEY_BLOB_ID = "id"
        const val RESPONSE_KEY_ERROR = "error"
    }

    internal object PayloadDataKeys {
        const val ANALYTICS_DEBUG_API_ENABLED = "analytics.debugApiEnabled"
        const val STATE_DATA = "state.data"
        const val XDM_STATE_DATA = "xdm.state.data"
        const val METADATA = "metadata"
        const val TYPE = "type"
        const val DETAIL = "detail"
    }

    internal object ClientInfoKeys {
        const val VERSION = "version"
        const val DEVICE_INFO = "deviceInfo"
        const val APP_SETTINGS = "appSettings"
    }

    internal object DeviceInfoKeys {
        const val PLATFORM_NAME = "Canonical platform name"
        const val DEVICE_NAME = "Device name"
        const val DEVICE_MANUFACTURER = "Device manufacturer"
        const val OPERATING_SYSTEM = "Operating system"
        const val CARRIER_NAME = "Carrier name"
        const val DEVICE_TYPE = "Device type"
        const val MODEL = "Model"
        const val SCREEN_SIZE = "Screen size"
        const val LOCATION_SERVICE_ENABLED = "Location service enabled"
        const val LOCATION_AUTHORIZATION_STATUS = "Location authorization status"
        const val LOW_POWER_BATTERY_ENABLED = "Low power mode enabled"
        const val BATTERY_LEVEL = "Battery level"
    }

    internal object SocketURLKeys {
        const val SESSION_ID = "sessionId"
        const val CLIENT_ID = "clientId"
        const val ORG_ID = "orgId"
        const val TOKEN = "token"
    }

    internal object SocketCloseCode {
        /**
         * Represents an the result of a successful intentional closure. Example: the user clicking
         * the disconnect button in the UI.
         */
        const val NORMAL = 1000

        /** Represents a class of unexpected errors.  */
        const val ABNORMAL = 1006

        /**
         * Represents a client error during socket connection resulting due the socket connection
         * specifying an incorrect organization identifier.
         */
        const val ORG_MISMATCH = 4900

        /**
         * Represents a client error during socket connection Example: If clientInfoEvent is not the
         * first event to socket or, if there are any missing parameters in the socket URL.
         */
        const val CLIENT_ERROR = 4400

        /**
         * Represents an error resulting due to the number of connections per session exceeding the
         * default value - 200.
         */
        const val CONNECTION_LIMIT = 4901

        /**
         * Represents an error resulting due to the number of Assurance events sent per minute. The
         * default limit is 10k events per minute.
         */
        const val EVENT_LIMIT = 4902

        /**
         * Represents an error resulting due to the client attempting to connect to a session that
         * has been deleted (session may have been deleted manually, or due to exceeding the 30 day
         * limit)
         */
        const val SESSION_DELETED = 4903

        /**
         * Converts a socket close code to an `AssuranceConnectionError` if such a mapping
         * exists. Not all socket close codes are error codes and not all AssuranceConnectionErrors
         * are socket errors. So this utility is needed to bridge socket codes and
         * AssuranceConnectionError.
         *
         * @param closeCode a socket close code for which an AssuranceConnectionError is needed
         * @return an `AssuranceConnectionError`
         */
        @JvmStatic
        fun toAssuranceConnectionError(closeCode: Int): AssuranceConnectionError? {
            return when (closeCode) {
                ORG_MISMATCH -> AssuranceConnectionError.ORG_ID_MISMATCH
                CLIENT_ERROR -> AssuranceConnectionError.CLIENT_ERROR
                CONNECTION_LIMIT -> AssuranceConnectionError.CONNECTION_LIMIT
                EVENT_LIMIT -> AssuranceConnectionError.EVENT_LIMIT
                SESSION_DELETED -> AssuranceConnectionError.SESSION_DELETED
                ABNORMAL -> AssuranceConnectionError.GENERIC_ERROR
                else -> null
            }
        }
    }

    internal object QuickConnect {
        const val BASE_DEVICE_API_URL = "https://device.griffon.adobe.com/device"
        const val DEVICE_API_PATH_CREATE = "create"
        const val DEVICE_API_PATH_STATUS = "status"
        const val KEY_SESSION_ID = "sessionUuid"
        const val KEY_SESSION_TOKEN = "token"
        const val KEY_ORG_ID = "orgId"
        const val KEY_DEVICE_NAME = "deviceName"
        const val KEY_CLIENT_ID = "clientId"
        val CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5).toInt()
        val READ_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5).toInt()
        val STATUS_CHECK_DELAY_MS = TimeUnit.SECONDS.toMillis(2)
        const val MAX_RETRY_COUNT = 300
    }

    internal enum class AssuranceConnectionError(
        private val errorResId: Int,
        private val descriptionResId: Int,
        @JvmField val isRetryable: Boolean
    ) {
        GENERIC_ERROR(
            R.string.error_title_incorrect_pin_or_network,
            R.string.error_desc_incorrect_pin_or_network,
            true
        ),
        NO_ORG_ID(
            R.string.error_title_invalid_org_id,
            R.string.error_desc_invalid_org_id,
            false
        ),
        ORG_ID_MISMATCH(
            R.string.error_title_unauthorized_access,
            R.string.error_desc_unauthorized_access,
            false
        ),
        CONNECTION_LIMIT(
            R.string.error_title_connection_limit,
            R.string.error_desc_connection_limit,
            false
        ),
        EVENT_LIMIT(
            R.string.error_title_event_limit,
            R.string.error_desc_event_limit,
            false
        ),
        CLIENT_ERROR(
            R.string.error_title_unexpected_error,
            R.string.error_desc_unexpected_error,
            false
        ),
        SESSION_DELETED(
            R.string.error_title_session_deleted,
            R.string.error_desc_session_deleted,
            false
        ),
        CREATE_DEVICE_REQUEST_MALFORMED(
            R.string.error_title_invalid_registration_request,
            R.string.error_desc_invalid_registration_request,
            false
        ),
        STATUS_CHECK_REQUEST_MALFORMED(
            R.string.error_title_invalid_registration_request,
            R.string.error_desc_invalid_registration_request,
            false
        ),
        RETRY_LIMIT_REACHED(
            R.string.error_title_retry_limit_reached,
            R.string.error_desc_retry_limit_reached,
            true
        ),
        CREATE_DEVICE_REQUEST_FAILED(
            R.string.error_title_registration_error,
            R.string.error_desc_registration_error,
            true
        ),
        DEVICE_STATUS_REQUEST_FAILED(
            R.string.error_title_registration_error,
            R.string.error_desc_registration_error,
            true
        ),
        UNEXPECTED_ERROR(
            R.string.error_title_invalid_registration_response,
            R.string.error_desc_invalid_registration_response,
            true
        );

        val error: String
            get() {
                val context = ServiceProvider.getInstance().appContextService.applicationContext
                // This code path should never be crossed if the app context is null because the SDK
                // operations require a valid context. Default to empty string if context is null
                // because we don't want to crash the app.
                return context?.getString(errorResId) ?: ""
            }

        val description: String
            get() {
                val context = ServiceProvider.getInstance().appContextService.applicationContext
                // This code path should never be crossed if the app context is null because the SDK
                // operations require a valid context. Default to empty string if context is null
                // because we don't want to crash the app.
                return context?.getString(descriptionResId) ?: ""
            }
    }

    internal enum class UILogColorVisibility(val value: Int) {
        LOW(0),
        NORMAL(1),
        HIGH(2),
        CRITICAL(3)
    }

    /**
     * Returns [AssuranceEnvironment] value of the provided string.
     *
     *
     * Returns null if the provided string is not a valid [AssuranceEnvironment] enum
     * value
     *
     * @return [AssuranceEnvironment] value for provided status string
     */
    internal enum class AssuranceEnvironment(@JvmField val stringValue: String) {
        PROD("prod"),
        STAGE("stage"),
        QA("qa"),
        DEV("dev");

        /**
         * Returns [AssuranceEnvironment] enum value for the provided string.
         *
         * Defaults to PROD if the provided string is not a valid [AssuranceEnvironment]
         * enum value
         *
         * @return [AssuranceEnvironment] value for provided status string
         */
        internal companion object {
            private val lookup: Map<String, AssuranceEnvironment> = values().associateBy { it.stringValue }

            @JvmName("get")
            @JvmStatic
            internal operator fun get(stringValue: String): AssuranceEnvironment {
                val enumValue = lookup[stringValue]
                return enumValue ?: PROD
            }
        }
    }
}
