/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.network.protocol;

import static org.junit.Assert.*;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.Message;
import com.jme3.network.serializing.Serializable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.ngengine.network.protocol.messages.BinaryMessage;
import org.ngengine.network.protocol.messages.ByteDataMessage;
import org.ngengine.network.protocol.messages.CompressedMessage;
import org.ngengine.network.protocol.messages.TextDataMessage;
import org.ngengine.network.protocol.messages.TextMessage;

public class TestDynamicSerializerProtocol {

    private DynamicSerializerProtocol protocol;

    // Custom serializable test message for SpiderMonkey compatibility
    @Serializable
    public static class TestMessage extends AbstractMessage {

        private int intValue;
        private String stringValue;
        private boolean boolValue;
        private float floatValue;
        private List<String> listValue;
        private Map<String, Integer> mapValue;

        public TestMessage() {}

        public TestMessage(int intValue, String stringValue, boolean boolValue, float floatValue) {
            this.intValue = intValue;
            this.stringValue = stringValue;
            this.boolValue = boolValue;
            this.floatValue = floatValue;
            this.listValue = new ArrayList<>();
            this.mapValue = new HashMap<>();
        }

        // Getters and setters omitted for brevity
        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        public int getIntValue() {
            return intValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setBoolValue(boolean boolValue) {
            this.boolValue = boolValue;
        }

        public boolean getBoolValue() {
            return boolValue;
        }

        public void setFloatValue(float floatValue) {
            this.floatValue = floatValue;
        }

        public float getFloatValue() {
            return floatValue;
        }

        public void setListValue(List<String> listValue) {
            this.listValue = listValue;
        }

        public List<String> getListValue() {
            return listValue;
        }

        public void setMapValue(Map<String, Integer> mapValue) {
            this.mapValue = mapValue;
        }

        public Map<String, Integer> getMapValue() {
            return mapValue;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestMessage that = (TestMessage) obj;
            return (
                intValue == that.intValue &&
                Float.compare(that.floatValue, floatValue) == 0 &&
                boolValue == that.boolValue &&
                Objects.equals(stringValue, that.stringValue) &&
                Objects.equals(listValue, that.listValue) &&
                Objects.equals(mapValue, that.mapValue)
            );
        }

        @Override
        public String toString() {
            return (
                "TestMessage{" +
                "intValue=" +
                intValue +
                ", stringValue='" +
                stringValue +
                '\'' +
                ", boolValue=" +
                boolValue +
                ", floatValue=" +
                floatValue +
                ", listValue=" +
                listValue +
                ", mapValue=" +
                mapValue +
                '}'
            );
        }
    }

    @Before
    public void setUp() {
        // Create protocol in SpiderMonkey compatible mode
        protocol = new DynamicSerializerProtocol(true);
        protocol.setForceStaticBuffer(false);
    }

    // Helper method for round-trip serialization testing
    private <T extends Message> T roundTripSerialize(T message) {
        ByteBuffer buffer = protocol.toByteBuffer(message, null);
        // copy buffer
        ByteBuffer copy = ByteBuffer.allocate(buffer.remaining());
        buffer.mark();
        copy.put(buffer);
        buffer.reset();
        buffer.flip();

        return (T) protocol.toMessage(copy);
    }

    @Test
    public void testPrimitiveTypes() {
        TestMessage original = new TestMessage(42, "test string", true, 3.14159f);
        TestMessage result = roundTripSerialize(original);

        assertEquals("Integer value should match", original.getIntValue(), result.getIntValue());
        assertEquals("String value should match", original.getStringValue(), result.getStringValue());
        assertEquals("Boolean value should match", original.getBoolValue(), result.getBoolValue());
        assertEquals("Float value should match", original.getFloatValue(), result.getFloatValue(), 0.0001f);
    }

    @Test
    public void testCollectionTypes() {
        TestMessage original = new TestMessage(1, "collection test", false, 1.0f);

        // Add different types of collections
        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        list.add("three");
        original.setListValue(list);

        TestMessage result = roundTripSerialize(original);

        assertNotNull("List shouldn't be null after serialization", result.getListValue());
        assertEquals("List size should match", original.getListValue().size(), result.getListValue().size());
        assertEquals("List contents should match", original.getListValue(), result.getListValue());
    }

    @Test
    public void testMapTypes() {
        TestMessage original = new TestMessage(2, "map test", true, 2.0f);

        // Add a map
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        original.setMapValue(map);

        TestMessage result = roundTripSerialize(original);

        assertNotNull("Map shouldn't be null after serialization", result.getMapValue());
        assertEquals("Map size should match", original.getMapValue().size(), result.getMapValue().size());
        assertEquals("Map contents should match", original.getMapValue(), result.getMapValue());
    }

