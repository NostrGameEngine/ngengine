package org.ngengine.network.components;

import java.math.BigInteger;
import java.util.Objects;
import jakarta.annotation.Nullable;

/**
 * Shared network-id partitioning model used by authority and persistence flows.
 *
 * <p>The id space is split into three regions:
 * <ul>
 * <li>Shared ids: {@code [0 .. SHARED_MAX]}. ~2 billion for general use.
 * These are distributed to peers via stable hash partitioning.
 * <li>Reserved ids: {@code [RESERVED_BASE .. PERSISTENT_BASE)}. ~4 billion per peer
 * These are temporary/local ids that encode the creator peer key plus a local sequence.
 * <li>Persistent ids: {@code [PERSISTENT_BASE .. +inf)}. ~1 million per peer
 * These are long-lived ids allocated in per-peer blocks.
 * </li>
 * </ul>
 */
public final class NetcodePartitioning {
    /** Upper bound of the legacy/shared id space. */
    public static final BigInteger SHARED_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    /** First value of the reserved-id region. */
    public static final BigInteger RESERVED_BASE = BigInteger.ONE.shiftLeft(300);
    /** First value of the persistent-id region. */
    public static final BigInteger PERSISTENT_BASE = BigInteger.ONE.shiftLeft(304);
    /** Number of low bits reserved for the local sequence in reserved ids. */
    public static final int RESERVED_SEQ_BITS = 32;
    /** Bit-mask selecting the low {@link #RESERVED_SEQ_BITS} bits. */
    public static final BigInteger RESERVED_SEQ_MASK = BigInteger.ONE.shiftLeft(RESERVED_SEQ_BITS).subtract(BigInteger.ONE);
    /** Number of bits used to size each peer's persistent block. */
    public static final int PERSISTENT_RANGE_BITS = 20;
    /** Size of one peer-owned persistent-id block. */
    public static final BigInteger PERSISTENT_BLOCK_SIZE = BigInteger.ONE.shiftLeft(PERSISTENT_RANGE_BITS);

    private NetcodePartitioning() {}

    /**
     * Returns true if {@code id} belongs to the reserved-id region.
     */
    public static boolean isReservedId(@Nullable BigInteger id) {
        return id != null && id.compareTo(RESERVED_BASE) >= 0 && id.compareTo(PERSISTENT_BASE) < 0;
    }

    /**
     * Returns true if {@code id} belongs to the persistent-id region.
     */
    public static boolean isPersistentId(@Nullable BigInteger id) {
        return id != null && id.compareTo(PERSISTENT_BASE) >= 0;
    }

    /**
     * Extracts the owner key encoded inside a reserved id.
     *
     * <p>Reserved ids are laid out as:
     * {@code RESERVED_BASE + (ownerKey << RESERVED_SEQ_BITS) + seqBits}.
     */
    public static @Nullable BigInteger decodeReservedOwnerKey(@Nullable BigInteger networkId) {
        if (!isReservedId(networkId)) {
            return null;
        }
        BigInteger rel = Objects.requireNonNull(networkId).subtract(RESERVED_BASE);
        if (rel.signum() < 0) {
            return null;
        }
        return rel.shiftRight(RESERVED_SEQ_BITS);
    }

    /**
     * Extracts the owner key encoded inside a persistent id.
     *
     * <p>Persistent ids are laid out as:
     * {@code PERSISTENT_BASE + (ownerKey << PERSISTENT_RANGE_BITS) + seqBits}.
     */
    public static @Nullable BigInteger decodePersistentOwnerKey(@Nullable BigInteger networkId) {
        if (!isPersistentId(networkId)) {
            return null;
        }
        BigInteger rel = Objects.requireNonNull(networkId).subtract(PERSISTENT_BASE);
        if (rel.signum() < 0) {
            return null;
        }
        return rel.shiftRight(PERSISTENT_RANGE_BITS);
    }

    /**
     * Returns the first persistent id in the block owned by {@code requesterKey}.
     */
    public static BigInteger persistentRangeStart(BigInteger requesterKey) {
        return PERSISTENT_BASE.add(requesterKey.shiftLeft(PERSISTENT_RANGE_BITS));
    }

    /**
     * Returns true if {@code id} is inside the persistent block owned by {@code requesterKey}.
     */
    public static boolean isPersistentIdInRequesterRange(BigInteger id, BigInteger requesterKey) {
        BigInteger start = persistentRangeStart(requesterKey);
        BigInteger end = start.add(PERSISTENT_BLOCK_SIZE).subtract(BigInteger.ONE);
        return id.compareTo(start) >= 0 && id.compareTo(end) <= 0;
    }

  
    public static BigInteger nextLocalReservedId(BigInteger ownerKey, long skip) {
        if (skip < 0L || skip > RESERVED_SEQ_MASK.longValue()) {
            throw new IllegalArgumentException("Skip out of reserved id sequence range: " + skip);
        }
        BigInteger seqBits = BigInteger.valueOf(skip).and(RESERVED_SEQ_MASK);
        return RESERVED_BASE.add(ownerKey.shiftLeft(RESERVED_SEQ_BITS)).add(seqBits);
    }

   
    public static BigInteger nextLocalPersistentReservedId(BigInteger ownerKey, long skip) {
        if (skip < 0L || skip > RESERVED_SEQ_MASK.longValue()) {
            throw new IllegalArgumentException("Skip out of reserved id sequence range: " + skip);
        }
        BigInteger seqBits = BigInteger.valueOf(skip).and(RESERVED_SEQ_MASK);
        return persistentRangeStart(ownerKey).add(seqBits);
    }
}
