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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class OutboundEventChunkerTest {

    @Test
    public void test_chunk_nullEvent() {
        final OutboundEventQueueWorker.OutboundEventChunker outboundEventChunker =
                new OutboundEventQueueWorker.OutboundEventChunker(
                        OutboundEventQueueWorker.MAX_PAYLOAD_CHUNK_SIZE);

        // Test
        List<AssuranceEvent> chunkedEvents = outboundEventChunker.chunk(null);

        // Verify that the chunking results in an empty list.
        assertEquals(0, chunkedEvents.size());
    }

    @Test
    public void test_chunk_nullPayload() {
        final OutboundEventQueueWorker.OutboundEventChunker outboundEventChunker =
                new OutboundEventQueueWorker.OutboundEventChunker(
                        OutboundEventQueueWorker.MAX_PAYLOAD_CHUNK_SIZE);
        final AssuranceEvent assuranceEvent =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.GENERIC, null);

        // Test
        final List<AssuranceEvent> chunkedEvents = outboundEventChunker.chunk(assuranceEvent);

        // Verify that the chunking results in the same event.
        assertEquals(1, chunkedEvents.size());
        assertEquals(assuranceEvent, chunkedEvents.get(0));
    }

    @Test
    public void test_chunk_eventSizeWthinLimit() {
        final OutboundEventQueueWorker.OutboundEventChunker outboundEventChunker =
                new OutboundEventQueueWorker.OutboundEventChunker(
                        OutboundEventQueueWorker.MAX_PAYLOAD_CHUNK_SIZE);
        final HashMap<String, Object> payload = new HashMap<>();
        payload.put("Key1", "Value1");
        payload.put("Key2", "Value2");

        // Test
        final AssuranceEvent assuranceEvent =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.GENERIC, payload);
        final List<AssuranceEvent> chunkedEvents = outboundEventChunker.chunk(assuranceEvent);

        // Verify that the chunking results in the same event.
        assertEquals(1, chunkedEvents.size());
        assertEquals(assuranceEvent, chunkedEvents.get(0));
        assertEquals(assuranceEvent.getPayload(), chunkedEvents.get(0).getPayload());
    }

    @Test
    public void test_chunk_eventPayloadSize5KB() {
        try {
            // Prepare & read large payload from resources.
            final String expectedPayloadKeyValue =
                    readPayloadFromResource("assurance_event_payload_key_value_5KB.txt");

            // Construct expected event payload
            final HashMap<String, Object> expectedEventPayload = new HashMap<>();
            expectedEventPayload.put("largeKey", expectedPayloadKeyValue);
            final AssuranceEvent largeAssuranceEvent =
                    new AssuranceEvent(
                            AssuranceConstants.AssuranceEventType.GENERIC, expectedEventPayload);

            final OutboundEventQueueWorker.OutboundEventChunker outboundEventChunker =
                    new OutboundEventQueueWorker.OutboundEventChunker(
                            OutboundEventQueueWorker.MAX_PAYLOAD_CHUNK_SIZE);

            final List<AssuranceEvent> chunkedEvents =
                    outboundEventChunker.chunk(largeAssuranceEvent);

            // Verify that the chunking results in the same event.
            assertEquals(1, chunkedEvents.size());
            assertEquals(largeAssuranceEvent, chunkedEvents.get(0));
            assertEquals(largeAssuranceEvent.getPayload(), chunkedEvents.get(0).getPayload());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void test_chunk_eventPayloadSize40KB() {
        try {
            // Prepare & read large payload from resources.
            final String expectedPayloadKeyValue =
                    readPayloadFromResource("assurance_large_event_payload_key_value_40KB.txt");

            // Construct expected event payload
            final HashMap<String, Object> expectedEventPayload = new HashMap<>();
            expectedEventPayload.put("largeKey", expectedPayloadKeyValue);
            final AssuranceEvent largeAssuranceEvent =
                    new AssuranceEvent(
                            AssuranceConstants.AssuranceEventType.GENERIC, expectedEventPayload);

            final OutboundEventQueueWorker.OutboundEventChunker outboundEventChunker =
                    new OutboundEventQueueWorker.OutboundEventChunker(
                            OutboundEventQueueWorker.MAX_PAYLOAD_CHUNK_SIZE);

            final List<AssuranceEvent> chunkedEvents =
                    outboundEventChunker.chunk(largeAssuranceEvent);
            assertEquals(4, chunkedEvents.size());

            final StringBuilder actualPayloadValue = new StringBuilder();

            for (int i = 0; i < chunkedEvents.size(); i++) {
                final AssuranceEvent assuranceEvent = chunkedEvents.get(i);
                // Verify that the chunked bytes sent is below the permitted limit.
                assertTrue(
                        assuranceEvent
                                        .getJSONRepresentation()
                                        .getBytes(StandardCharsets.UTF_8)
                                        .length
                                < OutboundEventQueueWorker.MAX_EVENT_SIZE);

                final String chunkData =
                        (String)
                                assuranceEvent
                                        .getPayload()
                                        .get(AssuranceConstants.AssuranceEventKeys.CHUNK_DATA);
                // Construct the actual de-chunked event payload from chunked events.
                actualPayloadValue.append(chunkData);

                // Validate metadata keys - sequence number, total chunks per event.
                final Map<String, Object> metadata = assuranceEvent.getMetadata();
                assertEquals(
                        i,
                        metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER));
                assertEquals(4, metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL));

                // Verify that the other event values are same as the original event
                assertEquals(largeAssuranceEvent.vendor, assuranceEvent.vendor);
                assertEquals(largeAssuranceEvent.type, assuranceEvent.type);
                assertEquals(largeAssuranceEvent.timestamp, assuranceEvent.timestamp);
            }

            // Validate that the de-chunked payload matches the combined chunks.
            final JSONObject actualPayloadJson = new JSONObject(actualPayloadValue.toString());
            assertEquals(expectedPayloadKeyValue, actualPayloadJson.getString("largeKey"));

        } catch (IOException | JSONException e) {
            fail();
        }
    }

    @Test
    public void test_chunk_eventPayloadSize20KB() {
        try {
            // Prepare & read large payload from resources.
            final String expectedPayloadKeyValue =
                    readPayloadFromResource("assurance_large_event_payload_key_value_20KB.txt");

            // Construct expected event payload
            final HashMap<String, Object> expectedEventPayload = new HashMap<>();
            expectedEventPayload.put("largeKey", expectedPayloadKeyValue);
            final AssuranceEvent largeAssuranceEvent =
                    new AssuranceEvent(
                            AssuranceConstants.AssuranceEventType.GENERIC, expectedEventPayload);

            final OutboundEventQueueWorker.OutboundEventChunker outboundEventChunker =
                    new OutboundEventQueueWorker.OutboundEventChunker(
                            OutboundEventQueueWorker.MAX_PAYLOAD_CHUNK_SIZE);

            final List<AssuranceEvent> chunkedEvents =
                    outboundEventChunker.chunk(largeAssuranceEvent);
            assertEquals(2, chunkedEvents.size());

            final StringBuilder actualPayloadValue = new StringBuilder();

            for (int i = 0; i < chunkedEvents.size(); i++) {
                final AssuranceEvent assuranceEvent = chunkedEvents.get(i);
                // Verify that the chunked bytes sent is below the permitted limit.
                assertTrue(
                        assuranceEvent
                                        .getJSONRepresentation()
                                        .getBytes(StandardCharsets.UTF_8)
                                        .length
                                < OutboundEventQueueWorker.MAX_EVENT_SIZE);

                final String chunkData =
                        (String)
                                assuranceEvent
                                        .getPayload()
                                        .get(AssuranceConstants.AssuranceEventKeys.CHUNK_DATA);
                // Construct the actual de-chunked event payload from chunked events.
                actualPayloadValue.append(chunkData);

                // Validate metadata keys - sequence number, total chunks per event.
                final Map<String, Object> metadata = assuranceEvent.getMetadata();
                assertEquals(
                        i,
                        metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER));
                assertEquals(2, metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL));

                // Verify that the other event values are same as the original event
                assertEquals(largeAssuranceEvent.vendor, assuranceEvent.vendor);
                assertEquals(largeAssuranceEvent.type, assuranceEvent.type);
                assertEquals(largeAssuranceEvent.timestamp, assuranceEvent.timestamp);
            }

            // Validate that the de-chunked payload matches the combined chunks.
            final JSONObject actualPayloadJson = new JSONObject(actualPayloadValue.toString());
            assertEquals(expectedPayloadKeyValue, actualPayloadJson.getString("largeKey"));

        } catch (IOException | JSONException e) {
            fail();
        }
    }

    @Test
    public void test_chunk_eventPayloadTypeHTML() {
        try {
            // Prepare & read large payload from resources.
            final String expectedPayloadKeyValue =
                    readPayloadFromResource("assurance_large_event_payload_key_value_html.txt");

            // Construct expected event payload
            final HashMap<String, Object> expectedEventPayload = new HashMap<>();
            expectedEventPayload.put("largeKey", expectedPayloadKeyValue);
            final AssuranceEvent largeAssuranceEvent =
                    new AssuranceEvent(
                            AssuranceConstants.AssuranceEventType.GENERIC, expectedEventPayload);

            final OutboundEventQueueWorker.OutboundEventChunker outboundEventChunker =
                    new OutboundEventQueueWorker.OutboundEventChunker(
                            OutboundEventQueueWorker.MAX_PAYLOAD_CHUNK_SIZE);

            final List<AssuranceEvent> chunkedEvents =
                    outboundEventChunker.chunk(largeAssuranceEvent);
            assertEquals(4, chunkedEvents.size());

            final StringBuilder actualPayloadValue = new StringBuilder();

            for (int i = 0; i < chunkedEvents.size(); i++) {
                final AssuranceEvent assuranceEvent = chunkedEvents.get(i);
                // Verify that the chunked bytes sent is below the permitted limit.
                assertTrue(
                        assuranceEvent
                                        .getJSONRepresentation()
                                        .getBytes(StandardCharsets.UTF_8)
                                        .length
                                < OutboundEventQueueWorker.MAX_EVENT_SIZE);

                final String chunkData =
                        (String)
                                assuranceEvent
                                        .getPayload()
                                        .get(AssuranceConstants.AssuranceEventKeys.CHUNK_DATA);
                // Construct the actual de-chunked event payload from chunked events.
                actualPayloadValue.append(chunkData);

                // Validate metadata keys - sequence number, total chunks per event.
                final Map<String, Object> metadata = assuranceEvent.getMetadata();
                assertEquals(
                        i,
                        metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER));
                assertEquals(4, metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL));

                // Verify that the other event values are same as the original event
                assertEquals(largeAssuranceEvent.vendor, assuranceEvent.vendor);
                assertEquals(largeAssuranceEvent.type, assuranceEvent.type);
                assertEquals(largeAssuranceEvent.timestamp, assuranceEvent.timestamp);
            }

            // Validate that the de-chunked payload matches the combined chunks.
            final JSONObject actualPayloadJson = new JSONObject(actualPayloadValue.toString());
            assertEquals(expectedPayloadKeyValue, actualPayloadJson.getString("largeKey"));

        } catch (IOException | JSONException e) {
            fail();
        }
    }

    @Test
    public void test_chunk_eventPayload_emptyLines() {
        try {
            // Prepare & read large payload from resources.
            final String expectedPayloadKeyValue =
                    readPayloadFromResource(
                            "assurance_large_event_payload_key_value_emptylines.txt");

            // Construct expected event payload
            final HashMap<String, Object> expectedEventPayload = new HashMap<>();
            expectedEventPayload.put("largeKey", expectedPayloadKeyValue);
            final AssuranceEvent largeAssuranceEvent =
                    new AssuranceEvent(
                            AssuranceConstants.AssuranceEventType.GENERIC, expectedEventPayload);

            final OutboundEventQueueWorker.OutboundEventChunker outboundEventChunker =
                    new OutboundEventQueueWorker.OutboundEventChunker(
                            OutboundEventQueueWorker.MAX_PAYLOAD_CHUNK_SIZE);

            final List<AssuranceEvent> chunkedEvents =
                    outboundEventChunker.chunk(largeAssuranceEvent);
            assertEquals(2, chunkedEvents.size());

            final StringBuilder actualPayloadValue = new StringBuilder();

            for (int i = 0; i < chunkedEvents.size(); i++) {
                final AssuranceEvent assuranceEvent = chunkedEvents.get(i);
                // Verify that the chunked bytes sent is below the permitted limit.
                assertTrue(
                        assuranceEvent
                                        .getJSONRepresentation()
                                        .getBytes(StandardCharsets.UTF_8)
                                        .length
                                < OutboundEventQueueWorker.MAX_EVENT_SIZE);

                final String chunkData =
                        (String)
                                assuranceEvent
                                        .getPayload()
                                        .get(AssuranceConstants.AssuranceEventKeys.CHUNK_DATA);
                // Construct the actual de-chunked event payload from chunked events.
                actualPayloadValue.append(chunkData);

                // Validate metadata keys - sequence number, total chunks per event.
                final Map<String, Object> metadata = assuranceEvent.getMetadata();
                assertEquals(
                        i,
                        metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER));
                assertEquals(2, metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL));

                // Verify that the other event values are same as the original event
                assertEquals(largeAssuranceEvent.vendor, assuranceEvent.vendor);
                assertEquals(largeAssuranceEvent.type, assuranceEvent.type);
                assertEquals(largeAssuranceEvent.timestamp, assuranceEvent.timestamp);
            }

            // Validate that the de-chunked payload matches the combined chunks.
            final JSONObject actualPayloadJson = new JSONObject(actualPayloadValue.toString());
            assertEquals(expectedPayloadKeyValue, actualPayloadJson.getString("largeKey"));

        } catch (IOException | JSONException e) {
            fail();
        }
    }

    private String readPayloadFromResource(final String resourceName) throws IOException {
        final InputStream payloadValueStream =
                this.getClass().getClassLoader().getResourceAsStream(resourceName);
        final BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(payloadValueStream));
        final StringBuilder resourceContent = new StringBuilder();

        String curentLine;

        while ((curentLine = bufferedReader.readLine()) != null) {
            resourceContent.append(curentLine);
            resourceContent.append("\n");
        }

        return resourceContent.toString();
    }
}
