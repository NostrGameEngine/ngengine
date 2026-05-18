package org.ngengine.network.interpolation;


import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Interpolates and predicts jME transforms from timestamped snapshots.
 *
 * Main behavior:
 * - accepts heavily out-of-order points
 * - stores them sorted by timestamp using binary insertion
 * - samples at (now - interpolationDelay)
 * - uses Catmull-Rom for translation and scale when 4 points are available
 * - uses SLERP for normal quaternion interpolation
 * - estimates linear velocity from recent samples using linear regression
 * - estimates angular velocity from recent quaternion deltas
 * - extrapolates for a bounded time if target time is slightly beyond the newest point
 * - returns LAG when data is too stale / too sparse / too unreliable
 * - returns RECOVERED_WITH_SNAP when recovering from lag or from a teleport-like correction
 *
 * Notes:
 * - API is Instant-based because the engine already uses Instant.
 * - Internally timestamps are stored in epoch millis for consistency with the network layer.
 * - This class is intentionally self-contained. Nested types can be extracted later.
 * - Thread-safety: this class is NOT thread-safe. Callers must externally synchronize
 *   addTransformPoint/sample or call both methods from the same thread.
 */
public final class TransformInterpolatorAndPredictor {

    private static final Duration DEFAULT_INTERPOLATION_DELAY = Duration.ofMillis(360);
    private static final Duration DEFAULT_RETENTION_WINDOW = Duration.ofSeconds(6);
    private static final Duration DEFAULT_MAX_EXTRAPOLATION = Duration.ofMillis(70);
    private static final Duration DEFAULT_MAX_GAP_BEFORE_LAG = Duration.ofMillis(1500);
    private static final int DEFAULT_MAX_STORED_POINTS = 256;
    private static final float DEFAULT_CATMULL_TENSION = 0.35f;
    private static final float DEFAULT_TELEPORT_DISTANCE = 5f;
    private static final float DEFAULT_TELEPORT_SCALE = 2f;
    private static final float DEFAULT_MAX_LINEAR_SPEED = 12f;
    private static final float DEFAULT_MAX_SCALE_SPEED = 1.0f;
    private static final float DEFAULT_MAX_ANGULAR_SPEED = 2.6f;

    public enum Status {
        OK_INTERPOLATED,
        OK_EXTRAPOLATED,
        RECOVERED_WITH_SNAP,
        EMPTY,
        LAG
    }

    public static final class SampleResult {
        public final Status status;
        public final Instant targetTime;
        public final int pointCount;
        public final long extrapolationMillis;

        public SampleResult(Status status, Instant targetTime, int pointCount, long extrapolationMillis) {
            this.status = status;
            this.targetTime = targetTime;
            this.pointCount = pointCount;
            this.extrapolationMillis = extrapolationMillis;
        }

        public boolean isLagging() {
            return status == Status.EMPTY || status == Status.LAG;
        }
    }

    private static final class TimedPoint {
        final long epochMillis;
        final Transform transform;

        TimedPoint(long epochMillis, Transform transform) {
            this.epochMillis = epochMillis;
            this.transform = deepCopy(transform);
        }
    }

    private final long interpolationDelayMillis;
    private final long retentionWindowMillis;
    private final long maxExtrapolationMillis;
    private final long maxGapBeforeLagMillis;
    private final int maxStoredPoints;
    private final float catmullRomTension;
    private final float teleportDistanceThreshold;
    private final float teleportScaleThreshold;
    private final float maxLinearSpeed;
    private final float maxScaleSpeed;
    private final float maxAngularSpeed;

    private final ArrayList<TimedPoint> points = new ArrayList<>();

    private final Transform lastSampledTransform = new Transform();
    private boolean hasLastSampledTransform = false;
    private boolean wasLagging = false;

    // Scratch vectors/quaternions to avoid allocations.
    private final Vector3f scratchPos0 = new Vector3f();
    private final Vector3f scratchPos1 = new Vector3f();
    private final Vector3f scratchScale0 = new Vector3f();
    private final Vector3f scratchScale1 = new Vector3f();

