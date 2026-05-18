package org.ngengine.network.protocol;

import static org.junit.Assert.assertTrue;

import com.jme3.network.AbstractMessage;
import com.jme3.network.Message;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.ngengine.network.quantization.TransformQuantizer;

public class TestDynamicSerializerProtocolBenchmarkTest {

    private enum PacketKind {
        TRANSFORM,
        CHUNK_SYNC,
        COMBAT_ACTION
    }

    private enum Scenario {
        MOVEMENT_HEAVY(
            "Heavy movement with many concurrent players.",
            "Transform-only snapshots with frequent movement and direction changes.",
            "Think of a crowded arena match where many remote players are always moving.",
            24,
            PacketKind.TRANSFORM,
            true
        ),
        MOSTLY_STATIC(
            "Mostly static world state.",
            "Transform-only snapshots with tiny movement deltas and long idle periods.",
            "Think of players waiting in a lobby or safe area and interacting from time to time.",
            24,
            PacketKind.TRANSFORM,
            true
        ),
        BURSTY_STATE(
            "Bursty gameplay events.",
            "Transform-only snapshots with short bursts of acceleration and quick heading changes.",
            "Think of dodge/sprint phases alternating with slower movement.",
            24,
            PacketKind.TRANSFORM,
            true
        ),
        LARGE_PAYLOAD(
            "Chunk synchronization payloads.",
            "This simulates map chunk sync packets (tiles/collision/light) with large structured payloads.",
            "Think of streaming chunk updates while traversing the world.",
            24,
            PacketKind.CHUNK_SYNC,
            false
        ),
        ATTACK_ANIMATION(
            "Combat action packets.",
            "This simulates attack + animation events with direction and hit-point payloads.",
            "Think of rapid melee/ranged actions during close combat.",
            24,
            PacketKind.COMBAT_ACTION,
            false
        );

        private final String summary;
        private final String technicalDescription;
        private final String inGameExample;
        private final int entitiesPerTick;
        private final PacketKind packetKind;
        private final boolean includeQuantizedStream;

        Scenario(
            String summary,
            String technicalDescription,
            String inGameExample,
            int entitiesPerTick,
            PacketKind packetKind,
            boolean includeQuantizedStream
        ) {
            this.summary = summary;
            this.technicalDescription = technicalDescription;
            this.inGameExample = inGameExample;
            this.entitiesPerTick = entitiesPerTick;
            this.packetKind = packetKind;
            this.includeQuantizedStream = includeQuantizedStream;
        }
    }

    private static final TransformQuantizer ARENA_FPS_TRANSFORM_QUANTIZER = new TransformQuantizer(
        new Vector3f(-512f, -16f, -512f),
        new Vector3f(1024f, 64f, 1024f),
        0.02f,
        12,
        1e-5f,
        1e-5f,
        14,
        0.125f,
        8.0f,
        12,
        0.125f,
        8.0f
    );

    @NetworkSafe
    public static class DiffableTransformMessage extends AbstractMessage implements DiffableMessage {
        public String entityId;
        public String mapId;
        public float tx;
        public float ty;
        public float tz;
        public float rx;
        public float ry;
        public float rz;
        public float rw;
        public float sx;
        public float sy;
        public float sz;

        @Override
        public long getDiffGroup() {
            return hash64(entityId + "|" + mapId + "|transform");
        }
    }

    @NetworkSafe
    public static class PlainTransformMessage extends AbstractMessage {
        public String entityId;
        public String mapId;
        public float tx;
        public float ty;
        public float tz;
        public float rx;
        public float ry;
        public float rz;
        public float rw;
        public float sx;
        public float sy;
        public float sz;
    }

    @NetworkSafe
    public static class QuantizedTransformMessage extends AbstractMessage implements DiffableMessage {
        public String entityId;
        public String mapId;
        public long packedTranslation;
        public long packedRotation;
        public long packedScale;

        @Override
        public long getDiffGroup() {
            return hash64(entityId + "|" + mapId + "|transform");
        }
    }

    @NetworkSafe
    public static class PlainQuantizedTransformMessage extends AbstractMessage {
        public String entityId;
        public String mapId;
        public long packedTranslation;
        public long packedRotation;
        public long packedScale;
    }

    @NetworkSafe
    public static class DiffableChunkSyncMessage extends AbstractMessage implements DiffableMessage {
        public String mapId;
        public int chunkX;
        public int chunkY;
        public int layer;
        public long revision;
        public boolean delta;
        public List<Integer> tileIds = Collections.emptyList();
        public List<Integer> collisionBits = Collections.emptyList();
        public List<Integer> lightLevels = Collections.emptyList();

        @Override
        public long getDiffGroup() {
            return hash64(mapId + "|" + layer + "|" + chunkX + "|" + chunkY + "|chunk");
        }
    }

