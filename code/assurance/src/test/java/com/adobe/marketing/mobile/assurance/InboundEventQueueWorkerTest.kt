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

import com.adobe.marketing.mobile.util.SerialWorkDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class InboundEventQueueWorkerTest {

    @Mock
    private lateinit var mockSerialWorkDispatcher: SerialWorkDispatcher<AssuranceEvent>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `InboundEventQueueWorker start() calls work dispatcher when dispatcher is NOT_STARTED`() {
        // setup
        val worker = InboundEventQueueWorker(mockSerialWorkDispatcher)
        `when`(mockSerialWorkDispatcher.getState()).thenReturn(SerialWorkDispatcher.State.NOT_STARTED)

        // test
        worker.start()

        // verify
        verify(mockSerialWorkDispatcher).start()
    }

    @Test
    fun `InboundEventQueueWorker start() does not start work dispatcher when dispatcher is ACTIVE`() {
        // setup
        val worker = InboundEventQueueWorker(mockSerialWorkDispatcher)
        `when`(mockSerialWorkDispatcher.getState()).thenReturn(SerialWorkDispatcher.State.ACTIVE)

        // test
        worker.start()

        // verify
        verify(mockSerialWorkDispatcher, never()).start()
    }

    @Test
    fun `InboundEventQueueWorker start() does not start work dispatcher when dispatcher is PAUSED`() {
        // setup
        val worker = InboundEventQueueWorker(mockSerialWorkDispatcher)
        `when`(mockSerialWorkDispatcher.getState()).thenReturn(SerialWorkDispatcher.State.PAUSED)

        // test
        worker.start()

        // verify
        verify(mockSerialWorkDispatcher, never()).start()
    }

    @Test
    fun `InboundEventQueueWorker start() does not start work dispatcher when dispatcher is SHUTDOWN`() {
        // setup
        val worker = InboundEventQueueWorker(mockSerialWorkDispatcher)
        `when`(mockSerialWorkDispatcher.getState()).thenReturn(SerialWorkDispatcher.State.SHUTDOWN)

        // test
        worker.start()

        // verify
        verify(mockSerialWorkDispatcher, never()).start()
    }

    @Test
    fun `InboundEventQueueWorker queues events to the dispatcher when dispatcher is started`() {
        // setup
        val worker = InboundEventQueueWorker(mockSerialWorkDispatcher)
        `when`(mockSerialWorkDispatcher.getState()).thenReturn(SerialWorkDispatcher.State.ACTIVE)

        // test
        val mockAssuranceEvent = Mockito.mock(AssuranceEvent::class.java)
        worker.offer(mockAssuranceEvent)

        // verify
        verify(mockSerialWorkDispatcher).offer(mockAssuranceEvent)
    }

    @Test
    fun `InboundEventQueueWorker does not queue events to the dispatcher when dispatcher is SHUTDOWN`() {
        // setup
        val worker = InboundEventQueueWorker(mockSerialWorkDispatcher)
        `when`(mockSerialWorkDispatcher.getState()).thenReturn(SerialWorkDispatcher.State.SHUTDOWN)

        // test
        val mockAssuranceEvent = Mockito.mock(AssuranceEvent::class.java)
        worker.offer(mockAssuranceEvent)

        // verify
        verify(mockSerialWorkDispatcher, never()).offer(mockAssuranceEvent)
    }

    @Test
    fun `InboundEventQueueWorker stop() shuts down the dispatcher`() {
        // setup
        val worker = InboundEventQueueWorker(mockSerialWorkDispatcher)

        // test
        worker.stop()

        // verify
        verify(mockSerialWorkDispatcher).shutdown()
    }

    @Test
    fun `InboundEventQueueWorker's WorkHandlerImpl does not process null control types`() {
        // setup
        val mockEventStitcher: EventStitcher = Mockito.mock(EventStitcher::class.java)
        val workHandlerImpl: InboundEventQueueWorker.WorkHandlerImpl = InboundEventQueueWorker.WorkHandlerImpl(mockEventStitcher)
        val mockAssuranceEvent: AssuranceEvent = Mockito.mock(AssuranceEvent::class.java)
        `when`(mockAssuranceEvent.controlType).thenReturn(null)

        // test
        workHandlerImpl.doWork(mockAssuranceEvent)

        // verify
        verify(mockEventStitcher, never()).onEvent(mockAssuranceEvent)
    }

    @Test
    fun `InboundEventQueueWorker's WorkHandlerImpl notifies event stitcher`() {
        // setup
        val mockEventStitcher: EventStitcher = Mockito.mock(EventStitcher::class.java)
        val workHandlerImpl: InboundEventQueueWorker.WorkHandlerImpl = InboundEventQueueWorker.WorkHandlerImpl(mockEventStitcher)
        val mockAssuranceEvent: AssuranceEvent = Mockito.mock(AssuranceEvent::class.java)
        `when`(mockAssuranceEvent.controlType).thenReturn(AssuranceConstants.ControlType.FAKE_EVENT)

        // test
        workHandlerImpl.doWork(mockAssuranceEvent)

        // verify
        verify(mockEventStitcher).onEvent(mockAssuranceEvent)
    }

    @After
    fun tearDown() {
    }
}
