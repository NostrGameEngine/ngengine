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

package org.ngengine.network;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;

/** One lifetime per native peer: the wire ACK has no attempt identifier, so failed gates never reopen. */
final class ReliableChannelGate {
    static final int MAX_CHANNELS = 64;
    static final int MAX_PENDING_COUNT = 256;
    static final long MAX_PENDING_BYTES = 4L * 1024L * 1024L;
    static final long TIMEOUT_SECONDS = 30L;

    enum Failure { CONTROL_SEND_FAILED, ACK_TIMEOUT, SEND_FAILED, SEND_TIMEOUT, CAPACITY, TIMER_FAILED }
    enum FailureOperation { NONE, OPEN_SYNC, OPEN_ASYNC, SEND_SYNC, SEND_ASYNC, ACK_REPLY_SYNC, ACK_REPLY_ASYNC,
        WATCHDOG_SYNC, WATCHDOG_ASYNC }
    interface Transport {
        AsyncTask<Void> open(int channel, ByteBuffer control);
        AsyncTask<Void> send(int channel, ByteBuffer frame);
    }
    record Diagnostic(String state, int pendingCount, long pendingBytes, int openControlSendFailed,
            int timeouts, int acknowledgements, int nativeSends) {}

    record FailureDiagnostic(Failure reason, int channel, boolean acknowledged, int pendingCount, long pendingBytes,
            FailureOperation operation, List<String> causeClasses) {
        FailureDiagnostic { causeClasses = List.copyOf(causeClasses); }
    }

    /** Only the direct sanitized class name; never invoke custom cause accessors or retain private text. */
    static List<String> failureCauseClasses(Throwable failure) {
        if (failure == null) return List.of();
        String name = failure.getClass().getName();
        StringBuilder safe = new StringBuilder(Math.min(96, name.length()));
        for (int index = 0; index < name.length() && index < 96; index++) {
            char character = name.charAt(index);
            safe.append(character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9' || character == '.' || character == '$'
                    || character == '_' ? character : '_');
        }
        return List.of(safe.toString());
    }

    private static final class Channel {
        final int number;
        final ArrayDeque<ByteBuffer> pending = new ArrayDeque<>();
        boolean acknowledged;
        boolean pumping;
        boolean sending;
        int inFlightBytes;
        long operation;
        long timerToken;
        long deadline;
        boolean timerScheduled;
        Failure expiry;
        AsyncTask<Void> timer;
        AsyncTask<Void> opening;
        AsyncTask<Void> sendingTask;
        Channel(int number) { this.number = number; }
    }

    private final AsyncExecutor executor; // Shared with P2PConnection; its owner closes it.
    private final LongSupplier clock;
    private final Transport transport;
    private final Consumer<Failure> onFailure;
    private final Map<Integer, Channel> channels = new HashMap<>();
    private boolean closed;
    private boolean firstFailureCaptured;
    private Failure pendingFailure;
    private FailureDiagnostic firstFailure;
    private int pendingCount;
    private long pendingBytes;
    private int controlFailures;
    private int timeouts;
    private int acknowledgements;
    private int nativeSends;

    ReliableChannelGate(AsyncExecutor executor, LongSupplier clock, Transport transport, Consumer<Failure> onFailure) {
        this.executor = Objects.requireNonNull(executor);
        this.clock = Objects.requireNonNull(clock);
        this.transport = Objects.requireNonNull(transport);
        this.onFailure = Objects.requireNonNull(onFailure);
    }

    void submit(int number, ByteBuffer frame, ByteBuffer control) {
        try {
            synchronized (this) {
                if (isClosed()) return;
                int bytes = frame.remaining();
                Channel channel = channels.get(number);
                if (pendingCount >= MAX_PENDING_COUNT || bytes > MAX_PENDING_BYTES - pendingBytes
                        || (channel == null && channels.size() >= MAX_CHANNELS)) {
                    fail(Failure.CAPACITY, number);
                    return;
                }
                ByteBuffer owned = ByteBuffer.allocate(bytes);
                owned.put(frame.duplicate()).flip();
                boolean first = channel == null;
                if (first) {
                    channel = new Channel(number);
                    channels.put(number, channel); // Publish before transport callbacks, including synchronous failures/ACKs.
                }
                channel.pending.add(owned);
                pendingCount++;
                pendingBytes += bytes;
                if (first) start(channel, control);
                else pump(channel);

            }
        } finally {
            reportFailure();
        }
    }

