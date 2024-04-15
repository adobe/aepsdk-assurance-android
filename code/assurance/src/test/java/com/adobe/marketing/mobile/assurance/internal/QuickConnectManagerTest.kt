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

package com.adobe.marketing.mobile.assurance.internal

import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants.AssuranceConnectionError
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants.QuickConnect
import com.adobe.marketing.mobile.assurance.internal.AssuranceTestUtils.simulateNetworkResponse
import com.adobe.marketing.mobile.services.DeviceInforming
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.ServiceProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class QuickConnectManagerTest {

    companion object {
        private const val TEST_ORG_ID = "SampleOrgId@AdobeOrg"
        private const val TEST_CLIENT_ID = "SampleClientId"
        private const val TEST_DEVICE_NAME = "SampleDeviceName"
    }

    @Mock
    private lateinit var mockAssuranceStateManager: AssuranceStateManager

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    @Mock
    private lateinit var mockNetworkService: Networking

    @Mock
    private lateinit var mockDeviceInfoService: DeviceInforming

    @Mock
    private lateinit var mockExecutorService: ScheduledExecutorService

    @Mock
    private lateinit var mockQuickConnectCallback: QuickConnectCallback

    @Mock
    private lateinit var response: HttpConnecting

    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>
    private lateinit var quickConnectManager: QuickConnectManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockedStaticServiceProvider = Mockito.mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)

        `when`(mockServiceProvider.deviceInfoService).thenReturn(mockDeviceInfoService)
        `when`(mockServiceProvider.networkService).thenReturn(mockNetworkService)

        quickConnectManager = QuickConnectManager(
            mockAssuranceStateManager,
            mockExecutorService,
            "",
            mockQuickConnectCallback
        )
    }

    @Test
    fun `Invoking register launches DeviceCreationTask`() {
        `when`(mockAssuranceStateManager.getClientId()).thenReturn(TEST_CLIENT_ID)
        `when`(mockAssuranceStateManager.getOrgId(false)).thenReturn(TEST_CLIENT_ID)
        `when`(mockDeviceInfoService.deviceName).thenReturn(TEST_DEVICE_NAME)

        quickConnectManager.registerDevice()

        val quickConnectDeviceCreatorCaptor: KArgumentCaptor<QuickConnectDeviceCreator> = argumentCaptor()
        verify(mockExecutorService).submit(quickConnectDeviceCreatorCaptor.capture())

        val capturedDeviceCreationTask = quickConnectDeviceCreatorCaptor.firstValue
        assertNotNull(capturedDeviceCreationTask)
    }

    @Test
    fun `Invoking register called multiple times without cancelling launches DeviceCreationTask only once`() {
        `when`(mockAssuranceStateManager.getClientId()).thenReturn(TEST_CLIENT_ID)
        `when`(mockAssuranceStateManager.getOrgId(false)).thenReturn(TEST_CLIENT_ID)
        `when`(mockDeviceInfoService.deviceName).thenReturn(TEST_DEVICE_NAME)

        quickConnectManager.registerDevice()
        quickConnectManager.registerDevice()
        quickConnectManager.registerDevice()

        val quickConnectDeviceCreatorCaptor: KArgumentCaptor<QuickConnectDeviceCreator> = argumentCaptor()
        verify(mockExecutorService, times(1)).submit(quickConnectDeviceCreatorCaptor.capture())

        val capturedDeviceCreationTask = quickConnectDeviceCreatorCaptor.firstValue
        assertNotNull(capturedDeviceCreationTask)
    }

    @Test
    fun `Invoking register results in launching DeviceStatusCheckerTask on successful response`() {
        // setup
        `when`(mockAssuranceStateManager.getClientId()).thenReturn(TEST_CLIENT_ID)
        `when`(mockAssuranceStateManager.getOrgId(false)).thenReturn(TEST_CLIENT_ID)
        `when`(mockDeviceInfoService.deviceName).thenReturn(TEST_DEVICE_NAME)

        // test
        quickConnectManager.registerDevice()

        // verify
        val quickConnectDeviceCreatorCaptor: KArgumentCaptor<QuickConnectDeviceCreator> = argumentCaptor()
        verify(mockExecutorService).submit(quickConnectDeviceCreatorCaptor.capture())

        val capturedDeviceCreationTask = quickConnectDeviceCreatorCaptor.firstValue
        assertNotNull(capturedDeviceCreationTask)

        // simulate successful response
        val quickConnectDeviceStatusCheckerCaptor: KArgumentCaptor<QuickConnectDeviceStatusChecker> = argumentCaptor()
        capturedDeviceCreationTask.getCallback().call(Response.Success(response))

        verify(mockExecutorService).schedule(quickConnectDeviceStatusCheckerCaptor.capture(), eq(QuickConnect.STATUS_CHECK_DELAY_MS), eq(TimeUnit.MILLISECONDS))

        val capturedDeviceStatusCheckerTask = quickConnectDeviceStatusCheckerCaptor.firstValue
        assertNotNull(capturedDeviceStatusCheckerTask)
    }

    @Test
    fun `Invoking register calls onError on failed device creation request`() {
        // setup
        `when`(mockAssuranceStateManager.getClientId()).thenReturn(TEST_CLIENT_ID)
        `when`(mockAssuranceStateManager.getOrgId(false)).thenReturn(TEST_CLIENT_ID)
        `when`(mockDeviceInfoService.deviceName).thenReturn(TEST_DEVICE_NAME)

        // test
        quickConnectManager.registerDevice()

        // verify
        val quickConnectDeviceCreatorCaptor: KArgumentCaptor<QuickConnectDeviceCreator> = argumentCaptor()
        verify(mockExecutorService).submit(quickConnectDeviceCreatorCaptor.capture())

        val capturedDeviceCreationTask = quickConnectDeviceCreatorCaptor.firstValue
        assertNotNull(capturedDeviceCreationTask)

        // simulate REQUEST_FAILED response
        val quickConnectDeviceStatusCheckerCaptor: KArgumentCaptor<QuickConnectDeviceStatusChecker> = argumentCaptor()
        capturedDeviceCreationTask.getCallback().call(Response.Failure(AssuranceConnectionError.CREATE_DEVICE_REQUEST_FAILED))
        verify(mockExecutorService, never()).schedule(quickConnectDeviceStatusCheckerCaptor.capture(), anyLong(), any(TimeUnit::class.java))

        verify(mockQuickConnectCallback).onError(AssuranceConnectionError.CREATE_DEVICE_REQUEST_FAILED)

        // simulate UNEXPECTED_ERROR response
        capturedDeviceCreationTask.getCallback().call(Response.Failure(AssuranceConnectionError.UNEXPECTED_ERROR))
        verify(mockExecutorService, never()).schedule(quickConnectDeviceStatusCheckerCaptor.capture(), anyLong(), any(TimeUnit::class.java))

        verify(mockQuickConnectCallback).onError(AssuranceConnectionError.UNEXPECTED_ERROR)
    }

    @Test
    fun `Check status retries on a successful response without session details`() {
        // Setup
        `when`(mockAssuranceStateManager.getClientId()).thenReturn(TEST_CLIENT_ID)
        `when`(mockAssuranceStateManager.getOrgId(false)).thenReturn(TEST_CLIENT_ID)
        `when`(mockDeviceInfoService.deviceName).thenReturn(TEST_DEVICE_NAME)
        // simulate device registration and "activeness"
        quickConnectManager.registerDevice()

        // Test
        quickConnectManager.checkDeviceStatus(TEST_ORG_ID, TEST_CLIENT_ID)

        val quickConnectDeviceStatusCheckerCaptor: KArgumentCaptor<QuickConnectDeviceStatusChecker> = argumentCaptor()
        verify(mockExecutorService).schedule(quickConnectDeviceStatusCheckerCaptor.capture(), anyLong(), any(TimeUnit::class.java))

        val capturedDeviceStatusCheckerTask = quickConnectDeviceStatusCheckerCaptor.firstValue
        assertNotNull(capturedDeviceStatusCheckerTask)

        reset(mockExecutorService)

        val simulatedResponse = simulateNetworkResponse(
            HttpsURLConnection.HTTP_OK,
            JSONObject(emptyMap<String, String>()).toString().byteInputStream(), // no session information
            mapOf()
        )

        capturedDeviceStatusCheckerTask.getCallback().call(Response.Success(simulatedResponse))

        val retryQuickConnectDeviceStatusCheckerCaptor: KArgumentCaptor<QuickConnectDeviceStatusChecker> = argumentCaptor()
        verify(mockExecutorService).schedule(retryQuickConnectDeviceStatusCheckerCaptor.capture(), eq(QuickConnect.STATUS_CHECK_DELAY_MS), eq(TimeUnit.MILLISECONDS))
        val capturedRetryDeviceStatusCheckerTask = retryQuickConnectDeviceStatusCheckerCaptor.firstValue
        assertNotNull(capturedRetryDeviceStatusCheckerTask)
    }

    @Test
    fun `Check status calls QuickConnectCallback-onSuccess on a successful response with session details`() {
        val mockStatusCheckFuture = mock<ScheduledFuture<*>>()
        `when`(mockExecutorService.schedule(any(QuickConnectDeviceStatusChecker::class.java), anyLong(), any(TimeUnit::class.java))).thenReturn(mockStatusCheckFuture)

        // test
        quickConnectManager.checkDeviceStatus(TEST_ORG_ID, TEST_CLIENT_ID)

        val quickConnectDeviceStatusCheckerCaptor: KArgumentCaptor<QuickConnectDeviceStatusChecker> = argumentCaptor()
        verify(mockExecutorService).schedule(quickConnectDeviceStatusCheckerCaptor.capture(), anyLong(), any(TimeUnit::class.java))

        val capturedDeviceStatusCheckerTask = quickConnectDeviceStatusCheckerCaptor.firstValue
        assertNotNull(capturedDeviceStatusCheckerTask)

        reset(mockExecutorService)

        val simulatedResponse = simulateNetworkResponse(
            HttpsURLConnection.HTTP_OK,
            JSONObject(
                mapOf(
                    QuickConnect.KEY_SESSION_ID to "SampleSessionID",
                    QuickConnect.KEY_SESSION_TOKEN to "SampleToken"
                )
            ).toString().byteInputStream(),
            mapOf()
        )

        // simulate successful response
        capturedDeviceStatusCheckerTask.getCallback().call(Response.Success(simulatedResponse))

        val retryQuickConnectDeviceStatusCheckerCaptor: KArgumentCaptor<QuickConnectDeviceStatusChecker> = argumentCaptor()
        verify(mockExecutorService, never()).schedule(retryQuickConnectDeviceStatusCheckerCaptor.capture(), anyLong(), any(TimeUnit::class.java))

        verify(mockQuickConnectCallback).onSuccess("SampleSessionID", "SampleToken")

        verify(mockStatusCheckFuture).cancel(true)
        assertNull(quickConnectManager.deviceCreationTaskHandle)
        assertNull(quickConnectManager.deviceStatusTaskHandle)
        assertFalse(quickConnectManager.isActive)
    }

    @Test
    fun `Cancel cleans up all existing tasks`() {
        // Setup
        val mockCreatorFuture = mock<Future<*>>()
        val mockStatusCheckFuture = mock<ScheduledFuture<*>>()

        `when`(mockAssuranceStateManager.getClientId()).thenReturn(TEST_CLIENT_ID)
        `when`(mockAssuranceStateManager.getOrgId(false)).thenReturn(TEST_CLIENT_ID)
        `when`(mockDeviceInfoService.deviceName).thenReturn(TEST_DEVICE_NAME)
        `when`(mockExecutorService.submit(any(QuickConnectDeviceCreator::class.java))).thenReturn(mockCreatorFuture)
        `when`(mockExecutorService.schedule(any(QuickConnectDeviceStatusChecker::class.java), anyLong(), any(TimeUnit::class.java))).thenReturn(mockStatusCheckFuture)

        // trigger registration
        quickConnectManager.registerDevice()

        val quickConnectDeviceCreatorCaptor: KArgumentCaptor<QuickConnectDeviceCreator> = argumentCaptor()
        verify(mockExecutorService).submit(quickConnectDeviceCreatorCaptor.capture())

        val capturedDeviceCreationTask = quickConnectDeviceCreatorCaptor.firstValue
        assertNotNull(capturedDeviceCreationTask)

        // simulate successful response to simulate status check
        val quickConnectDeviceStatusCheckerCaptor: KArgumentCaptor<QuickConnectDeviceStatusChecker> = argumentCaptor()
        capturedDeviceCreationTask.getCallback().call(Response.Success(response))

        verify(mockExecutorService).schedule(quickConnectDeviceStatusCheckerCaptor.capture(), eq(QuickConnect.STATUS_CHECK_DELAY_MS), eq(TimeUnit.MILLISECONDS))

        val capturedDeviceStatusCheckerTask = quickConnectDeviceStatusCheckerCaptor.firstValue
        assertNotNull(capturedDeviceStatusCheckerTask)

        // Test
        quickConnectManager.cancel()

        verify(mockCreatorFuture).cancel(true)
        verify(mockStatusCheckFuture).cancel(true)
        assertNull(quickConnectManager.deviceCreationTaskHandle)
        assertNull(quickConnectManager.deviceStatusTaskHandle)
        assertFalse(quickConnectManager.isActive)
    }

    @After
    fun teardown() {
        mockedStaticServiceProvider.close()
    }
}