    private final Vector3f scratchVecA = new Vector3f();
    private final Vector3f scratchVecB = new Vector3f();
    private final Vector3f scratchVecC = new Vector3f();
    private final Vector3f scratchAxis = new Vector3f();

    private final Quaternion scratchQuatA = new Quaternion();
    private final Quaternion scratchQuatB = new Quaternion();
    private final Quaternion scratchQuatC = new Quaternion();

    /**
     * Sane defaults tuned for smoothness-first gameplay, scaled by world coordinates.
     *
     * <p>Distance-based limits are multiplied by {@code worldScale}, while
     * dimensionless limits are kept unchanged.
     *
     * @param worldScale linear scale of your world coordinates (must be > 0)
     */
    public TransformInterpolatorAndPredictor(float worldScale) {
        this(
                DEFAULT_INTERPOLATION_DELAY,
                DEFAULT_RETENTION_WINDOW,
                DEFAULT_MAX_EXTRAPOLATION,
                DEFAULT_MAX_GAP_BEFORE_LAG,
                DEFAULT_MAX_STORED_POINTS,
                DEFAULT_CATMULL_TENSION,
                DEFAULT_TELEPORT_DISTANCE * requirePositiveFiniteScale(worldScale),
                DEFAULT_TELEPORT_SCALE,
                DEFAULT_MAX_LINEAR_SPEED * requirePositiveFiniteScale(worldScale),
                DEFAULT_MAX_SCALE_SPEED,
                DEFAULT_MAX_ANGULAR_SPEED
        );
    }

    public TransformInterpolatorAndPredictor(
            Duration interpolationDelay,
            Duration retentionWindow,
            Duration maxExtrapolation,
            Duration maxGapBeforeLag,
            int maxStoredPoints,
            float catmullRomTension,
            float teleportDistanceThreshold,
            float teleportScaleThreshold
    ) {
        this(
                interpolationDelay,
                retentionWindow,
                maxExtrapolation,
                maxGapBeforeLag,
                maxStoredPoints,
                catmullRomTension,
                teleportDistanceThreshold,
                teleportScaleThreshold,
                250f,
                50f,
                25f
        );
    }

    public TransformInterpolatorAndPredictor(
            Duration interpolationDelay,
            Duration retentionWindow,
            Duration maxExtrapolation,
            Duration maxGapBeforeLag,
            int maxStoredPoints,
            float catmullRomTension,
            float teleportDistanceThreshold,
            float teleportScaleThreshold,
            float maxLinearSpeed,
            float maxScaleSpeed,
            float maxAngularSpeed
    ) {
        if (interpolationDelay == null || retentionWindow == null || maxExtrapolation == null || maxGapBeforeLag == null) {
            throw new IllegalArgumentException("Durations cannot be null");
        }
        if (interpolationDelay.isNegative()) {
            throw new IllegalArgumentException("interpolationDelay cannot be negative");
        }
        if (retentionWindow.isNegative() || retentionWindow.isZero()) {
            throw new IllegalArgumentException("retentionWindow must be > 0");
        }
        if (maxExtrapolation.isNegative()) {
            throw new IllegalArgumentException("maxExtrapolation cannot be negative");
        }
        if (maxGapBeforeLag.isNegative() || maxGapBeforeLag.isZero()) {
            throw new IllegalArgumentException("maxGapBeforeLag must be > 0");
        }
        if (maxStoredPoints < 4) {
            throw new IllegalArgumentException("maxStoredPoints must be >= 4");
        }
        if (!Float.isFinite(catmullRomTension)) {
            throw new IllegalArgumentException("catmullRomTension must be finite");
        }
        if (!Float.isFinite(teleportDistanceThreshold) || teleportDistanceThreshold < 0f) {
            throw new IllegalArgumentException("teleportDistanceThreshold must be >= 0");
        }
        if (!Float.isFinite(teleportScaleThreshold) || teleportScaleThreshold < 0f) {
            throw new IllegalArgumentException("teleportScaleThreshold must be >= 0");
        }
        if (!Float.isFinite(maxLinearSpeed) || maxLinearSpeed <= 0f) {
            throw new IllegalArgumentException("maxLinearSpeed must be > 0 and finite");
        }
        if (!Float.isFinite(maxScaleSpeed) || maxScaleSpeed <= 0f) {
            throw new IllegalArgumentException("maxScaleSpeed must be > 0 and finite");
        }
        if (!Float.isFinite(maxAngularSpeed) || maxAngularSpeed <= 0f) {
            throw new IllegalArgumentException("maxAngularSpeed must be > 0 and finite");
        }

        this.interpolationDelayMillis = interpolationDelay.toMillis();
        this.retentionWindowMillis = retentionWindow.toMillis();
        this.maxExtrapolationMillis = maxExtrapolation.toMillis();
        this.maxGapBeforeLagMillis = maxGapBeforeLag.toMillis();
        this.maxStoredPoints = maxStoredPoints;
        this.catmullRomTension = catmullRomTension;
        this.teleportDistanceThreshold = teleportDistanceThreshold;
        this.teleportScaleThreshold = teleportScaleThreshold;
        this.maxLinearSpeed = maxLinearSpeed;
        this.maxScaleSpeed = maxScaleSpeed;
        this.maxAngularSpeed = maxAngularSpeed;
    }

