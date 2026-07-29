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
    private static final long SENDER_SNAPSHOT_TTL_MS = 5_000L;
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
        private final Object snapshot;
        private final long createdAt;

        private SnapshotEntry(Object snapshot, long createdAt) {
            this.snapshot = snapshot;
            this.createdAt = createdAt;
        }
    }

    private static final class GroupSendState {
        private long nextPacketId = 1L;
        private long lastTouchedAt;
        private long reliableBasePacketId = -1L;
        private Object reliableBaseSnapshot;
        private int updatesSinceReliable = 0;
        private int reliableSendsSinceFull = 0;
        private long lastReliableFullAt = Long.MIN_VALUE;
        private final LinkedHashMap<Long, SnapshotEntry> reliableHistory = new LinkedHashMap<>();
    }

    private static final class GroupReceiveState {
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
        GroupSendState state = senderGroups.computeIfAbsent(group, ignored -> new GroupSendState());
        state.lastTouchedAt = now;

        Serializer serializer = protocol.bestSerializer(message.getClass());
        if (!(serializer instanceof GenericMessageSerializer)) {
            return EncodeOutcome.BYPASS;
        }

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
            DiffBuildResult diff = buildDiff(state.reliableBaseSnapshot, normalizedCurrent, message.getClass());
            if (!diff.changed) {
                return EncodeOutcome.SKIP;
            }
            long packetId = state.nextPacketId++;
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
            long packetId = state.nextPacketId++;
            ByteBuffer body = encodeFullBody(normalizedCurrent, serializer, LANE_RELIABLE, group, packetId);
            protocol.writeEnvelopedBody(message, out, true, body);
            Object cloned = protocol.cloneWithSerializer(normalizedCurrent, serializer, message.getClass());
            updateReliableBase(state, packetId, cloned, now, true);
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

        DiffBuildResult diff = buildDiff(state.reliableBaseSnapshot, normalizedCurrent, message.getClass());
        if (!diff.changed) {
            return EncodeOutcome.SKIP;
        }

        long packetId = state.nextPacketId++;
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
        Object cloned = protocol.cloneWithSerializer(normalizedCurrent, serializer, message.getClass());
        updateReliableBase(state, packetId, cloned, now, true);
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

        GroupReceiveState state = receiverGroups.computeIfAbsent(group, ignored -> new GroupReceiveState());
        state.lastTouchedAt = now;
        if (lane == LANE_RELIABLE && state.reliableBasePacketId >= 0 && packetId <= state.reliableBasePacketId) {
            return DecodeResult.dropped();
        }
        if (lane == LANE_UNRELIABLE && state.reliableBasePacketId >= 0 && packetId <= state.reliableBasePacketId) {
            return DecodeResult.dropped();
        }

        if (mode == MODE_FULL) {
            Object full = serializer.readObject(body, messageClass);
            if (lane == LANE_RELIABLE) {
                Object cloned = protocol.cloneWithSerializer(full, serializer, messageClass);
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

        Object result = applyDiff(body, baseEntry.snapshot, serializer, messageClass);
        if (lane == LANE_RELIABLE) {
            Object cloned = protocol.cloneWithSerializer(result, serializer, messageClass);
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
        GrowableByteBuffer body = new GrowableByteBuffer(ByteBuffer.allocate(512), 512);
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
        GrowableByteBuffer body = new GrowableByteBuffer(ByteBuffer.allocate(512), 512);
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

        Object result = protocol.cloneWithSerializer(base, serializer, messageClass);
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

    private void updateReliableBase(GroupSendState state, long packetId, Object snapshot, long now, boolean sender) {
        state.reliableBasePacketId = packetId;
        state.reliableBaseSnapshot = snapshot;
        state.reliableHistory.put(packetId, new SnapshotEntry( snapshot, now));
        trimHistory(state.reliableHistory, now, SENDER_SNAPSHOT_TTL_MS, HISTORY_CAP, state.reliableBasePacketId);
        if (sender) {
            state.lastTouchedAt = now;
        }
    }

    private void updateReliableBase(GroupReceiveState state, long packetId, Object snapshot, long now) {
        state.reliableBasePacketId = packetId;
        state.reliableHistory.put(packetId, new SnapshotEntry( snapshot, now));
        trimHistory(state.reliableHistory, now, RECEIVER_SNAPSHOT_TTL_MS, HISTORY_CAP, state.reliableBasePacketId);
        state.lastTouchedAt = now;
    }

    private void evictSender() {
        long now = protocol.nowMillis();
        Iterator<Map.Entry<Long, GroupSendState>> groups = senderGroups.entrySet().iterator();
        while (groups.hasNext()) {
            Map.Entry<Long, GroupSendState> entry = groups.next();
            GroupSendState state = entry.getValue();
            if (now - state.lastTouchedAt > SENDER_GROUP_IDLE_TTL_MS) {
                groups.remove();
                continue;
            }
            trimHistory(state.reliableHistory, now, SENDER_SNAPSHOT_TTL_MS, HISTORY_CAP, state.reliableBasePacketId);
        }
    }

    private void evictReceiver() {
        long now = protocol.nowMillis();
        Iterator<Map.Entry<Long, GroupReceiveState>> groups = receiverGroups.entrySet().iterator();
        while (groups.hasNext()) {
            Map.Entry<Long, GroupReceiveState> entry = groups.next();
            GroupReceiveState state = entry.getValue();
            if (now - state.lastTouchedAt > RECEIVER_GROUP_IDLE_TTL_MS) {
                groups.remove();
                continue;
            }
            trimHistory(state.reliableHistory, now, RECEIVER_SNAPSHOT_TTL_MS, HISTORY_CAP, state.reliableBasePacketId);
        }
    }

    private static void trimHistory(
        LinkedHashMap<Long, SnapshotEntry> history,
        long now,
        long ttlMillis,
        int cap,
        long protectedPacketId
    ) {
        Iterator<Map.Entry<Long, SnapshotEntry>> ageIt = history.entrySet().iterator();
        while (ageIt.hasNext()) {
            Map.Entry<Long, SnapshotEntry> entry = ageIt.next();
            if (entry.getKey() == protectedPacketId) {
                continue;
            }
            if (now - entry.getValue().createdAt > ttlMillis) {
                ageIt.remove();
            }
        }

        while (history.size() > cap) {
            Long eldestKey = history.keySet().iterator().next();
            if (eldestKey == protectedPacketId) {
                if (history.size() == 1) {
                    break;
                }
                // rotate protected entry to keep trimming others
                SnapshotEntry protectedEntry = history.remove(eldestKey);
                history.put(eldestKey, protectedEntry);
                continue;
            }
            history.remove(eldestKey);
        }
    }
}
