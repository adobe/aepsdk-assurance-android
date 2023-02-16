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

import static com.adobe.marketing.mobile.assurance.AssuranceTestUtils.setInternalState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Intent;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AssuranceSessionPresentationManagerTest extends TestCase {

    private AssuranceSessionPresentationManager assuranceSessionPresentationManager;

    @Mock private AssuranceSessionOrchestrator.ApplicationHandle mockApplicationHandle;

    @Mock private AssuranceFloatingButton mockAssuranceFloatingButton;

    @Mock private AssuranceConnectionStatusUI mockConnectionStatusUI;

    @Mock private AssurancePinCodeEntryURLProvider mockPinCodeEntryURLProvider;

    @Mock private AssuranceStateManager mockAssuranceStateManager;

    @Mock
    private AssuranceSessionOrchestrator.SessionUIOperationHandler mockSessionUIOperationHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        assuranceSessionPresentationManager =
                new AssuranceSessionPresentationManager(
                        mockAssuranceStateManager,
                        mockSessionUIOperationHandler,
                        mockApplicationHandle);
        setInternalState(
                assuranceSessionPresentationManager, "button", mockAssuranceFloatingButton);
        setInternalState(assuranceSessionPresentationManager, "statusUI", mockConnectionStatusUI);
        setInternalState(
                assuranceSessionPresentationManager, "urlProvider", mockPinCodeEntryURLProvider);
    }

    @Test
    public void testLogLocalUI_AddsUILog() {
        assuranceSessionPresentationManager.logLocalUI(
                AssuranceConstants.UILogColorVisibility.HIGH, "Sample StatusUI Message");

        verify(mockConnectionStatusUI)
                .addUILog(
                        eq(AssuranceConstants.UILogColorVisibility.HIGH),
                        eq("Sample StatusUI Message"));
    }

    @Test
    public void testOnSessionInitialized_LaunchesPinDiaLog() {
        assuranceSessionPresentationManager.onSessionInitialized();

        verify(mockPinCodeEntryURLProvider).launchPinDialog();
    }

    @Test
    public void testOnSessionConnecting_NotifiesPINProvider() {
        assuranceSessionPresentationManager.onSessionConnecting();
        verify(mockPinCodeEntryURLProvider).onConnecting();
    }

    @Test
    public void testOnSessionConnected_NotifiesPINDialog_ShowsFloatingButton_LogsUI() {
        assuranceSessionPresentationManager.onSessionConnected();

        verify(mockPinCodeEntryURLProvider).onConnectionSucceeded();
        verify(mockAssuranceFloatingButton)
                .setCurrentGraphic(AssuranceFloatingButtonView.Graphic.CONNECTED);
        verify(mockAssuranceFloatingButton).display();
        verify(mockConnectionStatusUI)
                .addUILog(
                        eq(AssuranceConstants.UILogColorVisibility.LOW),
                        eq("Assurance connection established."));
    }

    @Test
    public void testOnSessionDisconnected_NORMAL() {
        assuranceSessionPresentationManager.onSessionDisconnected(
                AssuranceConstants.SocketCloseCode.NORMAL);
        verify(mockAssuranceFloatingButton).remove();
        verify(mockConnectionStatusUI).dismiss();
    }

    @Test
    public void testOnSessionDisconnected_ORG_MISMATCH() {
        final Activity mockActivity = mock(Activity.class);
        when(mockApplicationHandle.getCurrentActivity()).thenReturn(mockActivity);

        assuranceSessionPresentationManager.onSessionDisconnected(
                AssuranceConstants.SocketCloseCode.ORG_MISMATCH);

        verify(mockAssuranceFloatingButton).remove();
        verify(mockConnectionStatusUI).dismiss();
        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockActivity).startActivity(intentArgumentCaptor.capture());
    }

    @Test
    public void testOnSessionDisconnected_CLIENT_ERROR() {
        final Activity mockActivity = mock(Activity.class);
        when(mockApplicationHandle.getCurrentActivity()).thenReturn(mockActivity);

        assuranceSessionPresentationManager.onSessionDisconnected(
                AssuranceConstants.SocketCloseCode.CLIENT_ERROR);

        verify(mockAssuranceFloatingButton).remove();
        verify(mockConnectionStatusUI).dismiss();
        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockActivity).startActivity(intentArgumentCaptor.capture());
    }

    @Test
    public void testOnSessionDisconnected_CONNECTION_LIMIT() {
        final Activity mockActivity = mock(Activity.class);
        when(mockApplicationHandle.getCurrentActivity()).thenReturn(mockActivity);

        assuranceSessionPresentationManager.onSessionDisconnected(
                AssuranceConstants.SocketCloseCode.CONNECTION_LIMIT);

        verify(mockAssuranceFloatingButton).remove();
        verify(mockConnectionStatusUI).dismiss();
        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockActivity).startActivity(intentArgumentCaptor.capture());
    }

    @Test
    public void testOnSessionDisconnected_EVENT_LIMIT() {
        final Activity mockActivity = mock(Activity.class);
        when(mockApplicationHandle.getCurrentActivity()).thenReturn(mockActivity);

        assuranceSessionPresentationManager.onSessionDisconnected(
                AssuranceConstants.SocketCloseCode.EVENT_LIMIT);

        verify(mockAssuranceFloatingButton).remove();
        verify(mockConnectionStatusUI).dismiss();
        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockActivity, times(1)).startActivity(intentArgumentCaptor.capture());
    }

    @Test
    public void testOnSessionDisconnected_SESSION_DELETED() {
        final Activity mockActivity = mock(Activity.class);
        when(mockApplicationHandle.getCurrentActivity()).thenReturn(mockActivity);

        assuranceSessionPresentationManager.onSessionDisconnected(
                AssuranceConstants.SocketCloseCode.EVENT_LIMIT);

        verify(mockAssuranceFloatingButton).remove();
        verify(mockConnectionStatusUI).dismiss();
        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockActivity, times(1)).startActivity(intentArgumentCaptor.capture());
    }

    @Test
    public void testOnSessionDisconnected_ABNORMAL() {
        final Activity mockActivity = mock(Activity.class);
        when(mockApplicationHandle.getCurrentActivity()).thenReturn(mockActivity);
        when(mockPinCodeEntryURLProvider.isDisplayed()).thenReturn(false);

        assuranceSessionPresentationManager.onSessionDisconnected(
                AssuranceConstants.SocketCloseCode.ABNORMAL);

        verify(mockAssuranceFloatingButton, times(0)).remove();
        verify(mockConnectionStatusUI, times(0)).dismiss();
        verify(mockActivity, times(0)).startActivity(any(Intent.class));
    }

    @Test
    public void testOnSessionReconnecting() {
        assuranceSessionPresentationManager.onSessionReconnecting();

        verify(mockAssuranceFloatingButton)
                .setCurrentGraphic(AssuranceFloatingButtonView.Graphic.DISCONNECTED);
        verify(mockAssuranceFloatingButton).display();
        verify(mockConnectionStatusUI)
                .addUILog(
                        eq(AssuranceConstants.UILogColorVisibility.HIGH),
                        eq("Assurance disconnected, attempting to reconnect ..."));
    }

    @Test
    public void testOnSessionStateChange() {
        assuranceSessionPresentationManager.onSessionStateChange(
                AssuranceWebViewSocket.SocketReadyState.OPEN);
        verify(mockAssuranceFloatingButton)
                .setCurrentGraphic(AssuranceFloatingButtonView.Graphic.CONNECTED);
        reset(mockAssuranceFloatingButton);

        assuranceSessionPresentationManager.onSessionStateChange(
                AssuranceWebViewSocket.SocketReadyState.CLOSED);
        verify(mockAssuranceFloatingButton)
                .setCurrentGraphic(AssuranceFloatingButtonView.Graphic.DISCONNECTED);
        reset(mockAssuranceFloatingButton);

        assuranceSessionPresentationManager.onSessionStateChange(
                AssuranceWebViewSocket.SocketReadyState.UNKNOWN);
        verify(mockAssuranceFloatingButton)
                .setCurrentGraphic(AssuranceFloatingButtonView.Graphic.DISCONNECTED);
        reset(mockAssuranceFloatingButton);

        assuranceSessionPresentationManager.onSessionStateChange(
                AssuranceWebViewSocket.SocketReadyState.CLOSING);
        verify(mockAssuranceFloatingButton)
                .setCurrentGraphic(AssuranceFloatingButtonView.Graphic.DISCONNECTED);
        reset(mockAssuranceFloatingButton);

        assuranceSessionPresentationManager.onSessionStateChange(
                AssuranceWebViewSocket.SocketReadyState.CONNECTING);
        verify(mockAssuranceFloatingButton)
                .setCurrentGraphic(AssuranceFloatingButtonView.Graphic.DISCONNECTED);
        reset(mockAssuranceFloatingButton);
    }

    @Test
    public void testOnActivityResumed() {
        final Activity mockActivity = mock(Activity.class);
        when(mockApplicationHandle.getCurrentActivity()).thenReturn(mockActivity);
        final Runnable mockDeferredRunnable = mock(Runnable.class);
        mockPinCodeEntryURLProvider.deferredActivityRunnable = mockDeferredRunnable;

        assuranceSessionPresentationManager.onActivityResumed(mockActivity);

        verify(mockAssuranceFloatingButton).onActivityResumed(mockActivity);
        verify(mockDeferredRunnable).run();
        assertNull(mockPinCodeEntryURLProvider.deferredActivityRunnable);
    }

    @Test
    public void testOnActivityDestroyed() {
        final Activity mockActivity = mock(Activity.class);
        when(mockApplicationHandle.getCurrentActivity()).thenReturn(mockActivity);

        assuranceSessionPresentationManager.onActivityDestroyed(mockActivity);

        verify(mockAssuranceFloatingButton).onActivityDestroyed(mockActivity);
    }

    private void verifyIntent(final Intent intent) {}
}
