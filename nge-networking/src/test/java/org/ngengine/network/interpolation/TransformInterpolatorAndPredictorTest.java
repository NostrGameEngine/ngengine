package org.ngengine.network.interpolation;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import org.ngengine.network.interpolation.TransformInterpolatorAndPredictor.SampleResult;
import org.ngengine.network.interpolation.TransformInterpolatorAndPredictor.Status;

public class TransformInterpolatorAndPredictorTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    private static TransformInterpolatorAndPredictor newInterpolator() {
        return new TransformInterpolatorAndPredictor(
                Duration.ofMillis(200),
                Duration.ofSeconds(3),
                Duration.ofMillis(150),
                Duration.ofMillis(400),
                128,
                0.5f,
                10.0f,
                10.0f
        );
    }

    private static TransformInterpolatorAndPredictor newInterpolatorNoDelay() {
        return new TransformInterpolatorAndPredictor(
                Duration.ZERO,
                Duration.ofSeconds(3),
                Duration.ofMillis(150),
                Duration.ofMillis(400),
                128,
                0.5f,
                1_000_000f,
                1_000_000f
        );
    }

    @Test
    public void worldScaleConstructorMatchesExplicitDefaultsAtScaleOne() {
        TransformInterpolatorAndPredictor explicit = new TransformInterpolatorAndPredictor(
                Duration.ofMillis(360),
                Duration.ofSeconds(6),
                Duration.ofMillis(70),
                Duration.ofMillis(1500),
                256,
                0.35f,
                5f,
                2f,
                12f,
                1.0f,
                2.6f
        );
        TransformInterpolatorAndPredictor scaled = new TransformInterpolatorAndPredictor(1f);

        for (int i = 0; i <= 10; i++) {
            long t = i * 100L;
            Transform point = transform(i * 0.75f, 0f, i * 0.4f, yawDeg(i * 9f), 1f);
            addPoint(explicit, t, point);
            addPoint(scaled, t, point);
        }

        Transform outExplicit = new Transform();
        Transform outScaled = new Transform();
        SampleResult a = explicit.sample(BASE.plusMillis(980), outExplicit);
        SampleResult b = scaled.sample(BASE.plusMillis(980), outScaled);

        assertEquals(a.status, b.status);
        assertVecNear(outExplicit.getTranslation(), outScaled.getTranslation(), 1e-5f);
        assertQuatNear(outExplicit.getRotation(), outScaled.getRotation(), 1e-5f);
        assertVecNear(outExplicit.getScale(), outScaled.getScale(), 1e-5f);
    }

    @Test
    public void interpolatesPreciselyWithOrderedPoints() {
        TransformInterpolatorAndPredictor ip = newInterpolator();

        addPoint(ip, 0,   transform(0f, 0f, 0f, yawDeg(0f), 1f));
        addPoint(ip, 100, transform(1f, 0f, 2f, yawDeg(10f), 1.1f));
        addPoint(ip, 200, transform(2f, 0f, 4f, yawDeg(20f), 1.2f));
        addPoint(ip, 300, transform(3f, 0f, 6f, yawDeg(30f), 1.3f));
        addPoint(ip, 400, transform(4f, 0f, 8f, yawDeg(40f), 1.4f));

        // now=450, delay=200 => target=250
        Transform out = new Transform();
        SampleResult result = ip.sample(BASE.plusMillis(450), out);

        assertEquals(Status.OK_INTERPOLATED, result.status);
        assertVecNear(new Vector3f(2.5f, 0f, 5f), out.getTranslation(), 1e-4f);
        assertVecNear(new Vector3f(1.25f, 1.25f, 1.25f), out.getScale(), 1e-4f);
        assertYawNear(25f, out.getRotation(), 0.05f);
    }

    @Test
    public void unorderedInsertionMatchesOrderedResult() {
        List<Long> times = new ArrayList<>();
        for (int i = 0; i <= 8; i++) {
            times.add((long) i * 100L);
        }

        TransformInterpolatorAndPredictor ordered = newInterpolator();
        for (long t : times) {
            addPoint(ordered, t, transform(
                    t / 100f,
                    0f,
                    (t / 100f) * 3f,
                    yawDeg((float) t / 10f),
                    1f + (t / 1000f)
            ));
        }

        TransformInterpolatorAndPredictor shuffled = newInterpolator();
        List<Long> shuffledTimes = new ArrayList<>(times);
        Collections.shuffle(shuffledTimes, new Random(12345L));
        for (long t : shuffledTimes) {
            addPoint(shuffled, t, transform(
                    t / 100f,
                    0f,
                    (t / 100f) * 3f,
                    yawDeg((float) t / 10f),
                    1f + (t / 1000f)
            ));
        }

        Transform outOrdered = new Transform();
        Transform outShuffled = new Transform();

        SampleResult a = ordered.sample(BASE.plusMillis(850), outOrdered);   // target=650
        SampleResult b = shuffled.sample(BASE.plusMillis(850), outShuffled);

        assertEquals(Status.OK_INTERPOLATED, a.status);
        assertEquals(Status.OK_INTERPOLATED, b.status);

        assertVecNear(outOrdered.getTranslation(), outShuffled.getTranslation(), 1e-5f);
        assertVecNear(outOrdered.getScale(), outShuffled.getScale(), 1e-5f);
        assertQuatNear(outOrdered.getRotation(), outShuffled.getRotation(), 1e-5f);
    }

    @Test
    public void duplicateTimestampReplacesExistingPoint() {
        TransformInterpolatorAndPredictor ip = newInterpolator();

        addPoint(ip, 0,   transform(0f, 0f, 0f, yawDeg(0f), 1f));
        addPoint(ip, 100, transform(1f, 0f, 0f, yawDeg(10f), 1f));
        addPoint(ip, 200, transform(2f, 0f, 0f, yawDeg(20f), 1f));
        addPoint(ip, 300, transform(3f, 0f, 0f, yawDeg(30f), 1f));

        // Replace timestamp 200 with a very different value.
        addPoint(ip, 200, transform(20f, 0f, 0f, yawDeg(200f), 2f));

        Transform out = new Transform();
        SampleResult result = ip.sample(BASE.plusMillis(450), out); // target=250

        assertEquals(Status.OK_INTERPOLATED, result.status);
        assertTrue(out.getTranslation().x > 10f);
    }

    @Test
    public void shortPacketDropUsesExtrapolation() {
        TransformInterpolatorAndPredictor ip = newInterpolator();

        addPoint(ip, 0,   transform(0f, 0f, 0f, yawDeg(0f), 1f));
        addPoint(ip, 100, transform(1f, 0f, 0f, yawDeg(10f), 1f));
        addPoint(ip, 200, transform(2f, 0f, 0f, yawDeg(20f), 1f));
        addPoint(ip, 300, transform(3f, 0f, 0f, yawDeg(30f), 1f));

        // now=620 => target=420, 120ms beyond latest point, within maxExtrapolation=150
        Transform out = new Transform();
        SampleResult result = ip.sample(BASE.plusMillis(620), out);

        assertEquals(Status.OK_EXTRAPOLATED, result.status);
        assertVecNear(new Vector3f(4.2f, 0f, 0f), out.getTranslation(), 0.03f);
        assertYawNear(42f, out.getRotation(), 1.0f);
    }

    @Test
    public void longPacketDropReturnsLagAndDoesNotModifyOutput() {
        TransformInterpolatorAndPredictor ip = newInterpolator();

        addPoint(ip, 0,   transform(0f, 0f, 0f, yawDeg(0f), 1f));
        addPoint(ip, 100, transform(1f, 0f, 0f, yawDeg(10f), 1f));
        addPoint(ip, 200, transform(2f, 0f, 0f, yawDeg(20f), 1f));

        Transform out = new Transform(
                new Vector3f(99f, 99f, 99f),
                yawDeg(99f),
                new Vector3f(9f, 9f, 9f)
        );

        SampleResult result = ip.sample(BASE.plusMillis(1000), out);

        assertEquals(Status.LAG, result.status);
        assertVecNear(new Vector3f(99f, 99f, 99f), out.getTranslation(), 0f);
        assertVecNear(new Vector3f(9f, 9f, 9f), out.getScale(), 0f);
        assertYawNear(99f, out.getRotation(), 0.0001f);
    }

    @Test
    public void recoveryAfterLagReturnsRecoveredWithSnap() {
        TransformInterpolatorAndPredictor ip = newInterpolator();

        addPoint(ip, 0,   transform(0f, 0f, 0f, yawDeg(0f), 1f));
        addPoint(ip, 100, transform(1f, 0f, 0f, yawDeg(10f), 1f));
        addPoint(ip, 200, transform(2f, 0f, 0f, yawDeg(20f), 1f));

        Transform out = new Transform();

        SampleResult lag = ip.sample(BASE.plusMillis(1000), out);
        assertEquals(Status.LAG, lag.status);

        addPoint(ip, 900,  transform(9f, 0f, 0f, yawDeg(90f), 1f));
        addPoint(ip, 1000, transform(10f, 0f, 0f, yawDeg(100f), 1f));
        addPoint(ip, 1100, transform(11f, 0f, 0f, yawDeg(110f), 1f));
        addPoint(ip, 1200, transform(12f, 0f, 0f, yawDeg(120f), 1f));

        SampleResult recovered = ip.sample(BASE.plusMillis(1300), out); // target=1100
        assertEquals(Status.RECOVERED_WITH_SNAP, recovered.status);
        assertVecNear(new Vector3f(11f, 0f, 0f), out.getTranslation(), 1e-4f);
        assertYawNear(110f, out.getRotation(), 0.05f);
    }

    @Test
    public void increasinglyShuffledDataStillTracksExpectedPath() {
        List<Transform> expected = new ArrayList<>();
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            long t = i * 100L;
            times.add(t);
            expected.add(transform(
                    i * 1.5f,
                    0f,
                    i * 0.5f,
                    yawDeg(i * 7f),
                    1f + i * 0.02f
            ));
        }

        for (int seed = 1; seed <= 5; seed++) {
            TransformInterpolatorAndPredictor ip = newInterpolator();

            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < times.size(); i++) {
                indices.add(i);
            }
            Collections.shuffle(indices, new Random(seed * 999L));

            for (int index : indices) {
                addPoint(ip, times.get(index), expected.get(index));
            }

            Transform out = new Transform();
            SampleResult result = ip.sample(BASE.plusMillis(1550), out); // target=1350

            assertTrue(result.status == Status.OK_INTERPOLATED || result.status == Status.RECOVERED_WITH_SNAP);
            assertVecNear(new Vector3f(20.25f, 0f, 6.75f), out.getTranslation(), 0.08f);
            assertYawNear(94.5f, out.getRotation(), 1.0f);
            assertVecNear(new Vector3f(1.27f, 1.27f, 1.27f), out.getScale(), 0.03f);
        }
    }

    @Test
    public void heavyDisorderAndLossStillProducesReasonableStates() {
        TransformInterpolatorAndPredictor ip = newInterpolator();
        Random rnd = new Random(424242L);

        List<Long> times = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            times.add((long) i * 100L);
        }

        Collections.shuffle(times, rnd);

        for (Long t : times) {
            // Drop some points on purpose.
            if (rnd.nextFloat() < 0.25f) {
                continue;
            }
            float sec = t / 1000f;
            addPoint(ip, t, transform(
                    sec * 2f,
                    0f,
                    sec * sec,
                    yawDeg(sec * 20f),
                    1f + sec * 0.05f
            ));
        }

        Transform out = new Transform();
        SampleResult result = ip.sample(BASE.plusMillis(2600), out); // target=2400

        assertTrue(
                result.status == Status.OK_INTERPOLATED
                        || result.status == Status.OK_EXTRAPOLATED
                        || result.status == Status.RECOVERED_WITH_SNAP
                        || result.status == Status.LAG
        );

        if (!result.isLagging()) {
            assertTrue(Float.isFinite(out.getTranslation().x));
            assertTrue(Float.isFinite(out.getTranslation().y));
            assertTrue(Float.isFinite(out.getTranslation().z));
            assertTrue(Float.isFinite(out.getScale().x));
            assertTrue(out.getScale().x > 0f);
            assertTrue(out.getScale().y > 0f);
            assertTrue(out.getScale().z > 0f);
        }
    }

    @Test
    public void teleportThresholdTriggersRecoveredWithSnap() {
        TransformInterpolatorAndPredictor ip = new TransformInterpolatorAndPredictor(
                Duration.ofMillis(200),
                Duration.ofSeconds(3),
                Duration.ofMillis(150),
                Duration.ofMillis(400),
                128,
                0.5f,
                2.0f,
                10.0f
        );

        addPoint(ip, 0,   transform(0f, 0f, 0f, yawDeg(0f), 1f));
        addPoint(ip, 100, transform(1f, 0f, 0f, yawDeg(10f), 1f));
        addPoint(ip, 200, transform(2f, 0f, 0f, yawDeg(20f), 1f));
        addPoint(ip, 300, transform(3f, 0f, 0f, yawDeg(30f), 1f));

        Transform out = new Transform();
        SampleResult normal = ip.sample(BASE.plusMillis(450), out); // target=250
        assertEquals(Status.OK_INTERPOLATED, normal.status);

        addPoint(ip, 400, transform(100f, 0f, 0f, yawDeg(40f), 1f));
        addPoint(ip, 500, transform(101f, 0f, 0f, yawDeg(50f), 1f));
        addPoint(ip, 600, transform(102f, 0f, 0f, yawDeg(60f), 1f));
        addPoint(ip, 700, transform(103f, 0f, 0f, yawDeg(70f), 1f));

        SampleResult snapped = ip.sample(BASE.plusMillis(850), out); // target=650
        assertEquals(Status.RECOVERED_WITH_SNAP, snapped.status);
        assertTrue(out.getTranslation().x > 100f);
    }

    @Test
    public void staleHugeGapInsideBracketReturnsLag() {
        TransformInterpolatorAndPredictor ip = newInterpolator();

        addPoint(ip, 0,    transform(0f, 0f, 0f, yawDeg(0f), 1f));
        addPoint(ip, 100,  transform(1f, 0f, 0f, yawDeg(10f), 1f));
        addPoint(ip, 1000, transform(10f, 0f, 0f, yawDeg(100f), 1f));
        addPoint(ip, 1100, transform(11f, 0f, 0f, yawDeg(110f), 1f));

        Transform out = new Transform();
        // target=500 lies in huge bracket gap [100,1000]
        SampleResult result = ip.sample(BASE.plusMillis(700), out);

        assertEquals(Status.LAG, result.status);
    }

    @Test
    public void emptyReturnsEmptyAndNeverMutatesOutput() {
        TransformInterpolatorAndPredictor ip = newInterpolator();
        Transform out = new Transform(
                new Vector3f(7f, 8f, 9f),
                yawDeg(37f),
                new Vector3f(2f, 3f, 4f)
        );

        SampleResult result = ip.sample(BASE.plusMillis(250), out);

        assertEquals(Status.EMPTY, result.status);
        assertVecNear(new Vector3f(7f, 8f, 9f), out.getTranslation(), 0f);
        assertVecNear(new Vector3f(2f, 3f, 4f), out.getScale(), 0f);
        assertYawNear(37f, out.getRotation(), 0.0001f);
    }

    @Test
    public void addPointDeepCopiesInputTransform() {
        TransformInterpolatorAndPredictor ip = newInterpolatorNoDelay();

        Transform source = transform(10f, 2f, 3f, yawDeg(55f), 1.5f);
        ip.addTransformPoint(BASE.plusMillis(100), source);

        source.getTranslation().set(999f, 999f, 999f);
        source.getRotation().fromAngles(0f, 0f, 0f);
        source.getScale().set(9f, 9f, 9f);

        Transform out = new Transform();
        SampleResult result = ip.sample(BASE.plusMillis(100), out);

        assertEquals(Status.OK_INTERPOLATED, result.status);
        assertVecNear(new Vector3f(10f, 2f, 3f), out.getTranslation(), 0f);
        assertVecNear(new Vector3f(1.5f, 1.5f, 1.5f), out.getScale(), 0f);
        assertYawNear(55f, out.getRotation(), 0.01f);
    }

    @Test
    public void duplicateStormAlwaysUsesLatestValueForTimestamp() {
        TransformInterpolatorAndPredictor ip = newInterpolatorNoDelay();
        for (int i = 0; i < 300; i++) {
            addPoint(ip, 500, transform(i, 0f, -i, yawDeg(i), 1f + (i * 0.001f)));
        }

        Transform out = new Transform();
        SampleResult result = ip.sample(BASE.plusMillis(500), out);

        assertEquals(Status.OK_INTERPOLATED, result.status);
        assertVecNear(new Vector3f(299f, 0f, -299f), out.getTranslation(), 0f);
        assertYawNear(299f, out.getRotation(), 0.01f);
    }

    @Test
    public void randomOutOfOrderWithDuplicatesIsDeterministicPerTimestamp() {
        TransformInterpolatorAndPredictor ip = newInterpolatorNoDelay();
        Random rnd = new Random(998877L);

        Map<Long, Transform> latestByTime = new HashMap<>();

        for (int i = 0; i < 1500; i++) {
            long t = rnd.nextInt(120) * 25L;
            float f = i * 0.01f;
            Transform tr = transform(t * 0.01f + f, 0f, -t * 0.005f, yawDeg((t * 0.03f) + i), 1f + (f * 0.1f));
            latestByTime.put(t, cloneTransform(tr));
            addPoint(ip, t, tr);
        }

        List<Long> times = new ArrayList<>(latestByTime.keySet());
        Collections.sort(times);

        Transform out = new Transform();
        for (Long t : times) {
            SampleResult result = ip.sample(BASE.plusMillis(t), out);
            assertTrue(result.status == Status.OK_INTERPOLATED || result.status == Status.OK_EXTRAPOLATED);

            Transform expected = latestByTime.get(t);
            assertVecNear(expected.getTranslation(), out.getTranslation(), 1e-4f);
            assertQuatNear(expected.getRotation(), out.getRotation(), 1e-4f);
            assertVecNear(expected.getScale(), out.getScale(), 1e-4f);
        }
    }

    @Test
    public void burstPacketLossThenFreshBurstRecoversDeterministically() {
        TransformInterpolatorAndPredictor ip = newInterpolator();
        for (int i = 0; i <= 8; i++) {
            addPoint(ip, i * 100L, transform(i, 0f, 0f, yawDeg(i * 8f), 1f));
        }

        Transform out = new Transform();
        SampleResult beforeLoss = ip.sample(BASE.plusMillis(1000), out); // target=800
        assertTrue(beforeLoss.status == Status.OK_INTERPOLATED || beforeLoss.status == Status.OK_EXTRAPOLATED);
        Transform beforeLagOutput = cloneTransform(out);

        SampleResult lag = ip.sample(BASE.plusMillis(1400), out); // target=1200, too far
        assertEquals(Status.LAG, lag.status);
        assertVecNear(beforeLagOutput.getTranslation(), out.getTranslation(), 0f);
        assertQuatNear(beforeLagOutput.getRotation(), out.getRotation(), 0f);
        assertVecNear(beforeLagOutput.getScale(), out.getScale(), 0f);

        addPoint(ip, 1300, transform(13f, 0f, 0f, yawDeg(104f), 1f));
        addPoint(ip, 1400, transform(14f, 0f, 0f, yawDeg(112f), 1f));
        addPoint(ip, 1500, transform(15f, 0f, 0f, yawDeg(120f), 1f));
        addPoint(ip, 1600, transform(16f, 0f, 0f, yawDeg(128f), 1f));

        SampleResult recovered = ip.sample(BASE.plusMillis(1800), out); // target=1600
        assertEquals(Status.RECOVERED_WITH_SNAP, recovered.status);
        assertVecNear(new Vector3f(16f, 0f, 0f), out.getTranslation(), 1e-4f);
    }

    @Test
    public void nonUniformScaleStaysPositiveForInterpolationAndExtrapolation() {
        TransformInterpolatorAndPredictor ip = newInterpolatorNoDelay();

        ip.addTransformPoint(BASE.plusMillis(0), new Transform(new Vector3f(0f, 0f, 0f), yawDeg(0f), new Vector3f(2f, 1f, 0.75f)));
        ip.addTransformPoint(BASE.plusMillis(100), new Transform(new Vector3f(1f, 0f, 0f), yawDeg(10f), new Vector3f(1.7f, 0.9f, 0.7f)));
        ip.addTransformPoint(BASE.plusMillis(200), new Transform(new Vector3f(2f, 0f, 0f), yawDeg(20f), new Vector3f(1.4f, 0.8f, 0.65f)));
        ip.addTransformPoint(BASE.plusMillis(300), new Transform(new Vector3f(3f, 0f, 0f), yawDeg(30f), new Vector3f(1.1f, 0.7f, 0.6f)));

        Transform out = new Transform();
        SampleResult interpolated = ip.sample(BASE.plusMillis(250), out);
        assertEquals(Status.OK_INTERPOLATED, interpolated.status);
        assertTrue(out.getScale().x > 0f);
        assertTrue(out.getScale().y > 0f);
        assertTrue(out.getScale().z > 0f);

        SampleResult extrapolated = ip.sample(BASE.plusMillis(380), out);
        assertEquals(Status.OK_EXTRAPOLATED, extrapolated.status);
        assertTrue(out.getScale().x > 0f);
        assertTrue(out.getScale().y > 0f);
        assertTrue(out.getScale().z > 0f);
    }

    @Test
    public void nearOneEightyRotationsStayStable() {
        TransformInterpolatorAndPredictor ip = newInterpolatorNoDelay();
        addPoint(ip, 0, transform(0f, 0f, 0f, yawDeg(170f), 1f));
        addPoint(ip, 100, transform(1f, 0f, 0f, yawDeg(175f), 1f));
        addPoint(ip, 200, transform(2f, 0f, 0f, yawDeg(179f), 1f));
        addPoint(ip, 300, transform(3f, 0f, 0f, yawDeg(-179f), 1f));
        addPoint(ip, 400, transform(4f, 0f, 0f, yawDeg(-175f), 1f));
        addPoint(ip, 500, transform(5f, 0f, 0f, yawDeg(-170f), 1f));

        Transform out = new Transform();
        SampleResult result = ip.sample(BASE.plusMillis(350), out);
        assertEquals(Status.OK_INTERPOLATED, result.status);

        float[] angles = out.getRotation().toAngles(null);
        assertTrue(Float.isFinite(angles[0]));
        assertTrue(Float.isFinite(angles[1]));
        assertTrue(Float.isFinite(angles[2]));
        assertYawNear(-177f, out.getRotation(), 8f);
    }

    @Test
    public void repeatedSamplingFollowsTrajectoryWithoutRecoveredFlagWhenHealthy() {
        TransformInterpolatorAndPredictor ip = newInterpolatorNoDelay();
        for (int i = 0; i <= 40; i++) {
            float x = i * 0.8f;
            addPoint(ip, i * 50L, transform(x, 0f, x * 0.25f, yawDeg(i * 3f), 1f));
        }

        Transform out = new Transform();
        float previousX = Float.NEGATIVE_INFINITY;
        for (long t = 400; t <= 1800; t += 40) {
            SampleResult result = ip.sample(BASE.plusMillis(t), out);
            assertTrue(result.status == Status.OK_INTERPOLATED || result.status == Status.OK_EXTRAPOLATED);
            assertTrue("X went backwards at t=" + t, out.getTranslation().x >= previousX - 1e-4f);
            assertTrue("Unexpected recovered state in healthy stream", result.status != Status.RECOVERED_WITH_SNAP);
            previousX = out.getTranslation().x;
        }
    }

    @Test
    public void retentionTrimKeepsEnoughPointsForInterpolationAndExtrapolation() {
        TransformInterpolatorAndPredictor ip = new TransformInterpolatorAndPredictor(
                Duration.ZERO,
                Duration.ofMillis(220),
                Duration.ofMillis(200),
                Duration.ofMillis(300),
                16,
                0.5f,
                1_000_000f,
                1_000_000f
        );

        for (int i = 0; i < 120; i++) {
            addPoint(ip, i * 25L, transform(i, 0f, 0f, yawDeg(i), 1f));
            assertTrue(ip.size() <= 16);
            assertTrue(ip.size() >= 1);
        }

        assertTrue(ip.size() >= 4);
        Transform out = new Transform();
        SampleResult result = ip.sample(BASE.plusMillis(2975), out);
        assertTrue(result.status == Status.OK_INTERPOLATED || result.status == Status.OK_EXTRAPOLATED);
    }

    private static void addPoint(TransformInterpolatorAndPredictor ip, long millis, Transform t) {
        ip.addTransformPoint(BASE.plusMillis(millis), t);
    }

    private static Transform cloneTransform(Transform transform) {
        return new Transform(
                transform.getTranslation().clone(),
                transform.getRotation().clone(),
                transform.getScale().clone()
        );
    }

    private static Transform transform(float x, float y, float z, Quaternion rotation, float uniformScale) {
        return new Transform(
                new Vector3f(x, y, z),
                rotation.clone(),
                new Vector3f(uniformScale, uniformScale, uniformScale)
        );
    }

    private static Quaternion yawDeg(float degrees) {
        return new Quaternion().fromAngles(0f, degrees * FastMath.DEG_TO_RAD, 0f);
    }

    private static void assertVecNear(Vector3f expected, Vector3f actual, float eps) {
        assertEquals(expected.x, actual.x, eps);
        assertEquals(expected.y, actual.y, eps);
        assertEquals(expected.z, actual.z, eps);
    }

    private static void assertYawNear(float expectedDeg, Quaternion actual, float epsDeg) {
        float[] angles = actual.toAngles(null);
        float actualDeg = normalizeDegrees(angles[1] * FastMath.RAD_TO_DEG);
        float expectedNorm = normalizeDegrees(expectedDeg);
        assertEquals(expectedNorm, actualDeg, epsDeg);
    }

    private static void assertQuatNear(Quaternion expected, Quaternion actual, float eps) {
        Quaternion e = expected.clone().normalizeLocal();
        Quaternion a = actual.clone().normalizeLocal();
        float dot = Math.abs(e.dot(a));
        assertTrue("Quaternion similarity too low: " + dot, 1f - dot <= eps);
    }

    private static float normalizeDegrees(float degrees) {
        float out = degrees;
        while (out < -180f) {
            out += 360f;
        }
        while (out > 180f) {
            out -= 360f;
        }
        return out;
    }
}
