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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.webkit.WebView;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
// TODO: Remove power mock usages
@RunWith(PowerMockRunner.class)
@PrepareForTest({Base64.class, Uri.class})
public class AssuranceWebViewSocketTest {

    private static final String CONST_URL =
            "wss://connect.griffon.adobe.com/client/v1?"
                    + "sessionId=d600bba7-f90e-45a9-8022-78edda3edda5&"
                    + "token=1234&"
                    + "orgId=972C898555E9F7BC7F000101@AdobeOrg&"
                    + "clientId=C8385D85-9CE3-409E-92C2-565E7E59D69C";
    ;

    AssuranceWebViewSocket assuranceWebViewSocket;

    @Mock Uri mockUri;

    @Mock AssuranceWebViewSocketHandler mockAssuranceWebViewSocketHandler;

    @Mock WebView mockWebview;

    @Mock ExecutorService webViewExecutor;

    @Before
    public void setup() {
        assuranceWebViewSocket = new AssuranceWebViewSocket(mockAssuranceWebViewSocketHandler);

        PowerMockito.mockStatic(Uri.class);
        when(Uri.parse(anyString())).thenReturn(mockUri);
        Whitebox.setInternalState(assuranceWebViewSocket, "webView", mockWebview);
        Whitebox.setInternalState(assuranceWebViewSocket, "webViewExecutor", webViewExecutor);
    }

