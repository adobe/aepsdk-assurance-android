/*
 * Copyright 2023 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance

import com.adobe.marketing.mobile.AdobeCallback
import com.adobe.marketing.mobile.assurance.AssuranceConstants.QuickConnect
import com.adobe.marketing.mobile.assurance.AssuranceTestUtils.simulateNetworkResponse
import com.adobe.marketing.mobile.assurance.AssuranceTestUtils.verifyNetworkRequestParams
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.HttpMethod
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.NetworkRequest
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.NetworkingConstants
import com.adobe.marketing.mobile.services.ServiceProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.net.HttpURLConnection

class QuickConnectDeviceStatusCheckerTest {

    companion object {
        private const val TEST_ORG_ID = "SampleOrgId@AdobeOrg"
        private const val TEST_CLIENT_ID = "SampleClientId"
    }

    @Mock
    private lateinit var mockNetworkService: Networking

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    @Mock
    private lateinit var mockCallback: AdobeCallback<Response<HttpConnecting, AssuranceConstants.AssuranceQuickConnectError>>

    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockedStaticServiceProvider = Mockito.mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)
        Mockito.`when`(mockServiceProvider.networkService).thenReturn(mockNetworkService)
    }

    @Test
    fun `Verify DeviceStatusCheckerTask makes network request`() {
        // setup
        val quickConnectDeviceStatusChecker = QuickConnectDeviceStatusChecker(TEST_ORG_ID, TEST_CLIENT_ID, mockCallback)

        // test
        quickConnectDeviceStatusChecker.run()

        // Verify
        val networkRequestCaptor: KArgumentCaptor<NetworkRequest> = argumentCaptor()
        val networkCallbackCaptor: KArgumentCaptor<NetworkCallback> = argumentCaptor()
        verify(mockNetworkService).connectAsync(networkRequestCaptor.capture(), networkCallbackCaptor.capture())

        val capturedNetworkRequest = networkRequestCaptor.firstValue
        assertNotNull(capturedNetworkRequest)

        val expectedBody = JSONObject(
            mapOf(
                QuickConnect.KEY_ORG_ID to TEST_ORG_ID,
                QuickConnect.KEY_CLIENT_ID to TEST_CLIENT_ID
            )
        ).toString().toByteArray()

        val expectedNetworkRequest = NetworkRequest(
            "${QuickConnect.BASE_DEVICE_API_URL}/${QuickConnect.DEVICE_API_PATH_STATUS}",
            HttpMethod.POST,
            expectedBody,
            mapOf(
                NetworkingConstants.Headers.ACCEPT to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION,
                NetworkingConstants.Headers.CONTENT_TYPE to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION
            ),
            QuickConnect.CONNECTION_TIMEOUT_MS,
            QuickConnect.READ_TIMEOUT_MS
        )

        verifyNetworkRequestParams(expectedNetworkRequest, capturedNetworkRequest)

        val capturedNetworkCallback = networkCallbackCaptor.firstValue
        assertNotNull(capturedNetworkCallback)
    }

    @Test
    fun `Verify DeviceStatusCheckerTask invoked callback with Success response when request successful`() {
        // setup
        val quickConnectDeviceStatusChecker = QuickConnectDeviceStatusChecker(TEST_ORG_ID, TEST_CLIENT_ID, mockCallback)

        // test
        quickConnectDeviceStatusChecker.run()

        // Verify
        val networkRequestCaptor: KArgumentCaptor<NetworkRequest> = argumentCaptor()
        val networkCallbackCaptor: KArgumentCaptor<NetworkCallback> = argumentCaptor()
        verify(mockNetworkService).connectAsync(networkRequestCaptor.capture(), networkCallbackCaptor.capture())

        val capturedNetworkRequest = networkRequestCaptor.firstValue
        assertNotNull(capturedNetworkRequest)

        val expectedBody = JSONObject(
            mapOf(
                QuickConnect.KEY_ORG_ID to TEST_ORG_ID,
                QuickConnect.KEY_CLIENT_ID to TEST_CLIENT_ID
            )
        ).toString().toByteArray()

        val expectedNetworkRequest = NetworkRequest(
            "${QuickConnect.BASE_DEVICE_API_URL}/${QuickConnect.DEVICE_API_PATH_STATUS}",
            HttpMethod.POST,
            expectedBody,
            mapOf(
                NetworkingConstants.Headers.ACCEPT to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION,
                NetworkingConstants.Headers.CONTENT_TYPE to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION
            ),
            QuickConnect.CONNECTION_TIMEOUT_MS,
            QuickConnect.READ_TIMEOUT_MS
        )

        verifyNetworkRequestParams(expectedNetworkRequest, capturedNetworkRequest)

        val capturedNetworkCallback = networkCallbackCaptor.firstValue
        assertNotNull(capturedNetworkCallback)

        // simulate HTTP.OK response for the request
        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, "ResponseData".byteInputStream(), mapOf())
        capturedNetworkCallback.call(simulatedResponse)

        val responseCaptor: KArgumentCaptor<Response<HttpConnecting, AssuranceConstants.AssuranceQuickConnectError>> = argumentCaptor()
        verify(mockCallback).call(responseCaptor.capture())

        val capturedResponse: Response<HttpConnecting, AssuranceConstants.AssuranceQuickConnectError> = responseCaptor.firstValue
        if (capturedResponse is Response.Success) {
            assertEquals(simulatedResponse.inputStream, capturedResponse.data.inputStream)
            assertEquals(simulatedResponse.responseCode, capturedResponse.data.responseCode)
        } else {
            fail("Successful response should have been delivered.")
        }

        verify(simulatedResponse).close()
    }

    @Test
    fun `Verify DeviceStatusCheckerTask invokes callback with DEVICE_STATUS_REQUEST_FAILED response when request fails`() {
        // setup
        val quickConnectDeviceStatusChecker = QuickConnectDeviceStatusChecker(TEST_ORG_ID, TEST_CLIENT_ID, mockCallback)

        // test
        quickConnectDeviceStatusChecker.run()

        // Verify
        val networkRequestCaptor: KArgumentCaptor<NetworkRequest> = argumentCaptor()
        val networkCallbackCaptor: KArgumentCaptor<NetworkCallback> = argumentCaptor()
        verify(mockNetworkService).connectAsync(networkRequestCaptor.capture(), networkCallbackCaptor.capture())

        val capturedNetworkRequest = networkRequestCaptor.firstValue
        assertNotNull(capturedNetworkRequest)

        val expectedBody = JSONObject(
            mapOf(
                QuickConnect.KEY_ORG_ID to TEST_ORG_ID,
                QuickConnect.KEY_CLIENT_ID to TEST_CLIENT_ID
            )
        ).toString().toByteArray()

        val expectedNetworkRequest = NetworkRequest(
            "${QuickConnect.BASE_DEVICE_API_URL}/${QuickConnect.DEVICE_API_PATH_STATUS}",
            HttpMethod.POST,
            expectedBody,
            mapOf(
                NetworkingConstants.Headers.ACCEPT to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION,
                NetworkingConstants.Headers.CONTENT_TYPE to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION
            ),
            QuickConnect.CONNECTION_TIMEOUT_MS,
            QuickConnect.READ_TIMEOUT_MS
        )

        verifyNetworkRequestParams(expectedNetworkRequest, capturedNetworkRequest)

        val capturedNetworkCallback = networkCallbackCaptor.firstValue
        assertNotNull(capturedNetworkCallback)

        // simulate HTTP_NOT_FOUND response for the request
        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_NOT_FOUND, null, mapOf())
        capturedNetworkCallback.call(simulatedResponse)

        val responseCaptor: KArgumentCaptor<Response<HttpConnecting, AssuranceConstants.AssuranceQuickConnectError>> = argumentCaptor()
        verify(mockCallback).call(responseCaptor.capture())

        val capturedResponse: Response<HttpConnecting, AssuranceConstants.AssuranceQuickConnectError> = responseCaptor.firstValue

        if (capturedResponse is Response.Failure) {
            assertEquals(AssuranceConstants.AssuranceQuickConnectError.DEVICE_STATUS_REQUEST_FAILED, capturedResponse.error)
        } else {
            fail("Error response should have been delivered.")
        }

        verify(simulatedResponse).close()
    }

    @Test
    fun `Verify DeviceStatusCheckerTask invokes callback with UNEXPECTED_ERROR response when request fails`() {
        // setup
        val quickConnectDeviceStatusChecker = QuickConnectDeviceStatusChecker(TEST_ORG_ID, TEST_CLIENT_ID, mockCallback)

        // test
        quickConnectDeviceStatusChecker.run()

        // Verify
        val networkRequestCaptor: KArgumentCaptor<NetworkRequest> = argumentCaptor()
        val networkCallbackCaptor: KArgumentCaptor<NetworkCallback> = argumentCaptor()
        verify(mockNetworkService).connectAsync(networkRequestCaptor.capture(), networkCallbackCaptor.capture())

        val capturedNetworkRequest = networkRequestCaptor.firstValue
        assertNotNull(capturedNetworkRequest)

        val expectedBody = JSONObject(
            mapOf(
                QuickConnect.KEY_ORG_ID to TEST_ORG_ID,
                QuickConnect.KEY_CLIENT_ID to TEST_CLIENT_ID
            )
        ).toString().toByteArray()

        val expectedNetworkRequest = NetworkRequest(
            "${QuickConnect.BASE_DEVICE_API_URL}/${QuickConnect.DEVICE_API_PATH_STATUS}",
            HttpMethod.POST,
            expectedBody,
            mapOf(
                NetworkingConstants.Headers.ACCEPT to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION,
                NetworkingConstants.Headers.CONTENT_TYPE to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION
            ),
            QuickConnect.CONNECTION_TIMEOUT_MS,
            QuickConnect.READ_TIMEOUT_MS
        )

        verifyNetworkRequestParams(expectedNetworkRequest, capturedNetworkRequest)

        val capturedNetworkCallback = networkCallbackCaptor.firstValue
        assertNotNull(capturedNetworkCallback)

        // simulate null response for the request
        capturedNetworkCallback.call(null)

        val responseCaptor: KArgumentCaptor<Response<HttpConnecting, AssuranceConstants.AssuranceQuickConnectError>> = argumentCaptor()
        verify(mockCallback).call(responseCaptor.capture())

        val capturedResponse: Response<HttpConnecting, AssuranceConstants.AssuranceQuickConnectError> = responseCaptor.firstValue

        if (capturedResponse is Response.Failure) {
            assertEquals(AssuranceConstants.AssuranceQuickConnectError.UNEXPECTED_ERROR, capturedResponse.error)
        } else {
            fail("Error response should have been delivered.")
        }
    }

    @After
    fun teardown() {
        mockedStaticServiceProvider.close()
    }
}