    @NetworkSafe
    public static class PlainChunkSyncMessage extends AbstractMessage {
        public String mapId;
        public int chunkX;
        public int chunkY;
        public int layer;
        public long revision;
        public boolean delta;
        public List<Integer> tileIds = Collections.emptyList();
        public List<Integer> collisionBits = Collections.emptyList();
        public List<Integer> lightLevels = Collections.emptyList();
    }

    @NetworkSafe
    public static class DiffableCombatActionMessage extends AbstractMessage implements DiffableMessage {
        public String mapId;
        public String attackerId;
        public String targetId;
        public int skillId;
        public int animationId;
        public int animationFrame;
        public int damage;
        public boolean critical;
        public float hitX;
        public float hitY;
        public float hitZ;
        public float dirX;
        public float dirY;
        public float dirZ;
        public long eventSeq;

        @Override
        public long getDiffGroup() {
            return hash64(mapId + "|" + attackerId + "|" + targetId + "|combat");
        }
    }

    @NetworkSafe
    public static class PlainCombatActionMessage extends AbstractMessage {
        public String mapId;
        public String attackerId;
        public String targetId;
        public int skillId;
        public int animationId;
        public int animationFrame;
        public int damage;
        public boolean critical;
        public float hitX;
        public float hitY;
        public float hitZ;
        public float dirX;
        public float dirY;
        public float dirZ;
        public long eventSeq;
    }

    private static final class TransformSample {
        private final Vector3f translation;
        private final Quaternion rotation;
        private final Vector3f scale;
        private final boolean reliable;

        private TransformSample(Vector3f translation, Quaternion rotation, Vector3f scale, boolean reliable) {
            this.translation = translation;
            this.rotation = rotation;
            this.scale = scale;
            this.reliable = reliable;
        }
    }

