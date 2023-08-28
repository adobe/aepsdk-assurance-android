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

import androidx.annotation.VisibleForTesting
import com.adobe.marketing.mobile.Assurance.LOG_TAG
import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceConnectionError
import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceEnvironment
import com.adobe.marketing.mobile.assurance.AssuranceConstants.QuickConnect
import com.adobe.marketing.mobile.services.DataStoring
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.util.StreamUtils
import com.adobe.marketing.mobile.util.StringUtils
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Responsible for manging the workflow that registers the device as one capable of initiating a QuickConnect session.
 * A typical flow includes device creation, status checks with retries, success / failure notifications.
 */
internal class QuickConnectManager(
    private val assuranceSharedStateManager: AssuranceStateManager,
    private val executorService: ScheduledExecutorService,
    private val quickConnectCallback: QuickConnectCallback,
    private val dataStoreService: DataStoring = ServiceProvider.getInstance().dataStoreService
) {

    companion object {
        private const val LOG_SOURCE = "QuickConnectManager"
    }

    @VisibleForTesting
    internal val quickConnectEnvironment: String

    init {
        val envString = dataStoreService.getNamedCollection(AssuranceConstants.DataStoreKeys.DATASTORE_NAME).getString(AssuranceConstants.DataStoreKeys.ENVIRONMENT, "")
        val assuranceEnvironment = AssuranceEnvironment.get(envString)
        quickConnectEnvironment = if (assuranceEnvironment == AssuranceEnvironment.PROD) "" else assuranceEnvironment.stringValue()
    }

    /**
     * A data class to hold the details of the response from the create device API.
     */
    internal data class QuickConnectSessionDetails(val sessionId: String, val token: String)

    /**
     * Represents the number of retries made for a status check.
     */
    @Volatile
    private var retryCount = 0

    /**
     * Represents if there is an active attempt to initiate a QuickConnect session.
     */
    @VisibleForTesting
    @Volatile
    internal var isActive = false
        private set

    /**
     * A handle to the device creation task.
     */
    @VisibleForTesting
    internal var deviceCreationTaskHandle: Future<*>? = null
        private set

    /**
     * A handle to the device status check task.
     */
    @VisibleForTesting
    internal var deviceStatusTaskHandle: ScheduledFuture<*>? = null
        private set

    /**
     * Initiates device registration by triggering the [QuickConnectDeviceCreator]
     */
    internal fun registerDevice() {
        if (isActive) {
            return
        }

        isActive = true

        val orgId = assuranceSharedStateManager.getOrgId(false)
        val clientId = assuranceSharedStateManager.clientId
        val deviceName = ServiceProvider.getInstance().deviceInfoService.deviceName
        Log.trace(LOG_TAG, LOG_SOURCE, "Attempting to register device with deviceName:$deviceName, orgId: $orgId, clientId: $clientId.")

        val quickConnectDeviceCreator = QuickConnectDeviceCreator(orgId, clientId, deviceName, quickConnectEnvironment) {
            when (it) {
                is Response.Success -> checkDeviceStatus(orgId, clientId)
                is Response.Failure -> {
                    quickConnectCallback.onError(it.error)
                    cleanup()
                }
            }
        }

        deviceCreationTaskHandle = executorService.submit(quickConnectDeviceCreator)
    }

    /**
     * Periodically checks the status of quick connect device registration that was triggered by [registerDevice]
     *
     * @param orgId the orgId for which quick connect was initiated
     * @param clientId the clientId for which quick connect was initiated
     */
    @VisibleForTesting
    internal fun checkDeviceStatus(orgId: String, clientId: String) {
        val statusCheckerTask = QuickConnectDeviceStatusChecker(orgId, clientId, quickConnectEnvironment) { response ->
            handleStatusCheckResponse(orgId, clientId, response)
        }

        deviceStatusTaskHandle = executorService.schedule(statusCheckerTask, QuickConnect.STATUS_CHECK_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    /**
     * Cancels an ongoing quick connect device registration workflow (if any).
     */
    internal fun cancel() {
        cleanup()
    }

    /**
     * Handles the response from the device status check. Conditionally triggers a new status check
     * if the request was successful without session details.
     *
     * @param orgId the orgId for which quick connect was initiated
     * @param clientId the clientId for which quick connect was initiated
     * @param response the [Response] from the [checkDeviceStatus] request that is to be handled
     */
    private fun handleStatusCheckResponse(orgId: String, clientId: String, response: Response<HttpConnecting, AssuranceConnectionError>) {
        when (response) {
            is Response.Success -> {
                val sessionDetails = extractSessionDetails(StreamUtils.readAsString(response.data.inputStream))
                if (sessionDetails != null) {
                    // quick connect session details are available. Notify about successful the result.
                    Log.trace(LOG_TAG, LOG_SOURCE, "Received session details.")

                    quickConnectCallback.onSuccess(sessionDetails.sessionId, sessionDetails.token)
                    cleanup()
                    return
                }

                // The request was successful but the session data is not yet present.

                if (!isActive) {
                    // The workflow is likely cancelled due to user interaction. Do not retry.
                    Log.trace(LOG_TAG, LOG_SOURCE, "Will not retry. QuickConnect workflow already cancelled.")
                    return
                }

                if (++retryCount < QuickConnect.MAX_RETRY_COUNT) {
                    Log.trace(LOG_TAG, LOG_SOURCE, "Will retry device status check.")
                    checkDeviceStatus(orgId, clientId)
                } else {
                    // Maximum allowed retries for checking the status has been reached.
                    Log.trace(LOG_TAG, LOG_SOURCE, "Will not retry. Maximum allowed retries for status check have been reached.")
                    quickConnectCallback.onError(AssuranceConnectionError.RETRY_LIMIT_REACHED)
                    cleanup()
                }
            }

            is Response.Failure -> {
                Log.trace(LOG_TAG, LOG_SOURCE, "Device status check request failed.")
                quickConnectCallback.onError(response.error)
                cleanup()
            }
        }
    }

    /**
     * Extracts quick connect session details from the provided [jsonString].
     *
     * @return valid [QuickConnectSessionDetails] when successfully parsed;
     *         null if json string is empty, or details are unavailable.
     */
    private fun extractSessionDetails(jsonString: String?): QuickConnectSessionDetails? {
        if (jsonString.isNullOrEmpty()) return null

        return try {
            val jsonObject = JSONObject(JSONTokener(jsonString))
            val sessionUUID = jsonObject.optString(QuickConnect.KEY_SESSION_ID)
            val token = jsonObject.optString(QuickConnect.KEY_SESSION_TOKEN)
            if (StringUtils.isNullOrEmpty(sessionUUID) ||
                StringUtils.isNullOrEmpty(token) ||
                "null".equals(sessionUUID, true) ||
                "null".equals(token, true)
            ) {
                null
            } else {
                QuickConnectSessionDetails(sessionUUID, token)
            }
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Terminates any pending tasks and resets the state of this class.
     */
    private fun cleanup() {
        deviceCreationTaskHandle?.let {
            it.cancel(true)
            Log.trace(LOG_TAG, LOG_SOURCE, "QuickConnect device creation task cancelled")
        }.also { deviceCreationTaskHandle = null }

        deviceStatusTaskHandle?.let {
            it.cancel(true)
            Log.debug(LOG_TAG, LOG_SOURCE, "QuickConnect device status task cancelled")
        }.also { deviceStatusTaskHandle = null }

        retryCount = 0
        isActive = false
    }
}
