package org.ngengine.network.protocol;

import com.jme3.network.Message;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.ngengine.network.components.ActionMessage;
import org.ngengine.network.protocol.serializers.GenericMessageSerializer;

/**
 * Runtime diff state machine used by {@link DynamicSerializerProtocol}.
 *
 * <p>This class owns the diff header wire format, packet references and sender/receiver histories.
 * The protocol remains the envelope orchestrator.
 */
final class DiffRuntime {
    static final long DIFF_RUNTIME_MARKER = 2L;
    static final long MODE_FULL = 0L;
    static final long MODE_DIFF = 1L;
    static final long LANE_UNRELIABLE = 0L;
    static final long LANE_RELIABLE = 1L;

    private static final int HISTORY_CAP = 32;
    static final int MAX_PEER_GROUPS = 1024;
    static final int MAX_GLOBAL_GROUPS = 8192;
    static final long MAX_PEER_BASE_BYTES = 8L * 1024 * 1024;
    static final long MAX_GLOBAL_BASE_BYTES = 64L * 1024 * 1024;
    private static int liveGroups;
    private static long liveBaseBytes;
    private long retainedBytes;
    private long nextPacketId = 1L;

    private void reserveGroup() throws IOException {
        synchronized (DiffRuntime.class) {
            if (senderGroups.size() + receiverGroups.size() >= MAX_PEER_GROUPS || liveGroups >= MAX_GLOBAL_GROUPS) {
                throw new IOException("Diff group retention bound exceeded");
            }
            liveGroups++;
        }
    }

    private void adjustBytes(long delta) throws IOException {
        synchronized (DiffRuntime.class) {
            if (delta > MAX_PEER_BASE_BYTES - retainedBytes || delta > MAX_GLOBAL_BASE_BYTES - liveBaseBytes) {
                throw new IOException("Diff base retention bound exceeded");
            }
            retainedBytes += delta;
            liveBaseBytes += delta;
        }
    }

    void clear() {
        synchronized (DiffRuntime.class) {
            liveGroups -= senderGroups.size() + receiverGroups.size();
            liveBaseBytes -= retainedBytes;
            retainedBytes = 0;
            senderGroups.clear();
            receiverGroups.clear();
        }
    }
    private static final long SENDER_GROUP_IDLE_TTL_MS = 15_000L;
    private static final long RECEIVER_SNAPSHOT_TTL_MS = 10_000L;
    private static final long RECEIVER_GROUP_IDLE_TTL_MS = 30_000L;
    private static final int RELIABLE_INTERVAL_UPDATES = 10;
    private static final int RELIABLE_FULL_CHECKPOINT_EVERY = 6;
    static final long RELIABLE_FULL_RECOVERY_INTERVAL_MS = 5_000L;

    enum EncodeOutcome {
        HANDLED,
        SKIP,
        BYPASS
    }

    static final class DecodeResult {
        private final boolean matched;
        private final boolean dropped;
        private final Object message;

        private DecodeResult(boolean matched, boolean dropped, Object message) {
            this.matched = matched;
            this.dropped = dropped;
            this.message = message;
        }

        static DecodeResult noMatch() {
            return new DecodeResult(false, false, null);
        }

        static DecodeResult dropped() {
            return new DecodeResult(true, true, null);
        }

        static DecodeResult message(Object message) {
            return new DecodeResult(true, false, message);
        }

        boolean matched() {
            return matched;
        }

        boolean isDropped() {
            return dropped;
        }

        Object message() {
            return message;
        }
    }

    private static final class SnapshotEntry {
        private final ByteBuffer snapshot;
        private final long createdAt;

        private SnapshotEntry(ByteBuffer snapshot, long createdAt) {
            this.snapshot = snapshot;
            this.createdAt = createdAt;
        }
    }