    @Test
    public void benchmarkDiffableBandwidthSavings() {
        StringBuilder report = new StringBuilder(2048);
        report.append("\n=== DynamicSerializerProtocol Diff Benchmark ===\n");
        report.append("Tick rates considered: 30Hz and 60Hz\n");
        report.append("Per-peer full-mesh P2P formula (payload only): avgBytesPerPacket * (N - 1) * tickRate * 8 / 1000\n");
        report.append("N is the total peers in the session for that scenario.\n");
        report.append("Note: transport overhead (WebRTC/IP/DTLS/SCTP) is not included.\n");

        for (Scenario scenario : Scenario.values()) {
            DynamicSerializerProtocol diffProtocol = new DynamicSerializerProtocol(true, id -> {}, 0, true);
            DynamicSerializerProtocol diffNoRecoveryProtocol = new DynamicSerializerProtocol(true, id -> {}, 0, false);
            DynamicSerializerProtocol plainProtocol = new DynamicSerializerProtocol(true, id -> {}, 0);
            DynamicSerializerProtocol plainQuantizedProtocol = scenario.includeQuantizedStream
                ? new DynamicSerializerProtocol(true, id -> {}, 0)
                : null;
            DynamicSerializerProtocol diffQuantizedProtocol = scenario.includeQuantizedStream
                ? new DynamicSerializerProtocol(true, id -> {}, 0, true)
                : null;
            DynamicSerializerProtocol diffNoRecoveryQuantizedProtocol = scenario.includeQuantizedStream
                ? new DynamicSerializerProtocol(true, id -> {}, 0, false)
                : null;

            List<Integer> plainSizes = new ArrayList<>();
            List<Integer> diffSizes = new ArrayList<>();
            List<Integer> diffNoRecoverySizes = new ArrayList<>();
            List<Integer> plainQuantizedSizes = scenario.includeQuantizedStream ? new ArrayList<>() : null;
            List<Integer> diffQuantizedSizes = scenario.includeQuantizedStream ? new ArrayList<>() : null;
            List<Integer> diffNoRecoveryQuantizedSizes = scenario.includeQuantizedStream ? new ArrayList<>() : null;

            final int ticks = 1_000;
            for (int tick = 0; tick < ticks; tick++) {
                for (int entityIndex = 0; entityIndex < scenario.entitiesPerTick; entityIndex++) {
                    Message plain = createPlainPacket(tick, entityIndex, scenario);
                    Message diff = createDiffablePacket(tick, entityIndex, scenario);
                    Message diffNoRecovery = createDiffablePacket(tick, entityIndex, scenario);
                    plainSizes.add(sizeOf(plainProtocol.toByteBuffer(plain, null)));
                    diffSizes.add(sizeOf(diffProtocol.toByteBuffer(diff, null)));
                    diffNoRecoverySizes.add(sizeOf(diffNoRecoveryProtocol.toByteBuffer(diffNoRecovery, null)));

                    if (scenario.includeQuantizedStream) {
                        Message plainQuantized = createPlainQuantizedPacket(tick, entityIndex, scenario);
                        Message diffQuantized = createQuantizedPacket(tick, entityIndex, scenario);
                        Message diffNoRecoveryQuantized = createQuantizedPacket(tick, entityIndex, scenario);
                        plainQuantizedSizes.add(sizeOf(plainQuantizedProtocol.toByteBuffer(plainQuantized, null)));
                        diffQuantizedSizes.add(sizeOf(diffQuantizedProtocol.toByteBuffer(diffQuantized, null)));
                        diffNoRecoveryQuantizedSizes.add(
                            sizeOf(diffNoRecoveryQuantizedProtocol.toByteBuffer(diffNoRecoveryQuantized, null))
                        );
                    }
                }
            }

            long plainTotal = plainSizes.stream().mapToLong(Integer::longValue).sum();
            long diffTotal = diffSizes.stream().mapToLong(Integer::longValue).sum();
            long diffNoRecoveryTotal = diffNoRecoverySizes.stream().mapToLong(Integer::longValue).sum();
            long plainQuantizedTotal = scenario.includeQuantizedStream
                ? plainQuantizedSizes.stream().mapToLong(Integer::longValue).sum()
                : 0L;
            long diffQuantizedTotal = scenario.includeQuantizedStream
                ? diffQuantizedSizes.stream().mapToLong(Integer::longValue).sum()
                : 0L;
            long diffNoRecoveryQuantizedTotal = scenario.includeQuantizedStream
                ? diffNoRecoveryQuantizedSizes.stream().mapToLong(Integer::longValue).sum()
                : 0L;

            assertTrue("Diff stream should save bandwidth in scenario " + scenario.name(), diffTotal < plainTotal);
            assertTrue(
                "Diff no-recovery stream should save bandwidth in scenario " + scenario.name(),
                diffNoRecoveryTotal < plainTotal
            );
            if (scenario.includeQuantizedStream) {
                assertTrue(
                    "Plain quantized stream should save bandwidth in scenario " + scenario.name(),
                    plainQuantizedTotal < plainTotal
                );
                assertTrue(
                    "Diff quantized stream should improve over diff in scenario " + scenario.name(),
                    diffQuantizedTotal < diffTotal
                );
                assertTrue(
                    "Diff no-recovery quantized stream should improve over diff no-recovery in scenario " +
                    scenario.name(),
                    diffNoRecoveryQuantizedTotal < diffNoRecoveryTotal
                );
            }

            List<Integer> plainSorted = new ArrayList<>(plainSizes);
            List<Integer> diffSorted = new ArrayList<>(diffSizes);
            List<Integer> diffNoRecoverySorted = new ArrayList<>(diffNoRecoverySizes);
            List<Integer> plainQuantizedSorted = scenario.includeQuantizedStream ? new ArrayList<>(plainQuantizedSizes) : null;
            List<Integer> diffQuantizedSorted = scenario.includeQuantizedStream ? new ArrayList<>(diffQuantizedSizes) : null;
            List<Integer> diffNoRecoveryQuantizedSorted = scenario.includeQuantizedStream
                ? new ArrayList<>(diffNoRecoveryQuantizedSizes)
                : null;
            Collections.sort(plainSorted);
            Collections.sort(diffSorted);
            Collections.sort(diffNoRecoverySorted);
            if (scenario.includeQuantizedStream) {
                Collections.sort(plainQuantizedSorted);
                Collections.sort(diffQuantizedSorted);
                Collections.sort(diffNoRecoveryQuantizedSorted);
            }

            long packets = plainSizes.size();
            long plainAvg = avg(plainTotal, plainSizes.size());
            long diffAvg = avg(diffTotal, diffSizes.size());
            long diffNoRecoveryAvg = avg(diffNoRecoveryTotal, diffNoRecoverySizes.size());
            long plainQuantizedAvg = scenario.includeQuantizedStream ? avg(plainQuantizedTotal, plainQuantizedSizes.size()) : 0L;
            long diffQuantizedAvg = scenario.includeQuantizedStream ? avg(diffQuantizedTotal, diffQuantizedSizes.size()) : 0L;
            long diffNoRecoveryQuantizedAvg = scenario.includeQuantizedStream
                ? avg(diffNoRecoveryQuantizedTotal, diffNoRecoveryQuantizedSizes.size())
                : 0L;

            int plainP50 = percentile(plainSorted, 50);
            int plainP95 = percentile(plainSorted, 95);
            int diffP50 = percentile(diffSorted, 50);
            int diffP95 = percentile(diffSorted, 95);
            int diffNoRecoveryP50 = percentile(diffNoRecoverySorted, 50);
            int diffNoRecoveryP95 = percentile(diffNoRecoverySorted, 95);
            int plainQuantizedP50 = scenario.includeQuantizedStream ? percentile(plainQuantizedSorted, 50) : 0;
            int plainQuantizedP95 = scenario.includeQuantizedStream ? percentile(plainQuantizedSorted, 95) : 0;
            int diffQuantizedP50 = scenario.includeQuantizedStream ? percentile(diffQuantizedSorted, 50) : 0;
            int diffQuantizedP95 = scenario.includeQuantizedStream ? percentile(diffQuantizedSorted, 95) : 0;
            int diffNoRecoveryQuantizedP50 = scenario.includeQuantizedStream
                ? percentile(diffNoRecoveryQuantizedSorted, 50)
                : 0;
            int diffNoRecoveryQuantizedP95 = scenario.includeQuantizedStream
                ? percentile(diffNoRecoveryQuantizedSorted, 95)
                : 0;

            long savedDiff = plainTotal - diffTotal;
            long savedDiffNoRecovery = plainTotal - diffNoRecoveryTotal;
            long savedPlainQuantized = scenario.includeQuantizedStream ? plainTotal - plainQuantizedTotal : 0L;
            long savedDiffQuantized = scenario.includeQuantizedStream ? plainTotal - diffQuantizedTotal : 0L;
            long savedDiffNoRecoveryQuantized = scenario.includeQuantizedStream
                ? plainTotal - diffNoRecoveryQuantizedTotal
                : 0L;
            double savedDiffPct = plainTotal == 0 ? 0d : (savedDiff * 100.0d / plainTotal);
            double savedDiffNoRecoveryPct = plainTotal == 0 ? 0d : (savedDiffNoRecovery * 100.0d / plainTotal);
            double savedPlainQuantizedPct = scenario.includeQuantizedStream && plainTotal != 0
                ? (savedPlainQuantized * 100.0d / plainTotal)
                : 0d;
            double savedDiffQuantizedPct = scenario.includeQuantizedStream && plainTotal != 0
                ? (savedDiffQuantized * 100.0d / plainTotal)
                : 0d;
            double savedDiffNoRecoveryQuantizedPct = scenario.includeQuantizedStream && plainTotal != 0
                ? (savedDiffNoRecoveryQuantized * 100.0d / plainTotal)
                : 0d;

            int peerCount = scenario.entitiesPerTick;
            double plainUpload30 = kbitPerSecondPerPeerFullMesh(plainAvg, peerCount, 30);
            double plainUpload60 = kbitPerSecondPerPeerFullMesh(plainAvg, peerCount, 60);
            double diffUpload30 = kbitPerSecondPerPeerFullMesh(diffAvg, peerCount, 30);
            double diffUpload60 = kbitPerSecondPerPeerFullMesh(diffAvg, peerCount, 60);
            double diffNoRecoveryUpload30 = kbitPerSecondPerPeerFullMesh(diffNoRecoveryAvg, peerCount, 30);
            double diffNoRecoveryUpload60 = kbitPerSecondPerPeerFullMesh(diffNoRecoveryAvg, peerCount, 60);
            double plainQuantizedUpload30 = scenario.includeQuantizedStream
                ? kbitPerSecondPerPeerFullMesh(plainQuantizedAvg, peerCount, 30)
                : 0d;
            double plainQuantizedUpload60 = scenario.includeQuantizedStream
                ? kbitPerSecondPerPeerFullMesh(plainQuantizedAvg, peerCount, 60)
                : 0d;
            double diffQuantizedUpload30 = scenario.includeQuantizedStream
                ? kbitPerSecondPerPeerFullMesh(diffQuantizedAvg, peerCount, 30)
                : 0d;
            double diffQuantizedUpload60 = scenario.includeQuantizedStream
                ? kbitPerSecondPerPeerFullMesh(diffQuantizedAvg, peerCount, 60)
                : 0d;
            double diffNoRecoveryQuantizedUpload30 = scenario.includeQuantizedStream
                ? kbitPerSecondPerPeerFullMesh(diffNoRecoveryQuantizedAvg, peerCount, 30)
                : 0d;
            double diffNoRecoveryQuantizedUpload60 = scenario.includeQuantizedStream
                ? kbitPerSecondPerPeerFullMesh(diffNoRecoveryQuantizedAvg, peerCount, 60)
                : 0d;

            report
                .append("\n--- Scenario: ")
                .append(scenario.name())
                .append(" ---\n")
                .append(scenario.summary)
                .append("\n")
                .append(scenario.technicalDescription)
                .append("\n")
                .append(scenario.inGameExample)
                .append("\n")
                .append("Simulated over ")
                .append(ticks)
                .append(" ticks with ")
                .append(scenario.entitiesPerTick)
                .append(" entities per tick for a total of ")
                .append(packets)
                .append(" packets.")
                .append(
                    scenario.includeQuantizedStream
                        ? "\nQuantized transform profile: arena bounds origin(-512,-16,-512), size(1024,64,1024), max position error 0.02 m."
                        : ""
                )
                .append("\n\n")
                .append(
                    String.format(
                        java.util.Locale.ROOT,
                        "%-30s | %-24s | %-10s | %-23s | %-23s%n",
                        "Stream",
                        "Total",
                        "Avg pkt",
                        "Typical packet",
                        "High packet"
                    )
                )
                .append(
                    String.format(
                        java.util.Locale.ROOT,
                        "%-30s | %-24s | %-10d | %-23s | %-23s%n",
                        "Plain",
                        formatBytes(plainTotal),
                        plainAvg,
                        formatPacketSize(plainP50),
                        formatPacketSize(plainP95)
                    )
                )
                .append(
                    scenario.includeQuantizedStream
                        ? String.format(
                            java.util.Locale.ROOT,
                            "%-30s | %-24s | %-10d | %-23s | %-23s%n",
                            "Plain quantized",
                            formatBytes(plainQuantizedTotal),
                            plainQuantizedAvg,
                            formatPacketSize(plainQuantizedP50),
                            formatPacketSize(plainQuantizedP95)
                        )
                        : ""
                )
                .append(
                    String.format(
                        java.util.Locale.ROOT,
                        "%-30s | %-24s | %-10d | %-23s | %-23s%n",
                        "Diff",
                        formatBytes(diffTotal),
                        diffAvg,
                        formatPacketSize(diffP50),
                        formatPacketSize(diffP95)
                    )
                )
                .append(
                    scenario.includeQuantizedStream
                        ? String.format(
                            java.util.Locale.ROOT,
                            "%-30s | %-24s | %-10d | %-23s | %-23s%n",
                            "Diff quantized",
                            formatBytes(diffQuantizedTotal),
                            diffQuantizedAvg,
                            formatPacketSize(diffQuantizedP50),
                            formatPacketSize(diffQuantizedP95)
                        )
                        : ""
                )
                .append(
                    String.format(
                        java.util.Locale.ROOT,
                        "%-30s | %-24s | %-10d | %-23s | %-23s%n",
                        "Diff no recover",
                        formatBytes(diffNoRecoveryTotal),
                        diffNoRecoveryAvg,
                        formatPacketSize(diffNoRecoveryP50),
                        formatPacketSize(diffNoRecoveryP95)
                    )
                )
                .append(
                    scenario.includeQuantizedStream
                        ? String.format(
                            java.util.Locale.ROOT,
                            "%-30s | %-24s | %-10d | %-23s | %-23s%n",
                            "Diff no recover quantized",
                            formatBytes(diffNoRecoveryQuantizedTotal),
                            diffNoRecoveryQuantizedAvg,
                            formatPacketSize(diffNoRecoveryQuantizedP50),
                            formatPacketSize(diffNoRecoveryQuantizedP95)
                        )
                        : ""
                )
                .append("\nSavings vs Plain: Diff ")
                .append(formatBytes(savedDiff))
                .append(" (")
                .append(String.format(java.util.Locale.ROOT, "%.2f", savedDiffPct))
                .append("%)")
                .append("\nSavings vs Plain: Diff no recover ")
                .append(formatBytes(savedDiffNoRecovery))
                .append(" (")
                .append(String.format(java.util.Locale.ROOT, "%.2f", savedDiffNoRecoveryPct))
                .append("%)")
                .append(
                    scenario.includeQuantizedStream
                        ? "\nSavings vs Plain: Plain quantized " +
                        formatBytes(savedPlainQuantized) +
                        " (" +
                        String.format(java.util.Locale.ROOT, "%.2f", savedPlainQuantizedPct) +
                        "%)"
                        : ""
                )
                .append(
                    scenario.includeQuantizedStream
                        ? "\nSavings vs Plain: Diff quantized " +
                        formatBytes(savedDiffQuantized) +
                        " (" +
                        String.format(java.util.Locale.ROOT, "%.2f", savedDiffQuantizedPct) +
                        "%)"
                        : ""
                )
                .append(
                    scenario.includeQuantizedStream
                        ? "\nSavings vs Plain: Diff no recover quantized " +
                        formatBytes(savedDiffNoRecoveryQuantized) +
                        " (" +
                        String.format(java.util.Locale.ROOT, "%.2f", savedDiffNoRecoveryQuantizedPct) +
                        "%)"
                        : ""
                )
                .append("\n\nPer-peer full-mesh P2P bandwidth (N=")
                .append(peerCount)
                .append(", broadcast to N-1 peers):")
                .append("\nPlain: upload @30Hz ")
                .append(formatKbit(plainUpload30))
                .append(", download @30Hz ")
                .append(formatKbit(plainUpload30))
                .append(", upload @60Hz ")
                .append(formatKbit(plainUpload60))
                .append(", download @60Hz ")
                .append(formatKbit(plainUpload60))
                .append(
                    scenario.includeQuantizedStream
                        ? "\nPlain quantized: upload @30Hz " +
                        formatKbit(plainQuantizedUpload30) +
                        ", download @30Hz " +
                        formatKbit(plainQuantizedUpload30) +
                        ", upload @60Hz " +
                        formatKbit(plainQuantizedUpload60) +
                        ", download @60Hz " +
                        formatKbit(plainQuantizedUpload60)
                        : ""
                )
                .append("\nDiff: upload @30Hz ")
                .append(formatKbit(diffUpload30))
                .append(", download @30Hz ")
                .append(formatKbit(diffUpload30))
                .append(", upload @60Hz ")
                .append(formatKbit(diffUpload60))
                .append(", download @60Hz ")
                .append(formatKbit(diffUpload60))
                .append(
                    scenario.includeQuantizedStream
                        ? "\nDiff quantized: upload @30Hz " +
                        formatKbit(diffQuantizedUpload30) +
                        ", download @30Hz " +
                        formatKbit(diffQuantizedUpload30) +
                        ", upload @60Hz " +
                        formatKbit(diffQuantizedUpload60) +
                        ", download @60Hz " +
                        formatKbit(diffQuantizedUpload60)
                        : ""
                )
                .append("\nDiff no recover: upload @30Hz ")
                .append(formatKbit(diffNoRecoveryUpload30))
                .append(", download @30Hz ")
                .append(formatKbit(diffNoRecoveryUpload30))
                .append(", upload @60Hz ")
                .append(formatKbit(diffNoRecoveryUpload60))
                .append(", download @60Hz ")
                .append(formatKbit(diffNoRecoveryUpload60))
                .append(
                    scenario.includeQuantizedStream
                        ? "\nDiff no recover quantized: upload @30Hz " +
                        formatKbit(diffNoRecoveryQuantizedUpload30) +
                        ", download @30Hz " +
                        formatKbit(diffNoRecoveryQuantizedUpload30) +
                        ", upload @60Hz " +
                        formatKbit(diffNoRecoveryQuantizedUpload60) +
                        ", download @60Hz " +
                        formatKbit(diffNoRecoveryQuantizedUpload60)
                        : ""
                )
                .append("\n");
        }

        System.out.println(report.toString());
    }

