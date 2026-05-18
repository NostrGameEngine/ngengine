package org.ngengine.network.quantization;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

/**
 * Quantizer for jME {@link Transform}.
 *
 * <p>Outputs are designed to be serialized with VarInt/VarLong, so low bits carry compact type tags.
 * This class is application-level utility code and is not enforced by the networking protocol.
 */
public final class TransformQuantizer {

    public static final int TYPE_POSITION = 0b00;
    public static final int TYPE_ROTATION = 0b01;
    public static final int TYPE_SCALE = 0b10;
    public static final int TYPE_RESERVED = 0b11;

    private static final long TYPE_MASK = 0b11L;
    private static final int TYPE_BITS = 2;

    private static final int SCALE_MODE_ONE = 0b00;
    private static final int SCALE_MODE_HALF = 0b01;
    private static final int SCALE_MODE_UNIFORM = 0b10;
    private static final int SCALE_MODE_NON_UNIFORM = 0b11;

    private static final long SCALE_MODE_MASK = 0b11L;
    private static final int SCALE_MODE_BITS = 2;

    private final Vector3f originMin = new Vector3f();
    private final Vector3f mapSize = new Vector3f();

    private final int cellsX;
    private final int cellsY;
    private final int cellsZ;

    private final int posBitsX;
    private final int posBitsY;
    private final int posBitsZ;
    private final int totalPositionPayloadBits;

    private final long posMaskX;
    private final long posMaskY;
    private final long posMaskZ;

    private final float cellSizeX;
    private final float cellSizeY;
    private final float cellSizeZ;

    private final int rotationComponentBits;
    private final long rotationComponentMask;
    private final int totalRotationPayloadBits;

    private static final float SMALLEST_THREE_MAX = 0.7071067811865476f;

    private final float uniformScaleEpsilon;
    private final float specialScaleEpsilon;

    private final int uniformScaleBits;
    private final long uniformScaleMask;

    private final int nonUniformScaleBitsPerAxis;
    private final long nonUniformScaleAxisMask;

    private final float uniformMinLog2;
    private final float uniformMaxLog2;

    private final float nonUniformMinLog2;
    private final float nonUniformMaxLog2;

    private final int totalScaleUniformPayloadBits;
    private final int totalScaleNonUniformPayloadBits;