    private void start(Channel channel, ByteBuffer control) {
        arm(channel, Failure.ACK_TIMEOUT);
        if (closed) return;
        try {
            AsyncTask<Void> opening = Objects.requireNonNull(transport.open(channel.number, Objects.requireNonNull(control)));
            if (closed) { cancel(opening); return; }
            channel.opening = opening;
            opening.catchException(error -> openingFailure(channel, FailureOperation.OPEN_ASYNC, error));
        } catch (Throwable error) {
            openingFailure(channel, FailureOperation.OPEN_SYNC, error);
        }
    }

    private void openingFailure(Channel channel, FailureOperation operation, Throwable error) {
        try {
            synchronized (this) {
                // A valid logical ACK proves opening even if the transport's separate delivery ACK is lost.
                if (closed || channel.acknowledged) return;
                fail(Failure.CONTROL_SEND_FAILED, channel.number, operation, error);
            }
        } finally {
            reportFailure();
        }
    }

    void acknowledge(int number) {
        try {
            synchronized (this) {
                if (isClosed()) return;
                Channel channel = channels.get(number);
                if (channel == null || channel.acknowledged) return;
                if (clock.getAsLong() - channel.deadline >= 0L) { fail(Failure.ACK_TIMEOUT, channel.number); return; }
                channel.acknowledged = true;
                acknowledgements = increment(acknowledgements);
                disarm(channel);
                pump(channel);

            }
        } finally {
            reportFailure();
        }
    }

    private void pump(Channel channel) {
        if (isClosed() || !channel.acknowledged || channel.pumping) return;
        channel.pumping = true;
        try {
            while (!isClosed() && !channel.sending && !channel.pending.isEmpty()) {
                ByteBuffer frame = channel.pending.remove();
                channel.sending = true;
                channel.inFlightBytes = frame.remaining();
                long operation = ++channel.operation;
                arm(channel, Failure.SEND_TIMEOUT);
                if (closed) return;
                try {
                    nativeSends = increment(nativeSends);
                    AsyncTask<Void> task = Objects.requireNonNull(transport.send(channel.number, frame));
                    if (closed) { cancel(task); return; }
                    channel.sendingTask = task;
                    task.then(value -> { complete(channel, operation); return null; })
                            .catchException(error -> failureCallback(Failure.SEND_FAILED, channel.number,
                                    FailureOperation.SEND_ASYNC, error));
                } catch (Throwable error) {
                    fail(Failure.SEND_FAILED, channel.number, FailureOperation.SEND_SYNC, error);
                }
            }
        } finally {
            channel.pumping = false;
        }
    }

    private void complete(Channel channel, long operation) {
        try {
            synchronized (this) {
                if (isClosed() || !channel.sending || channel.operation != operation) return;
                if (clock.getAsLong() - channel.deadline >= 0L) { fail(Failure.SEND_TIMEOUT, channel.number); return; }
                pendingCount--;
                pendingBytes -= channel.inFlightBytes;
                channel.inFlightBytes = 0;
                channel.sending = false;
                channel.sendingTask = null;
                disarm(channel);
                pump(channel);

            }
        } finally {
            reportFailure();
        }
    }