    public void clear() {
        points.clear();
        hasLastSampledTransform = false;
        wasLagging = false;
    }

    public int size() {
        return points.size();
    }

    public void addTransformPoint(Instant timestamp, Transform transform) {
        if (timestamp == null || transform == null) {
            throw new IllegalArgumentException("timestamp/transform cannot be null");
        }
        addTransformPoint(timestamp.toEpochMilli(), transform);
    }

    public void addTransformPoint(long epochMillis, Transform transform) {
        if (transform == null) {
            throw new IllegalArgumentException("transform cannot be null");
        }

        TimedPoint point = new TimedPoint(epochMillis, transform);
        int index = findInsertIndex(epochMillis);

        // Replace exact duplicate timestamp to keep semantics deterministic.
        if (index < points.size() && points.get(index).epochMillis == epochMillis) {
            points.set(index, point);
        } else {
            points.add(index, point);
        }

        // Use latest-known timestamp as trim reference so stale out-of-order packets
        // do not accidentally keep old points alive.
        trimStorage(points.get(points.size() - 1).epochMillis);
    }

    public SampleResult sample(Instant now, Transform out) {
        if (now == null || out == null) {
            throw new IllegalArgumentException("now/out cannot be null");
        }
        return sample(now.toEpochMilli(), out);
    }