    private static Message createDiffablePacket(int tick, int entityIndex, Scenario scenario) {
        switch (scenario.packetKind) {
            case TRANSFORM: {
                DiffableTransformMessage msg = new DiffableTransformMessage();
                msg.entityId = String.format(java.util.Locale.ROOT, "player-%02d", entityIndex);
                msg.mapId = "arena-main";
                TransformSample sample = buildTransformSample(tick, entityIndex, scenario);
                msg.tx = sample.translation.x;
                msg.ty = sample.translation.y;
                msg.tz = sample.translation.z;
                msg.rx = sample.rotation.getX();
                msg.ry = sample.rotation.getY();
                msg.rz = sample.rotation.getZ();
                msg.rw = sample.rotation.getW();
                msg.sx = sample.scale.x;
                msg.sy = sample.scale.y;
                msg.sz = sample.scale.z;
                msg.setReliable(sample.reliable);
                return msg;
            }
            case CHUNK_SYNC: {
                DiffableChunkSyncMessage msg = new DiffableChunkSyncMessage();
                msg.mapId = "arena-main";
                msg.layer = 2;
                msg.chunkX = (tick / 20 + entityIndex) % 16;
                msg.chunkY = (tick / 15 + entityIndex * 3) % 16;
                msg.revision = tick;
                msg.delta = (tick % 5) != 0;
                msg.tileIds = buildChunkTiles(tick, entityIndex);
                msg.collisionBits = buildChunkCollision(tick, entityIndex);
                msg.lightLevels = buildChunkLight(tick, entityIndex);
                msg.setReliable(true);
                return msg;
            }
            case COMBAT_ACTION: {
                DiffableCombatActionMessage msg = new DiffableCombatActionMessage();
                msg.mapId = "arena-main";
                msg.attackerId = String.format(java.util.Locale.ROOT, "player-%02d", entityIndex);
                msg.targetId = String.format(java.util.Locale.ROOT, "player-%02d", (entityIndex + 7 + tick) % 24);
                msg.skillId = (tick + entityIndex) % 6;
                msg.animationId = (tick / 3 + entityIndex) % 14;
                msg.animationFrame = tick % 24;
                msg.damage = 8 + ((tick + entityIndex * 5) % 22);
                msg.critical = ((tick + entityIndex) % 11) == 0;
                msg.hitX = (float) (Math.sin((tick + entityIndex) * 0.2d) * 4.0d);
                msg.hitY = 1.2f + (entityIndex % 3) * 0.15f;
                msg.hitZ = (float) (Math.cos((tick + entityIndex) * 0.2d) * 4.0d);
                msg.dirX = (float) Math.cos((tick + entityIndex) * 0.12d);
                msg.dirY = 0f;
                msg.dirZ = (float) Math.sin((tick + entityIndex) * 0.12d);
                msg.eventSeq = tick * 100L + entityIndex;
                msg.setReliable((tick + entityIndex) % 6 == 0);
                return msg;
            }
            default:
                throw new IllegalStateException("Unsupported packet kind: " + scenario.packetKind);
        }
    }

