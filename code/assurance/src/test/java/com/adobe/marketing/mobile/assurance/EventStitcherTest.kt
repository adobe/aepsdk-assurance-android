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

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.UUID

class EventStitcherTest {

    private lateinit var eventStitcher: EventStitcher
    private val queue: MutableMap<String, MutableList<AssuranceEvent>> = mutableMapOf()
    private val stitchedEvents = mutableListOf<AssuranceEvent>()

    @Before
    fun setUp() {
        eventStitcher = EventStitcher(queue) {
            stitchedEvents.add(it)
        }
    }

    @Test
    fun `onEvent should call notifier with same event if event is not chunked`() {
        // setup
        val nonChunkedEvent = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mutableMapOf(),
            mutableMapOf(),
            System.currentTimeMillis()
        )

        // test
        eventStitcher.onEvent(nonChunkedEvent)

        // verify
        assert(stitchedEvents.size == 1)
        assert(stitchedEvents[0] == nonChunkedEvent)
    }

    @Test
    fun `onEvent drops chunked event when event does not have appropriate metadata details`() {
        val metadata = mutableMapOf<String, Any>(
            AssuranceConstants.AssuranceEventKeys.CHUNK_ID to "myChunkId"
            // no CHUNK_TOTAL or CHUNK_SEQUENCE_NUMBER
        )
        val chunkedEvent = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            metadata,
            mutableMapOf(),
            System.currentTimeMillis()
        )

        // test
        eventStitcher.onEvent(chunkedEvent)

        // verify
        assert(stitchedEvents.isEmpty())
    }

    @Test
    fun `onEvent should cal notifier when final chunk arrives`() {
        val chunkId = UUID.randomUUID().toString()
        val chunkedEventPart1 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to chunkId,
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 0,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 3
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to "{ \"myKey\": { \"str\":"
            ),
            System.currentTimeMillis()
        )

        // simulate event 1 incidence
        eventStitcher.onEvent(chunkedEventPart1)
        assertTrue(stitchedEvents.isEmpty())
        assertTrue(queue.size == 1)
        assertEquals(1, queue[chunkId]?.size)

        val chunkedEventPart2 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to chunkId,
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 1,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 3
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to " \"Hello\", \"num\": 56000,"
            ),
            System.currentTimeMillis()
        )

        // simulate event 2 incidence
        eventStitcher.onEvent(chunkedEventPart2)
        assert(stitchedEvents.size == 0)
        assert(queue.size == 1)
        assertEquals(2, queue[chunkId]?.size)

        val chunkedEventPart3 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to chunkId,
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 2,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 3
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to " \"bool\": true } }"
            ),
            System.currentTimeMillis()
        )

        // simulate event 3 incidence
        eventStitcher.onEvent(chunkedEventPart3)

        assertEquals(1, stitchedEvents.size)
        assertTrue(queue.isEmpty())
        assertEquals(AssuranceConstants.AssuranceEventType.CONTROL, stitchedEvents[0].type)
        assertEquals(AssuranceConstants.VENDOR_ASSURANCE_MOBILE, stitchedEvents[0].vendor)

        val eventPayload = stitchedEvents[0].payload
        assertNotNull(eventPayload)
        assertEquals(
            eventPayload["myKey"],
            mapOf("str" to "Hello", "num" to 56000, "bool" to true)
        )
    }

    @Test
    fun `onEvent bails and does not throw on bad payload`() {
        val chunkedEventPart1 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to "myChunkId",
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 0,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 2
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to "someRandomStringThatIsNotJson"
            ),
            System.currentTimeMillis()
        )

        // simulate event 1 incidence
        eventStitcher.onEvent(chunkedEventPart1)
        assertTrue(stitchedEvents.isEmpty())
        assertEquals(1, queue.size)

        val chunkedEventPart2 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to "myChunkId",
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 1,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 2
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to "anotherRandomStringThatIsNotJson"
            ),
            System.currentTimeMillis()
        )

        // simulate event 2 incidence
        try {
            eventStitcher.onEvent(chunkedEventPart2)
            assertTrue(stitchedEvents.isEmpty())
        } catch (e: Exception) {
            fail()
        }
    }

    @Test
    fun `stitch fails on bad payload`() {
        val chunkedEventPart1 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to "myChunkId",
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 0,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 2
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to "someRandomStringThatIsNotJson"
            ),
            System.currentTimeMillis()
        )

        val chunkedEventPart2 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to "myChunkId",
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 1,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 2
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to "anotherRandomStringThatIsNotJson"
            ),
            System.currentTimeMillis()
        )

        val response = eventStitcher.stitch(mutableListOf(chunkedEventPart1, chunkedEventPart2))
        assertTrue(response is Response.Failure)
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `stitch succeeds on valid payload`() {
        val chunkedEventPart1 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to "myChunkId",
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 0,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 3
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to "{ \"myKey\": { \"str\":"
            ),
            System.currentTimeMillis()
        )

        val chunkedEventPart2 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to "myChunkId",
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 1,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 3
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to " \"Hello\", \"num\": 56000,"
            ),
            System.currentTimeMillis()
        )

        val chunkedEventPart3 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to "myChunkId",
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 2,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 3
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to " \"bool\": true } }"
            ),
            System.currentTimeMillis()
        )

        val response: Response<AssuranceEvent, Exception> = eventStitcher.stitch(
            mutableListOf(
                chunkedEventPart1,
                chunkedEventPart2,
                chunkedEventPart3
            )
        )

        assertTrue(response is Response.Success)
        val stitchedEvent = (response as Response.Success).data
        assertNotNull(stitchedEvent)
        assertEquals(AssuranceConstants.AssuranceEventType.CONTROL, stitchedEvent.type)
        assertEquals(AssuranceConstants.VENDOR_ASSURANCE_MOBILE, stitchedEvent.vendor)

        val eventPayload = stitchedEvent.payload
        assertNotNull(eventPayload)
        assertEquals(
            eventPayload["myKey"],
            mapOf("str" to "Hello", "num" to 56000, "bool" to true)
        )

        assertTrue(queue.isEmpty())
    }

    @Test
    fun `stitch event allows UTF-8 characters`() {
        val chunkId = UUID.randomUUID().toString()
        val chunkedEventPart1 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to chunkId,
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 0,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 3
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to "{ \"myKey\": { \"str\":"
            ),
            System.currentTimeMillis()
        )

        // simulate event 1 incidence
        eventStitcher.onEvent(chunkedEventPart1)
        assertTrue(stitchedEvents.isEmpty())
        assertTrue(queue.size == 1)
        assertEquals(1, queue[chunkId]?.size)

        val chunkedEventPart2 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to chunkId,
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 1,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 3
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to " \"Hello みみみみみみ\", \"num\": 56000,"
            ),
            System.currentTimeMillis()
        )

        // simulate event 2 incidence
        eventStitcher.onEvent(chunkedEventPart2)
        assert(stitchedEvents.size == 0)
        assert(queue.size == 1)
        assertEquals(2, queue[chunkId]?.size)

        val chunkedEventPart3 = AssuranceEvent(
            AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
            AssuranceConstants.AssuranceEventType.CONTROL,
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_ID to chunkId,
                AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER to 2,
                AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL to 3
            ),
            mapOf(
                AssuranceConstants.AssuranceEventKeys.CHUNK_DATA to " \"bool\": true } }"
            ),
            System.currentTimeMillis()
        )

        // simulate event 3 incidence
        eventStitcher.onEvent(chunkedEventPart3)

        assertEquals(1, stitchedEvents.size)
        assertTrue(queue.isEmpty())
        assertEquals(AssuranceConstants.AssuranceEventType.CONTROL, stitchedEvents[0].type)
        assertEquals(AssuranceConstants.VENDOR_ASSURANCE_MOBILE, stitchedEvents[0].vendor)

        val eventPayload = stitchedEvents[0].payload
        assertNotNull(eventPayload)
        assertEquals(
            eventPayload["myKey"],
            mapOf("str" to "Hello みみみみみみ", "num" to 56000, "bool" to true)
        )
    }

    @After
    fun tearDown() {
    }
}
