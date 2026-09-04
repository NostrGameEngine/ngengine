package com.jme3.util;

import com.jme3.system.Annotations.Internal;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Supplier;

/**
 * The factory of buffer allocators.
 *
 * @author JavaSaBR
 */
@Internal
public class BufferAllocatorFactory {

    public static final String PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION = "com.jme3.BufferAllocatorImplementation";

    private static final Logger LOGGER = Logger.getLogger(BufferAllocatorFactory.class.getName());
    private static volatile Supplier<? extends BufferAllocator> allocatorSupplier;

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private BufferAllocatorFactory() {
    }

    /**
     * Configures an allocator factory without requiring reflective construction.
     * Platform backends should call this during their system initialization,
     * before {@link BufferUtils} is initialized.
     *
     * @param supplier allocator supplier, or {@code null} to restore property-based lookup
     */
    public static void setBufferAllocatorSupplier(
            Supplier<? extends BufferAllocator> supplier) {
        allocatorSupplier = supplier;
    }

    @Internal
    protected static BufferAllocator create() {

        Supplier<? extends BufferAllocator> supplier = allocatorSupplier;
        if (supplier != null) {
            return supplier.get();
        }

        final String className = System.getProperty(PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION, ReflectionAllocator.class.getName());
        try {
            return (BufferAllocator) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (final Throwable e) {
            LOGGER.log(Level.WARNING, "Unable to access {0}", className);
            return new PrimitiveAllocator();
        }
    }
}
