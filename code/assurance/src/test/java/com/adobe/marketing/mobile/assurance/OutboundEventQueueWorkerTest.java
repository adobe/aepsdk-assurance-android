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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class OutboundEventQueueWorkerTest {
    @Mock private AssuranceWebViewSocket mockAssuranceWebViewSocket;
    @Mock private ExecutorService mockExecutorService;
    @Mock private AssuranceClientInfo mockAssuranceClientInfo;

    private OutboundEventQueueWorker outboundEventQueueWorker;
    private LinkedBlockingQueue<AssuranceEvent> queue = new LinkedBlockingQueue<>();
    private HashMap<String, Object> clientInfoData;
    private AssuranceEvent clientInfoEvent;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        Mockito.doAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocationOnMock)
                                    throws Throwable {
                                Runnable runnable = (Runnable) invocationOnMock.getArgument(0);
                                runnable.run();
                                return null;
                            }
                        })
                .when(mockExecutorService)
                .submit(any(Runnable.class));

        clientInfoEvent =
                new AssuranceEvent(
                        AssuranceConstants.AssuranceEventType.CLIENT, Collections.EMPTY_MAP);
        when(mockAssuranceClientInfo.getData()).thenReturn(clientInfoData);

        outboundEventQueueWorker =
                new OutboundEventQueueWorker(
                        mockExecutorService,
                        mockAssuranceWebViewSocket,
                        mockAssuranceClientInfo,
                        queue,
                        new OutboundEventQueueWorker.OutboundEventChunker(
                                OutboundEventQueueWorker.MAX_PAYLOAD_CHUNK_SIZE));
    }

    @Test
    public void test_prepare_sendsClientInfoEvent() {
        // Simulate the socket connection being open.
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);

        outboundEventQueueWorker.prepare();

        // Ensure that no events from the queue are submitted yet.
        verify(mockExecutorService, times(0)).submit(any(EventQueueWorker.class));
        // Ensure that the client info event is sent.
        verify(mockAssuranceWebViewSocket).sendData(ArgumentMatchers.<byte[]>any());
    }

    @Test
    public void test_canWork_when_socketDisconnected_workerUnblocked() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.CLOSING);
        outboundEventQueueWorker.unblock();

        assertFalse(outboundEventQueueWorker.canWork());
    }

    @Test
    public void test_canWork_socketConnected_workerUnblocked() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);
        outboundEventQueueWorker.unblock();

        assertTrue(outboundEventQueueWorker.canWork());
    }

    @Test
    public void test_canWork_socketConnected_workerBlocked() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);
        outboundEventQueueWorker.block();

        assertFalse(outboundEventQueueWorker.canWork());
    }

    @Test
    public void test_canWork_socketDisconnected_workerBlocked() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.CLOSED);
        outboundEventQueueWorker.block();

        assertFalse(outboundEventQueueWorker.canWork());
    }

    //    Scenario Tests    //

    @Test
    public void test_start_sendsClientInfoEvent() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);

        outboundEventQueueWorker.start();

        verify(mockExecutorService).submit(any(EventQueueWorker.class));
        verify(mockAssuranceWebViewSocket).sendData(ArgumentMatchers.<byte[]>any());
    }

    @Test
    public void test_offer_workerNotYetStarted() {
        // Simulate the socket connection being open.
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);
        // Simulate the outbound event worker being unblocked.
        outboundEventQueueWorker.unblock();

        AssuranceEvent assuranceEvent = new AssuranceEvent("type", Collections.EMPTY_MAP);
        outboundEventQueueWorker.offer(assuranceEvent);

        // Verify that the event is enqueued irrespective of the worker state.
        assertEquals(1, queue.size());
        assertEquals(assuranceEvent, queue.peek());

        // Verify that no work is processed despite being enqueued.
        verify(mockExecutorService, never()).submit(any(EventQueueWorker.class));
    }

    @Test
    public void test_runnable_eventsSentWhenWorkIsQueued() {
        // Simulate the socket connection being open.
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);

        final AssuranceEvent event1 = new AssuranceEvent("type", Collections.EMPTY_MAP);
        final AssuranceEvent event2 = new AssuranceEvent("type", Collections.EMPTY_MAP);
        final AssuranceEvent event3 = new AssuranceEvent("type", Collections.EMPTY_MAP);

        // Simulate worker being offered work and being started.
        outboundEventQueueWorker.offer(event1);
        outboundEventQueueWorker.offer(event2);
        outboundEventQueueWorker.offer(event3);
        outboundEventQueueWorker.start();
        outboundEventQueueWorker.unblock();

        // Verify that the worker thread is submitted to the ExecutorService.
        // Should be submitted twice as the first invocation on start() is a no-op due to blocked
        // queue.
        verify(mockExecutorService, times(2)).submit(any(EventQueueWorker.class));

        // Verify that the events are being processed
        ArgumentCaptor<byte[]> eventByteCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockAssuranceWebViewSocket, times(4)).sendData(eventByteCaptor.capture());
        final List<byte[]> capturedEventData = eventByteCaptor.getAllValues();
        assertEquals(4, capturedEventData.size()); // 1 client info event + 3 offered events.
        // Verify that the events enqueues are the same events that are being sent over socket.
        assertEquals(
                event1.getJSONRepresentation(),
                new String(capturedEventData.get(1), Charset.forName("UTF-8")));
        assertEquals(
                event2.getJSONRepresentation(),
                new String(capturedEventData.get(2), Charset.forName("UTF-8")));
        assertEquals(
                event3.getJSONRepresentation(),
                new String(capturedEventData.get(3), Charset.forName("UTF-8")));
    }

    @Test
    public void test_runnable_eventsBlockedWhenCannotStartForwarding() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);

        final AssuranceEvent event1 = new AssuranceEvent("type", Collections.EMPTY_MAP);
        final AssuranceEvent event2 = new AssuranceEvent("type", Collections.EMPTY_MAP);
        final AssuranceEvent event3 = new AssuranceEvent("type", Collections.EMPTY_MAP);

        // Simulate worker being blocked, offered work and being started.
        outboundEventQueueWorker.block();
        outboundEventQueueWorker.start();
        outboundEventQueueWorker.offer(event1);
        outboundEventQueueWorker.offer(event2);
        outboundEventQueueWorker.offer(event3);

        // Verify that only the client info event is sent.
        ArgumentCaptor<byte[]> eventByteCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockAssuranceWebViewSocket, times(1)).sendData(eventByteCaptor.capture());
        final byte[] capturedEvent = eventByteCaptor.getValue();
        // It is not feasible to match the client info event due to the random Event Id that is
        // generated internally.
        // So, verify that the offered events do not match the event sent instead.
        assertNotEquals(
                event1.getJSONRepresentation(),
                new String(capturedEvent, Charset.forName("UTF-8")));
        assertNotEquals(
                event2.getJSONRepresentation(),
                new String(capturedEvent, Charset.forName("UTF-8")));
        assertNotEquals(
                event3.getJSONRepresentation(),
                new String(capturedEvent, Charset.forName("UTF-8")));
        verifyNoMoreInteractions(mockAssuranceWebViewSocket);
    }

    @Test
    public void test_runnable_eventsBlockedWhenSocketNotConnected() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);

        final AssuranceEvent event1 = new AssuranceEvent("type", Collections.EMPTY_MAP);
        final AssuranceEvent event2 = new AssuranceEvent("type", Collections.EMPTY_MAP);
        final AssuranceEvent event3 = new AssuranceEvent("type", Collections.EMPTY_MAP);

        outboundEventQueueWorker.start();
        // Simulate socket disconnection.
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.CLOSED);
        outboundEventQueueWorker.offer(event1);
        outboundEventQueueWorker.offer(event2);
        outboundEventQueueWorker.offer(event3);

        // Verify that only the client info event is sent.
        ArgumentCaptor<byte[]> eventByteCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockAssuranceWebViewSocket, times(1)).sendData(eventByteCaptor.capture());
        final byte[] capturedEvent = eventByteCaptor.getValue();
        // It is not feasible to match the client info event due to the random Event Id that is
        // generated internally.
        // So, verify that the offered events do not match the event sent instead.
        assertNotEquals(
                event1.getJSONRepresentation(),
                new String(capturedEvent, Charset.forName("UTF-8")));
        assertNotEquals(
                event2.getJSONRepresentation(),
                new String(capturedEvent, Charset.forName("UTF-8")));
        assertNotEquals(
                event3.getJSONRepresentation(),
                new String(capturedEvent, Charset.forName("UTF-8")));
        verifyNoMoreInteractions(mockAssuranceWebViewSocket);
    }

    @Test
    public void test_sendClientInfoEvent_workerBlocked() {
        outboundEventQueueWorker.block();
        outboundEventQueueWorker.sendClientInfoEvent();

        // Verify that the client info event is sent.
        verify(mockAssuranceWebViewSocket, times(1)).sendData(any(byte[].class));
    }

    @Test
    public void test_sendClientInfoEvent_workerAlreadyUnBlocked() {
        outboundEventQueueWorker.unblock();
        outboundEventQueueWorker.sendClientInfoEvent();

        // Verify that the client info event is NOT sent.
        verify(mockAssuranceWebViewSocket, times(0)).sendData(any(byte[].class));
    }

    @Test
    public void test_sendEvent_payloadOverMaxPayloadSize_20KB() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);

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

            // Simulate event enqueueing into outbound queue.
            outboundEventQueueWorker.offer(largeAssuranceEvent);
            outboundEventQueueWorker.start();
            outboundEventQueueWorker.unblock();

            // Capture date being sent through the socket and Verify 2 events trigger in total.
            // 1 client info event and 1 chunked Assurance event (which is same as the original)
            ArgumentCaptor<byte[]> socketDataCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(mockAssuranceWebViewSocket, times(2)).sendData(socketDataCaptor.capture());
            final List<byte[]> capturedEventData = socketDataCaptor.getAllValues();
            assertEquals(2, capturedEventData.size());

            final String assuranceEventString =
                    new String(capturedEventData.get(1), Charset.forName("UTF-8"));
            final AssuranceEvent actualEvent = new AssuranceEvent(assuranceEventString);
            assertEquals(
                    largeAssuranceEvent.getJSONRepresentation(),
                    actualEvent.getJSONRepresentation());
        } catch (JSONException | IOException e) {
            fail();
        }
    }

    @Test
    public void test_sendEvent_payloadOverMaxPayloadSize_40KB() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);

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

            // Simulate event enqueueing into outbound queue.
            outboundEventQueueWorker.offer(largeAssuranceEvent);
            outboundEventQueueWorker.start();
            outboundEventQueueWorker.unblock();

            // Capture date being sent through the socket and Verify 5 events trigger in total.
            // 1 client info event and 4 chunked Assurance events.
            ArgumentCaptor<byte[]> socketDataCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(mockAssuranceWebViewSocket, times(5)).sendData(socketDataCaptor.capture());
            final List<byte[]> capturedEventData = socketDataCaptor.getAllValues();
            assertEquals(5, capturedEventData.size());

            final StringBuilder actualPayloadValue = new StringBuilder();
            final Set<String> chunkIds = new HashSet<>();

            for (int i = 1; i < capturedEventData.size(); i++) {
                // Verify that the chunked bytes sent is below the permitted limit.
                final byte[] capturedChunkedEventBytes = capturedEventData.get(i);
                assertTrue(
                        capturedChunkedEventBytes.length < OutboundEventQueueWorker.MAX_EVENT_SIZE);

                final String assuranceEventString =
                        new String(capturedEventData.get(i), Charset.forName("UTF-8"));
                final AssuranceEvent actualEvent = new AssuranceEvent(assuranceEventString);
                final String chunkData =
                        (String)
                                actualEvent
                                        .getPayload()
                                        .get(AssuranceConstants.AssuranceEventKeys.CHUNK_DATA);
                // Construct the actual de-chunked event payload from chunked events.
                actualPayloadValue.append(chunkData);

                // Validate metadata keys - sequence number, total chunks per event.
                final Map<String, Object> metadata = actualEvent.getMetadata();
                // collect chunkId's for verifying similarity.
                chunkIds.add((String) metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_ID));
                assertEquals(
                        i - 1,
                        metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER));
                assertEquals(4, metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL));

                // Verify that the other event values are same as the original event
                assertEquals(largeAssuranceEvent.vendor, actualEvent.vendor);
                assertEquals(largeAssuranceEvent.type, actualEvent.type);
                assertEquals(largeAssuranceEvent.timestamp, actualEvent.timestamp);
            }

            // Validate that the chunkId is same across events.
            assertEquals(1, chunkIds.size());

            // Validate that the de-chunked payload matches the combined chunks.
            final JSONObject actualPayloadJson = new JSONObject(actualPayloadValue.toString());
            assertEquals(expectedPayloadKeyValue, actualPayloadJson.getString("largeKey"));
        } catch (JSONException | IOException e) {
            fail();
        }
    }

    @Test
    public void test_sendEvent_payloadOverMaxPayloadSize_HTML() {
        when(mockAssuranceWebViewSocket.getState())
                .thenReturn(AssuranceWebViewSocket.SocketReadyState.OPEN);

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

            // Simulate event enqueueing into outbound queue.
            outboundEventQueueWorker.offer(largeAssuranceEvent);
            outboundEventQueueWorker.start();
            outboundEventQueueWorker.unblock();

            // Capture data being sent through the socket and Verify 5 events trigger in total.
            // 1 client info event and 4 chunked Assurance events.
            ArgumentCaptor<byte[]> socketDataCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(mockAssuranceWebViewSocket, times(5)).sendData(socketDataCaptor.capture());
            final List<byte[]> capturedEventData = socketDataCaptor.getAllValues();
            assertEquals(5, capturedEventData.size());

            final StringBuilder actualPayloadValue = new StringBuilder();
            final Set<String> chunkIds = new HashSet<>();

            for (int i = 1; i < capturedEventData.size(); i++) {
                // Verify that the chunked bytes sent is below the permitted limit.
                final byte[] capturedChunkedEventBytes = capturedEventData.get(i);
                assertTrue(
                        capturedChunkedEventBytes.length < OutboundEventQueueWorker.MAX_EVENT_SIZE);

                final String assuranceEventString =
                        new String(capturedEventData.get(i), Charset.forName("UTF-8"));
                final AssuranceEvent actualEvent = new AssuranceEvent(assuranceEventString);
                final String chunkData =
                        (String)
                                actualEvent
                                        .getPayload()
                                        .get(AssuranceConstants.AssuranceEventKeys.CHUNK_DATA);
                // Construct the actual de-chunked event payload from chunked events.
                actualPayloadValue.append(chunkData);

                // Validate metadata keys - chunkId, sequence number, total chunks per event.
                final Map<String, Object> metadata = actualEvent.getMetadata();
                // collect chunkId's for verifying similarity.
                chunkIds.add((String) metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_ID));
                assertEquals(
                        i - 1,
                        metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_SEQUENCE_NUMBER));
                assertEquals(4, metadata.get(AssuranceConstants.AssuranceEventKeys.CHUNK_TOTAL));

                // Verify that the other event values are same as the original event
                assertEquals(largeAssuranceEvent.vendor, actualEvent.vendor);
                assertEquals(largeAssuranceEvent.type, actualEvent.type);
                assertEquals(largeAssuranceEvent.timestamp, actualEvent.timestamp);
            }

            // Validate that the chunkId is same across events.
            assertEquals(1, chunkIds.size());

            // Validate that the de-chunked payload matches the combined chunks.
            final JSONObject actualPayloadJson = new JSONObject(actualPayloadValue.toString());
            assertEquals(expectedPayloadKeyValue, actualPayloadJson.getString("largeKey"));
        } catch (JSONException | IOException e) {
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
