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

package com.adobe.marketing.mobile.assurance.testapp

import android.app.Application
import android.os.FileUtils
import android.util.Log
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Lifecycle
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.Places
import com.adobe.marketing.mobile.Signal

class AssuranceTestApp : Application() {

    companion object {
        private const val TAG = "App"
        private const val APP_ID = "YOUR_APP_ID"
    }

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        try {
            MobileCore.registerExtensions(
                listOf(
                    Assurance.EXTENSION,
                    Places.EXTENSION,
                    Lifecycle.EXTENSION,
                )
            ) {
                //MobileCore.configureWithAppID(APP_ID)
                MobileCore.configureWithAppID("94f571f308d5/d9220cd8c3aa/launch-2e799e530b10-development")
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "")
        }
    }

}