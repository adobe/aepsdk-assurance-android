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

import androidx.annotation.VisibleForTesting
import com.adobe.marketing.mobile.AdobeCallback
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants.AssuranceConnectionError
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants.QuickConnect
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.HttpMethod
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.NetworkRequest
import com.adobe.marketing.mobile.services.NetworkingConstants
import com.adobe.marketing.mobile.services.ServiceProvider
import org.json.JSONObject
import javax.net.ssl.HttpsURLConnection

/**
 * Responsible for making a network request to create/register a device.
 *
 * @param orgId orgId that was used for the the device creation/registration
 * @param clientId clientId to be used for the the device creation/registration
 * @param deviceName deviceName to be used for the the device creation/registration
 * @param callback a callback that will be used to be notify of the response to the network request
 */
internal class QuickConnectDeviceCreator(
    private val orgId: String,
    private val clientId: String,
    private val deviceName: String,
    private val environment: String = "",
    private val callback: AdobeCallback<Response<HttpConnecting, AssuranceConnectionError>>
) : Runnable {

    companion object {
        private const val LOG_SOURCE = "QuickConnectDeviceCreator"
    }

    override fun run() {
        val networkRequest: NetworkRequest? = try {
            buildRequest()
        } catch (e: Exception) {
            Log.trace(Assurance.LOG_TAG, LOG_SOURCE, "Exception attempting to build request. ${e.message}")
            null
        }

        if (networkRequest == null) {
            callback.call(Response.Failure(AssuranceConnectionError.CREATE_DEVICE_REQUEST_MALFORMED))
            return
        }

        makeRequest(networkRequest)
    }

    /**
     * Builds the network request for creating device.
     */
    private fun buildRequest(): NetworkRequest {
        val prefixedEnv = if (environment.isNotEmpty()) "-$environment" else ""
        val url = "${String.format(QuickConnect.BASE_DEVICE_API_URL_FORMAT, prefixedEnv)}/${QuickConnect.DEVICE_API_PATH_CREATE}"

        val headers: Map<String, String> = mapOf(
            NetworkingConstants.Headers.ACCEPT to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION,
            NetworkingConstants.Headers.CONTENT_TYPE to NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION
        )

        val body: Map<String, String> = mapOf(
            QuickConnect.KEY_ORG_ID to orgId,
            QuickConnect.KEY_DEVICE_NAME to deviceName,
            QuickConnect.KEY_CLIENT_ID to clientId
        )

        val jsonBody = JSONObject(body)

        val bodyBytes: ByteArray = jsonBody.toString().toByteArray()
        return NetworkRequest(
            url,
            HttpMethod.POST,
            bodyBytes,
            headers,
            QuickConnect.CONNECTION_TIMEOUT_MS,
            QuickConnect.READ_TIMEOUT_MS
        )
    }

    /**
     * Makes the network request to create device API. Uses [callback] to notify about the response.
     */
    private fun makeRequest(networkRequest: NetworkRequest) {
        ServiceProvider.getInstance().networkService.connectAsync(networkRequest) { response: HttpConnecting? ->
            if (response == null) {
                callback.call(Response.Failure(AssuranceConnectionError.UNEXPECTED_ERROR))
                return@connectAsync
            }

            val responseCode = response.responseCode
            if (responseCode == HttpsURLConnection.HTTP_CREATED || HttpsURLConnection.HTTP_OK == responseCode) {
                Log.debug(
                    Assurance.LOG_TAG,
                    Assurance.LOG_TAG,
                    "Registration request succeeded: %s",
                    responseCode
                )
                callback.call(Response.Success(response))
            } else {
                Log.trace(Assurance.LOG_TAG, LOG_SOURCE, "Device registration failed with code : $responseCode and message: ${response.responseMessage}.")
                callback.call(
                    Response.Failure(AssuranceConnectionError.CREATE_DEVICE_REQUEST_FAILED)
                )
            }

            response.close()
        }
    }

    /**
     * Exists to retrieve the [callback] reference for the sake of tests only. Used instead of the
     * exposing the getter for callback in the constructor itself because the annotations are not retained with that way.
     */
    @VisibleForTesting
    fun getCallback(): AdobeCallback<Response<HttpConnecting, AssuranceConnectionError>> {
        return callback
    }
}
