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

package com.adobe.marketing.mobile.assurance

import androidx.annotation.VisibleForTesting
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.services.Log

/**
 * Provides components necessary for native presentations to interact with the session.
 * This should be initialized when the AssuranceExtension is created to ensure that the
 * components are available.
 */
internal object AssuranceComponentRegistry {
    private const val LOG_SOURCE = "AssuranceComponentRegistry"

    internal var assuranceStateManager: AssuranceStateManager? = null
        private set

    internal var sessionUIOperationHandler: SessionUIOperationHandler? = null
        private set

    @JvmField
    internal val appState: AssuranceAppState = AssuranceAppState()

    @JvmName("initialize")
    @Synchronized
    internal fun initialize(
        assuranceStateManager: AssuranceStateManager,
        uiOperationHandler: SessionUIOperationHandler
    ) {
        if (this.assuranceStateManager != null || this.sessionUIOperationHandler != null) {
            Log.warning(Assurance.LOG_TAG, LOG_SOURCE, "Components already initialized.")
            return
        }

        this.assuranceStateManager = assuranceStateManager
        this.sessionUIOperationHandler = uiOperationHandler
    }

    /**
     * Exists ONLY for testing purposes for resetting the state of the registry.
     */
    @VisibleForTesting
    internal fun reset() {
        assuranceStateManager = null
        sessionUIOperationHandler = null
    }
}