    private static Message createPlainPacket(int tick, int entityIndex, Scenario scenario) {
        switch (scenario.packetKind) {
            case TRANSFORM: {
                PlainTransformMessage msg = new PlainTransformMessage();
                msg.entityId = String.format(java.util.Locale.ROOT, "player-%02d", entityIndex);
                msg.mapId = "arena-main";
                TransformSample sample = buildTransformSample(tick, entityIndex, scenario);
                msg.tx = sample.translation.x;
                msg.ty = sample.translation.y;
                msg.tz = sample.translation.z;
                msg.rx = sample.rotation.getX();
                msg.ry = sample.rotation.getY();
                msg.rz = sample.rotation.getZ();
                msg.rw = sample.rotation.getW();
                msg.sx = sample.scale.x;
                msg.sy = sample.scale.y;
                msg.sz = sample.scale.z;
                msg.setReliable(sample.reliable);
                return msg;
            }
            case CHUNK_SYNC: {
                PlainChunkSyncMessage msg = new PlainChunkSyncMessage();
                msg.mapId = "arena-main";
                msg.layer = 2;
                msg.chunkX = (tick / 20 + entityIndex) % 16;
                msg.chunkY = (tick / 15 + entityIndex * 3) % 16;
                msg.revision = tick;
                msg.delta = (tick % 5) != 0;
                msg.tileIds = buildChunkTiles(tick, entityIndex);
                msg.collisionBits = buildChunkCollision(tick, entityIndex);
                msg.lightLevels = buildChunkLight(tick, entityIndex);
                msg.setReliable(true);
                return msg;
            }
            case COMBAT_ACTION: {
                PlainCombatActionMessage msg = new PlainCombatActionMessage();
                msg.mapId = "arena-main";
                msg.attackerId = String.format(java.util.Locale.ROOT, "player-%02d", entityIndex);
                msg.targetId = String.format(java.util.Locale.ROOT, "player-%02d", (entityIndex + 7 + tick) % 24);
                msg.skillId = (tick + entityIndex) % 6;
                msg.animationId = (tick / 3 + entityIndex) % 14;
                msg.animationFrame = tick % 24;
                msg.damage = 8 + ((tick + entityIndex * 5) % 22);
                msg.critical = ((tick + entityIndex) % 11) == 0;
                msg.hitX = (float) (Math.sin((tick + entityIndex) * 0.2d) * 4.0d);
                msg.hitY = 1.2f + (entityIndex % 3) * 0.15f;
                msg.hitZ = (float) (Math.cos((tick + entityIndex) * 0.2d) * 4.0d);
                msg.dirX = (float) Math.cos((tick + entityIndex) * 0.12d);
                msg.dirY = 0f;
                msg.dirZ = (float) Math.sin((tick + entityIndex) * 0.12d);
                msg.eventSeq = tick * 100L + entityIndex;
                msg.setReliable((tick + entityIndex) % 6 == 0);
                return msg;
            }
            default:
                throw new IllegalStateException("Unsupported packet kind: " + scenario.packetKind);
        }
    }

