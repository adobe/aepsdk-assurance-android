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

package com.adobe.marketing.mobile.assurance

import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceConnectionError
import com.adobe.marketing.mobile.assurance.AssuranceSession.AssuranceSessionStatusListener
import com.adobe.marketing.mobile.services.AppContextService
import com.adobe.marketing.mobile.services.ServiceProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

class QuickConnectAuthorizingPresentationTest {

    @Mock private lateinit var mockPresentationDelegate: AssuranceSessionStatusListener

    @Mock private lateinit var mockAppContextService: AppContextService

    @Mock private lateinit var mockQuickConnectActivity: AssuranceQuickConnectActivity

    @Mock private lateinit var mockNonQuickConnectActivity: AssuranceFullScreenTakeoverActivity

    @Mock private lateinit var mockServiceProvider: ServiceProvider
    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>
    private lateinit var quickConnectPresentation: QuickConnectAuthorizingPresentation

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockedStaticServiceProvider = Mockito.mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)
        `when`(mockServiceProvider.appContextService).thenReturn(mockAppContextService)

        quickConnectPresentation = QuickConnectAuthorizingPresentation(mockPresentationDelegate)
    }

    @Test
    fun `Verify isDisplayed is true when current activity is QuickConnectActivity`() {
        `when`(mockAppContextService.currentActivity).thenReturn(mockQuickConnectActivity)

        assertTrue(quickConnectPresentation.isDisplayed)
    }

    @Test
    fun `Verify isDisplayed is false when current activity is NOT a QuickConnectActivity`() {
        `when`(mockAppContextService.currentActivity).thenReturn(mockNonQuickConnectActivity)

        assertFalse(quickConnectPresentation.isDisplayed)
    }

    @Test
    fun `Verify reorderToFront does nothing`() {
        quickConnectPresentation.reorderToFront()
        verifyNoInteractions(mockPresentationDelegate)
    }

    @Test
    fun `Verify showAuthorization does nothing`() {
        quickConnectPresentation.showAuthorization()
        verifyNoInteractions(mockPresentationDelegate)
    }

    @Test
    fun `Verify onConnectionSucceeded notifies delegate`() {
        quickConnectPresentation.onConnectionSucceeded()
        verify(mockPresentationDelegate).onSessionConnected()
    }

    @Test
    fun `Verify onConnectionFailed notifies delegate`() {
        AssuranceConnectionError.values().forEach {
            quickConnectPresentation.onConnectionFailed(it, it.isRetryable)
            verify(mockPresentationDelegate).onSessionTerminated(it)
            reset(mockPresentationDelegate)
        }
    }

    @After
    fun teardown() {
        mockedStaticServiceProvider.close()
    }
}
