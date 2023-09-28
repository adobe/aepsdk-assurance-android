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
import com.adobe.marketing.mobile.AdobeCallback
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONException
import org.json.JSONObject

internal class EventStitcher {

    companion object {
        const val LOG_TAG = "EventStitcher"
    }

    /**
     * Queue of events that are yet to be stitched i.e waiting for the final chunk. Stored as a
     * map with chunkId as the key and list of events as the value.
     */
    private val queue: MutableMap<String, MutableList<AssuranceEvent>>

    /**
     * A callback to notify the caller with the processed event (stitched or not stitched).
     */
    private val notifier: AdobeCallback<AssuranceEvent>

    constructor(notifier: AdobeCallback<AssuranceEvent>) : this(mutableMapOf(), notifier)

    constructor(queue: MutableMap<String, MutableList<AssuranceEvent>>, notifier: AdobeCallback<AssuranceEvent>) {
        this.queue = queue
        this.notifier = notifier
    }

    /**
     * Receives AssuranceEvents and stitches them if they are chunked and notifies the notifier with the stitched event.
     * If the event is not chunked, it is directly notified via the notifier.
     *
     * @param event the AssuranceEvent
     */
    @JvmName("onEvent")
    internal fun onEvent(event: AssuranceEvent) {
        if (!isChunked(event)) {
            notifier.call(event)
            return
        }

        val chunkId: String = event.metadata[AssuranceConstants.AssuranceEventKeys.CHUNK_ID] as String? ?: return
        val totalChunks: Int = event.metadata[AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL] as Int? ?: return

        // check if there are prior chunks associated with this chunkId in the queue, if not create a new queue
        val chunkedEventsForId: MutableList<AssuranceEvent> = queue[chunkId] ?: mutableListOf()
        chunkedEventsForId.add(event)

        // if all chunks are received, stitch the events and notify the caller
        if (chunkedEventsForId.size == totalChunks) {
            val result: Response<AssuranceEvent, Exception> = stitch(chunkedEventsForId)
            when (result) {
                is Response.Success -> notifier.call(result.data)
                is Response.Failure -> Log.error(Assurance.LOG_TAG, LOG_TAG, "Failed to stitch events for chunkId: $chunkId due to: ${result.error.message}")
            }
            // remove the event from the queue after stitching
            queue.remove(chunkId)
        } else {
            // else add the event to the queue for stitching once all pieces are received
            queue[chunkId] = chunkedEventsForId
        }
    }

    /**
     * Checks whether the event is chunked. An event is chunked if it has chunkId and chunkSequenceNumber in its metadata.
     * @param event the event to check
     * @return true if the event is chunked, false otherwise
     */
    @JvmName("isChunked")
    internal fun isChunked(event: AssuranceEvent): Boolean {
        if (event.metadata == null) return false

        // An event is chunked if it has chunkId and chunkSequenceNumber in its metadata
        val chunkId = event.metadata[AssuranceConstants.AssuranceEventKeys.CHUNK_ID] as String?
        val chunkSequenceNumber = event.metadata[AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER] as Int?
        return chunkId != null && chunkSequenceNumber != null
    }

    /**
     * Stitches the chunked events and returns the stitched event as part of the response.
     * @param chunkedEvents the list of chunked events to stitch
     * @return a stitched event as part of [Response.Success] or an exeption as part of [Response.Failure]
     */
    @VisibleForTesting
    internal fun stitch(chunkedEvents: MutableList<AssuranceEvent>): Response<AssuranceEvent, Exception> {
        if (chunkedEvents.isEmpty()) return Response.Failure(Exception("No events to stitch"))

        Log.trace(Assurance.LOG_TAG, LOG_TAG, "Stitching ${chunkedEvents.size} events")

        // sort the events by sequence number
        chunkedEvents.sortBy { it.metadata[AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER] as Int }

        // The eventType for the intended event should be the same for all chunked events with the same chunkId
        // use the initial event type as the eventType for the stitched event
        val eventType = chunkedEvents[0].eventType

        // The payload for the intended event is a concatenation of all the chunk data of chunked events
        val payload: StringBuilder = StringBuilder()
        chunkedEvents.forEach { chunkedEvent ->
            val chunkData =
                chunkedEvent.payload[AssuranceConstants.AssuranceEventKeys.CHUNK_DATA] as String?
            chunkData?.let {
                val utf8ChunkData = String(it.toByteArray(), Charsets.UTF_8)
                payload.append(utf8ChunkData)
            }
        }

        return try {
            val payloadJson = JSONObject(payload.toString())
            val payloadMap = JSONUtils.toMap(payloadJson)
            Response.Success(AssuranceEvent(eventType, payloadMap))
        } catch (e: JSONException) {
            Response.Failure(e)
        }
    }
}
