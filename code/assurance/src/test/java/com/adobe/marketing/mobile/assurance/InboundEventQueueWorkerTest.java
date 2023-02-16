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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class InboundEventQueueWorkerTest {
    @Mock private InboundEventQueueWorker.InboundQueueEventListener mockInboundQueueEventListener;
    @Mock private ExecutorService mockExecutorService;
    private InboundEventQueueWorker inboundEventQueueWorker;

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
        inboundEventQueueWorker =
                new InboundEventQueueWorker(mockExecutorService, mockInboundQueueEventListener);
    }

    @Test
    public void test_canWork() {
        // Verify that the inbound queue worker can always process work.
        assertTrue(inboundEventQueueWorker.canWork());
    }

    @Test
    public void test_doWork_NullEvent() {
        inboundEventQueueWorker.doWork(null);

        // Verify that listeners are not notified about null events.
        verifyNoInteractions(mockInboundQueueEventListener);
    }

    @Test
    public void test_doWork_NullControlType() {
        final AssuranceEvent assuranceEvent = new AssuranceEvent(null, Collections.EMPTY_MAP);
        inboundEventQueueWorker.doWork(assuranceEvent);

        // Verify that listeners are not notified about non-control events.
        verifyNoInteractions(mockInboundQueueEventListener);
    }

    @Test
    public void test_doWork_NotifiesListener() {
        final AssuranceEvent assuranceEvent = constructAssuranceControlEvent();
        inboundEventQueueWorker.doWork(assuranceEvent);

        // Verify that listeners are notified about control events.
        verify(mockInboundQueueEventListener, times(1)).onInboundEvent(assuranceEvent);
    }

    @Test
    public void test_runnable_NotifiesListener() {
        final AssuranceEvent assuranceEvent1 = constructAssuranceControlEvent();
        final AssuranceEvent assuranceEvent2 = constructAssuranceControlEvent();

        // Simulate worker being offered work after starting.
        inboundEventQueueWorker.start();
        inboundEventQueueWorker.offer(assuranceEvent1);
        inboundEventQueueWorker.offer(assuranceEvent2);

        // Verify that the listener is notified about each of the events incident.
        final ArgumentCaptor<AssuranceEvent> eventCaptor =
                ArgumentCaptor.forClass(AssuranceEvent.class);
        verify(mockInboundQueueEventListener, times(2)).onInboundEvent(eventCaptor.capture());
        final List<AssuranceEvent> capturedEvents = eventCaptor.getAllValues();

        // Verify that the events that the listener is notified of match the events enqueued to the
        // worker.
        assertEquals(assuranceEvent1, capturedEvents.get(0));
        assertEquals(assuranceEvent2, capturedEvents.get(1));
    }

    private AssuranceEvent constructAssuranceControlEvent() {
        final HashMap<String, Object> payload = new HashMap<>();
        payload.put("type", AssuranceConstants.AssuranceEventType.CONTROL);
        return new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);
    }
}