    public SampleResult sample(long nowEpochMillis, Transform out) {
        if (out == null) {
            throw new IllegalArgumentException("out cannot be null");
        }

        if (points.isEmpty()) {
            wasLagging = true;
            return new SampleResult(
                    Status.EMPTY,
                    Instant.ofEpochMilli(nowEpochMillis - interpolationDelayMillis),
                    0,
                    0L
            );
        }

        final long targetMillis = nowEpochMillis - interpolationDelayMillis;

        // Trim using whichever is newer: current time or newest point.
        trimStorage(Math.max(nowEpochMillis, points.get(points.size() - 1).epochMillis));

        if (points.isEmpty()) {
            wasLagging = true;
            return new SampleResult(Status.EMPTY, Instant.ofEpochMilli(targetMillis), 0, 0L);
        }

        // If target is before the first point, snap to first point.
        if (targetMillis <= points.get(0).epochMillis) {
            copyTransform(points.get(0).transform, out);
            boolean recovered = wasLagging || shouldTeleport(out);
            updateLastSampled(out);
            wasLagging = false;
            return new SampleResult(
                    recovered ? Status.RECOVERED_WITH_SNAP : Status.OK_INTERPOLATED,
                    Instant.ofEpochMilli(targetMillis),
                    points.size(),
                    0L
            );
        }

        int rightIndex = findFirstStrictlyAfter(targetMillis);
        int leftIndex = rightIndex - 1;

        // Interpolation path.
        if (leftIndex >= 0 && rightIndex < points.size()) {
            TimedPoint left = points.get(leftIndex);
            TimedPoint right = points.get(rightIndex);

            long gap = right.epochMillis - left.epochMillis;
            if (gap <= 0L || gap > maxGapBeforeLagMillis) {
                wasLagging = true;
                return new SampleResult(
                        Status.LAG,
                        Instant.ofEpochMilli(targetMillis),
                        points.size(),
                        0L
                );
            }

            interpolateAt(leftIndex, rightIndex, targetMillis, out);

            boolean recovered = wasLagging || shouldTeleport(out);
            updateLastSampled(out);
            wasLagging = false;

            return new SampleResult(
                    recovered ? Status.RECOVERED_WITH_SNAP : Status.OK_INTERPOLATED,
                    Instant.ofEpochMilli(targetMillis),
                    points.size(),
                    0L
            );
        }

        // Extrapolation path: target is beyond newest point.
        TimedPoint last = points.get(points.size() - 1);
        long extrapolationMillis = targetMillis - last.epochMillis;
        if (extrapolationMillis < 0L) {
            // Defensive fallback.
            copyTransform(last.transform, out);
            boolean recovered = wasLagging || shouldTeleport(out);
            updateLastSampled(out);
            wasLagging = false;
            return new SampleResult(
                    recovered ? Status.RECOVERED_WITH_SNAP : Status.OK_INTERPOLATED,
                    Instant.ofEpochMilli(targetMillis),
                    points.size(),
                    0L
            );
        }

        if (extrapolationMillis > maxExtrapolationMillis) {
            wasLagging = true;
            return new SampleResult(
                    Status.LAG,
                    Instant.ofEpochMilli(targetMillis),
                    points.size(),
                    extrapolationMillis
            );
        }

        if (points.size() < 2) {
            wasLagging = true;
            return new SampleResult(
                    Status.LAG,
                    Instant.ofEpochMilli(targetMillis),
                    points.size(),
                    extrapolationMillis
            );
        }

        TimedPoint prev = points.get(points.size() - 2);
        long recentGap = last.epochMillis - prev.epochMillis;
        if (recentGap <= 0L || recentGap > maxGapBeforeLagMillis) {
            wasLagging = true;
            return new SampleResult(
                    Status.LAG,
                    Instant.ofEpochMilli(targetMillis),
                    points.size(),
                    extrapolationMillis
            );
        }

        extrapolateFromTail(targetMillis, out);

        boolean recovered = wasLagging || shouldTeleport(out);
        updateLastSampled(out);
        wasLagging = false;

        return new SampleResult(
                recovered ? Status.RECOVERED_WITH_SNAP : Status.OK_EXTRAPOLATED,
                Instant.ofEpochMilli(targetMillis),
                points.size(),
                extrapolationMillis
        );
    }

