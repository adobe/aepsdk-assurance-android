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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class InboundEventWorkerTests {

    companion object {
        private const val WAIT_TIME_SECONDS = 5L
    }

    @Test
    fun testInboundEventWorkerDoesNotNotifyListenerOnEventWhenNotStarted() {
        val countDownLatch = CountDownLatch(1)
        val inboundQueueEventListener: InboundEventQueueWorker.InboundQueueEventListener =
            object : InboundEventQueueWorker.InboundQueueEventListener {
                override fun onInboundEvent(event: AssuranceEvent) {
                    fail("Inbound event listener should not be called when worker is not started")
                }
            }
        val inboundEventQueueWorker = InboundEventQueueWorker(inboundQueueEventListener)

        val event1 = constructAssuranceControlEvent(mutableMapOf("key1" to "value1"))
        val event2 = constructAssuranceControlEvent(mutableMapOf("key2" to "value2"))
        val event3 = constructAssuranceControlEvent(mutableMapOf("key3" to "value3"))

        inboundEventQueueWorker.offer(event1)
        inboundEventQueueWorker.offer(event2)
        inboundEventQueueWorker.offer(event3)

        assertFalse(countDownLatch.await(WAIT_TIME_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun testInboundEventWorkerNotifiesListenerOnEventWhenStarted() {
        val countDownLatch = CountDownLatch(1)
        val eventsCollected = mutableListOf<AssuranceEvent>()
        val inboundQueueEventListener: InboundEventQueueWorker.InboundQueueEventListener =
            object : InboundEventQueueWorker.InboundQueueEventListener {
                override fun onInboundEvent(event: AssuranceEvent) {
                    eventsCollected.add(event)
                    if (eventsCollected.size == 3) {
                        countDownLatch.countDown()
                    }
                }
            }
        val inboundEventQueueWorker = InboundEventQueueWorker(inboundQueueEventListener)

        val event1 = constructAssuranceControlEvent(mutableMapOf("key1" to "value1"))
        val event2 = constructAssuranceControlEvent(mutableMapOf("key2" to "value2"))
        val event3 = constructAssuranceControlEvent(mutableMapOf("key3" to "value3"))

        inboundEventQueueWorker.start()

        inboundEventQueueWorker.offer(event1)
        inboundEventQueueWorker.offer(event2)
        inboundEventQueueWorker.offer(event3)

        assertTrue(countDownLatch.await(WAIT_TIME_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun testInboundEventWorkerQueuesAndNotifiesListenerOnEvents() {
        val countDownLatch = CountDownLatch(1)
        val eventsCollected = mutableListOf<AssuranceEvent>()
        val inboundQueueEventListener: InboundEventQueueWorker.InboundQueueEventListener =
            object : InboundEventQueueWorker.InboundQueueEventListener {
                override fun onInboundEvent(event: AssuranceEvent) {
                    eventsCollected.add(event)
                    if (eventsCollected.size == 3) {
                        countDownLatch.countDown()
                    }
                }
            }
        val inboundEventQueueWorker = InboundEventQueueWorker(inboundQueueEventListener)

        val event1 = constructAssuranceControlEvent(mutableMapOf("key1" to "value1"))
        val event2 = constructAssuranceControlEvent(mutableMapOf("key2" to "value2"))
        val event3 = constructAssuranceControlEvent(mutableMapOf("key3" to "value3"))

        inboundEventQueueWorker.offer(event1)
        inboundEventQueueWorker.offer(event2)
        inboundEventQueueWorker.offer(event3)

        inboundEventQueueWorker.start()

        assertTrue(countDownLatch.await(WAIT_TIME_SECONDS, TimeUnit.SECONDS))
    }

    private fun constructAssuranceControlEvent(payload: MutableMap<String, Any>): AssuranceEvent {
        payload["type"] = AssuranceConstants.AssuranceEventType.CONTROL
        return AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload)
    }
}