    @Test
    public void testByteMessage() {
        byte[] data = "Hello, world!".getBytes();
        ByteDataMessage original = new BinaryMessage(ByteBuffer.wrap(data));

        ByteBuffer buffer = protocol.toByteBuffer(original, null);
        ByteDataMessage result = (ByteDataMessage) protocol.toMessage(buffer);

        byte[] resultData = new byte[result.getData().remaining()];
        result.getData().get(resultData);

        byte[] originalData = new byte[original.getData().remaining()];
        original.getData().get(originalData);

        assertArrayEquals("Byte array content should match", resultData, originalData);
    }

    @Test
    public void testTextMessage() {
        TextDataMessage original = new TextMessage("This is a text message");

        ByteBuffer buffer = protocol.toByteBuffer(original, null);
        TextDataMessage result = (TextDataMessage) protocol.toMessage(buffer);

        assertEquals("Text content should match", original.getData(), result.getData());
    }

    @Test
    public void testCompressedMessage() {
        // Create data that compresses well
        byte[] data = new byte[1000];
        Arrays.fill(data, (byte) 65); // Fill with 'A's
        BinaryMessage raw = new BinaryMessage(ByteBuffer.wrap(data));
        CompressedMessage original = new CompressedMessage(raw);

        ByteBuffer buffer = protocol.toByteBuffer(original, null);
        CompressedMessage result = (CompressedMessage) protocol.toMessage(buffer);

        BinaryMessage resultRaw = (BinaryMessage) result.getMessage();

        byte b0[] = new byte[raw.getData().remaining()];
        byte b1[] = new byte[resultRaw.getData().remaining()];
        raw.getData().get(b0);
        resultRaw.getData().get(b1);

        assertArrayEquals("Original data should match after decompression", b0, b1);
    }