    private static Message createQuantizedPacket(int tick, int entityIndex, Scenario scenario) {
        if (scenario.packetKind != PacketKind.TRANSFORM) {
            throw new IllegalArgumentException("Quantized packet stream is supported only for TRANSFORM scenarios");
        }
        QuantizedTransformMessage msg = new QuantizedTransformMessage();
        msg.entityId = String.format(java.util.Locale.ROOT, "player-%02d", entityIndex);
        msg.mapId = "arena-main";
        TransformSample sample = buildTransformSample(tick, entityIndex, scenario);
        long[] packed = ARENA_FPS_TRANSFORM_QUANTIZER.quantizeTransform(
            new Transform(sample.translation, sample.rotation, sample.scale)
        );
        msg.packedTranslation = packed[0];
        msg.packedRotation = packed[1];
        msg.packedScale = packed[2];
        msg.setReliable(sample.reliable);
        return msg;
    }

    private static Message createPlainQuantizedPacket(int tick, int entityIndex, Scenario scenario) {
        if (scenario.packetKind != PacketKind.TRANSFORM) {
            throw new IllegalArgumentException("Quantized packet stream is supported only for TRANSFORM scenarios");
        }
        PlainQuantizedTransformMessage msg = new PlainQuantizedTransformMessage();
        msg.entityId = String.format(java.util.Locale.ROOT, "player-%02d", entityIndex);
        msg.mapId = "arena-main";
        TransformSample sample = buildTransformSample(tick, entityIndex, scenario);
        long[] packed = ARENA_FPS_TRANSFORM_QUANTIZER.quantizeTransform(
            new Transform(sample.translation, sample.rotation, sample.scale)
        );
        msg.packedTranslation = packed[0];
        msg.packedRotation = packed[1];
        msg.packedScale = packed[2];
        msg.setReliable(sample.reliable);
        return msg;
    }

