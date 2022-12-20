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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides a template for managing the processing of a queue of work items. Aims to separate the
 * lifecycle of the worker thread(s) processing work items with the queue that they are fetched
 * from. Allows sub-classes to be agnostic of the thread management.
 *
 * <p>TODO: Core 2.0 now provides a {@link com.adobe.marketing.mobile.util.SerialWorkDispatcher}
 * which can be used to replace this class.
 */
abstract class EventQueueWorker<T> implements Runnable {
    private static final String LOG_TAG = "EventChunker";

    /**
     * Interface contract for chunking an event of type T into events of type V.
     *
     * @param <T> source item type which needs to be chunked.
     * @param <V> target item type that {@code T} will be chunked into.
     */
    interface EventChunker<T, V> {
        /**
         * Splits the item {@param T} into a list of items of type {@code V}
         *
         * @param item source item type which needs to be chunked.
         * @return a List of target items of type {@code V} that {@code T} will be chunked into.
         */
        List<V> chunk(final T item);
    }

    /** Holds the work items that need to be processed by this worker. */
    private final LinkedBlockingQueue<T> workQueue;

    /** Executor to which work is submitted to. */
    private final ExecutorService executorService;

    /** A handle for identifying and manipulating the state of the thread that this worker owns. */
    private Future<?> future;

    /**
     * Denotes the activeness of the {@link EventQueueWorker}. An {@link EventQueueWorker} is said
     * to be active if {@link #start()} is invoked at-least once after {@link #stop()} (if ever
     * invoked). Note that this is not the state of the worker-thread.
     */
    private boolean isActive;

    /** Used for guarding the "activeness" logic. */
    private final Object activenessMutex = new Object();

    @VisibleForTesting
    EventQueueWorker(
            final ExecutorService executorService, final LinkedBlockingQueue<T> workQueue) {
        this.workQueue = workQueue;
        this.executorService = executorService;
    }

    /**
     * Enqueues an item to the end of the {@link #workQueue}. Additionally, resumes the queue
     * processing if the {@link EventQueueWorker} is active (but processing was stopped earlier due
     * to lack of work).
     *
     * @param workItem item that needs to be processed.
     * @return true if
     */
    boolean offer(final T workItem) {
        boolean result = workQueue.offer(workItem);
        resume();
        return result;
    }

    /**
     * Puts the {@link EventQueueWorker} in active state and starts processing the {@link
     * #workQueue} if not already active.
     *
     * @return true - if start was successful, false if worker is already active.
     */
    boolean start() {
        synchronized (activenessMutex) {
            if (isActive) {
                Log.debug(Assurance.LOG_TAG, LOG_TAG, "EventQueueWorker is already running.");
                return false;
            }

            isActive = true;
        }

        prepare();
        resume();
        return true;
    }

    /**
     * Invoked on the calling thread, immediately before processing the items in the queue for the
     * first time. Implementers are expected to perform any one-time setup operations (bound by the
     * activeness of {@link EventQueueWorker}) before processing starts.
     */
    protected abstract void prepare();

    /**
     * Invoked before processing each work item. Results in the worker thread being completed if the
     * implementer returns false. Implementers are expected to enforce any conditions that needs to
     * be checked before performing work here. This is invoked from the background worker thread
     * that the {@link EventQueueWorker} maintains.
     *
     * @return true if all conditions are met for performing work. false otherwise.
     */
    protected abstract boolean canWork();

    @Override
    public void run() {
        while (!Thread.interrupted() && canWork() && workQueue.peek() != null) {
            try {
                final T event = workQueue.poll();
                doWork(event);
            } catch (final InterruptedException exception) {
                Log.error(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Background worker thread(InboundEventWorker) interrupted: "
                                + exception.getLocalizedMessage());
                // https://docs.oracle.com/javase/7/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
                // read in answer for question : How do I stop a thread that waits for long periods
                // (e.g., for input)?
                // When you catch InterruptedException it is a good idea to immediately
                // re-interrupt the thread to preserve the interrupt flag because when the
                // exception is thrown, the interrupt bit is cleared.
                Thread.currentThread().interrupt();
            }
        }

        Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                "No more items to process. Finishing current job : %s for %s",
                Thread.currentThread().toString(),
                this.getClass().getSimpleName());
    }

    /**
     * Perform processing on the workItem. This is invoked on a thread different from the rest of
     * the operations on this class. This is invoked from the background worker thread that the
     * {@link EventQueueWorker} maintains.
     *
     * @param workItem foremost item in the queue that currently being processed.
     * @throws InterruptedException
     */
    protected abstract void doWork(final T workItem) throws InterruptedException;

    /**
     * Puts the {@link EventQueueWorker} into inactive state and clears the {@link #workQueue}. The
     * {@link EventQueueWorker} needs to be started again via {@link #start()} to do new work.
     * Calling {@link #resume()} will have no affect on the sate of the {@link EventQueueWorker}
     * after this method is invoked.
     */
    void stop() {
        synchronized (activenessMutex) {
            if (future != null) {
                future.cancel(true);
                future = null;
            }

            isActive = false;
        }

        workQueue.clear();
    }

    /**
     * Resumes processing the work items in the {@link #workQueue} if the {@link EventQueueWorker}
     * is active and if no worker thread is actively processing the {@link #workQueue}
     */
    protected void resume() {
        synchronized (activenessMutex) {
            if (!isActive || (future != null && !future.isDone())) {
                return;
            }

            future = executorService.submit(this);
        }
    }
}