    @Test
    public void testJME3MathTypes() {
        // Create a custom message to hold JME3 math types

        Vector2f vec2 = new Vector2f(1, 2);
        Vector3f vec3 = new Vector3f(1, 2, 3);
        Vector4f vec4 = new Vector4f(1, 2, 3, 4);
        ColorRGBA color = new ColorRGBA(0.1f, 0.2f, 0.3f, 1.0f);
        Matrix3f mat3 = new Matrix3f(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Matrix4f mat4 = new Matrix4f(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);

        MathMessage original = new MathMessage(vec2, vec3, vec4, color, mat3, mat4);

        ByteBuffer buffer = protocol.toByteBuffer(original, null);
        MathMessage result = (MathMessage) protocol.toMessage(buffer);

        assertEquals("Vector2f should match", original.getVec2(), result.getVec2());
        assertEquals("Vector3f should match", original.getVec3(), result.getVec3());
        assertEquals("Vector4f should match", original.getVec4(), result.getVec4());
        assertEquals("ColorRGBA should match", original.getColor(), result.getColor());
        assertEquals("Matrix3f should match", original.getMat3(), result.getMat3());
        assertEquals("Matrix4f should match", original.getMat4(), result.getMat4());
    }

    @Serializable
    public static class MathMessage extends AbstractMessage {

        private Vector2f vec2;
        private Vector3f vec3;
        private Vector4f vec4;
        private ColorRGBA color;
        private Matrix3f mat3;
        private Matrix4f mat4;

        public MathMessage() {}

        public MathMessage(Vector2f vec2, Vector3f vec3, Vector4f vec4, ColorRGBA color, Matrix3f mat3, Matrix4f mat4) {
            this.vec2 = vec2;
            this.vec3 = vec3;
            this.vec4 = vec4;
            this.color = color;
            this.mat3 = mat3;
            this.mat4 = mat4;
        }

        // Getters omitted for brevity
        public Vector2f getVec2() {
            return vec2;
        }

        public Vector3f getVec3() {
            return vec3;
        }

        public Vector4f getVec4() {
            return vec4;
        }

        public ColorRGBA getColor() {
            return color;
        }

        public Matrix3f getMat3() {
            return mat3;
        }

        public Matrix4f getMat4() {
            return mat4;
        }
    }

    @Serializable
    public static class DateTimeMessage extends AbstractMessage {

        private Date date;
        private Instant instant;
        private Duration duration;

        public DateTimeMessage() {}

        public DateTimeMessage(Date date, Instant instant, Duration duration) {
            this.date = date;
            this.instant = instant;
            this.duration = duration;
        }

        public Date getDate() {
            return date;
        }

        public Instant getInstant() {
            return instant;
        }

        public Duration getDuration() {
            return duration;
        }
    }

    @Test
    public void testDateTimeTypes() {
        Date date = new Date();
        Instant instant = Instant.now();
        Duration duration = Duration.ofSeconds(3600);

        DateTimeMessage original = new DateTimeMessage(date, instant, duration);

        ByteBuffer buffer = protocol.toByteBuffer(original, null);
        DateTimeMessage result = (DateTimeMessage) protocol.toMessage(buffer);

        assertEquals("Date should match", original.getDate(), result.getDate());
        assertEquals("Instant should match", original.getInstant().toEpochMilli(), result.getInstant().toEpochMilli());
        assertEquals("Duration should match", original.getDuration().toMillis(), result.getDuration().toMillis());
    }

    @Serializable
    public static class ArrayMessage extends AbstractMessage {

        private int[] intArray;
        private String[] stringArray;
        private boolean[] boolArray;

        public ArrayMessage() {}

        public ArrayMessage(int[] intArray, String[] stringArray, boolean[] boolArray) {
            this.intArray = intArray;
            this.stringArray = stringArray;
            this.boolArray = boolArray;
        }

        public int[] getIntArray() {
            return intArray;
        }

        public String[] getStringArray() {
            return stringArray;
        }

        public boolean[] getBoolArray() {
            return boolArray;
        }
    }

    @Test
    public void testArrays() {
        int[] intArray = { 1, 2, 3, 4, 5 };
        String[] stringArray = { "one", "two", "three" };
        boolean[] boolArray = { true, false, true };

        ArrayMessage original = new ArrayMessage(intArray, stringArray, boolArray);

        ByteBuffer buffer = protocol.toByteBuffer(original, null);
        ArrayMessage result = (ArrayMessage) protocol.toMessage(buffer);

        assertArrayEquals("Int array should match", original.getIntArray(), result.getIntArray());
        assertArrayEquals("String array should match", original.getStringArray(), result.getStringArray());
        assertArrayEquals("Boolean array should match", original.getBoolArray(), result.getBoolArray());
    }

    @Serializable
    public static class ChildObject extends AbstractMessage {

        private String name;
        private int age;

        public ChildObject() {}

        public ChildObject(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ChildObject that = (ChildObject) obj;
            return age == that.age && Objects.equals(name, that.name);
        }
    }

    @Serializable
    public static class NestedMessage extends AbstractMessage {

        private ChildObject child;
        private List<ChildObject> children;
        private Map<String, ChildObject> childMap;

        public NestedMessage() {}

        public NestedMessage(ChildObject child, List<ChildObject> children, Map<String, ChildObject> childMap) {
            this.child = child;
            this.children = children;
            this.childMap = childMap;
        }

        public ChildObject getChild() {
            return child;
        }

        public List<ChildObject> getChildren() {
            return children;
        }

        public Map<String, ChildObject> getChildMap() {
            return childMap;
        }
    }

    @Test
    public void testNestedObjects() {
        // Create test objects
        ChildObject child = new ChildObject("Test Child", 10);

        List<ChildObject> children = new ArrayList<>();
        children.add(new ChildObject("Child 1", 5));
        children.add(new ChildObject("Child 2", 7));

        Map<String, ChildObject> childMap = new HashMap<>();
        childMap.put("key1", new ChildObject("Map Child 1", 3));
        childMap.put("key2", new ChildObject("Map Child 2", 4));

        NestedMessage original = new NestedMessage(child, children, childMap);

        ByteBuffer buffer = protocol.toByteBuffer(original, null);
        NestedMessage result = (NestedMessage) protocol.toMessage(buffer);

        assertEquals("Child object should match", original.getChild(), result.getChild());

        assertEquals("Children list size should match", original.getChildren().size(), result.getChildren().size());
        for (int i = 0; i < original.getChildren().size(); i++) {
            assertEquals("Child at index " + i + " should match", original.getChildren().get(i), result.getChildren().get(i));
        }

        assertEquals("ChildMap size should match", original.getChildMap().size(), result.getChildMap().size());
        for (Map.Entry<String, ChildObject> entry : original.getChildMap().entrySet()) {
            assertTrue("Map should contain key: " + entry.getKey(), result.getChildMap().containsKey(entry.getKey()));
            assertEquals(
                "Child for key " + entry.getKey() + " should match",
                entry.getValue(),
                result.getChildMap().get(entry.getKey())
            );
        }
    }

    @Test
    public void testEdgeCases() {
        // Test with null values
        TestMessage nullMessage = new TestMessage(0, null, false, 0);
        nullMessage.setListValue(null);
        nullMessage.setMapValue(null);

        TestMessage nullResult = roundTripSerialize(nullMessage);
        assertNull("Null string should remain null", nullResult.getStringValue());

        // Test with empty collections
        TestMessage emptyCollections = new TestMessage(0, "", false, 0);
        emptyCollections.setListValue(new ArrayList<>());
        emptyCollections.setMapValue(new HashMap<>());

        TestMessage emptyResult = roundTripSerialize(emptyCollections);
        assertNotNull("Empty list should not be null", emptyResult.getListValue());
        assertEquals("Empty list should have size 0", 0, emptyResult.getListValue().size());
        assertNotNull("Empty map should not be null", emptyResult.getMapValue());
        assertEquals("Empty map should have size 0", 0, emptyResult.getMapValue().size());
    }

    @NetworkSafe
    public static class ComboMessage extends AbstractMessage {

        public boolean b;
        public int i;
        public String s;
        public float f;

        public ComboMessage() {}

        public ComboMessage(boolean b, int i, String s, float f) {
            this.b = b;
            this.i = i;
            this.s = s;
            this.f = f;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ComboMessage)) return false;
            ComboMessage other = (ComboMessage) o;
            return b == other.b && i == other.i && Objects.equals(s, other.s) && f == other.f;
        }
    }