    public TransformQuantizer(
        Vector3f originMin,
        Vector3f mapSize,
        float maxPositionError,
        int rotationComponentBits,
        float uniformScaleEpsilon,
        float specialScaleEpsilon,
        int uniformScaleBits,
        float uniformMinScale,
        float uniformMaxScale,
        int nonUniformScaleBitsPerAxis,
        float nonUniformMinScale,
        float nonUniformMaxScale
    ) {
        if (originMin == null || mapSize == null) {
            throw new IllegalArgumentException("originMin/mapSize cannot be null");
        }
        if (mapSize.x <= 0f || mapSize.y <= 0f || mapSize.z <= 0f) {
            throw new IllegalArgumentException("mapSize must be > 0 on all axes");
        }
        if (maxPositionError <= 0f) {
            throw new IllegalArgumentException("maxPositionError must be > 0");
        }
        if (rotationComponentBits < 2 || rotationComponentBits > 20) {
            throw new IllegalArgumentException("rotationComponentBits out of sane range: " + rotationComponentBits);
        }
        if (uniformScaleEpsilon < 0f || specialScaleEpsilon < 0f) {
            throw new IllegalArgumentException("Scale epsilons must be >= 0");
        }
        if (uniformScaleBits < 1 || uniformScaleBits > 30) {
            throw new IllegalArgumentException("uniformScaleBits out of range: " + uniformScaleBits);
        }
        if (nonUniformScaleBitsPerAxis < 1 || nonUniformScaleBitsPerAxis > 20) {
            throw new IllegalArgumentException(
                "nonUniformScaleBitsPerAxis out of range: " + nonUniformScaleBitsPerAxis
            );
        }
        if (uniformMinScale <= 0f || uniformMaxScale <= 0f || uniformMinScale >= uniformMaxScale) {
            throw new IllegalArgumentException("Invalid uniform scale range");
        }
        if (nonUniformMinScale <= 0f || nonUniformMaxScale <= 0f || nonUniformMinScale >= nonUniformMaxScale) {
            throw new IllegalArgumentException("Invalid non-uniform scale range");
        }

        this.originMin.set(originMin);
        this.mapSize.set(mapSize);

        float maxCell = maxPositionError * 2f;
        this.cellsX = Math.max(1, (int) Math.ceil(mapSize.x / maxCell));
        this.cellsY = Math.max(1, (int) Math.ceil(mapSize.y / maxCell));
        this.cellsZ = Math.max(1, (int) Math.ceil(mapSize.z / maxCell));

        this.cellSizeX = mapSize.x / cellsX;
        this.cellSizeY = mapSize.y / cellsY;
        this.cellSizeZ = mapSize.z / cellsZ;

        this.posBitsX = bitsRequired(cellsX);
        this.posBitsY = bitsRequired(cellsY);
        this.posBitsZ = bitsRequired(cellsZ);

        this.posMaskX = mask(posBitsX);
        this.posMaskY = mask(posBitsY);
        this.posMaskZ = mask(posBitsZ);
        this.totalPositionPayloadBits = posBitsX + posBitsY + posBitsZ;

        if (TYPE_BITS + totalPositionPayloadBits > 64) {
            throw new IllegalArgumentException(
                "Position payload does not fit in a long: " + (TYPE_BITS + totalPositionPayloadBits) + " bits"
            );
        }

        this.rotationComponentBits = rotationComponentBits;
        this.rotationComponentMask = mask(rotationComponentBits);
        this.totalRotationPayloadBits = 2 + (3 * rotationComponentBits);
        if (TYPE_BITS + totalRotationPayloadBits > 64) {
            throw new IllegalArgumentException(
                "Rotation payload does not fit in a long: " + (TYPE_BITS + totalRotationPayloadBits) + " bits"
            );
        }

        this.uniformScaleEpsilon = uniformScaleEpsilon;
        this.specialScaleEpsilon = specialScaleEpsilon;

        this.uniformScaleBits = uniformScaleBits;
        this.uniformScaleMask = mask(uniformScaleBits);

        this.nonUniformScaleBitsPerAxis = nonUniformScaleBitsPerAxis;
        this.nonUniformScaleAxisMask = mask(nonUniformScaleBitsPerAxis);

        this.uniformMinLog2 = log2(uniformMinScale);
        this.uniformMaxLog2 = log2(uniformMaxScale);

        this.nonUniformMinLog2 = log2(nonUniformMinScale);
        this.nonUniformMaxLog2 = log2(nonUniformMaxScale);

        this.totalScaleUniformPayloadBits = uniformScaleBits;
        this.totalScaleNonUniformPayloadBits = nonUniformScaleBitsPerAxis * 3;

        if (TYPE_BITS + SCALE_MODE_BITS + totalScaleUniformPayloadBits > 64) {
            throw new IllegalArgumentException(
                "Uniform scale payload does not fit in a long: " +
                (TYPE_BITS + SCALE_MODE_BITS + totalScaleUniformPayloadBits) +
                " bits"
            );
        }
        if (TYPE_BITS + SCALE_MODE_BITS + totalScaleNonUniformPayloadBits > 64) {
            throw new IllegalArgumentException(
                "Non-uniform scale payload does not fit in a long: " +
                (TYPE_BITS + SCALE_MODE_BITS + totalScaleNonUniformPayloadBits) +
                " bits"
            );
        }
    }

