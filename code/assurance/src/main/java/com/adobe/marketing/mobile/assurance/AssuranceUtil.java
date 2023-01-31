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


import android.net.ParseException;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceEnvironment;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AssuranceUtil {

    private static final HashSet<String> VALID_CONNECTION_PARAMETER_NAMES =
            new HashSet<>(
                    Arrays.asList(
                            AssuranceConstants.SocketURLKeys.CLIENT_ID,
                            AssuranceConstants.SocketURLKeys.ORG_ID,
                            AssuranceConstants.SocketURLKeys.SESSION_ID,
                            AssuranceConstants.SocketURLKeys.TOKEN));

    private static final Pattern CONNECTION_ROUTE_REGEX =
            Pattern.compile("(connect)(-)?(.*)(\\.griffon\\.adobe\\.com)");

    /**
     * Method to return the assuranceEnvironment URL format to be appending to the host of the url
     *
     * <p>This method never returns a null value. This method return an empty string for prod
     * assuranceEnvironment.
     *
     * @param assuranceEnvironment an {@link AssuranceEnvironment} enum representing the current
     *     assuranceEnvironment of Assurance session.
     * @return {@link String} value representing the assuranceEnvironment URL format.
     */
    static String getURLFormatForEnvironment(final AssuranceEnvironment assuranceEnvironment) {
        if (assuranceEnvironment == null || assuranceEnvironment == AssuranceEnvironment.PROD) {
            return "";
        }

        return String.format("-%s", assuranceEnvironment.stringValue());
    }

    /**
     * Method to return the environment from query value.
     *
     * <p>If the query value is null or empty, the environment value returned is {@link
     * AssuranceEnvironment#PROD}. This is because the deeplink doesn't contain a query value for
     * prod environment Assurance session.
     *
     * @param queryValue A {@link String} query value for environment obtained from deeplink URL
     * @return {@link AssuranceEnvironment} value representing the environment of the Assurance
     *     session
     */
    static AssuranceEnvironment getEnvironmentFromQueryValue(final String queryValue) {
        if (StringUtils.isNullOrEmpty(queryValue)) {
            return AssuranceEnvironment.PROD;
        }

        return AssuranceEnvironment.get(queryValue);
    }

    /**
     * Use this method parse and obtain a valid sessionID from the deeplink URL
     *
     * <p>This method returns null in following occasions: 1.If the provided Uri is null 2.If the
     * Uri doesn't contain sessionId value in predefined key 3.If the sessionId is not a valid UUID
     *
     * @param deeplinkUri A {@link Uri} assurance deeplink Uri used to start Assurance session
     * @return {@link String} value representing a valid UUID, null otherwise
     */
    static String getValidSessionIDFromUri(final Uri deeplinkUri) {
        if (deeplinkUri == null) {
            return null;
        }

        final String uuidSessionID =
                deeplinkUri.getQueryParameter(
                        AssuranceConstants.DeeplinkURLKeys.START_URL_QUERY_KEY_SESSION_ID);

        if (StringUtils.isNullOrEmpty(uuidSessionID)) {
            return null;
        }

        try {
            UUID id = UUID.fromString(uuidSessionID);

            if (id.toString().equals(uuidSessionID)) {
                return uuidSessionID;
            }
        } catch (final IllegalArgumentException exception) {
            return null;
        }

        return null;
    }

    /**
     * Checks if a {@code map} is null or empty.
     *
     * @param map the {@link Map} that we want to check
     * @return {@code boolean} with the evaluation result
     */
    static boolean isNullOrEmpty(final Map<String, Object> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Checks if a {@code map} can be cast as {@code Map<String, ?}
     *
     * @param map the {@link Map} that we want to check
     * @return {@code boolean} with the evaluation result
     */
    static boolean isStringMap(final Map<?, ?> map) {
        if (map == null) {
            return false;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || !(entry.getKey() instanceof String)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines if the connection string provided is safe against JS injection attack. Note that
     * this method does not validate existence or correctness of all the required URL query
     * parameters. So a non existent parameter is considered safe.
     *
     * @param url the url string to be validated
     * @return true if the connection string is safe from JS injection
     */
    static boolean isSafe(final String url) {
        if (url == null) {
            return true;
        }

        final Uri uri = Uri.parse(url);

        if (uri == null) {
            return false;
        }

        final String host = uri.getHost();
        final String scheme = uri.getScheme();
        final String path = uri.getPath();

        if (!isValidScheme(scheme) || !isValidHostPath(host, path) || !hasValidQueryParams(uri)) {
            return false;
        }

        final String sessionId = uri.getQueryParameter(AssuranceConstants.SocketURLKeys.SESSION_ID);

        if (sessionId != null && !isValidUUID(sessionId)) {
            return false;
        }

        final String clientId = uri.getQueryParameter(AssuranceConstants.SocketURLKeys.CLIENT_ID);

        if (clientId != null && !isValidUUID(clientId)) {
            return false;
        }

        final String orgId = uri.getQueryParameter(AssuranceConstants.SocketURLKeys.ORG_ID);

        if (orgId != null && !isValidOrgId(orgId)) {
            return false;
        }

        final String token = uri.getQueryParameter(AssuranceConstants.SocketURLKeys.TOKEN);

        if (token != null && !isValidToken(token)) {
            return false;
        }

        return true;
    }

    /**
     * Check if the provided scheme is valid
     *
     * @param scheme the scheme whose validity needs to be checked
     * @return true if the scheme is valid, false otherwise
     */
    private static boolean isValidScheme(final String scheme) {
        return "wss".equalsIgnoreCase(scheme);
    }

    /**
     * Check if the host and path combination are valid
     *
     * @param host the connection host whose validity needs to be checked
     * @param path the connection path whose validity needs to be checked
     * @return true if both the host and path are valid, false otherwise
     */
    private static boolean isValidHostPath(final String host, String path) {
        // Check host
        final Matcher matcher = CONNECTION_ROUTE_REGEX.matcher(host);

        if (!matcher.find()) {
            return false;
        }

        final String env = matcher.group(3);

        if (!isValidEnvironment(env)) {
            return false;
        }

        // Check path
        return "/client/v1".equalsIgnoreCase(path);
    }

    /**
     * Check if the uuid string provided is a valid {@code UUID}
     *
     * @param uuid the uuid string to be validated
     * @return true if the uuid string is a valid UUID
     */
    private static boolean isValidUUID(@NonNull final String uuid) {
        try {
            final UUID resolvedUuid = UUID.fromString(uuid);
        } catch (final IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    /**
     * Check if the org id provided is valid
     *
     * @param orgId the org id that needs to be validated
     * @return true if the org id is valid
     */
    private static boolean isValidOrgId(@NonNull final String orgId) {
        return orgId.endsWith("@AdobeOrg");
    }

    /**
     * Check if the Assurance environment provided is valid
     *
     * @param env the environment that needs to be validated
     * @return true if the environment is valid
     */
    private static boolean isValidEnvironment(final String env) {
        // for prod the environment is absent in the connection url
        if (StringUtils.isNullOrEmpty(env)) {
            return true;
        }

        final String resolvedEnv = AssuranceConstants.AssuranceEnvironment.get(env).stringValue();
        return resolvedEnv.equalsIgnoreCase(env);
    }

    /**
     * Check if the session token provided is valid
     *
     * @param token the session token that needs to be validated
     * @return true if the Assurance session token is valid
     */
    private static boolean isValidToken(@NonNull final String token) {
        try {
            return token.length() == 4 && Integer.parseInt(token) > 0;
        } catch (final ParseException e) {
            return false;
        }
    }

    /**
     * Checks if there are any invalid parameters present in the connection {@code Uri} candidate
     * provided. Any query parameter that does not exist in {@link
     * AssuranceUtil#VALID_CONNECTION_PARAMETER_NAMES} is considered unwanted and therefore invalid.
     *
     * @param uri the {@code URI} to validate for invalid query parameters
     * @return true if there are no invalid query parameters; false otherwise
     */
    private static boolean hasValidQueryParams(@NonNull final Uri uri) {
        for (final String param : uri.getQueryParameterNames()) {
            if (!VALID_CONNECTION_PARAMETER_NAMES.contains(param)) {
                return false;
            }
        }

        return true;
    }
}