    private void interpolateAt(int leftIndex, int rightIndex, long targetMillis, Transform out) {
        TimedPoint p1 = points.get(leftIndex);
        TimedPoint p2 = points.get(rightIndex);

        float u = normalizedTime(p1.epochMillis, p2.epochMillis, targetMillis);

        // Catmull-Rom if we have four points around the target interval.
        boolean hasCatmull = leftIndex - 1 >= 0 && rightIndex + 1 < points.size();

        if (hasCatmull) {
            TimedPoint p0 = points.get(leftIndex - 1);
            TimedPoint p3 = points.get(rightIndex + 1);
            long dt10Millis = p1.epochMillis - p0.epochMillis;
            long dt21Millis = p2.epochMillis - p1.epochMillis;
            long dt32Millis = p3.epochMillis - p2.epochMillis;

            // Guard Catmull against sparse/irregular neighbors.
            if (dt10Millis <= 0L
                    || dt21Millis <= 0L
                    || dt32Millis <= 0L
                    || dt10Millis > maxGapBeforeLagMillis
                    || dt32Millis > maxGapBeforeLagMillis) {
                hasCatmull = false;
            } else {
                interpolateCatmullRomTimeAware(
                        u,
                        catmullRomTension,
                        p0.transform.getTranslation(),
                        p1.transform.getTranslation(),
                        p2.transform.getTranslation(),
                        p3.transform.getTranslation(),
                        dt10Millis,
                        dt21Millis,
                        dt32Millis,
                        out.getTranslation(),
                        scratchPos0,
                        scratchPos1
                );
                interpolateCatmullRomTimeAware(
                        u,
                        catmullRomTension,
                        p0.transform.getScale(),
                        p1.transform.getScale(),
                        p2.transform.getScale(),
                        p3.transform.getScale(),
                        dt10Millis,
                        dt21Millis,
                        dt32Millis,
                        out.getScale(),
                        scratchScale0,
                        scratchScale1
                );
            }
        }

        if (!hasCatmull) {
            // Fallback when only two/three points are available or neighbors are unreliable.
            out.getTranslation().set(p1.transform.getTranslation());
            out.getTranslation().interpolateLocal(p2.transform.getTranslation(), u);

            out.getScale().set(p1.transform.getScale());
            out.getScale().interpolateLocal(p2.transform.getScale(), u);
        }

        // Rotation interpolation stays on quaternion SLERP.
        // In this jME version Quaternion#slerp(Quaternion,float) may mutate q2
        // when it flips sign for shortest-path interpolation, so keep using scratchQuatA.
        out.getRotation().set(p1.transform.getRotation());
        scratchQuatA.set(p2.transform.getRotation());
        out.getRotation().slerp(scratchQuatA, u);
        out.getRotation().normalizeLocal();
        clampScalePositive(out.getScale());
    }

    private static void interpolateCatmullRomTimeAware(
            float u,
            float tension,
            Vector3f p0,
            Vector3f p1,
            Vector3f p2,
            Vector3f p3,
            long dt10Millis,
            long dt21Millis,
            long dt32Millis,
            Vector3f out,
            Vector3f tangent1Scratch,
            Vector3f tangent2Scratch
    ) {
        float dt10 = dt10Millis / 1000f;
        float dt21 = dt21Millis / 1000f;
        float dt32 = dt32Millis / 1000f;
        float dt20 = dt10 + dt21;
        float dt31 = dt21 + dt32;

        // Degenerate time spacing should never happen because callers gate it.
        if (dt20 <= 0f || dt31 <= 0f) {
            out.set(p1).interpolateLocal(p2, u);
            return;
        } else {
            float tangentScale1 = tension * (dt21 / dt20);
            float tangentScale2 = tension * (dt21 / dt31);
            tangent1Scratch.set(p2).subtractLocal(p0).multLocal(tangentScale1);
            tangent2Scratch.set(p3).subtractLocal(p1).multLocal(tangentScale2);
        }

        float u2 = u * u;
        float u3 = u2 * u;
        float h00 = (2f * u3) - (3f * u2) + 1f;
        float h10 = u3 - (2f * u2) + u;
        float h01 = (-2f * u3) + (3f * u2);
        float h11 = u3 - u2;

        out.set(
                h00 * p1.x + h10 * tangent1Scratch.x + h01 * p2.x + h11 * tangent2Scratch.x,
                h00 * p1.y + h10 * tangent1Scratch.y + h01 * p2.y + h11 * tangent2Scratch.y,
                h00 * p1.z + h10 * tangent1Scratch.z + h01 * p2.z + h11 * tangent2Scratch.z
        );
    }

