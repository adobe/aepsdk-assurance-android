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

package com.adobe.marketing.mobile.assurance;


import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.JSONObject;

/**
 * An {@link EventQueueWorker} that is responsible for sending {@link AssuranceEvent}'s to the
 * server over {@link AssuranceWebViewSocket}. Additionally, allows components controlling this
 * class to pause and resume processing (event forwarding). Note that the queue processing is paused
 * by default until controlling component invokes {@link #unblock()}.
 */
class OutboundEventQueueWorker extends EventQueueWorker<AssuranceEvent> {
    private static final String LOG_TAG = "OutboundEventQueueWorker";

    /**
     * Maximum number of bytes beyond which the {@code OutboundEventQueueWorker} fails to send data
     * over the socket.
     *
     * <p>Note that the data sent over {@link AssuranceWebViewSocket} is Base64 encoded. So the
     * limit for an event in Base64 terms is (UTF8_EVENT_SIZE_BYTES * 3)/4
     */
    @VisibleForTesting
    static final int MAX_EVENT_SIZE =
            (int) Math.floor((AssuranceWebViewSocket.MAX_DATA_LENGTH * 3) / 4.0);

    /**
     * Maximum number of bytes that the payload of the chunked AssuranceEvent can be comprised of,
     * to be processed by {@code OutboundEventQueueWorker}. This is inferred by doing the following
     * :
     *
     * <ol>
     *   <li>allot a default ceiling size of 2KB for the metadata associated with the {@code
     *       AssuranceEvent}
     *   <li/>
     *   <li>subtract above from {@code AssuranceWebViewSocket.MAX_DATA_LENGTH} that can be
     *       transported by the {@link AssuranceWebViewSocket}
     *   <li/>
     *   <li>reduce the remaining by half to accommodate JSON escaping with reconstruction
     *   <li/>
     *   <li>Since the data sent over {@code AssuranceWebViewSocket} is Base64 encoded, the limit
     *       for an event in Base64 terms is (UTF8_PAYLOAD_SIZE_BYTES) * (3/4)
     *   <li/>
     *       <ol/>
     */
    @VisibleForTesting
    static final int MAX_PAYLOAD_CHUNK_SIZE = (int) Math.floor((15 * 1024 * 3) / 4.0);

    private final AssuranceWebViewSocket socket;
    private final AssuranceClientInfo clientInfo;
    private final OutboundEventChunker outboundEventChunker;
    private volatile boolean canStartForwarding;

    OutboundEventQueueWorker(
            final ExecutorService executorService,
            final AssuranceWebViewSocket socket,
            final AssuranceClientInfo clientInfo) {
        this(
                executorService,
                socket,
                clientInfo,
                new LinkedBlockingQueue<AssuranceEvent>(),
                new OutboundEventChunker(MAX_PAYLOAD_CHUNK_SIZE));
    }

    @VisibleForTesting
    OutboundEventQueueWorker(
            final ExecutorService executorService,
            final AssuranceWebViewSocket socket,
            final AssuranceClientInfo clientInfo,
            final LinkedBlockingQueue<AssuranceEvent> queue,
            final OutboundEventChunker outboundEventChunker) {
        super(executorService, queue);
        this.socket = socket;
        this.clientInfo = clientInfo;
        this.outboundEventChunker = outboundEventChunker;
        canStartForwarding = false;
    }

    @Override
    protected void prepare() {
        // Sends a "client info event" before the rest of the queue processing starts.
        sendClientInfoEvent();
    }

    @Override
    protected boolean canWork() {
        // Ensure that the events are only attempted if the OutboundEventQueueWorker is unblocked
        // and
        // the socket is connected.
        return canStartForwarding
                && !(socket == null
                        || socket.getState() != AssuranceWebViewSocket.SocketReadyState.OPEN);
    }

    @Override
    protected void doWork(AssuranceEvent assuranceEvent) {
        sendEventToSocket(assuranceEvent);
    }

    /** Pauses any further events being sent by blocking queue processing. */
    void block() {
        canStartForwarding = false;
    }

    /** Resumes sending events by unblocking queue processing. */
    void unblock() {
        canStartForwarding = true;
        resume();
    }

    /**
     * Creates and sends the clientInfo event to Assurance only if the the worker is blocked.
     * Invocation will be a no-op if the worker is already unblocked to prevent unnecessary client
     * info events being sent.
     *
     * <p>This is the first event sent after a successful socket connection. This {@link
     * AssuranceEvent} is not queued. ClientInfoEvent contains three major keys :
     *
     * <ul>
     *   <li>Version : Representing the version of the Assurance SDK
     *   <li>AppSettings : A json representing AndroidManifest file
     *   <li>DeviceInfo : A map representing device information and current device state
     * </ul>
     */
    void sendClientInfoEvent() {
        if (canStartForwarding) {
            return;
        }

        Log.debug(Assurance.LOG_TAG, LOG_TAG, "Sending client info event to Assurance");
        final AssuranceEvent clientInfoEvent =
                new AssuranceEvent(
                        AssuranceConstants.AssuranceEventType.CLIENT, clientInfo.getData());
        sendEventToSocket(clientInfoEvent);
    }