    @Test
    public void testPrimitiveOrderCombinations() {
        // Test all permutations of primitive fields

        List<ComboMessage> combos = Arrays.asList(
            new ComboMessage(true, 123, "abc", 1.23f),
            new ComboMessage(false, -1, "", -0.0f),
            new ComboMessage(true, Integer.MAX_VALUE, "xyz", Float.MAX_VALUE)
        );
        for (ComboMessage original : combos) {
            ByteBuffer buffer = protocol.toByteBuffer(original, null);
            ComboMessage result = (ComboMessage) protocol.toMessage(buffer);
            assertEquals(original, result);
        }
    }

    @Test
    public void testStreamOfDifferentObjects() {
        // Serialize a stream of different objects, then deserialize in order
        List<Message> messages = new ArrayList<>();
        messages.add(new TestMessage(1, "one", true, 1.1f));
        messages.add(new TestMessage(2, "two", false, 2.2f));
        messages.add(new ArrayMessage(new int[] { 1, 2 }, new String[] { "a", "b" }, new boolean[] { true, false }));
        messages.add(new TestMessage(3, null, true, 3.3f));
        messages.add(new ArrayMessage(new int[] {}, new String[] {}, new boolean[] {}));

        // Serialize all to a single buffer
        GrowableByteBuffer streamBuffer = new GrowableByteBuffer(ByteBuffer.allocate(1024), 1024);
        for (Message msg : messages) {
            ByteBuffer buf = protocol.toByteBuffer(msg, null);
            streamBuffer.putInt(buf.remaining());
            streamBuffer.put(buf);
        }
        ByteBuffer readBuffer = streamBuffer.getBuffer();
        readBuffer.flip();

        // Deserialize in order
        for (Message original : messages) {
            int len = readBuffer.getInt();
            ByteBuffer slice = readBuffer.slice();
            slice.limit(len);
            Message result = protocol.toMessage(slice);
            assertEquals(original.getClass(), result.getClass());
            // For array and test messages, check equality
            if (original instanceof TestMessage) {
                assertEquals(original, result);
            } else if (original instanceof ArrayMessage) {
                ArrayMessage oArr = (ArrayMessage) original;
                ArrayMessage rArr = (ArrayMessage) result;
                assertArrayEquals(oArr.getIntArray(), rArr.getIntArray());
                assertArrayEquals(oArr.getStringArray(), rArr.getStringArray());
                assertArrayEquals(oArr.getBoolArray(), rArr.getBoolArray());
            }
            readBuffer.position(readBuffer.position() + len);
        }
    }

    @Test
    public void testNullAndEmptyStrings() {
        TestMessage nullString = new TestMessage(0, null, false, 0f);
        TestMessage emptyString = new TestMessage(0, "", false, 0f);
        assertEquals(nullString, roundTripSerialize(nullString));
        assertEquals(emptyString, roundTripSerialize(emptyString));
    }

