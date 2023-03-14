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

package com.adobe.marketing.mobile.assurance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.NetworkRequest;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import org.mockito.Mockito;

/** Utility class for tests. */
class AssuranceTestUtils {

    /**
     * Sets a private field using reflection. Used for setting states of classes where dependency
     * injection is not trivial.
     *
     * @param classToSet the class whose member should be set
     * @param name the name of the member that should be set
     * @param value the value of the memberto be set
     */
    static void setInternalState(Object classToSet, String name, Object value) {
        try {
            final Field privateField = classToSet.getClass().getDeclaredField(name);
            privateField.setAccessible(true);
            privateField.set(classToSet, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(String.format("Failed to set %s.%s", classToSet, name));
        }
    }

    /**
     * Compares the provided {@code NetworkRequest} parameters and determines if they represent the
     * same request.
     *
     * @param expectedNetworkRequest the {@code NetworkRequest} to be compared against
     * @param actualNetworkRequest the {@code NetworkRequest} to be validated
     */
    static void verifyNetworkRequestParams(
            final NetworkRequest expectedNetworkRequest,
            final NetworkRequest actualNetworkRequest) {
        assertEquals(expectedNetworkRequest.getUrl(), actualNetworkRequest.getUrl());
        assertEquals(expectedNetworkRequest.getMethod(), actualNetworkRequest.getMethod());
        assertEquals(
                new String(expectedNetworkRequest.getBody()),
                new String(actualNetworkRequest.getBody()));
        assertEquals(
                expectedNetworkRequest.getConnectTimeout(),
                actualNetworkRequest.getConnectTimeout());
        assertEquals(
                expectedNetworkRequest.getReadTimeout(), actualNetworkRequest.getReadTimeout());
        assertEquals(expectedNetworkRequest.getHeaders(), actualNetworkRequest.getHeaders());
    }

    /**
     * Simulates a {@code HttpConnecting} with the provided parameters. Intended for use with a
     * Mockito.when() to mock network responses.
     *
     * @param responseCode a mock response code
     * @param responseStream response to be simulated
     * @param metadata headers to be simulated
     * @return a mock {@code HttpConnecting} with the provided parameters
     */
    static HttpConnecting simulateNetworkResponse(
            int responseCode, InputStream responseStream, Map<String, String> metadata) {
        final HttpConnecting mockResponse = Mockito.mock(HttpConnecting.class);
        when(mockResponse.getResponseCode()).thenReturn(responseCode);
        when(mockResponse.getInputStream()).thenReturn(responseStream);
        doAnswer(
                        invocation -> {
                            final String key = invocation.getArgument(0);
                            return metadata.get(key);
                        })
                .when(mockResponse)
                .getResponsePropertyValue(any());

        return mockResponse;
    }
}
