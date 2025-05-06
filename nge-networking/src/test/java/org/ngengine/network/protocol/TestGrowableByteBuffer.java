package org.ngengine.network.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.ReadOnlyBufferException;


import java.nio.ByteBuffer;

import org.junit.Test;

public class TestGrowableByteBuffer {
    
    @Test(expected = RuntimeException.class)
    public void testNonGrowableBuffer() {
        // Test with non-growable buffer (chunkSize <= 0)
        GrowableByteBuffer buffer = new GrowableByteBuffer(ByteBuffer.allocate(10), 0);
        buffer.put(new byte[11]); // Should throw RuntimeException
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullInitialBuffer() {
        // Test with null initial buffer
        GrowableByteBuffer buffer = new GrowableByteBuffer(null, 10);
        buffer.put((byte)1); // Should throw NullPointerException
    }
    
    @Test
    public void testVeryLargeDataExceedingChunkSize() {
        // Test putting data much larger than chunk size
        GrowableByteBuffer buffer = new GrowableByteBuffer(ByteBuffer.allocate(10), 5);
        byte[] largeArray = new byte[100]; // Much larger than buffer + chunk
        buffer.put(largeArray); // Should resize multiple times
        assertEquals(buffer.position(), 100);
    }
    
    @Test
    public void testPositionAfterMultipleGrowth() {
        // Test if position is maintained correctly after multiple growths
        GrowableByteBuffer buffer = new GrowableByteBuffer(ByteBuffer.allocate(5), 5);
        buffer.put(new byte[4]); // Position at 4
        assertEquals(4, buffer.position());
        buffer.put(new byte[3]); // Should resize and position at 7
        assertEquals(7, buffer.position());
    }
    
    @Test
    public void testBufferLimitAfterGrowth() {
        // Test if limit is maintained correctly after growth
        ByteBuffer initial = ByteBuffer.allocate(10);
        initial.limit(5); // Set a custom limit
        assertEquals(5, initial.limit());
        GrowableByteBuffer buffer = new GrowableByteBuffer(initial, 10);
        assertEquals(5, buffer.limit()); // Initial limit should be 5

        buffer.put(new byte[6]); // Should grow
        assertEquals(6, buffer.limit()); // Limit should still be 5
        assertEquals(10, buffer.capacity()); // Limit should still be 5
        buffer.put(new byte[5]); // Should grow again
        assertEquals(11, buffer.limit()); // Limit should be 11
        assertEquals(20, buffer.capacity()); // New capacity should be 20
    }

    @Test
    public void testReadOnlyBuffer() {
        // Test with read-only buffer
        ByteBuffer readOnly = ByteBuffer.allocate(10).asReadOnlyBuffer();
        GrowableByteBuffer buffer = new GrowableByteBuffer(readOnly, 5);
        try {
            buffer.put((byte)1); // Should throw ReadOnlyBufferException
            fail("Expected ReadOnlyBufferException");
        } catch (ReadOnlyBufferException e) {
            // Expected
        }
    }
    
    @Test
    public void testIntegerOverflow() {
        // Test integer overflow in capacity calculation
        GrowableByteBuffer buffer = new GrowableByteBuffer(ByteBuffer.allocate(Integer.MAX_VALUE - 5), 10);
        try {
            buffer.position(Integer.MAX_VALUE - 5);
            buffer.put(new byte[6]); // Should cause overflow in new capacity calculation
            fail("Expected exception due to integer overflow");
        } catch (Exception e) {
            // Expected some kind of exception
        }
    }
    
    @Test
    public void testPutByteBufferCornerCase() {
        // Test put(ByteBuffer) with a ByteBuffer at capacity limit
        GrowableByteBuffer buffer = new GrowableByteBuffer(ByteBuffer.allocate(5), 5);
        ByteBuffer source = ByteBuffer.allocate(10);
        source.put(new byte[10]).flip(); // Fill and prepare for reading
        buffer.put(source); // Should grow and copy all 10 bytes
        assertEquals(10, buffer.position());
    }
    
    @Test
    public void testSequentialResizing() {
        // Test many sequential small puts that cause multiple resizes
        GrowableByteBuffer buffer = new GrowableByteBuffer(ByteBuffer.allocate(2), 2);
        for (int i = 0; i < 100; i++) {
            buffer.put((byte)i);
        }
        assertEquals(100, buffer.position());
        for(int i = 0; i < 100; i++) {
            assertEquals((byte)i, buffer.get(i));
        }
    }

    @Test
    public void testDataIntegrityDuringResizes() {
        // Create a buffer with small initial capacity and chunk size
        int initialCapacity = 16;
        int chunkSize = 8;
        GrowableByteBuffer buffer = new GrowableByteBuffer(ByteBuffer.allocate(initialCapacity), chunkSize);

        // Phase 1: Fill with a recognizable pattern that exceeds initial capacity
        byte[] pattern1 = new byte[initialCapacity * 2];
        for (int i = 0; i < pattern1.length; i++) {
            pattern1[i] = (byte) (i % 256);
        }
        buffer.put(pattern1);

        // Verify pattern1 data integrity
        buffer.flip();
        for (int i = 0; i < pattern1.length; i++) {
            byte expected = (byte) (i % 256);
            byte actual = buffer.get();
            assertEquals("Data corruption at index " + i + " after first resize", expected, actual);
        }

        // Phase 2: Clear and fill with mixed data types to force different resize paths
        buffer.clear();

        // Add different data types forcing multiple different resizes
        for (int i = 0; i < 100; i++) {
            buffer.putInt(i); // 4 bytes
            buffer.putLong(i * 1000); // 8 bytes
            buffer.putDouble(i / 10.0); // 8 bytes

            // Add a string of varying length every 10 iterations to cause uneven resizes
            if (i % 10 == 0) {
                String text = "Test string " + i;
                byte[] textBytes = text.getBytes();
                buffer.put(textBytes);
            }
        }

        // Verify mixed data
        buffer.flip();
        for (int i = 0; i < 100; i++) {
            int readInt = buffer.getInt();
            assertEquals("Integer data corruption at iteration " + i, i, readInt);

            long readLong = buffer.getLong();
            assertEquals("Long data corruption at iteration " + i, i * 1000, readLong);

            double readDouble = buffer.getDouble();
            assertEquals("Double data corruption at iteration " + i, i / 10.0, readDouble, 0.0001);

            if (i % 10 == 0) {
                String expected = "Test string " + i;
                byte[] textBytes = expected.getBytes();
                byte[] readBytes = new byte[textBytes.length];
                buffer.get(readBytes);
                String readString = new String(readBytes);
                assertEquals("String data corruption at iteration " + i, expected, readString);
            }
        }

        // Phase 3: Test a single, very large resize
        buffer.clear();
        int smallInitialSize = buffer.capacity();

        // Create data 20x the current capacity to force significant resize
        byte[] hugeData = new byte[smallInitialSize * 20];
        for (int i = 0; i < hugeData.length; i++) {
            hugeData[i] = (byte) ((i * 17) % 256); // Use a different pattern
        }

        buffer.put(hugeData);

        // Verify huge data
        buffer.flip();
        for (int i = 0; i < hugeData.length; i++) {
            byte expected = (byte) ((i * 17) % 256);
            byte actual = buffer.get();
            assertEquals("Data corruption at index " + i + " after huge resize", expected, actual);
        }

        // Phase 4: Test resize at edge of current capacity
        buffer.clear();
        int currentCapacity = buffer.capacity();

        // Fill to exactly capacity-1
        byte[] almostFullData = new byte[currentCapacity - 1];
        for (int i = 0; i < almostFullData.length; i++) {
            almostFullData[i] = (byte) (i + 1);
        }
        buffer.put(almostFullData);

        // Add one more byte to trigger resize at the edge
        buffer.put((byte) 99);

        // Add more data after the resize
        byte[] additionalData = new byte[chunkSize * 3];
        for (int i = 0; i < additionalData.length; i++) {
            additionalData[i] = (byte) (200 - i);
        }
        buffer.put(additionalData);

        // Verify edge resize data
        buffer.flip();

        // Check almost full data
        for (int i = 0; i < almostFullData.length; i++) {
            byte expected = (byte) (i + 1);
            byte actual = buffer.get();
            assertEquals("Data corruption in almost-full section at index " + i, expected, actual);
        }

        // Check edge byte
        byte edgeByte = buffer.get();
        assertEquals("Edge byte corrupted", (byte) 99, edgeByte);

        // Check additional data
        for (int i = 0; i < additionalData.length; i++) {
            byte expected = (byte) (200 - i);
            byte actual = buffer.get();
            assertEquals("Data corruption in post-edge section at index " + i, expected, actual);
        }
    }
}
