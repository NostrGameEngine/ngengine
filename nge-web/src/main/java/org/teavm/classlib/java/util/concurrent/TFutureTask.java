/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
public class TFutureTask<V> implements TFuture<V> {
    
    private volatile boolean cancelled = false;
    private volatile boolean completed = false;
    private V result;
    private Exception exception;

    /**
     * Attempts to cancel execution of this task. This attempt will fail if the
     * task has already completed, has already been cancelled, or could not be
     * cancelled for some other reason. If successful, and this task has not
     * started when {@code cancel} is called, this task should never run. If the
     * task has already started, then the {@code mayInterruptIfRunning}
     * parameter determines whether the thread executing this task should be
     * interrupted in an attempt to stop the task.
     *
     * <p>
     * After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}. Subsequent calls to {@link #isCancelled} will
     * always return {@code true} if this method returned {@code true}.
     *
     * @param mayInterruptIfRunning
     *            {@code true} if the thread executing this task should be
     *            interrupted; otherwise, in-progress tasks are allowed to
     *            complete
     * @return {@code false} if the task could not be cancelled, typically
     *         because it has already completed normally; {@code true} otherwise
     */
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (completed || cancelled) {
            return false;
        }
        if (mayInterruptIfRunning) {
            // You can add interruption logic here if required.
        }
        cancelled = true;
        return true;
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Returns {@code true} if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    public synchronized boolean isDone() {
        return completed;
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves
     * its result.
     *
     * @return the computed result
     * @throws CancellationException
     *             if the computation was cancelled
     * @throws TExecutionException
     *             if the computation threw an exception
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     */
    public synchronized V get() throws InterruptedException, TExecutionException, CancellationException {
        while (!completed) {
            if (cancelled) {
                throw new CancellationException();
            }
            wait();
        }

        if (exception != null) {
            throw new TExecutionException(exception);
        }

        return result;
    }

    /**
     * Waits if necessary for at most the given time for the computation to
     * complete, and then retrieves its result, if available.
     *
     * @param timeout
     *            the maximum time to wait
     * @param unit
     *            the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException
     *             if the computation was cancelled
     * @throws TExecutionException
     *             if the computation threw an exception
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     * @throws TimeoutException
     *             if the wait timed out
     */
    public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException,CancellationException, TExecutionException, TimeoutException {
        long timeoutMillis = unit.toMillis(timeout);
        long endTime = System.currentTimeMillis() + timeoutMillis;

        while (!completed) {
            if (cancelled) {
                throw new CancellationException();
            }

            long remainingTime = endTime - System.currentTimeMillis();
            if (remainingTime <= 0) {
                throw new TimeoutException();
            }

            wait(remainingTime);
        }

        if (exception != null) {
            throw new TExecutionException(exception);
        }

        return result;
    }

    /**
     * Sets the result of the computation. This method should be called when the
     * computation is complete or when it is cancelled.
     *
     * @param result
     *            the result of the computation
     */
    public synchronized void setResult(V result) {
        this.result = result;
        completed = true;
        notifyAll();
    }

    /**
     * Sets the exception that occurred during the computation. This method
     * should be called when the computation throws an exception.
     *
     * @param exception
     *            the exception that occurred during computation
     */
    public synchronized void setException(Exception exception) {
        this.exception = exception;
        completed = true;
        notifyAll();
    }
}