    @Test
    public void testExtremePrimitiveValues() {
        TestMessage minMax = new TestMessage(Integer.MIN_VALUE, "min", false, Float.MIN_VALUE);
        TestMessage maxMax = new TestMessage(Integer.MAX_VALUE, "max", true, Float.MAX_VALUE);
        assertEquals(minMax, roundTripSerialize(minMax));
        assertEquals(maxMax, roundTripSerialize(maxMax));
    }

    @Test
    public void testNestedEmptyCollections() {
        TestMessage msg = new TestMessage(0, "nested", false, 0f);
        msg.setListValue(Arrays.asList());
        msg.setMapValue(Collections.emptyMap());
        TestMessage result = roundTripSerialize(msg);
        assertNotNull(result.getListValue());
        assertNotNull(result.getMapValue());
        assertTrue(result.getListValue().isEmpty());
        assertTrue(result.getMapValue().isEmpty());
    }

    @Test
    public void testPrimitiveArrayEdgeCases() {
        ArrayMessage empty = new ArrayMessage(new int[0], new String[0], new boolean[0]);
        ArrayMessage single = new ArrayMessage(new int[] { 42 }, new String[] { "x" }, new boolean[] { true });
        ArrayMessage mixed = new ArrayMessage(new int[] { -1, 0, 1 }, new String[] { "a", "b" }, new boolean[] { false, true });
        assertArrayEquals(empty.getIntArray(), roundTripSerialize(empty).getIntArray());
        assertArrayEquals(single.getIntArray(), roundTripSerialize(single).getIntArray());
        assertArrayEquals(mixed.getIntArray(), roundTripSerialize(mixed).getIntArray());
        assertArrayEquals(single.getBoolArray(), roundTripSerialize(single).getBoolArray());
        assertArrayEquals(mixed.getBoolArray(), roundTripSerialize(mixed).getBoolArray());
    }

    @Test
    public void testUnicodeAndSpecialStrings() {
        TestMessage unicode = new TestMessage(0, "こんにちは世界🌏", true, 0f);
        TestMessage special = new TestMessage(0, "line1\nline2\t\u0000", false, 0f);
        assertEquals(unicode, roundTripSerialize(unicode));
        assertEquals(special, roundTripSerialize(special));
    }

    @Test
    public void testMultipleMessagesInOneBuffer() {
        // Serialize several messages into one buffer, then read them back
        TestMessage m1 = new TestMessage(1, "a", true, 1.0f);
        TestMessage m2 = new TestMessage(2, "b", false, 2.0f);
        TestMessage m3 = new TestMessage(3, "c", true, 3.0f);

        ByteBuffer b1 = protocol.toByteBuffer(m1, null);
        ByteBuffer b2 = protocol.toByteBuffer(m2, null);
        ByteBuffer b3 = protocol.toByteBuffer(m3, null);

        GrowableByteBuffer stream = new GrowableByteBuffer(ByteBuffer.allocate(1024), 1024);
        stream.putInt(b1.remaining());
        stream.put(b1);
        stream.putInt(b2.remaining());
        stream.put(b2);
        stream.putInt(b3.remaining());
        stream.put(b3);

        ByteBuffer all = stream.getBuffer();
        all.flip();

        for (TestMessage expected : Arrays.asList(m1, m2, m3)) {
            int len = all.getInt();
            ByteBuffer slice = all.slice();
            slice.limit(len);
            TestMessage actual = (TestMessage) protocol.toMessage(slice);
            assertEquals(expected, actual);
            all.position(all.position() + len);
        }
    }

    @Test
    public void testNullFieldsInNestedObjects() {
        NestedMessage original = new NestedMessage(null, null, null);
        NestedMessage result = roundTripSerialize(original);
        assertNull(result.getChild());
        assertNull(result.getChildren());
        assertNull(result.getChildMap());
    }

    @NetworkSafe
    public static class DeepMessage extends AbstractMessage {

        public Map<String, List<List<String>>> map;

        public DeepMessage() {}

        public DeepMessage(Map<String, List<List<String>>> map) {
            this.map = map;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DeepMessage)) return false;
            return Objects.equals(map, ((DeepMessage) o).map);
        }
    }

    @Test
    public void testDeeplyNestedCollections() {
        // Map<String, List<List<String>>>
        Map<String, List<List<String>>> deepMap = new HashMap<>();
        deepMap.put("a", Arrays.asList(Arrays.asList("x", "y"), Arrays.asList("z")));
        deepMap.put("b", Arrays.asList(Arrays.asList("1", "2")));
        // Wrap in a message

        DeepMessage original = new DeepMessage(deepMap);
        DeepMessage result = roundTripSerialize(original);
        assertEquals(original, result);
    }
}