    private void extrapolateFromTail(long targetMillis, Transform out) {
        TimedPoint last = points.get(points.size() - 1);
        float futureSeconds = (targetMillis - last.epochMillis) / 1000f;

        estimateVelocityByRegression(points, true, scratchVecA);
        estimateVelocityByRegression(points, false, scratchVecB);
        estimateAngularVelocity(points, scratchVecC);
        clampMagnitude(scratchVecA, maxLinearSpeed);
        clampMagnitude(scratchVecB, maxScaleSpeed);
        clampMagnitude(scratchVecC, maxAngularSpeed);

        // Position extrapolation.
        out.getTranslation().set(last.transform.getTranslation());
        out.getTranslation().x += scratchVecA.x * futureSeconds;
        out.getTranslation().y += scratchVecA.y * futureSeconds;
        out.getTranslation().z += scratchVecA.z * futureSeconds;

        // Scale extrapolation.
        out.getScale().set(last.transform.getScale());
        out.getScale().x += scratchVecB.x * futureSeconds;
        out.getScale().y += scratchVecB.y * futureSeconds;
        out.getScale().z += scratchVecB.z * futureSeconds;

        // Prevent pathological negative / zero scale from bad extrapolation.
        clampScalePositive(out.getScale());

        // Rotation extrapolation from averaged angular velocity.
        float angularSpeed = scratchVecC.length();
        if (angularSpeed < 1e-6f) {
            out.getRotation().set(last.transform.getRotation());
        } else {
            scratchAxis.set(scratchVecC).normalizeLocal();
            float angle = angularSpeed * futureSeconds;
            scratchQuatA.fromAngleAxis(angle, scratchAxis);
            out.getRotation().set(last.transform.getRotation()).multLocal(scratchQuatA).normalizeLocal();
        }
    }

    /**
     * Estimates velocity by linear regression on up to the last 6 points.
     * translation == true  -> translation velocity
     * translation == false -> scale velocity
     *
     * Output units: value per second.
     */
    private static void estimateVelocityByRegression(List<TimedPoint> points, boolean translation, Vector3f outVelocity) {
        outVelocity.set(0f, 0f, 0f);

        int count = points.size();
        int start = Math.max(0, count - 6);
        int n = count - start;
        if (n < 2) {
            return;
        }

        long t0 = points.get(count - 1).epochMillis;

        double sumT = 0.0;
        double sumTT = 0.0;

        double sumX = 0.0;
        double sumY = 0.0;
        double sumZ = 0.0;

        double sumTX = 0.0;
        double sumTY = 0.0;
        double sumTZ = 0.0;

        for (int i = start; i < count; i++) {
            TimedPoint p = points.get(i);
            double t = (p.epochMillis - t0) / 1000.0; // seconds relative to latest point

            Vector3f v = translation ? p.transform.getTranslation() : p.transform.getScale();

            sumT += t;
            sumTT += t * t;

            sumX += v.x;
            sumY += v.y;
            sumZ += v.z;

            sumTX += t * v.x;
            sumTY += t * v.y;
            sumTZ += t * v.z;
        }

        double denom = (n * sumTT) - (sumT * sumT);
        if (Math.abs(denom) < 1e-12) {
            return;
        }

        outVelocity.x = (float) (((n * sumTX) - (sumT * sumX)) / denom);
        outVelocity.y = (float) (((n * sumTY) - (sumT * sumY)) / denom);
        outVelocity.z = (float) (((n * sumTZ) - (sumT * sumZ)) / denom);
    }

    /**
     * Estimates averaged angular velocity vector (rad/s) from up to the last 5 segments.
     * Direction = axis, length = angular speed.
     */
    private void estimateAngularVelocity(List<TimedPoint> points, Vector3f outAngularVelocity) {
        outAngularVelocity.set(0f, 0f, 0f);

        int count = points.size();
        int start = Math.max(1, count - 5);

        float totalWeight = 0f;
        int segmentOrdinal = 1;

        for (int i = start; i < count; i++) {
            TimedPoint a = points.get(i - 1);
            TimedPoint b = points.get(i);

            long dtMillis = b.epochMillis - a.epochMillis;
            if (dtMillis <= 0L) {
                continue;
            }

            scratchQuatA.set(a.transform.getRotation()).normalizeLocal();
            scratchQuatB.set(b.transform.getRotation()).normalizeLocal();

            // delta = inverse(a) * b
            scratchQuatC.set(scratchQuatA).inverseLocal().multLocal(scratchQuatB).normalizeLocal();

            // shortest-path delta
            if (scratchQuatC.getW() < 0f) {
                scratchQuatC.set(
                        -scratchQuatC.getX(),
                        -scratchQuatC.getY(),
                        -scratchQuatC.getZ(),
                        -scratchQuatC.getW()
                );
            }

            float angle = scratchQuatC.toAngleAxis(scratchAxis);
            if (!Float.isFinite(angle) || scratchAxis.lengthSquared() < 1e-12f) {
                continue;
            }

            scratchAxis.normalizeLocal();
            float dtSeconds = dtMillis / 1000f;
            float weight = segmentOrdinal;

            outAngularVelocity.x += (scratchAxis.x * (angle / dtSeconds)) * weight;
            outAngularVelocity.y += (scratchAxis.y * (angle / dtSeconds)) * weight;
            outAngularVelocity.z += (scratchAxis.z * (angle / dtSeconds)) * weight;

            totalWeight += weight;
            segmentOrdinal++;
        }

        if (totalWeight > 0f) {
            outAngularVelocity.divideLocal(totalWeight);
        }
    }