    public TransformQuantizer(Vector3f originMin, Vector3f mapSize, float maxPositionError) {
        this(
            originMin,
            mapSize,
            maxPositionError,
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
    }

    public long quantizePosition(Vector3f position) {
        if (position == null) {
            throw new IllegalArgumentException("position cannot be null");
        }
        int ix = quantizeAxis(position.x, originMin.x, mapSize.x, cellsX);
        int iy = quantizeAxis(position.y, originMin.y, mapSize.y, cellsY);
        int iz = quantizeAxis(position.z, originMin.z, mapSize.z, cellsZ);

        long packed = TYPE_POSITION;
        int shift = TYPE_BITS;
        packed |= ((long) ix & posMaskX) << shift;
        shift += posBitsX;
        packed |= ((long) iy & posMaskY) << shift;
        shift += posBitsY;
        packed |= ((long) iz & posMaskZ) << shift;
        return packed;
    }

    public Vector3f dequantizePosition(long packed) {
        Vector3f out = new Vector3f();
        dequantizePosition(packed, out);
        return out;
    }

    public void dequantizePosition(long packed, Vector3f out) {
        requireType(packed, TYPE_POSITION);
        if (out == null) {
            throw new IllegalArgumentException("out cannot be null");
        }

        int shift = TYPE_BITS;
        int ix = (int) ((packed >>> shift) & posMaskX);
        shift += posBitsX;
        int iy = (int) ((packed >>> shift) & posMaskY);
        shift += posBitsY;
        int iz = (int) ((packed >>> shift) & posMaskZ);

        out.x = dequantizeAxis(ix, originMin.x, cellSizeX, cellsX);
        out.y = dequantizeAxis(iy, originMin.y, cellSizeY, cellsY);
        out.z = dequantizeAxis(iz, originMin.z, cellSizeZ, cellsZ);
    }

    public long quantizeRotation(Quaternion rotation) {
        if (rotation == null) {
            throw new IllegalArgumentException("rotation cannot be null");
        }

        Quaternion q = rotation.clone().normalizeLocal();
        float x = q.getX();
        float y = q.getY();
        float z = q.getZ();
        float w = q.getW();

        int omittedIndex = 0;
        float maxAbs = Math.abs(x);
        if (Math.abs(y) > maxAbs) {
            omittedIndex = 1;
            maxAbs = Math.abs(y);
        }
        if (Math.abs(z) > maxAbs) {
            omittedIndex = 2;
            maxAbs = Math.abs(z);
        }
        if (Math.abs(w) > maxAbs) {
            omittedIndex = 3;
        }

        float omittedValue = componentByIndex(x, y, z, w, omittedIndex);
        if (omittedValue < 0f) {
            x = -x;
            y = -y;
            z = -z;
            w = -w;
        }

        float a;
        float b;
        float c;
        switch (omittedIndex) {
            case 0:
                a = y;
                b = z;
                c = w;
                break;
            case 1:
                a = x;
                b = z;
                c = w;
                break;
            case 2:
                a = x;
                b = y;
                c = w;
                break;
            default:
                a = x;
                b = y;
                c = z;
                break;
        }

        int qa = quantizeSignedFloatToBits(a, -SMALLEST_THREE_MAX, SMALLEST_THREE_MAX, rotationComponentBits);
        int qb = quantizeSignedFloatToBits(b, -SMALLEST_THREE_MAX, SMALLEST_THREE_MAX, rotationComponentBits);
        int qc = quantizeSignedFloatToBits(c, -SMALLEST_THREE_MAX, SMALLEST_THREE_MAX, rotationComponentBits);

        long packed = TYPE_ROTATION;
        int shift = TYPE_BITS;
        packed |= ((long) omittedIndex & 0b11L) << shift;
        shift += 2;
        packed |= ((long) qa & rotationComponentMask) << shift;
        shift += rotationComponentBits;
        packed |= ((long) qb & rotationComponentMask) << shift;
        shift += rotationComponentBits;
        packed |= ((long) qc & rotationComponentMask) << shift;
        return packed;
    }

    public Quaternion dequantizeRotation(long packed) {
        Quaternion out = new Quaternion();
        dequantizeRotation(packed, out);
        return out;
    }

    public void dequantizeRotation(long packed, Quaternion out) {
        requireType(packed, TYPE_ROTATION);
        if (out == null) {
            throw new IllegalArgumentException("out cannot be null");
        }

        int shift = TYPE_BITS;
        int omittedIndex = (int) ((packed >>> shift) & 0b11L);
        shift += 2;
        int qa = (int) ((packed >>> shift) & rotationComponentMask);
        shift += rotationComponentBits;
        int qb = (int) ((packed >>> shift) & rotationComponentMask);
        shift += rotationComponentBits;
        int qc = (int) ((packed >>> shift) & rotationComponentMask);

        float a = dequantizeSignedFloatFromBits(qa, -SMALLEST_THREE_MAX, SMALLEST_THREE_MAX, rotationComponentBits);
        float b = dequantizeSignedFloatFromBits(qb, -SMALLEST_THREE_MAX, SMALLEST_THREE_MAX, rotationComponentBits);
        float c = dequantizeSignedFloatFromBits(qc, -SMALLEST_THREE_MAX, SMALLEST_THREE_MAX, rotationComponentBits);

        float missingSq = 1f - (a * a + b * b + c * c);
        float missing = (float) Math.sqrt(Math.max(0f, missingSq));

        float x;
        float y;
        float z;
        float w;
        switch (omittedIndex) {
            case 0:
                x = missing;
                y = a;
                z = b;
                w = c;
                break;
            case 1:
                x = a;
                y = missing;
                z = b;
                w = c;
                break;
            case 2:
                x = a;
                y = b;
                z = missing;
                w = c;
                break;
            case 3:
                x = a;
                y = b;
                z = c;
                w = missing;
                break;
            default:
                throw new IllegalStateException("Invalid omittedIndex: " + omittedIndex);
        }

        out.set(x, y, z, w).normalizeLocal();
    }

    public long quantizeScale(Vector3f scale) {
        if (scale == null) {
            throw new IllegalArgumentException("scale cannot be null");
        }

        if (
            isAlmost(scale.x, 1f, specialScaleEpsilon) &&
            isAlmost(scale.y, 1f, specialScaleEpsilon) &&
            isAlmost(scale.z, 1f, specialScaleEpsilon)
        ) {
            return TYPE_SCALE | (((long) SCALE_MODE_ONE) << TYPE_BITS);
        }

        if (
            isAlmost(scale.x, 0.5f, specialScaleEpsilon) &&
            isAlmost(scale.y, 0.5f, specialScaleEpsilon) &&
            isAlmost(scale.z, 0.5f, specialScaleEpsilon)
        ) {
            return TYPE_SCALE | (((long) SCALE_MODE_HALF) << TYPE_BITS);
        }

        if (
            isAlmost(scale.x, scale.y, uniformScaleEpsilon) &&
            isAlmost(scale.y, scale.z, uniformScaleEpsilon)
        ) {
            float s = clampPositive(scale.x);
            float logv = clamp(log2(s), uniformMinLog2, uniformMaxLog2);
            int q = quantizeUnsignedFloatToBits(logv, uniformMinLog2, uniformMaxLog2, uniformScaleBits);
            long packed = TYPE_SCALE | (((long) SCALE_MODE_UNIFORM) << TYPE_BITS);
            packed |= ((long) q & uniformScaleMask) << (TYPE_BITS + SCALE_MODE_BITS);
            return packed;
        }

        float sx = clampPositive(scale.x);
        float sy = clampPositive(scale.y);
        float sz = clampPositive(scale.z);

        int qx = quantizeUnsignedFloatToBits(
            clamp(log2(sx), nonUniformMinLog2, nonUniformMaxLog2),
            nonUniformMinLog2,
            nonUniformMaxLog2,
            nonUniformScaleBitsPerAxis
        );
        int qy = quantizeUnsignedFloatToBits(
            clamp(log2(sy), nonUniformMinLog2, nonUniformMaxLog2),
            nonUniformMinLog2,
            nonUniformMaxLog2,
            nonUniformScaleBitsPerAxis
        );
        int qz = quantizeUnsignedFloatToBits(
            clamp(log2(sz), nonUniformMinLog2, nonUniformMaxLog2),
            nonUniformMinLog2,
            nonUniformMaxLog2,
            nonUniformScaleBitsPerAxis
        );

        long packed = TYPE_SCALE | (((long) SCALE_MODE_NON_UNIFORM) << TYPE_BITS);
        int shift = TYPE_BITS + SCALE_MODE_BITS;
        packed |= ((long) qx & nonUniformScaleAxisMask) << shift;
        shift += nonUniformScaleBitsPerAxis;
        packed |= ((long) qy & nonUniformScaleAxisMask) << shift;
        shift += nonUniformScaleBitsPerAxis;
        packed |= ((long) qz & nonUniformScaleAxisMask) << shift;
        return packed;
    }

    public Vector3f dequantizeScale(long packed) {
        Vector3f out = new Vector3f();
        dequantizeScale(packed, out);
        return out;
    }

    public void dequantizeScale(long packed, Vector3f out) {
        requireType(packed, TYPE_SCALE);
        if (out == null) {
            throw new IllegalArgumentException("out cannot be null");
        }

        int mode = getScaleMode(packed);
        switch (mode) {
            case SCALE_MODE_ONE:
                out.set(1f, 1f, 1f);
                return;
            case SCALE_MODE_HALF:
                out.set(0.5f, 0.5f, 0.5f);
                return;
            case SCALE_MODE_UNIFORM: {
                int shift = TYPE_BITS + SCALE_MODE_BITS;
                int q = (int) ((packed >>> shift) & uniformScaleMask);
                float logv = dequantizeUnsignedFloatFromBits(q, uniformMinLog2, uniformMaxLog2, uniformScaleBits);
                float s = exp2(logv);
                out.set(s, s, s);
                return;
            }
            case SCALE_MODE_NON_UNIFORM: {
                int shift = TYPE_BITS + SCALE_MODE_BITS;
                int qx = (int) ((packed >>> shift) & nonUniformScaleAxisMask);
                shift += nonUniformScaleBitsPerAxis;
                int qy = (int) ((packed >>> shift) & nonUniformScaleAxisMask);
                shift += nonUniformScaleBitsPerAxis;
                int qz = (int) ((packed >>> shift) & nonUniformScaleAxisMask);
                float sx = exp2(
                    dequantizeUnsignedFloatFromBits(
                        qx,
                        nonUniformMinLog2,
                        nonUniformMaxLog2,
                        nonUniformScaleBitsPerAxis
                    )
                );
                float sy = exp2(
                    dequantizeUnsignedFloatFromBits(
                        qy,
                        nonUniformMinLog2,
                        nonUniformMaxLog2,
                        nonUniformScaleBitsPerAxis
                    )
                );
                float sz = exp2(
                    dequantizeUnsignedFloatFromBits(
                        qz,
                        nonUniformMinLog2,
                        nonUniformMaxLog2,
                        nonUniformScaleBitsPerAxis
                    )
                );
                out.set(sx, sy, sz);
                return;
            }
            default:
                throw new IllegalStateException("Unknown scale mode: " + mode);
        }
    }

    public long[] quantizeTransform(Transform transform) {
        if (transform == null) {
            throw new IllegalArgumentException("transform cannot be null");
        }
        long[] out = new long[3];
        quantizeTransform(transform, out);
        return out;
    }

    public void quantizeTransform(Transform transform, long[] out) {
        if (transform == null) {
            throw new IllegalArgumentException("transform cannot be null");
        }
        if (out == null || out.length < 3) {
            throw new IllegalArgumentException("out must be a long[3] or larger");
        }
        out[0] = quantizePosition(transform.getTranslation());
        out[1] = quantizeRotation(transform.getRotation());
        out[2] = quantizeScale(transform.getScale());
    }

    public Transform dequantizeTransform(long[] in) {
        if (in == null || in.length < 3) {
            throw new IllegalArgumentException("in must be a long[3] or larger");
        }
        Vector3f translation = dequantizePosition(in[0]);
        Quaternion rotation = dequantizeRotation(in[1]);
        Vector3f scale = dequantizeScale(in[2]);
        return new Transform(translation, rotation, scale);
    }

    public void dequantizeTransform(long[] in, Transform out) {
        if (in == null || in.length < 3) {
            throw new IllegalArgumentException("in must be a long[3] or larger");
        }
        if (out == null) {
            throw new IllegalArgumentException("out cannot be null");
        }
        dequantizePosition(in[0], out.getTranslation());
        dequantizeRotation(in[1], out.getRotation());
        dequantizeScale(in[2], out.getScale());
    }

    @SuppressWarnings("unchecked")
    public <T> T dequantize(long packed) {
        int type = getType(packed);
        if (type == TYPE_POSITION) {
            return (T) dequantizePosition(packed);
        }
        if (type == TYPE_ROTATION) {
            return (T) dequantizeRotation(packed);
        }
        if (type == TYPE_SCALE) {
            return (T) dequantizeScale(packed);
        }
        throw new IllegalArgumentException("Unsupported packed type: " + type);
    }

    public int getType(long packed) {
        return (int) (packed & TYPE_MASK);
    }

    public int getScaleMode(long packed) {
        requireType(packed, TYPE_SCALE);
        return (int) ((packed >>> TYPE_BITS) & SCALE_MODE_MASK);
    }

    public Vector3f getOriginMin() {
        return originMin.clone();
    }

    public Vector3f getMapSize() {
        return mapSize.clone();
    }

    public float getActualMaxPositionErrorX() {
        return cellSizeX * 0.5f;
    }

    public float getActualMaxPositionErrorY() {
        return cellSizeY * 0.5f;
    }

    public float getActualMaxPositionErrorZ() {
        return cellSizeZ * 0.5f;
    }

    public int getPositionPayloadBits() {
        return totalPositionPayloadBits;
    }

    public int getRotationPayloadBits() {
        return totalRotationPayloadBits;
    }

    public int getRotationComponentBits() {
        return rotationComponentBits;
    }

    public int getUniformScaleBits() {
        return uniformScaleBits;
    }

    public int getNonUniformScaleBitsPerAxis() {
        return nonUniformScaleBitsPerAxis;
    }

    private static void requireType(long packed, int expectedType) {
        int actualType = (int) (packed & TYPE_MASK);
        if (actualType != expectedType) {
            throw new IllegalArgumentException(
                "Packed value type mismatch. Expected " + expectedType + ", got " + actualType
            );
        }
    }

    private static int quantizeAxis(float value, float min, float size, int cells) {
        float max = min + size;
        float clamped = clamp(value, min, Math.nextDown(max));
        float normalized = (clamped - min) / size;
        int idx = (int) Math.floor(normalized * cells);
        return clamp(idx, 0, cells - 1);
    }

    private static float dequantizeAxis(int index, float min, float cellSize, int cells) {
        int clamped = clamp(index, 0, cells - 1);
        return min + (clamped + 0.5f) * cellSize;
    }

    private static int bitsRequired(int distinctValues) {
        if (distinctValues <= 1) {
            return 1;
        }
        return Math.max(1, 32 - Integer.numberOfLeadingZeros(distinctValues - 1));
    }

    private static long mask(int bits) {
        if (bits <= 0) {
            return 0L;
        }
        if (bits >= 64) {
            return -1L;
        }
        return (1L << bits) - 1L;
    }

    private static float componentByIndex(float x, float y, float z, float w, int index) {
        switch (index) {
            case 0:
                return x;
            case 1:
                return y;
            case 2:
                return z;
            case 3:
                return w;
            default:
                throw new IllegalArgumentException("Invalid index: " + index);
        }
    }

    private static int quantizeSignedFloatToBits(float value, float min, float max, int bits) {
        float clamped = clamp(value, min, max);
        float normalized = (clamped - min) / (max - min);
        int levels = (1 << bits) - 1;
        return clamp(Math.round(normalized * levels), 0, levels);
    }

    private static float dequantizeSignedFloatFromBits(int q, float min, float max, int bits) {
        int levels = (1 << bits) - 1;
        int clamped = clamp(q, 0, levels);
        float normalized = levels == 0 ? 0f : ((float) clamped / (float) levels);
        return min + normalized * (max - min);
    }

    private static int quantizeUnsignedFloatToBits(float value, float min, float max, int bits) {
        float clamped = clamp(value, min, max);
        float normalized = (clamped - min) / (max - min);
        int levels = (1 << bits) - 1;
        return clamp(Math.round(normalized * levels), 0, levels);
    }

    private static float dequantizeUnsignedFloatFromBits(int q, float min, float max, int bits) {
        int levels = (1 << bits) - 1;
        int clamped = clamp(q, 0, levels);
        float normalized = levels == 0 ? 0f : ((float) clamped / (float) levels);
        return min + normalized * (max - min);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float log2(float v) {
        return (float) (Math.log(v) / Math.log(2.0));
    }

    private static float exp2(float v) {
        return (float) Math.pow(2.0, v);
    }

    private static float clampPositive(float v) {
        return Math.max(v, 1e-8f);
    }

    private static boolean isAlmost(float a, float b, float eps) {
        return Math.abs(a - b) <= eps;
    }
}