    private static final class GroupSendState {
        private long lastTouchedAt;
        private long reliableBasePacketId = -1L;
        private ByteBuffer reliableBaseSnapshot;
        private Class<?> messageClass;
        private int updatesSinceReliable = 0;
        private int reliableSendsSinceFull = 0;
        private long lastReliableFullAt = Long.MIN_VALUE;

    }

    private static final class GroupReceiveState {
        private Class<?> messageClass;
        private long lastTouchedAt;
        private long reliableBasePacketId = -1L;
        private final LinkedHashMap<Long, SnapshotEntry> reliableHistory = new LinkedHashMap<>();
    }

    private static final class DiffBuildResult {
        private final boolean changed;
        private final byte[] bitmask;
        private final List<Field> fields;

        private DiffBuildResult(boolean changed, byte[] bitmask, List<Field> fields) {
            this.changed = changed;
            this.bitmask = bitmask;
            this.fields = fields;
        }
    }

    private final DynamicSerializerProtocol protocol;
    private final LinkedHashMap<Long, GroupSendState> senderGroups = new LinkedHashMap<>();
    private final LinkedHashMap<Long, GroupReceiveState> receiverGroups = new LinkedHashMap<>();

    DiffRuntime(DynamicSerializerProtocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Encodes a top-level message body with runtime diff metadata when supported.
     *
     * <p>Returns:
     * <ul>
     * <li>{@link EncodeOutcome#HANDLED} when a runtime body was written.</li>
     * <li>{@link EncodeOutcome#SKIP} when message is unchanged and should not be sent.</li>
     * <li>{@link EncodeOutcome#BYPASS} when caller must serialize regular full body.</li>
     * </ul>
     */
    EncodeOutcome encode(Message message, GrowableByteBuffer out) throws IOException {
        if (!(message instanceof DiffableMessage)) {
            return EncodeOutcome.BYPASS;
        }

        evictSender();
        long now = protocol.nowMillis();
        DiffableMessage diffable = (DiffableMessage) message;
        long group = diffable.getDiffGroup();
        Serializer serializer = protocol.bestSerializer(message.getClass());
        if (!(serializer instanceof GenericMessageSerializer)) {
            return EncodeOutcome.BYPASS;
        }

        GroupSendState state = senderGroups.get(group);
        if (state == null) {
            reserveGroup();
            state = new GroupSendState();
            state.messageClass = message.getClass();
            senderGroups.put(group, state);
        }
        if (state.messageClass != message.getClass()) throw new IOException("Diff group class changed");
        state.lastTouchedAt = now;

        boolean originalReliable = message.isReliable();
        state.updatesSinceReliable++;
        boolean forceReliable = !originalReliable && state.updatesSinceReliable >= RELIABLE_INTERVAL_UPDATES;
        boolean reliable = originalReliable || forceReliable;
        if (forceReliable) {
            // RemotePeer chooses transport lane from message.isReliable() after serialization.
            message.setReliable(true);
        }

        Object normalizedCurrent = protocol.normalizeForDiff(message);

        if (!reliable) {
            if (state.reliableBaseSnapshot == null || state.reliableBasePacketId < 0) {
                // Full unreliable snapshots bypass diff state entirely.
                return EncodeOutcome.BYPASS;
            }
            DiffBuildResult diff = buildDiff(serializer.readObject(state.reliableBaseSnapshot.duplicate(), message.getClass()), normalizedCurrent, message.getClass());
            if (!diff.changed) {
                return EncodeOutcome.SKIP;
            }
            long packetId = nextPacketId++;
            ByteBuffer body = encodeDiffBody(
                message,
                normalizedCurrent,
                serializer,
                LANE_UNRELIABLE,
                group,
                packetId,
                state.reliableBasePacketId,
                diff
            );
            protocol.writeEnvelopedBody(message, out, true, body);
            if (protocol.logEnabled(Level.FINEST)) {
                protocol.logFinest("DIFF[SEND] DIFF lane=unreliable group=" + group + " packet=" + packetId
                    + " base=" + state.reliableBasePacketId);
            }
            return EncodeOutcome.HANDLED;
        }

        state.updatesSinceReliable = 0;
        boolean checkpointFull = protocol.isReliableFullCheckpointEnabled()
            && state.reliableSendsSinceFull >= RELIABLE_FULL_CHECKPOINT_EVERY;
        boolean recoveryFull = protocol.isReliableFullCheckpointEnabled()
            && state.reliableBaseSnapshot != null
            && state.reliableBasePacketId >= 0
            && now - state.lastReliableFullAt >= RELIABLE_FULL_RECOVERY_INTERVAL_MS;
        boolean mustSendFull =
            state.reliableBaseSnapshot == null
                || state.reliableBasePacketId < 0
                || checkpointFull
                || recoveryFull;

        if (mustSendFull) {
            long packetId = nextPacketId++;
            ByteBuffer body = encodeFullBody(normalizedCurrent, serializer, LANE_RELIABLE, group, packetId);
            protocol.writeEnvelopedBody(message, out, true, body);
            ByteBuffer cloned = snapshotBytes(normalizedCurrent, serializer);
            updateReliableBase(state, packetId, cloned, now);
            state.reliableSendsSinceFull = 0;
            state.lastReliableFullAt = now;
            if (protocol.logEnabled(Level.FINEST)) {
                String reason = recoveryFull
                    ? "recovery-ttl"
                    : checkpointFull ? "checkpoint" : "base";
                protocol.logFinest(
                    "DIFF[SEND] FULL lane=reliable group=" + group
                        + " packet=" + packetId
                        + " reason=" + reason
                );
            }
            return EncodeOutcome.HANDLED;
        }

        DiffBuildResult diff = buildDiff(serializer.readObject(state.reliableBaseSnapshot.duplicate(), message.getClass()), normalizedCurrent, message.getClass());
        if (!diff.changed) {
            return EncodeOutcome.SKIP;
        }

        long packetId = nextPacketId++;
        ByteBuffer body = encodeDiffBody(
            message,
            normalizedCurrent,
            serializer,
            LANE_RELIABLE,
            group,
            packetId,
            state.reliableBasePacketId,
            diff
        );
        protocol.writeEnvelopedBody(message, out, true, body);
        ByteBuffer cloned = snapshotBytes(normalizedCurrent, serializer);
        updateReliableBase(state, packetId, cloned, now);
        state.reliableSendsSinceFull++;
        if (protocol.logEnabled(Level.FINEST)) {
            protocol.logFinest("DIFF[SEND] DIFF lane=reliable group=" + group + " packet=" + packetId
                + " base=" + state.reliableBasePacketId);
        }
        return EncodeOutcome.HANDLED;
    }

    /**
     * Decodes a runtime diff body if marker is present.
     *
     * <p>When marker does not match, caller must decode with regular full-body path.
     */
    DecodeResult decodeIfRuntime(ByteBuffer body, Class<?> messageClass, Serializer serializer) throws IOException {
        ByteBuffer headerProbe = body.duplicate();
        if (!headerProbe.hasRemaining()) {
            return DecodeResult.noMatch();
        }
        long marker = VarInt.decodeUnsigned(headerProbe);
        if (marker != DIFF_RUNTIME_MARKER) {
            return DecodeResult.noMatch();
        }
        VarInt.decodeUnsigned(body); // consume runtime marker

        evictReceiver();
        long now = protocol.nowMillis();
        long mode = VarInt.decodeUnsigned(body);
        long lane = VarInt.decodeUnsigned(body);
        long group = VarInt.decodeSigned(body);
        long packetId = VarInt.decodeUnsigned(body);

        if ((mode != MODE_FULL && mode != MODE_DIFF) || (lane != LANE_RELIABLE && lane != LANE_UNRELIABLE)
                || packetId <= 0 || !protocol.acceptsRuntimeLane(lane)
                || !DiffableMessage.class.isAssignableFrom(messageClass)) {
            throw new IOException("Invalid runtime diff header");
        }
        GroupReceiveState state = receiverGroups.get(group);
        if (state == null) {
            if (lane == LANE_UNRELIABLE) return DecodeResult.dropped();
            if (mode != MODE_FULL) throw new IOException("Reliable diff requires an initial FULL");
            reserveGroup();
            state = new GroupReceiveState();
            state.messageClass = messageClass;
            receiverGroups.put(group, state);
        }
        if (state.messageClass != messageClass) throw new IOException("Diff group class changed");
        state.lastTouchedAt = now;
        if (lane == LANE_RELIABLE && state.reliableBasePacketId >= 0 && packetId <= state.reliableBasePacketId) {
            return DecodeResult.dropped();
        }
        if (lane == LANE_UNRELIABLE && state.reliableBasePacketId >= 0 && packetId <= state.reliableBasePacketId) {
            return DecodeResult.dropped();
        }

        if (mode == MODE_FULL) {
            Object full = serializer.readObject(body, messageClass);
            if (body.hasRemaining()) throw new IOException("Trailing runtime FULL data");
            if (lane == LANE_RELIABLE) {
                ByteBuffer cloned = snapshotBytes(full, serializer);
                updateReliableBase(state, packetId, cloned, now);
            }
            if (protocol.logEnabled(Level.FINEST)) {
                protocol.logFinest("DIFF[RECV] FULL lane=" + (lane == LANE_RELIABLE ? "reliable" : "unreliable")
                    + " group=" + group + " packet=" + packetId);
            }
            return DecodeResult.message(full);
        }

        if (mode != MODE_DIFF) {
            throw new IOException("Unknown runtime diff mode: " + mode);
        }

        long basePacketId = VarInt.decodeUnsigned(body);
        if (!(serializer instanceof GenericMessageSerializer)) {
            if (lane == LANE_UNRELIABLE) {
                return DecodeResult.dropped();
            }
            throw new IOException("Diff body received for unsupported serializer: " + serializer.getClass().getName());
        }

        if (basePacketId <= 0 || basePacketId >= packetId) throw new IOException("Invalid diff packet reference");
        SnapshotEntry baseEntry = state.reliableHistory.get(basePacketId);
        if (baseEntry == null) {
            if (lane == LANE_UNRELIABLE) {
                if (protocol.logEnabled(Level.FINEST)) {
                    protocol.logFinest("DIFF[RECV] DROP lane=unreliable group=" + group + " packet=" + packetId
                        + " reason=missing-base base=" + basePacketId);
                }
                return DecodeResult.dropped();
            }
            throw new IOException("Reliable diff requires base packet " + basePacketId + " in group " + group);
        }

        Object result = applyDiff(body, serializer.readObject(baseEntry.snapshot.duplicate(), messageClass), serializer, messageClass);
        if (body.hasRemaining()) throw new IOException("Trailing runtime DIFF data");
        if (lane == LANE_RELIABLE) {
            ByteBuffer cloned = snapshotBytes(result, serializer);
            updateReliableBase(state, packetId, cloned, now);
        }
        if (protocol.logEnabled(Level.FINEST)) {
            protocol.logFinest("DIFF[RECV] DIFF lane=" + (lane == LANE_RELIABLE ? "reliable" : "unreliable")
                + " group=" + group + " packet=" + packetId + " base=" + basePacketId);
        }
        return DecodeResult.message(result);
    }

    private ByteBuffer encodeFullBody(Object normalizedCurrent, Serializer serializer, long lane, long group, long packetId)
        throws IOException {
        GrowableByteBuffer body = new GrowableByteBuffer(ByteBuffer.allocate(512), 512, DynamicSerializerProtocol.MAX_FRAME_BYTES);
        VarInt.encodeUnsigned(DIFF_RUNTIME_MARKER, body);
        VarInt.encodeUnsigned(MODE_FULL, body);
        VarInt.encodeUnsigned(lane, body);
        VarInt.encodeSigned(group, body);
        VarInt.encodeUnsigned(packetId, body);
        protocol.writeBodyWithSerializerBridge(normalizedCurrent, serializer, body);
        return body.getBuffer();
    }

    private ByteBuffer encodeDiffBody(
        Message message,
        Object normalizedCurrent,
        Serializer serializer,
        long lane,
        long group,
        long packetId,
        long basePacketId,
        DiffBuildResult diff
    )
        throws IOException {
        GrowableByteBuffer body = new GrowableByteBuffer(ByteBuffer.allocate(512), 512, DynamicSerializerProtocol.MAX_FRAME_BYTES);
        VarInt.encodeUnsigned(DIFF_RUNTIME_MARKER, body);
        VarInt.encodeUnsigned(MODE_DIFF, body);
        VarInt.encodeUnsigned(lane, body);
        VarInt.encodeSigned(group, body);
        VarInt.encodeUnsigned(packetId, body);
        VarInt.encodeUnsigned(basePacketId, body);
        VarInt.encodeUnsigned(diff.bitmask.length, body);
        body.put(diff.bitmask);
        for (int i = 0; i < diff.fields.size(); i++) {
            if ((diff.bitmask[i >>> 3] & (1 << (i & 7))) == 0) {
                continue;
            }
            Field field = diff.fields.get(i);
            try {
                Object value = field.get(normalizedCurrent);
                protocol.serializeNestedValue(value, body);
            } catch (IllegalAccessException e) {
                throw new IOException("Unable to access field " + field + " for " + message.getClass().getName(), e);
            }
        }
        return body.getBuffer();
    }

    private DiffBuildResult buildDiff(Object previous, Object current, Class<?> currentClass) throws IOException {
        if (previous == null || previous.getClass() != current.getClass()) {
            return new DiffBuildResult(false, new byte[0], java.util.Collections.emptyList());
        }
        List<Field> fields = ReflectionFieldSchema.getSchema(currentClass).fields();
        byte[] bitmask = new byte[(fields.size() + 7) / 8];
        int changedCount = 0;
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            try {
                Object previousValue = field.get(previous);
                Object currentValue = field.get(current);
                boolean changed = !Objects.equals(previousValue, currentValue);
                boolean forceIdentity = forceIdentityField(currentClass, field);
                if (changed || forceIdentity) {
                    bitmask[i >>> 3] |= (byte) (1 << (i & 7));
                }
                if (changed) {
                    changedCount++;
                }
            } catch (IllegalAccessException e) {
                throw new IOException("Unable to inspect field " + field, e);
            }
        }
        return new DiffBuildResult(changedCount > 0, bitmask, fields);
    }

    private Object applyDiff(ByteBuffer body, Object base, Serializer serializer, Class<?> messageClass) throws IOException {
        ReflectionFieldSchema.Schema schema = ReflectionFieldSchema.getSchema(messageClass);
        List<Field> fields = schema.fields();
        long bitmaskLenLong = VarInt.decodeUnsigned(body);
        if (bitmaskLenLong > Integer.MAX_VALUE) {
            throw new IOException("Bitmask length too large: " + bitmaskLenLong);
        }
        int bitmaskLen = (int) bitmaskLenLong;
        int expected = (fields.size() + 7) / 8;
        if (bitmaskLen != expected) {
            throw new IOException("Invalid diff bitmask length: " + bitmaskLen + ", expected " + expected);
        }
        if (bitmaskLen > body.remaining()) {
            throw new IOException("Diff bitmask exceeds remaining payload: " + bitmaskLen);
        }
        byte[] bitmask = new byte[bitmaskLen];
        body.get(bitmask);

        Object result = base; // The retained bytes were decoded into a fresh object for this packet.
        for (int i = 0; i < fields.size(); i++) {
            if ((bitmask[i >>> 3] & (1 << (i & 7))) == 0) {
                continue;
            }
            Field field = fields.get(i);
            Object value = protocol.deserializeNestedValue(body, field.getType());
            try {
                field.set(result, value);
            } catch (IllegalAccessException e) {
                throw new IOException("Unable to apply diff field " + field, e);
            }
        }
        return result;
    }

    private boolean forceIdentityField(Class<?> currentClass, Field field) {
        if (!ActionMessage.class.isAssignableFrom(currentClass)) {
            return false;
        }
        String fieldName = field.getName();
        return "componentId".equals(fieldName) || "networkId".equals(fieldName);
    }

    private ByteBuffer snapshotBytes(Object snapshot, Serializer serializer) throws IOException {
        GrowableByteBuffer output = new GrowableByteBuffer(ByteBuffer.allocate(512), 512, DynamicSerializerProtocol.MAX_FRAME_BYTES);
        protocol.writeBodyWithSerializerBridge(snapshot, serializer, output);
        ByteBuffer bytes = output.getBuffer();
        bytes.flip();
        return bytes.asReadOnlyBuffer();
    }

    private void updateReliableBase(GroupSendState state, long packetId, ByteBuffer snapshot, long now)
        throws IOException {
        long previousBytes = state.reliableBaseSnapshot == null ? 0 : state.reliableBaseSnapshot.capacity();
        adjustBytes(snapshot.capacity() - previousBytes);
        state.reliableBasePacketId = packetId;
        state.reliableBaseSnapshot = snapshot;
        state.lastTouchedAt = now;
    }

    private void updateReliableBase(GroupReceiveState state, long packetId, ByteBuffer snapshot, long now) throws IOException {
        adjustBytes(snapshot.capacity());
        state.reliableBasePacketId = packetId;
        state.reliableHistory.put(packetId, new SnapshotEntry(snapshot, now));
        trimHistory(state.reliableHistory, now, RECEIVER_SNAPSHOT_TTL_MS, HISTORY_CAP, packetId);
        state.lastTouchedAt = now;
    }

    private void evictSender() throws IOException {
        long now = protocol.nowMillis();
        Iterator<GroupSendState> groups = senderGroups.values().iterator();
        while (groups.hasNext()) {
            GroupSendState state = groups.next();
            if (now - state.lastTouchedAt > SENDER_GROUP_IDLE_TTL_MS) {
                if (state.reliableBaseSnapshot != null) adjustBytes(-state.reliableBaseSnapshot.capacity());
                groups.remove();
                synchronized (DiffRuntime.class) { liveGroups--; }
            }
        }
    }

    private void evictReceiver() throws IOException {
        long now = protocol.nowMillis();
        Iterator<GroupReceiveState> groups = receiverGroups.values().iterator();
        while (groups.hasNext()) {
            GroupReceiveState state = groups.next();
            if (now - state.lastTouchedAt > RECEIVER_GROUP_IDLE_TTL_MS) {
                for (SnapshotEntry entry : state.reliableHistory.values()) adjustBytes(-entry.snapshot.capacity());
                groups.remove();
                synchronized (DiffRuntime.class) { liveGroups--; }
            } else {
                trimHistory(state.reliableHistory, now, RECEIVER_SNAPSHOT_TTL_MS, HISTORY_CAP, state.reliableBasePacketId);
            }
        }
    }

    private void trimHistory(LinkedHashMap<Long, SnapshotEntry> history, long now, long ttlMillis, int cap,
            long protectedPacketId) throws IOException {
        Iterator<Map.Entry<Long, SnapshotEntry>> entries = history.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Long, SnapshotEntry> entry = entries.next();
            if (entry.getKey() != protectedPacketId
                    && (now - entry.getValue().createdAt > ttlMillis || history.size() > cap)) {
                adjustBytes(-entry.getValue().snapshot.capacity());
                entries.remove();
            }
        }
    }
}