    private boolean shouldTeleport(Transform newSample) {
        if (!hasLastSampledTransform) {
            return false;
        }

        float distance = lastSampledTransform.getTranslation().distance(newSample.getTranslation());
        if (teleportDistanceThreshold > 0f && distance > teleportDistanceThreshold) {
            return true;
        }

        if (teleportScaleThreshold > 0f) {
            Vector3f a = lastSampledTransform.getScale();
            Vector3f b = newSample.getScale();
            float diff = Math.max(Math.abs(a.x - b.x), Math.max(Math.abs(a.y - b.y), Math.abs(a.z - b.z)));
            if (diff > teleportScaleThreshold) {
                return true;
            }
        }

        return false;
    }

    private void updateLastSampled(Transform sampled) {
        copyTransform(sampled, lastSampledTransform);
        hasLastSampledTransform = true;
    }

    private void trimStorage(long newestRelevantTime) {
        long minKeepTime = newestRelevantTime - retentionWindowMillis;

        // Trim by time, but keep at least 4 points when possible so Catmull/extrapolation remain available.
        while (points.size() > 4 && points.get(0).epochMillis < minKeepTime) {
            points.remove(0);
        }

        // Hard cap by count.
        while (points.size() > maxStoredPoints) {
            points.remove(0);
        }
    }

    /**
     * Lower-bound binary search insertion index.
     * Efficient even when snapshots are heavily out of order.
     */
    private int findInsertIndex(long epochMillis) {
        int low = 0;
        int high = points.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (points.get(mid).epochMillis < epochMillis) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /**
     * First index whose timestamp is strictly greater than targetMillis.
     */
    private int findFirstStrictlyAfter(long targetMillis) {
        int low = 0;
        int high = points.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (points.get(mid).epochMillis <= targetMillis) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private static float normalizedTime(long t0, long t1, long target) {
        if (t1 <= t0) {
            return 0f;
        }
        double u = (double) (target - t0) / (double) (t1 - t0);
        return (float) clamp(u, 0.0, 1.0);
    }

    private static void copyTransform(Transform from, Transform to) {
        // jME Transform getters return mutable internal objects, so copy component-wise.
        to.getTranslation().set(from.getTranslation());
        to.getRotation().set(from.getRotation());
        to.getScale().set(from.getScale());
    }

    private static void clampScalePositive(Vector3f scale) {
        scale.x = Math.max(0.0001f, scale.x);
        scale.y = Math.max(0.0001f, scale.y);
        scale.z = Math.max(0.0001f, scale.z);
    }

    private static void clampMagnitude(Vector3f vector, float maxMagnitude) {
        float lenSq = vector.lengthSquared();
        float maxSq = maxMagnitude * maxMagnitude;
        if (lenSq <= maxSq || lenSq < 1e-12f) {
            return;
        }
        vector.multLocal(maxMagnitude / FastMath.sqrt(lenSq));
    }

    private static Transform deepCopy(Transform transform) {
        return new Transform(
                transform.getTranslation().clone(),
                transform.getRotation().clone(),
                transform.getScale().clone()
        );
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float requirePositiveFiniteScale(float worldScale) {
        if (!Float.isFinite(worldScale) || worldScale <= 0f) {
            throw new IllegalArgumentException("worldScale must be > 0 and finite");
        }
        return worldScale;
    }
}