    private static TransformSample buildTransformSample(int tick, int entityIndex, Scenario scenario) {
        if (scenario == Scenario.MOVEMENT_HEAVY) {
            double t = tick * 0.08d;
            double phase = entityIndex * 0.35d;
            float x = (float) ((Math.sin(t + phase) * 8.0d) + entityIndex * 0.15d);
            float z = (float) ((Math.cos(t * 0.8d + phase) * 6.0d) + entityIndex * 0.12d);
            float vx = (float) (Math.cos(t + phase) * 0.6d);
            float vz = (float) (-Math.sin(t * 0.8d + phase) * 0.5d);
            float yaw = (float) Math.atan2(vz, vx);
            return new TransformSample(
                new Vector3f(x, 0f, z),
                new Quaternion().fromAngles(0f, yaw, 0f),
                new Vector3f(1f, 1f, 1f),
                (tick + entityIndex) % 10 == 0
            );
        }
        if (scenario == Scenario.MOSTLY_STATIC) {
            int phase = tick / 20;
            float x = phase * 0.02f + entityIndex * 0.01f;
            float z = phase * 0.01f + entityIndex * 0.01f;
            float yaw = ((tick + entityIndex) % 240 < 20) ? 0.35f : 0f;
            return new TransformSample(
                new Vector3f(x, 0f, z),
                new Quaternion().fromAngles(0f, yaw, 0f),
                new Vector3f(1f, 1f, 1f),
                (tick + entityIndex) % 12 == 0
            );
        }
        // BURSTY_STATE
        int burst = ((tick + entityIndex * 3) / 40) % 3;
        float speedMul = burst == 2 ? 2.2f : (burst == 1 ? 1.2f : 0.5f);
        float x = (float) ((tick * 0.03d * speedMul) + entityIndex * 0.04d);
        float z = (float) ((tick * 0.02d * speedMul) + entityIndex * 0.03d);
        float yaw = (float) ((tick % 120) * (Math.PI / 180.0));
        Vector3f scale = burst == 2 ? new Vector3f(0.95f, 0.95f, 0.95f) : new Vector3f(1f, 1f, 1f);
        return new TransformSample(
            new Vector3f(x, 0f, z),
            new Quaternion().fromAngles(0f, yaw, 0f),
            scale,
            (tick + entityIndex) % 8 == 0
        );
    }