    private void arm(Channel channel, Failure expiry) {
        channel.expiry = expiry;
        channel.deadline = clock.getAsLong() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        // Reuse one watchdog per channel across successive sends, even if cancelled timers remain in an executor FIFO.
        if (!channel.timerScheduled) scheduleTimer(channel, TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS));
    }

    private void scheduleTimer(Channel channel, long delayNanos) {
        long token = ++channel.timerToken;
        channel.timerScheduled = true;
        try {
            AsyncTask<Void> timer = executor.runLater(() -> {
                try {
                    synchronized (ReliableChannelGate.this) {
                        if (closed || channel.timerToken != token) return null;
                        channel.timer = null;
                        channel.timerScheduled = false;
                        if (channel.expiry == null) return null;
                        long remaining = channel.deadline - clock.getAsLong();
                        if (remaining <= 0L) fail(channel.expiry, channel.number);
                        else scheduleTimer(channel, remaining);
                    }
                    return null;
                } finally {
                    reportFailure();
                }
            }, delayNanos, TimeUnit.NANOSECONDS);
            if (closed || channel.timerToken != token || !channel.timerScheduled) { cancel(timer); return; }
            channel.timer = Objects.requireNonNull(timer);
            timer.catchException(error -> {
                try {
                    synchronized (ReliableChannelGate.this) {
                        if (!closed && channel.timerToken == token && channel.timerScheduled)
                            fail(Failure.TIMER_FAILED, channel.number, FailureOperation.WATCHDOG_ASYNC, error);
                    }
                } finally {
                    reportFailure();
                }
            });
        } catch (Throwable error) {
            fail(Failure.TIMER_FAILED, channel.number, FailureOperation.WATCHDOG_SYNC, error);
        }
    }

    private void disarm(Channel channel) { channel.expiry = null; }

    private void fail(Failure reason, int number) {
        fail(reason, number, FailureOperation.NONE, null);
    }

    private void fail(Failure reason, int number, FailureOperation operation, Throwable error) {
        if (closed || firstFailureCaptured) return;
        firstFailureCaptured = true; // Freeze the initiating failure before collecting cause metadata.
        Channel channel = channels.get(number);
        firstFailure = new FailureDiagnostic(reason, number,
                channel != null && channel.acknowledged, pendingCount, pendingBytes, operation, failureCauseClasses(error));
        if (reason == Failure.CONTROL_SEND_FAILED) controlFailures = increment(controlFailures);
        if (reason == Failure.ACK_TIMEOUT || reason == Failure.SEND_TIMEOUT) timeouts = increment(timeouts);
        close();
        pendingFailure = reason;
    }

    private void failureCallback(Failure reason, int number, FailureOperation operation, Throwable error) {
        try {
            synchronized (this) { fail(reason, number, operation, error); }
        } finally {
            reportFailure();
        }
    }

    private void reportFailure() {
        // A synchronous AsyncTask callback can still be inside an outer submit/pump critical section.
        if (Thread.holdsLock(this)) return;
        Failure failure;
        synchronized (this) {
            failure = pendingFailure;
            pendingFailure = null;
        }
        if (failure != null) onFailure.accept(failure);
    }

    synchronized void close() {
        if (closed) return;
        closed = true;
        for (Channel channel : channels.values()) {
            disarm(channel);
            channel.timerToken++;
            channel.timerScheduled = false;
            cancel(channel.timer);
            channel.timer = null;
            cancel(channel.opening);
            cancel(channel.sendingTask);
            channel.opening = null;
            channel.sendingTask = null;
            channel.pending.clear();
            channel.sending = false;
            channel.inFlightBytes = 0;
        }
        pendingCount = 0;
        pendingBytes = 0;
    }

    synchronized FailureDiagnostic failureDiagnostic() { return firstFailure; }

    synchronized boolean needsOpen(int number) { return !isClosed() && !channels.containsKey(number); }
    synchronized boolean isClosed() { return closed || firstFailureCaptured; }
    synchronized Diagnostic diagnostic(int number) {
        Channel channel = channels.get(number);
        String state = isClosed() ? "CLOSED" : channel == null ? "UNUSED" : channel.acknowledged ? "OPEN" : "WAITING_ACK";
        return new Diagnostic(state, pendingCount, pendingBytes, controlFailures, timeouts, acknowledgements, nativeSends);
    }
    private static int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }
    private static void cancel(AsyncTask<?> task) {
        if (task != null) {
            try { task.cancel(); } catch (Throwable ignored) { /* Teardown must release every retained frame. */ }
        }
    }
}
