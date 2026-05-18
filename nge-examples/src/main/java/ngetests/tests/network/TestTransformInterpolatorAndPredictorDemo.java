package ngetests.tests.network;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Sphere;
import java.time.Duration;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import org.ngengine.network.interpolation.TransformInterpolatorAndPredictor;

/**
 * Visual playground for TransformInterpolatorAndPredictor with intentionally jittered snapshots.
 *
 * Controls:
 * - I: toggle interpolation/predictor usage
 * - P: toggle prediction (extrapolation acceptance)
 * - C: toggle periodic path correction jumps
 * - V: cycle sphere visibility (all / truth / raw / smooth)
 * - R: switch network scenario + reset simulation
 */
public class TestTransformInterpolatorAndPredictorDemo extends SimpleApplication implements ActionListener {

    private static final String TOGGLE_INTERPOLATION = "toggleInterpolation";
    private static final String TOGGLE_PREDICTION = "togglePrediction";
    private static final String TOGGLE_CORRECTION = "toggleCorrection";
    private static final String TOGGLE_SPHERES = "toggleSpheres";
    private static final String CYCLE_SCENARIO = "cycleScenario";

    private static final long MIN_NETWORK_DELAY_MS = 12L;

    private enum SphereViewMode {
        ALL,
        TRUTH_ONLY,
        RAW_ONLY,
        SMOOTH_ONLY
    }

    private enum NetworkScenario {
        LAN_STABLE("LAN Stable", 50L, 16L, 5f, 4f, 0.005f, 0.02f, 8L, 0.005f),
        HOME_WIFI("Home Wi-Fi", 66L, 40L, 16f, 16f, 0.03f, 0.06f, 20L, 0.02f),
        GOOD_INTERNET("Good Internet", 70L, 70L, 28f, 28f, 0.07f, 0.10f, 32L, 0.05f),
        MOBILE_4G("Mobile 4G", 85L, 95L, 40f, 50f, 0.12f, 0.14f, 45L, 0.08f),
        CONGESTED_WIFI("Congested Wi-Fi", 100L, 120L, 55f, 75f, 0.18f, 0.20f, 70L, 0.10f);

        final String label;
        final long snapshotPeriodMs;
        final long baseLatencyMs;
        final float gaussianJitterMs;
        final float positiveJitterMs;
        final float dropChance;
        final float earlyArrivalChance;
        final long earlyArrivalMaxMs;
        final float duplicateChance;

        NetworkScenario(
            String label,
            long snapshotPeriodMs,
            long baseLatencyMs,
            float gaussianJitterMs,
            float positiveJitterMs,
            float dropChance,
            float earlyArrivalChance,
            long earlyArrivalMaxMs,
            float duplicateChance
        ) {
            this.label = label;
            this.snapshotPeriodMs = snapshotPeriodMs;
            this.baseLatencyMs = baseLatencyMs;
            this.gaussianJitterMs = gaussianJitterMs;
            this.positiveJitterMs = positiveJitterMs;
            this.dropChance = dropChance;
            this.earlyArrivalChance = earlyArrivalChance;
            this.earlyArrivalMaxMs = earlyArrivalMaxMs;
            this.duplicateChance = duplicateChance;
        }
    }

    private static final class SnapshotPacket {
        final long serverTimeMs;
        final long arrivalTimeMs;
        final Transform transform;

        SnapshotPacket(long serverTimeMs, long arrivalTimeMs, Transform transform) {
            this.serverTimeMs = serverTimeMs;
            this.arrivalTimeMs = arrivalTimeMs;
            this.transform = transform;
        }
    }

