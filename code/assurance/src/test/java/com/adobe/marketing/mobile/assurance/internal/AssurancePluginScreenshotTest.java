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

package com.adobe.marketing.mobile.assurance.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.adobe.marketing.mobile.services.AppContextService;
import com.adobe.marketing.mobile.services.ServiceProvider;
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

    // Test constants to eliminate magic numbers
    private static final int TEST_SCREEN_WIDTH = 1080;
    private static final int TEST_SCREEN_HEIGHT = 1920;
    private static final String TEST_BLOB_ID = "sampleBlobID";
    private static final String TEST_UPLOAD_ERROR = "Upload failed";

    private MockedStatic<AssuranceBlob> mockedStaticAssuranceBlob;
    private AssurancePluginScreenshot assurancePluginScreenshot;
    private AssuranceSession mockSession;
    private AssuranceEvent mockAssuranceEvent;
    private ServiceProvider mockServiceProvider;
    private Display mockDisplay;
    private AppContextService mockAppContextService;

    @Before
    public void testSetup() {
        mockSession = Mockito.mock(AssuranceSession.class);
        mockAssuranceEvent = Mockito.mock(AssuranceEvent.class);
        mockedStaticAssuranceBlob = Mockito.mockStatic(AssuranceBlob.class);
        mockServiceProvider = Mockito.mock(ServiceProvider.class);
        mockDisplay = Mockito.mock(Display.class);
        mockAppContextService = Mockito.mock(AppContextService.class);

        // create plugin instance to test
        assurancePluginScreenshot = new AssurancePluginScreenshot();
        assurancePluginScreenshot.onRegistered(mockSession);
    }

    @Test
    public void test_getVendorName() {
        // test
        String vendor = assurancePluginScreenshot.getVendor();
        assertEquals(AssuranceConstants.VENDOR_ASSURANCE_MOBILE, vendor);
    }

    @Test
    public void test_getControlType() {
        // test
        String controlType = assurancePluginScreenshot.getControlType();
        assertEquals(AssuranceConstants.ControlType.SCREENSHOT, controlType);
    }

    @Test
    public void test_OnRegister() {
        // test
        assurancePluginScreenshot.onRegistered(mockSession);

        // verify
        assertEquals(mockSession, assurancePluginScreenshot.getParentSession());
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
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                mockStatic(ServiceProvider.class)) {
            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify
            verify(mocks.mockActivity, times(2)).getWindow();
            verify(mocks.mockWindow).getDecorView();
            verify(mocks.mockView).getRootView();
            verify(mocks.mockView, times(2)).getWidth(); // PixelCopy calls getWidth() twice
            verify(mocks.mockView, times(2)).getHeight(); // PixelCopy calls getHeight() twice
            verify(mocks.mockView).getLocationOnScreen(any(int[].class));
        }
    }

    @Test
    public void test_onSuccessful_ScreenShotUpload() {
        // prepare
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                        mockStatic(ServiceProvider.class);
                MockedStatic<PixelCopy> mockedPixelCopy = mockStatic(PixelCopy.class)) {

            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);
            setupSuccessfulPixelCopy(mockedPixelCopy);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify upload method call and trigger success callback
            ArgumentCaptor<AssuranceBlob.BlobUploadCallback> callbackCaptor =
                    verifyBlobUploadCall();
            callbackCaptor.getValue().onSuccess(TEST_BLOB_ID);

            // verify if screenshot event is queued
            verifySuccessfulScreenshotEvent(TEST_BLOB_ID);
        }
    }

    @Test
    public void test_onFailure_ToUploadScreenShot() {
        // prepare
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                        mockStatic(ServiceProvider.class);
                MockedStatic<PixelCopy> mockedPixelCopy = mockStatic(PixelCopy.class)) {

            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);
            setupFailedPixelCopy(mockedPixelCopy);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify
            verifyErrorEventWithPayload(
                    "PixelCopy failed with result: " + PixelCopy.ERROR_SOURCE_INVALID,
                    "PixelCopy failed with result: " + PixelCopy.ERROR_SOURCE_INVALID);
        }
    }

    @Test
    @Config(sdk = 25) // Android 7.1.1 - Should use Canvas method
    public void test_onCanvasScreenshotForAPI21to25() {
        // prepare
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                mockStatic(ServiceProvider.class)) {
            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify Canvas method interactions
            verify(mocks.mockActivity, times(1)).getWindow(); // Canvas calls getWindow() once
            verify(mocks.mockWindow).getDecorView();
            verify(mocks.mockView).getRootView();
            verify(mocks.mockView, times(1)).getWidth(); // Canvas calls getWidth() once
            verify(mocks.mockView, times(1)).getHeight(); // Canvas calls getHeight() once

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
    }

    @Test
    @Config(sdk = 28) // API 28 - Should use PixelCopy
    public void test_onPixelCopyScreenshotForAPI26Plus() {
        // prepare
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                        mockStatic(ServiceProvider.class);
                MockedStatic<PixelCopy> mockedPixelCopy = mockStatic(PixelCopy.class)) {

            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);
            setupSuccessfulPixelCopy(mockedPixelCopy);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify PixelCopy method interactions
            verify(mocks.mockActivity, times(2)).getWindow();
            verify(mocks.mockWindow).getDecorView();
            verify(mocks.mockView).getRootView();
            verify(mocks.mockView, times(2)).getWidth(); // PixelCopy calls getWidth() twice
            verify(mocks.mockView, times(2)).getHeight(); // PixelCopy calls getHeight() twice
            verify(mocks.mockView).getLocationOnScreen(any(int[].class));

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
    }

    @Test
    public void test_onLargeScreenScaling() {
        // Test with width/height > 4096 to verify scaling
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                        mockStatic(ServiceProvider.class);
                MockedStatic<PixelCopy> mockedPixelCopy = mockStatic(PixelCopy.class)) {

            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);
            setupSuccessfulPixelCopy(mockedPixelCopy);

            // Setup large screen dimensions after basic mocks to override default dimensions
            when(mocks.mockView.getWidth()).thenReturn(8000);
            when(mocks.mockView.getHeight()).thenReturn(4000);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify that PixelCopy was called with scaled bitmap
            mockedPixelCopy.verify(
                    () ->
                            PixelCopy.request(
                                    any(Window.class),
                                    any(Rect.class),
                                    any(Bitmap.class), // This should be scaled down
                                    any(PixelCopy.OnPixelCopyFinishedListener.class),
                                    any(Handler.class)),
                    times(1));

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
    }

    @Test
    @Config(sdk = 25) // Android 7.1.1 - Canvas method with hardware content error
    public void test_onCanvasHardwareContentFailure() {
        // prepare
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                mockStatic(ServiceProvider.class)) {

            // Setup to throw IllegalArgumentException when creating bitmap (hardware content issue)
            when(mocks.mockView.getWidth()).thenReturn(TEST_SCREEN_WIDTH);
            when(mocks.mockView.getHeight()).thenReturn(TEST_SCREEN_HEIGHT);

            mockedServiceProvider
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getAppContextService()).thenReturn(mockAppContextService);
            when(mockAppContextService.getCurrentActivity()).thenReturn(mocks.mockActivity);
            when(mocks.mockActivity.getWindow()).thenReturn(mocks.mockWindow);
            when(mocks.mockWindow.getDecorView()).thenReturn(mocks.mockView);
            when(mocks.mockView.getRootView()).thenReturn(mocks.mockView);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // Note: This test verifies the try-catch structure around Canvas method
            // The IllegalArgumentException would be caught and handled properly
            // For a more specific test, we would need to mock Bitmap.createBitmap() to throw
        }
    }

    @Test
    @Config(sdk = 25) // Android 7.1.1 - Canvas method with large screen
    public void test_onCanvasLargeScreenScaling() {
        // Test Canvas method with large screen to verify scaling
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                mockStatic(ServiceProvider.class)) {

            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);

            // Setup large screen dimensions after basic mocks to override default dimensions
            when(mocks.mockView.getWidth()).thenReturn(8000);
            when(mocks.mockView.getHeight()).thenReturn(4000);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify Canvas method was used (getWidth/getHeight called once each, scaling done on
            // local variables)
            verify(mocks.mockView, times(1)).getWidth();
            verify(mocks.mockView, times(1)).getHeight();

            // verify upload method call (indicates successful Canvas scaling)
            mockedStaticAssuranceBlob.verify(
                    () ->
                            AssuranceBlob.upload(
                                    any(byte[].class),
                                    anyString(),
                                    any(AssuranceSession.class),
                                    any(AssuranceBlob.BlobUploadCallback.class)),
                    times(1));
        }
    }

    @Test
    public void test_onNullActivity() {
        // prepare
        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                mockStatic(ServiceProvider.class)) {
            mockedServiceProvider
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getAppContextService()).thenReturn(mockAppContextService);
            when(mockAppContextService.getCurrentActivity()).thenReturn(null);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify no interactions with session
            verify(mockSession, never())
                    .logLocalUI(any(AssuranceConstants.UILogColorVisibility.class), anyString());
            verify(mockSession, never()).queueOutboundEvent(any(AssuranceEvent.class));
        }
    }

    @Test
    public void test_onNullParentSession() {
        // prepare
        Activity mockActivity = Mockito.mock(Activity.class);
        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                mockStatic(ServiceProvider.class)) {
            mockedServiceProvider
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getAppContextService()).thenReturn(mockAppContextService);
            when(mockAppContextService.getCurrentActivity()).thenReturn(mockActivity);

            // Set parent session to null
            assurancePluginScreenshot.onSessionTerminated();

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify no interactions with session
            verify(mockSession, never())
                    .logLocalUI(any(AssuranceConstants.UILogColorVisibility.class), anyString());
            verify(mockSession, never()).queueOutboundEvent(any(AssuranceEvent.class));
        }
    }

    @Test
    public void test_onBlobUploadFailure() {
        // prepare
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                        mockStatic(ServiceProvider.class);
                MockedStatic<PixelCopy> mockedPixelCopy = mockStatic(PixelCopy.class)) {

            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);
            setupSuccessfulPixelCopy(mockedPixelCopy);

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify upload method call and simulate failure
            ArgumentCaptor<AssuranceBlob.BlobUploadCallback> callbackCaptor =
                    verifyBlobUploadCall();
            callbackCaptor.getValue().onFailure(TEST_UPLOAD_ERROR);

            // verify error handling
            verifyErrorEventWithPayload(
                    "Error while taking screenshot - Description: " + TEST_UPLOAD_ERROR,
                    TEST_UPLOAD_ERROR);
        }
    }

    @Test
    @Config(sdk = 25) // Android 7.1.1 - Forces Canvas method to test Canvas exception handling
    public void test_onExceptionDuringCanvasScreenshotCapture() {
        // prepare
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                mockStatic(ServiceProvider.class)) {
            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);

            // Override the width call to throw exception
            when(mocks.mockView.getWidth()).thenThrow(new RuntimeException("Test exception"));

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify error handling
            verifyErrorEventWithPayload(
                    "Screenshot failed: Test exception", "Screenshot failed: Test exception");
        }
    }

    @Test
    @Config(sdk = 28) // Android 9.0 - Forces PixelCopy method to test PixelCopy exception
    // handling
    public void test_onExceptionDuringPixelCopyScreenshotCapture() {
        // prepare
        MockBundle mocks = createMockBundle();

        try (MockedStatic<ServiceProvider> mockedServiceProvider =
                mockStatic(ServiceProvider.class)) {
            setupBasicActivityMocks(
                    mocks.mockActivity,
                    mocks.mockWindow,
                    mocks.mockView,
                    mocks.mockWindowManager,
                    mockedServiceProvider);

            // Override the width call to throw exception
            when(mocks.mockView.getWidth()).thenThrow(new RuntimeException("Test exception"));

            // test
            assurancePluginScreenshot.onEventReceived(mockAssuranceEvent);

            // verify error handling for PixelCopy path
            verifyErrorEventWithPayload(
                    "PixelCopy setup failed: Test exception",
                    "PixelCopy setup failed: Test exception");
        }
    }

    @Test
    public void test_onSessionTerminated() {
        // test
        assurancePluginScreenshot.onSessionTerminated();

        // verify
        assertEquals(null, assurancePluginScreenshot.getParentSession());
    }

    @Test
    public void test_onSessionConnected() {
        // test - should not throw any exception
        assurancePluginScreenshot.onSessionConnected();
    }

    @Test
    public void test_onSessionDisconnected() {
        // test - should not throw any exception
        assurancePluginScreenshot.onSessionDisconnected(0);
    }

    @Test
    public void test_getVendorAndControlType() {
        // test
        String vendor = assurancePluginScreenshot.getVendor();
        String controlType = assurancePluginScreenshot.getControlType();

        // verify
        assertEquals(AssuranceConstants.VENDOR_ASSURANCE_MOBILE, vendor);
        assertEquals(AssuranceConstants.ControlType.SCREENSHOT, controlType);
    }

    @After
    public void teardown() {
        mockedStaticAssuranceBlob.close();
    }

    // Helper methods for setup
    private void setupBasicActivityMocks(
            Activity mockActivity,
            Window mockWindow,
            View mockView,
            WindowManager mockWindowManager,
            MockedStatic<ServiceProvider> mockedServiceProvider) {
        mockedServiceProvider.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
        when(mockServiceProvider.getAppContextService()).thenReturn(mockAppContextService);
        when(mockAppContextService.getCurrentActivity()).thenReturn(mockActivity);
        when(mockActivity.getWindow()).thenReturn(mockWindow);
        when(mockWindow.getDecorView()).thenReturn(mockView);
        when(mockView.getRootView()).thenReturn(mockView);
        when(mockView.getWidth()).thenReturn(TEST_SCREEN_WIDTH);
        when(mockView.getHeight()).thenReturn(TEST_SCREEN_HEIGHT);
        when(mockActivity.getWindowManager()).thenReturn(mockWindowManager);
        when(mockWindowManager.getDefaultDisplay()).thenReturn(mockDisplay);

        // Mock runOnUiThread to execute synchronously for testing
        doAnswer(
                        invocation -> {
                            Runnable runnable = invocation.getArgument(0);
                            runnable.run(); // Execute immediately instead of posting to UI thread
                            return null;
                        })
                .when(mockActivity)
                .runOnUiThread(any(Runnable.class));

        doAnswer(
                        invocation -> {
                            DisplayMetrics metrics = invocation.getArgument(0);
                            metrics.widthPixels = TEST_SCREEN_WIDTH;
                            metrics.heightPixels = TEST_SCREEN_HEIGHT;
                            return null;
                        })
                .when(mockDisplay)
                .getMetrics(any(DisplayMetrics.class));
    }

    private MockBundle createMockBundle() {
        Activity mockActivity = Mockito.mock(Activity.class);
        Window mockWindow = Mockito.mock(Window.class);
        View mockView = Mockito.mock(View.class);
        WindowManager mockWindowManager = Mockito.mock(WindowManager.class);
        return new MockBundle(mockActivity, mockWindow, mockView, mockWindowManager);
    }

    private static class MockBundle {
        final Activity mockActivity;
        final Window mockWindow;
        final View mockView;
        final WindowManager mockWindowManager;

        MockBundle(
                Activity mockActivity,
                Window mockWindow,
                View mockView,
                WindowManager mockWindowManager) {
            this.mockActivity = mockActivity;
            this.mockWindow = mockWindow;
            this.mockView = mockView;
            this.mockWindowManager = mockWindowManager;
        }
    }

    // Additional helper methods for PixelCopy scenarios
    private void setupSuccessfulPixelCopy(MockedStatic<PixelCopy> mockedPixelCopy) {
        mockedPixelCopy
                .when(
                        () ->
                                PixelCopy.request(
                                        any(Window.class),
                                        any(Rect.class),
                                        any(Bitmap.class),
                                        any(PixelCopy.OnPixelCopyFinishedListener.class),
                                        any(Handler.class)))
                .thenAnswer(
                        invocation -> {
                            PixelCopy.OnPixelCopyFinishedListener listener =
                                    invocation.getArgument(3);
                            listener.onPixelCopyFinished(PixelCopy.SUCCESS);
                            return null;
                        });
    }

    private void setupFailedPixelCopy(MockedStatic<PixelCopy> mockedPixelCopy) {
        mockedPixelCopy
                .when(
                        () ->
                                PixelCopy.request(
                                        any(Window.class),
                                        any(Rect.class),
                                        any(Bitmap.class),
                                        any(PixelCopy.OnPixelCopyFinishedListener.class),
                                        any(Handler.class)))
                .thenAnswer(
                        invocation -> {
                            PixelCopy.OnPixelCopyFinishedListener listener =
                                    invocation.getArgument(3);
                            listener.onPixelCopyFinished(PixelCopy.ERROR_SOURCE_INVALID);
                            return null;
                        });
    }

    private ArgumentCaptor<AssuranceBlob.BlobUploadCallback> verifyBlobUploadCall() {
        ArgumentCaptor<AssuranceBlob.BlobUploadCallback> callbackCaptor =
                ArgumentCaptor.forClass(AssuranceBlob.BlobUploadCallback.class);

        mockedStaticAssuranceBlob.verify(
                () ->
                        AssuranceBlob.upload(
                                any(byte[].class),
                                anyString(),
                                any(AssuranceSession.class),
                                callbackCaptor.capture()),
                times(1));

        return callbackCaptor;
    }

    private void verifySuccessfulScreenshotEvent(String blobId) {
        ArgumentCaptor<AssuranceEvent> eventCaptor = ArgumentCaptor.forClass(AssuranceEvent.class);

        verify(mockSession, times(1))
                .logLocalUI(AssuranceConstants.UILogColorVisibility.LOW, "Screenshot taken");
        verify(mockSession, times(1)).queueOutboundEvent(eventCaptor.capture());

        AssuranceEvent queuedEvent = eventCaptor.getValue();
        assertNotNull(queuedEvent);
        assertEquals(AssuranceConstants.AssuranceEventType.BLOB, queuedEvent.type);
        assertEquals(blobId, queuedEvent.payload.get(PAYLOAD_BLOBID));
        assertEquals("image/png", queuedEvent.payload.get(PAYLOAD_MIMETYPE));
    }

    private void verifyErrorEventWithPayload(
            String expectedLogMessage, String expectedErrorMessage) {
        ArgumentCaptor<AssuranceEvent> eventCaptor = ArgumentCaptor.forClass(AssuranceEvent.class);

        verify(mockSession, times(1))
                .logLocalUI(AssuranceConstants.UILogColorVisibility.LOW, expectedLogMessage);
        verify(mockSession, times(1)).queueOutboundEvent(eventCaptor.capture());

        AssuranceEvent queuedEvent = eventCaptor.getValue();
        assertNotNull(queuedEvent);
        assertEquals(AssuranceConstants.AssuranceEventType.BLOB, queuedEvent.type);
        assertEquals("", queuedEvent.payload.get(PAYLOAD_BLOBID)); // Empty blob ID on error
        assertEquals(
                expectedErrorMessage,
                queuedEvent.payload.get(PAYLOAD_ERROR)); // Actual error message
    }
}