    @Test
    public void test_connect() throws InterruptedException {
        mockValidURL();
        mockExecutorService();
        mockMainHandlerAndRunTheRunnable();

        // test
        assuranceWebViewSocket.connect(CONST_URL);

        // verify connection url is updated
        assertEquals(CONST_URL, assuranceWebViewSocket.getConnectionURL());

        // verify state is updated
        assertEquals(
                AssuranceWebViewSocket.SocketReadyState.CONNECTING,
                assuranceWebViewSocket.getState());

        // verify onSocketStateChange is called
        verify(mockAssuranceWebViewSocketHandler)
                .onSocketStateChange(
                        assuranceWebViewSocket, AssuranceWebViewSocket.SocketReadyState.CONNECTING);

        // verify webview load url is called
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockWebview).loadUrl(argumentCaptor.capture());
        assertEquals("javascript: connect('" + CONST_URL + "')", argumentCaptor.getValue());
    }

    @Test
    public void test_connect_invalidURL() throws InterruptedException {
        mockValidInvalidURI();
        mockExecutorService();
        mockMainHandlerAndRunTheRunnable();

        // test
        assuranceWebViewSocket.connect(CONST_URL);

        // verify connection url is not updated
        assertNull(assuranceWebViewSocket.getConnectionURL());

        // verify state is not updated
        assertNotEquals(
                AssuranceWebViewSocket.SocketReadyState.CONNECTING,
                assuranceWebViewSocket.getState());

        // verify onSocketStateChange is not called
        verify(mockAssuranceWebViewSocketHandler, never())
                .onSocketStateChange(
                        assuranceWebViewSocket, AssuranceWebViewSocket.SocketReadyState.CONNECTING);

        // verify webview load url is not called
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockWebview, never()).loadUrl(argumentCaptor.capture());
    }

    @Test
    public void test_disconnect() {
        mockValidURL();
        mockExecutorService();
        mockMainHandlerAndRunTheRunnable();

        // test
        assuranceWebViewSocket.disconnect();

        // verify connection url is updated
        assertNull(assuranceWebViewSocket.getConnectionURL());

        // verify state is updated
        assertEquals(
                AssuranceWebViewSocket.SocketReadyState.CLOSING, assuranceWebViewSocket.getState());

        // verify onSocketStateChange is called
        verify(mockAssuranceWebViewSocketHandler)
                .onSocketStateChange(
                        assuranceWebViewSocket, AssuranceWebViewSocket.SocketReadyState.CLOSING);

        // verify webview load url is called
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockWebview).loadUrl(argumentCaptor.capture());
        assertEquals("javascript: disconnect()", argumentCaptor.getValue());
    }

    @Test
    public void test_sendData() throws InterruptedException {
        mockValidURL();
        mockExecutorService();
        mockMainHandlerAndRunTheRunnable();

        // mock encodedDataString
        String encodedDataString = "encodedDataString";

        // Mock Base64
        PowerMockito.mockStatic(Base64.class);
        PowerMockito.when(Base64.encodeToString(any(byte[].class), anyInt()))
                .thenReturn(encodedDataString);

        byte[] mockDataBytes = "MockData".getBytes();
        // test
        assuranceWebViewSocket.sendData(mockDataBytes);

        Thread.sleep(4000);

        // verify arguments are same as the mock arguments
        ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Integer> argumentCaptor1 = ArgumentCaptor.forClass(Integer.class);

        // verify encodeToString is called
        PowerMockito.verifyStatic(Base64.class);
        Base64.encodeToString(argumentCaptor.capture(), argumentCaptor1.capture());

        assertEquals(mockDataBytes, argumentCaptor.getValue());
        assertEquals(
                Integer.valueOf(Base64.NO_WRAP | Base64.NO_PADDING), argumentCaptor1.getValue());

        // verify webview load url is called
        ArgumentCaptor<String> argumentCaptor2 = ArgumentCaptor.forClass(String.class);
        verify(mockWebview).loadUrl(argumentCaptor2.capture());
        assertEquals(
                "javascript: sendData('" + encodedDataString + "')", argumentCaptor2.getValue());
    }

    private void mockExecutorService() {
        // Mock the submit call with the runnable and invoke in between
        doAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                ((Runnable) invocation.getArguments()[0]).run();
                                return null;
                            }
                        })
                .when(webViewExecutor)
                .submit(any(Runnable.class));
    }

    private void mockMainHandlerAndRunTheRunnable() {
        // Mock run on main thread method
        Handler mainHandlerMock = Mockito.mock(Handler.class);
        Whitebox.setInternalState(assuranceWebViewSocket, "mainThreadHandler", mainHandlerMock);
        doAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                ((Runnable) invocation.getArguments()[0]).run();
                                return null;
                            }
                        })
                .when(mainHandlerMock)
                .post(any(Runnable.class));
    }

    private void mockValidURL() {
        when(mockUri.getHost()).thenReturn(("connect.griffon.adobe.com"));
        when(mockUri.getPath()).thenReturn(("/client/v1"));
        when(mockUri.getScheme()).thenReturn(("wss"));
        when(mockUri.getQueryParameter("sessionId"))
                .thenReturn(("d600bba7-f90e-45a9-8022-78edda3edda5"));
        when(mockUri.getQueryParameter("sessionId"))
                .thenReturn(("d600bba7-f90e-45a9-8022-78edda3edda5"));
        when(mockUri.getQueryParameter("token")).thenReturn(("1234"));
        when(mockUri.getQueryParameter("orgId")).thenReturn("972C898555E9F7BC7F000101@AdobeOrg");
        when(mockUri.getQueryParameter("clientId"))
                .thenReturn(("C8385D85-9CE3-409E-92C2-565E7E59D69C"));
    }

    private void mockValidInvalidURI() {
        when(mockUri.getHost()).thenReturn(("connect.griffon.adobe.com"));
        when(mockUri.getPath()).thenReturn(("/client/v3"));
        when(mockUri.getScheme()).thenReturn(("http"));
        when(mockUri.getQueryParameter("sessionId"))
                .thenReturn(("RANDOM_d600bba7-f90e-45a9-8022-78edda3edda5"));
        when(mockUri.getQueryParameter("sessionId"))
                .thenReturn(("d600bba7-f90e-45a9-8022-78edda3edda5"));
        when(mockUri.getQueryParameter("token")).thenReturn(("1234"));
        when(mockUri.getQueryParameter("orgId")).thenReturn("972C898555E9F7BC7F000101@AdobeOrg");
        when(mockUri.getQueryParameter("clientId"))
                .thenReturn(("C8385D85-9CE3-409E-92C2-565E7E59D69C"));
    }
}
