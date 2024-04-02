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

package com.adobe.marketing.mobile.assurance.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class AssuranceAppStateTest {

    @Test
    fun `Test initial state of AssuranceAppState`() {
        val assuranceAppState = AssuranceAppState()
        val expectedSessionPhase = AssuranceAppState.SessionPhase.Disconnected(null, false)
        val expectedStatusLogs = emptyList<AssuranceAppState.StatusLog>()
        assertEquals(expectedSessionPhase, assuranceAppState.sessionPhase.value)
        assertEquals(expectedStatusLogs, assuranceAppState.statusLogs.value)
    }

    @Test
    fun `Test #onSessionPhaseChange`() {
        val assuranceAppState = AssuranceAppState()
        val expectedSessionPhase = AssuranceAppState.SessionPhase.Disconnected(null, false)
        assertEquals(expectedSessionPhase, assuranceAppState.sessionPhase.value)

        val newSessionPhase = AssuranceAppState.SessionPhase.Authorizing(AssuranceAppState.AssuranceAuthorization.QuickConnect())
        assuranceAppState.onSessionPhaseChange(newSessionPhase)
        assertEquals(newSessionPhase, assuranceAppState.sessionPhase.value)

        val anotherSessionPhase = AssuranceAppState.SessionPhase.Disconnected(AssuranceConstants.AssuranceConnectionError.CLIENT_ERROR, true)
        assuranceAppState.onSessionPhaseChange(anotherSessionPhase)
        assertEquals(anotherSessionPhase, assuranceAppState.sessionPhase.value)
    }

    @Test
    fun `Test #onStatusLog`() {
        val assuranceAppState = AssuranceAppState()
        val expectedStatusLogs = emptyList<AssuranceAppState.StatusLog>()
        assertEquals(expectedStatusLogs, assuranceAppState.statusLogs.value)
        val message = "Test message"
        assuranceAppState.logStatus(AssuranceConstants.UILogColorVisibility.HIGH, message)
        assuranceAppState.statusLogs.value.let {
            assertEquals(1, it.size)
            assertEquals(AssuranceConstants.UILogColorVisibility.HIGH, it[0].level)
            assertEquals(message, it[0].message)
        }

        val anotherMessage = "Another test message"
        assuranceAppState.logStatus(AssuranceConstants.UILogColorVisibility.LOW, anotherMessage)
        assuranceAppState.statusLogs.value.let {
            assertEquals(2, it.size)
            assertEquals(AssuranceConstants.UILogColorVisibility.LOW, it[1].level)
            assertEquals(anotherMessage, it[1].message)
        }
    }
}