    private static List<Integer> buildChunkTiles(int tick, int entityIndex) {
        ArrayList<Integer> tiles = new ArrayList<>(256);
        int base = (tick / 8 + entityIndex * 17) % 128;
        for (int i = 0; i < 256; i++) {
            tiles.add((base + i + ((tick % 6 == 0) ? (i % 5) : 0)) % 256);
        }
        return tiles;
    }

    private static List<Integer> buildChunkCollision(int tick, int entityIndex) {
        ArrayList<Integer> collision = new ArrayList<>(64);
        int seed = tick + entityIndex * 31;
        for (int i = 0; i < 64; i++) {
            collision.add((seed + i * 13) & 0xFF);
        }
        return collision;
    }

    private static List<Integer> buildChunkLight(int tick, int entityIndex) {
        ArrayList<Integer> light = new ArrayList<>(128);
        int phase = (tick / 12 + entityIndex) % 16;
        for (int i = 0; i < 128; i++) {
            light.add((phase + i) % 16);
        }
        return light;
    }

    private static long hash64(String key) {
        byte[] bytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long hash = 0xcbf29ce484222325L;
        for (byte b : bytes) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static int sizeOf(java.nio.ByteBuffer bb) {
        return bb == null ? 0 : bb.remaining();
    }

    private static long avg(long total, int count) {
        return count == 0 ? 0 : total / count;
    }

    private static String formatBytes(long bytes) {
        double kib = bytes / 1024.0d;
        return String.format(java.util.Locale.ROOT, "%d B (%.2f KiB)", bytes, kib);
    }

    private static double kbitPerSecondPerPeerFullMesh(long avgBytesPerPacket, int totalPeers, int tickRateHz) {
        int remotePeers = Math.max(0, totalPeers - 1);
        return (avgBytesPerPacket * remotePeers * tickRateHz * 8.0d) / 1000.0d;
    }

    private static String formatKbit(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f kbit/s", value);
    }

    private static String formatPacketSize(int bytes) {
        return bytes + " B";
    }

    private static int percentile(List<Integer> sorted, int percentile) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = Math.min(sorted.size() - 1, Math.max(0, (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1));
        return sorted.get(index);
    }
}
