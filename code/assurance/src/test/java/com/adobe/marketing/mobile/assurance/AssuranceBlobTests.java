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

import static junit.framework.TestCase.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AssuranceBlobTests {
    @Mock AssuranceSession mockSession;

    private static final String SAMPLE_CONTENT_TYPE = "image/jpeg";
    private static final byte[] SAMPLE_BYTE_ARRAY = "SecretString".getBytes();
    private static final String SAMPLE_SESSION_ID = "sessionId";
    private boolean onSuccessCallbackCalled = false;
    private String onSuccessBlobId;
    private boolean onFailureCallbackCalled = false;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        Mockito.when(mockSession.getAssuranceEnvironment())
                .thenReturn(AssuranceConstants.AssuranceEnvironment.PROD);
        Mockito.when(mockSession.getSessionId()).thenReturn(SAMPLE_SESSION_ID);
    }

    @Test
    public void test_uploadBlob_whenNullData() throws Exception {
        // test
        CountDownLatch latch = new CountDownLatch(1);
        AssuranceBlob.upload(null, SAMPLE_CONTENT_TYPE, mockSession, createTestableCallback(latch));

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));

        // verify
        assertTrue(onFailureCallbackCalled);
    }

    @Test
    public void test_uploadBlob_whenNullSession() throws Exception {
        // test
        CountDownLatch latch = new CountDownLatch(1);
        AssuranceBlob.upload(
                SAMPLE_BYTE_ARRAY, SAMPLE_CONTENT_TYPE, null, createTestableCallback(latch));

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));

        // verify
        assertTrue(onFailureCallbackCalled);
    }

    private AssuranceBlob.BlobUploadCallback createTestableCallback(final CountDownLatch latch) {
        onSuccessCallbackCalled = false;
        onFailureCallbackCalled = false;
        onSuccessBlobId = null;
        return new AssuranceBlob.BlobUploadCallback() {
            @Override
            public void onSuccess(String blobID) {
                onSuccessCallbackCalled = true;
                onSuccessBlobId = blobID;
                latch.countDown();
            }

            @Override
            public void onFailure(String reason) {
                onFailureCallbackCalled = true;
                latch.countDown();
            }
        };
    }
}
