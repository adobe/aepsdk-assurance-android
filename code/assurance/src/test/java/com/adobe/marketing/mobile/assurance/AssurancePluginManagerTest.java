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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AssurancePluginManagerTest {
    @Mock private AssuranceSession mockAssuranceSession;
    @Mock private AssurancePlugin mockPlugin1;
    @Mock private AssurancePlugin mockPlugin2;

    private AssurancePluginManager assurancePluginManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mockPlugin1.getVendor()).thenReturn(AssuranceConstants.VENDOR_ASSURANCE_MOBILE);
        when(mockPlugin2.getVendor()).thenReturn(AssuranceConstants.VENDOR_ASSURANCE_MOBILE);
        assurancePluginManager = new AssurancePluginManager(mockAssuranceSession);
    }

    @Test
    public void test_addPlugin_nullPlugin() {
        try {
            assurancePluginManager.addPlugin(null);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void test_addPlugin_notifiesPlugin() {
        assurancePluginManager.addPlugin(mockPlugin1);

        verify(mockPlugin1).onRegistered(mockAssuranceSession);
    }

    @Test
    public void test_onAssuranceEvent_nullOrEmptyVendor() {
        when(mockPlugin1.getControlType()).thenReturn(null);
        when(mockPlugin2.getControlType()).thenReturn("");
        assurancePluginManager.addPlugin(mockPlugin1);
        assurancePluginManager.addPlugin(mockPlugin2);
        AssuranceEvent mockAssuranceEvent = mock(AssuranceEvent.class);
        when(mockAssuranceEvent.getVendor()).thenReturn(AssuranceConstants.VENDOR_ASSURANCE_MOBILE);

        assurancePluginManager.onAssuranceEvent(mockAssuranceEvent);

        verify(mockPlugin1, never()).onEventReceived(mockAssuranceEvent);
        verify(mockPlugin2, never()).onEventReceived(mockAssuranceEvent);
    }

    @Test
    public void test_onAssuranceEvent_wildCardEvent() {
        when(mockPlugin1.getControlType()).thenReturn(AssuranceConstants.ControlType.WILDCARD);
        assurancePluginManager.addPlugin(mockPlugin1);
        AssuranceEvent mockAssuranceEvent = mock(AssuranceEvent.class);
        when(mockAssuranceEvent.getVendor()).thenReturn(AssuranceConstants.VENDOR_ASSURANCE_MOBILE);

        assurancePluginManager.onAssuranceEvent(mockAssuranceEvent);

        verify(mockPlugin1, times(1)).onEventReceived(mockAssuranceEvent);
    }

    @Test
    public void test_onAssuranceEvent_matchingControlType() {
        when(mockPlugin1.getControlType()).thenReturn(AssuranceConstants.ControlType.SCREENSHOT);
        assurancePluginManager.addPlugin(mockPlugin1);
        AssuranceEvent mockAssuranceEvent = mock(AssuranceEvent.class);
        when(mockAssuranceEvent.getVendor()).thenReturn(AssuranceConstants.VENDOR_ASSURANCE_MOBILE);
        when(mockAssuranceEvent.getControlType())
                .thenReturn(AssuranceConstants.ControlType.SCREENSHOT);

        assurancePluginManager.onAssuranceEvent(mockAssuranceEvent);

        verify(mockPlugin1, times(1)).onEventReceived(mockAssuranceEvent);
    }

    @Test
    public void test_onSessionConnected_notifiesPlugin() {
        assurancePluginManager.addPlugin(mockPlugin1);

        assurancePluginManager.onSessionConnected();

        verify(mockPlugin1, times(1)).onSessionConnected();
    }

    @Test
    public void test_onSessionDisconnected_notifiesPlugin() {
        assurancePluginManager.addPlugin(mockPlugin1);

        assurancePluginManager.onSessionDisconnected(AssuranceConstants.SocketCloseCode.NORMAL);

        verify(mockPlugin1, times(1))
                .onSessionDisconnected(AssuranceConstants.SocketCloseCode.NORMAL);
    }

    @Test
    public void test_onSessionTerminated_notifiesPlugin() {
        assurancePluginManager.addPlugin(mockPlugin1);

        assurancePluginManager.onSessionTerminated();

        verify(mockPlugin1, times(1)).onSessionTerminated();
    }
}
