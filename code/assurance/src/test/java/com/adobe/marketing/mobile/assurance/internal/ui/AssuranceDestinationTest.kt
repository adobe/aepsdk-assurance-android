/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal.ui

import com.adobe.marketing.mobile.assurance.internal.AssuranceAppState
import com.adobe.marketing.mobile.assurance.internal.AssuranceConstants
import org.junit.Test
import kotlin.test.assertEquals

class AssuranceDestinationTest {

    @Test
    fun `Test AssuranceDestination#fromSessionPhase - Connected`() {
        assertEquals(
            AssuranceDestination.StatusDestination,
            AssuranceDestination.fromSessionPhase(AssuranceAppState.SessionPhase.Connected)
        )
    }

    @Test
    fun `Test AssuranceDestination#fromSessionPhase - Disconnected`() {
        assertEquals(
            AssuranceDestination.Unknown,
            AssuranceDestination.fromSessionPhase(AssuranceAppState.SessionPhase.Disconnected())
        )

        assertEquals(
            AssuranceDestination.ErrorDestination(AssuranceAppState.SessionPhase.Disconnected(error = AssuranceConstants.AssuranceConnectionError.SESSION_DELETED)),
            AssuranceDestination.fromSessionPhase(AssuranceAppState.SessionPhase.Disconnected(error = AssuranceConstants.AssuranceConnectionError.SESSION_DELETED))
        )

        assertEquals(
            AssuranceDestination.StatusDestination,
            AssuranceDestination.fromSessionPhase(
                AssuranceAppState.SessionPhase.Disconnected(
                    reconnecting = true
                )
            )
        )
    }

    @Test
    fun `Test AssuranceDestination#fromSessionPhase - Connecting`() {
        val sessionId = "somesessionid"
        val environment = AssuranceConstants.AssuranceEnvironment.QA
        assertEquals(
            AssuranceDestination.PinDestination(
                AssuranceAppState.AssuranceAuthorization.PinConnect(sessionId, environment)
            ),
            AssuranceDestination.fromSessionPhase(
                AssuranceAppState.SessionPhase.Authorizing(
                    AssuranceAppState.AssuranceAuthorization.PinConnect(sessionId, environment)
                )
            )
        )

        assertEquals(
            AssuranceDestination.QuickConnectDestination(AssuranceAppState.AssuranceAuthorization.QuickConnect(environment)),
            AssuranceDestination.fromSessionPhase(
                AssuranceAppState.SessionPhase.Authorizing(AssuranceAppState.AssuranceAuthorization.QuickConnect(environment))
            )
        )
    }
}
