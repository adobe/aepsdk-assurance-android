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

public class AssuranceTestConstants {
    static final String VENDOR_ASSURANCE_MOBILE = "com.adobe.griffon.mobile";

    final class GenericEventPayloadKey {
        static final String ACP_EXTENSION_EVENT_TYPE = "ACPExtensionEventType";
        static final String ACP_EXTENSION_EVENT_SOURCE = "ACPExtensionEventSource";
        static final String ACP_EXTENSION_EVENT_NAME = "ACPExtensionEventName";
        static final String ACP_EXTENSION_EVENT_DATA = "ACPExtensionEventData";
        static final String ACP_EXTENSION_EVENT_UNIQUE_IDENTIFIER =
                "ACPExtensionEventUniqueIdentifier";
        static final String ACP_EXTENSION_EVENT_NUMBER = "ACPExtensionEventNumber";

        private GenericEventPayloadKey() {}
    }

    final class SDKSharedStateName {
        static final String CONFIGURATION = "com.adobe.module.configuration";

        private SDKSharedStateName() {}
    }

    final class SDKConfigurationKey {
        static final String ORG_ID = "experienceCloud.org";

        private SDKConfigurationKey() {}
    }

    final class AssuranceEventType {
        static final String GENERIC = "generic";
        static final String LOG = "log";
        static final String CONTROL = "control";
        static final String CLIENT = "client";
        static final String BLOB = "blob";

        private AssuranceEventType() {}
    }

    final class ControlType {
        static final String START_EVENT_FORWARDING = "startEventForwarding";
        static final String SCREENSHOT = "screenshot";
        static final String LOG_FORWARDING = "logForwarding";
        static final String FAKE_EVENT = "fakeEvent";
        static final String CONFIG_UPDATE = "configUpdate";
        static final String NONE = "none";
        static final String WILDCARD = "wildcard";

        private ControlType() {}
    }

    final class SharedStateKeys {
        static final String ASSURANCE_STATE_SESSION_ID = "sessionid";
        static final String ASSURANCE_STATE_CLIENT_ID = "clientid";
        static final String ASSURANCE_STATE_INTEGRATION_ID = "integrationid";

        private SharedStateKeys() {}
    }

    final class PayloadDataKeys {
        static final String ANALYTICS_DEBUG_API_ENABLED = "analytics.debugApiEnabled";
        static final String XDM_STATE_DATA = "xdm.state.data";
        static final String STATE_CONTENTS = "state.data";
        static final String METADATA = "metadata";
        static final String TYPE = "type";
        static final String DETAIL = "detail";

        private PayloadDataKeys() {}
    }
}
