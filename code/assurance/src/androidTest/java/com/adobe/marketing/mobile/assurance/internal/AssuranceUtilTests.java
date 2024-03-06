/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal;

import android.net.Uri;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link AssuranceUtil} that involve android specific constructs like {@link
 * android.net.Uri} that cannot be mocked.
 */
public class AssuranceUtilTests {

    @Test
    public void testGetEnvironmentFromSocketUri() {
        final Uri devSocketUri = Uri.parse("wss://connect-dev.griffon.adobe.com/client/v1");
        Assert.assertEquals(
                AssuranceConstants.AssuranceEnvironment.DEV,
                AssuranceUtil.getEnvironmentFromSocketUri(devSocketUri));

        final Uri qaSocketUri = Uri.parse("wss://connect-qa.griffon.adobe.com/client/v1");
        Assert.assertEquals(
                AssuranceConstants.AssuranceEnvironment.QA,
                AssuranceUtil.getEnvironmentFromSocketUri(qaSocketUri));

        final Uri stageSocketUri = Uri.parse("wss://connect-stage.griffon.adobe.com/client/v1");

        Assert.assertEquals(
                AssuranceConstants.AssuranceEnvironment.STAGE,
                AssuranceUtil.getEnvironmentFromSocketUri(stageSocketUri));

        final Uri prodSocketUri = Uri.parse("wss://connect.griffon.adobe.com/client/v1");
        Assert.assertEquals(
                AssuranceConstants.AssuranceEnvironment.PROD,
                AssuranceUtil.getEnvironmentFromSocketUri(prodSocketUri));
    }

    @Test
    public void testGetEnvironmentFromSocketUri_InvalidURI() {

        final Uri invalidSocketUri = Uri.parse("wss://invalidconnect.griffon.adobe.com/client/v1");
        Assert.assertEquals(
                AssuranceConstants.AssuranceEnvironment.PROD,
                AssuranceUtil.getEnvironmentFromSocketUri(invalidSocketUri));

        final Uri invalidFormatUri = Uri.parse("not:://a_valid_uri");
        Assert.assertEquals(
                AssuranceConstants.AssuranceEnvironment.PROD,
                AssuranceUtil.getEnvironmentFromSocketUri(invalidFormatUri));
    }
}
