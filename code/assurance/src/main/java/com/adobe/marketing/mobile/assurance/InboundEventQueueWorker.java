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


import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Responsible for handling any {@link AssuranceEvent}'s from the socket and forwarding them to
 * {@link InboundQueueEventListener} as necessary.
 */
class InboundEventQueueWorker extends EventQueueWorker<AssuranceEvent> {
    private static final String LOG_TAG = "InboundEventQueueWorker";

    interface InboundQueueEventListener {
        void onInboundEvent(final AssuranceEvent event);
    }

    private final InboundQueueEventListener listener;

    InboundEventQueueWorker(
            final ExecutorService executorService, final InboundQueueEventListener listener) {
        super(executorService, new LinkedBlockingQueue<AssuranceEvent>());
        this.listener = listener;
    }

    @Override
    protected void prepare() {
        // no-op
    }

    @Override
    protected boolean canWork() {
        // Does not have any specific gating mechanism.
        // Always return true.
        return true;
    }

    @Override
    protected void doWork(final AssuranceEvent assuranceEvent) {
        if (assuranceEvent == null) {
            return;
        }

        final String controlType = assuranceEvent.getControlType();

        if (controlType == null) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format(
                            "Received a nonControl Assurance event."
                                    + "Ignoring processing of the inbound event - %s",
                            assuranceEvent.toString()));
            return;
        }

        listener.onInboundEvent(assuranceEvent);
    }
}
