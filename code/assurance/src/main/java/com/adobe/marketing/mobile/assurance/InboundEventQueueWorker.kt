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
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.assurance.InboundEventQueueWorker.InboundQueueEventListener
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.util.SerialWorkDispatcher

/**
 * Responsible for handling any [AssuranceEvent]'s from the socket and forwarding them to
 * [InboundQueueEventListener] as necessary.
 */
internal class InboundEventQueueWorker {

    companion object {
        private const val LOG_TAG = "InboundEventQueueWorker"
    }

    /**
     * Interface for listening to incoming [AssuranceEvent]'s from the socket.
     */
    internal interface InboundQueueEventListener {
        /**
         * Called when an [AssuranceEvent] is received from the socket.
         * @param event the [AssuranceEvent] received from the socket.
         */
        fun onInboundEvent(event: AssuranceEvent)
    }

    private val workDispatcher: SerialWorkDispatcher<AssuranceEvent>

    internal constructor(listener: InboundQueueEventListener) : this(
        SerialWorkDispatcher(
            LOG_TAG,
            WorkHandlerImpl(EventStitcher(listener::onInboundEvent))
        )
    )

    @VisibleForTesting
    internal constructor(workDispatcher: SerialWorkDispatcher<AssuranceEvent>) {
        this.workDispatcher = workDispatcher
    }

    /**
     * Starts the [SerialWorkDispatcher] that the [InboundEventQueueWorker] maintains
     * to process the inbound events.
     * If the dispatcher is already started, this method does nothing.
     */
    fun start() {
        val dispatcherState = workDispatcher.getState()
        if (dispatcherState != SerialWorkDispatcher.State.NOT_STARTED) {
            Log.trace(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Work dispatcher was already started and is in $dispatcherState state."
            )
            return
        }

        workDispatcher.start()
    }

    /**
     * Queues the [AssuranceEvent] to be processed by the [SerialWorkDispatcher] that the
     * [InboundEventQueueWorker] maintains.
     * If the dispatcher is shutdown, this method does nothing.
     */
    fun offer(event: AssuranceEvent): Boolean {
        if (workDispatcher.getState() == SerialWorkDispatcher.State.SHUTDOWN) {
            Log.trace(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Cannot queue event. Work dispatcher was shutdown."
            )
            return false
        }

        return workDispatcher.offer(event)
    }

    /**
     * Stops the [SerialWorkDispatcher] that the [InboundEventQueueWorker] maintains.
     */
    fun stop() {
        workDispatcher.shutdown()
    }

    @VisibleForTesting
    internal class WorkHandlerImpl(
        private val eventStitcher: EventStitcher
    ) : SerialWorkDispatcher.WorkHandler<AssuranceEvent> {

        override fun doWork(item: AssuranceEvent): Boolean {
            if (!EventStitcher.isChunked(item) && item.controlType == null) {
                Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format(
                        "Received a nonControl Assurance event." +
                            "Ignoring processing of the inbound event - %s",
                        item.toString()
                    )
                )

                // mark this event as processed so that it can be removed from the queue
                return true
            }

            try { // We do not want any exceptions thrown during event stitching to crash the worker thread or the process
                eventStitcher.onEvent(item)
            } catch (e: Exception) {
                Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Error while processing inbound event",
                    e.localizedMessage
                )
            }

            return true
        }
    }
}