    private final TransformInterpolatorAndPredictor interpolator = new TransformInterpolatorAndPredictor(
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
    private final PriorityQueue<SnapshotPacket> inFlight = new PriorityQueue<>(Comparator.comparingLong(p -> p.arrivalTimeMs));
    private final Random random = new Random(1337L);

    private final Transform authoritativeScratch = new Transform(new Vector3f(), new Quaternion(), new Vector3f(1f, 1f, 1f));
    private final Transform authoritativeAheadScratch = new Transform(new Vector3f(), new Quaternion(), new Vector3f(1f, 1f, 1f));
    private final Transform sampledScratch = new Transform(new Vector3f(), new Quaternion(), new Vector3f(1f, 1f, 1f));
    private final Transform rawLatest = new Transform(new Vector3f(), new Quaternion(), new Vector3f(1f, 1f, 1f));

    private Geometry truthSphere;
    private Geometry rawSphere;
    private Geometry smoothSphere;
    private BitmapText hud;

    private Material truthMaterial;
    private Material rawMaterial;
    private Material smoothMaterial;

    private double simulatedNowMs = 0;
    private long nextSnapshotServerMs = 0;
    private boolean hasRaw = false;

    private boolean useInterpolation = true;
    private boolean usePrediction = true;
    private boolean correctionEnabled = true;
    private SphereViewMode sphereViewMode = SphereViewMode.ALL;
    private NetworkScenario currentScenario = NetworkScenario.GOOD_INTERNET;

    private TransformInterpolatorAndPredictor.SampleResult lastSampleResult = null;
    private long receivedPackets = 0;
    private long droppedPackets = 0;
    private long outOfOrderPackets = 0;
    private long lastDeliveredServerMs = Long.MIN_VALUE;

    public static void main(String[] args) {
        TestTransformInterpolatorAndPredictorDemo app = new TestTransformInterpolatorAndPredictorDemo();
        app.setShowSettings(true);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(30f);
        flyCam.setDragToRotate(true);
        cam.setLocation(new Vector3f(0f, 14f, 24f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        AmbientLight ambient = new AmbientLight(new ColorRGBA(0.45f, 0.45f, 0.45f, 1f));
        rootNode.addLight(ambient);
        DirectionalLight sun = new DirectionalLight(new Vector3f(-0.5f, -1f, -0.4f).normalizeLocal(), ColorRGBA.White.mult(1.2f));
        rootNode.addLight(sun);

        Geometry floorGrid = new Geometry("floor-grid", new Grid(60, 60, 1f));
        Material floorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMat.setColor("Color", new ColorRGBA(0.25f, 0.25f, 0.25f, 1f));
        floorGrid.setMaterial(floorMat);
        floorGrid.center().move(0f, 0.01f, 0f);
        rootNode.attachChild(floorGrid);

        Sphere sphereMesh = new Sphere(24, 24, 0.45f);
        truthSphere = new Geometry("truth", sphereMesh);
        rawSphere = new Geometry("raw", sphereMesh);
        smoothSphere = new Geometry("smooth", sphereMesh);

        truthMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        rawMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        smoothMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        truthMaterial.setColor("Color", new ColorRGBA(0.15f, 1f, 0.15f, 0.1f));
        rawMaterial.setColor("Color", new ColorRGBA(1f, 0.22f, 0.22f, 0.1f));
        smoothMaterial.setColor("Color", new ColorRGBA(0.2f, 0.85f, 1f, 1f));
        truthMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        rawMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);

        truthSphere.setMaterial(truthMaterial);
        rawSphere.setMaterial(rawMaterial);
        smoothSphere.setMaterial(smoothMaterial);

        rootNode.attachChild(truthSphere);
        rootNode.attachChild(rawSphere);
        rootNode.attachChild(smoothSphere);

        truthSphere.move(0f, 0.55f, 0f);
        rawSphere.move(0f, 0.55f, 0f);
        smoothSphere.move(0f, 0.55f, 0f);

        inputManager.addMapping(TOGGLE_INTERPOLATION, new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping(TOGGLE_PREDICTION, new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping(TOGGLE_CORRECTION, new KeyTrigger(KeyInput.KEY_C));
        inputManager.addMapping(TOGGLE_SPHERES, new KeyTrigger(KeyInput.KEY_V));
        inputManager.addMapping(CYCLE_SCENARIO, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(this, TOGGLE_INTERPOLATION, TOGGLE_PREDICTION, TOGGLE_CORRECTION, TOGGLE_SPHERES, CYCLE_SCENARIO);

        hud = new BitmapText(guiFont);
        hud.setLocalTranslation(12f, cam.getHeight() - 12f, 0f);
        guiNode.attachChild(hud);

        applySphereVisibilityMode();
        refreshHudText();
    }

    @Override
    public void simpleUpdate(float tpf) {
        simulatedNowMs += tpf * 1000.0;
        long nowMs = (long) simulatedNowMs;

        fillAuthoritativeTransform(nowMs, authoritativeScratch);
        applyTransform(truthSphere, authoritativeScratch, 0f);

        while (nextSnapshotServerMs <= nowMs) {
            fillAuthoritativeTransform(nextSnapshotServerMs, authoritativeScratch);
            queueSnapshot(nextSnapshotServerMs, authoritativeScratch);
            nextSnapshotServerMs += currentScenario.snapshotPeriodMs;
        }

        while (!inFlight.isEmpty() && inFlight.peek().arrivalTimeMs <= nowMs) {
            SnapshotPacket packet = inFlight.poll();
            receivedPackets++;
            if (packet.serverTimeMs < lastDeliveredServerMs) {
                outOfOrderPackets++;
            }
            lastDeliveredServerMs = Math.max(lastDeliveredServerMs, packet.serverTimeMs);

            interpolator.addTransformPoint(packet.serverTimeMs, packet.transform);
            copyTransform(packet.transform, rawLatest);
            hasRaw = true;
            applyTransform(rawSphere, rawLatest, 0.45f);
        }

        if (!hasRaw) {
            refreshHudText();
            return;
        }

        if (!useInterpolation) {
            applyTransform(smoothSphere, rawLatest, -0.45f);
            smoothMaterial.setColor("Color", new ColorRGBA(1f, 1f, 0.15f, 1f));
            lastSampleResult = null;
            refreshHudText();
            return;
        }

        lastSampleResult = interpolator.sample(nowMs, sampledScratch);
        if (lastSampleResult.status == TransformInterpolatorAndPredictor.Status.EMPTY
            || lastSampleResult.status == TransformInterpolatorAndPredictor.Status.LAG) {
            smoothMaterial.setColor("Color", new ColorRGBA(0.55f, 0.55f, 0.55f, 1f));
            refreshHudText();
            return;
        }

        if (!usePrediction && lastSampleResult.status == TransformInterpolatorAndPredictor.Status.OK_EXTRAPOLATED) {
            smoothMaterial.setColor("Color", new ColorRGBA(0.8f, 0.5f, 0.1f, 1f));
            refreshHudText();
            return;
        }

        applyTransform(smoothSphere, sampledScratch, -0.45f);
        if (lastSampleResult.status == TransformInterpolatorAndPredictor.Status.OK_INTERPOLATED) {
            smoothMaterial.setColor("Color", new ColorRGBA(0.2f, 0.85f, 1f, 1f));
        } else if (lastSampleResult.status == TransformInterpolatorAndPredictor.Status.OK_EXTRAPOLATED) {
            smoothMaterial.setColor("Color", new ColorRGBA(1f, 0.65f, 0.2f, 1f));
        } else if (lastSampleResult.status == TransformInterpolatorAndPredictor.Status.RECOVERED_WITH_SNAP) {
            smoothMaterial.setColor("Color", new ColorRGBA(1f, 0.2f, 1f, 1f));
        }
        refreshHudText();
    }

    private void queueSnapshot(long serverTimeMs, Transform transform) {
        if (random.nextFloat() < currentScenario.dropChance) {
            droppedPackets++;
            return;
        }

        long jitter = Math.round(
            random.nextGaussian() * currentScenario.gaussianJitterMs
                + random.nextFloat() * currentScenario.positiveJitterMs
        );
        long latency = Math.max(MIN_NETWORK_DELAY_MS, currentScenario.baseLatencyMs + jitter);
        if (random.nextFloat() < currentScenario.earlyArrivalChance) {
            long subtract = 6L + random.nextInt((int) currentScenario.earlyArrivalMaxMs + 1);
            latency = Math.max(MIN_NETWORK_DELAY_MS, latency - subtract);
        }

        long arrivalMs = serverTimeMs + latency;
        inFlight.add(new SnapshotPacket(serverTimeMs, arrivalMs, deepCopy(transform)));

        if (random.nextFloat() < currentScenario.duplicateChance) {
            long duplicateArrival = arrivalMs + 16L + random.nextInt(120);
            inFlight.add(new SnapshotPacket(serverTimeMs, duplicateArrival, deepCopy(transform)));
        }
    }

    private void fillAuthoritativeTransform(long timeMs, Transform out) {
        fillPathPoint(timeMs, out.getTranslation());
        out.getTranslation().y = 0.55f;
        out.getScale().set(1f, 1f, 1f);

        fillPathPoint(timeMs + 22L, authoritativeAheadScratch.getTranslation());
        authoritativeAheadScratch.getTranslation().y = 0.55f;
        Vector3f dir = authoritativeAheadScratch.getTranslation().subtract(out.getTranslation(), authoritativeAheadScratch.getTranslation());
        if (dir.lengthSquared() < 1e-6f) {
            out.getRotation().set(Quaternion.IDENTITY);
            return;
        }
        dir.normalizeLocal();
        float yaw = FastMath.atan2(dir.x, dir.z);
        out.getRotation().fromAngleAxis(yaw, Vector3f.UNIT_Y);
    }

    private void fillPathPoint(long timeMs, Vector3f out) {
        float t = timeMs * 0.001f;
        float x = FastMath.sin(t * 1.15f) * 7.5f + FastMath.sin(t * 0.31f + 1.4f) * 2.7f;
        float z = FastMath.cos(t * 0.83f) * 5.3f + FastMath.sin(t * 0.53f) * 2.1f;

        if (correctionEnabled) {
            long phase = timeMs % 7000L;
            if (phase >= 3500L) {
                x += ((timeMs / 7000L) & 1L) == 0L ? 3.2f : -3.2f;
            }
        }
        out.set(x, 0.55f, z);
    }

    private void applyTransform(Geometry geometry, Transform transform, float sideOffset) {
        geometry.setLocalTranslation(
            transform.getTranslation().x + sideOffset,
            transform.getTranslation().y,
            transform.getTranslation().z
        );
        geometry.setLocalRotation(transform.getRotation());
    }

    private void refreshHudText() {
        String status = lastSampleResult == null ? "RAW_ONLY" : String.valueOf(lastSampleResult.status);
        long extrapolationMs = lastSampleResult == null ? 0L : lastSampleResult.extrapolationMillis;
        hud.setText(
            "TransformInterpolatorAndPredictor demo\n"
                + "Green: exact path | Red: jittered sampled points | Cyan/Orange/Magenta: smoothed\n"
                + "[I] Interpolation: " + useInterpolation
                + "  [P] Prediction: " + usePrediction
                + "  [C] Path correction jumps: " + correctionEnabled
                + "  [V] Sphere view: " + sphereViewMode
                + "  [R] Scenario+Reset: " + currentScenario.label + "\n"
                + "Status: " + status
                + "  extrapolationMs=" + extrapolationMs
                + "  bufferedPoints=" + interpolator.size() + "\n"
                + "snapshotPeriod=" + currentScenario.snapshotPeriodMs + "ms"
                + "  baseLatency=" + currentScenario.baseLatencyMs + "ms"
                + "  drop=" + Math.round(currentScenario.dropChance * 100f) + "%"
                + "  jitterSigma~" + Math.round(currentScenario.gaussianJitterMs) + "ms\n"
                + "received=" + receivedPackets
                + "  dropped=" + droppedPackets
                + "  outOfOrder=" + outOfOrderPackets
                + "  inFlight=" + inFlight.size()
        );
    }

    private void resetSimulation() {
        interpolator.clear();
        inFlight.clear();
        simulatedNowMs = 0;
        nextSnapshotServerMs = 0;
        hasRaw = false;
        lastSampleResult = null;
        receivedPackets = 0;
        droppedPackets = 0;
        outOfOrderPackets = 0;
        lastDeliveredServerMs = Long.MIN_VALUE;

        authoritativeScratch.getTranslation().set(0f, 0.55f, 0f);
        authoritativeScratch.getRotation().set(Quaternion.IDENTITY);
        rawLatest.getTranslation().set(0f, 0.55f, 0f);
        rawLatest.getRotation().set(Quaternion.IDENTITY);
        sampledScratch.getTranslation().set(0f, 0.55f, 0f);
        sampledScratch.getRotation().set(Quaternion.IDENTITY);

        applyTransform(truthSphere, authoritativeScratch, 0f);
        applyTransform(rawSphere, rawLatest, 0.45f);
        applyTransform(smoothSphere, sampledScratch, -0.45f);
        smoothMaterial.setColor("Color", new ColorRGBA(0.2f, 0.85f, 1f, 1f));
        applySphereVisibilityMode();
        refreshHudText();
    }

    private void applySphereVisibilityMode() {
        boolean showTruth = sphereViewMode == SphereViewMode.ALL || sphereViewMode == SphereViewMode.TRUTH_ONLY;
        boolean showRaw = sphereViewMode == SphereViewMode.ALL || sphereViewMode == SphereViewMode.RAW_ONLY;
        boolean showSmooth = sphereViewMode == SphereViewMode.ALL || sphereViewMode == SphereViewMode.SMOOTH_ONLY;

        truthSphere.setCullHint(showTruth ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
        rawSphere.setCullHint(showRaw ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
        smoothSphere.setCullHint(showSmooth ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    private void cycleScenario() {
        NetworkScenario[] scenarios = NetworkScenario.values();
        int next = currentScenario.ordinal() + 1;
        if (next >= scenarios.length) {
            next = 0;
        }
        currentScenario = scenarios[next];
    }

    private static Transform deepCopy(Transform source) {
        return new Transform(
            source.getTranslation().clone(),
            source.getRotation().clone(),
            source.getScale().clone()
        );
    }

    private static void copyTransform(Transform source, Transform target) {
        target.getTranslation().set(source.getTranslation());
        target.getRotation().set(source.getRotation());
        target.getScale().set(source.getScale());
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
            return;
        }
        if (TOGGLE_INTERPOLATION.equals(name)) {
            useInterpolation = !useInterpolation;
        } else if (TOGGLE_PREDICTION.equals(name)) {
            usePrediction = !usePrediction;
        } else if (TOGGLE_CORRECTION.equals(name)) {
            correctionEnabled = !correctionEnabled;
        } else if (TOGGLE_SPHERES.equals(name)) {
            if (sphereViewMode == SphereViewMode.ALL) {
                sphereViewMode = SphereViewMode.TRUTH_ONLY;
            } else if (sphereViewMode == SphereViewMode.TRUTH_ONLY) {
                sphereViewMode = SphereViewMode.RAW_ONLY;
            } else if (sphereViewMode == SphereViewMode.RAW_ONLY) {
                sphereViewMode = SphereViewMode.SMOOTH_ONLY;
            } else {
                sphereViewMode = SphereViewMode.ALL;
            }
            applySphereVisibilityMode();
        } else if (CYCLE_SCENARIO.equals(name)) {
            cycleScenario();
            resetSimulation();
            return;
        }
        refreshHudText();
    }
}