    /**
     * Sends the provided {@link AssuranceEvent} to Assurance via the connected socket connection.
     *
     * @param event the {@link AssuranceEvent} the needs to be sent.
     */
    private void sendEventToSocket(final AssuranceEvent event) {
        if (event == null) {
            Log.error(Assurance.LOG_TAG, LOG_TAG, "Cannot send null event.");
            return;
        }

        try {
            final byte[] eventData =
                    event.getJSONRepresentation().getBytes(Charset.forName("UTF-8"));

            // Check if the AssuranceEvent is within transportable limits, if not, perform chunking
            // and resend resulting chunks.
            if (eventData.length < MAX_EVENT_SIZE) {
                socket.sendData(eventData);
            } else {
                if (event.getPayload() == null) {
                    // The payload is null and the event size exceeds MAX_EVENT_SIZE. This implies
                    // that
                    // the metadata is contributing to the event size increase. Metadata currently
                    // is data about
                    // chunks. It follows that metadata cannot be chunked. The current logic assumes
                    // that
                    // metadata is always within a sane limit (as it is being added internally) and
                    // any event
                    // with a large metadata cannot be handled currently. So, discard this event.
                    // When Assurance event is publicly instantiable, this assumption about metadata
                    // does not hold.
                    // If such a case arises, then the AssuranceEvent creation MUST handle
                    // restricting the size
                    // of metadata accordingly.
                    Log.warning(
                            Assurance.LOG_TAG,
                            LOG_TAG,
                            "Cannot send eventId: %s that exceeds permitted limit"
                                    + "but has an empty payload!",
                            event.eventID);
                    return;
                }

                final List<AssuranceEvent> chunkedEvents = outboundEventChunker.chunk(event);

                for (final AssuranceEvent chunkedEvent : chunkedEvents) {
                    socket.sendData(
                            chunkedEvent
                                    .getJSONRepresentation()
                                    .getBytes(Charset.forName("UTF-8")));
                }
            }
        } catch (final UnsupportedCharsetException ex) {
            // This can be thrown by Charset.forName(*). However, it is unlikely to reach here
            // as we hardcode the charset name to UTF-8.
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format(
                            "UnsupportedCharsetException while converting Assurance event object"
                                    + " to bytes representation: %s",
                            ex.getLocalizedMessage()));
        }
    }

    @Override
    void stop() {
        super.stop();
        canStartForwarding = false;
    }

    /**
     * Responsible for splitting {@code AssuranceEvent} with large payloads to be below the
     * configured limit of the socket.
     */
    @VisibleForTesting
    static class OutboundEventChunker implements EventChunker<AssuranceEvent, AssuranceEvent> {
        private final int maxChunkSize;

        OutboundEventChunker(final int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
        }

        /**
         * Converts {@param AssuranceEvent} into {@code AssuranceEvent}'s with payloads below {@code
         * maxChunkSize}
         *
         * @param event AssuranceEvent that needs to be chunked.
         * @return an empty List if the {@param event} is null; singleton List comprising of {@param
         *     event} if payload is null; singleton List comprising of {@param event} if content is
         *     within {@code maxChunkSize}; {@code List<AssuranceEvent>} resulting after chunking
         *     otherwise.
         */
        @Override
        public List<AssuranceEvent> chunk(final AssuranceEvent event) {
            if (event == null) {
                return Collections.EMPTY_LIST;
            }

            // Return the same event if the original payload is null. Nothing to chunk.
            if (event.getPayload() == null) {
                Log.warning(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Cannot chunk event: %s with an empty payload!",
                        event.eventID);
                return Collections.singletonList(event);
            }

            final JSONObject payloadJson = new JSONObject(event.getPayload());
            final byte[] payloadBytes = payloadJson.toString().getBytes(Charset.forName("UTF-8"));

            // Original payload is within deliverable limit. Nothing to chunk.
            if (payloadBytes.length < maxChunkSize) {
                return Collections.singletonList(event);
            }

            final List<AssuranceEvent> chunkedEvents = new ArrayList<>();
            final double totalChunks = Math.ceil(payloadBytes.length / (double) maxChunkSize);
            final ByteArrayInputStream byteArrayInputStream =
                    new ByteArrayInputStream(payloadBytes);
            final byte[] buffer = new byte[maxChunkSize];

            try {
                final String chunkId = UUID.randomUUID().toString();
                int chunkNumber = 0;

                while (byteArrayInputStream.read(buffer) != -1) {
                    final HashMap<String, Object> payload = new HashMap<>();
                    payload.put(
                            AssuranceConstants.AssuranceEventKeys.CHUNK_DATA,
                            new String(buffer, Charset.forName("UTF-8")));

                    final HashMap<String, Object> metadata = new HashMap<>();
                    metadata.put(AssuranceConstants.AssuranceEventKeys.CHUNK_ID, chunkId);
                    metadata.put(
                            AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL, (int) totalChunks);
                    metadata.put(
                            AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER,
                            chunkNumber++);

                    final AssuranceEvent assuranceEvent =
                            new AssuranceEvent(
                                    event.vendor, event.type, metadata, payload, event.timestamp);
                    chunkedEvents.add(assuranceEvent);
                }
            } catch (final IOException e) {
                Log.warning(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Failed to chunk event with ID: %s. Exception: %s",
                        event.eventID,
                        e.getMessage());
                return Collections.EMPTY_LIST;
            }

            return chunkedEvents;
        }
    }
}
