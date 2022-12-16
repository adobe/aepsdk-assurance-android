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

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AssuranceUtilTest {

    @Test
    public void test_getURLFormatForEnvironment_WhenNull() {
        assertEquals("", AssuranceUtil.getURLFormatForEnvironment(null));
    }

    @Test
    public void test_getURLFormatForEnvironment() {
        assertEquals(
                "",
                AssuranceUtil.getURLFormatForEnvironment(
                        AssuranceConstants.AssuranceEnvironment.PROD));
        assertEquals(
                "-stage",
                AssuranceUtil.getURLFormatForEnvironment(
                        AssuranceConstants.AssuranceEnvironment.STAGE));
        assertEquals(
                "-qa",
                AssuranceUtil.getURLFormatForEnvironment(
                        AssuranceConstants.AssuranceEnvironment.QA));
        assertEquals(
                "-dev",
                AssuranceUtil.getURLFormatForEnvironment(
                        AssuranceConstants.AssuranceEnvironment.DEV));
    }

    @Test
    public void test_getEnvironmentFromQueryValue_whenNullOrEmpty() {
        assertEquals(
                AssuranceConstants.AssuranceEnvironment.PROD,
                AssuranceUtil.getEnvironmentFromQueryValue(null));
        assertEquals(
                AssuranceConstants.AssuranceEnvironment.PROD,
                AssuranceUtil.getEnvironmentFromQueryValue(""));
    }

    @Test
    public void test_getEnvironmentFromQueryValue() {
        assertEquals(
                AssuranceConstants.AssuranceEnvironment.PROD,
                AssuranceUtil.getEnvironmentFromQueryValue("prod"));
        assertEquals(
                AssuranceConstants.AssuranceEnvironment.STAGE,
                AssuranceUtil.getEnvironmentFromQueryValue("stage"));
        assertEquals(
                AssuranceConstants.AssuranceEnvironment.QA,
                AssuranceUtil.getEnvironmentFromQueryValue("qa"));
        assertEquals(
                AssuranceConstants.AssuranceEnvironment.DEV,
                AssuranceUtil.getEnvironmentFromQueryValue("dev"));
        assertEquals(
                AssuranceConstants.AssuranceEnvironment.PROD,
                AssuranceUtil.getEnvironmentFromQueryValue("randomString"));
    }

    @Test
    public void test_getValidSessionIDFromUri() {
        assertEquals(
                "2a22cbdc-d705-4f1a-88b3-b73f7fe67a4e",
                AssuranceUtil.getValidSessionIDFromUri(
                        Uri.parse(
                                "baseball://?adb_validation_sessionid=2a22cbdc-d705-4f1a-88b3-b73f7fe67a4e")));
        assertNull(
                AssuranceUtil.getValidSessionIDFromUri(
                        Uri.parse(
                                "baseball://?adb_validation_sessionid=2-22cbdc-d705-4f1a-88b3-b73f7fe67a4e")));
        assertNull(
                AssuranceUtil.getValidSessionIDFromUri(
                        Uri.parse("baseball://?wrongKey=2a22cbdc-d705-4f1a-88b3-b73f7fe67a4e")));
        assertNull(
                AssuranceUtil.getValidSessionIDFromUri(
                        Uri.parse(
                                "baseball://?adb_validation_sessionid=2a22cbdc-d705-4f1a-88b3-b73f7fe67a4e2a22cbdc-d705-4f1a-88b3-b73f7fe67a4e")));
        assertNull(
                AssuranceUtil.getValidSessionIDFromUri(
                        Uri.parse(
                                "baseball://?adb_validation_sessionid=2a22cbdcd7054f1a88b3b73f7fe67a4e")));
        assertNull(AssuranceUtil.getValidSessionIDFromUri(Uri.parse("Invalid")));
    }

    @Test
    public void test_isSafe_connectionURL() {
        final String connectionURL =
                "wss://connect.griffon.adobe.com/client/v1?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5-SOME_RANDOM_CONTENT&"
                        + "token=9124&"
                        + "orgId=972C898555E9F7BC7F000101%40AdobeOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";

        assertFalse(AssuranceUtil.isSafe(connectionURL));
    }

    @Test
    public void test_isSafe_URLWithInvalidClientId() {
        final String connectionURL =
                "wss://connect.griffon.adobe.com/client/v1?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                        + "token=9124&"
                        + "orgId=972C898555E9F7BC7F000101%40AdobeOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C_SOME_RANDOM_CONTENT";

        assertFalse(AssuranceUtil.isSafe(connectionURL));
    }

    @Test
    public void test_isSafe_URLWithInvalidOrgId() {
        final String connectionURL =
                "wss://connect.griffon.adobe.com/client/v1?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                        + "token=9124&"
                        + "orgId=972C898555E9F7BC7F000101%40SomeRandomOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";

        assertFalse(AssuranceUtil.isSafe(connectionURL));
    }

    @Test
    public void test_isSafe_URLWithLongerToken() {
        final String connectionURL =
                "wss://connect.griffon.adobe.com/client/v1?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                        + "token=123456&"
                        + "orgId=972C898555E9F7BC7F000101%40AdobeOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";

        assertFalse(AssuranceUtil.isSafe(connectionURL));
    }

    @Test
    public void test_isSafe_URLWithNonNumericToken() {
        final String connectionURL =
                "wss://connect.griffon.adobe.com/client/v1?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                        + "token=HAHAHA&"
                        + "orgId=972C898555E9F7BC7F000101%40AdobeOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";

        assertFalse(AssuranceUtil.isSafe(connectionURL));
    }

    @Test
    public void test_isSafe_URLWithUnsupportedScheme() {
        final String connectionURL =
                "http://connect.griffon.adobe.com/client/v1?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                        + "token=1234&"
                        + "orgId=972C898555E9F7BC7F000101%40AdobeOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";

        assertFalse(AssuranceUtil.isSafe(connectionURL));
    }

    @Test
    public void test_isSafe_URLWithUnsupportedPath() {
        final String connectionURL =
                "wss://connect.griffon.adobe.com/client/v4?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                        + "token=1234&"
                        + "orgId=972C898555E9F7BC7F000101%40AdobeOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";

        assertFalse(AssuranceUtil.isSafe(connectionURL));
    }

    @Test
    public void test_isSafe_URLWithUnsupportedEnvironment() {
        final String connectionURL =
                "wss://connect-randomenv.griffon.adobe.com/client/v1?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                        + "token=1234&"
                        + "orgId=972C898555E9F7BC7F000101%40AdobeOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";

        assertFalse(AssuranceUtil.isSafe(connectionURL));
    }

    @Test
    public void test_isSafe_ValidURL() {
        final String connectionURLQA =
                "wss://connect-qa.griffon.adobe.com/client/v1?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                        + "token=1234&"
                        + "orgId=972C898555E9F7BC7F000101%40AdobeOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";

        assertTrue(AssuranceUtil.isSafe(connectionURLQA));

        final String connectionURLProd =
                "wss://connect.griffon.adobe.com/client/v1?"
                        + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                        + "token=1234&"
                        + "orgId=972C898555E9F7BC7F000101%40AdobeOrg&"
                        + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";
        assertTrue(AssuranceUtil.isSafe(connectionURLProd));
    }
}
