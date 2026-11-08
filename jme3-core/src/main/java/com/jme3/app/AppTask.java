/*
 * Copyright (c) 2009-2020 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.app;


import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A queued application task. Invocation executes the callable at most once.
 * Cancellation never interrupts the executing thread, which may be an engine-owned
 * render or physics thread. An {@link Error} is recorded for waiting callers and rethrown by
 * {@link #invoke()}.
 *
 * @param <V> the result type
 * @author Matthew D. Hicks, lazloh
 */
public class AppTask<V> implements Future<V> {
    private static final Logger logger = Logger.getLogger(AppTask.class.getName());
    private final Callable<V> callable;
    // TeaVM in nge-web replaces FutureTask with a manually completed future.
    // Keep the state on a monitor shared by JVM and web instead.
    private final Object stateLock = new Object();
    private boolean started;
    private boolean finished;
    private boolean cancelled;
    private V result;
    private Throwable failure;

    /**
     * Creates a task without executing it.
     *
     * @param callable the non-null work to execute
     */
    public AppTask(Callable<V> callable) {
        this.callable = Objects.requireNonNull(callable, "callable");
    }

    /**
     * Cancels the task without interrupting the thread that executes it.
     * A callable already running is allowed to finish, but its result is discarded.
     *
     * @param mayInterruptIfRunning ignored; engine-owned threads are never interrupted
     * @return true if the task was canceled, false if it was already completed or canceled
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (stateLock) {
            if (finished || cancelled) {
                return false;
            }
            cancelled = true;
            stateLock.notifyAll();
            return true;
        }
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        synchronized (stateLock) {
            while (!finished && !cancelled) {
                stateLock.wait();
            }
            return completedResult();
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long timeoutNanos = unit.toNanos(timeout);
        long remaining = timeoutNanos;
        long start = System.nanoTime();
        synchronized (stateLock) {
            while (!finished && !cancelled) {
                if (remaining <= 0) {
                    throw new TimeoutException();
                }
                TimeUnit.NANOSECONDS.timedWait(stateLock, remaining);
                remaining = timeoutNanos - (System.nanoTime() - start);
            }
            return completedResult();
        }
    }

    private V completedResult() throws ExecutionException {
        if (cancelled) {
            throw new CancellationException();
        }
        if (failure != null) {
            throw new ExecutionException(failure);
        }
        return result;
    }

    @Override
    public boolean isCancelled() {
        synchronized (stateLock) {
            return cancelled;
        }
    }

    @Override
    public boolean isDone() {
        synchronized (stateLock) {
            return finished || cancelled;
        }
    }

    public Callable<V> getCallable() {
        return callable;
    }

    /**
     * Executes the callable on this thread, unless already invoked or canceled.
     *
     * @throws Error if the callable throws an Error; its failure is also available through get()
     */
    public void invoke() {
        synchronized (stateLock) {
            if (started || cancelled) {
                return;
            }
            started = true;
        }

        V value = null;
        Throwable thrown = null;
        try {
            value = callable.call();
        } catch (Throwable error) {
            thrown = error;
            if (error instanceof Error) {
                throw (Error) error;
            }
            logger.logp(Level.SEVERE, getClass().toString(), "invoke()", "Exception", error);
        } finally {
            synchronized (stateLock) {
                if (!cancelled) {
                    result = value;
                    failure = thrown;
                }
                finished = true;
                stateLock.notifyAll();
            }
        }
    }
}
