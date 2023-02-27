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

import com.adobe.marketing.mobile.Assurance.LOG_TAG
import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceQuickConnectError
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
import java.util.concurrent.TimeUnit

/**
 * Responsible for manging the workflow that registers the device as one capable for making a QuickConnect session.
 * A typical flow includes device creation, status checks with retries, success / failure notifications.
 */
internal class DeviceRegistrationManager(
    private val assuranceSharedStateManager: AssuranceStateManager,
    private val serviceProvider: ServiceProvider,
    private val executorService: ScheduledExecutorService,
    private val deviceRegistrationCallback: DeviceRegistrationCallback
) {

    /**
     * A data class to hold the details of the response from the create device API.
     */
    internal data class QuickConnectSessionDetails(val sessionId: String, val token: String)

    internal companion object {
        internal const val BASE_DEVICE_API_URL = "https://device.griffon.adobe.com/device"
        internal const val DEVICE_API_PATH_CREATE = "create"
        internal const val DEVICE_API_PATH_STATUS = "status"
        internal const val KEY_SESSION_ID = "sessionUuid"
        internal const val KEY_SESSION_TOKEN = "token"
        internal const val KEY_ORG_ID = "orgId"
        internal const val KEY_DEVICE_NAME = "deviceName"
        internal const val KEY_CLIENT_ID = "clientId"
        internal const val CONNECTION_TIMEOUT_MS = 5000
        internal const val READ_TIMEOUT_MS = 5000
        private const val MAX_RETRY_COUNT = 10
    }

    /**
     * Represents the number of retries made for a status check.
     */
    @Volatile
    private var retryCount = 0

    /**
     * A handle to the device creation task.
     */
    private var deviceCreationTaskHandle: Future<*>? = null

    /**
     * A handle to the device status check task.
     */
    private var deviceStatusTaskHandle: Future<*>? = null

    /**
     * Represents if there is an active attempt to initiate a QuickConnect session.
     */
    @Volatile
    private var isActive = false

    /**
     * Initiates device registration by triggering the [DeviceCreationTask]
     */
    internal fun register() {
        if (isActive) {
            return
        }

        isActive = true

        val deviceCreationTask = DeviceCreationTask(
            assuranceSharedStateManager.getOrgId(false),
            assuranceSharedStateManager.clientId,
            serviceProvider.deviceInfoService.deviceName,
            serviceProvider.networkService
        ) {
            when (it) {
                is Response.Success -> checkStatus()
                is Response.Failure -> deviceRegistrationCallback.onError(it.error.toString())
            }
        }

        deviceCreationTaskHandle = executorService.submit(deviceCreationTask)
    }

    /**
     * Cancels an ongoing device registration workflow (if any).
     */
    internal fun cancel() {
        deviceCreationTaskHandle?.let {
            Log.trace(LOG_TAG, LOG_TAG, "Creation task cancelled")
            it.cancel(true)
        }

        deviceStatusTaskHandle?.let {
            Log.debug(LOG_TAG, LOG_TAG, "Status task cancelled")
            it.cancel(true)
        }

        deviceRegistrationCallback.onError("Cancelled")
        cleanup()
    }

    private fun checkStatus() {
        val statusCheckerTask = DeviceStatusCheckerTask(
            assuranceSharedStateManager.getOrgId(false),
            assuranceSharedStateManager.clientId,
            serviceProvider.networkService
        ) { response ->
            handleStatusCheckResponse(response)
        }

        deviceStatusTaskHandle = executorService.schedule(statusCheckerTask, 5, TimeUnit.SECONDS)
    }

    private fun handleStatusCheckResponse(response: Response<HttpConnecting, AssuranceQuickConnectError>) {
        when (response) {
            is Response.Success -> {
                val sessionDetails = extractSessionDetails(StreamUtils.readAsString(response.data.inputStream))
                if (sessionDetails != null) {
                    deviceRegistrationCallback.onSuccess(sessionDetails.sessionId, sessionDetails.token)
                    cleanup()
                } else {
                    // The request was successful but the data is not yet present, retry
                    if (++retryCount < MAX_RETRY_COUNT) {
                        checkStatus()
                    } else {
                        deviceRegistrationCallback.onError(AssuranceQuickConnectError.RETRY_LIMIT_REACHED.toString())
                    }
                }
            }

            is Response.Failure -> {
                deviceRegistrationCallback.onError(response.error.toString())
            }
        }
    }

    private fun extractSessionDetails(jsonString: String?): QuickConnectSessionDetails? {
        if (jsonString.isNullOrEmpty()) return null

        return try {
            val jsonObject = JSONObject(JSONTokener(jsonString))
            val sessionUUID = jsonObject.optString(KEY_SESSION_ID)
            val token = jsonObject.optString(KEY_SESSION_TOKEN)
            if (StringUtils.isNullOrEmpty(sessionUUID) || StringUtils.isNullOrEmpty(token)) {
                null
            } else {
                QuickConnectSessionDetails(sessionUUID, token)
            }
        } catch (e: JSONException) {
            null
        }
    }

    private fun cleanup() {
        deviceCreationTaskHandle = null
        deviceStatusTaskHandle = null
        retryCount = 0
        isActive = false
    }
}
