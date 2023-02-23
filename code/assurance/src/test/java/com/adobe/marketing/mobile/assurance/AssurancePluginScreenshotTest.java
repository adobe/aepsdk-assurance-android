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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AssurancePluginScreenshotTest {

    private static final String PAYLOAD_BLOBID = "blobId";
    private static final String PAYLOAD_MIMETYPE = "mimeType";
    private static final String PAYLOAD_ERROR = "error";

    private MockedStatic<AssuranceBlob> mockedStaticAssuranceBlob;
    private AssurancePluginScreenshot assurancePluginScreenshot;
    private AssuranceSession mockSession;
    private AssuranceEvent mockAssuranceEvent;

    @Before
    public void testSetup() {
        mockSession = Mockito.mock(AssuranceSession.class);
        mockAssuranceEvent = Mockito.mock(AssuranceEvent.class);
        mockedStaticAssuranceBlob = Mockito.mockStatic(AssuranceBlob.class);

        // create plugin instance to test
        assurancePluginScreenshot = new AssurancePluginScreenshot();
        assurancePluginScreenshot.onRegistered(mockSession);
    }

    @Test
    public void test_getVendorName() {
        // test
        String vendor = assurancePluginScreenshot.getVendor();
        assertEquals(vendor, AssuranceTestConstants.VENDOR_ASSURANCE_MOBILE);
    }

    @Test
    public void test_getControlType() {
        // test
        String vendor = assurancePluginScreenshot.getControlType();
        assertEquals(vendor, AssuranceTestConstants.ControlType.SCREENSHOT);
    }

    @Test
    public void test_OnRegister() {
        // test
        assurancePluginScreenshot.onRegistered(mockSession);

        // verify
        assertEquals(mockSession, assurancePluginScreenshot.getParentSession());
    }

    @Test
    public void test_SessionDisconnected() {
        // test
        assurancePluginScreenshot.onRegistered(mockSession);

        // verify
        assertNull(assurancePluginScreenshot.getCaptureScreenShotListener());
    }

    @Test
    public void test_noOpMethods_ShouldNotCrash() {
        // test
        assurancePluginScreenshot.onSessionConnected();
        assurancePluginScreenshot.onSessionDisconnected(0);
    }

    @Test
    public void test_onTakeScreenShotEventReceived() {
        // prepare
        assurancePluginScreenshot.onRegistered(mockSession);

        // test
        assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);
        AssurancePluginScreenshot.CaptureScreenShotListener listener =
                assurancePluginScreenshot.getCaptureScreenShotListener();
        listener.onCaptureScreenshot(sampleBitMapImage());

        // verify upload method call
        mockedStaticAssuranceBlob.verify(
                () ->
                        AssuranceBlob.upload(
                                any(byte[].class),
                                anyString(),
                                any(AssuranceSession.class),
                                any(AssuranceBlob.BlobUploadCallback.class)),
                times(1));
    }

    @Test
    public void test_onSuccessful_ScreenShotUpload() {
        // prepare
        assurancePluginScreenshot.onRegistered(mockSession);
        final ArgumentCaptor<AssuranceBlob.BlobUploadCallback> assuranceBlobCallbackCaptor =
                ArgumentCaptor.forClass(AssuranceBlob.BlobUploadCallback.class);
        final ArgumentCaptor<AssuranceEvent> assuranceEventCaptor =
                ArgumentCaptor.forClass(AssuranceEvent.class);

        // test
        assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);
        AssurancePluginScreenshot.CaptureScreenShotListener listener =
                assurancePluginScreenshot.getCaptureScreenShotListener();
        listener.onCaptureScreenshot(sampleBitMapImage());

        // verify upload method call
        mockedStaticAssuranceBlob.verify(
                () ->
                        AssuranceBlob.upload(
                                any(byte[].class),
                                anyString(),
                                any(AssuranceSession.class),
                                assuranceBlobCallbackCaptor.capture()),
                times(1));

        // test 2 - Call Success callback
        assuranceBlobCallbackCaptor.getValue().onSuccess("sampleBlobID");

        // verify if screenshot event is queued
        verify(mockSession, times(1))
                .logLocalUI(AssuranceConstants.UILogColorVisibility.LOW, "Screenshot taken");
        verify(mockSession, times(1)).queueOutboundEvent(assuranceEventCaptor.capture());
        AssuranceEvent queuedEvent = assuranceEventCaptor.getValue();
        assertNotNull(queuedEvent);
        assertEquals(AssuranceTestConstants.AssuranceEventType.BLOB, queuedEvent.type);
        assertEquals("sampleBlobID", queuedEvent.payload.get(PAYLOAD_BLOBID));
        assertEquals("image/png", queuedEvent.payload.get(PAYLOAD_MIMETYPE));
    }

    @Test
    public void test_onFailure_ToUploadScreenShot() {
        // prepare
        assurancePluginScreenshot.onRegistered(mockSession);
        final ArgumentCaptor<AssuranceBlob.BlobUploadCallback> assuranceBlobCallbackCaptor =
                ArgumentCaptor.forClass(AssuranceBlob.BlobUploadCallback.class);
        final ArgumentCaptor<AssuranceEvent> assuranceEventCaptor =
                ArgumentCaptor.forClass(AssuranceEvent.class);

        // test
        assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);
        AssurancePluginScreenshot.CaptureScreenShotListener listener =
                assurancePluginScreenshot.getCaptureScreenShotListener();
        listener.onCaptureScreenshot(sampleBitMapImage());

        // verify upload method call
        mockedStaticAssuranceBlob.verify(
                () ->
                        AssuranceBlob.upload(
                                any(byte[].class),
                                anyString(),
                                any(AssuranceSession.class),
                                assuranceBlobCallbackCaptor.capture()),
                times(1));

        // test 2 - Call Failure callback
        assuranceBlobCallbackCaptor.getValue().onFailure("give no reason");

        // verify if screenshot failure event is queued
        verify(mockSession, times(1))
                .logLocalUI(
                        AssuranceConstants.UILogColorVisibility.LOW,
                        "Error while taking screenshot - Description: give no reason");
        verify(mockSession, times(1)).queueOutboundEvent(assuranceEventCaptor.capture());
        AssuranceEvent queuedEvent = assuranceEventCaptor.getValue();
        assertNotNull(queuedEvent);
        assertEquals(AssuranceTestConstants.AssuranceEventType.BLOB, queuedEvent.type);
        assertEquals("", queuedEvent.payload.get(PAYLOAD_BLOBID));
        assertEquals("give no reason", queuedEvent.payload.get(PAYLOAD_ERROR));
    }

    @After
    public void teardown() {
        mockedStaticAssuranceBlob.close();
    }

    private Bitmap sampleBitMapImage() {
        int width = 200;
        int height = 100;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);

        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTextSize(14.f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Hello Android!", (width / 2.f), (height / 2.f), paint);
        return bitmap;
    }
}
